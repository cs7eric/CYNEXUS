import { useEffect, useEffectEvent, useRef, useState, type FormEvent } from "react"
import { Input } from "@heroui/react/input"
import { TextArea } from "@heroui/react/textarea"
import { Check, CirclePause, Clock3, FilePlus2, LoaderCircle, Play, RefreshCw, ShieldCheck, Square, ThumbsDown, ThumbsUp, X } from "lucide-react"

import { MobileButton } from "@/mobile/ui/button"
import {
  confirmAgentWrite, controlAgentTask, fetchAgentFeedback, fetchAgentTasks, previewAgentWrite,
  rejectAgentWrite, scheduleAgentTask, submitAgentFeedback,
  type AgentFeedback, type AgentRun, type AgentTask, type AgentWritePreview,
} from "@/services/ai/agent"
import "./agent-panels.css"

function errorMessage(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback
}

function futureScheduleTimestamp(value: string) {
  const timestamp = Date.parse(value)
  return Number.isFinite(timestamp) && timestamp > Date.now() ? timestamp : null
}

function draftPreview(preview: AgentWritePreview | null) {
  if (!preview || preview.toolCode !== "knowledge.document.create") return null
  try {
    const value: unknown = JSON.parse(preview.argumentsJson)
    if (!value || typeof value !== "object" || Array.isArray(value)) return null
    const draft = value as Record<string, unknown>
    if (typeof draft.title !== "string" || !draft.title.trim() || typeof draft.content !== "string" || !draft.content.trim()) return null
    if (draft.contentFormat !== "markdown" || draft.status !== "DRAFT") return null
    return { title: draft.title, content: draft.content }
  } catch {
    return null
  }
}

function useMounted() {
  const mounted = useRef(false)
  useEffect(() => { mounted.current = true; return () => { mounted.current = false } }, [])
  return mounted
}

type BusyPanelProps = { onBusyChange?: (busy: boolean) => void }

function useBusyChange(busy: boolean, onBusyChange: BusyPanelProps["onBusyChange"]) {
  const reportBusy = useEffectEvent((nextBusy: boolean) => onBusyChange?.(nextBusy))
  useEffect(() => { reportBusy(busy) }, [busy])
  useEffect(() => () => { reportBusy(false) }, [])
}

type WritePanelProps = BusyPanelProps & { conversationNo?: string | null; runNo?: string | null; answer?: string | null }

export function MobileAgentWritePanel(props: WritePanelProps) {
  return <WritePanel key={`${props.conversationNo ?? "new"}:${props.runNo ?? "new"}`} {...props} />
}

