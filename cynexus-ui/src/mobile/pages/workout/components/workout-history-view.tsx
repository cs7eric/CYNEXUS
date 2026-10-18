import { Activity, ChevronLeft, ChevronRight, Dumbbell, Plus } from "lucide-react"
import { useState, type CSSProperties } from "react"
import type { WorkoutSession } from "../lib/model"
import { historyBounds, historyData, historyDate, historyKey, shiftHistoryPeriod, type HistoryFilter, type HistoryPeriod } from "../lib/history"
import "./workout-history.css"

const longDate = (date: Date) => date.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })
const sessionName = (session: WorkoutSession) => session.category === "Cardio" ? "Cardio" : session.split ?? "Strength"

export function WorkoutHistoryView({ today, sessions, isSample, onLogWorkout, onStartLog }: {
  today: Date
  sessions: readonly WorkoutSession[]
  isSample: boolean
  onLogWorkout: (date?: string) => void
  onStartLog: () => void
}) {
  const [period, setPeriod] = useState<HistoryPeriod>("week")
  const [anchor, setAnchor] = useState(() => historyKey(today))
  const [selected, setSelected] = useState(() => historyKey(today))
  const [filter, setFilter] = useState<HistoryFilter>("All")
  const data = historyData(sessions, historyDate(anchor), period, filter, today)
  const selectedBucket = data.buckets.find(bucket => bucket.key === selected)
  const selectedRecords = data.records.filter(session => session.date === selected)
  const maximum = Math.max(1, ...data.buckets.map(bucket => bucket.minutes))
  const todayKey = historyKey(today)
  const nextStart = historyBounds(shiftHistoryPeriod(historyDate(anchor), period, 1), period).start
  const title = period === "year" ? String(data.start.getFullYear()) : period === "month"
    ? data.start.toLocaleDateString("en-US", { month: "long", year: "numeric" })
    : `${data.start.toLocaleDateString("en-US", { month: "short", day: "numeric", ...(data.start.getFullYear() !== data.end.getFullYear() ? { year: "numeric" } : {}) })} – ${longDate(data.end)}`

  function choosePeriod(next: HistoryPeriod) {
    setPeriod(next)
    setAnchor(selected)
  }

  function move(direction: number) {
    const date = shiftHistoryPeriod(historyDate(anchor), period, direction)
    const bounds = historyBounds(date, period)
    setAnchor(historyKey(date))
    setSelected(historyKey(bounds.end) > todayKey ? todayKey : historyKey(bounds.end))
  }

  function chooseBucket(key: string) {
    setSelected(key)
    if (period === "year") {
      setAnchor(key)
      setPeriod("month")
    }
  }

  return <section className="workout-history" aria-label="Workout history">
    <div className="wh-title"><h2>Your history</h2>{isSample && <div className="wh-sample-actions"><span className="wh-sample">Sample data</span><button type="button" onClick={onStartLog}>Start my log</button></div>}</div>
    <div className="wh-periods" aria-label="History period">{(["week", "month", "year"] as const).map(value => <button type="button" key={value} aria-pressed={period === value} onClick={() => choosePeriod(value)}>{value[0].toUpperCase() + value.slice(1)}</button>)}</div>
    <div className="wh-date-select">
      <button type="button" aria-label={`Previous ${period}`} onClick={() => move(-1)}><ChevronLeft size={17} /></button>
      <strong aria-live="polite">{title}</strong>
      <button type="button" aria-label={`Next ${period}`} disabled={historyKey(nextStart) > todayKey} onClick={() => move(1)}><ChevronRight size={17} /></button>
    </div>
    <div className="wh-today"><button type="button" onClick={() => { setAnchor(todayKey); setSelected(todayKey) }}>Today</button></div>
    <div className="wh-metrics" aria-live="polite">
      <div><span className="wh-number">{data.minutes.toLocaleString("en-US")}<small> min</small></span><span>Total time</span></div>
      <div><span className="wh-number">{data.records.length}</span><span>Sessions</span></div>
      <div><span className="wh-number">{data.activeDays}<small> / {data.days}</small></span><span>Active days</span></div>
    </div>
    <section className="wh-chart" aria-label={period === "month" ? "Training calendar" : "Training time chart"}>
      <div className="wh-chart-heading"><h3>{period === "month" ? "Training calendar" : period === "year" ? "Monthly training time" : "Daily training time"}</h3><span>{period === "month" ? "Training days" : "Minutes"}</span></div>
      <div className={`wh-plot ${period === "month" ? "is-calendar" : "is-bars"} ${period === "year" ? "is-year" : ""}`}>
        {period === "month" && <>{["M", "T", "W", "T", "F", "S", "S"].map((day, index) => <span className="wh-weekday" key={index}>{day}</span>)}{Array.from({ length: (data.start.getDay() + 6) % 7 }, (_, index) => <span key={`empty-${index}`} />)}</>}
        {data.buckets.map(bucket => <button type="button" key={bucket.key} disabled={bucket.future} aria-pressed={period === "year" ? undefined : selected === bucket.key}
          aria-label={`${period === "year" ? bucket.date.toLocaleDateString("en-US", { month: "long", year: "numeric" }) : longDate(bucket.date)}: ${bucket.future ? "Future date" : bucket.minutes ? `${bucket.minutes} minutes` : "No record"}${period === "year" && !bucket.future ? ", view month" : ""}`}
          className={`${bucket.minutes ? "is-logged" : ""} ${bucket.minutes >= 50 ? "is-busy" : ""} ${selected === bucket.key ? "is-selected" : ""} ${bucket.key === todayKey ? "is-today" : ""}`}
          style={{ "--bar-height": `${bucket.minutes / maximum * 100}%` } as CSSProperties} onClick={() => chooseBucket(bucket.key)}>
          {period === "month" ? <>{bucket.date.getDate()}{bucket.minutes > 0 && <i aria-hidden="true" />}</> : <><i aria-hidden="true" /><span>{period === "year" ? bucket.date.toLocaleDateString("en-US", { month: "narrow" }) : bucket.date.toLocaleDateString("en-US", { weekday: "narrow" })}</span></>}
        </button>)}
      </div>
      <div className="wh-caption" aria-live="polite"><span>{period === "year" ? title : longDate(historyDate(selected))}</span><strong>{period === "year" ? "Select a month to explore" : selectedBucket?.minutes ? `${selectedBucket.minutes} min` : "No record"}</strong></div>
    </section>
    <div className="wh-filter-row"><h3>Training log</h3><div aria-label="Workout type">{(["All", "Strength", "Cardio"] as const).map(value => <button key={value} type="button" aria-pressed={filter === value} onClick={() => setFilter(value)}>{value}</button>)}</div></div>
    <div className="wh-log">
      {data.records.map(session => <button type="button" className={`wh-session ${session.date === selected ? "is-selected" : ""}`} key={session.id} aria-pressed={session.date === selected} onClick={() => { setSelected(session.date); if (period === "year") { setAnchor(session.date); setPeriod("month") } }}>
        <span className="wh-session-icon">{session.category === "Cardio" ? <Activity size={19} /> : <Dumbbell size={19} />}</span>
        <span className="wh-session-content"><strong>{sessionName(session)}</strong><span>{historyDate(session.date).toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" })} · {session.category}</span></span>
        <span className="wh-session-time">{session.minutes} min<small>{session.category}</small></span>
      </button>)}
      {!data.records.length && <div className="wh-empty"><p>No {filter === "All" ? "workouts" : filter.toLowerCase() + " workouts"} recorded this {period}.</p><button type="button" onClick={onStartLog}>Log a workout</button></div>}
    </div>
    <div className="wh-selection" aria-live="polite">{period === "year" ? "Select a month to explore its training days." : selectedRecords.length ? selectedRecords.map(session => `${sessionName(session)} · ${longDate(historyDate(session.date))} · ${session.minutes} minutes`).join("; ") : `${longDate(historyDate(selected))} · No ${filter === "All" ? "workout" : filter.toLowerCase() + " workout"} recorded.`}</div>
    {period !== "year" && <button type="button" className="wh-log-action" onClick={() => onLogWorkout(selected)}><Plus size={16} />{selectedRecords.length ? "Edit workout" : "Log workout"}<span>{historyDate(selected).toLocaleDateString("en-US", { month: "short", day: "numeric" })}</span></button>}
  </section>
}
