import { Beef, Droplet, Dumbbell, Utensils, Wheat } from "lucide-react"
import { useId, useRef, useState, type KeyboardEvent } from "react"

import { MobileButton } from "@/mobile/ui/button"
import { activityCalendarDays, activityDateFromKey, activityDateKey, shiftActivityDate, type DailyActivity } from "./model"
import "./styles.css"

const weekdays = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]
const shortDate = new Intl.DateTimeFormat("en-US", { month: "short", day: "numeric" })
const fullDate = new Intl.DateTimeFormat("en-US", { weekday: "long", year: "numeric", month: "long", day: "numeric" })
const compactNumber = new Intl.NumberFormat("en-US", { notation: "compact", maximumFractionDigits: 1 })
const number = new Intl.NumberFormat("en-US")
const grams = new Intl.NumberFormat("en-US", { maximumFractionDigits: 1 })
const macronutrients = [
  { key: "carbsGrams", label: "Carbs", icon: Wheat, tone: "carbs" },
  { key: "proteinGrams", label: "Protein", icon: Beef, tone: "protein" },
  { key: "fatGrams", label: "Fat", icon: Droplet, tone: "fat" },
] as const

export function MobileActivityCalendar({ today, records, isSample = false, showWeek = true }: { today: Date; records: readonly DailyActivity[]; isSample?: boolean; showWeek?: boolean }) {
  const id = useId()
  const todayKey = activityDateKey(today)
  const [selectedKey, setSelectedKey] = useState(todayKey)
  const buttons = useRef(new Map<string, HTMLButtonElement>())
  const selected = activityDateFromKey(selectedKey)
  const days = activityCalendarDays(today, selected, false)
  const tabStop = days.find((day) => day.available && day.key === selectedKey)?.key ?? days.find((day) => day.available)?.key
  const entries = new Map(records.map((entry) => [entry.date, entry]))
  const activity = entries.get(selectedKey)
  const nutrition = activity?.nutrition
  const targets = activity?.nutritionTargets
  const workout = activity?.workout
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
        <div className="mobile-activity-stat mobile-activity-nutrition">
          <span className="mobile-activity-stat-label"><Utensils aria-hidden="true" />Food intake</span>
          <p className="mobile-activity-calorie-amount">
            <span className="sr-only">{nutrition ? `${number.format(nutrition.calories)} kcal consumed` : "Food intake not logged"}, {targets?.calories != null ? `daily target ${number.format(targets.calories)} kcal` : "daily target not set"}</span>
            <span aria-hidden="true"><strong>{nutrition ? number.format(nutrition.calories) : "—"}</strong><span className="mobile-activity-calorie-target"> / {targets?.calories != null ? number.format(targets.calories) : "—"}</span><span className="mobile-activity-calorie-unit"> kcal</span></span>
          </p>
          <small>{nutrition ? `${nutrition.meals} ${nutrition.meals === 1 ? "meal" : "meals"} logged` : "No meals logged"}</small>
          <dl className="mobile-activity-macros" aria-label="Macronutrients, consumed / daily target in grams">
            {macronutrients.map(({ key, label, icon: Icon, tone }) => {
              const value = nutrition?.[key]
              const target = targets?.[key]
              const hasProgress = value != null && Number.isFinite(value) && value >= 0 && target != null && Number.isFinite(target) && target > 0
              const progress = hasProgress ? Math.min(value / target, 1) * 100 : 0
              return (
                <div key={key} className="mobile-activity-macro" data-tone={tone}>
                  <dt><Icon aria-hidden="true" />{label}</dt>
                  <dd>
                    <span className="sr-only">{value != null ? `${grams.format(value)} g consumed` : "Not logged"}, {target != null ? `daily target ${grams.format(target)} g` : "daily target not set"}</span>
                    <span className="mobile-activity-macro-amount" aria-hidden="true">{value != null ? grams.format(value) : "—"}<span className="mobile-activity-macro-target">/ {target != null ? grams.format(target) : "—"}</span><span className="mobile-activity-macro-unit">g</span></span>
                  </dd>
                  <dd className="mobile-activity-macro-track" data-available={hasProgress} aria-hidden="true"><span style={{ width: `${progress}%` }} /></dd>
                </div>
              )
            })}
          </dl>
        </div>
        <div className="mobile-activity-stat mobile-activity-workout">
          <span className="mobile-activity-stat-label"><Dumbbell aria-hidden="true" />Workout</span>
          <div className="mobile-activity-workout-detail">
            <p><strong>{workout ? number.format(workout.minutes) : "—"}</strong>{workout && <span>min</span>}</p>
            <span className="mobile-activity-workout-name">{workout ? workout.label : "No workout logged"}</span>
          </div>
          <small className="mobile-activity-workout-sessions">{workout ? workout.minutes === 0 ? "Recovery day" : `${number.format(workout.sessions)} ${workout.sessions === 1 ? "session" : "sessions"} logged` : "No sessions logged"}</small>
        </div>
      </div>

    </section>
  )
}
