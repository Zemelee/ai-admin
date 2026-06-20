package com.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI（GLM）调用审计日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_call_log")
public class AiCallLog extends BaseEntity {

    /** 调用场景：CHAT_QA / SENSITIVE_DETECT 等 */
    private String scene;

    /** 调用人 user_id（系统调用为空） */
    private Long userId;

    /** 模型名 */
    private String model;

    /** 请求体 JSON */
    private String requestPayload;

    /** 响应体 JSON */
    private String responsePayload;

    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;

    /** 耗时（毫秒） */
    private Integer costMs;

    /** 是否成功 */
    private Integer success;

    /** 失败信息 */
    private String errorMsg;

    /** 关联业务类型：LOG/WEEKLY 等 */
    private String bizType;

    /** 关联业务单 ID */
    private Long bizId;
}
