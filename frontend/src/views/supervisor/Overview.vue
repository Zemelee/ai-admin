<template>
  <div v-loading="loading" class="overview">
    <!-- 4 张统计卡片 -->
    <div class="stat-grid">
      <div class="stat-card" style="--c: #409eff">
        <div class="stat-label">系统账号</div>
        <div class="stat-num">{{ data?.user?.total ?? '-' }}</div>
        <div class="stat-sub">已启用 {{ data?.user?.enabled ?? '-' }}</div>
      </div>
      <div class="stat-card" style="--c: #67c23a">
        <div class="stat-label">实习生（学生）</div>
        <div class="stat-num">{{ data?.student?.total ?? '-' }}</div>
        <div class="stat-sub">实习中 {{ activeCount }}</div>
      </div>
      <div class="stat-card" style="--c: #e6a23c">
        <div class="stat-label">指导教师</div>
        <div class="stat-num">{{ data?.teacher?.total ?? '-' }}</div>
        <div class="stat-sub">企业指导 mentor {{ data?.mentor?.total ?? '-' }}</div>
      </div>
      <div class="stat-card" style="--c: #f56c6c">
        <div class="stat-label">合作企业</div>
        <div class="stat-num">{{ data?.company?.total ?? '-' }}</div>
        <div class="stat-sub">禁入清单 {{ data?.company?.blacklist ?? 0 }}</div>
      </div>
    </div>

    <!-- 主体两栏 -->
    <div class="main-grid">
      <div class="page-card">
        <div class="page-title">实习状态分布</div>
        <div ref="chartRef" style="height: 280px"></div>
      </div>

      <div class="page-card">
        <div class="page-title">企业禁入清单（最近）</div>
        <el-empty v-if="!data?.blacklistTop5?.length" description="暂无禁入企业" />
        <el-table v-else :data="data.blacklistTop5" size="small" border>
          <el-table-column type="index" width="50" label="#" />
          <el-table-column prop="name" label="企业名称" />
          <el-table-column prop="industry" label="行业" width="100" />
          <el-table-column prop="blacklistReason" label="禁入原因" />
        </el-table>
      </div>
    </div>

    <div class="page-card">
      <div class="page-title">最近录入的学生</div>
      <el-empty v-if="!data?.recentStudents?.length" description="暂无数据" />
      <el-table v-else :data="data.recentStudents" size="small" border stripe>
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="className" label="班级" />
        <el-table-column prop="major" label="专业" />
        <el-table-column prop="grade" label="年级" width="80" />
        <el-table-column prop="internStatus" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.internStatus)" size="small">
              {{ statusLabel(row.internStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="internStart" label="开始" width="120" />
        <el-table-column prop="internEnd" label="结束" width="120" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { req } from '@/utils/http'

const loading = ref(true)
const data = ref(null)
const chartRef = ref(null)
let chart = null

const activeCount = computed(() => data.value?.student?.statusMap?.ACTIVE ?? 0)

const STATUS_MAP = {
  ACTIVE: { label: '实习中', type: 'success' },
  SUSPEND: { label: '已暂停', type: 'warning' },
  FINISHED: { label: '已结束', type: 'info' },
  QUIT: { label: '已退出', type: 'danger' }
}
function statusLabel(s) {
  return STATUS_MAP[s]?.label || s
}
function statusType(s) {
  return STATUS_MAP[s]?.type || 'info'
}

function renderChart() {
  if (!chartRef.value || !data.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const sm = data.value.student.statusMap
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        avoidLabelOverlap: false,
        label: { show: true, formatter: '{b}\n{c} 人' },
        data: Object.entries(sm).map(([k, v]) => ({
          name: STATUS_MAP[k]?.label || k,
          value: v
        }))
      }
    ]
  })
}

function onResize() {
  chart?.resize()
}

onMounted(async () => {
  try {
    data.value = await req('get', '/supervisor/overview')
    await nextTick()
    renderChart()
    window.addEventListener('resize', onResize)
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
  chart = null
})

watch(data, () => renderChart(), { deep: true })
</script>

<style scoped>
.overview {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 18px 20px;
  border-left: 4px solid var(--c);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
.stat-label {
  font-size: 13px;
  color: #909399;
}
.stat-num {
  font-size: 28px;
  font-weight: 600;
  color: var(--c);
  margin: 6px 0 4px;
}
.stat-sub {
  font-size: 12px;
  color: #909399;
}
.main-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
@media (max-width: 960px) {
  .main-grid {
    grid-template-columns: 1fr;
  }
}
</style>
