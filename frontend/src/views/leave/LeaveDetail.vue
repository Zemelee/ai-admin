<template>
  <div class="page">
    <div class="page-card" v-loading="loading">
      <div class="card-head">
        <el-button :icon="ArrowLeft" link @click="goBack">返回</el-button>
        <span class="card-title">请假详情</span>
        <el-tag v-if="info" :type="STATUS_TAG[info.status]" effect="light" style="margin-left:12px">
          {{ STATUS_LABEL[info.status] }}
        </el-tag>
      </div>

      <template v-if="info">
        <el-descriptions :column="2" border title="申请信息" style="margin-top:16px">
          <el-descriptions-item label="学生">{{ info.studentName }}（{{ info.studentNo }}）</el-descriptions-item>
          <el-descriptions-item label="班级">{{ info.className || '—' }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ info.major || '—' }}</el-descriptions-item>
          <el-descriptions-item label="实习单位">{{ info.companyName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="假别">{{ TYPE_LABEL[info.leaveType] || info.leaveType }}</el-descriptions-item>
          <el-descriptions-item label="时长"><b>{{ info.durationDays }} 天</b></el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ fmtDateTime(info.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ fmtDateTime(info.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ fmtDateTime(info.submitTime) }}</el-descriptions-item>
          <el-descriptions-item label="家长确认">
            <el-tag v-if="info.parentConfirm" type="success" size="small">已确认</el-tag>
            <el-tag v-else type="info" size="small">未确认</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="请假事由" :span="2">{{ info.reason }}</el-descriptions-item>
          <el-descriptions-item label="当前节点" :span="2">
            <span v-if="info.status === 'PENDING'">{{ NODE_LABEL[info.currentNode] || info.currentNode }}</span>
            <span v-else style="color:#999">流程已结束</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="info.finishTime" label="结束时间" :span="2">
            {{ fmtDateTime(info.finishTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="block-title">审批流</div>
        <el-timeline>
          <!-- 提交节点 -->
          <el-timeline-item
            type="primary"
            :timestamp="fmtDateTime(info.submitTime)"
            placement="top"
          >
            <div class="flow-card">
              <div class="flow-head">
                <b>{{ info.studentName }}</b> 提交了请假申请
              </div>
            </div>
          </el-timeline-item>

          <!-- 审批节点 -->
          <el-timeline-item
            v-for="f in info.flows"
            :key="f.id"
            :type="flowDotType(f)"
            :timestamp="f.actTime ? fmtDateTime(f.actTime) : '待处理'"
            placement="top"
          >
            <div class="flow-card">
              <div class="flow-head">
                <b>{{ NODE_LABEL[f.node] || f.node }}</b>
                <span v-if="f.approverName"> · {{ f.approverName }}</span>
                <el-tag v-if="f.result" :type="RESULT_TAG[f.result]" size="small" style="margin-left:8px">
                  {{ RESULT_LABEL[f.result] }}
                </el-tag>
                <el-tag v-else type="warning" size="small" style="margin-left:8px">待处理</el-tag>
              </div>
              <div v-if="f.comment" class="flow-comment">意见：{{ f.comment }}</div>
            </div>
          </el-timeline-item>

          <!-- 终态 -->
          <el-timeline-item
            v-if="info.status !== 'PENDING'"
            :type="info.status === 'APPROVED' ? 'success' : 'danger'"
            :timestamp="fmtDateTime(info.finishTime)"
            placement="top"
          >
            <div class="flow-card">
              <div class="flow-head">
                流程结束 ·
                <el-tag :type="STATUS_TAG[info.status]" size="small">{{ STATUS_LABEL[info.status] }}</el-tag>
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  leaveDetail,
  TYPE_LABEL, STATUS_LABEL, STATUS_TAG, NODE_LABEL,
  RESULT_LABEL, RESULT_TAG, fmtDateTime
} from '@/api/leave'

const route = useRoute()
const router = useRouter()

const info = ref(null)
const loading = ref(false)

function flowDotType(f) {
  if (!f.result) return 'warning'
  return f.result === 'APPROVED' ? 'success' : 'danger'
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/dispatch')
}

async function load() {
  loading.value = true
  try {
    info.value = await leaveDetail(route.params.id)
  } finally {
    loading.value = false
  }
}

onMounted(load)
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
  align-items: center;
  gap: 8px;
}
.card-title { font-size: 16px; font-weight: 600; }
.block-title {
  margin: 24px 0 12px;
  font-size: 15px;
  font-weight: 600;
  border-left: 3px solid #409eff;
  padding-left: 8px;
}
.flow-card {
  background: #fafafa;
  border-radius: 4px;
  padding: 10px 14px;
}
.flow-head { font-size: 14px; }
.flow-comment {
  margin-top: 6px;
  color: #555;
  font-size: 13px;
  line-height: 1.7;
}
</style>
