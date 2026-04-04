package com.atguigu.tingshu.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.exception.GuiguException;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.model.user.UserInfoVo;
import com.atguigu.tingshu.order.client.OrderInfoFeignClient;
import com.atguigu.tingshu.payment.config.WxPayV3Config;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.service.partnerpayments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.Amount;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.Payer;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.PrepayWithRequestPaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import com.atguigu.tingshu.common.execption.GuiguException;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService
{

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private RSAAutoCertificateConfig rsaAutoCertificateConfig;

    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Autowired
    private WxPayV3Config wxPayV3Config;

    @Override
    public Map<String, Object> createJsapi(String paymentType, String orderNo, Long userId)
    {
        try
        {
            // 1. 如果不是充值订单，则先查询订单状态，若订单已取消则直接返回 null
            if (!paymentType.equals(SystemConstant.PAYMENT_TYPE_RECHARGE))
            {
                Result<OrderInfo> orderInfoResult = this.orderInfoFeignClient.getOrderInfo(orderNo);
                OrderInfo orderInfo = orderInfoResult.getData();

                if (SystemConstant.ORDER_STATUS_CANCEL.equals(orderInfo.getOrderStatus()))
                {
                    return null;
                }
            }

            // 2. 保存支付信息
            PaymentInfo paymentInfo = paymentInfoService.savePaymentInfo(paymentType, orderNo);

            // 3. 创建微信 JSAPI 下单服务对象
            JsapiServiceExtension service = new JsapiServiceExtension.Builder()
                    .config(rsaAutoCertificateConfig)
                    .build();

            // 4. 封装预支付请求参数
            PrepayRequest request = new PrepayRequest();

            // 注意：金额单位是“分”
            Amount amount = new Amount();
            amount.setTotal(1); // 这里是测试金额，实际开发应使用真实订单金额
            request.setAmount(amount);

            request.setAppid(wxPayV3Config.getAppid());
            request.setMchid(wxPayV3Config.getMerchantId());
            request.setDescription(paymentInfo.getContent());
            request.setNotifyUrl(wxPayV3Config.getNotifyUrl());
            request.setOutTradeNo(paymentInfo.getOrderNo());

            // 5. 查询用户信息，获取微信 openid
            Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(paymentInfo.getUserId());
            Assert.notNull(userInfoVoResult, "返回用户结果集对象不能为空");

            UserInfoVo userInfoVo = userInfoVoResult.getData();
            Assert.notNull(userInfoVo, "用户对象不能为空");

            String openid = userInfoVo.getWxOpenId();

            Payer payer = new Payer();
            payer.setOpenid(openid);
            request.setPayer(payer);

            // 6. 调用微信支付统一下单接口
            PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);
            log.info("微信支付下单返回参数：{}", JSON.toJSONString(response));

            // 7. 封装前端调起支付所需参数
            Map<String, Object> result = new HashMap<>();
            result.put("timeStamp", response.getTimeStamp());
            result.put("nonceStr", response.getNonceStr());
            result.put("package", response.getPackageVal());
            result.put("signType", response.getSignType());
            result.put("paySign", response.getPaySign());

            return result;
        }
        catch (ServiceException e)
        {
            e.printStackTrace();
            throw new GuiguException(201, e.getErrorMessage());
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
            throw new GuiguException(201, "订单号不存在");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new GuiguException(201, "微信下单异常");
        }
    }
}