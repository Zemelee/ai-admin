package com.dto.warning;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "预警审核请求")
public class WarningReviewReq {
    @NotBlank
    @Schema(description = "REVIEWED/IGNORED")
    private String status;

    @Schema(description = "审核备注")
    private String reviewNote;
}
