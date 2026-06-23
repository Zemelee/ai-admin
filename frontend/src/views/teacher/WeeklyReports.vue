<template>
  <div class="page">
    <div class="page-card">
      <div class="card-head">
        <div class="card-title">周记评分</div>
        <div class="card-tools">
          <el-radio-group v-model="tab" @change="loadList">
            <el-radio-button label="pending">待评分</el-radio-button>
            <el-radio-button label="all">全部</el-radio-button>
          </el-radio-group>
          <el-button :icon="Refresh" @click="loadList">刷新</el-button>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column label="学生" min-width="130">
          <template #default="{ row }">
            <div>{{ row.studentName || '—' }}</div>
            <div class="muted">{{ row.studentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="周编号" width="110" prop="yearWeek" />
        <el-table-column label="周期" width="200">
          <template #default="{ row }">{{ fmtDate(row.weekStart) }} ~ {{ fmtDate(row.weekEnd) }}</template>
        </el-table-column>
        <el-table-column prop="summarySummary" label="本周总结摘要" min-width="220" show-overflow-tooltip />
        <el-table-column label="敏感" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.sensitiveHit" type="danger" size="small" effect="dark">命中</el-tag>
            <span v-else style="color:#c0c4cc">—</span>
          </template>
        </el-table-column>
        <el-table-column label="图片" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.attachmentCount" type="info" size="small">{{ row.attachmentCount }}</el-tag>
            <span v-else style="color:#c0c4cc">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status]" effect="light">{{ STATUS_LABEL[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="130" align="center">
          <template #default="{ row }">
            <el-rate v-if="row.teacherScore" :model-value="row.teacherScore" disabled size="small" />
            <span v-else style="color:#c0c4cc">—</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ fmtDateTime(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openDetail(row.id)">查看</el-button>
            <el-button v-if="row.status === 'SUBMITTED'" size="small" link type="success" @click="openReview(row)">评分</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无周记" />
        </template>
      </el-table>
    </div>

    <!-- 评分对话框 -->
    <el-dialog v-model="reviewVisible" title="周记评分" width="640px" @close="resetReview">
      <div v-loading="loadingDetail">
        <el-descriptions :column="2" border v-if="detail">
          <el-descriptions-item label="学生">{{ detail.studentName }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ detail.studentNo }}</el-descriptions-item>
          <el-descriptions-item label="周编号">{{ detail.yearWeek }}</el-descriptions-item>
          <el-descriptions-item label="周期">{{ fmtDate(detail.weekStart) }} ~ {{ fmtDate(detail.weekEnd) }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="detail" class="section">
          <div class="label">
            本周总结
            <el-tag v-if="detail.sensitiveHit" type="danger" size="small" effect="dark" style="margin-left:8px">AI 检出敏感词</el-tag>
            <span v-if="detail.sensitiveWords" class="sensitive-words">命中：{{ detail.sensitiveWords }}</span>
          </div>
          <div v-if="detail.sensitiveHit && detail.sensitiveMarkedHtml" class="content sensitive" v-html="detail.sensitiveMarkedHtml" />
          <div v-else class="content">{{ detail.summary }}</div>
        </div>
        <div v-if="detail && detail.nextPlan" class="section">
          <div class="label">下周计划</div>
          <div class="content">{{ detail.nextPlan }}</div>
        </div>
        <div v-if="detail && detail.attachments && detail.attachments.length" class="section">
          <div class="label">图片附件</div>
          <ImageUploader :model-value="detail.attachments" biz-type="WEEKLY" readonly />
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="84px" style="margin-top:16px">
          <el-form-item label="评分结果" prop="result">
            <el-radio-group v-model="form.result">
              <el-radio value="REVIEWED">通过评分</el-radio>
              <el-radio value="REJECTED">驳回</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="form.result === 'REVIEWED'" label="评分" prop="score">
            <el-rate v-model="form.score" :max="5" show-text :texts="['较差', '一般', '合格', '良好', '优秀']" />
          </el-form-item>
          <el-form-item label="评语" prop="comment">
            <el-input
              v-model="form.comment"
              type="textarea"
              :rows="3"
              :placeholder="form.result === 'REJECTED' ? '驳回必须填写评语' : '可选'"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onReview">提交评分</el-button>
      </template>
    </el-dialog>

    <WeeklyDetailDialog v-model="detailVisible" :weekly-id="currentId" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import ImageUploader from '@/components/ImageUploader.vue'
import WeeklyDetailDialog from '@/views/weekly/WeeklyDetailDialog.vue'
import {
  teacherPendingWeeklies, teacherAllWeeklies, teacherReviewWeekly, weeklyDetail,
  STATUS_LABEL, STATUS_TAG, fmtDate, fmtDateTime
} from '@/api/weekly'

const tab = ref('pending')
const list = ref([])
const loading = ref(false)

const reviewVisible = ref(false)
const loadingDetail = ref(false)
const submitting = ref(false)
const detail = ref(null)
const currentId = ref(null)
const formRef = ref(null)
const form = reactive({ result: 'REVIEWED', score: 5, comment: '' })
const rules = {
  result: [{ required: true, message: '请选择评分结果', trigger: 'change' }],
  score: [
    {
      validator: (_r, _v, cb) => {
        if (form.result === 'REVIEWED' && !form.score) cb(new Error('请打分 1-5 星'))
        else cb()
      },
      trigger: 'change'
    }
  ],
  comment: [
    {
      validator: (_r, v, cb) => {
        if (form.result === 'REJECTED' && !v?.trim()) cb(new Error('驳回必须填写评语'))
        else cb()
      },
      trigger: 'blur'
    }
  ]
}

const detailVisible = ref(false)

async function loadList() {
  loading.value = true
  try {
    list.value = tab.value === 'pending' ? await teacherPendingWeeklies() : await teacherAllWeeklies()
  } finally {
    loading.value = false
  }
}

async function openReview(row) {
  currentId.value = row.id
  reviewVisible.value = true
  loadingDetail.value = true
  form.result = 'REVIEWED'
  form.score = 5
  form.comment = ''
  try {
    detail.value = await weeklyDetail(row.id)
  } finally {
    loadingDetail.value = false
  }
}

function openDetail(id) {
  currentId.value = id
  detailVisible.value = true
}

function resetReview() {
  detail.value = null
  form.result = 'REVIEWED'
  form.score = 5
  form.comment = ''
}

async function onReview() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    await teacherReviewWeekly(currentId.value, {
      result: form.result,
      score: form.result === 'REVIEWED' ? form.score : null,
      comment: form.comment
    })
    ElMessage.success(form.result === 'REVIEWED' ? '评分已提交' : '已驳回')
    reviewVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.page { padding: 16px; }
.page-card { background: #fff; border-radius: 6px; padding: 16px 20px; box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06); }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.card-title { font-size: 16px; font-weight: 600; }
.card-tools { display: flex; gap: 12px; }
.muted { color: #909399; font-size: 12px; }
.section { margin-top: 16px; }
.label { font-size: 13px; color: #909399; margin-bottom: 6px; display: flex; align-items: center; }
.sensitive-words { margin-left: 8px; color: #f56c6c; font-size: 12px; }
.content { background: #f5f7fa; border-radius: 6px; padding: 10px 12px; font-size: 14px; line-height: 1.6; white-space: pre-wrap; }
.content.sensitive :deep(.sensitive) { color: #fff; background: #f56c6c; border-radius: 3px; padding: 0 4px; font-weight: 600; }
</style>
