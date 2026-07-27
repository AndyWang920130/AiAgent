<script lang="ts" setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { HeartOutlined } from '@ant-design/icons-vue'
import { ecgApi, type EcgSummary, type EcgRecord } from '../api/ecg'
import { theme as appTheme } from '../utils/theme'

const { t } = useI18n()

const records = ref<EcgSummary[]>([])
const selectedId = ref<number | null>(null)
const current = ref<EcgRecord | null>(null)
const listLoading = ref(false)
const chartLoading = ref(false)

// Display controls
const gain = ref(10)        // mm per mV (standard ECG = 10 mm/mV)
const speed = ref(25)       // mm per second (standard ECG = 25 mm/s)
const scroll = ref(0)       // start fraction [0,1] of the visible window into the full signal
const PX_PER_MM = 4         // canvas pixels per ECG millimetre

const canvasRef = ref<HTMLCanvasElement | null>(null)
let resizeObserver: ResizeObserver | null = null

const durationSec = computed(() =>
  current.value ? current.value.samples.length / current.value.sampleRate : 0
)

// How many seconds fit across the canvas at the current sweep speed.
function visibleSeconds(canvasWidthPx: number): number {
  const mm = canvasWidthPx / PX_PER_MM
  return mm / speed.value
}

async function loadList() {
  listLoading.value = true
  try {
    records.value = await ecgApi.list()
    if (records.value.length && selectedId.value === null) {
      selectedId.value = records.value[0].id
      await loadRecord(records.value[0].id)
    }
  } catch {
    message.error(t('ecg.loadFailed'))
  } finally {
    listLoading.value = false
  }
}

async function loadRecord(id: number) {
  chartLoading.value = true
  try {
    current.value = await ecgApi.get(id)
    scroll.value = 0
    await nextTick()
    draw()
  } catch {
    message.error(t('ecg.loadFailed'))
  } finally {
    chartLoading.value = false
  }
}

function onSelect(id: number) {
  selectedId.value = id
  loadRecord(id)
}

function draw() {
  const canvas = canvasRef.value
  const rec = current.value
  if (!canvas) return

  // Fit the canvas backing store to its rendered size (crisp lines).
  const rect = canvas.getBoundingClientRect()
  const width = Math.max(1, Math.floor(rect.width))
  const height = canvas.height
  if (canvas.width !== width) canvas.width = width

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const dark = appTheme.value === 'dark'
  ctx.clearRect(0, 0, width, height)
  ctx.fillStyle = dark ? '#1a1414' : '#fff5f5'
  ctx.fillRect(0, 0, width, height)

  drawGrid(ctx, width, height, dark)

  if (!rec || rec.samples.length === 0) return

  // Determine the visible sample window from the scroll position.
  const winSec = visibleSeconds(width)
  const winSamples = Math.min(rec.samples.length, Math.round(winSec * rec.sampleRate))
  const maxStart = Math.max(0, rec.samples.length - winSamples)
  const startIdx = Math.round(scroll.value * maxStart)
  const endIdx = Math.min(rec.samples.length, startIdx + winSamples)

  const midY = height / 2
  const pxPerSample = width / winSamples
  const pxPerMv = gain.value * PX_PER_MM

  ctx.beginPath()
  ctx.strokeStyle = dark ? '#ff6b6b' : '#c0392b'
  ctx.lineWidth = 1.5
  for (let i = startIdx; i < endIdx; i++) {
    const x = (i - startIdx) * pxPerSample
    const y = midY - rec.samples[i] * pxPerMv
    if (i === startIdx) ctx.moveTo(x, y)
    else ctx.lineTo(x, y)
  }
  ctx.stroke()
}

