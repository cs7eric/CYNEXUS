import { Activity, ArrowUpRight, BedDouble, CalendarDays, Check, Dumbbell, Ruler, Scale, Settings2, TimerReset, X } from "lucide-react"
import { useMemo, useState, type CSSProperties, type FormEvent } from "react"
import { useLocation, useNavigate } from "react-router-dom"
import { calculateBmi } from "@/mobile/components/activity-calendar/model"
import { useWorkoutRecords } from "@/mobile/hooks/use-workout-records"
import { WorkoutHistoryView } from "./components/workout-history-view"
import { WorkoutBodyView } from "./components/body-view"
import { WorkoutFormDialog } from "./components/workout-form-dialog"
import { WORKOUT_CYCLE, dateKey, parseDate, formatDate, formatMinutes, latestObservation, saveObservation, saveSession, startPersonalLog, splitOnDate, type WorkoutCategory, type SplitDay } from "./lib/model"
import "@/mobile/pages/workout/workout.css"

type WorkoutFilter = "All" | WorkoutCategory
type WorkoutView = "overview" | "activity" | "body" | "history"
const cycle = WORKOUT_CYCLE
const splitColors: Record<SplitDay, string> = { "Chest + Triceps": "is-blue", "Back + Biceps": "is-mint", Shoulders: "is-lilac", Rest: "is-rest" }
function formatLongDate(value: string) { return parseDate(value).toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric", year: "numeric" }) }
function getView(hash: string): WorkoutView {
  if (hash === "#workout-activity") return "activity"
  if (hash === "#workout-body") return "body"
  if (hash === "#workout-history") return "history"
  return "overview"
}

export default function MobileWorkoutPage() {
  const { data: records, storageError, update, savePlanRemote, saveSessionRemote, saveMeasurementRemote, deleteSessionRemote, deleteMeasurementRemote } = useWorkoutRecords()
  const location = useLocation()
  const navigate = useNavigate()
  const today = useMemo(() => new Date(), [])
  const todayKey = dateKey(today)
  const activeView = getView(location.hash)
  const cycleStart = records.cycleStart
  const [sessionDate, setSessionDate] = useState(todayKey)
  const [showForm, setShowForm] = useState(false)
  const [showSettings, setShowSettings] = useState(false)
  const [showBodyMetrics, setShowBodyMetrics] = useState(false)
  const [category, setCategory] = useState<WorkoutCategory>("Strength")
  const [overviewFilter, setOverviewFilter] = useState<WorkoutFilter>("All")
  const [minutes, setMinutes] = useState("45")
  const [formError, setFormError] = useState<string | null>(null)
  const currentDay = splitOnDate(cycleStart, todayKey)
  const currentDayIndex = currentDay ? cycle.indexOf(currentDay) : -1
  const displaySessions = records.sessions
  const todaySession = displaySessions.find((session) => session.date === todayKey)
  const recentSessions = displaySessions.slice().sort((a, b) => b.date.localeCompare(a.date)).slice(0, 6)
  const visibleRecentSessions = overviewFilter === "All" ? recentSessions : recentSessions.filter((session) => session.category === overviewFilter)
  const totals = { total: displaySessions.reduce((sum, session) => sum + session.minutes, 0) }
  const workoutCards = cycle.slice(0, 3).map((split) => {
    const latestSession = displaySessions.filter((session) => session.split === split && (overviewFilter === "All" || session.category === overviewFilter)).sort((a, b) => b.date.localeCompare(a.date))[0]
    return { split, latestSession, category: latestSession?.category ?? (overviewFilter === "Cardio" ? "Cardio" : "Strength") as WorkoutCategory }
  }).filter(({ latestSession, split }) => overviewFilter === "All" || latestSession || currentDay === split)
  const bodyMeasurements = useMemo(() => {
    const value = (metric: "height" | "weight" | "chest" | "waist" | "upperArm" | "forearm") => latestObservation(records.observations, metric, metric === "upperArm" || metric === "forearm" ? "left" : null, todayKey)?.value ?? null
    return {
      recordedAt: [...records.observations].sort((a, b) => b.measuredAt.localeCompare(a.measuredAt))[0]?.measuredAt ?? todayKey,
      heightCm: value("height"), weightKg: value("weight"), chestCm: value("chest"), waistCm: value("waist"), upperArmCm: value("upperArm"), forearmCm: value("forearm"),
    }
  }, [records.observations, todayKey])
  const bmi = calculateBmi(bodyMeasurements.weightKg, bodyMeasurements.heightCm)
  const waistMarker = bodyMeasurements.waistCm == null ? null : 9 + Math.min(1, Math.max(0, (bodyMeasurements.waistCm - 60) / 30)) * 162

  function openWorkoutForm(nextCategory: WorkoutCategory = todaySession?.category ?? "Strength", date = todayKey) {
    const existing = records.isSample ? null : displaySessions.find((session) => session.date === date)
    setSessionDate(date); setCategory(existing?.category ?? nextCategory); setMinutes(String(existing?.minutes ?? 45)); setFormError(null); setShowForm(true)
  }
  function addSession(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    try {
      const result = update((data) => saveSession(data, { date: sessionDate, category, minutes: Number(minutes) }, todayKey, crypto.randomUUID()))
      const saved = result.data.sessions.find((session) => session.date === sessionDate)
      if (saved) void saveSessionRemote(saved).catch(() => undefined)
      setShowForm(false)
    } catch (error) { setFormError(error instanceof Error ? error.message : "Could not save workout.") }
  }
  function renderWorkoutForm() {
    if (!showForm) return null
    const existing = records.isSample ? null : displaySessions.find((session) => session.date === sessionDate)
    const savedSplit = existing?.category === "Strength" ? existing.split : null
    const formTitle = category === "Cardio" ? "Cardio" : savedSplit ?? splitOnDate(cycleStart, sessionDate) ?? "Strength"
    return <WorkoutFormDialog onClose={() => setShowForm(false)}><form className="mobile-workout-form" onSubmit={addSession}>
      <div className="mobile-workout-form-heading"><strong>{formTitle}</strong><span><CalendarDays aria-hidden="true" />{formatDate(sessionDate)}</span><button type="button" className="mobile-workout-form-close" aria-label="Close workout form" onClick={() => setShowForm(false)}><X aria-hidden="true" /></button></div>
      <label><span>Workout date</span><input required type="date" max={todayKey} value={sessionDate} onChange={(event) => setSessionDate(event.target.value)} /></label>
      <div className="mobile-workout-segmented" role="group" aria-label="Workout category">{(["Strength", "Cardio"] as WorkoutCategory[]).map((item) => <button type="button" key={item} className={category === item ? "is-selected" : ""} aria-pressed={category === item} onClick={() => setCategory(item)}>{item}</button>)}</div>
      <label><span>Total duration</span><div className="mobile-workout-duration-input"><input required min="1" max="1440" step="1" type="number" inputMode="numeric" value={minutes} onChange={(event) => setMinutes(event.target.value)} /><span>minutes</span></div></label>
      {records.isSample && <p className="mobile-workout-empty">Saving starts your personal log. Sample records are excluded.</p>}
      {formError && <p role="alert">{formError}</p>}
      <button type="submit" className="mobile-workout-save"><Check aria-hidden="true" />Save workout</button>
    </form></WorkoutFormDialog>
  }
  return (
    <div className={`mobile-workout is-${activeView}`}>
      {storageError && <p className="mobile-workout-empty" role="alert">{storageError}</p>}
      {activeView === "overview" && <>
        <header className="mobile-workout-intro">
          <span className="mobile-workout-date-pill">{formatLongDate(todayKey)}</span>
          <h1>Today,<br /><span>Workout+</span></h1>
            <div className="mobile-workout-activity-summary" aria-label={`${displaySessions.length} workout sessions logged`}>
            <span className="mobile-workout-summary-avatar is-strength"><Dumbbell aria-hidden="true" /></span>
            <span className="mobile-workout-summary-avatar is-cardio"><Activity aria-hidden="true" /></span>
            <span className="mobile-workout-summary-avatar is-count">{displaySessions.length}</span>
            <span><strong>{displaySessions.length} sessions logged</strong><small>{formatMinutes(totals.total)} total time</small></span>
          </div>
        </header>

        <section id="workout-overview" className={`mobile-workout-current ${currentDay === "Rest" ? "is-rest" : ""}`} aria-label="Current training day">
          <div className="mobile-workout-current-top"><span>Workout Progress</span><span className="mobile-workout-current-actions"><span>{currentDay === "Rest" ? "Recovery day" : currentDay ? `Day ${currentDayIndex + 1} of 3` : "Set your start date"}</span><button type="button" className="mobile-workout-settings-button" onClick={() => setShowSettings((current) => !current)} aria-expanded={showSettings} aria-label="Set cycle start date"><Settings2 aria-hidden="true" /></button></span></div>
          <div className="mobile-workout-current-main">
            <div className="mobile-workout-current-copy">
              <p className="mobile-workout-total-time">Total Time: {formatMinutes(totals.total)}</p>
              <strong>{currentDay ?? "Cycle has not started"}</strong>
              <small>{currentDay === "Rest" ? "Rest and recover" : currentDay ? `${currentDayIndex + 1} of 3 training days` : `Starts ${formatDate(cycleStart)}`}</small>
            </div>
            <div className={`mobile-workout-progress-ring${currentDay === "Rest" ? " is-rest" : ""}`} role="img" aria-label={currentDay === "Rest" ? "Rest day" : currentDay ? `${currentDayIndex + 1} of 3 training days` : `Cycle starts ${formatDate(cycleStart)}`} style={{ "--progress": `${currentDay === "Rest" ? 100 : currentDay ? ((currentDayIndex + 1) / 3) * 100 : 0}%` } as CSSProperties}>{currentDay === "Rest" ? <BedDouble aria-hidden="true" /> : <span>{currentDay ? currentDayIndex + 1 : "–"}<small>/3</small></span>}</div>
          </div>
        </section>

        {showSettings && <form className="mobile-workout-settings" onSubmit={(event) => { event.preventDefault(); setShowSettings(false) }}><label><span>Cycle starts on</span><input type="date" value={cycleStart} onChange={(event) => { if (event.target.value) { update((data) => ({ ...data, cycleStart: event.target.value })); void savePlanRemote(event.target.value).catch(() => undefined) } }} /></label><button type="submit"><Check aria-hidden="true" />Apply</button></form>}

        <section className="mobile-workout-activities" aria-labelledby="mobile-workout-activities-title">
          <h2 id="mobile-workout-activities-title" className="sr-only">Workout activity</h2>
          <div className="mobile-workout-filter" role="group" aria-label="Filter workout activity">{(["All", "Strength", "Cardio"] as WorkoutFilter[]).map((item) => <button key={item} type="button" aria-pressed={overviewFilter === item} className={overviewFilter === item ? "is-active" : ""} onClick={() => setOverviewFilter(item)}>{item === "All" && <Activity aria-hidden="true" />}{item}</button>)}</div>
          <div className="mobile-workout-carousel" aria-live="polite">
            {workoutCards.map(({ split, latestSession, category: item }, index) => {
              const Icon = item === "Strength" ? Dumbbell : TimerReset
              return <article key={split} className={`mobile-workout-activity-card ${index === 0 ? "is-sky" : index === 1 ? "is-mint" : "is-lilac"}`}>
                <div className="mobile-workout-card-top"><span>{split}</span><span className="mobile-workout-card-arrow"><ArrowUpRight aria-hidden="true" /></span></div>
                <p>{latestSession ? latestSession.date === todayKey ? "Today's session" : `Last session · ${formatDate(latestSession.date)}` : "No session logged"}</p>
                <strong>{latestSession?.minutes ?? "—"}<small>{latestSession ? "min" : ""}</small></strong>
                <div className="mobile-workout-card-footer"><span><Icon aria-hidden="true" />{latestSession?.category ?? "Planned split"}</span><button type="button" className="mobile-workout-card-action" onClick={() => openWorkoutForm(todaySession?.category ?? item)}>{todaySession && currentDay === split ? "Edit today" : "Continue"}</button></div>
              </article>
            })}
          </div>
          {renderWorkoutForm()}
        </section>

        <section className="mobile-workout-body" aria-labelledby="mobile-workout-body-title">
          <button type="button" className="mobile-workout-body-link" aria-expanded={showBodyMetrics} onClick={() => setShowBodyMetrics((current) => !current)}>
            <span className="mobile-workout-body-link-icon-left"><Activity aria-hidden="true" /></span>
            <span><strong id="mobile-workout-body-title">Body Metrics</strong><small>Latest measurements · {records.isSample ? "Sample data" : "Account sync enabled"}</small></span>
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

        <section className="mobile-workout-sessions" aria-labelledby="mobile-workout-sessions-title">
          <div className="mobile-workout-section-heading"><div><h2 id="mobile-workout-sessions-title">Recent activity</h2><p>Daily workout duration</p></div></div>
          <div className="mobile-workout-session-list">{visibleRecentSessions.length > 0 ? visibleRecentSessions.map((session) => { const split = session.category === "Cardio" ? "Cardio" : session.split ?? "Strength"; const Icon = session.category === "Cardio" ? Activity : Dumbbell; return <article key={session.id} className="mobile-workout-session-row"><span className={`mobile-workout-session-icon ${session.split ? splitColors[session.split] : "is-mint"}`}><Icon aria-hidden="true" /></span><div><strong>{split} <small>{session.category}</small></strong><span>{session.date === todayKey ? "Today" : formatDate(session.date)}</span></div><b>{formatMinutes(session.minutes)}</b></article> }) : <p className="mobile-workout-empty">No {overviewFilter.toLowerCase()} sessions yet.</p>}</div>
        </section>
      </>}

      {activeView === "activity" && <>
        <header className="mobile-workout-daily-heading">
          <h1>Daily<br />Activities</h1>
          <span className="mobile-workout-weekday">{new Intl.DateTimeFormat("en-US", { weekday: "long" }).format(today)}</span>
        </header>
        <section className="mobile-workout-daily-summary" aria-label="Today's workout summary">
          <div className="mobile-workout-agenda"><span>{todaySession ? "1 Agenda:" : "Agenda:"}</span><strong>{currentDay ?? "Training cycle not set"}</strong></div>
          <button type="button" className="mobile-workout-duration-summary" onClick={() => openWorkoutForm()} aria-label={todaySession ? "Edit today's workout duration" : "Log today's workout duration"}>
            <span>Workout time:</span><strong>{todaySession?.minutes ?? "--"}<small>{todaySession ? "min" : ""}</small></strong>
          </button>
        </section>
        {renderWorkoutForm()}
        <section className="mobile-workout-daily-metrics" aria-label="Daily body metrics">
          <button type="button" className="mobile-workout-daily-metric is-weight" onClick={() => navigate({ pathname: location.pathname, search: location.search, hash: "#workout-body" })}>
            <span className="mobile-workout-daily-metric-title"><Scale aria-hidden="true" />Weight<span className="mobile-workout-daily-metric-arrow"><ArrowUpRight aria-hidden="true" /></span></span>
            <strong>{bodyMeasurements.weightKg ?? "--"}<small>kg</small></strong><span className="mobile-workout-daily-metric-note">Latest weight</span>
          </button>
          <button type="button" className="mobile-workout-daily-metric is-bmi" onClick={() => navigate({ pathname: location.pathname, search: location.search, hash: "#workout-body" })}>
            <span className="mobile-workout-daily-metric-title"><Activity aria-hidden="true" />BMI<span className="mobile-workout-daily-metric-arrow"><ArrowUpRight aria-hidden="true" /></span></span>
            <strong>{bmi?.toFixed(1) ?? "--"}</strong><span className="mobile-workout-daily-metric-note is-badge">Height {bodyMeasurements.heightCm ?? "--"} cm</span>
          </button>
        </section>
        <section className="mobile-workout-composition" aria-labelledby="mobile-workout-composition-title">
          <div className="mobile-workout-composition-heading"><span id="mobile-workout-composition-title"><Ruler aria-hidden="true" />Body Composition</span><button type="button" className="mobile-workout-composition-arrow" aria-label="View all body measurements" title="View all body measurements" onClick={() => navigate({ pathname: location.pathname, search: location.search, hash: "#workout-body" })}><ArrowUpRight aria-hidden="true" /></button></div>
          <div className="mobile-workout-composition-content">
            <div className="mobile-workout-waist"><strong>{bodyMeasurements.waistCm ?? "--"}<small>cm</small></strong><span>Waist</span></div>
            <svg className="mobile-workout-composition-scale" viewBox="0 0 180 88" role="img" aria-label={`Waist: ${bodyMeasurements.waistCm == null ? "not recorded" : `${bodyMeasurements.waistCm} centimeters`}. Display scale: 60 to 90 centimeters.`}>
              <path className="mobile-workout-composition-band" d="M13 13H43C52 13 52 19 61 19S71 13 80 13S90 19 99 19S109 13 118 13H151V53H118C109 53 108 47 99 47S89 53 80 53S70 47 61 47S51 53 43 53H13Q3 53 3 43V23Q3 13 13 13Z" />
              <rect className="mobile-workout-composition-band" x="156" y="13" width="21" height="40" rx="10.5" />
              {waistMarker != null && <g className="mobile-workout-composition-marker"><path d={`M${waistMarker} 8V66`} /><path d={`M${waistMarker} 63l-4 7h8Z`} /></g>}
              {[60, 70, 80, 90].map((value, index) => <text x={9 + index * 54} y="85" textAnchor="middle" key={value}>{value}</text>)}
            </svg>
          </div>
        </section>
        <p className="mobile-workout-activity-source">{records.isSample ? "Sample data" : "Account sync enabled"}</p>
      </>}

      {activeView === "body" && <WorkoutBodyView today={today} observations={records.observations} isSample={records.isSample}
        onSaveObservation={(input) => { const result = update((data) => saveObservation(data, input, todayKey, crypto.randomUUID())); const saved = result.data.observations.find((item) => item.metric === input.metric && item.side === input.side && item.measuredAt === input.measuredAt); if (saved) void saveMeasurementRemote(saved).catch(() => undefined) }}
        onDeleteObservation={(id) => { update((data) => ({ ...data, observations: data.observations.filter((item) => item.id !== id) })); void deleteMeasurementRemote(id).catch(() => undefined) }}
        onStartLog={() => { const result = update(startPersonalLog); void savePlanRemote(result.data.cycleStart).catch(() => undefined) }} />}
      {activeView === "history" && <>
        <WorkoutHistoryView today={today} sessions={displaySessions} isSample={records.isSample}
          onLogWorkout={(date) => openWorkoutForm("Strength", date)}
          onStartLog={() => { if (records.isSample) { const result = update(startPersonalLog); void savePlanRemote(result.data.cycleStart).catch(() => undefined) }; openWorkoutForm() }}
          onDeleteSession={(id) => { update((data) => ({ ...data, sessions: data.sessions.filter((item) => item.id !== id) })); void deleteSessionRemote(id).catch(() => undefined) }} />
        {renderWorkoutForm()}
      </>}
    </div>
  )
}
