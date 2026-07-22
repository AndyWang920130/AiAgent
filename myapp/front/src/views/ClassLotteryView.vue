<script lang="ts" setup>
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { TeamOutlined } from '@ant-design/icons-vue'
import { classPrizes, spinDurationSeconds, fetchGameConfig } from '../stores/gameConfig'
import SlotMachineReel from '../components/SlotMachineReel.vue'

const { t } = useI18n()

onMounted(() => {
  if (!classPrizes.value.length) fetchGameConfig()
})

const prizes = computed(() => classPrizes.value.map(p => ({ label: p.name })))
</script>

<template>
  <div class="class-lottery-view">
    <a-card :bordered="false">
      <template #title><TeamOutlined /> {{ t('classLottery.title') }}</template>
      <SlotMachineReel :prizes="prizes" :spin-duration-seconds="spinDurationSeconds" />
    </a-card>
  </div>
</template>

<style scoped>
.class-lottery-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
