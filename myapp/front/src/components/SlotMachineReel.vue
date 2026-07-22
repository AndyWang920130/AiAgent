<script lang="ts" setup>
import { ref, computed, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  prizes: { label: string }[]
  spinDurationSeconds: number
}>()

const { t } = useI18n()

const ITEM_HEIGHT = 56
const VISIBLE_ROWS = 5
const CENTER_ROW = Math.floor(VISIBLE_ROWS / 2)
const EXTRA_LOOPS = 4

const stripItems = computed(() => {
  const count = props.prizes.length
  if (!count) return []
  const totalRows = count * (EXTRA_LOOPS + 3)
  return Array.from({ length: totalRows }, (_, i) => props.prizes[i % count])
})

const spinning = ref(false)
const transitionEnabled = ref(false)
const offset = ref(0)
const result = ref<string | null>(null)

function spin() {
  if (spinning.value || !props.prizes.length) return
  spinning.value = true
  result.value = null

  const count = props.prizes.length
  const winnerIndex = Math.floor(Math.random() * count)
  const targetAbsoluteIndex = EXTRA_LOOPS * count + winnerIndex

  transitionEnabled.value = false
  offset.value = 0

  nextTick(() => {
    transitionEnabled.value = true
    offset.value = -(targetAbsoluteIndex - CENTER_ROW) * ITEM_HEIGHT
  })

  setTimeout(() => {
    spinning.value = false
    result.value = props.prizes[winnerIndex].label
  }, props.spinDurationSeconds * 1000)
}
</script>

<template>
  <div class="slot-reel">
    <div class="reel-window" :style="{ height: `${VISIBLE_ROWS * ITEM_HEIGHT}px` }">
      <div
        class="reel-strip"
        :style="{
          transform: `translateY(${offset}px)`,
          transitionDuration: transitionEnabled ? `${spinDurationSeconds}s` : '0s',
        }"
      >
        <div
          v-for="(item, i) in stripItems"
          :key="i"
          class="reel-row"
          :style="{ height: `${ITEM_HEIGHT}px` }"
        >
          {{ item.label }}
        </div>
      </div>
      <div class="top-fade" />
      <div class="bottom-fade" />
      <div class="center-highlight" :style="{ top: `${CENTER_ROW * ITEM_HEIGHT}px`, height: `${ITEM_HEIGHT}px` }">
        <span class="arrow arrow-left">▶︎</span>
        <span class="arrow arrow-right">◀︎</span>
      </div>
    </div>

    <a-empty v-if="!prizes.length" :description="t('lottery.noPrizes')" />

    <div class="actions">
      <a-button type="primary" size="large" :loading="spinning" :disabled="!prizes.length" @click="spin">
        {{ spinning ? t('lottery.spinning') : t('lottery.spin') }}
      </a-button>
    </div>

    <a-alert
      v-if="result"
      type="success"
      show-icon
      class="result-alert"
      :message="t('lottery.won', { prize: result })"
    />
  </div>
</template>

<style scoped>
.slot-reel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.reel-window {
  position: relative;
  width: 280px;
  margin: 24px auto;
  overflow: hidden;
  border-radius: 12px;
  background: #1f2937;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}
.reel-strip {
  transition-property: transform;
  transition-timing-function: cubic-bezier(0.17, 0.67, 0.12, 0.99);
}
.reel-row {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #f8fafc;
  font-size: 16px;
  font-weight: 600;
  text-align: center;
  padding: 0 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.top-fade,
.bottom-fade {
  position: absolute;
  left: 0;
  right: 0;
  height: 70px;
  pointer-events: none;
  z-index: 2;
}
.top-fade {
  top: 0;
  background: linear-gradient(to bottom, #1f2937, transparent);
}
.bottom-fade {
  bottom: 0;
  background: linear-gradient(to top, #1f2937, transparent);
}
.center-highlight {
  position: absolute;
  left: 0;
  right: 0;
  border-top: 2px solid #faad14;
  border-bottom: 2px solid #faad14;
  background: rgba(250, 173, 20, 0.08);
  pointer-events: none;
  z-index: 3;
}
.arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  color: #faad14;
  font-size: 16px;
}
.arrow-left {
  left: 8px;
}
.arrow-right {
  right: 8px;
}
.actions {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}
.result-alert {
  max-width: 360px;
  margin: 0 auto;
}
</style>
