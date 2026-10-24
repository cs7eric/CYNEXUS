import { ChevronRight, Leaf, RefreshCw } from "lucide-react"
import { useEffect, useMemo, useRef, useState } from "react"
import { useLocation, useNavigate } from "react-router-dom"
import { useNutritionConfig } from "@/mobile/hooks/use-nutrition-config"
import { useWorkoutRecords } from "@/mobile/hooks/use-workout-records"
import { announceFoodChange, createGoal, setDayStatus } from "@/services/food/api"
import { fetchFridgeInventory } from "@/services/fridge/api"
import { dateKey, defaultMealType, historyBounds, readableDate, sumNutrition, type HistoryPeriod } from "@/services/food/model"
import type { DiaryItem, Food, MealType, Nutrients } from "@/services/food/types"
import { calculateNutritionTargets } from "@/services/food/nutrition-config"
import { latestObservation } from "@/services/workout/model"
import { FoodCatalog } from "./components/food-catalog"
import { CustomFoodForm, FoodEntry, NutritionPreview } from "./components/food-entry"
import { FoodHistory, FoodGoals } from "./components/food-insights"
import { FoodSheet } from "./components/food-sheet"
import { FoodToday } from "./components/food-today"
import { foodAmount } from "./components/scene-model"
import { fridgeItemAsFood, useFoodDay, useFoodGoals, useFoodHistory } from "./hooks/use-food-data"
import { useFridgeRecords } from "@/mobile/hooks/use-fridge-records"
import MobileFridgePage from "../fridge"
import "./food.css"

type Sheet = { type: "search"; meal: MealType } | { type: "entry"; meal: MealType; food?: Food; item?: DiaryItem } | { type: "custom"; meal: MealType } | { type: "group"; items: DiaryItem[] } | { type: "reopen"; next: Sheet | null } | { type: "complete" }

function FoodHistoryPage({ today, onSelectDate }: { today: string; onSelectDate: (date: string) => void }) {
  const [anchor, setAnchor] = useState(today), [period, setPeriod] = useState<HistoryPeriod>("week")
  const bounds = historyBounds(anchor, period), result = useFoodHistory(bounds.start, bounds.end)
  return <><RemoteFeedback {...result} />{result.data && <FoodHistory days={result.data} anchor={anchor} period={period} onAnchorChange={setAnchor} onPeriodChange={setPeriod} onSelectDate={onSelectDate} />}</>
}
function FoodGoalsPage({ today }: { today: string }) {
  const result = useFoodGoals()
  const request = useRef({ draft: "", key: crypto.randomUUID() })
  async function save(effectiveFrom: string, nutrients: Nutrients) {
    const draft = JSON.stringify({ effectiveFrom, nutrients })
    if (request.current.draft !== draft) request.current = { draft, key: crypto.randomUUID() }
    await createGoal({ effectiveFrom, nutrients, idempotencyKey: request.current.key }); announceFoodChange()
  }
  return <><RemoteFeedback {...result} />{result.data && <FoodGoals goals={result.data} today={today} onSave={save} />}</>
}
function RemoteFeedback({ loading, error, data, reload }: { loading: boolean; error: string | null; data: unknown; reload: () => void }) {
  if (error) return <div className="food-feedback food-load-error" role="alert"><Leaf /><h2>Food couldn’t load.</h2><p>{error}</p><button type="button" className="food-text-button" onClick={reload}><RefreshCw />Try again</button></div>
  if (loading) return <div className={`food-feedback${data ? " food-refreshing" : " food-loading"}`} role="status"><Leaf /><span>{data ? "Updating your diary…" : "Setting the table…"}</span></div>
  return null
}

