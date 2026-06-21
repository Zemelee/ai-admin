package com.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 企业档案保存请求体
 */
@Data
@Schema(description = "企业档案保存（新增/编辑）")
public class AdminCompanySaveReq {

    @Schema(description = "企业名称")
    @NotBlank(message = "企业名称不能为空")
    @Size(max = 128, message = "企业名称不超过 128 字")
    private String name;

    @Schema(description = "统一社会信用代码")
    @Size(max = 32, message = "信用代码不超过 32 字")
    private String socialCode;

    @Schema(description = "地址")
    @Size(max = 255, message = "地址不超过 255 字")
    private String address;

    @Schema(description = "所属行业")
    @Size(max = 64, message = "行业不超过 64 字")
    private String industry;

    @Schema(description = "联系人")
    @Size(max = 64, message = "联系人不超过 64 字")
    private String contactPerson;

    @Schema(description = "联系电话")
    @Size(max = 20, message = "联系电话不超过 20 字")
    private String contactPhone;

    @Schema(description = "是否禁入清单：1是 0否")
    private Integer isBlacklist;

    @Schema(description = "禁入原因")
    @Size(max = 255, message = "禁入原因不超过 255 字")
    private String blacklistReason;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注不超过 500 字")
    private String remark;
}
