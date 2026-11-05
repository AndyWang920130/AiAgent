<script lang="ts" setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { ArrowLeftOutlined, TrophyOutlined } from '@ant-design/icons-vue'
import {
  cardGameApi,
  type CardGameOpponent,
  type CardGameState,
  type CardGameTrickEntry,
} from '../api/cardGame'
import { canPlay, rankOfPlay, totalPoints } from '../utils/cards'
import { useImmersive } from '../utils/immersive'
import { useFitScale } from '../utils/fitScale'
import CardHand from '../components/cardgame/CardHand.vue'
import PlayerSeat from '../components/cardgame/PlayerSeat.vue'
import PlayingCard from '../components/cardgame/PlayingCard.vue'

const { t } = useI18n()
const router = useRouter()

/* ----- Lobby state --------------------------------------------------------- */

const opponents = ref<CardGameOpponent[]>([])
const selectedInvitees = ref<string[]>([])
const cpuCount = ref(1)
const incoming = ref<CardGameState[]>([])
const outgoing = ref<CardGameState[]>([])
const creating = ref(false)
const actionLoading = ref(false)

/* ----- Game state ---------------------------------------------------------- */

const game = ref<CardGameState | null>(null)
const selectedIndices = ref<number[]>([])
const playing = ref(false)
const autoPlaySaving = ref(false)

/** Countdown shown between polls, ticked locally once a second. */
const turnRemaining = ref<number | null>(null)

let lobbyTimer: ReturnType<typeof setInterval> | null = null
let gameTimer: ReturnType<typeof setInterval> | null = null
let tickTimer: ReturnType<typeof setInterval> | null = null

/* ----- Derived ------------------------------------------------------------- */

/** Total seats the host is about to fill: themselves, their invitees and the CPUs. */
const plannedSeats = computed(() => 1 + selectedInvitees.value.length + cpuCount.value)
const seatsValid = computed(() => plannedSeats.value >= 2 && plannedSeats.value <= 5)

/** The most CPUs that still leave the table within the five-seat limit. */
const maxCpu = computed(() => Math.max(0, 5 - 1 - selectedInvitees.value.length))

const inGame = computed(() => !!game.value && game.value.status === 'ACTIVE')
const gameOver = computed(
  () => !!game.value && game.value.status !== 'ACTIVE' && game.value.status !== 'PENDING'
)

/** The table the host set up, still waiting on invitees. */
const waitingTable = computed(() => (game.value?.status === 'PENDING' ? game.value : null))

/* The table claims the whole window, so nothing about the game needs scrolling to.
   Only once there is a table to show: the lobby is a list of invitations that can be longer
   than the screen, and a waiting table is a small box that would just be stretched. The
   result state keeps it, since it is drawn in the same box the game was played in. */
const immersive = useImmersive()
watch([inGame, gameOver], ([playing, over]) => immersive.set(playing || over), {
  immediate: true,
})

/* The table is a fixed-size board scaled to whatever room the window actually has, rather
   than a stack of bands whose heights are supposed to add up to the viewport. The sum was
   the previous approach and it was off: it has to agree with the card's title row, the
   layout header, two layers of padding, the browser font size and the zoom level.

   fitBox is the measured space; tableEl keeps its natural height inside it. */
const fitBox = ref<HTMLElement | null>(null)
const tableEl = ref<HTMLElement | null>(null)
const { scale: fitScale } = useFitScale(fitBox, tableEl)

/* transform-origin: top centre — the table shrinks towards the top of the space rather than
   away from its own middle, which keeps the seat row in the same place as the scale changes.
   Width is divided back out so the board still fills the space sideways at any scale. */
const tableStyle = computed(() =>
  fitScale.value < 1
    ? {
        transform: `scale(${fitScale.value})`,
        transformOrigin: 'top center',
        width: `${100 / fitScale.value}%`,
      }
    : undefined,
)

const myHand = computed(() => game.value?.myHand ?? [])
const selectedCards = computed(() => selectedIndices.value.map(i => myHand.value[i]).filter(Boolean))

/** Rank of the play on the table, recomputed from its cards (the server sends the cards). */
const currentRank = computed(() => rankOfPlay(game.value?.currentPlay ?? null))

/**
 * Whether the selection can be played. The same rules the server enforces, duplicated here
 * only so the button can disable itself — the server stays the authority.
 */
const selectionPlayable = computed(() => {
  if (!game.value?.myTurn || !selectedCards.value.length) return false
  return canPlay(selectedCards.value, game.value.currentShape, currentRank.value)
})

/** Leading a fresh trick: a play is required, so Pass is not offered. */
const mustLead = computed(() => !!game.value && game.value.myTurn && !game.value.currentPlay)

const me = computed(() => game.value?.players.find(p => p.username === game.value?.myUsername) ?? null)
const others = computed(() =>
  (game.value?.players ?? []).filter(p => p.username !== game.value?.myUsername)
)

/** The player the server declared the winner, or null if the game ended without one. */
const winner = computed(() => {
  const g = game.value
  if (!g?.winnerUsername) return null
  return g.players.find(p => p.username === g.winnerUsername) ?? null
})

/** The player who emptied their hand first, who collects the others' leftover points. */
const firstOut = computed(() => {
  const g = game.value
  if (!g?.firstOutUsername) return null
  return g.players.find(p => p.username === g.firstOutUsername) ?? null
})

/**
 * Final standings, highest score first, each with its placing. Equal scores share a placing and
 * the next one skips accordingly (1, 2, 2, 4), so a tie is not shown as an order the scores do
 * not support. The winner is whoever the server named, which is not always the top row: a tie on
 * score is broken by who went out first.
 */
const standings = computed(() => {
  const sorted = [...(game.value?.players ?? [])].sort((a, b) => b.score - a.score)
  let place = 0
  let previousScore: number | null = null
  return sorted.map((player, index) => {
    if (previousScore === null || player.score !== previousScore) {
      place = index + 1
      previousScore = player.score
    }
    return { player, place }
  })
})

/**
 * Placing by username, for the badge on each seat. The seats keep their own order — seat order
 * is what the table is laid out in — so the standing is carried to the seat rather than the
 * seats being reordered into it.
 *
 * Empty until the game is over, which is what keeps the badge off the seats mid-game.
 */
