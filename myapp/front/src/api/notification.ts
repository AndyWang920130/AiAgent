import http from '../utils/axios'

export type NotificationType = 'info' | 'success' | 'warning' | 'error'

export interface NotificationItem {
  id: number
  type: NotificationType
  title: string
  content: string | null
  link: string | null
  read: boolean
  createdDate: string
}

export const notificationApi = {
  list: (): Promise<NotificationItem[]> =>
    http.get('/api/v1/notifications', { params: { size: 50, sort: 'createdDate,desc' } }).then(r => r.data),

  unreadCount: (): Promise<number> =>
    http.get('/api/v1/notifications/unread-count').then(r => Number(r.data.count || 0)),

  markRead: (id: number): Promise<void> =>
    http.post(`/api/v1/notifications/${id}/read`).then(() => undefined),

  markAllRead: (): Promise<void> =>
    http.post('/api/v1/notifications/read-all').then(() => undefined),

  remove: (id: number): Promise<void> =>
    http.delete(`/api/v1/notifications/${id}`).then(() => undefined),

  clearRead: (): Promise<void> =>
    http.delete('/api/v1/notifications/read').then(() => undefined),
}
