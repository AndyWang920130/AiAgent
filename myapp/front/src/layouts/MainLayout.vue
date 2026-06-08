<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useRouter, RouterView } from 'vue-router'
import {
  HomeOutlined,
  UserOutlined,
  LogoutOutlined,
  BulbOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  ToolOutlined,
} from '@ant-design/icons-vue'
import { theme as appTheme, toggleTheme } from '../utils/theme'
import { clearAuth, getUser } from '../utils/auth'
import { theme as antTheme } from 'ant-design-vue'

const router = useRouter()
const collapsed = ref(false)
const user = getUser<{ username: string; name: string }>()

const selectedKeys = ref<string[]>(['home'])

const antConfig = computed(() => ({
  algorithm: appTheme.value === 'dark' ? antTheme.darkAlgorithm : antTheme.defaultAlgorithm,
}))

function logout() {
  clearAuth()
  router.push('/login')
}

function navigate(key: string) {
  selectedKeys.value = [key]
  router.push('/' + key)
}
</script>

<template>
  <a-config-provider :theme="antConfig">
    <a-layout style="min-height: 100vh">
      <!-- Sider -->
      <a-layout-sider v-model:collapsed="collapsed" collapsible :trigger="null" :width="220">
        <div class="logo">
          <span v-if="!collapsed">🚀 MyApp</span>
          <span v-else>🚀</span>
        </div>
        <a-menu
          v-model:selectedKeys="selectedKeys"
          theme="dark"
          mode="inline"
        >
          <a-menu-item key="home" @click="navigate('home')">
            <HomeOutlined />
            <span>Home</span>
          </a-menu-item>
          <a-menu-item key="tools" @click="navigate('tools')">
            <ToolOutlined />
            <span>Tools</span>
          </a-menu-item>
          <a-menu-item key="personal" @click="navigate('personal')">
            <UserOutlined />
            <span>Personal</span>
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
            <a-tooltip :title="appTheme === 'dark' ? 'Switch to Light' : 'Switch to Dark'">
              <a-button type="text" @click="toggleTheme">
                <BulbOutlined />
                {{ appTheme === 'dark' ? 'Light' : 'Dark' }}
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
                    <UserOutlined /> Profile
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="logout" @click="logout">
                    <LogoutOutlined /> Logout
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
          MyApp ©2025
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
