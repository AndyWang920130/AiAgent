<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { AppstoreOutlined, RedoOutlined, RollbackOutlined } from '@ant-design/icons-vue'
import { gomokuApi, type GomokuGame, type GomokuOpponent } from '../api/gomoku'
import GomokuBoard from '../components/GomokuBoard.vue'

const { t } = useI18n()

const SIZE = 15
type Cell = 0 | 1 | 2
type Player = 1 | 2

const mode = ref<'local' | 'online'>('local')

/* =========================================================================
 * Local hotseat mode (two players, one device) — unchanged behaviour.
 * ========================================================================= */
interface Move {
  r: number
  c: number
}

const DIRS = [
  [0, 1],
  [1, 0],
  [1, 1],
  [1, -1],
] as const

function createBoard(): Cell[][] {
  return Array.from({ length: SIZE }, () => Array<Cell>(SIZE).fill(0))
}

const board = ref<Cell[][]>(createBoard())
const current = ref<Player>(1)
const winner = ref<Cell>(0)
const isDraw = ref(false)
const moves = ref<Move[]>([])
const scores = ref({ black: 0, white: 0 })

const lastMove = computed<Move | null>(() => moves.value[moves.value.length - 1] ?? null)
const gameOver = computed(() => winner.value !== 0 || isDraw.value)

const status = computed(() => {
  if (winner.value === 1) return t('gomoku.blackWins')
  if (winner.value === 2) return t('gomoku.whiteWins')
  if (isDraw.value) return t('gomoku.draw')
  return current.value === 1 ? t('gomoku.blackTurn') : t('gomoku.whiteTurn')
})

function countDir(b: Cell[][], r: number, c: number, dr: number, dc: number, player: Player): number {
  let n = 0
  let rr = r + dr
  let cc = c + dc
  while (rr >= 0 && rr < SIZE && cc >= 0 && cc < SIZE && b[rr][cc] === player) {
    n++
    rr += dr
    cc += dc
  }
  return n
}

function checkWin(b: Cell[][], r: number, c: number, player: Player): boolean {
  for (const [dr, dc] of DIRS) {
    const count = 1 + countDir(b, r, c, dr, dc, player) + countDir(b, r, c, -dr, -dc, player)
    if (count >= 5) return true
  }
  return false
}

function place(r: number, c: number): void {
  if (gameOver.value || board.value[r][c] !== 0) return
  const player = current.value
  board.value[r][c] = player
  moves.value.push({ r, c })

  if (checkWin(board.value, r, c, player)) {
    winner.value = player
    if (player === 1) scores.value.black++
    else scores.value.white++
    return
  }
  if (moves.value.length === SIZE * SIZE) {
    isDraw.value = true
    return
  }
  current.value = player === 1 ? 2 : 1
}

function undo(): void {
  const last = moves.value.pop()
  if (!last) return
  board.value[last.r][last.c] = 0
  if (winner.value === 1) scores.value.black--
  else if (winner.value === 2) scores.value.white--
  winner.value = 0
  isDraw.value = false
  current.value = moves.value.length % 2 === 0 ? 1 : 2
}

function restart(): void {
  board.value = createBoard()
  moves.value = []
  current.value = 1
  winner.value = 0
  isDraw.value = false
}

/* =========================================================================
 * Online mode — invite a fan, then play over HTTP polling.
 * ========================================================================= */
const opponents = ref<GomokuOpponent[]>([])
const selectedOpponent = ref<string | undefined>(undefined)
const incoming = ref<GomokuGame[]>([])
const outgoing = ref<GomokuGame[]>([])
const game = ref<GomokuGame | null>(null)
const inviting = ref(false)
const actionLoading = ref(false)

// Rematch (post-game "play again") state. A rematch is just a fresh invite between the same
// two players, so it reuses the invite/accept/notification machinery.
const rematchIncoming = ref<GomokuGame | null>(null) // opponent proposed a rematch
const rematchOutgoing = ref<GomokuGame | null>(null) // my rematch invite, awaiting the opponent
const rematchLoading = ref(false)

