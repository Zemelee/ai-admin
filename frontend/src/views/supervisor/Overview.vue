<template>
  <div v-loading="loading" class="overview">
    <!-- 4 张统计卡片 -->
    <div class="stat-grid">
      <div class="stat-card" style="--c: #409eff">
        <div class="stat-label">系统账号</div>
        <div class="stat-num">{{ overview?.user?.total ?? '-' }}</div>
        <div class="stat-sub">已启用 {{ overview?.user?.enabled ?? '-' }}</div>
      </div>
      <div class="stat-card" style="--c: #67c23a">
        <div class="stat-label">实习生（学生）</div>
        <div class="stat-num">{{ overview?.student?.total ?? '-' }}</div>
        <div class="stat-sub">实习中 {{ activeCount }}</div>
      </div>
      <div class="stat-card" style="--c: #e6a23c">
        <div class="stat-label">指导教师</div>
        <div class="stat-num">{{ overview?.teacher?.total ?? '-' }}</div>
        <div class="stat-sub">企业指导 mentor {{ overview?.mentor?.total ?? '-' }}</div>
      </div>
      <div class="stat-card" style="--c: #f56c6c">
        <div class="stat-label">合作企业</div>
        <div class="stat-num">{{ overview?.company?.total ?? '-' }}</div>
        <div class="stat-sub">禁入清单 {{ overview?.company?.blacklist ?? 0 }}</div>
      </div>
    </div>

    <!-- 第 1 行：两饼图 -->
    <div class="chart-row">
      <div class="page-card">
        <div class="page-title">实习状态分布</div>
        <div ref="statusChartRef" class="chart-box"></div>
      </div>
      <div class="page-card">
        <div class="page-title">成绩等级分布</div>
        <div ref="gradeChartRef" class="chart-box"></div>
      </div>
    </div>

    <!-- 第 2 行：两柱状图 -->
    <div class="chart-row">
      <div class="page-card">
        <div class="page-title">各专业平均分 TOP 10</div>
        <div ref="majorChartRef" class="chart-box"></div>
      </div>
      <div class="page-card">
        <div class="page-title">各企业平均分 TOP 10</div>
        <div ref="companyChartRef" class="chart-box"></div>
      </div>
    </div>

    <!-- 第 3 行：预警 + 鉴定雷达 -->
    <div class="chart-row">
      <div class="page-card">
        <div class="page-title">三色预警统计</div>
        <div ref="warningChartRef" class="chart-box"></div>
      </div>
      <div class="page-card">
        <div class="page-title">企业鉴定维度平均分</div>
        <div ref="evalRadarRef" class="chart-box"></div>
      </div>
    </div>

    <!-- 数据摘要 -->
    <div class="summary-row" v-if="dashboard?.scoreStats">
      <div class="summary-item">
        <span class="summary-label">已评分学生</span>
        <span class="summary-value">{{ dashboard.scoreStats.count }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">平均分</span>
        <span class="summary-value" style="color:#409eff">{{ fmt(dashboard.scoreStats.avg) }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">最高分</span>
        <span class="summary-value" style="color:#67c23a">{{ fmt(dashboard.scoreStats.max) }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">最低分</span>
        <span class="summary-value" style="color:#f56c6c">{{ fmt(dashboard.scoreStats.min) }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">待处理预警</span>
        <span class="summary-value" style="color:#e6a23c">{{ dashboard.warningStats?.pendingTotal ?? '-' }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">企业鉴定数</span>
        <span class="summary-value" style="color:#909399">{{ dashboard.evalDimensionAvg?.count ?? 0 }}</span>
      </div>
    </div>

    <!-- 底部两表 -->
    <div class="chart-row">
      <div class="page-card">
        <div class="page-title">最近录入的学生</div>
        <el-empty v-if="!overview?.recentStudents?.length" description="暂无数据" />
        <el-table v-else :data="overview.recentStudents" size="small" border stripe>
          <el-table-column prop="studentNo" label="学号" width="130" />
          <el-table-column prop="className" label="班级" />
          <el-table-column prop="major" label="专业" />
          <el-table-column prop="grade" label="年级" width="80" />
          <el-table-column prop="internStatus" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.internStatus)" size="small">{{ statusLabel(row.internStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="internStart" label="开始" width="120" />
          <el-table-column prop="internEnd" label="结束" width="120" />
        </el-table>
      </div>
      <div class="page-card">
        <div class="page-title">企业禁入清单（最近）</div>
        <el-empty v-if="!overview?.blacklistTop5?.length" description="暂无禁入企业" />
        <el-table v-else :data="overview.blacklistTop5" size="small" border>
          <el-table-column type="index" width="50" label="#" />
          <el-table-column prop="name" label="企业名称" />
          <el-table-column prop="industry" label="行业" width="100" />
          <el-table-column prop="blacklistReason" label="禁入原因" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { req } from '@/utils/http'

const loading = ref(true)
const overview = ref(null)
const dashboard = ref(null)

// 图表 refs
const statusChartRef = ref(null)
const gradeChartRef = ref(null)
const majorChartRef = ref(null)
const companyChartRef = ref(null)
const warningChartRef = ref(null)
const evalRadarRef = ref(null)

// 图表实例
const charts = {}

const activeCount = computed(() => overview.value?.student?.statusMap?.ACTIVE ?? 0)

const STATUS_MAP = {
  ACTIVE: { label: '实习中', type: 'success' },
  SUSPEND: { label: '已暂停', type: 'warning' },
  FINISHED: { label: '已结束', type: 'info' },
  QUIT: { label: '已退出', type: 'danger' }
}
function statusLabel(s) { return STATUS_MAP[s]?.label || s }
function statusType(s) { return STATUS_MAP[s]?.type || 'info' }

const GRADE_COLORS = {
  '优秀': '#67c23a',
  '良好': '#409eff',
  '中等': '#e6a23c',
  '及格': '#f56c6c',
  '不及格': '#909399',
  '无法评定': '#dcdfe6'
}

function fmt(v) {
  if (v == null) return '-'
  return typeof v === 'number' ? v.toFixed(1) : String(v)
}

// ---------- 图表渲染 ----------
// 图表 ref 映射
const chartRefMap = {
  statusChartRef, gradeChartRef, majorChartRef,
  companyChartRef, warningChartRef, evalRadarRef
}

function initChart(name) {
  const el = chartRefMap[name]?.value
  if (!el) return null
  if (charts[name]) charts[name].dispose()
  charts[name] = echarts.init(el)
  return charts[name]
}

function renderAll() {
  if (!dashboard.value) return

  // 1. 实习状态饼图（数据来自 overview）
  if (overview.value?.student?.statusMap) {
    const ch = initChart('statusChartRef')
    if (ch) {
      const sm = overview.value.student.statusMap
      ch.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} 人 ({d}%)' },
        legend: { bottom: 0 },
        series: [{
          type: 'pie', radius: ['45%', '70%'], avoidLabelOverlap: false,
          label: { show: true, formatter: '{b}\n{c} 人' },
          data: Object.entries(sm).map(([k, v]) => ({
            name: STATUS_MAP[k]?.label || k, value: v
          }))
        }]
      })
    }
  }

  // 2. 成绩等级饼图
  const gd = dashboard.value.gradeDistribution
  if (gd) {
    const ch = initChart('gradeChartRef')
    if (ch) {
      const keys = Object.keys(gd)
      // 确保优秀的绿色在前
      const order = ['优秀', '良好', '中等', '及格', '不及格', '无法评定']
      const sorted = order.filter(k => k in gd).concat(keys.filter(k => !order.includes(k)))
      ch.setOption({
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
  }

  // 3. 各专业平均分柱状图
  const majors = dashboard.value.majorAvg
  if (majors && majors.length) {
    const ch = initChart('majorChartRef')
    if (ch) {
      ch.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 100, right: 20, top: 10, bottom: 30 },
        xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value} 分' } },
        yAxis: { type: 'category', data: majors.map(m => m.major).reverse(), axisLabel: { fontSize: 11 } },
        series: [{
          type: 'bar', data: majors.map(m => m.avgScore).reverse(),
          itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#409eff' }, { offset: 1, color: '#79bbff' }
          ]) },
          label: { show: true, position: 'right', formatter: p => p.value.toFixed(1) }
        }]
      })
    }
  }

  // 4. 各企业平均分柱状图
  const companies = dashboard.value.companyAvg
  if (companies && companies.length) {
    const ch = initChart('companyChartRef')
    if (ch) {
      ch.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 110, right: 20, top: 10, bottom: 30 },
        xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value} 分' } },
        yAxis: { type: 'category', data: companies.map(m => m.companyName).reverse(), axisLabel: { fontSize: 11 } },
        series: [{
          type: 'bar', data: companies.map(m => m.avgScore).reverse(),
          itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#67c23a' }, { offset: 1, color: '#95d475' }
          ]) },
          label: { show: true, position: 'right', formatter: p => p.value.toFixed(1) }
        }]
      })
    }
  }

  // 5. 三色预警统计
  const ws = dashboard.value.warningStats
  if (ws?.ruleMap) {
    const ch = initChart('warningChartRef')
    if (ch) {
      const RULE_LABELS = {
        NO_LOG_3D: '超3天未写日志',
        SENSITIVE_WORD: '敏感词命中',
        COMPANY_BLACKLIST: '企业禁入清单',
        TRANSFER_PENDING_3D: '变更停滞超3天',
        LEAVE_OVER_3M: '请假超90天'
      }
      const YELLOW_RULES = ['NO_LOG_3D', 'TRANSFER_PENDING_3D', 'LEAVE_OVER_3M']
      const entryList = Object.entries(ws.ruleMap)
      ch.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 10, right: 110, top: 10, bottom: 30 },
        xAxis: { type: 'category', data: entryList.map(([k]) => RULE_LABELS[k] || k), axisLabel: { rotate: 15, fontSize: 10 } },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{
          type: 'bar',
          data: entryList.map(([k, v]) => ({
            value: v,
            itemStyle: { color: YELLOW_RULES.includes(k) ? '#e6a23c' : '#f56c6c' }
          })),
          label: { show: true, position: 'top', formatter: '{c}' }
        }]
      })
    }
  }

  // 6. 企业鉴定维度雷达图
  const dim = dashboard.value.evalDimensionAvg
  if (dim && dim.attendance !== undefined && dim.attendance > 0) {
    const ch = initChart('evalRadarRef')
    if (ch) {
      ch.setOption({
        tooltip: {},
        radar: {
          indicator: [
            { name: '出勤与纪律', max: 5 },
            { name: '专业能力', max: 5 },
            { name: '工作态度', max: 5 },
            { name: '综合评价', max: 5 }
          ],
          center: ['50%', '50%'],
          radius: '65%'
        },
        series: [{
          type: 'radar',
          data: [{
            value: [dim.attendance, dim.ability, dim.attitude, dim.overall],
            name: '维度平均分',
            areaStyle: { color: 'rgba(64,158,255,0.2)' },
            lineStyle: { color: '#409eff', width: 2 },
            itemStyle: { color: '#409eff' }
          }]
        }]
      })
    }
  }
}

