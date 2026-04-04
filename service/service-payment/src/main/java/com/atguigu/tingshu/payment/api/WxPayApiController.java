package com.atguigu.tingshu.payment.api;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.wechat.pay.java.service.payments.model.Transaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "微信支付接口")
@RestController
@RequestMapping("api/payment/wxPay")
@Slf4j
public class WxPayApiController
{

    @Autowired
    private WxPayService wxPayService;
    @Autowired
    private PaymentInfoService paymentInfoService;

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

    @Operation(summary = "支付状态查询")
    @GetMapping("/queryPayStatus/{orderNo}")
    public Result queryPayStatus(@PathVariable String orderNo)
    {
        try
        {
            Transaction transaction = wxPayService.queryPayStatus(orderNo);
            System.out.println("queryPayStatus: " + JSON.toJSONString(transaction));
            if (null != transaction && transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS)
            {
                paymentInfoService.updatePaymentStatus(transaction);
                return Result.ok(true);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return Result.ok(false);
    }

    @Operation(summary = "微信支付异步通知接口")
    @PostMapping("/notify")
    public Map<String, Object> notify(HttpServletRequest request)
    {
        Map<String, Object> result = new HashMap<>();
        try
        {
            wxPayService.wxnotify(request);
            result.put("code", "SUCCESS");
            result.put("message", "成功");
            return result;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        result.put("code", "FAIL");
        result.put("message", "失败");
        return result;
    }

    @GuiGuLogin
    @Operation(summary = "微信Native下单")
    @Parameters({
            @Parameter(name = "paymentType", description = "支付类型：1301-订单 1302-充值", in = ParameterIn.PATH, required = true),
            @Parameter(name = "orderNo", description = "订单号", required = true, in = ParameterIn.PATH),
    })
    @PostMapping("/createNative/{paymentType}/{orderNo}")
    public Result<Map<String, Object>> createNative(@PathVariable String paymentType, @PathVariable String orderNo)
    {
        Map<String, Object> map = wxPayService.createNative(paymentType, orderNo, AuthContextHolder.getUserId());
        return Result.ok(map);
    }

    @Operation(summary = "关闭交易记录")
    @GetMapping("/closePayment/{orderNo}")
    public Result closePayment(@PathVariable String orderNo)
    {
        boolean flag = this.paymentInfoService.closePayment(orderNo);
        return Result.ok(flag);
    }

    @Operation(summary = "退款")
    @GetMapping("/refund/{orderNo}")
    public Result refund(@PathVariable String orderNo)
    {
        boolean flag = this.wxPayService.refund(orderNo);
        return Result.ok(flag);
    }

}
