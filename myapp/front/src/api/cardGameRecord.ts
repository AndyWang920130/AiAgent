import http from '../utils/axios'

/**
 * How a recorded match ended. Matches the backend `CardGameRecordOutcome` enum — deliberately
 * narrower than `CardGameStatus`, which distinguishes states that are the same outcome here.
 */
export type CardGameRecordOutcome = 'SCORED' | 'ABANDONED'

/** One seat's result, highest score first within a record. */
export interface CardGameRecordPlayerDTO {
  username: string
  /** Display name as it stands now; a CPU seat reads "CPU 1". */
  name: string
  seat: number
  score: number
  cpu: boolean
  winner: boolean
}

export interface CardGameRecordDTO {
  id: number
  /** The room it was played in. Several records can share this — a rematch is a new match. */
  gameId: number
  hostUsername: string
  hostName: string
  startedDate: string
  endedDate: string
  outcome: CardGameRecordOutcome
  /** Null on an abandoned match, which never reached scoring. */
  winnerUsername?: string | null
  winnerName?: string | null
  firstOutUsername?: string | null
  /** Who walked out, on an abandoned match. */
  leftByUsername?: string | null
  leftByName?: string | null
  trickCount: number
  playerCount: number
  players: CardGameRecordPlayerDTO[]
}

export interface AdminCardGameRecordQuery {
  /** Matched against the creator, any player, or the winner. */
  query?: string
  outcome?: CardGameRecordOutcome
  page?: number
  size?: number
}

export interface CardGameRecordPage {
  items: CardGameRecordDTO[]
  total: number
}

function toPage(r: {
  data: CardGameRecordDTO[]
  headers: Record<string, unknown>
}): CardGameRecordPage {
  return {
    items: r.data,
    total: Number(r.headers['x-total-count'] ?? r.data.length),
  }
}

export const cardGameRecordApi = {
  /**
   * One page of 70235 match records, most recently ended first. Administrators only; the
   * backend restricts the whole `/api/v1/admin` tree.
   */
  adminList: (params: AdminCardGameRecordQuery = {}) =>
    http
      .get('/api/v1/admin/card-game-records', {
        params: {
          query: params.query || undefined,
          outcome: params.outcome,
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      })
      .then(toPage),
}
