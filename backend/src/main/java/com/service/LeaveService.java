package com.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.common.BizException;
import com.common.LeaveConst;
import com.dto.leave.LeaveApproveReq;
import com.dto.leave.LeaveDetailVO;
import com.dto.leave.LeaveListItemVO;
import com.dto.leave.LeaveSubmitReq;
import com.entity.ApprovalFlow;
import com.entity.LeaveApply;
import com.entity.Student;
import com.entity.SysUser;
import com.entity.Teacher;
import com.mapper.ApprovalFlowMapper;
import com.mapper.LeaveApplyMapper;
import com.mapper.SysUserMapper;
import com.mapper.TeacherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 请假业务核心服务（学生提交、教师/监管者审批、状态机推进）
 */
@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveApplyMapper leaveApplyMapper;
    private final ApprovalFlowMapper approvalFlowMapper;
    private final TeacherMapper teacherMapper;
    private final SysUserMapper sysUserMapper;
    private final LeaveQueryHelper helper;
    private final AttachmentService attachmentService;

    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, LeaveSubmitReq req) {
        Student student = helper.requireStudentByUserId(userId);
        if (req.getEndTime().isBefore(req.getStartTime()) || req.getEndTime().isEqual(req.getStartTime())) {
            throw BizException.validate("结束时间必须晚于开始时间");
        }
        if (!helper.validLeaveType(req.getLeaveType())) {
            throw BizException.validate("非法的请假类型：" + req.getLeaveType());
        }
        BigDecimal duration = helper.computeDurationDays(req.getStartTime(), req.getEndTime());

        LeaveApply leave = new LeaveApply();
        leave.setStudentId(student.getId());
        leave.setLeaveType(req.getLeaveType());
        leave.setStartTime(req.getStartTime());
        leave.setEndTime(req.getEndTime());
        leave.setDurationDays(duration);
        leave.setReason(req.getReason());
        leave.setParentConfirm(Boolean.TRUE.equals(req.getParentConfirm()) ? 1 : 0);
        leave.setStatus(LeaveConst.STATUS_PENDING);
        leave.setCurrentNode(LeaveConst.NODE_TEACHER);
        leave.setSubmitTime(LocalDateTime.now());
        leaveApplyMapper.insert(leave);

        ApprovalFlow tf = new ApprovalFlow();
        tf.setBizType(LeaveConst.BIZ_LEAVE);
        tf.setBizId(leave.getId());
        tf.setNode(LeaveConst.NODE_TEACHER);
        tf.setNodeSeq(1);
        if (student.getTeacherId() != null) {
            Teacher teacher = teacherMapper.selectById(student.getTeacherId());
            if (teacher != null) {
                tf.setApproverId(teacher.getUserId());
                SysUser tu = sysUserMapper.selectById(teacher.getUserId());
                if (tu != null) {
                    tf.setApproverName(tu.getRealName());
                }
            }
        }
        approvalFlowMapper.insert(tf);

        // 绑定上传时 bizId 为空的佐证材料附件到本请假单
        attachmentService.bindBiz(userId, LeaveConst.BIZ_LEAVE, leave.getId(), req.getAttachmentIds());
        return leave.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long leaveId) {
        Student student = helper.requireStudentByUserId(userId);
        LeaveApply leave = helper.requireLeave(leaveId);
        if (!Objects.equals(leave.getStudentId(), student.getId())) {
            throw BizException.forbidden("无权操作他人申请单");
        }
        if (!LeaveConst.STATUS_PENDING.equals(leave.getStatus())) {
            throw BizException.validate("仅待审批状态可取消，当前状态：" + leave.getStatus());
        }
        leave.setStatus(LeaveConst.STATUS_CANCELLED);
        leave.setCurrentNode(null);
        leave.setFinishTime(LocalDateTime.now());
        leaveApplyMapper.updateById(leave);
    }

    /**
     * 学生端 - 我的请假列表
     * <p>根据登录用户（学生）查询其本人的所有请假申请，按创建时间倒序排列。
     * 支持按状态过滤（如 PENDING/APPROVED/CANCELLED）。
     *
     * @param userId  当前登录用户的 userId（学生）
     * @param status  请假状态过滤条件，null 或 blank 表示返回所有状态
     * @return 请假申请列表（包含基础信息的 VO 列表，用于前端列表展示）
     */
    public List<LeaveListItemVO> myLeaves(Long userId, String status) {
        // 1. 校验当前用户是学生，并获取其学生信息（不存在则抛异常）
        Student student = helper.requireStudentByUserId(userId);
        // 2. 构建查询条件：WHERE student_id = ? ORDER BY create_time DESC
        LambdaQueryWrapper<LeaveApply> qw = new LambdaQueryWrapper<LeaveApply>()
                // 筛选当前学生的请假申请（根据 student_id 关联）
                .eq(LeaveApply::getStudentId, student.getId())
                // 按创建时间倒序排列
                .orderByDesc(LeaveApply::getCreateTime);

        // 3. 如果指定了状态参数，则追加 WHERE 条件（例如 status='PENDING'）
        if (StrUtil.isNotBlank(status)) {
            qw.eq(LeaveApply::getStatus, status);
        }
        // 4. 执行查询，获取该学生的所有请假申请记录
        List<LeaveApply> list = leaveApplyMapper.selectList(qw);
        // 5. 如果没有记录，直接返回空列表
        if (list.isEmpty()) return new ArrayList<>();
        // 6. 根据学生关联的 sys_user.id 查询用户名（realName），用于 VO 展示（如"张三"）
        SysUser u = sysUserMapper.selectById(student.getUserId());
        String studentName = u == null ? null : u.getRealName();
        // 7. 将实体 LeaveApply 转换为 VO（含补充信息：学生姓名、班级等），填充列表
        List<LeaveListItemVO> result = new ArrayList<>(list.size());
        for (LeaveApply l : list) {
            result.add(helper.toListItem(l, student, studentName));
        }
        // 8. 返回结果（包含该学生所有请假记录的 VO 列表）
        return result;
    }

    public LeaveDetailVO detail(Long userId, String roleCode, Long leaveId) {
        LeaveApply leave = helper.requireLeave(leaveId);
        helper.checkDetailAccess(leave, userId, roleCode);
        return helper.buildDetail(leave);
    }

    public List<LeaveListItemVO> teacherPending(Long userId) {
        List<ApprovalFlow> flows = approvalFlowMapper.selectList(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getBizType, LeaveConst.BIZ_LEAVE)
                .eq(ApprovalFlow::getNode, LeaveConst.NODE_TEACHER)
                .eq(ApprovalFlow::getApproverId, userId)
                .isNull(ApprovalFlow::getResult));
        if (flows.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> bizIds = flows.stream().map(ApprovalFlow::getBizId).distinct().collect(Collectors.toList());
        List<LeaveApply> leaves = leaveApplyMapper.selectList(new LambdaQueryWrapper<LeaveApply>()
                .in(LeaveApply::getId, bizIds)
                .eq(LeaveApply::getStatus, LeaveConst.STATUS_PENDING)
                .eq(LeaveApply::getCurrentNode, LeaveConst.NODE_TEACHER)
                .orderByAsc(LeaveApply::getSubmitTime));
        return helper.enrichListItems(leaves);
    }

    public List<LeaveListItemVO> supervisorPending() {
        List<LeaveApply> leaves = leaveApplyMapper.selectList(new LambdaQueryWrapper<LeaveApply>()
                .eq(LeaveApply::getStatus, LeaveConst.STATUS_PENDING)
                .eq(LeaveApply::getCurrentNode, LeaveConst.NODE_SUPERVISOR)
                .orderByAsc(LeaveApply::getSubmitTime));
        return helper.enrichListItems(leaves);
    }

    public List<LeaveListItemVO> approvedHistoryByMe(Long userId) {
        List<ApprovalFlow> flows = approvalFlowMapper.selectList(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getBizType, LeaveConst.BIZ_LEAVE)
                .eq(ApprovalFlow::getApproverId, userId)
                .isNotNull(ApprovalFlow::getResult)
                .orderByDesc(ApprovalFlow::getActTime));
        if (flows.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> bizIds = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (ApprovalFlow f : flows) {
            if (seen.add(f.getBizId())) {
                bizIds.add(f.getBizId());
            }
        }
        List<LeaveApply> leaves = leaveApplyMapper.selectByIds(bizIds);
        Map<Long, LeaveApply> map = leaves.stream().collect(Collectors.toMap(LeaveApply::getId, l -> l));
        List<LeaveApply> ordered = new ArrayList<>();
        for (Long id : bizIds) {
            LeaveApply l = map.get(id);
            if (l != null) {
                ordered.add(l);
            }
        }
        return helper.enrichListItems(ordered);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long userId, Long leaveId, String expectNode, LeaveApproveReq req) {
        if (!LeaveConst.RESULT_APPROVED.equals(req.getResult())
                && !LeaveConst.RESULT_REJECTED.equals(req.getResult())) {
            throw BizException.validate("非法的审批结果：" + req.getResult());
        }
        if (LeaveConst.RESULT_REJECTED.equals(req.getResult()) && StrUtil.isBlank(req.getComment())) {
            throw BizException.validate("驳回必须填写审批意见");
        }
        LeaveApply leave = helper.requireLeave(leaveId);
        if (!LeaveConst.STATUS_PENDING.equals(leave.getStatus())) {
            throw BizException.validate("当前申请已不在待审批状态");
        }
        if (!expectNode.equals(leave.getCurrentNode())) {
            throw BizException.forbidden("当前节点为 " + leave.getCurrentNode() + "，无权审批");
        }

        ApprovalFlow flow = helper.currentPendingFlow(leaveId, expectNode);
        if (flow == null) {
            throw BizException.validate("未找到该节点的待办流水（数据异常）");
        }
        // TEACHER 节点必须由该单流水中已分配的 approver 处理；SUPERVISOR 节点任意监管者可领取
        if (LeaveConst.NODE_TEACHER.equals(expectNode)) {
            if (flow.getApproverId() != null && !Objects.equals(flow.getApproverId(), userId)) {
                throw BizException.forbidden("该单已指派给其他教师审批");
            }
            if (flow.getApproverId() == null) {
                flow.setApproverId(userId);
            }
        } else if (LeaveConst.NODE_SUPERVISOR.equals(expectNode)) {
            // 抢占式领取
            flow.setApproverId(userId);
        }

        SysUser actor = sysUserMapper.selectById(userId);
        if (actor != null) {
            flow.setApproverName(actor.getRealName());
        }
        flow.setResult(req.getResult());
        flow.setComment(req.getComment());
        flow.setActTime(LocalDateTime.now());
        approvalFlowMapper.updateById(flow);

        if (LeaveConst.RESULT_REJECTED.equals(req.getResult())) {
            leave.setStatus(LeaveConst.STATUS_REJECTED);
            leave.setCurrentNode(null);
            leave.setFinishTime(LocalDateTime.now());
            leaveApplyMapper.updateById(leave);
            return;
        }

        // 通过：判断是否需要进入下一个节点
        if (LeaveConst.NODE_TEACHER.equals(expectNode)) {
            BigDecimal threshold = new BigDecimal(LeaveConst.SUPERVISOR_THRESHOLD_DAYS);
            if (leave.getDurationDays() != null && leave.getDurationDays().compareTo(threshold) > 0) {
                // 进入 SUPERVISOR 节点
                leave.setCurrentNode(LeaveConst.NODE_SUPERVISOR);
                leaveApplyMapper.updateById(leave);

                ApprovalFlow sf = new ApprovalFlow();
                sf.setBizType(LeaveConst.BIZ_LEAVE);
                sf.setBizId(leave.getId());
                sf.setNode(LeaveConst.NODE_SUPERVISOR);
                sf.setNodeSeq(2);
                approvalFlowMapper.insert(sf);
                return;
            }
        }
        // 已是终审节点（teacher 但 ≤30 天 / supervisor）通过 -> 整单 APPROVED
        leave.setStatus(LeaveConst.STATUS_APPROVED);
        leave.setCurrentNode(null);
        leave.setFinishTime(LocalDateTime.now());
        leaveApplyMapper.updateById(leave);
    }
}
