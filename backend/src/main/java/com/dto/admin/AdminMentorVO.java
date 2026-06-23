package com.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "企业指导档案视图")
public class AdminMentorVO {

    private Long id;
    private Long userId;

    private String username;
    private String realName;
    private String phone;
    private Integer status;

    private Long companyId;
    private String companyName;
    private String position;
    private String dept;

    @Schema(description = "指导学生数")
    private Long studentCount;

    private LocalDateTime createTime;
}
