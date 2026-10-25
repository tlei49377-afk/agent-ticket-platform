<template>
  <div class="atp-page" v-loading="loading">
    <div class="atp-back">
      <el-button link :icon="ArrowLeft" @click="router.push('/tickets')">返回工单列表</el-button>
    </div>

    <template v-if="detail">
      <div class="atp-card atp-head">
        <div class="atp-head-top">
          <span class="atp-mono atp-head-no">{{ detail.ticketNo }}</span>
          <span class="atp-tag" :style="tagStyle(statusOf(detail.status))">{{ detail.statusLabel }}</span>
          <span class="atp-tag" :style="tagStyle(priorityOf(detail.priority))">{{ detail.priorityLabel }}</span>
          <span class="atp-head-time">创建于 {{ formatDateTime(detail.createTime) }}</span>
        </div>
        <h3 class="atp-head-title">{{ detail.title }}</h3>
        <p class="atp-head-content">{{ detail.content }}</p>

        <el-row :gutter="12" class="atp-head-meta">
          <el-col :span="6"><span class="k">分类</span><span class="v">{{ detail.categoryName || '-' }}</span></el-col>
          <el-col :span="6"><span class="k">提交人</span><span class="v">{{ detail.requesterName || '-' }}</span></el-col>
          <el-col :span="6"><span class="k">处理人</span><span class="v">{{ detail.assigneeName || '未指派' }}</span></el-col>
          <el-col :span="6"><span class="k">响应时限</span><span class="v">{{ formatDateTime(detail.respondDeadline) }}</span></el-col>
          <el-col :span="6" class="mt"><span class="k">解决时限</span><span class="v">{{ formatDateTime(detail.resolveDeadline) }}</span></el-col>
        </el-row>
      </div>

      <div class="atp-toolbar" style="margin-top: 14px">
        <span class="atp-toolbar-label">可流转到</span>
        <el-button
          v-for="s in detail.allowedNextStatus || []"
          :key="s"
          size="small"
          plain
          :disabled="!store.can('ticket:update')"
          @click="openStatus(s)"
        >{{ statusOf(s).label }}</el-button>
        <span v-if="!(detail.allowedNextStatus || []).length" class="atp-toolbar-hint">当前状态没有下一跳</span>

        <el-button
          size="small"
          :icon="UserFilled"
          :disabled="!store.can('ticket:assign')"
          style="margin-left: auto"
          @click="assignVisible = true"
        >指派</el-button>
        <el-button
          size="small"
          :icon="Top"
          :disabled="!store.can('ticket:update')"
          @click="priorityVisible = true"
        >调整优先级</el-button>
      </div>

      <div class="atp-two-col">
        <div class="atp-col-main">
          <div class="atp-section-title">沟通记录</div>
          <el-timeline>
            <el-timeline-item
              v-for="m in detail.messages || []"
              :key="m.id"
              :timestamp="formatDateTime(m.createTime)"
              placement="top"
              :color="roleColor(m.role)"
            >
              <div class="atp-msg-line">
                <span class="atp-msg-who">{{ m.senderName || roleLabel(m.role) }}</span>
                <span class="atp-tag" :style="m.visible === 'internal' ? internalTag : publicTag">
                  {{ m.visible === 'internal' ? '内部备注' : '公开' }}
                </span>
                <span class="atp-msg-role">{{ roleLabel(m.role) }}</span>
              </div>
              <div class="atp-msg-text">{{ m.content }}</div>
            </el-timeline-item>
          </el-timeline>
          <div v-if="!(detail.messages || []).length" class="atp-empty">还没有沟通记录</div>

          <div v-if="store.can('ticket:reply')" class="atp-reply">
            <el-input v-model="replyContent" type="textarea" :rows="3" resize="none" placeholder="写点什么回复客户" />
            <div class="atp-reply-bar">
              <el-radio-group v-model="replyVisible" size="small">
                <el-radio-button label="public">公开回复</el-radio-button>
                <el-radio-button label="internal">内部备注</el-radio-button>
              </el-radio-group>
              <el-button type="primary" :loading="replying" @click="submitReply">发送</el-button>
            </div>
          </div>
        </div>

        <div class="atp-col-side">
          <div class="atp-section-title">状态变更历史</div>
          <el-timeline>
            <el-timeline-item
              v-for="h in history"
              :key="h.id"
              :timestamp="formatDateTime(h.createTime)"
              placement="top"
              color="#e8833a"
            >
              <div class="atp-his-line">
                {{ statusOf(h.fromStatus).label }} →
                <b>{{ statusOf(h.toStatus).label }}</b>
              </div>
              <div class="atp-his-reason">{{ h.reason || '（未填写原因）' }}</div>
              <div class="atp-his-op">操作人：{{ h.operatorName || h.operatorId || '-' }}</div>
            </el-timeline-item>
          </el-timeline>
          <div v-if="!history.length" class="atp-empty">没有状态变更记录</div>
        </div>
      </div>
    </template>

    <!-- 流转状态 -->
    <el-dialog v-model="statusVisible" :title="'流转到：' + statusOf(targetStatus).label" width="440px">
      <el-input v-model="statusReason" type="textarea" :rows="3" placeholder="填写流转原因（可选）" />
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitStatus">确定</el-button>
      </template>
    </el-dialog>

    <!-- 指派 -->
    <el-dialog v-model="assignVisible" title="指派工单" width="440px">
      <el-select v-model="assigneeId" placeholder="选择处理人" style="width: 100%">
        <el-option v-for="u in users" :key="u.id" :label="u.name + '（' + u.username + '）'" :value="u.id" />
      </el-select>
      <el-input v-model="assignReason" type="textarea" :rows="3" placeholder="指派原因（可选）" style="margin-top: 10px" />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAssign">确定</el-button>
      </template>
    </el-dialog>

    <!-- 调整优先级 -->
    <el-dialog v-model="priorityVisible" title="调整优先级" width="440px">
      <el-select v-model="targetPriority" style="width: 100%">
        <el-option v-for="o in priorityOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-input v-model="priorityReason" type="textarea" :rows="3" placeholder="调整原因（可选）" style="margin-top: 10px" />
      <template #footer>
        <el-button @click="priorityVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPriority">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Top, UserFilled } from '@element-plus/icons-vue'
