<template>
  <div v-loading="loading" class="overview">
    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div class="stat-card" style="--c: #409eff" @click="$router.push('/teacher/students')" title="查看学生列表">
        <div class="stat-label">我带的学生</div>
        <div class="stat-num">{{ data?.studentCount ?? '-' }}</div>
        <div class="stat-sub">点击查看明细</div>
      </div>
      <div class="stat-card" style="--c: #e6a23c" @click="$router.push('/teacher/weekly-reports')" title="前往周记评分">
        <div class="stat-label">待批周记</div>
        <div class="stat-num">{{ data?.pendingWeeklyCount ?? '-' }}</div>
        <div class="stat-sub">点击前往评分</div>
      </div>
      <div class="stat-card" style="--c: #f56c6c" @click="$router.push('/teacher/intern-reports')" title="前往报告评分">
        <div class="stat-label">待批实习报告</div>
        <div class="stat-num">{{ data?.pendingReportCount ?? '-' }}</div>
        <div class="stat-sub">点击前往评分</div>
      </div>
      <div class="stat-card" style="--c: #67c23a" @click="$router.push('/teacher/leaves')" title="前往请假审批">
        <div class="stat-label">待批假条</div>
        <div class="stat-num">{{ data?.pendingLeaveCount ?? '-' }}</div>
        <div class="stat-sub">点击前往审批</div>
      </div>
    </div>

    <!-- 第二行：成绩分布 + 分数摘要 -->
    <div class="chart-row">
      <div class="page-card">
        <div class="page-title">分管学生成绩等级分布</div>
        <div ref="gradeChartRef" class="chart-box"></div>
        <el-empty v-if="noScoreData" description="暂无成绩数据" />
      </div>
      <div class="page-card">
        <div class="page-title">成绩统计摘要</div>
        <div class="summary-grid" v-if="data?.scoreStats">
          <div class="summary-item">
            <span class="summary-label">已评分人数</span>
            <span class="summary-value" style="color:#409eff">{{ data.scoreStats.count }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">平均分</span>
            <span class="summary-value" style="color:#67c23a">{{ fmt(data.scoreStats.avg) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">最高分</span>
            <span class="summary-value" style="color:#e6a23c">{{ fmt(data.scoreStats.max) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">最低分</span>
            <span class="summary-value" style="color:#f56c6c">{{ fmt(data.scoreStats.min) }}</span>
          </div>
        </div>
        <el-divider />
        <div class="quick-links">
          <div class="ql-title">快捷入口</div>
          <div class="ql-grid">
            <el-tag v-if="data?.pendingWeeklyCount > 0" type="warning" @click="$router.push('/teacher/weekly-reports')" style="cursor:pointer">
              待批周记 {{ data.pendingWeeklyCount }} 篇
            </el-tag>
            <el-tag v-if="data?.pendingReportCount > 0" type="danger" @click="$router.push('/teacher/intern-reports')" style="cursor:pointer">
              待批报告 {{ data.pendingReportCount }} 篇
            </el-tag>
            <el-tag v-if="data?.pendingLeaveCount > 0" type="success" @click="$router.push('/teacher/leaves')" style="cursor:pointer">
              待批假条 {{ data.pendingLeaveCount }} 条
            </el-tag>
            <el-tag v-if="data?.pendingTransferCount > 0" type="warning" @click="$router.push('/teacher/transfers')" style="cursor:pointer">
              待批变更 {{ data.pendingTransferCount }} 条
            </el-tag>
            <el-tag v-if="!data?.pendingWeeklyCount && !data?.pendingReportCount && !data?.pendingLeaveCount && !data?.pendingTransferCount" type="success">
              暂无待办事项 ✨
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 待办事项列表 -->
    <div class="page-card">
      <div class="page-title">待办事项总览</div>
      <el-table :data="todoList" size="small" border stripe style="width:100%">
        <el-table-column label="事项类型" width="140">
          <template #default="{ row }">
            <el-tag :type="row.tagType" size="small">{{ row.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="待处理数" prop="count" width="100" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="$router.push(row.path)">前往处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!hasTodo" description="暂无待办事项，所有任务已处理完毕 🎉" />
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { req } from '@/utils/http'

const loading = ref(true)
const data = ref(null)
const gradeChartRef = ref(null)
let gradeChart = null

const GRADE_COLORS = {
  '优秀': '#67c23a', '良好': '#409eff', '中等': '#e6a23c',
  '及格': '#f56c6c', '不及格': '#909399', '无法评定': '#dcdfe6'
}

const noScoreData = computed(() => {
  const gd = data.value?.gradeDistribution
  return !gd || Object.keys(gd).length === 0
})

const todoList = computed(() => {
  const d = data.value
  if (!d) return []
  const items = []
  if (d.pendingWeeklyCount > 0) items.push({ label: '待批周记', count: d.pendingWeeklyCount, tagType: 'warning', path: '/teacher/weekly-reports' })
  if (d.pendingReportCount > 0) items.push({ label: '待批报告', count: d.pendingReportCount, tagType: 'danger', path: '/teacher/intern-reports' })
  if (d.pendingLeaveCount > 0) items.push({ label: '待批假条', count: d.pendingLeaveCount, tagType: 'success', path: '/teacher/leaves' })
  if (d.pendingTransferCount > 0) items.push({ label: '待批变更', count: d.pendingTransferCount, tagType: 'warning', path: '/teacher/transfers' })
  return items
})

const hasTodo = computed(() => todoList.value.length > 0)

function fmt(v) {
  if (v == null) return '-'
  return typeof v === 'number' ? v.toFixed(1) : String(v)
}

function renderChart() {
  if (!gradeChartRef.value || noScoreData.value) return
  if (!gradeChart) gradeChart = echarts.init(gradeChartRef.value)
  const gd = data.value.gradeDistribution
  const order = ['优秀', '良好', '中等', '及格', '不及格', '无法评定']
  const sorted = order.filter(k => k in gd).concat(Object.keys(gd).filter(k => !order.includes(k)))
  gradeChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 人 ({d}%)' },
    legend: { bottom: 0 },
    color: sorted.map(k => GRADE_COLORS[k] || '#409eff'),
    series: [{
      type: 'pie', radius: ['45%', '70%'], avoidLabelOverlap: false,
      label: { show: true, formatter: '{b}\n{c} 人' },
      data: sorted.map(k => ({ name: k, value: gd[k] }))
    }]
  })
}

function onResize() { gradeChart?.resize() }

onMounted(async () => {
  try {
    data.value = await req('get', '/teacher/dashboard')
    await nextTick()
    renderChart()
    window.addEventListener('resize', onResize)
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  gradeChart?.dispose()
})

watch(data, () => renderChart(), { deep: true })
</script>

<style scoped>
.overview { display: flex; flex-direction: column; gap: 16px; }

.stat-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; }
.stat-card { background: #fff; border-radius: 8px; padding: 18px 20px; border-left: 4px solid var(--c); box-shadow: 0 1px 3px rgba(0,0,0,0.05); cursor: pointer; transition: transform 0.15s, box-shadow 0.15s; }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.stat-label { font-size: 13px; color: #909399; }
.stat-num { font-size: 28px; font-weight: 600; color: var(--c); margin: 6px 0 4px; }
.stat-sub { font-size: 12px; color: #909399; }

.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
@media (max-width: 960px) { .chart-row { grid-template-columns: 1fr; } }

.page-card { background: #fff; border-radius: 6px; padding: 16px 20px; box-shadow: 0 1px 4px rgba(0,21,41,0.06); }
.page-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 8px; }
.chart-box { height: 280px; }

.summary-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 8px; }
.summary-item { text-align: center; padding: 12px; background: #f5f7fa; border-radius: 6px; }
.summary-label { font-size: 12px; color: #909399; display: block; }
.summary-value { font-size: 22px; font-weight: 600; margin-top: 4px; display: block; }

.quick-links { margin-top: 8px; }
.ql-title { font-size: 13px; color: #606266; margin-bottom: 8px; font-weight: 500; }
.ql-grid { display: flex; gap: 8px; flex-wrap: wrap; }
</style>