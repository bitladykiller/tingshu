package com.atguigu.tingshu.common.cache;

import com.atguigu.tingshu.common.constant.RedisConstant;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class GuiGuCacheAspect
{

    private static final String NULL_VALUE = "NULL_VALUE";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(com.atguigu.tingshu.common.cache.GuiGuCache)")
    public Object cacheAspect(ProceedingJoinPoint point) throws Throwable
    {

        Object[] args = point.getArgs();

        MethodSignature signature = (MethodSignature) point.getSignature();
        GuiGuCache guiGuCache = signature.getMethod().getAnnotation(GuiGuCache.class);

        String prefix = guiGuCache.prefix();
        String key = buildCacheKey(prefix,
                args);

        Object cacheObj = redisTemplate.opsForValue().get(key);

        if (NULL_VALUE.equals(cacheObj))
            return null;

        if (cacheObj != null)
            return cacheObj;


        String lockKey = key + ":lock";
        RLock lock = redissonClient.getLock(lockKey);

        boolean locked = false;
        try
        {
            locked = lock.tryLock(
                    RedisConstant.CACHE_LOCK_EXPIRE_PX1,
                    RedisConstant.CACHE_LOCK_EXPIRE_PX2,
                    TimeUnit.SECONDS
            );

            if (locked)
            {

                cacheObj = redisTemplate.opsForValue().get(key);

                if (NULL_VALUE.equals(cacheObj))
                {
                    return null;
                }

                if (cacheObj != null)
                {
                    return cacheObj;
                }

                Object result = point.proceed(args);

                if (result == null)
                {
                    redisTemplate.opsForValue().set(
                            key,
                            NULL_VALUE,
                            RedisConstant.CACHE_TEMPORARY_TIMEOUT,
                            TimeUnit.SECONDS
                    );
                    return null;
                }

                redisTemplate.opsForValue().set(
                        key,
                        result,
                        RedisConstant.CACHE_TIMEOUT,
                        TimeUnit.SECONDS
                );

                return result;
            }

            for (int i = 0; i < 5; i++)
            {
                Thread.sleep(50);

                cacheObj = redisTemplate.opsForValue().get(key);

                if (NULL_VALUE.equals(cacheObj))
                {
                    return null;
                }

                if (cacheObj != null)
                {
                    return cacheObj;
                }
            }

            return point.proceed(args);

        } catch (Exception e)
        {
            return point.proceed(args);
        } finally
        {
            if (locked && lock.isHeldByCurrentThread())
            {
                lock.unlock();
            }
        }
    }

    private String buildCacheKey(String prefix,
                                 Object[] args)
    {
        return prefix + Arrays.deepToString(args);
    }
}