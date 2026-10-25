<template>
  <div class="atp-kb-hits">
    <div v-if="!hits.length" class="atp-empty">没有命中知识库</div>

    <div v-for="(hit, i) in hits" :key="hit.chunkId || hit.id || i" class="atp-hit">
      <div class="atp-hit-head">
        <el-icon><Document /></el-icon>
        <span class="atp-hit-title">{{ titleOf(hit) }}</span>
        <span v-if="hit.index" class="atp-hit-index">[{{ hit.index }}]</span>
        <span class="atp-hit-score">score {{ scoreOf(hit).toFixed(4) }}</span>
      </div>
      <div class="atp-hit-content">{{ contentOf(hit) }}</div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  // 同时兼容引用接口 KbReferenceVO 和检索接口 RetrievedChunk
  hits: { type: Array, default: () => [] }
})

const titleOf = (hit) => hit.docTitle || hit.title || (hit.documentId ? `文档 ${hit.documentId}` : '知识片段')
const contentOf = (hit) => hit.snippet || hit.content || '-'
const scoreOf = (hit) => {
  const n = Number(hit.score ?? hit.rrfScore ?? 0)
  return Number.isFinite(n) ? n : 0
}
</script>

<style scoped>
.atp-kb-hits { display: flex; flex-direction: column; gap: 8px; }
.atp-hit { padding: 8px 10px; border: 1px solid var(--atp-border); border-radius: 6px; background: #fff; }
.atp-hit-head { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.atp-hit-title { color: var(--atp-text); font-size: 12.5px; font-weight: 600; }
.atp-hit-index { color: var(--atp-primary-dark); font-family: Consolas, Monaco, monospace; font-size: 11.5px; }
.atp-hit-score { margin-left: auto; color: var(--atp-text-mute); font-size: 11.5px; }
.atp-hit-content { max-height: 120px; overflow: hidden; color: var(--atp-text-weak); font-size: 12.5px; line-height: 19px; white-space: pre-wrap; }
</style>
