package com.atguigu.tingshu.user.strategy.impl;


import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
import com.atguigu.tingshu.user.strategy.ItemTypeStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Component("1002")  //beanID:trackStrategy
public class TrackStrategy implements ItemTypeStrategy
{

    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;

    @Autowired
    private TrackInfoFeignClient trackInfoFeignClient;



    @Override
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo)
    {
        log.info("处理购买项目类型为：声音");
        LambdaQueryWrapper<UserPaidTrack> userPaidTrackLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userPaidTrackLambdaQueryWrapper.eq(UserPaidTrack::getOrderNo, userPaidRecordVo.getOrderNo());
        Long count = userPaidTrackMapper.selectCount(userPaidTrackLambdaQueryWrapper);
        if (count > 0)
        {
            return;
        }
        TrackInfo trackInfo = trackInfoFeignClient.getTrackInfo(userPaidRecordVo.getItemIdList()
                                                                                .get(0))
                                                  .getData();
        Long albumId = trackInfo.getAlbumId();
        userPaidRecordVo.getItemIdList()
                        .forEach(trackId ->
                                 {
                                     UserPaidTrack userPaidTrack = new UserPaidTrack();
                                     userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
                                     userPaidTrack.setUserId(userPaidRecordVo.getUserId());
                                     userPaidTrack.setAlbumId(albumId);
                                     userPaidTrack.setTrackId(trackId);
                                     userPaidTrackMapper.insert(userPaidTrack);
                                 });
    }
}