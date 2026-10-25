<template>
  <div class="atp-page">
    <h2 class="atp-page-title">统计看板</h2>
    <p class="atp-page-desc">
      上半部分看工单分布和用户反馈，下半部分看 Agent 的调用量和花费。
      指标数据来自 sys_metric_daily，按天 + 维度聚合，跑一次评测或聊几句就会有数。
    </p>

    <!-- 指标卡 -->
    <div class="atp-cards">
      <div class="atp-card atp-stat" v-for="c in cards" :key="c.label">
        <div class="atp-stat-label">{{ c.label }}</div>
        <div class="atp-stat-value" :style="{ color: c.color }">{{ c.value }}</div>
      </div>
    </div>

    <div class="atp-chart-row">
      <div class="atp-card atp-chart-box">
        <div class="atp-section-title">工单状态分布</div>
        <div ref="pieEl" class="atp-chart"></div>
      </div>

      <div class="atp-card atp-chart-box">
        <div class="atp-section-title">用户反馈</div>
        <div class="atp-feedback">
          <div class="atp-fb-item">
            <div class="atp-fb-value">{{ feedback.total || 0 }}</div>
            <div class="atp-fb-label">反馈总数</div>
          </div>
          <div class="atp-fb-item">
            <div class="atp-fb-value" style="color: #15803d">{{ feedback.up || 0 }}</div>
            <div class="atp-fb-label">有用</div>
          </div>
          <div class="atp-fb-item">
            <div class="atp-fb-value" style="color: #dc2626">{{ feedback.down || 0 }}</div>
            <div class="atp-fb-label">没用</div>
          </div>
          <div class="atp-fb-item">
            <div class="atp-fb-value" style="color: #e8833a">{{ feedback.satisfactionRate || 0 }}%</div>
            <div class="atp-fb-label">满意率</div>
          </div>
        </div>
        <div class="atp-overview">
          <div class="atp-ov-item"><span>已接入 Agent</span><b>{{ overview.agents || 0 }}</b></div>
          <div class="atp-ov-item"><span>注册工具</span><b>{{ overview.tools || 0 }}</b></div>
        </div>
      </div>
    </div>

    <div class="atp-card atp-chart-box" style="margin-top: 16px">
      <div class="atp-toolbar">
        <div class="atp-section-title" style="margin: 0">Agent 调用指标</div>
        <el-date-picker
          v-model="range"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始"
          end-placeholder="结束"
          size="small"
          style="width: 240px; margin-left: 12px"
        />
        <el-select v-model="dimType" size="small" style="width: 140px">
          <el-option label="全部（ALL）" value="ALL" />
          <el-option label="按 Agent" value="AGENT" />
          <el-option label="按模型" value="MODEL" />
          <el-option label="按工具" value="TOOL" />
        </el-select>
        <el-button size="small" type="primary" :loading="loadingMetrics" @click="loadMetrics">查询</el-button>
        <el-button size="small" :icon="Refresh" @click="loadMetrics">刷新</el-button>
      </div>

      <el-table :data="metrics" border stripe size="small" style="margin-bottom: 14px">
        <el-table-column prop="statDate" label="日期" width="110" />
        <el-table-column prop="dimType" label="维度" width="90" />
        <el-table-column prop="dimValue" label="维度值" min-width="140">
          <template #default="{ row }">{{ row.dimValue || '（合计）' }}</template>
        </el-table-column>
        <el-table-column prop="callCount" label="调用次数" width="96" />
        <el-table-column prop="tokenIn" label="输入 token" width="106" />
        <el-table-column prop="tokenOut" label="输出 token" width="106" />
        <el-table-column label="费用" width="100">
          <template #default="{ row }">{{ formatCost(row.cost) }}</template>
        </el-table-column>
        <el-table-column prop="errorCount" label="错误数" width="86" />
        <el-table-column prop="avgLatency" label="平均耗时(ms)" width="118" />
        <template #empty>
          <div class="atp-empty">这个时间范围内还没有指标数据</div>
        </template>
      </el-table>

      <div ref="barEl" class="atp-chart atp-chart-tall"></div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { agentOverview, feedbackStats, metricStats, ticketStats } from '../api/stats'
import { useChart } from '../utils/chart'
import { formatCost } from '../utils/format'

const statsMap = ref({})
const feedback = ref({})
const overview = ref({})
const metrics = ref([])
const loadingMetrics = ref(false)

const dimType = ref('ALL')
const range = ref(defaultRange())

function defaultRange() {
  const end = new Date()
  const start = new Date(end.getTime() - 29 * 86400000)
  const iso = (d) => d.toISOString().slice(0, 10)
  return [iso(start), iso(end)]
}

// 后端返回的是 { status_1_待受理: 5, ..., total: 14 } 这种扁平结构，
// 这里统一转成 [{ name, value, color }] 给卡片和饼图共用。
const statusList = computed(() => {
  const list = []
  Object.entries(statsMap.value || {}).forEach(([key, value]) => {
    if (!key.startsWith('status_')) return
    const parts = key.split('_')
    const code = Number(parts[1])
    const label = parts.slice(2).join('_')
    const color = STATUS_COLOR[code] || '#78716c'
    list.push({ code, name: label, value: Number(value || 0), color })
  })
  return list.sort((a, b) => a.code - b.code)
})

