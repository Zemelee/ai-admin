package com.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.common.BizException;
import com.dto.score.ScoreItemVO;
import com.entity.Company;
import com.entity.CompanyEval;
import com.entity.InternLog;
import com.entity.InternReport;
import com.entity.InternWeekly;
import com.entity.Student;
import com.entity.SysUser;
import com.entity.Teacher;
import com.mapper.CompanyEvalMapper;
import com.mapper.CompanyMapper;
import com.mapper.InternLogMapper;
import com.mapper.InternReportMapper;
import com.mapper.InternWeeklyMapper;
import com.mapper.StudentMapper;
import com.mapper.SysUserMapper;
import com.mapper.TeacherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 实习成绩汇总服务。
 * 四维加权（百分制）：
 * - 日志 25% = 提交率 × 25（已提交数 / 应提交天数）
 * - 周记 25% = 已评分周记均分 / 5 × 25
 * - 报告 20% = 已评分报告均分 / 5 × 20
 * - 企业鉴定 30% = 综合评价 / 5 × 30
 */
@Service
@RequiredArgsConstructor
public class ScoreService {

    private static final BigDecimal WEIGHT_LOG = new BigDecimal("25");
    private static final BigDecimal WEIGHT_WEEKLY = new BigDecimal("25");
    private static final BigDecimal WEIGHT_REPORT = new BigDecimal("20");
    private static final BigDecimal WEIGHT_EVAL = new BigDecimal("30");
    private static final BigDecimal FIVE = new BigDecimal("5");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal NINETY = new BigDecimal("90");
    private static final BigDecimal EIGHTY = new BigDecimal("80");
    private static final BigDecimal SEVENTY = new BigDecimal("70");
    private static final BigDecimal SIXTY = new BigDecimal("60");

    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final SysUserMapper sysUserMapper;
    private final CompanyMapper companyMapper;
    private final InternLogMapper internLogMapper;
    private final InternWeeklyMapper internWeeklyMapper;
    private final InternReportMapper internReportMapper;
    private final CompanyEvalMapper companyEvalMapper;

    /** 教师：分管学生成绩汇总 */
    public List<ScoreItemVO> teacherScores(Long userId) {
        Set<Long> studentIds = teacherStudentIds(userId);
        return buildScores(studentIds);
    }

    /** supervisor：全部学生成绩汇总 */
    public List<ScoreItemVO> allScores() {
        Set<Long> studentIds = studentMapper.selectList(new LambdaQueryWrapper<Student>())
                .stream().map(Student::getId).collect(Collectors.toSet());
        return buildScores(studentIds);
    }

