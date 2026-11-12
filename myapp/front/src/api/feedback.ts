import http from '../utils/axios'

/** What a submission is about. Matches the backend `FeedbackCategory` enum. */
export type FeedbackCategory = 'BUG' | 'FEATURE' | 'CONTENT' | 'OTHER'

/** Where a submission stands. NEW and IN_PROGRESS both count as open. */
export type FeedbackStatus = 'NEW' | 'IN_PROGRESS' | 'RESOLVED'

export interface FeedbackDTO {
  id: number
  /** Submitter's login. */
  username: string
  /** Submitter's display name, falling back to the login. */
  name: string
  category: FeedbackCategory
  title: string
  content: string
  contact?: string | null
  status: FeedbackStatus
  /** Null until an administrator answers. */
  reply?: string | null
  repliedBy?: string | null
  repliedDate?: string | null
  createdDate: string
}

export interface FeedbackCreateRequest {
  category: FeedbackCategory
  title: string
  content: string
  contact?: string
}

/** An administrator's answer and/or status move; the backend refuses a request with neither. */
export interface FeedbackReplyRequest {
  reply?: string
  status?: FeedbackStatus
}

export interface AdminFeedbackQuery {
  query?: string
  status?: FeedbackStatus
  category?: FeedbackCategory
  page?: number
  size?: number
}

export interface FeedbackPage {
  items: FeedbackDTO[]
  total: number
}

function toPage(r: { data: FeedbackDTO[]; headers: Record<string, unknown> }): FeedbackPage {
  return {
    items: r.data,
    total: Number(r.headers['x-total-count'] ?? r.data.length),
  }
}

export const feedbackApi = {
  /** Send feedback to the administrators. */
  submit: (data: FeedbackCreateRequest) =>
    http.post('/api/v1/feedbacks', data).then(r => r.data as FeedbackDTO),

  /** One page of the caller's own submissions, newest first. */
  listMine: (page = 0, size = 20) =>
    http.get('/api/v1/feedbacks/mine', { params: { page, size } }).then(toPage),

  /** Withdraw one's own submission. Refused once it has been answered. */
  withdraw: (id: number) => http.delete(`/api/v1/feedbacks/${id}`),

  /** Administrator triage listing; the total count comes back in X-Total-Count. */
  adminList: (params: AdminFeedbackQuery = {}) =>
    http
      .get('/api/v1/admin/feedbacks', {
        params: {
          query: params.query || undefined,
          status: params.status,
          category: params.category,
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      })
      .then(toPage),

  /** Submissions still awaiting attention, for the triage page's badge. */
  openCount: () =>
    http.get('/api/v1/admin/feedbacks/open-count').then(r => Number(r.data.count || 0)),

  /** Answer and/or move a submission. A reply with no status resolves it. */
  reply: (id: number, data: FeedbackReplyRequest) =>
    http.put(`/api/v1/admin/feedbacks/${id}/reply`, data).then(r => r.data as FeedbackDTO),

  /** Remove a submission outright. Used for spam; the submitter is not notified. */
  remove: (id: number) => http.delete(`/api/v1/admin/feedbacks/${id}`),
}
