<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  DeleteOutlined,
  LineChartOutlined,
  AreaChartOutlined,
} from '@ant-design/icons-vue'
import {
  stocks,
  loading,
  refreshing,
  history,
  historyLoading,
  fetchStocks,
  refreshStocks,
  refreshStock,
  addStock,
  removeStock,
  fetchHistory,
  clearHistory,
} from '../stores/stock'
import type { StockDTO, StockMarket } from '../api/stock'
import { theme as appTheme } from '../utils/theme'

const { t } = useI18n()

const markets: { value: StockMarket; label: string }[] = [
  { value: 'SH', label: 'SH · 上海' },
  { value: 'SZ', label: 'SZ · 深圳' },
  { value: 'HK', label: 'HK · 香港' },
  { value: 'US', label: 'US · 美股' },
]

onMounted(async () => {
  try {
    await fetchStocks()
  } catch {
    message.error(t('stock.loadFailed'))
  }
})

const columns = computed(() => [
  { title: t('stock.code'), key: 'code', width: 130 },
  { title: t('stock.name'), dataIndex: 'name', key: 'name', ellipsis: true },
  { title: t('stock.latestPrice'), key: 'latestPrice', width: 120, align: 'right' as const },
  { title: t('stock.change'), key: 'priceChange', width: 110, align: 'right' as const },
  { title: t('stock.changePercent'), key: 'changePercent', width: 110, align: 'right' as const },
  { title: t('stock.dayHigh'), key: 'dayHigh', width: 100, align: 'right' as const },
  { title: t('stock.dayLow'), key: 'dayLow', width: 100, align: 'right' as const },
  { title: t('stock.marketTime'), dataIndex: 'marketTimeText', key: 'marketTimeText', width: 170 },
  { title: t('stock.actions'), key: 'actions', width: 150 },
])

const historyColumns = computed(() => [
  { title: t('stock.recordedDate'), key: 'recordedDate', width: 180 },
  { title: t('stock.price'), key: 'price', width: 110, align: 'right' as const },
  { title: t('stock.change'), key: 'priceChange', width: 110, align: 'right' as const },
  { title: t('stock.changePercent'), key: 'changePercent', width: 110, align: 'right' as const },
  { title: t('stock.marketTime'), dataIndex: 'marketTimeText', key: 'marketTimeText' },
])

/** Trim trailing zeros the backend's decimal(19,4) columns add to every price. */
function formatPrice(value?: number | null): string {
  if (value === null || value === undefined) return '—'
  return Number(value).toFixed(2)
}

function formatPercent(value?: number | null): string {
  if (value === null || value === undefined) return '—'
  const n = Number(value)
  return `${n > 0 ? '+' : ''}${n.toFixed(2)}%`
}

function formatChange(value?: number | null): string {
  if (value === null || value === undefined) return '—'
  const n = Number(value)
  return `${n > 0 ? '+' : ''}${n.toFixed(2)}`
}

function formatVolume(value?: number | null): string {
  if (value === null || value === undefined) return '—'
  return Number(value).toLocaleString()
}

function formatInstant(value?: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

/** Red for a rise, green for a fall — the convention on Chinese exchanges. */
function changeClass(value?: number | null): string {
  if (value === null || value === undefined || Number(value) === 0) return 'flat'
  return Number(value) > 0 ? 'up' : 'down'
}

// --- Add ------------------------------------------------------------------

const addOpen = ref(false)
const adding = ref(false)
const form = ref<{ market: StockMarket; code: string }>({ market: 'SH', code: '' })

function openAdd() {
  form.value = { market: 'SH', code: '' }
  addOpen.value = true
}

async function handleAdd() {
  const code = form.value.code.trim()
  if (!code) {
    message.warning(t('stock.codeRequired'))
    return
  }
  adding.value = true
  try {
    await addStock({ market: form.value.market, code })
    message.success(t('stock.added'))
    addOpen.value = false
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { title?: string } } })?.response?.data?.title
    message.error(detail || t('stock.addFailed'))
  } finally {
    adding.value = false
  }
}