function WritePanel({ conversationNo, runNo, answer, onBusyChange }: WritePanelProps) {
  const [title, setTitle] = useState("")
  const [content, setContent] = useState(() => answer ?? "")
  const [preview, setPreview] = useState<AgentWritePreview | null>(null)
  const [expired, setExpired] = useState(false)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [failed, setFailed] = useState(false)
  const operation = useRef(false)
  const mounted = useMounted()
  const reviewedDraft = draftPreview(preview)
  useBusyChange(busy, onBusyChange)

  useEffect(() => {
    if (!preview) return
    const expiry = Date.parse(preview.expiresAt)
    const timer = window.setTimeout(() => setExpired(true), Number.isFinite(expiry) ? Math.max(0, expiry - Date.now()) : 0)
    return () => window.clearTimeout(timer)
  }, [preview])

  function edit(field: "title" | "content", value: string) {
    if (field === "title") setTitle(value)
    else setContent(value)
    setPreview(null)
    setExpired(false)
    setMessage(null)
  }

  async function createPreview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (operation.current || !title.trim() || !content.trim()) return
    operation.current = true
    setBusy(true); setMessage(null); setFailed(false)
    try {
      const next = await previewAgentWrite({
        toolCode: "knowledge.document.create", conversationNo: conversationNo ?? undefined, runNo: runNo ?? undefined,
        argumentsJson: JSON.stringify({ title: title.trim(), content, contentFormat: "markdown", status: "DRAFT" }),
      })
      if (!mounted.current) return
      setPreview(next)
      setExpired(false)
    } catch (error) {
      if (mounted.current) { setFailed(true); setMessage(errorMessage(error, "Unable to prepare the draft preview.")) }
    } finally {
      operation.current = false
      if (mounted.current) setBusy(false)
    }
  }

  async function finishPreview(action: "confirm" | "reject") {
    if (!preview || operation.current) return
    if (action === "confirm" && !draftPreview(preview)) {
      setFailed(true); setMessage("The draft preview is unavailable. Prepare it again before confirming."); return
    }
    if (action === "confirm" && (!Number.isFinite(Date.parse(preview.expiresAt)) || Date.parse(preview.expiresAt) <= Date.now())) {
      setExpired(true); setFailed(true); setMessage("This preview has expired. Prepare a new preview before confirming."); return
    }
    operation.current = true
    setBusy(true); setMessage(null); setFailed(false)
    try {
      if (action === "confirm") {
        const result = await confirmAgentWrite(preview.commandNo, preview.confirmationToken)
        if (!mounted.current) return
        if (result.status !== "EXECUTED") throw new Error(result.errorMessage || `Draft was not created (${result.status}).`)
        setTitle(""); setContent(""); setPreview(null); setMessage("Draft created in Knowledge Base.")
      } else {
        const result = await rejectAgentWrite(preview.commandNo)
        if (!mounted.current) return
        if (result.status !== "REJECTED") throw new Error(result.errorMessage || `Unable to reject the preview (${result.status}).`)
        setPreview(null); setMessage("Preview rejected. No draft was created.")
      }
    } catch (error) {
      if (mounted.current) { setFailed(true); setMessage(errorMessage(error, "Unable to update the draft preview.")) }
    } finally {
      operation.current = false
      if (mounted.current) setBusy(false)
    }
  }

  return <section className="ma-panels ma-panel-write" aria-labelledby="ma-write-title">
    <header className="ma-panel-heading"><FilePlus2 aria-hidden="true" /><h3 id="ma-write-title">Save to Knowledge Base</h3></header>
    <p className="ma-panel-description">Review the draft first. It is created only after you confirm.</p>
    <form className="ma-panel-form" onSubmit={(event) => void createPreview(event)}>
      <label className="ma-panel-field"><span>Title</span><Input className="ma-panel-control" value={title} onChange={(event) => edit("title", event.target.value)} placeholder="Draft title" maxLength={240} required disabled={busy} /></label>
      <label className="ma-panel-field"><span>Content</span><TextArea className="ma-panel-control" value={content} onChange={(event) => edit("content", event.target.value)} placeholder="Write or paste draft content" maxLength={50000} rows={6} required disabled={busy} /></label>
      {preview ? <div className="ma-panel-preview">
        <h4><ShieldCheck aria-hidden="true" />Review this draft</h4>
        {reviewedDraft ? <>
          <p className="ma-preview-context">Knowledge Base · Draft</p>
          <h5 className="ma-preview-title">{reviewedDraft.title}</h5>
          <pre>{reviewedDraft.content}</pre>
        </> : <p className="ma-panel-message is-error" role="alert">Preview unavailable. Prepare the draft again before confirming.</p>}
        <p>{expired ? "Preview expired. Prepare a new preview to continue." : `Valid until ${new Date(preview.expiresAt).toLocaleString("en-US")}.`}</p>
        <div className="ma-panel-actions">
          {expired || !reviewedDraft ? <MobileButton type="submit" variant="secondary" className="ma-panel-button" isDisabled={busy}>Prepare again</MobileButton> : <MobileButton type="button" variant="primary" className="ma-panel-button" isDisabled={busy} onPress={() => void finishPreview("confirm")}><Check aria-hidden="true" />Confirm draft</MobileButton>}
          <MobileButton type="button" variant="secondary" className="ma-panel-button" isDisabled={busy} onPress={() => void finishPreview("reject")}><X aria-hidden="true" />Reject</MobileButton>
        </div>
      </div> : <MobileButton type="submit" variant="secondary" className="ma-panel-button" isDisabled={busy || !title.trim() || !content.trim()}>{busy && <LoaderCircle className="ma-panel-spinner" aria-hidden="true" />}Preview draft</MobileButton>}
    </form>
    {message && <p className={`ma-panel-message${failed ? " is-error" : ""}`} role={failed ? "alert" : "status"}>{message}</p>}
  </section>
}

