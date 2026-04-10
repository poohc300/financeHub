const BASE_URL = '/api'

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  username: string
  roles: string[]
  permissions: string[]
}

export async function login(username: string, password: string): Promise<LoginResponse> {
  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ username, password }),
  })
  if (!res.ok) {
    const msg = res.status === 401 ? '아이디 또는 비밀번호가 올바르지 않습니다.' : `로그인 실패 (${res.status})`
    throw new Error(msg)
  }
  return res.json()
}

export async function refreshToken(): Promise<LoginResponse> {
  const res = await fetch(`${BASE_URL}/auth/refresh`, {
    method: 'POST',
    credentials: 'include',
  })
  if (!res.ok) throw new Error('Token refresh failed')
  return res.json()
}

export async function logout(): Promise<void> {
  await fetch(`${BASE_URL}/auth/logout`, {
    method: 'POST',
    credentials: 'include',
  })
}
