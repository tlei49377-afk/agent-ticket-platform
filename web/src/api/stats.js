import request from './request'

// 各状态工单数量，统计看板顶部的卡片和饼图用
export const ticketStats = () => request.get('/stats/tickets')

// dimType 可选：ALL / AGENT / MODEL / TOOL
export const metricStats = (params) => request.get('/stats/metrics', { params })

export const feedbackStats = () => request.get('/stats/feedback')

export const agentOverview = () => request.get('/stats/agents/overview')
