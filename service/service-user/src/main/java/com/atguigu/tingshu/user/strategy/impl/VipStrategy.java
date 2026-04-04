package com.atguigu.tingshu.user.strategy.impl;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserVipService;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserVipServiceMapper;
import com.atguigu.tingshu.user.mapper.VipServiceConfigMapper;
import com.atguigu.tingshu.user.strategy.ItemTypeStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component("1003")
public class VipStrategy implements ItemTypeStrategy
{


    @Autowired
    private UserInfoMapper userInfoMapper;


    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;


    @Autowired
    private UserVipServiceMapper userVipServiceMapper;


    @Override
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo)
    {
        log.info("处理购买项目类型为：VIP会员");
        UserVipService userVipService = new UserVipService();
        Long vipConfigId = userPaidRecordVo.getItemIdList()
                                           .get(0);
        VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(vipConfigId);
        UserInfo userInfo = userInfoMapper.selectById(userPaidRecordVo.getUserId());
        Date currentTime = new Date();
        if (userInfo.getIsVip()
                    .intValue() == 1 && userInfo.getVipExpireTime()
                                                .after(new Date()))
        {
            currentTime = userInfo.getVipExpireTime();
        }
        currentTime = new LocalDateTime(currentTime).plusMonths(vipServiceConfig.getServiceMonth())
                                                    .toDate();
        userVipService.setExpireTime(currentTime);
        userVipService.setStartTime(new Date());
        userVipService.setUserId(userPaidRecordVo.getUserId());
        userVipService.setOrderNo(userPaidRecordVo.getOrderNo());
        userVipServiceMapper.insert(userVipService);
        userInfo.setIsVip(1);
        userInfo.setVipExpireTime(userVipService.getExpireTime());
        userInfoMapper.updateById(userInfo);
    }
}