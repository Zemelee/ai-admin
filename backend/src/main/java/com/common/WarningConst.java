package com.common;

/**
 * 三色预警相关常量
 */
public final class WarningConst {
    private WarningConst() {}

    /** 预警级别 */
    public static final String LEVEL_RED = "RED";
    public static final String LEVEL_YELLOW = "YELLOW";
    public static final String LEVEL_GREEN = "GREEN";

    /** 预警状态 */
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_REVIEWED = "REVIEWED";
    public static final String STATUS_IGNORED = "IGNORED";

    /** 规则码 */
    public static final String RULE_NO_LOG_3D = "NO_LOG_3D";
    public static final String RULE_SENSITIVE_WORD = "SENSITIVE_WORD";
    public static final String RULE_COMPANY_BLACKLIST = "COMPANY_BLACKLIST";
    public static final String RULE_TRANSFER_PENDING_3D = "TRANSFER_PENDING_3D";
    public static final String RULE_LEAVE_OVER_3M = "LEAVE_OVER_3M";

    /** 缺交日志阈值（天） */
    public static final int NO_LOG_DAYS = 3;
    /** 单位变更停滞阈值（天） */
    public static final int TRANSFER_PENDING_DAYS = 3;
    /** 长期请假阈值（天） */
    public static final int LEAVE_LONG_DAYS = 90;
}