export function MobileAgentTaskPanel({ onBusyChange }: BusyPanelProps) {
  const [tasks, setTasks] = useState<AgentTask[]>([])
  const [input, setInput] = useState("")
  const [scheduleAt, setScheduleAt] = useState("")
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [busyTasks, setBusyTasks] = useState<Set<string>>(() => new Set())
  const [loadError, setLoadError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [failed, setFailed] = useState(false)
  const request = useRef(0)
  const scheduleBusy = useRef(false)
  const taskOperations = useRef(new Set<string>())
  const mounted = useRef(false)
  useBusyChange(saving || busyTasks.size > 0, onBusyChange)

  useEffect(() => { mounted.current = true; return () => { mounted.current = false } }, [])

  async function reload() {
    const version = ++request.current
    try {
      const next = await fetchAgentTasks()
      if (mounted.current && version === request.current) setTasks(next)
    } catch (error) {
      if (mounted.current && version === request.current) setLoadError(errorMessage(error, "Unable to load scheduled tasks."))
    } finally {
      if (mounted.current && version === request.current) setLoading(false)
    }
  }

  useEffect(() => {
    const version = ++request.current
    void fetchAgentTasks().then((next) => {
      if (mounted.current && version === request.current) setTasks(next)
    }).catch((error: unknown) => {
      if (mounted.current && version === request.current) setLoadError(errorMessage(error, "Unable to load scheduled tasks."))
    }).finally(() => {
      if (mounted.current && version === request.current) setLoading(false)
    })
    return () => { request.current += 1 }
  }, [])

  function refresh() {
    setLoading(true); setLoadError(null)
    void reload()
  }

  async function handleScheduleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (scheduleBusy.current || !input.trim()) return
    const timestamp = futureScheduleTimestamp(scheduleAt)
    if (timestamp == null) { setFailed(true); setMessage("Choose a date and time in the future."); return }
    scheduleBusy.current = true
    setSaving(true); setFailed(false); setMessage(null)
    try {
      await scheduleAgentTask({ userInput: input.trim(), scheduleAt: new Date(timestamp).toISOString(), maxAttempts: 3 })
      if (!mounted.current) return
      setInput(""); setScheduleAt(""); setMessage("Task queued.")
      setLoading(true); setLoadError(null)
      await reload()
    } catch (error) {
      if (mounted.current) { setFailed(true); setMessage(errorMessage(error, "Unable to schedule the task.")) }
    } finally {
      scheduleBusy.current = false
      if (mounted.current) setSaving(false)
    }
  }

  async function control(taskNo: string, action: "pause" | "resume" | "cancel") {
    if (taskOperations.current.has(taskNo)) return
    taskOperations.current.add(taskNo)
    setBusyTasks(new Set(taskOperations.current)); setMessage(null); setFailed(false)
    try {
      const updated = await controlAgentTask(taskNo, action)
      if (!updated) throw new Error("The task could not be updated. Refresh and try again.")
      if (!mounted.current) return
      setLoading(true); setLoadError(null)
      await reload()
    } catch (error) {
      if (mounted.current) { setFailed(true); setMessage(errorMessage(error, "Unable to update the task.")) }
    } finally {
      taskOperations.current.delete(taskNo)
      if (mounted.current) setBusyTasks(new Set(taskOperations.current))
    }
  }

  return <section className="ma-panels ma-panel-tasks" aria-labelledby="ma-tasks-title">
    <header className="ma-panel-heading"><Clock3 aria-hidden="true" /><h3 id="ma-tasks-title">Scheduled tasks</h3><MobileButton type="button" variant="ghost" isIconOnly className="ma-panel-icon" aria-label="Refresh tasks" isDisabled={loading} onPress={refresh}><RefreshCw className={loading ? "ma-panel-spinner" : undefined} aria-hidden="true" /></MobileButton></header>
    <p className="ma-panel-description">Ask Agent to run a task at a time you choose.</p>
    <form className="ma-panel-form" onSubmit={handleScheduleSubmit}>
      <label className="ma-panel-field"><span>Task request</span><TextArea className="ma-panel-control" value={input} onChange={(event) => setInput(event.target.value)} placeholder="What should Agent do?" rows={3} maxLength={50000} required disabled={saving} /></label>
      <label className="ma-panel-field"><span>Run at</span><Input className="ma-panel-control" type="datetime-local" value={scheduleAt} onChange={(event) => setScheduleAt(event.target.value)} required disabled={saving} /><small>Your local time. Choose a future date and time.</small></label>
      <MobileButton type="submit" variant="secondary" className="ma-panel-button" isDisabled={saving || !input.trim() || !scheduleAt}>{saving && <LoaderCircle className="ma-panel-spinner" aria-hidden="true" />}Queue task</MobileButton>
    </form>
    {message && <p className={`ma-panel-message${failed ? " is-error" : ""}`} role={failed ? "alert" : "status"}>{message}</p>}
    <div className="ma-panel-task-list" aria-busy={loading}>
      {loadError ? <div className="ma-panel-state"><p role="alert">{loadError}</p><MobileButton variant="secondary" className="ma-panel-button" onPress={refresh}>Retry</MobileButton></div> : loading && !tasks.length ? <p className="ma-panel-description" role="status">Loading tasks…</p> : !tasks.length ? <p className="ma-panel-description">No scheduled tasks yet.</p> : tasks.map((task) => <article className="ma-panel-task" key={task.taskNo}>
        <div className="ma-panel-task-heading"><strong>{task.userInput}</strong><span>{task.status}</span></div>
        <p>{new Date(task.scheduleAt).toLocaleString("en-US")}</p>
        {task.lastError && <p className="ma-panel-message is-error">{task.lastError}</p>}
        {task.status === "QUEUED" || task.status === "PAUSED" ? <div className="ma-panel-actions">
          {task.status === "QUEUED" ? <MobileButton variant="secondary" className="ma-panel-button" isDisabled={busyTasks.has(task.taskNo)} onPress={() => void control(task.taskNo, "pause")}><CirclePause aria-hidden="true" />Pause</MobileButton> : <MobileButton variant="secondary" className="ma-panel-button" isDisabled={busyTasks.has(task.taskNo)} onPress={() => void control(task.taskNo, "resume")}><Play aria-hidden="true" />Resume</MobileButton>}
          <MobileButton variant="ghost" className="ma-panel-button" isDisabled={busyTasks.has(task.taskNo)} onPress={() => void control(task.taskNo, "cancel")}><Square aria-hidden="true" />Cancel</MobileButton>
        </div> : null}
      </article>)}
    </div>
  </section>
}

