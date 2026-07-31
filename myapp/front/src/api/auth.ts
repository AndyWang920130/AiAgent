import http from '../utils/axios'

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export interface ResetPasswordRequest {
  email: string
  code: string
  newPassword: string
}

export interface ProfileResponse {
  username: string
  name: string
  email: string
  bio: string | null
  joinDate: string | null
}

export interface UpdateProfileRequest {
  name: string
  email: string
  bio: string
}

export const authApi = {
  changePassword: (data: ChangePasswordRequest) =>
    http.post('/api/v1/auth/change-password', data).then(r => r.data),

  getProfile: (): Promise<ProfileResponse> =>
    http.get('/api/v1/auth/profile').then(r => r.data),

  updateProfile: (data: UpdateProfileRequest): Promise<ProfileResponse> =>
    http.put('/api/v1/auth/profile', data).then(r => r.data),

  forgotPassword: (email: string) =>
    http.post('/api/v1/auth/forgot-password', { email }).then(r => r.data),

  resetPassword: (data: ResetPasswordRequest) =>
    http.post('/api/v1/auth/reset-password', data).then(r => r.data),
}
