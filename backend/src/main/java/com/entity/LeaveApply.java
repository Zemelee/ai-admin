package com.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假申请单
 * status: PENDING / APPROVED / REJECTED / CANCELLED
 * currentNode: TEACHER / SUPERVISOR （MVP 简化：暂不走 MENTOR 节点）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("leave_apply")
public class LeaveApply extends BaseEntity {
    private Long studentId;
    private String leaveType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal durationDays;
    private String reason;
    private Integer parentConfirm;
    private String status;
    /** 终审通过/驳回时需要写 null，因此使用 IGNORED 策略 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String currentNode;
    private LocalDateTime submitTime;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDateTime finishTime;
}
