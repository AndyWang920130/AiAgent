import http from '../utils/axios'

export interface EcgSummary {
  id: number
  name: string
  leadName: string
  sampleRate: number
  heartRate: number
  sampleCount: number
  durationMs: number
}

export interface EcgRecord {
  id: number
  name: string
  leadName: string
  sampleRate: number
  heartRate: number
  durationMs: number
  samples: number[]
}

export const ecgApi = {
  list: () =>
    http.get('/api/v1/ecg-records').then(r => r.data as EcgSummary[]),

  get: (id: number) =>
    http.get(`/api/v1/ecg-records/${id}`).then(r => r.data as EcgRecord),
}