// --- Refresh --------------------------------------------------------------

const refreshingId = ref<number | null>(null)

async function handleRefreshAll() {
  try {
    await refreshStocks()
    message.success(t('stock.refreshed'))
    if (historyStock.value) {
      await loadHistory(historyStock.value.id)
    }
  } catch {
    message.error(t('stock.refreshFailed'))
  }
}

async function handleRefreshOne(record: StockDTO) {
  refreshingId.value = record.id
  try {
    const updated = await refreshStock(record.id)
    if (updated.lastQuoteError) {
      message.warning(updated.lastQuoteError)
    } else {
      message.success(t('stock.refreshed'))
    }
    if (historyStock.value?.id === record.id) {
      await loadHistory(record.id)
    }
  } catch {
    message.error(t('stock.refreshFailed'))
  } finally {
    refreshingId.value = null
  }
}

// --- Delete ---------------------------------------------------------------

function handleDelete(record: StockDTO) {
  Modal.confirm({
    title: t('stock.deleteConfirm'),
    content: `${record.market} ${record.code}${record.name ? ' · ' + record.name : ''} — ${t('stock.deleteHint')}`,
    okText: t('stock.delete'),
    okType: 'danger',
    async onOk() {
      try {
        await removeStock(record.id)
        if (historyStock.value?.id === record.id) {
          historyOpen.value = false
        }
        message.success(t('stock.deleted'))
      } catch {
        message.error(t('stock.deleteFailed'))
      }
    },
  })
}

// --- History drawer -------------------------------------------------------

const historyOpen = ref(false)
const historyStock = ref<StockDTO | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
let resizeObserver: ResizeObserver | null = null

async function openHistory(record: StockDTO) {
  historyStock.value = record
  historyOpen.value = true
  clearHistory()
  await loadHistory(record.id)
}

async function loadHistory(id: number) {
  try {
    await fetchHistory(id)
    await nextTick()
    observeCanvas()
    draw()
  } catch {
    message.error(t('stock.historyLoadFailed'))
  }
}

function closeHistory() {
  resizeObserver?.disconnect()
  resizeObserver = null
  historyStock.value = null
  clearHistory()
}

function observeCanvas() {
  if (!canvasRef.value || resizeObserver) return
  resizeObserver = new ResizeObserver(() => draw())
  resizeObserver.observe(canvasRef.value)
}

/** Oldest first, so the line reads left-to-right in time. */
const chartPoints = computed(() =>
  [...history.value]
    .reverse()
    .map(p => ({ price: Number(p.price), label: p.marketTimeText || formatInstant(p.recordedDate) }))
)

/**
 * Line chart of the recorded prices. Drawn on a raw canvas — this app has no charting
 * dependency, and the ECG view establishes the same approach.
 */
