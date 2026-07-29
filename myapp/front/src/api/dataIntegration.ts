import http from '../utils/axios'

/** A key/value row used for headers, query params, body config, response config. */
export interface KeyValue {
  key: string
  value: string
}

/**
 * Wire shape of a data integration. The four config fields are JSON strings
 * (arrays of KeyValue) on the wire; the store (de)serializes them to KeyValue[].
 */
export interface DataIntegrationDTO {
  id?: number
  name: string
  description?: string | null
  baseUrl: string
  path?: string | null
  method?: string | null
  headers?: string | null
  queryParams?: string | null
  bodyConfig?: string | null
  bodyType?: string | null
  bodyRaw?: string | null
  responseConfig?: string | null
  authSourceId?: number | null
  authTokenPath?: string | null
  authHeaderName?: string | null
  authHeaderTemplate?: string | null
  authBodyProperty?: string | null
  createdBy?: string
  createdDate?: string
  lastModifiedBy?: string
  lastModifiedDate?: string
}

export interface ExecuteResult {
  status: number
  durationMs: number
  headers: Record<string, string>
  body: string
  success: boolean
  error: string | null
}

export const dataIntegrationApi = {
  list: () =>
    http.get('/api/v1/data-integrations').then(r => r.data as DataIntegrationDTO[]),

  get: (id: number) =>
    http.get(`/api/v1/data-integrations/${id}`).then(r => r.data as DataIntegrationDTO),

  create: (data: DataIntegrationDTO) =>
    http.post('/api/v1/data-integrations', data).then(r => r.data as DataIntegrationDTO),

  update: (id: number, data: DataIntegrationDTO) =>
    http.put(`/api/v1/data-integrations/${id}`, { ...data, id }).then(r => r.data as DataIntegrationDTO),

  remove: (id: number) =>
    http.delete(`/api/v1/data-integrations/${id}`),

  execute: (id: number) =>
    http.post(`/api/v1/data-integrations/${id}/execute`).then(r => r.data as ExecuteResult),
}
