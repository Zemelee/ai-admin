import { req } from '@/utils/http'

// mentor 端
export const myEvalStudents = () => req('get', '/company-eval/mentor/my-students')
export const saveEval = (data) => req('post', '/company-eval/mentor/save', data)
export const submitEval = (id) => req('post', `/company-eval/mentor/submit/${id}`)

// student 端
export const myAppraisal = () => req('get', '/company-eval/student/my')

// teacher / supervisor
export const teacherEvals = () => req('get', '/company-eval/teacher/my')
export const supervisorEvals = () => req('get', '/company-eval/supervisor/all')

// 通用详情
export const evalDetail = (id) => req('get', `/company-eval/${id}`)

export const STATUS_LABEL = { DRAFT: '草稿', SUBMITTED: '已提交' }
export const STATUS_TAG = { DRAFT: 'info', SUBMITTED: 'success' }

export const SCORE_TEXTS = ['极差', '较差', '一般', '良好', '优秀']

export function fmtDateTime(s) {
  if (!s) return ''
  return String(s).replace('T', ' ').slice(0, 16)
}
