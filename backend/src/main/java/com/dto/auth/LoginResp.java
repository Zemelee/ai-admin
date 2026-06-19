package com.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "登录响应")
public class LoginResp {

    @Schema(description = "Sa-Token 的 token 值，前端需放在请求头 satoken 中传递")
    private String token;

    @Schema(description = "Sa-Token 的请求头名称，固定为 satoken")
    private String tokenName;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "显示姓名")
    private String name;

    @Schema(description = "角色 code：supervisor/teacher/student/mentor")
    private String role;

    @Schema(description = "角色中文名")
    private String roleLabel;
}
