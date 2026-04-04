package com.atguigu.tingshu.account.mapper;

import com.atguigu.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    int checkAndDeduct(Long userId, BigDecimal amount);

    void add(Long userId, BigDecimal amount);
}
