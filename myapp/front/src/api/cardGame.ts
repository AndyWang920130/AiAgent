import http from '../utils/axios'

/**
 * A card is two characters, rank then suit: ranks `4 6 8 9 T J Q K A 5 3 2` plus `X` for a
 * joker; suits `S H D C`, or `B`/`L` for the big/little joker. `T` displays as 10 and either
 * joker displays as 0.
 */
export type Card = string

export type CardShape = 'SINGLE' | 'PAIR' | 'TRIPLE' | 'QUAD' | 'FULL_HOUSE' | 'QUAD_PLUS_ONE'

export type CardGameStatus =
  | 'PENDING'
  | 'ACTIVE'
  /** Scored, seats still filled — the result stands while the players decide on another game. */
  | 'REMATCH'
  | 'FINISHED'
  | 'CANCELLED'
  | 'EXPIRED'
  | 'TIMED_OUT'
  | 'ABANDONED'

export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED'

/**
 * One seat at the table. There is no hand here by design — the server sends card counts for
 * everyone else and only the current user's own cards, on `CardGameState.myHand`.
 */
export interface CardGamePlayer {
  username: string
  name: string
  avatar: string | null
  seat: number
  cpu: boolean
  /**
   * This player has handed their turns to the server. Reported for every seat, not just your
   * own — it is what explains a seat answering instantly.
   */
  autoPlay: boolean
  invitationStatus: InvitationStatus
  handCount: number
  score: number
  passed: boolean
  out: boolean
  /**
   * Wants the next game, while the status is `REMATCH`. Reported for every seat, not just your
   * own, so the table can show who it is waiting on. True for CPU seats, which have nothing to
   * decide and read as "waiting on nobody here"; every human seat reports its own answer, the
   * host's included — the host agrees to another game the same way everyone else does. Always
   * false in any other status, where the question is not being asked.
   */
  ready: boolean
}

/** One action in the current trick: a play, or a pass. These cards are all face up. */
export interface CardGameTrickEntry {
  username: string
  name: string
  seat: number
  /** Empty for a pass. */
  cards: Card[]
  passed: boolean
  shape: CardShape | null
}

export interface CardGameState {
  id: number
  status: CardGameStatus
  hostUsername: string
  hostName: string
  deckCount: number
  trickNumber: number
  currentShape: CardShape | null
  currentPlay: Card[] | null
  currentPlayUsername: string | null
  currentPlayName: string | null
  trickPoints: number
  /**
   * Every play and pass in the current trick, oldest first. The server resolves all the CPU
   * seats between two human turns in one call, so this is the only way the intermediate plays
   * of a raised lead (8, then 9, then 10) are visible to a client.
   */
  trickHistory: CardGameTrickEntry[]
  /**
   * The trick `trickHistory` describes. Equal to `trickNumber` mid-trick, one behind it just
   * after a trick is scored — a trick is won by its last play, so that play stays part of the
   * trick it won rather than appearing under the next one.
   */
  trickHistoryNumber: number
  currentTurnUsername: string | null
  players: CardGamePlayer[]
  /** The current user's own cards, strongest first. */
  myHand: Card[]
  myUsername: string
  host: boolean
  myTurn: boolean
  canStart: boolean
  firstOutUsername: string | null
  winnerUsername: string | null
  leftByUsername: string | null
  createdDate: string
  turnTimeoutSeconds: number
  turnSecondsRemaining: number | null
  gameSecondsRemaining: number | null
  inviteSecondsRemaining: number | null
  /** How long the room stays open for another game. Only set while the status is `REMATCH`. */
  rematchSecondsRemaining: number | null
}

/** A follower the current user can invite (one-way follow is enough). */
export interface CardGameOpponent {
  username: string
  name: string
  avatar: string | null
}

export interface CardGameInvites {
  incoming: CardGameState[]
  outgoing: CardGameState[]
}

export const cardGameApi = {
  listOpponents: (): Promise<CardGameOpponent[]> =>
    http.get('/api/v1/card-games/opponents').then(r => r.data),

  createTable: (cpuCount: number, invitees: string[]): Promise<CardGameState> =>
    http.post('/api/v1/card-games/tables', { cpuCount, invitees }).then(r => r.data),

  getInvites: (): Promise<CardGameInvites> =>
    http.get('/api/v1/card-games/invites').then(r => r.data),

  accept: (id: number): Promise<CardGameState> =>
    http.post(`/api/v1/card-games/games/${id}/accept`).then(r => r.data),

  decline: (id: number): Promise<CardGameState> =>
    http.post(`/api/v1/card-games/games/${id}/decline`).then(r => r.data),

  cancel: (id: number): Promise<CardGameState> =>
    http.post(`/api/v1/card-games/games/${id}/cancel`).then(r => r.data),

  start: (id: number): Promise<CardGameState> =>
    http.post(`/api/v1/card-games/games/${id}/start`).then(r => r.data),

  play: (id: number, cards: Card[]): Promise<CardGameState> =>
    http.post(`/api/v1/card-games/games/${id}/plays`, { cards }).then(r => r.data),

  pass: (id: number): Promise<CardGameState> =>
    http.post(`/api/v1/card-games/games/${id}/pass`).then(r => r.data),

  /** Hand your own turns to the server, or take them back. Always your own seat. */
  setAutoPlay: (id: number, enabled: boolean): Promise<CardGameState> =>
    http.post(`/api/v1/card-games/games/${id}/auto-play`, { enabled }).then(r => r.data),

  /**
   * Say whether you want the next game, once this one has been scored. Always your own seat,
   * the host's being no different. When this is the last seat the room was waiting on, the
   * state that comes back is the next game already dealt — there is no separate call for it.
   */
  setReady: (id: number, ready: boolean): Promise<CardGameState> =>
    http.post(`/api/v1/card-games/games/${id}/ready`, { ready }).then(r => r.data),

  /** Leave the table. Ends the game, or closes the room, for everyone still in it. */
  leave: (id: number): Promise<CardGameState> =>
    http.post(`/api/v1/card-games/games/${id}/leave`).then(r => r.data),

  getGame: (id: number): Promise<CardGameState> =>
    http.get(`/api/v1/card-games/games/${id}`).then(r => r.data),

  // 204 No Content when the user has no active game → resolves to null.
  getActiveGame: (): Promise<CardGameState | null> =>
    http.get('/api/v1/card-games/games/active').then(r => (r.status === 204 ? null : r.data)),
}
