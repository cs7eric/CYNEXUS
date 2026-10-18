import { Activity, ArrowUpRight, BedDouble, CalendarDays, Check, Dumbbell, Ruler, Scale, Settings2, TimerReset } from "lucide-react"
import { useMemo, useState, type CSSProperties, type FormEvent } from "react"
import { sidebarConfig } from "@/config/sidebar"
import { calculateBmi } from "@/mobile/components/activity-calendar/model"
import { createBodyMeasurementPreview } from "@/mobile/components/activity-calendar/preview"
import "@/mobile/pages/workout/workout.css"

type WorkoutCategory = "Strength" | "Cardio"
type WorkoutFilter = "All" | WorkoutCategory
type SplitDay = "Chest + Triceps" | "Back + Biceps" | "Shoulders" | "Rest"
type WorkoutSession = { id: number; date: string; category: WorkoutCategory; minutes: number }

const cycle: SplitDay[] = ["Chest + Triceps", "Back + Biceps", "Shoulders", "Rest"]
const splitColors: Record<SplitDay, string> = { "Chest + Triceps": "is-blue", "Back + Biceps": "is-mint", Shoulders: "is-lilac", Rest: "is-rest" }

function dateKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`
}

function parseDate(value: string) {
  const [year, month, day] = value.split("-").map(Number)
  return new Date(year, month - 1, day, 12)
}

function daysBetween(start: Date, end: Date) {
  const startDay = new Date(start.getFullYear(), start.getMonth(), start.getDate(), 12).getTime()
  const endDay = new Date(end.getFullYear(), end.getMonth(), end.getDate(), 12).getTime()
  return Math.round((endDay - startDay) / 86400000)
}

function formatMinutes(minutes: number) {
  if (minutes < 60) return `${minutes} min`
  const hours = Math.floor(minutes / 60)
  const remaining = minutes % 60
  return remaining ? `${hours}h ${remaining}m` : `${hours}h`
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("en-US", { month: "short", day: "numeric" }).format(parseDate(value))
}

function formatLongDate(value: string) {
  return new Intl.DateTimeFormat("en-US", { weekday: "short", month: "short", day: "numeric", year: "numeric" }).format(parseDate(value))
}

function initialSessions(today: Date): WorkoutSession[] {
  const todayKey = dateKey(today)
  const twoDaysAgo = new Date(today)
  twoDaysAgo.setDate(twoDaysAgo.getDate() - 2)
  const threeDaysAgo = new Date(today)
  threeDaysAgo.setDate(threeDaysAgo.getDate() - 3)
  return [
    { id: 1, date: todayKey, category: "Strength", minutes: 52 },
    { id: 2, date: dateKey(twoDaysAgo), category: "Strength", minutes: 46 },
    { id: 3, date: dateKey(threeDaysAgo), category: "Cardio", minutes: 32 },
  ]
}

export default function MobileWorkoutPage() {
  const today = useMemo(() => new Date(), [])
  const todayKey = dateKey(today)
  const [cycleStart, setCycleStart] = useState(todayKey)
  const [sessions, setSessions] = useState<WorkoutSession[]>(() => initialSessions(today))
  const [showForm, setShowForm] = useState(false)
  const [showSettings, setShowSettings] = useState(false)
  const [showBodyMetrics, setShowBodyMetrics] = useState(false)
  const [category, setCategory] = useState<WorkoutCategory>("Strength")
  const [activeFilter, setActiveFilter] = useState<WorkoutFilter>("All")
  const [minutes, setMinutes] = useState("45")

  const cycleStartDate = parseDate(cycleStart)
  const cycleOffset = daysBetween(cycleStartDate, today)
  const currentDay = cycleOffset >= 0 ? cycle[cycleOffset % cycle.length] : null
  const currentDayIndex = currentDay ? cycle.indexOf(currentDay) : -1
  const todaySession = sessions.find((session) => session.date === todayKey)
  const recentSessions = sessions.slice().sort((a, b) => b.date.localeCompare(a.date)).slice(0, 6)
  const visibleSessions = activeFilter === "All" ? recentSessions : recentSessions.filter((session) => session.category === activeFilter)
  const totals = useMemo(() => ({
    total: sessions.reduce((sum, session) => sum + session.minutes, 0),
    strength: sessions.filter((session) => session.category === "Strength").reduce((sum, session) => sum + session.minutes, 0),
    cardio: sessions.filter((session) => session.category === "Cardio").reduce((sum, session) => sum + session.minutes, 0),
  }), [sessions])
  const workoutCards = cycle.slice(0, 3).map((split) => {
    const loggedSessions = sessions
      .filter((session) => splitForDate(session.date) === split && (activeFilter === "All" || session.category === activeFilter))
      .sort((a, b) => b.date.localeCompare(a.date))
    const latestSession = loggedSessions[0]
    const loggedCategory = latestSession?.category ?? (activeFilter === "Cardio" ? "Cardio" : "Strength")
    return { split, latestSession, category: loggedCategory }
  }).filter(({ latestSession, split }) => activeFilter === "All" || latestSession || currentDay === split)
  const bodyMeasurements = useMemo(() => createBodyMeasurementPreview(today), [today])
  const bmi = calculateBmi(bodyMeasurements.weightKg, bodyMeasurements.heightCm)

  function splitForDate(value: string) {
    const offset = daysBetween(cycleStartDate, parseDate(value))
    return offset >= 0 ? cycle[offset % cycle.length] : "Rest"
  }

  function addSession(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const parsed = Number(minutes)
    if (!Number.isFinite(parsed) || parsed <= 0) return
    const next = Math.round(parsed)
    setSessions((current) => {
      const existing = current.find((session) => session.date === todayKey)
      if (existing) return current.map((session) => session.date === todayKey ? { ...session, category, minutes: next } : session)
      return [{ id: Date.now(), date: todayKey, category, minutes: next }, ...current]
    })
    setShowForm(false)
  }

  return (
    <div className="mobile-workout">
      <div className="mobile-workout-profile">
        <span className="mobile-workout-profile-avatar" aria-hidden="true">{sidebarConfig.user.name.slice(0, 1).toUpperCase()}</span>
        <span className="mobile-workout-profile-copy"><small>Good Morning</small><strong>{sidebarConfig.user.name}</strong></span>
        <button type="button" className="mobile-workout-settings-button" onClick={() => setShowSettings((current) => !current)} aria-expanded={showSettings} aria-label="Set cycle start date"><Settings2 aria-hidden="true" /></button>
      </div>
      <header className="mobile-workout-intro">
        <span className="mobile-workout-date-pill">{formatLongDate(todayKey)}</span>
        <h1>Today,<br /><span>Workout+</span></h1>
        <div className="mobile-workout-activity-summary" aria-label={`${sessions.length} workout sessions logged`}>
          <span className="mobile-workout-summary-avatar is-strength"><Dumbbell aria-hidden="true" /></span>
          <span className="mobile-workout-summary-avatar is-cardio"><Activity aria-hidden="true" /></span>
          <span className="mobile-workout-summary-avatar is-count">{sessions.length}</span>
          <span><strong>{sessions.length} sessions logged</strong><small>{formatMinutes(totals.total)} total time</small></span>
        </div>
      </header>

      <section id="workout-overview" className={`mobile-workout-current ${currentDay === "Rest" ? "is-rest" : ""}`} aria-label="Current training day">
        <div className="mobile-workout-current-top"><span>Workout Progress</span><span>{currentDay === "Rest" ? "Recovery day" : currentDay ? `Day ${currentDayIndex + 1} of 3` : "Set your start date"}</span></div>
        <div className="mobile-workout-current-main">
          <div className="mobile-workout-current-copy">
            <p className="mobile-workout-total-time">Total Time: {formatMinutes(totals.total)}</p>
            <strong>{currentDay ?? "Cycle has not started"}</strong>
            <small>{currentDay === "Rest" ? "Rest and recover" : currentDay ? `${currentDayIndex + 1} of 3 training days` : `Starts ${formatDate(cycleStart)}`}</small>
          </div>
          <div className={`mobile-workout-progress-ring${currentDay === "Rest" ? " is-rest" : ""}`} role="img" aria-label={currentDay === "Rest" ? "Rest day" : currentDay ? `${currentDayIndex + 1} of 3 training days` : `Cycle starts ${formatDate(cycleStart)}`} style={{ "--progress": `${currentDay === "Rest" ? 100 : currentDay ? ((currentDayIndex + 1) / 3) * 100 : 0}%` } as CSSProperties}>{currentDay === "Rest" ? <BedDouble aria-hidden="true" /> : <span>{currentDay ? currentDayIndex + 1 : "–"}<small>/3</small></span>}</div>
        </div>
      </section>

      {showSettings && <form className="mobile-workout-settings" onSubmit={(event) => { event.preventDefault(); setShowSettings(false) }}><label><span>Cycle starts on</span><input type="date" value={cycleStart} onChange={(event) => setCycleStart(event.target.value)} /></label><button type="submit"><Check aria-hidden="true" />Apply</button></form>}

      <section id="workout-activity" className="mobile-workout-activities" aria-labelledby="mobile-workout-activities-title">
        <h2 id="mobile-workout-activities-title" className="sr-only">Workout activity</h2>
        <div className="mobile-workout-filter" role="group" aria-label="Filter workout activity">{(["All", "Strength", "Cardio"] as WorkoutFilter[]).map((item) => <button key={item} type="button" aria-pressed={activeFilter === item} className={activeFilter === item ? "is-active" : ""} onClick={() => setActiveFilter(item)}>{item === "All" && <Activity aria-hidden="true" />}{item}</button>)}</div>
        <div className="mobile-workout-carousel" aria-live="polite">
          {workoutCards.map(({ split, latestSession, category: item }, index) => {
            const Icon = item === "Strength" ? Dumbbell : TimerReset
            return <article key={split} className={`mobile-workout-activity-card ${index === 0 ? "is-sky" : index === 1 ? "is-mint" : "is-lilac"}`}>
              <div className="mobile-workout-card-top"><span>{split}</span><span className="mobile-workout-card-arrow"><ArrowUpRight aria-hidden="true" /></span></div>
              <p>{latestSession ? latestSession.date === todayKey ? "Today's session" : `Last session · ${formatDate(latestSession.date)}` : "No session logged"}</p>
              <strong>{latestSession?.minutes ?? "—"}<small>{latestSession ? "min" : ""}</small></strong>
              <div className="mobile-workout-card-footer"><span><Icon aria-hidden="true" />{latestSession?.category ?? "Planned split"}</span><button type="button" className="mobile-workout-card-action" onClick={() => { setCategory(todaySession?.category ?? item); setMinutes(String(todaySession?.minutes ?? 45)); setShowForm(true) }}>{todaySession && currentDay === split ? "Edit today" : "Continue"}</button></div>
            </article>
          })}
        </div>
        {showForm && <form className="mobile-workout-form" onSubmit={addSession}><div className="mobile-workout-form-heading"><strong>{currentDay ?? "Workout"}</strong><span><CalendarDays aria-hidden="true" />{formatDate(todayKey)}</span></div><div className="mobile-workout-segmented" role="group" aria-label="Workout category">{(["Strength", "Cardio"] as WorkoutCategory[]).map((item) => <button type="button" key={item} className={category === item ? "is-selected" : ""} aria-pressed={category === item} onClick={() => setCategory(item)}>{item}</button>)}</div><label><span>Total duration</span><div className="mobile-workout-duration-input"><input required min="1" max="600" type="number" inputMode="numeric" value={minutes} onChange={(event) => setMinutes(event.target.value)} /><span>minutes</span></div></label><button type="submit" className="mobile-workout-save"><Check aria-hidden="true" />Save today</button></form>}
      </section>

      <section id="workout-body" className="mobile-workout-body" aria-labelledby="mobile-workout-body-title">
        <button type="button" className="mobile-workout-body-link" aria-expanded={showBodyMetrics} onClick={() => setShowBodyMetrics((current) => !current)}>
          <span className="mobile-workout-body-link-icon-left"><Activity aria-hidden="true" /></span>
          <span><strong id="mobile-workout-body-title">Body Metrics</strong><small>Latest measurements · Sample data</small></span>
          <span className="mobile-workout-body-link-icon"><ArrowUpRight aria-hidden="true" /></span>
        </button>
        {showBodyMetrics && <div className="mobile-workout-body-details">
          <div className="mobile-workout-body-stats">
            <article className="mobile-workout-body-weight"><span><Scale aria-hidden="true" />Weight</span><strong>{bodyMeasurements.weightKg ?? "--"}<small>kg</small></strong><p>Height {bodyMeasurements.heightCm ?? "--"} cm</p></article>
            <article className="mobile-workout-body-bmi"><span><Activity aria-hidden="true" />BMI</span><strong>{bmi?.toFixed(1) ?? "--"}</strong><p>Based on height and weight</p></article>
          </div>
          <article className="mobile-workout-measurements">
            <div className="mobile-workout-measurements-heading"><span><Ruler aria-hidden="true" />Body measurements</span><small>{formatDate(bodyMeasurements.recordedAt)}</small></div>
            <div className="mobile-workout-measurements-grid">
              <div><span>Chest</span><strong>{bodyMeasurements.chestCm ?? "--"}<small>cm</small></strong></div>
              <div><span>Waist</span><strong>{bodyMeasurements.waistCm ?? "--"}<small>cm</small></strong></div>
              <div><span>Upper arm</span><strong>{bodyMeasurements.upperArmCm ?? "--"}<small>cm</small></strong></div>
              <div><span>Forearm</span><strong>{bodyMeasurements.forearmCm ?? "--"}<small>cm</small></strong></div>
            </div>
          </article>
        </div>}
      </section>

      <section id="workout-history" className="mobile-workout-sessions" aria-labelledby="mobile-workout-sessions-title">
        <div className="mobile-workout-section-heading"><div><h2 id="mobile-workout-sessions-title">Recent activity</h2><p>Daily workout duration</p></div></div>
        <div className="mobile-workout-session-list">{visibleSessions.length > 0 ? visibleSessions.map((session) => { const split = splitForDate(session.date); return <article key={session.id} className="mobile-workout-session-row"><span className={`mobile-workout-session-icon ${splitColors[split]}`}><Dumbbell aria-hidden="true" /></span><div><strong>{split} <small>{session.category}</small></strong><span>{session.date === todayKey ? "Today" : formatDate(session.date)}</span></div><b>{formatMinutes(session.minutes)}</b></article> }) : <p className="mobile-workout-empty">No {activeFilter.toLowerCase()} sessions yet.</p>}</div>
      </section>
    </div>
  )
}
