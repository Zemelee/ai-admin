<template>
  <div class="page">
    <div class="page-card">
      <div class="card-head">
        <div class="card-title">学生档案管理</div>
        <div class="card-tools">
          <el-input v-model="keyword" placeholder="学号/姓名/账号" clearable style="width: 200px" :prefix-icon="Search" @keyup.enter="onSearch" @clear="onSearch" />
          <el-select v-model="statusFilter" placeholder="账号状态" clearable style="width: 130px" @change="onSearch">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-select v-model="internFilter" placeholder="实习状态" clearable style="width: 130px" @change="onSearch">
            <el-option v-for="(l, k) in INTERN_STATUS_LABEL" :key="k" :label="l" :value="k" />
          </el-select>
          <el-button type="primary" :icon="Plus" @click="openCreate">新增学生</el-button>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column label="学号" width="120" prop="studentNo" />
        <el-table-column label="姓名" width="90" prop="realName" />
        <el-table-column label="账号" width="110" prop="username" />
        <el-table-column label="班级" width="130" prop="className" show-overflow-tooltip />
        <el-table-column label="指导教师" width="90">
          <template #default="{ row }">{{ row.teacherName || '—' }}</template>
        </el-table-column>
        <el-table-column label="实习企业" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.companyName || '—' }}</template>
        </el-table-column>
        <el-table-column label="企业指导" width="90">
          <template #default="{ row }">{{ row.mentorName || '—' }}</template>
        </el-table-column>
        <el-table-column label="实习状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="INTERN_STATUS_TAG[row.internStatus]" size="small" effect="light">
              {{ INTERN_STATUS_LABEL[row.internStatus] || row.internStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="plain">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" link type="warning" @click="openBind(row)">绑定</el-button>
            <el-popconfirm title="确认删除该学生档案及账号？" @confirm="onDelete(row.id)">
              <template #reference>
                <el-button size="small" link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无学生档案" />
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page.page"
          v-model:page-size="page.size"
          :total="page.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadList"
          @size-change="onSearch"
        />
      </div>
    </div>

    <!-- 新增/编辑 对话框 -->
    <el-dialog
      v-model="formVisible"
      :title="editingId ? '编辑学生' : '新增学生'"
      width="720px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px" v-loading="formLoading">
        <el-divider content-position="left">账号</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="登录账号" prop="username">
              <el-input v-model="form.username" placeholder="学号作为登录账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="form.realName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="editingId ? '重设密码' : '初始密码'">
              <el-input v-model="form.password" :placeholder="editingId ? '留空表示不修改' : '留空默认 123456'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账号状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">档案</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学号" prop="studentNo">
              <el-input v-model="form.studentNo" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班级">
              <el-input v-model="form.className" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="专业">
              <el-input v-model="form.major" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年级">
              <el-input v-model="form.grade" placeholder="如 2024" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号">
              <el-input v-model="form.idCard" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="家长电话">
              <el-input v-model="form.parentPhone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实习状态">
              <el-select v-model="form.internStatus" style="width: 100%">
                <el-option v-for="(l, k) in INTERN_STATUS_LABEL" :key="k" :label="l" :value="k" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实习开始">
              <el-date-picker v-model="form.internStart" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实习结束">
              <el-date-picker v-model="form.internEnd" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ editingId ? '保存' : '新增' }}</el-button>
      </template>
    </el-dialog>

    <!-- 绑定对话框 -->
    <el-dialog v-model="bindVisible" title="绑定关系" width="480px" @close="resetBind">
      <el-form label-width="92px" v-loading="bindLoading">
        <el-form-item label="学生">
          <span>{{ bindRow?.realName }}（{{ bindRow?.studentNo }}）</span>
        </el-form-item>
        <el-form-item label="指导教师">
          <el-select v-model="bindForm.teacherId" clearable placeholder="选择教师" style="width: 100%">
            <el-option v-for="t in teachers" :key="t.id" :label="`${t.name}（${t.teacherNo}）`" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="实习企业">
          <el-select v-model="bindForm.companyId" clearable placeholder="选择企业" style="width: 100%" @change="onCompanyChange">
            <el-option v-for="c in companies" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="企业指导">
          <el-select v-model="bindForm.mentorId" clearable placeholder="选择企业指导" style="width: 100%">
            <el-option v-for="m in filteredMentors" :key="m.id" :label="`${m.name}（${m.companyName}）`" :value="m.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindSubmitting" @click="onBind">保存绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import {
  adminStudentPage, adminStudentCreate, adminStudentUpdate, adminStudentDelete, adminStudentBind,
  optionTeachers, optionMentors, optionCompanies,
  INTERN_STATUS_LABEL, INTERN_STATUS_TAG
} from '@/api/admin'

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref(null)
const internFilter = ref('')
const page = reactive({ page: 1, size: 10, total: 0 })

