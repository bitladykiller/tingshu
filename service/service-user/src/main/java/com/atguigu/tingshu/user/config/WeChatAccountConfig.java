package com.atguigu.tingshu.user.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;


@Data
@ConfigurationProperties(prefix = "wechat.login")
@Component
@RefreshScope
public class WeChatAccountConfig
{
    private String appId;
    private String appSecret;
}
