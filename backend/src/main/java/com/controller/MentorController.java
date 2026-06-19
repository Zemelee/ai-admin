package com.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.common.BizException;
import com.common.CurrentUserUtil;
import com.common.R;
import com.entity.Mentor;
import com.entity.Student;
import com.entity.SysUser;
import com.mapper.MentorMapper;
import com.mapper.StudentMapper;
import com.mapper.SysUserMapper;
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
 * 企业指导（mentor）端 demo 接口
 */
@Tag(name = "30.企业指导端", description = "mentor 角色专属接口")
@RestController
@RequestMapping("/mentor")
@RequiredArgsConstructor
@SaCheckRole("mentor")
public class MentorController {

    private final MentorMapper mentorMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper sysUserMapper;

    @Operation(summary = "我负责的学生列表")
    @GetMapping("/my-students")
    public R<List<Map<String, Object>>> myStudents() {
        Long userId = CurrentUserUtil.userId();
        Mentor mentor = mentorMapper.selectOne(new LambdaQueryWrapper<Mentor>()
                .eq(Mentor::getUserId, userId).last("LIMIT 1"));
        if (mentor == null) {
            throw BizException.validate("未找到企业指导档案，请联系管理员补全");
        }
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .eq(Student::getMentorId, mentor.getId())
                .orderByAsc(Student::getStudentNo));

        if (students.isEmpty()) {
            return R.ok(new ArrayList<>());
        }

        List<Long> userIds = students.stream().map(Student::getUserId).collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? new HashMap<>()
                : sysUserMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

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
            list.add(row);
        }
        return R.ok(list);
    }
}
