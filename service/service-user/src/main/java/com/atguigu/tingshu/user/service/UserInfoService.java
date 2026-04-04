package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

public interface UserInfoService extends IService<UserInfo>
{


    Map<Long, Integer> userIsPaidTrack(Long userId,
                                       Long albumId,
                                       List<Long> trackIdList);

    Boolean isPaidAlbum(Long userId,
                        Long albumId);

    List<Long> findUserPaidTrackList(Long userId,
                                     Long albumId);

    void savePaidRecord(UserPaidRecordVo userPaidRecordVo);

    void userPayRecord(UserPaidRecordVo userPaidRecordVo);
}
