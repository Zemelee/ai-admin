package com.dto.internlog;

import com.dto.attachment.AttachmentVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 实习日志详情 VO（含附件 + mentor 确认信息）
 */
@Data
@Schema(description = "实习日志详情")
public class InternLogDetailVO {

    @Schema(description = "日志 ID")
    private Long id;

    @Schema(description = "学生 ID")
    private Long studentId;

    @Schema(description = "学生姓名")
    private String studentName;

    @Schema(description = "学号")
    private String studentNo;

    @Schema(description = "日志日期")
    private LocalDate logDate;

    @Schema(description = "日志内容全文")
    private String content;

    @Schema(description = "状态：SUBMITTED/CONFIRMED/REJECTED")
    private String status;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "企业指导确认时间")
    private LocalDateTime mentorReviewTime;

    @Schema(description = "确认人姓名")
    private String mentorReviewUserName;

    @Schema(description = "企业指导确认意见")
    private String mentorComment;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "图片附件列表（含预签名预览 URL）")
    private List<AttachmentVO> attachments;
}
