<script lang="ts" setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { CheckOutlined, RobotOutlined, TrophyOutlined, UserOutlined } from '@ant-design/icons-vue'
import type { CardGamePlayer, CardGameTrickEntry } from '../../api/cardGame'
import PlayingCard from './PlayingCard.vue'

const props = withDefaults(
  defineProps<{
    player: CardGamePlayer
    /** Highlights the seat whose turn it is. */
    active?: boolean
    /**
     * Marks the seat holding the play currently on the table. The view passes this only while
     * the game is running: a game closed by the round cap is scored mid-trick, so the last
     * play's owner is still recorded and the highlight would outlive the game it belonged to.
     */
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
    /**
     * The game has finished. The seat then shows the result only — placing, name, cards left
     * and score — and drops everything that described how the game was being played: whose
     * turn it was, who had passed, who had handed their turns to the server, and the cards
     * from the last trick. None of that is an outcome, and five seats' worth of it buries the
     * two numbers a player actually wants to read at that point.
     */
    over?: boolean
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
    over: false,
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
  // Both of these describe a seat's state in the running game, and both are read off the card
  // count once it is over: empty hand, nothing left to pass on.
  if (props.over) return null
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
          <!-- Not a CPU seat, which plays itself anyway: a human who handed over their turns.
               Gone once the game is over, where how a seat was played is no longer news. -->
          <a-tag v-if="player.autoPlay && !player.cpu && !over" color="cyan">
            {{ t('cardGame.autoPlayTag') }}
          </a-tag>
          <!-- This seat wants the next game. Needs no guard of its own: the server reports it
               only inside the rematch window, and false everywhere else, so the tick appears
               with the question and goes with it.

               Here rather than only in the hint sentence under the hand, which names who is
               still being waited on and leaves you to match names to seats. On five seats that
               is a lookup; on the seat it is a glance. The sentence stays, because this row
               scrolls sideways on a phone and a tick can be off-screen.

               A tick, not a hand going up. This is a state the seat is in, not a moment: the
               table polls at 1500ms, so several seats answering inside one window would all
               animate at once and read as a glitch. It also has no resolution to play towards
               — the last ready flag deals, so no client ever draws a table of ticks.

               CPU seats get one too. They have nothing to decide, and a seat left blank would
               read as a seat being waited on. -->
          <Transition name="seat-ready">
            <a-tag v-if="player.ready" color="green">
              <CheckOutlined />
              {{ t('cardGame.readyTag') }}
            </a-tag>
          </Transition>
        </div>
      </div>
    </div>

    <!-- What this seat last did in the trick, and what it is saying. Both under the seat rather
         than only in the middle of the table, so a play — and a phrase — reads as belonging to
         the player it came from.

         Rendered for the whole game, empty or not: the cards arrive and are cleared every
         trick, and a row that came and went with them would resize every seat at the table
         several times a trick. The space is held from the start instead.

         Dropped at game over. It is the last trick's cards, which is a step in a game that
         has finished rather than part of its result, and the row the seat gives up is the
         largest part of it. -->
    <div v-if="!lobby && !over" class="seat-played">
      <!-- The phrase takes this row while it is showing, rather than getting a row of its own.

           Not a preference. During play the seat is a fixed box that clips (see the height
           rules below) and the seat band above it scrolls sideways, and an overflow that is
           not `visible` on one axis forces the other to `auto` — so a bubble floating above
           the seat is clipped twice over and there is no CSS that lets it out. Reserving
           another row instead would mean a seventh pixel height that has to agree with the
           six already spread across this file and the view's three short-screen tiers, and
           that arithmetic is what put a scrollbar on this table twice.

           This row is already the one part of the seat whose content is interchangeable: a
           play, a pass marker, or nothing, at one height throughout. A phrase is a fourth
           thing it can hold. The cost is that the seat's last play is hidden for the few
           seconds the phrase shows — affordable because those cards are not the only copy of
           themselves: the play on the table is in the trick area and the raise history names
           the seat behind each play. The phrase is also the one thing here a player asked to
           see.

           No status guard. The server reports `phrase` only while a phrase is live and null
           otherwise, so a second condition here would be a copy of that rule able to disagree
           with it. -->
      <Transition name="seat-phrase">
        <div v-if="player.phrase" class="seat-phrase">
          {{ t(`cardGame.phrases.${player.phrase}`) }}
        </div>
      </Transition>
      <template v-if="played && !player.phrase">
        <span v-if="played.passed" class="played-pass">{{ t('cardGame.passed') }}</span>
        <template v-else>
          <PlayingCard v-for="(c, i) in played.cards" :key="`${c}-${i}`" :card="c" small />
        </template>
      </template>
    </div>

    <!-- Between games the seat has no played row to lend, so the phrase gets one of its own.
         Free here, unlike during play: at game over the seat is `height: auto` and the band
         around it is `overflow: visible`, so the seat simply grows by the bubble and no fixed
         height has to be revised. This is also where phrases matter most — "good game" belongs
         to the moment a game ends. -->
    <Transition name="seat-phrase">
      <div v-if="over && player.phrase" class="seat-phrase is-standalone">
        {{ t(`cardGame.phrases.${player.phrase}`) }}
      </div>
    </Transition>
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

/* The winner's seat once the game is over. Last rule of the three so it wins the cascade,
   though .is-leading is no longer passed at game over for the seat to contend with. */
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

/* The ready tick fades in rather than appearing between two frames, which at a 1500ms poll is
   the difference between a seat answering and a seat glitching. Opacity only: the tag takes its
   space immediately, so the other tags in the row do not shift as it arrives. Short enough not
   to still be running when the next poll lands. */
.seat-ready-enter-active,
.seat-ready-leave-active {
  transition: opacity 0.15s ease;
}

.seat-ready-enter-from,
.seat-ready-leave-to {
  opacity: 0;
}

/* The phrase bubble. Sized by its text and clipped by the row it sits in, so it cannot change
   the seat's height whatever is in it — the longest phrase in either language is well inside a
   seat's width, and a clip is the backstop rather than the plan.

   Looks like speech without being positioned like it: the tail-less rounded box reads as a
   bubble beside the player's name, and nothing about it has to escape the seat to say so. */
.seat-phrase {
  max-width: 100%;
  padding: 3px 8px;
  border-radius: 10px;
  background: #e6f4ff;
  border: 1px solid #91caff;
  color: #0958d9;
  font-size: 12px;
  line-height: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Between games, where the bubble is its own row rather than a borrower of the played row. */
.seat-phrase.is-standalone {
  align-self: flex-start;
}

/* Fades both ways, like the ready tick, and opacity only for the same reason: the row the
   bubble shares is a fixed height during play, and animating anything that takes space would
   move the cards it is covering on the way in and on the way out. Slightly longer than the
   tick's 0.15s — this one replaces what was in the row rather than joining a row of tags, so
   the swap reads better a touch slower. Still well inside the 1500ms poll. */
.seat-phrase-enter-active,
.seat-phrase-leave-active {
  transition: opacity 0.2s ease;
}

.seat-phrase-enter-from,
.seat-phrase-leave-to {
  opacity: 0;
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
