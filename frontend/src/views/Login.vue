<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <div class="logo-circle">实</div>
        <h1>实习管理系统</h1>
        <p class="muted">旅游管理系 · 实习生过程管理 AI 智能体（MVP）</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="onSubmit"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>

        <el-button
          type="primary"
          native-type="submit"
          :loading="loading"
          style="width: 100%"
          size="large"
          @click="onSubmit"
        >
          登 录
        </el-button>
      </el-form>

      <el-divider>测试账号（密码统一 123456）</el-divider>
      <div class="quick-login">
        <el-tag
          v-for="q in QUICK"
          :key="q.username"
          :type="q.tag"
          effect="plain"
          class="quick-tag"
          @click="quickFill(q.username)"
        >
          {{ q.label }} · {{ q.username }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { homePathOf } from '@/router/menus'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const QUICK = [
  { username: 'supervisor01', label: '监管者', tag: 'danger' },
  { username: 'teacher01', label: '教师', tag: 'warning' },
  { username: 'student01', label: '学生', tag: 'primary' },
  { username: 'mentor01', label: '企业导师', tag: 'success' }
]

function quickFill(u) {
  form.username = u
  form.password = '123456'
}

async function onSubmit() {
  if (!formRef.value) return
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return
  loading.value = true
  try {
    await user.login(form.username.trim(), form.password)
    await user.fetchMe()
    ElMessage.success(`欢迎，${user.info?.name}（${user.info?.roleLabel}）`)
    const redirect = route.query.redirect || (user.role ? homePathOf(user.role) : '/')
    router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 20% 20%, #d8eaff 0%, transparent 40%),
    radial-gradient(circle at 80% 70%, #e9f7ff 0%, transparent 40%),
    linear-gradient(135deg, #f0f5ff 0%, #ffffff 100%);
}
.login-card {
  width: 420px;
  background: #fff;
  border-radius: 12px;
  padding: 36px 36px 28px;
  box-shadow: 0 8px 32px rgba(64, 158, 255, 0.12);
}
.brand {
  text-align: center;
  margin-bottom: 24px;
}
.logo-circle {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 28px;
  font-weight: bold;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}
.brand h1 {
  font-size: 20px;
  margin: 6px 0 4px;
}
.quick-login {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}
.quick-tag {
  cursor: pointer;
}
</style>
