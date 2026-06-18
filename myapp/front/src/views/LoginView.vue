<script lang="ts" setup>
import { reactive, ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { UserOutlined, LockOutlined, BulbOutlined, ReloadOutlined, SafetyOutlined, TranslationOutlined } from '@ant-design/icons-vue'
import { message, theme as antTheme } from 'ant-design-vue'
import { saveAuth } from '../utils/auth'
import { theme as appTheme, toggleTheme } from '../utils/theme'
import axiosInstance from '../utils/axios'
import { setLocale, getLocale } from '../i18n'
import antZhCN from 'ant-design-vue/es/locale/zh_CN'
import antEnUS from 'ant-design-vue/es/locale/en_US'

const { t, locale } = useI18n()
const router = useRouter()
const loading = ref(false)
const captchaLoading = ref(false)
const captchaCanvas = ref<HTMLCanvasElement | null>(null)
const captchaAnswer = ref('')
const captchaUuid = ref('')

const form = reactive({ username: '', password: '', captcha: '' })

const antConfig = computed(() => ({
  algorithm: appTheme.value === 'dark' ? antTheme.darkAlgorithm : antTheme.defaultAlgorithm,
  locale: locale.value === 'zh-CN' ? antZhCN : antEnUS,
}))

async function fetchCaptcha() {
  captchaLoading.value = true
  try {
    const res = await axiosInstance.get('/api/v1/code/image')
    const text = String(res.data).trim()
    const [uuid, code] = text.split('_')
    captchaUuid.value = uuid || ''
    captchaAnswer.value = code || ''
  } catch {
    message.error(t('login.failedCaptcha'))
  } finally {
    captchaLoading.value = false
  }
}

function drawCaptcha(code: string) {
  const canvas = captchaCanvas.value
  if (!canvas || !code) return
  const ctx = canvas.getContext('2d')!
  const W = canvas.width
  const H = canvas.height

  ctx.clearRect(0, 0, W, H)
  ctx.fillStyle = appTheme.value === 'dark' ? '#2a2a2a' : '#f5f5f5'
  ctx.fillRect(0, 0, W, H)

  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = `hsla(${Math.random() * 360},50%,60%,0.6)`
    ctx.lineWidth = 1.5
    ctx.beginPath()
    ctx.moveTo(Math.random() * W, Math.random() * H)
    ctx.bezierCurveTo(
      Math.random() * W, Math.random() * H,
      Math.random() * W, Math.random() * H,
      Math.random() * W, Math.random() * H,
    )
    ctx.stroke()
  }

  const palette = ['#e74c3c', '#2980b9', '#27ae60', '#8e44ad', '#e67e22', '#16a085']
  code.toUpperCase().split('').forEach((char, i) => {
    ctx.save()
    ctx.font = `bold ${22 + Math.random() * 6}px monospace`
    ctx.fillStyle = palette[i % palette.length]
    ctx.translate(14 + i * 28, H / 2 + 6)
    ctx.rotate((Math.random() - 0.5) * 0.6)
    ctx.fillText(char, 0, 0)
    ctx.restore()
  })

  for (let i = 0; i < 40; i++) {
    ctx.fillStyle = `rgba(128,128,128,0.25)`
    ctx.fillRect(Math.random() * W, Math.random() * H, 2, 2)
  }
}

watch(captchaAnswer, val => nextTick(() => drawCaptcha(val)))
watch(appTheme, () => nextTick(() => drawCaptcha(captchaAnswer.value)))

onMounted(fetchCaptcha)

async function handleLogin() {
  if (!form.username || !form.password) {
    message.warning(t('login.fillUsernamePassword'))
    return
  }
  if (!form.captcha) {
    message.warning(t('login.fillCaptcha'))
    return
  }
  if (form.captcha.toUpperCase() !== captchaAnswer.value.toUpperCase()) {
    message.error(t('login.wrongCaptcha'))
    form.captcha = ''
    fetchCaptcha()
    return
  }
  loading.value = true
  try {
    const res = await axiosInstance.post('/api/v1/auth/login', {
      username: form.username,
      password: form.password,
    })
    const { token, user } = res.data
    saveAuth(token, user)
    message.success(t('login.welcome', { name: user.name }))
    router.push('/home')
  } catch (err: any) {
    const msg = err.response?.data?.message || t('login.invalidCredentials')
    message.error(msg)
    form.captcha = ''
    fetchCaptcha()
  } finally {
    loading.value = false
  }
}

