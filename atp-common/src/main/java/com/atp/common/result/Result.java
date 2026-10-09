package com.atp.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结构。前端只会拿到这一种形状。
 */
@Data
public class Result<T> implements Serializable {
    private Integer code; // 状态码
    private String msg; // 提示信息
    private T data; // 数据

    // 成功，不带数据
    public static <T> Result<T> success() {
        Result<T> r = new Result<>();
        r.code = 1;
        r.msg = "success";
        return r;
    }

    // 成功，带数据
    public static <T> Result<T> success(T data) {
        Result<T> r = success();
        r.data = data;
        return r;
    }

    // 失败，传消息
    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.msg = msg;
        return r;
    }
}
