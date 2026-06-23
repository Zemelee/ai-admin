package com.dto.weekly;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "实习周记提交")
public class WeeklySubmitReq {

    @Schema(description = "ISO 周编号", example = "2026-W25")
    @NotBlank(message = "周编号不能为空")
    @Size(max = 8)
    private String yearWeek;

    @Schema(description = "周一日期")
    @NotNull(message = "周一日期不能为空")
    private LocalDate weekStart;

    @Schema(description = "周日日期")
    @NotNull(message = "周日日期不能为空")
    private LocalDate weekEnd;

    @Schema(description = "本周总结")
    @NotBlank(message = "本周总结不能为空")
    @Size(min = 10, max = 3000, message = "本周总结长度 10~3000 字")
    private String summary;

    @Schema(description = "下周计划")
    @Size(max = 2000, message = "下周计划不超过 2000 字")
    private String nextPlan;

    @Schema(description = "附件 ID 列表")
    private List<Long> attachmentIds;
}
