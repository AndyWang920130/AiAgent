<script lang="ts" setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { RobotOutlined, TrophyOutlined, UserOutlined } from '@ant-design/icons-vue'
import type { CardGamePlayer, CardGameTrickEntry } from '../../api/cardGame'
import PlayingCard from './PlayingCard.vue'

const props = withDefaults(
  defineProps<{
    player: CardGamePlayer
    /** Highlights the seat whose turn it is. */
    active?: boolean
    /** Marks the seat holding the play currently on the table. */
    leading?: boolean
    /** Shown while the table is still filling, where card counts are meaningless. */
    lobby?: boolean
    /**
     * This seat's most recent action in the trick being shown, displayed under the seat so the
     * cards sit with the player who put them down. Null before they have acted this trick.
     */
    played?: CardGameTrickEntry | null
    /**
     * Final placing once the game is over, so the standings are read off the table itself
     * rather than from a separate list. Null while the game is still running.
     */
    place?: number | null
    /** The player the server named the winner. Not always the top score — a tie goes to whoever went out first. */
    winner?: boolean
    /** Emptied their hand first, collecting the point cards left in the other hands. */
    firstOut?: boolean
    /**
     * The viewer's own seat. Only affects phone layout, where it collapses to a single line:
     * the cards it would show beneath it are already in the hand directly below, so on a
     * screen that cannot fit everything it is the one seat that can give up the space.
     */
    self?: boolean
  }>(),
  {
    active: false,
    leading: false,
    lobby: false,
    played: null,
    place: null,
    winner: false,
    firstOut: false,
    self: false,
  }
)

const { t } = useI18n()

const statusTag = computed(() => {
  if (props.lobby) {
    if (props.player.invitationStatus === 'PENDING') return { color: 'orange', text: t('cardGame.invitePending') }
    if (props.player.invitationStatus === 'DECLINED') return { color: 'red', text: t('cardGame.inviteDeclined') }
    return { color: 'green', text: t('cardGame.inviteAccepted') }
  }
  if (props.player.out) return { color: 'blue', text: t('cardGame.outOfCards') }
  if (props.player.passed) return { color: 'default', text: t('cardGame.passed') }
  return null
})
</script>

<template>
  <div
    class="player-seat"
    :class="{
      'is-active': active,
      'is-leading': leading,
      'is-winner': winner,
      'is-lobby': lobby,
      'is-self': self,
    }"
  >
    <div class="seat-main">
      <!-- Final placing, in front of the avatar so the order reads down a column of seats. -->
      <span v-if="place !== null" class="seat-place">{{ place }}</span>
      <a-avatar :size="36" :src="player.avatar || undefined">
        <template #icon>
          <RobotOutlined v-if="player.cpu" />
          <UserOutlined v-else />
        </template>
      </a-avatar>
      <div class="seat-body">
        <div class="seat-name">
          <TrophyOutlined v-if="winner" class="seat-trophy" />
          <span class="name">{{ player.name }}</span>
          <a-tag v-if="player.cpu" color="purple">{{ t('cardGame.cpu') }}</a-tag>
        </div>
        <div class="seat-meta">
          <span v-if="!lobby">{{ t('cardGame.cardsHeld', { count: player.handCount }) }}</span>
          <span v-if="!lobby" class="score">{{ t('cardGame.scoreValue', { score: player.score }) }}</span>
          <a-tag v-if="statusTag" :color="statusTag.color">{{ statusTag.text }}</a-tag>
          <!-- Not a CPU seat, which plays itself anyway: a human who handed over their turns. -->
          <a-tag v-if="player.autoPlay && !player.cpu" color="cyan">{{ t('cardGame.autoPlayTag') }}</a-tag>
          <a-tag v-if="firstOut" color="green">{{ t('cardGame.firstOut') }}</a-tag>
        </div>
      </div>
    </div>

    <!-- What this seat last did in the trick. Kept under the seat rather than only in the
         middle of the table, so a play reads as belonging to the player who made it.

         Rendered for the whole game, empty or not: the cards arrive and are cleared every
         trick, and a row that came and went with them would resize every seat at the table
         several times a trick. The space is held from the start instead. -->
    <div v-if="!lobby" class="seat-played">
      <template v-if="played">
        <span v-if="played.passed" class="played-pass">{{ t('cardGame.passed') }}</span>
        <template v-else>
          <PlayingCard v-for="(c, i) in played.cards" :key="`${c}-${i}`" :card="c" small />
        </template>
      </template>
    </div>
  </div>
</template>

<style scoped>
/* A column now: the seat's own row, with that seat's played cards beneath it.

   Every part of this is a fixed size during play. A seat's content changes constantly —
   tags appear, the card count shrinks, five cards land under it — and at the table those
   seats sit in a row above the trick area, so anything that resizes a seat moves the middle
   of the table with it. Sizes live in custom properties so the view can read the same
   numbers when it reserves the band these sit in. */