const placeByUsername = computed(() => {
  const places = new Map<string, number>()
  if (!gameOver.value) return places
  for (const row of standings.value) {
    places.set(row.player.username, row.place)
  }
  return places
})

/** Whether the server is playing my seat for me. */
const autoPlayOn = computed(() => me.value?.autoPlay ?? false)

const shapeLabel = computed(() => {
  const shape = game.value?.currentShape
  return shape ? t(`cardGame.shapes.${shape}`) : ''
})

const selectionLabel = computed(() => {
  if (!selectedCards.value.length) return ''
  const points = totalPoints(selectedCards.value)
  return points ? t('cardGame.selectionPoints', { points }) : ''
})

/** How many earlier actions of the trick to show. The recent ones are what inform a decision. */
const HISTORY_LIMIT = 3

/**
 * The trick so far. Worth showing because the server resolves every CPU seat between two of
 * our turns inside one response: without this, a lead that went 8 → 9 → 10 would look like the
 * 8 was simply replaced by a 10, as though the seats in between never acted.
 *
 * While a trick is live its last entry is the play on the table, so it is dropped here and
 * left to the card display above. Once a trick has been scored there is no play on the table,
 * so every entry — including the one that won it — belongs in the history.
 */
const trickHistory = computed(() => {
  const g = game.value
  if (!g) return []
  const entries = g.trickHistory ?? []
  const live = g.currentPlay !== null && g.currentPlay.length > 0
  const earlier = live ? entries.slice(0, -1) : entries
  return earlier.slice(-HISTORY_LIMIT)
})

/**
 * Each seat's most recent action in the trick being shown, by username, for the cards displayed
 * under that seat. Built from the whole of `trickHistory` rather than the trimmed version above,
 * because every seat's own last action matters here however far back it was; entries arrive
 * oldest first, so a later one simply overwrites the seat's earlier entry.
 *
 * A seat that has not acted this trick is absent, and the server clears the trick when it is
 * scored, so the row under each seat empties on its own when the next trick starts.
 */
const lastActionByUsername = computed(() => {
  const latest = new Map<string, CardGameTrickEntry>()
  for (const entry of game.value?.trickHistory ?? []) {
    latest.set(entry.username, entry)
  }
  return latest
})

/**
 * Which trick the history above belongs to. Lags the current trick immediately after one is
 * scored: the winning play stays part of the trick it won instead of being relabelled as the
 * next one.
 */
const trickHistoryLabel = computed(() => {
  const g = game.value
  if (!g) return ''
  return t('cardGame.trickHistory', { n: g.trickHistoryNumber })
})

const statusText = computed(() => {
  const g = game.value
  if (!g) return ''
  if (g.status === 'FINISHED') {
    const winner = g.players.find(p => p.username === g.winnerUsername)
    return winner
      ? t('cardGame.winnerIs', { name: winner.name, score: winner.score })
      : t('cardGame.gameFinished')
  }
  if (g.status === 'ABANDONED') return t('cardGame.gameAbandoned')
  if (g.status === 'CANCELLED') return t('cardGame.gameCancelled')
  if (g.status === 'EXPIRED') return t('cardGame.gameExpired')
  if (g.status === 'TIMED_OUT') return t('cardGame.gameTimedOut')
  if (g.myTurn) return t('cardGame.yourTurn')
  const turn = g.players.find(p => p.username === g.currentTurnUsername)
  return turn ? t('cardGame.waitingFor', { name: turn.name }) : ''
})

