package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService
{

    @Autowired
    private AlbumInfoMapper albumInfoMapper;
    @Autowired
    private AlbumAttributeValueMapper albumAttributeValueMapper;
    @Autowired
    private AlbumStatMapper albumStatMapper;
    @Autowired
    private AlbumAttributeValueService albumAttributeValueService;
    @Autowired
    private TrackInfoMapper trackInfoMapper;
    @Autowired
    private RabbitService rabbitService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAlbumInfo(AlbumInfoVo albumInfoVo)
    {
        AlbumInfo albumInfo = new AlbumInfo();
        BeanUtils.copyProperties(albumInfoVo,
                albumInfo);

        //TODO userId 用户 id 需要完善
        albumInfo.setUserId(1L);
        albumInfo.setStatus(SystemConstant.ALBUM_STATUS_PASS);
        String payType = albumInfo.getPayType();
        if (SystemConstant.ALBUM_PAY_TYPE_FREE.equals(payType))
            albumInfo.setTracksForFree(3);
        albumInfoMapper.insert(albumInfo);

        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        List<AlbumAttributeValue> albumAttributeValues = albumAttributeValueVoList.stream().map(albumAttributeValueVo ->
        {
            AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
            BeanUtils.copyProperties(albumAttributeValueVo,
                    albumAttributeValue);
            albumAttributeValue.setAlbumId(albumInfo.getId());
            return albumAttributeValue;
        }).collect(Collectors.toList());
        albumAttributeValueService.saveBatch(albumAttributeValues);

        this.saveAlbumStat(albumInfo.getId(),
                SystemConstant.ALBUM_STAT_PLAY);
        this.saveAlbumStat(albumInfo.getId(),
                SystemConstant.ALBUM_STAT_BROWSE);
        this.saveAlbumStat(albumInfo.getId(),
                SystemConstant.ALBUM_STAT_COMMENT);
        this.saveAlbumStat(albumInfo.getId(),
                SystemConstant.ALBUM_STAT_SUBSCRIBE);
        String isOpen = albumInfo.getIsOpen();
        if ("1".equals(isOpen))
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM,
                    MqConst.ROUTING_ALBUM_UPPER,
                    albumInfo.getId());


    }

    @Override
    public IPage<AlbumInfoVo> selectAlbumPage(Page<AlbumInfoVo> pageParam,
                                              AlbumInfoQuery albumInfoQuery)
    {
        return albumInfoMapper.selectAlbumPage(pageParam,
                albumInfoQuery);
    }

    @Override
    public void removeAlbumInfo(Long albumId)
    {
        LambdaQueryWrapper<TrackInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrackInfo::getAlbumId,
                albumId);
        Long count = trackInfoMapper.selectCount(queryWrapper);
        if (count > 0)
        {
            throw new RuntimeException("专辑下有声音，请先删除专辑下的声音");
        }
        albumInfoMapper.deleteById(albumId);
        LambdaQueryWrapper<AlbumAttributeValue> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AlbumAttributeValue::getAlbumId,
                albumId);
        albumAttributeValueMapper.delete(lambdaQueryWrapper);
        LambdaQueryWrapper<AlbumStat> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
        lambdaQueryWrapper1.eq(AlbumStat::getAlbumId,
                albumId);
        albumStatMapper.delete(lambdaQueryWrapper1);
        rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM,
                MqConst.ROUTING_ALBUM_LOWER,
                albumId);
    }


    @Override
    public AlbumInfo getAlbumInfo(Long albumId)
    {
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        LambdaQueryWrapper<AlbumAttributeValue> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AlbumAttributeValue::getAlbumId,
                albumId);
        List<AlbumAttributeValue> albumAttributeValues = albumAttributeValueMapper.selectList(queryWrapper);
        albumInfo.setAlbumAttributeValueVoList(albumAttributeValues);
        return albumInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlbumInfo(Long albumId,
                                AlbumInfoVo albumInfoVo)
    {
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        BeanUtils.copyProperties(albumInfoVo,
                albumInfo);
        albumInfoMapper.updateById(albumInfo);
        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        LambdaQueryWrapper<AlbumAttributeValue> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AlbumAttributeValue::getAlbumId,
                albumId);
        albumAttributeValueMapper.delete(queryWrapper);
        if (!CollectionUtils.isEmpty(albumAttributeValueVoList))
        {
            List<AlbumAttributeValue> albumAttributeValues =
                    albumAttributeValueVoList.stream().map(albumAttributeValueVo ->
                    {
                        AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
                        BeanUtils.copyProperties(albumAttributeValueVo,
                                albumAttributeValue);
                        albumAttributeValue.setAlbumId(albumId);
                        return albumAttributeValue;
                    }).collect(Collectors.toList());
            albumAttributeValueService.saveBatch(albumAttributeValues);
        }

    }

    @Override
    public List<AlbumInfo> findUserAllAlbumList(Long userId)
    {
        Page<AlbumInfo> pageParam = new Page(1,
                100);
        LambdaQueryWrapper<AlbumInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(AlbumInfo::getId,
                AlbumInfo::getAlbumTitle);
        queryWrapper.eq(AlbumInfo::getUserId,
                userId);
        queryWrapper.orderByDesc(AlbumInfo::getId);
        IPage<AlbumInfo> albumInfoPage = albumInfoMapper.selectPage(pageParam,
                queryWrapper);
        return albumInfoPage.getRecords();

    }

    @Override
    public Map<String, Object> getAlbumInfoStat(Long albumId)
    {
        return albumStatMapper.getAlbumInfoStat(albumId);
    }

    @Override
    public List<AlbumAttributeValue> findAlbumAttributeValueByAlbumId(Long albumId)
    {
        LambdaQueryWrapper<AlbumAttributeValue> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AlbumAttributeValue::getAlbumId,
                albumId);
        List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueMapper.selectList(lambdaQueryWrapper);
        //	返回集合数据
        return albumAttributeValueList;
    }

    @Override
    public AlbumStatVo getAlbumStatVoByAlbumId(Long albumId)
    {
        //	调用mapper 层方法
        return albumInfoMapper.selectAlbumStat(albumId);
    }


    private void saveAlbumStat(Long albumId,
                               String statType)
    {
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(albumId);
        albumStat.setStatType(statType);
        albumStat.setStatNum(0);
        albumStatMapper.insert(albumStat);
    }
}
