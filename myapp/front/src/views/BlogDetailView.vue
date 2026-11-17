<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  LikeOutlined,
  MessageOutlined,
  UserOutlined,
  ShareAltOutlined,
} from '@ant-design/icons-vue'
import { getPost, deletePost, type Post } from '../stores/blog'
import { blogApi } from '../api/blog'
import { getUser } from '../utils/auth'
import BlogCommentSection from '../components/BlogCommentSection.vue'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const post = ref<Post | undefined>(undefined)
const loading = ref(false)
const liking = ref(false)
const liked = ref(false)

const currentUsername = getUser<{ username: string }>()?.username
const canManage = computed(() => !!post.value && post.value.author === currentUsername)

/**
 * Only a public post can be shared. GET /api/v1/blogs/{id} does not filter on visibility, so
 * a private post's link would in fact open for any signed-in recipient — handing that link
 * out is not something to build a button for.
 */
const canShare = computed(() => !!post.value && post.value.visibility === 'PUBLIC')

const shareLink = computed(() =>
  post.value ? new URL(router.resolve('/blog/' + post.value.id).href, window.location.origin).href : '',
)

const shareOpen = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    post.value = await getPost(id)
    try {
      const likeStatus = await blogApi.getLikeStatus(id)
      liked.value = likeStatus.liked
      if (post.value) post.value.likes = likeStatus.totalLikes
    } catch {
      // A status failure should not prevent the blog itself from rendering.
    }
    blogApi.incrementView(id)
    if (post.value) post.value.views++
  } finally {
    loading.value = false
  }
})

async function handleLike() {
  if (!post.value) return
  if (liked.value) {
    message.info(t('blog.alreadyLiked'))
    return
  }
  liking.value = true
  try {
    const result = await blogApi.like(post.value.id)
    post.value.likes = result.totalLikes
    liked.value = result.liked
    message.success(t('blog.liked'))
  } catch (error: any) {
    const detail = error?.response?.data?.message
    message.error(detail || t('blog.likeFailed'))
  } finally {
    liking.value = false
  }
}

/**
 * Share the post, by whichever route the browser actually supports.
 *
 * The OS share sheet first, where there is one: on a phone that is what sharing means, and it
 * reaches WeChat and the rest, which a copied link does not. Then the clipboard. Both of those
 * APIs exist only in a secure context, so over plain HTTP on a LAN address neither is defined —
 * there the dialog shows the link to copy by hand, which is why the fallback is a selectable
 * field and not an error message.
 */
async function handleShare() {
  if (!post.value) return
  const url = shareLink.value

  if (navigator.share) {
    try {
      await navigator.share({ title: post.value.title, url })
      return
    } catch (error: any) {
      // Dismissing the sheet rejects with AbortError. That is not a failure to report.
      if (error?.name === 'AbortError') return
      // Anything else: fall through to the clipboard.
    }
  }

  if (navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(url)
      message.success(t('blog.shareCopied'))
      return
    } catch {
      // Permission refused, or no clipboard write in this context. Show the link instead.
    }
  }

  shareOpen.value = true
}

async function copyFromDialog() {
  try {
    await navigator.clipboard.writeText(shareLink.value)
    message.success(t('blog.shareCopied'))
    shareOpen.value = false
  } catch {
    message.error(t('blog.shareCopyFailed'))
  }
}

function handleDelete() {
  if (!post.value) return
  Modal.confirm({
    title: t('blog.deleteTitle'),
    content: t('blog.deleteContent', { title: post.value.title }),
    okText: t('blog.delete'),
    okType: 'danger',
    async onOk() {
      try {
        await deletePost(post.value!.id)
        message.success(t('blog.deleted'))
        router.push('/blog')
      } catch {
        message.error(t('blog.deleteFailed'))
      }
    },
  })
}
</script>

<template>
  <div class="blog-detail-view">
    <a-spin :spinning="loading">
      <template v-if="post">
        <a-page-header :title="post.title" @back="router.push('/blog')">
          <template #extra>
            <a-button v-if="canShare" @click="handleShare">
              <template #icon><ShareAltOutlined /></template>
              {{ t('blog.share') }}
            </a-button>
            <a-button v-if="canManage" @click="router.push('/blog/' + post.id + '/edit')">
              <template #icon><EditOutlined /></template>
              {{ t('blog.edit') }}
            </a-button>
            <a-button v-if="canManage" danger @click="handleDelete">
              <template #icon><DeleteOutlined /></template>
              {{ t('blog.delete') }}
            </a-button>
          </template>
          <template #tags>
            <a-tag :color="post.tagColor">{{ post.tag }}</a-tag>
            <a-tag>{{ post.category }}</a-tag>
          </template>
          <template #footer>
            <a-space size="large" class="meta-row">
              <a v-if="post.author" class="user-link" @click="router.push('/users/' + post.author)">
                <UserOutlined /> {{ post.author }}
              </a>
              <span>📅 {{ post.date }}</span>
              <span><EyeOutlined /> {{ post.views }}</span>
              <a-button
                type="text"
                class="like-button"
                :class="{ liked }"
                :loading="liking"
                @click="handleLike"
              >
                <LikeOutlined /> {{ post.likes }}
              </a-button>
              <span><MessageOutlined /> {{ post.comments }}</span>
            </a-space>
          </template>
        </a-page-header>

        <a-card :bordered="false" class="content-card">
          <div class="post-content" v-html="post.content" />
        </a-card>

        <BlogCommentSection
          :blog-id="post.id"
          @update:count="(count: number) => post!.comments = count"
        />

        <a-modal v-model:open="shareOpen" :title="t('blog.shareTitle')" :footer="null">
          <p class="share-hint">{{ t('blog.shareHint') }}</p>
          <a-input :value="shareLink" readonly @focus="(e: FocusEvent) => (e.target as HTMLInputElement).select()">
            <template #addonAfter>
              <a @click="copyFromDialog">{{ t('blog.shareCopy') }}</a>
            </template>
          </a-input>
        </a-modal>
      </template>

      <a-result v-else-if="!loading" status="404" :title="t('blog.notFound')" :sub-title="t('blog.notFoundDesc')">
        <template #extra>
          <a-button type="primary" @click="router.push('/blog')">{{ t('blog.backToList') }}</a-button>
        </template>
      </a-result>
    </a-spin>
  </div>
</template>

<style scoped>
.blog-detail-view { display: flex; flex-direction: column; gap: 16px; }
.content-card { border-radius: 10px; }
.post-content { line-height: 1.8; font-size: 15px; }
.post-content :deep(code) {
  background: #1f2937;
  color: #f8fafc;
  padding: 2px 6px;
  border: 1px solid #334155;
  border-radius: 4px;
  font-size: 0.9em;
}
.post-content :deep(pre) {
  background: #1e1e1e;
  color: #fff;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
}
.post-content :deep(pre code) {
  background: none;
  padding: 0;
  border: none;
}
.meta-row { color: #888; }
.share-hint { color: #888; margin: 0 0 12px; }
.user-link { color: #1890ff; cursor: pointer; }
.user-link:hover { text-decoration: underline; }
.like-button { padding: 0 4px; color: inherit; }
.like-button.liked { color: #1677ff; }
</style>
