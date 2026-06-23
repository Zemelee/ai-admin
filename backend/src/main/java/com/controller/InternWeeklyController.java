package com.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.common.CurrentUserUtil;
import com.common.R;
import com.dto.weekly.WeeklyDetailVO;
import com.dto.weekly.WeeklyListItemVO;
import com.dto.weekly.WeeklyReviewReq;
import com.dto.weekly.WeeklySubmitReq;
import com.service.InternWeeklyService;
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
 * 实习周记接口
 * - /intern-weekly/student/** 学生提交/编辑/我的列表
 * - /intern-weekly/teacher/** 教师待评分列表/全部/评分
 * - /intern-weekly/{id}       通用详情（按角色鉴权）
 */
@Tag(name = "30.实习周记", description = "学生每周周记提交与教师评分")
@RestController
@RequestMapping("/intern-weekly")
@RequiredArgsConstructor
public class InternWeeklyController {

    private final InternWeeklyService weeklyService;

    @Operation(summary = "[学生] 提交本周周记")
    @SaCheckRole("student")
    @PostMapping("/student/submit")
    public R<Long> submit(@Valid @RequestBody WeeklySubmitReq req) {
        return R.ok(weeklyService.submit(CurrentUserUtil.userId(), req));
    }

    @Operation(summary = "[学生] 修改未评分的周记")
    @SaCheckRole("student")
    @PutMapping("/student/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody WeeklySubmitReq req) {
        weeklyService.update(CurrentUserUtil.userId(), id, req);
        return R.ok();
    }

    @Operation(summary = "[学生] 我的周记列表")
    @SaCheckRole("student")
    @GetMapping("/student/my")
    public R<List<WeeklyListItemVO>> my() {
        return R.ok(weeklyService.myWeeklies(CurrentUserUtil.userId()));
    }

    @Operation(summary = "[教师] 待评分周记")
    @SaCheckRole("teacher")
    @GetMapping("/teacher/pending")
    public R<List<WeeklyListItemVO>> teacherPending() {
        return R.ok(weeklyService.teacherPending(CurrentUserUtil.userId()));
    }

    @Operation(summary = "[教师] 分管学生全部周记")
    @SaCheckRole("teacher")
    @GetMapping("/teacher/my")
    public R<List<WeeklyListItemVO>> teacherAll() {
        return R.ok(weeklyService.teacherAll(CurrentUserUtil.userId()));
    }

    @Operation(summary = "[教师] 评分/驳回")
    @SaCheckRole("teacher")
    @PostMapping("/teacher/review/{id}")
    public R<Void> teacherReview(@PathVariable Long id, @Valid @RequestBody WeeklyReviewReq req) {
        weeklyService.teacherReview(CurrentUserUtil.userId(), id, req);
        return R.ok();
    }

    @Operation(summary = "[通用] 周记详情")
    @GetMapping("/{id}")
    public R<WeeklyDetailVO> detail(@PathVariable Long id) {
        return R.ok(weeklyService.detail(CurrentUserUtil.userId(), CurrentUserUtil.roleCode(), id));
    }
}
