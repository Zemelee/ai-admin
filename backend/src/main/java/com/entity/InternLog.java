package com.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 实习日志（日报）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("intern_log")
public class InternLog extends BaseEntity {

    private Long studentId;

    /** 日志日期 */
    private LocalDate logDate;

    /** 日志内容 */
    private String content;

    /** AI 是否命中敏感词 */
    private Integer sensitiveHit;

    /** 命中词，逗号分隔 */
    private String sensitiveWords;

    /** AI 高亮后 HTML 片段 */
    private String sensitiveMarkedHtml;

    /** 企业指导是否已查阅（CONFIRMED 时置 1） */
    private Integer mentorReview;

    /** 指导教师是否已查阅 */
    private Integer teacherReview;

    /** SUBMITTED/CONFIRMED/REJECTED */
    private String status;

    /** 学生提交时间 */
    private LocalDateTime submitTime;

    /** 企业指导确认时间 */
    private LocalDateTime mentorReviewTime;

    /** 确认人 user_id */
    private Long mentorReviewUserId;

    /** 企业指导确认意见（驳回必填） */
    private String mentorComment;
}
