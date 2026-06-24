<template>
  <div class="page">
    <div class="page-card">
      <div class="card-head">
        <div class="card-title">我的单位变更</div>
        <div class="card-tools">
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px" @change="loadList">
            <el-option label="审批中" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="已撤回" value="CANCELLED" />
          </el-select>
          <el-button type="primary" :icon="Plus" @click="openSubmit">发起变更</el-button>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column label="原单位" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.fromCompanyName || '—' }}</template>
        </el-table-column>
        <el-table-column label="新单位" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.toCompanyName || '—' }}</span>
            <el-tag v-if="!row.toCompanyId" size="small" type="info" style="margin-left:6px">外部</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预计入职" width="110">
          <template #default="{ row }">{{ fmtDate(row.expectedStart) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="变更原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status]" effect="light">{{ STATUS_LABEL[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前节点" width="130" align="center">
          <template #default="{ row }">
            <span v-if="row.status === 'PENDING'">{{ NODE_LABEL[row.currentNode] || row.currentNode }}</span>
            <span v-else style="color:#999">—</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ fmtDateTime(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="goDetail(row.id)">详情</el-button>
            <el-popconfirm
              v-if="row.status === 'PENDING'"
              title="确认撤回该变更申请？"
              @confirm="onCancel(row.id)"
            >
              <template #reference>
                <el-button size="small" link type="danger">撤回</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无变更记录，点击右上角发起一条" />
        </template>
      </el-table>
    </div>

    <!-- 发起变更对话框 -->
    <el-dialog v-model="dialogVisible" title="发起单位变更" width="640px" :close-on-click-modal="false" @close="resetForm">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px">
        <template #title>
          单位变更需满足学院规定：原单位实习时长 ≥ 1 个月（总实习期 ≤ 半年）或 ≥ 3 个月（总实习期超过半年）；新单位不得在禁入清单中。
        </template>
      </el-alert>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="新单位来源">
          <el-radio-group v-model="form.companySource">
            <el-radio value="INTERNAL">系统内已登记企业</el-radio>
            <el-radio value="EXTERNAL">外部新企业（填名称）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.companySource === 'INTERNAL'" label="新单位" prop="toCompanyId">
          <el-select
            v-model="form.toCompanyId"
            placeholder="选择新单位"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="c in companies"
              :key="c.id"
              :label="c.name"
              :value="c.id"
              :disabled="c.id === currentCompanyId"
            >
              <span>{{ c.name }}</span>
              <span v-if="c.id === currentCompanyId" style="color:#c0c4cc;margin-left:6px;font-size:12px">
                （当前单位）
              </span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item v-else label="新单位名称" prop="toCompanyName">
          <el-input v-model="form.toCompanyName" maxlength="128" show-word-limit placeholder="请填写完整工商登记名称" />
        </el-form-item>

        <el-form-item label="预计入职日期" prop="expectedStart">
          <el-date-picker
            v-model="form.expectedStart"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledPast"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="变更原因" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="4"
            placeholder="详细说明变更原因（不少于 5 字，最多 1000 字）"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="佐证材料">
          <ImageUploader v-model="attachments" biz-type="TRANSFER" :max="6" />
          <div class="muted">建议上传：新单位接收函、家长同意书、原单位离岗证明等</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">提交申请</el-button>
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
  myTransfers, submitTransfer, cancelTransfer,
  STATUS_LABEL, STATUS_TAG, NODE_LABEL, fmtDate, fmtDateTime
} from '@/api/transfer'
import { req as httpReq } from '@/utils/http'

const router = useRouter()

const list = ref([])
const loading = ref(false)
const statusFilter = ref('')
const companies = ref([])
const currentCompanyId = ref(null)

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const attachments = ref([])
const form = reactive(defaultForm())

const rules = {
  toCompanyId: [{
    validator: (_r, _v, cb) => {
      if (form.companySource === 'INTERNAL' && !form.toCompanyId) cb(new Error('请选择新单位'))
      else cb()
    },
    trigger: 'change'
  }],
  toCompanyName: [{
    validator: (_r, v, cb) => {
      if (form.companySource === 'EXTERNAL' && !(v || '').trim()) cb(new Error('请填写新单位名称'))
      else cb()
    },
    trigger: 'blur'
  }],
  expectedStart: [{ required: true, message: '请选择预计入职日期', trigger: 'change' }],
  reason: [
    { required: true, message: '请填写变更原因', trigger: 'blur' },
    { min: 5, max: 1000, message: '变更原因长度 5~1000 字', trigger: 'blur' }
  ]
}

function defaultForm() {
  return {
    companySource: 'INTERNAL',
    toCompanyId: null,
    toCompanyName: '',
    expectedStart: '',
    reason: ''
  }
}

function disabledPast(d) {
  // 今天之前禁止
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return d.getTime() < today.getTime()
}

async function loadList() {
  loading.value = true
  try {
    list.value = await myTransfers(statusFilter.value || undefined)
  } finally {
    loading.value = false
  }
}

async function loadProfileAndCompanies() {
  // 当前学生档案中已绑定企业 id —— 用于禁选"原单位"
  try {
    const me = await httpReq('get', '/student/my-info')
    currentCompanyId.value = me?.companyId ?? null
    // 如果学生已绑定企业，加入到 companies 供禁选
    if (me?.companyId && me.companyName) {
      companies.value.push({ id: me.companyId, name: me.companyName })
    }
  } catch {}
}

function openSubmit() {
  Object.assign(form, defaultForm())
  attachments.value = []
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, defaultForm())
  attachments.value = []
}

async function onSubmit() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    const payload = {
      toCompanyId: form.companySource === 'INTERNAL' ? form.toCompanyId : null,
      toCompanyName: form.companySource === 'EXTERNAL' ? form.toCompanyName.trim() : null,
      expectedStart: form.expectedStart,
      reason: form.reason,
      attachmentIds: attachments.value.map((a) => a.id)
    }
    await submitTransfer(payload)
    ElMessage.success('提交成功，等待企业指导审批')
    dialogVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

async function onCancel(id) {
  await cancelTransfer(id)
  ElMessage.success('已撤回')
  loadList()
}

function goDetail(id) {
  router.push({ name: 'transfer-detail', params: { id } })
}

onMounted(() => {
  loadList()
  loadProfileAndCompanies()
})
</script>

<style scoped>
.page { padding: 16px; }
.page-card { background: #fff; border-radius: 6px; padding: 16px 20px; box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06); }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.card-title { font-size: 16px; font-weight: 600; }
.card-tools { display: flex; gap: 12px; }
.muted { color: #909399; font-size: 12px; margin-top: 4px; }
</style>
