import { createRouter, createWebHistory } from 'vue-router'

// 只注册当前后端真实支持的页面，避免菜单点进去全是 404。
const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/chat',
    children: [
      {
        path: 'chat',
        name: 'chat',
        component: () => import('../views/ChatView.vue'),
        meta: { title: '对话工作台', icon: 'ChatDotRound' }
      },
      {
        path: 'approvals',
        name: 'approvals',
        component: () => import('../views/ApprovalView.vue'),
        meta: { title: '审批中心', icon: 'Stamp', perm: 'ticket:approve' }
      },
      {
        path: 'tickets',
        name: 'tickets',
        component: () => import('../views/TicketListView.vue'),
        meta: { title: '工单管理', icon: 'Tickets', perm: 'ticket:list' }
      },
      {
        path: 'tickets/:id',
        name: 'ticket-detail',
        component: () => import('../views/TicketDetailView.vue'),
        meta: { title: '工单详情', hidden: true, perm: 'ticket:list' }
      },
      {
        path: 'kb',
        name: 'kb',
        component: () => import('../views/KbView.vue'),
        meta: { title: '知识库检索', icon: 'Collection', perm: 'kb:read' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/chat' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('atp_token')
  if (to.meta.public) {
    return token && to.path === '/login' ? '/chat' : true
  }
  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.perm) {
    const user = JSON.parse(localStorage.getItem('atp_user') || 'null')
    const permissions = (user && user.permissions) || []
    // 权限尚未加载时先放行，请求到后端后仍会由权限切面拦截。
    const loaded = !!(user && Array.isArray(user.permissions))
    if (loaded && permissions.length > 0 && !permissions.includes(to.meta.perm)) return '/chat'
  }
  return true
})

export default router


