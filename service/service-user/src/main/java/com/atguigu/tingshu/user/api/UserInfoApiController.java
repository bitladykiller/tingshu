package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user/userInfo")
@SuppressWarnings({"all"})
public class UserInfoApiController
{

    @Autowired
    private UserInfoService userInfoService;

    @Operation(summary = "根据用户id获取用户信息")
    @GetMapping("getUserInfoVo/{userId}")
    public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId)
    {
        // 获取用户信息
        UserInfo userInfo = userInfoService.getById(userId);
        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtils.copyProperties(userInfo,
                                 userInfoVo);
        return Result.ok(userInfoVo);
    }

    @GuiGuLogin(required = false)
    @Operation(summary = "判断用户是否购买声音列表")
    @PostMapping("userIsPaidTrack/{albumId}")
    public Result<Map<Long, Integer>> userIsPaidTrack(@PathVariable Long albumId,
                                                      @RequestBody List<Long> trackIdList)
    {
        Long userId = AuthContextHolder.getUserId();
        Map<Long, Integer> map = userInfoService.userIsPaidTrack(userId,
                                                                 albumId,
                                                                 trackIdList);
        return Result.ok(map);
    }

    @GuiGuLogin
    @Operation(summary = "判断用户是否购买过专辑")
    @GetMapping("isPaidAlbum/{albumId}")
    public Result<Boolean> isPaidAlbum(@PathVariable Long albumId)
    {
        Long userId = AuthContextHolder.getUserId();
        Boolean flag = userInfoService.isPaidAlbum(userId,
                                                   albumId);
        return Result.ok(flag);
    }

    @GuiGuLogin
    @Operation(summary = "根据专辑id获取用户支付过的声音id列表")
    @GetMapping("findUserPaidTrackList/{albumId}")
    public Result findUserPaidTrackList(@PathVariable Long albumId)
    {
        Long userId = AuthContextHolder.getUserId();
        List<Long> trackIdList = this.userInfoService.findUserPaidTrackList(userId,
                                                                            albumId);
        return Result.ok(trackIdList);
    }

    @Operation(summary = "处理用户购买记录")
    @PostMapping("/savePaidRecord")
    public Result savePaidRecord(@RequestBody UserPaidRecordVo userPaidRecordVo)
    {
        userInfoService.savePaidRecord(userPaidRecordVo);
        return Result.ok();
    }


}

