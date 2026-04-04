package com.atguigu.tingshu.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.order.client.OrderInfoFeignClient;
import com.atguigu.tingshu.payment.config.WxPayV3Config;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.atguigu.tingshu.payment.util.PayUtil;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.Status;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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
    @Autowired
    private Config config;


    @Override
    public Map<String, Object> createJsapi(String paymentType, String orderNo, Long userId)
    {
        try
        {
            if (!paymentType.equals(SystemConstant.PAYMENT_TYPE_RECHARGE))
            {
                Result<OrderInfo> orderInfoResult = this.orderInfoFeignClient.getOrderInfo(orderNo);
                OrderInfo orderInfo = orderInfoResult.getData();

                if (SystemConstant.ORDER_STATUS_CANCEL.equals(orderInfo.getOrderStatus()))
                {
                    return null;
                }
            }

            PaymentInfo paymentInfo = paymentInfoService.savePaymentInfo(paymentType, orderNo);

            JsapiServiceExtension service = new JsapiServiceExtension.Builder()
                    .config(rsaAutoCertificateConfig)
                    .build();

            PrepayRequest request = new PrepayRequest();

            Amount amount = new Amount();
            amount.setTotal(1);
            request.setAmount(amount);

            request.setAppid(wxPayV3Config.getAppid());
            request.setMchid(wxPayV3Config.getMerchantId());
            request.setDescription(paymentInfo.getContent());
            request.setNotifyUrl(wxPayV3Config.getNotifyUrl());
            request.setOutTradeNo(paymentInfo.getOrderNo());
            Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(paymentInfo.getUserId());
            Assert.notNull(userInfoVoResult, "返回用户结果集对象不能为空");

            UserInfoVo userInfoVo = userInfoVoResult.getData();
            Assert.notNull(userInfoVo, "用户对象不能为空");

            String openid = userInfoVo.getWxOpenId();

            Payer payer = new Payer();
            payer.setOpenid(openid);
            request.setPayer(payer);

            PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);
            log.info("微信支付下单返回参数：{}", JSON.toJSONString(response));

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

    @Override
    public Transaction queryPayStatus(String orderNo)
    {
        try
        {
            //	构建service
            JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig)
                                                                               .build();
            QueryOrderByOutTradeNoRequest queryRequest = new QueryOrderByOutTradeNoRequest();
            queryRequest.setMchid(wxPayV3Config.getMerchantId());
            queryRequest.setOutTradeNo(orderNo);

            Transaction result = service.queryOrderByOutTradeNo(queryRequest);
            log.info("Transaction:\t" + JSON.toJSONString(result));
            return result;
        }
        catch (ServiceException e)
        {
            // API返回失败, 例如ORDER_NOT_EXISTS
            System.out.printf("code=[%s], message=[%s]\n", e.getErrorCode(), e.getErrorMessage());
            System.out.printf("reponse body=[%s]\n", e.getResponseBody());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void wxnotify(HttpServletRequest request)
    {
        String wechatPaySerial = request.getHeader("Wechatpay-Serial");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String signature = request.getHeader("Wechatpay-Signature");
        String requestBody = PayUtil.readData(request);

        //2.构造 RequestParam
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(wechatPaySerial)
                .nonce(nonce)
                .signature(signature)
                .timestamp(timestamp)
                .body(requestBody)
                .build();

        //3.初始化 NotificationParser
        NotificationParser parser = new NotificationParser(rsaAutoCertificateConfig);
        //4.以支付通知回调为例，验签、解密并转换成 Transaction
        Transaction transaction = parser.parse(requestParam, Transaction.class);
        log.info("成功解析：{}", JSON.toJSONString(transaction));
        if (null != transaction && transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS)
        {
            // 5.处理支付业务
            paymentInfoService.updatePaymentStatus(transaction);
        }
    }

    @Override
    public Map<String, Object> createNative(String paymentType, String orderNo, Long userId)
    {
        try
        {
            //保存支付记录
            PaymentInfo paymentInfo = paymentInfoService.savePaymentInfo(paymentType, orderNo);

            // 构建service
            NativePayService service = new NativePayService.Builder().config(rsaAutoCertificateConfig)
                                                                     .build();
            // request.setXxx(val)设置所需参数，具体参数可见Request定义
            com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest request = new com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest();
            com.wechat.pay.java.service.payments.nativepay.model.Amount amount = new com.wechat.pay.java.service.payments.nativepay.model.Amount();
            amount.setTotal(1);
            request.setAmount(amount);
            request.setAppid(wxPayV3Config.getAppid());
            request.setMchid(wxPayV3Config.getMerchantId());
            request.setDescription(paymentInfo.getContent());
            request.setNotifyUrl(wxPayV3Config.getNotifyUrl());
            request.setOutTradeNo(paymentInfo.getOrderNo());

            // 调用下单方法，得到应答
            com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse response = service.prepay(request);
            // 使用微信扫描 code_url 对应的二维码，即可体验Native支付
            System.out.println(response.getCodeUrl());

            Map result = new HashMap<>();
            result.put("codeUrl", response.getCodeUrl());
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

    @Override
    public boolean refund(String orderNo)
    {
        RefundService refundService = new RefundService.Builder().config(config)
                                                                 .build();
        //  创建退款请求对象
        CreateRequest createRequest = new CreateRequest();
        createRequest.setOutTradeNo(orderNo);
        createRequest.setOutRefundNo(orderNo);
        AmountReq amountReq = new AmountReq();
        amountReq.setRefund(1l);
        amountReq.setTotal(1l);
        amountReq.setCurrency("CNY");
        createRequest.setAmount(amountReq);
        Refund refund = refundService.create(createRequest);
        if (refund.getStatus()
                  .equals(Status.SUCCESS))
        {
            this.paymentInfoService.closePayment(orderNo);
            return true;
        }
        return false;
    }


}