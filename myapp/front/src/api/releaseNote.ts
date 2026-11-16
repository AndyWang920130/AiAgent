import http from '../utils/axios'

/**
 * Whether a note is public. A DRAFT is only ever returned by the admin endpoint, so a
 * non-administrator never receives one. Matches the backend `ReleaseNoteStatus` enum.
 */
export type ReleaseNoteStatus = 'DRAFT' | 'PUBLISHED'

export interface ReleaseNoteDTO {
  id: number
  /** The release, as the administrator typed it — e.g. `1.4.0`. No format is imposed. */
  version: string
  title?: string | null
  /** HTML from the rich editor. */
  content: string
  /** When the release happened, which is what the list is ordered by. */
  releasedDate: string
  status: ReleaseNoteStatus
  /** Author's login. */
  author: string
  /** Author's current display name, falling back to the login. */
  authorName: string
  lastModifiedDate: string
}

/** Create and update send the same shape. `status` omitted means DRAFT. */
export interface ReleaseNoteSaveRequest {
  version: string
  title?: string
  content: string
  releasedDate: string
  status?: ReleaseNoteStatus
}

export interface ReleaseNotePage {
  items: ReleaseNoteDTO[]
  total: number
}

function toPage(r: { data: ReleaseNoteDTO[]; headers: Record<string, unknown> }): ReleaseNotePage {
  return {
    items: r.data,
    total: Number(r.headers['x-total-count'] ?? r.data.length),
  }
}

export const releaseNoteApi = {
  /** Published notes, newest release first. Any signed-in user. */
  list: (page = 0, size = 20) =>
    http.get('/api/v1/release-notes', { params: { page, size } }).then(toPage),

  /**
   * Newest published version, for the About page. Null until the first note is published,
   * which the caller renders as a placeholder rather than a wrong number.
   */
  currentVersion: () =>
    http
      .get('/api/v1/release-notes/current-version')
      .then(r => (r.data?.version as string | null) ?? null),

  /** Every note including drafts. Administrators only; the server enforces it. */
  adminList: (page = 0, size = 20) =>
    http.get('/api/v1/admin/release-notes', { params: { page, size } }).then(toPage),

  create: (data: ReleaseNoteSaveRequest) =>
    http.post('/api/v1/admin/release-notes', data).then(r => r.data as ReleaseNoteDTO),

  update: (id: number, data: ReleaseNoteSaveRequest) =>
    http.put(`/api/v1/admin/release-notes/${id}`, data).then(r => r.data as ReleaseNoteDTO),

  /** Publish or withdraw without resending the body. */
  setStatus: (id: number, status: ReleaseNoteStatus) =>
    http.put(`/api/v1/admin/release-notes/${id}/status`, { status }).then(r => r.data as ReleaseNoteDTO),

  remove: (id: number) => http.delete(`/api/v1/admin/release-notes/${id}`),
}
