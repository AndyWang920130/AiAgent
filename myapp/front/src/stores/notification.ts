import { ref, computed } from 'vue'
import { notificationApi, type NotificationItem, type NotificationType } from '../api/notification'

export type { NotificationType }

/** Shape consumed by the bell popover and NotificationView (adds `time` for the formatters). */
export interface Notification {
  id: number
  type: NotificationType
  title: string
  content: string
  link: string | null
  time: string // 'YYYY-MM-DD HH:mm' — what formatTime/formatPopoverTime slice
  read: boolean
}

export const notifications = ref<Notification[]>([])
export const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

let pollTimer: ReturnType<typeof setInterval> | null = null

// Normalize the backend ISO instant into the 'YYYY-MM-DD HH:mm' local-ish string the existing
// string-slicing formatters expect.
function toDisplayTime(iso: string): string {
  const d = new Date(iso)
  if (isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function mapItem(item: NotificationItem): Notification {
  return {
    id: item.id,
    type: item.type,
    title: item.title,
    content: item.content || '',
    link: item.link,
    time: toDisplayTime(item.createdDate),
    read: item.read,
  }
}

export async function fetchNotifications(): Promise<void> {
  try {
    const list = await notificationApi.list()
    notifications.value = list.map(mapItem)
  } catch {
    // non-fatal — leave the current list in place
  }
}

async function refreshUnread(): Promise<void> {
  try {
    const count = await notificationApi.unreadCount()
    // If the badge count and our loaded list disagree (e.g. a new notification arrived),
    // refetch the list so the bell/popover reflect it.
    const localUnread = notifications.value.filter(n => !n.read).length
    if (count !== localUnread) await fetchNotifications()
  } catch {
    // non-fatal
  }
}

export function startPolling(): void {
  if (pollTimer) return
  fetchNotifications()
  pollTimer = setInterval(refreshUnread, 15000)
}

export function stopPolling(): void {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

export async function markAsRead(id: number): Promise<void> {
  const n = notifications.value.find(n => n.id === id)
  if (!n || n.read) return
  n.read = true // optimistic
  try {
    await notificationApi.markRead(id)
  } catch {
    n.read = false
  }
}

export async function markAllAsRead(): Promise<void> {
  const previouslyUnread = notifications.value.filter(n => !n.read)
  previouslyUnread.forEach(n => (n.read = true))
  try {
    await notificationApi.markAllRead()
  } catch {
    previouslyUnread.forEach(n => (n.read = false))
  }
}

export async function deleteNotification(id: number): Promise<void> {
  const idx = notifications.value.findIndex(n => n.id === id)
  if (idx === -1) return
  const [removed] = notifications.value.splice(idx, 1)
  try {
    await notificationApi.remove(id)
  } catch {
    notifications.value.splice(idx, 0, removed)
  }
}

export async function clearRead(): Promise<void> {
  const kept = notifications.value.filter(n => !n.read)
  const previous = notifications.value
  notifications.value = kept
  try {
    await notificationApi.clearRead()
  } catch {
    notifications.value = previous
  }
}
