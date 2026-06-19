<template>
  <div class="page-card">
    <div class="page-title">我负责的学生</div>
    <el-table v-loading="loading" :data="rows" border stripe style="width: 100%">
      <el-table-column type="index" width="60" label="#" />
      <el-table-column prop="studentNo" label="学号" width="130" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="className" label="班级" />
      <el-table-column prop="major" label="专业" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column label="实习状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.internStatus)" size="small">
            {{ statusLabel(row.internStatus) }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading" class="muted footer">
      共 {{ rows.length }} 名学生
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { req } from '@/utils/http'

const loading = ref(true)
const rows = ref([])

const STATUS_MAP = {
  ACTIVE: { label: '实习中', type: 'success' },
  SUSPEND: { label: '已暂停', type: 'warning' },
  FINISHED: { label: '已结束', type: 'info' },
  QUIT: { label: '已退出', type: 'danger' }
}
function statusLabel(s) {
  return STATUS_MAP[s]?.label || s
}
function statusType(s) {
  return STATUS_MAP[s]?.type || 'info'
}

onMounted(async () => {
  try {
    rows.value = await req('get', '/mentor/my-students')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.footer {
  margin-top: 12px;
  text-align: right;
}
</style>
