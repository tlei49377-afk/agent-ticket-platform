<template>
  <div class="atp-tools">
    <div class="atp-tools-title">本轮调用</div>

    <div v-if="!names.length" class="atp-empty">这一轮没有调用工具</div>

    <div v-else class="atp-tools-list">
      <div v-for="(name, i) in names" :key="name + i" class="atp-tool-item">
        <el-icon><Tools /></el-icon>
        <span class="atp-tool-name">{{ name }}</span>
        <span class="atp-tool-desc">{{ descOf(name) }}</span>
      </div>
    </div>

    <template v-if="meta && (meta.cost || meta.tokenIn || meta.latencyMs)">
      <div class="atp-tools-title atp-tools-title-gap">本轮开销</div>
      <div class="atp-meta">
        <div class="atp-meta-item"><span>输入 token</span><b>{{ meta.tokenIn || 0 }}</b></div>
        <div class="atp-meta-item"><span>输出 token</span><b>{{ meta.tokenOut || 0 }}</b></div>
        <div class="atp-meta-item"><span>费用</span><b>{{ formatCost(meta.cost) }}</b></div>
        <div class="atp-meta-item"><span>耗时</span><b>{{ formatDuration(meta.latencyMs) }}</b></div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatCost, formatDuration } from '../utils/format'

const props = defineProps({
  // 后端返回的是工具 code 数组，例如 ["ticket_query", "kb_search"]
  toolCalls: { type: Array, default: () => [] },
  meta: { type: Object, default: null }
})

// 兼容两种形态：字符串数组，或者带 code 字段的对象数组
const names = computed(() =>
  (props.toolCalls || []).map((t) => (typeof t === 'string' ? t : (t.code || t.toolCode || ''))).filter(Boolean)
)

// 工具 code 换个中文说法，纯粹为了看得舒服
const TOOL_LABEL = {
  ticket_query: '查工单列表',
  ticket_detail: '查工单详情',
  ticket_message_list: '查沟通记录',
  user_query: '查用户',
  sla_status_query: '查 SLA 时效',
  kb_search: '检索知识库',
  faq_search: '匹配常见问题',
  stats_query: '查统计数据',
  ticket_create: '创建工单（写）',
  ticket_assign: '指派工单（写）',
  ticket_update_status: '流转状态（写）',
  ticket_reply: '回复工单（写）',
  ticket_escalate: '升级工单（写）'
}
const descOf = (name) => TOOL_LABEL[name] || '自定义工具'
</script>

<style scoped>
.atp-tools-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--atp-text-weak);
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}

.atp-tools-title-gap {
  margin-top: 14px;
}

.atp-tools-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.atp-tool-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  padding: 6px 8px;
  border-radius: 5px;
  background: var(--atp-primary-soft);
  color: var(--atp-text);
}

.atp-tool-name {
  font-weight: 600;
  color: var(--atp-primary-dark);
}

.atp-tool-desc {
  color: var(--atp-text-weak);
  font-size: 12px;
}

.atp-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12.5px;
}

.atp-meta-item {
  display: flex;
  justify-content: space-between;
  color: var(--atp-text-weak);
}

.atp-meta-item b {
  color: var(--atp-text);
  font-weight: 600;
}
</style>
