import { ref, computed } from 'vue'

export type NotificationType = 'info' | 'success' | 'warning' | 'error'

export interface Notification {
  id: number
  type: NotificationType
  title: string
  content: string
  time: string
  read: boolean
}

export const notifications = ref<Notification[]>([
  {
    id: 1,
    type: 'info',
    title: '系统维护公告',
    content: '系统将于今晚 22:00 进行例行维护，预计持续 2 小时，期间服务暂时不可用，请提前做好准备。',
    time: '2026-06-18 10:30',
    read: false,
  },
  {
    id: 2,
    type: 'success',
    title: '博客发布成功',
    content: '您的博客《Getting Started with Vue 3 Composition API》已成功发布，快去查看阅读情况吧！',
    time: '2026-06-18 09:15',
    read: false,
  },
  {
    id: 3,
    type: 'warning',
    title: '存储空间预警',
    content: '您的存储空间已使用 80%，请及时清理无用文件，以免影响正常使用。',
    time: '2026-06-17 16:00',
    read: false,
  },
  {
    id: 4,
    type: 'info',
    title: '新版本上线',
    content: 'MyApp v2.1.0 已发布，本次更新新增暗色模式、多语言国际化等功能，欢迎体验！',
    time: '2026-06-17 10:00',
    read: true,
  },
  {
    id: 5,
    type: 'success',
    title: '成就解锁 🏆',
    content: '恭喜您解锁「多产作家」成就！您已累计发布超过 100 篇技术文章，感谢您的持续贡献。',
    time: '2026-06-16 14:00',
    read: true,
  },
  {
    id: 6,
    type: 'error',
    title: '登录异常提醒',
    content: '检测到您的账号于昨日 08:00 在异地登录，如非本人操作，请立即修改密码并联系管理员。',
    time: '2026-06-15 08:00',
    read: true,
  },
  {
    id: 7,
    type: 'info',
    title: '欢迎加入 MyApp',
    content: '感谢您注册 MyApp！开始您的技术博客之旅，记录成长，分享知识，连接世界。',
    time: '2024-01-15 09:00',
    read: true,
  },
])

export const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

export function markAsRead(id: number) {
  const n = notifications.value.find(n => n.id === id)
  if (n) n.read = true
}

export function markAllAsRead() {
  notifications.value.forEach(n => (n.read = true))
}

export function deleteNotification(id: number) {
  notifications.value = notifications.value.filter(n => n.id !== id)
}

export function clearRead() {
  notifications.value = notifications.value.filter(n => !n.read)
}
