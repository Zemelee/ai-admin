package com.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.common.CurrentUserUtil;
import com.common.R;
import com.dto.report.ReportDetailVO;
import com.dto.report.ReportListItemVO;
import com.dto.report.ReportReviewReq;
import com.dto.report.ReportSubmitReq;
import com.service.InternReportService;
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
 * 实习报告接口
 * - /intern-report/student/** 学生提交/编辑/我的列表
 * - /intern-report/teacher/** 教师待评分列表/全部/评分
 * - /intern-report/{id}       通用详情（按角色鉴权）
 */
@Tag(name = "31.实习报告", description = "学生实习报告提交与教师评分")
@RestController
@RequestMapping("/intern-report")
@RequiredArgsConstructor
public class InternReportController {

    private final InternReportService reportService;

    @Operation(summary = "[学生] 提交实习报告")
    @SaCheckRole("student")
    @PostMapping("/student/submit")
    public R<Long> submit(@Valid @RequestBody ReportSubmitReq req) {
        return R.ok(reportService.submit(CurrentUserUtil.userId(), req));
    }

    @Operation(summary = "[学生] 修改未评分的报告")
    @SaCheckRole("student")
    @PutMapping("/student/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ReportSubmitReq req) {
        reportService.update(CurrentUserUtil.userId(), id, req);
        return R.ok();
    }

    @Operation(summary = "[学生] 我的报告列表")
    @SaCheckRole("student")
    @GetMapping("/student/my")
    public R<List<ReportListItemVO>> my() {
        return R.ok(reportService.myReports(CurrentUserUtil.userId()));
    }

    @Operation(summary = "[教师] 待评分报告")
    @SaCheckRole("teacher")
    @GetMapping("/teacher/pending")
    public R<List<ReportListItemVO>> teacherPending() {
        return R.ok(reportService.teacherPending(CurrentUserUtil.userId()));
    }

    @Operation(summary = "[教师] 分管学生全部报告")
    @SaCheckRole("teacher")
    @GetMapping("/teacher/my")
    public R<List<ReportListItemVO>> teacherAll() {
        return R.ok(reportService.teacherAll(CurrentUserUtil.userId()));
    }

    @Operation(summary = "[教师] 评分/驳回")
    @SaCheckRole("teacher")
    @PostMapping("/teacher/review/{id}")
    public R<Void> teacherReview(@PathVariable Long id, @Valid @RequestBody ReportReviewReq req) {
        reportService.teacherReview(CurrentUserUtil.userId(), id, req);
        return R.ok();
    }

    @Operation(summary = "[通用] 报告详情")
    @GetMapping("/{id}")
    public R<ReportDetailVO> detail(@PathVariable Long id) {
        return R.ok(reportService.detail(CurrentUserUtil.userId(), CurrentUserUtil.roleCode(), id));
    }
}
