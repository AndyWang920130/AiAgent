<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import axiosInstance from '../utils/axios'

const { t } = useI18n()
const router = useRouter()
const loading = ref(false)
const sendingCode = ref(false)
const sendCooldown = ref(0)
let cooldownTimer: number | undefined

const form = reactive({
  username: '',
  name: '',
  email: '',
  emailCode: '',
  password: '',
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
    if (sendCooldown.value <= 0) {
      window.clearInterval(cooldownTimer)
    }
  }, 1000)
}

async function sendEmailCode() {
  if (!form.email || !isValidEmail(form.email)) {
    message.warning(t('register.validEmailRequired'))
    return
  }
  sendingCode.value = true
  try {
    await axiosInstance.post('/api/v1/code/email', { email: form.email })
    message.success(t('register.emailCodeSent'))
    startCooldown()
  } catch (err: any) {
    const msg = err.response?.data?.message || t('register.emailCodeFailed')
    message.error(msg)
  } finally {
    sendingCode.value = false
  }
}

async function handleRegister() {
  if (!form.username || !form.password || !form.name || !form.email || !form.emailCode) {
    message.warning(t('register.fillAllFields'))
    return
  }
  if (!isValidEmail(form.email)) {
    message.warning(t('register.validEmailRequired'))
    return
  }
  if (form.password !== form.confirmPassword) {
    message.error(t('register.passwordsNoMatch'))
    return
  }
  if (form.password.length < 6) {
    message.error(t('register.passwordTooShort'))
    return
  }
  loading.value = true
  try {
    await axiosInstance.post('/api/v1/auth/register', {
      username: form.username,
      name: form.name,
      email: form.email,
      emailCode: form.emailCode,
      password: form.password,
    })
    message.success(t('register.accountCreated'))
    router.push('/login')
  } catch (err: any) {
    const msg = err.response?.data?.message || t('register.failed')
    message.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <a-card class="register-card">
      <div class="register-header">
        <div class="app-logo">🚀</div>
        <h2>{{ t('register.title') }}</h2>
        <p class="subtitle">{{ t('register.subtitle') }}</p>
      </div>

      <a-form layout="vertical" @submit.prevent>
        <a-form-item :label="t('register.fullName')" required>
          <a-input v-model:value="form.name" size="large" :placeholder="t('register.fullNamePlaceholder')">
            <template #prefix><UserOutlined /></template>
          </a-input>
        </a-form-item>

        <a-form-item :label="t('register.username')" required>
          <a-input v-model:value="form.username" size="large" :placeholder="t('register.usernamePlaceholder')">
            <template #prefix><UserOutlined /></template>
          </a-input>
        </a-form-item>

        <a-form-item :label="t('register.email')" required>
          <a-input v-model:value="form.email" size="large" :placeholder="t('register.emailPlaceholder')">
            <template #prefix><MailOutlined /></template>
          </a-input>
        </a-form-item>

        <a-form-item :label="t('register.emailCode')" required>
          <a-input-group compact>
            <a-input
              v-model:value="form.emailCode"
              class="email-code-input"
              size="large"
              :placeholder="t('register.emailCodePlaceholder')"
            />
            <a-button
              class="email-code-button"
              size="large"
              html-type="button"
              :loading="sendingCode"
              :disabled="sendCooldown > 0"
              @click="sendEmailCode"
            >
              {{ sendCooldown > 0 ? t('register.resendIn', { seconds: sendCooldown }) : t('register.sendCode') }}
            </a-button>
          </a-input-group>
        </a-form-item>

        <a-form-item :label="t('register.password')" required>
          <a-input-password v-model:value="form.password" size="large" :placeholder="t('register.passwordPlaceholder')">
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>

        <a-form-item :label="t('register.confirmPassword')" required>
          <a-input-password v-model:value="form.confirmPassword" size="large" :placeholder="t('register.confirmPasswordPlaceholder')">
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>

        <a-button type="primary" size="large" block :loading="loading" @click="handleRegister">
          {{ t('register.createAccount') }}
        </a-button>
      </a-form>

      <div class="login-link">
        {{ t('register.hasAccount') }}
        <router-link to="/login">{{ t('register.signIn') }}</router-link>
      </div>
    </a-card>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px;
}
.register-card {
  width: 100%;
  max-width: 440px;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}
.register-header {
  text-align: center;
  margin-bottom: 24px;
}
.app-logo { font-size: 48px; margin-bottom: 8px; }
.register-header h2 { margin: 0; font-size: 24px; font-weight: 700; }
.subtitle { color: #888; margin: 4px 0 0; }
.login-link { text-align: center; color: #888; margin-top: 16px; }
.email-code-input {
  width: calc(100% - 132px);
}
.email-code-button {
  width: 132px;
}
</style>
