<script lang="ts" setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { MailOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

const router = useRouter()
const loading = ref(false)
const sent = ref(false)
const email = ref('')

async function handleSubmit() {
  if (!email.value) {
    message.warning('Please enter your email')
    return
  }
  loading.value = true
  await new Promise(r => setTimeout(r, 1000))
  loading.value = false
  sent.value = true
}
</script>

<template>
  <div class="forgot-page">
    <a-card class="forgot-card">
      <div class="forgot-header">
        <div class="app-logo">🔑</div>
        <h2>Reset Password</h2>
        <p class="subtitle">Enter your email and we'll send a reset link</p>
      </div>

      <template v-if="!sent">
        <a-form layout="vertical" @finish="handleSubmit">
          <a-form-item label="Email Address" required>
            <a-input v-model:value="email" size="large" placeholder="your@email.com">
              <template #prefix><MailOutlined /></template>
            </a-input>
          </a-form-item>
          <a-button type="primary" html-type="submit" size="large" block :loading="loading">
            Send Reset Link
          </a-button>
        </a-form>
      </template>

      <template v-else>
        <a-result
          status="success"
          title="Check your inbox!"
          :sub-title="`We sent a reset link to ${email}`"
        >
          <template #extra>
            <a-button type="primary" @click="router.push('/login')">Back to Login</a-button>
          </template>
        </a-result>
      </template>

      <div class="back-link" v-if="!sent">
        <router-link to="/login">← Back to login</router-link>
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
</style>
