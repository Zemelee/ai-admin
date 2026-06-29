package com.dto.companyeval;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "企业评价列表项")
public class EvalListItemVO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String className;
    private String major;
    private String internStatus;
    private Integer scoreAttendance;
    private Integer scoreAbility;
    private Integer scoreAttitude;
    private Integer scoreOverall;
    private String status;
    private LocalDateTime submitTime;
    private LocalDateTime createTime;
    /** 是否已存在评价记录（未评价学生此值为 false，id 为空） */
    private Boolean evaluated;
}
