package com.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业档案 VO（含 mentor/学生计数）
 */
@Data
@Schema(description = "企业档案视图")
public class AdminCompanyVO {

    private Long id;
    private String name;
    private String socialCode;
    private String address;
    private String industry;
    private String contactPerson;
    private String contactPhone;
    private Integer isBlacklist;
    private String blacklistReason;
    private String remark;

    @Schema(description = "该企业下企业指导数")
    private Long mentorCount;

    @Schema(description = "该企业在岗实习学生数")
    private Long studentCount;

    private LocalDateTime createTime;
}
