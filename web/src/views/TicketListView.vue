<template>
  <div class="atp-page">
    <h2 class="atp-page-title">工单管理</h2>
    <p class="atp-page-desc">
      状态、优先级、处理人、关键词都能筛。列表里的“剩余时效”来自 sla_timer，负数就是已经超期。
    </p>

    <div class="atp-toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="工单号 / 标题 / 内容"
        style="width: 220px"
        clearable
        @keyup.enter="search"
        @clear="search"
      />
      <el-select v-model="query.status" placeholder="状态" style="width: 130px" clearable @change="search">
        <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-select v-model="query.priority" placeholder="优先级" style="width: 120px" clearable @change="search">
        <el-option v-for="o in priorityOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-select v-if="store.can('user:list')" v-model="query.assigneeId" placeholder="处理人" style="width: 140px" clearable @change="search">
        <el-option v-for="u in users" :key="u.id" :label="u.name + '（' + u.username + '）'" :value="u.id" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
      <el-button
        v-if="store.can('ticket:add')"
        type="primary"
        plain
        :icon="Plus"
        style="margin-left: auto"
        @click="openCreate"
      >新建工单</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe class="atp-clickable-table" @row-click="toDetail">
      <el-table-column prop="ticketNo" label="工单号" width="164">
        <template #default="{ row }">
          <span class="atp-mono">{{ row.ticketNo }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
      <el-table-column label="状态" width="104">
        <template #default="{ row }">
          <span class="atp-tag" :style="tagStyle(statusOf(row.status))">{{ row.statusLabel || statusOf(row.status).label }}</span>
        </template>
      </el-table-column>
      <el-table-column label="优先级" width="88">
        <template #default="{ row }">
          <span class="atp-tag" :style="tagStyle(priorityOf(row.priority))">{{ row.priorityLabel || priorityOf(row.priority).label }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="104" />
      <el-table-column prop="requesterName" label="提交人" width="90" />
      <el-table-column prop="assigneeName" label="处理人" width="90">
        <template #default="{ row }">{{ row.assigneeName || '未指派' }}</template>
      </el-table-column>
      <el-table-column label="剩余时效" width="126">
        <template #default="{ row }">
          <span :class="row.slaRemainMinutes <= 0 ? 'atp-overdue' : ''">{{ formatRemain(row.slaRemainMinutes) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="166">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click.stop="toDetail(row)">详情</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="atp-empty">没有符合条件的工单</div>
      </template>
    </el-table>

    <div class="atp-pager">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @current-change="load"
        @size-change="load"
      />
    </div>

    <el-dialog v-model="createVisible" title="新建工单" width="560px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="86px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="createForm.title" placeholder="一句话说清楚问题" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="createForm.content" type="textarea" :rows="4" placeholder="补充细节" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="createForm.categoryId" placeholder="请选择" style="width: 100%" clearable>
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="createForm.priority" style="width: 100%">
            <el-option v-for="o in priorityOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onActivated, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, RefreshLeft, Search } from '@element-plus/icons-vue'
import { categoryFlat, createTicket, listTickets } from '../api/ticket'
import { listAllUsers } from '../api/user'
import { useUserStore } from '../store/user'
import { priorityOf, priorityOptions, statusOf, statusOptions } from '../utils/dict'
import { formatDateTime, formatRemain } from '../utils/format'

// keep-alive 缓存列表页，从详情返回时筛选条件和滚动位置都还在
defineOptions({ name: 'TicketListView' })

const router = useRouter()
const store = useUserStore()

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const users = ref([])
const categories = ref([])

const createVisible = ref(false)
const creating = ref(false)
const createFormRef = ref()
const createForm = reactive({ title: '', content: '', categoryId: null, priority: 3 })
const createRules = {
  title: [
    { required: true, message: '请填写标题', trigger: 'blur' },
    { min: 4, message: '标题至少 4 个字', trigger: 'blur' }
  ],
  content: [{ required: true, message: '请填写内容', trigger: 'blur' }]
}

const query = reactive({
  keyword: '',
  status: null,
  priority: null,
  assigneeId: null,
  page: 1,
  pageSize: 10
})

const tagStyle = (info) => ({ background: info.color + '1a', color: info.color })

onMounted(async () => {
  await loadRefs()
  load()
})

// 从详情页返回时刷新一次，状态可能被改过
onActivated(() => {
  if (rows.value.length) load()
})

async function loadRefs() {
  // 这两个接口分别要 user:list 和工单相关权限，拿不到就静默降级，不影响列表本身
  if (store.can('user:list')) {
    try {
      users.value = await listAllUsers()
    } catch (e) {
      users.value = []
    }
  }
  try {
    categories.value = await categoryFlat()
  } catch (e) {
    categories.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const page = await listTickets({
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      priority: query.priority || undefined,
      assigneeId: query.assigneeId || undefined,
      page: query.page,
      pageSize: query.pageSize
    })
    rows.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  query.keyword = ''
  query.status = null
  query.priority = null
  query.assigneeId = null
  search()
}

function toDetail(row) {
  router.push('/tickets/' + row.id)
}

function openCreate() {
  createForm.title = ''
  createForm.content = ''
  createForm.categoryId = null
  createForm.priority = 3
  createVisible.value = true
}

async function submitCreate() {
  await createFormRef.value.validate()
  creating.value = true
  try {
    const id = await createTicket({ ...createForm })
    ElMessage.success('工单已创建')
    createVisible.value = false
    await load()
    router.push('/tickets/' + id)
  } catch (e) {
    // 拦截器已提示
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.atp-clickable-table :deep(.el-table__row) { cursor: pointer; }

.atp-mono {
  font-family: Consolas, Monaco, monospace;
  font-size: 12.5px;
  color: var(--atp-text);
}

.atp-pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.atp-overdue {
  color: #dc2626;
  font-weight: 600;
}
</style>




