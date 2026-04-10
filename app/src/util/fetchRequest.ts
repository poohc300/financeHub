import { useAuth } from '../composables/useAuth'
import { refreshToken } from '../api/authAPI'
import router from '../router'

const BASE_URL = '/api'

async function doFetch(url: string, options: RequestInit): Promise<Response> {
  const { auth } = useAuth()
  const headers = new Headers(options.headers as HeadersInit)
  if (auth.accessToken) {
    headers.set('Authorization', `Bearer ${auth.accessToken}`)
  }
  return fetch(url, { ...options, headers, credentials: 'include' })
}

export async function fetchRequest<T>(endpoint: string, method: string = 'GET', body?: unknown): Promise<T> {
  const { setAuth, clearAuth } = useAuth()

  const options: RequestInit = {
    method,
    headers: { 'Content-Type': 'application/json' },
  }
  if (body) {
    options.body = JSON.stringify(body)
  }

  let response = await doFetch(`${BASE_URL}${endpoint}`, options)

  // 401: Access Token 만료 → Refresh 시도 후 1회 재요청
  if (response.status === 401) {
    try {
      const refreshed = await refreshToken()
      setAuth(refreshed.accessToken, refreshed.username, refreshed.roles, refreshed.permissions)
      response = await doFetch(`${BASE_URL}${endpoint}`, options)
    } catch {
      clearAuth()
      router.push('/login')
      throw new Error('Session expired. Please log in again.')
    }
  }

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }

  return response.json()
}
