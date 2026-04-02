package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "用户声音播放进度管理接口")
@RestController
@RequestMapping("api/user/userListenProcess")
@SuppressWarnings({"all"})
public class UserListenProcessApiController
{

    @Autowired
    private UserListenProcessService userListenProcessService;

    @GuiGuLogin
    @Operation(summary = "获取声音的上次跳出时间")
    @GetMapping("/getTrackBreakSecond/{trackId}")
    public Result<BigDecimal> getTrackBreakSecond(@PathVariable Long trackId)
    {
        Long userId = AuthContextHolder.getUserId();
        BigDecimal trackBreakSecond = userListenProcessService.getTrackBreakSecond(userId,
                trackId);
        return Result.ok(trackBreakSecond);
    }

    @GuiGuLogin
    @Operation(summary = "更新播放进度")
    @PostMapping("/updateListenProcess")
    public Result updateListenProcess(@RequestBody UserListenProcessVo userListenProcessVo)
    {
        Long userId = AuthContextHolder.getUserId();
        userListenProcessService.updateListenProcess(userId,
                userListenProcessVo);
        return Result.ok();
    }

    @GuiGuLogin
    @Operation(summary = "获取最近一次播放声音")
    @GetMapping("/getLatelyTrack")
    public Result<Map<String, Object>> getLatelyTrack()
    {
        Long userId = AuthContextHolder.getUserId();
        Map<String, Object> map = userListenProcessService.getLatelyTrack(userId);
        return Result.ok(map);
    }

}

