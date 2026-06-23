package com.dto.weekly;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "实习周记列表项")
public class WeeklyListItemVO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String yearWeek;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private String summarySummary; // 摘要（避免与 summary 冲突）
    private Integer attachmentCount;
    private Integer sensitiveHit;
    private String status;
    private Integer teacherScore;
    private LocalDateTime submitTime;
    private LocalDateTime teacherReviewTime;
    private LocalDateTime createTime;
}
