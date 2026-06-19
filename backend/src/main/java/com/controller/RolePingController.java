package com.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.common.CurrentUserUtil;
import com.common.R;
import com.common.RoleEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 角色访问烟雾测试接口（开发期）
 *
 * 用途：
 *   - 验证 Sa-Token 登录拦截 + StpInterfaceImpl + @SaCheckRole 三件套是否正确生效
 *   - 后续阶段：每个 ping 演化为该角色的真实业务 controller（leave / log / approval 等）
 *
 * 期望行为：
 *   未登录 -> 401 NotLoginException
 *   登录但角色不符 -> 403 NotRoleException
 *   登录且角色匹配 -> 200 R.ok({...})
 */
@Tag(name = "99.角色烟雾测试", description = "开发期验证 @SaCheckRole 是否生效")
@RestController
@RequestMapping("/ping")
public class RolePingController {

    @Operation(summary = "supervisor 通道")
    @SaCheckRole("supervisor")
    @GetMapping("/supervisor")
    public R<Map<String, Object>> supervisor() {
        return R.ok(payload(RoleEnum.SUPERVISOR));
    }

    @Operation(summary = "teacher 通道")
    @SaCheckRole("teacher")
    @GetMapping("/teacher")
    public R<Map<String, Object>> teacher() {
        return R.ok(payload(RoleEnum.TEACHER));
    }

    @Operation(summary = "student 通道")
    @SaCheckRole("student")
    @GetMapping("/student")
    public R<Map<String, Object>> student() {
        return R.ok(payload(RoleEnum.STUDENT));
    }

    @Operation(summary = "mentor 通道")
    @SaCheckRole("mentor")
    @GetMapping("/mentor")
    public R<Map<String, Object>> mentor() {
        return R.ok(payload(RoleEnum.MENTOR));
    }

    private Map<String, Object> payload(RoleEnum expect) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("expectRole", expect.getCode());
        m.put("expectLabel", expect.getLabel());
        m.put("currentUserId", CurrentUserUtil.userId());
        m.put("currentRole", CurrentUserUtil.roleCode());
        return m;
    }
}
