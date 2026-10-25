<template>
  <div class="atp-page">
    <h2 class="atp-page-title">评测看板</h2>
    <p class="atp-page-desc">
      选一个评测集点“跑评测”，后端会把用例逐条喂给 Agent，再算命中率、召回和忠实度。
      100 条用例大约几秒，跑完左边会多一条 run 记录。
    </p>

    <div class="atp-two-col">
      <!-- 左：评测集与历史 -->
      <div class="atp-col-side">
        <div class="atp-section-title">评测集</div>
        <div v-for="d in datasets" :key="d.id" class="atp-dataset">
          <div class="atp-dataset-head">
            <span class="atp-dataset-name">{{ d.name }}</span>
            <span class="atp-tag atp-dataset-count">{{ d.caseCount }} 条</span>
          </div>
          <div class="atp-dataset-desc">{{ d.description }}</div>
          <el-button
            type="primary"
            size="small"
            :loading="runningId === d.id"
            style="margin-top: 8px"
            @click="run(d)"
          >跑评测</el-button>
        </div>
        <div v-if="!datasets.length" class="atp-empty">还没有评测集</div>

        <div class="atp-section-title" style="margin-top: 18px">历史运行</div>
        <div class="atp-run-list">
          <div
            v-for="r in runs"
            :key="r.id"
            class="atp-run"
            :class="{ 'is-active': current && current.id === r.id }"
            @click="select(r)"
          >
            <div class="atp-run-line">
              <span class="atp-mono">#{{ r.id }}</span>
              <span class="atp-tag" :style="tagStyle(evalOf(r.status))">{{ evalOf(r.status).label }}</span>
            </div>
            <div class="atp-run-sub">
              {{ r.passedCases }}/{{ r.totalCases }} 通过 · {{ fromNow(r.endedAt || r.startedAt) }}
            </div>
          </div>
        </div>
        <div v-if="!runs.length" class="atp-empty">还没有跑过评测</div>
      </div>

      <!-- 右：结果 -->
      <div class="atp-col-main">
        <template v-if="current">
          <div class="atp-cards">
            <div class="atp-card atp-stat" v-for="m in metricCards" :key="m.label">
              <div class="atp-stat-label">{{ m.label }}</div>
              <div class="atp-stat-value" :style="{ color: m.color }">{{ m.value }}</div>
            </div>
          </div>

          <div class="atp-card atp-chart-box">
            <div class="atp-section-title">核心指标</div>
            <div ref="chartEl" class="atp-chart"></div>
          </div>

          <div class="atp-card" style="margin-top: 14px; padding: 14px 16px">
            <div class="atp-toolbar">
              <div class="atp-section-title" style="margin: 0">用例明细</div>
              <el-switch
                v-model="failedOnly"
                active-text="只看失败"
                style="margin-left: 14px"
                @change="loadResults"
              />
              <span class="atp-toolbar-tip">共 {{ results.length }} 条</span>
            </div>

            <el-table :data="results" border stripe size="small" max-height="380">
              <el-table-column prop="caseId" label="用例" width="76" />
              <el-table-column label="命中" width="72">
                <template #default="{ row }">
                  <el-icon v-if="row.toolHit" color="#15803d"><CircleCheck /></el-icon>
                  <el-icon v-else color="#dc2626"><CircleClose /></el-icon>
                </template>
              </el-table-column>
              <el-table-column label="回答" min-width="280">
                <template #default="{ row }">
                  <span class="atp-answer">{{ row.actualAnswer }}</span>
                </template>
              </el-table-column>
              <el-table-column label="忠实度" width="82">
                <template #default="{ row }">{{ fmtNum(row.faithfulness) }}</template>
              </el-table-column>
              <el-table-column label="相关性" width="82">
                <template #default="{ row }">{{ fmtNum(row.relevance) }}</template>
              </el-table-column>
              <template #empty>
                <div class="atp-empty">没有用例明细</div>
              </template>
            </el-table>
          </div>
        </template>

        <div v-else class="atp-empty atp-big-empty">左侧选一条历史运行，或者先跑一次评测</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { getRun, listDatasets, listResults, listRuns, runEval } from '../api/eval'
import { useChart } from '../utils/chart'
import { evalOf } from '../utils/dict'
import { formatCost, fromNow } from '../utils/format'

const datasets = ref([])
const runs = ref([])
const current = ref(null)
const results = ref([])
const failedOnly = ref(false)
const runningId = ref(null)

const tagStyle = (info) => ({ background: info.color + '1a', color: info.color })

const pct = (v) => (v === null || v === undefined ? '-' : (Number(v) * 100).toFixed(1) + '%')
const fmtNum = (v) => (v === null || v === undefined ? '-' : Number(v).toFixed(2))

