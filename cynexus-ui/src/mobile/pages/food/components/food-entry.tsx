import { ArrowLeft, Star, Trash2 } from "lucide-react"
import { useState, type FormEvent } from "react"
import { addFavorite, announceFoodChange, createCustomFood, createFridgeMeal, createItem, deleteFavorite, deleteItem, updateItem } from "@/services/food/api"
import { formatNutrition, meals, nutrientKeys, nutrientNames, readableDate, scaledNutrition } from "@/services/food/model"
import type { DiaryItem, Food, MealType, NutrientKey, Nutrients } from "@/services/food/types"
import { FoodArt } from "./food-art"
import { draftNutrients, readNutrients } from "./food-form"
import { NutrientFields } from "./food-nutrient-fields"
import { foodAmount } from "./scene-model"
import "./food-entry.css"

function initialAmount(food?: Food) {
  if (!food?.fridgeItemId) return 100
  const available = food.availableQuantity ?? 0
  return food.availableUnit === "g" || food.availableUnit === "ml" ? Math.min(100, available) : Math.min(1, available)
}

function targetShare(value: number | null, target: number | null) {
  if (value == null) return "Unknown"
  if (target == null) return "No target"
  if (target <= 0) return "Zero target"
  const percent = value / target * 100
  return `${percent < 10 ? percent.toFixed(1) : Math.round(percent)}%`
}

function NutritionMeasure({ nutrient, value, target }: { nutrient: NutrientKey; value: number | null; target: number | null }) {
  const percent = value != null && target != null && target > 0 ? value / target * 100 : null
  const unit = nutrient === "caloriesKcal" ? "kcal" : "g"
  return <div className={`food-preview-measure food-preview-${nutrient}`}>
    <div className="food-preview-measure-head"><span>{nutrientNames[nutrient]}</span><strong>{targetShare(value, target)}</strong></div>
    <p>{formatNutrition(value, 1)} <small>{unit}</small></p>
    <div className="food-preview-track" role={percent == null ? undefined : "progressbar"} aria-label={percent == null ? undefined : `${nutrientNames[nutrient]} share of daily target`} aria-valuemin={percent == null ? undefined : 0} aria-valuemax={percent == null ? undefined : 100} aria-valuenow={percent == null ? undefined : Math.min(100, percent)}><i style={{ width: `${Math.min(100, percent ?? 0)}%` }} /></div>
    <small className="food-preview-target">{target == null ? "Daily goal not set" : target <= 0 ? "Zero daily goal" : `of ${formatNutrition(target, 1)} ${unit}`}</small>
  </div>
}

function AmountCard({ amount, unit, max, available, disabled, onChange }: { amount: string; unit: string; max?: number; available?: number | null; disabled: boolean; onChange: (value: string) => void }) {
  return <label className="food-amount-card">
    <span className="food-amount-label">Amount</span>
    <input autoFocus type="number" inputMode="decimal" required min="0.001" max={max} step="any" value={amount} disabled={disabled} onChange={event => onChange(event.target.value)} />
    <span className="food-amount-unit">{unit}</span>
    {available != null && <small className="food-amount-available">{formatNutrition(available, 1)} available</small>}
  </label>
}

export function NutritionPreview({ values, caption, targets }: { values: Nutrients; caption: string; targets?: Nutrients }) {
  return <section className={`food-nutrition-preview${targets ? " food-nutrition-goal-preview" : ""}`} aria-label="Nutrition preview">
    <div className="food-preview-heading"><span>{targets ? "THIS SERVING" : caption}</span>{targets && <small>{caption}</small>}</div>
    {targets ? <>
      <NutritionMeasure nutrient="caloriesKcal" value={values.caloriesKcal} target={targets.caloriesKcal} />
      <div className="food-preview-macros">{(["carbsG", "proteinG", "fatG"] as const).map(key => <NutritionMeasure key={key} nutrient={key} value={values[key]} target={targets[key]} />)}</div>
      {nutrientKeys.some(key => values[key] == null) && <small className="food-preview-partial">Partial nutrition · unavailable values stay unknown.</small>}
    </> : <><p className="food-preview-caption">{caption}</p><dl>{nutrientKeys.map(key => <div key={key}><dt>{nutrientNames[key]}</dt><dd>{formatNutrition(values[key], 1)}<small>{key === "caloriesKcal" ? " kcal" : " g"}</small></dd></div>)}</dl>{nutrientKeys.some(key => values[key] == null) && <small>Partial nutrition · unavailable values stay unknown.</small>}</>}
  </section>
}

