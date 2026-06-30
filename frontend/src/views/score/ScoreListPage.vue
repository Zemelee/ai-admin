<template>
  <div class="page">
    <!-- 统计卡 -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">学生总数</div>
        <div class="stat-num">{{ stats.total }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #67c23a">
        <div class="stat-label">优秀（≥90）</div>
        <div class="stat-num">{{ stats.excellent }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #409eff">
        <div class="stat-label">良好（≥80）</div>
        <div class="stat-num">{{ stats.good }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #e6a23c">
        <div class="stat-label">平均分</div>
        <div class="stat-num">{{ stats.avg }}</div>
      </div>
    </div>

    <div class="page-card">
      <div class="card-head">
        <div class="card-title">{{ title }}</div>
        <div class="card-tools">
          <el-button :icon="Refresh" @click="loadList">刷新</el-button>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column label="排名" width="70" align="center" type="index" />
        <el-table-column label="学生" min-width="150">
          <template #default="{ row }">
            <div>{{ row.studentName || '—' }}</div>
            <div class="muted">{{ row.studentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" width="120" show-overflow-tooltip />
        <el-table-column prop="major" label="专业" min-width="120" show-overflow-tooltip />
        <el-table-column label="实习单位" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.companyName || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.internStatus || '—' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="日志(25%)" width="110" align="center">
          <template #default="{ row }">
            <div>{{ row.logSubmitted }}/{{ row.logExpected }}</div>
            <div class="score-text">{{ fmtScore(row.logScore) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="周记(25%)" width="110" align="center">
          <template #default="{ row }">
            <div v-if="row.weeklyAvg">均{{ row.weeklyAvg }}分</div>
            <div v-else class="muted">—</div>
            <div class="score-text">{{ fmtScore(row.weeklyScore) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="报告(20%)" width="110" align="center">
          <template #default="{ row }">
            <div v-if="row.reportAvg">均{{ row.reportAvg }}分</div>
            <div v-else class="muted">—</div>
            <div class="score-text">{{ fmtScore(row.reportScore) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="鉴定(30%)" width="110" align="center">
          <template #default="{ row }">
            <div v-if="row.evalSubmitted">{{ row.evalScore5 }}分</div>
            <div v-else class="muted">未提交</div>
            <div class="score-text">{{ fmtScore(row.evalScore) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="综合成绩" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <div class="total-score">{{ fmtScore(row.totalScore) }}</div>
            <el-tag :type="GRADE_TAG[row.grade]" size="small" style="margin-top:4px">{{ row.grade }}</el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无成绩数据" />
        </template>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { GRADE_TAG, fmtScore } from '@/api/score'

const props = defineProps({ title: { type: String, default: '实习成绩汇总' }, fetcher: { type: Function, required: true } })

const list = ref([])
const loading = ref(false)

const stats = computed(() => {
  const valid = list.value.filter(s => s.grade !== '无法评定' && s.totalScore > 0)
  const total = list.value.length
  const excellent = valid.filter(s => s.totalScore >= 90).length
  const good = valid.filter(s => s.totalScore >= 80 && s.totalScore < 90).length
  const avg = valid.length ? (valid.reduce((sum, s) => sum + Number(s.totalScore), 0) / valid.length).toFixed(1) : '—'
  return { total, excellent, good, avg }
})

async function loadList() {
  loading.value = true
  try {
    list.value = await props.fetcher()
  } finally {
    loading.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.page { padding: 16px; }
.page-card { background: #fff; border-radius: 6px; padding: 16px 20px; box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06); }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.card-title { font-size: 16px; font-weight: 600; }
.muted { color: #909399; font-size: 12px; }
.score-text { font-size: 12px; color: #909399; margin-top: 2px; }
.total-score { font-size: 18px; font-weight: 600; color: #409eff; }

.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px; }
.stat-card { background: #fff; border-radius: 6px; padding: 16px 20px; border-left: 4px solid #409eff; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.stat-label { font-size: 13px; color: #909399; }
.stat-num { font-size: 24px; font-weight: 600; color: #303133; margin-top: 6px; }

@media (max-width: 900px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
