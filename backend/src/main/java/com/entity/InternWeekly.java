package com.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 实习周记
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("intern_weekly")
public class InternWeekly extends BaseEntity {

    private Long studentId;

    /** ISO 周编号，如 2026-W12 */
    private String yearWeek;

    private LocalDate weekStart;
    private LocalDate weekEnd;

    /** 本周总结 */
    private String summary;

    /** 下周计划 */
    private String nextPlan;

    private Integer sensitiveHit;
    private String sensitiveWords;
    private String sensitiveMarkedHtml;

    /** 教师评分 1-5 */
    private Integer teacherScore;
    private String teacherComment;

    /** SUBMITTED/REVIEWED/REJECTED */
    private String status;

    private LocalDateTime submitTime;
    private LocalDateTime teacherReviewTime;
    private Long teacherReviewUserId;
}
