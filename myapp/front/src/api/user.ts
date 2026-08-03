import http from '../utils/axios'
import { mapBlog } from './blog'
import type { Post } from '../stores/blog'

export interface PublicUserProfile {
  login: string
  name: string
  bio: string | null
  avatar: string | null
  gender: string | null
  joinDate: string | null
  postCount: number
  likesReceived: number
  achievementPoints: number
  followerCount: number
  followingCount: number
  following: boolean
}

export interface FollowStatus {
  username: string
  followerCount: number
  followingCount: number
  following: boolean
}

export interface FollowUser {
  login: string
  name: string
  avatar: string | null
  followedDate: string
}

export const userApi = {
  getProfile: (login: string): Promise<PublicUserProfile> =>
    http.get(`/api/v1/users/${encodeURIComponent(login)}`).then(r => r.data),

  getBlogs: (login: string): Promise<Post[]> =>
    http.get(`/api/v1/users/${encodeURIComponent(login)}/blogs`, { params: { size: 100, sort: 'id,desc' } })
      .then(r => (r.data as any[]).map(mapBlog)),

  getFollowStatus: (login: string): Promise<FollowStatus> =>
    http.get(`/api/v1/users/${encodeURIComponent(login)}/follow-status`).then(r => r.data),

  getFollowers: (login: string): Promise<FollowUser[]> =>
    http.get(`/api/v1/users/${encodeURIComponent(login)}/followers`, { params: { size: 100, sort: 'followedDate,desc' } })
      .then(r => r.data),

  getFollowing: (login: string): Promise<FollowUser[]> =>
    http.get(`/api/v1/users/${encodeURIComponent(login)}/following`, { params: { size: 100, sort: 'followedDate,desc' } })
      .then(r => r.data),

  follow: (login: string): Promise<FollowStatus> =>
    http.post(`/api/v1/users/${encodeURIComponent(login)}/follow`).then(r => r.data),

  unfollow: (login: string): Promise<FollowStatus> =>
    http.delete(`/api/v1/users/${encodeURIComponent(login)}/follow`).then(r => r.data),
}
