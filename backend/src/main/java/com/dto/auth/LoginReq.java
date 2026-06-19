package com.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "登录请求体")
public class LoginReq {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过 64")
    @Schema(description = "登录账号（学号/工号/企业账号）", example = "zhangsan")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 4, max = 64, message = "密码长度 4~64")
    @Schema(description = "明文密码", example = "123456")
    private String password;
}
