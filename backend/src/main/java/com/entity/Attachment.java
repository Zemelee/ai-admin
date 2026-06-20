package com.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 附件元数据（MinIO 对象）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("attachment")
public class Attachment extends BaseEntity {

    /** 业务类型：LEAVE/LOG/WEEKLY/TRANSFER/USER_AVATAR 等 */
    private String bizType;

    /** 业务单 ID（上传时未生成可为空，提交后回填） */
    private Long bizId;

    /** 原始文件名 */
    private String fileName;

    /** MinIO object key */
    private String objectKey;

    /** 桶名 */
    private String bucket;

    /** MIME 类型 */
    private String contentType;

    /** 文件大小（字节） */
    private Long sizeBytes;

    /** 上传人 user_id */
    private Long uploaderId;
}
