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
  if (!response.ok) throw new Error("Unable to verify your session. Please try again.")
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
    if (response.status === 401) throw new Error("Incorrect password.")
    if (response.status === 429) throw new Error("Too many attempts. Please try again in 15 minutes.")
    throw new Error("Unable to sign in. Please try again later.")
  }
  const session = await readOwnerSession()
  if (!session.authenticated) throw new Error("Your session could not be saved. Please check your cookie settings.")
  window.dispatchEvent(new Event(AUTH_CHANGED))
}

export async function logoutOwner(allDevices = false) {
  const response = await fetch(`/api/auth/${allDevices ? "logout-all" : "logout"}`, { method: "POST" })
  if (!response.ok && response.status !== 401) throw new Error("Unable to sign out. Please try again.")
  csrfToken = null
  window.dispatchEvent(new Event(AUTH_REQUIRED))
  const channel = typeof BroadcastChannel === "undefined" ? null : new BroadcastChannel("cynexus-owner-auth")
  channel?.postMessage("recheck")
  channel?.close()
}
