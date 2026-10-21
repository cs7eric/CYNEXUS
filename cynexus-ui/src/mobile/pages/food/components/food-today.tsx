import { Apple, Beef, CalendarDays, Check, ChevronLeft, ChevronRight, CookingPot, Droplet, Leaf, Moon, Pencil, Plus, Sun, Wheat } from "lucide-react"
import { addDays, formatNutrition, macroKeys, meals, nutrientNames, parseDate, readableDate, sumNutrition } from "@/services/food/model"
import type { DiaryItem, FoodDay, MealType } from "@/services/food/types"
import { FoodScene } from "./food-scene"
import { foodAmount } from "./scene-model"
import "../today.css"

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

export function FoodToday({ day, today, onDateChange, onAdd, onEdit, onSelectGroup, onComplete, busy }: FoodTodayProps) {
  const start = addDays(day.date, -parseDate(day.date).getDay())
  const remaining = day.targets.caloriesKcal != null && day.totals.caloriesKcal != null ? day.targets.caloriesKcal - day.totals.caloriesKcal : null
  const partial = day.missingCounts.caloriesKcal > 0
  return <div className="food-today">
    <div className="food-page-heading"><div><p className="food-eyebrow"><Leaf />Food / daily diary</p><h1>A little <em>balance.</em></h1></div><label className="food-date-picker"><CalendarDays /><span>{readableDate(day.date)}</span><input aria-label="Choose diary date" type="date" value={day.date} max={today} onChange={event => { if (event.target.value && event.target.value <= today) onDateChange(event.target.value) }} /></label></div>
    <div className="food-week-toolbar"><button type="button" onClick={() => onDateChange(addDays(day.date, -7))} aria-label="Previous week" title="Previous week"><ChevronLeft /></button><span>{parseDate(start).toLocaleDateString("en-GB", { month: "long", year: "numeric" })}</span>{day.date !== today && <button type="button" onClick={() => onDateChange(today)} className="food-back-today">Today</button>}<button type="button" disabled={addDays(start, 7) > today} onClick={() => onDateChange(addDays(day.date, 7) > today ? today : addDays(day.date, 7))} aria-label="Next week" title="Next week"><ChevronRight /></button></div>
    <div className="food-week-strip" role="group" aria-label="Select diary date">{Array.from({ length: 7 }, (_, index) => {
      const date = addDays(start, index), value = parseDate(date)
      return <button type="button" key={date} disabled={date > today} aria-pressed={date === day.date} aria-current={date === today ? "date" : undefined} aria-label={readableDate(date, true)} onClick={() => onDateChange(date)}><small>{value.toLocaleDateString("en-US", { weekday: "short" }).slice(0, 1)}</small><strong>{value.getDate()}</strong>{date === today && <i aria-hidden="true" />}</button>
    })}</div>
    <section className="food-table-card" aria-label="Daily nutrition summary and food visualization">
      <div className="food-table-summary"><div><p className="food-intake-title">{day.date === today ? "Today’s intake" : "Daily intake"}{partial && " · Partial"}</p><h2>On your table<span>.</span></h2></div><div className="food-table-energy"><strong>{formatNutrition(partial ? day.knownTotals.caloriesKcal : day.totals.caloriesKcal)}</strong><span>{partial ? "known kcal · " : ""}{day.targets.caloriesKcal == null ? "kcal · No target" : `/ ${formatNutrition(day.targets.caloriesKcal)} kcal`}</span></div></div>
      <FoodScene key={day.date} items={day.items} onSelectGroup={onSelectGroup} onAdd={onAdd} busy={busy} />
      <div className="food-macro-grid">{macroKeys.map(key => {
        const Icon = macroIcons[key], isPartial = day.missingCounts[key] > 0, total = day.totals[key], target = day.targets[key]
        return <div key={key} className={`food-macro food-macro-${key}`}><p><Icon />{nutrientNames[key]}{isPartial && " · Partial"}</p><div><strong>{formatNutrition(isPartial ? day.knownTotals[key] : total, 1)}</strong><span>/ {formatNutrition(target, 1)} g</span></div><div className="food-macro-track" aria-hidden="true"><i style={{ width: `${target != null && target > 0 && total != null ? Math.min(100, total / target * 100) : 0}%` }} /></div></div>
      })}</div>
      <div className="food-daily-balance"><Leaf /><p>{remaining == null ? partial ? "Partial nutrition" : "Target not set" : `${formatNutrition(Math.abs(remaining))} kcal ${remaining >= 0 ? "remaining" : "above target"}`}</p><span>Daily balance</span></div>
    </section>
    <div className="food-diary-heading"><h2>On the menu</h2><span>{day.status === "complete" ? <><Check />Complete</> : day.items.length ? `${day.items.length} foods logged` : "Nothing logged yet"}</span></div>
    <div className="food-meal-list">{meals.map(meal => {
      const items = day.items.filter(item => item.mealType === meal.id), summary = sumNutrition(items), Icon = mealIcons[meal.id]
      return <section key={meal.id} className={`food-meal food-meal-${meal.id}`} aria-label={meal.name}><div className="food-meal-top"><span className="food-meal-symbol"><Icon /></span><h3>{meal.name}</h3><span className="food-meal-total">{items.length ? <>{formatNutrition(summary.knownTotals.caloriesKcal)} <small>kcal{summary.missingCounts.caloriesKcal > 0 && " · Partial"}</small></> : <small>Not logged</small>}</span><button type="button" className="food-meal-add" disabled={busy} onClick={() => onAdd(meal.id)} aria-label={`Add to ${meal.name.toLowerCase()}`} title={`Add to ${meal.name.toLowerCase()}`}><Plus /></button></div>{items.length ? items.map(item => <button type="button" className="food-diary-row" key={item.id} disabled={busy} onClick={() => onEdit(item)}><span className="food-diary-row-text"><strong>{item.foodName}</strong><small>{foodAmount([item])}{item.unit && item.unit !== "g" && item.consumedGrams != null ? ` · ${formatNutrition(item.consumedGrams, 1)} g` : ""}</small></span><span className="food-diary-row-energy">{formatNutrition(item.nutrients.caloriesKcal)}<span className="food-sr-only"> kcal</span></span><ChevronRight /></button>) : <p className="food-meal-empty">Something good is still to come.</p>}</section>
    })}</div>
    <button type="button" className="food-complete-day" disabled={busy} onClick={onComplete}>{day.status === "complete" ? <Pencil /> : <Check />}{day.status === "complete" ? "Reopen this day" : "Finish this day"}</button>
  </div>
}

export default FoodToday
