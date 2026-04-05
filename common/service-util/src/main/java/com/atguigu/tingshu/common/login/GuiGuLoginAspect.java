package com.atguigu.tingshu.common.login;


import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class GuiGuLoginAspect
{
    @Autowired
    private RedisTemplate redisTemplate;

    @Around("execution(* com.atguigu.tingshu.*.api.*.*(..)) && @annotation(guiGuLogin)")
    public Object login(ProceedingJoinPoint joinPoint,
                        GuiGuLogin guiGuLogin) throws Throwable
    {
        try {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            ServletRequestAttributes attributes = (ServletRequestAttributes) requestAttributes;
            HttpServletRequest request = null;
            if (attributes != null)
                request = attributes.getRequest();
            String token = null;
            if (request != null)
                token = request.getHeader("token");
            if (guiGuLogin.required()) {
                if (!StringUtils.hasText(token)){
                    throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
                }
                String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX+token;
                UserInfo userInfo = (UserInfo) this.redisTemplate.opsForValue().get(loginKey);
                if (null == userInfo){
                    throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
                }
            }
            if (StringUtils.hasText(token)){
                String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX+token;
                UserInfo userInfo = (UserInfo) this.redisTemplate.opsForValue().get(loginKey);
                if (null != userInfo){
                    AuthContextHolder.setUserId(userInfo.getId());
                }
            }
            return joinPoint.proceed();
        } finally {
            AuthContextHolder.removeUserId();
        }
    }
}
