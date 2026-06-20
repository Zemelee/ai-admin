<template>
  <div class="page">
    <div class="page-card">
      <div class="card-head">
        <div class="card-title">学生实习日志</div>
        <div class="card-tools">
          <el-input
            v-model="keyword"
            placeholder="搜索学生姓名/学号"
            clearable
            style="width: 200px"
            :prefix-icon="Search"
          />
          <el-select v-model="onlySensitive" placeholder="全部" clearable style="width: 140px">
            <el-option label="仅敏感词" :value="1" />
          </el-select>
          <el-button :icon="Refresh" @click="loadList">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="sensitiveCount > 0"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      >
        检出 <b>{{ sensitiveCount }}</b> 条日志含敏感词（用工红线），请重点关注并跟进处置。
      </el-alert>

      <el-table :data="filteredList" v-loading="loading" border stripe style="width: 100%">
        <el-table-column label="学生" min-width="130">
          <template #default="{ row }">
            <div>{{ row.studentName || '—' }}</div>
            <div class="muted">{{ row.studentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="日期" width="120">
          <template #default="{ row }">{{ fmtDate(row.logDate) }}</template>
        </el-table-column>
        <el-table-column prop="contentSummary" label="内容摘要" min-width="220" show-overflow-tooltip />
        <el-table-column label="敏感词" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.sensitiveHit" type="danger" size="small" effect="dark">命中</el-tag>
            <span v-else style="color:#c0c4cc">—</span>
          </template>
        </el-table-column>
        <el-table-column label="图片" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.attachmentCount" type="info" size="small">{{ row.attachmentCount }} 张</el-tag>
            <span v-else style="color:#c0c4cc">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status]" effect="light">{{ STATUS_LABEL[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ fmtDateTime(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openDetail(row.id)">查看</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无学生日志" />
        </template>
      </el-table>
    </div>

    <LogDetailDialog v-model="detailVisible" :log-id="currentId" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import LogDetailDialog from '@/views/internlog/LogDetailDialog.vue'
import { teacherLogs, STATUS_LABEL, STATUS_TAG, fmtDate, fmtDateTime } from '@/api/internshipLog'

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const onlySensitive = ref(null)
const detailVisible = ref(false)
const currentId = ref(null)

const sensitiveCount = computed(() => list.value.filter((r) => r.sensitiveHit).length)

const filteredList = computed(() => {
  let arr = list.value
  if (onlySensitive.value === 1) arr = arr.filter((r) => r.sensitiveHit)
  const kw = keyword.value.trim()
  if (kw) {
    arr = arr.filter(
      (r) => (r.studentName || '').includes(kw) || (r.studentNo || '').includes(kw)
    )
  }
  return arr
})

async function loadList() {
  loading.value = true
  try {
    list.value = await teacherLogs()
  } finally {
    loading.value = false
  }
}

function openDetail(id) {
  currentId.value = id
  detailVisible.value = true
}

onMounted(loadList)
</script>

<style scoped>
.page { padding: 16px; }
.page-card {
  background: #fff;
  border-radius: 6px;
  padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.card-title { font-size: 16px; font-weight: 600; }
.card-tools { display: flex; gap: 12px; }
.muted { color: #909399; font-size: 12px; }
</style>
