<script lang="ts" setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute, RouterView } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  HomeOutlined,
  UserOutlined,
  LogoutOutlined,
  BulbOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  ToolOutlined,
  UnorderedListOutlined,
  PlusCircleOutlined,
  TranslationOutlined,
  BellOutlined,
  InfoCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  CloseCircleOutlined,
  SettingOutlined,
  TagsOutlined,
  GiftOutlined,
  TrophyOutlined,
  TeamOutlined,
  HeartOutlined,
  AppstoreOutlined,
  LockOutlined,
  NumberOutlined,
} from '@ant-design/icons-vue'
import { theme as appTheme, toggleTheme } from '../utils/theme'
import { clearAuth, getUser } from '../utils/auth'
import { theme as antTheme } from 'ant-design-vue'
import { setLocale, getLocale } from '../i18n'
import antZhCN from 'ant-design-vue/es/locale/zh_CN'
import antEnUS from 'ant-design-vue/es/locale/en_US'
import {
  notifications,
  unreadCount,
  markAsRead,
  markAllAsRead,
  type NotificationType,
} from '../stores/notification'

const { t, locale } = useI18n()
const router = useRouter()
const route = useRoute()
const collapsed = ref(false)
const bellOpen = ref(false)
const user = getUser<{ username: string; name: string; role?: string }>()
const isAdmin = computed(() => user?.role === 'ADMIN')

const keyToPath: Record<string, string> = {
  'home': '/home',
  'blog-list': '/blog',
  'blog-add': '/blog/add',
  'third-party-tools': '/tools',
  'aes-tool': '/tools/aes',
  'sha-tool': '/tools/sha',
  'personal': '/personal',
  'blog-config': '/settings/blog-config',
  'game-config': '/settings/game-config',
  'ecg-chart': '/ecg',
  'lottery': '/mini-game/lottery',
  'class-lottery': '/mini-game/class-lottery',
}

const pathToKey: Record<string, string> = Object.fromEntries(
  Object.entries(keyToPath).map(([k, v]) => [v, k])
)

function resolveSelectedKey(path: string): string {
  if (pathToKey[path]) return pathToKey[path]
  for (const [p, k] of Object.entries(pathToKey)) {
    if (path.startsWith(p + '/')) return k
  }
  return 'home'
}

const selectedKeys = ref<string[]>([resolveSelectedKey(route.path)])
const openKeys = ref<string[]>(['home-sub'])

watch(() => route.path, path => {
  selectedKeys.value = [resolveSelectedKey(path)]
})

const themeConfig = computed(() => ({
  algorithm: appTheme.value === 'dark' ? antTheme.darkAlgorithm : antTheme.defaultAlgorithm,
}))

const antLocale = computed(() => locale.value === 'zh-CN' ? antZhCN : antEnUS)

const typeConfig: Record<NotificationType, { icon: object; color: string; bg: string }> = {
  info:    { icon: InfoCircleOutlined,        color: '#1890ff', bg: '#e6f4ff' },
  success: { icon: CheckCircleOutlined,       color: '#52c41a', bg: '#f6ffed' },
  warning: { icon: ExclamationCircleOutlined, color: '#faad14', bg: '#fffbe6' },
  error:   { icon: CloseCircleOutlined,       color: '#ff4d4f', bg: '#fff2f0' },
}

const recentNotifications = computed(() =>
  [...notifications.value].slice(0, 5)
)

function logout() {
  clearAuth()
  router.push('/login')
}

function navigate(key: string) {
  selectedKeys.value = [key]
  router.push(keyToPath[key] || '/' + key)
}

function toggleLang() {
  setLocale(getLocale() === 'zh-CN' ? 'en-US' : 'zh-CN')
}

function goToNotifications() {
  bellOpen.value = false
  router.push('/notifications')
}

function formatPopoverTime(timeStr: string): string {
  const date = new Date(timeStr.replace(' ', 'T'))
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yestStart = new Date(todayStart.getTime() - 86400000)
  const notifDay = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const hm = timeStr.slice(11, 16)
  if (notifDay >= todayStart) return hm
  if (notifDay >= yestStart) return locale.value === 'zh-CN' ? '昨天' : 'Yesterday'
  return timeStr.slice(5, 10)
}
</script>

