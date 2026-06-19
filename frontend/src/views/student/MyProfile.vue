<template>
  <div v-if="loading" class="page-card">
    <el-skeleton :rows="6" animated />
  </div>

  <div v-else-if="info" class="profile">
    <div class="page-card head">
      <div class="avatar">{{ (info.realName || info.username).slice(0, 1) }}</div>
      <div class="meta">
        <div class="name">{{ info.realName }} <span class="muted">@{{ info.username }}</span></div>
        <div class="row">
          <el-tag size="small">学号 {{ info.studentNo }}</el-tag>
          <el-tag size="small" type="info">{{ info.major }}</el-tag>
          <el-tag size="small" type="info">{{ info.className }}</el-tag>
          <el-tag size="small" :type="statusTagType(info.internStatus)">
            {{ statusLabel(info.internStatus) }}
          </el-tag>
        </div>
        <div class="row muted">
          实习周期：{{ info.internStart || '—' }} 至 {{ info.internEnd || '—' }}
          ｜ 联系电话：{{ info.phone || '—' }}
        </div>
      </div>
    </div>

    <div class="grid">
      <div class="page-card">
        <div class="page-title">指导教师</div>
        <template v-if="info.teacher">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="姓名">{{ info.teacher.realName }}</el-descriptions-item>
            <el-descriptions-item label="工号">{{ info.teacher.teacherNo }}</el-descriptions-item>
            <el-descriptions-item label="所在系">{{ info.teacher.department }}</el-descriptions-item>
            <el-descriptions-item label="职称">{{ info.teacher.title }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ info.teacher.phone || '—' }}</el-descriptions-item>
          </el-descriptions>
        </template>
        <el-empty v-else description="尚未分配指导教师" />
      </div>

      <div class="page-card">
        <div class="page-title">实习企业</div>
        <template v-if="info.company">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="名称">
              {{ info.company.name }}
              <el-tag v-if="info.company.isBlacklist" type="danger" size="small" style="margin-left: 6px">
                黑名单
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="行业">{{ info.company.industry }}</el-descriptions-item>
            <el-descriptions-item label="地址">{{ info.company.address }}</el-descriptions-item>
            <el-descriptions-item label="HR">
              {{ info.company.contactPerson }} / {{ info.company.contactPhone }}
            </el-descriptions-item>
          </el-descriptions>
        </template>
        <el-empty v-else description="尚未确定实习企业" />
      </div>

      <div class="page-card">
        <div class="page-title">企业指导（mentor）</div>
        <template v-if="info.mentor">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="姓名">{{ info.mentor.realName }}</el-descriptions-item>
            <el-descriptions-item label="岗位">{{ info.mentor.position }}</el-descriptions-item>
            <el-descriptions-item label="部门">{{ info.mentor.dept }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ info.mentor.phone || '—' }}</el-descriptions-item>
          </el-descriptions>
        </template>
        <el-empty v-else description="尚未分配企业指导" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { req } from '@/utils/http'

const loading = ref(true)
const info = ref(null)

const STATUS_MAP = {
  ACTIVE: { label: '实习中', type: 'success' },
  SUSPEND: { label: '已暂停', type: 'warning' },
  FINISHED: { label: '已结束', type: 'info' },
  QUIT: { label: '已退出', type: 'danger' }
}
function statusLabel(s) {
  return STATUS_MAP[s]?.label || s
}
function statusTagType(s) {
  return STATUS_MAP[s]?.type || 'info'
}

onMounted(async () => {
  try {
    info.value = await req('get', '/student/my-info')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.profile {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.head {
  display: flex;
  align-items: center;
  gap: 20px;
}
.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 6px;
}
.row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 4px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
}
</style>
