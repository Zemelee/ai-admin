import { req } from '@/utils/http'

// 触发扫描
export const scanWarnings = () => req('post', '/warning/scan')

// 统计聚合
export const warningStats = () => req('get', '/warning/stats')

// 列表（默认待处理）
export const warningList = (params) => req('get', '/warning', params)

// 审核
export const reviewWarning = (id, data) => req('post', `/warning/review/${id}`, data)

// 级别
export const LEVEL_LABEL = { RED: '红色预警', YELLOW: '黄色预警', GREEN: '正常' }
export const LEVEL_TAG = { RED: 'danger', YELLOW: 'warning', GREEN: 'success' }

// 状态
export const STATUS_LABEL = { PENDING: '待处理', REVIEWED: '已处理', IGNORED: '已忽略' }
export const STATUS_TAG = { PENDING: 'danger', REVIEWED: 'success', IGNORED: 'info' }

// 规则码
export const RULE_LABEL = {
  NO_LOG_3D: '缺交日志',
  SENSITIVE_WORD: '敏感词命中',
  COMPANY_BLACKLIST: '禁入企业',
  TRANSFER_PENDING_3D: '变更停滞',
  LEAVE_OVER_3M: '长期请假'
}
export const RULE_TAG = {
  NO_LOG_3D: 'warning',
  SENSITIVE_WORD: 'danger',
  COMPANY_BLACKLIST: 'danger',
  TRANSFER_PENDING_3D: 'warning',
  LEAVE_OVER_3M: 'warning'
}

export function fmtDateTime(s) {
  if (!s) return ''
  return String(s).replace('T', ' ').slice(0, 16)
}
