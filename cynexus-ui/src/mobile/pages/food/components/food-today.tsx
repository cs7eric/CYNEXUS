import { Apple, Beef, Check, CookingPot, Droplet, Leaf, Moon, Pencil, Plus, Sun, Wheat } from "lucide-react"
import { Fragment, useRef, useState, type CSSProperties, type KeyboardEvent, type TouchEvent } from "react"
import { addDays, formatNutrition, macroKeys, meals, nutrientNames, parseDate, sumNutrition } from "@/services/food/model"
import type { DiaryItem, FoodDay, MealType } from "@/services/food/types"
import { FoodArt } from "./food-art"
import { foodAmount } from "./scene-model"
import "../today.css"
import "../today-redesign.css"

export interface FoodTodayProps {
  day: FoodDay
  today: string
  onDateChange: (date: string) => void
  onAdd: (meal: MealType) => void
  onEdit: (item: DiaryItem) => void
  onSelectGroup: (items: DiaryItem[]) => void
  onComplete: () => void
  busy: boolean
}
const mealIcons = { breakfast: Sun, lunch: CookingPot, dinner: Moon, snack: Apple }
const macroIcons = { carbsG: Wheat, proteinG: Beef, fatG: Droplet, caloriesKcal: Leaf }

export function FoodToday(props: FoodTodayProps) {
  return <FoodTodayContent key={props.day.date} {...props} />
}

