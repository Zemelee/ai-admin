<template>
  <div class="page">
    <div class="page-card">
      <div class="card-head">
        <div class="card-title">我的请假申请</div>
        <div class="card-tools">
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px" @change="loadList">
            <el-option label="审批中" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="已撤回" value="CANCELLED" />
          </el-select>
          <el-button type="primary" :icon="Plus" @click="openSubmit">新建请假</el-button>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column label="假别" width="90">
          <template #default="{ row }">{{ TYPE_LABEL[row.leaveType] || row.leaveType }}</template>
        </el-table-column>
        <el-table-column label="开始时间" width="160">
          <template #default="{ row }">{{ fmtDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="160">
          <template #default="{ row }">{{ fmtDateTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="durationDays" label="天数" width="90" align="center" />
        <el-table-column prop="reason" label="事由" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status]" effect="light">{{ STATUS_LABEL[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前节点" width="120" align="center">
          <template #default="{ row }">
            <span v-if="row.status === 'PENDING'">{{ NODE_LABEL[row.currentNode] || row.currentNode }}</span>
            <span v-else style="color:#999">—</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ fmtDateTime(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="goDetail(row.id)">详情</el-button>
            <el-popconfirm
              v-if="row.status === 'PENDING'"
              title="确认撤回该请假申请？"
              @confirm="onCancel(row.id)"
            >
              <template #reference>
                <el-button size="small" link type="danger">撤回</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无请假记录，点击右上角新建一条吧" />
        </template>
      </el-table>
    </div>

    <!-- 新建请假对话框 -->
    <el-dialog v-model="dialogVisible" title="新建请假申请" width="560px" :close-on-click-modal="false" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="假别" prop="leaveType">
          <el-radio-group v-model="form.leaveType">
            <el-radio value="SICK">病假</el-radio>
            <el-radio value="PERSONAL">事假</el-radio>
            <el-radio value="OTHER">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="预估天数">
          <el-tag :type="durationTag">{{ duration }} 天</el-tag>
          <span v-if="duration > 30" style="margin-left:8px;color:#e6a23c;font-size:12px">
            ⚠ 超过 30 天，需教师 + 系主任两级审批
          </span>
        </el-form-item>
        <el-form-item label="家长确认">
          <el-switch v-model="form.parentConfirm" active-text="已联系家长确认" inactive-text="未确认" />
        </el-form-item>
        <el-form-item label="请假事由" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="4"
            placeholder="请详细说明请假原因（不少于 5 字，最多 1000 字）"
            maxlength="1000"
            show-word-limit
          />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  myLeaves, submitLeave, cancelLeave,
  TYPE_LABEL, STATUS_LABEL, STATUS_TAG, NODE_LABEL, fmtDateTime
} from '@/api/leave'

const router = useRouter()

const list = ref([])
const loading = ref(false)
const statusFilter = ref('')

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({
  leaveType: 'PERSONAL',
  startTime: '',
  endTime: '',
  parentConfirm: false,
  reason: ''
})

const rules = {
  leaveType: [{ required: true, message: '请选择假别', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' },
    {
      validator: (_r, v, cb) => {
        if (v && form.startTime && new Date(v) <= new Date(form.startTime)) {
          cb(new Error('结束时间必须晚于开始时间'))
        } else cb()
      },
      trigger: 'change'
    }
  ],
  reason: [
    { required: true, message: '请填写请假事由', trigger: 'blur' },
    { min: 5, max: 1000, message: '事由长度 5~1000 字', trigger: 'blur' }
  ]
}

const duration = computed(() => {
  if (!form.startTime || !form.endTime) return 0
  const ms = new Date(form.endTime) - new Date(form.startTime)
  if (ms <= 0) return 0
  return Math.round((ms / 86400000) * 100) / 100
})
const durationTag = computed(() => duration.value > 30 ? 'warning' : 'success')

async function loadList() {
  loading.value = true
  try {
    list.value = await myLeaves(statusFilter.value || undefined)
  } finally {
    loading.value = false
  }
}

function openSubmit() {
  dialogVisible.value = true
}
function resetForm() {
  formRef.value?.resetFields()
  form.leaveType = 'PERSONAL'
  form.parentConfirm = false
}

async function onSubmit() {
  try {
    await formRef.value.validate()
  } catch { return }
  submitting.value = true
  try {
    await submitLeave({
      leaveType: form.leaveType,
      startTime: form.startTime,
      endTime: form.endTime,
      reason: form.reason,
      parentConfirm: form.parentConfirm
    })
    ElMessage.success('提交成功，等待教师审批')
    dialogVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

async function onCancel(id) {
  await cancelLeave(id)
  ElMessage.success('已撤回')
  loadList()
}

function goDetail(id) {
  router.push({ name: 'leave-detail', params: { id } })
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
</style>
