package com.atguigu.tingshu.payment.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.payment.service.WxPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "微信支付接口")
@RestController
@RequestMapping("api/payment/wxPay")
@Slf4j
public class WxPayApiController
{

    @Autowired
    private WxPayService wxPayService;

    @GuiGuLogin
    @Operation(summary = "微信下单")
    @Parameters({
            @Parameter(name = "paymentType", description = "支付类型：1301-订单 1302-充值", in = ParameterIn.PATH, required = true),
            @Parameter(name = "orderNo", description = "订单号", required = true, in = ParameterIn.PATH),
    })
    @PostMapping("/createJsapi/{paymentType}/{orderNo}")
    public Result createJsapi(@PathVariable String paymentType, @PathVariable String orderNo)
    {
        Map map = wxPayService.createJsapi(paymentType, orderNo, AuthContextHolder.getUserId());
        return Result.ok(map);
    }

}