function onResize() {
  Object.values(charts).forEach(ch => ch?.resize())
}

onMounted(async () => {
  try {
    const [ov, db] = await Promise.all([
      req('get', '/supervisor/overview'),
      req('get', '/supervisor/dashboard')
    ])
    overview.value = ov
    dashboard.value = db
    await nextTick()
    renderAll()
    window.addEventListener('resize', onResize)
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  Object.values(charts).forEach(ch => ch?.dispose())
})

watch(dashboard, () => renderAll(), { deep: true })
</script>

<style scoped>
.overview { display: flex; flex-direction: column; gap: 16px; }

.stat-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; }
.stat-card { background: #fff; border-radius: 8px; padding: 18px 20px; border-left: 4px solid var(--c); box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
.stat-label { font-size: 13px; color: #909399; }
.stat-num { font-size: 28px; font-weight: 600; color: var(--c); margin: 6px 0 4px; }
.stat-sub { font-size: 12px; color: #909399; }

.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
@media (max-width: 960px) { .chart-row { grid-template-columns: 1fr; } }

.page-card { background: #fff; border-radius: 6px; padding: 16px 20px; box-shadow: 0 1px 4px rgba(0,21,41,0.06); }
.page-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 8px; }
.chart-box { height: 300px; }

.summary-row { display: flex; gap: 16px; flex-wrap: wrap; }
.summary-item { flex: 1; min-width: 120px; background: #fff; border-radius: 6px; padding: 14px 18px; box-shadow: 0 1px 4px rgba(0,21,41,0.06); text-align: center; }
.summary-label { font-size: 12px; color: #909399; display: block; }
.summary-value { font-size: 22px; font-weight: 600; margin-top: 4px; display: block; }
</style>