<template>
  <div class="page">
    <div class="page-card">
      <div class="card-head">
        <div class="card-title">{{ title }}</div>
      </div>

      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="待我审批" name="pending" />
        <el-tab-pane label="我已审批" name="history" />
      </el-tabs>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column label="学生" width="120">
          <template #default="{ row }">
            <div>{{ row.studentName }}</div>
            <div style="color:#999;font-size:12px">{{ row.studentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="班级" width="160" prop="className" show-overflow-tooltip />
        <el-table-column label="假别" width="80">
          <template #default="{ row }">{{ TYPE_LABEL[row.leaveType] || row.leaveType }}</template>
        </el-table-column>
        <el-table-column label="时长" width="80" align="center">
          <template #default="{ row }">{{ row.durationDays }}天</template>
        </el-table-column>
        <el-table-column label="开始" width="140">
          <template #default="{ row }">{{ fmtDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束" width="140">
          <template #default="{ row }">{{ fmtDateTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="事由" prop="reason" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status]" effect="light">{{ STATUS_LABEL[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="150">
          <template #default="{ row }">{{ fmtDateTime(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="goDetail(row.id)">详情</el-button>
            <template v-if="activeTab === 'pending'">
              <el-button size="small" type="success" @click="openApprove(row, 'APPROVED')">通过</el-button>
              <el-button size="small" type="danger" @click="openApprove(row, 'REJECTED')">驳回</el-button>
            </template>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="activeTab === 'pending' ? '当前没有待审批的请假' : '暂无审批历史'" />
        </template>
      </el-table>
    </div>

    <!-- 审批对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" :close-on-click-modal="false">
      <div v-if="current" class="approve-summary">
        <div><b>学生：</b>{{ current.studentName }} ({{ current.studentNo }})</div>
        <div><b>假别：</b>{{ TYPE_LABEL[current.leaveType] }} · <b>{{ current.durationDays }} 天</b></div>
        <div><b>时段：</b>{{ fmtDateTime(current.startTime) }} ~ {{ fmtDateTime(current.endTime) }}</div>
        <div><b>事由：</b>{{ current.reason }}</div>
      </div>
      <el-form :model="approveForm" label-width="80px" style="margin-top:12px">
        <el-form-item label="审批意见">
          <el-input
            v-model="approveForm.comment"
            type="textarea"
            :rows="3"
            :placeholder="approveForm.result === 'REJECTED' ? '驳回必须填写意见（不少于 5 字）' : '可填写补充意见（选填）'"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          :type="approveForm.result === 'APPROVED' ? 'success' : 'danger'"
          :loading="approving"
          @click="onApprove"
        >
          确认{{ approveForm.result === 'APPROVED' ? '通过' : '驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { TYPE_LABEL, STATUS_LABEL, STATUS_TAG, fmtDateTime } from '@/api/leave'

const props = defineProps({
  title: { type: String, required: true },
  pendingApi: { type: Function, required: true },
  historyApi: { type: Function, required: true },
  approveApi: { type: Function, required: true }
})

const router = useRouter()
const activeTab = ref('pending')
const list = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const approving = ref(false)
const current = ref(null)
const approveForm = reactive({ result: 'APPROVED', comment: '' })
const dialogTitle = computed(() => approveForm.result === 'APPROVED' ? '通过审批' : '驳回审批')

async function loadList() {
  loading.value = true
  try {
    list.value = activeTab.value === 'pending' ? await props.pendingApi() : await props.historyApi()
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  loadList()
}

function goDetail(id) {
  router.push({ name: 'leave-detail', params: { id } })
}

function openApprove(row, result) {
  current.value = row
  approveForm.result = result
  approveForm.comment = ''
  dialogVisible.value = true
}

async function onApprove() {
  if (approveForm.result === 'REJECTED' && (approveForm.comment || '').trim().length < 5) {
    ElMessage.warning('驳回必须填写不少于 5 字的审批意见')
    return
  }
  approving.value = true
  try {
    await props.approveApi(current.value.id, {
      result: approveForm.result,
      comment: approveForm.comment
    })
    ElMessage.success(approveForm.result === 'APPROVED' ? '已通过' : '已驳回')
    dialogVisible.value = false
    loadList()
  } finally {
    approving.value = false
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
.card-head { margin-bottom: 8px; }
.card-title { font-size: 16px; font-weight: 600; }
.approve-summary {
  background: #fafafa;
  border-radius: 4px;
  padding: 12px 14px;
  line-height: 1.9;
  font-size: 14px;
}
.approve-summary > div { padding: 2px 0; }
</style>
