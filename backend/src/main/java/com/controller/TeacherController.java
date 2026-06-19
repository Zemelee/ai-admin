package com.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.common.BizException;
import com.common.CurrentUserUtil;
import com.common.R;
import com.entity.Company;
import com.entity.Student;
import com.entity.SysUser;
import com.entity.Teacher;
import com.mapper.CompanyMapper;
import com.mapper.StudentMapper;
import com.mapper.SysUserMapper;
import com.mapper.TeacherMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
}