function toggleLang() {
  setLocale(getLocale() === 'zh-CN' ? 'en-US' : 'zh-CN')
}
</script>

<template>
  <a-config-provider :theme="antConfig" :locale="antConfig.locale">
    <div class="login-page">
      <div class="theme-btn">
        <a-button type="text" @click="toggleTheme">
          <BulbOutlined />
          {{ appTheme === 'dark' ? t('login.lightMode') : t('login.darkMode') }}
        </a-button>
        <a-button type="text" @click="toggleLang">
          <TranslationOutlined />
          {{ locale === 'zh-CN' ? t('lang.en') : t('lang.zh') }}
        </a-button>
      </div>

      <a-card class="login-card">
        <div class="login-header">
          <div class="app-logo">🚀</div>
          <h2>{{ t('login.title') }}</h2>
          <p class="subtitle">{{ t('login.subtitle') }}</p>
        </div>

        <a-form layout="vertical" :model="form" @finish="handleLogin">
          <a-form-item :label="t('login.username')" name="username">
            <a-input
              v-model:value="form.username"
              size="large"
              :placeholder="t('login.usernamePlaceholder')"
              allow-clear
            >
              <template #prefix><UserOutlined /></template>
            </a-input>
          </a-form-item>

          <a-form-item :label="t('login.password')" name="password">
            <a-input-password
              v-model:value="form.password"
              size="large"
              :placeholder="t('login.passwordPlaceholder')"
            >
              <template #prefix><LockOutlined /></template>
            </a-input-password>
          </a-form-item>

          <a-form-item :label="t('login.captcha')" name="captcha">
            <div class="captcha-row">
              <a-input
                v-model:value="form.captcha"
                size="large"
                :placeholder="t('login.captchaPlaceholder')"
                :maxlength="4"
                autocomplete="off"
              >
                <template #prefix><SafetyOutlined /></template>
              </a-input>
              <a-spin :spinning="captchaLoading">
                <canvas
                  ref="captchaCanvas"
                  width="130"
                  height="44"
                  class="captcha-canvas"
                  @click="fetchCaptcha"
                />
              </a-spin>
              <a-button type="text" size="small" @click="fetchCaptcha" :loading="captchaLoading">
                <ReloadOutlined />
              </a-button>
            </div>
          </a-form-item>

          <div class="form-links">
            <a-checkbox>{{ t('login.rememberMe') }}</a-checkbox>
            <router-link to="/forgot-password">{{ t('login.forgotPassword') }}</router-link>
          </div>

          <a-button
            type="primary"
            html-type="submit"
            size="large"
            block
            :loading="loading"
            style="margin-top: 8px"
          >
            {{ t('login.signIn') }}
          </a-button>
        </a-form>

        <a-divider>{{ t('login.or') }}</a-divider>

        <div class="register-link">
          {{ t('login.noAccount') }}
          <router-link to="/register">{{ t('login.registerNow') }}</router-link>
        </div>

        <div class="hint">
          <a-alert
            :message="t('login.demoCredentials')"
            type="info"
            show-icon
            style="margin-top: 16px"
          />
        </div>
      </a-card>
    </div>
  </a-config-provider>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px;
}
.theme-btn {
  position: fixed;
  top: 16px;
  right: 16px;
  display: flex;
  gap: 4px;
}
.login-card {
  width: 100%;
  max-width: 420px;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}
.login-header {
  text-align: center;
  margin-bottom: 24px;
}
.app-logo {
  font-size: 48px;
  margin-bottom: 8px;
}
.login-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
}
.subtitle {
  color: #888;
  margin: 4px 0 0;
}
.form-links {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.register-link {
  text-align: center;
  color: #888;
}
.captcha-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.captcha-canvas {
  border-radius: 6px;
  border: 1px solid #d9d9d9;
  cursor: pointer;
  flex-shrink: 0;
  display: block;
}
.captcha-canvas:hover {
  opacity: 0.85;
}
</style>
