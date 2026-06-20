package com.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.common.BizException;
import com.config.MinioConfig;
import com.dto.attachment.AttachmentVO;
import com.entity.Attachment;
import com.mapper.AttachmentMapper;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 通用附件服务：MinIO 对象存储 + attachment 元数据落库。
 * <p>统一支撑请假/日志/周记/单位变更等业务的图片附件上传、预览、绑定、删除。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final MinioConfig minioConfig;
    private final MinioClient minioClient;
    private final AttachmentMapper attachmentMapper;

    /** 允许上传的图片 MIME 白名单 */
    private static final Set<String> IMAGE_MIME = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp");

    /** 用于生成对外预签名 URL 的 client（基于 publicEndpoint） */
    private MinioClient presignClient;

    @PostConstruct
    void init() {
        presignClient = MinioClient.builder()
                .endpoint(minioConfig.getPublicEndpoint())
                .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                .build();
    }

    /**
     * 上传文件到 MinIO 并落库。
     *
     * @param userId  上传人 user_id
     * @param bizType 业务类型（LOG/LEAVE/...）
     * @param bizId   业务单 ID，可空（提交前上传，提交后回填）
     */
    public AttachmentVO upload(Long userId, String bizType, Long bizId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.validate("上传文件不能为空");
        }
        if (bizType == null || bizType.isBlank()) {
            throw BizException.validate("业务类型不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !IMAGE_MIME.contains(contentType.toLowerCase())) {
            throw BizException.validate("仅支持图片格式（jpg/png/gif/webp/bmp）");
        }

        ensureBucket();
        String bucket = minioConfig.getBucket();
        String objectKey = buildObjectKey(bizType, userId, file.getOriginalFilename());

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(is, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            log.error("上传 MinIO 失败 objectKey={}", objectKey, e);
            throw new BizException("附件上传失败，请重试");
        }

        Attachment att = new Attachment();
        att.setBizType(bizType);
        att.setBizId(bizId);
        att.setFileName(file.getOriginalFilename());
        att.setObjectKey(objectKey);
        att.setBucket(bucket);
        att.setContentType(contentType);
        att.setSizeBytes(file.getSize());
        att.setUploaderId(userId);
        attachmentMapper.insert(att);

        return toVO(att);
    }

    /**
     * 把上传时 bizId 为空、属当前用户的附件回填到某业务单。
     */
    public void bindBiz(Long userId, String bizType, Long bizId, List<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        attachmentMapper.update(null, new LambdaUpdateWrapper<Attachment>()
                .eq(Attachment::getBizType, bizType)
                .eq(Attachment::getUploaderId, userId)
                .isNull(Attachment::getBizId)
                .in(Attachment::getId, attachmentIds)
                .set(Attachment::getBizId, bizId));
    }

    /**
     * 查询某业务单的全部附件（带预签名预览 URL）。
     */
    public List<AttachmentVO> listByBiz(String bizType, Long bizId) {
        if (bizId == null) {
            return new ArrayList<>();
        }
        List<Attachment> list = attachmentMapper.selectList(new LambdaQueryWrapper<Attachment>()
                .eq(Attachment::getBizType, bizType)
                .eq(Attachment::getBizId, bizId)
                .orderByAsc(Attachment::getId));
        List<AttachmentVO> result = new ArrayList<>(list.size());
        for (Attachment a : list) {
            result.add(toVO(a));
        }
        return result;
    }

    /**
     * 统计某业务单的附件数。
     */
    public long countByBiz(String bizType, Long bizId) {
        if (bizId == null) return 0L;
        return attachmentMapper.selectCount(new LambdaQueryWrapper<Attachment>()
                .eq(Attachment::getBizType, bizType)
                .eq(Attachment::getBizId, bizId));
    }

    /**
     * 逻辑删除附件（仅上传人本人可删）。
     */
    public void softDelete(Long userId, Long id) {
        Attachment att = attachmentMapper.selectById(id);
        if (att == null) {
            throw BizException.notFound("附件不存在：id=" + id);
        }
        if (!att.getUploaderId().equals(userId)) {
            throw BizException.forbidden("无权删除他人附件");
        }
        attachmentMapper.deleteById(id);
    }

    // ---------- 内部工具 ----------

    /** 生成 object key：attachments/{bizType}/{yyyy/MM/dd}/{userId}/{uuid}-{safeName} */
    private String buildObjectKey(String bizType, Long userId, String originalName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String safeName = originalName == null ? "file" : originalName.replaceAll("[^\\w.\\-]", "_");
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "attachments/" + bizType + "/" + datePath + "/" + userId + "/" + uuid + "-" + safeName;
    }

    private void ensureBucket() {
        try {
            String bucket = minioConfig.getBucket();
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket 已创建：{}", bucket);
            }
        } catch (Exception e) {
            log.error("检查/创建 MinIO bucket 失败", e);
            throw new BizException("存储服务初始化失败，请联系管理员");
        }
    }

    private AttachmentVO toVO(Attachment a) {
        AttachmentVO vo = new AttachmentVO();
        vo.setId(a.getId());
        vo.setFileName(a.getFileName());
        vo.setContentType(a.getContentType());
        vo.setSizeBytes(a.getSizeBytes());
        vo.setCreateTime(a.getCreateTime());
        vo.setPreviewUrl(presignedGetUrl(a.getBucket(), a.getObjectKey()));
        return vo;
    }

    private String presignedGetUrl(String bucket, String objectKey) {
        try {
            return presignClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(minioConfig.getPresignedExpirySeconds())
                    .build());
        } catch (Exception e) {
            log.error("生成预签名 URL 失败 objectKey={}", objectKey, e);
            throw new BizException("附件预览地址生成失败");
        }
    }
}
