<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { LockOutlined, UnlockOutlined, CopyOutlined, SwapOutlined } from '@ant-design/icons-vue'

const { t } = useI18n()

// ---- Options (each with a default) ----
type Algo = 'AES-GCM' | 'AES-CBC'

const algorithm = ref<Algo>('AES-GCM')      // default AES-GCM
const keySize = ref<128 | 192 | 256>(256)   // default 256-bit

const input = ref('')
const password = ref('')
const saltInput = ref('')                     // optional fixed salt (Base64), blank = random
const ivInput = ref('')                       // optional fixed IV (Base64), blank = random
const output = ref('')
const busy = ref(false)

const DEFAULT_PASSWORD = 'twsny'
const effectivePassword = () => password.value || DEFAULT_PASSWORD

// Web Crypto (crypto.subtle) is only exposed in a secure context: HTTPS, or
// localhost/127.0.0.1. Over plain http:// on a deployed server it is undefined,
// so encryption/decryption cannot work — detect that up front and explain it
// rather than throwing a generic error.
const cryptoAvailable = typeof crypto !== 'undefined' && !!crypto.subtle

// IV length depends on the mode: GCM = 12 bytes, CBC = 16 bytes.
const ivLength = computed(() => (algorithm.value === 'AES-GCM' ? 12 : 16))
const SALT_LENGTH = 16

const enc = new TextEncoder()
const dec = new TextDecoder()

// ---- Base64 <-> bytes ----
function bytesToBase64(bytes: Uint8Array): string {
  let bin = ''
  for (let i = 0; i < bytes.length; i++) bin += String.fromCharCode(bytes[i])
  return btoa(bin)
}
function base64ToBytes(b64: string): Uint8Array<ArrayBuffer> {
  const bin = atob(b64.trim())
  const bytes = new Uint8Array(bin.length) as Uint8Array<ArrayBuffer>
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i)
  return bytes
}
function randomBytes(len: number): Uint8Array<ArrayBuffer> {
  return crypto.getRandomValues(new Uint8Array(len) as Uint8Array<ArrayBuffer>)
}

// Derive an AES key from password + salt via PBKDF2.
async function deriveKey(pass: string, salt: Uint8Array<ArrayBuffer>): Promise<CryptoKey> {
  const baseKey = await crypto.subtle.importKey('raw', enc.encode(pass), 'PBKDF2', false, ['deriveKey'])
  return crypto.subtle.deriveKey(
    { name: 'PBKDF2', salt, iterations: 150000, hash: 'SHA-256' },
    baseKey,
    { name: algorithm.value, length: keySize.value },
    false,
    ['encrypt', 'decrypt']
  )
}

// Resolve salt/IV from the optional fixed inputs, else random.
function resolveSalt(): Uint8Array<ArrayBuffer> {
  return saltInput.value.trim() ? base64ToBytes(saltInput.value) : randomBytes(SALT_LENGTH)
}
function resolveIv(): Uint8Array<ArrayBuffer> {
  return ivInput.value.trim() ? base64ToBytes(ivInput.value) : randomBytes(ivLength.value)
}

// Packed layout: salt(16) || iv || ciphertext
async function encrypt() {
  if (!validate()) return
  busy.value = true
  try {
    const iv = resolveIv()
    if (iv.length !== ivLength.value) throw new Error('bad-iv')
    const salt = resolveSalt()
    const key = await deriveKey(effectivePassword(), salt)

    const ct = new Uint8Array(
      await crypto.subtle.encrypt({ name: algorithm.value, iv }, key, enc.encode(input.value))
    )

    const packed = new Uint8Array(salt.length + iv.length + ct.length)
    packed.set(salt, 0)
    packed.set(iv, salt.length)
    packed.set(ct, salt.length + iv.length)
    output.value = bytesToBase64(packed)
  } catch {
    message.error(t('aes.encryptFailed'))
  } finally {
    busy.value = false
  }
}

