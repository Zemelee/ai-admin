package com.dto.transfer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 单位变更审批操作请求
 */
@Data
@Schema(description = "审批操作")
public class TransferApproveReq {

    @Schema(description = "审批结果：APPROVED 通过 / REJECTED 驳回", example = "APPROVED")
    @NotBlank(message = "审批结果不能为空")
    private String result;

    @Schema(description = "审批意见（驳回必填，通过可选）")
    @Size(max = 500, message = "审批意见不超过 500 字")
    private String comment;
}
