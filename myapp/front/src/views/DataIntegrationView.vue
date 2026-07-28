<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, EditOutlined, DeleteOutlined, ApiOutlined, ThunderboltOutlined, CopyOutlined } from '@ant-design/icons-vue'
import { integrations, loading, fetchIntegrations, removeIntegration } from '../stores/dataIntegration'
import { dataIntegrationApi, type DataIntegrationDTO, type ExecuteResult } from '../api/dataIntegration'

const { t } = useI18n()
const router = useRouter()

onMounted(() => fetchIntegrations())

const methodColor: Record<string, string> = {
  GET: 'green', POST: 'blue', PUT: 'orange', PATCH: 'purple', DELETE: 'red',
}

const columns = computed(() => [
  { title: t('dataIntegration.name'), dataIndex: 'name', key: 'name', ellipsis: true },
  { title: t('dataIntegration.method'), dataIndex: 'method', key: 'method', width: 100 },
  { title: t('dataIntegration.baseUrl'), dataIndex: 'baseUrl', key: 'baseUrl', ellipsis: true },
  { title: t('dataIntegration.path'), dataIndex: 'path', key: 'path', ellipsis: true },
  { title: t('dataIntegration.actions'), key: 'actions', width: 190 },
])

// Execute + result modal state
const resultOpen = ref(false)
const result = ref<ExecuteResult | null>(null)
const executingId = ref<number | null>(null)

const prettyBody = computed(() => {
  const body = result.value?.body ?? ''
  if (!body) return ''
  try {
    return JSON.stringify(JSON.parse(body), null, 2)
  } catch {
    return body
  }
})

const resultHeaders = computed(() =>
  Object.entries(result.value?.headers ?? {}).map(([key, value]) => ({ key, value }))
)

async function handleExecute(record: DataIntegrationDTO) {
  executingId.value = record.id!
  try {
    result.value = await dataIntegrationApi.execute(record.id!)
    resultOpen.value = true
  } catch {
    message.error(t('dataIntegration.execFailed'))
  } finally {
    executingId.value = null
  }
}

async function copyBody() {
  if (!prettyBody.value) return
  try {
    await navigator.clipboard.writeText(prettyBody.value)
    message.success(t('dataIntegration.copied'))
  } catch {
    message.error(t('dataIntegration.copyFailed'))
  }
}

function handleDelete(record: DataIntegrationDTO) {
  Modal.confirm({
    title: t('dataIntegration.deleteConfirm'),
    content: record.name,
    okText: t('dataIntegration.delete'),
    okType: 'danger',
    async onOk() {
      try {
        await removeIntegration(record.id!)
        message.success(t('dataIntegration.deleted'))
      } catch {
        message.error(t('dataIntegration.deleteFailed'))
      }
    },
  })
}
</script>

<template>
  <div class="data-integration-view">
    <a-card :bordered="false">
      <template #title>
        <ApiOutlined style="color: #1890ff; margin-right: 8px" />{{ t('dataIntegration.title') }}
      </template>
      <template #extra>
        <a-button type="primary" @click="router.push('/data-integration/add')">
          <template #icon><PlusOutlined /></template>
          {{ t('dataIntegration.new') }}
        </a-button>
      </template>

      <a-table
        :columns="columns"
        :data-source="integrations"
        :loading="loading"
        :row-key="(r: DataIntegrationDTO) => r.id!"
        :pagination="{ pageSize: 10, showSizeChanger: false }"
        :scroll="{ x: 800 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'method'">
            <a-tag :color="methodColor[record.method] || 'default'">{{ record.method || '—' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-tooltip :title="t('dataIntegration.execute')">
                <a-button size="small" :loading="executingId === record.id" @click="handleExecute(record)">
                  <template #icon><ThunderboltOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip :title="t('dataIntegration.edit')">
                <a-button size="small" type="primary" @click="router.push('/data-integration/' + record.id + '/edit')">
                  <template #icon><EditOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip :title="t('dataIntegration.delete')">
                <a-button size="small" danger @click="handleDelete(record)">
                  <template #icon><DeleteOutlined /></template>
                </a-button>
              </a-tooltip>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Execution result -->
    <a-modal
      v-model:open="resultOpen"
      :title="t('dataIntegration.result')"
      :footer="null"
      width="720px"
    >
      <template v-if="result">
        <a-space style="margin-bottom: 12px">
          <span>{{ t('dataIntegration.status') }}:</span>
          <a-tag :color="result.success ? 'green' : 'red'">
            {{ result.status || '—' }}
          </a-tag>
          <span>{{ t('dataIntegration.duration') }}: {{ result.durationMs }} ms</span>
        </a-space>

        <a-alert
          v-if="result.error"
          type="error"
          show-icon
          :message="result.error"
          style="margin-bottom: 12px"
        />

        <template v-if="resultHeaders.length">
          <div class="section-label">{{ t('dataIntegration.respHeaders') }}</div>
          <a-descriptions bordered size="small" :column="1" style="margin-bottom: 12px">
            <a-descriptions-item v-for="h in resultHeaders" :key="h.key" :label="h.key">
              {{ h.value }}
            </a-descriptions-item>
          </a-descriptions>
        </template>

        <div class="section-label">
          {{ t('dataIntegration.respBody') }}
          <a-button size="small" type="text" :disabled="!prettyBody" @click="copyBody">
            <template #icon><CopyOutlined /></template>
            {{ t('dataIntegration.copy') }}
          </a-button>
        </div>
        <pre class="resp-body">{{ prettyBody || '—' }}</pre>
      </template>
    </a-modal>
  </div>
</template>

<style scoped>
.data-integration-view { display: flex; flex-direction: column; gap: 16px; }
.section-label { font-weight: 600; margin-bottom: 8px; }
.resp-body {
  background: #f5f5f5;
  border-radius: 6px;
  padding: 12px;
  max-height: 340px;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
