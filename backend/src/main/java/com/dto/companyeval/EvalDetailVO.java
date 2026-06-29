package com.dto.companyeval;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "企业评价详情")
public class EvalDetailVO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String className;
    private String major;
    private String companyName;
    private LocalDate internStart;
    private LocalDate internEnd;
    private String internStatus;

    private String mentorName;
    private Integer scoreAttendance;
    private Integer scoreAbility;
    private Integer scoreAttitude;
    private Integer scoreOverall;
    private String comment;
    private String status;
    private LocalDateTime submitTime;
    private LocalDateTime createTime;
}
