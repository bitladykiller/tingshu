package com.atguigu.tingshu.user.strategy;

import com.atguigu.tingshu.common.execption.GuiguException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;


@Slf4j
@Component
public class StrategyFactory
{


    @Autowired
    private Map<String, ItemTypeStrategy> strategyMap;

    public ItemTypeStrategy getStrategy(String itemType)
    {
        if (strategyMap.containsKey(itemType))
        {
            return strategyMap.get(itemType);
        }
        log.error("该策略实现类不存在");
        throw new GuiguException(500, "该策略" + itemType + "实现类不存在");
    }
}