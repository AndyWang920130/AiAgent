<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MailOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { authApi } from '../api/auth'

const { t } = useI18n()
const router = useRouter()

// step 1 = enter email + request code, step 2 = enter code + new password, done = success screen
const step = ref<'email' | 'reset' | 'done'>('email')
const sendingCode = ref(false)
const resetting = ref(false)
const sendCooldown = ref(0)
let cooldownTimer: number | undefined

const form = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
})

function isValidEmail(email: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

function startCooldown() {
  sendCooldown.value = 60
  window.clearInterval(cooldownTimer)
  cooldownTimer = window.setInterval(() => {
    sendCooldown.value -= 1
    if (sendCooldown.value <= 0) window.clearInterval(cooldownTimer)
  }, 1000)
}

async function sendCode() {
  if (!form.email || !isValidEmail(form.email)) {
    message.warning(t('forgot.validEmailRequired'))
    return
  }
  sendingCode.value = true
  try {
    await authApi.forgotPassword(form.email)
    // Backend always returns a generic success (no email enumeration); advance to step 2.
    message.success(t('forgot.codeSent'))
    step.value = 'reset'
    startCooldown()
  } catch (err: any) {
    message.error(err?.response?.data?.message || t('forgot.sendFailed'))
  } finally {
    sendingCode.value = false
  }
}

async function resetPassword() {
  if (!form.code || !form.newPassword) {
    message.warning(t('forgot.fillAllFields'))
    return
  }
  if (form.newPassword.length < 6) {
    message.error(t('forgot.passwordTooShort'))
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    message.error(t('forgot.passwordsNoMatch'))
    return
  }
  resetting.value = true
  try {
    await authApi.resetPassword({
      email: form.email,
      code: form.code,
      newPassword: form.newPassword,
    })
    step.value = 'done'
  } catch (err: any) {
    message.error(err?.response?.data?.message || t('forgot.resetFailed'))
  } finally {
    resetting.value = false
  }
}
</script>

<template>
  <div class="forgot-page">
    <a-card class="forgot-card">
      <div class="forgot-header">
        <div class="app-logo">🔑</div>
        <h2>{{ t('forgot.title') }}</h2>
        <p class="subtitle">{{ step === 'reset' ? t('forgot.subtitleCode') : t('forgot.subtitle') }}</p>
      </div>

      <!-- Step 1: request a reset code -->
      <template v-if="step === 'email'">
        <a-form layout="vertical" @submit.prevent>
          <a-form-item :label="t('forgot.emailAddress')" required>
            <a-input v-model:value="form.email" size="large" :placeholder="t('register.emailPlaceholder')">
              <template #prefix><MailOutlined /></template>
            </a-input>
          </a-form-item>
          <a-button type="primary" size="large" block :loading="sendingCode" @click="sendCode">
            {{ t('forgot.sendCode') }}
          </a-button>
        </a-form>
      </template>

      <!-- Step 2: enter code + new password -->
      <template v-else-if="step === 'reset'">
        <a-form layout="vertical" @submit.prevent>
          <a-form-item :label="t('forgot.emailCode')" required>
            <a-input-group compact>
              <a-input
                v-model:value="form.code"
                class="code-input"
                size="large"
                :placeholder="t('register.emailCodePlaceholder')"
              >
                <template #prefix><SafetyOutlined /></template>
              </a-input>
              <a-button
                class="code-button"
                size="large"
                html-type="button"
                :loading="sendingCode"
                :disabled="sendCooldown > 0"
                @click="sendCode"
              >
                {{ sendCooldown > 0 ? t('forgot.resendIn', { seconds: sendCooldown }) : t('forgot.resend') }}
              </a-button>
            </a-input-group>
          </a-form-item>
          <a-form-item :label="t('forgot.newPassword')" required>
            <a-input-password v-model:value="form.newPassword" size="large" :placeholder="t('register.passwordPlaceholder')">
              <template #prefix><LockOutlined /></template>
            </a-input-password>
          </a-form-item>
          <a-form-item :label="t('forgot.confirmPassword')" required>
            <a-input-password v-model:value="form.confirmPassword" size="large" :placeholder="t('register.confirmPasswordPlaceholder')">
              <template #prefix><LockOutlined /></template>
            </a-input-password>
          </a-form-item>
          <a-button type="primary" size="large" block :loading="resetting" @click="resetPassword">
            {{ t('forgot.resetPassword') }}
          </a-button>
        </a-form>
      </template>

      <!-- Done -->
      <template v-else>
        <a-result
          status="success"
          :title="t('forgot.resetSuccess')"
          :sub-title="t('forgot.resetSuccessSub')"
        >
          <template #extra>
            <a-button type="primary" @click="router.push('/login')">{{ t('forgot.backToLogin') }}</a-button>
          </template>
        </a-result>
      </template>

      <div class="back-link" v-if="step !== 'done'">
        <router-link to="/login">{{ t('forgot.backLink') }}</router-link>
      </div>
    </a-card>
  </div>
</template>

<style scoped>
.forgot-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px;
}
.forgot-card {
  width: 100%;
  max-width: 420px;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}
.forgot-header { text-align: center; margin-bottom: 24px; }
.app-logo { font-size: 48px; margin-bottom: 8px; }
.forgot-header h2 { margin: 0; font-size: 24px; font-weight: 700; }
.subtitle { color: #888; margin: 4px 0 0; }
.back-link { text-align: center; margin-top: 16px; }
.code-input { width: calc(100% - 120px); }
.code-button { width: 120px; }
</style>
