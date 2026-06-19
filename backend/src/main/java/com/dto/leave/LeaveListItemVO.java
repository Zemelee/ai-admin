package com.dto.leave;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假列表行（学生端 / 教师端待办 共用）
 */
@Data
@Schema(description = "请假列表行")
public class LeaveListItemVO {

    private Long id;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String className;

    private String leaveType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal durationDays;

    private String status;
    private String currentNode;
    private LocalDateTime submitTime;
    private LocalDateTime createTime;

    /** 请假事由 */
    private String reason;
}
