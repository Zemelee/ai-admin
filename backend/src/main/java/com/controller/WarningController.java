package com.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.common.CurrentUserUtil;
import com.common.R;
import com.dto.warning.WarningReviewReq;
import com.dto.warning.WarningStatsVO;
import com.dto.warning.WarningVO;
import com.service.WarningService;
import io.swagger.v3.oas.annotations.Operation;
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
 * 三色预警驾驶舱接口（仅 supervisor）
 */
@Tag(name = "50.三色预警", description = "实习三色预警驾驶舱：扫描/统计/列表/审核")
@RestController
@RequestMapping("/warning")
@RequiredArgsConstructor
@SaCheckRole("supervisor")
public class WarningController {

    private final WarningService warningService;

    @Operation(summary = "手动触发扫描")
    @PostMapping("/scan")
    public R<Integer> scan() {
        return R.ok(warningService.scan());
    }

    @Operation(summary = "预警统计聚合")
    @GetMapping("/stats")
    public R<WarningStatsVO> stats() {
        return R.ok(warningService.stats());
    }

    @Operation(summary = "预警列表（默认待处理）")
    @GetMapping
    public R<List<WarningVO>> list(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ruleCode) {
        return R.ok(warningService.list(level, status, ruleCode));
    }

    @Operation(summary = "审核预警（标记已处理/忽略）")
    @PostMapping("/review/{id}")
    public R<Void> review(@PathVariable Long id, @Valid @RequestBody WarningReviewReq req) {
        warningService.review(CurrentUserUtil.userId(), id, req);
        return R.ok();
    }
}
