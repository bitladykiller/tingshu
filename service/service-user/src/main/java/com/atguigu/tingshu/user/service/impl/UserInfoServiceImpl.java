package com.atguigu.tingshu.user.service.impl;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.user.service.UserPaidTrackService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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


    @Override
    public Map<Long, Integer> userIsPaidTrack(Long userId,
                                              Long albumId,
                                              List<Long> trackIdList)
    {
        LambdaQueryWrapper<UserPaidAlbum> userPaidAlbumLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userPaidAlbumLambdaQueryWrapper.eq(UserPaidAlbum::getUserId,
                userId).eq(UserPaidAlbum::getAlbumId,
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
        } else
        {
            LambdaQueryWrapper<UserPaidTrack> userPaidTrackLambdaQueryWrapper = new LambdaQueryWrapper<>();
            userPaidTrackLambdaQueryWrapper.eq(UserPaidTrack::getUserId,
                    userId).in(UserPaidTrack::getTrackId,
                    trackIdList);
            List<UserPaidTrack> userPaidTrackList = userPaidTrackService.list(userPaidTrackLambdaQueryWrapper);
            List<Long> userPaidTrackIdList = userPaidTrackList.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
            HashMap<Long, Integer> map = new HashMap<>();
            trackIdList.forEach(trackId ->
            {
                if (userPaidTrackIdList.contains(trackId))
                {
                    map.put(trackId,
                            1);
                } else
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
        Long count = userPaidAlbumMapper.selectCount(new LambdaQueryWrapper<UserPaidAlbum>().eq(UserPaidAlbum::getUserId,
                userId).eq(UserPaidAlbum::getAlbumId,
                albumId));
        if (count > 0)
        {
            return true;
        }
        return false;
    }
}
