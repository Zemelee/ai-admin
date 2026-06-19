package com.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "当前登录用户信息")
public class UserInfoResp {

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "显示姓名")
    private String name;

    @Schema(description = "角色 code")
    private String role;

    @Schema(description = "角色中文名")
    private String roleLabel;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像")
    private String avatar;
}
