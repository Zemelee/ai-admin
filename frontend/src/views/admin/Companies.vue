<template>
  <div class="page">
    <div class="page-card">
      <div class="card-head">
        <div class="card-title">实习合作企业管理</div>
        <div class="card-tools">
          <el-input v-model="keyword" placeholder="企业名称" clearable style="width: 200px" :prefix-icon="Search" @keyup.enter="onSearch" @clear="onSearch" />
          <el-select v-model="blackFilter" placeholder="黑名单" clearable style="width: 130px" @change="onSearch">
            <el-option label="正常" :value="0" />
            <el-option label="禁入清单" :value="1" />
          </el-select>
          <el-button type="primary" :icon="Plus" @click="openCreate">新增企业</el-button>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column label="企业名称" min-width="180" prop="name" show-overflow-tooltip />
        <el-table-column label="信用代码" width="180" prop="socialCode" show-overflow-tooltip />
        <el-table-column label="行业" width="90" prop="industry" />
        <el-table-column label="联系人" width="100">
          <template #default="{ row }">
            <div>{{ row.contactPerson || '—' }}</div>
            <div class="muted">{{ row.contactPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column label="企业指导" width="90" align="center" prop="mentorCount" />
        <el-table-column label="实习学生" width="90" align="center" prop="studentCount" />
        <el-table-column label="黑名单" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isBlacklist === 1" type="danger" size="small" effect="dark">禁入</el-tag>
            <el-tag v-else type="success" size="small" effect="plain">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除该企业？" @confirm="onDelete(row.id)">
              <template #reference>
                <el-button size="small" link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无企业档案" />
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
      :title="editingId ? '编辑企业' : '新增企业'"
      width="640px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="企业名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="信用代码">
          <el-input v-model="form.socialCode" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属行业">
              <el-input v-model="form.industry" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="form.contactPerson" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="禁入清单">
          <el-switch v-model="form.isBlacklist" :active-value="1" :inactive-value="0" active-text="加入禁入清单" inactive-text="正常" />
        </el-form-item>
        <el-form-item v-if="form.isBlacklist === 1" label="禁入原因">
          <el-input v-model="form.blacklistReason" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
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
import {
  adminCompanyPage, adminCompanyCreate, adminCompanyUpdate, adminCompanyDelete
} from '@/api/admin'

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const blackFilter = ref(null)
const page = reactive({ page: 1, size: 10, total: 0 })

async function loadList() {
  loading.value = true
  try {
    const params = { page: page.page, size: page.size }
    if (keyword.value) params.keyword = keyword.value
    if (blackFilter.value !== null && blackFilter.value !== '') params.isBlacklist = blackFilter.value
    const res = await adminCompanyPage(params)
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
  name: [{ required: true, message: '请输入企业名称', trigger: 'blur' }]
}

function defaultForm() {
  return {
    name: '', socialCode: '', address: '', industry: '',
    contactPerson: '', contactPhone: '', isBlacklist: 0, blacklistReason: '', remark: ''
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
    name: row.name, socialCode: row.socialCode || '', address: row.address || '', industry: row.industry || '',
    contactPerson: row.contactPerson || '', contactPhone: row.contactPhone || '',
    isBlacklist: row.isBlacklist ?? 0, blacklistReason: row.blacklistReason || '', remark: row.remark || ''
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
      await adminCompanyUpdate(editingId.value, { ...form })
      ElMessage.success('已保存')
    } else {
      await adminCompanyCreate({ ...form })
      ElMessage.success('已新增')
    }
    formVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

async function onDelete(id) {
  await adminCompanyDelete(id)
  ElMessage.success('已删除')
  loadList()
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
.muted { color: #909399; font-size: 12px; }
</style>
