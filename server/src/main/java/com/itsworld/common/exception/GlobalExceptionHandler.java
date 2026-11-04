package com.itsworld.common.exception;

import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 全局异常处理器
 * 捕获所有异常并返回统一的Result格式
 *
 * @author Claude
 * @date 2026-10-30
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数验证异常处理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .orElse("参数验证失败");
        log.warn("参数验证异常：{}", message);
        return Result.fail(ErrorCode.INVALID_PARAM.getCode(), message);
    }

    /**
     * 参数绑定异常处理
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBindException(BindException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .orElse("参数绑定失败");
        log.warn("参数绑定异常：{}", message);
        return Result.fail(ErrorCode.INVALID_PARAM.getCode(), message);
    }

    /**
     * 缺少请求参数异常处理
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        String message = "缺少请求参数：" + e.getParameterName();
        log.warn(message);
        return Result.fail(ErrorCode.INVALID_PARAM.getCode(), message);
    }

    /**
     * 参数类型不匹配异常处理
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "参数类型不正确：" + e.getName();
        log.warn(message);
        return Result.fail(ErrorCode.INVALID_PARAM.getCode(), message);
    }

    /**
     * 数据库约束冲突异常处理
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("数据库约束冲突：{}", e.getMessage());
        return Result.fail(ErrorCode.RESOURCE_CONFLICT.getCode(), "数据冲突，请检查唯一字段");
    }

    /**
     * HTTP方法不支持异常处理
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleHttpMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        String message = "不支持的HTTP方法：" + e.getMethod();
        log.warn(message);
        return Result.fail(ErrorCode.INVALID_REQUEST.getCode(), message);
    }

    /**
     * 请求体格式不正确异常处理
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误：{}", e.getMessage());
        return Result.fail(ErrorCode.INVALID_PARAM.getCode(), "请求体格式不正确");
    }

    /**
     * 404异常处理
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleNoHandlerFound(NoHandlerFoundException e) {
        String message = "请求的资源不存在：" + e.getRequestURL();
        log.warn(message);
        return Result.fail(ErrorCode.NOT_FOUND.getCode(), message);
    }

    /**
     * 其他所有异常处理
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ErrorCode.SERVER_ERROR.getCode(), "服务异常，请联系管理员");
    }

}
