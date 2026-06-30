package com.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.common.CurrentUserUtil;
import com.dto.export.ScoreExportRow;
import com.dto.export.StudentExportRow;
import com.dto.export.WarningExportRow;
import com.dto.score.ScoreItemVO;
import com.entity.Student;
import com.entity.SysUser;
import com.entity.Warning;
import com.mapper.StudentMapper;
import com.mapper.SysUserMapper;
import com.mapper.WarningMapper;
import com.service.AdminStudentService;
import com.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Tag(name = "50.数据导出", description = "Excel 导出：成绩 / 学生 / 预警")
@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final ScoreService scoreService;
    private final AdminStudentService adminStudentService;
    private final WarningMapper warningMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper sysUserMapper;

    @Operation(summary = "导出成绩汇总（supervisor 全部 / teacher 分管）")
    @GetMapping("/scores")
    @SaCheckRole({"supervisor", "teacher"})
    public void exportScores(HttpServletResponse response) throws IOException {
        String role = CurrentUserUtil.roleCode();
        List<ScoreItemVO> scores;
        if ("teacher".equals(role)) {
            scores = scoreService.teacherScores(CurrentUserUtil.userId());
        } else {
            scores = scoreService.allScores();
        }

        List<ScoreExportRow> rows = new ArrayList<>(scores.size());
        int rank = 1;
        for (ScoreItemVO s : scores) {
            rows.add(new ScoreExportRow(
                    rank++, s.getStudentNo(), s.getStudentName(),
                    s.getClassName(), s.getMajor(), s.getCompanyName(),
                    s.getInternStatus(), s.getLogScore(), s.getWeeklyScore(),
                    s.getReportScore(), s.getEvalScore(),
                    s.getTotalScore(), s.getGrade()
            ));
        }

        writeExcel(response, "实习成绩汇总", rows);
    }

    @Operation(summary = "导出学生档案（supervisor only）")
    @GetMapping("/students")
    @SaCheckRole("supervisor")
    public void exportStudents(HttpServletResponse response) throws IOException {
        var page = adminStudentService.page(null, null, null, 1, 99999);
        List<StudentExportRow> rows = page.getRecords().stream()
                .map(v -> new StudentExportRow(
                        v.getStudentNo(), v.getRealName(), v.getUsername(),
                        v.getClassName(), v.getMajor(), v.getGrade(),
                        v.getPhone(), v.getTeacherName(), v.getCompanyName(),
                        v.getMentorName(), v.getInternStatus(),
                        Integer.valueOf(1).equals(v.getStatus()) ? "启用" : "停用"
                ))
                .collect(Collectors.toList());

        writeExcel(response, "学生档案", rows);
    }

    @Operation(summary = "导出预警列表（supervisor only）")
    @GetMapping("/warnings")
    @SaCheckRole("supervisor")
    public void exportWarnings(HttpServletResponse response) throws IOException {
        List<Warning> warnings = warningMapper.selectList(
                new LambdaQueryWrapper<Warning>().orderByDesc(Warning::getLevel).orderByDesc(Warning::getCreateTime));

        List<Long> studentIds = warnings.stream().map(Warning::getStudentId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Student> studentMap = studentIds.isEmpty() ? Map.of()
                : studentMapper.selectBatchIds(studentIds).stream().collect(Collectors.toMap(Student::getId, s -> s));
        List<Long> userIds = studentMap.values().stream().map(Student::getUserId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        List<WarningExportRow> rows = new ArrayList<>(warnings.size());
        for (Warning w : warnings) {
            Student s = studentMap.get(w.getStudentId());
            SysUser u = s == null ? null : userMap.get(s.getUserId());
            rows.add(new WarningExportRow(
                    "RED".equals(w.getLevel()) ? "红色" : "黄色",
                    w.getRuleDesc(),
                    w.getStudentName() != null ? w.getStudentName() : (u == null ? null : u.getRealName()),
                    s == null ? null : s.getStudentNo(),
                    s == null ? null : s.getClassName(),
                    s == null ? null : s.getMajor(),
                    w.getDetail(),
                    "PENDING".equals(w.getStatus()) ? "待处理" : "已处理",
                    w.getCreateTime()
            ));
        }

        writeExcel(response, "预警列表", rows);
    }

    private <T> void writeExcel(HttpServletResponse response, String fileName, List<T> data) throws IOException {
        if (data.isEmpty()) {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("暂无数据可导出");
            return;
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String encodedName = URLEncoder.encode(fileName + "_" + timestamp + ".xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
        EasyExcel.write(response.getOutputStream(), data.get(0).getClass()).sheet("Sheet1").doWrite(data);
    }
}