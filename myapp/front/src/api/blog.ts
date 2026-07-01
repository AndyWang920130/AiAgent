import http from '../utils/axios'
import type { Post } from '../stores/blog'

function mapBlog(dto: any): Post {
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
    content: dto.content || '',
  }
}

export const blogApi = {
  list: () =>
    http.get('/api/v1/blogs', { params: { size: 100, sort: 'id,desc' } })
      .then(r => (r.data as any[]).map(mapBlog)),

  get: (id: number) =>
    http.get(`/api/v1/blogs/${id}`).then(r => mapBlog(r.data)),

  create: (post: Omit<Post, 'id' | 'views' | 'likes' | 'comments' | 'date'>) =>
    http.post('/api/v1/blogs', {
      title: post.title,
      summary: post.excerpt,
      content: post.content,
      category: post.category,
      tag: post.tag,
      tagColor: post.tagColor,
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
      viewCount: post.views,
      likes: post.likes,
      commentCount: post.comments,
      status: 'PUBLISHED',
      deleted: false,
    }).then(r => mapBlog(r.data)),

  incrementView: (id: number) =>
    http.post(`/api/v1/blogs/${id}/view`),

  remove: (id: number) =>
    http.delete(`/api/v1/blogs/${id}`),
}
