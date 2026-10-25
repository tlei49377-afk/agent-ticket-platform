<template>
  <div class="atp-page">
    <div class="atp-page-head">
      <div>
        <h2 class="atp-page-title">知识库</h2>
        <p class="atp-page-desc">
          查看全部知识文档、预览知识全文，并对比向量、关键词和混合检索结果。
        </p>
      </div>
      <el-button
        v-if="store.can('kb:manage')"
        type="primary"
        :icon="Upload"
        @click="openUpload"
      >
        导入知识文件
      </el-button>
    </div>

    <el-tabs v-model="pageTab" class="atp-kb-tabs">
      <el-tab-pane label="文档库" name="documents">
        <div class="atp-card atp-document-card">
          <div class="atp-toolbar">
            <el-input
              v-model="documentQuery.keyword"
              placeholder="按标题搜索知识文档"
              clearable
              style="width: 260px"
              @keyup.enter="searchDocuments"
              @clear="searchDocuments"
            />
            <el-select
              v-model="documentQuery.status"
              placeholder="入库状态"
              clearable
              style="width: 140px"
              @change="searchDocuments"
            >
              <el-option label="待处理" value="pending" />
              <el-option label="处理中" value="processing" />
              <el-option label="已就绪" value="ready" />
              <el-option label="失败" value="failed" />
            </el-select>
            <el-button :icon="Search" @click="searchDocuments">查询</el-button>
            <el-button :icon="Refresh" @click="reloadDocuments">刷新</el-button>
            <span class="atp-toolbar-tip">共 {{ documentTotal }} 篇文档</span>
          </div>

          <el-table
            v-loading="documentLoading"
            :data="documents"
            border
            stripe
            class="atp-clickable-table"
            @row-click="viewDocument"
          >
            <el-table-column prop="title" label="文档标题" min-width="280" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <span class="atp-tag" :style="statusStyle(row.status)">
                  {{ statusLabel(row.status) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="chunkCount" label="分块数" width="90" align="center" />
            <el-table-column label="版本" width="80" align="center">
              <template #default="{ row }">v{{ row.version || 1 }}</template>
            </el-table-column>
            <el-table-column label="创建时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :icon="View" @click.stop="viewDocument(row)">
                  查看全文
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <div class="atp-empty">没有查询到知识文档</div>
            </template>
          </el-table>

          <div class="atp-pager">
            <el-pagination
              v-model:current-page="documentQuery.page"
              v-model:page-size="documentQuery.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="documentTotal"
              layout="total, sizes, prev, pager, next"
              @current-change="loadDocuments"
              @size-change="searchDocuments"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="检索测试" name="retrieve">
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
          <el-tabs v-model="resultTab">
            <el-tab-pane label="最终结果" name="results">
              <KbHitList :hits="debug.results || []" @select="openHit" />
            </el-tab-pane>
            <el-tab-pane :label="'向量路 (' + (debug.vectorHits?.length || 0) + ')'" name="vector">
              <KbHitList :hits="debug.vectorHits || []" @select="openHit" />
            </el-tab-pane>
            <el-tab-pane :label="'关键词路 (' + (debug.keywordHits?.length || 0) + ')'" name="keyword">
              <KbHitList :hits="debug.keywordHits || []" @select="openHit" />
            </el-tab-pane>
          </el-tabs>
        </div>

        <div v-else class="atp-card atp-placeholder">
          <el-icon><Search /></el-icon>
          <div>输入问题后执行一次检索</div>
          <span>点击检索结果可查看知识全文</span>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="uploadVisible"
      title="导入知识文件"
      width="560px"
      :close-on-click-modal="false"
      @closed="resetUpload"
    >
      <el-form label-position="top">
        <el-form-item label="文档标题">
          <el-input
            v-model="uploadTitle"
            maxlength="200"
            show-word-limit
            placeholder="不填写则使用文件名"
          />
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :limit="1"
            accept=".pdf,.md,.txt"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
            :on-exceed="onFileExceed"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到这里，或<em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 PDF、Markdown、TXT，单文件不超过 20MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">开始导入</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="viewVisible"
      :title="documentDetail?.title || '知识文档'"
      width="820px"
      top="4vh"
    >
      <div v-if="documentDetail" class="atp-document-detail">
        <div class="atp-detail-meta">
          <span class="atp-tag" :style="statusStyle(documentDetail.status)">
            {{ statusLabel(documentDetail.status) }}
          </span>
          <span>分块 {{ documentDetail.chunkCount || 0 }}</span>
          <span>版本 v{{ documentDetail.version || 1 }}</span>
          <span>{{ formatDateTime(documentDetail.createTime) }}</span>
        </div>

        <el-alert
          v-if="documentDetail.status === 'failed' && documentDetail.errorMsg"
          :title="documentDetail.errorMsg"
          type="error"
          :closable="false"
          show-icon
        />

        <el-tabs v-model="detailTab">
          <el-tab-pane label="完整正文" name="full">
            <pre class="atp-document-full">{{ documentDetail.fullContent || '暂无正文' }}</pre>
          </el-tab-pane>
          <el-tab-pane :label="'分块 (' + (documentDetail.chunks?.length || 0) + ')'" name="chunks">
            <div class="atp-chunk-list">
              <div
                v-for="chunk in documentDetail.chunks || []"
                :key="chunk.id"
                class="atp-chunk"
                :class="{ 'is-hit': previewChunkId === chunk.id }"
              >
                <div class="atp-chunk-head">
                  <b>分块 #{{ chunk.seq }}</b>
                  <span v-if="chunk.category">{{ chunk.category }}</span>
                  <span class="atp-chunk-vector">{{ chunk.vectorId || '未生成向量' }}</span>
                </div>
                <pre>{{ chunk.content }}</pre>
              </div>
              <div v-if="!(documentDetail.chunks || []).length" class="atp-empty">暂无分块内容</div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
      <div v-else class="atp-empty">正在读取知识文档…</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search, Upload, View } from '@element-plus/icons-vue'
import { getDocumentContent, listDocuments, testRetrieve, uploadDocument } from '../api/kb'
import KbHitList from '../components/KbHitList.vue'
import { useUserStore } from '../store/user'
import { formatDateTime } from '../utils/format'

const store = useUserStore()

const pageTab = ref('documents')
const resultTab = ref('results')
const detailTab = ref('full')

const documents = ref([])
const documentTotal = ref(0)
const documentLoading = ref(false)
const documentQuery = reactive({
  keyword: '',
  status: '',
  page: 1,
  pageSize: 10
})

const uploadVisible = ref(false)
const uploadRef = ref()
const uploadTitle = ref('')
const selectedFile = ref(null)
const uploading = ref(false)

const viewVisible = ref(false)
const documentDetail = ref(null)
const previewChunkId = ref(null)

const query = ref('退款一般多久能到账')
const mode = ref('hybrid')
const topK = ref(5)
const searching = ref(false)
const debug = ref(null)

const STATUS = {
  pending: { label: '待处理', color: '#d97706' },
  processing: { label: '处理中', color: '#2563eb' },
  ready: { label: '已就绪', color: '#15803d' },
  failed: { label: '失败', color: '#dc2626' }
}

const statusLabel = (status) => (STATUS[status] || { label: status || '-' }).label
const statusStyle = (status) => {
  const info = STATUS[status] || { color: '#78716c' }
  return { background: info.color + '1a', color: info.color }
}

onMounted(loadDocuments)

async function loadDocuments() {
  documentLoading.value = true
  try {
    const page = await listDocuments({
      page: documentQuery.page,
      pageSize: documentQuery.pageSize,
      keyword: documentQuery.keyword || undefined,
      status: documentQuery.status || undefined
    })
    documents.value = page?.records || []
    documentTotal.value = Number(page?.total || 0)
  } finally {
    documentLoading.value = false
  }
}

function searchDocuments() {
  documentQuery.page = 1
  return loadDocuments()
}

function reloadDocuments() {
  return loadDocuments()
}

async function viewDocument(row) {
  await openDocument(row.id, null)
}

async function openHit(hit) {
  if (!hit.documentId) {
    ElMessage.warning('这条结果没有关联文档 ID')
    return
  }
  await openDocument(hit.documentId, hit.chunkId || null)
}

async function openDocument(documentId, chunkId) {
  viewVisible.value = true
  documentDetail.value = null
  previewChunkId.value = chunkId
  detailTab.value = chunkId ? 'chunks' : 'full'
  try {
    documentDetail.value = await getDocumentContent(documentId)
  } catch (e) {
    viewVisible.value = false
  }
}

function openUpload() {
  uploadVisible.value = true
}

function onFileChange(file) {
  selectedFile.value = file.raw || null
  if (!uploadTitle.value && file.name) {
    uploadTitle.value = file.name.replace(/\.[^.]+$/, '')
  }
}

function onFileRemove() {
  selectedFile.value = null
}

function onFileExceed(files) {
  ElMessage.warning('一次只能导入一个文件，请先移除已选文件')
  uploadRef.value?.clearFiles()
  const raw = files?.[0]
  if (raw) {
    uploadRef.value?.handleStart(raw)
  }
}

function resetUpload() {
  selectedFile.value = null
  uploadTitle.value = ''
  uploadRef.value?.clearFiles()
}

async function submitUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择知识文件')
    return
  }

  const form = new FormData()
  form.append('file', selectedFile.value, selectedFile.value.name)
  if (uploadTitle.value.trim()) {
    form.append('title', uploadTitle.value.trim())
  }

  uploading.value = true
  try {
    const id = await uploadDocument(form)
    ElMessage.success('文件已提交，正在切分和生成向量')
    uploadVisible.value = false
    pageTab.value = 'documents'
    await loadDocuments()
    waitForIngest(id)
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    uploading.value = false
  }
}

