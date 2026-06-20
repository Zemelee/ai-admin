package com.dto.internlog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 实习日志列表项 VO
 */
@Data
@Schema(description = "实习日志列表项")
public class InternLogListItemVO {

    @Schema(description = "日志 ID")
    private Long id;

    @Schema(description = "学生 ID")
    private Long studentId;

    @Schema(description = "学生姓名（mentor 视角需要）")
    private String studentName;

    @Schema(description = "学号（mentor 视角需要）")
    private String studentNo;

    @Schema(description = "日志日期")
    private LocalDate logDate;

    @Schema(description = "内容摘要（前 50 字）")
    private String contentSummary;

    @Schema(description = "图片附件数")
    private Integer attachmentCount;

    @Schema(description = "AI 是否命中敏感词")
    private Integer sensitiveHit;

    @Schema(description = "状态：SUBMITTED/CONFIRMED/REJECTED")
    private String status;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "企业指导确认时间")
    private LocalDateTime mentorReviewTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
