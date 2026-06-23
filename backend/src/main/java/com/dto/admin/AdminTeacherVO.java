package com.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师档案 VO（账号 + 档案 + 分管学生数）
 */
@Data
@Schema(description = "教师档案视图")
public class AdminTeacherVO {

    private Long id;
    private Long userId;

    private String username;
    private String realName;
    private String phone;
    private Integer status;

    private String teacherNo;
    private String department;
    private String title;
    private String officePhone;

    @Schema(description = "分管学生数")
    private Long studentCount;

    private LocalDateTime createTime;
}
