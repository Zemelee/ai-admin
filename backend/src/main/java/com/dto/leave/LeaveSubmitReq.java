package com.dto.leave;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生提交请假申请请求体
 */
@Data
@Schema(description = "请假申请提交")
public class LeaveSubmitReq {

    @Schema(description = "请假类型：SICK 病假 / PERSONAL 事假 / OTHER 其他", example = "PERSONAL")
    @NotBlank(message = "请假类型不能为空")
    private String leaveType;

    @Schema(description = "开始时间", example = "2026-06-20 09:00:00")
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", example = "2026-06-22 18:00:00")
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @Schema(description = "请假事由", example = "感冒发烧需就医休息")
    @NotBlank(message = "请假事由不能为空")
    @Size(max = 1000, message = "请假事由不超过 1000 字")
    private String reason;

    @Schema(description = "家长是否已确认", example = "true")
    private Boolean parentConfirm;

    @Schema(description = "佐证材料附件 ID 列表（诊断证明/家长知情书等图片）")
    private List<Long> attachmentIds;
}
