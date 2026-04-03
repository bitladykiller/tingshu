package com.atguigu.tingshu.user.client;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.client.impl.UserInfoDegradeFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 *
 */
@FeignClient(value = "service-user", fallback = UserInfoDegradeFeignClient.class)
public interface UserInfoFeignClient
{
    @GetMapping("api/user/userInfo/getUserInfoVo/{userId}")
    Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId);

    @PostMapping("api/user/userInfo/userIsPaidTrack/{albumId}")
    Result<Map<Long, Integer>> userIsPaidTrack(@PathVariable("albumId") Long albumId,
                                               @RequestBody List<Long> trackIdList);

    @GetMapping("api/user/userInfo/isPaidAlbum/{albumId}")
    Result<Boolean> isPaidAlbum(@PathVariable("albumId") Long albumId);

}