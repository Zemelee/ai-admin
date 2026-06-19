package com.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.common.R;
import com.entity.Company;
import com.entity.Student;
import com.entity.SysUser;
import com.mapper.CompanyMapper;
import com.mapper.MentorMapper;
import com.mapper.StudentMapper;
import com.mapper.SysUserMapper;
import com.mapper.TeacherMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 监管者（系主任）端 demo 接口
 */
@Tag(name = "40.监管者端", description = "supervisor 角色专属接口")
@RestController
@RequestMapping("/supervisor")
@RequiredArgsConstructor
@SaCheckRole("supervisor")
public class SupervisorController {

    private final SysUserMapper sysUserMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final CompanyMapper companyMapper;
    private final MentorMapper mentorMapper;

    @Operation(summary = "全局总览（账号 / 学生 / 教师 / 企业 / mentor 数量与状态分布）")
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 账号统计
        Long userTotal = sysUserMapper.selectCount(null);
        Long userEnabled = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1));
        Map<String, Object> userBox = new LinkedHashMap<>();
        userBox.put("total", userTotal);
        userBox.put("enabled", userEnabled);
        data.put("user", userBox);

        // 学生：总数 + 4 种实习状态分布
        Long studentTotal = studentMapper.selectCount(null);
        Map<String, Long> studentStatus = new LinkedHashMap<>();
        for (String st : new String[]{"ACTIVE", "SUSPEND", "FINISHED", "QUIT"}) {
            Long cnt = studentMapper.selectCount(
                    new LambdaQueryWrapper<Student>().eq(Student::getInternStatus, st));
            studentStatus.put(st, cnt);
        }
        Map<String, Object> studentBox = new LinkedHashMap<>();
        studentBox.put("total", studentTotal);
        studentBox.put("statusMap", studentStatus);
        data.put("student", studentBox);

        // 教师 / 企业 / mentor 计数
        Long teacherTotal = teacherMapper.selectCount(null);
        Long companyTotal = companyMapper.selectCount(null);
        Long companyBlacklist = companyMapper.selectCount(
                new LambdaQueryWrapper<Company>().eq(Company::getIsBlacklist, 1));
        Long mentorTotal = mentorMapper.selectCount(null);

        data.put("teacher", Map.of("total", teacherTotal));
        data.put("company", Map.of("total", companyTotal, "blacklist", companyBlacklist));
        data.put("mentor", Map.of("total", mentorTotal));

        // Top5 黑名单企业
        List<Company> blacklistTop = companyMapper.selectList(
                new LambdaQueryWrapper<Company>()
                        .eq(Company::getIsBlacklist, 1)
                        .orderByDesc(Company::getCreateTime)
                        .last("LIMIT 5"));
        data.put("blacklistTop5", blacklistTop);

        // 最近 5 个学生（用于演示卡片）
        List<Student> recentStudents = studentMapper.selectList(
                new LambdaQueryWrapper<Student>()
                        .orderByDesc(Student::getCreateTime)
                        .last("LIMIT 5"));
        data.put("recentStudents", recentStudents);

        return R.ok(data);
    }
}
