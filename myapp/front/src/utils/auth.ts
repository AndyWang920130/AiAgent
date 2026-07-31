const TOKEN_KEY = 'app_token'
const USER_KEY = 'app_user'
const TTL = 24 * 60 * 60 * 1000 // 1 day

interface StoredItem<T> {
  value: T
  expiry: number
}

export function setWithExpiry<T>(key: string, value: T): void {
  const item: StoredItem<T> = { value, expiry: Date.now() + TTL }
  localStorage.setItem(key, JSON.stringify(item))
}

export function getWithExpiry<T>(key: string): T | null {
  const raw = localStorage.getItem(key)
  if (!raw) return null
  const item: StoredItem<T> = JSON.parse(raw)
  if (Date.now() > item.expiry) {
    localStorage.removeItem(key)
    return null
  }
  return item.value
}

export function saveAuth(token: string, user: object): void {
  setWithExpiry(TOKEN_KEY, token)
  setWithExpiry(USER_KEY, user)
}

export function clearAuth(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getToken(): string | null {
  return getWithExpiry<string>(TOKEN_KEY)
}

export function getUser<T>(): T | null {
  return getWithExpiry<T>(USER_KEY)
}

// Update the stored user in place (keeps the existing TTL semantics) so name/email
// changes made on the profile page are reflected wherever the cached user is read.
export function setUser(user: object): void {
  setWithExpiry(USER_KEY, user)
}

export function isLoggedIn(): boolean {
  return !!getToken()
}
