import { req } from '@/utils/http'

// supervisor 管理端
export const announcementList = (params) => req('get', '/announcement/list', params)
export const announcementCreate = (data) => req('post', '/announcement', data)
export const announcementUpdate = (id, data) => req('put', `/announcement/${id}`, data)
export const announcementDelete = (id) => req('delete', `/announcement/${id}`)

// 公开（所有角色可见）
export const announcementLatest = (limit) => req('get', '/announcement/latest', { limit })

export const PRIORITY_LABEL = { NORMAL: '普通', IMPORTANT: '重要', URGENT: '紧急' }
export const PRIORITY_TAG = { NORMAL: 'info', IMPORTANT: 'warning', URGENT: 'danger' }
export const STATUS_LABEL = { DRAFT: '草稿', PUBLISHED: '已发布' }
export const STATUS_TAG = { DRAFT: 'info', PUBLISHED: 'success' }