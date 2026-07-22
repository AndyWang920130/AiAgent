import { computed, ref } from 'vue'
import { gameConfigApi, type GameConfigDTO } from '../api/gameConfig'

export interface GamePrize {
  id: number
  name: string
  color: string
  sortOrder?: number | null
}

export interface GameParameter {
  id: number
  name: string
  value: string
  description: string
  sortOrder?: number | null
}

export const gamePrizes = ref<GamePrize[]>([])
export const gameParameters = ref<GameParameter[]>([])
export const gameConfigLoading = ref(false)

export const spinDurationSeconds = computed(() => {
  const raw = gameParameters.value.find(p => p.name === 'spinDurationSeconds')?.value
  const parsed = Number(raw)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 4
})

function mapPrize(dto: GameConfigDTO): GamePrize {
  return {
    id: Number(dto.id),
    name: dto.name || '',
    color: dto.value || '',
    sortOrder: dto.sortOrder,
  }
}

function mapParameter(dto: GameConfigDTO): GameParameter {
  return {
    id: Number(dto.id),
    name: dto.name || '',
    value: dto.value || '',
    description: dto.description || '',
    sortOrder: dto.sortOrder,
  }
}

export async function fetchGameConfig() {
  gameConfigLoading.value = true
  try {
    const [prizes, parameters] = await Promise.all([
      gameConfigApi.list('PRIZE'),
      gameConfigApi.list('PARAMETER'),
    ])
    gamePrizes.value = prizes.map(mapPrize)
    gameParameters.value = parameters.map(mapParameter)
  } finally {
    gameConfigLoading.value = false
  }
}

export async function addPrize(name: string, color: string) {
  const created = await gameConfigApi.create({
    type: 'PRIZE',
    name,
    value: color,
    sortOrder: nextSortOrder(gamePrizes.value),
  })
  gamePrizes.value.push(mapPrize(created))
}

export async function updatePrize(id: number, data: Partial<Omit<GamePrize, 'id'>>) {
  const existing = gamePrizes.value.find(prize => prize.id === id)
  if (!existing) return
  const updated = await gameConfigApi.update(id, {
    id,
    type: 'PRIZE',
    name: data.name ?? existing.name,
    value: data.color ?? existing.color,
    sortOrder: data.sortOrder ?? existing.sortOrder,
  })
  replaceById(gamePrizes.value, mapPrize(updated))
}

export async function removePrize(id: number) {
  await gameConfigApi.remove(id)
  gamePrizes.value = gamePrizes.value.filter(prize => prize.id !== id)
}

export async function addParameter(name: string, value: string, description = '') {
  const created = await gameConfigApi.create({
    type: 'PARAMETER',
    name,
    value,
    description,
    sortOrder: nextSortOrder(gameParameters.value),
  })
  gameParameters.value.push(mapParameter(created))
}

export async function updateParameter(id: number, data: Partial<Omit<GameParameter, 'id'>>) {
  const existing = gameParameters.value.find(parameter => parameter.id === id)
  if (!existing) return
  const updated = await gameConfigApi.update(id, {
    id,
    type: 'PARAMETER',
    name: data.name ?? existing.name,
    value: data.value ?? existing.value,
    description: data.description ?? existing.description,
    sortOrder: data.sortOrder ?? existing.sortOrder,
  })
  replaceById(gameParameters.value, mapParameter(updated))
}

export async function removeParameter(id: number) {
  await gameConfigApi.remove(id)
  gameParameters.value = gameParameters.value.filter(parameter => parameter.id !== id)
}

function nextSortOrder(items: Array<{ sortOrder?: number | null }>) {
  return Math.max(0, ...items.map(item => item.sortOrder || 0)) + 10
}

function replaceById<T extends { id: number }>(items: T[], updated: T) {
  const index = items.findIndex(item => item.id === updated.id)
  if (index !== -1) items[index] = updated
}