/**
 * 上传接口只返回文档 ID，向量化在后台异步执行。
 * 页面轮询详情状态，直到 ready/failed；超过 40 秒停止轮询但保留列表状态。
 */
async function waitForIngest(documentId) {
  for (let attempt = 0; attempt < 20; attempt++) {
    await sleep(2000)
    try {
      const detail = await getDocumentContent(documentId)
      await loadDocuments()
      if (detail.status === 'ready') {
        ElMessage.success(`《${detail.title}》已完成入库`)
        return
      }
      if (detail.status === 'failed') {
        ElMessage.error(detail.errorMsg || '知识文件入库失败')
        return
      }
    } catch (e) {
      return
    }
  }
  ElMessage.warning('文件仍在处理中，请稍后刷新文档列表')
}

async function doSearch() {
  const text = query.value.trim()
  if (!text) {
    ElMessage.warning('先输入要检索的内容')
    return
  }
  searching.value = true
  try {
    debug.value = await testRetrieve({ query: text, mode: mode.value, topK: topK.value })
    resultTab.value = 'results'
  } catch (e) {
    debug.value = null
  } finally {
    searching.value = false
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
</script>

<style scoped>
.atp-page-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.atp-kb-tabs { margin-top: 4px; }
.atp-document-card { padding: 14px 16px 16px; }
.atp-toolbar-tip { margin-left: auto; color: var(--atp-text-mute); font-size: 12.5px; }
.atp-pager { display: flex; justify-content: flex-end; margin-top: 14px; }
.atp-clickable-table :deep(.el-table__row) { cursor: pointer; }
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
.atp-document-detail { display: flex; flex-direction: column; gap: 12px; }
.atp-detail-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; color: var(--atp-text-mute); font-size: 12.5px; }
.atp-document-full { max-height: 62vh; overflow: auto; margin: 0; padding: 16px; border: 1px solid var(--atp-border); border-radius: 6px; background: #fafafa; color: var(--atp-text); font-family: "Microsoft YaHei", "PingFang SC", sans-serif; font-size: 13.5px; line-height: 1.9; white-space: pre-wrap; word-break: break-word; }
.atp-chunk-list { display: flex; flex-direction: column; gap: 10px; max-height: 62vh; overflow: auto; }
.atp-chunk { padding: 12px; border: 1px solid var(--atp-border); border-radius: 6px; background: #fff; }
.atp-chunk.is-hit { border-color: #e8833a; box-shadow: 0 0 0 2px rgba(232, 131, 58, 0.12); }
.atp-chunk-head { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; color: var(--atp-text-weak); font-size: 12px; }
.atp-chunk-vector { margin-left: auto; font-family: Consolas, Monaco, monospace; font-size: 11px; color: var(--atp-text-mute); }
.atp-chunk pre { margin: 0; white-space: pre-wrap; word-break: break-word; color: var(--atp-text); font-family: "Microsoft YaHei", "PingFang SC", sans-serif; font-size: 13px; line-height: 1.85; }
</style>
