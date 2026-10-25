<template>
  <el-container class="atp-layout">
    <el-aside width="220px" class="atp-aside">
      <div class="atp-brand">
        <div class="atp-brand-logo">AT</div>
        <div class="atp-brand-text">
          <div class="atp-brand-name">智能工单平台</div>
          <div class="atp-brand-sub">Agent Ticket Platform</div>
        </div>
      </div>
      <SideMenu />
    </el-aside>

    <el-container>
      <el-header class="atp-header">
        <div class="atp-header-title">{{ pageTitle }}</div>
        <el-dropdown @command="onCommand">
          <span class="atp-user">
            <el-icon><UserFilled /></el-icon>
            <span class="atp-user-name">{{ store.name }}</span>
            <span class="atp-tag atp-user-role">{{ roleText }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>共 {{ store.permissions.length }} 项权限</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="atp-main">
        <router-view v-slot="{ Component }">
          <keep-alive :include="['TicketListView', 'ChatView']">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import SideMenu from './SideMenu.vue'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const pageTitle = computed(() => route.meta.title || '智能工单平台')

onMounted(async () => {
  if (!store.token) return
  try {
    await store.loadMe()
  } catch (e) {
    // 401 已由请求拦截器处理，这里不重复弹错
  }
})

// 角色码转中文，顶栏只展示第一个角色
const ROLE_LABEL = { admin: '管理员', supervisor: '主管', agent: '客服', requester: '提交人' }
const roleText = computed(() => {
  const code = store.roles[0]
  return ROLE_LABEL[code] || code || '访客'
})

async function onCommand(command) {
  if (command !== 'logout') return
  await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
  await store.doLogout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.atp-layout {
  height: 100%;
}

.atp-aside {
  background: var(--atp-side-bg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.atp-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 16px 14px;
  border-bottom: 1px solid #3f3f3f;
}

/* 左上角的圆形标记：橙金渐变，和整站的暖色主色呼应 */
.atp-brand-logo {
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffb066 0%, #e8833a 100%);
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 0.5px;
}

.atp-brand-name {
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  line-height: 18px;
}

.atp-brand-sub {
  color: #8f8f8f;
  font-size: 11px;
  line-height: 15px;
}

.atp-header {
  height: 54px;
  background: #ffffff;
  border-bottom: 1px solid var(--atp-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.atp-header-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--atp-text);
}

.atp-user {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  outline: none;
  color: var(--atp-text);
}

.atp-user-name {
  font-size: 13px;
}

.atp-user-role {
  background: var(--atp-primary-soft);
  color: var(--atp-primary-dark);
}

.atp-main {
  background: var(--atp-bg);
  padding: 0;
  overflow-y: auto;
}
</style>

