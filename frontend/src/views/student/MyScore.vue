<template>
  <div class="page">
    <div class="page-card" v-loading="loading">
      <div class="card-head">
        <div class="card-title">我的实习成绩</div>
      </div>

      <el-empty v-if="!score" description="暂无成绩数据" />

      <template v-else>
        <!-- 基础信息 -->
        <el-descriptions :column="2" border title="基本信息">
          <el-descriptions-item label="学生">{{ score.studentName }}（{{ score.studentNo }}）</el-descriptions-item>
          <el-descriptions-item label="班级">{{ score.className || '—' }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ score.major || '—' }}</el-descriptions-item>
          <el-descriptions-item label="实习单位">{{ score.companyName || '—' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 四维成绩卡片 -->
        <div class="score-cards">
          <div class="score-card">
            <div class="score-name">日志（25%）</div>
            <div class="score-dim">{{ score.logSubmitted }}/{{ score.logExpected }}</div>
            <div class="score-val">{{ fmtScore(score.logScore) }}分</div>
          </div>
          <div class="score-card">
            <div class="score-name">周记（25%）</div>
            <div class="score-dim">均分{{ score.weeklyAvg }}</div>
            <div class="score-val">{{ fmtScore(score.weeklyScore) }}分</div>
          </div>
          <div class="score-card">
            <div class="score-name">报告（20%）</div>
            <div class="score-dim">均分{{ score.reportAvg }}</div>
            <div class="score-val">{{ fmtScore(score.reportScore) }}分</div>
          </div>
          <div class="score-card">
            <div class="score-name">企业鉴定（30%）</div>
            <div class="score-dim">{{ score.evalSubmitted ? score.evalScore5 + '分' : '未提交' }}</div>
            <div class="score-val">{{ fmtScore(score.evalScore) }}分</div>
          </div>
        </div>

        <!-- 总分 -->
        <div class="total-box">
          <div class="total-label">综合成绩</div>
          <div class="total-score">{{ fmtScore(score.totalScore) }}</div>
          <el-tag :type="GRADE_TAG[score.grade]" size="large" style="margin-left: 16px">{{ score.grade }}</el-tag>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { myScore, GRADE_TAG, fmtScore } from '@/api/score'

const loading = ref(false)
const score = ref(null)

async function load() {
  loading.value = true
  try {
    score.value = await myScore()
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

.score-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin: 24px 0; }
.score-card { background: #f5f7fa; border-radius: 6px; padding: 16px; text-align: center; }
.score-name { font-size: 13px; color: #909399; margin-bottom: 8px; }
.score-dim { font-size: 14px; color: #606266; margin-bottom: 4px; }
.score-val { font-size: 22px; font-weight: 600; color: #409eff; }

.total-box { display: flex; align-items: center; justify-content: center; padding: 24px; background: linear-gradient(135deg, #ecf5ff 0%, #d9ecff 100%); border-radius: 6px; }
.total-label { font-size: 16px; font-weight: 500; margin-right: 12px; }
.total-score { font-size: 36px; font-weight: 700; color: #409eff; }

@media (max-width: 900px) {
  .score-cards { grid-template-columns: repeat(2, 1fr); }
}
</style>
