<template>
  <div class="atp-page">
    <h2 class="atp-page-title">审批中心</h2>
    <p class="atp-page-desc">
      Agent 的写操作会停在这里等待人工决定。通过后页面会继续调用 resume，让原先被拦截的工具真正执行。
    </p>

    <div class="atp-toolbar">
      <el-radio-group v-model="status" @change="search">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="pending">待审批</el-radio-button>
        <el-radio-button label="approved">已通过</el-radio-button>
        <el-radio-button label="rejected">已驳回</el-radio-button>
        <el-radio-button label="expired">已超时</el-radio-button>
        <el-radio-button label="execution_failed">执行失败</el-radio-button>
      </el-radio-group>
      <el-button :icon="Refresh" @click="reload">刷新</el-button>
      <span class="atp-toolbar-tip">共 {{ total }} 条</span>
    </div>

    <el-table
      v-loading="loading"
      :data="rows"
      border
      stripe
      class="atp-clickable-table"
      @row-click="open"
    >
      <el-table-column prop="id" label="审批单" width="86" />
      <el-table-column prop="runId" label="运行 ID" width="90" />
      <el-table-column label="动作" width="188">
        <template #default="{ row }">
          <span class="atp-mono">{{ row.toolName || row.toolCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="参数" min-width="250">
        <template #default="{ row }">
          <span class="atp-ellipsis">{{ compact(row.arguments) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="98">
        <template #default="{ row }">
          <span class="atp-tag" :style="tagStyle(row.status)">{{ approvalOf(row.status).label }}</span>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="166">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="104" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click.stop="open(row)">去处理</el-button>
        </template>
      </el-table-column>
      <template #empty><div class="atp-empty">当前筛选条件下没有审批单</div></template>
    </el-table>

    <div class="atp-pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @current-change="load"
        @size-change="search"
      />
    </div>

    <el-drawer v-model="drawer" size="540px" :title="'审批单 #' + (current ? current.id : '')">
      <template v-if="current">
        <ApprovalCard :approval="current" @decided="onDecided" />

        <div class="atp-detail-block">
          <div class="atp-detail-title">完整参数</div>
          <pre class="atp-pre">{{ prettyJson(current.arguments) }}</pre>
        </div>

        <div class="atp-detail-block">
          <div class="atp-detail-title">运行轨迹</div>
          <el-timeline v-if="steps.length">
            <el-timeline-item
              v-for="s in steps"
              :key="s.id"
              :timestamp="'#' + s.seq + ' · ' + s.node"
              placement="top"
              :color="s.status === 'error' ? '#dc2626' : '#e8833a'"
            >
              <div v-if="s.toolCode" class="atp-step-tool">{{ s.toolCode }}</div>
              <div class="atp-step-line">{{ s.outputSnap || s.inputSnap || '-' }}</div>
              <div v-if="s.errorMsg" class="atp-step-error">{{ s.errorMsg }}</div>
            </el-timeline-item>
          </el-timeline>
          <div v-else class="atp-empty">没有轨迹记录</div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { listApprovals } from '../api/approval'
import { getRunSteps } from '../api/chat'
import ApprovalCard from '../components/ApprovalCard.vue'
import { approvalOf } from '../utils/dict'
import { formatDateTime } from '../utils/format'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const drawer = ref(false)
const current = ref(null)
const steps = ref([])
const status = ref('pending')
const page = ref(1)
const pageSize = ref(10)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const result = await listApprovals({
      page: page.value,
      pageSize: pageSize.value,
      status: status.value || undefined
    })
    rows.value = result?.records || []
    total.value = Number(result?.total || 0)
    if (page.value > 1 && !rows.value.length) {
      page.value = 1
      await load()
    }
  } finally {
    loading.value = false
  }
}

function reload() {
  return load()
}

function search() {
  page.value = 1
  return load()
}

async function open(row) {
  current.value = { ...row }
  steps.value = []
  drawer.value = true
  try {
    steps.value = await getRunSteps(row.runId)
  } catch (e) {
    steps.value = []
  }
}

async function onDecided(result) {
  if (current.value) {
    current.value = {
      ...current.value,
      status: result?.approved ? 'executed' : 'rejected'
    }
  }
  await load()
}

const tagStyle = (value) => {
  const info = approvalOf(value)
  return { background: info.color + '1a', color: info.color }
}

const compact = (raw) => {
  if (!raw) return '-'
  const s = String(raw).replace(/\s+/g, ' ')
  return s.length > 76 ? s.slice(0, 76) + '…' : s
}

const prettyJson = (raw) => {
  if (!raw) return '-'
  try {
    return JSON.stringify(typeof raw === 'string' ? JSON.parse(raw) : raw, null, 2)
  } catch (e) {
    return String(raw)
  }
}
</script>

<style scoped>
.atp-clickable-table :deep(.el-table__row) { cursor: pointer; }
.atp-mono { font-family: Consolas, Monaco, monospace; font-size: 12.5px; }
.atp-ellipsis { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.atp-toolbar-tip { margin-left: auto; color: var(--atp-text-mute); font-size: 12.5px; }
.atp-pager { display: flex; justify-content: flex-end; margin-top: 14px; }
.atp-detail-block { margin-top: 18px; }
.atp-detail-title { margin-bottom: 8px; color: var(--atp-text); font-size: 13px; font-weight: 600; }
.atp-pre { max-height: 260px; overflow: auto; padding: 10px; border: 1px solid var(--atp-border); border-radius: 5px; background: #fafafa; font-size: 12px; }
.atp-step-tool { margin-bottom: 2px; color: var(--atp-primary-dark); font-family: Consolas, Monaco, monospace; }
.atp-step-line { color: var(--atp-text-weak); font-size: 12px; line-height: 18px; word-break: break-all; }
.atp-step-error { color: #dc2626; font-size: 12px; }
</style>


