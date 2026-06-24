import { req } from '@/utils/http'

// 学生端
export const submitTransfer = (data) => req('post', '/transfer/student/submit', data)
export const cancelTransfer = (id) => req('post', `/transfer/student/cancel/${id}`)
export const myTransfers = (status) => req('get', '/transfer/student/my', status ? { status } : null)

// 企业指导端
export const mentorPendingTransfers = () => req('get', '/transfer/mentor/pending')
export const mentorHistoryTransfers = () => req('get', '/transfer/mentor/history')
export const mentorApproveTransfer = (id, data) => req('post', `/transfer/mentor/approve/${id}`, data)

// 教师端
export const teacherPendingTransfers = () => req('get', '/transfer/teacher/pending')
export const teacherHistoryTransfers = () => req('get', '/transfer/teacher/history')
export const teacherApproveTransfer = (id, data) => req('post', `/transfer/teacher/approve/${id}`, data)

// 监管者端
export const supervisorPendingTransfers = () => req('get', '/transfer/supervisor/pending')
export const supervisorHistoryTransfers = () => req('get', '/transfer/supervisor/history')
export const supervisorApproveTransfer = (id, data) => req('post', `/transfer/supervisor/approve/${id}`, data)

// 通用
export const transferDetail = (id) => req('get', `/transfer/${id}`)

// 中文映射
export const STATUS_LABEL = { PENDING: '审批中', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已撤回' }
export const STATUS_TAG = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }
export const NODE_LABEL = { MENTOR: '企业指导审批', TEACHER: '教师审批', SUPERVISOR: '系主任审批' }
export const RESULT_LABEL = { APPROVED: '通过', REJECTED: '驳回' }
export const RESULT_TAG = { APPROVED: 'success', REJECTED: 'danger' }

export function fmtDate(s) {
  if (!s) return ''
  return String(s).slice(0, 10)
}
export function fmtDateTime(s) {
  if (!s) return ''
  return String(s).replace('T', ' ').slice(0, 16)
}
