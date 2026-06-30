<template>
  <div class="page">
    <div class="page-card">
      <div class="card-head">
        <div class="card-title">系统公告管理</div>
        <div class="card-tools">
          <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 110px" @change="loadList">
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="草稿" value="DRAFT" />
          </el-select>
          <el-button type="primary" :icon="Plus" @click="openCreate">发布公告</el-button>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="优先级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="PRIORITY_TAG[row.priority]" size="small" effect="light">
              {{ PRIORITY_LABEL[row.priority] || row.priority }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status]" size="small" effect="light">
              {{ STATUS_LABEL[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">{{ row.publishTime ? fmtDateTime(row.publishTime) : '—' }}</template>
        </el-table-column>
        <el-table-column label="发布人" width="100" prop="createUserName" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除该公告？" @confirm="onDelete(row.id)">
              <template #reference>
                <el-button size="small" link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无公告" />
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
          @size-change="loadList"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="formVisible"
      :title="editingId ? '编辑公告' : '发布公告'"
      width="640px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="公告标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="form.priority">
            <el-radio value="NORMAL">普通</el-radio>
            <el-radio value="IMPORTANT">重要</el-radio>
            <el-radio value="URGENT">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="PUBLISHED">立即发布</el-radio>
            <el-radio value="DRAFT">存为草稿</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="公告内容（支持 Markdown 语法）"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ editingId ? '保存' : '发布' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  announcementList, announcementCreate, announcementUpdate, announcementDelete,
  PRIORITY_LABEL, PRIORITY_TAG, STATUS_LABEL, STATUS_TAG
} from '@/api/announcement'

const list = ref([])
const loading = ref(false)
const statusFilter = ref('')
const page = reactive({ page: 1, size: 10, total: 0 })

const formVisible = ref(false)
const submitting = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const form = reactive({ title: '', priority: 'NORMAL', status: 'PUBLISHED', content: '' })
const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

function fmtDateTime(s) {
  if (!s) return ''
  return String(s).replace('T', ' ').slice(0, 16)
}

async function loadList() {
  loading.value = true
  try {
    const params = { page: page.page, size: page.size }
    if (statusFilter.value) params.status = statusFilter.value
    const res = await announcementList(params)
    list.value = res.records || []
    page.total = res.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { title: '', priority: 'NORMAL', status: 'PUBLISHED', content: '' })
  formVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    title: row.title || '',
    priority: row.priority || 'NORMAL',
    status: row.status || 'PUBLISHED',
    content: row.content || ''
  })
  formVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  editingId.value = null
}

async function onSubmit() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    const payload = { ...form }
    if (editingId.value) {
      await announcementUpdate(editingId.value, payload)
      ElMessage.success('已保存')
    } else {
      await announcementCreate(payload)
      ElMessage.success('已发布')
    }
    formVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

async function onDelete(id) {
  await announcementDelete(id)
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