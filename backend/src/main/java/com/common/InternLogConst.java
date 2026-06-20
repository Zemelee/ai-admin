package com.common;

/**
 * 实习日志相关常量
 */
public final class InternLogConst {

    private InternLogConst() {}

    /** 附件业务类型 */
    public static final String BIZ_LOG = "LOG";

    /** 日志状态：待企业指导确认 */
    public static final String STATUS_SUBMITTED = "SUBMITTED";
    /** 日志状态：企业指导已通过 */
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    /** 日志状态：企业指导已驳回 */
    public static final String STATUS_REJECTED = "REJECTED";

    /** mentor 确认结果 */
    public static final String RESULT_CONFIRMED = "CONFIRMED";
    public static final String RESULT_REJECTED = "REJECTED";
}
