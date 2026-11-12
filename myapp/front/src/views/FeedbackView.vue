<script lang="ts" setup>
// The submitter's side of feedback: a form that sends a message to the administrators, and
// the caller's own submissions below it with whatever reply has come back.
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import type { FeedbackCategory, FeedbackStatus } from '../api/feedback'
import {
  myFeedbacks,
  myLoading,
  myPage,
  myPageSize,
  myTotal,
  fetchMyFeedbacks,
  submitFeedback,
  withdrawFeedback,
} from '../stores/feedback'

const { t } = useI18n()

const form = reactive({
  category: 'BUG' as FeedbackCategory,
  title: '',
  content: '',
  contact: '',
})

const submitting = ref(false)

const categories: FeedbackCategory[] = ['BUG', 'FEATURE', 'CONTENT', 'OTHER']

const categoryOptions = computed(() =>
  categories.map(value => ({ value, label: t(`feedback.categories.${value}`) })),
)

const statusColor: Record<FeedbackStatus, string> = {
  NEW: 'blue',
  IN_PROGRESS: 'orange',
  RESOLVED: 'green',
}

onMounted(() => {
  fetchMyFeedbacks()
})

async function handleSubmit() {
  const title = form.title.trim()
  const content = form.content.trim()
  if (!title || !content) return message.warning(t('feedback.titleAndContentRequired'))
  submitting.value = true
  try {
    await submitFeedback({
      category: form.category,
      title,
      content,
      contact: form.contact.trim() || undefined,
    })
    form.title = ''
    form.content = ''
    form.contact = ''
    message.success(t('feedback.submitted'))
  } catch {
    message.error(t('feedback.submitFailed'))
  } finally {
    submitting.value = false
  }
}

async function handleWithdraw(id: number) {
  try {
    await withdrawFeedback(id)
    message.success(t('feedback.withdrawn'))
  } catch {
    message.error(t('feedback.withdrawFailed'))
  }
}

function onPageChange(current: number, size: number) {
  myPage.value = current
  myPageSize.value = size
  fetchMyFeedbacks()
}

function formatDate(value?: string | null) {
  if (!value) return ''
  return new Date(value).toLocaleString()
}
</script>

<template>
  <div class="feedback-view">
    <a-page-header :title="t('feedback.pageTitle')" :sub-title="t('feedback.pageSubtitle')" />

    <a-card :bordered="false" class="feedback-card">
      <a-form layout="vertical" @submit.prevent>
        <a-form-item :label="t('feedback.category')">
          <a-radio-group v-model:value="form.category" :options="categoryOptions" option-type="button" />
        </a-form-item>
        <a-form-item :label="t('feedback.title')" required>
          <a-input
            v-model:value="form.title"
            :maxlength="200"
            show-count
            :placeholder="t('feedback.titlePlaceholder')"
            allow-clear
          />
        </a-form-item>
        <a-form-item :label="t('feedback.content')" required>
          <a-textarea
            v-model:value="form.content"
            :rows="6"
            :maxlength="2000"
            show-count
            :placeholder="t('feedback.contentPlaceholder')"
          />
        </a-form-item>
        <a-form-item :label="t('feedback.contact')" :extra="t('feedback.contactHint')">
          <a-input
            v-model:value="form.contact"
            :maxlength="200"
            :placeholder="t('feedback.contactPlaceholder')"
            allow-clear
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ t('feedback.submit') }}
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card :bordered="false" class="feedback-card" :title="t('feedback.mySubmissions')">
      <a-spin :spinning="myLoading">
        <a-empty v-if="!myFeedbacks.length" :description="t('feedback.noSubmissions')" />
        <a-list v-else item-layout="vertical" :data-source="myFeedbacks">
          <template #renderItem="{ item }">
            <a-list-item :key="item.id">
              <a-list-item-meta>
                <template #title>
                  <div class="item-title">
                    <a-tag :color="statusColor[item.status as FeedbackStatus]">
                      {{ t(`feedback.statuses.${item.status}`) }}
                    </a-tag>
                    <a-tag>{{ t(`feedback.categories.${item.category}`) }}</a-tag>
                    <span class="title-text">{{ item.title }}</span>
                  </div>
                </template>
                <template #description>
                  <span class="submitted-at">{{ formatDate(item.createdDate) }}</span>
                </template>
              </a-list-item-meta>
              <div class="item-content">{{ item.content }}</div>
              <div v-if="item.reply" class="item-reply">
                <div class="reply-head">
                  {{ t('feedback.replyFrom', { name: item.repliedBy || t('feedback.administrator') }) }}
                  <span class="submitted-at">{{ formatDate(item.repliedDate) }}</span>
                </div>
                <div>{{ item.reply }}</div>
              </div>
              <template #actions>
                <a-popconfirm
                  v-if="item.status !== 'RESOLVED'"
                  :title="t('feedback.withdrawConfirm')"
                  @confirm="handleWithdraw(item.id)"
                >
                  <a-button danger type="link" size="small">{{ t('feedback.withdraw') }}</a-button>
                </a-popconfirm>
                <span v-else class="awaiting">{{ t('feedback.answered') }}</span>
              </template>
            </a-list-item>
          </template>
        </a-list>
        <a-pagination
          v-if="myTotal > myPageSize"
          class="pager"
          :current="myPage"
          :page-size="myPageSize"
          :total="myTotal"
          :show-size-changer="false"
          @change="onPageChange"
        />
      </a-spin>
    </a-card>
  </div>
</template>

<style scoped>
.feedback-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feedback-card {
  border-radius: 8px;
}

.item-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.title-text {
  font-weight: 600;
}

.submitted-at {
  color: var(--ant-color-text-tertiary, rgba(0, 0, 0, 0.45));
  font-size: 12px;
}

.item-content {
  white-space: pre-wrap;
  word-break: break-word;
}

.item-reply {
  margin-top: 12px;
  padding: 12px;
  border-radius: 6px;
  background: rgba(22, 119, 255, 0.06);
  white-space: pre-wrap;
  word-break: break-word;
}

.reply-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-weight: 600;
}

.awaiting {
  color: var(--ant-color-text-tertiary, rgba(0, 0, 0, 0.45));
  font-size: 12px;
}

.pager {
  margin-top: 16px;
  text-align: right;
}
</style>
