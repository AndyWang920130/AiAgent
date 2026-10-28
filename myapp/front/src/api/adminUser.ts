import http from '../utils/axios'

/**
 * A role code from the Role Config catalog, not a closed set: roles are rows in the
 * catalog, so a code added on the Role Config page is assignable here too. The backend
 * rejects any code that does not name an enabled role.
 */
export type Role = string
export type Gender = 'MALE' | 'FEMALE'
export type UserType = 'WEB' | 'APP' | 'MINI_PROGRAM'

/**
 * Wire shape of an account as seen by an administrator. There is deliberately no
 * password field: the stored value is a hash and the API never returns it.
 */
export interface AdminUserDTO {
  id: number
  login: string
  realName?: string | null
  nickName?: string | null
  gender?: Gender | null
  userType?: UserType | null
  role?: Role | null
  phoneNumber?: string | null
  email?: string | null
  avatar?: string | null
  description?: string | null
  /** True when the account is disabled and cannot sign in. */
  deleted?: boolean | null
  deleteReason?: string | null
  createdDate?: string | null
  lastModifiedBy?: string | null
  lastModifiedDate?: string | null
}

export interface AdminUserQuery {
  query?: string
  role?: Role
  /** true for disabled accounts only, false for active only, undefined for all. */
  disabled?: boolean
  page?: number
  size?: number
}

export interface AdminUserPage {
  items: AdminUserDTO[]
  total: number
}

export interface AdminCreateUserRequest {
  login: string
  realName: string
  email: string
  password: string
  role: Role
  nickName?: string
  gender?: Gender
  userType?: UserType
  phoneNumber?: string
  description?: string
}

export interface AdminUpdateUserRequest {
  realName: string
  email: string
  role: Role
  nickName?: string
  gender?: Gender
  userType?: UserType
  phoneNumber?: string
  description?: string
}

export const adminUserApi = {
  /** One page of accounts; the total count comes back in the X-Total-Count header. */
  list: (params: AdminUserQuery = {}) =>
    http
      .get('/api/v1/admin/users', {
        params: {
          query: params.query || undefined,
          role: params.role,
          disabled: params.disabled,
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      })
      .then(
        r =>
          ({
            items: r.data as AdminUserDTO[],
            total: Number(r.headers['x-total-count'] ?? (r.data as AdminUserDTO[]).length),
          }) as AdminUserPage,
      ),

  get: (id: number) =>
    http.get(`/api/v1/admin/users/${id}`).then(r => r.data as AdminUserDTO),

  create: (data: AdminCreateUserRequest) =>
    http.post('/api/v1/admin/users', data).then(r => r.data as AdminUserDTO),

  update: (id: number, data: AdminUpdateUserRequest) =>
    http.put(`/api/v1/admin/users/${id}`, data).then(r => r.data as AdminUserDTO),

  /** Disable or re-enable an account. The backend refuses self-lockout and last-admin changes. */
  setStatus: (id: number, deleted: boolean, reason?: string) =>
    http
      .put(`/api/v1/admin/users/${id}/status`, { deleted, reason })
      .then(r => r.data as AdminUserDTO),

  resetPassword: (id: number, newPassword: string) =>
    http.post(`/api/v1/admin/users/${id}/reset-password`, { newPassword }),
}
