<script lang="ts" setup>
// The administrator's side of feedback: a filterable list of every submission, with a drawer
// for reading one in full, answering it and moving its status.
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import type { FeedbackCategory, FeedbackDTO, FeedbackStatus } from '../api/feedback'
import {
  feedbacks,
  loading,
  openCount,
  page,
  pageSize,
  total,
  query,
  statusFilter,
  categoryFilter,
  applyFilters,
  fetchFeedbacks,
  fetchOpenCount,
  removeFeedback,
  replyToFeedback,
} from '../stores/feedback'

const { t } = useI18n()

const statuses: FeedbackStatus[] = ['NEW', 'IN_PROGRESS', 'RESOLVED']
const categories: FeedbackCategory[] = ['BUG', 'FEATURE', 'CONTENT', 'OTHER']

const statusColor: Record<FeedbackStatus, string> = {
  NEW: 'blue',
  IN_PROGRESS: 'orange',
  RESOLVED: 'green',
}

const statusOptions = computed(() =>
  statuses.map(value => ({ value, label: t(`feedback.statuses.${value}`) })),
)

const categoryOptions = computed(() =>
  categories.map(value => ({ value, label: t(`feedback.categories.${value}`) })),
)

const columns = computed(() => [
  { title: t('feedback.admin.status'), key: 'status', width: 130 },
  { title: t('feedback.admin.category'), key: 'category', width: 120 },
  { title: t('feedback.admin.title'), key: 'title' },
  { title: t('feedback.admin.submitter'), key: 'submitter', width: 160 },
  { title: t('feedback.admin.submittedAt'), key: 'createdDate', width: 180 },
  { title: t('feedback.admin.actions'), key: 'actions', width: 150 },
])

const drawerOpen = ref(false)
const current = ref<FeedbackDTO | null>(null)
const replyForm = reactive({ reply: '', status: 'RESOLVED' as FeedbackStatus })
const saving = ref(false)

onMounted(() => {
  fetchFeedbacks()
  fetchOpenCount()
})

function openDrawer(record: FeedbackDTO) {
  current.value = record
  // Pre-fill with whatever is already on the row so a second visit edits rather than clears.
  replyForm.reply = record.reply || ''
  replyForm.status = record.status === 'NEW' ? 'RESOLVED' : record.status
  drawerOpen.value = true
}

async function handleReply() {
  const target = current.value
  if (!target) return
  const reply = replyForm.reply.trim()
  if (!reply && replyForm.status === target.status) {
    return message.warning(t('feedback.admin.nothingToApply'))
  }
  saving.value = true
  try {
    const updated = await replyToFeedback(target.id, {
      reply: reply || undefined,
      status: replyForm.status,
    })
    current.value = updated
    message.success(t('feedback.admin.replied'))
    drawerOpen.value = false
  } catch {
    message.error(t('feedback.admin.replyFailed'))
  } finally {
    saving.value = false
  }
}

/** Move a row's status straight from the table, without writing a reply. */
async function setStatus(record: FeedbackDTO, status: FeedbackStatus) {
  if (status === record.status) return
  try {
    await replyToFeedback(record.id, { status })
    message.success(t('feedback.admin.replied'))
  } catch {
    message.error(t('feedback.admin.replyFailed'))
  }
}

async function handleDelete(id: number) {
  try {
    await removeFeedback(id)
    message.success(t('feedback.admin.deleted'))
  } catch {
    message.error(t('feedback.admin.deleteFailed'))
  }
}

function onTableChange(pagination: { current?: number; pageSize?: number }) {
  page.value = pagination.current || 1
  pageSize.value = pagination.pageSize || 20
  fetchFeedbacks()
}

function resetFilters() {
  query.value = ''
  statusFilter.value = undefined
  categoryFilter.value = undefined
  applyFilters()
}

function formatDate(value?: string | null) {
  if (!value) return ''
  return new Date(value).toLocaleString()
}
</script>

