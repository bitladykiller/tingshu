package com.atguigu.tingshu.payment.service;

import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface WxPayService {

    Map createJsapi(String paymentType, String orderNo, Long userId);

    Transaction queryPayStatus(String orderNo);

    void wxnotify(HttpServletRequest request);

    Map<String, Object> createNative(String paymentType, String orderNo, Long userId);

    boolean refund(String orderNo);
}