// Locally-ticked countdowns (seconds). Seeded from the server on each poll and decremented
// once per second between polls so the display stays smooth; re-synced on the next poll.
const moveRemaining = ref<number | null>(null)
const gameRemaining = ref<number | null>(null)

// Client-side session score for the current match series between the two players. Keyed by
// login, it accumulates while the two keep rematching and resets when a new series starts
// (fresh invite/accept, back to lobby, or (re)entering online). Both clients compute the same
// tally by observing each game's terminal winner, so they agree; a page refresh clears it.
const seriesScores = ref<Record<string, number>>({})
const countedGames = new Set<number>() // game ids already scored, so each game counts once

function resetSeries(): void {
  seriesScores.value = {}
  countedGames.clear()
}

// Count the winner of a just-decided game exactly once. Draws (winner 0) score nothing.
function recordResultIfNeeded(g: GomokuGame | null): void {
  if (!g) return
  const decided = g.status === 'FINISHED' || g.status === 'RESIGNED' || g.status === 'TIMED_OUT'
  if (!decided || !g.winner || g.winner < 1) return
  if (countedGames.has(g.id)) return
  countedGames.add(g.id)
  const winnerLogin = g.winner === 1 ? g.blackUsername : g.whiteUsername
  seriesScores.value = { ...seriesScores.value, [winnerLogin]: (seriesScores.value[winnerLogin] || 0) + 1 }
}

const blackScore = computed(() => (game.value ? seriesScores.value[game.value.blackUsername] || 0 : 0))
const whiteScore = computed(() => (game.value ? seriesScores.value[game.value.whiteUsername] || 0 : 0))

let lobbyTimer: ReturnType<typeof setInterval> | null = null
let gameTimer: ReturnType<typeof setInterval> | null = null
let uiTimer: ReturnType<typeof setInterval> | null = null
let rematchTimer: ReturnType<typeof setInterval> | null = null

// Convert the server's 225-char board string into the 2D array the board component renders.
const onlineBoard = computed<Cell[][]>(() => {
  const g = game.value
  const grid = createBoard()
  if (!g) return grid
  for (let i = 0; i < g.board.length; i++) {
    const v = g.board.charCodeAt(i) - 48 // '0' -> 0
    if (v === 1 || v === 2) grid[Math.floor(i / SIZE)][i % SIZE] = v as Cell
  }
  return grid
})

const onlineLastMove = computed<Move | null>(() => {
  const g = game.value
  if (!g || g.lastMoveRow == null || g.lastMoveCol == null) return null
  return { r: g.lastMoveRow, c: g.lastMoveCol }
})

const isMyTurn = computed(() => !!game.value && game.value.status === 'ACTIVE' && game.value.currentPlayer === game.value.myColor)
const onlineOver = computed(() => !!game.value && game.value.status !== 'ACTIVE')

// The opponent's login in the current game (whichever colour I am not).
const opponentLogin = computed(() => {
  const g = game.value
  if (!g) return ''
  return g.myColor === 1 ? g.whiteUsername : g.blackUsername
})

const onlineStatus = computed(() => {
  const g = game.value
  if (!g) return ''
  if (g.status === 'TIMED_OUT') {
    return g.winner === g.myColor ? t('gomoku.wonOnTime') : t('gomoku.lostOnTime')
  }
  if (g.status === 'RESIGNED') {
    return g.winner === g.myColor ? t('gomoku.opponentResigned') : t('gomoku.youResigned')
  }
  if (g.status === 'FINISHED') {
    if (g.winner === 0) return t('gomoku.draw')
    return g.winner === g.myColor ? t('gomoku.youWin') : t('gomoku.youLose')
  }
  return isMyTurn.value ? t('gomoku.yourTurn') : t('gomoku.opponentTurn')
})

// My clock has visually run out — block clicks until the server confirms the timeout on the next poll.
const myTimeUp = computed(() => isMyTurn.value && moveRemaining.value === 0)

