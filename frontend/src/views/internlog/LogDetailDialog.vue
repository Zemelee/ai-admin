<template>
  <el-dialog v-model="visible" title="日志详情" width="660px" @close="onClose">
    <div v-loading="loading">
      <el-descriptions :column="2" border v-if="data">
        <el-descriptions-item label="学生">{{ data.studentName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ data.studentNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="日期">{{ fmtDate(data.logDate) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="STATUS_TAG[data.status]" effect="light">{{ STATUS_LABEL[data.status] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ fmtDateTime(data.submitTime) || '—' }}</el-descriptions-item>
        <el-descriptions-item label="企业确认">{{ fmtDateTime(data.mentorReviewTime) || '—' }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="data" class="detail-section">
        <div class="detail-label">
          日志内容
          <el-tag v-if="data.sensitiveHit" type="danger" size="small" effect="dark" style="margin-left:8px">
            AI 检出敏感词
          </el-tag>
          <span v-if="data.sensitiveWords" class="sensitive-words">命中：{{ data.sensitiveWords }}</span>
        </div>
        <!-- 命中敏感词时展示高亮 HTML，否则纯文本 -->
        <div
          v-if="data.sensitiveHit && data.sensitiveMarkedHtml"
          class="detail-content sensitive"
          v-html="data.sensitiveMarkedHtml"
        />
        <div v-else class="detail-content">{{ data.content }}</div>
      </div>

      <div v-if="data && data.mentorComment" class="detail-section">
        <div class="detail-label">企业指导意见</div>
        <div class="detail-content">{{ data.mentorComment }}</div>
      </div>

      <div v-if="data && data.attachments && data.attachments.length" class="detail-section">
        <div class="detail-label">图片附件</div>
        <ImageUploader :model-value="data.attachments" biz-type="LOG" readonly />
      </div>
      <el-empty v-if="data && !data.attachments?.length" description="无图片附件" :image-size="60" />
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import ImageUploader from '@/components/ImageUploader.vue'
import { logDetail, STATUS_LABEL, STATUS_TAG, fmtDate, fmtDateTime } from '@/api/internshipLog'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  logId: { type: [Number, String], default: null }
})
const emit = defineEmits(['update:modelValue'])

const visible = ref(props.modelValue)
const loading = ref(false)
const data = ref(null)

watch(
  () => props.modelValue,
  (v) => {
    visible.value = v
    if (v && props.logId) load()
  }
)
watch(visible, (v) => emit('update:modelValue', v))

async function load() {
  loading.value = true
  data.value = null
  try {
    data.value = await logDetail(props.logId)
  } finally {
    loading.value = false
  }
}
function onClose() {
  data.value = null
}
</script>

<style scoped>
.detail-section { margin-top: 16px; }
.detail-label { font-size: 13px; color: #909399; margin-bottom: 6px; display: flex; align-items: center; }
.sensitive-words { margin-left: 8px; color: #f56c6c; font-size: 12px; }
.detail-content {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}
.detail-content.sensitive :deep(.sensitive) {
  color: #fff;
  background: #f56c6c;
  border-radius: 3px;
  padding: 0 4px;
  font-weight: 600;
}
</style>
