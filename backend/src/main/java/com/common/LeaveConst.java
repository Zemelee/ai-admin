package com.common;

/**
 * 请假/审批流相关常量
 */
public final class LeaveConst {

    private LeaveConst() {}

    // 请假类型
    public static final String TYPE_SICK = "SICK";
    public static final String TYPE_PERSONAL = "PERSONAL";
    public static final String TYPE_OTHER = "OTHER";

    // 单据状态
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // 审批节点
    public static final String NODE_MENTOR = "MENTOR";
    public static final String NODE_TEACHER = "TEACHER";
    public static final String NODE_SUPERVISOR = "SUPERVISOR";

    // 审批结果
    public static final String RESULT_APPROVED = "APPROVED";
    public static final String RESULT_REJECTED = "REJECTED";

    // 业务类型
    public static final String BIZ_LEAVE = "LEAVE";
    public static final String BIZ_TRANSFER = "TRANSFER";

    /** 监管者复核阈值（>30 天进入监管者节点） */
    public static final int SUPERVISOR_THRESHOLD_DAYS = 30;
}
