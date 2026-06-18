<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useRouter, RouterView } from 'vue-router'
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
} from '@ant-design/icons-vue'
import { theme as appTheme, toggleTheme } from '../utils/theme'
import { clearAuth, getUser } from '../utils/auth'
import { theme as antTheme } from 'ant-design-vue'
import { setLocale, getLocale } from '../i18n'
import antZhCN from 'ant-design-vue/es/locale/zh_CN'
import antEnUS from 'ant-design-vue/es/locale/en_US'

const { t, locale } = useI18n()
const router = useRouter()
const collapsed = ref(false)
const user = getUser<{ username: string; name: string }>()

const selectedKeys = ref<string[]>(['blog-list'])
const openKeys = ref<string[]>(['home-sub'])

const keyToPath: Record<string, string> = {
  'blog-list': '/blog',
  'blog-add': '/blog/add',
}

const antConfig = computed(() => ({
  algorithm: appTheme.value === 'dark' ? antTheme.darkAlgorithm : antTheme.defaultAlgorithm,
  locale: locale.value === 'zh-CN' ? antZhCN : antEnUS,
}))

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
</script>

<template>
  <a-config-provider :theme="antConfig" :locale="antConfig.locale">
    <a-layout style="min-height: 100vh">
      <!-- Sider -->
      <a-layout-sider v-model:collapsed="collapsed" collapsible :trigger="null" :width="220">
        <div class="logo">
          <span v-if="!collapsed">🚀 MyApp</span>
          <span v-else>🚀</span>
        </div>
        <a-menu
          v-model:selectedKeys="selectedKeys"
          v-model:openKeys="openKeys"
          theme="dark"
          mode="inline"
        >
          <a-sub-menu key="home-sub">
            <template #title>
              <HomeOutlined />
              <span>{{ t('menu.home') }}</span>
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
          <a-menu-item key="tools" @click="navigate('tools')">
            <ToolOutlined />
            <span>{{ t('menu.tools') }}</span>
          </a-menu-item>
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
            <a-tooltip :title="appTheme === 'dark' ? t('theme.switchToLight') : t('theme.switchToDark')">
              <a-button type="text" @click="toggleTheme">
                <BulbOutlined />
                {{ appTheme === 'dark' ? t('theme.light') : t('theme.dark') }}
              </a-button>
            </a-tooltip>
            <a-tooltip :title="locale === 'zh-CN' ? t('lang.en') : t('lang.zh')">
              <a-button type="text" @click="toggleLang">
                <TranslationOutlined />
                {{ locale === 'zh-CN' ? t('lang.en') : t('lang.zh') }}
              </a-button>
            </a-tooltip>
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
.app-content {
  margin: 24px;
  padding: 24px;
  border-radius: 8px;
  min-height: 280px;
  transition: background 0.3s;
}
</style>
