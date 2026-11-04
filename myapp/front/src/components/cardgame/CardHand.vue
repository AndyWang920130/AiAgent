<script lang="ts" setup>
import type { Card } from '../../api/cardGame'
import PlayingCard from './PlayingCard.vue'

const props = withDefaults(
  defineProps<{
    cards: Card[]
    /** Indices of the selected cards. Indices, not card codes: with two packs a hand can hold
        two identical cards, and they must be selectable independently. */
    selected: number[]
    disabled?: boolean
  }>(),
  { disabled: false }
)

const emit = defineEmits<{ toggle: [index: number] }>()

function onCardClick(index: number) {
  if (props.disabled) return
  emit('toggle', index)
}
</script>

<template>
  <div class="card-hand" :class="{ 'is-disabled': disabled }">
    <div
      v-for="(card, index) in cards"
      :key="`${card}-${index}`"
      class="card-slot"
      @click="onCardClick(index)"
    >
      <PlayingCard :card="card" :selected="selected.includes(index)" :disabled="disabled" />
    </div>
    <span v-if="!cards.length" class="empty">—</span>
  </div>
</template>

<style scoped>
.card-hand {
  display: flex;
  align-items: flex-end;
  /* Room above for the lift a selected card makes, and below for the shadow. */
  padding: 16px 0 6px;
  min-height: 92px;
}

.card-slot {
  cursor: pointer;
}

/* Cards overlap so a six-card hand fits a narrow screen; the last one shows in full. */
.card-slot + .card-slot {
  margin-inline-start: -14px;
}

.card-hand.is-disabled .card-slot {
  cursor: default;
}

.empty {
  color: rgba(0, 0, 0, 0.35);
}

@media (max-width: 768px) {
  .card-slot + .card-slot {
    margin-inline-start: -20px;
  }
}
</style>
