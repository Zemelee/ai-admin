package com.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.common.BizException;
import com.common.LeaveConst;
import com.dto.leave.LeaveDetailVO;
import com.dto.leave.LeaveListItemVO;
import com.entity.ApprovalFlow;
import com.entity.Company;
import com.entity.LeaveApply;
import com.entity.Student;
import com.entity.SysUser;
import com.mapper.ApprovalFlowMapper;
import com.mapper.CompanyMapper;
import com.mapper.LeaveApplyMapper;
import com.mapper.StudentMapper;
import com.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 请假业务查询/工具方法集合
 */
@Component
@RequiredArgsConstructor
public class LeaveQueryHelper {

    private final StudentMapper studentMapper;
    private final SysUserMapper sysUserMapper;
    private final CompanyMapper companyMapper;
    private final LeaveApplyMapper leaveApplyMapper;
    private final ApprovalFlowMapper approvalFlowMapper;

    public Student requireStudentByUserId(Long userId) {
        Student s = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getUserId, userId).last("LIMIT 1"));
        if (s == null) {
            throw BizException.validate("未找到学生档案，请联系管理员补全");
        }
        return s;
    }

    public LeaveApply requireLeave(Long id) {
        LeaveApply l = leaveApplyMapper.selectById(id);
        if (l == null) {
            throw BizException.notFound("请假申请不存在：id=" + id);
        }
        return l;
    }

    public boolean validLeaveType(String t) {
        return LeaveConst.TYPE_SICK.equals(t)
                || LeaveConst.TYPE_PERSONAL.equals(t)
                || LeaveConst.TYPE_OTHER.equals(t);
    }

    public BigDecimal computeDurationDays(LocalDateTime start, LocalDateTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes <= 0) {
            return new BigDecimal("0.01");
        }
        return new BigDecimal(minutes)
                .divide(new BigDecimal(60 * 24), 2, RoundingMode.HALF_UP);
    }

    public LeaveListItemVO toListItem(LeaveApply l, Student s, String studentName) {
        LeaveListItemVO vo = new LeaveListItemVO();
        vo.setId(l.getId());
        vo.setStudentId(l.getStudentId());
        vo.setStudentName(studentName);
        vo.setStudentNo(s == null ? null : s.getStudentNo());
        vo.setClassName(s == null ? null : s.getClassName());
        vo.setLeaveType(l.getLeaveType());
        vo.setStartTime(l.getStartTime());
        vo.setEndTime(l.getEndTime());
        vo.setDurationDays(l.getDurationDays());
        vo.setStatus(l.getStatus());
        vo.setCurrentNode(l.getCurrentNode());
        vo.setSubmitTime(l.getSubmitTime());
        vo.setCreateTime(l.getCreateTime());
        vo.setReason(l.getReason());
        return vo;
    }

    public List<LeaveListItemVO> enrichListItems(List<LeaveApply> leaves) {
        if (leaves == null || leaves.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> studentIds = leaves.stream().map(LeaveApply::getStudentId)
                .distinct().collect(Collectors.toList());
        Map<Long, Student> studentMap = studentMapper.selectByIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, x -> x));
        List<Long> userIds = studentMap.values().stream().map(Student::getUserId)
                .distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? new HashMap<>()
                : sysUserMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        List<LeaveListItemVO> result = new ArrayList<>(leaves.size());
        for (LeaveApply l : leaves) {
            Student s = studentMap.get(l.getStudentId());
            SysUser u = s == null ? null : userMap.get(s.getUserId());
            result.add(toListItem(l, s, u == null ? null : u.getRealName()));
        }
        return result;
    }

    public LeaveDetailVO buildDetail(LeaveApply leave) {
        Student student = studentMapper.selectById(leave.getStudentId());
        SysUser user = student == null ? null : sysUserMapper.selectById(student.getUserId());
        Company company = (student != null && student.getCompanyId() != null)
                ? companyMapper.selectById(student.getCompanyId()) : null;

        LeaveDetailVO vo = new LeaveDetailVO();
        vo.setId(leave.getId());
        vo.setStudentId(leave.getStudentId());
        vo.setStudentName(user == null ? null : user.getRealName());
        vo.setStudentNo(student == null ? null : student.getStudentNo());
        vo.setClassName(student == null ? null : student.getClassName());
        vo.setMajor(student == null ? null : student.getMajor());
        vo.setCompanyName(company == null ? null : company.getName());

        vo.setLeaveType(leave.getLeaveType());
        vo.setStartTime(leave.getStartTime());
        vo.setEndTime(leave.getEndTime());
        vo.setDurationDays(leave.getDurationDays());
        vo.setReason(leave.getReason());
        vo.setParentConfirm(leave.getParentConfirm());

        vo.setStatus(leave.getStatus());
        vo.setCurrentNode(leave.getCurrentNode());
        vo.setSubmitTime(leave.getSubmitTime());
        vo.setFinishTime(leave.getFinishTime());
        vo.setCreateTime(leave.getCreateTime());

        List<ApprovalFlow> flows = approvalFlowMapper.selectList(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getBizType, LeaveConst.BIZ_LEAVE)
                .eq(ApprovalFlow::getBizId, leave.getId())
                .orderByAsc(ApprovalFlow::getNodeSeq));

        List<LeaveDetailVO.FlowItem> items = new ArrayList<>(flows.size());
        for (ApprovalFlow f : flows) {
            LeaveDetailVO.FlowItem fi = new LeaveDetailVO.FlowItem();
            fi.setId(f.getId());
            fi.setNode(f.getNode());
            fi.setNodeSeq(f.getNodeSeq());
            fi.setApproverId(f.getApproverId());
            fi.setApproverName(f.getApproverName());
            fi.setResult(f.getResult());
            fi.setComment(f.getComment());
            fi.setActTime(f.getActTime());
            items.add(fi);
        }
        vo.setFlow(items);
        return vo;
    }

    public void checkDetailAccess(LeaveApply leave, Long userId, String roleCode) {
        if ("supervisor".equals(roleCode)) {
            return;
        }
        if ("student".equals(roleCode)) {
            Student s = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                    .eq(Student::getUserId, userId).last("LIMIT 1"));
            if (s != null && Objects.equals(s.getId(), leave.getStudentId())) {
                return;
            }
        }
        if ("teacher".equals(roleCode)) {
            ApprovalFlow tf = approvalFlowMapper.selectOne(new LambdaQueryWrapper<ApprovalFlow>()
                    .eq(ApprovalFlow::getBizType, LeaveConst.BIZ_LEAVE)
                    .eq(ApprovalFlow::getBizId, leave.getId())
                    .eq(ApprovalFlow::getNode, LeaveConst.NODE_TEACHER)
                    .last("LIMIT 1"));
            if (tf != null && Objects.equals(tf.getApproverId(), userId)) {
                return;
            }
        }
        throw BizException.forbidden("无权查看该申请单");
    }

    public ApprovalFlow currentPendingFlow(Long leaveId, String node) {
        return approvalFlowMapper.selectOne(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getBizType, LeaveConst.BIZ_LEAVE)
                .eq(ApprovalFlow::getBizId, leaveId)
                .eq(ApprovalFlow::getNode, node)
                .isNull(ApprovalFlow::getResult)
                .last("LIMIT 1"));
    }
}
