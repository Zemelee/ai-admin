import { req } from '@/utils/http'

// 教师：分管学生成绩
export const teacherScores = () => req('get', '/score/teacher/my')

// supervisor：全部学生成绩
export const allScores = () => req('get', '/score/supervisor/all')

// 学生：我的成绩
export const myScore = () => req('get', '/score/student/my')

// 等级颜色映射
export const GRADE_TAG = {
  '优秀': 'success',
  '良好': 'primary',
  '中等': 'warning',
  '及格': 'info',
  '不及格': 'danger',
  '无法评定': 'info'
}

export function fmtScore(s) {
  if (s == null) return '-'
  return Number(s).toFixed(1)
}
