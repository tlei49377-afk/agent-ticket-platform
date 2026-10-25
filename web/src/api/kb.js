import request from './request'

// 知识库检索测试接口。文档上传和正文管理尚未在当前后端主线开放。
export const testRetrieve = (data) => request.post('/kb/test-retrieve', data)
