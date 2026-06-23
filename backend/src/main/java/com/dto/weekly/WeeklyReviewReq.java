package com.dto.weekly;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "教师周记评分")
public class WeeklyReviewReq {

    @Schema(description = "结果：REVIEWED 通过评分 / REJECTED 驳回")
    @NotBlank(message = "结果不能为空")
    private String result;

    @Schema(description = "评分 1-5（通过时必填）")
    @Min(value = 1, message = "评分至少 1 分")
    @Max(value = 5, message = "评分最多 5 分")
    private Integer score;

    @Schema(description = "评语（驳回必填）")
    @Size(max = 500)
    private String comment;
}
