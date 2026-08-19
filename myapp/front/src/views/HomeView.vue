<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  posts, loading, fetchPosts,
  followingPosts, loadingFollowing, fetchFollowingPosts,
} from '../stores/blog'
import {
  EyeOutlined,
  LikeOutlined,
  MessageOutlined,
  EditOutlined,
} from '@ant-design/icons-vue'

const { t } = useI18n()
const router = useRouter()

const feedTab = ref<'recommended' | 'following'>('recommended')

onMounted(() => {
  fetchPosts()
  fetchFollowingPosts()
})

// "Recommended" ranks public posts by engagement (likes, then views) and shows the top slice.
const recommendedPosts = computed(() =>
  [...posts.value]
    .sort((a, b) => (b.likes - a.likes) || (b.views - a.views))
    .slice(0, 6),
)

const followingList = computed(() => followingPosts.value.slice(0, 6))

const displayedPosts = computed(() =>
  feedTab.value === 'following' ? followingList.value : recommendedPosts.value,
)

const listLoading = computed(() =>
  feedTab.value === 'following' ? loadingFollowing.value : loading.value,
)
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

    <!-- Posts -->
    <a-card :bordered="false" class="feed-card">
      <template #title>
        <a-tabs v-model:activeKey="feedTab" size="small" class="feed-tabs">
          <a-tab-pane key="recommended" :tab="t('home.recommended')" />
          <a-tab-pane key="following" :tab="t('home.following')" />
        </a-tabs>
      </template>
      <template #extra>
        <a-button type="primary" size="small" class="write-btn" @click="router.push('/blog/add')">
          <template #icon><EditOutlined /></template>
          <span class="write-label">{{ t('home.writePost') }}</span>
        </a-button>
      </template>

      <a-list :data-source="displayedPosts" :loading="listLoading" item-layout="vertical">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <div class="post-title-row">
                  <span class="post-title" @click="router.push('/blog/' + item.id)">{{ item.title }}</span>
                  <a-tag :color="item.tagColor">{{ item.tag }}</a-tag>
                </div>
              </template>
              <template #description>{{ item.date }} · {{ item.category }} · {{ item.author }}</template>
            </a-list-item-meta>
            <p class="post-excerpt">{{ item.excerpt }}</p>
            <template #actions>
              <span><EyeOutlined /> {{ item.views }}</span>
              <span><LikeOutlined /> {{ item.likes }}</span>
              <span><MessageOutlined /> {{ item.comments }}</span>
            </template>
          </a-list-item>
        </template>
        <template #empty>
          <a-empty
            :description="feedTab === 'following' ? t('home.noFollowingPosts') : t('home.noPosts')"
          />
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

.post-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.post-title { font-size: 16px; font-weight: 600; cursor: pointer; }
.post-title:hover { color: #1890ff; }
.post-excerpt { color: #888; margin: 4px 0 0; line-height: 1.6; }

/* The feed tabs live in the card title, whose default `overflow: hidden` clips the top of
   the tab labels. Let the title overflow show and drop the tab-bar's own bottom margin. */
.feed-card :deep(.ant-card-head-title) { overflow: visible; min-width: 0; }
.feed-tabs :deep(.ant-tabs-nav) { margin: 0; }

/* Keep the tabs and the Write Post button on one stable row: the title may shrink, the
   button never does (prevents the English label squeezing / overlapping the tabs). */
.feed-card :deep(.ant-card-head-wrapper) { align-items: center; gap: 8px; }
.feed-card :deep(.ant-card-extra) { flex: 0 0 auto; }
.write-btn { white-space: nowrap; }

/* On phones, collapse the button to its icon so the tabs get the full width. */
@media (max-width: 575px) {
  .write-label { display: none; }
}
</style>