    // ========== 内部：学生查自己成绩 ==========
    public ScoreItemVO myScore(Long userId) {
        Student s = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getUserId, userId).last("LIMIT 1"));
        if (s == null) return null;
        List<ScoreItemVO> list = buildScores(Set.of(s.getId()));
        return list.isEmpty() ? null : list.get(0);
    }

    // ========== 聚合核心 ==========
    private List<ScoreItemVO> buildScores(Set<Long> studentIds) {
        if (studentIds.isEmpty()) return new ArrayList<>();

        // 学生基础信息
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        List<Long> userIds = students.stream().map(Student::getUserId).filter(Objects::nonNull).distinct().toList();
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        // 企业信息
        List<Long> companyIds = students.stream().map(Student::getCompanyId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Company> companyMap = companyIds.isEmpty() ? Map.of()
                : companyMapper.selectBatchIds(companyIds).stream().collect(Collectors.toMap(Company::getId, c -> c));

        // 批量聚合
        List<InternLog> logs = internLogMapper.selectList(new LambdaQueryWrapper<InternLog>().in(InternLog::getStudentId, studentIds));
        List<InternWeekly> weeklies = internWeeklyMapper.selectList(new LambdaQueryWrapper<InternWeekly>().in(InternWeekly::getStudentId, studentIds));
        List<InternReport> reports = internReportMapper.selectList(new LambdaQueryWrapper<InternReport>().in(InternReport::getStudentId, studentIds));
        List<CompanyEval> evals = companyEvalMapper.selectList(new LambdaQueryWrapper<CompanyEval>().in(CompanyEval::getStudentId, studentIds));

        Map<Long, List<InternLog>> logMap = logs.stream().collect(Collectors.groupingBy(InternLog::getStudentId));
        Map<Long, List<InternWeekly>> weeklyMap = weeklies.stream().collect(Collectors.groupingBy(InternWeekly::getStudentId));
        Map<Long, List<InternReport>> reportMap = reports.stream().collect(Collectors.groupingBy(InternReport::getStudentId));
        Map<Long, CompanyEval> evalMap = evals.stream().collect(Collectors.toMap(CompanyEval::getStudentId, e -> e, (a, b) -> a));

        List<ScoreItemVO> result = new ArrayList<>(students.size());
        for (Student s : students) {
            ScoreItemVO vo = new ScoreItemVO();
            vo.setStudentId(s.getId());
            vo.setStudentNo(s.getStudentNo());
            vo.setClassName(s.getClassName());
            vo.setMajor(s.getMajor());
            vo.setInternStatus(s.getInternStatus());
            SysUser u = userMap.get(s.getUserId());
            vo.setStudentName(u == null ? null : u.getRealName());
            Company c = companyMap.get(s.getCompanyId());
            vo.setCompanyName(c == null ? null : c.getName());

            // 1. 日志：提交率×25
            List<InternLog> myLogs = logMap.getOrDefault(s.getId(), List.of());
            vo.setLogSubmitted(myLogs.size());
            int expected = expectedLogDays(s.getInternStart(), s.getInternEnd());
            vo.setLogExpected(expected);
            if (expected > 0 && !myLogs.isEmpty()) {
                BigDecimal rate = new BigDecimal(myLogs.size()).divide(new BigDecimal(expected), 4, RoundingMode.HALF_UP);
                vo.setLogRate(rate);
                vo.setLogScore(rate.multiply(WEIGHT_LOG).setScale(2, RoundingMode.HALF_UP));
            } else {
                vo.setLogRate(BigDecimal.ZERO);
                vo.setLogScore(BigDecimal.ZERO);
            }

            // 2. 周记：均分/5×25（仅统计 REVIEWED 的）
            List<InternWeekly> myWeeklies = weeklyMap.getOrDefault(s.getId(), List.of())
                    .stream().filter(w -> "REVIEWED".equals(w.getStatus())).toList();
            if (!myWeeklies.isEmpty()) {
                BigDecimal sum = myWeeklies.stream()
                        .map(w -> w.getTeacherScore() == null ? BigDecimal.ZERO : new BigDecimal(w.getTeacherScore()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal avg = sum.divide(new BigDecimal(myWeeklies.size()), 2, RoundingMode.HALF_UP);
                vo.setWeeklyAvg(avg);
                vo.setWeeklyScore(avg.divide(FIVE, 4, RoundingMode.HALF_UP).multiply(WEIGHT_WEEKLY).setScale(2, RoundingMode.HALF_UP));
            } else {
                vo.setWeeklyAvg(BigDecimal.ZERO);
                vo.setWeeklyScore(BigDecimal.ZERO);
            }

            // 3. 报告：均分/5×20（仅统计 REVIEWED 的）
            List<InternReport> myReports = reportMap.getOrDefault(s.getId(), List.of())
                    .stream().filter(r -> "REVIEWED".equals(r.getStatus())).toList();
            if (!myReports.isEmpty()) {
                BigDecimal sum = myReports.stream()
                        .map(r -> r.getTeacherScore() == null ? BigDecimal.ZERO : new BigDecimal(r.getTeacherScore()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal avg = sum.divide(new BigDecimal(myReports.size()), 2, RoundingMode.HALF_UP);
                vo.setReportAvg(avg);
                vo.setReportScore(avg.divide(FIVE, 4, RoundingMode.HALF_UP).multiply(WEIGHT_REPORT).setScale(2, RoundingMode.HALF_UP));
            } else {
                vo.setReportAvg(BigDecimal.ZERO);
                vo.setReportScore(BigDecimal.ZERO);
            }

            // 4. 企业鉴定：综合分/5×30
            CompanyEval e = evalMap.get(s.getId());
            if (e != null && "SUBMITTED".equals(e.getStatus())) {
                vo.setEvalSubmitted(true);
                BigDecimal eval5 = e.getScoreOverall() == null ? BigDecimal.ZERO : new BigDecimal(e.getScoreOverall());
                vo.setEvalScore5(eval5);
                vo.setEvalScore(eval5.divide(FIVE, 4, RoundingMode.HALF_UP).multiply(WEIGHT_EVAL).setScale(2, RoundingMode.HALF_UP));
            } else {
                vo.setEvalSubmitted(false);
                vo.setEvalScore5(BigDecimal.ZERO);
                vo.setEvalScore(BigDecimal.ZERO);
            }

            // 5. 综合成绩与等级
            BigDecimal total = vo.getLogScore()
                    .add(vo.getWeeklyScore())
                    .add(vo.getReportScore())
                    .add(vo.getEvalScore());
            vo.setTotalScore(total.setScale(2, RoundingMode.HALF_UP));
            vo.setGrade(gradeOf(total));

            result.add(vo);
        }

        // 按总分降序排列
        result.sort((a, b) -> b.getTotalScore().compareTo(a.getTotalScore()));
        return result;
    }

    /** 应提交日志天数：实习开始至结束（含）之间的工作日（简化为自然日），每周按 5 天算，MVP直接用自然日） */
    private int expectedLogDays(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            // 若无起止日期，默认按实习开始至今
            if (start == null) return 0;
            end = LocalDate.now();
        }
        if (end.isBefore(start)) return 0;
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        return (int) days;
    }

    private String gradeOf(BigDecimal total) {
        // 任意维度为 0 或数据缺失（如未评价/未报告），标记无法评定
        if (total.compareTo(BigDecimal.ZERO) <= 0) return "无法评定";
        if (total.compareTo(NINETY) >= 0) return "优秀";
        if (total.compareTo(EIGHTY) >= 0) return "良好";
        if (total.compareTo(SEVENTY) >= 0) return "中等";
        if (total.compareTo(SIXTY) >= 0) return "及格";
        return "不及格";
    }

    private Set<Long> teacherStudentIds(Long userId) {
        Teacher t = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getUserId, userId).last("LIMIT 1"));
        if (t == null) throw BizException.validate("未找到教师档案");
        return studentMapper.selectList(new LambdaQueryWrapper<Student>().eq(Student::getTeacherId, t.getId()))
                .stream().map(Student::getId).collect(Collectors.toSet());
    }
}
