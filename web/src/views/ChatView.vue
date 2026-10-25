<template>
  <div class="atp-chat">
    <aside class="atp-chat-left">
      <div class="atp-assistant-card">
        <div class="atp-assistant-avatar"><el-icon><MagicStick /></el-icon></div>
        <div>
          <div class="atp-assistant-name">售后工单助手</div>
          <div class="atp-assistant-desc">可查工单、检索知识库，写操作会进入审批中心</div>
        </div>
      </div>

      <el-button class="atp-new-btn" type="primary" plain :loading="creating" @click="newSession">
        <el-icon><Plus /></el-icon> 新建会话
      </el-button>

      <div class="atp-side-title">历史会话</div>
      <div class="atp-session-list">
        <div v-if="!sessions.length" class="atp-empty">还没有历史会话</div>
        <div
          v-for="s in sessions"
          :key="s.memoryId"
          class="atp-session"
          :class="{ 'is-active': s.memoryId === memoryId }"
          @click="openSession(s)"
        >
          <div class="atp-session-main">
            <div class="atp-session-title">{{ s.title || '新会话' }}</div>
            <div class="atp-session-time">{{ fromNow(s.lastActiveAt) }}</div>
          </div>
          <el-button
            class="atp-session-delete"
            link
            type="danger"
            :icon="Delete"
            @click.stop="removeSession(s)"
          />
        </div>
      </div>
    </aside>

    <section class="atp-chat-center">
      <div ref="scrollerRef" class="atp-messages">
        <div v-if="!messages.length" class="atp-chat-welcome">
          <div class="atp-welcome-title">开始和售后工单助手对话</div>
          <div class="atp-welcome-sub">回答会流式返回，工具执行和引用可在右侧查看</div>
          <div class="atp-chips">
            <span v-for="q in quickQuestions" :key="q" class="atp-chip" @click="send(q)">{{ q }}</span>
          </div>
        </div>

        <div v-for="(m, i) in messages" :key="m.key || i" class="atp-msg" :class="'is-' + m.role">
          <div class="atp-msg-avatar">
            <el-icon v-if="m.role === 'user'"><UserFilled /></el-icon>
            <el-icon v-else><MagicStick /></el-icon>
          </div>

          <div class="atp-msg-body">
            <div class="atp-msg-meta">
              <span class="atp-msg-who">{{ m.role === 'user' ? store.name : 'Agent' }}</span>
              <span v-if="m.runNo" class="atp-msg-run">{{ m.runNo }}</span>
              <span v-if="m.status" class="atp-tag" :style="runTagStyle(m.status)">{{ runLabel(m.status) }}</span>
            </div>

            <div v-if="m.loading" class="atp-msg-content atp-msg-loading">
              <el-icon class="is-loading"><Loading /></el-icon> 正在思考…
            </div>
            <div v-else class="atp-msg-content">{{ m.content }}</div>

            <div v-if="m.toolCalls && m.toolCalls.length" class="atp-msg-tools">
              <span v-for="t in m.toolCalls" :key="t" class="atp-tool-chip">
                <el-icon><Tools /></el-icon>{{ t }}
              </span>
            </div>

            <ApprovalCard
              v-if="m.approval"
              :approval="m.approval"
              @decided="() => onDecided(m)"
            />

            <div v-if="m.references && m.references.length" class="atp-msg-refs">
              <div class="atp-msg-refs-title">知识库引用</div>
              <KbHitList :hits="m.references" />
            </div>

            <div v-if="m.runId && !m.loading" class="atp-msg-actions">
              <el-button link size="small" @click="showSteps(m)">看运行轨迹</el-button>
            </div>
          </div>
        </div>
      </div>

      <div class="atp-composer">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="说点什么，Ctrl + Enter 发送"
          @keydown.ctrl.enter.prevent="send()"
        />
        <div class="atp-composer-bar">
          <span class="atp-composer-hint">{{ memoryId ? '会话 ' + memoryId : '正在准备会话…' }}</span>
          <el-button type="primary" :loading="sending" :disabled="!memoryId" @click="send()">
            <el-icon><Promotion /></el-icon> 发送
          </el-button>
        </div>
      </div>
    </section>

    <aside class="atp-chat-right">
      <ToolCallPanel :tool-calls="lastRun.toolCalls" :meta="lastRun" />

      <div class="atp-tools-title atp-mt">运行轨迹</div>
      <div v-if="!steps.length" class="atp-empty">完成一次回答后可查看</div>
      <el-timeline v-else class="atp-timeline">
        <el-timeline-item
          v-for="s in steps"
          :key="s.id"
          :timestamp="'#' + s.seq + ' · ' + s.node"
          placement="top"
          :color="s.status === 'error' ? '#dc2626' : '#e8833a'"
        >
          <div class="atp-step">
            <div v-if="s.toolCode" class="atp-step-tool">
              <el-icon><Tools /></el-icon>{{ s.toolCode }}
            </div>
            <div class="atp-step-line">{{ compact(s.outputSnap || s.inputSnap || '-') }}</div>
            <div class="atp-step-foot">
              <span>{{ s.status || '-' }}</span>
              <span>{{ s.costMs || 0 }} ms</span>
            </div>
            <div v-if="s.errorMsg" class="atp-step-err">{{ s.errorMsg }}</div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </aside>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import {
  createSession,
  deleteSession,
  getLatestRun,
  getReferences,
  getRunSteps,
  listSessionMessages,
  listSessions,
  streamChat
} from '../api/chat'
import { listApprovalsByRun } from '../api/approval'
import ApprovalCard from '../components/ApprovalCard.vue'
import ToolCallPanel from '../components/ToolCallPanel.vue'
import KbHitList from '../components/KbHitList.vue'
import { useUserStore } from '../store/user'
import { fromNow } from '../utils/format'

