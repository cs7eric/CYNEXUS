import { AlertCircle, CheckCircle2, ChevronDown, Clock3, FileImage, FileVideo2, ListTodo, LoaderCircle, RefreshCw, RotateCcw, ScanLine, WalletCards } from "lucide-react"
import { useCallback, useEffect, useMemo, useRef, useState } from "react"

import { listApiWorkflows, retryApiWorkflow, type ApiWorkflowProcess, type ApiWorkflowStatus } from "@/services/core/api-workflows"
import { listVisionTasks, retryVisionTask, type AiVisionTaskSummary } from "@/services/ai/vision"
import { isFoodRecognitionVisionTask, listFoodRecognitionTasks, retryFoodRecognitionTask, type FoodRecognitionTask } from "@/services/food/recognition"
import { useFoodRecognitionTasks } from "@/mobile/pages/food/hooks/use-food-recognition-tasks"
import { MobileButton } from "@/mobile/ui/button"
import { NutritionTaskResult } from "./components/nutrition-task-result"

import "@/mobile/pages/tasks/tasks.css"

type TaskType = ApiWorkflowProcess["workflowType"] | "AI_VISION"
type TypeFilter = "all" | TaskType
type StatusFilter = "all" | "active" | "failed" | "complete"

const typeLabels: Record<TaskType, string> = {
  MEDIA_TRANSCRIPTION: "Media transcription",
  PORTFOLIO_TRADE_IMPORT: "Portfolio import",
  FINANCE_AI_CAPTURE: "Finance capture",
  AI_VISION: "Food recognition",
}

const typeDescriptions: Record<TaskType, string> = {
  MEDIA_TRANSCRIPTION: "URL downloads, transcription and enrichment",
  PORTFOLIO_TRADE_IMPORT: "Screenshot recognition and trade review",
  FINANCE_AI_CAPTURE: "Receipt recognition and bookkeeping",
  AI_VISION: "Nutrition labels and fridge inventory images",
}

const statusLabels: Record<ApiWorkflowStatus, string> = {
  QUEUED: "Queued",
  RUNNING: "Running",
  WAITING_REVIEW: "Waiting review",
  SUCCEEDED: "Success",
  FAILED: "Failed",
  CANCELLED: "Cancelled",
}

const typeIcons: Record<TaskType, typeof FileVideo2> = {
  MEDIA_TRANSCRIPTION: FileVideo2,
  PORTFOLIO_TRADE_IMPORT: FileImage,
  FINANCE_AI_CAPTURE: WalletCards,
  AI_VISION: ScanLine,
}

function statusLabel(status: ApiWorkflowStatus) {
  return statusLabels[status] ?? status
}

function statusTone(status: ApiWorkflowStatus) {
  if (status === "FAILED" || status === "CANCELLED") return "is-failed"
  if (status === "SUCCEEDED" || status === "WAITING_REVIEW") return "is-success"
  return "is-active"
}

function statusFilterMatch(item: TaskItem, filter: StatusFilter) {
  if (filter === "all") return true
  if (filter === "active") return item.status === "QUEUED" || item.status === "RUNNING"
  if (filter === "failed") return item.status === "FAILED" || item.status === "CANCELLED"
  return item.status === "SUCCEEDED" || item.status === "WAITING_REVIEW"
}

function formatTime(value?: string | null) {
  if (!value) return "No timestamp"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString("en-US", { month: "short", day: "numeric", hour: "numeric", minute: "2-digit" })
}

type TaskItem = ApiWorkflowProcess & { workflowType: Exclude<TaskType, "AI_VISION">; retryable?: boolean }
  | (Omit<ApiWorkflowProcess, "workflowType"> & { workflowType: "AI_VISION"; retryable?: boolean })

function visionTaskItem(task: AiVisionTaskSummary): TaskItem {
  const status = task.status.toUpperCase() as ApiWorkflowStatus
  const title = task.scenarioCode === "food_nutrition_label_parse" ? "Nutrition label scan"
    : task.scenarioCode === "food_fridge_inventory" ? "Fridge inventory scan"
      : "Food image recognition"
  return {
    id: `ai:${task.id}`,
    workflowType: "AI_VISION",
    retryable: task.retryable,
    workflowKey: `ai.vision.${task.scenarioCode}`,
    source: "AI",
    sourceRef: task.id,
    title,
    status,
    phase: status === "FAILED" ? "Failed" : status === "SUCCEEDED" ? "Completed" : "Processing",
    progress: task.progress ?? 0,
    attemptCount: task.attemptCount ?? 0,
    errorMessage: task.errorMessage,
    createdAt: task.createdAt,
    startedAt: task.startedAt,
    completedAt: task.completedAt,
    updatedAt: task.updatedAt,
    events: [],
  }
}

