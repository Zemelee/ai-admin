package com.dto.warning;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "预警统计聚合")
public class WarningStatsVO {
    /** 按级别聚合的待处理数：RED/YELLOW */
    private Map<String, Long> levelMap;
    /** 按规则码聚合的待处理数 */
    private Map<String, Long> ruleMap;
    /** 待处理总数 */
    private Long pendingTotal;
    /** 已处理总数（REVIEWED+IGNORED） */
    private Long handledTotal;
}