import {
  changeAssignee,
  changePriority,
  changeStatus,
  getTicket,
  getTicketHistory,
  replyTicket
} from '../api/ticket'
import { listAssignableUsers } from '../api/user'
import { useUserStore } from '../store/user'
import { priorityOf, priorityOptions, statusOf } from '../utils/dict'
import { formatDateTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const loading = ref(false)
const detail = ref(null)
const history = ref([])
const users = ref([])
const submitting = ref(false)

const statusVisible = ref(false)
const targetStatus = ref(null)
const statusReason = ref('')

const assignVisible = ref(false)
const assigneeId = ref(null)
const assignReason = ref('')

const priorityVisible = ref(false)
const targetPriority = ref(3)
const priorityReason = ref('')

const replyContent = ref('')
const replyVisible = ref('public')
const replying = ref(false)

const internalTag = { background: '#f3e8ff', color: '#7c3aed' }
const publicTag = { background: '#e8f5ee', color: '#15803d' }

const tagStyle = (info) => ({ background: info.color + '1a', color: info.color })

const ROLE_LABEL = { requester: '用户', agent: '客服', ai: 'AI', system: '系统' }
const roleLabel = (role) => ROLE_LABEL[role] || role
const roleColor = (role) => (role === 'ai' ? '#e8833a' : role === 'agent' ? '#0f766e' : '#9a9a9a')

onMounted(async () => {
  await load()
  if (store.can('ticket:assign')) {
    try {
      users.value = await listAssignableUsers()
    } catch (e) {
      users.value = []
    }
  }
})

async function load() {
  loading.value = true
  try {
    const id = route.params.id
    detail.value = await getTicket(id)
    try {
      history.value = await getTicketHistory(id)
    } catch (e) {
      history.value = []
    }
  } finally {
    loading.value = false
  }
}

function openStatus(s) {
  targetStatus.value = s
  statusReason.value = ''
  statusVisible.value = true
}

async function submitStatus() {
  submitting.value = true
  try {
    await changeStatus(route.params.id, { targetStatus: targetStatus.value, reason: statusReason.value })
    ElMessage.success('状态已更新')
    statusVisible.value = false
    await load()
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

async function submitAssign() {
  if (!assigneeId.value) {
    ElMessage.warning('请先选择处理人')
    return
  }
  submitting.value = true
  try {
    await changeAssignee(route.params.id, { assigneeId: assigneeId.value, reason: assignReason.value })
    ElMessage.success('已指派')
    assignVisible.value = false
    await load()
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

async function submitPriority() {
  submitting.value = true
  try {
    await changePriority(route.params.id, { targetPriority: targetPriority.value, reason: priorityReason.value })
    ElMessage.success('优先级已调整')
    priorityVisible.value = false
    await load()
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

async function submitReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning('回复内容不能为空')
    return
  }
  replying.value = true
  try {
    await replyTicket(route.params.id, { content: replyContent.value, visible: replyVisible.value })
    ElMessage.success('已发送')
    replyContent.value = ''
    await load()
  } catch (e) {
  } finally {
    replying.value = false
  }
}
</script>

<style scoped>
.atp-back { margin-bottom: 10px; }

.atp-head { padding: 16px 18px; }

.atp-head-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.atp-mono { font-family: Consolas, Monaco, monospace; }

.atp-head-no {
  font-size: 13px;
  font-weight: 600;
  color: var(--atp-text);
}

.atp-head-time {
  margin-left: auto;
  font-size: 12px;
  color: var(--atp-text-mute);
}

.atp-head-title {
  margin: 0 0 6px;
  font-size: 17px;
  font-weight: 600;
  color: var(--atp-text);
}

.atp-head-content {
  margin: 0 0 14px;
  font-size: 13px;
  line-height: 21px;
  color: var(--atp-text-weak);
}

.atp-head-meta { font-size: 12.5px; }
.atp-head-meta .mt { margin-top: 8px; }
.atp-head-meta .k { color: var(--atp-text-mute); margin-right: 8px; }
.atp-head-meta .v { color: var(--atp-text); }

.atp-toolbar-label { font-size: 12.5px; color: var(--atp-text-weak); }
.atp-toolbar-hint { font-size: 12.5px; color: var(--atp-text-mute); }

.atp-two-col {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.atp-col-main {
  flex: 1 1 auto;
  min-width: 0;
  background: #ffffff;
  border: 1px solid var(--atp-border);
  border-radius: 6px;
  padding: 14px 16px;
}

.atp-col-side {
  width: 340px;
  flex: 0 0 340px;
  background: #ffffff;
  border: 1px solid var(--atp-border);
  border-radius: 6px;
  padding: 14px 16px;
}

.atp-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--atp-text);
  margin-bottom: 12px;
}

.atp-msg-line {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.atp-msg-who { font-size: 12.5px; font-weight: 600; color: var(--atp-text); }
.atp-msg-role { font-size: 11.5px; color: var(--atp-text-mute); }

.atp-msg-text {
  font-size: 13px;
  line-height: 20px;
  color: var(--atp-text);
  background: #fafafa;
  border-radius: 5px;
  padding: 7px 10px;
  white-space: pre-wrap;
  word-break: break-word;
}

.atp-reply { margin-top: 18px; }

.atp-reply-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.atp-his-line { font-size: 12.5px; color: var(--atp-text); }
.atp-his-reason { font-size: 12px; color: var(--atp-text-weak); line-height: 18px; }
.atp-his-op { font-size: 11.5px; color: var(--atp-text-mute); }
</style>


