import { ref } from 'vue'
import {
  cardGameRecordApi,
  type AdminCardGameRecordQuery,
  type CardGameRecordDTO,
  type CardGameRecordOutcome,
} from '../api/cardGameRecord'

// ----- The administrator's 70235 match record list ----------------------------------------

export const records = ref<CardGameRecordDTO[]>([])
export const total = ref(0)
export const loading = ref(false)

/** Current filters, kept here so the table and the filter bar stay in step. */
export const query = ref('')
export const outcomeFilter = ref<CardGameRecordOutcome | undefined>(undefined)
export const page = ref(1)
export const pageSize = ref(20)

export async function fetchRecords() {
  loading.value = true
  try {
    const params: AdminCardGameRecordQuery = {
      query: query.value,
      outcome: outcomeFilter.value,
      page: page.value - 1,
      size: pageSize.value,
    }
    const result = await cardGameRecordApi.adminList(params)
    records.value = result.items
    total.value = result.total
  } finally {
    loading.value = false
  }
}

/** Apply a filter change: results move back to the first page. */
export async function applyFilters() {
  page.value = 1
  await fetchRecords()
}
