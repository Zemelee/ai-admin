import { BASE_URL, getToken } from './http'

/**
 * 带认证的下载：通过 fetch + satoken header 请求后端，将响应存为 Blob 下载。
 * 解决了 window.open 无法携带 Authorization 头的问题。
 */
export async function download(url, filename) {
  const resp = await fetch(BASE_URL + url, {
    headers: { satoken: getToken() || '' }
  })
  if (!resp.ok) {
    const text = await resp.text()
    throw new Error(text || `下载失败 (${resp.status})`)
  }
  const blob = await resp.blob()
  // 如果后端返回的是纯文本（无数据场景），直接报错
  if (blob.type.includes('text') && blob.size < 200) {
    const text = await blob.text()
    if (text.includes('暂无数据')) {
      throw new Error(text)
    }
  }
  // 后端返回 HTML 说明走到了错误页（如未授权）
  if (blob.type.includes('text/html')) {
    throw new Error('请求失败，请确认登录状态')
  }
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  // 从 Content-Disposition 或 URL 中取文件名
  const disposition = resp.headers.get('Content-Disposition') || ''
  const match = disposition.match(/filename\*?=(?:UTF-8'')?([^;\s]+)/i)
  a.download = match ? decodeURIComponent(match[1]) : (filename || 'export.xlsx')
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(a.href)
}