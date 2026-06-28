<template>
  <div class="page">
    <div class="page-card">
      <div class="card-head">
        <div class="card-title">报告评分</div>
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
        <el-table-column label="报告类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="REPORT_TYPE_TAG[row.reportType]" size="small" effect="light">
              {{ REPORT_TYPE_LABEL[row.reportType] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="报告标题" min-width="200" show-overflow-tooltip />
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
          <el-empty description="暂无报告" />
        </template>
      </el-table>
    </div>

    <!-- 评分对话框 -->
    <el-dialog v-model="reviewVisible" title="报告评分" width="700px" @close="resetReview">
      <div v-loading="loadingDetail">
        <el-descriptions :column="2" border v-if="detail">
          <el-descriptions-item label="学生">{{ detail.studentName }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ detail.studentNo }}</el-descriptions-item>
          <el-descriptions-item label="报告类型">
            <el-tag :type="REPORT_TYPE_TAG[detail.reportType]" size="small" effect="light">
              {{ REPORT_TYPE_LABEL[detail.reportType] }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ fmtDateTime(detail.submitTime) }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="detail" class="section">
          <div class="label">报告标题</div>
          <div class="content-title">{{ detail.title }}</div>
        </div>
        <div v-if="detail" class="section">
          <div class="label">报告内容</div>
          <div class="content" v-html="renderContent(detail.content)" />
        </div>
        <div v-if="detail && detail.attachments && detail.attachments.length" class="section">
          <div class="label">图片附件</div>
          <ImageUploader :model-value="detail.attachments" biz-type="REPORT" readonly />
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

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="报告详情" width="700px" @close="resetDetail">
      <div v-loading="detailLoading" class="detail-main" v-if="detail">
        <div class="detail-header">
          <el-tag :type="REPORT_TYPE_TAG[detail.reportType]" size="small" effect="light" style="margin-right:8px">
            {{ REPORT_TYPE_LABEL[detail.reportType] }}
          </el-tag>
          <h3 class="detail-title">{{ detail.title }}</h3>
        </div>
        <div v-if="detail.attachments && detail.attachments.length" class="detail-attachments">
          <ImageUploader :model-value="detail.attachments" biz-type="REPORT" readonly />
        </div>
        <div class="detail-content-body" v-html="renderContent(detail.content)" />
        <div v-if="detail.teacherScore !== null" class="detail-score">
          <div class="score-label">教师评分：</div>
          <el-rate v-model="detail.teacherScore" disabled size="small" />
        </div>
        <div v-if="detail.teacherComment" class="detail-comment">
          <div class="comment-label">教师评语：</div>
          <p class="comment-text">{{ detail.teacherComment }}</p>
        </div>
        <div class="detail-meta">
          <span>提交时间：{{ fmtDateTime(detail.submitTime) }}</span>
          <span v-if="detail.teacherReviewTime" style="margin-left:20px">
            评阅时间：{{ fmtDateTime(detail.teacherReviewTime) }}
          </span>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import ImageUploader from '@/components/ImageUploader.vue'
import {
  teacherPendingReports, teacherAllReports, teacherReviewReport, reportDetail,
  REPORT_TYPE_LABEL, REPORT_TYPE_TAG,
  STATUS_LABEL, STATUS_TAG, fmtDateTime
} from '@/api/report'

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
const detailLoading = ref(false)
const detailInfo = ref(null)

function renderContent(content) {
  if (!content) return ''
  let html = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`{3}([\s\S]*?)`{3}/g, '<pre><code>$1</code></pre>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
  return html
}

async function loadList() {
  loading.value = true
  try {
    list.value = tab.value === 'pending' ? await teacherPendingReports() : await teacherAllReports()
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
    detail.value = await reportDetail(row.id)
  } finally {
    loadingDetail.value = false
  }
}

function openDetail(id) {
  currentId.value = id
  loadDetail(id)
  detailVisible.value = true
}

async function loadDetail(id) {
  detailLoading.value = true
  try {
    detailInfo.value = await reportDetail(id)
  } finally {
    detailLoading.value = false
  }
}

function resetReview() {
  detail.value = null
  form.result = 'REVIEWED'
  form.score = 5
  form.comment = ''
}

function resetDetail() {
  detailInfo.value = null
}

async function onReview() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    await teacherReviewReport(currentId.value, {
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
.label { font-size: 13px; color: #909399; margin-bottom: 6px; }
.content-title { font-size: 16px; font-weight: 600; margin: 8px 0; }
.content { background: #f5f7fa; border-radius: 6px; padding: 10px 12px; font-size: 14px; line-height: 1.6; white-space: pre-wrap; }
.detail-main { padding: 16px 0; }
.detail-header { display: flex; align-items: center; margin-bottom: 12px; }
.detail-title { font-size: 18px; font-weight: 600; margin: 0; flex: 1; }
.detail-attachments { margin: 12px 0; }
.detail-content-body { line-height: 1.8; color: #333; margin: 16px 0; white-space: pre-wrap; }
.detail-score { margin: 16px 0; display: flex; align-items: center; gap: 8px; }
.score-label { font-weight: 500; }
.detail-comment { margin: 16px 0; padding: 12px; background: #f5f7fa; border-radius: 4px; }
.comment-label { font-weight: 500; margin-bottom: 4px; }
.comment-text { margin: 0; line-height: 1.6; color: #555; }
.detail-meta { margin-top: 16px; padding-top: 12px; border-top: 1px solid #eee; font-size: 12px; color: #909399; }
.detail-meta span { margin-right: 20px; }
</style>
