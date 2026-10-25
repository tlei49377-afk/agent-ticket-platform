// 时间 / 金额 / 耗时的统一格式化，避免每个页面各写一份

const pad = (n) => String(n).padStart(2, '0')

// 后端给的是 2026-10-08T15:50:00 这种 ISO 串，直接展示会带个 T
export function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(String(value).replace('T', ' ').replace(/-/g, '/'))
  if (Number.isNaN(d.getTime())) return String(value)
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
    ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds())
}

export function formatDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

// 相对时间：刚刚 / 5 分钟前 / 3 小时前 / 2 天前
export function fromNow(value) {
  if (!value) return '-'
  const d = new Date(String(value).replace('T', ' ').replace(/-/g, '/'))
  if (Number.isNaN(d.getTime())) return String(value)
  const diff = Date.now() - d.getTime()
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return Math.floor(diff / 60000) + ' 分钟前'
  if (diff < 24 * 60 * 60 * 1000) return Math.floor(diff / 3600000) + ' 小时前'
  return Math.floor(diff / 86400000) + ' 天前'
}

export function formatCost(value) {
  const n = Number(value || 0)
  return n === 0 ? '0' : n.toFixed(6)
}

export function formatDuration(ms) {
  const n = Number(ms || 0)
  if (n === 0) return '0ms'
  if (n < 1000) return n + 'ms'
  return (n / 1000).toFixed(2) + 's'
}

// SLA 剩余分钟转成人话，列表里显示“还剩 3 小时 35 分”
export function formatRemain(minutes) {
  const n = Number(minutes)
  if (Number.isNaN(n)) return '-'
  if (n <= 0) return '已超期'
  if (n < 60) return '还剩 ' + n + ' 分'
  const h = Math.floor(n / 60)
  const m = n % 60
  return '还剩 ' + h + ' 小时' + (m ? ' ' + m + ' 分' : '')
}