.player-seat {
  --seat-width: 228px;
  --seat-main-height: 46px;
  --seat-played-height: 60px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fff;
  transition: border-color 0.15s, box-shadow 0.15s;
}

/* The table: a fixed box. The lobby keeps the width for a tidy row of seats but is left to
   its natural height, since it shows neither card counts nor played cards and a fixed
   play-time height would be mostly empty there. */
.player-seat:not(.is-lobby) {
  width: var(--seat-width);
  flex: 0 0 var(--seat-width);
  /* 8px top + main + 8px gap + played + 8px bottom, matching the rule above. */
  height: calc(var(--seat-main-height) + var(--seat-played-height) + 24px);
  /* A clip rather than a scrollbar: if some unusually long name or an unexpected extra tag
     ever overran the box, losing the tail of it is better than the whole table shifting. */
  overflow: hidden;
}

.player-seat.is-lobby {
  width: var(--seat-width);
  flex: 0 0 var(--seat-width);
}

.seat-main {
  display: flex;
  align-items: center;
  gap: 10px;
  /* Two lines of meta fit here, which is the worst case: a count, a score and up to three
     tags. Fixed so a seat that picks up a tag mid-game does not grow. */
  height: var(--seat-main-height);
}

/* The seat to act is outlined, so whose turn it is reads without hunting for text. */
.player-seat.is-active {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.15);
}

.player-seat.is-leading {
  background: #f6ffed;
}

/* The winner's seat once the game is over. Last rule of the three, so it wins over
   .is-leading when the winner also happens to hold the final play. */
.player-seat.is-winner {
  background: #fffbe6;
  border-color: #ffe58f;
}

.seat-place {
  flex: 0 0 auto;
  min-width: 18px;
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.45);
  font-variant-numeric: tabular-nums;
}

.seat-trophy {
  color: #d48806;
}

.seat-body {
  min-width: 0;
  flex: 1;
}

.seat-name {
  display: flex;
  align-items: center;
  gap: 6px;
}

.name {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.seat-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.55);
}

.score {
  font-variant-numeric: tabular-nums;
}

/* Cards overlap a little so a five-card play still fits a narrow seat. The row keeps its
   height whether it holds cards, a pass marker or nothing at all, which is what stops seats
   jumping as the trick runs — a five-card play is 38px of card plus the dashed rule. */
.seat-played {
  display: flex;
  align-items: center;
  gap: 2px;
  height: var(--seat-played-height);
  padding-top: 6px;
  border-top: 1px dashed #f0f0f0;
  overflow: hidden;
}

.seat-played .playing-card + .playing-card {
  margin-inline-start: -12px;
}

.played-pass {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
}

/* Phones. The seat keeps its fixed-box behaviour — that is what stops the table shifting —
   but every number in it comes down, because the whole table has to fit a screen without
   scrolling. Narrower, tighter padding, smaller type, and a shorter played row off the
   smaller cards. */
@media (max-width: 768px) {
  .player-seat {
    --seat-width: 152px;
    --seat-main-height: 48px;
    /* 4px rule + 30x42 card from PlayingCard.vue's phone sizes. */
    --seat-played-height: 38px;
    gap: 4px;
    padding: 6px 8px;
  }
  .player-seat:not(.is-lobby) {
    /* 6px top + main + 4px gap + played + 6px bottom. */
    height: calc(var(--seat-main-height) + var(--seat-played-height) + 16px);
  }
  .seat-main {
    gap: 6px;
  }
  .seat-name {
    gap: 4px;
    font-size: 13px;
  }
  .seat-meta {
    gap: 4px;
    font-size: 11px;
  }
  /* The tags carry a lot of padding for their text at this size. */
  .seat-meta :deep(.ant-tag) {
    margin-inline-end: 0;
    padding-inline: 4px;
    font-size: 10px;
    line-height: 16px;
  }
  .seat-name :deep(.ant-tag) {
    margin-inline-end: 0;
    padding-inline: 4px;
    font-size: 10px;
    line-height: 16px;
  }
  .seat-played {
    padding-top: 4px;
  }
  .seat-played .playing-card + .playing-card {
    margin-inline-start: -10px;
  }

  /* Your own seat, which sits directly above your hand: one line, no played row. Those cards
     are the ones you just put down out of the hand immediately below it, and on a phone that
     duplicate is the cheapest thing on the table to give up. */
  .player-seat.is-self {
    --seat-width: 100%;
    --seat-main-height: 36px;
  }
  .player-seat.is-self:not(.is-lobby) {
    height: calc(var(--seat-main-height) + 12px);
  }
  .player-seat.is-self .seat-played {
    display: none;
  }
}
</style>