<template>
  <a-config-provider :theme="themeConfig" :locale="antLocale">
    <a-layout style="min-height: 100vh">
      <!-- Sider -->
      <a-layout-sider v-model:collapsed="collapsed" collapsible :trigger="null" :width="220" class="app-sider">
        <div class="logo" @click="navigate('home')">
          <span v-if="!collapsed">🚀 MyApp</span>
          <span v-else>🚀</span>
        </div>
        <a-menu
          v-model:selectedKeys="selectedKeys"
          v-model:openKeys="openKeys"
          theme="dark"
          mode="inline"
        >
          <a-menu-item key="home" @click="navigate('home')">
            <HomeOutlined />
            <span>{{ t('menu.home') }}</span>
          </a-menu-item>
          <a-sub-menu key="home-sub">
            <template #title>
              <UnorderedListOutlined />
              <span>{{ t('menu.blog') }}</span>
            </template>
            <a-menu-item key="blog-list" @click="navigate('blog-list')">
              <UnorderedListOutlined />
              <span>{{ t('menu.blogList') }}</span>
            </a-menu-item>
            <a-menu-item key="blog-add" @click="navigate('blog-add')">
              <PlusCircleOutlined />
              <span>{{ t('menu.addBlog') }}</span>
            </a-menu-item>
          </a-sub-menu>
          <a-sub-menu v-if="isAdmin" key="settings-sub">
            <template #title>
              <SettingOutlined />
              <span>{{ t('menu.systemSetting') }}</span>
            </template>
            <a-menu-item key="blog-config" @click="navigate('blog-config')">
              <TagsOutlined />
              <span>{{ t('menu.blogConfig') }}</span>
            </a-menu-item>
            <a-menu-item key="game-config" @click="navigate('game-config')">
              <TrophyOutlined />
              <span>{{ t('menu.gameConfig') }}</span>
            </a-menu-item>
          </a-sub-menu>
          <a-menu-item v-if="isAdmin" key="ecg-chart" @click="navigate('ecg-chart')">
            <HeartOutlined />
            <span>{{ t('menu.ecgChart') }}</span>
          </a-menu-item>
          <a-sub-menu key="mini-game-sub">
            <template #title>
              <GiftOutlined />
              <span>{{ t('menu.miniGame') }}</span>
            </template>
            <a-menu-item key="lottery" @click="navigate('lottery')">
              <GiftOutlined />
              <span>{{ t('menu.lottery') }}</span>
            </a-menu-item>
            <a-menu-item key="class-lottery" @click="navigate('class-lottery')">
              <TeamOutlined />
              <span>{{ t('menu.classLottery') }}</span>
            </a-menu-item>
          </a-sub-menu>
          <a-sub-menu key="tools-sub">
            <template #title>
              <ToolOutlined />
              <span>{{ t('menu.tools') }}</span>
            </template>
            <a-menu-item key="third-party-tools" @click="navigate('third-party-tools')">
              <AppstoreOutlined />
              <span>{{ t('menu.thirdPartyTools') }}</span>
            </a-menu-item>
            <a-menu-item key="aes-tool" @click="navigate('aes-tool')">
              <LockOutlined />
              <span>{{ t('menu.aesTool') }}</span>
            </a-menu-item>
            <a-menu-item key="sha-tool" @click="navigate('sha-tool')">
              <NumberOutlined />
              <span>{{ t('menu.shaTool') }}</span>
            </a-menu-item>
          </a-sub-menu>
          <a-menu-item key="personal" @click="navigate('personal')">
            <UserOutlined />
            <span>{{ t('menu.personal') }}</span>
          </a-menu-item>
        </a-menu>
      </a-layout-sider>

      <a-layout>
        <!-- Header -->
        <a-layout-header
          class="app-header"
          :style="{
            background: appTheme === 'dark' ? '#141414' : '#fff',
            color: appTheme === 'dark' ? 'rgba(255,255,255,0.85)' : 'rgba(0,0,0,0.85)',
          }"
        >
          <div class="header-left">
            <component
              :is="collapsed ? MenuUnfoldOutlined : MenuFoldOutlined"
              class="trigger"
              :style="{ color: appTheme === 'dark' ? 'rgba(255,255,255,0.85)' : 'rgba(0,0,0,0.85)' }"
              @click="collapsed = !collapsed"
            />
          </div>
          <div class="header-right">
            <!-- Theme toggle -->
            <a-tooltip :title="appTheme === 'dark' ? t('theme.switchToLight') : t('theme.switchToDark')">
              <a-button type="text" @click="toggleTheme">
                <BulbOutlined />
                {{ appTheme === 'dark' ? t('theme.light') : t('theme.dark') }}
              </a-button>
            </a-tooltip>

            <!-- Language toggle -->
            <a-tooltip :title="locale === 'zh-CN' ? t('lang.en') : t('lang.zh')">
              <a-button type="text" @click="toggleLang">
                <TranslationOutlined />
                {{ locale === 'zh-CN' ? t('lang.en') : t('lang.zh') }}
              </a-button>
            </a-tooltip>

            <!-- Bell with notification popover -->
            <a-popover
              v-model:open="bellOpen"
              placement="bottomRight"
              trigger="click"
              :overlayInnerStyle="{ padding: 0, width: '360px' }"
            >
              <template #content>
                <!-- Popover header -->
                <div class="notif-pop-header">
                  <span class="notif-pop-title">
                    {{ t('notification.title') }}
                    <template v-if="unreadCount > 0">
                      （{{ t('notification.unreadCount', { count: unreadCount }) }}）
                    </template>
                  </span>
                  <a-button
                    type="link"
                    size="small"
                    :disabled="unreadCount === 0"
                    @click="markAllAsRead"
                  >
                    {{ t('notification.markAllRead') }}
                  </a-button>
                </div>

                <!-- Notification items -->
                <div class="notif-pop-body">
                  <template v-if="recentNotifications.length">
                    <div
                      v-for="item in recentNotifications"
                      :key="item.id"
                      class="notif-pop-item"
                      :class="{ 'is-unread': !item.read }"
                      @click="markAsRead(item.id)"
                    >
                      <div
                        class="notif-pop-icon"
                        :style="{ background: typeConfig[item.type].bg }"
                      >
                        <component
                          :is="typeConfig[item.type].icon"
                          :style="{ color: typeConfig[item.type].color, fontSize: '16px' }"
                        />
                      </div>
                      <div class="notif-pop-content">
                        <div class="notif-pop-title-row">
                          <span class="unread-dot" v-if="!item.read" />
                          <span class="notif-pop-name" :class="{ bold: !item.read }">
                            {{ item.title }}
                          </span>
                          <span class="notif-pop-time">{{ formatPopoverTime(item.time) }}</span>
                        </div>
                        <div class="notif-pop-text">{{ item.content }}</div>
                      </div>
                    </div>
                  </template>
                  <a-empty
                    v-else
                    :description="t('notification.noMessages')"
                    style="padding: 24px 0"
                  />
                </div>

                <!-- Popover footer -->
                <div class="notif-pop-footer" @click="goToNotifications">
                  {{ t('notification.viewAll') }} →
                </div>
              </template>

              <a-badge :dot="unreadCount > 0">
                <a-button type="text" class="bell-btn">
                  <BellOutlined />
                </a-button>
              </a-badge>
            </a-popover>

            <!-- User dropdown -->
            <a-dropdown>
              <a-button type="text">
                <UserOutlined />
                {{ user?.name || user?.username || 'User' }}
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="personal" @click="router.push('/personal')">
                    <UserOutlined /> {{ t('header.profile') }}
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="logout" @click="logout">
                    <LogoutOutlined /> {{ t('header.logout') }}
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
        </a-layout-header>

        <!-- Content -->
        <a-layout-content
          class="app-content"
          :style="{ background: appTheme === 'dark' ? '#1f1f1f' : '#fff' }"
        >
          <RouterView />
        </a-layout-content>

        <a-layout-footer style="text-align: center; opacity: 0.5">
          {{ t('footer.copyright') }}
        </a-layout-footer>
      </a-layout>
    </a-layout>
  </a-config-provider>
