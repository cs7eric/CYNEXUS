import { CalendarDays, ChartNoAxesColumnIncreasing, Check, ChevronLeft, ChevronRight, Leaf, LoaderCircle, Save, Target } from "lucide-react"
import { useState, type CSSProperties, type FormEvent } from "react"
import { Area, AreaChart, CartesianGrid, XAxis } from "recharts"
import { addDays, dateKey, emptyNutrition, formatNutrition, historyBounds, macroKeys, nutrientKeys, nutrientNames, parseDate, readableDate, type HistoryPeriod } from "@/services/food/model"
import type { FoodDay, FoodGoal, NutrientKey, Nutrients } from "@/services/food/types"
import { ChartContainer, ChartLegend, ChartLegendContent, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart"
import { FoodArt } from "./food-art"
import { foodAmount, groupFoods } from "./scene-model"
import "../insights.css"
import "../insights-compact.css"

interface FoodHistoryProps {
  days: FoodDay[]
  anchor: string
  period: HistoryPeriod
  onAnchorChange: (date: string) => void
  onPeriodChange: (period: HistoryPeriod) => void
  onSelectDate: (date: string) => void
}

function average(days: FoodDay[], key: NutrientKey) {
  const known = days.filter(day => day.knownTotals[key] != null)
  return { value: !known.length ? null : known.reduce((sum, day) => sum + day.knownTotals[key]!, 0) / known.length, partial: days.some(day => day.missingCounts[key] > 0 || day.totals[key] == null) }
}

function shiftedAnchor(anchor: string, period: HistoryPeriod, direction: number) {
  const current = parseDate(anchor)
  if (period === "week") return addDays(anchor, direction * 7)
  return dateKey(period === "month" ? new Date(current.getFullYear(), current.getMonth() + direction, 1) : new Date(current.getFullYear() + direction, current.getMonth(), 1))
}

const foodHistoryChartConfig = {
  kcal: { label: "Energy", color: "#378da2" },
  target: { label: "Daily target", color: "#6d9a83" },
} satisfies ChartConfig

const historyFoodSpots = [
  { x: 42, y: 28, angle: -12 }, { x: 31, y: 38, angle: 8 }, { x: 53, y: 14, angle: 11 },
  { x: 36, y: 48, angle: 5 }, { x: 48, y: 8, angle: -9 }, { x: 59, y: 42, angle: 4 },
]

function chartTarget(days: FoodDay[]) {
  const known = days.filter(day => day.targets.caloriesKcal != null)
  return known.length ? known.reduce((sum, day) => sum + day.targets.caloriesKcal!, 0) / known.length : null
}

export function FoodHistory({ days, anchor, period, onAnchorChange, onPeriodChange, onSelectDate }: FoodHistoryProps) {
  const [expandedDate, setExpandedDate] = useState<string | null>(null)
  const today = dateKey(new Date())
  const bounds = historyBounds(anchor, period)
  const logged = days.filter(day => day.date >= bounds.start && day.date <= bounds.end && day.date <= today && (day.items.length > 0 || day.status === "complete")).sort((a, b) => a.date.localeCompare(b.date))
  const kcal = average(logged, "caloriesKcal")
  const byDate = new Map(logged.map(day => [day.date, day]))
  const bars: { date: string; label: string; fullLabel: string; days: FoodDay[] }[] = []
  if (period === "year") {
    for (let month = 0; month < 12; month++) {
      const date = dateKey(new Date(parseDate(anchor).getFullYear(), month, 1))
      bars.push({ date, label: parseDate(date).toLocaleDateString("en-GB", { month: "short" }).slice(0, 1), fullLabel: parseDate(date).toLocaleDateString("en-GB", { month: "long", year: "numeric" }), days: logged.filter(day => day.date.slice(0, 7) === date.slice(0, 7)) })
    }
  } else {
    for (let date = bounds.start; date <= bounds.end; date = addDays(date, 1)) {
      bars.push({ date, label: period === "week" ? parseDate(date).toLocaleDateString("en-GB", { weekday: "short" }).slice(0, 1) : String(parseDate(date).getDate()), fullLabel: readableDate(date, true), days: byDate.has(date) ? [byDate.get(date)!] : [] })
    }
  }
  const values = bars.map(bar => average(bar.days, "caloriesKcal"))
  const chartData = bars.map((bar, index) => ({
    date: bar.date,
    label: bar.label,
    fullLabel: bar.fullLabel,
    kcal: values[index].value,
    target: chartTarget(bar.days),
    partial: values[index].partial,
    available: bar.days.length > 0,
  }))
  const hasTarget = chartData.some(item => item.target != null)
  const next = shiftedAnchor(anchor, period, 1)
  const rangeLabel = period === "year" ? String(parseDate(anchor).getFullYear()) : period === "month" ? parseDate(anchor).toLocaleDateString("en-GB", { month: "long", year: "numeric" }) : `${readableDate(bounds.start)} – ${readableDate(bounds.end)}, ${parseDate(bounds.end).getFullYear()}`
  return <div className="food-insights food-history">
    <div className="food-insight-heading"><p><ChartNoAxesColumnIncreasing />A little perspective</p><h1>Your daily<br /><em>rhythm.</em></h1></div>
    <div className="food-insight-tabs" role="group" aria-label="History period">{(["week", "month", "year"] as const).map(value => <button type="button" key={value} aria-pressed={period === value} onClick={() => onPeriodChange(value)}>{value[0].toUpperCase() + value.slice(1)}</button>)}</div>
    <div className="food-history-range"><button type="button" title={`Previous ${period}`} aria-label={`Previous ${period}`} onClick={() => onAnchorChange(shiftedAnchor(anchor, period, -1))}><ChevronLeft /></button><span aria-live="polite">{rangeLabel}</span><button type="button" title={`Next ${period}`} aria-label={`Next ${period}`} disabled={historyBounds(next, period).start > today} onClick={() => onAnchorChange(next > today ? today : next)}><ChevronRight /></button></div>
    <div className="food-history-jump"><label><CalendarDays /><span>Choose date</span><input type="date" max={today} value={anchor} aria-label="Choose history date" onChange={event => { if (event.target.value && event.target.value <= today) onAnchorChange(event.target.value) }} /></label>{anchor !== today && <button type="button" onClick={() => onAnchorChange(today)}>Back to today</button>}</div>
    <section className="food-history-summary" aria-label="Recorded nutrition averages"><div className="food-history-key-stat"><span>Daily average{kcal.partial && " · Partial"}</span><strong>{formatNutrition(kcal.value)} <small>{kcal.partial ? "known kcal" : "kcal"}</small></strong><p>{logged.length} {logged.length === 1 ? "day" : "days"} logged · {logged.filter(day => day.status === "complete").length} complete</p></div>
      <div className="food-history-macros">{macroKeys.map(key => { const value = average(logged, key); return <div key={key}><span>{nutrientNames[key]}</span><strong>{formatNutrition(value.value, 1)} <small>g</small></strong><small>{value.partial ? "Known average · Partial" : "Daily average"}</small></div> })}</div>
      <div className="food-history-chart-wrap">
        {chartData.some(item => item.kcal != null) ? <ChartContainer config={foodHistoryChartConfig} className="food-history-chart h-[190px] w-full aspect-auto" initialDimension={{ width: 320, height: 190 }}>
          <AreaChart data={chartData} margin={{ top: 10, right: 4, left: 4, bottom: 0 }} onClick={(state) => {
            const index = Number(state.activeTooltipIndex)
            const point = Number.isInteger(index) ? chartData[index] : undefined
            if (!point?.available) return
            if (period === "year") { onPeriodChange("month"); onAnchorChange(point.date) } else onSelectDate(point.date)
          }}>
            <defs>
              <linearGradient id="foodHistoryEnergyFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="var(--color-kcal)" stopOpacity={0.34} />
                <stop offset="95%" stopColor="var(--color-kcal)" stopOpacity={0.03} />
              </linearGradient>
            </defs>
            <CartesianGrid vertical={false} />
            <XAxis dataKey="label" tickLine={false} axisLine={false} tickMargin={8} minTickGap={period === "month" ? 12 : 8} />
            <ChartLegend content={<ChartLegendContent className="gap-3 text-[9px]" />} />
            <ChartTooltip cursor={false} content={<ChartTooltipContent labelFormatter={(_, payload) => String(payload?.[0]?.payload?.fullLabel ?? "")} formatter={(value, name, item) => [
              value == null ? "No data" : `${formatNutrition(Number(value))} kcal`,
              item?.payload?.partial && name === "kcal" ? "Known energy · Partial" : name === "target" ? "Daily target" : "Energy",
            ]} />} />
            <Area dataKey="kcal" type="natural" stroke="var(--color-kcal)" strokeWidth={2.5} fill="url(#foodHistoryEnergyFill)" connectNulls={false} activeDot={{ r: 4, fill: "var(--color-kcal)", stroke: "#fff", strokeWidth: 2 }} />
            {hasTarget && <Area dataKey="target" type="monotone" stroke="var(--color-target)" strokeWidth={1.5} strokeDasharray="4 4" fill="none" dot={false} connectNulls={false} />}
          </AreaChart>
        </ChartContainer> : <div className="food-history-chart-empty">No nutrition values in this period.</div>}
      </div>
      <div className="food-trend-caption"><span>{period === "year" ? "Monthly average · kcal" : "Daily intake · kcal"}</span><span>{period === "year" ? "Tap a month to explore" : "Tap a day to open diary"}</span></div>
    </section>
    <p className="food-insight-note"><Leaf />Averages use available nutrition on logged days. Partial values include known nutrition; missing values are excluded, not zero. Future and unlogged days are excluded.</p>
    <div className="food-history-list-heading"><h2>Your days</h2><span>{logged.length} recorded</span></div>
    {!logged.length ? <div className="food-insight-empty"><CalendarDays /><h3>A little space for a fresh start.</h3><p>No food diary entries in this period.</p><button type="button" onClick={() => onSelectDate(anchor)}>Open this day<ChevronRight /></button></div> : [...logged].reverse().map(day => {
      const isExpanded = expandedDate === day.date
      const foods = isExpanded ? groupFoods(day.items).map((group, index) => {
        const food = group.entries[0]!
        const knownEnergy = group.entries.filter(item => item.nutrients.caloriesKcal != null)
        const totalEnergy = knownEnergy.reduce((sum, item) => sum + item.nutrients.caloriesKcal!, 0)
        const spot = historyFoodSpots[index % historyFoodSpots.length]
        const cycle = Math.floor(index / historyFoodSpots.length)
        return {
          group,
          food,
          meals: [...new Set(group.entries.map(item => item.mealType))].map(meal => meal[0].toUpperCase() + meal.slice(1)).join(" + "),
          amount: foodAmount(group.entries),
          energy: knownEnergy.length === group.entries.length
            ? `${formatNutrition(totalEnergy)} kcal`
            : knownEnergy.length ? `${formatNutrition(totalEnergy)} known kcal` : "Energy unavailable",
          style: {
            "--food-left": `${spot.x + cycle}%`,
            "--food-top": `${spot.y + cycle}%`,
            "--food-angle": `${spot.angle + cycle * 2}deg`,
            "--food-layer": String(index + 1),
            "--food-scale": String(Math.max(0.72, 1 - cycle * 0.08)),
          } as CSSProperties,
        }
      }) : []
      const dateLabel = parseDate(day.date)
      return <div key={day.date} className="food-history-day-wrap">
        <button type="button" className="food-history-day" aria-expanded={isExpanded} aria-controls={`food-history-day-${day.date}`} onClick={() => setExpandedDate(current => current === day.date ? null : day.date)}>
          <span className="food-history-day-date"><strong>{dateLabel.getDate()}</strong><small>{dateLabel.toLocaleDateString("en-GB", { month: "short" })}</small></span>
          <span className="food-history-day-description"><strong>{dateLabel.toLocaleDateString("en-GB", { weekday: "long" })}</strong><small>{day.items.length} {day.items.length === 1 ? "food" : "foods"} · {day.status === "complete" ? "Complete" : "Open"}{day.missingCounts.caloriesKcal > 0 && " · Partial"}</small></span>
          <span className="food-history-day-energy">{formatNutrition(day.missingCounts.caloriesKcal ? day.knownTotals.caloriesKcal : day.totals.caloriesKcal)}<small>{day.missingCounts.caloriesKcal ? "known kcal" : "kcal"}</small></span>
          <ChevronRight aria-hidden="true" />
        </button>
        <div id={`food-history-day-${day.date}`} className="food-history-day-details" role="region" aria-label={`${dateLabel.toLocaleDateString("en-GB", { weekday: "long", day: "numeric", month: "long" })} foods`} hidden={!isExpanded}>
          {isExpanded && (foods.length ? <>
            <div className="food-history-gathering" aria-hidden="true">
              <div className="food-history-gathered-items">
                {foods.map(({ group, food, style }) => <span className="food-history-gathered-item" key={group.key} style={style}>
                  <FoodArt food={food} className="food-history-gathered-art" />
                  {group.entries.length > 1 && <small>×{group.entries.length}</small>}
                </span>)}
              </div>
            </div>
            <div className="food-history-food-summary" role="list" aria-label="Food items and combined amounts">
              {foods.map(({ group, food, meals, amount, energy }) => <div className="food-history-food-summary-item" role="listitem" key={group.key}>
                <strong title={food.foodName}>{food.foodName}</strong>
                <small>{meals} · {amount}{group.entries.length > 1 ? ` · ${group.entries.length} entries` : ""} · {energy}</small>
              </div>)}
            </div>
          </> : <p className="food-history-no-items">No food entries for this day.</p>)}
        </div>
      </div>
    })}
  </div>
}

interface FoodGoalsProps { goals: FoodGoal[]; today: string; onSave: (effectiveFrom: string, nutrients: Nutrients) => Promise<void> }
const unitFor = (key: NutrientKey) => key === "caloriesKcal" ? "kcal" : "g"
const goalFields = (values: Nutrients) => Object.fromEntries(nutrientKeys.map(key => [key, values[key] == null ? "" : String(values[key])])) as Record<NutrientKey, string>

export function FoodGoals({ goals, today, onSave }: FoodGoalsProps) {
  const sorted = [...goals].sort((a, b) => b.effectiveFrom.localeCompare(a.effectiveFrom))
  const active = sorted.find(goal => goal.effectiveFrom <= today)
  const [effectiveFrom, setEffectiveFrom] = useState(today)
  const [fields, setFields] = useState(() => goalFields(active?.nutrients ?? emptyNutrition()))
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (saving) return
    setError(null); setSaved(false)
    if (!/^\d{4}-\d{2}-\d{2}$/.test(effectiveFrom) || !Number.isFinite(parseDate(effectiveFrom).getTime()) || dateKey(parseDate(effectiveFrom)) !== effectiveFrom) { setError("Choose a valid effective date."); return }
    const nutrients = emptyNutrition()
    for (const key of nutrientKeys) {
      if (!fields[key].trim()) continue
      const value = Number(fields[key])
      if (!Number.isFinite(value) || value < 0) { setError(`${nutrientNames[key]} must be a non-negative number, or leave it blank.`); return }
      nutrients[key] = value
    }
    setSaving(true)
    try { await onSave(effectiveFrom, nutrients); setSaved(true) }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Could not save your targets. Please try again.") }
    finally { setSaving(false) }
  }
  return <div className="food-insights food-goals"><div className="food-insight-heading"><p><Target />A little intention</p><h1>Your pace,<br /><em>your goals.</em></h1></div>
    <form className="food-goal-form" onSubmit={save}><h2>Daily targets</h2><p>Set the numbers that work for you.</p><label className="food-goal-date-label" htmlFor="food-goal-effective">Effective from</label><input id="food-goal-effective" type="date" required value={effectiveFrom} disabled={saving} onChange={event => { setEffectiveFrom(event.target.value); setSaved(false) }} />
      {nutrientKeys.map(key => <label className="food-goal-field" key={key} htmlFor={`food-goal-${key}`}><span>{nutrientNames[key]}</span><span><input id={`food-goal-${key}`} type="number" inputMode="decimal" min="0" step="any" placeholder="Not set" value={fields[key]} disabled={saving} onChange={event => { setFields(values => ({ ...values, [key]: event.target.value })); setSaved(false) }} aria-describedby="food-goal-help" /><small>{unitFor(key)}</small></span></label>)}
      <p id="food-goal-help" className="food-goal-help">Leave a field blank for no target. Zero is a saved target. Changes apply from the effective date; earlier diary targets stay unchanged.</p>{error && <p className="food-goal-error" role="alert">{error}</p>}{saved && <p className="food-goal-success" role="status"><Check />Targets saved.</p>}<button type="submit" className="food-goal-save" disabled={saving}>{saving ? <LoaderCircle className="food-insight-spinner" /> : <Save />}{saving ? "Saving…" : "Save targets"}</button>
    </form>
    <p className="food-insight-note"><Leaf />Targets are personal reference values. Nutrition totals always come from your recorded foods.</p>
    <div className="food-history-list-heading"><h2>Target history</h2><span>{sorted.length} {sorted.length === 1 ? "version" : "versions"}</span></div>
    {!sorted.length ? <p className="food-goal-no-history">No targets set yet. Your first saved targets will appear here.</p> : sorted.map(goal => <div className="food-goal-history-row" key={goal.id}><div><strong>From {readableDate(goal.effectiveFrom)} {parseDate(goal.effectiveFrom).getFullYear()}</strong><span>{goal.id === active?.id ? "Current" : goal.effectiveFrom > today ? "Scheduled" : "Previous"}</span></div><dl>{nutrientKeys.map(key => <div key={key}><dt>{nutrientNames[key]}</dt><dd>{formatNutrition(goal.nutrients[key], 1)} <small>{unitFor(key)}</small></dd></div>)}</dl></div>)}
  </div>
}
