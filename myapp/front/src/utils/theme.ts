import { ref, watch } from 'vue'

type Theme = 'light' | 'dark'

const THEME_KEY = 'app_theme'
const stored = (localStorage.getItem(THEME_KEY) as Theme) || 'light'

export const theme = ref<Theme>(stored)

export function toggleTheme(): void {
  theme.value = theme.value === 'light' ? 'dark' : 'light'
}

watch(theme, val => {
  localStorage.setItem(THEME_KEY, val)
  document.documentElement.setAttribute('data-theme', val)
})

document.documentElement.setAttribute('data-theme', theme.value)
