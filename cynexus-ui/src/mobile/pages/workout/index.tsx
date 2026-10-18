import { CalendarDays, Check, Dumbbell, HeartPulse, Plus, Settings2, TimerReset, X } from "lucide-react"
import { useMemo, useState, type FormEvent } from "react"
import "@/mobile/pages/workout/workout.css"

type WorkoutCategory = "Strength" | "Cardio"
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
  const [category, setCategory] = useState<WorkoutCategory>("Strength")
  const [minutes, setMinutes] = useState("45")

  const cycleStartDate = parseDate(cycleStart)
  const cycleOffset = daysBetween(cycleStartDate, today)
  const currentDay = cycleOffset >= 0 ? cycle[cycleOffset % cycle.length] : null
  const currentDayIndex = currentDay ? cycle.indexOf(currentDay) : -1
  const recentSessions = sessions.slice().sort((a, b) => b.date.localeCompare(a.date)).slice(0, 6)
  const totals = useMemo(() => ({
    total: sessions.reduce((sum, session) => sum + session.minutes, 0),
    strength: sessions.filter((session) => session.category === "Strength").reduce((sum, session) => sum + session.minutes, 0),
    cardio: sessions.filter((session) => session.category === "Cardio").reduce((sum, session) => sum + session.minutes, 0),
  }), [sessions])

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
      <header className="mobile-workout-heading">
        <div><p className="mobile-workout-eyebrow">Workout</p><h2>Train on your rhythm.</h2><p className="mobile-workout-subtitle">Three training days, one rest day. The current focus follows your start date.</p></div>
        <span className="mobile-workout-header-icon"><Dumbbell aria-hidden="true" /></span>
      </header>

      <section className={`mobile-workout-current ${currentDay === "Rest" ? "is-rest" : ""}`} aria-label="Current training day">
        <div className="mobile-workout-current-top"><span>Today</span><span>{formatDate(todayKey)}</span></div>
        <strong>{currentDay ?? "Cycle has not started"}</strong>
        <p>{currentDay === "Rest" ? "Recover today. Your next training day is already planned." : currentDay ? `${currentDayIndex + 1} of 3 training days` : `Starts ${formatDate(cycleStart)}`}</p>
        {currentDay && currentDay !== "Rest" && <div className="mobile-workout-current-progress"><i style={{ width: `${((currentDayIndex + 1) / 3) * 100}%` }} /></div>}
      </section>

      <section className="mobile-workout-total" aria-label="Workout summary">
        <div className="mobile-workout-total-copy"><span>Logged time</span><strong>{formatMinutes(totals.total)}</strong><small>{sessions.length} days recorded</small></div>
        <div className="mobile-workout-total-metrics"><div><HeartPulse aria-hidden="true" /><span>Strength</span><strong>{formatMinutes(totals.strength)}</strong></div><div><TimerReset aria-hidden="true" /><span>Cardio</span><strong>{formatMinutes(totals.cardio)}</strong></div></div>
      </section>

      <section className="mobile-workout-cycle" aria-labelledby="mobile-workout-cycle-title">
        <div className="mobile-workout-section-heading"><div><h3 id="mobile-workout-cycle-title">Three-day split</h3><p>3 on / 1 off</p></div><button type="button" className="mobile-workout-settings-button" onClick={() => setShowSettings((current) => !current)} aria-expanded={showSettings} aria-label="Set cycle start date"><Settings2 aria-hidden="true" /></button></div>
        {showSettings && <form className="mobile-workout-settings" onSubmit={(event) => { event.preventDefault(); setShowSettings(false) }}><label><span>Cycle starts on</span><input type="date" value={cycleStart} onChange={(event) => setCycleStart(event.target.value)} /></label><button type="submit"><Check aria-hidden="true" />Apply</button></form>}
        <div className="mobile-workout-split-grid">{cycle.map((name, index) => <article key={name} className={`mobile-workout-split-card ${splitColors[name]}${currentDay === name ? " is-current" : ""}`}><span className="mobile-workout-split-index">{index + 1}</span><strong>{name}</strong><small>{name === "Rest" ? "Recovery" : "Strength focus"}</small></article>)}</div>
      </section>

      <section className="mobile-workout-sessions" aria-labelledby="mobile-workout-sessions-title">
        <div className="mobile-workout-section-heading"><div><h3 id="mobile-workout-sessions-title">Daily duration</h3><p>One total time per day</p></div><button type="button" className="mobile-workout-add-button" onClick={() => setShowForm((current) => !current)} aria-expanded={showForm}>{showForm ? <X aria-hidden="true" /> : <Plus aria-hidden="true" />}<span>{showForm ? "Close" : "Log today"}</span></button></div>
        {showForm && <form className="mobile-workout-form" onSubmit={addSession}><div className="mobile-workout-form-heading"><strong>{currentDay ?? "Workout"}</strong><span><CalendarDays aria-hidden="true" />{formatDate(todayKey)}</span></div><div className="mobile-workout-segmented" role="group" aria-label="Workout category">{(["Strength", "Cardio"] as WorkoutCategory[]).map((item) => <button type="button" key={item} className={category === item ? "is-selected" : ""} onClick={() => setCategory(item)}>{item}</button>)}</div><label><span>Total duration</span><div className="mobile-workout-duration-input"><input required min="1" max="600" type="number" inputMode="numeric" value={minutes} onChange={(event) => setMinutes(event.target.value)} /><span>minutes</span></div></label><button type="submit" className="mobile-workout-save"><Check aria-hidden="true" />Save today</button></form>}
        <div className="mobile-workout-session-list">{recentSessions.map((session) => { const split = splitForDate(session.date); return <article key={session.id} className="mobile-workout-session-row"><span className={`mobile-workout-session-icon ${splitColors[split]}`}><Dumbbell aria-hidden="true" /></span><div><strong>{split} <small>{session.category}</small></strong><span>{session.date === todayKey ? "Today" : formatDate(session.date)}</span></div><b>{formatMinutes(session.minutes)}</b></article> })}</div>
      </section>
    </div>
  )
}
