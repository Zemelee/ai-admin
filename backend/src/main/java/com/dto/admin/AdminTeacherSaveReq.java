package com.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 教师账号 + 档案 保存请求
 */
@Data
@Schema(description = "教师档案保存")
public class AdminTeacherSaveReq {

    @Schema(description = "登录账号（工号）")
    @NotBlank(message = "登录账号不能为空")
    @Size(max = 64)
    private String username;

    @Schema(description = "真实姓名")
    @NotBlank(message = "姓名不能为空")
    @Size(max = 64)
    private String realName;

    @Schema(description = "手机号")
    @Size(max = 20)
    private String phone;

    @Schema(description = "密码（新增留空默认 123456，编辑留空不改）")
    @Size(max = 64)
    private String password;

    @Schema(description = "状态 1启用 0停用")
    private Integer status;

    @Schema(description = "工号")
    @NotBlank(message = "工号不能为空")
    @Size(max = 32)
    private String teacherNo;

    @Schema(description = "所属系/部门")
    @Size(max = 64)
    private String department;

    @Schema(description = "职称")
    @Size(max = 32)
    private String title;

    @Schema(description = "办公电话")
    @Size(max = 20)
    private String officePhone;
}
