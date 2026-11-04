<script lang="ts" setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Card } from '../../api/cardGame'
import { isJoker, isBigJoker, isRed, pointsOf, rankLabel, suitSymbol } from '../../utils/cards'

const props = withDefaults(
  defineProps<{
    card: Card
    selected?: boolean
    disabled?: boolean
    /** Compact rendering for opponents' played cards, where there is less room. */
    small?: boolean
  }>(),
  { selected: false, disabled: false, small: false }
)

const { t } = useI18n()

const joker = computed(() => isJoker(props.card))
// Jokers are named, not numbered, so they read as "Joker" rather than a rank character.
const label = computed(() => (joker.value ? t('cardGame.joker') : rankLabel(props.card)))
const suit = computed(() => suitSymbol(props.card))
const red = computed(() => isRed(props.card))
// Point cards get a marker so players can see what a trick is worth at a glance.
const points = computed(() => pointsOf(props.card))
// Big and little jokers rank the same; the mark is shown only to tell two cards apart.
const jokerMark = computed(() => t(isBigJoker(props.card) ? 'cardGame.jokerBig' : 'cardGame.jokerLittle'))
</script>

<template>
  <div
    class="playing-card"
    :class="{ 'is-selected': selected, 'is-disabled': disabled, 'is-red': red, 'is-small': small }"
  >
    <span class="rank" :class="{ 'is-word': joker }">{{ label }}</span>
    <span v-if="joker" class="joker-mark">{{ jokerMark }}</span>
    <span v-else class="suit">{{ suit }}</span>
    <span v-if="points" class="points">{{ points }}</span>
  </div>
</template>

<style scoped>
/* Size comes from a custom property so the hand and the trick area can scale the same card
   component differently, and so a phone can shrink every card at once. */
.playing-card {
  --card-width: 48px;
  --card-height: 68px;
  position: relative;
  width: var(--card-width);
  height: var(--card-height);
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
  color: #262626;
  font-weight: 600;
  line-height: 1;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
  transition: transform 0.15s, box-shadow 0.15s, border-color 0.15s;
  user-select: none;
}

.playing-card.is-small {
  --card-width: 38px;
  --card-height: 54px;
}

.playing-card.is-red {
  color: #cf1322;
}

/* A selected card lifts out of the hand, which reads clearly even on overlapping cards. */
.playing-card.is-selected {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.2);
  transform: translateY(-12px);
}

.playing-card.is-disabled {
  opacity: 0.55;
}

.rank {
  font-size: 18px;
}

.playing-card.is-small .rank {
  font-size: 15px;
}

/* "Joker" is a word, not a rank character, so it needs to be shrunk to fit the card width.
   A single-character translation (zh "王") stays legible at this size. */
.rank.is-word {
  font-size: 11px;
  letter-spacing: -0.3px;
}

.playing-card.is-small .rank.is-word {
  font-size: 9px;
}

.suit,
.joker-mark {
  font-size: 14px;
}

.joker-mark {
  font-size: 10px;
  opacity: 0.6;
}

/* Point value, tucked into the corner. */
.points {
  position: absolute;
  top: 2px;
  right: 3px;
  font-size: 9px;
  font-weight: 700;
  color: #d48806;
}
</style>
