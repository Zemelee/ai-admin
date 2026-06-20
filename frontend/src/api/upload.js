import http from '@/utils/http'
import { req } from '@/utils/http'

/**
 * 上传图片附件到 MinIO（经后端中转）。
 * 需要 progress 时用默认导出 http（原生 axios 实例）；返回业务体 data。
 * @param {File} file
 * @param {string} bizType  LOG / LEAVE / WEEKLY / TRANSFER ...
 * @param {number|null} bizId  业务单 ID，提交前上传可留空
 * @param {(percent:number)=>void} [onProgress]
 */
export function uploadImage(file, bizType, bizId, onProgress) {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('bizType', bizType)
  if (bizId != null) fd.append('bizId', bizId)
  return http
    .post('/attachment/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (onProgress && e.total) onProgress(Math.round((e.loaded / e.total) * 100))
      }
    })
    .then((r) => r.data.data)
}

/** 查询某业务单的附件列表（含预签名预览 URL） */
export const listAttachments = (bizType, bizId) =>
  req('get', '/attachment/list', { bizType, bizId })

/** 删除附件（仅上传人本人） */
export const removeAttachment = (id) => req('delete', `/attachment/${id}`)
