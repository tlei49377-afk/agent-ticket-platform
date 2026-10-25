import request from './request'

// 工单指派下拉和筛选项使用的用户列表
export const listAllUsers = () => request.get('/users/all')
