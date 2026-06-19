package com.common;

import lombok.Getter;

/**
 * 业务异常 — 由 GlobalExceptionHandler 统一捕获并转 R.fail
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String msg) {
        this(500, msg);
    }

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    /** 校验失败 422 */
    public static BizException validate(String msg) {
        return new BizException(422, msg);
    }

    /** 资源不存在 404 */
    public static BizException notFound(String msg) {
        return new BizException(404, msg);
    }

    /** 禁止访问 403 */
    public static BizException forbidden(String msg) {
        return new BizException(403, msg);
    }
}
