package com.atguigu.tingshu.user.service.impl;

import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.*;
import com.atguigu.tingshu.user.mapper.*;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.user.service.UserPaidTrackService;
import com.atguigu.tingshu.user.strategy.ItemTypeStrategy;
import com.atguigu.tingshu.user.strategy.StrategyFactory;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService
{

    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;
    @Autowired
    private UserPaidTrackService userPaidTrackService;
    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;
    @Autowired
    private TrackInfoFeignClient trackInfoFeignClient;
    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;
    @Autowired
    private UserVipServiceMapper userVipServiceMapper;
    @Autowired
    private StrategyFactory strategyFactory;


    @Override
    public Map<Long, Integer> userIsPaidTrack(Long userId,
                                              Long albumId,
                                              List<Long> trackIdList)
    {
        LambdaQueryWrapper<UserPaidAlbum> userPaidAlbumLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userPaidAlbumLambdaQueryWrapper.eq(UserPaidAlbum::getUserId,
                                           userId)
                                       .eq(UserPaidAlbum::getAlbumId,
                                           albumId);
        UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(userPaidAlbumLambdaQueryWrapper);
        if (null != userPaidAlbum)
        {
            HashMap<Long, Integer> map = new HashMap<>();
            trackIdList.forEach(trackId ->
                                {
                                    map.put(trackId,
                                            1);
                                });
            return map;
        }
        else
        {
            LambdaQueryWrapper<UserPaidTrack> userPaidTrackLambdaQueryWrapper = new LambdaQueryWrapper<>();
            userPaidTrackLambdaQueryWrapper.eq(UserPaidTrack::getUserId,
                                               userId)
                                           .in(UserPaidTrack::getTrackId,
                                               trackIdList);
            List<UserPaidTrack> userPaidTrackList = userPaidTrackService.list(userPaidTrackLambdaQueryWrapper);
            List<Long> userPaidTrackIdList = userPaidTrackList.stream()
                                                              .map(UserPaidTrack::getTrackId)
                                                              .collect(Collectors.toList());
            HashMap<Long, Integer> map = new HashMap<>();
            trackIdList.forEach(trackId ->
                                {
                                    if (userPaidTrackIdList.contains(trackId))
                                    {
                                        map.put(trackId,
                                                1);
                                    }
                                    else
                                    {
                                        map.put(trackId,
                                                0);
                                    }
                                });
            return map;
        }
    }

    @Override
    public Boolean isPaidAlbum(Long userId,
                               Long albumId)
    {
        Long count = userPaidAlbumMapper.selectCount(
                new LambdaQueryWrapper<UserPaidAlbum>().eq(UserPaidAlbum::getUserId,
                                                           userId)
                                                       .eq(UserPaidAlbum::getAlbumId,
                                                           albumId));
        if (count > 0)
        {
            return true;
        }
        return false;
    }

    @Override
    public List<Long> findUserPaidTrackList(Long userId,
                                            Long albumId)
    {
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(
                new LambdaQueryWrapper<UserPaidTrack>().eq(UserPaidTrack::getUserId,
                                                           userId)
                                                       .eq(UserPaidTrack::getAlbumId,
                                                           albumId));
        List<Long> trackIdList = userPaidTrackList.stream()
                                                  .map(UserPaidTrack::getTrackId)
                                                  .collect(Collectors.toList());
        return trackIdList;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo)
    {
        if (userPaidRecordVo.getItemType()
                            .equals(SystemConstant.ORDER_ITEM_TYPE_ALBUM))
        {
            LambdaQueryWrapper<UserPaidAlbum> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserPaidAlbum::getUserId, userPaidRecordVo.getUserId());
            wrapper.eq(UserPaidAlbum::getOrderNo, userPaidRecordVo.getOrderNo());

            UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(wrapper);
            if (userPaidAlbum != null)
            {
                return;
            }

            userPaidAlbum = new UserPaidAlbum();
            userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
            userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
            userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList()
                                                     .get(0));
            userPaidAlbumMapper.insert(userPaidAlbum);
        }
        else if (userPaidRecordVo.getItemType()
                                 .equals(SystemConstant.ORDER_ITEM_TYPE_TRACK))
        {
            LambdaQueryWrapper<UserPaidTrack> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserPaidTrack::getUserId, userPaidRecordVo.getUserId());
            wrapper.eq(UserPaidTrack::getOrderNo, userPaidRecordVo.getOrderNo());

            Long count = userPaidTrackMapper.selectCount(wrapper);
            if (count > 0)
            {
                return;
            }

            Result<TrackInfo> trackInfoResult =
                    trackInfoFeignClient.getTrackInfo(userPaidRecordVo.getItemIdList()
                                                                      .get(0));
            TrackInfo trackInfo = trackInfoResult.getData();

            userPaidRecordVo.getItemIdList()
                            .forEach(trackId ->
                                     {
                                         UserPaidTrack userPaidTrack = new UserPaidTrack();
                                         userPaidTrack.setTrackId(trackId);
                                         userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
                                         userPaidTrack.setUserId(userPaidRecordVo.getUserId());
                                         userPaidTrack.setAlbumId(trackInfo.getAlbumId());
                                         userPaidTrackMapper.insert(userPaidTrack);
                                     });
        }
        else
        {
            UserInfo userInfo = userInfoMapper.selectById(userPaidRecordVo.getUserId());

            LocalDateTime baseTime = LocalDateTime.now();

            if (userInfo.getIsVip() != null
                    && userInfo.getIsVip()
                               .intValue() == 1
                    && userInfo.getVipExpireTime() != null
                    && userInfo.getVipExpireTime()
                               .after(new Date()))
            {
                baseTime = LocalDateTime.ofInstant(
                        userInfo.getVipExpireTime()
                                .toInstant(),
                        ZoneId.systemDefault()
                );
            }

            VipServiceConfig vipServiceConfig =
                    vipServiceConfigMapper.selectById(userPaidRecordVo.getItemIdList()
                                                                      .get(0));
            Integer serviceMonth = vipServiceConfig.getServiceMonth();

            LocalDateTime expireLocalDateTime = baseTime.plusMonths(serviceMonth);

            Date expireTime = Date.from(
                    expireLocalDateTime.atZone(ZoneId.systemDefault())
                                       .toInstant()
            );

            UserVipService userVipService = new UserVipService();
            userVipService.setOrderNo(userPaidRecordVo.getOrderNo());
            userVipService.setUserId(userPaidRecordVo.getUserId());
            userVipService.setStartTime(new Date());
            userVipService.setExpireTime(expireTime);
            userVipServiceMapper.insert(userVipService);

            userInfo.setIsVip(1);
            userInfo.setVipExpireTime(expireTime);
            this.userInfoMapper.updateById(userInfo);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userPayRecord(UserPaidRecordVo userPaidRecordVo)
    {
        ItemTypeStrategy strategy = strategyFactory.getStrategy(userPaidRecordVo.getItemType());
        strategy.savePaidRecord(userPaidRecordVo);
    }
}
