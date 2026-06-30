package com.dto.score;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "学生实习综合成绩")
public class ScoreItemVO {
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String className;
    private String major;
    private String companyName;
    private String internStatus;

    /** 日志：已提交数 */
    private Integer logSubmitted;
    /** 日志：应提交天数 */
    private Integer logExpected;
    /** 日志：提交率 0-1 */
    private BigDecimal logRate;
    /** 日志得分 0-25 */
    private BigDecimal logScore;

    /** 周记均分 0-5（已评分） */
    private BigDecimal weeklyAvg;
    /** 周记得分 0-25 */
    private BigDecimal weeklyScore;

    /** 报告均分 0-5（已评分） */
    private BigDecimal reportAvg;
    /** 报告得分 0-20 */
    private BigDecimal reportScore;

    /** 企业鉴定综合分 0-5 */
    private BigDecimal evalScore5;
    /** 是否已提交鉴定 */
    private Boolean evalSubmitted;
    /** 企业鉴定得分 0-30 */
    private BigDecimal evalScore;

    /** 综合成绩 0-100 */
    private BigDecimal totalScore;
    /** 等级：优秀/良好/中等/及格/不及格/无法评定 */
    private String grade;
}
