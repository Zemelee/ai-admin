package com.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 企业指导（mentor）账号 + 档案 保存请求
 */
@Data
@Schema(description = "企业指导档案保存")
public class AdminMentorSaveReq {

    @Schema(description = "登录账号")
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

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "所属企业 company.id")
    @NotNull(message = "所属企业不能为空")
    private Long companyId;

    @Schema(description = "岗位")
    @Size(max = 64)
    private String position;

    @Schema(description = "所在部门")
    @Size(max = 64)
    private String dept;
}
