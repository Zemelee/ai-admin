<template>
  <template v-if="list.length">
    <div class="announce-bar">
      <div class="bar-inner" v-for="item in list" :key="item.id">
        <el-tag
          :type="priorityTag(item.priority)"
          size="small"
          effect="dark"
          class="tag"
        >
          {{ priorityLabel(item.priority) }}
        </el-tag>
        <span class="text" @click="openDetail(item)">{{ item.title }}</span>
        <span class="time">{{ fmtDate(item.publishTime) }}</span>
      </div>
    </div>

    <!-- 公告详情弹窗 -->
    <el-dialog v-model="visible" title="公告详情" width="640px" @close="current = null">
      <div v-if="current" class="detail">
        <div class="detail-head">
          <el-tag :type="priorityTag(current.priority)" size="small" effect="dark" class="tag">
            {{ priorityLabel(current.priority) }}
          </el-tag>
          <span class="detail-title">{{ current.title }}</span>
        </div>
        <div class="detail-time">
          {{ current.publishTime ? '发布：' + fmtDateTime(current.publishTime) : '' }}
        </div>
        <div class="detail-content" v-html="renderContent(current.content)" />
      </div>
      <template #footer>
        <el-button @click="visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </template>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { announcementLatest, PRIORITY_LABEL, PRIORITY_TAG } from '@/api/announcement'

const list = ref([])
const visible = ref(false)
const current = ref(null)

function priorityLabel(p) { return PRIORITY_LABEL[p] || p }
function priorityTag(p) { return PRIORITY_TAG[p] || 'info' }

function fmtDate(s) {
  if (!s) return ''
  return String(s).slice(0, 10)
}

function fmtDateTime(s) {
  if (!s) return ''
  return String(s).replace('T', ' ').slice(0, 16)
}

function renderContent(content) {
  if (!content) return ''
  let html = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`{3}([\s\S]*?)`{3}/g, '<pre><code>$1</code></pre>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
  return html
}

function openDetail(item) {
  current.value = item
  visible.value = true
}

onMounted(async () => {
  try {
    list.value = await announcementLatest(3)
  } catch {
    // 静默失败，不影响主页面
  }
})
</script>

<style scoped>
.announce-bar {
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
  border-bottom: 1px solid #bae7ff;
  padding: 6px 20px;
  display: flex;
  gap: 24px;
  overflow-x: auto;
  font-size: 13px;
}
.bar-inner {
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}
.tag { flex-shrink: 0; }
.text { color: #333; cursor: pointer; }
.text:hover { color: #409eff; }
.time { color: #999; font-size: 12px; }

.detail { padding: 8px 0; }
.detail-head { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.detail-title { font-size: 18px; font-weight: 600; }
.detail-time { font-size: 12px; color: #909399; margin-bottom: 16px; }
.detail-content { font-size: 14px; line-height: 1.8; color: #333; white-space: pre-wrap; }
</style>