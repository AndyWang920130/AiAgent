<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  prizes: { label: string; color: string }[]
  spinDurationSeconds: number
}>()

const { t } = useI18n()

const segmentAngle = computed(() => 360 / (props.prizes.length || 1))

const wheelBackground = computed(() => {
  if (!props.prizes.length) return '#e5e7eb'
  const seg = segmentAngle.value
  const stops = props.prizes.map((p, i) => `${p.color} ${i * seg}deg ${(i + 1) * seg}deg`).join(', ')
  return `conic-gradient(${stops})`
})

const labelWidth = computed(() => (props.prizes.length > 10 ? 60 : 90))
const labelFontSize = computed(() => (props.prizes.length > 10 ? '10px' : '12px'))

const spinning = ref(false)
const rotation = ref(0)
const result = ref<string | null>(null)

function spin() {
  if (spinning.value || !props.prizes.length) return
  spinning.value = true
  result.value = null
  const seg = segmentAngle.value
  const index = Math.floor(Math.random() * props.prizes.length)
  const desiredMod = (360 - index * seg - seg / 2) % 360
  const currentMod = ((rotation.value % 360) + 360) % 360
  let delta = desiredMod - currentMod
  if (delta <= 0) delta += 360
  rotation.value += 360 * 5 + delta
  setTimeout(() => {
    spinning.value = false
    result.value = props.prizes[index].label
  }, props.spinDurationSeconds * 1000)
}
</script>

<template>
  <div class="lottery-wheel">
    <div class="wheel-wrap">
      <div class="pointer">▼</div>
      <div
        class="wheel"
        :style="{
          background: wheelBackground,
          transform: `rotate(${rotation}deg)`,
          transitionDuration: `${spinDurationSeconds}s`,
        }"
      >
        <span
          v-for="(p, i) in prizes"
          :key="i"
          class="segment-label"
          :style="{
            width: `${labelWidth}px`,
            fontSize: labelFontSize,
            transform: `translate(-50%, -50%) rotate(${i * segmentAngle + segmentAngle / 2}deg) translate(0, -95px)`,
          }"
        >
          {{ p.label }}
        </span>
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
.lottery-wheel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.wheel-wrap {
  position: relative;
  width: 260px;
  height: 260px;
  margin: 24px auto;
}
.wheel {
  width: 260px;
  height: 260px;
  border-radius: 50%;
  position: relative;
  border: 6px solid #fff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  transition: transform 4s cubic-bezier(0.17, 0.67, 0.12, 0.99);
}
.segment-label {
  position: absolute;
  top: 50%;
  left: 50%;
  text-align: center;
  color: #fff;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.4);
}
.pointer {
  position: absolute;
  top: -14px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 24px;
  color: #ff4d4f;
  z-index: 2;
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
