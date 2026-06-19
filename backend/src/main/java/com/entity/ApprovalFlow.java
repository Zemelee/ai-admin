package com.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 审批流水（leave_apply / transfer_apply 共用）
 * bizType: LEAVE / TRANSFER
 * node:    MENTOR / TEACHER / SUPERVISOR
 * result:  APPROVED / REJECTED ；NULL=待审批
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_flow")
public class ApprovalFlow extends BaseEntity {
    private String bizType;
    private Long bizId;
    private String node;
    private Integer nodeSeq;
    private Long approverId;
    private String approverName;
    private String result;
    private String comment;
    private LocalDateTime actTime;
}
