import http from '../utils/axios'

export interface GomokuGame {
  id: number
  blackUsername: string
  blackName: string
  blackAvatar: string | null
  whiteUsername: string
  whiteName: string
  whiteAvatar: string | null
  status: 'PENDING' | 'ACTIVE' | 'FINISHED' | 'DECLINED' | 'CANCELLED' | 'RESIGNED' | 'EXPIRED' | 'TIMED_OUT'
  board: string // 225 chars of '0' | '1' | '2', row-major
  currentPlayer: number // 1 = black, 2 = white
  winner: number | null // 1, 2, or 0 (draw); null while unresolved
  lastMoveRow: number | null
  lastMoveCol: number | null
  moveCount: number
  myColor: number // 1 or 2 — which side the current user plays
  createdDate: string
  moveTimeoutSeconds: number // full per-move budget
  moveSecondsRemaining: number | null // remaining move clock (ACTIVE only)
  gameSecondsRemaining: number | null // remaining round clock (ACTIVE only)
  inviteSecondsRemaining: number | null // remaining invite clock (PENDING only)
}

export interface GomokuOpponent {
  login: string
  name: string
  avatar: string | null
}

export interface GomokuInvites {
  incoming: GomokuGame[]
  outgoing: GomokuGame[]
}

export const gomokuApi = {
  listOpponents: (): Promise<GomokuOpponent[]> =>
    http.get('/api/v1/gomoku/opponents').then(r => r.data),

  invite: (opponent: string): Promise<GomokuGame> =>
    http.post('/api/v1/gomoku/invites', { opponent }).then(r => r.data),

  getInvites: (): Promise<GomokuInvites> =>
    http.get('/api/v1/gomoku/invites').then(r => r.data),

  accept: (id: number): Promise<GomokuGame> =>
    http.post(`/api/v1/gomoku/games/${id}/accept`).then(r => r.data),

  decline: (id: number): Promise<GomokuGame> =>
    http.post(`/api/v1/gomoku/games/${id}/decline`).then(r => r.data),

  cancel: (id: number): Promise<GomokuGame> =>
    http.post(`/api/v1/gomoku/games/${id}/cancel`).then(r => r.data),

  resign: (id: number): Promise<GomokuGame> =>
    http.post(`/api/v1/gomoku/games/${id}/resign`).then(r => r.data),

  getGame: (id: number): Promise<GomokuGame> =>
    http.get(`/api/v1/gomoku/games/${id}`).then(r => r.data),

  move: (id: number, row: number, col: number): Promise<GomokuGame> =>
    http.post(`/api/v1/gomoku/games/${id}/moves`, { row, col }).then(r => r.data),

  // 204 No Content when the user has no active game → resolves to null.
  getActiveGame: (): Promise<GomokuGame | null> =>
    http.get('/api/v1/gomoku/games/active').then(r => (r.status === 204 ? null : r.data)),
}
