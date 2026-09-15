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

// Page size for the home feeds. "Show more" fetches one more page of this size from the
// server, so the number of rendered posts stays bounded no matter how large the dataset grows.
export const PAGE_SIZE = 6

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

// --- Home "Recommended" feed (server-ranked by engagement, paged) ---
export const recommendedPosts = ref<Post[]>([])
export const recommendedTotal = ref(0)
export const loadingRecommended = ref(false)
let recommendedPage = 0

export async function fetchRecommended() {
  loadingRecommended.value = true
  try {
    const { items, total } = await blogApi.listRecommended({ page: 0, size: PAGE_SIZE })
    recommendedPosts.value = items
    recommendedTotal.value = total
    recommendedPage = 0
  } finally {
    loadingRecommended.value = false
  }
}

export async function loadMoreRecommended() {
  if (loadingRecommended.value) return
  loadingRecommended.value = true
  try {
    const next = recommendedPage + 1
    const { items, total } = await blogApi.listRecommended({ page: next, size: PAGE_SIZE })
    recommendedPosts.value = [...recommendedPosts.value, ...items]
    recommendedTotal.value = total
    recommendedPage = next
  } finally {
    loadingRecommended.value = false
  }
}

// --- Home "Following" feed (newest first, paged) ---
export const followingPosts = ref<Post[]>([])
export const followingTotal = ref(0)
export const loadingFollowing = ref(false)
let followingPage = 0

export async function fetchFollowingPosts() {
  loadingFollowing.value = true
  try {
    const { items, total } = await blogApi.listFollowing({ page: 0, size: PAGE_SIZE })
    followingPosts.value = items
    followingTotal.value = total
    followingPage = 0
  } finally {
    loadingFollowing.value = false
  }
}

export async function loadMoreFollowing() {
  if (loadingFollowing.value) return
  loadingFollowing.value = true
  try {
    const next = followingPage + 1
    const { items, total } = await blogApi.listFollowing({ page: next, size: PAGE_SIZE })
    followingPosts.value = [...followingPosts.value, ...items]
    followingTotal.value = total
    followingPage = next
  } finally {
    loadingFollowing.value = false
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
