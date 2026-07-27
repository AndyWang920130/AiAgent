<script lang="ts" setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { NumberOutlined, CopyOutlined } from '@ant-design/icons-vue'

const { t } = useI18n()

const input = ref('')
const algorithm = ref<'SHA-1' | 'SHA-256' | 'SHA-384' | 'SHA-512'>('SHA-256')
const digest = ref('')
const busy = ref(false)

const algorithms = ['SHA-1', 'SHA-256', 'SHA-384', 'SHA-512'] as const

const enc = new TextEncoder()

function bytesToHex(bytes: Uint8Array): string {
  let hex = ''
  for (let i = 0; i < bytes.length; i++) {
    hex += bytes[i].toString(16).padStart(2, '0')
  }
  return hex
}

async function hash() {
  if (!input.value) {
    message.warning(t('sha.needInput'))
    return
  }
  busy.value = true
  try {
    const buf = await crypto.subtle.digest(algorithm.value, enc.encode(input.value))
    digest.value = bytesToHex(new Uint8Array(buf))
  } catch {
    message.error(t('sha.hashFailed'))
  } finally {
    busy.value = false
  }
}

async function copyDigest() {
  if (!digest.value) return
  try {
    await navigator.clipboard.writeText(digest.value)
    message.success(t('sha.copied'))
  } catch {
    message.error(t('sha.copyFailed'))
  }
}
</script>

<template>
  <div class="sha-view">
    <a-card :bordered="false">
      <template #title>
        <NumberOutlined style="color: #722ed1; margin-right: 8px" />{{ t('sha.title') }}
      </template>

      <a-alert :message="t('sha.hint')" type="info" show-icon style="margin-bottom: 16px" />

      <a-form layout="vertical">
        <a-form-item :label="t('sha.input')">
          <a-textarea
            v-model:value="input"
            :rows="5"
            :placeholder="t('sha.inputPlaceholder')"
            allow-clear
          />
        </a-form-item>

        <a-form-item :label="t('sha.algorithm')">
          <a-radio-group v-model:value="algorithm" button-style="solid">
            <a-radio-button v-for="algo in algorithms" :key="algo" :value="algo">{{ algo }}</a-radio-button>
          </a-radio-group>
        </a-form-item>

        <a-space wrap style="margin-bottom: 16px">
          <a-button type="primary" :loading="busy" @click="hash">
            <template #icon><NumberOutlined /></template>
            {{ t('sha.hash') }}
          </a-button>
        </a-space>

        <a-form-item :label="t('sha.digest')">
          <a-textarea :value="digest" :rows="3" readonly :placeholder="t('sha.digestPlaceholder')" class="digest-box" />
          <a-button size="small" style="margin-top: 8px" :disabled="!digest" @click="copyDigest">
            <template #icon><CopyOutlined /></template>
            {{ t('sha.copy') }}
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<style scoped>
.sha-view { display: flex; flex-direction: column; gap: 16px; }
.digest-box :deep(textarea) { font-family: monospace; word-break: break-all; }
</style>
