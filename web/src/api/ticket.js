import request from './request'

export const listTickets = (params) => request.get('/tickets', { params })

export const getTicket = (id) => request.get('/tickets/' + id)

export const getTicketHistory = (id) => request.get('/tickets/' + id + '/history')

export const createTicket = (data) => request.post('/tickets', data)

export const changeStatus = (id, data) => request.put('/tickets/' + id + '/status', data)

export const changeAssignee = (id, data) => request.put('/tickets/' + id + '/assignee', data)

export const changePriority = (id, data) => request.put('/tickets/' + id + '/priority', data)

export const replyTicket = (id, data) => request.post('/tickets/' + id + '/replies', data)

// ---- 工单分类（新建工单时选） ----
export const categoryTree = () => request.get('/categories/tree')

export const categoryFlat = () => request.get('/categories/flat')

export const createCategory = (data) => request.post('/categories', data)

export const deleteCategory = (id) => request.delete('/categories/' + id)
