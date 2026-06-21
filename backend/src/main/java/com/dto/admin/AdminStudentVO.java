package com.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生档案管理列表/详情 VO（含账号 + 关联名 + 绑定）
 */
@Data
@Schema(description = "学生档案视图")
public class AdminStudentVO {

    private Long id;
    private Long userId;

    // 账号
    private String username;
    private String realName;
    private String phone;
    private Integer status;

    // 档案
    private String studentNo;
    private String className;
    private String major;
    private String grade;
    private String idCard;
    private Integer gender;
    private String parentPhone;
    private LocalDate internStart;
    private LocalDate internEnd;
    private String internStatus;

    // 绑定
    private Long teacherId;
    private Long companyId;
    private Long mentorId;

    // 关联名（列表展示）
    private String teacherName;
    private String companyName;
    private String mentorName;

    private LocalDateTime createTime;
}
