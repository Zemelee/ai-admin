<template>
  <div class="loading">
    <el-icon class="is-loading" :size="32"><Loading /></el-icon>
    <span>正在为您准备工作台…</span>
  </div>
</template>

<script setup>
import { Loading } from '@element-plus/icons-vue'
</script>

<style scoped>
.loading {
  height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #909399;
}
</style>
