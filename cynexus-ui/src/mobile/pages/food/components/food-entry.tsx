import { ArrowLeft, Camera, ChevronRight, ImagePlus, LoaderCircle, ScanSearch, Star, Trash2, X } from "lucide-react"
import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from "react"
import { enqueueVisionScenario, fetchVisionTask } from "@/services/ai/vision"
import { addFavorite, announceFoodChange, createCustomFood, createFridgeMeal, createItem, deleteFavorite, deleteItem, fetchFoodAssets, updateItem } from "@/services/food/api"
import { mattingFoodImage } from "@/services/food/matting"
import { submitFoodRecognition, type FoodRecognitionTask } from "@/services/food/recognition"
import { formatNutrition, meals, nutrientKeys, nutrientNames, readableDate, scaledNutrition } from "@/services/food/model"
import { clearUneditedNutritionLabelDraft, mergeNutritionLabelDraft, nutritionLabelPer100g, parseNutritionLabelRecognition, prepareFoodPhotoImage, prepareNutritionLabelImage, type NutritionLabelDraftField } from "@/services/food/nutrition-label"
import type { DiaryItem, Food, FoodAssetMapping, MealType, NutrientKey, Nutrients } from "@/services/food/types"
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
    {available != null && <small className="food-amount-available"><strong>{formatNutrition(available, 1)}</strong> available</small>}
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