defineOptions({ name: 'ChatView' })

const store = useUserStore()
const route = useRoute()
const router = useRouter()

const sessions = ref([])
const memoryId = ref('')
const messages = ref([])
const draft = ref('')
const sending = ref(false)
const creating = ref(false)
const steps = ref([])
const scrollerRef = ref()
let controller = null

const quickQuestions = [
  '帮我查一下待受理的工单',
  '现在有哪些已经超期的工单',
  '把工单 TK202610010001 指派给李四',
  '退款一般多久能到账',
  '机器人回答不准，我要投诉'
]

const lastRun = computed(() => {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    const m = messages.value[i]
    if (m.role === 'ai' && m.runId) {
      return { ...m, toolCalls: m.toolCalls || [] }
    }
  }
  return { toolCalls: [] }
})

const RUN_STATUS = {
  running: { label: '执行中', color: '#d97706' },
  waiting_approval: { label: '待审批', color: '#d97706' },
  success: { label: '成功', color: '#15803d' },
  failed: { label: '失败', color: '#dc2626' },
  cancelled: { label: '已取消', color: '#78716c' }
}
const runLabel = (status) => (RUN_STATUS[status] || { label: status || '-' }).label
const runTagStyle = (status) => {
  const info = RUN_STATUS[status] || { color: '#78716c' }
  return { background: info.color + '1a', color: info.color }
}

onMounted(async () => {
  await loadSessions()
  if (sessions.value.length) {
    await openSession(sessions.value[0])
  } else {
    await newSession()
  }
})

onBeforeUnmount(() => controller?.abort())

async function loadSessions() {
  try {
    sessions.value = await listSessions()
  } catch (e) {
    sessions.value = []
  }
}

async function newSession() {
  if (creating.value) return
  creating.value = true
  try {
    memoryId.value = await createSession(1)
    messages.value = []
    steps.value = []
    draft.value = ''
    await loadSessions()
  } finally {
    creating.value = false
  }
}

async function openSession(session) {
  if (!session?.memoryId) return
  memoryId.value = session.memoryId
  steps.value = []
  try {
    const history = await listSessionMessages(session.memoryId)
    messages.value = (history || [])
      .filter((m) => ['user', 'ai'].includes(m.role) && m.content)
      .map((m, index) => ({
        key: 'h-' + (m.id || index),
        role: m.role === 'user' ? 'user' : 'ai',
        content: m.content,
        toolCalls: [],
        references: []
      }))
    await scrollToBottom()
  } catch (e) {
    messages.value = []
  }
}

async function removeSession(session) {
  await ElMessageBox.confirm('删除后该会话的历史消息也会一起删除，确定吗？', '提示', { type: 'warning' })
  await deleteSession(session.memoryId)
  ElMessage.success('会话已删除')
  if (session.memoryId === memoryId.value) {
    memoryId.value = ''
    messages.value = []
    steps.value = []
  }
  await loadSessions()
  if (!sessions.value.length) await newSession()
}

