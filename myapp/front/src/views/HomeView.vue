<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  RocketOutlined,
  TrophyOutlined,
  ThunderboltOutlined,
  StarOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  EyeOutlined,
  LikeOutlined,
  MessageOutlined,
  EditOutlined,
} from '@ant-design/icons-vue'

const { t } = useI18n()
const router = useRouter()

const stats = computed(() => [
  { title: t('home.totalPosts'), value: 128, icon: RocketOutlined, color: '#1890ff', change: 12, up: true },
  { title: t('home.followers'), value: 3840, icon: StarOutlined, color: '#52c41a', change: 8, up: true },
  { title: t('home.achievements'), value: 47, icon: TrophyOutlined, color: '#faad14', change: 3, up: true },
  { title: t('home.streakDays'), value: 21, icon: ThunderboltOutlined, color: '#eb2f96', change: 2, up: false },
])

const posts = ref([
  {
    id: 1,
    title: 'Getting Started with Vue 3 Composition API',
    excerpt: 'The Composition API is one of the most exciting features in Vue 3. It provides a flexible way to organize component logic...',
    category: 'Frontend',
    date: '2025-06-01',
    views: 1240,
    likes: 89,
    comments: 23,
    tag: 'Vue',
    tagColor: 'green',
  },
  {
    id: 2,
    title: 'Building Scalable APIs with Spring Boot',
    excerpt: 'Spring Boot makes it easy to create stand-alone, production-grade Spring applications with minimal configuration...',
    category: 'Backend',
    date: '2025-05-28',
    views: 980,
    likes: 64,
    comments: 15,
    tag: 'Java',
    tagColor: 'orange',
  },
  {
    id: 3,
    title: 'TypeScript Best Practices in 2025',
    excerpt: 'TypeScript has become the go-to choice for large-scale JavaScript applications. Here are the patterns that matter most...',
    category: 'Language',
    date: '2025-05-22',
    views: 2100,
    likes: 142,
    comments: 37,
    tag: 'TypeScript',
    tagColor: 'blue',
  },
  {
    id: 4,
    title: 'Docker & Kubernetes: A Practical Guide',
    excerpt: 'Containerization has revolutionized how we deploy applications. Learn how to effectively use Docker and K8s together...',
    category: 'DevOps',
    date: '2025-05-15',
    views: 1560,
    likes: 98,
    comments: 29,
    tag: 'DevOps',
    tagColor: 'purple',
  },
])

const activeTab = ref('all')
const categories = ['all', 'Frontend', 'Backend', 'Language', 'DevOps']

const filteredPosts = () =>
  activeTab.value === 'all' ? posts.value : posts.value.filter(p => p.category === activeTab.value)
</script>

<template>
  <div class="home-view">
    <!-- Welcome banner -->
    <div class="welcome-banner">
      <div class="banner-content">
        <h1>{{ t('home.welcome') }}</h1>
        <p>{{ t('home.tagline') }}</p>
      </div>
    </div>

    <!-- Stats -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 24px">
      <a-col :xs="24" :sm="12" :lg="6" v-for="stat in stats" :key="stat.title">
        <a-card :bordered="false" class="stat-card">
          <a-statistic
            :title="stat.title"
            :value="stat.value"
            :value-style="{ color: stat.color, fontSize: '28px', fontWeight: '700' }"
          >
            <template #prefix>
              <component :is="stat.icon" :style="{ color: stat.color }" />
            </template>
            <template #suffix>
              <span :style="{ fontSize: '14px', color: stat.up ? '#52c41a' : '#ff4d4f' }">
                <component :is="stat.up ? ArrowUpOutlined : ArrowDownOutlined" />
                {{ stat.change }}%
              </span>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <!-- Posts -->
    <a-card :title="t('home.latestPosts')" :bordered="false">
      <template #extra>
        <a-space>
          <a-tabs v-model:activeKey="activeTab" size="small">
            <a-tab-pane v-for="cat in categories" :key="cat" :tab="cat === 'all' ? t('home.all') : cat" />
          </a-tabs>
          <a-button type="primary" size="small" @click="router.push('/blog/add')">
            <template #icon><EditOutlined /></template>
            {{ t('home.writePost') }}
          </a-button>
        </a-space>
      </template>

      <a-list :data-source="filteredPosts()" item-layout="vertical">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <div class="post-title-row">
                  <span class="post-title">{{ item.title }}</span>
                  <a-tag :color="item.tagColor">{{ item.tag }}</a-tag>
                </div>
              </template>
              <template #description>{{ item.date }} · {{ item.category }}</template>
            </a-list-item-meta>
            <p class="post-excerpt">{{ item.excerpt }}</p>
            <template #actions>
              <span><EyeOutlined /> {{ item.views }}</span>
              <span><LikeOutlined /> {{ item.likes }}</span>
              <span><MessageOutlined /> {{ item.comments }}</span>
            </template>
          </a-list-item>
        </template>
      </a-list>
    </a-card>
  </div>
</template>

<style scoped>
.home-view { display: flex; flex-direction: column; gap: 24px; }

.welcome-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 32px;
  color: #fff;
}
.welcome-banner h1 { color: #fff; margin: 0 0 8px; font-size: 28px; }
.welcome-banner p { margin: 0; opacity: 0.9; font-size: 15px; }

.stat-card { border-radius: 10px; }

.post-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.post-title { font-size: 16px; font-weight: 600; cursor: pointer; }
.post-title:hover { color: #1890ff; }
.post-excerpt { color: #888; margin: 4px 0 0; line-height: 1.6; }
</style>
