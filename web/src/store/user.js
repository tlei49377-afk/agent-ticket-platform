import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, me as meApi } from '../api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('atp_token') || '',
    user: JSON.parse(localStorage.getItem('atp_user') || 'null'),
    permissionsLoaded: !!localStorage.getItem('atp_user')
  }),

  getters: {
    isLogin: (state) => !!state.token,
    name: (state) => (state.user && state.user.name) || '未登录',
    roles: (state) => (state.user && state.user.roles) || [],
    permissions: (state) => (state.user && state.user.permissions) || []
  },

  actions: {
    // 后端仍是最终权限边界；权限信息尚未加载时才放行界面渲染。
    can(code) {
      return !this.permissionsLoaded || this.permissions.includes(code)
    },

    async doLogin(form) {
      const loginData = await loginApi(form)
      this.token = loginData.token
      this.user = { ...loginData, roles: [], permissions: [] }
      this.permissionsLoaded = false
      localStorage.setItem('atp_token', loginData.token)
      localStorage.setItem('atp_user', JSON.stringify(this.user))

      try {
        const profile = await meApi()
        this.user = { ...loginData, ...profile }
        this.permissionsLoaded = true
        localStorage.setItem('atp_user', JSON.stringify(this.user))
      } catch (e) {
        // 登录本身成功时不让权限刷新失败把用户卡在登录页
      }
      return this.user
    },

    async loadMe() {
      if (!this.token) return null
      const profile = await meApi()
      this.user = { ...(this.user || {}), ...profile }
      this.permissionsLoaded = true
      localStorage.setItem('atp_user', JSON.stringify(this.user))
      return this.user
    },

    async doLogout() {
      try {
        await logoutApi()
      } catch (e) {
        // 本地登录态必须清干净
      }
      this.token = ''
      this.user = null
      this.permissionsLoaded = false
      localStorage.removeItem('atp_token')
      localStorage.removeItem('atp_user')
    }
  }
})
