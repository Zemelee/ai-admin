package com.dto.report;

import com.dto.attachment.AttachmentVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "实习报告详情")
public class ReportDetailVO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentNo;

    private String reportType;
    private String title;
    private String content;

    private String status;
    private Integer teacherScore;
    private String teacherComment;
    private LocalDateTime submitTime;
    private LocalDateTime teacherReviewTime;
    private String teacherReviewUserName;

    private LocalDateTime createTime;

    private List<AttachmentVO> attachments;
}
