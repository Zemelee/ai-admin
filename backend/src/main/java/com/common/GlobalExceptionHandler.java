package com.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * 全局异常处理：业务异常 → R.fail；登录态异常 → 401；其它异常 → 500 + 日志
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBiz(BizException e) {
        // 业务异常使用 200 响应码 + 业务 code，前端按 code 判断
        return ResponseEntity.ok(R.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<R<Void>> handleNotLogin(NotLoginException e) {
        log.debug("未登录: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(R.fail(401, "未登录或登录已过期"));
    }

    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<R<Void>> handleNotRole(NotRoleException e) {
        log.debug("无权限: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(R.fail(403, "无权访问该资源"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.ok(R.fail(422, msg));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.ok(R.fail(422, msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraint(ConstraintViolationException e) {
        return ResponseEntity.ok(R.fail(422, e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<R<Void>> handleUploadSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.ok(R.fail(413, "上传文件过大"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleAll(Exception e) {
        log.error("未捕获异常", e);
        return ResponseEntity.ok(R.fail(500, "服务器内部错误，请联系管理员"));
    }
}
