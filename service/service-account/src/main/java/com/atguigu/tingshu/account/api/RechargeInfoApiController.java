package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@Tag(name = "充值管理")
@RestController
@RequestMapping("api/account/rechargeInfo")
@SuppressWarnings({"all"})
public class RechargeInfoApiController
{

    @Autowired
    private RechargeInfoService rechargeInfoService;

    @GuiGuLogin
    @Operation(summary = "根据订单号获取充值信息")
    @GetMapping("/getRechargeInfo/{orderNo}")
    public Result<RechargeInfo> getRechargeInfo(@PathVariable("orderNo") String orderNo)
    {
        RechargeInfo rechargeInfo = rechargeInfoService.getRechargeInfoByOrderNo(orderNo);
        return Result.ok(rechargeInfo);
    }

    @GuiGuLogin
    @Operation(summary = "充值")
    @PostMapping("submitRecharge")
    public Result submitRecharge(@RequestBody RechargeInfoVo rechargeInfoVo)
    {
        Long userId = AuthContextHolder.getUserId();
        String orderNo = this.rechargeInfoService.submitRecharge(rechargeInfoVo, userId);
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderNo", orderNo);
        return Result.ok(map);
    }

}

