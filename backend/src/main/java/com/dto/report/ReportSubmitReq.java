package com.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "实习报告提交")
public class ReportSubmitReq {

    @Schema(description = "报告类型：MID_TERM/FINAL")
    @NotBlank(message = "报告类型不能为空")
    private String reportType;

    @Schema(description = "报告标题")
    @NotBlank(message = "报告标题不能为空")
    @Size(min = 3, max = 100, message = "标题长度 3~100 字")
    private String title;

    @Schema(description = "报告内容（Markdown）")
    @NotBlank(message = "报告内容不能为空")
    @Size(min = 50, max = 5000, message = "内容长度 50~5000 字")
    private String content;

    @Schema(description = "附件 ID 列表")
    private List<Long> attachmentIds;
}
