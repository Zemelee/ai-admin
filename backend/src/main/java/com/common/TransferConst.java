package com.common;

/**
 * 单位变更申请相关常量
 * <p>
 * MVP 串行链路：MENTOR(原企业指导) → TEACHER(分管教师) → SUPERVISOR(系部) → APPROVED
 * （并行会签留待二期）
 */
public final class TransferConst {

    private TransferConst() {}

    public static final String BIZ_TRANSFER = "TRANSFER";

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

    /** 总实习期阈值（天）：≤ 180 天要求原单位满 1 个月；> 180 天要求满 3 个月 */
    public static final int TOTAL_HALF_YEAR_DAYS = 180;
    /** 原单位最低实习天数（总实习期 ≤ 半年） */
    public static final int MIN_AT_ORIGIN_HALF_YEAR = 30;
    /** 原单位最低实习天数（总实习期 > 半年） */
    public static final int MIN_AT_ORIGIN_OVER_HALF_YEAR = 90;
}
