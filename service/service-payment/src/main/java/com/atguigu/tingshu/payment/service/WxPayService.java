package com.atguigu.tingshu.payment.service;

import java.util.Map;

public interface WxPayService {

    Map createJsapi(String paymentType, String orderNo, Long userId);
}
