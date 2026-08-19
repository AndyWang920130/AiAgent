<script lang="ts" setup>
// Sidebar navigation menu, shared by the desktop sider and the mobile drawer in MainLayout.
// Routing state (selectedKeys/openKeys) is owned by the parent and passed via v-model; this
// component only renders the menu and emits `navigate(key)` on item click.
import { useI18n } from 'vue-i18n'
import {
  HomeOutlined,
  UserOutlined,
  ToolOutlined,
  UnorderedListOutlined,
  PlusCircleOutlined,
  SettingOutlined,
  TagsOutlined,
  GiftOutlined,
  TrophyOutlined,
  TeamOutlined,
  HeartOutlined,
  LockOutlined,
  NumberOutlined,
  ApiOutlined,
  CodeOutlined,
  DatabaseOutlined,
  BorderOutlined,
} from '@ant-design/icons-vue'

defineProps<{
  selectedKeys: string[]
  openKeys: string[]
  isAdmin: boolean
  collapsed?: boolean
}>()

const emit = defineEmits<{
  (e: 'navigate', key: string): void
  (e: 'update:selectedKeys', keys: string[]): void
  (e: 'update:openKeys', keys: string[]): void
}>()

const { t } = useI18n()
</script>

<template>
  <div class="logo" @click="emit('navigate', 'home')">
    <span v-if="!collapsed">🚀 MyApp</span>
    <span v-else>🚀</span>
  </div>
  <a-menu
    :selectedKeys="selectedKeys"
    :openKeys="openKeys"
    theme="dark"
    mode="inline"
    @update:selectedKeys="(k: string[]) => emit('update:selectedKeys', k)"
    @update:openKeys="(k: string[]) => emit('update:openKeys', k)"
  >
    <a-menu-item key="home" @click="emit('navigate', 'home')">
      <HomeOutlined />
      <span>{{ t('menu.home') }}</span>
    </a-menu-item>
    <a-sub-menu v-if="isAdmin" key="home-sub">
      <template #title>
        <UnorderedListOutlined />
        <span>{{ t('menu.blog') }}</span>
      </template>
      <a-menu-item key="blog-list" @click="emit('navigate', 'blog-list')">
        <UnorderedListOutlined />
        <span>{{ t('menu.blogList') }}</span>
      </a-menu-item>
      <a-menu-item key="blog-add" @click="emit('navigate', 'blog-add')">
        <PlusCircleOutlined />
        <span>{{ t('menu.addBlog') }}</span>
      </a-menu-item>
    </a-sub-menu>
    <a-sub-menu key="mini-game-sub">
      <template #title>
        <GiftOutlined />
        <span>{{ t('menu.miniGame') }}</span>
      </template>
      <a-menu-item key="lottery" @click="emit('navigate', 'lottery')">
        <GiftOutlined />
        <span>{{ t('menu.lottery') }}</span>
      </a-menu-item>
      <a-menu-item key="class-lottery" @click="emit('navigate', 'class-lottery')">
        <TeamOutlined />
        <span>{{ t('menu.classLottery') }}</span>
      </a-menu-item>
      <a-menu-item key="gomoku" @click="emit('navigate', 'gomoku')">
        <BorderOutlined />
        <span>{{ t('menu.gomoku') }}</span>
      </a-menu-item>
    </a-sub-menu>
    <a-sub-menu v-if="isAdmin" key="settings-sub">
      <template #title>
        <SettingOutlined />
        <span>{{ t('menu.systemSetting') }}</span>
      </template>
      <a-menu-item key="blog-config" @click="emit('navigate', 'blog-config')">
        <TagsOutlined />
        <span>{{ t('menu.blogConfig') }}</span>
      </a-menu-item>
      <a-menu-item key="game-config" @click="emit('navigate', 'game-config')">
        <TrophyOutlined />
        <span>{{ t('menu.gameConfig') }}</span>
      </a-menu-item>
    </a-sub-menu>
    <a-sub-menu v-if="isAdmin" key="data-center-sub">
      <template #title>
        <DatabaseOutlined />
        <span>{{ t('menu.dataCenter') }}</span>
      </template>
      <a-menu-item key="ecg-chart" @click="emit('navigate', 'ecg-chart')">
        <HeartOutlined />
        <span>{{ t('menu.ecgChart') }}</span>
      </a-menu-item>
      <a-menu-item key="data-integration" @click="emit('navigate', 'data-integration')">
        <ApiOutlined />
        <span>{{ t('menu.dataIntegration') }}</span>
      </a-menu-item>
    </a-sub-menu>
    <a-sub-menu key="tools-sub">
      <template #title>
        <ToolOutlined />
        <span>{{ t('menu.tools') }}</span>
      </template>
      <a-menu-item key="aes-tool" @click="emit('navigate', 'aes-tool')">
        <LockOutlined />
        <span>{{ t('menu.aesTool') }}</span>
      </a-menu-item>
      <a-menu-item key="sha-tool" @click="emit('navigate', 'sha-tool')">
        <NumberOutlined />
        <span>{{ t('menu.shaTool') }}</span>
      </a-menu-item>
      <a-menu-item key="json-to-entity" @click="emit('navigate', 'json-to-entity')">
        <CodeOutlined />
        <span>{{ t('menu.jsonToEntity') }}</span>
      </a-menu-item>
    </a-sub-menu>
    <a-menu-item key="personal" @click="emit('navigate', 'personal')">
      <UserOutlined />
      <span>{{ t('menu.personal') }}</span>
    </a-menu-item>
  </a-menu>
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
</style>
