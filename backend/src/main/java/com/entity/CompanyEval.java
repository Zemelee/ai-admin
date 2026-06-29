package com.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 企业评价 / 实习鉴定
 * status: DRAFT/SUBMITTED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("company_eval")
public class CompanyEval extends BaseEntity {

    private Long studentId;
    /** 评价企业指导 mentor.id 快照 */
    private Long mentorId;
    /** 评价人 user_id */
    private Long mentorUserId;
    /** 出勤与纪律 1-5 */
    private Integer scoreAttendance;
    /** 专业能力 1-5 */
    private Integer scoreAbility;
    /** 工作态度 1-5 */
    private Integer scoreAttitude;
    /** 综合评价 1-5 */
    private Integer scoreOverall;
    /** 鉴定评语 */
    private String comment;
    /** DRAFT/SUBMITTED */
    private String status;
    private LocalDateTime submitTime;
}