function foodRecognitionTaskItem(task: FoodRecognitionTask): TaskItem {
  const status = task.status.toUpperCase() as ApiWorkflowStatus
  return {
    id: `food:${task.id}`,
    workflowType: "AI_VISION",
    workflowKey: "food.nutrition-recognition",
    source: "Core",
    sourceRef: task.id,
    title: task.foodName || "Create food from photos",
    status,
    phase: status === "SUCCEEDED" ? "Saved to My foods" : status === "FAILED" ? "Failed" : "Processing",
    progress: status === "SUCCEEDED" || status === "FAILED" ? 100 : status === "RUNNING" ? 50 : 0,
    attemptCount: task.attemptCount ?? 1,
    retryable: task.retryable ?? false,
    errorMessage: task.errorMessage,
    createdAt: task.createdAt,
    updatedAt: task.updatedAt,
    completedAt: task.completedAt,
    resultRoute: task.foodId ? "/m/food#food-foods" : null,
    resultRef: task.foodId,
    events: [],
  }
}

function taskTitle(item: TaskItem) {
  if (item.title?.trim()) return item.title.trim()
  if (item.workflowType === "PORTFOLIO_TRADE_IMPORT") return `Import #${item.sourceRef}`
  if (item.workflowType === "FINANCE_AI_CAPTURE") return `Capture ${item.sourceRef}`
  return "Media processing"
}

