<template>
  <div class="page">
    <div class="page-card">
      <div class="card-head">
        <div class="card-title">指导教师管理</div>
        <div class="card-tools">
          <el-input v-model="keyword" placeholder="工号/姓名/账号" clearable style="width: 200px" :prefix-icon="Search" @keyup.enter="onSearch" @clear="onSearch" />
          <el-select v-model="statusFilter" placeholder="账号状态" clearable style="width: 130px" @change="onSearch">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-button type="primary" :icon="Plus" @click="openCreate">新增教师</el-button>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column label="工号" width="130" prop="teacherNo" />
        <el-table-column label="姓名" width="100" prop="realName" />
        <el-table-column label="账号" width="120" prop="username" />
        <el-table-column label="所属系" min-width="140" prop="department" show-overflow-tooltip />
        <el-table-column label="职称" width="100" prop="title" />
        <el-table-column label="办公电话" width="140" prop="officePhone" />
        <el-table-column label="分管学生" width="100" align="center" prop="studentCount" />
        <el-table-column label="账号" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除该教师及账号？" @confirm="onDelete(row.id)">
              <template #reference>
                <el-button size="small" link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无教师档案" />
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

    <el-dialog
      v-model="formVisible"
      :title="editingId ? '编辑教师' : '新增教师'"
      width="640px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-divider content-position="left">账号</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="登录账号" prop="username">
              <el-input v-model="form.username" />
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
              <el-input v-model="form.password" :placeholder="editingId ? '留空不改' : '留空默认 123456'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账号状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">档案</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工号" prop="teacherNo">
              <el-input v-model="form.teacherNo" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属系">
              <el-input v-model="form.department" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职称">
              <el-input v-model="form.title" placeholder="讲师/副教授/教授" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="办公电话">
              <el-input v-model="form.officePhone" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ editingId ? '保存' : '新增' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { adminTeacherPage, adminTeacherCreate, adminTeacherUpdate, adminTeacherDelete } from '@/api/admin'

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref(null)
const page = reactive({ page: 1, size: 10, total: 0 })

async function loadList() {
  loading.value = true
  try {
    const params = { page: page.page, size: page.size }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value !== null && statusFilter.value !== '') params.status = statusFilter.value
    const res = await adminTeacherPage(params)
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

const formVisible = ref(false)
const submitting = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const form = reactive(defaultForm())
const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  teacherNo: [{ required: true, message: '请输入工号', trigger: 'blur' }]
}

function defaultForm() {
  return {
    username: '', realName: '', phone: '', password: '', status: 1,
    teacherNo: '', department: '', title: '', officePhone: ''
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, defaultForm())
  formVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, defaultForm(), {
    username: row.username, realName: row.realName, phone: row.phone || '', password: '', status: row.status ?? 1,
    teacherNo: row.teacherNo, department: row.department || '', title: row.title || '', officePhone: row.officePhone || ''
  })
  formVisible.value = true
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
    if (editingId.value) {
      await adminTeacherUpdate(editingId.value, { ...form })
      ElMessage.success('已保存')
    } else {
      await adminTeacherCreate({ ...form })
      ElMessage.success('已新增')
    }
    formVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

async function onDelete(id) {
  await adminTeacherDelete(id)
  ElMessage.success('已删除')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.page { padding: 16px; }
.page-card { background: #fff; border-radius: 6px; padding: 16px 20px; box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06); }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.card-title { font-size: 16px; font-weight: 600; }
.card-tools { display: flex; gap: 12px; }
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
