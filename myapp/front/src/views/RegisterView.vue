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

const form = reactive({
  username: '',
  name: '',
  email: '',
  password: '',
  confirmPassword: '',
})

async function handleRegister() {
  if (!form.username || !form.password || !form.name) {
    message.warning(t('register.fillAllFields'))
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

      <a-form layout="vertical" @finish="handleRegister">
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

        <a-form-item :label="t('register.email')">
          <a-input v-model:value="form.email" size="large" :placeholder="t('register.emailPlaceholder')">
            <template #prefix><MailOutlined /></template>
          </a-input>
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

        <a-button type="primary" html-type="submit" size="large" block :loading="loading">
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
</style>
