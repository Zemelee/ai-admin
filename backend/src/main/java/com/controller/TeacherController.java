package com.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.common.BizException;
import com.common.CurrentUserUtil;
import com.common.R;
import com.entity.Company;
import com.entity.InternReport;
import com.entity.InternWeekly;
import com.entity.LeaveApply;
import com.entity.Student;
import com.entity.SysUser;
import com.entity.Teacher;
import com.entity.TransferApply;
import com.mapper.CompanyMapper;
import com.mapper.InternReportMapper;
import com.mapper.InternWeeklyMapper;
import com.mapper.LeaveApplyMapper;
import com.mapper.StudentMapper;
import com.mapper.SysUserMapper;
import com.mapper.TeacherMapper;
import com.mapper.TransferApplyMapper;
import com.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教师端 demo 接口
 */
@Tag(name = "20.教师端", description = "教师角色专属接口")
@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
@SaCheckRole("teacher")
public class TeacherController {

    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper sysUserMapper;
    private final CompanyMapper companyMapper;
    private final LeaveApplyMapper leaveApplyMapper;
    private final InternWeeklyMapper internWeeklyMapper;
    private final InternReportMapper internReportMapper;
    private final TransferApplyMapper transferApplyMapper;
    private final ScoreService scoreService;

    @Operation(summary = "我带的学生列表")
    @GetMapping("/my-students")
    public R<List<Map<String, Object>>> myStudents() {
        Long userId = CurrentUserUtil.userId();
        Teacher teacher = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getUserId, userId).last("LIMIT 1"));
        if (teacher == null) {
            throw BizException.validate("未找到教师档案，请联系管理员补全");
        }
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .eq(Student::getTeacherId, teacher.getId())
                .orderByAsc(Student::getStudentNo));

        if (students.isEmpty()) {
            return R.ok(new ArrayList<>());
        }

        // 批量加载关联数据
        List<Long> userIds = students.stream().map(Student::getUserId).collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? new HashMap<>()
                : sysUserMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        List<Long> companyIds = students.stream()
                .map(Student::getCompanyId).filter(java.util.Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Long, Company> companyMap = companyIds.isEmpty() ? new HashMap<>()
                : companyMapper.selectByIds(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, c -> c));

        List<Map<String, Object>> list = new ArrayList<>(students.size());
        for (Student s : students) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", s.getId());
            row.put("studentNo", s.getStudentNo());
            row.put("className", s.getClassName());
            row.put("major", s.getMajor());
            row.put("internStatus", s.getInternStatus());
            SysUser u = userMap.get(s.getUserId());
            row.put("realName", u == null ? null : u.getRealName());
            row.put("phone", u == null ? null : u.getPhone());
            Company c = s.getCompanyId() == null ? null : companyMap.get(s.getCompanyId());
            row.put("companyName", c == null ? null : c.getName());
            row.put("isBlacklistCompany", c != null && Integer.valueOf(1).equals(c.getIsBlacklist()));
            list.add(row);
        }
        return R.ok(list);
    }

    @Operation(summary = "教师工作台仪表盘（待办统计 + 成绩分布）")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard() {
        Long userId = CurrentUserUtil.userId();
        Teacher teacher = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getUserId, userId).last("LIMIT 1"));
        if (teacher == null) {
            throw BizException.validate("未找到教师档案");
        }

        Set<Long> studentIds = studentMapper.selectList(
                new LambdaQueryWrapper<Student>().eq(Student::getTeacherId, teacher.getId()))
                .stream().map(Student::getId).collect(Collectors.toSet());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("studentCount", studentIds.size());

        // 待办统计
        long pendingLeaves = studentIds.isEmpty() ? 0 : leaveApplyMapper.selectCount(
                new LambdaQueryWrapper<LeaveApply>()
                        .in(LeaveApply::getStudentId, studentIds)
                        .eq(LeaveApply::getStatus, "PENDING"));
        data.put("pendingLeaveCount", pendingLeaves);

        long pendingWeeklies = studentIds.isEmpty() ? 0 : internWeeklyMapper.selectCount(
                new LambdaQueryWrapper<InternWeekly>()
                        .in(InternWeekly::getStudentId, studentIds)
                        .eq(InternWeekly::getStatus, "SUBMITTED"));
        data.put("pendingWeeklyCount", pendingWeeklies);

        long pendingReports = studentIds.isEmpty() ? 0 : internReportMapper.selectCount(
                new LambdaQueryWrapper<InternReport>()
                        .in(InternReport::getStudentId, studentIds)
                        .eq(InternReport::getStatus, "SUBMITTED"));
        data.put("pendingReportCount", pendingReports);

        long pendingTransfers = studentIds.isEmpty() ? 0 : transferApplyMapper.selectCount(
                new LambdaQueryWrapper<TransferApply>()
                        .in(TransferApply::getStudentId, studentIds)
                        .eq(TransferApply::getStatus, "PENDING"));
        data.put("pendingTransferCount", pendingTransfers);

        // 成绩分布（复用 ScoreService）
        var scores = scoreService.teacherScores(userId);
        data.put("scoreCount", scores.size());

        // 等级分布
        Map<String, Long> gradeDist = scores.stream()
                .filter(s -> s.getGrade() != null)
                .collect(Collectors.groupingBy(com.dto.score.ScoreItemVO::getGrade, Collectors.counting()));
        data.put("gradeDistribution", gradeDist);

        // 分数统计
        List<BigDecimal> scoreVals = scores.stream()
                .map(com.dto.score.ScoreItemVO::getTotalScore)
                .filter(s -> s.compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        if (!scoreVals.isEmpty()) {
            BigDecimal sum = scoreVals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avg = sum.divide(new BigDecimal(scoreVals.size()), 2, RoundingMode.HALF_UP);
            BigDecimal max = scoreVals.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal min = scoreVals.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("avg", avg);
            stats.put("max", max);
            stats.put("min", min);
            stats.put("count", scoreVals.size());
            data.put("scoreStats", stats);
        } else {
            data.put("scoreStats", Map.of("avg", 0, "max", 0, "min", 0, "count", 0));
        }

        return R.ok(data);
    }
}
