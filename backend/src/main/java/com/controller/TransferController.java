package com.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.common.CurrentUserUtil;
import com.common.R;
import com.common.TransferConst;
import com.dto.transfer.TransferApproveReq;
import com.dto.transfer.TransferDetailVO;
import com.dto.transfer.TransferListItemVO;
import com.dto.transfer.TransferSubmitReq;
import com.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 实习单位变更申请相关接口
 * - /transfer/student/**     学生端：提交、撤回、我的列表
 * - /transfer/mentor/**      企业指导端：待审批/历史/审批（第 1 节点）
 * - /transfer/teacher/**     教师端：待审批/历史/审批（第 2 节点）
 * - /transfer/supervisor/**  监管者端：待审批/历史/审批（第 3 节点 / 终审）
 * - /transfer/{id}           通用：详情，按角色控制可见
 */
@Tag(name = "35.实习单位变更", description = "学生发起、企业指导→教师→监管者 串行审批")
@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    // ========== 学生端 ==========

    @Operation(summary = "[学生] 提交单位变更申请")
    @SaCheckRole("student")
    @PostMapping("/student/submit")
    public R<Long> submit(@Valid @RequestBody TransferSubmitReq req) {
        return R.ok(transferService.submit(CurrentUserUtil.userId(), req));
    }

    @Operation(summary = "[学生] 撤回变更申请（仅 PENDING）")
    @SaCheckRole("student")
    @PostMapping("/student/cancel/{id}")
    public R<Void> cancel(@PathVariable Long id) {
        transferService.cancel(CurrentUserUtil.userId(), id);
        return R.ok();
    }

    @Operation(summary = "[学生] 我的变更申请列表")
    @SaCheckRole("student")
    @GetMapping("/student/my")
    public R<List<TransferListItemVO>> myTransfers(
            @Parameter(description = "状态过滤：PENDING/APPROVED/REJECTED/CANCELLED，留空查全部")
            @RequestParam(required = false) String status) {
        return R.ok(transferService.myTransfers(CurrentUserUtil.userId(), status));
    }

    // ========== 企业指导端 ==========

    @Operation(summary = "[企业指导] 待我审批的变更列表")
    @SaCheckRole("mentor")
    @GetMapping("/mentor/pending")
    public R<List<TransferListItemVO>> mentorPending() {
        return R.ok(transferService.pendingForApprover(CurrentUserUtil.userId(), TransferConst.NODE_MENTOR));
    }

    @Operation(summary = "[企业指导] 我已审批过的历史")
    @SaCheckRole("mentor")
    @GetMapping("/mentor/history")
    public R<List<TransferListItemVO>> mentorHistory() {
        return R.ok(transferService.approvedHistoryByMe(CurrentUserUtil.userId()));
    }

    @Operation(summary = "[企业指导] 审批变更")
    @SaCheckRole("mentor")
    @PostMapping("/mentor/approve/{id}")
    public R<Void> mentorApprove(@PathVariable Long id, @Valid @RequestBody TransferApproveReq req) {
        transferService.approve(CurrentUserUtil.userId(), id, TransferConst.NODE_MENTOR, req);
        return R.ok();
    }

    // ========== 教师端 ==========

    @Operation(summary = "[教师] 待我审批的变更列表")
    @SaCheckRole("teacher")
    @GetMapping("/teacher/pending")
    public R<List<TransferListItemVO>> teacherPending() {
        return R.ok(transferService.pendingForApprover(CurrentUserUtil.userId(), TransferConst.NODE_TEACHER));
    }

    @Operation(summary = "[教师] 我已审批过的历史")
    @SaCheckRole("teacher")
    @GetMapping("/teacher/history")
    public R<List<TransferListItemVO>> teacherHistory() {
        return R.ok(transferService.approvedHistoryByMe(CurrentUserUtil.userId()));
    }

    @Operation(summary = "[教师] 审批变更")
    @SaCheckRole("teacher")
    @PostMapping("/teacher/approve/{id}")
    public R<Void> teacherApprove(@PathVariable Long id, @Valid @RequestBody TransferApproveReq req) {
        transferService.approve(CurrentUserUtil.userId(), id, TransferConst.NODE_TEACHER, req);
        return R.ok();
    }

    // ========== 监管者端 ==========

    @Operation(summary = "[监管者] 待审批列表（教师已批）")
    @SaCheckRole("supervisor")
    @GetMapping("/supervisor/pending")
    public R<List<TransferListItemVO>> supervisorPending() {
        return R.ok(transferService.pendingForApprover(CurrentUserUtil.userId(), TransferConst.NODE_SUPERVISOR));
    }

    @Operation(summary = "[监管者] 我已审批过的历史")
    @SaCheckRole("supervisor")
    @GetMapping("/supervisor/history")
    public R<List<TransferListItemVO>> supervisorHistory() {
        return R.ok(transferService.approvedHistoryByMe(CurrentUserUtil.userId()));
    }

    @Operation(summary = "[监管者] 审批变更（终审）")
    @SaCheckRole("supervisor")
    @PostMapping("/supervisor/approve/{id}")
    public R<Void> supervisorApprove(@PathVariable Long id, @Valid @RequestBody TransferApproveReq req) {
        transferService.approve(CurrentUserUtil.userId(), id, TransferConst.NODE_SUPERVISOR, req);
        return R.ok();
    }

    // ========== 通用详情 ==========

    @Operation(summary = "[通用] 变更详情（含审批流），按角色控制可见范围")
    @GetMapping("/{id}")
    public R<TransferDetailVO> detail(@PathVariable Long id) {
        return R.ok(transferService.detail(CurrentUserUtil.userId(), CurrentUserUtil.roleCode(), id));
    }
}
