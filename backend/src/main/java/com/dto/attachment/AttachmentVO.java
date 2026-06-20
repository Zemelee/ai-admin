package com.dto.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件展示 VO（含 MinIO 预签名预览 URL）
 */
@Data
@Schema(description = "附件信息")
public class AttachmentVO {

    @Schema(description = "附件 ID")
    private Long id;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "MIME 类型")
    private String contentType;

    @Schema(description = "文件大小（字节）")
    private Long sizeBytes;

    @Schema(description = "预签名预览 URL（有效期 2 小时）")
    private String previewUrl;

    @Schema(description = "上传时间")
    private LocalDateTime createTime;
}
