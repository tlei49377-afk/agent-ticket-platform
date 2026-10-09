package com.atp.server.handler;

import com.atp.common.constant.MessageConstant;
import com.atp.common.exception.BaseException;
import com.atp.common.exception.PermissionDeniedException;
import com.atp.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理。所有异常统一转成 Result，不让 Whitelabel 页漏出去。
 */
@RestControllerAdvice // 全局异常处理
@Slf4j // 日志
public class GlobalExceptionHandler {
    /**
     * 业务异常：直接把文案返回给前端
     */
    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBaseException(BaseException ex) {
        log.warn("业务异常：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 权限不足
     */
    @ExceptionHandler(PermissionDeniedException.class)
    public Result<Void> handlePermission(PermissionDeniedException ex) {
        log.warn("权限不足：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * @RequestBody 上的 @Valid 校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String msg = fieldError == null ? "参数校验失败" : fieldError.getDefaultMessage();
        return Result.error(msg);
    }

    /**
     * 唯一键冲突
     */
    @ExceptionHandler({SQLIntegrityConstraintViolationException.class, DuplicateKeyException.class})
    public Result<Void> handleDuplicate(Exception ex) {
        log.warn("唯一键冲突：{}", ex.getMessage());
        return Result.error("数据已存在或违反唯一约束");
    }

    /**
     * 接口不存在
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(NoResourceFoundException ex) {
        log.warn("接口不存在：{}", ex.getMessage());
        return Result.error("接口不存在");
    }

    /**
     * 剩下的所有异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        // 堆栈只进日志，不返回给前端
        log.error("系统异常", ex);
        return Result.error(MessageConstant.SYSTEM_ERROR);
    }

}
