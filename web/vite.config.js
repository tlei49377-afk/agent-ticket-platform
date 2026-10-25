import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '127.0.0.1',
    port: 5174,
    // 当前后端监听 18096，并把 /api 前缀去掉后再转发
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:18096',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
