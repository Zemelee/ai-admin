<template>
  <div class="page">
    <div class="page-card">
      <div class="card-head">
        <div class="card-title">我的实习日志</div>
        <div class="card-tools">
          <el-button type="primary" :icon="Plus" @click="openSubmit">提交今日日志</el-button>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column label="日期" width="120">
          <template #default="{ row }">{{ fmtDate(row.logDate) }}</template>
        </el-table-column>
        <el-table-column prop="contentSummary" label="内容摘要" min-width="220" show-overflow-tooltip />
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
        <el-table-column label="企业确认" width="160">
          <template #default="{ row }">{{ fmtDateTime(row.mentorReviewTime) || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openDetail(row.id)">详情</el-button>
            <el-button v-if="row.status === 'SUBMITTED'" size="small" link type="warning" @click="openEdit(row.id)">编辑</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无日志记录，点击右上角提交今日日志" />
        </template>
      </el-table>
    </div>

    <!-- 提交/编辑 对话框 -->
    <el-dialog
      v-model="formVisible"
      :title="editingId ? '编辑实习日志' : '提交实习日志'"
      width="600px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="84px" v-loading="editLoading">
        <el-form-item label="日志日期" prop="logDate">
          <el-date-picker
            v-model="form.logDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledFuture"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="日志内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="记录今日实习工作内容、收获与问题（5~2000 字）"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="图片附件">
          <ImageUploader v-model="attachments" biz-type="LOG" :max="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ editingId ? '保存修改' : '提交' }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <LogDetailDialog v-model="detailVisible" :log-id="currentId" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import ImageUploader from '@/components/ImageUploader.vue'
import LogDetailDialog from '@/views/internlog/LogDetailDialog.vue'
import {
  myLogs, submitLog, updateLog, logDetail,
  STATUS_LABEL, STATUS_TAG, fmtDate, fmtDateTime
} from '@/api/internshipLog'

const list = ref([])
const loading = ref(false)

const formVisible = ref(false)
const submitting = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const attachments = ref([])
const form = reactive({ logDate: '', content: '' })
const rules = {
  logDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  content: [
    { required: true, message: '请填写日志内容', trigger: 'blur' },
    { min: 5, max: 2000, message: '日志内容长度 5~2000 字', trigger: 'blur' }
  ]
}

const detailVisible = ref(false)
const currentId = ref(null)
const editLoading = ref(false)

function disabledFuture(d) {
  return d.getTime() > Date.now()
}

async function loadList() {
  loading.value = true
  try {
    list.value = await myLogs()
  } finally {
    loading.value = false
  }
}

function openSubmit() {
  editingId.value = null
  attachments.value = []
  form.logDate = todayStr()
  form.content = ''
  formVisible.value = true
}

async function openEdit(id) {
  editingId.value = id
  editLoading.value = true
  formVisible.value = true
  try {
    const d = await logDetail(id)
    form.logDate = fmtDate(d.logDate)
    form.content = d.content
    attachments.value = d.attachments || []
  } finally {
    editLoading.value = false
  }
}

function openDetail(id) {
  currentId.value = id
  detailVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  form.logDate = ''
  form.content = ''
  attachments.value = []
  editingId.value = null
}

async function onSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const payload = {
      logDate: form.logDate,
      content: form.content,
      attachmentIds: attachments.value.map((a) => a.id)
    }
    if (editingId.value) {
      await updateLog(editingId.value, payload)
      ElMessage.success('已修改')
    } else {
      await submitLog(payload)
      ElMessage.success('提交成功，等待企业指导确认')
    }
    formVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

function todayStr() {
  const d = new Date()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mm}-${dd}`
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
.detail-section { margin-top: 16px; }
.detail-label { font-size: 13px; color: #909399; margin-bottom: 6px; }
.detail-content {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
