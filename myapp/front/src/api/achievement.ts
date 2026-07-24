import http from '../utils/axios'

export interface AchievementItem {
  type: string
  points: number
}

export interface AchievementSummary {
  total: number
  items: AchievementItem[]
}

export const achievementApi = {
  getMy: () =>
    http.get('/api/v1/achievements/my').then(r => r.data as AchievementSummary),
}
