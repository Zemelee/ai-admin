<template>
  <div class="page">
    <div class="page-card">
      <div class="card-head">
        <div class="card-title">{{ title }}</div>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width:100%">
        <el-table-column label="学生" min-width="150">
          <template #default="{ row }">
            <div>{{ row.studentName || '—' }}</div>
            <div class="muted">{{ row.studentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" width="120" show-overflow-tooltip />
        <el-table-column prop="major" label="专业" min-width="120" show-overflow-tooltip />
        <el-table-column label="出勤" width="70" align="center">
          <template #default="{ row }"><b>{{ row.scoreAttendance || '—' }}</b></template>
        </el-table-column>
        <el-table-column label="能力" width="70" align="center">
          <template #default="{ row }"><b>{{ row.scoreAbility || '—' }}</b></template>
        </el-table-column>
        <el-table-column label="态度" width="70" align="center">
          <template #default="{ row }"><b>{{ row.scoreAttitude || '—' }}</b></template>
        </el-table-column>
        <el-table-column label="综合" width="110" align="center">
          <template #default="{ row }">
            <el-rate v-if="row.scoreOverall" :model-value="row.scoreOverall" disabled size="small" />
            <span v-else style="color:#c0c4cc">—</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ fmtDateTime(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openDetail(row.id)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无已提交的鉴定" />
        </template>
      </el-table>
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="实习鉴定详情" width="640px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="学生">{{ detail.studentName }}（{{ detail.studentNo }}）</el-descriptions-item>
            <el-descriptions-item label="实习单位">{{ detail.companyName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="班级">{{ detail.className || '—' }}</el-descriptions-item>
            <el-descriptions-item label="专业">{{ detail.major || '—' }}</el-descriptions-item>
            <el-descriptions-item label="企业指导">{{ detail.mentorName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ fmtDateTime(detail.submitTime) }}</el-descriptions-item>
          </el-descriptions>
          <div class="score-grid">
            <div class="score-item"><div class="score-label">出勤与纪律</div><el-rate :model-value="detail.scoreAttendance" disabled show-text :texts="SCORE_TEXTS" /></div>
            <div class="score-item"><div class="score-label">专业能力</div><el-rate :model-value="detail.scoreAbility" disabled show-text :texts="SCORE_TEXTS" /></div>
            <div class="score-item"><div class="score-label">工作态度</div><el-rate :model-value="detail.scoreAttitude" disabled show-text :texts="SCORE_TEXTS" /></div>
            <div class="score-item"><div class="score-label">综合评价</div><el-rate :model-value="detail.scoreOverall" disabled show-text :texts="SCORE_TEXTS" /></div>
          </div>
          <div class="block-title">鉴定评语</div>
          <div class="comment-box">{{ detail.comment || '（企业指导未填写评语）' }}</div>
        </template>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { evalDetail, SCORE_TEXTS, fmtDateTime } from '@/api/companyEval'

const props = defineProps({ title: { type: String, default: '学生鉴定' }, fetcher: { type: Function, required: true } })

const list = ref([])
const loading = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

async function loadList() {
  loading.value = true
  try {
    list.value = await props.fetcher()
  } finally {
    loading.value = false
  }
}

async function openDetail(id) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await evalDetail(id)
  } finally {
    detailLoading.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.page { padding: 16px; }
.page-card { background: #fff; border-radius: 6px; padding: 16px 20px; box-shadow: 0 1px 4px rgba(0,21,41,0.06); }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.card-title { font-size: 16px; font-weight: 600; }
.muted { color: #909399; font-size: 12px; }
.score-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 16px; }
.score-item { background: #f5f7fa; border-radius: 6px; padding: 14px 16px; }
.score-label { font-size: 13px; color: #909399; margin-bottom: 8px; }
.block-title { margin: 20px 0 10px; font-size: 15px; font-weight: 600; border-left: 3px solid #409eff; padding-left: 8px; }
.comment-box { background: #f5f7fa; border-radius: 6px; padding: 14px 16px; line-height: 1.8; color: #333; white-space: pre-wrap; }
@media (max-width: 760px) { .score-grid { grid-template-columns: 1fr; } }
</style>