function mmss(total: number | null): string {
  if (total == null) return '--:--'
  const m = Math.floor(total / 60)
  const s = total % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

function opponentOf(g: GomokuGame): { name: string } {
  return g.myColor === 1 ? { name: g.whiteName } : { name: g.blackName }
}

// Re-seed the countdowns from authoritative server values whenever the game state changes.
watch(game, g => {
  moveRemaining.value = g?.moveSecondsRemaining ?? null
  gameRemaining.value = g?.gameSecondsRemaining ?? null
  recordResultIfNeeded(g) // tally the winner once when a game reaches a terminal state
})

async function loadOpponents(): Promise<void> {
  try {
    opponents.value = await gomokuApi.listOpponents()
  } catch {
    // non-fatal
  }
}

async function pollLobby(): Promise<void> {
  try {
    const data = await gomokuApi.getInvites()
    incoming.value = data.incoming
    outgoing.value = data.outgoing
    // If an invite we sent was accepted, the outgoing invite disappears and an active game exists.
    if (!game.value) {
      const active = await gomokuApi.getActiveGame()
      if (active) enterGame(active)
    }
  } catch {
    // non-fatal — keep polling
  }
}

async function sendInvite(): Promise<void> {
  if (!selectedOpponent.value) return
  inviting.value = true
  resetSeries() // a fresh invite from the lobby starts a new series at 0–0
  try {
    const g = await gomokuApi.invite(selectedOpponent.value)
    if (g.status === 'ACTIVE') {
      enterGame(g)
    } else {
      outgoing.value = [g, ...outgoing.value.filter(o => o.id !== g.id)]
      message.success(t('gomoku.inviteSent'))
    }
    selectedOpponent.value = undefined
  } catch (err: any) {
    message.error(err?.response?.data?.message || t('gomoku.inviteFailed'))
  } finally {
    inviting.value = false
  }
}

async function acceptInvite(g: GomokuGame): Promise<void> {
  // Accepting while a finished game is on screen is a rematch (continue the series); accepting
  // from the lobby (no game shown) is a fresh match, so reset the score.
  const isRematch = game.value != null
  actionLoading.value = true
  try {
    if (!isRematch) resetSeries()
    enterGame(await gomokuApi.accept(g.id))
  } catch (err: any) {
    message.error(err?.response?.data?.message || t('gomoku.actionFailed'))
  } finally {
    actionLoading.value = false
  }
}

async function declineInvite(g: GomokuGame): Promise<void> {
  actionLoading.value = true
  try {
    await gomokuApi.decline(g.id)
    incoming.value = incoming.value.filter(i => i.id !== g.id)
  } catch (err: any) {
    message.error(err?.response?.data?.message || t('gomoku.actionFailed'))
  } finally {
    actionLoading.value = false
  }
}

async function cancelInvite(g: GomokuGame): Promise<void> {
  actionLoading.value = true
  try {
    await gomokuApi.cancel(g.id)
    outgoing.value = outgoing.value.filter(o => o.id !== g.id)
  } catch (err: any) {
    message.error(err?.response?.data?.message || t('gomoku.actionFailed'))
  } finally {
    actionLoading.value = false
  }
}

/* ----- Rematch (from the game-over screen) --------------------------------- */

// Propose a rematch to the same opponent. Reuses invite(): a rematch is a fresh invite.
async function sendRematch(): Promise<void> {
  const oppo = opponentLogin.value
  if (!oppo) return
  rematchLoading.value = true
  try {
    const res = await gomokuApi.invite(oppo)
    if (res.status === 'ACTIVE') {
      enterGame(res)
      return
    }
    message.success(t('gomoku.inviteSent'))
    // Reconcile incoming/outgoing — handles both players clicking Rematch at once, where
    // invite() returns the opponent's existing pending invite (which we should accept).
    await pollRematch()
  } catch (err: any) {
    message.error(err?.response?.data?.message || t('gomoku.actionFailed'))
  } finally {
    rematchLoading.value = false
  }
}

async function declineRematch(inv: GomokuGame): Promise<void> {
  actionLoading.value = true
  try {
    await gomokuApi.decline(inv.id)
    rematchIncoming.value = null
  } catch (err: any) {
    message.error(err?.response?.data?.message || t('gomoku.actionFailed'))
  } finally {
    actionLoading.value = false
  }
}

async function cancelRematch(o: GomokuGame): Promise<void> {
  actionLoading.value = true
  try {
    await gomokuApi.cancel(o.id)
    rematchOutgoing.value = null
  } catch (err: any) {
    message.error(err?.response?.data?.message || t('gomoku.actionFailed'))
  } finally {
    actionLoading.value = false
  }
}

async function playOnline(r: number, c: number): Promise<void> {
  const g = game.value
  if (!g || !isMyTurn.value) {
    if (g && !isMyTurn.value && g.status === 'ACTIVE') message.info(t('gomoku.notYourTurn'))
    return
  }
  if (onlineBoard.value[r][c] !== 0) return
  try {
    game.value = await gomokuApi.move(g.id, r, c)
    if (onlineOver.value) stopGamePolling()
  } catch (err: any) {
    message.error(err?.response?.data?.message || t('gomoku.actionFailed'))
  }
}

async function resignOnline(): Promise<void> {
  const g = game.value
  if (!g) return
  actionLoading.value = true
  try {
    game.value = await gomokuApi.resign(g.id)
    stopGamePolling()
  } catch (err: any) {
    message.error(err?.response?.data?.message || t('gomoku.actionFailed'))
  } finally {
    actionLoading.value = false
  }
}

async function pollGame(): Promise<void> {
  const g = game.value
  if (!g) return
  try {
    const fresh = await gomokuApi.getGame(g.id)
    game.value = fresh
    if (fresh.status !== 'ACTIVE') stopGamePolling()
  } catch {
    // non-fatal
  }
}

function enterGame(g: GomokuGame): void {
  game.value = g
  stopLobbyPolling()
  if (g.status === 'ACTIVE') startGamePolling()
}

async function backToLobby(): Promise<void> {
  // Clean up any in-flight rematch handshake server-side before leaving: withdraw my own
  // pending rematch invite and decline any the opponent sent me. This stops them waiting on
  // a ghost offer (they get a notification via cancel/decline) and prevents an accepted
  // rematch from pulling me back into a game after I've left. Read the refs first — nulling
  // `game` triggers the onlineOver watcher, which clears them.
  const outgoing = rematchOutgoing.value
  const incoming = rematchIncoming.value
  if (outgoing) {
    try { await gomokuApi.cancel(outgoing.id) } catch { /* non-fatal — leaving anyway */ }
  }
  if (incoming) {
    try { await gomokuApi.decline(incoming.id) } catch { /* non-fatal — leaving anyway */ }
  }
  game.value = null
  rematchIncoming.value = null
  rematchOutgoing.value = null
  resetSeries() // leaving the match ends the series; the score resets to 0–0
  startLobbyPolling()
}

function startLobbyPolling(): void {
  if (lobbyTimer) return
  pollLobby()
  lobbyTimer = setInterval(pollLobby, 2000)
}

function stopLobbyPolling(): void {
  if (lobbyTimer) {
    clearInterval(lobbyTimer)
    lobbyTimer = null
  }
}

function startGamePolling(): void {
  if (gameTimer) return
  gameTimer = setInterval(pollGame, 1500)
}

function stopGamePolling(): void {
  if (gameTimer) {
    clearInterval(gameTimer)
    gameTimer = null
  }
}

// While the game is over, watch for the opponent's rematch response: a new active game
// (either side accepted) → enter it; otherwise surface the pending rematch invite between us.
async function pollRematch(): Promise<void> {
  const g = game.value
  if (!g) return
  try {
    const active = await gomokuApi.getActiveGame()
    if (active) {
      enterGame(active)
      return
    }
    const oppo = opponentLogin.value
    const data = await gomokuApi.getInvites()
    rematchIncoming.value = data.incoming.find(i => i.blackUsername === oppo || i.whiteUsername === oppo) ?? null
    rematchOutgoing.value = data.outgoing.find(o => o.blackUsername === oppo || o.whiteUsername === oppo) ?? null
  } catch {
    // non-fatal — keep watching
  }
}

function startRematchWatch(): void {
  if (rematchTimer) return
  pollRematch()
  rematchTimer = setInterval(pollRematch, 2000)
}

function stopRematchWatch(): void {
  if (rematchTimer) {
    clearInterval(rematchTimer)
    rematchTimer = null
  }
  rematchIncoming.value = null
  rematchOutgoing.value = null
}

// Run the rematch watch exactly while an online game is finished (and not while playing/in lobby).
watch(onlineOver, over => {
  if (over) startRematchWatch()
  else stopRematchWatch()
})

// Ticks the displayed countdowns down once per second between server polls, flooring at 0.
function tick(): void {
  if (game.value?.status === 'ACTIVE') {
    if (moveRemaining.value != null) moveRemaining.value = Math.max(0, moveRemaining.value - 1)
    if (gameRemaining.value != null) gameRemaining.value = Math.max(0, gameRemaining.value - 1)
  }
  const decInvite = (inv: GomokuGame) => {
    if (inv.inviteSecondsRemaining != null) inv.inviteSecondsRemaining = Math.max(0, inv.inviteSecondsRemaining - 1)
  }
  incoming.value.forEach(decInvite)
  outgoing.value.forEach(decInvite)
}

function startUiTicker(): void {
  if (uiTimer) return
  uiTimer = setInterval(tick, 1000)
}

function stopUiTicker(): void {
  if (uiTimer) {
    clearInterval(uiTimer)
    uiTimer = null
  }
}

function stopAllPolling(): void {
  stopLobbyPolling()
  stopGamePolling()
  stopUiTicker()
  stopRematchWatch()
}

async function enterOnline(): Promise<void> {
  startUiTicker()
  resetSeries() // entering/resuming online can't recover a prior series, so start clean
  await loadOpponents()
  const active = await gomokuApi.getActiveGame().catch(() => null)
  if (active) {
    enterGame(active)
  } else {
    startLobbyPolling()
  }
}

watch(mode, m => {
  if (m === 'online') {
    enterOnline()
  } else {
    stopAllPolling()
  }
})

onMounted(() => {
  if (mode.value === 'online') enterOnline()
})

onUnmounted(stopAllPolling)
</script>

<template>
  <div class="gomoku-view">
    <a-card :bordered="false">
      <template #title><AppstoreOutlined /> {{ t('gomoku.title') }}</template>

      <a-radio-group v-model:value="mode" button-style="solid" class="mode-switch">
        <a-radio-button value="local">{{ t('gomoku.modeLocal') }}</a-radio-button>
        <a-radio-button value="online">{{ t('gomoku.modeOnline') }}</a-radio-button>
      </a-radio-group>

      <!-- ===================== LOCAL MODE ===================== -->
      <template v-if="mode === 'local'">
        <a-alert :message="t('gomoku.hint')" type="info" show-icon banner class="hint" />
        <div class="toolbar">
          <div class="status">
            <span
              class="turn-dot"
              :class="{
                black: winner === 1 || (!gameOver && current === 1),
                white: winner === 2 || (!gameOver && current === 2),
              }"
            />
            <span class="status-text">{{ status }}</span>
          </div>
          <div class="scores">
            <a-tag color="default">{{ t('gomoku.black') }}: {{ scores.black }}</a-tag>
            <a-tag color="default">{{ t('gomoku.white') }}: {{ scores.white }}</a-tag>
          </div>
          <a-space>
            <a-button :disabled="!moves.length" @click="undo">
              <template #icon><RollbackOutlined /></template>
              {{ t('gomoku.undo') }}
            </a-button>
            <a-button type="primary" @click="restart">
              <template #icon><RedoOutlined /></template>
              {{ t('gomoku.restart') }}
            </a-button>
          </a-space>
        </div>
        <GomokuBoard :board="board" :last-move="lastMove" :disabled="gameOver" @place="place" />
      </template>

      <!-- ===================== ONLINE MODE ===================== -->
      <template v-else>
        <!-- Lobby: no active game -->
        <div v-if="!game" class="lobby">
          <a-card size="small" :title="t('gomoku.inviteFan')" class="lobby-card">
            <div class="invite-row">
              <a-select
                v-model:value="selectedOpponent"
                :placeholder="t('gomoku.selectFan')"
                style="min-width: 220px"
                :not-found-content="t('gomoku.noOpponents')"
                show-search
                option-filter-prop="label"
              >
                <a-select-option v-for="o in opponents" :key="o.login" :value="o.login" :label="o.name">
                  {{ o.name }} (@{{ o.login }})
                </a-select-option>
              </a-select>
              <a-button type="primary" :loading="inviting" :disabled="!selectedOpponent" @click="sendInvite">
                {{ t('gomoku.sendInvite') }}
              </a-button>
            </div>

            <div v-if="outgoing.length" class="outgoing">
              <div v-for="o in outgoing" :key="o.id" class="pending-row">
                <span>
                  {{ t('gomoku.waitingAccept', { name: opponentOf(o).name }) }}
                  <a-tag v-if="o.inviteSecondsRemaining !== null" color="default" class="expiry-tag">
                    {{ t('gomoku.expiresIn', { s: o.inviteSecondsRemaining }) }}
                  </a-tag>
                </span>
                <a-button size="small" :loading="actionLoading" @click="cancelInvite(o)">
                  {{ t('gomoku.cancel') }}
                </a-button>
              </div>
            </div>
          </a-card>

          <a-card size="small" :title="t('gomoku.incomingInvites')" class="lobby-card">
            <a-empty v-if="!incoming.length" :description="t('gomoku.noInvites')" />
            <div v-for="inv in incoming" :key="inv.id" class="invite-item">
              <span class="invite-from">
                {{ t('gomoku.invitedYou', { name: opponentOf(inv).name }) }}
                <a-tag v-if="inv.inviteSecondsRemaining !== null" color="default" class="expiry-tag">
                  {{ t('gomoku.expiresIn', { s: inv.inviteSecondsRemaining }) }}
                </a-tag>
              </span>
              <a-space>
                <a-button type="primary" size="small" :loading="actionLoading" @click="acceptInvite(inv)">
                  {{ t('gomoku.accept') }}
                </a-button>
                <a-button size="small" :loading="actionLoading" @click="declineInvite(inv)">
                  {{ t('gomoku.decline') }}
                </a-button>
              </a-space>
            </div>
          </a-card>
        </div>

        <!-- In-game -->
        <div v-else class="online-game">
          <div class="players">
            <span class="player" :class="{ active: !onlineOver && game.currentPlayer === 1 }">
              <span class="turn-dot black" />
              {{ game.blackName }}
              <a-tag v-if="game.myColor === 1" color="blue">{{ t('gomoku.you') }}</a-tag>
              <span class="win-tally" :title="t('gomoku.seriesScore')">{{ blackScore }}</span>
              <!-- Move clock belongs only to whoever is to move -->
              <span
                v-if="!onlineOver && game.currentPlayer === 1"
                class="move-clock"
                :class="{ urgent: moveRemaining !== null && moveRemaining <= 10 }"
              >⏱ {{ moveRemaining ?? game.moveTimeoutSeconds }}s</span>
            </span>
            <span class="vs">vs</span>
            <span class="player" :class="{ active: !onlineOver && game.currentPlayer === 2 }">
              <span class="turn-dot white" />
              {{ game.whiteName }}
              <a-tag v-if="game.myColor === 2" color="blue">{{ t('gomoku.you') }}</a-tag>
              <span class="win-tally" :title="t('gomoku.seriesScore')">{{ whiteScore }}</span>
              <span
                v-if="!onlineOver && game.currentPlayer === 2"
                class="move-clock"
                :class="{ urgent: moveRemaining !== null && moveRemaining <= 10 }"
              >⏱ {{ moveRemaining ?? game.moveTimeoutSeconds }}s</span>
            </span>
          </div>

          <div class="toolbar">
            <span class="status-text">{{ onlineStatus }}</span>
            <a-tag v-if="!onlineOver" color="default">{{ t('gomoku.roundTimeLeft', { time: mmss(gameRemaining) }) }}</a-tag>
            <a-space v-if="!onlineOver">
              <a-button danger :loading="actionLoading" @click="resignOnline">
                {{ t('gomoku.resign') }}
              </a-button>
            </a-space>
            <a-space v-else class="rematch-bar" wrap>
              <template v-if="rematchIncoming">
                <span class="rematch-text">{{ t('gomoku.rematchIncoming', { name: opponentOf(game).name }) }}</span>
                <a-button type="primary" :loading="actionLoading" @click="acceptInvite(rematchIncoming)">
                  {{ t('gomoku.accept') }}
                </a-button>
                <a-button :loading="actionLoading" @click="declineRematch(rematchIncoming)">
                  {{ t('gomoku.decline') }}
                </a-button>
              </template>
              <template v-else-if="rematchOutgoing">
                <span class="rematch-text">{{ t('gomoku.rematchWaiting', { name: opponentOf(game).name }) }}</span>
                <a-button :loading="actionLoading" @click="cancelRematch(rematchOutgoing)">
                  {{ t('gomoku.cancel') }}
                </a-button>
              </template>
              <a-button v-else type="primary" :loading="rematchLoading" @click="sendRematch">
                <template #icon><RedoOutlined /></template>
                {{ t('gomoku.rematch') }}
              </a-button>
              <a-button @click="backToLobby">
                {{ t('gomoku.backToLobby') }}
              </a-button>
            </a-space>
          </div>

          <GomokuBoard
            :board="onlineBoard"
            :last-move="onlineLastMove"
            :disabled="onlineOver || !isMyTurn || myTimeUp"
            @place="playOnline"
          />
        </div>
      </template>
    </a-card>
  </div>
