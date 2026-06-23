<template>
  <div class="page">
    <div class="page-card">
      <div class="card-head">
        <div class="card-title">我的实习周记</div>
        <el-button type="primary" :icon="Plus" @click="openSubmit">提交本周周记</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
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
        <el-table-column label="评分" width="120" align="center">
          <template #default="{ row }">
            <el-rate v-if="row.teacherScore" :model-value="row.teacherScore" disabled size="small" />
            <span v-else style="color:#c0c4cc">—</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ fmtDateTime(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openDetail(row.id)">详情</el-button>
            <el-button v-if="row.status === 'SUBMITTED'" size="small" link type="warning" @click="openEdit(row.id)">编辑</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无周记记录" />
        </template>
      </el-table>
    </div>

    <!-- 提交/编辑 -->
    <el-dialog
      v-model="formVisible"
      :title="editingId ? '编辑周记' : '提交周记'"
      width="640px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px" v-loading="editLoading">
        <el-form-item label="周期" prop="anyDate">
          <el-date-picker
            v-model="form.anyDate"
            type="week"
            placeholder="选择本周内任意日期"
            format="YYYY 第 ww 周"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledFuture"
            style="width: 100%"
            @change="onWeekChange"
          />
          <div v-if="form.yearWeek" class="muted" style="margin-top:4px">
            {{ form.yearWeek }} · {{ form.weekStart }} ~ {{ form.weekEnd }}
          </div>
        </el-form-item>
        <el-form-item label="本周总结" prop="summary">
          <el-input v-model="form.summary" type="textarea" :rows="5" maxlength="3000" show-word-limit placeholder="10~3000 字，描述本周完成工作、收获与问题" />
        </el-form-item>
        <el-form-item label="下周计划">
          <el-input v-model="form.nextPlan" type="textarea" :rows="3" maxlength="2000" show-word-limit placeholder="可选" />
        </el-form-item>
        <el-form-item label="图片附件">
          <ImageUploader v-model="attachments" biz-type="WEEKLY" :max="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ editingId ? '保存修改' : '提交' }}</el-button>
      </template>
    </el-dialog>

    <WeeklyDetailDialog v-model="detailVisible" :weekly-id="currentId" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import ImageUploader from '@/components/ImageUploader.vue'
import WeeklyDetailDialog from '@/views/weekly/WeeklyDetailDialog.vue'
import {
  myWeeklies, submitWeekly, updateWeekly, weeklyDetail,
  STATUS_LABEL, STATUS_TAG, isoWeekOf, fmtDate, fmtDateTime
} from '@/api/weekly'

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
  anyDate: [{ required: true, message: '请选择周期', trigger: 'change' }],
  summary: [
    { required: true, message: '请填写本周总结', trigger: 'blur' },
    { min: 10, max: 3000, message: '本周总结长度 10~3000 字', trigger: 'blur' }
  ]
}

const detailVisible = ref(false)
const currentId = ref(null)

function defaultForm() {
  return { anyDate: '', yearWeek: '', weekStart: '', weekEnd: '', summary: '', nextPlan: '' }
}

function disabledFuture(d) {
  return d.getTime() > Date.now()
}

function onWeekChange(v) {
  if (!v) {
    form.yearWeek = ''
    form.weekStart = ''
    form.weekEnd = ''
    return
  }
  const info = isoWeekOf(new Date(v))
  form.yearWeek = info.yearWeek
  form.weekStart = info.weekStart
  form.weekEnd = info.weekEnd
}

async function loadList() {
  loading.value = true
  try {
    list.value = await myWeeklies()
  } finally {
    loading.value = false
  }
}

function openSubmit() {
  editingId.value = null
  attachments.value = []
  Object.assign(form, defaultForm())
  // 默认本周
  const today = new Date()
  form.anyDate = todayStr()
  onWeekChange(today)
  formVisible.value = true
}

async function openEdit(id) {
  editingId.value = id
  editLoading.value = true
  formVisible.value = true
  try {
    const d = await weeklyDetail(id)
    form.anyDate = d.weekStart
    form.yearWeek = d.yearWeek
    form.weekStart = d.weekStart
    form.weekEnd = d.weekEnd
    form.summary = d.summary
    form.nextPlan = d.nextPlan || ''
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
  Object.assign(form, defaultForm())
  attachments.value = []
  editingId.value = null
}

async function onSubmit() {
  try { await formRef.value.validate() } catch { return }
  if (!form.yearWeek) {
    ElMessage.warning('请选择周期')
    return
  }
  submitting.value = true
  try {
    const payload = {
      yearWeek: form.yearWeek,
      weekStart: form.weekStart,
      weekEnd: form.weekEnd,
      summary: form.summary,
      nextPlan: form.nextPlan || null,
      attachmentIds: attachments.value.map((a) => a.id)
    }
    if (editingId.value) {
      await updateWeekly(editingId.value, payload)
      ElMessage.success('已修改')
    } else {
      await submitWeekly(payload)
      ElMessage.success('提交成功，等待教师评分')
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
.page-card { background: #fff; border-radius: 6px; padding: 16px 20px; box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06); }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.card-title { font-size: 16px; font-weight: 600; }
.muted { color: #909399; font-size: 12px; }
</style>