export function CustomFoodForm({ onSaved, onQueued, onBack }: { onSaved: (food: Food) => void; onQueued: (task: FoodRecognitionTask) => void; onBack: () => void }) {
  const [name, setName] = useState(""), [brand, setBrand] = useState("")
  const [values, setValues] = useState(() => draftNutrients())
  const [assetMappings, setAssetMappings] = useState<FoodAssetMapping[]>([])
  const [assetKey, setAssetKey] = useState("")
  const [assetFilter, setAssetFilter] = useState("")
  const [assetMappingOpen, setAssetMappingOpen] = useState(false)
  const [assetLoading, setAssetLoading] = useState(true), [assetError, setAssetError] = useState<string | null>(null)
  const [assetReload, setAssetReload] = useState(0)
  const [requestKey, setRequestKey] = useState(() => crypto.randomUUID())
  const [busy, setBusy] = useState(false), [error, setError] = useState<string | null>(null)
  const [foodPhoto, setFoodPhoto] = useState<File | null>(null)
  const [foodPhotoUrl, setFoodPhotoUrl] = useState<string | null>(null)
  const [nutritionPhoto, setNutritionPhoto] = useState<File | null>(null)
  const [nutritionPhotoUrl, setNutritionPhotoUrl] = useState<string | null>(null)
  const [cutoutUrl, setCutoutUrl] = useState<string | null>(null)
  const [cutoutDataUrl, setCutoutDataUrl] = useState<string | null>(null)
  const [scanning, setScanning] = useState(false), [scanError, setScanError] = useState<string | null>(null)
  const [scanMessage, setScanMessage] = useState<string | null>(null)
  const [matting, setMatting] = useState(false), [mattingError, setMattingError] = useState<string | null>(null)
  const [mattingMessage, setMattingMessage] = useState<string | null>(null)
  const foodFileInput = useRef<HTMLInputElement>(null)
  const nutritionFileInput = useRef<HTMLInputElement>(null)
  const foodPhotoUrlRef = useRef<string | null>(null)
  const nutritionPhotoUrlRef = useRef<string | null>(null)
  const cutoutUrlRef = useRef<string | null>(null)
  const mattingRequest = useRef<AbortController | null>(null)
  const manuallyEdited = useRef(new Set<NutritionLabelDraftField>())
  const requiresNutritionRecognition = !!nutritionPhoto && nutrientKeys.some(key => values[key].trim() === "")
  useEffect(() => {
    const controller = new AbortController()
    fetchFoodAssets(controller.signal)
      .then(setAssetMappings)
      .catch(cause => {
        if (cause instanceof DOMException && cause.name === "AbortError") return
        setAssetError(cause instanceof Error ? cause.message : "Unable to load food artwork mappings.")
      })
      .finally(() => { if (!controller.signal.aborted) setAssetLoading(false) })
    return () => controller.abort()
  }, [assetReload])
  useEffect(() => () => {
    mattingRequest.current?.abort()
    if (foodPhotoUrlRef.current) URL.revokeObjectURL(foodPhotoUrlRef.current)
    if (nutritionPhotoUrlRef.current) URL.revokeObjectURL(nutritionPhotoUrlRef.current)
    if (cutoutUrlRef.current) URL.revokeObjectURL(cutoutUrlRef.current)
  }, [])
  async function save(event: FormEvent) {
    event.preventDefault(); if (busy || scanning || (matting && !requiresNutritionRecognition)) return; setBusy(true); setError(null)
    try {
      if (requiresNutritionRecognition) {
        const [labelImage, foodImage] = await Promise.all([
          prepareNutritionLabelImage(nutritionPhoto!),
          foodPhoto ? prepareFoodPhotoImage(foodPhoto) : null,
        ])
        const nutrients = Object.fromEntries(nutrientKeys.map(key => [key, values[key].trim() === "" ? null : Number(values[key])])) as Nutrients
        if (nutrientKeys.some(key => nutrients[key] != null && (!Number.isFinite(nutrients[key]) || nutrients[key]! < 0 || nutrients[key]! > 10_000_000))) throw new Error("Enter valid non-negative nutrition values.")
        const submission = await submitFoodRecognition({
          idempotencyKey: requestKey,
          nutritionLabelImage: { fileName: "nutrition-label.jpg", ...labelImage },
          foodImage: foodImage ? { fileName: "food-photo.jpg", ...foodImage } : undefined,
          name: name.trim() || undefined,
          brand: brand.trim() || undefined,
          assetKey: assetKey || undefined,
          nutrients,
        })
        onQueued(submission)
        return
      }
      // Persist the selected food photo even when the user saves without first
      // pressing the separate cutout action. The cutout is reused by the saved
      // artwork, so this cannot diverge from the image shown in the form.
      const assetImageDataUrl = foodPhoto ? (cutoutDataUrl ?? await ensureFoodCutout() ?? undefined) : undefined
      const food = await createCustomFood({ idempotencyKey: requestKey, name: name.trim(), brand: brand.trim() || undefined, nutrients: readNutrients(values), assetKey: assetKey || undefined, assetImageDataUrl }); announceFoodChange(); onSaved(food)
    }
    catch (cause) {
      setError(cause instanceof Error ? cause.message : "Unable to create food.")
    } finally { setBusy(false) }
  }
  function selectFoodPhoto(event: ChangeEvent<HTMLInputElement>) {
    const selected = event.target.files?.[0]
    event.target.value = ""
    if (!selected) return
    if (foodPhotoUrlRef.current) URL.revokeObjectURL(foodPhotoUrlRef.current)
    if (cutoutUrlRef.current) URL.revokeObjectURL(cutoutUrlRef.current)
    foodPhotoUrlRef.current = null
    cutoutUrlRef.current = null
    setFoodPhoto(null)
    setFoodPhotoUrl(null)
    setCutoutUrl(null)
    setCutoutDataUrl(null)
    setMattingMessage(null)
    setMattingError(null)
    if (!selected.type.startsWith("image/")) { setMattingError("Choose an image file."); return }
    if (selected.size > 12 * 1024 * 1024) { setMattingError("Choose an image smaller than 12 MB."); return }
    foodPhotoUrlRef.current = URL.createObjectURL(selected)
    setFoodPhotoUrl(foodPhotoUrlRef.current)
    setFoodPhoto(selected)
  }
  function selectNutritionPhoto(event: ChangeEvent<HTMLInputElement>) {
    const selected = event.target.files?.[0]
    event.target.value = ""
    if (!selected) return
    const draft = clearUneditedNutritionLabelDraft({ name, brand, values }, manuallyEdited.current)
    setName(draft.name)
    setBrand(draft.brand)
    setValues(draft.values)
    if (nutritionPhotoUrlRef.current) URL.revokeObjectURL(nutritionPhotoUrlRef.current)
    nutritionPhotoUrlRef.current = null
    setNutritionPhoto(null)
    setNutritionPhotoUrl(null)
    setScanMessage(null)
    setScanError(null)
    if (!selected.type.startsWith("image/")) { setScanError("Choose an image file."); return }
    if (selected.size > 12 * 1024 * 1024) { setScanError("Choose an image smaller than 12 MB."); return }
    nutritionPhotoUrlRef.current = URL.createObjectURL(selected)
    setNutritionPhotoUrl(nutritionPhotoUrlRef.current)
    setNutritionPhoto(selected)
  }
  async function scanLabel() {
    if (!nutritionPhoto || scanning || busy) return
    setScanning(true); setScanError(null); setScanMessage(null)
    try {
      const [labelImage, foodImage] = await Promise.all([
        prepareNutritionLabelImage(nutritionPhoto),
        foodPhoto ? prepareFoodPhotoImage(foodPhoto) : null,
      ])
      const images = [
        ...(foodImage ? [{ fileName: "food-photo.jpg", contentType: foodImage.contentType, dataUrl: foodImage.dataUrl }] : []),
        { fileName: "nutrition-label.jpg", contentType: labelImage.contentType, dataUrl: labelImage.dataUrl },
      ]
      const submission = await enqueueVisionScenario("food_nutrition_label_parse", images, crypto.randomUUID())
      setScanMessage("Submission accepted. Nutrition analysis is queued.")

      // Poll the workflow instead of holding the browser request open while the
      // vision provider processes both images. This keeps the mobile request
      // below the proxy timeout and mirrors the API Workflows screen semantics.
      let snapshot = await fetchVisionTask(submission)
      const terminalSuccess = new Set(["SUCCEEDED", "SUCCESS", "COMPLETED", "DONE"])
      const terminalFailure = new Set(["FAILED", "ERROR", "CANCELLED", "CANCELED"])
      let pollCount = 0
      while (!terminalSuccess.has(snapshot.status.toUpperCase()) && !terminalFailure.has(snapshot.status.toUpperCase())) {
        if (++pollCount >= 90) throw new Error("Nutrition analysis is taking too long. Check the task history and try again later.")
        await new Promise(resolve => window.setTimeout(resolve, 2_000))
        snapshot = await fetchVisionTask(submission)
      }
      if (terminalFailure.has(snapshot.status.toUpperCase())) {
        throw new Error(snapshot.errorMessage || snapshot.error || snapshot.message || "Nutrition analysis failed. Try again.")
      }
      const content = typeof snapshot.result === "string"
        ? snapshot.result
        : snapshot.content ?? snapshot.result?.content
      if (!content) throw new Error("Nutrition analysis completed without a result.")
      const label = parseNutritionLabelRecognition(content)
      const per100g = nutritionLabelPer100g(label)
      const draft = mergeNutritionLabelDraft({ name, brand, values }, label, manuallyEdited.current)
      setName(draft.name)
      setBrand(draft.brand)
      setValues(draft.values)
      if (nutrientKeys.some(key => per100g[key] != null)) {
        setScanMessage(`Label detected · ${label.confidence} confidence. Review each value before creating.`)
      } else if (!label.recognized) {
        setScanMessage("No readable nutrition table found. You can enter the details manually.")
      } else if (label.basis === "unknown") {
        setScanMessage("Label detected, but the nutrition basis is unclear. Enter values per 100 g.")
      } else if (label.basis === "per_serving" && label.servingSizeGrams == null) {
        setScanMessage("Label detected, but serving weight is missing. Enter values per 100 g.")
      } else {
        setScanMessage("Label detected, but no nutrient values were readable. Enter the values manually.")
      }
    } catch (cause) {
      setScanError(cause instanceof Error ? cause.message : "Unable to recognize this label. Try another image.")
    } finally { setScanning(false) }
  }
  async function ensureFoodCutout() {
    if (!foodPhoto) return null
    if (cutoutDataUrl) return cutoutDataUrl
    const controller = new AbortController()
    mattingRequest.current = controller
    setMatting(true); setMattingError(null); setMattingMessage(null)
    try {
      const result = await mattingFoodImage(foodPhoto, controller.signal)
      controller.signal.throwIfAborted()
      if (cutoutUrlRef.current) URL.revokeObjectURL(cutoutUrlRef.current)
      cutoutUrlRef.current = URL.createObjectURL(result.blob)
      setCutoutUrl(cutoutUrlRef.current)
      setCutoutDataUrl(result.dataUrl)
      setMattingMessage("Food image extracted. It will be saved with this food.")
      return result.dataUrl
    } finally {
      if (mattingRequest.current === controller) mattingRequest.current = null
      if (!controller.signal.aborted) setMatting(false)
    }
  }
  async function extractFoodImage() {
    if (!foodPhoto || matting || busy) return
    try {
      await ensureFoodCutout()
    } catch (cause) {
      if (cause instanceof DOMException && cause.name === "AbortError") return
      setMattingError(cause instanceof Error ? cause.message : "Unable to extract the food image. Try a clearer photo.")
    }
  }
  const formBusy = busy || scanning || matting
  const selectedAsset = assetMappings.find(mapping => mapping.assetKey === assetKey)
  const normalizedAssetFilter = assetFilter.trim().toLocaleLowerCase()
  const visibleAssets = normalizedAssetFilter
    ? assetMappings.filter(mapping => `${mapping.catalogKey} ${mapping.assetKey} ${mapping.printedLabel ?? ""}`.toLocaleLowerCase().includes(normalizedAssetFilter))
    : assetMappings
  return <form className="food-form food-create-form" onSubmit={save} onChange={() => setRequestKey(crypto.randomUUID())}>
    <button type="button" className="food-text-button" disabled={formBusy} onClick={onBack}><ArrowLeft />Back</button>
    <section className="food-label-scan" aria-label="Food photo and nutrition label">
      <button type="button" className="food-label-row" disabled={formBusy} onClick={() => foodFileInput.current?.click()}>
        <span className="food-label-row-icon"><ImagePlus aria-hidden="true" /></span>
        <strong>Food photo</strong>
        <span className="food-label-row-action">{foodPhoto ? "Retake" : "Take photo"}<ChevronRight aria-hidden="true" /></span>
      </button>
      <input ref={foodFileInput} className="food-label-file" type="file" accept="image/*" capture="environment" aria-label="Take a food photo" onChange={selectFoodPhoto} />
      {foodPhotoUrl ? <div className="food-label-previews"><div className="food-label-preview"><img src={foodPhotoUrl} alt="Selected food photo" /><span>Food photo</span><button type="button" className="food-label-remove" aria-label="Remove food image" title="Remove food image" disabled={formBusy} onClick={() => { if (foodPhotoUrlRef.current) URL.revokeObjectURL(foodPhotoUrlRef.current); if (cutoutUrlRef.current) URL.revokeObjectURL(cutoutUrlRef.current); foodPhotoUrlRef.current = null; cutoutUrlRef.current = null; setFoodPhotoUrl(null); setFoodPhoto(null); setCutoutUrl(null); setCutoutDataUrl(null); setMattingMessage(null); setMattingError(null) }}><X /></button></div>{cutoutUrl ? <div className="food-label-preview food-cutout-preview"><img src={cutoutUrl} alt="Extracted food image" /><span>Cutout</span></div> : null}</div> : null}
      {foodPhoto ? <div className="food-label-actions"><button type="button" className="food-label-pick" disabled={formBusy} onClick={() => foodFileInput.current?.click()}><Camera />Replace photo</button><button type="button" className="food-label-scan-button" disabled={formBusy} onClick={() => void extractFoodImage()}>{matting ? <LoaderCircle className="animate-spin" /> : <ImagePlus />}{matting ? "Cutting out…" : "Cut out food"}</button></div> : null}
      {mattingError ? <p className="food-label-error" role="alert">{mattingError}</p> : null}
      {mattingMessage ? <p className="food-label-result" role="status">{mattingMessage}</p> : null}

      <button type="button" className="food-label-row" disabled={formBusy} onClick={() => nutritionFileInput.current?.click()}>
        <span className="food-label-row-icon"><ScanSearch aria-hidden="true" /></span>
        <strong>Nutrition label photo</strong>
        <span className="food-label-row-action">{nutritionPhoto ? "Retake" : "Take photo"}<ChevronRight aria-hidden="true" /></span>
      </button>
      <input ref={nutritionFileInput} className="food-label-file" type="file" accept="image/*" capture="environment" aria-label="Take a nutrition label photo" onChange={selectNutritionPhoto} />
      {nutritionPhotoUrl ? <div className="food-label-previews"><div className="food-label-preview"><img src={nutritionPhotoUrl} alt="Selected nutrition label photo" /><span>Nutrition label</span><button type="button" className="food-label-remove" aria-label="Remove nutrition label image" title="Remove nutrition label image" disabled={formBusy} onClick={() => { if (nutritionPhotoUrlRef.current) URL.revokeObjectURL(nutritionPhotoUrlRef.current); nutritionPhotoUrlRef.current = null; setNutritionPhotoUrl(null); setNutritionPhoto(null); setScanMessage(null); setScanError(null) }}><X /></button></div></div> : null}
      {nutritionPhoto ? <div className="food-label-actions"><button type="button" className="food-label-pick" disabled={formBusy} onClick={() => nutritionFileInput.current?.click()}><Camera />Replace photo</button><button type="button" className="food-label-scan-button" disabled={busy || scanning} onClick={() => void scanLabel()}>{scanning ? <LoaderCircle className="animate-spin" /> : <ScanSearch />}{scanning ? "Analyzing…" : "Analyze food + nutrition"}</button></div> : null}
      {scanError ? <p className="food-label-error" role="alert">{scanError}</p> : null}
      {scanMessage ? <p className="food-label-result" role="status">{scanMessage}</p> : null}
    </section>
    <fieldset className="food-create-fields" aria-label="Food details" disabled={formBusy}>
      <div className="food-create-details-grid">
        <label className="food-field"><span>Food name</span><input required={!requiresNutritionRecognition} maxLength={200} value={name} placeholder="e.g. Greek yogurt" onChange={event => { manuallyEdited.current.add("name"); setName(event.target.value) }} /></label>
        <label className="food-field"><span>Brand <small>optional</small></span><input maxLength={200} value={brand} placeholder="e.g. Plain" onChange={event => { manuallyEdited.current.add("brand"); setBrand(event.target.value) }} /></label>
      </div>
      <section className="food-nutrition-editor" aria-labelledby="food-nutrition-editor-title">
        <div className="food-create-section-heading"><strong id="food-nutrition-editor-title">Nutrition</strong><span>Per 100 g</span></div>
        <NutrientFields values={values} placeholder="Add value" onChange={(key, value) => { manuallyEdited.current.add(key); setValues(previous => ({ ...previous, [key]: value })) }} />
      </section>
      <section className="food-asset-mapping" aria-labelledby="food-asset-mapping-title">
        <div className="food-asset-topline">
          <div className="food-asset-heading"><strong id="food-asset-mapping-title">Food artwork</strong><span className="food-asset-optional">Optional</span>{selectedAsset && <FoodArt food={{ name, assetKey: selectedAsset.assetKey, assetUrl: selectedAsset.assetUrl }} className="food-asset-preview" />}</div>
          {!assetLoading && !assetError && assetMappings.length > 0 ? <details className="food-asset-disclosure" onToggle={event => setAssetMappingOpen(event.currentTarget.open)}>
            <summary><span>{selectedAsset ? "Change mapping" : "Add mapping"}</span><ChevronRight aria-hidden="true" /></summary>
          </details> : null}
        </div>
        {assetLoading ? <p className="food-asset-state" role="status">Loading artwork mappings…</p> : assetError ? <div className="food-asset-state food-asset-error" role="alert"><span>{assetError}</span><button type="button" className="food-text-button" onClick={() => { setAssetLoading(true); setAssetError(null); setAssetReload(value => value + 1) }}>Retry</button></div> : assetMappings.length === 0 ? <p className="food-asset-state">No artwork mappings are available.</p> : null}
        {assetMappingOpen && assetMappings.length > 0 ? <div className="food-asset-controls">
          <label className="food-field food-asset-search"><span>Find mapping <small>optional</small></span><input value={assetFilter} onChange={event => setAssetFilter(event.target.value)} placeholder="food-201 or label" /></label>
          <label className="food-field"><span>Mapping</span><select value={assetKey} onChange={event => setAssetKey(event.target.value)}><option value="">No artwork</option>{visibleAssets.map(mapping => <option value={mapping.assetKey} key={mapping.catalogKey}>{mapping.catalogKey} · {mapping.assetKey}{mapping.printedLabel == null ? "" : ` · label ${mapping.printedLabel}`}</option>)}</select></label>
        </div> : null}
        {selectedAsset ? <div className="food-asset-selected"><div><strong>{selectedAsset.catalogKey}</strong><span>{selectedAsset.assetKey}{selectedAsset.sourceAtlas ? ` · ${selectedAsset.sourceAtlas}` : ""}</span></div><button type="button" className="food-text-button" onClick={() => setAssetKey("")}>Clear</button></div> : null}
      </section>
    </fieldset>
    <div className="food-form-footer">
      {error && <p className="food-form-error" role="alert">{error}</p>}
      <button className="food-primary" type="submit" disabled={busy || scanning || (matting && !requiresNutritionRecognition)}>{busy ? "Creating…" : "Create food"}</button>
    </div>
  </form>
}