async function send(text) {
  const content = (text || draft.value || '').trim()
  if (!content || sending.value) return
  if (!memoryId.value) {
    ElMessage.warning('会话还没有准备好，请稍后重试')
    return
  }

  messages.value.push({ key: 'u-' + Date.now(), role: 'user', content })
  draft.value = ''
  const placeholder = {
    key: 'a-' + Date.now(),
    role: 'ai',
    content: '',
    loading: true,
    toolCalls: [],
    references: []
  }
  messages.value.push(placeholder)
  sending.value = true
  await scrollToBottom()

  controller = new AbortController()
  try {
    await streamChat({
      memoryId: memoryId.value,
      message: content,
      signal: controller.signal,
      onChunk(chunk) {
        placeholder.loading = false
        placeholder.content += chunk
        scrollToBottom()
      }
    })
    placeholder.loading = false
    if (!placeholder.content.trim()) placeholder.content = '本次没有返回文本内容。'

    const run = await getLatestRun(memoryId.value)
    if (run) {
      placeholder.runId = run.id
      placeholder.runNo = run.runNo
      placeholder.status = run.status
      await hydrateRunEvidence(placeholder, run.id)
    }
    await loadSessions()
  } catch (e) {
    placeholder.loading = false
    if (e.name === 'AbortError') return
    if (/登录已过期/.test(e.message || '')) {
      router.push({ path: '/login', query: { redirect: route.fullPath } })
      return
    }
    placeholder.status = 'failed'
    placeholder.content = placeholder.content || ('请求失败：' + (e.message || '网络异常'))
    ElMessage.error(e.message || '对话请求失败')
  } finally {
    sending.value = false
    controller = null
    await scrollToBottom()
  }
}

async function hydrateRunEvidence(message, runId) {
  try {
    steps.value = await getRunSteps(runId)
    message.toolCalls = steps.value.map((s) => s.toolCode).filter(Boolean)
  } catch (e) {
    steps.value = []
  }

  if (store.can('kb:read')) {
    try {
      message.references = await getReferences(runId)
    } catch (e) {
      message.references = []
    }
  }

  if (store.can('ticket:approve')) {
    try {
      const approvals = await listApprovalsByRun(runId)
      message.approval = (approvals || []).find((a) => a.status === 'pending') || null
    } catch (e) {
      message.approval = null
    }
  }
}

async function showSteps(message) {
  try {
    steps.value = await getRunSteps(message.runId)
    ElMessage.success('右侧已显示这次运行的轨迹')
  } catch (e) {
    steps.value = []
  }
}

async function onDecided(message) {
  message.approval = null
  if (message.runId) {
    try {
      steps.value = await getRunSteps(message.runId)
    } catch (e) {
      steps.value = []
    }
  }
}

function compact(value) {
  const text = String(value || '-').replace(/\s+/g, ' ')
  return text.length > 160 ? text.slice(0, 160) + '…' : text
}

async function scrollToBottom() {
  await nextTick()
  const el = scrollerRef.value
  if (el) el.scrollTop = el.scrollHeight
}
</script>

