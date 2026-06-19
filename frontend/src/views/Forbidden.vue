<template>
  <el-result icon="warning" title="403" sub-title="您没有访问该页面的权限">
    <template #extra>
      <el-button type="primary" @click="back">返回首页</el-button>
    </template>
  </el-result>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { homePathOf } from '@/router/menus'

const router = useRouter()
const user = useUserStore()

function back() {
  if (user.role) {
    router.replace(homePathOf(user.role))
  } else {
    router.replace('/login')
  }
}
</script>