const STATUS_COLOR = {
  1: '#d97706',
  2: '#0f766e',
  3: '#78716c',
  4: '#7c3aed',
  5: '#15803d',
  6: '#52525b'
}

const cards = computed(() => {
  const total = Number(statsMap.value.total || 0)
  const byName = {}
  statusList.value.forEach((s) => { byName[s.name] = s.value })
  return [
    { label: '工单总数', value: total, color: '#333333' },
    { label: '待受理', value: byName['待受理'] || 0, color: '#d97706' },
    { label: '处理中', value: byName['处理中'] || 0, color: '#0f766e' },
    { label: '已解决', value: byName['已解决'] || 0, color: '#15803d' }
  ]
})

// ---- 饼图 ----
const { el: pieEl, render: renderPie } = useChart(() => ({
  tooltip: { trigger: 'item', formatter: '{b}：{c} 单（{d}%）' },
  legend: { bottom: 0, icon: 'circle', textStyle: { color: '#4d4d4d', fontSize: 12 } },
  series: [
    {
      type: 'pie',
      radius: ['46%', '70%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      data: statusList.value.map((s) => ({
        name: s.name,
        value: s.value,
        itemStyle: { color: s.color }
      }))
    }
  ]
}))

// ---- 柱状图 ----
const { el: barEl, render: renderBar } = useChart(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0, textStyle: { color: '#4d4d4d', fontSize: 12 } },
  grid: { left: 56, right: 24, top: 40, bottom: 40 },
  xAxis: {
    type: 'category',
    data: metrics.value.map((m) => m.dimValue || '合计'),
    axisLine: { lineStyle: { color: '#dcdcdc' } },
    axisLabel: { color: '#6b6b6b', fontSize: 11 }
  },
  yAxis: [
    {
      type: 'value',
      name: '调用次数',
      nameTextStyle: { color: '#9a9a9a', fontSize: 11 },
      axisLabel: { color: '#6b6b6b', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    {
      type: 'value',
      name: '费用',
      nameTextStyle: { color: '#9a9a9a', fontSize: 11 },
      axisLabel: { color: '#6b6b6b', fontSize: 11 },
      splitLine: { show: false }
    }
  ],
  series: [
    {
      name: '调用次数',
      type: 'bar',
      barMaxWidth: 40,
      itemStyle: { color: '#e8833a', borderRadius: [3, 3, 0, 0] },
      data: metrics.value.map((m) => m.callCount || 0)
    },
    {
      name: '费用',
      type: 'line',
      yAxisIndex: 1,
      smooth: true,
      symbolSize: 7,
      lineStyle: { color: '#333333', width: 2 },
      itemStyle: { color: '#333333' },
      data: metrics.value.map((m) => Number(m.cost || 0))
    }
  ]
}))

onMounted(async () => {
  await Promise.all([loadTicketStats(), loadFeedback(), loadMetrics()])
  renderPie()
})

async function loadTicketStats() {
  try {
    statsMap.value = await ticketStats()
  } catch (e) {
    statsMap.value = {}
  }
}

async function loadFeedback() {
  try {
    feedback.value = await feedbackStats()
    overview.value = await agentOverview()
  } catch (e) {
    feedback.value = {}
    overview.value = {}
  }
}

async function loadMetrics() {
  loadingMetrics.value = true
  try {
    metrics.value = await metricStats({
      from: range.value[0],
      to: range.value[1],
      dimType: dimType.value
    })
  } catch (e) {
    metrics.value = []
  } finally {
    loadingMetrics.value = false
    renderBar()
  }
}
</script>

<style scoped>
.atp-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.atp-stat {
  padding: 14px 16px;
}

.atp-stat-label {
  font-size: 12.5px;
  color: var(--atp-text-mute);
  margin-bottom: 6px;
}

.atp-stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 32px;
}

.atp-chart-row {
  display: flex;
  gap: 16px;
  align-items: stretch;
}

.atp-chart-box {
  padding: 14px 16px;
  min-width: 0;
}

.atp-chart-row .atp-chart-box { flex: 1 1 0; }

.atp-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--atp-text);
  margin-bottom: 12px;
}

.atp-chart { height: 260px; }
.atp-chart-tall { height: 300px; }

.atp-feedback {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  text-align: center;
  padding: 18px 0 10px;
}

.atp-fb-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--atp-text);
}

.atp-fb-label {
  font-size: 12px;
  color: var(--atp-text-mute);
  margin-top: 4px;
}

.atp-overview {
  border-top: 1px dashed var(--atp-border);
  padding-top: 12px;
}

.atp-ov-item {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  color: var(--atp-text-weak);
  padding: 3px 0;
}

.atp-ov-item b { color: var(--atp-text); }
</style>
