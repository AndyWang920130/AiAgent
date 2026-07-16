import { ref } from 'vue'
import { blogApi } from '../api/blog'

export interface Post {
  id: number
  title: string
  excerpt: string
  category: string
  date: string
  views: number
  likes: number
  comments: number
  tag: string
  tagColor: string
  author: string
  content: string
  visibility: 'PUBLIC' | 'PRIVATE'
}

export const posts = ref<Post[]>([])
export const myPosts = ref<Post[]>([])
export const loading = ref(false)
export const loadingMyPosts = ref(false)

export async function fetchPosts() {
  loading.value = true
  try {
    posts.value = await blogApi.list()
  } finally {
    loading.value = false
  }
}

export async function fetchMyPosts() {
  loadingMyPosts.value = true
  try {
    myPosts.value = await blogApi.listMine()
  } finally {
    loadingMyPosts.value = false
  }
}

export async function addPost(post: Omit<Post, 'id' | 'views' | 'likes' | 'comments' | 'date' | 'author'> & { author?: string }) {
  const created = await blogApi.create(post)
  posts.value.unshift(created)
  myPosts.value.unshift(created)
  return created
}

export async function updatePost(id: number, data: Partial<Omit<Post, 'id'>>) {
  const existing = posts.value.find(p => p.id === id)
  const merged: Partial<Post> = { ...existing, ...data, id }
  const updated = await blogApi.update(id, merged)
  const idx = posts.value.findIndex(p => p.id === id)
  if (idx !== -1) posts.value[idx] = updated
  const myIdx = myPosts.value.findIndex(p => p.id === id)
  if (myIdx !== -1) myPosts.value[myIdx] = updated
  return updated
}

export async function deletePost(id: number) {
  await blogApi.remove(id)
  posts.value = posts.value.filter(p => p.id !== id)
  myPosts.value = myPosts.value.filter(p => p.id !== id)
}

export async function getPost(id: number): Promise<Post | undefined> {
  const cached = posts.value.find(p => p.id === id)
  if (cached) return cached
  return blogApi.get(id)
}
