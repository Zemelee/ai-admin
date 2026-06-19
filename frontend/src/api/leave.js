import { req } from '@/utils/http'

// 学生端
export const submitLeave = (data) => req('post', '/leave/student/submit', data)
export const cancelLeave = (id) => req('post', `/leave/student/cancel/${id}`)
export const myLeaves = (status) => req('get', '/leave/student/my', status ? { status } : null)

// 教师端
export const teacherPending = () => req('get', '/leave/teacher/pending')
export const teacherHistory = () => req('get', '/leave/teacher/history')
export const teacherApprove = (id, data) => req('post', `/leave/teacher/approve/${id}`, data)

// 监管者端
export const supervisorPending = () => req('get', '/leave/supervisor/pending')
export const supervisorHistory = () => req('get', '/leave/supervisor/history')
export const supervisorApprove = (id, data) => req('post', `/leave/supervisor/approve/${id}`, data)

// 通用
export const leaveDetail = (id) => req('get', `/leave/${id}`)

// 类型/状态/节点 中文映射
export const TYPE_LABEL = { SICK: '病假', PERSONAL: '事假', OTHER: '其他' }
export const STATUS_LABEL = { PENDING: '审批中', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已撤回' }
export const STATUS_TAG = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }
export const NODE_LABEL = { TEACHER: '教师审批', SUPERVISOR: '系主任审批', MENTOR: '企业指导' }
export const RESULT_LABEL = { APPROVED: '通过', REJECTED: '驳回' }
export const RESULT_TAG = { APPROVED: 'success', REJECTED: 'danger' }

export function fmtDateTime(s) {
  if (!s) return ''
  // 后端返回 ISO 串或 'yyyy-MM-ddTHH:mm:ss'
  return String(s).replace('T', ' ').slice(0, 16)
}
