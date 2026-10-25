import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 60000
})

function clearLogin(message = '登录已过期，请重新登录') {
  localStorage.removeItem('atp_token')
  localStorage.removeItem('atp_user')
  ElMessage.error(message)
  if (router.currentRoute.value.path !== '/login') router.push('/login')
}

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('atp_token')
  if (token) config.headers.token = token
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body instanceof Blob) return body
    if (body && typeof body.code !== 'undefined') {
      const message = body.msg || body.message || '请求失败'
      if (body.code === 1) return body.data
      if (body.code === 0 && /未登录|登录已过期|token/i.test(message)) {
        clearLogin()
        return Promise.reject(new Error(message))
      }
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      clearLogin()
      return Promise.reject(error)
    }
    const backendMessage = error.response?.data?.msg || error.response?.data?.message
    const message = backendMessage || (status
      ? `HTTP ${status} ${error.response?.statusText || ''}`.trim()
      : (error.message || '网络异常'))
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