export function MobileAgentFeedback({ run }: { run: AgentRun }) {
  return run.runNo ? <Feedback key={run.runNo} runNo={run.runNo} /> : null
}

function Feedback({ runNo }: { runNo: string }) {
  const [feedback, setFeedback] = useState<AgentFeedback | null>(null)
  const [reason, setReason] = useState("")
  const [showReason, setShowReason] = useState(false)
  const [saving, setSaving] = useState(false)
  const [loading, setLoading] = useState(true)
  const [loadAttempt, setLoadAttempt] = useState(0)
  const [loadError, setLoadError] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [failed, setFailed] = useState(false)
  const busy = useRef(false)
  const mounted = useMounted()

  useEffect(() => {
    let active = true
    void fetchAgentFeedback(runNo).then((next) => {
      if (!active) return
      setFeedback(next); setReason(next?.feedbackReason ?? ""); setShowReason(Boolean(next?.feedbackReason) || Boolean(next?.rating && next.rating <= 2)); setLoadError(false)
    }).catch(() => { if (active) setLoadError(true) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [runNo, loadAttempt])

  async function submit(rating: number) {
    if (busy.current || loading) return
    busy.current = true
    setSaving(true); setMessage(null); setFailed(false)
    try {
      const next = await submitAgentFeedback(runNo, { rating, feedbackReason: reason, evaluationLabel: rating >= 4 ? "CORRECT" : "INCORRECT" })
      if (!mounted.current) return
      setFeedback(next); setMessage("Feedback saved."); setLoadError(false)
    } catch (error) {
      if (mounted.current) { setFailed(true); setMessage(errorMessage(error, "Unable to save feedback.")) }
    } finally {
      busy.current = false
      if (mounted.current) setSaving(false)
    }
  }

  return <section className="ma-feedback" aria-label="Answer feedback">
    <div className="ma-feedback-actions">
      <MobileButton variant={feedback?.rating && feedback.rating >= 4 ? "secondary" : "ghost"} isIconOnly className="ma-feedback-vote" aria-label="Helpful answer" aria-pressed={Boolean(feedback?.rating && feedback.rating >= 4)} isDisabled={saving || loading} onPress={() => void submit(5)}><ThumbsUp aria-hidden="true" /></MobileButton>
      <MobileButton variant={feedback?.rating && feedback.rating <= 2 ? "secondary" : "ghost"} isIconOnly className="ma-feedback-vote" aria-label="Unhelpful answer" aria-pressed={Boolean(feedback?.rating && feedback.rating <= 2)} isDisabled={saving || loading} onPress={() => { setShowReason(true); void submit(1) }}><ThumbsDown aria-hidden="true" /></MobileButton>
      {!showReason && <MobileButton variant="ghost" className="ma-feedback-note" isDisabled={saving || loading} onPress={() => setShowReason(true)}>Add a note</MobileButton>}
      {loading && <span className="ma-feedback-loading" role="status">Loading feedback…</span>}
    </div>
    {showReason && <form className="ma-feedback-form" onSubmit={(event) => { event.preventDefault(); void submit(feedback?.rating ?? 1) }}><label className="ma-panel-field"><span>Feedback note</span><Input className="ma-panel-control" value={reason} onChange={(event) => setReason(event.target.value)} placeholder="What could be better?" maxLength={120} disabled={saving || loading} /></label><MobileButton type="submit" variant="secondary" className="ma-panel-button" isDisabled={saving || loading || !reason.trim()}>Save note</MobileButton></form>}
    {loadError && <div className="ma-feedback-load-error"><span>Previous feedback is unavailable.</span><MobileButton variant="ghost" className="ma-feedback-note" isDisabled={saving || loading} onPress={() => { setLoading(true); setLoadAttempt((value) => value + 1) }}>Retry</MobileButton></div>}
    {message && <p className={`ma-panel-message${failed ? " is-error" : ""}`} role={failed ? "alert" : "status"}>{message}</p>}
  </section>
}
