import { ref } from 'vue'
import { dataIntegrationApi, type DataIntegrationDTO, type KeyValue } from '../api/dataIntegration'

export const integrations = ref<DataIntegrationDTO[]>([])
export const loading = ref(false)

export async function fetchIntegrations() {
  loading.value = true
  try {
    integrations.value = await dataIntegrationApi.list()
  } finally {
    loading.value = false
  }
}

export async function removeIntegration(id: number) {
  await dataIntegrationApi.remove(id)
  integrations.value = integrations.value.filter(i => i.id !== id)
}

/**
 * Parse a JSON string (array of {key,value}) into KeyValue rows. Tolerant of
 * null/blank/invalid input — returns an empty list rather than throwing.
 */
export function parseRows(json: string | null | undefined): KeyValue[] {
  if (!json) return []
  try {
    const parsed = JSON.parse(json)
    if (!Array.isArray(parsed)) return []
    return parsed
      .filter(item => item && typeof item === 'object')
      .map(item => ({ key: String(item.key ?? ''), value: String(item.value ?? '') }))
  } catch {
    return []
  }
}

/** Serialize KeyValue rows to a JSON string, dropping rows with an empty key. */
export function serializeRows(rows: KeyValue[]): string {
  return JSON.stringify(
    rows
      .filter(r => r.key.trim() !== '')
      .map(r => ({ key: r.key.trim(), value: r.value }))
  )
}
