import { req } from '@/utils/http'

// ========== 学生档案 ==========
export const adminStudentPage = (params) => req('get', '/admin/student/page', params)
export const adminStudentDetail = (id) => req('get', `/admin/student/${id}`)
export const adminStudentCreate = (data) => req('post', '/admin/student', data)
export const adminStudentUpdate = (id, data) => req('put', `/admin/student/${id}`, data)
export const adminStudentDelete = (id) => req('delete', `/admin/student/${id}`)
export const adminStudentBind = (id, data) => req('post', `/admin/student/${id}/bind`, data)

// ========== 企业档案 ==========
export const adminCompanyPage = (params) => req('get', '/admin/company/page', params)
export const adminCompanyCreate = (data) => req('post', '/admin/company', data)
export const adminCompanyUpdate = (id, data) => req('put', `/admin/company/${id}`, data)
export const adminCompanyDelete = (id) => req('delete', `/admin/company/${id}`)

// ========== 教师档案 ==========
export const adminTeacherPage = (params) => req('get', '/admin/teacher/page', params)
export const adminTeacherCreate = (data) => req('post', '/admin/teacher', data)
export const adminTeacherUpdate = (id, data) => req('put', `/admin/teacher/${id}`, data)
export const adminTeacherDelete = (id) => req('delete', `/admin/teacher/${id}`)

// ========== 企业指导档案 ==========
export const adminMentorPage = (params) => req('get', '/admin/mentor/page', params)
export const adminMentorCreate = (data) => req('post', '/admin/mentor', data)
export const adminMentorUpdate = (id, data) => req('put', `/admin/mentor/${id}`, data)
export const adminMentorDelete = (id) => req('delete', `/admin/mentor/${id}`)

// ========== 绑定下拉 ==========
export const optionTeachers = () => req('get', '/admin/option/teachers')
export const optionMentors = () => req('get', '/admin/option/mentors')
export const optionCompanies = () => req('get', '/admin/option/companies')

// ========== 映射 ==========
export const INTERN_STATUS_LABEL = { ACTIVE: '实习中', SUSPEND: '暂停', FINISHED: '已完成', QUIT: '已退出' }
export const INTERN_STATUS_TAG = { ACTIVE: 'success', SUSPEND: 'warning', FINISHED: 'info', QUIT: 'danger' }
export const GENDER_LABEL = { 1: '男', 2: '女' }

export function fmtDate(s) {
  if (!s) return ''
  return String(s).slice(0, 10)
}
export function fmtDateTime(s) {
  if (!s) return ''
  return String(s).replace('T', ' ').slice(0, 16)
}
