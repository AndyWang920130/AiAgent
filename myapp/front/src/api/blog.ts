import http from '../utils/axios'
import type { Post } from '../stores/blog'

export interface BlogViewHistory {
  id: number
  username: string
  blogId: number
  blogTitle: string
  blogAuthor: string
  blogViewCount: number
  viewCount: number
  firstViewedDate: string
  lastViewedDate: string
}

export interface BlogLikeStatus {
  blogId: number
  totalLikes: number
  liked: boolean
  likedDate: string | null
}

export interface BlogLikeHistory {
  id: number
  username: string
  blogId: number
  blogTitle: string
  blogAuthor: string
  blogLikes: number
  likedDate: string
}

export interface BlogComment {
  id: number
  blogId: number
  username: string
  content: string
  createdDate: string
  canDelete: boolean
}

export function mapBlog(dto: any): Post {
  return {
    id: Number(dto.id),
    title: dto.title || '',
    excerpt: dto.summary || '',
    category: dto.category || '',
    date: dto.createdDate ? String(dto.createdDate).slice(0, 10) : '',
    views: dto.viewCount || 0,
    likes: dto.likes || 0,
    comments: dto.commentCount || 0,
    tag: dto.tag || '',
    tagColor: dto.tagColor || 'blue',
    author: dto.author || dto.createdBy || '',
    content: dto.content || '',
    visibility: dto.visibility || 'PUBLIC',
  }
}

export const blogApi = {
  list: () =>
    http.get('/api/v1/blogs', { params: { size: 100, sort: 'id,desc' } })
      .then(r => (r.data as any[]).map(mapBlog)),

  listMine: () =>
    http.get('/api/v1/blogs/my', { params: { size: 100, sort: 'id,desc' } })
      .then(r => (r.data as any[]).map(mapBlog)),

  get: (id: number) =>
    http.get(`/api/v1/blogs/${id}`).then(r => mapBlog(r.data)),

  create: (post: Omit<Post, 'id' | 'views' | 'likes' | 'comments' | 'date' | 'author'> & { author?: string }) =>
    http.post('/api/v1/blogs', {
      title: post.title,
      summary: post.excerpt,
      content: post.content,
      category: post.category,
      tag: post.tag,
      tagColor: post.tagColor,
      author: post.author,
      visibility: post.visibility || 'PUBLIC',
      status: 'PUBLISHED',
      viewCount: 0,
      likes: 0,
      commentCount: 0,
      deleted: false,
    }).then(r => mapBlog(r.data)),

  update: (id: number, post: Partial<Post>) =>
    http.put(`/api/v1/blogs/${id}`, {
      id,
      title: post.title,
      summary: post.excerpt,
      content: post.content,
      category: post.category,
      tag: post.tag,
      tagColor: post.tagColor,
      author: post.author,
      visibility: post.visibility || 'PUBLIC',
      viewCount: post.views,
      likes: post.likes,
      commentCount: post.comments,
      status: 'PUBLISHED',
      deleted: false,
    }).then(r => mapBlog(r.data)),

  incrementView: (id: number) =>
    http.post(`/api/v1/blogs/${id}/view`),

  listMyViewHistory: () =>
    http.get('/api/v1/blogs/view-history/my', { params: { size: 100, sort: 'lastViewedDate,desc' } })
      .then(r => r.data as BlogViewHistory[]),

  like: (id: number) =>
    http.post(`/api/v1/blogs/${id}/like`).then(r => r.data as BlogLikeStatus),

  getLikeStatus: (id: number) =>
    http.get(`/api/v1/blogs/${id}/like-status`).then(r => r.data as BlogLikeStatus),

  listMyLikeHistory: () =>
    http.get('/api/v1/blogs/like-history/my', { params: { size: 100, sort: 'likedDate,desc' } })
      .then(r => r.data as BlogLikeHistory[]),

  getMyLikesReceived: () =>
    http.get('/api/v1/blogs/likes-received/my')
      .then(r => Number(r.data.totalLikes || 0)),

  getStats: () =>
    http.get('/api/v1/blogs/stats').then(r => r.data as {
      totalPosts: number; totalViews: number; totalLikes: number; totalComments: number
    }),

  remove: (id: number) =>
    http.delete(`/api/v1/blogs/${id}`),

  getComments: (blogId: number) =>
    http.get(`/api/v1/blogs/${blogId}/comments`, { params: { size: 100, sort: 'createdDate,asc' } })
      .then(r => r.data as BlogComment[]),

  addComment: (blogId: number, content: string) =>
    http.post(`/api/v1/blogs/${blogId}/comments`, { content }).then(r => r.data as BlogComment),

  deleteComment: (commentId: number) =>
    http.delete(`/api/v1/comments/${commentId}`),
}