function FoodDiaryPage({ today, date, setDate }: { today: string; date: string; setDate: (date: string) => void }) {
  const location = useLocation(), navigate = useNavigate()
  const view = location.hash.replace("#food-", "") || "today"
  const day = useFoodDay(date)
  const { data: workoutRecords } = useWorkoutRecords()
  const nutrition = useNutritionConfig()
  const nutritionConfig = nutrition.config
  const latestWeight = useMemo(() => workoutRecords.isSample ? null : latestObservation(workoutRecords.observations, "weight", null, date), [date, workoutRecords.isSample, workoutRecords.observations])
  const calculatedTargets = useMemo(() => calculateNutritionTargets(latestWeight?.value ?? null, nutritionConfig), [latestWeight?.value, nutritionConfig])
  const displayedDay = day.data && calculatedTargets.weightKg != null ? {
    ...day.data,
    targets: {
      caloriesKcal: calculatedTargets.caloriesKcal ?? day.data.targets.caloriesKcal,
      carbsG: calculatedTargets.carbsG,
      proteinG: calculatedTargets.proteinG,
      fatG: calculatedTargets.fatG,
    },
  } : day.data
  const fridge = useFridgeRecords()
  const [sheet, setSheet] = useState<Sheet | null>(null), [busy, setBusy] = useState(false), [error, setError] = useState<string | null>(null), [saveMessage, setSaveMessage] = useState<string | null>(null)
  const handledRequest = useRef<string | null>(null)
  const saveMessageTimer = useRef<number | null>(null)
  useEffect(() => () => { if (saveMessageTimer.current !== null) window.clearTimeout(saveMessageTimer.current) }, [])
  const action = location.state as { mobileAction?: string; requestId?: string; fridgeItemId?: string } | null
  useEffect(() => {
    if (!action?.requestId || !["food-entry", "food-fridge-entry"].includes(action.mobileAction ?? "") || handledRequest.current === action.requestId) return
    const timer = window.setTimeout(() => {
      handledRequest.current = action.requestId!
      setError(null)
      const meal = defaultMealType()
      const next: Sheet = { type: "search", meal }
      if (action.mobileAction === "food-fridge-entry" && action.fridgeItemId) {
        void fetchFridgeInventory().then(inventory => {
          const item = inventory.items.find(candidate => candidate.id === action.fridgeItemId)
          if (!item) { setError("This fridge item is no longer available."); return }
          const entry: Sheet = { type: "entry", food: fridgeItemAsFood(item), meal }
          setSheet(day.data?.status === "complete" ? { type: "reopen", next: entry } : entry)
        }).catch(cause => setError(cause instanceof Error ? cause.message : "Unable to open this fridge item."))
      } else setSheet(day.data?.status === "complete" ? { type: "reopen", next } : next)
    }, 0)
    return () => window.clearTimeout(timer)
  }, [action, day.data?.status])
  function open(next: Sheet) { setError(null); setSheet(day.data?.status === "complete" && next.type !== "group" ? { type: "reopen", next } : next) }
  function selectDate(value: string) { setDate(value); setSheet(null); setSaveMessage(null); setError(null); navigate({ pathname: location.pathname, search: location.search, hash: "#food-today" }) }
  function showSaveMessage() {
    if (saveMessageTimer.current !== null) window.clearTimeout(saveMessageTimer.current)
    setSaveMessage("A little goodness, logged.")
    saveMessageTimer.current = window.setTimeout(() => { setSaveMessage(null); saveMessageTimer.current = null }, 2400)
  }
  async function changeStatus(complete: boolean, next: Sheet | null = null) {
    if (busy) return; setBusy(true); setError(null)
    try { await setDayStatus(date, complete); announceFoodChange(); setSheet(next) }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Unable to update diary.") } finally { setBusy(false) }
  }
  const sheetTitle = !sheet ? "" : ({ search: "Log food", entry: sheet.type === "entry" && sheet.item ? "Your food" : "Log food", custom: "Create a food", group: "On your table", reopen: "Reopen this day?", complete: "Finish this day?" })[sheet.type]
  return <div className="mobile-food">
    {nutrition.error && <p className="food-load-error" role="status">Nutrition targets are using the last available configuration. <button type="button" className="food-text-button" onClick={nutrition.reload}>Try again</button></p>}
    {view === "foods" ? <FoodCatalog onSelect={food => open({ type: "entry", food, meal: defaultMealType() })} onCustom={() => setSheet({ type: "custom", meal: defaultMealType() })} /> : view === "history" ? <FoodHistoryPage today={today} onSelectDate={selectDate} /> : view === "goals" ? <FoodGoalsPage today={today} /> : <><RemoteFeedback {...day} />{displayedDay && <FoodToday day={displayedDay} today={today} onDateChange={selectDate} onAdd={meal => open({ type: "search", meal })} onEdit={item => open({ type: "entry", item, meal: item.mealType })} onSelectGroup={items => setSheet(items.length === 1 && displayedDay.status !== "complete" ? { type: "entry", item: items[0], meal: items[0].mealType } : { type: "group", items })} onComplete={() => displayedDay.status === "complete" ? setSheet({ type: "reopen", next: null }) : setSheet({ type: "complete" })} busy={busy || day.loading} />}</>}
    {sheet && <FoodSheet title={sheetTitle} busy={busy} onClose={() => { if (!busy) { setSheet(null); setError(null) } }}>
      <div className="mobile-food food-sheet-body">
        {sheet.type === "search" && <><p className="food-context">{readableDate(date, true)}</p><FoodCatalog compact onSelect={food => setSheet({ type: "entry", food, meal: sheet.meal })} onCustom={() => setSheet({ type: "custom", meal: sheet.meal })} onQuick={() => setSheet({ type: "entry", meal: sheet.meal })} /></>}
        {sheet.type === "entry" && <FoodEntry key={sheet.item?.id ?? `${sheet.food?.fridgeItemId ?? sheet.food?.kind}/${sheet.food?.id}`} date={date} initialMeal={sheet.meal} food={sheet.food} item={sheet.item} targets={displayedDay?.targets} onBack={() => setSheet({ type: "search", meal: sheet.meal })} onSaved={fridgeChanged => { if (fridgeChanged) void fridge.refresh(); if (sheet.item) { setSheet(null); return } setSheet(null); setError(null); showSaveMessage() }} />}
        {sheet.type === "custom" && <CustomFoodForm onBack={() => setSheet({ type: "search", meal: sheet.meal })} onSaved={food => setSheet({ type: "entry", food, meal: sheet.meal })} />}
        {sheet.type === "group" && <><div className="food-section-title"><h3>{sheet.items[0]?.foodName}</h3><p>{foodAmount(sheet.items)} · {sheet.items.length} {sheet.items.length === 1 ? "entry" : "entries"}</p></div><NutritionPreview values={sumNutrition(sheet.items).totals} caption="For these diary entries" /><p className="food-footnote">Select an entry to change its amount.</p>{sheet.items.map(item => <button key={item.id} type="button" className="food-group-entry" onClick={() => open({ type: "entry", item, meal: item.mealType })}><span>{item.mealType === "snack" ? "Snacks" : item.mealType.charAt(0).toUpperCase() + item.mealType.slice(1)}<small>{foodAmount([item])}</small></span><ChevronRight /></button>)}</>}
        {sheet.type === "reopen" && <div className="food-confirmation"><h3>Make room for a change.</h3><p>This diary is complete. Reopen {readableDate(date)} to add or edit food.</p><button type="button" className="food-primary" disabled={busy} onClick={() => changeStatus(false, sheet.next)}>{busy ? "Reopening…" : "Reopen diary"}</button><button type="button" className="food-text-button" disabled={busy} onClick={() => setSheet(null)}>Keep it complete</button></div>}
        {sheet.type === "complete" && <div className="food-confirmation"><h3>{day.data?.items.length ? "All good for today?" : "Nothing to log?"}</h3><p>{day.data?.items.length ? "Finish this diary when everything you ate is recorded. You can reopen it later." : "No food is recorded. Complete this date only if you intend to record a day with zero intake."}</p><button type="button" className="food-primary" disabled={busy} onClick={() => changeStatus(true)}>{busy ? "Finishing…" : "Finish diary"}</button><button type="button" className="food-text-button" disabled={busy} onClick={() => setSheet(null)}>Keep logging</button></div>}
        {error && <p className="food-form-error" role="alert">{error}</p>}
      </div>
    </FoodSheet>}
    {saveMessage && <p className="food-save-message" role="status">{saveMessage}</p>}
  </div>
}

export default function FoodPage() {
  const location = useLocation(), navigate = useNavigate()
  const today = dateKey(new Date())
  const [date, setDate] = useState(today)
  const view = location.hash.replace("#food-", "") || "today"
  return view === "fridge" ? <MobileFridgePage onLogMeal={item => navigate({ pathname: location.pathname, search: location.search, hash: "#food-today" }, { state: { mobileAction: "food-fridge-entry", fridgeItemId: item.id, requestId: crypto.randomUUID() } })} /> : <FoodDiaryPage today={today} date={date} setDate={setDate} />
}
