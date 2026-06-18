<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  InfoCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  CloseCircleOutlined,
  CheckOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue'
import {
  notifications,
  unreadCount,
  markAsRead,
  markAllAsRead,
  deleteNotification,
  clearRead,
} from '../stores/notification'

const { t, locale } = useI18n()

const activeTab = ref('all')

const typeConfig: Record<string, { icon: object; color: string; bg: string }> = {
  info:    { icon: InfoCircleOutlined,          color: '#1890ff', bg: '#e6f4ff' },
  success: { icon: CheckCircleOutlined,         color: '#52c41a', bg: '#f6ffed' },
  warning: { icon: ExclamationCircleOutlined,   color: '#faad14', bg: '#fffbe6' },
  error:   { icon: CloseCircleOutlined,         color: '#ff4d4f', bg: '#fff2f0' },
}

const displayList = computed(() =>
  activeTab.value === 'unread'
    ? notifications.value.filter(n => !n.read)
    : notifications.value
)

const readCount = computed(() => notifications.value.filter(n => n.read).length)

function formatTime(timeStr: string): string {
  const date = new Date(timeStr.replace(' ', 'T'))
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yestStart = new Date(todayStart.getTime() - 86400000)
  const notifDay = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const hm = timeStr.slice(11, 16)

  if (notifDay >= todayStart) {
    return locale.value === 'zh-CN' ? `今天 ${hm}` : `Today ${hm}`
  }
  if (notifDay >= yestStart) {
    return locale.value === 'zh-CN' ? `昨天 ${hm}` : `Yesterday ${hm}`
  }
  if (date.getFullYear() === now.getFullYear()) {
    return timeStr.slice(5, 10)
  }
  return timeStr.slice(0, 10)
}
</script>

<template>
  <div class="notification-view">
    <a-card :bordered="false">
      <template #title>
        <span>{{ t('notification.title') }}</span>
        <a-badge
          v-if="unreadCount > 0"
          :count="unreadCount"
          :overflow-count="99"
          style="margin-left: 10px"
        />
      </template>
      <template #extra>
        <a-space>
          <a-button
            size="small"
            :disabled="unreadCount === 0"
            @click="markAllAsRead"
          >
            {{ t('notification.markAllRead') }}
          </a-button>
          <a-button
            size="small"
            danger
            :disabled="readCount === 0"
            @click="clearRead"
          >
            {{ t('notification.clearRead') }}
          </a-button>
        </a-space>
      </template>

      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="all">
          <template #tab>
            {{ t('notification.all') }}
            <a-badge
              :count="notifications.length"
              :overflow-count="99"
              :color="'#bbb'"
              style="margin-left: 4px"
            />
          </template>
        </a-tab-pane>
        <a-tab-pane key="unread">
          <template #tab>
            {{ t('notification.unread') }}
            <a-badge
              :count="unreadCount"
              :overflow-count="99"
              style="margin-left: 4px"
            />
          </template>
        </a-tab-pane>
      </a-tabs>

      <!-- Empty state -->
      <a-empty
        v-if="displayList.length === 0"
        :description="activeTab === 'unread' ? t('notification.noUnread') : t('notification.noMessages')"
        style="padding: 48px 0"
      />

      <!-- Notification list -->
      <a-list v-else :data-source="displayList" item-layout="horizontal" :pagination="{ pageSize: 10, size: 'small', showTotal: (total: number) => t('notification.total', { total }) }">
        <template #renderItem="{ item }">
          <a-list-item class="notif-item" :class="{ 'is-unread': !item.read }">
            <a-list-item-meta>
              <template #avatar>
                <div
                  class="type-icon-wrap"
                  :style="{ background: typeConfig[item.type].bg }"
                >
                  <component
                    :is="typeConfig[item.type].icon"
                    :style="{ color: typeConfig[item.type].color, fontSize: '20px' }"
                  />
                </div>
              </template>
              <template #title>
                <div class="notif-title-row">
                  <span class="unread-dot" v-if="!item.read" />
                  <span class="notif-title" :class="{ bold: !item.read }">{{ item.title }}</span>
                  <span class="notif-time">{{ formatTime(item.time) }}</span>
                </div>
              </template>
              <template #description>
                <span class="notif-content">{{ item.content }}</span>
              </template>
            </a-list-item-meta>
            <template #actions>
              <a-tooltip v-if="!item.read" :title="t('notification.markRead')">
                <a-button type="text" size="small" @click="markAsRead(item.id)">
                  <template #icon><CheckOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip :title="t('notification.delete')">
                <a-button type="text" size="small" danger @click="deleteNotification(item.id)">
                  <template #icon><DeleteOutlined /></template>
                </a-button>
              </a-tooltip>
            </template>
          </a-list-item>
        </template>
      </a-list>
    </a-card>
  </div>
</template>

<style scoped>
.notification-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notif-item {
  padding: 12px 16px;
  transition: background 0.2s;
  border-left: 3px solid transparent;
}

.notif-item.is-unread {
  background: rgba(24, 144, 255, 0.04);
  border-left-color: #1890ff;
}

.type-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notif-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.unread-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #1890ff;
  flex-shrink: 0;
}

.notif-title {
  font-size: 14px;
  color: inherit;
}

.notif-title.bold {
  font-weight: 600;
}

.notif-time {
  margin-left: auto;
  font-size: 12px;
  color: #bbb;
  white-space: nowrap;
}

.notif-content {
  font-size: 13px;
  line-height: 1.6;
  color: #888;
}
</style>
