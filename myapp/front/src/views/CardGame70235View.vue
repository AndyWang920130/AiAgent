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
  <div class="card-game-view">
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
      <!-- ===================== TABLE ===================== -->
      <div v-else class="table-area">
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
    </a-card>
  </div>
</template>

<style scoped>
.card-game-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
  --trick-height: 300px;
  width: min(var(--table-width), 100%);
  margin-inline: auto;
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
   Budget: 300px box - 40px padding = 260px, spent as 28 + 110 + 98 with two 12px gaps. */
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
   appearing under the play once someone raises. Three rows is the cap HISTORY_LIMIT sets and
   they fit; the scroll is a fallback for a row that wraps, not the normal case. */
.trick-history {
  width: 100%;
  height: 98px;
  flex: 0 0 98px;
  padding-top: 8px;
  border-top: 1px dashed #e8e8e8;
  opacity: 0.75;
  overflow-y: auto;
}
.history-label {
  display: block;
  margin-bottom: 4px;
  font-size: 11px;
  color: rgba(0, 0, 0, 0.45);
}
.history-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 28px;
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
   a trail rather than compete with the current one. */
.history-cards {
  display: flex;
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

     The 150px is this view's chrome at this breakpoint: 56px app header + 44px card head +
     20px card body padding + 24px of .app-content frame left after the negative margin at
     the bottom of this block + 6px slack. A magic number, but the alternative is making
     MainLayout viewport-locked for every page in the app.
     dvh first so a phone's collapsing address bar is handled; vh for anything without it. */
  .table-area {
    height: calc(100vh - 150px);
    height: calc(100dvh - 150px);
    gap: 8px;
    /* Scroll, not clip, if a screen is smaller than anything budgeted for here — roughly
       568px of device height, a first-generation SE. Nothing scrolls on the sizes above,
       where it all fits; on one that small, a short scroll is much better than a Play button
       cut off below the fold. */
    overflow-y: auto;
  }
  /* 6px + 48px main + 4px gap + 38px played + 6px + 2px border = 104px. */
  .table-area .seat-list {
    --seat-band-height: 106px;
    gap: 8px;
  }
  /* The one band that is not fixed here: it takes what is left, down to a floor that still
     fits a five-card play with its caption — 20px padding + 24px head + 8px gap + 42px card
     + 16px owner line. */
  .trick {
    flex: 1 1 auto;
    height: auto;
    min-height: 110px;
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
  .card-game-view {
    margin: -12px -12px -12px;
    padding-inline: 4px;
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
</style>
