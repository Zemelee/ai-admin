package com.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.common.CurrentUserUtil;
import com.common.R;
import com.dto.score.ScoreItemVO;
import com.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 实习成绩汇总接口
 */
@Tag(name = "60.实习成绩", description = "四维加权汇总实习成绩（日志+周记+报告+企业鉴定）")
@RestController
@RequestMapping("/score")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    @Operation(summary = "[教师] 分管学生成绩汇总")
    @SaCheckRole("teacher")
    @GetMapping("/teacher/my")
    public R<List<ScoreItemVO>> teacherScores() {
        return R.ok(scoreService.teacherScores(CurrentUserUtil.userId()));
    }

    @Operation(summary = "[supervisor] 全部学生成绩汇总")
    @SaCheckRole("supervisor")
    @GetMapping("/supervisor/all")
    public R<List<ScoreItemVO>> allScores() {
        return R.ok(scoreService.allScores());
    }

    @Operation(summary = "[学生] 我的实习成绩")
    @SaCheckRole("student")
    @GetMapping("/student/my")
    public R<ScoreItemVO> myScore() {
        return R.ok(scoreService.myScore(CurrentUserUtil.userId()));
    }
}
