package com.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 单位变更申请单
 * status: PENDING / APPROVED / REJECTED / CANCELLED
 * currentNode: MENTOR / TEACHER / SUPERVISOR
 * <p>
 * 审批顺序（MVP 串行）：MENTOR(原企业) → TEACHER(分管教师) → SUPERVISOR(系部) → APPROVED
 * 终态 APPROVED 时回写 student.companyId/mentorId（若 toCompanyId 有值）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("transfer_apply")
public class TransferApply extends BaseEntity {
    private Long studentId;
    private Long fromCompanyId;
    /** 系统内已登记企业 id；外部新企业可空 */
    private Long toCompanyId;
    /** 新单位名称（外部企业冗余存） */
    private String toCompanyName;
    private String reason;
    private LocalDate expectedStart;
    private String status;
    /** 终审通过/驳回/撤回时需要写 null，因此使用 IGNORED 策略 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String currentNode;
    private LocalDateTime submitTime;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDateTime finishTime;
}
