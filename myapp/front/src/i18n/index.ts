import { createI18n } from 'vue-i18n'
import zhCN from '../locales/zh-CN'
import enUS from '../locales/en-US'

export type AppLocale = 'zh-CN' | 'en-US'

const saved = (localStorage.getItem('locale') as AppLocale) || 'zh-CN'

export const i18n = createI18n({
  legacy: false,
  locale: saved,
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
})

export function setLocale(locale: AppLocale) {
  ;(i18n.global.locale as any).value = locale
  localStorage.setItem('locale', locale)
}

export function getLocale(): AppLocale {
  return (i18n.global.locale as any).value
}
