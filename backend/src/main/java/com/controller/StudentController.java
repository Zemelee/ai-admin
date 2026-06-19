package com.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.common.BizException;
import com.common.CurrentUserUtil;
import com.common.R;
import com.entity.Company;
import com.entity.Mentor;
import com.entity.Student;
import com.entity.SysUser;
import com.entity.Teacher;
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
import java.util.Map;

/**
 * 学生端 demo 接口（MVP 阶段最小集，仅查询自己的档案）
 */
@Tag(name = "10.学生端", description = "学生角色专属接口")
@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
@SaCheckRole("student")
public class StudentController {

    private final StudentMapper studentMapper;
    private final SysUserMapper sysUserMapper;
    private final TeacherMapper teacherMapper;
    private final CompanyMapper companyMapper;
    private final MentorMapper mentorMapper;

    @Operation(summary = "我的档案（学生本人）")
    @GetMapping("/my-info")
    public R<Map<String, Object>> myInfo() {
        Long userId = CurrentUserUtil.userId();
        SysUser user = sysUserMapper.selectById(userId);
        Student s = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getUserId, userId).last("LIMIT 1"));
        if (s == null) {
            throw BizException.validate("未找到学生档案，请联系管理员补全");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId);
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("phone", user.getPhone());
        data.put("studentNo", s.getStudentNo());
        data.put("className", s.getClassName());
        data.put("major", s.getMajor());
        data.put("grade", s.getGrade());
        data.put("gender", s.getGender());
        data.put("internStart", s.getInternStart());
        data.put("internEnd", s.getInternEnd());
        data.put("internStatus", s.getInternStatus());

        // 关联：指导教师
        if (s.getTeacherId() != null) {
            Teacher t = teacherMapper.selectById(s.getTeacherId());
            if (t != null) {
                SysUser tu = sysUserMapper.selectById(t.getUserId());
                Map<String, Object> tm = new LinkedHashMap<>();
                tm.put("teacherId", t.getId());
                tm.put("teacherNo", t.getTeacherNo());
                tm.put("realName", tu == null ? null : tu.getRealName());
                tm.put("department", t.getDepartment());
                tm.put("title", t.getTitle());
                tm.put("phone", tu == null ? null : tu.getPhone());
                data.put("teacher", tm);
            }
        }

        // 关联：实习企业
        if (s.getCompanyId() != null) {
            Company c = companyMapper.selectById(s.getCompanyId());
            if (c != null) {
                Map<String, Object> cm = new LinkedHashMap<>();
                cm.put("companyId", c.getId());
                cm.put("name", c.getName());
                cm.put("address", c.getAddress());
                cm.put("industry", c.getIndustry());
                cm.put("contactPerson", c.getContactPerson());
                cm.put("contactPhone", c.getContactPhone());
                cm.put("isBlacklist", c.getIsBlacklist());
                data.put("company", cm);
            }
        }

        // 关联：企业指导
        if (s.getMentorId() != null) {
            Mentor m = mentorMapper.selectById(s.getMentorId());
            if (m != null) {
                SysUser mu = sysUserMapper.selectById(m.getUserId());
                Map<String, Object> mm = new LinkedHashMap<>();
                mm.put("mentorId", m.getId());
                mm.put("realName", mu == null ? null : mu.getRealName());
                mm.put("position", m.getPosition());
                mm.put("dept", m.getDept());
                mm.put("phone", mu == null ? null : mu.getPhone());
                data.put("mentor", mm);
            }
        }
        return R.ok(data);
    }
}