function drawGrid(ctx: CanvasRenderingContext2D, width: number, height: number, dark: boolean) {
  const minor = PX_PER_MM            // 1 mm
  const major = PX_PER_MM * 5        // 5 mm

  ctx.lineWidth = 1
  ctx.strokeStyle = dark ? 'rgba(255,107,107,0.12)' : 'rgba(233,150,150,0.35)'
  ctx.beginPath()
  for (let x = 0; x <= width; x += minor) { ctx.moveTo(x, 0); ctx.lineTo(x, height) }
  for (let y = 0; y <= height; y += minor) { ctx.moveTo(0, y); ctx.lineTo(width, y) }
  ctx.stroke()

  ctx.strokeStyle = dark ? 'rgba(255,107,107,0.3)' : 'rgba(200,80,80,0.5)'
  ctx.beginPath()
  for (let x = 0; x <= width; x += major) { ctx.moveTo(x, 0); ctx.lineTo(x, height) }
  for (let y = 0; y <= height; y += major) { ctx.moveTo(0, y); ctx.lineTo(width, y) }
  ctx.stroke()
}

// Redraw whenever display parameters or theme change.
watch([gain, speed, scroll, appTheme], () => draw())

onMounted(() => {
  loadList()
  if (canvasRef.value) {
    resizeObserver = new ResizeObserver(() => draw())
    resizeObserver.observe(canvasRef.value)
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
})
</script>

<template>
  <div class="ecg-view">
    <a-card :bordered="false">
      <template #title>
        <HeartOutlined style="color: #c0392b; margin-right: 8px" />{{ t('ecg.title') }}
      </template>
      <template #extra>
        <a-select
          :value="selectedId"
          :loading="listLoading"
          style="width: 260px"
          :placeholder="t('ecg.selectRecord')"
          @change="onSelect"
        >
          <a-select-option v-for="r in records" :key="r.id" :value="r.id">
            {{ r.name }}
          </a-select-option>
        </a-select>
      </template>

      <a-descriptions v-if="current" :column="{ xs: 1, sm: 2, lg: 4 }" size="small" bordered style="margin-bottom: 16px">
        <a-descriptions-item :label="t('ecg.lead')">{{ current.leadName }}</a-descriptions-item>
        <a-descriptions-item :label="t('ecg.heartRate')">{{ current.heartRate }} {{ t('ecg.bpm') }}</a-descriptions-item>
        <a-descriptions-item :label="t('ecg.sampleRate')">{{ current.sampleRate }} Hz</a-descriptions-item>
        <a-descriptions-item :label="t('ecg.duration')">{{ durationSec.toFixed(1) }} s</a-descriptions-item>
      </a-descriptions>

      <a-spin :spinning="chartLoading">
        <div class="chart-wrap">
          <canvas ref="canvasRef" height="320" class="ecg-canvas" />
        </div>
      </a-spin>

      <a-row :gutter="24" style="margin-top: 16px">
        <a-col :xs="24" :md="8">
          <div class="ctrl-label">{{ t('ecg.gain') }}: {{ gain }} mm/mV</div>
          <a-slider v-model:value="gain" :min="5" :max="20" :step="1" />
        </a-col>
        <a-col :xs="24" :md="8">
          <div class="ctrl-label">{{ t('ecg.speed') }}: {{ speed }} mm/s</div>
          <a-slider v-model:value="speed" :min="12.5" :max="50" :step="12.5" />
        </a-col>
        <a-col :xs="24" :md="8">
          <div class="ctrl-label">{{ t('ecg.scroll') }}</div>
          <a-slider v-model:value="scroll" :min="0" :max="1" :step="0.01" :tip-formatter="null" />
        </a-col>
      </a-row>
    </a-card>
  </div>
</template>

<style scoped>
.ecg-view { display: flex; flex-direction: column; gap: 16px; }
.chart-wrap { width: 100%; border: 1px solid #f0d5d5; border-radius: 8px; overflow: hidden; }
.ecg-canvas { display: block; width: 100%; height: 320px; }
.ctrl-label { font-size: 13px; color: #888; margin-bottom: 4px; }
</style>
