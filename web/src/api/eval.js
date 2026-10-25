import request from './request'

export const listDatasets = () => request.get('/eval/datasets')

export const listRuns = () => request.get('/eval/runs')

export const getRun = (id) => request.get('/eval/runs/' + id)

export const listResults = (id, failedOnly = false) =>
  request.get('/eval/runs/' + id + '/results', { params: { failedOnly } })

// 跑一批评测用例，后端是同步跑完才返回，100 条大约几秒
export const runEval = (datasetId) => request.post('/eval/run?datasetId=' + datasetId)
