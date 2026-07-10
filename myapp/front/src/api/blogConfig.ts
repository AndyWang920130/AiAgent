import http from '../utils/axios'

export type BlogConfigType = 'CATEGORY' | 'TAG' | 'TAG_COLOR'

export interface BlogConfigDTO {
  id?: number
  type: BlogConfigType
  name: string
  value?: string | null
  description?: string | null
  sortOrder?: number | null
}

export const blogConfigApi = {
  list: (type?: BlogConfigType) =>
    http.get('/api/v1/blog-configs', { params: type ? { type } : undefined })
      .then(r => r.data as BlogConfigDTO[]),

  create: (data: BlogConfigDTO) =>
    http.post('/api/v1/blog-configs', data).then(r => r.data as BlogConfigDTO),

  update: (id: number, data: BlogConfigDTO) =>
    http.put(`/api/v1/blog-configs/${id}`, { ...data, id }).then(r => r.data as BlogConfigDTO),

  remove: (id: number) =>
    http.delete(`/api/v1/blog-configs/${id}`),
}
