<template>
  <div class="page">
    <div class="page-card">
      <div class="card-head">
        <div class="card-title">我的实习报告</div>
        <el-button type="primary" :icon="Plus" @click="openSubmit">提交报告</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
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
        <el-table-column label="评分" width="120" align="center">
          <template #default="{ row }">
            <el-rate v-if="row.teacherScore" :model-value="row.teacherScore" disabled size="small" />
            <span v-else style="color:#c0c4cc">—</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ fmtDateTime(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="goDetail(row.id)">详情</el-button>
            <el-button v-if="row.status === 'SUBMITTED'" size="small" link type="warning" @click="openEdit(row.id)">编辑</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无报告记录，点击右上角提交" />
        </template>
      </el-table>
    </div>

    <!-- 提交/编辑对话框 -->
    <el-dialog
      v-model="formVisible"
      :title="editingId ? '编辑报告' : '提交实习报告'"
      width="700px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px">
        <template #title>
          提交要求：报告内容应不少于 50 字，需真实反映实习情况。中期报告在实习中期提交，终期报告在实习结束时提交。
        </template>
      </el-alert>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" v-loading="editLoading">
        <el-form-item label="报告类型" prop="reportType">
          <el-radio-group v-model="form.reportType">
            <el-radio value="MID_TERM">中期报告</el-radio>
            <el-radio value="FINAL">终期报告</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="报告标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="如：实习中期总结报告" />
        </el-form-item>
        <el-form-item label="报告内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" maxlength="5000" show-word-limit placeholder="50~5000 字，详细描述实习内容、收获与体会" />
        </el-form-item>
        <el-form-item label="图片附件">
          <ImageUploader v-model="attachments" biz-type="REPORT" :max="6" />
          <div class="muted">建议上传：实习现场照片、工作成果截图等</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ editingId ? '保存修改' : '提交' }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="报告详情"
      width="700px"
      :close-on-click-modal="false"
    >
      <div v-loading="detailLoading" class="detail-content">
        <div v-if="detailInfo" class="detail-main">
          <div class="detail-header">
            <el-tag :type="REPORT_TYPE_TAG[detailInfo.reportType]" size="small" effect="light" style="margin-right:8px">
              {{ REPORT_TYPE_LABEL[detailInfo.reportType] }}
            </el-tag>
            <h3 class="detail-title">{{ detailInfo.title }}</h3>
          </div>
          <div v-if="detailInfo.attachments && detailInfo.attachments.length" class="detail-attachments">
            <ImageUploader :model-value="detailInfo.attachments" biz-type="REPORT" readonly />
          </div>
          <div class="detail-content-body" v-html="renderContent(detailInfo.content)" />
          <div v-if="detailInfo.teacherScore !== null" class="detail-score">
            <div class="score-label">教师评分：</div>
            <el-rate v-model="detailInfo.teacherScore" disabled size="small" />
          </div>
          <div v-if="detailInfo.teacherComment" class="detail-comment">
            <div class="comment-label">教师评语：</div>
            <p class="comment-text">{{ detailInfo.teacherComment }}</p>
          </div>
          <div class="detail-meta">
            <span>提交时间：{{ fmtDateTime(detailInfo.submitTime) }}</span>
            <span v-if="detailInfo.teacherReviewTime" style="margin-left:20px">
              评阅时间：{{ fmtDateTime(detailInfo.teacherReviewTime) }}
            </span>
          </div>
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import ImageUploader from '@/components/ImageUploader.vue'
import {
  myReports, submitReport, updateReport,
  reportDetail,
  REPORT_TYPE_LABEL, REPORT_TYPE_TAG,
  STATUS_LABEL, STATUS_TAG, fmtDateTime
} from '@/api/report'

const router = useRouter()

const list = ref([])
const loading = ref(false)

const formVisible = ref(false)
const submitting = ref(false)
const editLoading = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const attachments = ref([])
const form = reactive(defaultForm())
const rules = {
  reportType: [{ required: true, message: '请选择报告类型', trigger: 'change' }],
  title: [
    { required: true, message: '请填写报告标题', trigger: 'blur' },
    { min: 3, max: 100, message: '标题长度 3~100 字', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请填写报告内容', trigger: 'blur' },
    { min: 50, max: 5000, message: '内容长度 50~5000 字', trigger: 'blur' }
  ]
}

const detailVisible = ref(false)
const currentId = ref(null)
const detailLoading = ref(false)
const detailInfo = ref(null)

function defaultForm() {
  return { reportType: 'MID_TERM', title: '', content: '' }
}

function renderContent(content) {
  // 简单的 Markdown 转 HTML（实际可使用 marked 等库）
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
    list.value = await myReports()
  } finally {
    loading.value = false
  }
}

function openSubmit() {
  editingId.value = null
  attachments.value = []
  Object.assign(form, defaultForm())
  formVisible.value = true
}

async function openEdit(id) {
  editingId.value = id
  editLoading.value = true
  formVisible.value = true
  try {
    const d = await reportDetail(id)
    form.reportType = d.reportType
    form.title = d.title
    form.content = d.content
    attachments.value = d.attachments || []
  } finally {
    editLoading.value = false
  }
}

function goDetail(id) {
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

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, defaultForm())
  attachments.value = []
  editingId.value = null
}

async function onSubmit() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    const payload = {
      reportType: form.reportType,
      title: form.title,
      content: form.content,
      attachmentIds: attachments.value.map((a) => a.id)
    }
    if (editingId.value) {
      await updateReport(editingId.value, payload)
      ElMessage.success('已修改')
    } else {
      await submitReport(payload)
      ElMessage.success('提交成功，等待教师评阅')
    }
    formVisible.value = false
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

.detail-content { max-height: 60vh; overflow-y: auto; }
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
