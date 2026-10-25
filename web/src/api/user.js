import request from './request'

// 工单指派下拉所需的精简用户列表
export const listAllUsers = () => request.get('/users/all')

// 工单指派候选人：只包含客服、主管和管理员，不包含提交人
export const listAssignableUsers = () => request.get('/users/assignable')

