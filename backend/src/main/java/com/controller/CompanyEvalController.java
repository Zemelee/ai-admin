package com.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.common.CurrentUserUtil;
import com.common.R;
import com.dto.companyeval.EvalDetailVO;
import com.dto.companyeval.EvalListItemVO;
import com.dto.companyeval.EvalSaveReq;
import com.service.CompanyEvalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 企业评价 / 实习鉴定接口
 * - /company-eval/mentor/**   mentor 评价 / 提交
 * - /company-eval/student/my  学生查看自己的鉴定
 * - /company-eval/teacher/**  教师查看分管学生鉴定
 * - /company-eval/supervisor  系主任查看全部鉴定
 * - /company-eval/{id}        通用详情（按角色鉴权）
 */
@Tag(name = "32.企业评价/实习鉴定", description = "企业指导对学生做实习鉴定，四维度评分+评语")
@RestController
@RequestMapping("/company-eval")
@RequiredArgsConstructor
public class CompanyEvalController {

    private final CompanyEvalService evalService;

    @Operation(summary = "[mentor] 我负责的学生（含评价状态）")
    @SaCheckRole("mentor")
    @GetMapping("/mentor/my-students")
    public R<List<EvalListItemVO>> myStudents() {
        return R.ok(evalService.myStudents(CurrentUserUtil.userId()));
    }

    @Operation(summary = "[mentor] 保存评价草稿")
    @SaCheckRole("mentor")
    @PostMapping("/mentor/save")
    public R<Long> save(@Valid @RequestBody EvalSaveReq req) {
        return R.ok(evalService.save(CurrentUserUtil.userId(), req));
    }

    @Operation(summary = "[mentor] 提交鉴定（锁定）")
    @SaCheckRole("mentor")
    @PostMapping("/mentor/submit/{id}")
    public R<Void> submit(@PathVariable Long id) {
        evalService.submit(CurrentUserUtil.userId(), id);
        return R.ok();
    }

    @Operation(summary = "[student] 我的实习鉴定")
    @SaCheckRole("student")
    @GetMapping("/student/my")
    public R<EvalDetailVO> myAppraisal() {
        return R.ok(evalService.myAppraisal(CurrentUserUtil.userId()));
    }

    @Operation(summary = "[teacher] 分管学生鉴定列表")
    @SaCheckRole("teacher")
    @GetMapping("/teacher/my")
    public R<List<EvalListItemVO>> teacherAll() {
        return R.ok(evalService.teacherAll(CurrentUserUtil.userId()));
    }

    @Operation(summary = "[supervisor] 全部鉴定列表")
    @SaCheckRole("supervisor")
    @GetMapping("/supervisor/all")
    public R<List<EvalListItemVO>> supervisorAll() {
        return R.ok(evalService.supervisorAll());
    }

    @Operation(summary = "[通用] 鉴定详情")
    @GetMapping("/{id}")
    public R<EvalDetailVO> detail(@PathVariable Long id) {
        return R.ok(evalService.detail(CurrentUserUtil.userId(), CurrentUserUtil.roleCode(), id));
    }
}
