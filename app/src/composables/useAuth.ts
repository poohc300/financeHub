import { reactive, readonly } from 'vue'

interface AuthState {
  accessToken: string | null
  username: string | null
  roles: string[]
  permissions: string[]
}

const state = reactive<AuthState>({
  accessToken: null,
  username: null,
  roles: [],
  permissions: [],
})

export function useAuth() {
  function setAuth(token: string, username: string, roles: string[], permissions: string[]) {
    state.accessToken = token
    state.username = username
    state.roles = roles
    state.permissions = permissions
  }

  function clearAuth() {
    state.accessToken = null
    state.username = null
    state.roles = []
    state.permissions = []
  }

  function isAuthenticated(): boolean {
    return state.accessToken !== null
  }

  function hasPermission(permission: string): boolean {
    return state.permissions.includes(permission)
  }

  return {
    auth: readonly(state),
    setAuth,
    clearAuth,
    isAuthenticated,
    hasPermission,
  }
}
