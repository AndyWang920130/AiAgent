<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons-vue'
import { dataIntegrationApi, type KeyValue } from '../api/dataIntegration'
import { parseRows, serializeRows } from '../stores/dataIntegration'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const editId = computed(() => {
  const id = route.params.id
  return id ? Number(id) : null
})
const isEdit = computed(() => editId.value !== null)

const methods = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE']

const form = reactive({
  name: '',
  description: '',
  baseUrl: '',
  path: '',
  method: 'GET',
  bodyType: 'KV',
  authSourceId: null as number | null,
  authTokenPath: '',
  authHeaderName: '',
  authHeaderTemplate: '',
  authBodyProperty: '',
})

const headers = ref<KeyValue[]>([])
const queryParams = ref<KeyValue[]>([])
const bodyConfig = ref<KeyValue[]>([])
const bodyRaw = ref('')
const responseConfig = ref<KeyValue[]>([])

// Other saved integrations that can serve as the auth source (self excluded when editing).
const authSourceOptions = ref<{ id: number; name: string }[]>([])

const submitting = ref(false)
const loading = ref(false)

function addRow(rows: KeyValue[]) {
  rows.push({ key: '', value: '' })
}
function removeRow(rows: KeyValue[], index: number) {
  rows.splice(index, 1)
}

onMounted(async () => {
  // Load selectable auth sources (exclude self when editing).
  try {
    const all = await dataIntegrationApi.list()
    authSourceOptions.value = all
      .filter(i => i.id != null && i.id !== editId.value)
      .map(i => ({ id: i.id!, name: i.name }))
  } catch {
    // non-fatal — the select just stays empty
  }

  if (isEdit.value) {
    loading.value = true
    try {
      const dto = await dataIntegrationApi.get(editId.value!)
      form.name = dto.name
      form.description = dto.description || ''
      form.baseUrl = dto.baseUrl
      form.path = dto.path || ''
      form.method = dto.method || 'GET'
      form.bodyType = dto.bodyType === 'RAW' ? 'RAW' : 'KV'
      form.authSourceId = dto.authSourceId ?? null
      form.authTokenPath = dto.authTokenPath || ''
      form.authHeaderName = dto.authHeaderName || ''
      form.authHeaderTemplate = dto.authHeaderTemplate || ''
      form.authBodyProperty = dto.authBodyProperty || ''
      headers.value = parseRows(dto.headers)
      queryParams.value = parseRows(dto.queryParams)
      bodyConfig.value = parseRows(dto.bodyConfig)
      bodyRaw.value = dto.bodyRaw || ''
      responseConfig.value = parseRows(dto.responseConfig)
    } catch {
      message.error(t('dataIntegration.loadFailed'))
      router.push('/data-integration')
    } finally {
      loading.value = false
    }
  }
})

async function handleSubmit() {
  if (!form.name.trim() || !form.baseUrl.trim()) {
    message.warning(t('dataIntegration.fillRequired'))
    return
  }
  // Validate raw JSON body up front so a malformed payload is caught before saving.
  if (form.bodyType === 'RAW' && bodyRaw.value.trim()) {
    try {
      JSON.parse(bodyRaw.value)
    } catch {
      message.error(t('dataIntegration.bodyRawInvalid'))
      return
    }
  }
  submitting.value = true
  const payload = {
    name: form.name.trim(),
    description: form.description,
    baseUrl: form.baseUrl.trim(),
    path: form.path,
    method: form.method,
    headers: serializeRows(headers.value),
    queryParams: serializeRows(queryParams.value),
    bodyConfig: serializeRows(bodyConfig.value),
    bodyType: form.bodyType,
    bodyRaw: bodyRaw.value,
    responseConfig: serializeRows(responseConfig.value),
    authSourceId: form.authSourceId,
    authTokenPath: form.authTokenPath,
    authHeaderName: form.authHeaderName,
    authHeaderTemplate: form.authHeaderTemplate,
    authBodyProperty: form.authBodyProperty,
  }
  try {
    if (isEdit.value) {
      await dataIntegrationApi.update(editId.value!, payload)
      message.success(t('dataIntegration.saved'))
    } else {
      await dataIntegrationApi.create(payload)
      message.success(t('dataIntegration.created'))
    }
    router.push('/data-integration')
  } catch (err: any) {
    message.error(err?.response?.data?.message || t('dataIntegration.saveFailed'))
  } finally {
    submitting.value = false
  }
}

// Key-value sections rendered around the body block (which has its own KV/raw toggle).
const sectionsBefore = computed(() => [
  { label: t('dataIntegration.headers'), rows: headers.value },
  { label: t('dataIntegration.queryParams'), rows: queryParams.value },
])
const sectionsAfter = computed(() => [
  { label: t('dataIntegration.responseConfig'), rows: responseConfig.value },
])
</script>