function FoodTodayContent({ day, today, onDateChange, onAdd, onEdit, onSelectGroup, onComplete, busy }: FoodTodayProps) {
  const [snackRevealed, setSnackRevealed] = useState(false)
  const snackSwipeStart = useRef<{ x: number; y: number } | null>(null)
  const daySwipeStart = useRef<{ x: number; y: number } | null>(null)
  const remaining = day.targets.caloriesKcal != null && day.totals.caloriesKcal != null ? day.targets.caloriesKcal - day.totals.caloriesKcal : null
  const partial = day.missingCounts.caloriesKcal > 0
  const dateValue = parseDate(day.date)
  const dateLabel = `${String(dateValue.getDate()).padStart(2, "0")}-${dateValue.toLocaleDateString("en-US", { month: "short" })}-${dateValue.getFullYear()}`
  const calories = partial ? day.knownTotals.caloriesKcal : day.totals.caloriesKcal
  const calorieTarget = day.targets.caloriesKcal
  const calorieProgress = calorieTarget && calorieTarget > 0 && calories != null ? Math.min(100, calories / calorieTarget * 100) : 0
  const remainingCalories = remaining == null ? null : Math.max(0, remaining)
  function handleSnackSwipeEnd(event: TouchEvent<HTMLDivElement>) {
    const start = snackSwipeStart.current
    snackSwipeStart.current = null
    if (!start) return
    const touch = event.changedTouches[0]
    if (!touch) return
    const dx = touch.clientX - start.x
    const dy = touch.clientY - start.y
    if (Math.abs(dx) < 32 || Math.abs(dx) < Math.abs(dy)) return
    setSnackRevealed(dx < 0)
  }

  function startDiarySwipe(event: TouchEvent<HTMLDivElement>) {
    const target = event.target
    if (event.touches.length !== 1 || (target instanceof Element && target.closest("button, a, input, textarea, select, [role='button'], .food-meal-pair"))) {
      daySwipeStart.current = null
      return
    }
    const touch = event.touches[0]
    daySwipeStart.current = touch ? { x: touch.clientX, y: touch.clientY } : null
  }

  function handleDiarySwipeEnd(event: TouchEvent<HTMLDivElement>) {
    const start = daySwipeStart.current
    daySwipeStart.current = null
    if (!start) return
    const touch = event.changedTouches[0]
    if (!touch) return
    const dx = touch.clientX - start.x
    const dy = touch.clientY - start.y
    if (Math.abs(dx) < 48 || Math.abs(dx) <= Math.abs(dy)) return
    if (dx < 0) {
      if (day.date < today) onDateChange(addDays(day.date, 1))
    } else {
      onDateChange(addDays(day.date, -1))
    }
  }

  function handleSnackSwipeKey(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === "ArrowLeft") {
      event.preventDefault()
      setSnackRevealed(true)
    } else if (event.key === "ArrowRight" || event.key === "Escape") {
      event.preventDefault()
      setSnackRevealed(false)
    }
  }
  function mealCard(meal: typeof meals[number], index: number, compact = false) {
    const items = day.items.filter(item => item.mealType === meal.id), summary = sumNutrition(items), Icon = mealIcons[meal.id]
    return <article className={`food-meal-card food-meal-${meal.id}${compact ? " food-meal-card-compact" : ""}`} aria-label={meal.name}>
      {!compact && <div className="food-meal-rail"><span className="food-meal-symbol"><Icon /></span>{index < meals.length - 1 && <i aria-hidden="true" />}</div>}
        <div className="food-meal-card-body">
          <div className="food-meal-card-head"><h2>{meal.name}</h2><span>{items.length ? <>{formatNutrition(summary.knownTotals.caloriesKcal)} <small>kcal</small></> : <><strong>0</strong> <small>kcal</small></>}</span></div>
        <div className={`food-meal-foods${items.length === 0 ? " is-empty" : ""}`}>{items.slice(0, compact ? 1 : 4).map(item => <button type="button" className="food-food-item" key={item.id} disabled={busy} onClick={() => items.length === 1 ? onEdit(item) : onSelectGroup(items)} aria-label={`View ${item.foodName}`}><FoodArt food={item} /><span className="food-food-item-label"><strong>{foodAmount([item])}</strong><small>{formatNutrition(item.nutrients.caloriesKcal)} kcal</small></span><span className="food-sr-only">{item.foodName}, {foodAmount([item])}, {formatNutrition(item.nutrients.caloriesKcal)} kcal</span></button>)}{Array.from({ length: Math.max(0, (compact ? 1 : 4) - items.length) }, (_, slot) => <button type="button" className="food-food-add" key={`add-${slot}`} disabled={busy} onClick={() => onAdd(meal.id)} aria-label={`Add food to ${meal.name.toLowerCase()}`}><Plus /></button>)}{items.length > (compact ? 1 : 4) && <button type="button" className="food-food-more" disabled={busy} onClick={() => onSelectGroup(items)} aria-label={`View all ${meal.name.toLowerCase()} foods`}>+{items.length - (compact ? 1 : 4)}</button>}</div>
      </div>
    </article>
  }
  return <div className="food-today food-today-v2" onTouchStart={startDiarySwipe} onTouchEnd={handleDiarySwipeEnd}>
    <div className="food-today-section-title"><div><p className="food-today-kicker"><Leaf />Nutrition diary</p><h1>Today's log</h1></div><label className="food-date-picker food-today-date"><span>{dateLabel}</span><input aria-label="Choose diary date" type="date" value={day.date} max={today} onChange={event => { if (event.target.value && event.target.value <= today) onDateChange(event.target.value) }} /></label></div>
    <div className="food-daily-summary" aria-label="Daily nutrition summary"><section className="food-calorie-card"><div className="food-energy-topline"><p className="food-energy-kicker"><Leaf />Still to eat{partial && " · Partial"}</p><div className="food-energy-target"><small>Budget</small><b>{calorieTarget == null ? "No target" : `${formatNutrition(calorieTarget)} kcal`}</b></div></div><div className="food-energy-main"><strong>{formatNutrition(remainingCalories)}</strong><span>kcal</span></div><div className="food-energy-lower"><div className="food-energy-stats"><span><span className="food-energy-stat-value"><strong>{formatNutrition(calories)}</strong><em>kcal</em></span><small>Intake</small></span><span><span className="food-energy-stat-value"><strong>—</strong><em>kcal</em></span><small>Burned</small></span></div><div className="food-energy-meter" role={calorieTarget == null ? undefined : "progressbar"} aria-label="Daily calorie progress" aria-valuemin={calorieTarget == null ? undefined : 0} aria-valuemax={calorieTarget == null ? undefined : 100} aria-valuenow={calorieTarget == null ? undefined : calorieProgress} style={{ "--food-meter-marker": `${calorieProgress}%` } as CSSProperties}><div className="food-energy-meter-segments" aria-hidden="true">{Array.from({ length: 26 }, (_, index) => <i key={index} className={index < 13 ? "is-logged" : index < 21 ? "is-budget" : "is-limit"} />)}<b /></div></div></div></section><section className="food-macro-card" aria-label="Macronutrients"><div className="food-macro-strip">{macroKeys.map(key => { const Icon = macroIcons[key], isPartial = day.missingCounts[key] > 0, total = isPartial ? day.knownTotals[key] : day.totals[key], target = day.targets[key], progress = target != null && target > 0 && total != null ? Math.min(100, total / target * 100) : 0; return <div key={key} className={`food-macro-ring food-macro-ring-${key}`}><div className="food-macro-ring-visual" style={{ "--food-progress": `${progress}%` } as CSSProperties}><span>{formatNutrition(total, 1)}</span></div><div><p><Icon />{nutrientNames[key]}</p><small>{target == null ? "No target" : `of ${formatNutrition(target, 1)} g`}</small></div></div> })}</div></section></div>
    <div className="food-meal-list food-meal-timeline">{meals.slice(0, 2).map((meal, index) => <Fragment key={meal.id}>{mealCard(meal, index)}</Fragment>)}<div className={`food-meal-pair${snackRevealed ? " is-snack-revealed" : ""}`} key="dinner-snacks" role="group" tabIndex={0} aria-expanded={snackRevealed} aria-label="Dinner and snacks. Swipe left to view snacks." onTouchStart={event => { const touch = event.touches[0]; if (touch) snackSwipeStart.current = { x: touch.clientX, y: touch.clientY } }} onTouchEnd={handleSnackSwipeEnd} onKeyDown={handleSnackSwipeKey}><div className="food-meal-swipe-track">{mealCard(meals[2], 2)}<div className="food-meal-snack-slide">{mealCard(meals[3], 3, true)}</div></div></div></div>
    <button type="button" className="food-complete-day" disabled={busy} onClick={onComplete}>{day.status === "complete" ? <Pencil /> : <Check />}{day.status === "complete" ? "Reopen this day" : "Finish this day"}</button>
  </div>
}

export default FoodToday
