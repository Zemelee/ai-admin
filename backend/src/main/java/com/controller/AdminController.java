package com.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.common.R;
import com.dto.admin.AdminCompanySaveReq;
import com.dto.admin.AdminCompanyVO;
import com.dto.admin.AdminMentorSaveReq;
import com.dto.admin.AdminMentorVO;
import com.dto.admin.AdminStudentSaveReq;
import com.dto.admin.AdminStudentVO;
import com.dto.admin.AdminTeacherSaveReq;
import com.dto.admin.AdminTeacherVO;
import com.dto.admin.StudentBindReq;
import com.entity.Company;
import com.entity.Mentor;
import com.entity.SysUser;
import com.entity.Teacher;
import com.service.AdminCompanyService;
import com.service.AdminMentorService;
import com.service.AdminStudentService;
import com.service.AdminTeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mapper.CompanyMapper;
import com.mapper.MentorMapper;
import com.mapper.SysUserMapper;
import com.mapper.TeacherMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 账号与档案管理（监管者 supervisor 维护）。
 * - /admin/student/**  学生档案 CRUD + 绑定
 * - /admin/company/**  企业档案 CRUD
 * - /admin/option/**   绑定下拉数据源
 */
@Tag(name = "40.账号档案管理", description = "supervisor 维护学生/企业档案与绑定关系")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@SaCheckRole("supervisor")
public class AdminController {

    private final AdminStudentService adminStudentService;
    private final AdminCompanyService adminCompanyService;
    private final AdminTeacherService adminTeacherService;
    private final AdminMentorService adminMentorService;
    private final TeacherMapper teacherMapper;
    private final MentorMapper mentorMapper;
    private final CompanyMapper companyMapper;
    private final SysUserMapper sysUserMapper;

    // ========== 学生档案 ==========

