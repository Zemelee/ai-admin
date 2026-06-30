<template>
  <div class="page">
    <!-- 三色统计卡片 -->
    <div class="stat-grid">
      <div class="stat-card red" @click="filterLevel('RED')">
        <div class="stat-label">红色预警</div>
        <div class="stat-num">{{ stats?.levelMap?.RED ?? 0 }}</div>
        <div class="stat-sub">禁入企业 / 敏感词命中</div>
      </div>
      <div class="stat-card yellow" @click="filterLevel('YELLOW')">
        <div class="stat-label">黄色预警</div>
        <div class="stat-num">{{ stats?.levelMap?.YELLOW ?? 0 }}</div>
        <div class="stat-sub">缺交日志 / 变更停滞 / 长期请假</div>
      </div>
      <div class="stat-card green" @click="filterLevel('')">
        <div class="stat-label">待处理合计</div>
        <div class="stat-num">{{ stats?.pendingTotal ?? 0 }}</div>
        <div class="stat-sub">已处理 {{ stats?.handledTotal ?? 0 }}</div>
      </div>
      <div class="stat-card scan" @click="onScan">
        <el-icon class="scan-icon"><Refresh /></el-icon>
        <div class="stat-label">立即扫描</div>
        <div class="stat-sub" style="margin-top:6px">刷新预警数据</div>
      </div>
    </div>

    <!-- 主体两栏 -->
    <div class="main-grid">
      <div class="page-card">
        <div class="page-title">预警规则分布</div>
        <div ref="chartRef" style="height: 280px"></div>
      </div>

      <div class="page-card">
        <div class="page-title">规则说明</div>
        <el-table :data="ruleDescs" size="small" border>
          <el-table-column prop="rule" label="规则" width="120">
            <template #default="{ row }">
              <el-tag :type="RULE_TAG[row.rule]" size="small">{{ RULE_LABEL[row.rule] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="level" label="级别" width="80">
            <template #default="{ row }">
              <el-tag :type="row.level === 'RED' ? 'danger' : 'warning'" size="small" effect="plain">
                {{ row.level === 'RED' ? '红' : '黄' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="desc" label="说明" />
          <el-table-column prop="count" label="待处理" width="80" align="center">
            <template #default="{ row }">
              <b :style="{ color: row.count ? '#f56c6c' : '#c0c4cc' }">{{ row.count }}</b>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 预警列表 -->
    <div class="page-card">
      <div class="card-head">
        <div class="page-title">预警事件</div>
        <div class="card-tools">
          <el-select v-model="q.level" placeholder="级别" clearable style="width:110px" @change="loadList">
            <el-option label="红色" value="RED" />
            <el-option label="黄色" value="YELLOW" />
          </el-select>
          <el-select v-model="q.ruleCode" placeholder="规则" clearable style="width:130px" @change="loadList">
            <el-option v-for="(v, k) in RULE_LABEL" :key="k" :label="v" :value="k" />
          </el-select>
          <el-select v-model="q.status" placeholder="状态" clearable style="width:110px" @change="loadList">
            <el-option label="待处理" value="PENDING" />
            <el-option label="已处理" value="REVIEWED" />
            <el-option label="已忽略" value="IGNORED" />
          </el-select>
          <el-button :icon="Download" @click="onExport">导出 Excel</el-button>
          <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width:100%">
        <el-table-column label="级别" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="LEVEL_TAG[row.level]" size="small" effect="dark">
              {{ row.level === 'RED' ? '红' : '黄' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="规则" width="120">
          <template #default="{ row }">
            <el-tag :type="RULE_TAG[row.ruleCode]" size="small">{{ RULE_LABEL[row.ruleCode] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="学生" min-width="140">
          <template #default="{ row }">
            <div>{{ row.studentName || '—' }}</div>
            <div class="muted">{{ row.studentNo }} · {{ row.className || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="ruleDesc" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="detail" label="详情" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status]" size="small" effect="light">{{ STATUS_LABEL[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发生时间" width="150">
          <template #default="{ row }">{{ fmtDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button size="small" link type="success" @click="openReview(row, 'REVIEWED')">已处理</el-button>
              <el-button size="small" link type="info" @click="openReview(row, 'IGNORED')">忽略</el-button>
            </template>
            <span v-else class="muted">{{ row.reviewerName }} {{ fmtDateTime(row.reviewTime) }}</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无预警事件，状态良好 🎉" />
        </template>
      </el-table>
    </div>

    <!-- 审核对话框 -->
    <el-dialog v-model="reviewVisible" :title="reviewForm.status === 'REVIEWED' ? '标记为已处理' : '忽略预警'" width="480px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="学生">{{ current?.studentName }}（{{ current?.studentNo }}）</el-descriptions-item>
        <el-descriptions-item label="预警">{{ RULE_LABEL[current?.ruleCode] }} · {{ current?.ruleDesc }}</el-descriptions-item>
        <el-descriptions-item label="详情">{{ current?.detail || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-form style="margin-top:16px">
        <el-form-item label="处理备注">
          <el-input v-model="reviewForm.reviewNote" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="可选，记录处理方式" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onReview">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { Download, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  scanWarnings, warningStats, warningList, reviewWarning,
  LEVEL_TAG, STATUS_LABEL, STATUS_TAG, RULE_LABEL, RULE_TAG, fmtDateTime
} from '@/api/warning'
import { download } from '@/utils/download'

const stats = ref(null)
const list = ref([])
const loading = ref(false)
const chartRef = ref(null)
let chart = null

const q = ref({ level: '', ruleCode: '', status: '' })

const ruleDescs = computed(() => [
  { rule: 'SENSITIVE_WORD', level: 'RED', desc: '日志/周记命中用工红线敏感词（夜班/加班/押金等）', count: stats.value?.ruleMap?.SENSITIVE_WORD ?? 0 },
  { rule: 'COMPANY_BLACKLIST', level: 'RED', desc: '学生当前所在企业属禁入清单', count: stats.value?.ruleMap?.COMPANY_BLACKLIST ?? 0 },
  { rule: 'NO_LOG_3D', level: 'YELLOW', desc: '实习中学生超过 3 天未提交日志', count: stats.value?.ruleMap?.NO_LOG_3D ?? 0 },
  { rule: 'TRANSFER_PENDING_3D', level: 'YELLOW', desc: '单位变更申请停滞超过 3 天', count: stats.value?.ruleMap?.TRANSFER_PENDING_3D ?? 0 },
  { rule: 'LEAVE_OVER_3M', level: 'YELLOW', desc: '请假累计超过 90 天', count: stats.value?.ruleMap?.LEAVE_OVER_3M ?? 0 }
])

function renderChart() {
  if (!chartRef.value || !stats.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const rm = stats.value.ruleMap || {}
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, type: 'scroll' },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      avoidLabelOverlap: false,
      label: { show: true, formatter: '{b}\n{c} 条' },
      data: Object.entries(rm).map(([k, v]) => ({
        name: RULE_LABEL[k] || k,
        value: v,
        itemStyle: { color: RULE_TAG[k] === 'danger' ? '#f56c6c' : '#e6a23c' }
      }))
    }]
  })
}

function onResize() { chart?.resize() }

async function loadStats() {
  stats.value = await warningStats()
  await nextTick()
  renderChart()
}

async function loadList() {
  loading.value = true
  try {
    const params = {}
    if (q.value.level) params.level = q.value.level
    if (q.value.ruleCode) params.ruleCode = q.value.ruleCode
    if (q.value.status) params.status = q.value.status
    list.value = await warningList(params)
  } finally {
    loading.value = false
  }
}

function filterLevel(level) {
  q.value.level = level
  loadList()
}

async function loadAll() {
  await Promise.all([loadStats(), loadList()])
}

async function onScan() {
  const n = await scanWarnings()
  ElMessage.success(`扫描完成，新增 ${n} 条预警`)
  loadAll()
}

const reviewVisible = ref(false)
const submitting = ref(false)
const current = ref(null)
const reviewForm = ref({ status: 'REVIEWED', reviewNote: '' })

function openReview(row, status) {
  current.value = row
  reviewForm.value = { status, reviewNote: '' }
  reviewVisible.value = true
}

async function onReview() {
  submitting.value = true
  try {
    await reviewWarning(current.value.id, reviewForm.value)
    ElMessage.success(reviewForm.value.status === 'REVIEWED' ? '已标记为已处理' : '已忽略')
    reviewVisible.value = false
    loadAll()
  } finally {
    submitting.value = false
  }
}

async function onExport() {
  try {
    await download('/export/warnings', '预警列表.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.warning(e.message || '导出失败')
  }
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', onResize)
})
onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
  chart = null
})
watch(stats, () => renderChart(), { deep: true })
</script>

<style scoped>
.page { padding: 16px; display: flex; flex-direction: column; gap: 16px; }
.page-card { background: #fff; border-radius: 6px; padding: 16px 20px; box-shadow: 0 1px 4px rgba(0,21,41,0.06); }
.page-title { font-size: 16px; font-weight: 600; margin-bottom: 12px; }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.card-tools { display: flex; gap: 12px; }
.muted { color: #909399; font-size: 12px; }

.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat-card { background: #fff; border-radius: 8px; padding: 18px 20px; border-left: 4px solid var(--c); box-shadow: 0 1px 3px rgba(0,0,0,0.05); cursor: pointer; transition: transform .15s; }
.stat-card:hover { transform: translateY(-2px); }
.stat-card.red { --c: #f56c6c; }
.stat-card.yellow { --c: #e6a23c; }
.stat-card.green { --c: #409eff; }
.stat-card.scan { --c: #67c23a; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; }
.scan-icon { font-size: 24px; color: #67c23a; }
.stat-label { font-size: 13px; color: #909399; }
.stat-num { font-size: 30px; font-weight: 600; color: var(--c); margin: 6px 0 4px; }
.stat-sub { font-size: 12px; color: #909399; }

.main-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
@media (max-width: 1100px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .main-grid { grid-template-columns: 1fr; }
}
</style>
