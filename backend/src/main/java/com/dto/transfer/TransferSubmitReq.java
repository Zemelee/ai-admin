package com.dto.transfer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 学生提交单位变更申请请求体
 */
@Data
@Schema(description = "单位变更申请提交")
public class TransferSubmitReq {

    @Schema(description = "新单位 id（系统内已登记企业，可空）", example = "12")
    private Long toCompanyId;

    @Schema(description = "新单位名称（toCompanyId 为空时必填，作为外部企业冗余存）", example = "上海星辰酒店")
    @Size(max = 128, message = "新单位名称不超过 128 字")
    private String toCompanyName;

    @Schema(description = "变更原因", example = "原单位与所学专业不符")
    @NotBlank(message = "变更原因不能为空")
    @Size(min = 5, max = 1000, message = "变更原因长度 5~1000 字")
    private String reason;

    @Schema(description = "预计入职日期", example = "2026-07-15")
    @NotNull(message = "预计入职日期不能为空")
    private LocalDate expectedStart;

    @Schema(description = "佐证材料附件 ID 列表（接收单位接收函/家长同意书等）")
    private List<Long> attachmentIds;
}
