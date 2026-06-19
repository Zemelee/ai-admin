package com.common;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 当前登录用户工具类
 *
 * 约定：
 *   loginId = sys_user.id
 *   StpUtil.getSession().get("role") = RoleEnum.code
 */
public final class CurrentUserUtil {

    private CurrentUserUtil() {}

    private static final String SESSION_KEY_ROLE = "role";

    /** 当前用户 id（未登录抛 NotLoginException 由 GlobalExceptionHandler 统一处理） */
    public static Long userId() {
        return Long.valueOf(StpUtil.getLoginIdAsString());
    }

    /** 当前用户角色 code，可能为 null */
    public static String roleCode() {
        Object v = StpUtil.getSession().get(SESSION_KEY_ROLE);
        return v == null ? null : v.toString();
    }

    public static RoleEnum role() {
        String c = roleCode();
        if (StrUtil.isBlank(c)) {
            throw BizException.forbidden("会话角色丢失");
        }
        return RoleEnum.of(c);
    }

    public static boolean isRole(RoleEnum target) {
        return target.getCode().equals(roleCode());
    }
}
