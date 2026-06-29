<template>
  <div class="page">
    <div class="page-card">
      <div class="card-head">
        <div class="card-title">企业评价 / 实习鉴定</div>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px">
        <template #title>
          对所负责学生进行实习鉴定：四维度评分（出勤纪律 / 专业能力 / 工作态度 / 综合评价）+ 评语。保存为草稿可继续修改，提交后锁定不可改。
        </template>
      </el-alert>

      <el-table :data="list" v-loading="loading" border stripe style="width:100%">
        <el-table-column label="学生" min-width="150">
          <template #default="{ row }">
            <div>{{ row.studentName || '—' }}</div>
            <div class="muted">{{ row.studentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" width="120" show-overflow-tooltip />
        <el-table-column prop="major" label="专业" min-width="120" show-overflow-tooltip />
        <el-table-column label="综合评分" width="130" align="center">
          <template #default="{ row }">
            <el-rate v-if="row.scoreOverall" :model-value="row.scoreOverall" disabled size="small" />
            <span v-else style="color:#c0c4cc">未评</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.evaluated" :type="STATUS_TAG[row.status]" size="small">{{ STATUS_LABEL[row.status] }}</el-tag>
            <el-tag v-else type="warning" size="small" effect="dark">待评价</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ row.evaluated ? fmtDateTime(row.submitTime) : '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openEval(row)">
              {{ row.evaluated ? (row.status === 'SUBMITTED' ? '查看' : '继续编辑') : '去评价' }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无负责学生" />
        </template>
      </el-table>
    </div>

    <!-- 评价对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="current?.evaluated && current?.status === 'SUBMITTED' ? '实习鉴定（已提交）' : '填写实习鉴定'"
      width="640px"
      :close-on-click-modal="false"
    >
      <div v-loading="loadingDetail">
        <el-descriptions :column="2" border v-if="current">
          <el-descriptions-item label="学生">{{ current.studentName }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ current.studentNo }}</el-descriptions-item>
          <el-descriptions-item label="班级">{{ current.className || '—' }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ current.major || '—' }}</el-descriptions-item>
        </el-descriptions>

        <el-form ref="formRef" :model="form" label-width="100px" style="margin-top:16px" :disabled="readonly">
          <el-form-item label="出勤与纪律">
            <el-rate v-model="form.scoreAttendance" :max="5" show-text :texts="SCORE_TEXTS" />
          </el-form-item>
          <el-form-item label="专业能力">
            <el-rate v-model="form.scoreAbility" :max="5" show-text :texts="SCORE_TEXTS" />
          </el-form-item>
          <el-form-item label="工作态度">
            <el-rate v-model="form.scoreAttitude" :max="5" show-text :texts="SCORE_TEXTS" />
          </el-form-item>
          <el-form-item label="综合评价">
            <el-rate v-model="form.scoreOverall" :max="5" show-text :texts="SCORE_TEXTS" />
          </el-form-item>
          <el-form-item label="鉴定评语">
            <el-input v-model="form.comment" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="对该生实习期间的整体表现进行鉴定（可选，不超过 1000 字）" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <template v-if="!readonly">
          <el-button :loading="saving" @click="onSave">保存草稿</el-button>
          <el-button type="primary" :loading="submitting" @click="onSubmit">提交鉴定</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { myEvalStudents, saveEval, submitEval, evalDetail, STATUS_LABEL, STATUS_TAG, SCORE_TEXTS, fmtDateTime } from '@/api/companyEval'

const list = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const loadingDetail = ref(false)
const saving = ref(false)
const submitting = ref(false)
const current = ref(null)
const formRef = ref(null)
const form = reactive({ scoreAttendance: 0, scoreAbility: 0, scoreAttitude: 0, scoreOverall: 0, comment: '' })

const readonly = ref(false)

async function loadList() {
  loading.value = true
  try {
    list.value = await myEvalStudents()
  } finally {
    loading.value = false
  }
}

function openEval(row) {
  current.value = row
  readonly.value = row.evaluated && row.status === 'SUBMITTED'
  form.scoreAttendance = row.scoreAttendance || 0
  form.scoreAbility = row.scoreAbility || 0
  form.scoreAttitude = row.scoreAttitude || 0
  form.scoreOverall = row.scoreOverall || 0
  form.comment = ''
  dialogVisible.value = true
  // 拉取详情补全评语
  if (row.evaluated && row.id) {
    loadingDetail.value = true
    evalDetail(row.id).then((d) => {
      form.comment = d.comment || ''
    }).finally(() => { loadingDetail.value = false })
  }
}

function validate() {
  const labels = { scoreAttendance: '出勤与纪律', scoreAbility: '专业能力', scoreAttitude: '工作态度', scoreOverall: '综合评价' }
  for (const k of ['scoreAttendance', 'scoreAbility', 'scoreAttitude', 'scoreOverall']) {
    if (!form[k]) {
      ElMessage.warning('请完成评分：' + labels[k])
      return false
    }
  }
  return true
}

async function onSave() {
  if (!validate()) return
  saving.value = true
  try {
    await saveEval({ studentId: current.value.studentId, ...form })
    ElMessage.success('草稿已保存')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function onSubmit() {
  if (!validate()) return
  await ElMessageBox.confirm('提交后鉴定将锁定，不可再修改。确认提交？', '提示', { type: 'warning' })
  submitting.value = true
  try {
    const id = await saveEval({ studentId: current.value.studentId, ...form })
    await submitEval(id)
    ElMessage.success('鉴定已提交')
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
.page-card { background: #fff; border-radius: 6px; padding: 16px 20px; box-shadow: 0 1px 4px rgba(0,21,41,0.06); }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.card-title { font-size: 16px; font-weight: 600; }
.muted { color: #909399; font-size: 12px; }
</style>
