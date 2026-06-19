package com.dto.leave;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 请假详情视图（含审批流水）
 */
@Data
@Schema(description = "请假申请详情")
public class LeaveDetailVO {

    private Long id;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String className;
    private String major;
    private String companyName;

    private String leaveType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal durationDays;
    private String reason;
    private Integer parentConfirm;

    private String status;
    private String currentNode;
    private LocalDateTime submitTime;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;

    /** 审批流水（按 nodeSeq 升序） */
    private List<FlowItem> flow;

    @Data
    @Schema(description = "审批流水节点")
    public static class FlowItem {
        private Long id;
        private String node;
        private Integer nodeSeq;
        private Long approverId;
        private String approverName;
        private String result;
        private String comment;
        private LocalDateTime actTime;
    }
}
