package com.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.common.CurrentUserUtil;
import com.common.R;
import com.dto.leave.LeaveApproveReq;
import com.dto.leave.LeaveDetailVO;
import com.dto.leave.LeaveListItemVO;
import com.dto.leave.LeaveSubmitReq;
import com.service.LeaveService;
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

import com.common.LeaveConst;

import java.util.List;

/**
 * 请假申请相关接口
 * - /leave/student/**     学生端：提交、撤回、我的列表
 * - /leave/teacher/**     教师端：待审批、历史、审批
 * - /leave/supervisor/**  监管者端：待审批（>30天）、历史、审批
 * - /leave/{id}           通用：详情（学生/教师/监管者均可，权限内见）
 */
@Tag(name = "30.请假申请", description = "学生请假申请的提交、查询与审批")
@RestController
@RequestMapping("/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    // ========== 学生端 ==========

    @Operation(summary = "[学生] 提交请假申请")
    @SaCheckRole("student")
    @PostMapping("/student/submit")
    public R<Long> submit(@Valid @RequestBody LeaveSubmitReq req) {
        Long id = leaveService.submit(CurrentUserUtil.userId(), req);
        return R.ok(id);
    }

    @Operation(summary = "[学生] 撤回请假申请（仅 PENDING 可撤回）")
    @SaCheckRole("student")
    @PostMapping("/student/cancel/{id}")
    public R<Void> cancel(@PathVariable Long id) {
        leaveService.cancel(CurrentUserUtil.userId(), id);
        return R.ok();
    }

    @Operation(summary = "[学生] 我的请假列表")
    @SaCheckRole("student")
    @GetMapping("/student/my")
    public R<List<LeaveListItemVO>> myLeaves(
            @Parameter(description = "状态过滤：PENDING/APPROVED/REJECTED/CANCELLED，留空查全部")
            @RequestParam(required = false) String status) {
        return R.ok(leaveService.myLeaves(CurrentUserUtil.userId(), status));
    }

    // ========== 教师端 ==========

    @Operation(summary = "[教师] 待我审批的请假列表")
    @SaCheckRole("teacher")
    @GetMapping("/teacher/pending")
    public R<List<LeaveListItemVO>> teacherPending() {
        return R.ok(leaveService.teacherPending(CurrentUserUtil.userId()));
    }

    @Operation(summary = "[教师] 我已审批过的请假历史")
    @SaCheckRole("teacher")
    @GetMapping("/teacher/history")
    public R<List<LeaveListItemVO>> teacherHistory() {
        return R.ok(leaveService.approvedHistoryByMe(CurrentUserUtil.userId()));
    }

    @Operation(summary = "[教师] 审批请假（result=APPROVED/REJECTED）")
    @SaCheckRole("teacher")
    @PostMapping("/teacher/approve/{id}")
    public R<Void> teacherApprove(@PathVariable Long id, @Valid @RequestBody LeaveApproveReq req) {
        leaveService.approve(CurrentUserUtil.userId(), id, LeaveConst.NODE_TEACHER, req);
        return R.ok();
    }

    // ========== 监管者端（系主任） ==========

    @Operation(summary = "[监管者] 待审批列表（教师已批 + 时长 >30 天）")
    @SaCheckRole("supervisor")
    @GetMapping("/supervisor/pending")
    public R<List<LeaveListItemVO>> supervisorPending() {
        return R.ok(leaveService.supervisorPending());
    }

    @Operation(summary = "[监管者] 我已审批过的请假历史")
    @SaCheckRole("supervisor")
    @GetMapping("/supervisor/history")
    public R<List<LeaveListItemVO>> supervisorHistory() {
        return R.ok(leaveService.approvedHistoryByMe(CurrentUserUtil.userId()));
    }

    @Operation(summary = "[监管者] 审批请假（result=APPROVED/REJECTED）")
    @SaCheckRole("supervisor")
    @PostMapping("/supervisor/approve/{id}")
    public R<Void> supervisorApprove(@PathVariable Long id, @Valid @RequestBody LeaveApproveReq req) {
        leaveService.approve(CurrentUserUtil.userId(), id, LeaveConst.NODE_SUPERVISOR, req);
        return R.ok();
    }

    // ========== 通用：详情（学生/教师/监管者均可访问，由 service 内部按角色鉴权） ==========

    @Operation(summary = "[通用] 请假详情（含审批流），按角色控制可见范围")
    @GetMapping("/{id}")
    public R<LeaveDetailVO> detail(@PathVariable Long id) {
        Long userId = CurrentUserUtil.userId();
        String roleCode = CurrentUserUtil.roleCode();
        return R.ok(leaveService.detail(userId, roleCode, id));
    }
}
