package com.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.common.CurrentUserUtil;
import com.common.R;
import com.dto.internlog.InternLogConfirmReq;
import com.dto.internlog.InternLogDetailVO;
import com.dto.internlog.InternLogListItemVO;
import com.dto.internlog.InternLogSubmitReq;
import com.service.InternLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 实习日志接口
 * - /intern-log/student/**  学生端：提交、修改、我的列表
 * - /intern-log/mentor/**   企业指导端：待确认列表、确认/驳回
 * - /intern-log/{id}        通用：详情（按角色控制可见范围）
 */
@Tag(name = "30.实习日志", description = "学生每日实习日志提交与企业指导确认")
@RestController
@RequestMapping("/intern-log")
@RequiredArgsConstructor
public class InternLogController {

    private final InternLogService internLogService;

    // ========== 学生端 ==========

    @Operation(summary = "[学生] 提交今日实习日志（可附图）")
    @SaCheckRole("student")
    @PostMapping("/student/submit")
    public R<Long> submit(@Valid @RequestBody InternLogSubmitReq req) {
        return R.ok(internLogService.submit(CurrentUserUtil.userId(), req));
    }

    @Operation(summary = "[学生] 修改未确认的日志")
    @SaCheckRole("student")
    @PutMapping("/student/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody InternLogSubmitReq req) {
        internLogService.update(CurrentUserUtil.userId(), id, req);
        return R.ok();
    }

    @Operation(summary = "[学生] 我的实习日志列表")
    @SaCheckRole("student")
    @GetMapping("/student/my")
    public R<List<InternLogListItemVO>> myLogs() {
        return R.ok(internLogService.myLogs(CurrentUserUtil.userId()));
    }

    // ========== 企业指导端 ==========

    @Operation(summary = "[企业指导] 待我确认的日志列表")
    @SaCheckRole("mentor")
    @GetMapping("/mentor/pending")
    public R<List<InternLogListItemVO>> mentorPending() {
        return R.ok(internLogService.mentorPending(CurrentUserUtil.userId()));
    }

    @Operation(summary = "[企业指导] 确认/驳回日志（result=CONFIRMED/REJECTED）")
    @SaCheckRole("mentor")
    @PostMapping("/mentor/confirm/{id}")
    public R<Void> mentorConfirm(@PathVariable Long id, @Valid @RequestBody InternLogConfirmReq req) {
        internLogService.mentorConfirm(CurrentUserUtil.userId(), id, req);
        return R.ok();
    }

    // ========== 通用：详情 ==========

    @Operation(summary = "[通用] 日志详情（含附件 + 确认信息），按角色控制可见范围")
    @GetMapping("/{id}")
    public R<InternLogDetailVO> detail(@PathVariable Long id) {
        Long userId = CurrentUserUtil.userId();
        String roleCode = CurrentUserUtil.roleCode();
        return R.ok(internLogService.detail(userId, roleCode, id));
    }
}
