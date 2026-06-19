package com.common;

import lombok.Getter;

import java.util.Arrays;

/**
 * 系统角色枚举
 */
@Getter
public enum RoleEnum {

    /** 监管者（系部管理领导） */
    SUPERVISOR("supervisor", "监管者"),
    /** 实习指导教师 */
    TEACHER("teacher", "实习指导教师"),
    /** 实习学生 */
    STUDENT("student", "实习学生"),
    /** 企业指导人员 */
    MENTOR("mentor", "企业指导人员");

    private final String code;
    private final String label;

    RoleEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static RoleEnum of(String code) {
        return Arrays.stream(values())
                .filter(r -> r.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new BizException("无效的角色类型: " + code));
    }
}
