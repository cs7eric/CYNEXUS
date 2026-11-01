import { Apple, CheckCircle2, ChevronLeft, ChevronRight, Clock3, FileVideo2, FileImage, WalletCards } from "lucide-react"
import { useMemo, useState } from "react"

import type { ApiWorkflowStatus } from "@/services/core/api-workflows"
import { formatTaskDuration, isTaskSummaryIncomplete, previousTaskSummaryRange, summarizeTasks, taskSummaryDays, taskSummaryRange, taskSummaryRecords, type TaskSummaryCoverage, type TaskSummaryPeriod, type TaskSummaryRecord } from "../lib/summary"
import "./summary.css"

type TaskType = "AI_VISION" | "FINANCE_AI_CAPTURE" | "MEDIA_TRANSCRIPTION" | "PORTFOLIO_TRADE_IMPORT"
type TaskViewItem = TaskSummaryRecord & {
  id: string
  workflowType: TaskType
  title?: string | null
  sourceRef: string
  progress?: number
  phase?: string
  attemptCount?: number
  errorMessage?: string | null
}
type Metric = "volume" | "rate"

const icons = { AI_VISION: Apple, FINANCE_AI_CAPTURE: WalletCards, MEDIA_TRANSCRIPTION: FileVideo2, PORTFOLIO_TRADE_IMPORT: FileImage } satisfies Record<TaskType, typeof Apple>
const statuses: Record<ApiWorkflowStatus, string> = { QUEUED: "Queued", RUNNING: "Running", WAITING_REVIEW: "Waiting review", SUCCEEDED: "Success", FAILED: "Failed", CANCELLED: "Cancelled" }