</template>

<style scoped>
.gomoku-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.mode-switch {
  margin-bottom: 16px;
}
.hint {
  margin-bottom: 16px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.status {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 140px;
}
.turn-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1px solid #bbb;
  background: transparent;
  display: inline-block;
}
.turn-dot.black {
  background: radial-gradient(circle at 30% 30%, #666, #000);
  border-color: #000;
}
.turn-dot.white {
  background: radial-gradient(circle at 30% 30%, #fff, #cfcfcf);
  border-color: #bbb;
}
.status-text {
  font-weight: 600;
  font-size: 15px;
  margin-right: auto;
}
.scores {
  display: flex;
  gap: 4px;
  margin-right: auto;
}
.lobby {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 560px;
}
.lobby-card {
  border-radius: 8px;
}
.invite-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.outgoing {
  margin-top: 12px;
}
.pending-row,
.invite-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
.pending-row:last-child,
.invite-item:last-child {
  border-bottom: none;
}
.online-game {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.players {
  display: flex;
  align-items: center;
  gap: 16px;
  font-weight: 600;
}
.player {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}
.player.active {
  background: rgba(24, 144, 255, 0.1);
}
.move-clock {
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  color: #1890ff;
}
.move-clock.urgent {
  color: #ff4d4f;
}
.vs {
  color: #999;
  font-weight: 400;
}
.rematch-text {
  font-weight: 600;
}
.win-tally {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 11px;
  background: rgba(24, 144, 255, 0.12);
  color: #1890ff;
  font-size: 13px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
</style>
