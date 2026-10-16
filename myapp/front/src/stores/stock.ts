import { ref } from 'vue'
import {
  stockApi,
  type AddStockRequest,
  type StockDTO,
  type StockPriceHistoryDTO,
} from '../api/stock'

export const stocks = ref<StockDTO[]>([])
export const loading = ref(false)
export const refreshing = ref(false)

/** History of the stock currently open in the detail drawer. */
export const history = ref<StockPriceHistoryDTO[]>([])
export const historyTotal = ref(0)
export const historyLoading = ref(false)

export async function fetchStocks() {
  loading.value = true
  try {
    stocks.value = await stockApi.list()
  } finally {
    loading.value = false
  }
}

/**
 * Pull fresh prices for the whole watch list. The backend writes each new price into
 * the stock's history, so this is also what grows the chart.
 */
export async function refreshStocks() {
  refreshing.value = true
  try {
    stocks.value = await stockApi.refreshAll()
  } finally {
    refreshing.value = false
  }
}

/** Refresh one stock and patch it in place so the table doesn't jump. */
export async function refreshStock(id: number) {
  const updated = await stockApi.refreshOne(id)
  const index = stocks.value.findIndex(s => s.id === id)
  if (index >= 0) {
    stocks.value[index] = updated
  }
  return updated
}

export async function addStock(request: AddStockRequest) {
  const created = await stockApi.add(request)
  stocks.value = [...stocks.value, created]
  return created
}

export async function removeStock(id: number) {
  await stockApi.remove(id)
  stocks.value = stocks.value.filter(s => s.id !== id)
}

export async function fetchHistory(id: number, page = 0, size = 100) {
  historyLoading.value = true
  try {
    const result = await stockApi.history(id, page, size)
    history.value = result.items
    historyTotal.value = result.total
  } finally {
    historyLoading.value = false
  }
}

export function clearHistory() {
  history.value = []
  historyTotal.value = 0
}