function dayKey(value: Date) { return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}` }
function dayLabel(value: Date, period: TaskSummaryPeriod) { return value.toLocaleDateString("en-US", period === "week" ? { weekday: "short" } : { month: "short", day: "numeric" }) }
function periodLabel(value: Date, period: TaskSummaryPeriod) { return period === "week" ? `Week of ${value.toLocaleDateString("en-US", { month: "short", day: "numeric" })}` : value.toLocaleDateString("en-US", { month: "long", year: "numeric" }) }
function rateText(value: number | null) { return value === null ? "—" : `${value.toFixed(1)}%` }

function TrendChart({ days, rows, period, metric, selected, onSelect, partial, rangeRate }: {
  days: Date[]
  rows: TaskViewItem[]
  period: TaskSummaryPeriod
  metric: Metric
  selected: number
  onSelect: (value: number) => void
  partial: boolean
  rangeRate: number | null
}) {
  const daily = days.map((date) => {
    const rowsForDay = rows.filter((item) => item.createdAt && dayKey(new Date(item.createdAt)) === dayKey(date))
    const stats = summarizeTasks(rowsForDay)
    return { date, ...stats }
  })
  const selectedDay = daily[selected]
  const max = metric === "rate" ? 100 : Math.max(4, ...daily.map((day) => day.total))
  const left = 28, right = 314, top = 12, bottom = 136
  const step = (right - left) / daily.length
  const x = (index: number) => left + step * (index + 0.5)
  const y = (value: number) => bottom - value / max * (bottom - top)
  const bars = metric === "volume" ? daily.map((day, index) => {
    const width = Math.min(18, step * 0.64)
    const start = x(index) - width / 2
    const total = day.success + day.failed + day.review + day.active
    const other = Math.max(0, day.total - total)
    let base = 0
    return [[day.success, "success"], [day.failed, "failed"], [day.review + day.active + other, "other"]].flatMap(([count, tone]) => {
      const value = Number(count)
      if (!value) return []
      const topY = y(base + value)
      const height = Math.max(0.5, y(base) - topY)
      base += value
      return [<rect key={`${index}-${tone}`} className={`task-summary-bar is-${tone}`} x={start} y={topY} width={width} height={height} rx="2" />]
    })
  }) : []
  const ratePoints = metric === "rate" ? daily.flatMap((day, index) => day.rate === null ? [] : [`${x(index)},${y(day.rate)}`]) : []
  const selectedValue = !selectedDay ? "—" : metric === "volume" ? `${selectedDay.total}${partial ? "+" : ""} tasks` : rateText(rangeRate === null ? null : selectedDay.rate)
  const labels = days.length <= 7 ? daily.map((_, index) => index)
    : [...new Set([0, Math.round((days.length - 1) / 3), Math.round((days.length - 1) * 2 / 3), days.length - 1])]

  return <>
    <svg className="mobile-task-summary-chart" viewBox="0 0 320 158" role="img" aria-labelledby="task-chart-title task-chart-description">
      <title id="task-chart-title">{metric === "volume" ? "Tasks created each day" : "Daily success rate"}</title>
      <desc id="task-chart-description">{daily.map((day) => `${dayLabel(day.date, period)}: ${day.total} tasks, ${day.success} success, ${day.failed} failed`).join(". ")}</desc>
      {[0, 0.5, 1].map((fraction) => {
        const value = metric === "rate" ? fraction * 100 : Math.max(4, max) * fraction
        return <g key={fraction}><path className="task-summary-grid" d={`M${left} ${y(value)}H${right}`} /><text x="0" y={y(value) + 3}>{metric === "rate" ? `${Math.round(value)}%` : Math.round(value)}</text></g>
      })}
      <rect className="task-summary-selected" x={left + selected * step} y={top - 4} width={step} height={bottom - top + 8} rx="4" />
      {metric === "volume" ? bars : <>
        {ratePoints.length > 1 && <polyline className="task-summary-rate-line" points={daily.map((day, index) => day.rate === null ? "" : `${x(index)},${y(day.rate)}`).filter(Boolean).join(" ")} />}
        {daily.map((day, index) => day.rate === null ? null : <circle key={dayKey(day.date)} className="task-summary-rate-dot" cx={x(index)} cy={y(day.rate)} r={index === selected ? 4 : 2.5} />)}
      </>}
      {labels.map((index) => <text key={dayKey(days[index])} textAnchor="middle" x={x(index)} y="154">{dayLabel(days[index], period).slice(0, period === "week" ? 1 : 6)}</text>)}
    </svg>
    <div className="mobile-task-summary-chart-readout" aria-live="polite"><strong>{selectedDay ? selectedDay.date.toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" }) : "—"}</strong><span>{selectedValue}{selectedDay && metric === "volume" ? ` · ${selectedDay.success} success · ${selectedDay.failed} failed` : ""}</span></div>
    <div className="mobile-task-summary-scrubber"><label htmlFor="task-summary-day">Explore {period === "week" ? "days" : "days"}</label><input id="task-summary-day" type="range" min="0" max={Math.max(0, days.length - 1)} step="1" value={selected} onChange={(event) => onSelect(Number(event.target.value))} aria-valuetext={selectedDay ? `${dayLabel(selectedDay.date, period)}, ${selectedValue}` : "No task activity"} /></div>
  </>
}

export function MobileTaskSummary({ items, typeFilter, typeOrder, typeLabels, coverage, unavailableSources, onSelectType, onViewHistory }: {
  items: TaskViewItem[]
  typeFilter: "all" | TaskType
  typeOrder: TaskType[]
  typeLabels: Record<TaskType, string>
  coverage: TaskSummaryCoverage[]
  unavailableSources: string[]
  onSelectType: (type: "all" | TaskType) => void
  onViewHistory: (filter?: "all" | "attention") => void
}) {
  const [period, setPeriod] = useState<TaskSummaryPeriod>("week")
  const [offset, setOffset] = useState(0)
  const [metric, setMetric] = useState<Metric>("volume")
  const [selectedDay, setSelectedDay] = useState(0)
  const today = new Date()
  const year = today.getFullYear()
  const month = today.getMonth()
  const date = today.getDate()
  const range = useMemo(() => taskSummaryRange(period, offset, new Date(year, month, date, 12)), [period, offset, year, month, date])
  const previousRange = useMemo(() => previousTaskSummaryRange(range, period), [range, period])
  const rows = useMemo(() => taskSummaryRecords(items, range) as TaskViewItem[], [items, range])
  const previousRows = useMemo(() => taskSummaryRecords(items, previousRange), [items, previousRange])
  const partial = isTaskSummaryIncomplete(coverage, range, unavailableSources)
  const previousPartial = isTaskSummaryIncomplete(coverage, previousRange, unavailableSources)
  const stats = summarizeTasks(rows)
  const previousStats = summarizeTasks(previousRows)
  const days = taskSummaryDays(range)
  const currentDay = range.end.toLocaleDateString("en-US", { month: "short", day: "numeric" })
  const periodTitle = periodLabel(range.start, period)
  const visibleTypes = typeFilter === "all" ? typeOrder : [typeFilter]
  const categories = visibleTypes.map((type) => ({ type, items: rows.filter((item) => item.workflowType === type) }))
  const categoryLeader = [...categories].sort((left, right) => right.items.length - left.items.length)[0]
  const attention = rows.filter((item) => item.status === "FAILED" || item.status === "WAITING_REVIEW").sort((left, right) => Number(right.status === "FAILED") - Number(left.status === "FAILED") || new Date(right.createdAt ?? 0).getTime() - new Date(left.createdAt ?? 0).getTime())
  const durationLabel = stats.medianSeconds === null ? "—" : formatTaskDuration(stats.medianSeconds)
  const completeComparison = !partial && !previousPartial && previousStats.closed > 0 && stats.closed > 0
  const rateDelta = completeComparison && stats.rate !== null && previousStats.rate !== null ? stats.rate - previousStats.rate : null
  const selectedIndex = Math.min(selectedDay, Math.max(0, days.length - 1))
  const statusCounts = (groupItems: TaskViewItem[]) => summarizeTasks(groupItems)

  function shift(direction: -1 | 1) {
    setOffset((value) => value + direction)
    setSelectedDay(0)
  }

  return <div className="mobile-task-summary">
    <div className="mobile-task-summary-period">
      <div className="mobile-task-summary-segmented" role="group" aria-label="Summary period">
        {(["week", "month"] as const).map((value) => <button key={value} type="button" aria-pressed={period === value} onClick={() => { setPeriod(value); setOffset(0); setSelectedDay(0) }}>{value === "week" ? "Week" : "Month"}</button>)}
      </div>
      <div className="mobile-task-summary-period-nav">
        <button type="button" aria-label={`Previous ${period}`} onClick={() => shift(-1)}><ChevronLeft aria-hidden="true" /></button>
        <span><strong>{offset === 0 ? `This ${period}` : periodTitle}</strong><small>{range.start.toLocaleDateString("en-US", { month: "short", day: "numeric" })} – {currentDay}</small></span>
        <button type="button" aria-label={`Next ${period}`} disabled={offset >= 0} onClick={() => shift(1)}><ChevronRight aria-hidden="true" /></button>
      </div>
    </div>

    {partial && <p className="mobile-task-summary-coverage" role="status"><Clock3 aria-hidden="true" /> Some sources have limited history. Values are at least the tasks currently available for this period; exact rates and comparisons are hidden until coverage is complete.</p>}
    {unavailableSources.length > 0 && <p className="mobile-task-summary-unavailable" role="status">Unavailable sources: {unavailableSources.join(", ")}. Summary values exclude those sources.</p>}

    <section className="mobile-task-summary-recap" aria-labelledby="mobile-task-summary-recap-title">
      <div className="mobile-task-summary-kicker"><CheckCircle2 aria-hidden="true" /> This period</div>
      <h3 id="mobile-task-summary-recap-title">{stats.total === 0 ? partial ? "No tasks in the available history." : `No tasks found this ${period}.` : partial ? `${typeLabels[categoryLeader?.type ?? typeOrder[0]]} leads the available history.` : `${typeLabels[categoryLeader?.type ?? typeOrder[0]]} led your ${period}.`}</h3>
      <p>{partial ? stats.total === 0 ? "More tasks may exist because some source history is incomplete." : `At least ${stats.total} ${stats.total === 1 ? "task is" : "tasks are"} recorded in the loaded history. ${stats.failed} failed and ${stats.review} ${stats.review === 1 ? "is" : "are"} waiting for review.` : stats.total === 0 ? "Choose another period or task type to explore your background work." : `${stats.success} of ${stats.total} tasks succeeded. ${stats.failed} failed and ${stats.review} ${stats.review === 1 ? "is" : "are"} waiting for review.`}</p>
      <div className="mobile-task-summary-recap-foot">{completeComparison ? `Success rate ${Math.abs(rateDelta ?? 0) < 0.05 ? "unchanged" : `${rateDelta! > 0 ? "up" : "down"} ${Math.abs(rateDelta!).toFixed(1)} pp`} vs ${period === "week" ? "last week" : "the same days last month"}.` : partial ? "Comparison unavailable while a source is incomplete." : "Not enough completed tasks for a reliable comparison."}</div>
    </section>

    <dl className="mobile-task-summary-metrics">
      <div><dt>Tasks created</dt><dd>{partial ? `≥${stats.total}` : stats.total}</dd><span>{stats.active} in progress</span></div>
      <div><dt>Success rate</dt><dd>{partial ? "—" : rateText(stats.rate)}</dd><span>{partial ? "incomplete history" : `${stats.success} / ${stats.closed} completed`}</span></div>
      <div><dt>Typical time</dt><dd>{partial ? "—" : durationLabel}</dd><span>{partial ? "incomplete history" : `median · ${stats.timedTasks} timed tasks`}</span></div>
    </dl>

    <section className="mobile-task-summary-section" aria-labelledby="mobile-task-summary-attention-title">
      <div className="mobile-task-summary-section-head"><h3 id="mobile-task-summary-attention-title">Needs attention <span>{partial ? `≥${attention.length}` : attention.length}</span></h3><button type="button" onClick={() => onViewHistory("attention")}>View tasks <ChevronRight aria-hidden="true" /></button></div>
      {attention.length ? <div className="mobile-task-summary-attention-list">{attention.slice(0, 3).map((item) => <button key={`${item.workflowType}-${item.id}`} type="button" onClick={() => onViewHistory("attention")}><span className={`mobile-task-summary-attention-icon ${item.status === "WAITING_REVIEW" ? "is-review" : ""}`}>{item.status === "FAILED" ? "!" : "…"}</span><span><strong>{item.title || typeLabels[item.workflowType]}</strong><small>{typeLabels[item.workflowType]} · {statuses[item.status as ApiWorkflowStatus]}</small></span><ChevronRight aria-hidden="true" /></button>)}</div> : <p className="mobile-task-summary-clear"><CheckCircle2 aria-hidden="true" /> No failed tasks or pending reviews in the loaded history.</p>}
    </section>

    <section className="mobile-task-summary-section" aria-labelledby="mobile-task-summary-categories-title">
      <div className="mobile-task-summary-section-head"><h3 id="mobile-task-summary-categories-title">By type</h3><span>Food first</span></div>
      <div className="mobile-task-summary-types">{categories.map(({ type, items: groupItems }) => {
        const Icon = icons[type]
        const group = statusCounts(groupItems)
        return <button type="button" key={type} className="mobile-task-summary-type-row" onClick={() => { onSelectType(type); onViewHistory("all") }}><span className={`mobile-task-summary-type-icon type-${type.toLowerCase()}`}><Icon aria-hidden="true" /></span><span className="mobile-task-summary-type-copy"><strong>{typeLabels[type]}</strong><small>{partial ? "At least " : ""}{group.success} success{group.failed ? ` · ${partial ? "≥" : ""}${group.failed} failed` : ""}{group.review ? ` · ${partial ? "≥" : ""}${group.review} review` : ""}</small></span><span className="mobile-task-summary-type-count">{partial ? `≥${groupItems.length}` : groupItems.length}<small>{partial ? "available" : stats.total ? `${Math.round(groupItems.length / stats.total * 100)}%` : "0%"}</small></span><ChevronRight aria-hidden="true" /></button>
      })}</div>
    </section>

    <section className="mobile-task-summary-section" aria-labelledby="mobile-task-summary-trends-title">
      <div className="mobile-task-summary-section-head"><h3 id="mobile-task-summary-trends-title">Activity trend</h3><span>{period === "week" ? "Day by day" : "Month to date"}</span></div>
      <div className="mobile-task-summary-trend-panel">
        <div className="mobile-task-summary-trend-top"><div><p>{metric === "volume" ? "Tasks created" : "Success rate"}</p><strong>{metric === "volume" ? `${partial ? "≥" : ""}${stats.total}` : partial ? "—" : rateText(stats.rate)}</strong></div><div className="mobile-task-summary-metric-switch" role="group" aria-label="Trend metric"><button type="button" aria-pressed={metric === "volume"} onClick={() => setMetric("volume")}>Volume</button><button type="button" aria-pressed={metric === "rate"} disabled={partial} onClick={() => setMetric("rate")}>Success %</button></div></div>
        <div className="mobile-task-summary-legend"><span><i />{metric === "volume" ? "Success" : "Success rate"}</span>{metric === "volume" && <><span><i className="is-failed" />Failed</span><span><i className="is-other" />Other statuses</span></>}</div>
        <TrendChart days={days} rows={rows} period={period} metric={metric} selected={selectedIndex} onSelect={setSelectedDay} partial={partial} rangeRate={stats.rate} />
      </div>
    </section>
    <p className="mobile-task-summary-footnote">{partial ? "Counts show the records returned by each source and may be incomplete." : "Tasks are grouped by creation date. Typical time is the median of successful tasks with recorded start and finish times."}</p>
  </div>
}
