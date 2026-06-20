package com.dto.internlog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 学生提交/更新实习日志请求体
 */
@Data
@Schema(description = "实习日志提交")
public class InternLogSubmitReq {

    @Schema(description = "日志日期", example = "2026-06-20")
    @NotNull(message = "日志日期不能为空")
    @PastOrPresent(message = "日志日期不能晚于今天")
    private LocalDate logDate;

    @Schema(description = "日志内容", example = "今天在前台跟岗学习接待流程……")
    @NotBlank(message = "日志内容不能为空")
    @Size(min = 5, max = 2000, message = "日志内容长度 5~2000 字")
    private String content;

    @Schema(description = "附件 ID 列表（上传接口返回的 id）")
    private List<Long> attachmentIds;
}