function mmss(total: number | null): string {
  if (total === null) return ''
  const m = Math.floor(total / 60)
  const s = total % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

/* ----- Loading ------------------------------------------------------------- */

async function loadOpponents() {
  try {
    opponents.value = await cardGameApi.listOpponents()
  } catch {
    // The invite list is optional — a table of CPUs still works without it.
  }
}

const seatStatusOf = (g: CardGameState) =>
  g.players.find(p => p.username === g.myUsername)?.invitationStatus ?? 'PENDING'

/**
 * Pending tables arrive here rather than from `/games/active`, which only reports games that
 * have actually started. The host's own table and any invitation already accepted are the
 * table the user is sitting at; the rest are invitations still to answer.
 */
async function loadInvites() {
  try {
    const invites = await cardGameApi.getInvites()
    incoming.value = invites.incoming.filter(g => seatStatusOf(g) === 'PENDING')
    outgoing.value = invites.outgoing

    const seated = invites.outgoing[0] ?? invites.incoming.find(g => seatStatusOf(g) === 'ACCEPTED')
    if (seated) {
      if (!game.value || game.value.status === 'PENDING') applyGame(seated)
    } else if (game.value?.status === 'PENDING') {
      // Started, cancelled or expired since the last poll — loadActive decides what replaces it.
      game.value = null
    }
  } catch {
    // Transient; the next poll retries.
  }
}

/**
 * Picks up whatever table the user is already sitting at, so a reload or a second tab
 * lands back in the same game rather than offering to create another.
 */
async function loadActive() {
  try {
    const active = await cardGameApi.getActiveGame()
    if (active) applyGame(active)
  } catch {
    // Transient; the next poll retries.
  }
}

async function refreshGame() {
  if (!game.value) return
  try {
    applyGame(await cardGameApi.getGame(game.value.id))
  } catch {
    // Transient; the next poll retries.
  }
}

/**
 * Stores a fresh server state. The selection is dropped whenever the hand changes under it,
 * because indices into the old hand would point at different cards.
 */
function applyGame(next: CardGameState) {
  const previous = game.value
  if (!previous || previous.id !== next.id || previous.myHand.join() !== next.myHand.join()) {
    selectedIndices.value = []
  }
  game.value = next
  turnRemaining.value = next.turnSecondsRemaining
}

/* ----- Actions ------------------------------------------------------------- */

async function createTable() {
  if (!seatsValid.value) return
  creating.value = true
  try {
    applyGame(await cardGameApi.createTable(cpuCount.value, selectedInvitees.value))
    selectedInvitees.value = []
  } catch (e: any) {
    message.error(e?.response?.data?.title ?? t('cardGame.createFailed'))
  } finally {
    creating.value = false
  }
}

/** Wraps a table action so the buttons disable while it runs and errors surface once. */
async function run(action: () => Promise<CardGameState>, onDone?: (s: CardGameState) => void) {
  actionLoading.value = true
  try {
    const state = await action()
    if (onDone) onDone(state)
    else applyGame(state)
  } catch (e: any) {
    message.error(e?.response?.data?.title ?? t('cardGame.actionFailed'))
    await pollLobby()
  } finally {
    actionLoading.value = false
  }
}

const acceptInvite = (id: number) => run(() => cardGameApi.accept(id))

// Also the "leave table" action for a guest who already accepted, so it has to drop the
// table being rendered, not just the row in the invitation list.
const declineInvite = (id: number) =>
  run(() => cardGameApi.decline(id), () => {
    incoming.value = incoming.value.filter(g => g.id !== id)
    if (game.value?.id === id) game.value = null
  })

const cancelTable = (id: number) =>
  run(() => cardGameApi.cancel(id), () => {
    game.value = null
    outgoing.value = outgoing.value.filter(g => g.id !== id)
  })

const startGame = (id: number) => run(() => cardGameApi.start(id))

async function playSelection() {
  // Reachable from a card click as well as the button, so a play already in flight is ignored
  // rather than sent twice.
  if (!game.value || !selectionPlayable.value || playing.value) return
  playing.value = true
  try {
    applyGame(await cardGameApi.play(game.value.id, selectedCards.value))
    selectedIndices.value = []
  } catch (e: any) {
    message.error(e?.response?.data?.title ?? t('cardGame.playRejected'))
    await refreshGame()
  } finally {
    playing.value = false
  }
}

async function passTurn() {
  if (!game.value) return
  playing.value = true
  try {
    applyGame(await cardGameApi.pass(game.value.id))
    selectedIndices.value = []
  } catch (e: any) {
    message.error(e?.response?.data?.title ?? t('cardGame.actionFailed'))
    await refreshGame()
  } finally {
    playing.value = false
  }
}

/**
 * Hand my turns to the server, or take them back. The server plays the seat with the same
 * strategy the CPU opponents use, so this keeps working while the tab is closed — which is
 * most of the point of it.
 */
async function toggleAutoPlay(enabled: boolean) {
  if (!game.value || autoPlaySaving.value) return
  autoPlaySaving.value = true
  try {
    applyGame(await cardGameApi.setAutoPlay(game.value.id, enabled))
    selectedIndices.value = []
  } catch (e: any) {
    message.error(e?.response?.data?.title ?? t('cardGame.actionFailed'))
    await refreshGame()
  } finally {
    autoPlaySaving.value = false
  }
}

async function leaveGame() {
  if (!game.value) return
  await run(() => cardGameApi.leave(game.value!.id), () => {
    game.value = null
  })
}

function closeResult() {
  game.value = null
  pollLobby()
}

/**
 * First click selects a card, a second click on an already-selected card plays the selection.
 *
 * <p>The re-click is what commits, which keeps leading and following identical: a single is two
 * clicks on the one card, a pair is a click on each card and then either of them again. Waiting
 * for a second click is what makes a lead unambiguous — a selected 9 could be a single or the
 * start of a pair, and there is nothing in a first click that says which was meant.
 *
 * <p>Re-clicking an illegal selection deselects that card instead of playing, so a wrong pick
 * is still undone by clicking it off; only a selection the server would accept is committed.
 */
function toggleCard(index: number) {
  const at = selectedIndices.value.indexOf(index)
  if (at < 0) {
    selectedIndices.value.push(index)
    return
  }
  if (selectionPlayable.value) {
    playSelection()
    return
  }
  selectedIndices.value.splice(at, 1)
}

function back() {
  router.push({ name: 'card-games' })
}

/* ----- Polling ------------------------------------------------------------- */

// There is no push channel, so the client polls: slowly in the lobby, faster at the table
// where a CPU turn can land at any moment. The server advances CPUs on read, so polling is
// also what makes them move.
const LOBBY_INTERVAL = 2000
const GAME_INTERVAL = 1500

// Sequential, not concurrent: loadInvites clears a table that has left PENDING and loadActive
// then picks it up as a started game, so running them in order avoids a flicker through the lobby.
async function pollLobby() {
  await loadInvites()
  await loadActive()
}

function startLobbyPolling() {
  if (lobbyTimer) return
  lobbyTimer = setInterval(pollLobby, LOBBY_INTERVAL)
}

function stopLobbyPolling() {
  if (lobbyTimer) clearInterval(lobbyTimer)
  lobbyTimer = null
}

function startGamePolling() {
  if (gameTimer) return
  gameTimer = setInterval(refreshGame, GAME_INTERVAL)
}

function stopGamePolling() {
  if (gameTimer) clearInterval(gameTimer)
  gameTimer = null
}

// Which poll runs depends on where the user is: at an active table only the game matters,
// and a finished game stops both until the result panel is dismissed.
watch(
  () => game.value?.status ?? null,
  status => {
    if (status === 'ACTIVE') {
      stopLobbyPolling()
      startGamePolling()
    } else if (status === 'PENDING') {
      stopGamePolling()
      startLobbyPolling()
    } else if (status) {
      stopGamePolling()
      stopLobbyPolling()
    } else {
      stopGamePolling()
      startLobbyPolling()
    }
  },
  { immediate: true }
)

onMounted(() => {
  loadOpponents()
  pollLobby()
  // Counts down between polls so the turn clock does not sit still for a second and a half.
  tickTimer = setInterval(() => {
    if (turnRemaining.value !== null && turnRemaining.value > 0) turnRemaining.value -= 1
  }, 1000)
})

onUnmounted(() => {
  stopLobbyPolling()
  stopGamePolling()
  if (tickTimer) clearInterval(tickTimer)
})
</script>

<template>
  <div class="card-game-view" :class="{ 'is-immersive': inGame || gameOver, 'is-over': gameOver }">
    <a-card :bordered="false">
      <template #title>
        <a-button type="text" size="small" class="back-btn" @click="back">
          <template #icon><ArrowLeftOutlined /></template>
        </a-button>
        {{ t('cardGame.title70235') }}
      </template>

      <!-- The way out of the table, in the corner: Leave while it is being played, and once it
           is over the same corner dismisses the finished table. -->
      <template #extra>
        <a-button v-if="inGame" danger :loading="actionLoading" @click="leaveGame">
          {{ t('cardGame.leaveGame') }}
        </a-button>
        <a-button v-else-if="gameOver" type="primary" @click="closeResult">
          {{ t('cardGame.backToLobby') }}
        </a-button>
      </template>

      <!-- ===================== LOBBY ===================== -->
      <div v-if="!game" class="lobby">
        <a-alert :message="t('cardGame.rulesHint')" type="info" show-icon banner class="hint" />

        <a-card size="small" :title="t('cardGame.newTable')" class="lobby-card">
          <div class="form-row">
            <span class="form-label">{{ t('cardGame.cpuCount') }}</span>
            <a-input-number v-model:value="cpuCount" :min="0" :max="maxCpu" />
          </div>

          <div class="form-row">
            <span class="form-label">{{ t('cardGame.inviteFollowers') }}</span>
            <a-select
              v-model:value="selectedInvitees"
              mode="multiple"
              class="invitee-select"
              :placeholder="t('cardGame.selectFollowers')"
              :not-found-content="t('cardGame.noOpponents')"
              :max-tag-count="3"
              show-search
              option-filter-prop="label"
            >
              <a-select-option v-for="o in opponents" :key="o.username" :value="o.username" :label="o.name">
                {{ o.name }} (@{{ o.username }})
              </a-select-option>
            </a-select>
          </div>

          <div class="form-row">
            <a-tag :color="seatsValid ? 'blue' : 'red'">
              {{ t('cardGame.seatCount', { count: plannedSeats }) }}
            </a-tag>
            <span v-if="!seatsValid" class="seat-warning">{{ t('cardGame.seatRange') }}</span>
          </div>

          <a-button type="primary" :loading="creating" :disabled="!seatsValid" @click="createTable">
            {{ t('cardGame.createTable') }}
          </a-button>
        </a-card>
        <a-card size="small" :title="t('cardGame.incomingInvites')" class="lobby-card">
          <a-empty v-if="!incoming.length" :description="t('cardGame.noInvites')" />
          <div v-for="inv in incoming" :key="inv.id" class="invite-item">
            <span class="invite-from">
              {{ t('cardGame.invitedYou', { name: inv.hostName }) }}
              <a-tag color="default">{{ t('cardGame.seatCount', { count: inv.players.length }) }}</a-tag>
              <a-tag v-if="inv.inviteSecondsRemaining !== null" color="default">
                {{ t('cardGame.expiresIn', { s: inv.inviteSecondsRemaining }) }}
              </a-tag>
            </span>
            <a-space>
              <a-button type="primary" size="small" :loading="actionLoading" @click="acceptInvite(inv.id)">
                {{ t('cardGame.accept') }}
              </a-button>
              <a-button size="small" :loading="actionLoading" @click="declineInvite(inv.id)">
                {{ t('cardGame.decline') }}
              </a-button>
            </a-space>
          </div>
        </a-card>
      </div>

      <!-- ===================== WAITING TABLE ===================== -->
      <div v-else-if="waitingTable" class="waiting">
        <a-alert
          :message="waitingTable.host ? t('cardGame.waitingAsHost') : t('cardGame.waitingAsGuest', { name: waitingTable.hostName })"
          type="info"
          show-icon
          class="hint"
        />
        <div class="seat-list">
          <PlayerSeat v-for="p in waitingTable.players" :key="p.username" :player="p" lobby />
        </div>
        <a-space wrap>
          <a-button
            v-if="waitingTable.host"
            type="primary"
            :loading="actionLoading"
            :disabled="!waitingTable.canStart"
            @click="startGame(waitingTable.id)"
          >
            {{ t('cardGame.start') }}
          </a-button>
          <a-button v-if="waitingTable.host" danger :loading="actionLoading" @click="cancelTable(waitingTable.id)">
            {{ t('cardGame.cancelTable') }}
          </a-button>
          <a-button v-else :loading="actionLoading" @click="declineInvite(waitingTable.id)">
            {{ t('cardGame.leaveTable') }}
          </a-button>
        </a-space>
        <p v-if="waitingTable.host && !waitingTable.canStart" class="hint-text">
          {{ t('cardGame.startBlocked') }}
        </p>
      </div>
      <!-- ===================== TABLE =====================
           Two boxes, not one. .table-fit is the space the layout has given us, measured; the
           .table-area inside it keeps its natural height and is scaled down by whatever it
           takes to fit (utils/fitScale.ts). Adding the bands up and subtracting them from the
           viewport was the previous approach and it left a scrollbar: the sum has to agree
           with the card's title row, the header, two layers of padding, the font size and the
           zoom level, and it did not. -->
      <div v-else ref="fitBox" class="table-fit">
      <div ref="tableEl" class="table-area" :style="tableStyle">
        <div class="seat-list">
          <PlayerSeat
            v-for="p in others"
            :key="p.username"
            :player="p"
            :active="inGame && p.username === game.currentTurnUsername"
            :leading="p.username === game.currentPlayUsername"
            :played="lastActionByUsername.get(p.username) ?? null"
            :place="placeByUsername.get(p.username) ?? null"
            :winner="gameOver && p.username === game.winnerUsername"
            :first-out="gameOver && p.username === game.firstOutUsername"
          />
        </div>

        <!-- The middle of the table: the play in front of everyone while the game runs, and the
             result in the same place once it is over. The outcome belongs where the game was
             happening, and this area is otherwise empty and captionless at that point. -->
        <div class="trick" :class="{ 'is-result': gameOver }">
          <template v-if="gameOver">
            <div class="result-headline">
              <TrophyOutlined class="result-trophy" />
              <span v-if="winner" class="result-winner">
                {{ winner.username === game.myUsername
                  ? t('cardGame.youWinWith', { score: winner.score })
                  : t('cardGame.winnerIs', { name: winner.name, score: winner.score }) }}
              </span>
              <!-- Abandoned and cancelled games have no winner: statusText says which. -->
              <span v-else class="result-winner">{{ statusText }}</span>
            </div>

            <p v-if="firstOut" class="result-firstout">
              {{ t('cardGame.firstOutWas', { name: firstOut.name }) }}
            </p>
            <!-- The scores themselves are on the seats, which is where the placings are too. -->
            <p class="result-hint">{{ t('cardGame.resultOnSeats') }}</p>
          </template>

          <template v-else>
            <div class="trick-head">
              <span class="trick-label">{{ t('cardGame.trickNumber', { n: game.trickNumber }) }}</span>
              <a-tag color="default">{{ t('cardGame.deckLeft', { count: game.deckCount }) }}</a-tag>
              <a-tag v-if="game.trickPoints" color="gold">
                {{ t('cardGame.trickPoints', { points: game.trickPoints }) }}
              </a-tag>
            </div>

            <!-- The play and who made it, in one band that is always here. The cards, the
                 empty-table message and the owner line all come and go, and with the box
                 centred on them any one of them appearing would slide the cards the player
                 is reading. Holding the band means the cards stay where they were. -->
            <div class="trick-play">
              <div v-if="game.currentPlay && game.currentPlay.length" class="trick-cards">
                <PlayingCard v-for="(c, i) in game.currentPlay" :key="`${c}-${i}`" :card="c" small />
              </div>
              <a-empty v-else :image="false" :description="t('cardGame.trickEmpty')" />

              <div v-if="game.currentPlayName" class="trick-owner">
                {{ t('cardGame.playedBy', { name: game.currentPlayName, shape: shapeLabel }) }}
              </div>
            </div>

            <!-- What the current play was raised from. Several seats can act in one server
                 response, so without this the intermediate plays would never be seen.
                 Always rendered, for the same reason as the band above. -->
            <div class="trick-history">
              <template v-if="trickHistory.length">
                <span class="history-label">{{ trickHistoryLabel }}</span>
                <div v-for="(entry, i) in trickHistory" :key="`${entry.seat}-${i}`" class="history-row">
                  <span class="history-name">{{ entry.name }}</span>
                  <span v-if="entry.passed" class="history-pass">{{ t('cardGame.passed') }}</span>
                  <span v-else class="history-cards">
                    <PlayingCard v-for="(c, j) in entry.cards" :key="`${c}-${j}`" :card="c" small />
                  </span>
                </div>
              </template>
            </div>
          </template>
        </div>

        <div class="status-bar">
          <span class="status-text">{{ statusText }}</span>
          <a-tag
            v-if="inGame && game.myTurn && turnRemaining !== null"
            :color="turnRemaining <= 15 ? 'red' : 'default'"
          >
            ⏱ {{ turnRemaining }}s
          </a-tag>
          <a-tag v-if="inGame && game.gameSecondsRemaining !== null" color="default">
            {{ t('cardGame.roundTimeLeft', { time: mmss(game.gameSecondsRemaining) }) }}
          </a-tag>
        </div>

        <div class="my-area">
          <div class="my-head">
            <PlayerSeat
              v-if="me"
              :player="me"
              :active="inGame && game.myTurn"
              :leading="me.username === game.currentPlayUsername"
              :played="lastActionByUsername.get(me.username) ?? null"
              :place="placeByUsername.get(me.username) ?? null"
              :winner="gameOver && me.username === game.winnerUsername"
              :first-out="gameOver && me.username === game.firstOutUsername"
              self
            />
            <span v-if="selectionLabel" class="selection-points">{{ selectionLabel }}</span>
          </div>

          <!-- Still shown while auto-play is on, so the hand can be watched being played; the
               cards just stop responding to clicks. -->
          <CardHand
            :cards="myHand"
            :selected="selectedIndices"
            :disabled="!inGame || !game.myTurn || playing || autoPlayOn"
            @toggle="toggleCard"
          />

          <!-- The second click is not visible in the cards themselves, so it is spelled out
               while it applies. The line is held empty when it does not: it applies only on
               your turn, and a line appearing and vanishing each turn would shift the buttons
               below it under the cursor that is reaching for them. -->
          <p v-if="inGame" class="play-hint is-reserved">
            {{ game.myTurn && !autoPlayOn ? t('cardGame.clickAgainToPlay') : '' }}
          </p>

          <!-- The buttons and the auto-play switch, wrapped together so a phone can put them
               on one line. They stay two rows on a desktop, where the switch reads better
               under the actions it governs than beside them. -->
          <div v-if="inGame" class="control-rows">
            <a-space wrap class="hand-actions">
              <a-button
                type="primary"
                :loading="playing"
                :disabled="!selectionPlayable || autoPlayOn"
                @click="playSelection"
              >
                {{ t('cardGame.playCards') }}
              </a-button>
              <a-button
                :loading="playing"
                :disabled="!game.myTurn || mustLead || autoPlayOn"
                @click="passTurn"
              >
                {{ t('cardGame.pass') }}
              </a-button>
              <a-button
                :disabled="!selectedIndices.length || autoPlayOn"
                @click="selectedIndices = []"
              >
                {{ t('cardGame.clearSelection') }}
              </a-button>
            </a-space>

            <!-- A mode rather than an action, so it sits with the hand it governs instead of in
                 the corner with Leave. The buttons above are disabled while it is on: the
                 server takes the turn on the next read, so a click would be racing something
                 it cannot beat. -->
            <div class="auto-play-row">
              <a-switch
                :checked="autoPlayOn"
                :loading="autoPlaySaving"
                @change="(checked: any) => toggleAutoPlay(!!checked)"
              />
              <span class="auto-play-label">{{ t('cardGame.autoPlay') }}</span>
            </div>
          </div>
          <p v-if="inGame" class="play-hint">
            {{ autoPlayOn ? t('cardGame.autoPlayOnHint') : t('cardGame.autoPlayOffHint') }}
          </p>
        </div>

      </div>
      </div>
    </a-card>
  </div>
</template>

<style scoped>
.card-game-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* While a table is on screen MainLayout is locked to the window (see utils/immersive.ts) and
   this view is handed what is left under the header. That height has to reach .table-area,
   which means every box between here and it becomes a flex item that can shrink: the view,
   the card, and the card's body. min-height: 0 at each step, or a flex item in a column
   stops at its content height and the overflow reappears at the bottom of the window.

   Scoped styles do not reach .ant-card-body, hence :deep. */
.card-game-view.is-immersive {
  height: 100%;
  gap: 0;
}
.card-game-view.is-immersive > :deep(.ant-card) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}
.card-game-view.is-immersive :deep(.ant-card-body) {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.back-btn {
  margin-inline-end: 4px;
}
.hint {
  margin-bottom: 16px;
}
.hint-text {
  margin: 8px 0 0;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
.lobby {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.lobby-card {
  width: 100%;
}
.form-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.form-label {
  min-width: 96px;
  color: rgba(0, 0, 0, 0.65);
}
.invitee-select {
  min-width: 260px;
  flex: 1;
}
.seat-warning {
  color: #cf1322;
  font-size: 12px;
}
.invite-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  flex-wrap: wrap;
}
.invite-item:last-child {
  border-bottom: none;
}
.waiting,
.table-area {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
/* The table is a fixed box, not one that grows with what is on it.

   Everything here changes size as the game runs: seats pick up tags, played cards appear
   under them and are cleared every trick, the history grows, hints come and go. Sized by
   content, all of that moves the trick area in the middle of the screen while the player is
   trying to read it and click cards in it. So each band below gets a fixed height and the
   table a fixed width, and the content fits inside or clips — the layout no longer depends
   on what is in it.

   min() rather than a flat width: below the fixed width the table still fits the screen,
   which is what the narrow-screen rules at the bottom of this file build on. */
.table-area {
  --table-width: 960px;
  /* A seat is 130px (see PlayerSeat.vue) + 2px of border + 2px for the row's own padding. */
  --seat-band-height: 134px;
  /* 40px padding + 28px head + 12px gap + 110px play + 12px gap + 144px history band. */
  --trick-height: 346px;
  width: min(var(--table-width), 100%);
  margin-inline: auto;
}
/* The space the layout has given us, and the only box in here with a height that comes from
   the window rather than from its contents. min-height: 0 is what lets it shrink below its
   own content; without it a flex item in a column refuses to go under its content height and
   the overflow moves to the bottom of the window.

   The clip is a backstop that should never fire: the table inside is scaled to fit this box,
   so the two are the same height by construction. It is here so that if a measurement ever
   arrives late — a web font swapping in, say — the result is one frame of a slightly large
   table rather than a scrollbar. */
.card-game-view.is-immersive .table-fit {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
/* The table keeps its natural height here and is scaled by utils/fitScale.ts. Nothing in the
   immersive rules divides the viewport up any more — that arithmetic is what produced a
   scrollbar twice, because it has to agree with the card's title row, the layout header, two
   layers of padding, the font size and the zoom level, and anything that reads the sum wrong
   by a pixel shows a scrollbar.

   A transform does not affect layout, so this box's measured height is the same before and
   after it is scaled: there is no feedback loop between the measurement and the fit. */
.card-game-view.is-immersive .table-area {
  height: auto;
  flex: 0 0 auto;
}
.card-game-view.is-immersive .seat-list,
.card-game-view.is-immersive .status-bar,
.card-game-view.is-immersive .my-area,
.card-game-view.is-immersive .trick {
  flex: 0 0 auto;
}

/* Game over. Every band on the table is a fixed height with its overflow clipped, which is
   right while the game runs — it is what stops a seat picking up a tag and moving the middle
   of the table under the cursor. It is wrong once the game is over, because that is the
   moment three of those bands get *more* in them: the seats gain a placing number and the
   winner a trophy, the status line gains a full sentence about who won, and the trick area is
   replaced by the result block. Clipped to the sizes that fitted during play, the ends of
   those are cut off.
   Nothing is moving any more, so the bands can size to their contents. The table as a whole
   is still scaled to fit, so growing here cannot bring a scrollbar back. */
.card-game-view.is-over .table-area .seat-list {
  height: auto;
  min-height: var(--seat-band-height);
  overflow: visible;
  flex-wrap: wrap;
}
.card-game-view.is-over .status-bar {
  height: auto;
  min-height: 24px;
  overflow: visible;
  flex-wrap: wrap;
  white-space: normal;
}
.card-game-view.is-over .status-bar .status-text {
  overflow: visible;
  text-overflow: clip;
  white-space: normal;
}
.card-game-view.is-over .trick.is-result {
  height: auto;
  min-height: var(--trick-height);
}
.card-game-view.is-over .table-area :deep(.player-seat:not(.is-lobby)) {
  height: auto;
  min-height: 62px;
}
.card-game-view.is-over .table-area :deep(.seat-main) {
  height: auto;
  min-height: var(--seat-main-height);
}
.seat-list {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
/* At the table the seats are one row of fixed-size boxes that never re-wraps: a second row
   would change the height of the band and push the trick area down the screen. The full
   table is four opponents, 948px of seats and gaps inside a 960px table, so they fit without
   scrolling — the overflow is there for a narrowed window, where sideways scroll is the trade
   for keeping the trick area still. The lobby's seat list keeps the wrapping default, having
   no trick area underneath to protect. */
.table-area .seat-list {
  flex-wrap: nowrap;
  height: var(--seat-band-height);
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 2px;
}
/* Fixed height, not a minimum. This box held a play of one to five cards, an owner line that
   is absent on a lead, and nothing to three history rows — so it changed height several times
   a trick, and being centred in the page that moved its own contents under the cursor. Now it
   is one size for the whole game, including the result shown in it at the end. */
.trick {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px 16px;
  border-radius: 10px;
  background: #f6ffed;
  border: 1px solid #d9f7be;
  height: var(--trick-height);
  justify-content: center;
}
/* While the game runs the three bands are stacked from the top at fixed heights, rather than
   centred as a group: centring is what made a new history row nudge the current play upward.
   The result state keeps the centring, since it has one short block to place.
   Budget: 346px box - 40px padding = 306px, spent as 28 + 110 + 144 with two 12px gaps. */
.trick:not(.is-result) {
  justify-content: flex-start;
}
.trick-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
  height: 28px;
  flex: 0 0 28px;
}
.trick-label {
  font-weight: 600;
}
/* Holds the play, or the empty-table message, plus the owner line under it. Centred within
   itself so one card and five cards both sit in the middle of the same band. */
.trick-play {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 110px;
  flex: 0 0 110px;
  width: 100%;
}
.trick-cards {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: center;
}
.trick-owner {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.55);
}

/* Earlier plays in the trick, dimmed so the play actually on the table stays dominant.

   Fixed height and always present, so the band is reserved from the first trick rather than
   appearing under the play once someone raises.

   Sized to hold HISTORY_LIMIT (3) rows exactly: 8px padding + 19px label + three 39px rows.
   It used to be 98px with `overflow-y: auto` as a "fallback for a row that wraps", and that
   fallback fired on every trick with more than one earlier play — see .history-cards below
   for why the rows are taller than they look. That was a scrollbar inside the table on an
   ordinary trick. Nothing here scrolls now; the band fits what the cap allows. */
.trick-history {
  width: 100%;
  height: 144px;
  flex: 0 0 144px;
  padding-top: 8px;
  border-top: 1px dashed #e8e8e8;
  opacity: 0.75;
  overflow: hidden;
}
.history-label {
  display: block;
  margin-bottom: 4px;
  font-size: 11px;
  color: rgba(0, 0, 0, 0.45);
}
/* One height for every row, whether it holds cards or the word "passed", so three rows is
   always exactly the band above and a mixed trick does not change it. */
.history-row {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 39px;
}
.history-name {
  flex: 0 0 auto;
  min-width: 72px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.65);
}
.history-pass {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
}
/* The cards are shown at a reduced scale: these are superseded plays, so they should read as
   a trail rather than compete with the current one.

   A transform does not affect layout, so without a height here each row reserves the full
   54px of an unscaled small card while showing 39px of it — three rows then needed 189px in a
   band built for 98px, which is where the scrollbar inside the table came from. The height is
   the scaled one, 54 * 0.72, so the row reserves what it actually draws. */
.history-cards {
  display: flex;
  height: 39px;
  align-items: center;
  transform: scale(0.72);
  transform-origin: left center;
}
.history-cards .playing-card + .playing-card {
  margin-inline-start: -10px;
}
/* One row, fixed: the turn clock tag only shows on your turn, and wrapping it onto a second
   line would move the hand down at the moment the turn arrives. The status text truncates
   instead — the tags beside it are the part that is time-critical. */
.status-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  height: 24px;
  overflow: hidden;
}
.status-bar .status-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.status-text {
  font-weight: 600;
}
.my-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}
.my-head {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.selection-points {
  font-size: 12px;
  color: #d48806;
}
.play-hint {
  margin: 0;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
/* Keeps its line whether or not there is text in it. */
.play-hint.is-reserved {
  min-height: 18px;
}
/* Two rows on a desktop; a phone turns this into one. */
.control-rows {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.hand-actions {
  margin-top: 4px;
}
.auto-play-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.auto-play-label {
  font-size: 13px;
}
.result-headline {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.result-trophy {
  font-size: 20px;
  color: #d48806;
}
.result-winner {
  font-size: 16px;
  font-weight: 600;
}
.result-firstout {
  margin: 0;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.55);
}
.result-hint {
  margin: 0;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

/* The same area as the trick, in the colour of a finished game rather than a live one, so it
   is clear at a glance that there is nothing left to answer. */
.trick.is-result {
  background: #fffbe6;
  border-color: #ffe58f;
  text-align: center;
}

/* Narrow screens: the seats stack and the hand gets the full width.

   The table stays a fixed box here too — that is the point of it — but a shorter one. A phone
   has no room for the 300px trick area, and `min()` on the width already lets the table
   narrow to the screen. The seat row scrolls sideways at this size for any table at all,
   which is the trade for keeping the trick area still. */
@media (max-width: 768px) {
  .form-label {
    min-width: auto;
  }
  .invitee-select {
    min-width: 100%;
  }
  .seat-list {
    gap: 8px;
  }
  /* The whole table on one screen, no page scroll.

     Desktop fixes every band and lets the table be as tall as it needs. A phone cannot
     afford that, so the relationship inverts: the table takes the height the screen has and
     the trick area absorbs whatever the other bands leave. The centre still does not move
     for a given device — the bands around it are all fixed — it is just measured from the
     viewport instead of from a constant.

     The height itself comes from the immersive rules above, which get it from the flex chain
     rather than from an estimate of the chrome. This used to subtract a hand-counted 150px,
     which was both fragile and short by the footer's ~70px — the reason a phone still had a
     little page scroll left. */
  .table-area {
    gap: 8px;
    /* A phone's trick band is a fixed size like every other band, and the table as a whole is
       scaled to the screen (utils/fitScale.ts). This used to be a flexible band inside a
       table stretched to `calc(100dvh - 150px)`, which is where the last of the phone's page
       scroll came from: that 150px had to equal the real chrome and did not. */
    --trick-height: 150px;
  }
  /* 6px + 48px main + 4px gap + 38px played + 6px + 2px border = 104px. */
  .table-area .seat-list {
    --seat-band-height: 106px;
    gap: 8px;
  }
  /* Fixed at the phone's --trick-height, like every other band. Written at the same
     specificity as the base immersive rule, which is not inside a media query and would
     otherwise win here. */
  .trick,
  .card-game-view.is-immersive .trick {
    padding: 10px 8px;
    gap: 8px;
  }
  .trick-head {
    height: 24px;
    flex: 0 0 24px;
    font-size: 12px;
  }
  .trick:not(.is-result) {
    justify-content: center;
  }
  .trick-play {
    height: auto;
    flex: 0 1 auto;
  }
  /* Dropped on a phone. It is the dimmed trail of plays already superseded, so of everything
     on the table it is the part whose absence costs the least — and it is the only band that
     grows with the game. The current play and who made it are still above it. */
  .trick-history {
    display: none;
  }
  /* The hints go the same way, for the same reason: explanation rather than game state. The
     second-click rule is also the one thing a phone player discovers by tapping. */
  .play-hint {
    display: none;
  }
  /* Buttons and the auto-play switch share a line. */
  .control-rows {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }
  .hand-actions {
    margin-top: 0;
  }
  .my-area {
    gap: 6px;
    padding-top: 6px;
  }
  .status-bar {
    height: 22px;
  }
  /* The card gives its padding back to the table, as StockListView does at this width. */
  .card-game-view :deep(.ant-card-body) {
    padding: 10px;
  }
  .card-game-view :deep(.ant-card-head) {
    padding-inline: 12px;
    min-height: 44px;
  }

  /* And the view claws back the layout's own frame — .app-content contributes 12px of margin
     and 12px of padding on each side at this width (MainLayout.vue), 48px of vertical space
     that a table trying to fit a 640px screen cannot spare. Pulled back with negative
     margins rather than by changing MainLayout, which would affect every page in the app for
     the sake of this one. A small side inset is kept so the cards do not touch the edge. */
  /* Claws back the .app-content frame, which on a phone is 12px of padding this view would
     rather spend on cards. Immersive mode sets that padding down to 6px/8px itself, so there
     is nothing left to reclaim and the negative margin would pull the card off the screen. */
  .card-game-view {
    margin: -12px -12px -12px;
    padding-inline: 4px;
  }
  .card-game-view.is-immersive {
    margin: 0;
    padding-inline: 0;
  }

  /* Lobby, not the table: an invite row has no width for a name and two buttons side by
     side at this size. */
  .invite-item {
    align-items: flex-start;
    flex-direction: column;
  }
}

/* Short screens — a small phone in portrait, roughly 640px of device height or less once the
   browser's own bars are out. Everything above still leaves the trick area below its floor
   here, so the played-card row under each opponent goes too, the way it already does on your
   own seat. The current play is still in the middle of the table and the seats still carry
   their counts and scores; what is lost is only which of those cards came from which seat.
   Keyed on the viewport rather than a device width, since this is a question of height. */
@media (max-width: 768px) and (max-height: 600px) {
  .table-area .seat-list {
    --seat-band-height: 68px;
  }
  .table-area :deep(.player-seat:not(.is-lobby)) {
    height: 62px;
  }
  .table-area :deep(.seat-played) {
    display: none;
  }
}

/* ---------------------------------------------------------------------------------------
   Immersive height tiers. Wide enough for the desktop layout, but not necessarily tall
   enough for all of it — a 1366x768 laptop is a desktop by width and shorter than a phone
   held in landscape. The phone block above is keyed on width and owns everything under
   769px; these are keyed on height and own everything over it.

   Each tier gives up the next-cheapest thing and its breakpoint is set to its own measured
   threshold, so a tier is never selected for a window it cannot fit. The thresholds, from
   the band heights in this file and in PlayerSeat/CardHand/PlayingCard:

     tier 1  everything on the table              827px of viewport
     tier 2  no raise history, no hints           739px
     tier 3  + played rows off the seats          595px

   Below 595px the table scrolls, as it does on a very small phone.
   --------------------------------------------------------------------------------------- */

/* Tier 1, and the base for the two below it. Two compressions the phone block already makes
   for its own reasons, both of which turn out to be free:

   Your own seat loses its played row — those are the cards you just put down out of the hand
   immediately below it, so the row is a duplicate of what is already on screen 92px further
   down. And the hand actions share a line with the auto-play switch, which reads fine beside
   the buttons it governs rather than under them.

   The auto-play hint goes too, being a sentence about a switch whose state is visible in the
   switch. The second-click hint stays: it is the one rule in the game you cannot see. */
@media (min-width: 769px) {
  /* The card gives its padding back to the table, as it already does on a phone. 24px of body
     padding and a 56px head are 68px that the table can put under the cards instead, and the
     border is what marks the edge of it, not the gap inside the border. */
  .card-game-view.is-immersive :deep(.ant-card-body) {
    padding: 10px;
  }
  .card-game-view.is-immersive :deep(.ant-card-head) {
    padding-inline: 16px;
    min-height: 44px;
  }
  .card-game-view.is-immersive .table-area :deep(.player-seat.is-self:not(.is-lobby)) {
    height: 62px;
  }
  .card-game-view.is-immersive .table-area :deep(.player-seat.is-self .seat-played) {
    display: none;
  }
  .card-game-view.is-immersive .control-rows {
    flex-direction: row;
    align-items: center;
    gap: 12px;
  }
  .card-game-view.is-immersive .hand-actions {
    margin-top: 0;
  }
  .card-game-view.is-immersive .play-hint:not(.is-reserved) {
    display: none;
  }
}

/* Tier 2. The history band is the dimmed trail of plays already superseded, so it is the
   first real thing to go; the current play and who made it are still in the middle of the
   table. The second-click hint follows it, which is the last of the explanatory text.

   The breakpoint is where tier 1 would have to scale below about 0.9 to fit. Dropping the
   band the player needs least reads better than shrinking the whole table including its
   text, and the two mechanisms compose: the tiers decide how much is on the table, the
   scale guarantees whatever is on it fits. */
@media (min-width: 769px) and (max-height: 862px) {
  .card-game-view.is-immersive .table-area {
    --trick-height: 190px;
    gap: 10px;
  }
  .card-game-view.is-immersive .trick-history,
  .card-game-view.is-immersive .play-hint {
    display: none;
  }
}

/* Tier 3. The played row comes off the opponents' seats as well, the way the short-screen
   phone block does it: the seats keep their names, card counts, scores and turn state, and
   what is lost is which of the cards on the table came from which seat — recoverable from
   the current play, which is still there. Everything else is squeezed by a few pixels. */
@media (min-width: 769px) and (max-height: 678px) {
  .card-game-view.is-immersive .table-area {
    --trick-height: 144px;
    gap: 8px;
  }
  .card-game-view.is-immersive .table-area .seat-list {
    --seat-band-height: 68px;
  }
  .card-game-view.is-immersive .table-area :deep(.player-seat:not(.is-lobby)) {
    height: 62px;
  }
  .card-game-view.is-immersive .table-area :deep(.seat-played) {
    display: none;
  }
  .card-game-view.is-immersive :deep(.card-hand) {
    padding: 8px 0 4px;
    min-height: 80px;
  }
  .card-game-view.is-immersive .my-area {
    gap: 8px;
    padding-top: 6px;
  }
  .card-game-view.is-immersive .status-bar {
    height: 22px;
  }
  .card-game-view.is-immersive .trick {
    padding: 10px 16px;
    gap: 8px;
  }
  .card-game-view.is-immersive .trick-head {
    height: 24px;
    flex: 0 0 24px;
  }
  .card-game-view.is-immersive .trick-play {
    height: auto;
    flex: 0 1 auto;
  }
}
</style>
