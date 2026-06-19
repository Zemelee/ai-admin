package com.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 角色/权限来源实现。
 *
 * 约定：
 *   登录时 AuthService 已经把 RoleEnum.code（小写：supervisor / teacher / student / mentor）
 *   写入了 StpUtil.getSession().set("role", code)
 *   这里直接读出来即可，从而支持 @SaCheckRole("supervisor") 等注解。
 *
 * MVP 期不实现细粒度 permission，统一返回空列表，权限粒度收敛到“角色”。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private static final String SESSION_KEY_ROLE = "role";

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roles = new ArrayList<>(1);
        Object v = StpUtil.getSessionByLoginId(loginId).get(SESSION_KEY_ROLE);
        if (v != null) {
            roles.add(v.toString());
        }
        return roles;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // MVP：不启用细粒度权限
        return new ArrayList<>();
    }
}
