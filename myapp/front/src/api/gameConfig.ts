import http from '../utils/axios'

export type GameConfigType = 'PRIZE' | 'PARAMETER'

export interface GameConfigDTO {
  id?: number
  type: GameConfigType
  name: string
  value?: string | null
  description?: string | null
  sortOrder?: number | null
}

export const gameConfigApi = {
  list: (type?: GameConfigType) =>
    http.get('/api/v1/game-configs', { params: type ? { type } : undefined })
      .then(r => r.data as GameConfigDTO[]),

  create: (data: GameConfigDTO) =>
    http.post('/api/v1/game-configs', data).then(r => r.data as GameConfigDTO),

  update: (id: number, data: GameConfigDTO) =>
    http.put(`/api/v1/game-configs/${id}`, { ...data, id }).then(r => r.data as GameConfigDTO),

  remove: (id: number) =>
    http.delete(`/api/v1/game-configs/${id}`),
}
