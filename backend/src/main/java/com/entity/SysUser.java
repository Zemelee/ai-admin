package com.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统用户表 sys_user
 *
 * 一套 sa-token + role 字段区分四类角色：supervisor / teacher / student / mentor
 * 与 student / teacher / mentor / company 等档案表通过 user_id 一对一关联（在那些表中用 user_id 外键引用本表 id）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /** 登录账号（学号 / 工号 / 企业账号），全局唯一 */
    private String username;

    /** BCrypt 加密后的密码 */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 角色 code，对应 RoleEnum.code（小写） */
    private String role;

    /** 手机号（可选） */
    private String phone;

    /** 头像 URL（可选） */
    private String avatar;

    /** 1=启用 0=停用 */
    private Integer status;

    /** 最近登录时间 */
    private LocalDateTime lastLoginTime;
}
