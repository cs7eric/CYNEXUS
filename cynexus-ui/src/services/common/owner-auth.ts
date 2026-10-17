export const AUTH_CHANGED = "cynexus:auth-changed"
export const AUTH_REQUIRED = "cynexus:auth-required"
let csrfToken: string | null = null
let installed = false

export type OwnerSession = { authenticated: boolean; csrfToken?: string; rememberDevice?: boolean }

/** Covers ordinary requests, uploads and fetch-based streams without sending secrets to other origins. */
export function installOwnerAuthTransport() {
  if (installed) return
  installed = true
  const originalFetch = window.fetch.bind(window)
  window.fetch = async (input, init) => {
    const url = new URL(input instanceof Request ? input.url : String(input), window.location.href)
    const protectedApi = url.origin === window.location.origin
      && (url.pathname.startsWith("/api/") || url.pathname.startsWith("/data-api/"))
    if (!protectedApi) return originalFetch(input, init)
    const effective = new Request(input instanceof Request ? input : url, init)
    const headers = new Headers(effective.headers)
    if (csrfToken && !["GET", "HEAD", "OPTIONS"].includes(effective.method)) {
      headers.set("X-Cynexus-CSRF", csrfToken)
    }
    const response = await originalFetch(new Request(effective, { headers, credentials: "same-origin" }))
    if (response.status === 401 && !url.pathname.startsWith("/api/auth/")) {
      csrfToken = null
      window.dispatchEvent(new Event(AUTH_REQUIRED))
    }
    return response
  }
}

export async function readOwnerSession(signal?: AbortSignal): Promise<OwnerSession> {
  const response = await fetch("/api/auth/session", { cache: "no-store", signal })
  if (!response.ok) throw new Error("暂时无法验证登录状态，请重试。")
  const session: OwnerSession = await response.json()
  csrfToken = session.authenticated ? session.csrfToken ?? null : null
  return session
}

export async function loginOwner(password: string, rememberDevice: boolean) {
  const response = await fetch("/api/auth/login", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ password, rememberDevice }),
  })
  if (!response.ok) {
    if (response.status === 401) throw new Error("密码不正确。")
    if (response.status === 429) throw new Error("尝试次数过多，请 15 分钟后重试。")
    throw new Error("暂时无法登录，请稍后重试。")
  }
  const session = await readOwnerSession()
  if (!session.authenticated) throw new Error("浏览器未能保存登录状态，请检查 Cookie 设置。")
  window.dispatchEvent(new Event(AUTH_CHANGED))
}

export async function logoutOwner(allDevices = false) {
  const response = await fetch(`/api/auth/${allDevices ? "logout-all" : "logout"}`, { method: "POST" })
  if (!response.ok && response.status !== 401) throw new Error("退出失败，请重试。")
  csrfToken = null
  window.dispatchEvent(new Event(AUTH_REQUIRED))
  const channel = typeof BroadcastChannel === "undefined" ? null : new BroadcastChannel("cynexus-owner-auth")
  channel?.postMessage("recheck")
  channel?.close()
}
