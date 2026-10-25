import request from './request'

export const listApprovals = (params) => request.get('/approvals', { params })

export const listApprovalsByRun = (runId) => request.get('/approvals/by-run/' + runId)

export const decideApproval = (data) => request.post('/approvals/decide', data)

export const resumeApproval = (id) => request.post('/approvals/' + id + '/resume')
