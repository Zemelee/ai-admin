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
        <el-table-column label="学生" width="130">
          <template #default="{ row }">
            <div>{{ row.studentName || '—' }}</div>
            <div class="muted">{{ row.studentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="原单位" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.fromCompanyName || '—' }}</template>
        </el-table-column>
        <el-table-column label="新单位" min-width="150" show-overflow-tooltip>
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
          <el-empty :description="activeTab === 'pending' ? '当前没有待审批的变更申请' : '暂无审批历史'" />
        </template>
      </el-table>
    </div>

    <!-- 审批对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" :close-on-click-modal="false">
      <div v-if="current" class="approve-summary">
        <div><b>学生：</b>{{ current.studentName }}（{{ current.studentNo }}）</div>
        <div><b>原单位：</b>{{ current.fromCompanyName || '—' }}</div>
        <div>
          <b>新单位：</b>{{ current.toCompanyName || '—' }}
          <el-tag v-if="!current.toCompanyId" size="small" type="info" style="margin-left:6px">外部</el-tag>
        </div>
        <div><b>预计入职：</b>{{ fmtDate(current.expectedStart) }}</div>
        <div><b>变更原因：</b>{{ current.reason }}</div>
      </div>
      <div v-if="detailAttachments.length" class="approve-attachments">
        <div class="approve-attachments-label">佐证材料</div>
        <ImageUploader :model-value="detailAttachments" biz-type="TRANSFER" readonly />
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
import ImageUploader from '@/components/ImageUploader.vue'
import {
  STATUS_LABEL, STATUS_TAG, NODE_LABEL,
  transferDetail, fmtDate, fmtDateTime
} from '@/api/transfer'

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
const detailAttachments = ref([])
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
  router.push({ name: 'transfer-detail', params: { id } })
}

function openApprove(row, result) {
  current.value = row
  approveForm.result = result
  approveForm.comment = ''
  detailAttachments.value = []
  dialogVisible.value = true
  transferDetail(row.id)
    .then((d) => { detailAttachments.value = d.attachments || [] })
    .catch(() => {})
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
.page-card { background: #fff; border-radius: 6px; padding: 16px 20px; box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06); }
.card-head { margin-bottom: 8px; }
.card-title { font-size: 16px; font-weight: 600; }
.muted { color: #909399; font-size: 12px; }
.approve-summary { background: #fafafa; border-radius: 4px; padding: 12px 14px; line-height: 1.9; font-size: 14px; }
.approve-summary > div { padding: 2px 0; }
.approve-attachments { margin-top: 12px; }
.approve-attachments-label { font-size: 13px; color: #909399; margin-bottom: 6px; }
</style>
