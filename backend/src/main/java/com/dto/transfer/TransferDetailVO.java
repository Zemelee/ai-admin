package com.dto.transfer;

import com.dto.attachment.AttachmentVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 单位变更详情视图（含审批流水）
 */
@Data
@Schema(description = "单位变更申请详情")
public class TransferDetailVO {

    private Long id;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String className;
    private String major;

    private Long fromCompanyId;
    private String fromCompanyName;
    private Long toCompanyId;
    private String toCompanyName;

    private String reason;
    private LocalDate expectedStart;

    private String status;
    private String currentNode;
    private LocalDateTime submitTime;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;

    /** 审批流水（按 nodeSeq 升序） */
    private List<FlowItem> flow;

    /** 佐证材料附件列表（含预签名预览 URL） */
    private List<AttachmentVO> attachments;

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
