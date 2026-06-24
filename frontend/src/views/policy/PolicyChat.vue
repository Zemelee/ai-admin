<template>
  <div class="page">
    <div class="page-card chat-card">
      <div class="card-head">
        <div class="card-title">
          合规知识库 · AI 问答
          <el-tag size="small" type="info" effect="plain" style="margin-left:8px">GLM-4-Air</el-tag>
        </div>
        <div class="card-tools">
          <el-button :icon="Delete" :disabled="!history.length" @click="onClear">清空对话</el-button>
        </div>
      </div>

      <div class="hint">
        基于《学院实习管理办法》核心规则（实习准入禁入 / 时长与变更 / 请假管控 / 用工红线 / 薪酬合规 / 三色预警等）给出参考答复，回答仅供参考，遇个案请联系系部教师。
      </div>

      <div ref="scrollerRef" class="scroller">
        <div v-if="!history.length" class="welcome">
          <div class="welcome-title">你好，我是合规知识库助手 👋</div>
          <div class="welcome-sub">点击下方常见问题快速开始，或直接输入你的问题。</div>
          <div class="quick">
            <el-button
              v-for="q in QUICK_QUESTIONS"
              :key="q"
              size="small"
              round
              :disabled="sending"
              @click="askQuick(q)"
            >
              {{ q }}
            </el-button>
          </div>
        </div>

        <div v-for="(m, i) in history" :key="i" :class="['bubble-row', m.role]">
          <el-avatar :class="['avatar', m.role]" :size="32">
            {{ m.role === 'user' ? 'U' : 'AI' }}
          </el-avatar>
          <div :class="['bubble', m.role]">
            <div v-if="m.role === 'assistant'" class="md" v-html="renderMd(m.content)" />
            <div v-else class="text">{{ m.content }}</div>
          </div>
        </div>

        <div v-if="sending" class="bubble-row assistant">
          <el-avatar class="avatar assistant" :size="32">AI</el-avatar>
          <div class="bubble assistant">
            <span class="dot-anim">●</span>
            <span class="dot-anim" style="animation-delay:.15s">●</span>
            <span class="dot-anim" style="animation-delay:.3s">●</span>
            <span style="margin-left:6px;color:#909399">正在思考…</span>
          </div>
        </div>
      </div>

      <div class="composer">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          resize="none"
          maxlength="500"
          show-word-limit
          placeholder="请输入你想咨询的实习政策问题，Ctrl+Enter 发送"
          @keydown.ctrl.enter.prevent="onSend"
        />
        <div class="send-row">
          <span class="tip">Ctrl + Enter 发送</span>
          <el-button type="primary" :icon="Promotion" :loading="sending" :disabled="!input.trim()" @click="onSend">
            发送
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Promotion } from '@element-plus/icons-vue'
import { askPolicy, QUICK_QUESTIONS } from '@/api/policy'

const history = ref([])  // [{role:'user'|'assistant', content}]
const input = ref('')
const sending = ref(false)
const scrollerRef = ref(null)

async function onSend() {
  const q = input.value.trim()
  if (!q || sending.value) return
  await sendQuestion(q)
}

function askQuick(q) {
  if (sending.value) return
  input.value = q
  onSend()
}

async function sendQuestion(q) {
  history.value.push({ role: 'user', content: q })
  input.value = ''
  await scrollToBottom()
  sending.value = true
  try {
    // 后端会自行裁剪 history，避免上下文过长
    const ctx = history.value.slice(0, -1).map((m) => ({ role: m.role, content: m.content }))
    const res = await askPolicy(q, ctx)
    history.value.push({ role: 'assistant', content: res?.answer || 'AI 暂未给出答复。' })
  } catch (e) {
    history.value.push({ role: 'assistant', content: 'AI 服务暂不可用，请稍后重试。' })
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

function onClear() {
  history.value = []
  ElMessage.success('对话已清空')
}

async function scrollToBottom() {
  await nextTick()
  const el = scrollerRef.value
  if (el) el.scrollTop = el.scrollHeight
}

/**
 * 轻量 markdown 渲染：仅处理换行、加粗、列表前缀、标题分组——
 * AI system prompt 已要求分点输出，无需引入完整 md 库。
 */
function renderMd(text) {
  if (!text) return ''
  const safe = String(text)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  return safe
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/^(\d+[\.、)]\s)/gm, '<span class="li-no">$1</span>')
    .replace(/^[-•]\s+/gm, '• ')
    .replace(/\n/g, '<br/>')
}
</script>

<style scoped>
.page { padding: 16px; height: 100%; box-sizing: border-box; }
.page-card { background: #fff; border-radius: 6px; padding: 16px 20px; box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06); }
.chat-card { display: flex; flex-direction: column; height: calc(100vh - 132px); }
.card-head { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-size: 16px; font-weight: 600; display: flex; align-items: center; }
.hint { color: #909399; font-size: 12px; background: #f5f7fa; padding: 8px 12px; border-radius: 6px; margin: 10px 0; }
.scroller { flex: 1; overflow-y: auto; padding: 8px 4px; }

.welcome { text-align: center; color: #606266; margin-top: 32px; }
.welcome-title { font-size: 18px; font-weight: 600; color: #303133; margin-bottom: 6px; }
.welcome-sub { font-size: 13px; color: #909399; margin-bottom: 16px; }
.quick { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; max-width: 700px; margin: 0 auto; }

.bubble-row { display: flex; gap: 10px; margin: 10px 0; }
.bubble-row.user { flex-direction: row-reverse; }
.avatar { flex-shrink: 0; font-weight: 600; }
.avatar.user { background: #409eff; color: #fff; }
.avatar.assistant { background: #67c23a; color: #fff; }
.bubble { max-width: 70%; padding: 10px 14px; border-radius: 10px; font-size: 14px; line-height: 1.7; }
.bubble.user { background: #ecf5ff; color: #303133; border: 1px solid #d9ecff; border-top-right-radius: 2px; }
.bubble.assistant { background: #f5f7fa; color: #303133; border: 1px solid #ebeef5; border-top-left-radius: 2px; }
.bubble .text { white-space: pre-wrap; word-break: break-word; }
.bubble .md :deep(.li-no) { color: #409eff; font-weight: 600; margin-right: 4px; }

.composer { border-top: 1px solid #ebeef5; padding-top: 10px; margin-top: 6px; }
.send-row { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; }
.tip { color: #c0c4cc; font-size: 12px; }

.dot-anim {
  display: inline-block;
  font-size: 14px;
  color: #409eff;
  animation: blink 1.2s infinite;
}
@keyframes blink {
  0%, 80%, 100% { opacity: 0.2; }
  40% { opacity: 1; }
}
</style>
