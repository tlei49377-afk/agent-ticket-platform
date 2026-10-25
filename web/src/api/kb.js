import request from './request'

/**
 * 混合检索调试接口。
 */
export const testRetrieve = (data) => request.post('/kb/test-retrieve', data)

/**
 * 分页查询知识库文档。
 *
 * @param {{page?:number,pageSize?:number,keyword?:string,status?:string,docType?:string}} params
 */
export const listDocuments = (params) => request.get('/kb/documents', { params })

/**
 * 查看文档全文和全部分块。
 *
 * @param {number|string} id 文档 ID
 */
export const getDocumentContent = (id) =>
  request.get('/kb/documents/' + id + '/content')

/**
 * 导入 PDF、Markdown 或 TXT 文件。
 *
 * FormData 由浏览器自动生成 multipart boundary，不要手工设置 Content-Type。
 *
 * @param {FormData} formData file + 可选 title
 */
export const uploadDocument = (formData) =>
  request.post('/kb/documents/upload', formData, { timeout: 120000 })
