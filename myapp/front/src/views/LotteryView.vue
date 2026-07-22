<script lang="ts" setup>
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { GiftOutlined } from '@ant-design/icons-vue'
import { gamePrizes, spinDurationSeconds, fetchGameConfig } from '../stores/gameConfig'
import LotteryWheel from '../components/LotteryWheel.vue'

const { t } = useI18n()

onMounted(() => {
  if (!gamePrizes.value.length) fetchGameConfig()
})

const prizes = computed(() => gamePrizes.value.map(p => ({ label: p.name })))
</script>

<template>
  <div class="lottery-view">
    <a-card :bordered="false">
      <template #title><GiftOutlined /> {{ t('lottery.title') }}</template>
      <LotteryWheel :prizes="prizes" :spin-duration-seconds="spinDurationSeconds" />
    </a-card>
  </div>
</template>

<style scoped>
.lottery-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
