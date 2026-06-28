package com.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 实习报告
 * 类型：MID_TERM（中期报告）、FINAL（终期报告）
 * 状态：SUBMITTED/REVIEWED/REJECTED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("intern_report")
public class InternReport extends BaseEntity {

    private Long studentId;

    /** 报告类型：MID_TERM/FINAL */
    private String reportType;

    /** 报告标题 */
    private String title;

    /** 报告内容（Markdown） */
    private String content;

    /** SUBMITTED/REVIEWED/REJECTED */
    private String status;

    /** 教师评分 1-5 */
    private Integer teacherScore;

    /** 教师评语 */
    private String teacherComment;

    /** 学生提交时间 */
    private LocalDateTime submitTime;

    /** 教师评阅时间 */
    private LocalDateTime teacherReviewTime;

    /** 教师评阅人 user_id */
    private Long teacherReviewUserId;
}
