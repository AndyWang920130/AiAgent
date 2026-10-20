import { ref } from 'vue'
import {
  adminUserApi,
  type AdminCreateUserRequest,
  type AdminUpdateUserRequest,
  type AdminUserDTO,
  type AdminUserQuery,
  type Role,
} from '../api/adminUser'

export const users = ref<AdminUserDTO[]>([])
export const total = ref(0)
export const loading = ref(false)

/** Current list filters, kept here so the table and the filter bar stay in step. */
export const query = ref('')
export const roleFilter = ref<Role | undefined>(undefined)
export const disabledFilter = ref<boolean | undefined>(undefined)
export const page = ref(1)
export const pageSize = ref(20)

export async function fetchUsers() {
  loading.value = true
  try {
    const params: AdminUserQuery = {
      query: query.value,
      role: roleFilter.value,
      disabled: disabledFilter.value,
      page: page.value - 1,
      size: pageSize.value,
    }
    const result = await adminUserApi.list(params)
    users.value = result.items
    total.value = result.total
  } finally {
    loading.value = false
  }
}

/** Apply a filter change: results move back to the first page. */
export async function applyFilters() {
  page.value = 1
  await fetchUsers()
}

export async function createUser(data: AdminCreateUserRequest) {
  const created = await adminUserApi.create(data)
  await fetchUsers()
  return created
}

export async function updateUser(id: number, data: AdminUpdateUserRequest) {
  const updated = await adminUserApi.update(id, data)
  patch(updated)
  return updated
}

export async function setUserStatus(id: number, deleted: boolean, reason?: string) {
  const updated = await adminUserApi.setStatus(id, deleted, reason)
  patch(updated)
  return updated
}

export async function resetUserPassword(id: number, newPassword: string) {
  await adminUserApi.resetPassword(id, newPassword)
}

/** Replace one row in place so the table doesn't jump after an edit. */
function patch(user: AdminUserDTO) {
  const index = users.value.findIndex(u => u.id === user.id)
  if (index >= 0) {
    users.value[index] = user
  }
}
