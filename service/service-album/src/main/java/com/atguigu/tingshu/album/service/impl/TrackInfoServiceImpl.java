package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.user.client.impl.UserInfoDegradeFeignClient;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService
{

    @Autowired
    private TrackInfoMapper trackInfoMapper;
    @Autowired
    private MinioConstantProperties minioConstantProperties;
    @Autowired
    private VodService vodService;
    @Autowired
    private AlbumInfoMapper albumInfoMapper;
    @Autowired
    private TrackStatMapper trackStatMapper;
    @Autowired
    private UserInfoDegradeFeignClient userInfoFeignClient;
    @Autowired
    private AlbumInfoService albumInfoService;


    @Override
    public Map<String, Object> uploadTrack(MultipartFile file)
    {
        MinioClient minioClient = MinioClient.builder()
                                             .endpoint(minioConstantProperties.getEndpointUrl())
                                             .credentials(minioConstantProperties.getAccessKey(),
                                                          minioConstantProperties.getSecreKey())
                                             .build();
        try
        {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                                                                     .bucket(minioConstantProperties.getBucketName())
                                                                     .build());
            if (!found)
            {
                minioClient.makeBucket(MakeBucketArgs.builder()
                                                     .bucket(minioConstantProperties.getBucketName())
                                                     .build());
            }
            String fileName = UUID.randomUUID()
                                  .toString()
                                  .replace("-",
                                           "") + "." + FilenameUtils.getExtension(file.getOriginalFilename());
            String objectName = "track/" + fileName;
            minioClient.putObject(PutObjectArgs.builder()
                                               .bucket(minioConstantProperties.getBucketName())
                                               .object(objectName)
                                               .stream(file.getInputStream(),
                                                       file.getSize(),
                                                       -1)
                                               .contentType(file.getContentType())
                                               .build());
            return Map.of("url",
                          minioConstantProperties.getEndpointUrl() + "/" + minioConstantProperties.getBucketName() + "/" + objectName);

        }
        catch (Exception e)
        {
            log.error("上传失败");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveTrackInfo(TrackInfoVo trackInfoVo)
    {
        //1 添加声音基本信息 track_info
        TrackInfo trackInfo = new TrackInfo();
        BeanUtils.copyProperties(trackInfoVo,
                                 trackInfo);

        //需要手动设置
        //用户id
        //TODO 后续完善登录功能之后需要来这个改善这个逻辑的
        trackInfo.setUserId(AuthContextHolder.getUserId());


        LambdaQueryWrapper<TrackInfo> wrapper = new LambdaQueryWrapper<TrackInfo>();
        wrapper.select(TrackInfo::getOrderNum);
        wrapper.eq(TrackInfo::getAlbumId,
                   trackInfoVo.getAlbumId());
        wrapper.orderByDesc(TrackInfo::getId);
        wrapper.last(" limit 1 ");
        TrackInfo trackInfo_ordernum = trackInfoMapper.selectOne(wrapper);

        int orderNum = 1;
        if (null != trackInfo_ordernum)
        {
            orderNum = trackInfo_ordernum.getOrderNum() + 1;
        }
        trackInfo.setOrderNum(orderNum);

        TrackMediaInfoVo trackMediaInfoVo = vodService.getmediaInfoByFileId(trackInfoVo.getMediaFileId());
        trackInfo.setMediaDuration(trackMediaInfoVo.getDuration());
        trackInfo.setMediaSize(trackMediaInfoVo.getSize());
        trackInfo.setMediaUrl(trackMediaInfoVo.getMediaUrl());
        trackInfo.setMediaType(trackMediaInfoVo.getType());
        trackInfoMapper.insert(trackInfo);
        AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfoVo.getAlbumId());
        Integer includeTrackCount = albumInfo.getIncludeTrackCount();
        albumInfo.setIncludeTrackCount(includeTrackCount + 1);
        albumInfoMapper.updateById(albumInfo);
        this.saveTrackStat(trackInfo.getId(),
                           SystemConstant.TRACK_STAT_PLAY);
        this.saveTrackStat(trackInfo.getId(),
                           SystemConstant.TRACK_STAT_COLLECT);
        this.saveTrackStat(trackInfo.getId(),
                           SystemConstant.TRACK_STAT_PRAISE);
        this.saveTrackStat(trackInfo.getId(),
                           SystemConstant.TRACK_STAT_COMMENT);
    }

    @Override
    public IPage<TrackListVo> findUserTrackPage(Page<TrackListVo> trackListVoPage,
                                                TrackInfoQuery trackInfoQuery)
    {
        return trackInfoMapper.selectUserTrackPage(trackListVoPage,
                                                   trackInfoQuery);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeTrackInfo(Long id)
    {
        TrackInfo trackInfo = this.getById(id);
        AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());
        Integer includeTrackCount = albumInfo.getIncludeTrackCount();
        albumInfo.setIncludeTrackCount(includeTrackCount - 1);
        albumInfoMapper.updateById(albumInfo);
        trackStatMapper.delete(new LambdaQueryWrapper<TrackStat>().eq(TrackStat::getTrackId,
                                                                      id));
        trackInfoMapper.updateTrackNum(trackInfo.getAlbumId(),
                                       trackInfo.getOrderNum());
        vodService.removeTrack(trackInfo.getMediaFileId());
    }

    @Override
    public void updateTrackInfo(Long id,
                                TrackInfoVo trackInfoVo)
    {
        TrackInfo trackInfo = this.getById(id);
        String mediaFileId = trackInfo.getMediaFileId();
        BeanUtils.copyProperties(trackInfoVo,
                                 trackInfo);
        if (!trackInfoVo.getMediaFileId()
                        .equals(mediaFileId))
        {
            TrackMediaInfoVo trackMediaInfoVo = vodService.getmediaInfoByFileId(trackInfoVo.getMediaFileId());
            if (null == trackMediaInfoVo)
            {
                throw new GuiguException(ResultCodeEnum.VOD_FILE_ID_ERROR);
            }
            trackInfo.setMediaUrl(trackMediaInfoVo.getMediaUrl());
            trackInfo.setMediaType(trackMediaInfoVo.getType());
            trackInfo.setMediaDuration(trackMediaInfoVo.getDuration());
            trackInfo.setMediaSize(trackMediaInfoVo.getSize());
            vodService.removeTrack(mediaFileId);
        }
        this.updateById(trackInfo);
    }

    @Override
    public IPage<AlbumTrackListVo> findAlbumTrackPage(Page<AlbumTrackListVo> pageParam,
                                                      Long albumId,
                                                      Long userId)
    {
        IPage<AlbumTrackListVo> pageInfo = trackInfoMapper.selectAlbumTrackPage(pageParam,
                                                                                albumId);

        AlbumInfo albumInfo = albumInfoService.getById(albumId);
        Assert.notNull(albumInfo,
                       "专辑对象不能为空");
        if (null == userId)
        {
            if (!SystemConstant.ALBUM_PAY_TYPE_FREE.equals(albumInfo.getPayType()))
            {
                List<AlbumTrackListVo> albumTrackNeedPaidListVoList = pageInfo.getRecords()
                                                                              .stream()
                                                                              .filter(albumTrackListVo -> albumTrackListVo.getOrderNum()
                                                                                                                          .intValue() > albumInfo.getTracksForFree())
                                                                              .collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(albumTrackNeedPaidListVoList))
                {
                    albumTrackNeedPaidListVoList.forEach(albumTrackListVo ->
                                                         {
                                                             albumTrackListVo.setIsShowPaidMark(true);
                                                         });
                }
            }
        }
        else
        {
            boolean isNeedPaid = false;
            if (SystemConstant.ALBUM_PAY_TYPE_VIPFREE.equals(albumInfo.getPayType()))
            {
                Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(userId);
                Assert.notNull(userInfoVoResult,
                               "用户信息不能为空");
                UserInfoVo userInfoVo = userInfoVoResult.getData();
                if (userInfoVo.getIsVip()
                              .intValue() == 0)
                {
                    isNeedPaid = true;
                }
                if (userInfoVo.getIsVip()
                              .intValue() == 1 && userInfoVo.getVipExpireTime()
                                                            .before(new Date()))
                {
                    isNeedPaid = true;
                }
            }
            else if (SystemConstant.ALBUM_PAY_TYPE_REQUIRE.equals(albumInfo.getPayType()))
            {
                isNeedPaid = true;
            }
            if (isNeedPaid)
            {
                List<AlbumTrackListVo> albumTrackNeedPaidListVoList = pageInfo.getRecords()
                                                                              .stream()
                                                                              .filter(albumTrackListVo -> albumTrackListVo.getOrderNum()
                                                                                                                          .intValue() > albumInfo.getTracksForFree())
                                                                              .collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(albumTrackNeedPaidListVoList))
                {
                    List<Long> trackIdList = albumTrackNeedPaidListVoList.stream()
                                                                         .map(AlbumTrackListVo::getTrackId)
                                                                         .collect(Collectors.toList());
                    Result<Map<Long, Integer>> mapResult = userInfoFeignClient.userIsPaidTrack(albumId,
                                                                                               trackIdList);
                    Assert.notNull(mapResult,
                                   "声音集合不能为空.");
                    Map<Long, Integer> map = mapResult.getData();
                    Assert.notNull(map,
                                   "map集合不能为空.");
                    albumTrackNeedPaidListVoList.forEach(albumTrackListVo ->
                                                         {
                                                             boolean isBuy = map.get(
                                                                     albumTrackListVo.getTrackId()) == 1 ? false : true;
                                                             albumTrackListVo.setIsShowPaidMark(isBuy);
                                                         });
                }
            }
        }
        return pageInfo;
    }

    @Transactional
    @Override
    public void updateStat(Long albumId,
                           Long trackId,
                           String statType,
                           Integer count)
    {
        trackInfoMapper.updateStat(trackId,
                                   statType,
                                   count);
        if (statType.equals(SystemConstant.TRACK_STAT_PLAY))
        {
            albumInfoService.updateStat(albumId,
                                        SystemConstant.ALBUM_STAT_PLAY,
                                        count);
        }
    }

    @Override
    public List<Map<String, Object>> findUserTrackPaidList(Long trackId)
    {
        TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
        AlbumInfo albumInfo = albumInfoService.getById(trackInfo.getAlbumId());
        Result<List<Long>> trackIdListResult = userInfoFeignClient.findUserPaidTrackList(trackInfo.getAlbumId());
        Assert.notNull(trackIdListResult,
                       "专辑Id集合不为空");
        List<Long> trackIdList = trackIdListResult.getData();
        Assert.notNull(trackIdList,
                       "声音专辑Id 不为空");

        List<TrackInfo> trackInfoList = trackInfoMapper.selectList(new LambdaQueryWrapper<TrackInfo>().eq(
                                                                                                              TrackInfo::getAlbumId,
                                                                                                              trackInfo.getAlbumId())
                                                                                                      .gt(TrackInfo::getOrderNum,
                                                                                                          trackInfo.getOrderNum())
                                                                                                      .select(TrackInfo::getId));
        List<Long> trackIdAllList = trackInfoList.stream()
                                                 .map(TrackInfo::getId)
                                                 .collect(Collectors.toList());
        List<Long> trackIdNoReaptList = new ArrayList<>();
        if (CollectionUtils.isEmpty(trackIdList))
        {
            trackIdNoReaptList = trackIdAllList;
        }
        else
        {
            trackIdNoReaptList = trackIdAllList.stream()
                                               .filter(itemId -> !trackIdList.contains(itemId))
                                               .collect(Collectors.toList());
        }

        List<Map<String, Object>> list = new ArrayList<>();
        if (trackIdNoReaptList.size() >= 0)
        {
            Map<String, Object> map = new HashMap<>();
            map.put("name",
                    "本集");
            map.put("price",
                    albumInfo.getPrice());
            map.put("trackCount",
                    0);
            list.add(map);
        }

        if (trackIdNoReaptList.size() > 0 && trackIdNoReaptList.size() <= 10)
        {
            Map<String, Object> map = new HashMap<>();
            int count = trackIdNoReaptList.size();
            BigDecimal price = albumInfo.getPrice()
                                        .multiply(new BigDecimal(count));
            map.put("name",
                    "后" + trackIdNoReaptList.size() + "集");
            map.put("price",
                    price);
            map.put("trackCount",
                    count);
            list.add(map);
        }
        if (trackIdNoReaptList.size() > 10)
        {
            Map<String, Object> map = new HashMap<>();
            BigDecimal price = albumInfo.getPrice()
                                        .multiply(new BigDecimal(10));
            map.put("name",
                    "后10集");
            map.put("price",
                    price);
            map.put("trackCount",
                    10);
            list.add(map);
        }
        if (trackIdNoReaptList.size() > 10 && trackIdNoReaptList.size() <= 20)
        {
            Map<String, Object> map = new HashMap<>();
            int count = trackIdNoReaptList.size();
            BigDecimal price = albumInfo.getPrice()
                                        .multiply(new BigDecimal(count));
            map.put("name",
                    "后" + count + "集");
            map.put("price",
                    price);
            map.put("trackCount",
                    count);
            list.add(map);
        }
        if (trackIdNoReaptList.size() > 20)
        {
            Map<String, Object> map = new HashMap<>();
            BigDecimal price = albumInfo.getPrice()
                                        .multiply(new BigDecimal(20));
            map.put("name",
                    "后20集");
            map.put("price",
                    price);
            map.put("trackCount",
                    20);
            list.add(map);
        }

        if (trackIdNoReaptList.size() > 20 && trackIdNoReaptList.size() <= 30)
        {
            Map<String, Object> map = new HashMap<>();
            int count = trackIdNoReaptList.size();
            BigDecimal price = albumInfo.getPrice()
                                        .multiply(new BigDecimal(count));
            map.put("name",
                    "后" + count + "集");
            map.put("price",
                    price);
            map.put("trackCount",
                    count);
            list.add(map);
        }
        if (trackIdNoReaptList.size() > 30)
        {
            Map<String, Object> map = new HashMap<>();
            BigDecimal price = albumInfo.getPrice()
                                        .multiply(new BigDecimal(30));
            map.put("name",
                    "后30集");
            map.put("price",
                    price);
            map.put("trackCount",
                    30);
            list.add(map);
        }

        if (trackIdNoReaptList.size() > 30 && trackIdNoReaptList.size() <= 50)
        {
            Map<String, Object> map = new HashMap<>();
            int count = trackIdNoReaptList.size();
            BigDecimal price = albumInfo.getPrice()
                                        .multiply(new BigDecimal(count));
            map.put("name",
                    "后" + count + "集");
            map.put("price",
                    price);
            map.put("trackCount",
                    count);
            list.add(map);
        }
        if (trackIdNoReaptList.size() > 50)
        {
            Map<String, Object> map = new HashMap<>();
            BigDecimal price = albumInfo.getPrice()
                                        .multiply(new BigDecimal(50));
            map.put("name",
                    "后50集");
            map.put("price",
                    price);
            map.put("trackCount",
                    50);
            list.add(map);
        }
        return list;
    }

    @Override
    public List<TrackInfo> findPaidTrackInfoList(Long trackId, Integer trackCount)
    {
        TrackInfo trackInfo = this.getById(trackId);
        Assert.notNull(trackInfo, "声音对象不能为空");

        Result<List<Long>> trackIdListResult = userInfoFeignClient.findUserPaidTrackList(trackInfo.getAlbumId());
        Assert.notNull(trackIdListResult);
        List<Long> trackIdList = trackIdListResult.getData();
        Assert.notNull(trackIdList);
        List<TrackInfo> trackInfoList = new ArrayList<>();
        if (trackCount > 0)
        {
            LambdaQueryWrapper<TrackInfo> trackInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
            trackInfoLambdaQueryWrapper.eq(TrackInfo::getAlbumId, trackInfo.getAlbumId());
            trackInfoLambdaQueryWrapper.gt(TrackInfo::getOrderNum, trackInfo.getOrderNum());
            trackInfoLambdaQueryWrapper.orderByAsc(TrackInfo::getOrderNum);
            if (!CollectionUtils.isEmpty(trackIdList))
            {
                trackInfoLambdaQueryWrapper.notIn(TrackInfo::getId, trackIdList);
            }
            trackInfoLambdaQueryWrapper.last("limit " + trackCount);
            trackInfoList = this.list(trackInfoLambdaQueryWrapper);
        }
        else
        {
            trackInfoList.add(trackInfo);
        }
        return trackInfoList;
    }

    private void saveTrackStat(Long trackId,
                               String trackType)
    {
        TrackStat trackStat = new TrackStat();
        trackStat.setTrackId(trackId);
        trackStat.setStatType(trackType);


        trackStat.setStatNum(0);
        this.trackStatMapper.insert(trackStat);
    }
}
