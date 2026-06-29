package com.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 三色预警事件
 * level: RED/YELLOW/GREEN
 * status: PENDING/REVIEWED/IGNORED
 * rule_code: NO_LOG_3D/SENSITIVE_WORD/COMPANY_BLACKLIST/TRANSFER_PENDING_3D/LEAVE_OVER_3M
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warning")
public class Warning extends BaseEntity {

    private String level;

    private String ruleCode;

    private String ruleDesc;

    private Long studentId;

    /** 学生姓名快照 */
    private String studentName;

    /** LOG/WEEKLY/LEAVE/TRANSFER/COMPANY */
    private String bizType;

    private Long bizId;

    /** 命中详情（敏感词列表 / 企业名 / 缺交天数 等） */
    private String detail;

    private String status;

    private Long reviewerId;

    private LocalDateTime reviewTime;

    private String reviewNote;
}