    @Operation(summary = "学生档案分页")
    @GetMapping("/student/page")
    public R<IPage<AdminStudentVO>> studentPage(
            @Parameter(description = "关键字（学号/姓名/账号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "实习状态") @RequestParam(required = false) String internStatus,
            @Parameter(description = "账号状态 1/0") @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return R.ok(adminStudentService.page(keyword, internStatus, status, page, size));
    }

    @Operation(summary = "学生档案详情")
    @GetMapping("/student/{id}")
    public R<AdminStudentVO> studentDetail(@PathVariable Long id) {
        return R.ok(adminStudentService.detail(id));
    }

    @Operation(summary = "新增学生档案（含账号 + 绑定）")
    @PostMapping("/student")
    public R<Long> studentCreate(@Valid @RequestBody AdminStudentSaveReq req) {
        return R.ok(adminStudentService.create(req));
    }

    @Operation(summary = "编辑学生档案")
    @PutMapping("/student/{id}")
    public R<Void> studentUpdate(@PathVariable Long id, @Valid @RequestBody AdminStudentSaveReq req) {
        adminStudentService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除学生档案（逻辑删除账号+档案）")
    @DeleteMapping("/student/{id}")
    public R<Void> studentDelete(@PathVariable Long id) {
        adminStudentService.delete(id);
        return R.ok();
    }

    @Operation(summary = "绑定/解绑 学生-教师-企业-mentor（0=解绑 null=不改）")
    @PostMapping("/student/{id}/bind")
    public R<Void> studentBind(@PathVariable Long id, @RequestBody StudentBindReq req) {
        adminStudentService.bind(id, req.getTeacherId(), req.getMentorId(), req.getCompanyId());
        return R.ok();
    }

    // ========== 企业档案 ==========

    @Operation(summary = "企业档案分页")
    @GetMapping("/company/page")
    public R<IPage<AdminCompanyVO>> companyPage(
            @RequestParam(required = false) String keyword,
            @Parameter(description = "黑名单 1/0") @RequestParam(required = false) Integer isBlacklist,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return R.ok(adminCompanyService.page(keyword, isBlacklist, page, size));
    }

    @Operation(summary = "新增企业档案")
    @PostMapping("/company")
    public R<Long> companyCreate(@Valid @RequestBody AdminCompanySaveReq req) {
        return R.ok(adminCompanyService.create(req));
    }

    @Operation(summary = "编辑企业档案")
    @PutMapping("/company/{id}")
    public R<Void> companyUpdate(@PathVariable Long id, @Valid @RequestBody AdminCompanySaveReq req) {
        adminCompanyService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除企业档案")
    @DeleteMapping("/company/{id}")
    public R<Void> companyDelete(@PathVariable Long id) {
        adminCompanyService.delete(id);
        return R.ok();
    }

    // ========== 教师档案 ==========

    @Operation(summary = "教师档案分页")
    @GetMapping("/teacher/page")
    public R<IPage<AdminTeacherVO>> teacherPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return R.ok(adminTeacherService.page(keyword, status, page, size));
    }

    @Operation(summary = "新增教师")
    @PostMapping("/teacher")
    public R<Long> teacherCreate(@Valid @RequestBody AdminTeacherSaveReq req) {
        return R.ok(adminTeacherService.create(req));
    }

    @Operation(summary = "编辑教师")
    @PutMapping("/teacher/{id}")
    public R<Void> teacherUpdate(@PathVariable Long id, @Valid @RequestBody AdminTeacherSaveReq req) {
        adminTeacherService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除教师")
    @DeleteMapping("/teacher/{id}")
    public R<Void> teacherDelete(@PathVariable Long id) {
        adminTeacherService.delete(id);
        return R.ok();
    }

    // ========== 企业指导（mentor）档案 ==========

    @Operation(summary = "企业指导分页")
    @GetMapping("/mentor/page")
    public R<IPage<AdminMentorVO>> mentorPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return R.ok(adminMentorService.page(keyword, companyId, status, page, size));
    }

    @Operation(summary = "新增企业指导")
    @PostMapping("/mentor")
    public R<Long> mentorCreate(@Valid @RequestBody AdminMentorSaveReq req) {
        return R.ok(adminMentorService.create(req));
    }

    @Operation(summary = "编辑企业指导")
    @PutMapping("/mentor/{id}")
    public R<Void> mentorUpdate(@PathVariable Long id, @Valid @RequestBody AdminMentorSaveReq req) {
        adminMentorService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除企业指导")
    @DeleteMapping("/mentor/{id}")
    public R<Void> mentorDelete(@PathVariable Long id) {
        adminMentorService.delete(id);
        return R.ok();
    }

    // ========== 绑定下拉数据源 ==========

    @Operation(summary = "教师下拉（id + 工号 + 姓名）")
    @GetMapping("/option/teachers")
    public R<List<Map<String, Object>>> optionTeachers() {
        List<Teacher> teachers = teacherMapper.selectList(new LambdaQueryWrapper<Teacher>()
                .orderByAsc(Teacher::getTeacherNo));
        if (teachers.isEmpty()) return R.ok(new ArrayList<>());
        List<Long> userIds = teachers.stream().map(Teacher::getUserId).collect(Collectors.toList());
        Map<Long, SysUser> userMap = sysUserMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));
        List<Map<String, Object>> list = new ArrayList<>(teachers.size());
        for (Teacher t : teachers) {
            SysUser u = userMap.get(t.getUserId());
            Map<String, Object> row = new HashMap<>();
            row.put("id", t.getId());
            row.put("teacherNo", t.getTeacherNo());
            row.put("name", u == null ? null : u.getRealName());
            list.add(row);
        }
        return R.ok(list);
    }

    @Operation(summary = "企业指导下拉（id + 姓名 + 企业名）")
    @GetMapping("/option/mentors")
    public R<List<Map<String, Object>>> optionMentors() {
        List<Mentor> mentors = mentorMapper.selectList(new LambdaQueryWrapper<Mentor>()
                .orderByDesc(Mentor::getCreateTime));
        if (mentors.isEmpty()) return R.ok(new ArrayList<>());
        List<Long> userIds = mentors.stream().map(Mentor::getUserId).collect(Collectors.toList());
        Map<Long, SysUser> userMap = sysUserMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));
        List<Long> companyIds = mentors.stream().map(Mentor::getCompanyId).distinct().collect(Collectors.toList());
        Map<Long, Company> companyMap = companyMapper.selectByIds(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, c -> c));
        List<Map<String, Object>> list = new ArrayList<>(mentors.size());
        for (Mentor m : mentors) {
            SysUser u = userMap.get(m.getUserId());
            Company c = companyMap.get(m.getCompanyId());
            Map<String, Object> row = new HashMap<>();
            row.put("id", m.getId());
            row.put("name", u == null ? null : u.getRealName());
            row.put("companyName", c == null ? null : c.getName());
            list.add(row);
        }
        return R.ok(list);
    }

    @Operation(summary = "企业下拉（id + 名称）")
    @GetMapping("/option/companies")
    public R<List<Map<String, Object>>> optionCompanies() {
        List<Company> companies = companyMapper.selectList(new LambdaQueryWrapper<Company>()
                .orderByAsc(Company::getName));
        List<Map<String, Object>> list = new ArrayList<>(companies.size());
        for (Company c : companies) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", c.getId());
            row.put("name", c.getName());
            list.add(row);
        }
        return R.ok(list);
    }
}