<template>
  <div class="di-edit-view">
    <a-card :bordered="false" :title="isEdit ? t('dataIntegration.editTitle') : t('dataIntegration.addTitle')">
      <a-spin :spinning="loading">
        <a-form layout="vertical" :model="form" @finish="handleSubmit">
          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item :label="t('dataIntegration.name')" required>
                <a-input v-model:value="form.name" :placeholder="t('dataIntegration.namePlaceholder')" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item :label="t('dataIntegration.method')">
                <a-select v-model:value="form.method">
                  <a-select-option v-for="m in methods" :key="m" :value="m">{{ m }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item :label="t('dataIntegration.description')">
            <a-input v-model:value="form.description" :placeholder="t('dataIntegration.descriptionPlaceholder')" />
          </a-form-item>

          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item :label="t('dataIntegration.baseUrl')" required>
                <a-input v-model:value="form.baseUrl" placeholder="https://api.example.com" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item :label="t('dataIntegration.path')">
                <a-input v-model:value="form.path" placeholder="/v1/resource" />
              </a-form-item>
            </a-col>
          </a-row>

          <!-- Key-value editors: headers / query params -->
          <a-form-item v-for="section in sectionsBefore" :key="section.label" :label="section.label">
            <div v-for="(row, idx) in section.rows" :key="idx" class="kv-row">
              <a-input v-model:value="row.key" :placeholder="t('dataIntegration.keyPlaceholder')" class="kv-key" />
              <a-input v-model:value="row.value" :placeholder="t('dataIntegration.valuePlaceholder')" class="kv-value" />
              <a-button type="text" danger @click="removeRow(section.rows, idx)">
                <template #icon><MinusCircleOutlined /></template>
              </a-button>
            </div>
            <a-button type="dashed" block @click="addRow(section.rows)">
              <template #icon><PlusOutlined /></template>
              {{ t('dataIntegration.addRow') }}
            </a-button>
          </a-form-item>

          <!-- Request body: key-value pairs or a raw JSON payload -->
          <a-form-item :label="t('dataIntegration.bodyConfig')">
            <a-radio-group v-model:value="form.bodyType" button-style="solid" size="small" style="margin-bottom: 12px">
              <a-radio-button value="KV">{{ t('dataIntegration.bodyModeKv') }}</a-radio-button>
              <a-radio-button value="RAW">{{ t('dataIntegration.bodyModeRaw') }}</a-radio-button>
            </a-radio-group>

            <template v-if="form.bodyType === 'RAW'">
              <a-textarea
                v-model:value="bodyRaw"
                :rows="10"
                :placeholder="t('dataIntegration.bodyRawPlaceholder')"
                class="body-raw"
              />
              <div class="body-raw-hint">{{ t('dataIntegration.bodyRawHelp') }}</div>
            </template>
            <template v-else>
              <div v-for="(row, idx) in bodyConfig" :key="idx" class="kv-row">
                <a-input v-model:value="row.key" :placeholder="t('dataIntegration.keyPlaceholder')" class="kv-key" />
                <a-input v-model:value="row.value" :placeholder="t('dataIntegration.valuePlaceholder')" class="kv-value" />
                <a-button type="text" danger @click="removeRow(bodyConfig, idx)">
                  <template #icon><MinusCircleOutlined /></template>
                </a-button>
              </div>
              <a-button type="dashed" block @click="addRow(bodyConfig)">
                <template #icon><PlusOutlined /></template>
                {{ t('dataIntegration.addRow') }}
              </a-button>
            </template>
          </a-form-item>

          <!-- Key-value editors: response data config -->
          <a-form-item v-for="section in sectionsAfter" :key="section.label" :label="section.label">
            <div v-for="(row, idx) in section.rows" :key="idx" class="kv-row">
              <a-input v-model:value="row.key" :placeholder="t('dataIntegration.keyPlaceholder')" class="kv-key" />
              <a-input v-model:value="row.value" :placeholder="t('dataIntegration.valuePlaceholder')" class="kv-value" />
              <a-button type="text" danger @click="removeRow(section.rows, idx)">
                <template #icon><MinusCircleOutlined /></template>
              </a-button>
            </div>
            <a-button type="dashed" block @click="addRow(section.rows)">
              <template #icon><PlusOutlined /></template>
              {{ t('dataIntegration.addRow') }}
            </a-button>
          </a-form-item>

          <!-- Authentication chaining (optional) -->
          <a-divider orientation="left">{{ t('dataIntegration.authSection') }}</a-divider>
          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item :label="t('dataIntegration.authSource')">
                <a-select
                  v-model:value="form.authSourceId"
                  allow-clear
                  :placeholder="t('dataIntegration.authSourceNone')"
                >
                  <a-select-option v-for="opt in authSourceOptions" :key="opt.id" :value="opt.id">
                    {{ opt.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item :label="t('dataIntegration.authTokenPath')" :help="t('dataIntegration.authTokenPathHelp')">
                <a-input v-model:value="form.authTokenPath" placeholder="data.token" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item :label="t('dataIntegration.authHeaderName')">
                <a-input v-model:value="form.authHeaderName" placeholder="Authorization" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item :label="t('dataIntegration.authHeaderTemplate')" :help="t('dataIntegration.authHeaderTemplateHelp')">
                <a-input v-model:value="form.authHeaderTemplate" placeholder="Bearer {{token}}" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item :label="t('dataIntegration.authBodyProperty')" :help="t('dataIntegration.authBodyPropertyHelp')">
                <a-input v-model:value="form.authBodyProperty" placeholder="data.token" />
              </a-form-item>
            </a-col>
          </a-row>

          <div style="display: flex; gap: 8px; margin-top: 8px">
            <a-button type="primary" html-type="submit" :loading="submitting">
              {{ t('dataIntegration.save') }}
            </a-button>
            <a-button @click="router.push('/data-integration')">{{ t('dataIntegration.cancel') }}</a-button>
          </div>
        </a-form>
      </a-spin>
    </a-card>
  </div>
</template>

<style scoped>
.di-edit-view { display: flex; flex-direction: column; gap: 16px; }
.kv-row { display: flex; gap: 8px; margin-bottom: 8px; align-items: center; }
.kv-key { flex: 1; }
.kv-value { flex: 2; }
.body-raw { font-family: 'Consolas', 'Monaco', monospace; font-size: 13px; }
.body-raw-hint { margin-top: 6px; color: rgba(0, 0, 0, 0.45); font-size: 12px; }
</style>