function draw() {
  const canvas = canvasRef.value
  if (!canvas) return

  const rect = canvas.getBoundingClientRect()
  const width = Math.max(1, Math.floor(rect.width))
  const height = canvas.height
  if (canvas.width !== width) canvas.width = width

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const dark = appTheme.value === 'dark'
  const padding = { top: 16, right: 56, bottom: 24, left: 12 }
  ctx.clearRect(0, 0, width, height)
  ctx.fillStyle = dark ? '#141414' : '#fafafa'
  ctx.fillRect(0, 0, width, height)

  const points = chartPoints.value
  const plotWidth = width - padding.left - padding.right
  const plotHeight = height - padding.top - padding.bottom
  if (points.length === 0 || plotWidth <= 0 || plotHeight <= 0) return

  // Pad a flat series so a single price doesn't render as a line on the axis.
  let min = Math.min(...points.map(p => p.price))
  let max = Math.max(...points.map(p => p.price))
  if (max - min < 1e-9) {
    const pad = Math.max(max * 0.01, 0.01)
    min -= pad
    max += pad
  }

  const xOf = (i: number) =>
    padding.left + (points.length === 1 ? plotWidth / 2 : (i / (points.length - 1)) * plotWidth)
  const yOf = (price: number) =>
    padding.top + plotHeight - ((price - min) / (max - min)) * plotHeight

  // Horizontal gridlines, labelled with the price at that level.
  const gridColor = dark ? 'rgba(255,255,255,0.12)' : 'rgba(0,0,0,0.08)'
  const textColor = dark ? 'rgba(255,255,255,0.55)' : 'rgba(0,0,0,0.55)'
  const steps = 4
  ctx.lineWidth = 1
  ctx.font = '11px sans-serif'
  ctx.textBaseline = 'middle'
  for (let i = 0; i <= steps; i++) {
    const price = min + ((max - min) * i) / steps
    const y = yOf(price)
    ctx.strokeStyle = gridColor
    ctx.beginPath()
    ctx.moveTo(padding.left, y)
    ctx.lineTo(padding.left + plotWidth, y)
    ctx.stroke()
    ctx.fillStyle = textColor
    ctx.textAlign = 'left'
    ctx.fillText(price.toFixed(2), padding.left + plotWidth + 6, y)
  }

  // The series: red when it closed above where it opened, green when below.
  const rising = points[points.length - 1].price >= points[0].price
  const lineColor = rising ? '#e4393c' : '#17a34a'
  ctx.strokeStyle = lineColor
  ctx.lineWidth = 2
  ctx.beginPath()
  points.forEach((p, i) => {
    const x = xOf(i)
    const y = yOf(p.price)
    if (i === 0) ctx.moveTo(x, y)
    else ctx.lineTo(x, y)
  })
  ctx.stroke()

  // Fill under the line for readability at a glance.
  const gradient = ctx.createLinearGradient(0, padding.top, 0, padding.top + plotHeight)
  gradient.addColorStop(0, rising ? 'rgba(228,57,60,0.22)' : 'rgba(23,163,74,0.22)')
  gradient.addColorStop(1, 'rgba(0,0,0,0)')
  ctx.fillStyle = gradient
  ctx.beginPath()
  ctx.moveTo(xOf(0), padding.top + plotHeight)
  points.forEach((p, i) => ctx.lineTo(xOf(i), yOf(p.price)))
  ctx.lineTo(xOf(points.length - 1), padding.top + plotHeight)
  ctx.closePath()
  ctx.fill()

  // Mark each recorded point; with few points the line alone reads as a guess.
  ctx.fillStyle = lineColor
  points.forEach((p, i) => {
    ctx.beginPath()
    ctx.arc(xOf(i), yOf(p.price), points.length > 60 ? 1.5 : 3, 0, Math.PI * 2)
    ctx.fill()
  })

  // First and last timestamps only — intermediate labels never fit.
  ctx.fillStyle = textColor
  ctx.textBaseline = 'bottom'
  ctx.textAlign = 'left'
  ctx.fillText(points[0].label, padding.left, height - 6)
  if (points.length > 1) {
    ctx.textAlign = 'right'
    ctx.fillText(points[points.length - 1].label, padding.left + plotWidth, height - 6)
  }
}

watch(appTheme, () => draw())
watch(history, () => draw())

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
})
</script>

