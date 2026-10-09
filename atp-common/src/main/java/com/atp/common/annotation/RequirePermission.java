package com.atp.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口所需的权限码，如 ticket:list
 */
@Target(ElementType.METHOD) // 可用于方法
@Retention(RetentionPolicy.RUNTIME) // 运行时可获取
public @interface RequirePermission {
    String value();
}
