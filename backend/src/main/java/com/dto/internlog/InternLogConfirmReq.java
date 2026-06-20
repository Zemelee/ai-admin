package com.dto.internlog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 企业指导确认日志请求体
 */
@Data
@Schema(description = "企业指导日志确认")
public class InternLogConfirmReq {

    @Schema(description = "确认结果：CONFIRMED 通过 / REJECTED 驳回", example = "CONFIRMED")
    @NotBlank(message = "确认结果不能为空")
    private String result;

    @Schema(description = "确认意见（驳回必填）")
    @Size(max = 500, message = "确认意见不超过 500 字")
    private String comment;
}
