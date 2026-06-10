<script lang="ts" setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  LikeOutlined,
  MessageOutlined,
} from '@ant-design/icons-vue'
import { getPost, deletePost } from '../stores/blog'

const router = useRouter()
const route = useRoute()

const post = computed(() => getPost(Number(route.params.id)))

function handleDelete() {
  if (!post.value) return
  Modal.confirm({
    title: 'Delete Post',
    content: `Are you sure you want to delete "${post.value.title}"?`,
    okText: 'Delete',
    okType: 'danger',
    onOk() {
      deletePost(post.value!.id)
      message.success('Post deleted')
      router.push('/blog')
    },
  })
}
</script>

<template>
  <div class="blog-detail-view">
    <template v-if="post">
      <a-page-header :title="post.title" @back="router.push('/blog')">
        <template #extra>
          <a-button @click="router.push('/blog/' + post.id + '/edit')">
            <template #icon><EditOutlined /></template>
            Edit
          </a-button>
          <a-button danger @click="handleDelete">
            <template #icon><DeleteOutlined /></template>
            Delete
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

    <a-result v-else status="404" title="Post not found" sub-title="The post you are looking for does not exist.">
      <template #extra>
        <a-button type="primary" @click="router.push('/blog')">Back to List</a-button>
      </template>
    </a-result>
  </div>
</template>

<style scoped>
.blog-detail-view { display: flex; flex-direction: column; gap: 16px; }
.content-card { border-radius: 10px; }
.post-content { line-height: 1.8; font-size: 15px; }
.meta-row { color: #888; }
</style>