async function loadList() {
  loading.value = true
  try {
    const params = { page: page.page, size: page.size }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value !== null && statusFilter.value !== '') params.status = statusFilter.value
    if (internFilter.value) params.internStatus = internFilter.value
    const res = await adminStudentPage(params)
    list.value = res.records || []
    page.total = res.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.page = 1
  loadList()
}

// ----- 新增/编辑 -----
const formVisible = ref(false)
const submitting = ref(false)
const formLoading = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const form = reactive(defaultForm())
const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }]
}

function defaultForm() {
  return {
    username: '', realName: '', phone: '', password: '', status: 1,
    studentNo: '', className: '', major: '', grade: '', idCard: '',
    gender: 1, parentPhone: '', internStart: '', internEnd: '', internStatus: 'ACTIVE',
    teacherId: null, companyId: null, mentorId: null
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, defaultForm())
  formVisible.value = true
}

async function openEdit(row) {
  editingId.value = row.id
  formLoading.value = true
  formVisible.value = true
  try {
    Object.assign(form, defaultForm(), {
      username: row.username, realName: row.realName, phone: row.phone || '', password: '', status: row.status ?? 1,
      studentNo: row.studentNo, className: row.className || '', major: row.major || '', grade: row.grade || '',
      idCard: row.idCard || '', gender: row.gender ?? 1, parentPhone: row.parentPhone || '',
      internStart: row.internStart || '', internEnd: row.internEnd || '', internStatus: row.internStatus || 'ACTIVE',
      teacherId: row.teacherId, companyId: row.companyId, mentorId: row.mentorId
    })
  } finally {
    formLoading.value = false
  }
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, defaultForm())
  editingId.value = null
}

async function onSubmit() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    const payload = { ...form }
    if (editingId.value) {
      await adminStudentUpdate(editingId.value, payload)
      ElMessage.success('已保存')
    } else {
      await adminStudentCreate(payload)
      ElMessage.success('已新增')
    }
    formVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

async function onDelete(id) {
  await adminStudentDelete(id)
  ElMessage.success('已删除')
  loadList()
}

// ----- 绑定 -----
const bindVisible = ref(false)
const bindLoading = ref(false)
const bindSubmitting = ref(false)
const bindRow = ref(null)
const bindForm = reactive({ teacherId: null, companyId: null, mentorId: null })
const teachers = ref([])
const mentors = ref([])
const companies = ref([])

const filteredMentors = computed(() => {
  if (!bindForm.companyId) return mentors.value
  return mentors.value.filter((m) => m.companyName === companies.value.find((c) => c.id === bindForm.companyId)?.name)
})

async function loadOptions() {
  const [t, m, c] = await Promise.all([optionTeachers(), optionMentors(), optionCompanies()])
  teachers.value = t
  mentors.value = m
  companies.value = c
}

async function openBind(row) {
  bindRow.value = row
  Object.assign(bindForm, { teacherId: row.teacherId, companyId: row.companyId, mentorId: row.mentorId })
  bindVisible.value = true
  if (!teachers.value.length) {
    bindLoading.value = true
    try { await loadOptions() } finally { bindLoading.value = false }
  }
}

function onCompanyChange() {
  // 切换企业后，若当前 mentor 不属于该企业则清空
  if (bindForm.mentorId && !filteredMentors.value.some((m) => m.id === bindForm.mentorId)) {
    bindForm.mentorId = null
  }
}

function resetBind() {
  bindRow.value = null
  Object.assign(bindForm, { teacherId: null, companyId: null, mentorId: null })
}

async function onBind() {
  bindSubmitting.value = true
  try {
    await adminStudentBind(bindRow.value.id, { ...bindForm })
    ElMessage.success('绑定已更新')
    bindVisible.value = false
    loadList()
  } finally {
    bindSubmitting.value = false
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
.card-tools { display: flex; gap: 12px; }
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
