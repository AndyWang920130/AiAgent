<script lang="ts" setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { CodeOutlined, ThunderboltOutlined, CopyOutlined } from '@ant-design/icons-vue'
import { jsonToJava } from '../utils/jsonToJava'

const { t } = useI18n()

const input = ref('')
const rootClassName = ref('Root')
const packageName = ref('')
const output = ref('')
const busy = ref(false)

const sample = `{
  "deviceId": "D-001",
  "status": 1,
  "active": true,
  "sensors": [
    { "type": "ecg", "value": 72.5 }
  ]
}`

function loadSample() {
  input.value = sample
}

function generate() {
  if (!input.value.trim()) {
    message.warning(t('jsonToEntity.needInput'))
    return
  }
  busy.value = true
  try {
    const parsed = JSON.parse(input.value)
    output.value = jsonToJava(parsed, {
      rootClassName: rootClassName.value.trim() || 'Root',
      packageName: packageName.value.trim(),
    })
  } catch (err: any) {
    output.value = ''
    message.error(err?.message ? `${t('jsonToEntity.invalidJson')}: ${err.message}` : t('jsonToEntity.invalidJson'))
  } finally {
    busy.value = false
  }
}

async function copyOutput() {
  if (!output.value) return
  try {
    await navigator.clipboard.writeText(output.value)
    message.success(t('jsonToEntity.copied'))
  } catch {
    message.error(t('jsonToEntity.copyFailed'))
  }
}
</script>

<template>
  <div class="j2e-view">
    <a-card :bordered="false">
      <template #title>
        <CodeOutlined style="color: #13c2c2; margin-right: 8px" />{{ t('jsonToEntity.title') }}
      </template>

      <a-alert :message="t('jsonToEntity.hint')" type="info" show-icon style="margin-bottom: 16px" />

      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item :label="t('jsonToEntity.rootClassName')">
              <a-input v-model:value="rootClassName" placeholder="Root" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item :label="t('jsonToEntity.packageName')">
              <a-input v-model:value="packageName" placeholder="com.example.myapp.domain" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item :label="t('jsonToEntity.input')">
          <a-textarea
            v-model:value="input"
            :rows="12"
            :placeholder="t('jsonToEntity.inputPlaceholder')"
            allow-clear
            class="code-box"
          />
        </a-form-item>

        <a-space wrap style="margin-bottom: 16px">
          <a-button type="primary" :loading="busy" @click="generate">
            <template #icon><ThunderboltOutlined /></template>
            {{ t('jsonToEntity.generate') }}
          </a-button>
          <a-button @click="loadSample">{{ t('jsonToEntity.loadSample') }}</a-button>
        </a-space>

        <a-form-item :label="t('jsonToEntity.output')">
          <a-textarea :value="output" :rows="16" readonly :placeholder="t('jsonToEntity.outputPlaceholder')" class="code-box" />
          <a-button size="small" style="margin-top: 8px" :disabled="!output" @click="copyOutput">
            <template #icon><CopyOutlined /></template>
            {{ t('jsonToEntity.copy') }}
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<style scoped>
.j2e-view { display: flex; flex-direction: column; gap: 16px; }
.code-box :deep(textarea) {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  white-space: pre;
}
</style>
