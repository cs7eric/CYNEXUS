import { Beef, BicepsFlexed, Droplet, Dumbbell, Gauge, LayoutGrid, RefreshCw, Ruler, Utensils, WalletCards, Weight, Wheat } from "lucide-react"
import { useId, useRef, useState, type KeyboardEvent, type PointerEvent } from "react"

import { MobileButton } from "@/mobile/ui/button"
import { activityCalendarDays, activityDateFromKey, activityDateKey, calculateBmi, shiftActivityDate, type BodyMeasurementSnapshot, type DailyActivity, type FinanceSummarySnapshot } from "./model"
import "./styles.css"

const weekdays = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]
const shortDate = new Intl.DateTimeFormat("en-US", { month: "short", day: "numeric" })
const fullDate = new Intl.DateTimeFormat("en-US", { weekday: "long", year: "numeric", month: "long", day: "numeric" })
const compactNumber = new Intl.NumberFormat("en-US", { notation: "compact", maximumFractionDigits: 1 })
const number = new Intl.NumberFormat("en-US")
const grams = new Intl.NumberFormat("en-US", { maximumFractionDigits: 1 })
const measurement = new Intl.NumberFormat("en-US", { maximumFractionDigits: 1 })
const measurementDate = new Intl.DateTimeFormat("en-US", { month: "short", day: "numeric" })
const financeAmount = new Intl.NumberFormat("en-US", { notation: "compact", maximumFractionDigits: 1 })
const macronutrients = [
  { key: "carbsGrams", label: "Carbs", icon: Wheat, tone: "carbs" },
  { key: "proteinGrams", label: "Protein", icon: Beef, tone: "protein" },
  { key: "fatGrams", label: "Fat", icon: Droplet, tone: "fat" },
] as const

function formatFinanceAmount(value: number | null, currency: string | null) {
  if (value == null || !Number.isFinite(value)) return "—"
  const amount = financeAmount.format(value)
  return currency ? `${currency} ${amount}` : amount
}

type SummaryView = "food" | "workout" | "finance"
const summaryViews: SummaryView[] = ["food", "workout", "finance"]

