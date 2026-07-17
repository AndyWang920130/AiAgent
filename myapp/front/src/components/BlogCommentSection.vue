<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { DeleteOutlined } from '@ant-design/icons-vue'
import { blogApi, type BlogComment } from '../api/blog'

const props = defineProps<{ blogId: number }>()
const emit = defineEmits<{ (e: 'update:count', count: number): void }>()

const { t } = useI18n()

const comments = ref<BlogComment[]>([])
const loading = ref(false)
const submitting = ref(false)
const newComment = ref('')

onMounted(loadComments)

async function loadComments() {
  loading.value = true
  try {
    comments.value = await blogApi.getComments(props.blogId)
    emit('update:count', comments.value.length)
  } catch {
    message.error(t('blog.comment.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  const content = newComment.value.trim()
  if (!content) return
  submitting.value = true
  try {
    const comment = await blogApi.addComment(props.blogId, content)
    comments.value.push(comment)
    newComment.value = ''
    emit('update:count', comments.value.length)
    message.success(t('blog.comment.postSuccess'))
  } catch (error: any) {
    const detail = error?.response?.data?.message
    message.error(detail || t('blog.comment.postFailed'))
  } finally {
    submitting.value = false
  }
}

function handleDelete(comment: BlogComment) {
  Modal.confirm({
    title: t('blog.comment.deleteTitle'),
    content: t('blog.comment.deleteContent'),
    okText: t('blog.delete'),
    okType: 'danger',
    async onOk() {
      try {
        await blogApi.deleteComment(comment.id)
        comments.value = comments.value.filter(c => c.id !== comment.id)
        emit('update:count', comments.value.length)
        message.success(t('blog.comment.deleteSuccess'))
      } catch {
        message.error(t('blog.comment.deleteFailed'))
      }
    },
  })
}
</script>

<template>
  <a-card :bordered="false" class="comment-section">
    <template #title>{{ t('blog.comment.title') }} ({{ comments.length }})</template>
    <a-spin :spinning="loading">
      <a-list :data-source="comments" :locale="{ emptyText: t('blog.comment.empty') }">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-comment :author="item.username" :content="item.content">
              <template #datetime>{{ item.createdDate }}</template>
              <template v-if="item.canDelete" #actions>
                <span @click="handleDelete(item)"><DeleteOutlined /> {{ t('blog.delete') }}</span>
              </template>
            </a-comment>
          </a-list-item>
        </template>
      </a-list>
    </a-spin>
    <div class="comment-form">
      <a-textarea
        v-model:value="newComment"
        :placeholder="t('blog.comment.placeholder')"
        :rows="3"
        :maxlength="2000"
      />
      <a-button
        type="primary"
        class="submit-button"
        :loading="submitting"
        :disabled="!newComment.trim()"
        @click="handleSubmit"
      >
        {{ t('blog.comment.submit') }}
      </a-button>
    </div>
  </a-card>
</template>

<style scoped>
.comment-section { border-radius: 10px; }
.comment-form { display: flex; flex-direction: column; gap: 8px; margin-top: 16px; }
.submit-button { align-self: flex-end; }
</style>
