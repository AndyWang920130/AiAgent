import { computed, ref } from 'vue'
import { roleConfigApi, type RoleConfigDTO } from '../api/roleConfig'

export interface RoleConfigItem {
  id: number
  code: string
  name: string
  description: string
  sortOrder?: number | null
  enabled: boolean
  builtIn: boolean
  userCount: number
}

export const roleConfigs = ref<RoleConfigItem[]>([])
export const roleConfigLoading = ref(false)

/**
 * The roles an account may be given: enabled only, in configured order. User Management
 * fills its dropdown from this rather than from a hard-coded list.
 */
export const assignableRoles = computed(() =>
  roleConfigs.value.filter(role => role.enabled),
)

/** The configured display name for a stored role code, falling back to the code itself. */
export function roleLabel(code?: string | null) {
  if (!code) return ''
  return roleConfigs.value.find(role => role.code === code)?.name || code
}

function mapRole(dto: RoleConfigDTO): RoleConfigItem {
  return {
    id: Number(dto.id),
    code: dto.code || '',
    name: dto.name || '',
    description: dto.description || '',
    sortOrder: dto.sortOrder,
    enabled: dto.enabled !== false,
    builtIn: dto.builtIn === true,
    userCount: dto.userCount || 0,
  }
}

export async function fetchRoleConfigs(enabledOnly = false) {
  roleConfigLoading.value = true
  try {
    const list = await roleConfigApi.list(enabledOnly)
    roleConfigs.value = list.map(mapRole)
  } finally {
    roleConfigLoading.value = false
  }
}

export async function addRoleConfig(data: Pick<RoleConfigItem, 'code' | 'name' | 'description'>) {
  const created = await roleConfigApi.create({
    code: data.code,
    name: data.name,
    description: data.description,
    sortOrder: nextSortOrder(roleConfigs.value),
    enabled: true,
  })
  roleConfigs.value.push(mapRole(created))
}

export async function updateRoleConfig(id: number, data: Partial<Omit<RoleConfigItem, 'id'>>) {
  const existing = roleConfigs.value.find(role => role.id === id)
  if (!existing) return
  const updated = await roleConfigApi.update(id, {
    id,
    code: data.code ?? existing.code,
    name: data.name ?? existing.name,
    description: data.description ?? existing.description,
    sortOrder: data.sortOrder ?? existing.sortOrder,
    enabled: data.enabled ?? existing.enabled,
  })
  replaceById(roleConfigs.value, mapRole(updated))
}

export async function removeRoleConfig(id: number) {
  await roleConfigApi.remove(id)
  roleConfigs.value = roleConfigs.value.filter(role => role.id !== id)
}

function nextSortOrder(items: Array<{ sortOrder?: number | null }>) {
  return Math.max(0, ...items.map(item => item.sortOrder || 0)) + 10
}

function replaceById<T extends { id: number }>(items: T[], updated: T) {
  const index = items.findIndex(item => item.id === updated.id)
  if (index !== -1) items[index] = updated
}