export function MobileActivityCalendar({ today, records, bodyMeasurements = null, finance = null, onFinanceRetry, showFinance = true, isSample = false, nutritionState = "ready", showWeek = true }: { today: Date; records: readonly DailyActivity[]; bodyMeasurements?: BodyMeasurementSnapshot | null; finance?: FinanceSummarySnapshot | null; onFinanceRetry?: () => void; showFinance?: boolean; isSample?: boolean; nutritionState?: "loading" | "ready" | "error"; showWeek?: boolean }) {
  const id = useId()
  const todayKey = activityDateKey(today)
  const [selectedKey, setSelectedKey] = useState(todayKey)
  const [summaryView, setSummaryView] = useState<SummaryView>("food")
  const [summaryDragOffset, setSummaryDragOffset] = useState(0)
  const [summaryDragging, setSummaryDragging] = useState(false)
  const summaryPointer = useRef<{ id: number; x: number } | null>(null)
  const buttons = useRef(new Map<string, HTMLButtonElement>())
  const visibleSummaryViews = showFinance ? summaryViews : summaryViews.slice(0, 2)
  const activeSummaryView = visibleSummaryViews.includes(summaryView) ? summaryView : "food"
  const selected = activityDateFromKey(selectedKey)
  const days = activityCalendarDays(today, selected, false)
  const tabStop = days.find((day) => day.available && day.key === selectedKey)?.key ?? days.find((day) => day.available)?.key
  const entries = new Map(records.map((entry) => [entry.date, entry]))
  const activity = entries.get(selectedKey)
  const nutrition = activity?.nutrition
  const targets = activity?.nutritionTargets
  const nutritionUnavailable = nutritionState !== "ready"
  const visibleTargets = nutritionUnavailable ? null : targets
  const workout = activity?.workout
  const bmi = calculateBmi(bodyMeasurements?.weightKg ?? null, bodyMeasurements?.heightCm ?? null)
  const bodyMetrics = [
    { label: "Height", icon: Ruler, value: bodyMeasurements?.heightCm, unit: "cm" },
    { label: "Weight", icon: Weight, value: bodyMeasurements?.weightKg, unit: "kg" },
    { label: "BMI", icon: Gauge, value: bmi, unit: "" },
    { label: "Chest", icon: null, value: bodyMeasurements?.chestCm, unit: "cm" },
    { label: "Waist", icon: null, value: bodyMeasurements?.waistCm, unit: "cm" },
    { label: "Upper arm", icon: BicepsFlexed, value: bodyMeasurements?.upperArmCm, unit: "cm" },
    { label: "Forearm", icon: null, value: bodyMeasurements?.forearmCm, unit: "cm" },
  ]
  const selectedLabel = `${selectedKey === todayKey ? "Today" : weekdays[selected.getDay()]}, ${shortDate.format(selected)}${selected.getFullYear() !== today.getFullYear() ? `, ${selected.getFullYear()}` : ""}`
  const showToday = selectedKey !== todayKey

  function focusDate(key: string) {
    setSelectedKey(key)
    requestAnimationFrame(() => buttons.current.get(key)?.focus())
  }

  function navigateDate(event: KeyboardEvent<HTMLButtonElement>, date: Date) {
    const offset = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7, Home: -date.getDay(), End: 6 - date.getDay() }[event.key]
    if (offset === undefined) return
    event.preventDefault()
    const next = shiftActivityDate(date, offset)
    focusDate(activityDateKey(next > shiftActivityDate(today, 0) ? today : next))
  }

  function selectSummaryView(view: SummaryView) {
    setSummaryView(view)
    setSummaryDragOffset(0)
  }

  function startSummarySwipe(event: PointerEvent<HTMLDivElement>) {
    if (!event.isPrimary) return
    summaryPointer.current = { id: event.pointerId, x: event.clientX }
    setSummaryDragging(true)
    event.currentTarget.setPointerCapture(event.pointerId)
  }

  function moveSummarySwipe(event: PointerEvent<HTMLDivElement>) {
    const pointer = summaryPointer.current
    if (!pointer || pointer.id !== event.pointerId) return
    const delta = event.clientX - pointer.x
    setSummaryDragOffset(Math.max(-120, Math.min(120, delta)))
  }

  function endSummarySwipe(event: PointerEvent<HTMLDivElement>) {
    const pointer = summaryPointer.current
    if (!pointer || pointer.id !== event.pointerId) return
    const delta = event.clientX - pointer.x
    summaryPointer.current = null
    setSummaryDragging(false)
    setSummaryDragOffset(0)
    if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId)
    if (Math.abs(delta) < 42) return
    const currentIndex = visibleSummaryViews.indexOf(activeSummaryView)
    const nextIndex = delta < 0 ? Math.min(currentIndex + 1, visibleSummaryViews.length - 1) : Math.max(currentIndex - 1, 0)
    selectSummaryView(visibleSummaryViews[nextIndex])
  }
  const summaryIndex = visibleSummaryViews.indexOf(activeSummaryView)

  return (
    <section className="mobile-activity" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`} className="sr-only">Activity summary</h2>
      {showWeek && <div id={`${id}-dates`} className="mobile-activity-dates">
        <div className="mobile-activity-weekdays" aria-hidden="true">{weekdays.map((day) => <span key={day}>{day}</span>)}</div>
        <ol className="mobile-activity-days" aria-label="Week, choose a date">
          {days.map(({ date, key, available }) => {
            const entry = entries.get(key)
            const current = key === todayKey
            const label = `${fullDate.format(date)}${current ? ", today" : ""}, ${entry?.nutrition ? `${entry.nutrition.calories} kcal consumed` : "no nutrition logged"}, ${entry?.workout ? entry.workout.minutes === 0 ? "rest day" : `${entry.workout.minutes} minutes of exercise` : "no workout logged"}`
            return (
              <li key={key}>
                <button type="button" ref={(element) => { if (element) buttons.current.set(key, element); else buttons.current.delete(key) }} className="mobile-activity-day" disabled={!available} tabIndex={key === tabStop ? 0 : -1} aria-label={label} aria-pressed={key === selectedKey} aria-current={current ? "date" : undefined} onClick={() => setSelectedKey(key)} onKeyDown={(event) => navigateDate(event, date)}>
                  <span className="mobile-activity-today-dot" aria-hidden="true" />
                  <time dateTime={key}>{date.getDate()}</time>
                  <span className="mobile-activity-mini" aria-hidden="true">
                    <span data-empty={!entry?.nutrition}><Utensils />{entry?.nutrition ? compactNumber.format(entry.nutrition.calories).toLowerCase() : "—"}</span>
                    <span data-empty={!entry?.workout}><Dumbbell />{entry?.workout ? entry.workout.minutes === 0 ? "Rest" : `${entry.workout.minutes}m` : "—"}</span>
                  </span>
                </button>
              </li>
            )
          })}
        </ol>
      </div>}
      {showToday && <div className="mobile-activity-detail-heading"><MobileButton variant="ghost" className="mobile-activity-today" onPress={() => focusDate(todayKey)}>Today</MobileButton></div>}
      <div className={showWeek ? "mobile-activity-summary" : "mobile-activity-summary is-summary-only"} aria-live="polite" aria-atomic="true">
        <p className="sr-only">{selectedLabel}{isSample ? ", sample data" : ""}</p>
        <div className="mobile-activity-summary-switcher" role="group" aria-label="Activity detail">
          <button type="button" aria-pressed={activeSummaryView === "food"} aria-controls={`${id}-food`} className="mobile-activity-summary-tab" onClick={() => selectSummaryView("food")}>
            <Utensils aria-hidden="true" /> Food
          </button>
          <button type="button" aria-pressed={activeSummaryView === "workout"} aria-controls={`${id}-workout`} className="mobile-activity-summary-tab" onClick={() => selectSummaryView("workout")}>
            <Dumbbell aria-hidden="true" /> Workout
          </button>
          {showFinance && <button type="button" aria-pressed={activeSummaryView === "finance"} aria-controls={`${id}-finance`} className="mobile-activity-summary-tab" onClick={() => selectSummaryView("finance")}>
            <WalletCards aria-hidden="true" /> Finance
          </button>}
        </div>
        <div className="mobile-activity-summary-viewport" onPointerDown={startSummarySwipe} onPointerMove={moveSummarySwipe} onPointerUp={endSummarySwipe} onPointerCancel={endSummarySwipe}>
          <div className={`mobile-activity-summary-track ${visibleSummaryViews.length === 3 ? "is-three" : "is-two"}${summaryDragging ? " is-dragging" : ""}`} style={{ transform: `translate3d(calc(${summaryIndex * -(100 / visibleSummaryViews.length)}% + ${summaryDragOffset}px), 0, 0)` }}>
        <div id={`${id}-food`} aria-hidden={activeSummaryView !== "food"} className="mobile-activity-stat mobile-activity-summary-panel mobile-activity-nutrition">
          <div className="mobile-activity-food-heading">
            <div>
              <p className="mobile-activity-food-eyebrow">Today’s intake</p>
              <h3>On your table<span>.</span></h3>
            </div>
            <div className="mobile-activity-food-energy">
              <strong>{nutritionUnavailable ? "—" : nutrition ? number.format(nutrition.calories) : "0"}</strong>
              <span>{nutritionUnavailable ? (nutritionState === "loading" ? "Loading…" : "Unavailable") : visibleTargets?.calories != null ? `/ ${number.format(visibleTargets.calories)} kcal` : "kcal · No target"}</span>
            </div>
          </div>
          <div className="mobile-activity-food-tabs" aria-label="Meals">
            <span className="is-active"><LayoutGrid aria-hidden="true" />All day</span>
          </div>
          <div className="mobile-activity-nutrition-grid">
            <div className="mobile-activity-nutrition-total">
              <p className="mobile-activity-calorie-amount">
                <span className="sr-only">{nutritionUnavailable ? (nutritionState === "loading" ? "Food data loading" : "Food data unavailable") : nutrition ? `${number.format(nutrition.calories)} kcal consumed` : "Food intake not logged"}, {visibleTargets?.calories != null ? `daily target ${number.format(visibleTargets.calories)} kcal` : "daily target not set"}</span>
                <span aria-hidden="true"><strong>{nutritionUnavailable ? "—" : nutrition ? number.format(nutrition.calories) : "0"}</strong><span className="mobile-activity-calorie-target"> / {visibleTargets?.calories != null ? number.format(visibleTargets.calories) : "—"}</span><span className="mobile-activity-calorie-unit"> kcal</span></span>
              </p>
              <small>{nutritionUnavailable ? (nutritionState === "loading" ? "Loading food data" : "Food data unavailable") : nutrition ? `${nutrition.meals} ${nutrition.meals === 1 ? "meal" : "meals"} logged` : "No meals logged"}</small>
            </div>
            <dl className="mobile-activity-macros" aria-label="Macronutrients, consumed / daily target in grams">
              {macronutrients.map(({ key, label, icon: Icon, tone }) => {
                const value = nutrition?.[key]
                const target = visibleTargets?.[key]
                const hasProgress = value != null && Number.isFinite(value) && value >= 0 && target != null && Number.isFinite(target) && target > 0
                const progress = hasProgress ? Math.min(value / target, 1) * 100 : 0
                return (
                  <div key={key} className="mobile-activity-macro" data-tone={tone}>
                    <dt><Icon aria-hidden="true" />{label}</dt>
                    <dd>
                      <span className="sr-only">{nutritionUnavailable ? (nutritionState === "loading" ? "Food data loading" : "Food data unavailable") : value != null ? `${grams.format(value)} g consumed` : "Not logged"}, {target != null ? `daily target ${grams.format(target)} g` : "daily target not set"}</span>
                      <span className="mobile-activity-macro-amount" aria-hidden="true">{nutritionUnavailable ? "—" : value != null ? grams.format(value) : "0"}<span className="mobile-activity-macro-target">/ {target != null ? grams.format(target) : "—"}</span><span className="mobile-activity-macro-unit">g</span></span>
                    </dd>
                    <dd className="mobile-activity-macro-track" data-available={hasProgress} aria-hidden="true"><span style={{ width: `${progress}%` }} /></dd>
                  </div>
                )
              })}
            </dl>
          </div>
        </div>
        <div id={`${id}-workout`} aria-hidden={activeSummaryView !== "workout"} className="mobile-activity-stat mobile-activity-summary-panel mobile-activity-workout">
          <span className="mobile-activity-stat-label"><Dumbbell aria-hidden="true" />Workout</span>
          <div className="mobile-activity-workout-content">
            <div className="mobile-activity-workout-overview">
              <div className="mobile-activity-workout-detail">
                <p><strong>{workout ? number.format(workout.minutes) : "—"}</strong>{workout && <span>min</span>}</p>
                <span className="mobile-activity-workout-name">{workout ? workout.label : "No workout logged"}</span>
              </div>
              <small className="mobile-activity-workout-sessions">{workout ? workout.minutes === 0 ? "Recovery day" : `${number.format(workout.sessions)} ${workout.sessions === 1 ? "session" : "sessions"} logged` : "No sessions logged"}</small>
            </div>
            <div className="mobile-activity-body-section">
              <div className="mobile-activity-body-heading">
                <span>Body</span>
                <small>{bodyMeasurements ? <>{isSample ? "Sample data · " : "Updated "}<time dateTime={bodyMeasurements.recordedAt}>{measurementDate.format(activityDateFromKey(bodyMeasurements.recordedAt))}</time></> : "No measurements"}</small>
              </div>
              <dl className="mobile-activity-body-metrics" aria-label="Latest body measurements">
                {bodyMetrics.map(({ label, icon: Icon, value, unit }) => {
                  const available = value != null && Number.isFinite(value) && value > 0
                  return (
                    <div key={label}>
                      <dt aria-label={label} title={Icon ? label : undefined} data-icon-label={Boolean(Icon)}>{Icon ? <Icon aria-hidden="true" /> : label}</dt>
                      <dd>{available ? `${measurement.format(value)}${unit ? ` ${unit}` : ""}` : <span aria-label="Not recorded">—</span>}</dd>
                    </div>
                  )
                })}
              </dl>
            </div>
          </div>
        </div>
        {showFinance && <div id={`${id}-finance`} aria-hidden={activeSummaryView !== "finance"} className="mobile-activity-stat mobile-activity-summary-panel mobile-activity-finance">
          <span className="mobile-activity-stat-label"><WalletCards aria-hidden="true" />Finance</span>
          {(!finance || finance.status === "loading") && <div className="mobile-activity-finance-state" aria-busy="true">Loading finance...</div>}
          {finance?.status === "error" && <div className="mobile-activity-finance-state" role="status">Finance is unavailable{onFinanceRetry && <button type="button" className="mobile-activity-finance-retry" aria-label="Retry finance summary" title="Retry finance summary" onClick={onFinanceRetry}><RefreshCw aria-hidden="true" /></button>}</div>}
          {finance?.status === "empty" && <div className="mobile-activity-finance-state">No finance data yet</div>}
          {finance?.status === "ready" && <div className="mobile-activity-finance-content">
            <div className="mobile-activity-finance-total">
              <span>Net assets</span>
              <strong>{formatFinanceAmount(finance.netAssets, finance.currency)}</strong>
            </div>
            <dl className="mobile-activity-finance-metrics" aria-label="Finance summary">
              <div><dt>Income</dt><dd>{formatFinanceAmount(finance.income, finance.currency)}</dd></div>
              <div><dt>Expense</dt><dd>{formatFinanceAmount(finance.expense, finance.currency)}</dd></div>
              <div><dt>Net</dt><dd>{formatFinanceAmount(finance.net, finance.currency)}</dd></div>
            </dl>
          </div>}
        </div>}
          </div>
        </div>
      </div>

    </section>
  )
}
