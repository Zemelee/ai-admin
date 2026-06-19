import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { setToken, clearToken, getToken, req } from '@/utils/http'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const info = ref(null)

  const isLogin = computed(() => !!token.value)
  const role = computed(() => info.value?.role ?? null)

  async function login(username, password) {
    const data = await req('post', '/auth/login', { username, password })
    token.value = data.token
    setToken(data.token)
    info.value = {
      userId: data.userId,
      username: data.username,
      name: data.name,
      role: data.role,
      roleLabel: data.roleLabel
    }
  }

  async function fetchMe() {
    const data = await req('get', '/auth/me')
    info.value = data
    return data
  }

  async function logout() {
    try {
      await req('post', '/auth/logout')
    } catch {
      /* ignore */
    } finally {
      token.value = null
      info.value = null
      clearToken()
    }
  }

  return { token, info, isLogin, role, login, fetchMe, logout }
})
