package com.dto.report;

import com.dto.attachment.AttachmentVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "实习报告列表项")
public class ReportListItemVO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String reportType;
    private String title;
    private Integer attachmentCount;
    private String status;
    private Integer teacherScore;
    private LocalDateTime submitTime;
    private LocalDateTime teacherReviewTime;
    private LocalDateTime createTime;
}
