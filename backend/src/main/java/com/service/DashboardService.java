package com.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dto.score.ScoreItemVO;
import com.dto.warning.WarningStatsVO;
import com.entity.CompanyEval;
import com.entity.Warning;
import com.mapper.CompanyEvalMapper;
import com.mapper.WarningMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 仪表盘数据聚合服务。
 * 汇总成绩分布、专业/企业平均分、鉴定维度、预警统计等，供 supervisor 总览页使用。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ScoreService scoreService;
    private final WarningService warningService;
    private final CompanyEvalMapper companyEvalMapper;
    private final WarningMapper warningMapper;

    /** 聚合全部仪表盘数据 */
    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 1. 复用成绩汇总
        List<ScoreItemVO> all = scoreService.allScores();
        data.put("scoreCount", all.size());

        // 2. 等级分布
        Map<String, Long> gradeDist = all.stream()
                .filter(s -> s.getGrade() != null)
                .collect(Collectors.groupingBy(ScoreItemVO::getGrade, Collectors.counting()));
        data.put("gradeDistribution", gradeDist);

        // 3. 分数统计（平均/最高/最低）
        List<BigDecimal> scores = all.stream()
                .map(ScoreItemVO::getTotalScore)
                .filter(s -> s.compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        if (!scores.isEmpty()) {
            BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avg = sum.divide(new BigDecimal(scores.size()), 2, RoundingMode.HALF_UP);
            BigDecimal max = scores.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal min = scores.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("avg", avg);
            stats.put("max", max);
            stats.put("min", min);
            stats.put("count", scores.size());
            data.put("scoreStats", stats);
        } else {
            data.put("scoreStats", Map.of("avg", 0, "max", 0, "min", 0, "count", 0));
        }

        // 4. 各专业平均分（TOP 10）
        List<Map<String, Object>> majorAvg = all.stream()
                .filter(s -> s.getMajor() != null && !s.getMajor().isEmpty()
                        && s.getTotalScore().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.groupingBy(ScoreItemVO::getMajor))
                .entrySet().stream()
                .map(e -> {
                    List<ScoreItemVO> list = e.getValue();
                    BigDecimal total = list.stream()
                            .map(ScoreItemVO::getTotalScore)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avg = total.divide(new BigDecimal(list.size()), 2, RoundingMode.HALF_UP);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("major", e.getKey());
                    m.put("avgScore", avg);
                    m.put("count", list.size());
                    return m;
                })
                .sorted((a, b) -> ((BigDecimal) b.get("avgScore")).compareTo((BigDecimal) a.get("avgScore")))
                .limit(10)
                .collect(Collectors.toList());
        data.put("majorAvg", majorAvg);

        // 5. 各企业平均分（TOP 10）
        List<Map<String, Object>> companyAvg = all.stream()
                .filter(s -> s.getCompanyName() != null && !s.getCompanyName().isEmpty()
                        && s.getTotalScore().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.groupingBy(ScoreItemVO::getCompanyName))
                .entrySet().stream()
                .map(e -> {
                    List<ScoreItemVO> list = e.getValue();
                    BigDecimal total = list.stream()
                            .map(ScoreItemVO::getTotalScore)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avg = total.divide(new BigDecimal(list.size()), 2, RoundingMode.HALF_UP);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("companyName", e.getKey());
                    m.put("avgScore", avg);
                    m.put("count", list.size());
                    return m;
                })
                .sorted((a, b) -> ((BigDecimal) b.get("avgScore")).compareTo((BigDecimal) a.get("avgScore")))
                .limit(10)
                .collect(Collectors.toList());
        data.put("companyAvg", companyAvg);

        // 6. 企业鉴定各维度平均分（1-5分）
        List<CompanyEval> submittedEvals = companyEvalMapper.selectList(
                new LambdaQueryWrapper<CompanyEval>().eq(CompanyEval::getStatus, "SUBMITTED"));
        if (!submittedEvals.isEmpty()) {
            int n = submittedEvals.size();
            double attSum = submittedEvals.stream().mapToInt(e -> e.getScoreAttendance() == null ? 0 : e.getScoreAttendance()).sum();
            double abilSum = submittedEvals.stream().mapToInt(e -> e.getScoreAbility() == null ? 0 : e.getScoreAbility()).sum();
            double attiSum = submittedEvals.stream().mapToInt(e -> e.getScoreAttitude() == null ? 0 : e.getScoreAttitude()).sum();
            double overSum = submittedEvals.stream().mapToInt(e -> e.getScoreOverall() == null ? 0 : e.getScoreOverall()).sum();
            Map<String, Object> dim = new LinkedHashMap<>();
            dim.put("attendance", round1(attSum / n));
            dim.put("ability", round1(abilSum / n));
            dim.put("attitude", round1(attiSum / n));
            dim.put("overall", round1(overSum / n));
            dim.put("count", n);
            data.put("evalDimensionAvg", dim);
        } else {
            data.put("evalDimensionAvg", Map.of("attendance", 0, "ability", 0, "attitude", 0, "overall", 0, "count", 0));
        }

        // 7. 预警统计（复用 WarningService）
        WarningStatsVO ws = warningService.stats();
        data.put("warningStats", ws);

        // 8. 近期预警趋势（最近 12 周，按周聚合）
        LocalDateTime since = LocalDateTime.now().minusWeeks(12);
        List<Warning> recentWarnings = warningMapper.selectList(
                new LambdaQueryWrapper<Warning>()
                        .ge(Warning::getCreateTime, since)
                        .orderByAsc(Warning::getCreateTime));
        // 按周聚合
        Map<String, Long> weeklyMap = new LinkedHashMap<>();
        for (Warning w : recentWarnings) {
            // 按 ISO 周聚合
            String isoWeek = String.format("%s-W%02d",
                    w.getCreateTime().getYear(),
                    w.getCreateTime().toLocalDate().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            weeklyMap.merge(isoWeek, 1L, Long::sum);
        }
        List<Map<String, Object>> trend = weeklyMap.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("week", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        data.put("warningTrend", trend);

        return data;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}