import { computed, ref } from 'vue'
import { blogConfigApi, type BlogConfigDTO } from '../api/blogConfig'

export interface BlogCategory {
  id: number
  name: string
  description: string
  sortOrder?: number | null
}

export interface BlogTag {
  id: number
  name: string
  sortOrder?: number | null
}

export interface BlogTagColor {
  id: number
  name: string
  color: string
  sortOrder?: number | null
}

export const blogCategories = ref<BlogCategory[]>([])
export const blogTags = ref<BlogTag[]>([])
export const blogTagColors = ref<BlogTagColor[]>([])
export const blogConfigLoading = ref(false)

export const categoryOptions = computed(() => blogCategories.value.map(category => category.name))
export const tagOptions = computed(() => blogTags.value.map(tag => tag.name))
export const tagColorOptions = computed(() => blogTagColors.value.map(color => color.color))

function mapCategory(dto: BlogConfigDTO): BlogCategory {
  return {
    id: Number(dto.id),
    name: dto.name || '',
    description: dto.description || '',
    sortOrder: dto.sortOrder,
  }
}

function mapTag(dto: BlogConfigDTO): BlogTag {
  return {
    id: Number(dto.id),
    name: dto.name || '',
    sortOrder: dto.sortOrder,
  }
}

function mapTagColor(dto: BlogConfigDTO): BlogTagColor {
  return {
    id: Number(dto.id),
    name: dto.name || '',
    color: dto.value || '',
    sortOrder: dto.sortOrder,
  }
}

export async function fetchBlogConfig() {
  blogConfigLoading.value = true
  try {
    const [categories, tags, tagColors] = await Promise.all([
      blogConfigApi.list('CATEGORY'),
      blogConfigApi.list('TAG'),
      blogConfigApi.list('TAG_COLOR'),
    ])
    blogCategories.value = categories.map(mapCategory)
    blogTags.value = tags.map(mapTag)
    blogTagColors.value = tagColors.map(mapTagColor)
  } finally {
    blogConfigLoading.value = false
  }
}

export async function addCategory(name: string, description = '') {
  const created = await blogConfigApi.create({
    type: 'CATEGORY',
    name,
    description,
    sortOrder: nextSortOrder(blogCategories.value),
  })
  blogCategories.value.push(mapCategory(created))
}

export async function updateCategory(id: number, data: Partial<Omit<BlogCategory, 'id'>>) {
  const existing = blogCategories.value.find(category => category.id === id)
  if (!existing) return
  const updated = await blogConfigApi.update(id, {
    id,
    type: 'CATEGORY',
    name: data.name ?? existing.name,
    description: data.description ?? existing.description,
    sortOrder: data.sortOrder ?? existing.sortOrder,
  })
  replaceById(blogCategories.value, mapCategory(updated))
}

export async function removeCategory(id: number) {
  await blogConfigApi.remove(id)
  blogCategories.value = blogCategories.value.filter(category => category.id !== id)
}

export async function addTag(name: string) {
  const created = await blogConfigApi.create({
    type: 'TAG',
    name,
    sortOrder: nextSortOrder(blogTags.value),
  })
  blogTags.value.push(mapTag(created))
}

export async function updateTag(id: number, name: string) {
  const existing = blogTags.value.find(tag => tag.id === id)
  if (!existing) return
  const updated = await blogConfigApi.update(id, {
    id,
    type: 'TAG',
    name,
    sortOrder: existing.sortOrder,
  })
  replaceById(blogTags.value, mapTag(updated))
}

export async function removeTag(id: number) {
  await blogConfigApi.remove(id)
  blogTags.value = blogTags.value.filter(tag => tag.id !== id)
}

export async function addTagColor(name: string, color: string) {
  const created = await blogConfigApi.create({
    type: 'TAG_COLOR',
    name,
    value: color,
    sortOrder: nextSortOrder(blogTagColors.value),
  })
  blogTagColors.value.push(mapTagColor(created))
}

export async function updateTagColor(id: number, data: Partial<Omit<BlogTagColor, 'id'>>) {
  const existing = blogTagColors.value.find(color => color.id === id)
  if (!existing) return
  const updated = await blogConfigApi.update(id, {
    id,
    type: 'TAG_COLOR',
    name: data.name ?? existing.name,
    value: data.color ?? existing.color,
    sortOrder: data.sortOrder ?? existing.sortOrder,
  })
  replaceById(blogTagColors.value, mapTagColor(updated))
}

export async function removeTagColor(id: number) {
  await blogConfigApi.remove(id)
  blogTagColors.value = blogTagColors.value.filter(color => color.id !== id)
}

function nextSortOrder(items: Array<{ sortOrder?: number | null }>) {
  return Math.max(0, ...items.map(item => item.sortOrder || 0)) + 10
}

function replaceById<T extends { id: number }>(items: T[], updated: T) {
  const index = items.findIndex(item => item.id === updated.id)
  if (index !== -1) items[index] = updated
}
