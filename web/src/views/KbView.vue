<template>
  <div class="atp-page">
    <h2 class="atp-page-title">知识库检索</h2>
    <p class="atp-page-desc">
      当前后端开放的是混合检索调试接口，可直接比较 hybrid、vector、keyword 三路结果以及 RRF 融合耗时。
    </p>

    <div class="atp-card atp-query-card">
      <el-input
        v-model="query"
        type="textarea"
        :rows="3"
        resize="none"
        placeholder="例如：退款一般多久能到账"
        @keydown.ctrl.enter.prevent="doSearch"
      />
      <div class="atp-query-bar">
        <div class="atp-query-options">
          <span class="atp-option-label">检索模式</span>
          <el-radio-group v-model="mode">
            <el-radio-button label="hybrid">混合</el-radio-button>
            <el-radio-button label="vector">向量</el-radio-button>
            <el-radio-button label="keyword">关键词</el-radio-button>
          </el-radio-group>
          <span class="atp-option-label">Top K</span>
          <el-input-number v-model="topK" :min="1" :max="20" />
        </div>
        <el-button type="primary" :loading="searching" @click="doSearch">执行检索</el-button>
      </div>
    </div>

    <div v-if="debug" class="atp-result-summary">
      <span>模式 <b>{{ debug.mode }}</b></span>
      <span>融合结果 <b>{{ debug.results?.length || 0 }}</b> 条</span>
      <span>向量路 <b>{{ debug.vectorHits?.length || 0 }}</b> 条</span>
      <span>关键词路 <b>{{ debug.keywordHits?.length || 0 }}</b> 条</span>
      <span>总耗时 <b>{{ debug.costMs || 0 }} ms</b></span>
    </div>

    <div v-if="debug" class="atp-card atp-result-card">
      <el-tabs v-model="tab">
        <el-tab-pane label="最终结果" name="results">
          <KbHitList :hits="debug.results || []" />
        </el-tab-pane>
        <el-tab-pane :label="'向量路 (' + (debug.vectorHits?.length || 0) + ')'" name="vector">
          <KbHitList :hits="debug.vectorHits || []" />
        </el-tab-pane>
        <el-tab-pane :label="'关键词路 (' + (debug.keywordHits?.length || 0) + ')'" name="keyword">
          <KbHitList :hits="debug.keywordHits || []" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <div v-else class="atp-card atp-placeholder">
      <el-icon><Search /></el-icon>
      <div>输入问题后执行一次检索</div>
      <span>后端接口：POST /kb/test-retrieve</span>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { testRetrieve } from '../api/kb'
import KbHitList from '../components/KbHitList.vue'

const query = ref('退款一般多久能到账')
const mode = ref('hybrid')
const topK = ref(5)
const searching = ref(false)
const debug = ref(null)
const tab = ref('results')

async function doSearch() {
  const text = query.value.trim()
  if (!text) {
    ElMessage.warning('先输入要检索的内容')
    return
  }
  searching.value = true
  try {
    debug.value = await testRetrieve({ query: text, mode: mode.value, topK: topK.value })
    tab.value = 'results'
  } catch (e) {
    debug.value = null
  } finally {
    searching.value = false
  }
}
</script>

<style scoped>
.atp-query-card { padding: 14px 16px; }
.atp-query-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 10px; }
.atp-query-options { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; }
.atp-option-label { color: var(--atp-text-mute); font-size: 12.5px; }
.atp-result-summary { display: flex; flex-wrap: wrap; gap: 18px; margin: 14px 2px 10px; color: var(--atp-text-weak); font-size: 12.5px; }
.atp-result-summary b { color: var(--atp-text); }
.atp-result-card { padding: 4px 16px 16px; }
.atp-placeholder { display: flex; flex-direction: column; align-items: center; gap: 8px; margin-top: 16px; padding: 54px 20px; color: var(--atp-text-mute); }
.atp-placeholder .el-icon { font-size: 28px; color: var(--atp-primary); }
.atp-placeholder span { font-family: Consolas, Monaco, monospace; font-size: 11.5px; }
</style>
