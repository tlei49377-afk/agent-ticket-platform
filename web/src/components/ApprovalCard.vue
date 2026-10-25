<template>
  <div class="atp-approval" :class="{ 'is-pending': isPending }">
    <div class="atp-approval-head">
      <el-icon class="atp-approval-icon"><Stamp /></el-icon>
      <span class="atp-approval-title">需要人工审批</span>
      <span class="atp-tag" :style="tagStyle">{{ statusInfo.label }}</span>
      <span class="atp-approval-time">{{ fromNow(approval.createTime) }}</span>
    </div>

    <div class="atp-approval-body">
      <div class="atp-kv">
        <span class="atp-kv-key">动作</span>
        <span class="atp-kv-val">{{ approval.toolName || approval.toolCode }}</span>
      </div>
      <div class="atp-kv">
        <span class="atp-kv-key">参数</span>
        <span class="atp-kv-val">{{ prettyArgs }}</span>
      </div>
      <div class="atp-kv">
        <span class="atp-kv-key">原因</span>
        <span class="atp-kv-val">{{ approval.reason || '-' }}</span>
      </div>
      <div class="atp-kv">
        <span class="atp-kv-key">截止</span>
        <span class="atp-kv-val">{{ formatDateTime(approval.expireAt) }}</span>
      </div>
    </div>

    <div v-if="isPending" class="atp-approval-actions">
      <el-input v-model="comment" size="small" placeholder="审批意见（可不填）" class="atp-comment" />
      <el-button size="small" type="primary" :loading="loading" @click="decide(true)">通过并继续</el-button>
      <el-button size="small" :loading="loading" @click="decide(false)">驳回</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { decideApproval, resumeApproval } from '../api/approval'
import { approvalOf } from '../utils/dict'
import { formatDateTime, fromNow } from '../utils/format'

const props = defineProps({
  approval: { type: Object, required: true }
})
const emit = defineEmits(['decided'])

const comment = ref('')
const loading = ref(false)

const statusInfo = computed(() => approvalOf(props.approval.status))
const isPending = computed(() => props.approval.status === 'pending')
const tagStyle = computed(() => ({ background: statusInfo.value.color + '1a', color: statusInfo.value.color }))

// 参数是后端存的 JSON 字符串，这里拍平成一行好读
const prettyArgs = computed(() => {
  const raw = props.approval.arguments
  if (!raw) return '-'
  try {
    const obj = typeof raw === 'string' ? JSON.parse(raw) : raw
    return Object.entries(obj).map(([k, v]) => k + '=' + (typeof v === 'object' ? JSON.stringify(v) : v)).join('  ')
  } catch (e) {
    return String(raw)
  }
})

async function decide(approved) {
  loading.value = true
  try {
    await decideApproval({ approvalId: props.approval.id, approved, comment: comment.value })
    if (approved) {
      // 通过之后必须调 resume，Agent 才会接着把后面的步骤跑完
      await resumeApproval(props.approval.id)
      ElMessage.success('已通过，Agent 已继续执行')
    } else {
      ElMessage.success('已驳回')
    }
    comment.value = ''
    emit('decided', { approved, approvalId: props.approval.id })
  } catch (e) {
    // 报错提示已经由 axios 拦截器统一弹过了
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.atp-approval {
  border: 1px solid var(--atp-border);
  border-left: 3px solid #b9b9b9;
  border-radius: 6px;
  background: #fafafa;
  padding: 10px 12px;
  margin: 8px 0;
}

.atp-approval.is-pending {
  border-left-color: #d97706;
  background: #fffaf3;
}

.atp-approval-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.atp-approval-icon {
  color: #d97706;
  font-size: 15px;
}

.atp-approval-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--atp-text);
}

.atp-approval-time {
  margin-left: auto;
  font-size: 12px;
  color: var(--atp-text-mute);
}

.atp-approval-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.atp-kv {
  display: flex;
  gap: 8px;
  font-size: 12.5px;
  line-height: 19px;
}

.atp-kv-key {
  flex: 0 0 34px;
  color: var(--atp-text-mute);
}

.atp-kv-val {
  color: var(--atp-text);
  word-break: break-all;
}

.atp-approval-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 10px;
}

.atp-comment {
  flex: 1 1 auto;
  max-width: 320px;
}
</style>
