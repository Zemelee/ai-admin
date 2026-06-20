package com.controller;

import com.common.CurrentUserUtil;
import com.common.R;
import com.dto.attachment.AttachmentVO;
import com.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 通用附件接口（MinIO 中转上传 + 预签名预览）
 */
@Tag(name = "30.通用附件", description = "图片附件上传、预览、删除")
@RestController
@RequestMapping("/attachment")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @Operation(summary = "上传图片附件")
    @PostMapping("/upload")
    public R<AttachmentVO> upload(
            @Parameter(description = "业务类型：LOG/LEAVE/WEEKLY/TRANSFER 等") @RequestParam String bizType,
            @Parameter(description = "业务单 ID（提交前上传可留空）") @RequestParam(required = false) Long bizId,
            @RequestParam("file") MultipartFile file) {
        return R.ok(attachmentService.upload(CurrentUserUtil.userId(), bizType, bizId, file));
    }

    @Operation(summary = "查询某业务单的附件列表（含预签名预览 URL）")
    @GetMapping("/list")
    public R<List<AttachmentVO>> list(
            @RequestParam String bizType,
            @RequestParam Long bizId) {
        return R.ok(attachmentService.listByBiz(bizType, bizId));
    }

    @Operation(summary = "删除附件（仅上传人本人）")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        attachmentService.softDelete(CurrentUserUtil.userId(), id);
        return R.ok();
    }
}
