package com.growthtrace.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    OK(0, "ok"),

    BAD_REQUEST(40000, "请求参数有误"),
    UNAUTHORIZED(40100, "未登录或登录已过期"),
    FORBIDDEN(40300, "无访问权限"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "资源冲突"),

    BUSINESS_ERROR(50000, "业务处理失败"),
    AI_UNAVAILABLE(50010, "AI 服务暂不可用，请稍后重试"),
    AI_PARSE_ERROR(50011, "AI 响应格式异常"),
    AI_TIMEOUT(50012, "AI 调用超时"),

    SERVER_ERROR(50090, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
