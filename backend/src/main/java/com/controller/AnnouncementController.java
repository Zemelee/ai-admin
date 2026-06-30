package com.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.common.CurrentUserUtil;
import com.common.R;
import com.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "41.系统公告", description = "公告管理（supervisor）+ 查看（所有角色）")
@RestController
@RequestMapping("/announcement")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "[supervisor] 分页查询公告")
    @SaCheckRole("supervisor")
    @GetMapping("/list")
    public R<IPage<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return R.ok(announcementService.page(status, page, size));
    }

    @Operation(summary = "[公开] 最新公告（所有角色，无需登录）")
    @GetMapping("/latest")
    public R<List<Map<String, Object>>> latest(@RequestParam(defaultValue = "5") int limit) {
        return R.ok(announcementService.latest(limit));
    }

    @Operation(summary = "[supervisor] 发布公告")
    @SaCheckRole("supervisor")
    @PostMapping
    public R<Long> create(@RequestBody Map<String, String> body) {
        Long id = announcementService.create(
                CurrentUserUtil.userId(),
                body.get("title"),
                body.get("content"),
                body.get("priority"),
                body.get("status"));
        return R.ok(id);
    }

    @Operation(summary = "[supervisor] 编辑公告")
    @SaCheckRole("supervisor")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        announcementService.update(
                id,
                body.get("title"),
                body.get("content"),
                body.get("priority"),
                body.get("status"));
        return R.ok();
    }

    @Operation(summary = "[supervisor] 删除公告")
    @SaCheckRole("supervisor")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return R.ok();
    }
}