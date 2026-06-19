import axios from 'axios'
import { ElMessage } from 'element-plus'

const TOKEN_KEY = 'ai-admin-token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}
export function setToken(t) {
  localStorage.setItem(TOKEN_KEY, t)
}
export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.set('satoken', token)
  }
  return config
})

// 响应拦截器：业务码非 0 全局提示并 reject；其余原样放行
http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body && body.code !== 0) {
      ElMessage.error(body.msg || `请求失败(${body.code})`)
      return Promise.reject(body)
    }
    return resp
  },
  (err) => {
    const status = err?.response?.status
    const msg = err?.response?.data?.msg || err?.message || '网络异常'
    if (status === 401) {
      ElMessage.warning('登录已失效，请重新登录')
      clearToken()
      if (location.hash.indexOf('/login') === -1) {
        location.hash = '#/login'
      }
    } else if (status === 403) {
      ElMessage.error('无权访问')
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(err)
  }
)

/** 解包业务体，调用方直接拿到 data */
export async function req(method, url, payload) {
  const config = method === 'get' || method === 'delete' ? { params: payload } : {}
  let r
  if (method === 'get') {
    r = await http.get(url, config)
  } else if (method === 'delete') {
    r = await http.delete(url, config)
  } else if (method === 'post') {
    r = await http.post(url, payload)
  } else {
    r = await http.put(url, payload)
  }
  return r.data.data
}

export default http
