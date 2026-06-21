package com.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.common.BizException;
import com.common.CurrentUserUtil;
import com.common.RoleEnum;
import com.dto.auth.LoginReq;
import com.dto.auth.LoginResp;
import com.dto.auth.UserInfoResp;
import com.entity.SysUser;
import com.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 认证服务：登录 / 登出 / 当前用户信息
 *
 * 设计：
 *   1) 一套 StpUtil + session.role 区分四类角色，避免维护 4 套 StpLogic 实例
 *   2) 登录时把 role 写入 session，便于 CurrentUserUtil 读取与 @SaCheckRole 校验
 *   3) 后续对 student / teacher / mentor 等档案表的解析放到 ProfileService（阶段 1.3）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String SESSION_KEY_ROLE = "role";

    private final SysUserMapper sysUserMapper;
    // TODO: 上线前接入 BCrypt 校验（PasswordEncoderConfig 已就绪），当前 MVP 使用明文比对方便联调

    /**
     * 登录
     */
    /**
     * 登录方法
     * @param req 登录请求参数（包含用户名和密码）
     * @return 登录响应（包含token和用户信息）
     */
    public LoginResp login(LoginReq req) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        // lambda user -> user.getUsername()
                        .eq(SysUser::getUsername, req.getUsername())
                        .last("LIMIT 1")
        );
        if (user == null) {
            throw BizException.validate("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BizException.forbidden("账号已停用，请联系管理员");
        }
        // MVP 期：数据库 password 字段直接存明文 123456，上线前替换为 BCrypt
        if (!req.getPassword().equals(user.getPassword())) {
            throw BizException.validate("用户名或密码错误");
        }

        // 校验 role 合法性（数据存量保护）
        RoleEnum role = RoleEnum.of(user.getRole());

        StpUtil.login(user.getId());
        StpUtil.getSession().set(SESSION_KEY_ROLE, role.getCode());

        log.info("用户登录成功: userId={} username={} role={}", user.getId(), user.getUsername(), role.getCode());

        return LoginResp.builder()
                .token(StpUtil.getTokenValue())
                .tokenName(StpUtil.getTokenName())
                .userId(user.getId())
                .username(user.getUsername())
                .name(user.getRealName())
                .role(role.getCode())
                .roleLabel(role.getLabel())
                .build();
    }

    /**
     * 登出（幂等：未登录不报错）
     */
    public void logout() {
        if (StpUtil.isLogin()) {
            log.info("用户登出: userId={}", CurrentUserUtil.userId());
            StpUtil.logout();
        }
    }

    /**
     * 当前登录用户信息
     */
    public UserInfoResp current() {
        Long userId = CurrentUserUtil.userId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            // 用户被删除但 token 还在 → 强制登出
            StpUtil.logout();
            throw BizException.forbidden("账号不存在，请重新登录");
        }
        RoleEnum role = RoleEnum.of(user.getRole());
        return UserInfoResp.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .name(user.getRealName())
                .role(role.getCode())
                .roleLabel(role.getLabel())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .build();
    }
}
