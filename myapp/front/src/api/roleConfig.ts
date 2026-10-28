import http from '../utils/axios'

/**
 * A role in the catalog managed on the Role Config page. `code` is what gets stored on an
 * account and what the backend turns into a `ROLE_<CODE>` authority.
 */
export interface RoleConfigDTO {
  id?: number
  code: string
  name: string
  description?: string | null
  sortOrder?: number | null
  enabled?: boolean | null
  /** True for the seeded USER and ADMIN rows, which cannot be re-coded, disabled or deleted. */
  builtIn?: boolean | null
  /** Accounts currently holding this role. Read-only; the backend computes it. */
  userCount?: number | null
}

export const roleConfigApi = {
  /** The catalog in configured order; `enabledOnly` for the assignable subset. */
  list: (enabledOnly = false) =>
    http.get('/api/v1/role-configs', { params: enabledOnly ? { enabledOnly: true } : undefined })
      .then(r => r.data as RoleConfigDTO[]),

  create: (data: RoleConfigDTO) =>
    http.post('/api/v1/role-configs', data).then(r => r.data as RoleConfigDTO),

  update: (id: number, data: RoleConfigDTO) =>
    http.put(`/api/v1/role-configs/${id}`, { ...data, id }).then(r => r.data as RoleConfigDTO),

  remove: (id: number) =>
    http.delete(`/api/v1/role-configs/${id}`),
}