export function FoodEntry({ date, initialMeal, food, item, targets, onBack, onSaved }: { date: string; initialMeal: MealType; food?: Food; item?: DiaryItem; targets?: Nutrients; onBack: () => void; onSaved: (fridgeChanged?: boolean) => void }) {
  const quick = item ? item.entryKind === "quick" : !food
  const [meal, setMeal] = useState(initialMeal)
  const [amount, setAmount] = useState(String(item?.consumedGrams ?? initialAmount(food)))
  const [name, setName] = useState(item?.foodName ?? "")
  const [nutrients, setNutrients] = useState(() => draftNutrients(item?.nutrients))
  const [requestKey, setRequestKey] = useState(() => crypto.randomUUID())
  const [busy, setBusy] = useState(false), [error, setError] = useState<string | null>(null)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [favorite, setFavorite] = useState(food?.favoriteId ?? null)
  const fridgeGrams = food?.fridgeItemId && food.availableQuantity && food.availableGrams != null
    ? Number(amount || 0) / food.availableQuantity * food.availableGrams
    : null
  const previewGrams = fridgeGrams ?? (Number(amount) || 0)
  const preview = quick ? Object.fromEntries(nutrientKeys.map(key => [key, nutrients[key] === "" ? null : Number(nutrients[key])])) as Nutrients : scaledNutrition(food?.nutrients ?? item!.per100g, previewGrams)
  async function save(event: FormEvent) {
    event.preventDefault(); if (busy) return
    setBusy(true); setError(null)
    try {
      const values = quick ? readNutrients(nutrients) : undefined
      if (!quick && (!Number.isFinite(Number(amount)) || Number(amount) <= 0 || Number(amount) > (food?.fridgeItemId ? food.availableQuantity ?? 0 : 1000000))) throw new Error(food?.fridgeItemId ? "Enter a quantity within your available fridge stock." : "Enter an amount between 0 and 1,000,000 g.")
      if (food?.fridgeItemId && !food.fridgeMealReady) throw new Error("This fridge item needs a confirmed nutrition match before it can be logged.")
      if (item) await updateItem(date, item.id, quick ? { mealType: meal, nutrients: values } : { mealType: meal, quantity: Number(amount), unit: "g", gramsPerUnit: 1 })
      else if (food?.fridgeItemId) await createFridgeMeal(date, { idempotencyKey: requestKey, timezone: Intl.DateTimeFormat().resolvedOptions().timeZone, mealType: meal, fridgeItemId: food.fridgeItemId, quantity: Number(amount), unit: food.availableUnit ?? "g" })
      else await createItem(date, { idempotencyKey: requestKey, timezone: Intl.DateTimeFormat().resolvedOptions().timeZone, mealType: meal, entryKind: food?.kind ?? "quick", foodId: food?.id, foodName: name.trim() || "Quick entry", ...(quick ? { nutrients: values } : { quantity: Number(amount), unit: "g", gramsPerUnit: 1 }) })
      announceFoodChange(); onSaved(!!food?.fridgeItemId)
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Unable to save. Try again.") } finally { setBusy(false) }
  }
  async function remove() {
    if (!item || busy) return
    setBusy(true); setError(null)
    try { await deleteItem(date, item.id); announceFoodChange(); onSaved(!!item.sourceFridgeItemId) } catch (cause) { setError(cause instanceof Error ? cause.message : "Unable to delete.") } finally { setBusy(false) }
  }
  async function toggleFavorite() {
    if (!food || busy) return
    setBusy(true); setError(null)
    try { if (favorite) { await deleteFavorite(favorite); setFavorite(null) } else { const saved = await addFavorite(food.kind, food.id, crypto.randomUUID()); setFavorite(saved.favoriteId ?? null) } announceFoodChange() }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Unable to update favorite.") } finally { setBusy(false) }
  }
  function changed() { setRequestKey(crypto.randomUUID()) }
  return <form className="food-form" onSubmit={save}>
    <button type="button" className="food-text-button" disabled={busy} onClick={onBack}><ArrowLeft />Back</button>
    {!quick && <div className="food-entry-identity"><FoodArt food={food ?? item!} /><div><h3>{food?.name ?? item?.foodName}</h3><p>{food?.brand ?? item?.brand ?? (food?.fridgeItemId || item?.sourceFridgeItemId ? "In your fridge" : food?.source ?? "Food library")}</p></div>{food && !food.fridgeItemId && <button type="button" className="food-round" aria-label={favorite ? "Remove favorite" : "Add favorite"} aria-pressed={!!favorite} disabled={busy} onClick={toggleFavorite}><Star fill={favorite ? "currentColor" : "none"} /></button>}</div>}
    <p className="food-context">{readableDate(date, true)}{item && ` · ${meals.find(value => value.id === item.mealType)?.name}`}</p>
    <label className="food-field"><span>Meal</span><select value={meal} disabled={busy} onChange={event => { setMeal(event.target.value as MealType); changed() }}>{meals.map(value => <option value={value.id} key={value.id}>{value.name}</option>)}</select></label>
    {quick ? <><label className="food-field"><span>Description</span><input maxLength={200} value={name} disabled={!!item || busy} placeholder="e.g. Afternoon snack" onChange={event => { setName(event.target.value); changed() }} /></label><p className="food-footnote">For this entry · enter the nutrition you actually consumed.</p><fieldset disabled={busy}><NutrientFields values={nutrients} onChange={(key, value) => { setNutrients(previous => ({ ...previous, [key]: value })); changed() }} /></fieldset><NutritionPreview values={preview} targets={targets} caption="Current entry" /></> : <div className="food-entry-summary-row">
      <NutritionPreview values={preview} targets={targets} caption={food?.fridgeItemId ? `${formatNutrition(Number(amount) || 0, 1)} ${food.availableUnit ?? "g"} · ${formatNutrition(previewGrams, 1)} g equivalent` : `${formatNutrition(previewGrams, 1)} g serving`} />
      <AmountCard amount={amount} unit={food?.fridgeItemId ? food.availableUnit ?? "g" : "g"} max={food?.fridgeItemId ? food.availableQuantity ?? undefined : 1000000} available={food?.fridgeItemId ? food.availableQuantity : null} disabled={busy} onChange={value => { setAmount(value); changed() }} />
    </div>}
    {!quick && item?.sourceFridgeItemId && <p className="food-footnote">Changing this amount updates your fridge stock and diary together.</p>}
    {!quick && !food?.fridgeItemId && item?.unit && item.unit !== "g" && <p className="food-footnote">Previously {foodAmount([item])} · {formatNutrition(item.consumedGrams, 1)} g.</p>}
    {food?.source && <details className="food-source"><summary>About this food</summary><p>Source: {food.source}</p>{food.license && <p>License: {food.license}</p>}{food.usageScope && <p>{food.usageScope}</p>}<p>Nutrition is saved with each diary entry. Later catalog changes do not rewrite your meals.</p></details>}
    {error && <p className="food-form-error" role="alert">{error}</p>}
    {food?.fridgeItemId && !food.fridgeMealReady && <p className="food-form-error" role="status">Confirm a food and nutrition match in the fridge before logging it as a meal.</p>}
    <button type="submit" className="food-primary" disabled={busy || (!!food?.fridgeItemId && !food.fridgeMealReady)}>{busy ? "Saving…" : item ? "Save changes" : food?.fridgeItemId ? `Log ${meals.find(value => value.id === meal)?.name.toLowerCase()}` : `Add to ${meals.find(value => value.id === meal)?.name.toLowerCase()}`}</button>
    {item && <div className="food-delete-section">{confirmDelete ? <><p>{item.sourceFridgeItemId ? "Remove this entry and return its quantity to your fridge?" : "Remove this entry from your diary?"}</p><div><button type="button" className="food-danger" disabled={busy} onClick={remove}>Remove entry</button><button type="button" className="food-text-button" disabled={busy} onClick={() => setConfirmDelete(false)}>Keep it</button></div></> : <button type="button" className="food-text-button food-danger" disabled={busy} onClick={() => setConfirmDelete(true)}><Trash2 />Remove entry</button>}</div>}
  </form>
}

export function CustomFoodForm({ onSaved, onBack }: { onSaved: (food: Food) => void; onBack: () => void }) {
  const [name, setName] = useState(""), [brand, setBrand] = useState("")
  const [values, setValues] = useState(() => draftNutrients())
  const [requestKey, setRequestKey] = useState(() => crypto.randomUUID())
  const [busy, setBusy] = useState(false), [error, setError] = useState<string | null>(null)
  async function save(event: FormEvent) {
    event.preventDefault(); if (busy) return; setBusy(true); setError(null)
    try { const food = await createCustomFood({ idempotencyKey: requestKey, name: name.trim(), brand: brand.trim() || undefined, nutrients: readNutrients(values) }); announceFoodChange(); onSaved(food) }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Unable to create food.") } finally { setBusy(false) }
  }
  return <form className="food-form" onSubmit={save} onChange={() => setRequestKey(crypto.randomUUID())}><button type="button" className="food-text-button" disabled={busy} onClick={onBack}><ArrowLeft />Back</button><p className="food-footnote">Create once, log whenever. Nutrition is per 100 g.</p><fieldset disabled={busy}><label className="food-field"><span>Food name</span><input required maxLength={200} autoFocus value={name} onChange={event => setName(event.target.value)} /></label><label className="food-field"><span>Brand <small>optional</small></span><input maxLength={200} value={brand} onChange={event => setBrand(event.target.value)} /></label><NutrientFields values={values} onChange={(key, value) => setValues(previous => ({ ...previous, [key]: value }))} /></fieldset>{error && <p className="food-form-error" role="alert">{error}</p>}<button className="food-primary" type="submit" disabled={busy}>{busy ? "Creating…" : "Create food"}</button></form>
}
