<template>
  <div class="page">
    <div class="page-card" v-loading="loading">
      <div class="card-head">
        <div class="card-title">我的实习鉴定</div>
      </div>

      <el-empty v-if="!loading && !info" description="企业指导尚未提交实习鉴定" />

      <template v-else-if="info">
        <el-descriptions :column="2" border title="基本信息">
          <el-descriptions-item label="学生">{{ info.studentName }}（{{ info.studentNo }}）</el-descriptions-item>
          <el-descriptions-item label="实习单位">{{ info.companyName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="班级">{{ info.className || '—' }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ info.major || '—' }}</el-descriptions-item>
          <el-descriptions-item label="实习起止">{{ info.internStart || '—' }} ~ {{ info.internEnd || '—' }}</el-descriptions-item>
          <el-descriptions-item label="鉴定状态">
            <el-tag :type="STATUS_TAG[info.status]" size="small">{{ STATUS_LABEL[info.status] }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="企业指导">{{ info.mentorName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ fmtDateTime(info.submitTime) }}</el-descriptions-item>
        </el-descriptions>

        <div class="block-title">企业鉴定评分</div>
        <div class="score-grid">
          <div class="score-item">
            <div class="score-label">出勤与纪律</div>
            <el-rate :model-value="info.scoreAttendance" disabled show-text :texts="SCORE_TEXTS" />
          </div>
          <div class="score-item">
            <div class="score-label">专业能力</div>
            <el-rate :model-value="info.scoreAbility" disabled show-text :texts="SCORE_TEXTS" />
          </div>
          <div class="score-item">
            <div class="score-label">工作态度</div>
            <el-rate :model-value="info.scoreAttitude" disabled show-text :texts="SCORE_TEXTS" />
          </div>
          <div class="score-item">
            <div class="score-label">综合评价</div>
            <el-rate :model-value="info.scoreOverall" disabled show-text :texts="SCORE_TEXTS" />
          </div>
        </div>

        <div class="block-title">鉴定评语</div>
        <div class="comment-box">{{ info.comment || '（企业指导未填写评语）' }}</div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { myAppraisal, STATUS_LABEL, STATUS_TAG, SCORE_TEXTS, fmtDateTime } from '@/api/companyEval'

const info = ref(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    info.value = await myAppraisal()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { padding: 16px; }
.page-card { background: #fff; border-radius: 6px; padding: 16px 20px; box-shadow: 0 1px 4px rgba(0,21,41,0.06); }
.card-head { margin-bottom: 16px; }
.card-title { font-size: 16px; font-weight: 600; }
.block-title { margin: 24px 0 12px; font-size: 15px; font-weight: 600; border-left: 3px solid #409eff; padding-left: 8px; }
.score-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.score-item { background: #f5f7fa; border-radius: 6px; padding: 14px 16px; }
.score-label { font-size: 13px; color: #909399; margin-bottom: 8px; }
.comment-box { background: #f5f7fa; border-radius: 6px; padding: 14px 16px; line-height: 1.8; color: #333; white-space: pre-wrap; }
@media (max-width: 760px) { .score-grid { grid-template-columns: 1fr; } }
</style>