<template>
  <div class="feedback-admin-view">
    <a-page-header :title="t('feedback.admin.pageTitle')" :sub-title="t('feedback.admin.pageSubtitle')">
      <template #extra>
        <a-tag :color="openCount ? 'orange' : 'green'">
          {{ t('feedback.admin.openCount', { count: openCount }) }}
        </a-tag>
      </template>
    </a-page-header>

    <a-card :bordered="false" class="list-card">
      <a-form layout="inline" class="filter-form" @submit.prevent>
        <a-form-item>
          <a-input
            v-model:value="query"
            :placeholder="t('feedback.admin.searchPlaceholder')"
            allow-clear
            @press-enter="applyFilters"
          />
        </a-form-item>
        <a-form-item>
          <a-select
            v-model:value="statusFilter"
            :options="statusOptions"
            :placeholder="t('feedback.admin.anyStatus')"
            style="width: 160px"
            allow-clear
            @change="applyFilters"
          />
        </a-form-item>
        <a-form-item>
          <a-select
            v-model:value="categoryFilter"
            :options="categoryOptions"
            :placeholder="t('feedback.admin.anyCategory')"
            style="width: 160px"
            allow-clear
            @change="applyFilters"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="applyFilters">{{ t('feedback.admin.search') }}</a-button>
        </a-form-item>
        <a-form-item>
          <a-button @click="resetFilters">{{ t('feedback.admin.reset') }}</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="feedbacks"
        :loading="loading"
        :pagination="{ current: page, pageSize, total, showSizeChanger: true }"
        row-key="id"
        size="middle"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor[record.status as FeedbackStatus]">
              {{ t(`feedback.statuses.${record.status}`) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'category'">
            {{ t(`feedback.categories.${record.category}`) }}
          </template>
          <template v-else-if="column.key === 'title'">
            <a @click="openDrawer(record as FeedbackDTO)">{{ record.title }}</a>
            <div v-if="record.reply" class="replied-hint">{{ t('feedback.admin.hasReply') }}</div>
          </template>
          <template v-else-if="column.key === 'submitter'">
            {{ record.name }}
          </template>
          <template v-else-if="column.key === 'createdDate'">
            <span class="timestamp">{{ formatDate(record.createdDate) }}</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="openDrawer(record as FeedbackDTO)">
              {{ t('feedback.admin.open') }}
            </a-button>
            <a-popconfirm :title="t('feedback.admin.deleteConfirm')" @confirm="handleDelete(record.id)">
              <a-button danger type="link" size="small">{{ t('feedback.admin.delete') }}</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer
      v-model:open="drawerOpen"
      :title="current?.title"
      :width="520"
      placement="right"
    >
      <template v-if="current">
        <a-descriptions :column="1" size="small" bordered>
          <a-descriptions-item :label="t('feedback.admin.submitter')">
            {{ current.name }} ({{ current.username }})
          </a-descriptions-item>
          <a-descriptions-item :label="t('feedback.admin.category')">
            {{ t(`feedback.categories.${current.category}`) }}
          </a-descriptions-item>
          <a-descriptions-item :label="t('feedback.admin.status')">
            <a-tag :color="statusColor[current.status]">
              {{ t(`feedback.statuses.${current.status}`) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="t('feedback.admin.submittedAt')">
            {{ formatDate(current.createdDate) }}
          </a-descriptions-item>
          <a-descriptions-item v-if="current.contact" :label="t('feedback.contact')">
            {{ current.contact }}
          </a-descriptions-item>
        </a-descriptions>

        <div class="drawer-section">
          <div class="section-label">{{ t('feedback.content') }}</div>
          <div class="message-body">{{ current.content }}</div>
        </div>

        <div v-if="current.reply" class="drawer-section">
          <div class="section-label">
            {{ t('feedback.admin.existingReply', { name: current.repliedBy || '' }) }}
            <span class="timestamp">{{ formatDate(current.repliedDate) }}</span>
          </div>
          <div class="message-body">{{ current.reply }}</div>
        </div>

        <div class="drawer-section">
          <div class="section-label">{{ t('feedback.admin.reply') }}</div>
          <a-textarea
            v-model:value="replyForm.reply"
            :rows="6"
            :maxlength="2000"
            show-count
            :placeholder="t('feedback.admin.replyPlaceholder')"
          />
          <div class="status-row">
            <span class="section-label">{{ t('feedback.admin.setStatus') }}</span>
            <a-radio-group v-model:value="replyForm.status" :options="statusOptions" option-type="button" size="small" />
          </div>
        </div>
      </template>

      <template #footer>
        <div class="drawer-footer">
          <a-button @click="drawerOpen = false">{{ t('feedback.admin.cancel') }}</a-button>
          <a-button
            v-if="current && current.status !== 'IN_PROGRESS'"
            @click="setStatus(current, 'IN_PROGRESS')"
          >
            {{ t('feedback.admin.markInProgress') }}
          </a-button>
          <a-button type="primary" :loading="saving" @click="handleReply">
            {{ t('feedback.admin.send') }}
          </a-button>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<style scoped>
.feedback-admin-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.list-card {
  border-radius: 8px;
}

.filter-form {
  margin-bottom: 16px;
  row-gap: 12px;
}

.timestamp,
.replied-hint {
  color: var(--ant-color-text-tertiary, rgba(0, 0, 0, 0.45));
  font-size: 12px;
}

.drawer-section {
  margin-top: 20px;
}

.section-label {
  margin-bottom: 8px;
  font-weight: 600;
}

.message-body {
  white-space: pre-wrap;
  word-break: break-word;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.status-row .section-label {
  margin-bottom: 0;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
