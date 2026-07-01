<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  LikeOutlined,
  MessageOutlined,
} from '@ant-design/icons-vue'
import { getPost, deletePost, type Post } from '../stores/blog'
import { blogApi } from '../api/blog'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const post = ref<Post | undefined>(undefined)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    post.value = await getPost(id)
    blogApi.incrementView(id)
    if (post.value) post.value.views++
  } finally {
    loading.value = false
  }
})

function handleDelete() {
  if (!post.value) return
  Modal.confirm({
    title: t('blog.deleteTitle'),
    content: t('blog.deleteContent', { title: post.value.title }),
    okText: t('blog.delete'),
    okType: 'danger',
    async onOk() {
      await deletePost(post.value!.id)
      message.success(t('blog.deleted'))
      router.push('/blog')
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
            <a-button @click="router.push('/blog/' + post.id + '/edit')">
              <template #icon><EditOutlined /></template>
              {{ t('blog.edit') }}
            </a-button>
            <a-button danger @click="handleDelete">
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
              <span>📅 {{ post.date }}</span>
              <span><EyeOutlined /> {{ post.views }}</span>
              <span><LikeOutlined /> {{ post.likes }}</span>
              <span><MessageOutlined /> {{ post.comments }}</span>
            </a-space>
          </template>
        </a-page-header>

        <a-card :bordered="false" class="content-card">
          <div class="post-content" v-html="post.content" />
        </a-card>
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
.meta-row { color: #888; }
</style>
