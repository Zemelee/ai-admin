import { req } from '@/utils/http'

// 学生端
export const submitReport = (data) => req('post', '/intern-report/student/submit', data)
export const updateReport = (id, data) => req('put', `/intern-report/student/${id}`, data)
export const myReports = (status) => req('get', '/intern-report/student/my', status ? { status } : null)

// 教师端
export const teacherPendingReports = () => req('get', '/intern-report/teacher/pending')
export const teacherAllReports = () => req('get', '/intern-report/teacher/my')
export const teacherReviewReport = (id, data) => req('post', `/intern-report/teacher/review/${id}`, data)

// 通用
export const reportDetail = (id) => req('get', `/intern-report/${id}`)

// 报告类型
export const REPORT_TYPE = { MID_TERM: 'MID_TERM', FINAL: 'FINAL' }
export const REPORT_TYPE_LABEL = { MID_TERM: '中期报告', FINAL: '终期报告' }
export const REPORT_TYPE_TAG = { MID_TERM: 'warning', FINAL: 'success' }

// 状态
export const STATUS_LABEL = { SUBMITTED: '待评阅', REVIEWED: '已评分', REJECTED: '已驳回' }
export const STATUS_TAG = { SUBMITTED: 'warning', REVIEWED: 'success', REJECTED: 'danger' }

// 结果
export const RESULT_LABEL = { REVIEWED: '通过', REJECTED: '驳回' }
export const RESULT_TAG = { REVIEWED: 'success', REJECTED: 'danger' }

export function fmtDate(s) {
  if (!s) return ''
  return String(s).slice(0, 10)
}
export function fmtDateTime(s) {
  if (!s) return ''
  return String(s).replace('T', ' ').slice(0, 16)
}