</template>

<style scoped>
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  background: rgba(255, 255, 255, 0.05);
  cursor: pointer;
  user-select: none;
}
.app-sider {
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
  z-index: 30;
}
.app-header {
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  transition: background 0.3s, color 0.3s;
}
.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.trigger {
  font-size: 18px;
  cursor: pointer;
  transition: color 0.3s;
}
.trigger:hover {
  color: #1890ff;
}
.bell-btn {
  font-size: 16px;
}
.app-content {
  margin: 24px;
  padding: 24px;
  border-radius: 8px;
  min-height: 280px;
  transition: background 0.3s;
}

/* Notification popover */
.notif-pop-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px 8px;
  border-bottom: 1px solid #f0f0f0;
}
.notif-pop-title {
  font-weight: 600;
  font-size: 14px;
}
.notif-pop-body {
  max-height: 360px;
  overflow-y: auto;
}
.notif-pop-item {
  display: flex;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-left: 3px solid transparent;
}
.notif-pop-item:hover {
  background: rgba(0, 0, 0, 0.03);
}
.notif-pop-item.is-unread {
  background: rgba(24, 144, 255, 0.04);
  border-left-color: #1890ff;
}
.notif-pop-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}
.notif-pop-content {
  flex: 1;
  min-width: 0;
}
.notif-pop-title-row {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 2px;
}
.unread-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #1890ff;
  flex-shrink: 0;
}
.notif-pop-name {
  font-size: 13px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notif-pop-name.bold {
  font-weight: 600;
}
.notif-pop-time {
  font-size: 12px;
  color: #bbb;
  white-space: nowrap;
  flex-shrink: 0;
}
.notif-pop-text {
  font-size: 12px;
  color: #999;
  line-height: 1.5;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.notif-pop-footer {
  text-align: center;
  padding: 10px;
  border-top: 1px solid #f0f0f0;
  color: #1890ff;
  cursor: pointer;
  font-size: 13px;
}
.notif-pop-footer:hover {
  background: rgba(24, 144, 255, 0.04);
}
</style>