const metricCards = computed(() => {
  const r = current.value
  if (!r) return []
  return [
    { label: '通过用例', value: r.passedCases + ' / ' + r.totalCases, color: '#333333' },
    { label: '工具命中率', value: pct(r.toolHitRate), color: '#15803d' },
    { label: '关键词命中率', value: pct(r.keywordHitRate), color: '#0f766e' },
    { label: '文档召回率', value: pct(r.docRecall), color: '#7c3aed' },
    { label: 'MRR', value: pct(r.mrr), color: '#e8833a' },
    { label: '平均忠实度', value: fmtNum(r.avgFaithfulness), color: '#d97706' },
    { label: '平均相关性', value: fmtNum(r.avgRelevance), color: '#ca8a04' },
    { label: '平均花费', value: formatCost(r.avgCost), color: '#6b7280' }
  ]
})

const { el: chartEl, render: renderChart } = useChart(() => {
  const r = current.value || {}
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 90, right: 40, top: 20, bottom: 24 },
    xAxis: {
      type: 'value',
      max: 1,
      axisLabel: { formatter: (v) => (v * 100) + '%', color: '#6b6b6b', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: ['工具命中率', '关键词命中率', '文档召回率', 'MRR'],
      axisLabel: { color: '#4d4d4d', fontSize: 12 },
      axisLine: { lineStyle: { color: '#dcdcdc' } }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 26,
        label: { show: true, position: 'right', formatter: (p) => (p.value * 100).toFixed(0) + '%', color: '#4d4d4d', fontSize: 11 },
        itemStyle: { color: '#e8833a', borderRadius: [0, 3, 3, 0] },
        data: [
          Number(r.toolHitRate || 0),
          Number(r.keywordHitRate || 0),
          Number(r.docRecall || 0),
          Number(r.mrr || 0)
        ]
      }
    ]
  }
})

onMounted(async () => {
  await Promise.all([loadDatasets(), loadRuns()])
  if (runs.value.length) select(runs.value[0])
})

async function loadDatasets() {
  try {
    datasets.value = await listDatasets()
  } catch (e) {
    datasets.value = []
  }
}

async function loadRuns() {
  try {
    runs.value = await listRuns()
  } catch (e) {
    runs.value = []
  }
}

async function run(dataset) {
  runningId.value = dataset.id
  ElMessage.info('评测已开始，100 条用例大概几秒…')
  try {
    const vo = await runEval(dataset.id)
    ElMessage.success('评测完成：' + vo.passedCases + ' / ' + vo.totalCases)
    await loadRuns()
    await select(vo)
  } catch (e) {
    // 拦截器已提示
  } finally {
    runningId.value = null
  }
}

async function select(row) {
  try {
    current.value = await getRun(row.id)
  } catch (e) {
    current.value = row
  }
  await loadResults()
  renderChart()
}

async function loadResults() {
  if (!current.value) return
  try {
    results.value = await listResults(current.value.id, failedOnly.value)
  } catch (e) {
    results.value = []
  }
}
</script>

<style scoped>
.atp-two-col {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.atp-col-side {
  width: 320px;
  flex: 0 0 320px;
  background: #ffffff;
  border: 1px solid var(--atp-border);
  border-radius: 6px;
  padding: 14px 16px;
}

.atp-col-main {
  flex: 1 1 auto;
  min-width: 0;
}

.atp-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--atp-text);
  margin-bottom: 12px;
}

.atp-dataset {
  border: 1px solid var(--atp-border);
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 10px;
}

.atp-dataset-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.atp-dataset-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--atp-text);
}

.atp-dataset-count {
  background: var(--atp-primary-soft);
  color: var(--atp-primary-dark);
}

.atp-dataset-desc {
  font-size: 12px;
  color: var(--atp-text-mute);
  margin-top: 4px;
}

.atp-run-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.atp-run {
  border: 1px solid var(--atp-border);
  border-radius: 5px;
  padding: 8px 10px;
  cursor: pointer;
}

.atp-run:hover { background: #fafafa; }

.atp-run.is-active {
  border-color: #f2d3b7;
  background: var(--atp-primary-soft);
}

.atp-run-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.atp-run-sub {
  font-size: 11.5px;
  color: var(--atp-text-mute);
  margin-top: 2px;
}

.atp-mono { font-family: Consolas, Monaco, monospace; font-size: 12.5px; }

.atp-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 14px;
}

.atp-stat { padding: 12px 14px; }

.atp-stat-label {
  font-size: 12px;
  color: var(--atp-text-mute);
  margin-bottom: 4px;
}

.atp-stat-value {
  font-size: 20px;
  font-weight: 700;
  line-height: 26px;
}

.atp-chart-box {
  padding: 14px 16px;
}

.atp-chart { height: 220px; }

.atp-toolbar-tip {
  margin-left: auto;
  font-size: 12.5px;
  color: var(--atp-text-mute);
}

.atp-answer {
  font-size: 12px;
  color: var(--atp-text-weak);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.atp-big-empty {
  background: #ffffff;
  border: 1px solid var(--atp-border);
  border-radius: 6px;
  padding: 80px 0;
}
</style>
