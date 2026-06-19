package com.controller;

import com.common.R;
import com.dto.auth.LoginReq;
import com.dto.auth.LoginResp;
import com.dto.auth.UserInfoResp;
import com.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 *
 * 路由统一前缀 /auth，登录/登出已在 SaTokenConfig 放行；
 * /auth/me 需要登录态。
 */
@Tag(name = "01.认证", description = "登录、登出、当前用户信息")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<LoginResp> login(@Valid @RequestBody LoginReq req) {
        return R.ok(authService.login(req));
    }

    @Operation(summary = "登出（幂等）")
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    @Operation(summary = "当前登录用户信息")
    @GetMapping("/me")
    public R<UserInfoResp> me() {
        return R.ok(authService.current());
    }
}
