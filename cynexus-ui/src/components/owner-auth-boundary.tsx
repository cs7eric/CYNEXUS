import { useEffect, useState, type FormEvent, type ReactNode } from "react"
import { Eye, EyeOff, Loader2, LockKeyhole, LogIn, RefreshCw } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { AUTH_CHANGED, AUTH_REQUIRED, loginOwner, readOwnerSession } from "@/services/common/owner-auth"

export function OwnerAuthBoundary({ children }: { children: ReactNode }) {
  const [state, setState] = useState<"loading" | "login" | "ready" | "error">("loading")
  const [retry, setRetry] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    let sequence = 0
    const check = async () => {
      const attempt = ++sequence
      try {
        const session = await readOwnerSession(controller.signal)
        if (!controller.signal.aborted && attempt === sequence) setState(session.authenticated ? "ready" : "login")
      } catch {
        if (!controller.signal.aborted && attempt === sequence) setState(current => current === "ready" ? current : "error")
      }
    }
    const expired = () => { sequence++; setState("login") }
    const focus = () => { if (document.visibilityState === "visible") void check() }
    const channel = typeof BroadcastChannel === "undefined" ? null : new BroadcastChannel("cynexus-owner-auth")
    if (channel) channel.onmessage = () => void check()
    void check()
    window.addEventListener(AUTH_CHANGED, check)
    window.addEventListener(AUTH_REQUIRED, expired)
    window.addEventListener("focus", focus)
    document.addEventListener("visibilitychange", focus)
    return () => {
      controller.abort()
      channel?.close()
      window.removeEventListener(AUTH_CHANGED, check)
      window.removeEventListener(AUTH_REQUIRED, expired)
      window.removeEventListener("focus", focus)
      document.removeEventListener("visibilitychange", focus)
    }
  }, [retry])
  if (state === "ready") return children
  if (state === "login") return <OwnerLogin />
  return (
    <main className="flex min-h-dvh items-center justify-center bg-background p-6 text-foreground">
      <div className="flex max-w-sm flex-col items-center gap-4 text-center" aria-live="polite">
        {state === "loading" ? <><Loader2 className="size-5 animate-spin" /><p className="text-sm">正在验证登录状态</p></>
          : <><p className="text-sm">暂时无法验证登录状态</p><Button variant="secondary" onClick={() => setRetry(value => value + 1)}><RefreshCw />重试</Button></>}
      </div>
    </main>
  )
}

function OwnerLogin() {
  const [password, setPassword] = useState("")
  const [remember, setRemember] = useState(true)
  const [visible, setVisible] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState("")
  async function submit(event: FormEvent) {
    event.preventDefault()
    if (busy) return
    setBusy(true)
    setError("")
    try { await loginOwner(password, remember) }
    catch (failure) { setError(failure instanceof Error ? failure.message : "登录失败，请重试。") }
    finally { setBusy(false) }
  }
  return (
    <main className="flex min-h-dvh items-center justify-center bg-background px-6 py-12 text-foreground">
      <section className="w-full max-w-[360px] min-w-0">
        <LockKeyhole className="mb-6 size-7 text-muted-foreground" aria-hidden="true" />
        <h1 className="text-2xl font-semibold tracking-normal">Cynexus</h1>
        <p className="mt-2 text-sm text-muted-foreground">登录个人工作空间</p>
        <form className="mt-8 flex flex-col gap-5" onSubmit={submit}>
          <input type="text" name="username" autoComplete="username" value="owner" readOnly hidden />
          <div className="space-y-2">
            <Label htmlFor="owner-password">访问密码</Label>
            <div className="relative">
              <Input id="owner-password" name="password" type={visible ? "text" : "password"}
                autoComplete="current-password" value={password} maxLength={1024} required
                disabled={busy} onChange={event => setPassword(event.target.value)}
                className="h-11 pr-12 text-base" aria-invalid={Boolean(error)} aria-describedby={error ? "login-error" : undefined} />
              <Button type="button" variant="ghost" size="icon" className="absolute right-1 top-1 h-9 w-9"
                aria-label={visible ? "隐藏密码" : "显示密码"} title={visible ? "隐藏密码" : "显示密码"}
                onClick={() => setVisible(value => !value)}>{visible ? <EyeOff /> : <Eye />}</Button>
            </div>
          </div>
          <div className="flex min-h-8 items-center gap-2">
            <Checkbox id="remember-owner" checked={remember} disabled={busy} onCheckedChange={value => setRemember(value === true)} />
            <Label htmlFor="remember-owner" className="cursor-pointer">记住此设备</Label>
          </div>
          {error && <p id="login-error" role="alert" className="text-sm text-destructive">{error}</p>}
          <Button type="submit" variant="default" disabled={busy || !password} className="h-11 w-full">
            {busy ? <Loader2 className="animate-spin" /> : <LogIn />}登录
          </Button>
        </form>
      </section>
    </main>
  )
}
