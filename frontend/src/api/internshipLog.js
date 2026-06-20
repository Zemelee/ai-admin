import { req } from '@/utils/http'

// 学生端
export const myLogs = () => req('get', '/intern-log/student/my')
export const submitLog = (data) => req('post', '/intern-log/student/submit', data)
export const updateLog = (id, data) => req('put', `/intern-log/student/${id}`, data)

// 企业指导端
export const mentorPendingLogs = () => req('get', '/intern-log/mentor/pending')
export const mentorConfirmLog = (id, data) => req('post', `/intern-log/mentor/confirm/${id}`, data)

// 通用详情
export const logDetail = (id) => req('get', `/intern-log/${id}`)

// 状态/结果 中文映射
export const STATUS_LABEL = { SUBMITTED: '待确认', CONFIRMED: '已通过', REJECTED: '已驳回' }
export const STATUS_TAG = { SUBMITTED: 'warning', CONFIRMED: 'success', REJECTED: 'danger' }
export const RESULT_LABEL = { CONFIRMED: '通过', REJECTED: '驳回' }

export function fmtDate(s) {
  if (!s) return ''
  return String(s).slice(0, 10)
}
export function fmtDateTime(s) {
  if (!s) return ''
  return String(s).replace('T', ' ').slice(0, 16)
}