<template>
  <div class="stock-list-view">
    <a-card :bordered="false">
      <template #title>
        <LineChartOutlined style="color: #1890ff; margin-right: 8px" />{{ t('stock.title') }}
      </template>
      <template #extra>
        <a-space>
          <a-button :loading="refreshing" @click="handleRefreshAll">
            <template #icon><ReloadOutlined /></template>
            {{ t('stock.refreshAll') }}
          </a-button>
          <a-button type="primary" @click="openAdd">
            <template #icon><PlusOutlined /></template>
            {{ t('stock.add') }}
          </a-button>
        </a-space>
      </template>

      <a-table
        :columns="columns"
        :data-source="stocks"
        :loading="loading"
        :row-key="(r: StockDTO) => r.id"
        :pagination="{ pageSize: 10, showSizeChanger: false }"
        :scroll="{ x: 1100 }"
      >
        <template #emptyText>
          <a-empty :description="t('stock.empty')" />
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'code'">
            <a-space :size="4">
              <a-tag color="blue">{{ record.market }}</a-tag>
              <span class="code">{{ record.code }}</span>
            </a-space>
          </template>
          <template v-else-if="column.key === 'name'">
            <a-tooltip v-if="record.lastQuoteError" :title="record.lastQuoteError">
              <span>{{ record.name || '—' }}</span>
              <a-tag color="orange" style="margin-left: 6px">{{ t('stock.quoteError') }}</a-tag>
            </a-tooltip>
            <span v-else>{{ record.name || '—' }}</span>
          </template>
          <template v-else-if="column.key === 'latestPrice'">
            <span :class="['price', changeClass(record.priceChange)]">
              {{ formatPrice(record.latestPrice) }}
            </span>
            <span v-if="record.currency" class="currency">{{ record.currency }}</span>
          </template>
          <template v-else-if="column.key === 'priceChange'">
            <span :class="changeClass(record.priceChange)">{{ formatChange(record.priceChange) }}</span>
          </template>
          <template v-else-if="column.key === 'changePercent'">
            <span :class="changeClass(record.changePercent)">{{ formatPercent(record.changePercent) }}</span>
          </template>
          <template v-else-if="column.key === 'dayHigh'">{{ formatPrice(record.dayHigh) }}</template>
          <template v-else-if="column.key === 'dayLow'">{{ formatPrice(record.dayLow) }}</template>
          <template v-else-if="column.key === 'marketTimeText'">
            {{ record.marketTimeText || t('stock.noQuote') }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-tooltip :title="t('stock.refresh')">
                <a-button
                  size="small"
                  :loading="refreshingId === record.id"
                  @click="handleRefreshOne(record)"
                >
                  <template #icon><ReloadOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip :title="t('stock.viewHistory')">
                <a-button size="small" type="primary" @click="openHistory(record)">
                  <template #icon><AreaChartOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip :title="t('stock.delete')">
                <a-button size="small" danger @click="handleDelete(record)">
                  <template #icon><DeleteOutlined /></template>
                </a-button>
              </a-tooltip>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Add a stock -->
    <a-modal
      v-model:open="addOpen"
      :title="t('stock.add')"
      :confirm-loading="adding"
      :ok-text="t('stock.add')"
      @ok="handleAdd"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('stock.market')">
          <a-select v-model:value="form.market" :options="markets" />
        </a-form-item>
        <a-form-item :label="t('stock.code')" :extra="t('stock.codeHint')">
          <a-input
            v-model:value="form.code"
            :placeholder="t('stock.codePlaceholder')"
            allow-clear
            @press-enter="handleAdd"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Latest quote + price history -->
    <a-drawer
      v-model:open="historyOpen"
      :title="historyStock ? t('stock.historyFor', { name: historyStock.name || historyStock.code }) : t('stock.history')"
      placement="right"
      width="min(760px, 100vw)"
      @after-open-change="(open: boolean) => (open ? (observeCanvas(), draw()) : closeHistory())"
    >
      <template v-if="historyStock">
        <a-descriptions bordered size="small" :column="2" style="margin-bottom: 16px">
          <a-descriptions-item :label="t('stock.latestPrice')">
            <span :class="['price', changeClass(historyStock.priceChange)]">
              {{ formatPrice(historyStock.latestPrice) }}
            </span>
            {{ historyStock.currency || '' }}
          </a-descriptions-item>
          <a-descriptions-item :label="t('stock.changePercent')">
            <span :class="changeClass(historyStock.changePercent)">
              {{ formatChange(historyStock.priceChange) }} / {{ formatPercent(historyStock.changePercent) }}
            </span>
          </a-descriptions-item>
          <a-descriptions-item :label="t('stock.open')">{{ formatPrice(historyStock.openPrice) }}</a-descriptions-item>
          <a-descriptions-item :label="t('stock.previousClose')">{{ formatPrice(historyStock.previousClose) }}</a-descriptions-item>
          <a-descriptions-item :label="t('stock.dayHigh')">{{ formatPrice(historyStock.dayHigh) }}</a-descriptions-item>
          <a-descriptions-item :label="t('stock.dayLow')">{{ formatPrice(historyStock.dayLow) }}</a-descriptions-item>
          <a-descriptions-item :label="t('stock.volume')">{{ formatVolume(historyStock.volume) }}</a-descriptions-item>
          <a-descriptions-item :label="t('stock.marketTime')">{{ historyStock.marketTimeText || t('stock.noQuote') }}</a-descriptions-item>
          <a-descriptions-item :label="t('stock.lastQuoteTime')">{{ formatInstant(historyStock.lastQuoteTime) }}</a-descriptions-item>
          <a-descriptions-item :label="t('stock.quoteProvider')">{{ historyStock.quoteProvider || '—' }}</a-descriptions-item>
          <a-descriptions-item :label="t('stock.addedDate')">{{ formatInstant(historyStock.addedDate) }}</a-descriptions-item>
        </a-descriptions>

        <a-alert
          v-if="historyStock.lastQuoteError"
          type="warning"
          show-icon
          :message="historyStock.lastQuoteError"
          style="margin-bottom: 16px"
        />

        <div class="section-label">{{ t('stock.chart') }}</div>
        <a-spin :spinning="historyLoading">
          <div class="chart-wrap">
            <canvas ref="canvasRef" height="240" class="chart"></canvas>
            <a-empty v-if="!historyLoading && !history.length" :description="t('stock.historyEmpty')" />
          </div>
        </a-spin>

        <div class="section-label">{{ t('stock.table') }}</div>
        <a-table
          :columns="historyColumns"
          :data-source="history"
          :loading="historyLoading"
          :row-key="(r: { id: number }) => r.id"
          :pagination="{ pageSize: 10, showSizeChanger: false }"
          size="small"
          :scroll="{ x: 600 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'recordedDate'">{{ formatInstant(record.recordedDate) }}</template>
            <template v-else-if="column.key === 'price'">
              <span class="price">{{ formatPrice(record.price) }}</span>
            </template>
            <template v-else-if="column.key === 'priceChange'">
              <span :class="changeClass(record.priceChange)">{{ formatChange(record.priceChange) }}</span>
            </template>
            <template v-else-if="column.key === 'changePercent'">
              <span :class="changeClass(record.changePercent)">{{ formatPercent(record.changePercent) }}</span>
            </template>
            <template v-else-if="column.key === 'marketTimeText'">{{ record.marketTimeText || '—' }}</template>
          </template>
        </a-table>
      </template>
    </a-drawer>
  </div>
</template>

<style scoped>
.stock-list-view { display: flex; flex-direction: column; gap: 16px; }
.section-label { font-weight: 600; margin: 16px 0 8px; }
.code { font-family: monospace; }
.price { font-weight: 600; }
.currency { color: var(--ant-color-text-tertiary, rgba(0, 0, 0, 0.45)); font-size: 11px; margin-left: 4px; }
/* Red up, green down — the convention on Chinese exchanges. */
.up { color: #e4393c; }
.down { color: #17a34a; }
.flat { color: inherit; }
.chart-wrap { position: relative; min-height: 60px; }
.chart { width: 100%; display: block; border-radius: 6px; }
</style>
