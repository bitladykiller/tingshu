package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.vo.user.UserListenProcessVo;

import java.math.BigDecimal;

public interface UserListenProcessService {

    BigDecimal getTrackBreakSecond(Long userId,
                                   Long trackId);

    void updateListenProcess(Long userId,
                             UserListenProcessVo userListenProcessVo);
}
