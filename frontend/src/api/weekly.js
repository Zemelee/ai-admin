import { req } from '@/utils/http'

// 学生
export const myWeeklies = () => req('get', '/intern-weekly/student/my')
export const submitWeekly = (data) => req('post', '/intern-weekly/student/submit', data)
export const updateWeekly = (id, data) => req('put', `/intern-weekly/student/${id}`, data)

// 教师
export const teacherPendingWeeklies = () => req('get', '/intern-weekly/teacher/pending')
export const teacherAllWeeklies = () => req('get', '/intern-weekly/teacher/my')
export const teacherReviewWeekly = (id, data) => req('post', `/intern-weekly/teacher/review/${id}`, data)

// 通用
export const weeklyDetail = (id) => req('get', `/intern-weekly/${id}`)

export const STATUS_LABEL = { SUBMITTED: '待评分', REVIEWED: '已评分', REJECTED: '已驳回' }
export const STATUS_TAG = { SUBMITTED: 'warning', REVIEWED: 'success', REJECTED: 'danger' }

/** Date -> ISO 周（YYYY-Www / 周一 / 周日） */
export function isoWeekOf(date) {
  const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()))
  const dayNum = d.getUTCDay() || 7 // Sun=7
  d.setUTCDate(d.getUTCDate() + 4 - dayNum) // 移到本周四确定年份
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1))
  const weekNum = Math.ceil(((d - yearStart) / 86400000 + 1) / 7)
  const year = d.getUTCFullYear()
  const weekStr = `${year}-W${String(weekNum).padStart(2, '0')}`

  // 计算原始 date 当周的周一和周日（本地时区）
  const local = new Date(date)
  const localDay = local.getDay() || 7
  const monday = new Date(local)
  monday.setDate(local.getDate() - (localDay - 1))
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  return { yearWeek: weekStr, weekStart: ymd(monday), weekEnd: ymd(sunday) }
}

function ymd(d) {
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mm}-${dd}`
}

export function fmtDate(s) {
  if (!s) return ''
  return String(s).slice(0, 10)
}
export function fmtDateTime(s) {
  if (!s) return ''
  return String(s).replace('T', ' ').slice(0, 16)
}
