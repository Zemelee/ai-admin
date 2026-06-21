package com.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * supervisor 维护学生档案（含账号 + 绑定）请求体
 */
@Data
@Schema(description = "学生档案保存（新增/编辑）")
public class AdminStudentSaveReq {

    // ---------- 账号 ----------
    @Schema(description = "登录账号（学号）", example = "2022039999")
    @NotBlank(message = "登录账号不能为空")
    @Size(max = 64, message = "账号不超过 64 字")
    private String username;

    @Schema(description = "真实姓名")
    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名不超过 64 字")
    private String realName;

    @Schema(description = "手机号")
    @Size(max = 20, message = "手机号不超过 20 字")
    private String phone;

    @Schema(description = "密码（留空则默认 123456，编辑时留空表示不改）")
    @Size(max = 64, message = "密码不超过 64 字")
    private String password;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    // ---------- 档案 ----------
    @Schema(description = "学号")
    @NotBlank(message = "学号不能为空")
    @Size(max = 32, message = "学号不超过 32 字")
    private String studentNo;

    @Schema(description = "班级")
    @Size(max = 64, message = "班级不超过 64 字")
    private String className;

    @Schema(description = "专业")
    @Size(max = 64, message = "专业不超过 64 字")
    private String major;

    @Schema(description = "年级")
    @Size(max = 16, message = "年级不超过 16 字")
    private String grade;

    @Schema(description = "身份证号")
    @Size(max = 32, message = "身份证号不超过 32 字")
    private String idCard;

    @Schema(description = "性别：1男 2女")
    private Integer gender;

    @Schema(description = "家长电话")
    @Size(max = 20, message = "家长电话不超过 20 字")
    private String parentPhone;

    @Schema(description = "实习开始日期")
    private LocalDate internStart;

    @Schema(description = "实习结束日期")
    private LocalDate internEnd;

    @Schema(description = "实习状态：ACTIVE/SUSPEND/FINISHED/QUIT")
    private String internStatus;

    // ---------- 绑定 ----------
    @Schema(description = "指导教师 teacher.id")
    private Long teacherId;

    @Schema(description = "实习企业 company.id")
    private Long companyId;

    @Schema(description = "企业指导 mentor.id")
    private Long mentorId;
}
