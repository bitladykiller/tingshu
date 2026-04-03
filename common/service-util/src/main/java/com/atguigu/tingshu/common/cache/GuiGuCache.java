package com.atguigu.tingshu.common.cache;


import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface GuiGuCache
{
    String prefix() default "";
}
