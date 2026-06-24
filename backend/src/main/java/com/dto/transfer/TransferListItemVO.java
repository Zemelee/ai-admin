package com.dto.transfer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 单位变更列表行（学生端 / 各角色待办 共用）
 */
@Data
@Schema(description = "单位变更列表行")
public class TransferListItemVO {

    private Long id;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String className;

    private Long fromCompanyId;
    private String fromCompanyName;
    private Long toCompanyId;
    private String toCompanyName;

    private LocalDate expectedStart;
    /** 变更原因 */
    private String reason;

    private String status;
    private String currentNode;
    private LocalDateTime submitTime;
    private LocalDateTime createTime;
}
