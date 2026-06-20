<template>
  <div class="page">
    <div class="page-card">
      <div class="card-head">
        <div class="card-title">日志确认</div>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column label="学生" min-width="120">
          <template #default="{ row }">
            <div>{{ row.studentName || '—' }}</div>
            <div class="muted">{{ row.studentNo }}</div>
          </template>
        </el-table-column>
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
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ fmtDateTime(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openConfirm(row.id)">确认</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无待确认日志" />
        </template>
      </el-table>
    </div>

    <!-- 确认对话框 -->
    <el-dialog v-model="dialogVisible" title="日志确认" width="640px" @close="reset">
      <div v-loading="loadingDetail">
        <el-descriptions :column="2" border v-if="detail">
          <el-descriptions-item label="学生">{{ detail.studentName }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ detail.studentNo }}</el-descriptions-item>
          <el-descriptions-item label="日期">{{ fmtDate(detail.logDate) }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ fmtDateTime(detail.submitTime) }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="detail" class="detail-section">
          <div class="detail-label">日志内容</div>
          <div class="detail-content">{{ detail.content }}</div>
        </div>
        <div v-if="detail && detail.attachments && detail.attachments.length" class="detail-section">
          <div class="detail-label">图片附件</div>
          <ImageUploader :model-value="detail.attachments" biz-type="LOG" readonly />
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="84px" style="margin-top:16px">
          <el-form-item label="确认结果" prop="result">
            <el-radio-group v-model="form.result">
              <el-radio value="CONFIRMED">通过</el-radio>
              <el-radio value="REJECTED">驳回</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="确认意见" prop="comment">
            <el-input
              v-model="form.comment"
              type="textarea"
              :rows="3"
              :placeholder="form.result === 'REJECTED' ? '驳回必须填写意见' : '可选，填写指导意见'"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onConfirm">提交确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import ImageUploader from '@/components/ImageUploader.vue'
import { mentorPendingLogs, mentorConfirmLog, logDetail, fmtDate, fmtDateTime } from '@/api/internshipLog'

const list = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const loadingDetail = ref(false)
const submitting = ref(false)
const detail = ref(null)
const formRef = ref(null)
const currentId = ref(null)
const form = reactive({ result: 'CONFIRMED', comment: '' })
const rules = {
  result: [{ required: true, message: '请选择确认结果', trigger: 'change' }],
  comment: [
    {
      validator: (_r, v, cb) => {
        if (form.result === 'REJECTED' && !v?.trim()) cb(new Error('驳回必须填写意见'))
        else cb()
      },
      trigger: 'blur'
    }
  ]
}

async function loadList() {
  loading.value = true
  try {
    list.value = await mentorPendingLogs()
  } finally {
    loading.value = false
  }
}

async function openConfirm(id) {
  currentId.value = id
  dialogVisible.value = true
  loadingDetail.value = true
  form.result = 'CONFIRMED'
  form.comment = ''
  try {
    detail.value = await logDetail(id)
  } finally {
    loadingDetail.value = false
  }
}

function reset() {
  detail.value = null
  currentId.value = null
}

async function onConfirm() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await mentorConfirmLog(currentId.value, {
      result: form.result,
      comment: form.comment
    })
    ElMessage.success(form.result === 'CONFIRMED' ? '已通过' : '已驳回')
    dialogVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
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
.muted { color: #909399; font-size: 12px; }
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
