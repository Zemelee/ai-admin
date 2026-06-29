package com.dto.warning;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "预警事件")
public class WarningVO {
    private Long id;
    private String level;
    private String ruleCode;
    private String ruleDesc;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String className;
    private String major;
    private String bizType;
    private Long bizId;
    private String detail;
    private String status;
    private Long reviewerId;
    private String reviewerName;
    private LocalDateTime reviewTime;
    private String reviewNote;
    private LocalDateTime createTime;
}
