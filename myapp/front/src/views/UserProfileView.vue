<script lang="ts" setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  TrophyOutlined,
  FileTextOutlined,
  HeartOutlined,
  UserAddOutlined,
  UserDeleteOutlined,
  TeamOutlined,
} from '@ant-design/icons-vue'
import { userApi, type PublicUserProfile } from '../api/user'
import { getUser } from '../utils/auth'
import type { Post } from '../stores/blog'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const login = computed(() => String(route.params.login || ''))
const currentUsername = getUser<{ username: string }>()?.username
const isSelf = computed(() => login.value === currentUsername)

const profile = ref<PublicUserProfile | null>(null)
const blogs = ref<Post[]>([])
const loading = ref(false)
const loadingBlogs = ref(false)
const following = ref(false)
const followerCount = ref(0)
const followingCount = ref(0)
const followLoading = ref(false)

async function load() {
  loading.value = true
  profile.value = null
  try {
    const data = await userApi.getProfile(login.value)
    profile.value = data
    following.value = data.following
    followerCount.value = data.followerCount
    followingCount.value = data.followingCount
    loadingBlogs.value = true
    userApi.getBlogs(login.value)
      .then(list => { blogs.value = list })
      .catch(() => {})
      .finally(() => { loadingBlogs.value = false })
  } catch {
    profile.value = null
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(login, load)

async function toggleFollow() {
  if (isSelf.value) return
  followLoading.value = true
  try {
    const status = following.value
      ? await userApi.unfollow(login.value)
      : await userApi.follow(login.value)
    following.value = status.following
    followerCount.value = status.followerCount
    followingCount.value = status.followingCount
    message.success(status.following ? t('userProfile.followSuccess') : t('userProfile.unfollowSuccess'))
  } catch (error: any) {
    message.error(error?.response?.data?.message || t('userProfile.followFailed'))
  } finally {
    followLoading.value = false
  }
}

const activityData = computed(() => [
  { label: () => t('personal.postsWritten'), value: profile.value?.postCount ?? 0, icon: FileTextOutlined, color: '#1890ff' },
  { label: () => t('personal.likesReceived'), value: profile.value?.likesReceived ?? 0, icon: HeartOutlined, color: '#eb2f96' },
  { label: () => t('personal.achievements'), value: profile.value?.achievementPoints ?? 0, icon: TrophyOutlined, color: '#faad14' },
])

const blogColumns = computed(() => [
  { title: t('blog.colTitle'), dataIndex: 'title', key: 'title', ellipsis: true },
  { title: t('blog.colCategory'), dataIndex: 'category', key: 'category', width: 120 },
  { title: t('blog.colDate'), dataIndex: 'date', key: 'date', width: 120, sorter: (a: Post, b: Post) => a.date.localeCompare(b.date) },
  { title: t('blog.colViews'), dataIndex: 'views', key: 'views', width: 90, sorter: (a: Post, b: Post) => a.views - b.views },
])
</script>

<template>
  <div class="user-profile-view">
    <a-spin :spinning="loading">
      <template v-if="profile">
        <a-row :gutter="[24, 24]">
          <!-- Left: Profile Card -->
          <a-col :xs="24" :lg="8">
            <a-card :bordered="false" class="profile-card">
              <div class="avatar-section">
                <a-avatar :size="96" :src="profile.avatar || undefined" :style="{ background: '#667eea', fontSize: '36px' }">
                  {{ (profile.name || profile.login).charAt(0).toUpperCase() }}
                </a-avatar>
                <h2>{{ profile.name || profile.login }}</h2>
                <p class="username">@{{ profile.login }}</p>
                <p class="bio" v-if="profile.bio">{{ profile.bio }}</p>
              </div>

              <a-divider />

              <div class="follow-counts">
                <div class="count-item">
                  <div class="count-value">{{ followerCount }}</div>
                  <div class="count-label">{{ t('userProfile.followers') }}</div>
                </div>
                <div class="count-item">
                  <div class="count-value">{{ followingCount }}</div>
                  <div class="count-label">{{ t('userProfile.followingCount') }}</div>
                </div>
              </div>

              <a-divider v-if="profile.joinDate" />
              <div class="profile-meta" v-if="profile.joinDate">
                <div class="meta-item">📅 {{ t('personal.joined') }} {{ profile.joinDate.slice(0, 10) }}</div>
              </div>

              <a-divider />

              <div class="action-buttons">
                <a-button
                  v-if="!isSelf"
                  :type="following ? 'default' : 'primary'"
                  block
                  :loading="followLoading"
                  @click="toggleFollow"
                >
                  <template #icon>
                    <UserDeleteOutlined v-if="following" />
                    <UserAddOutlined v-else />
                  </template>
                  {{ following ? t('userProfile.following') : t('userProfile.follow') }}
                </a-button>
                <a-button v-else block @click="router.push('/personal')">
                  <template #icon><TeamOutlined /></template>
                  {{ t('userProfile.viewMyProfile') }}
                </a-button>
              </div>
            </a-card>
          </a-col>

          <!-- Right: Activity & Blogs -->
          <a-col :xs="24" :lg="16">
            <a-row :gutter="[16, 16]" style="margin-bottom: 24px">
              <a-col :span="8" v-for="item in activityData" :key="item.label()">
                <a-card :bordered="false" class="activity-card" style="text-align: center">
                  <component :is="item.icon" :style="{ fontSize: '28px', color: item.color }" />
                  <div class="activity-value" :style="{ color: item.color }">{{ item.value }}</div>
                  <div class="activity-label">{{ item.label() }}</div>
                </a-card>
              </a-col>
            </a-row>

            <a-card :title="t('userProfile.userBlogs')" :bordered="false">
              <a-table
                :columns="blogColumns"
                :data-source="blogs"
                :loading="loadingBlogs"
                :row-key="(r: Post) => r.id"
                :pagination="{ pageSize: 8, showSizeChanger: false }"
                :scroll="{ x: 620 }"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'title'">
                    <a class="post-link" @click="router.push('/blog/' + record.id)">{{ record.title }}</a>
                  </template>
                </template>
              </a-table>
            </a-card>
          </a-col>
        </a-row>
      </template>

      <a-result
        v-else-if="!loading"
        status="404"
        :title="t('userProfile.notFound')"
        :sub-title="t('userProfile.notFoundDesc')"
      >
        <template #extra>
          <a-button type="primary" @click="router.push('/home')">{{ t('userProfile.backHome') }}</a-button>
        </template>
      </a-result>
    </a-spin>
  </div>
</template>

<style scoped>
.user-profile-view { display: flex; flex-direction: column; gap: 24px; }
.profile-card { border-radius: 12px; }
.avatar-section { text-align: center; padding-bottom: 8px; }
.avatar-section h2 { margin: 16px 0 4px; font-size: 22px; font-weight: 700; }
.username { color: #888; margin: 0 0 8px; }
.bio { color: #666; font-size: 14px; line-height: 1.6; }
.follow-counts { display: flex; justify-content: space-around; }
.count-item { text-align: center; }
.count-value { font-size: 20px; font-weight: 700; color: #333; }
.count-label { font-size: 13px; color: #888; }
.profile-meta { display: flex; flex-direction: column; gap: 10px; }
.meta-item { display: flex; align-items: center; gap: 8px; font-size: 14px; color: #555; justify-content: center; }
.activity-card { border-radius: 10px; }
.activity-value { font-size: 28px; font-weight: 700; margin: 8px 0 4px; }
.activity-label { font-size: 13px; color: #888; }
.post-link { color: #1890ff; cursor: pointer; }
.post-link:hover { text-decoration: underline; }
</style>
