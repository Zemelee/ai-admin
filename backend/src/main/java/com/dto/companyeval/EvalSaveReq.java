package com.dto.companyeval;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "企业评价保存请求")
public class EvalSaveReq {
    @NotNull
    @Schema(description = "学生 ID")
    private Long studentId;

    @NotNull
    @Schema(description = "出勤与纪律 1-5")
    private Integer scoreAttendance;

    @NotNull
    @Schema(description = "专业能力 1-5")
    private Integer scoreAbility;

    @NotNull
    @Schema(description = "工作态度 1-5")
    private Integer scoreAttitude;

    @NotNull
    @Schema(description = "综合评价 1-5")
    private Integer scoreOverall;

    @Schema(description = "鉴定评语")
    private String comment;
}