async function decrypt() {
  if (!validate()) return
  busy.value = true
  try {
    const packed = base64ToBytes(input.value)
    const ivLen = ivLength.value
    if (packed.length < SALT_LENGTH + ivLen + 1) throw new Error('too-short')

    const salt = packed.slice(0, SALT_LENGTH) as Uint8Array<ArrayBuffer>
    const iv = packed.slice(SALT_LENGTH, SALT_LENGTH + ivLen) as Uint8Array<ArrayBuffer>
    const ct = packed.slice(SALT_LENGTH + ivLen)
    const key = await deriveKey(effectivePassword(), salt)

    const pt = await crypto.subtle.decrypt({ name: algorithm.value, iv }, key, ct)
    output.value = dec.decode(pt)
  } catch {
    message.error(t('aes.decryptFailed'))
  } finally {
    busy.value = false
  }
}

function validate(): boolean {
  if (!cryptoAvailable) {
    message.error(t('aes.insecureContext'))
    return false
  }
  if (!input.value) {
    message.warning(t('aes.needInput'))
    return false
  }
  return true
}

function swap() {
  input.value = output.value
  output.value = ''
}

async function copyOutput() {
  if (!output.value) return
  try {
    await navigator.clipboard.writeText(output.value)
    message.success(t('aes.copied'))
  } catch {
    message.error(t('aes.copyFailed'))
  }
}
</script>

<template>
  <div class="aes-view">
    <a-card :bordered="false">
      <template #title>
        <LockOutlined style="color: #1890ff; margin-right: 8px" />{{ t('aes.title') }}
      </template>

      <a-alert
        v-if="!cryptoAvailable"
        :message="t('aes.insecureContextTitle')"
        :description="t('aes.insecureContext')"
        type="warning"
        show-icon
        style="margin-bottom: 16px"
      />
      <a-alert :message="t('aes.hint')" type="info" show-icon style="margin-bottom: 16px" />

      <a-form layout="vertical">
        <!-- Options row -->
        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item :label="t('aes.algorithm')">
              <a-radio-group v-model:value="algorithm" button-style="solid">
                <a-radio-button value="AES-GCM">AES-GCM</a-radio-button>
                <a-radio-button value="AES-CBC">AES-CBC</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item :label="t('aes.keySize')">
              <a-select v-model:value="keySize">
                <a-select-option :value="128">128-bit</a-select-option>
                <a-select-option :value="192">192-bit</a-select-option>
                <a-select-option :value="256">256-bit</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item :label="t('aes.input')">
          <a-textarea v-model:value="input" :rows="4" :placeholder="t('aes.inputPlaceholder')" allow-clear />
        </a-form-item>

        <a-form-item :label="t('aes.password')">
          <a-input-password v-model:value="password" :placeholder="t('aes.passwordPlaceholder', { pwd: 'twsny' })">
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>

        <a-form-item :label="t('aes.salt')" :help="t('aes.saltHelp')">
          <a-input v-model:value="saltInput" :placeholder="t('aes.randomPlaceholder')" allow-clear />
        </a-form-item>

        <a-form-item :label="t('aes.iv')" :help="t('aes.ivHelp', { len: ivLength })">
          <a-input v-model:value="ivInput" :placeholder="t('aes.randomPlaceholder')" allow-clear />
        </a-form-item>

        <a-space wrap style="margin-bottom: 16px">
          <a-button type="primary" :loading="busy" :disabled="!cryptoAvailable" @click="encrypt">
            <template #icon><LockOutlined /></template>
            {{ t('aes.encrypt') }}
          </a-button>
          <a-button :loading="busy" :disabled="!cryptoAvailable" @click="decrypt">
            <template #icon><UnlockOutlined /></template>
            {{ t('aes.decrypt') }}
          </a-button>
          <a-button :disabled="!output" @click="swap">
            <template #icon><SwapOutlined /></template>
            {{ t('aes.useAsInput') }}
          </a-button>
        </a-space>

        <a-form-item :label="t('aes.output')">
          <a-textarea :value="output" :rows="4" readonly :placeholder="t('aes.outputPlaceholder')" />
          <a-button size="small" style="margin-top: 8px" :disabled="!output" @click="copyOutput">
            <template #icon><CopyOutlined /></template>
            {{ t('aes.copy') }}
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<style scoped>
.aes-view { display: flex; flex-direction: column; gap: 16px; }
</style>