<style scoped>
.atp-chat { display: flex; height: 100%; min-height: 0; }
.atp-chat-left { width: 238px; flex: 0 0 238px; border-right: 1px solid var(--atp-border); background: #fff; padding: 14px 12px; overflow-y: auto; }
.atp-assistant-card { display: flex; gap: 9px; padding: 10px; border-radius: 7px; background: var(--atp-primary-soft); }
.atp-assistant-avatar { width: 32px; height: 32px; flex: 0 0 32px; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: linear-gradient(135deg, #ffb066, #e8833a); color: #fff; }
.atp-assistant-name { font-size: 13px; font-weight: 600; color: var(--atp-text); }
.atp-assistant-desc { margin-top: 3px; font-size: 11.5px; line-height: 17px; color: var(--atp-text-mute); }
.atp-new-btn { width: 100%; margin-top: 12px; }
.atp-side-title { margin: 17px 0 8px; font-size: 12px; font-weight: 600; color: var(--atp-text-weak); }
.atp-session-list { display: flex; flex-direction: column; gap: 4px; }
.atp-session { display: flex; align-items: center; padding: 8px 7px 8px 9px; border: 1px solid transparent; border-radius: 5px; cursor: pointer; }
.atp-session:hover { background: #f7f7f7; }
.atp-session.is-active { background: var(--atp-primary-soft); border-color: #f2d3b7; }
.atp-session-main { min-width: 0; flex: 1; }
.atp-session-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12.5px; color: var(--atp-text); }
.atp-session-time { margin-top: 2px; font-size: 11px; color: var(--atp-text-mute); }
.atp-session-delete { opacity: 0; }
.atp-session:hover .atp-session-delete { opacity: 1; }
.atp-chat-center { min-width: 0; flex: 1; display: flex; flex-direction: column; background: #f8f8f8; }
.atp-messages { flex: 1; overflow-y: auto; padding: 22px 24px; }
.atp-chat-welcome { padding: 40px 0; text-align: center; }
.atp-welcome-title { font-size: 15px; font-weight: 600; color: var(--atp-text); }
.atp-welcome-sub { margin: 6px 0 14px; font-size: 12.5px; color: var(--atp-text-mute); }
.atp-chips { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.atp-chip { cursor: pointer; padding: 5px 12px; border: 1px solid #f0cdae; border-radius: 14px; background: #fff; color: var(--atp-primary-dark); font-size: 12.5px; }
.atp-chip:hover { background: var(--atp-primary-soft); }
.atp-msg { display: flex; gap: 10px; margin-bottom: 18px; }
.atp-msg.is-user { flex-direction: row-reverse; }
.atp-msg-avatar { width: 32px; height: 32px; flex: 0 0 32px; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: #e9e9e9; color: var(--atp-text-weak); }
.atp-msg.is-ai .atp-msg-avatar { background: linear-gradient(135deg, #ffb066, #e8833a); color: #fff; }
.atp-msg-body { max-width: 80%; min-width: 0; }
.atp-msg-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; font-size: 11.5px; color: var(--atp-text-mute); }
.atp-msg.is-user .atp-msg-meta { justify-content: flex-end; }
.atp-msg-who { color: var(--atp-text-weak); }
.atp-msg-run { font-family: Consolas, Monaco, monospace; font-size: 11px; }
.atp-msg-content { padding: 10px 12px; border: 1px solid var(--atp-border); border-radius: 8px; background: #fff; color: var(--atp-text); font-size: 13.5px; line-height: 21px; white-space: pre-wrap; word-break: break-word; }
.atp-msg.is-user .atp-msg-content { background: var(--atp-primary-soft); border-color: #f2d3b7; }
.atp-msg-loading { color: var(--atp-text-mute); }
.atp-msg-tools { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 6px; }
.atp-tool-chip { display: inline-flex; align-items: center; gap: 4px; padding: 2px 8px; border-radius: 10px; background: #eef2f5; color: #4b5563; font-family: Consolas, Monaco, monospace; font-size: 11.5px; }
.atp-msg-refs { margin-top: 8px; }
.atp-msg-refs-title { margin-bottom: 6px; color: var(--atp-text-weak); font-size: 12px; }
.atp-msg-actions { display: flex; align-items: center; gap: 4px; margin-top: 6px; }
.atp-composer { flex: 0 0 auto; padding: 12px 20px 14px; border-top: 1px solid var(--atp-border); background: #fff; }
.atp-composer-bar { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; }
.atp-composer-hint { color: var(--atp-text-mute); font-family: Consolas, Monaco, monospace; font-size: 11.5px; }
.atp-chat-right { width: 292px; flex: 0 0 292px; overflow-y: auto; padding: 14px 14px 20px; border-left: 1px solid var(--atp-border); background: #fff; }
.atp-mt { margin-top: 16px; }
.atp-tools-title { margin-bottom: 8px; color: var(--atp-text-weak); font-size: 12px; font-weight: 600; }
.atp-timeline { padding-left: 2px; }
.atp-step { font-size: 12px; }
.atp-step-tool { display: inline-flex; align-items: center; gap: 4px; margin-bottom: 2px; color: var(--atp-primary-dark); font-family: Consolas, Monaco, monospace; }
.atp-step-line { max-height: 52px; overflow: hidden; color: var(--atp-text-weak); line-height: 17px; word-break: break-all; }
.atp-step-foot { display: flex; gap: 8px; color: var(--atp-text-mute); font-size: 11px; }
.atp-step-err { color: #dc2626; }
</style>

