<template>
  <el-menu
    class="atp-menu"
    :default-active="activePath"
    router
    background-color="#333333"
    text-color="#d6d6d6"
    active-text-color="#ffb066"
  >
    <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
      <el-icon><component :is="item.icon" /></el-icon>
      <span>{{ item.title }}</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../store/user'

const route = useRoute()
const store = useUserStore()

// 菜单不手写死，直接从路由表里挑：有 title、不在菜单里隐藏、权限够，才显示
const menus = computed(() => {
  const layoutRoute = route.matched.find((r) => r.path === '/')
  const children = (layoutRoute && layoutRoute.children) || []
  return children
    .filter((r) => r.meta && r.meta.title && !r.meta.hidden)
    .filter((r) => !r.meta.perm || store.can(r.meta.perm))
    .map((r) => ({
      path: '/' + r.path.replace(/^\//, ''),
      title: r.meta.title,
      icon: r.meta.icon || 'Menu'
    }))
})

// 详情页要让它父级的列表项保持高亮，否则点进详情菜单就全灭
const activePath = computed(() => {
  if (route.path.startsWith('/tickets')) return route.path === '/tickets' ? '/tickets' : '/tickets'
  return route.path
})
</script>

<style scoped>
.atp-menu {
  border-right: none;
  background: var(--atp-side-bg);
}

.atp-menu :deep(.el-menu-item) {
  height: 46px;
  line-height: 46px;
  margin: 2px 8px;
  border-radius: 5px;
  color: var(--atp-side-text);
}

.atp-menu :deep(.el-menu-item:hover) {
  background: var(--atp-side-hover);
  color: #ffffff;
}

.atp-menu :deep(.el-menu-item.is-active) {
  background: var(--atp-side-active);
  color: var(--atp-side-text-active);
  font-weight: 600;
}
</style>