function TaskRow({ item, retrying, savingDraft, onRetry }: { item: TaskItem; retrying: boolean; savingDraft: boolean; onRetry: (item: TaskItem) => Promise<void> }) {
  const [open, setOpen] = useState(false)
  const Icon = typeIcons[item.workflowType]
  const latestEvent = item.events.at(-1)
  const failure = item.errorMessage || item.errorCode || latestEvent?.errorMessage || latestEvent?.errorCode
  const active = item.status === "QUEUED" || item.status === "RUNNING"
  const canRetry = item.status === "FAILED" && (item.workflowType !== "AI_VISION" || item.retryable !== false)
  const progress = Math.min(100, Math.max(0, item.progress ?? 0))

  return <article className="mobile-task-row">
    <button type="button" className="mobile-task-row-main" aria-expanded={open} onClick={() => setOpen((value) => !value)}>
      <span className={`mobile-task-type-icon ${statusTone(item.status)}`}><Icon aria-hidden="true" /></span>
      <span className="mobile-task-row-copy"><strong>{taskTitle(item)}</strong><small>{formatTime(item.updatedAt || item.createdAt)} · Attempt {Math.max(1, item.attemptCount || 1)}</small></span>
      <span className={`mobile-task-status ${statusTone(item.status)}`}>{statusLabel(item.status)}</span>
      <ChevronDown className={`mobile-task-chevron${open ? " is-open" : ""}`} aria-hidden="true" />
    </button>
    {active && <div className="mobile-task-progress" role="progressbar" aria-label={`${taskTitle(item)} progress`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={progress}>
      <span style={{ width: `${progress}%` }} />
    </div>}
    {open && <div className="mobile-task-row-detail">
      <p className="mobile-task-description">{typeDescriptions[item.workflowType]}</p>
      {active && <p className="mobile-task-current"><LoaderCircle aria-hidden="true" /> {item.phase || "Processing"}{progress > 0 ? ` · ${progress}%` : ""}</p>}
      {failure && <p className="mobile-task-failure"><AlertCircle aria-hidden="true" /> {item.errorCode ? `${item.errorCode}: ` : ""}{failure}</p>}
      {latestEvent && <p className="mobile-task-event"><Clock3 aria-hidden="true" /> {latestEvent.message || latestEvent.phase || latestEvent.eventType} · {formatTime(latestEvent.occurredAt)}</p>}
      {item.workflowKey === "ai.vision.food_nutrition_label_parse" && item.status === "SUCCEEDED" && <NutritionTaskResult taskId={item.sourceRef} savingDraft={savingDraft} />}
      <div className="mobile-task-detail-actions">
        <span className="mobile-task-id" title={`Task ID ${item.id}`}>ID {item.id}</span>
        {canRetry && <MobileButton type="button" variant="ghost" className="mobile-task-retry" onPress={() => void onRetry(item)} isDisabled={retrying}><RotateCcw aria-hidden="true" className={retrying ? "is-spinning" : ""} />{retrying ? "Retrying…" : "Retry"}</MobileButton>}
      </div>
    </div>}
  </article>
}

export default function MobileTasksPage() {
  const [items, setItems] = useState<TaskItem[]>([])
  const [typeFilter, setTypeFilter] = useState<TypeFilter>("all")
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all")
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [unavailableSources, setUnavailableSources] = useState<string[]>([])
  const [retryingId, setRetryingId] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  useFoodRecognitionTasks(setMessage)
  const requestSequence = useRef(0)

  const load = useCallback(async (background = false) => {
    const sequence = ++requestSequence.current
    if (background) setRefreshing(true)
    else setLoading(true)
    setError(null)
    try {
      const needsWorkflows = typeFilter !== "AI_VISION"
      const needsVision = typeFilter === "all" || typeFilter === "AI_VISION"
      const [workflowResult, visionResult, foodResult] = await Promise.allSettled([
        needsWorkflows ? listApiWorkflows(1, 100, typeFilter, "all") : Promise.resolve(null),
        needsVision ? listVisionTasks(100) : Promise.resolve(null),
        needsVision ? listFoodRecognitionTasks(100) : Promise.resolve(null),
      ])
      if (sequence !== requestSequence.current) return
      const workflowPage = workflowResult.status === "fulfilled" ? workflowResult.value : null
      const visionTasks = visionResult.status === "fulfilled" ? visionResult.value : null
      const foodTasks = foodResult.status === "fulfilled" ? foodResult.value : null
      const failures = [
        ...(needsWorkflows && workflowResult.status === "rejected" ? [workflowResult.reason] : []),
        ...(needsVision && visionResult.status === "rejected" ? [visionResult.reason] : []),
        ...(needsVision && foodResult.status === "rejected" ? [foodResult.reason] : []),
      ]
      if (failures.length === Number(needsWorkflows) + Number(needsVision) * 2) throw failures[0]
      const internalAiTasks = new Set((foodTasks ?? []).map(task => task.aiTaskId).filter(Boolean))
      setItems([
        ...(workflowPage?.items ?? []),
        ...(visionTasks ?? []).filter(task => !isFoodRecognitionVisionTask(task) && !internalAiTasks.has(task.id)).map(visionTaskItem),
        ...(foodTasks ?? []).map(foodRecognitionTaskItem),
      ].sort((left, right) => new Date(right.createdAt ?? 0).getTime() - new Date(left.createdAt ?? 0).getTime()))
      setUnavailableSources([
        ...(workflowPage?.unavailableSources ?? []),
        ...(needsWorkflows && workflowResult.status === "rejected" ? ["Core and Market"] : []),
        ...(needsVision && visionResult.status === "rejected" ? ["Image analysis"] : []),
        ...(needsVision && foodResult.status === "rejected" ? ["Food recognition"] : []),
      ])
    } catch (cause) {
      if (sequence !== requestSequence.current) return
      setError(cause instanceof Error ? cause.message : "Unable to load async tasks")
      setUnavailableSources([])
    } finally {
      if (sequence === requestSequence.current) { setLoading(false); setRefreshing(false) }
    }
  }, [typeFilter])

  useEffect(() => {
    const timer = window.setTimeout(() => { void load() }, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  const hasActive = items.some((item) => item.status === "QUEUED" || item.status === "RUNNING")
  const refreshInterval = hasActive ? 4_000 : unavailableSources.length > 0 ? 10_000 : null
  useEffect(() => {
    if (refreshInterval === null) return
    const timer = window.setInterval(() => { void load(true) }, refreshInterval)
    return () => window.clearInterval(timer)
  }, [refreshInterval, load])

  const visibleItems = useMemo(() => items.filter((item) => statusFilterMatch(item, statusFilter)), [items, statusFilter])
  const groups = useMemo(() => {
    const types: TaskType[] = typeFilter === "all" ? ["MEDIA_TRANSCRIPTION", "PORTFOLIO_TRADE_IMPORT", "FINANCE_AI_CAPTURE", "AI_VISION"] : [typeFilter]
    return types.map((type) => ({ type, items: visibleItems.filter((item) => item.workflowType === type) })).filter((group) => group.items.length > 0)
  }, [typeFilter, visibleItems])

  const retry = useCallback(async (item: TaskItem) => {
    setRetryingId(`${item.workflowType}:${item.id}`)
    try {
      if (item.workflowType === "AI_VISION") {
        if (item.workflowKey === "food.nutrition-recognition") await retryFoodRecognitionTask(item.sourceRef)
        else await retryVisionTask(item.sourceRef)
      } else await retryApiWorkflow(item)
      await load(true)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Unable to retry task")
    } finally {
      setRetryingId(null)
    }
  }, [load])

  return <div className="mobile-tasks">
    <header className="mobile-tasks-heading">
      <div><p className="mobile-tasks-eyebrow"><ListTodo aria-hidden="true" /> Profile</p><h2>Async tasks</h2></div>
      <button type="button" className="mobile-tasks-refresh" onClick={() => void load(true)} disabled={refreshing} aria-label="Refresh async tasks" title="Refresh async tasks"><RefreshCw aria-hidden="true" className={refreshing ? "is-spinning" : ""} /></button>
    </header>

    <div className="mobile-tasks-filters" role="group" aria-label="Task type">
      <button type="button" aria-pressed={typeFilter === "all"} className={typeFilter === "all" ? "is-selected" : ""} onClick={() => setTypeFilter("all")}>All</button>
      {(Object.keys(typeLabels) as TaskType[]).map((type) => <button key={type} type="button" aria-pressed={typeFilter === type} className={typeFilter === type ? "is-selected" : ""} onClick={() => setTypeFilter(type)}>{typeLabels[type]}</button>)}
    </div>
    <div className="mobile-tasks-status-filters" role="group" aria-label="Task status">
      {(["all", "active", "failed", "complete"] as StatusFilter[]).map((filter) => <button key={filter} type="button" aria-pressed={statusFilter === filter} className={statusFilter === filter ? "is-selected" : ""} onClick={() => setStatusFilter(filter)}>{filter === "all" ? "All statuses" : filter === "active" ? "In progress" : filter === "failed" ? "Failed" : "Complete"}</button>)}
    </div>

    {unavailableSources.length > 0 && <p className="mobile-tasks-stale" role="status">Some task sources are unavailable: {unavailableSources.join(", ")}. Showing the available history.</p>}
    {message && <p className="mobile-tasks-message" role="status">{message}</p>}
    {loading && <div className="mobile-tasks-state"><LoaderCircle aria-hidden="true" className="is-spinning" /><span>Loading task history…</span></div>}
    {!loading && error && <div className="mobile-tasks-state is-error"><AlertCircle aria-hidden="true" /><span>{error}</span><button type="button" onClick={() => void load()}>Try again</button></div>}
    {!loading && !error && groups.length === 0 && <div className="mobile-tasks-state"><CheckCircle2 aria-hidden="true" /><span>{items.length ? "No tasks match these filters." : "No async tasks yet."}</span></div>}
    {!loading && !error && groups.map(({ type, items: groupItems }) => {
      const Icon = typeIcons[type]
      return <section key={type} className="mobile-task-group" aria-labelledby={`mobile-task-group-${type}`}>
        <div className="mobile-task-group-heading"><span className="mobile-task-group-icon"><Icon aria-hidden="true" /></span><div><h3 id={`mobile-task-group-${type}`}>{typeLabels[type]}</h3><p>{groupItems.length} {groupItems.length === 1 ? "task" : "tasks"}</p></div></div>
        <div className="mobile-task-list">{groupItems.map((item) => <TaskRow key={item.id} item={item} retrying={retryingId === `${item.workflowType}:${item.id}`} savingDraft={false} onRetry={retry} />)}</div>
      </section>
    })}
  </div>
}
