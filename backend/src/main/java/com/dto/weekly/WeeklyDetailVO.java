package com.dto.weekly;

import com.dto.attachment.AttachmentVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "实习周记详情")
public class WeeklyDetailVO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentNo;

    private String yearWeek;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private String summary;
    private String nextPlan;

    private Integer sensitiveHit;
    private String sensitiveWords;
    private String sensitiveMarkedHtml;

    private String status;
    private Integer teacherScore;
    private String teacherComment;
    private LocalDateTime submitTime;
    private LocalDateTime teacherReviewTime;
    private String teacherReviewUserName;

    private LocalDateTime createTime;

    private List<AttachmentVO> attachments;
}
