import { ArrowLeft, ArrowRight, CalendarDays, Check, ClipboardList, LoaderCircle, Mic, Minus, Plus, ShoppingCart, Sparkles, X } from "lucide-react"
import { useEffect, useMemo, useRef, useState, type CSSProperties, type FormEvent, type MouseEvent as ReactMouseEvent, type PointerEvent as ReactPointerEvent, type ReactNode } from "react"
import { createPortal } from "react-dom"

import { useFridgeRecords } from "@/mobile/hooks/use-fridge-records"
import { useMobilePortal } from "@/mobile/hooks/use-mobile-portal"
import { fetchFoodAssets } from "@/services/food/api"
import { previewFridgeInventory, previewFridgeNutritionMatches, searchFridgeMappings, type FridgeAddItemRequest, type FridgeAiPreviewItem, type FridgeMappingCandidate, type FridgeNutritionMatch } from "@/services/fridge/api"
import { addDays, changeFridgeQuantity, dateKey, getFridgeExpiryState, parseDate, saveFridgeItem, type FridgeItem, type FridgeUnit } from "@/services/fridge/model"
import { fridgeFoodCatalog, findFridgeFoodByName, getFridgeFoodAssetUrl, getFridgeFoodImageUrl, setFridgeFoodAssets } from "./lib/catalog"
import "./fridge.css"

type FridgeView = "overview" | "inside" | "records"
type ShelfZone = "top" | "middle" | "lower" | "bottom" | "drawer" | "door"

const shelves: readonly { id: ShelfZone; label: string }[] = [
  { id: "top", label: "Top shelf" }, { id: "middle", label: "Middle shelf" }, { id: "lower", label: "Lower shelf" },
  { id: "bottom", label: "Bottom shelf" }, { id: "drawer", label: "Produce drawer" }, { id: "door", label: "Door rack" },
]
const shelfFor = (item: FridgeItem): ShelfZone => {
  const sampleZones: Record<string, ShelfZone> = {
    "sample-apples": "top", "sample-strawberries": "top", "sample-blueberries": "top",
    "sample-broccoli": "middle", "sample-avocado": "middle", "sample-mushrooms": "middle",
    "sample-chicken-breast": "lower", "sample-salmon": "lower", "sample-steak": "lower", "sample-pork-ribs": "lower",
    "sample-potatoes": "bottom", "sample-onion": "bottom", "sample-pumpkin": "bottom",
    "sample-lettuce": "drawer", "sample-cucumber": "drawer", "sample-tomatoes": "drawer", "sample-carrots": "drawer",
    "sample-lemon": "door", "sample-ginger": "door",
  }
  if (item.id in sampleZones) return sampleZones[item.id]
  const food = item.catalogId ? fridgeFoodCatalog.find((entry) => entry.id === item.catalogId) : findFridgeFoodByName(item.name)
  if (food?.category === "fruit") return "top"
  if (food?.category === "vegetable") return "drawer"
  if (food?.category === "meat" || food?.category === "seafood") return "lower"
  if (/egg|milk|鸡蛋|牛奶/i.test(item.name)) return "door"
  return "middle"
}
const units: FridgeUnit[] = ["pcs", "pack", "mg", "g", "kg", "ml", "L", "head", "bunch", "bottle", "carton"]
function requiresMeasuredWeight(unit: FridgeUnit) { return unit !== "mg" && unit !== "g" && unit !== "kg" }
function measuredWeightLabel(unit: FridgeUnit) { return unit === "ml" || unit === "L" ? "Density (g/ml)" : "Weight per unit (g)" }
function quantityStep(unit: FridgeUnit) {
  if (unit === "mg") return 1000
  if (unit === "g" || unit === "ml") return 50
  if (unit === "kg" || unit === "L") return 0.1
  return 1
}
function formatQuantityStep(unit: FridgeUnit) {
  const step = quantityStep(unit)
  return step < 1 ? step.toFixed(1) : String(step)
}
const formatDate = (value: string) => parseDate(value).toLocaleDateString("en-US", { month: "short", day: "numeric" })
const formatWhen = (value: string) => new Date(value).toLocaleDateString("en-US", { month: "short", day: "numeric" })

function stateCopy(item: FridgeItem, today: string) {
  const state = getFridgeExpiryState(item.bestBefore, today)
  if (state === "expired") return { state, label: "Expired" }
  if (state === "use-soon") return { state, label: "Use soon" }
  return { state, label: "Fresh" }
}

function FoodArt({ item }: { item: FridgeItem }) {
  const visualKey = [item.assetCatalogKey, item.catalogId].find((key) => key && /^food-\d{3}$/.test(key))
  const mappedUrl = item.assetUrl ?? getFridgeFoodAssetUrl(visualKey)
  const food = (visualKey ? fridgeFoodCatalog.find((entry) => entry.id === visualKey) : undefined) ?? findFridgeFoodByName(item.name)
  return mappedUrl || food ? <img className="fridge-food-art" src={mappedUrl ?? getFridgeFoodImageUrl(food!)} alt="" width="96" height="96" draggable="false" /> : <span className="fridge-food-fallback" aria-hidden="true">{item.name.slice(0, 2)}</span>
}

/** Render the maintained food atlas for AI drafts, before the item is saved. */
function AiFoodArt({ name, catalogKey, assetUrl, onImageLoad }: { name: string; catalogKey?: string | null; assetUrl?: string | null; onImageLoad?: (image: HTMLImageElement) => void }) {
  const food = findFridgeFoodByName(name)
  const visualKey = catalogKey && /^food-\d{3}$/.test(catalogKey) ? catalogKey : undefined
  const mappedUrl = visualKey ? assetUrl ?? getFridgeFoodAssetUrl(visualKey) : undefined
  return food
    ? <img className="fridge-ai-food-art" src={mappedUrl ?? getFridgeFoodImageUrl(food)} alt="" width="64" height="64" draggable="false" onLoad={event => onImageLoad?.(event.currentTarget)} />
    : mappedUrl ? <img className="fridge-ai-food-art" src={mappedUrl} alt="" width="64" height="64" draggable="false" onLoad={event => onImageLoad?.(event.currentTarget)} />
    : <span className="fridge-ai-food-fallback" aria-hidden="true">{name.trim().slice(0, 2) || "?"}</span>
}

const MAX_VISIBLE_FOOD_LAYERS = 5

type FoodStackItem = {
  quantity: number | null
  unit: FridgeUnit | null
  stackCount?: number
  name?: string
  catalogId?: string | null
  catalogKey?: string | null
  assetCatalogKey?: string | null
}

/** Keep visual stacks bounded and consistent between fridge stock and AI drafts. */
function visualStackCount(item: FoodStackItem) {
  if (!item.unit) return 1
  const catalogKey = item.catalogId ?? item.catalogKey ?? item.assetCatalogKey
  const catalogFood = (catalogKey ? fridgeFoodCatalog.find((food) => food.id === catalogKey) : undefined)
    ?? (item.name ? findFridgeFoodByName(item.name) : undefined)
  const identity = `${item.name ?? ""} ${catalogFood?.name ?? ""} ${catalogFood?.englishName ?? ""}`
  if (item.unit === "ml" || item.unit === "L" || item.unit === "bottle" || item.unit === "carton") return 1
  if (catalogFood?.visualStackMode === "single" || /燕麦|\boat(?:s|meal)?\b/i.test(identity)) return 1
  if (catalogFood?.visualStackMode === "serving" && (item.unit === "g" || item.unit === "kg")) return 1

  const reported = typeof item.stackCount === "number" && Number.isFinite(item.stackCount) ? Math.ceil(item.stackCount) : 0
  const quantity = Math.max(0, item.quantity ?? 0)
  const fallback = item.unit === "g"
    ? Math.ceil(quantity / 250)
    : item.unit === "kg"
      ? Math.ceil(quantity)
      : Math.ceil(quantity)
  return Math.max(1, Math.min(MAX_VISIBLE_FOOD_LAYERS, Math.max(reported, fallback)))
}

function basketStackCount(item: AiDraftItem) {
  if (!item.unit) return 1
  return item.unit === "ml" || item.unit === "L" || item.unit === "bottle" || item.unit === "carton" ? 1 : visualStackCount(item)
}

function FoodArtStack({ count, renderFood }: { count: number; renderFood: () => ReactNode }) {
  return <span className={`fridge-food-stack${count > 1 ? ` has-stack stack-count-${count}` : ""}`} aria-hidden="true">{Array.from({ length: count }, (_, index) => <span className="fridge-food-stack-layer" key={index}>{renderFood()}</span>)}</span>
}

function ClosedFridge({ soonCount }: { soonCount: number }) {
  return <div className="fridge-hero" aria-label="Closed refrigerator with a use soon note" role="img">
    <div className="fridge-cabinet">
      <div className="fridge-upper">
        <div className="fridge-door"><strong>Shuaiqi Chen</strong><span className="fridge-sticker fridge-sticker-main">MY LITTLE<br /><b>FRESH NOTES</b><small>Good food, every day.</small></span><span className="fridge-sticker fridge-sticker-small">Eat<br /><b>well.</b></span></div>
        <div className="fridge-door"><span className="fridge-sticker fridge-sticker-ticket">STAY FRESH</span><span className="fridge-sticker fridge-sticker-green">GREENS</span><span className="fridge-note"><b>USE FIRST</b><small>{soonCount} items</small></span></div>
      </div>
      <div className="fridge-drawers"><span>FRESH ZONE</span><span>FREEZER</span></div>
    </div>
  </div>
}

function FridgeCard({ item, today, onSelect }: { item: FridgeItem; today: string; onSelect: () => void }) {
  const status = stateCopy(item, today)
  const stackCount = visualStackCount(item)
  return <button type="button" className={`fridge-food-card is-${status.state}`} aria-label={`Adjust ${item.name}, ${item.quantity} ${item.unit}, ${status.label}`} title={`Best before ${formatDate(item.bestBefore)}`} onClick={onSelect}>
    <span className={`fridge-food-visual${stackCount > 1 ? " has-stack" : ""}`}><FoodArtStack count={stackCount} renderFood={() => <FoodArt item={item} />} />{status.state !== "fresh" && <span className="fridge-warning" title={status.label} aria-label={status.label}>!</span>}</span>
    <span className="fridge-food-copy"><strong title={item.name}>{item.name}</strong><span>{item.quantity} {item.unit}</span></span>
  </button>
}

function ExpiringFoodRow({ item, onSelect }: { item: FridgeItem; onSelect: () => void }) {
  return <button type="button" className="fridge-expiring-row" aria-label={`Open ${item.name}, ${item.quantity} ${item.unit}, best before ${formatDate(item.bestBefore)}`} onClick={onSelect}>
    <span className="fridge-expiring-art"><FoodArt item={item} /></span>
    <span className="fridge-expiring-copy"><strong title={item.name}>{item.name}</strong><span>Best before {formatDate(item.bestBefore)}</span></span>
    <span className="fridge-expiring-quantity">{item.quantity} {item.unit}</span>
  </button>
}

function StockEditor({ item, today, onClose, onChange, onLogMeal, onConfirmMapping }: { item: FridgeItem; today: string; onClose: () => void; onChange: (delta: number) => void; onLogMeal: () => void; onConfirmMapping: (candidate: FridgeMappingCandidate) => Promise<void> }) {
  const portal = useMobilePortal()
  const dialog = useRef<HTMLDialogElement>(null)
  const status = stateCopy(item, today)
  const [mappingCandidates, setMappingCandidates] = useState<FridgeMappingCandidate[] | null>(null)
  const [mappingLoading, setMappingLoading] = useState(false)
  const [mappingError, setMappingError] = useState<string | null>(null)
  const [savingMapping, setSavingMapping] = useState<string | null>(null)
  useEffect(() => {
    const element = dialog.current
    element?.showModal()
    return () => element?.close()
  }, [])
  async function findMappings() {
    setMappingLoading(true); setMappingError(null)
    try { setMappingCandidates([...(await searchFridgeMappings(item.name)).candidates]) }
    catch (error) { setMappingError(error instanceof Error ? error.message : "Could not load nutrition matches.") }
    finally { setMappingLoading(false) }
  }
  async function chooseMapping(candidate: FridgeMappingCandidate) {
    const key = `${candidate.kind}:${candidate.id}`
    setSavingMapping(key); setMappingError(null)
    try { await onConfirmMapping(candidate); setMappingCandidates(null) }
    catch (error) { setMappingError(error instanceof Error ? error.message : "Could not save this food match.") }
    finally { setSavingMapping(null) }
  }
  const editor = <dialog ref={dialog} className="fridge-native-dialog" aria-labelledby="fridge-stock-title" onCancel={(event) => { event.preventDefault(); onClose() }} onClick={(event) => { if (event.target === event.currentTarget) onClose() }}><div className="fridge-stock-editor">
    <div className="fridge-editor-heading"><div><span>FRIDGE STOCK</span><h2 id="fridge-stock-title">{item.name}</h2></div><button type="button" className="fridge-icon-button" aria-label="Close stock adjustment" title="Close" onClick={onClose}><X aria-hidden="true" /></button></div>
    <div className="fridge-stock-summary"><FoodArt item={item} /><div><strong>{item.quantity} {item.unit}</strong><span className={`is-${status.state}`}>{status.label} · {formatDate(item.bestBefore)}</span></div></div>
    {item.mappingStatus !== "matched" && item.mappingStatus !== "user_confirmed" && <section className="fridge-mapping-panel" aria-label="Nutrition match"><div><strong>Nutrition match needed</strong><span>Choose the closest food so meals can use its calories and macros.</span></div>{mappingCandidates === null ? <button type="button" className="fridge-stock-log-meal" onClick={findMappings} disabled={mappingLoading}>{mappingLoading ? "Searching…" : "Find food match"}</button> : mappingCandidates.length ? <div className="fridge-mapping-options">{mappingCandidates.map(candidate => <button type="button" key={`${candidate.kind}:${candidate.id}`} disabled={savingMapping !== null} onClick={() => void chooseMapping(candidate)}><span><strong>{candidate.name}</strong><small>{candidate.kind === "custom" ? "Your food" : "Nutrition catalog"}</small></span><span>{candidate.nutrition?.caloriesKcal == null ? "Nutrition unavailable" : `${candidate.nutrition.caloriesKcal} kcal / 100 g`}</span>{savingMapping === `${candidate.kind}:${candidate.id}` && <LoaderCircle className="is-spinning" aria-hidden="true" />}</button>)}</div> : <p className="fridge-mapping-empty">No food matches found. Add a matching food to your Food catalog first.</p>}{mappingError && <p className="fridge-form-error" role="alert">{mappingError}</p>}</section>}
    <button type="button" className="fridge-stock-log-meal" disabled={!item.nutrition || item.grams == null || item.grams <= 0} onClick={onLogMeal}>Log as today&apos;s meal</button>
    <div className="fridge-stock-actions"><button type="button" aria-label={`Decrease ${item.name} by ${formatQuantityStep(item.unit)} ${item.unit}`} title={`Decrease by ${formatQuantityStep(item.unit)}`} onClick={() => onChange(-quantityStep(item.unit))}><Minus aria-hidden="true" /></button><span>Adjust by {formatQuantityStep(item.unit)} {item.unit}</span><button type="button" aria-label={`Increase ${item.name} by ${formatQuantityStep(item.unit)} ${item.unit}`} title={`Increase by ${formatQuantityStep(item.unit)}`} onClick={() => onChange(quantityStep(item.unit))}><Plus aria-hidden="true" /></button></div>
    <button type="button" className="fridge-stock-done" onClick={onClose}>Done</button>
  </div></dialog>
  return portal ? createPortal(editor, portal) : editor
}

type FoodInput = { name: string; brand?: string | null; quantity: number; unit: FridgeUnit; bestBefore: string; gramsPerUnit?: number; catalogKey?: string | null; assetKey?: string | null; assetUrl?: string | null; assetMappingStatus?: FridgeAiPreviewItem["assetMappingStatus"]; assetMappingConfidence?: number | null; catalogFoodId?: string; customFoodId?: string }
type AiDraftItem = FridgeAiPreviewItem & { id: string; bestBefore: string; gramsPerUnit: number | null; selected: boolean; mappingCandidates?: FridgeMappingCandidate[]; mappingCandidate?: FridgeMappingCandidate; mappingSource?: "exact" | "ai" | "user"; mappingAiConfidence?: number | null; mappingLoading?: boolean; mappingError?: string | null }
type BasketItemStyle = CSSProperties & { "--basket-tilt": string; "--basket-scale": string; "--basket-glow": string }
const basketScatter = [
  { x: 2, y: 30, tilt: -11 }, { x: 29, y: 3, tilt: 7 }, { x: 57, y: 25, tilt: 13 },
  { x: 20, y: 57, tilt: -5 }, { x: 52, y: 54, tilt: 9 }, { x: 8, y: 4, tilt: 4 },
  { x: 66, y: 53, tilt: -8 }, { x: 34, y: 36, tilt: 11 }, { x: 4, y: 54, tilt: 6 },
  { x: 58, y: 4, tilt: -13 }, { x: 31, y: 65, tilt: -2 }, { x: 68, y: 32, tilt: 8 },
]
function basketItemStyle(index: number, itemCount: number): BasketItemStyle {
  const position = basketScatter[index % basketScatter.length]
  const scale = itemCount > 9 ? 0.76 : itemCount > 6 ? 0.88 : 1
  const cycle = Math.floor(index / basketScatter.length)
  return {
    left: `${position.x + cycle * 2}%`,
    top: `${position.y + cycle * 2}%`,
    zIndex: index + 1,
    "--basket-tilt": `${position.tilt + cycle * 2}deg`,
    "--basket-scale": String(scale),
    "--basket-glow": "rgba(245, 202, 120, .9)",
  }
}

function sampleFoodEdgeColor(image: HTMLImageElement) {
  try {
    if (!image.complete || !image.naturalWidth || !image.naturalHeight) return null
    if (new URL(image.currentSrc || image.src, window.location.href).origin !== window.location.origin) return null
    const scale = Math.min(1, 128 / Math.max(image.naturalWidth, image.naturalHeight))
    const width = Math.max(1, Math.round(image.naturalWidth * scale))
    const height = Math.max(1, Math.round(image.naturalHeight * scale))
    const canvas = document.createElement("canvas")
    canvas.width = width
    canvas.height = height
    const context = canvas.getContext("2d", { willReadFrequently: true })
    if (!context) return null
    context.drawImage(image, 0, 0, width, height)
    const pixels = context.getImageData(0, 0, width, height).data
    let minX = width
    let minY = height
    let maxX = -1
    let maxY = -1
    for (let y = 0; y < height; y++) {
      for (let x = 0; x < width; x++) {
        if (pixels[(y * width + x) * 4 + 3] < 24) continue
        minX = Math.min(minX, x)
        minY = Math.min(minY, y)
        maxX = Math.max(maxX, x)
        maxY = Math.max(maxY, y)
      }
    }
    if (maxX < minX || maxY < minY) return null
    let red = 0
    let green = 0
    let blue = 0
    let weight = 0
    for (let y = minY; y <= maxY; y++) {
      for (let x = minX; x <= maxX; x++) {
        if (x > minX + 1 && x < maxX - 1 && y > minY + 1 && y < maxY - 1) continue
        const offset = (y * width + x) * 4
        const alpha = pixels[offset + 3]
        if (alpha < 24) continue
        const sampleWeight = alpha / 255
        red += pixels[offset] * sampleWeight
        green += pixels[offset + 1] * sampleWeight
        blue += pixels[offset + 2] * sampleWeight
        weight += sampleWeight
      }
    }
    if (!weight) return null
    return {
      color: `rgba(${Math.round(red / weight)}, ${Math.round(green / weight)}, ${Math.round(blue / weight)}, .6)`,
    }
  } catch {
    return null
  }
}
type SpeechRecognitionLike = {
  lang: string
  continuous: boolean
  interimResults: boolean
  onresult: ((event: { results: ArrayLike<ArrayLike<{ transcript: string }>> }) => void) | null
  onerror: (() => void) | null
  onend: (() => void) | null
  start: () => void
  stop: () => void
}
type SpeechRecognitionConstructor = new () => SpeechRecognitionLike

function AiBasketItem({ item, editing, onToggleEdit, onUpdate, onFindMatches, onSelectMatch, style }: { item: AiDraftItem; editing: boolean; onToggleEdit: () => void; onUpdate: (patch: Partial<AiDraftItem>) => void; onFindMatches: () => void; onSelectMatch: (candidate: FridgeMappingCandidate) => void; style: BasketItemStyle }) {
  const longPressTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const longPressTriggered = useRef(false)
  const lastPointerType = useRef<string | null>(null)
  const glowSampled = useRef(false)
  const [edgeGlow, setEdgeGlow] = useState(style["--basket-glow"])
  const [showMatchOptions, setShowMatchOptions] = useState(false)
  const stackCount = basketStackCount(item)
  function captureFoodEdge(image: HTMLImageElement) {
    if (glowSampled.current) return
    glowSampled.current = true
    const sampled = sampleFoodEdgeColor(image)
    if (sampled) {
      setEdgeGlow(sampled.color)
    }
  }
  const clearLongPress = () => {
    if (longPressTimer.current !== null) {
      clearTimeout(longPressTimer.current)
      longPressTimer.current = null
    }
  }
  useEffect(() => clearLongPress, [])
  function recordPointer(event: ReactPointerEvent<HTMLButtonElement>) {
    lastPointerType.current = event.pointerType
  }
  function startLongPress(event: ReactPointerEvent<HTMLSpanElement>) {
    lastPointerType.current = event.pointerType
    if ((event.pointerType === "mouse" || event.pointerType === "pen") && event.button !== 0) return
    clearLongPress()
    longPressTriggered.current = false
    longPressTimer.current = setTimeout(() => {
      longPressTriggered.current = true
      onToggleEdit()
    }, 520)
  }
  function handleProductClick(event: ReactMouseEvent<HTMLButtonElement>) {
    if (event.detail === 0) {
      onToggleEdit()
      return
    }
    if (longPressTriggered.current) {
      longPressTriggered.current = false
      return
    }
    // Touch users use a press-and-hold to reveal editing; keep a keyboard and desktop path.
    if (lastPointerType.current === "touch" || lastPointerType.current === "pen") return
    onToggleEdit()
  }
  return <article className={`fridge-ai-item${item.selected ? " is-selected" : ""}${editing ? " is-editing" : ""}`} style={{ ...style, "--basket-glow": edgeGlow } as BasketItemStyle}>
    <div className="fridge-ai-basket-card">
      <label className="fridge-ai-item-check"><input type="checkbox" aria-label={`Add ${item.name} to fridge`} checked={item.selected} onChange={event => onUpdate({ selected: event.target.checked })} /></label>
      <div className="fridge-ai-product-wrap"><button type="button" className="fridge-ai-product" onClick={handleProductClick} onPointerDown={recordPointer} onPointerUp={clearLongPress} onPointerCancel={clearLongPress} onContextMenu={event => event.preventDefault()} aria-expanded={editing} aria-label={`${item.name}, ${item.quantity ?? "quantity needed"} ${item.unit ?? "unit needed"}. Long press the image to edit.`}><span className="fridge-ai-item-art" onPointerDown={startLongPress} onPointerUp={clearLongPress} onPointerCancel={clearLongPress} onPointerLeave={clearLongPress}><FoodArtStack count={stackCount} renderFood={() => <AiFoodArt name={item.name} catalogKey={item.catalogKey} assetUrl={item.assetUrl} onImageLoad={captureFoodEdge} />} /></span><span className="fridge-ai-product-details"><span className="fridge-ai-item-copy"><strong>{item.name}</strong>{item.brand && <small>{item.brand}</small>}</span><span className="fridge-ai-product-quantity">{item.quantity == null ? "Set quantity" : `${item.quantity} ${item.unit ?? "unit needed"}`}</span><small className="fridge-ai-confidence">{Math.round(item.confidence * 100)}% match</small></span></button></div>
    </div>
    {editing && <div className="fridge-ai-quick-edit"><label><span>Quantity</span><input aria-label={`${item.name} quantity`} type="number" min="0.01" step={quantityStep(item.unit ?? "pcs")} value={item.quantity ?? ""} onChange={event => onUpdate({ quantity: event.target.value === "" ? null : Number(event.target.value) })} /></label><label><span>Unit</span><select aria-label={`${item.name} unit`} value={item.unit ?? ""} onChange={event => onUpdate({ unit: event.target.value as FridgeUnit, gramsPerUnit: null })}><option value="" disabled>Select unit</option>{units.map(value => <option key={value} value={value}>{value}</option>)}</select></label><label className="fridge-ai-date-edit"><span>Best before</span><input aria-label={`${item.name} best-before date`} type="date" value={item.bestBefore} onChange={event => onUpdate({ bestBefore: event.target.value })} /></label>{item.unit && requiresMeasuredWeight(item.unit) && <label className="fridge-ai-weight-edit"><span>{measuredWeightLabel(item.unit)}</span><input aria-label={`${item.name} ${measuredWeightLabel(item.unit).toLowerCase()}`} type="number" min="0.001" step="any" value={item.gramsPerUnit ?? ""} onChange={event => onUpdate({ gramsPerUnit: event.target.value === "" ? null : Number(event.target.value) })} /></label>}</div>}
    {item.notes.length > 0 && <small className="fridge-ai-item-note">{item.notes.join(" · ")}</small>}
    <div className="fridge-ai-match" aria-label={`Nutrition match for ${item.name}`}>
      <div className="fridge-ai-match-heading"><span><strong>Nutrition match</strong>{item.mappingCandidate ? <small>{item.mappingCandidate.name}{item.mappingCandidate.nutrition?.caloriesKcal != null ? ` · ${item.mappingCandidate.nutrition.caloriesKcal} kcal / 100 g` : ""}{item.mappingSource === "ai" && item.mappingAiConfidence != null ? ` · AI ${Math.round(item.mappingAiConfidence * 100)}%` : item.mappingSource === "exact" ? " · Exact match" : ""}</small> : item.mappingLoading ? <small>Searching food catalog…</small> : item.mappingError ? <small>Catalog suggestions shown</small> : <small>Choose a food to calculate nutrition</small>}</span>{item.mappingCandidate ? <button type="button" className="fridge-ai-match-action" onClick={() => setShowMatchOptions(value => !value)} aria-expanded={showMatchOptions}>{showMatchOptions ? "Close" : "Change"}</button> : <button type="button" className="fridge-ai-match-action" onClick={onFindMatches} disabled={item.mappingLoading}>{item.mappingLoading ? "Matching…" : item.mappingError ? "Retry AI" : "Find match"}</button>}</div>
      {item.mappingError && <p className="fridge-form-error" role="alert">{item.mappingError}</p>}
      {item.mappingCandidates && item.mappingCandidates.length > 0 && (!item.mappingCandidate || showMatchOptions) && <div className="fridge-ai-match-options">{item.mappingCandidates.slice(0, 3).map(candidate => <button type="button" key={`${candidate.kind}:${candidate.id}`} onClick={() => { onSelectMatch(candidate); setShowMatchOptions(false) }}><span><strong>{candidate.name}</strong><small>{candidate.kind === "custom" ? "Your food" : "Nutrition catalog"}{candidate.brand ? ` · ${candidate.brand}` : ""}</small></span><span>{candidate.nutrition?.caloriesKcal == null ? "Nutrition unavailable" : `${candidate.nutrition.caloriesKcal} kcal / 100 g`}</span></button>)}</div>}
      {item.mappingCandidates && item.mappingCandidates.length === 0 && <p className="fridge-ai-match-empty">No matches found. You can still add this item and match it later.</p>}
    </div>
  </article>
}

function FoodEditor({ onClose, onSave, onSaveMany }: { onClose: () => void; onSave: (input: FoodInput) => void; onSaveMany: (inputs: FoodInput[]) => void }) {
  const portal = useMobilePortal()
  const dialog = useRef<HTMLDialogElement>(null)
  const today = useMemo(() => new Date(), [])
  const [name, setName] = useState("Chicken breast")
  const [quantity, setQuantity] = useState("1")
  const [unit, setUnit] = useState<FridgeUnit>("pcs")
  const [gramsPerUnit, setGramsPerUnit] = useState("")
  const [bestBefore, setBestBefore] = useState(dateKey(addDays(today, 5)))
  const [error, setError] = useState<string | null>(null)
  const [captureMode, setCaptureMode] = useState<"manual" | "ai">("manual")
  const [aiInput, setAiInput] = useState("")
  const [aiInputMode, setAiInputMode] = useState<"text" | "voice">("text")
  const [aiLoading, setAiLoading] = useState(false)
  const [listening, setListening] = useState(false)
  const [editingAiId, setEditingAiId] = useState<string | null>(null)
  const [aiPreview, setAiPreview] = useState<{ provider: string; model: string; items: AiDraftItem[]; warnings: readonly string[]; isSample?: boolean } | null>(null)
  const recognition = useRef<SpeechRecognitionLike | null>(null)
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const amount = Number(quantity)
    const measuredWeight = Number(gramsPerUnit)
    if (!name.trim() || !Number.isFinite(amount) || amount <= 0 || !bestBefore) { setError("Enter a name, quantity, and best-before date."); return }
    if (requiresMeasuredWeight(unit) && (!Number.isFinite(measuredWeight) || measuredWeight <= 0)) { setError(`Enter ${measuredWeightLabel(unit).toLowerCase()} to calculate nutrition.`); return }
    onSave({ name: name.trim(), quantity: amount, unit, bestBefore, ...(requiresMeasuredWeight(unit) ? { gramsPerUnit: measuredWeight } : {}) })
  }
  function startVoiceCapture() {
    const constructor = (window as Window & { SpeechRecognition?: SpeechRecognitionConstructor; webkitSpeechRecognition?: SpeechRecognitionConstructor }).SpeechRecognition
      ?? (window as Window & { webkitSpeechRecognition?: SpeechRecognitionConstructor }).webkitSpeechRecognition
    if (!constructor) { setError("Voice input is not available in this browser. You can type the fridge note instead."); return }
    const next = new constructor()
    next.lang = "en-US"
    next.continuous = false
    next.interimResults = false
    next.onresult = event => {
      const transcript = Array.from(event.results).map(result => result[0]?.transcript ?? "").join(" ").trim()
      if (transcript) setAiInput(current => current ? `${current} ${transcript}` : transcript)
      setAiInputMode("voice")
    }
    next.onerror = () => { setListening(false); setError("Voice input could not be captured. Try again or type the note.") }
    next.onend = () => { setListening(false); recognition.current = null }
    recognition.current = next
    setError(null)
    setListening(true)
    next.start()
  }
  function stopVoiceCapture() { recognition.current?.stop(); setListening(false) }
  useEffect(() => () => {
    recognition.current?.stop()
    recognition.current = null
  }, [])
  async function createAiPreview() {
    if (!aiInput.trim()) { setError("Describe what is currently in your fridge first."); return }
    setAiLoading(true); setError(null)
    try {
      const result = await previewFridgeInventory(aiInput.trim(), aiInputMode)
      setAiPreview({
        provider: result.provider,
        model: result.model,
        warnings: result.warnings,
        isSample: false,
        items: result.items.map((item, index) => ({ ...item, id: `ai-${index}-${item.name}`, bestBefore: item.bestBefore ?? "", gramsPerUnit: null, selected: true, mappingLoading: true })),
      })
      await matchAiDrafts(result.items.map(item => ({ name: item.name, brand: item.brand })))
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Could not create an AI preview.") }
    finally { setAiLoading(false) }
  }
  function loadAiSample() {
    const sampleItems = [
      { name: "Milk", quantity: 2, unit: "bottle" as const, confidence: 0.98 },
      { name: "Eggs", quantity: 6, unit: "pcs" as const, confidence: 0.97 },
      { name: "Chicken breast", quantity: 2, unit: "pcs" as const, confidence: 0.96 },
      { name: "Spinach", quantity: 1, unit: "bunch" as const, confidence: 0.94 },
      { name: "Tomatoes", quantity: 3, unit: "pcs" as const, confidence: 0.95 },
    ]
    const fallbackDate = dateKey(addDays(today, 5))
    setAiInput("Two bottles of milk, six eggs, two chicken breasts, one bunch of spinach, and three tomatoes")
    setAiInputMode("text")
    setAiPreview({
      provider: "Sample",
      model: "Fridge preview",
      warnings: [],
      isSample: true,
      items: sampleItems.map((item, index) => ({
        ...item,
        id: `sample-${index}-${item.name}`,
        bestBefore: fallbackDate,
        gramsPerUnit: null,
        selected: true,
        notes: [],
        catalogKey: findFridgeFoodByName(item.name)?.id ?? null,
        assetMappingStatus: "matched",
      })),
    })
    setEditingAiId(null)
    setError(null)
  }
  function updateAiItem(id: string, patch: Partial<AiDraftItem>) {
    const nextPatch = patch.name !== undefined ? {
      ...patch,
      catalogKey: null,
      assetKey: null,
      assetUrl: null,
      assetMappingStatus: "unavailable" as const,
      assetMappingConfidence: null,
      mappingCandidates: undefined,
      mappingCandidate: undefined,
      mappingSource: undefined,
      mappingAiConfidence: null,
      mappingLoading: false,
      mappingError: null,
    } : patch
    setAiPreview(current => current ? { ...current, items: current.items.map(item => item.id === id ? { ...item, ...nextPatch } : item) } : current)
  }
  async function findAiMatches(id: string) {
    const item = aiPreview?.items.find(value => value.id === id)
    if (!item) return
    updateAiItem(id, { mappingLoading: true, mappingError: null })
    try {
      const result = await previewFridgeNutritionMatches([{ name: item.name, brand: item.brand }])
      applyNutritionMatch(id, result.items[0])
    } catch (cause) {
      try {
        const result = await searchFridgeMappings(item.name)
        updateAiItem(id, { mappingLoading: false, mappingCandidates: [...result.candidates], mappingError: cause instanceof Error ? `AI matching is unavailable. Showing catalog results: ${cause.message}` : "AI matching is unavailable. Showing catalog results." })
      } catch (fallbackCause) {
        updateAiItem(id, { mappingLoading: false, mappingError: fallbackCause instanceof Error ? fallbackCause.message : "Could not load nutrition matches. Add now and match later." })
      }
    }
  }
  function orderedCandidates(match: FridgeNutritionMatch) {
    const rank = new Map(match.rankedIds.map((id, index) => [id, index]))
    return [...match.candidates].sort((left, right) => (rank.get(left.id) ?? Number.MAX_SAFE_INTEGER) - (rank.get(right.id) ?? Number.MAX_SAFE_INTEGER))
  }
  function applyNutritionMatch(id: string, match: FridgeNutritionMatch | undefined) {
    if (!match) {
      updateAiItem(id, { mappingLoading: false, mappingError: "AI matching returned no result. Try again or choose a catalog item." })
      return
    }
    const candidates = orderedCandidates(match)
    const selected = match.selectedId ? candidates.find(candidate => candidate.id === match.selectedId) : undefined
    updateAiItem(id, {
      mappingLoading: false,
      mappingError: null,
      mappingCandidates: candidates,
      ...(selected ? { mappingCandidate: selected, mappingSource: match.status === "exact_match" ? "exact" : "ai", mappingAiConfidence: match.confidence } : { mappingCandidate: undefined, mappingSource: undefined, mappingAiConfidence: match.confidence }),
    })
  }
  async function matchAiDrafts(items: readonly { name: string; brand?: string | null }[]) {
    try {
      const preview = await previewFridgeNutritionMatches(items)
      setAiPreview(current => current ? { ...current, items: current.items.map((item, index) => {
        const match = preview.items[index]
        if (!match) return { ...item, mappingLoading: false, mappingError: "AI matching returned no result. Try again or choose a catalog item." }
        const requested = items[index]
        if (!requested || item.name.trim().toLocaleLowerCase() !== requested.name.trim().toLocaleLowerCase()
          || (item.brand?.trim().toLocaleLowerCase() ?? "") !== (requested.brand?.trim().toLocaleLowerCase() ?? "")) {
          return { ...item, mappingLoading: false }
        }
        const candidates = orderedCandidates(match)
        const selected = match.selectedId ? candidates.find(candidate => candidate.id === match.selectedId) : undefined
        const mappingSource = selected ? match.status === "exact_match" ? "exact" : "ai" : undefined
        return { ...item, mappingLoading: false, mappingError: null, mappingCandidates: candidates, mappingCandidate: selected, mappingSource, mappingAiConfidence: match.confidence }
      }) } : current)
    } catch (cause) {
      const uniqueNames = [...new Set(items.map(item => item.name.trim()).filter(Boolean))]
      await Promise.all(uniqueNames.map(async name => {
        try {
          const fallback = await searchFridgeMappings(name)
          const exact = fallback.candidates.filter(candidate => candidate.confidence === 1)
          const selected = exact.length === 1 ? exact[0] : undefined
          const error = cause instanceof Error ? `AI matching is unavailable. Showing catalog results: ${cause.message}` : "AI matching is unavailable. Showing catalog results."
          setAiPreview(current => current ? { ...current, items: current.items.map(draft => draft.name.trim().toLocaleLowerCase() === name.toLocaleLowerCase() ? { ...draft, mappingLoading: false, mappingCandidates: [...fallback.candidates], ...(selected ? { mappingCandidate: selected, mappingSource: "exact" as const } : {}), mappingError: error } : draft) } : current)
        } catch (fallbackCause) {
          const error = fallbackCause instanceof Error ? fallbackCause.message : "Could not load nutrition matches. Add now and match later."
          setAiPreview(current => current ? { ...current, items: current.items.map(draft => draft.name.trim().toLocaleLowerCase() === name.toLocaleLowerCase() ? { ...draft, mappingLoading: false, mappingError: error } : draft) } : current)
        }
      }))
    }
  }
  function confirmAiItems() {
    if (!aiPreview || aiPreview.isSample) return
    const selected = aiPreview.items.filter(item => item.selected)
    const inputs: FoodInput[] = []
    for (const item of selected) {
      const quantity = Number(item.quantity)
      if (!item.name.trim() || item.quantity == null || !Number.isFinite(quantity) || quantity <= 0 || !item.unit || !item.bestBefore) { setError(`Review ${item.name || "the item"}: enter a name, quantity, unit, and best-before date.`); setEditingAiId(item.id); return }
      if (requiresMeasuredWeight(item.unit) && (!Number.isFinite(item.gramsPerUnit) || (item.gramsPerUnit ?? 0) <= 0)) { setError(`Review ${item.name}: enter ${measuredWeightLabel(item.unit).toLowerCase()} for nutrition.`); setEditingAiId(item.id); return }
      inputs.push({
        name: item.name.trim(),
        brand: item.brand,
        quantity,
        unit: item.unit,
        bestBefore: item.bestBefore,
        ...(requiresMeasuredWeight(item.unit) ? { gramsPerUnit: item.gramsPerUnit! } : {}),
        ...(item.catalogKey ? { catalogKey: item.catalogKey } : {}),
        ...(item.assetKey ? { assetKey: item.assetKey } : {}),
        ...(item.assetUrl ? { assetUrl: item.assetUrl } : {}),
        ...(item.assetMappingStatus ? { assetMappingStatus: item.assetMappingStatus } : {}),
        ...(item.assetMappingConfidence !== undefined && item.assetMappingConfidence !== null ? { assetMappingConfidence: item.assetMappingConfidence } : {}),
        ...(item.mappingCandidate?.kind === "catalog" ? { catalogFoodId: item.mappingCandidate.id } : {}),
        ...(item.mappingCandidate?.kind === "custom" ? { customFoodId: item.mappingCandidate.id } : {}),
      })
    }
    if (!inputs.length) { setError("Select at least one item to add."); return }
    onSaveMany(inputs)
  }
  useEffect(() => {
    const element = dialog.current
    element?.showModal()
    return () => element?.close()
  }, [])
  const editor = <dialog ref={dialog} className="fridge-native-dialog" aria-labelledby={aiPreview ? undefined : "fridge-editor-title"} aria-label={aiPreview ? "Review food items" : undefined} onCancel={(event) => { event.preventDefault(); onClose() }} onPointerDownCapture={(event) => { const target = event.target; if (editingAiId && (!(target instanceof Element) || !target.closest(".fridge-ai-item.is-editing"))) setEditingAiId(null) }} onClick={(event) => { if (event.target === event.currentTarget) onClose() }}><div className={`fridge-editor${aiPreview ? " has-ai-preview" : ""}`}>
    <div className="fridge-editor-heading">{!aiPreview && <div><span>Add to cold storage</span><h2 id="fridge-editor-title">Add food</h2></div>}<button type="button" className={`fridge-icon-button${aiPreview ? " fridge-ai-close" : ""}`} aria-label="Close add food" title="Close" onClick={onClose}><X aria-hidden="true" /></button></div>
    <div className="fridge-capture-tabs" role="tablist" aria-label="Add food mode"><button type="button" role="tab" aria-selected={captureMode === "manual"} className={captureMode === "manual" ? "is-active" : ""} onClick={() => { setCaptureMode("manual"); setError(null) }}>Manual</button><button type="button" role="tab" aria-selected={captureMode === "ai"} className={captureMode === "ai" ? "is-active" : ""} onClick={() => { setCaptureMode("ai"); setError(null) }}><Sparkles aria-hidden="true" />AI capture</button></div>
    {captureMode === "manual" && <form className="fridge-editor-form" onSubmit={submit}>
      <label><span>Food name</span><input list="fridge-food-options" required value={name} onChange={(event) => setName(event.target.value)} autoComplete="off" /><datalist id="fridge-food-options">{fridgeFoodCatalog.map((food) => <option key={food.id} value={food.name}>{food.englishName}</option>)}<option value="鸡蛋">Eggs</option><option value="牛奶">Milk</option></datalist></label>
      <div className="fridge-form-columns"><label><span>Quantity</span><input required type="number" min="0.001" step={quantityStep(unit)} inputMode="decimal" value={quantity} onChange={(event) => setQuantity(event.target.value)} /></label><label><span>Unit</span><select value={unit} onChange={(event) => { setUnit(event.target.value as FridgeUnit); setGramsPerUnit("") }}>{units.map((value) => <option key={value} value={value}>{value}</option>)}</select></label></div>
      {requiresMeasuredWeight(unit) && <label><span>{measuredWeightLabel(unit)}</span><input required type="number" min="0.001" step="any" inputMode="decimal" value={gramsPerUnit} onChange={event => setGramsPerUnit(event.target.value)} /><small className="fridge-field-help">Used to calculate consumed grams and nutrition.</small></label>}
      <label><span>Best-before date</span><div className="fridge-date-field"><CalendarDays aria-hidden="true" /><input required type="date" value={bestBefore} onChange={(event) => setBestBefore(event.target.value)} /></div></label>
      {error && <p className="fridge-form-error" role="alert">{error}</p>}
      <div className="fridge-editor-actions"><button type="button" className="fridge-secondary-button" onClick={onClose}>Cancel</button><button type="submit" className="fridge-primary-button"><Check aria-hidden="true" />Add food</button></div>
    </form>}
    {captureMode === "ai" && <div className="fridge-ai-capture">
      {!aiPreview && <>
      <p className="fridge-ai-intro">Describe the food currently in your fridge. AI will prepare a draft for you to review.</p>
      <div className="fridge-ai-note-field"><div className="fridge-ai-note-heading"><label htmlFor="fridge-ai-note">Fridge note</label><button type="button" className="fridge-text-button" onClick={loadAiSample}>Load sample</button></div><textarea id="fridge-ai-note" value={aiInput} onChange={event => { setAiInput(event.target.value); setAiInputMode("text") }} placeholder="Two bottles of milk, six eggs, and chicken breast" rows={3} /></div>
      <div className="fridge-ai-actions"><button type="button" className={`fridge-voice-button${listening ? " is-listening" : ""}`} onClick={listening ? stopVoiceCapture : startVoiceCapture} aria-label={listening ? "Stop voice input" : "Start voice input"} title={listening ? "Stop voice input" : "Start voice input"}><Mic aria-hidden="true" />{listening ? "Listening" : "Voice input"}</button><button type="button" className="fridge-primary-button" onClick={createAiPreview} disabled={aiLoading}>{aiLoading ? <LoaderCircle className="is-spinning" aria-hidden="true" /> : <Sparkles aria-hidden="true" />}{aiLoading ? "Preparing" : "Review with AI"}</button></div>
      </>}
    {aiPreview && <div className="fridge-ai-review" aria-live="polite"><img className="fridge-ai-market-heading" src="/mobile/fridge/supermarket-heading.webp" alt="Supermarket. I spent a nice day." />{aiPreview.warnings.map(warning => <p className="fridge-ai-warning" key={warning}>{warning}</p>)}<div className="fridge-ai-basket" role="group" aria-label="AI identified food in shopping cart"><img className="fridge-ai-basket-art" src="/mobile/fridge/basket-top-view.webp" alt="" aria-hidden="true" /><div className="fridge-ai-basket-handle" aria-hidden="true"><ShoppingCart /></div><div className="fridge-ai-basket-rim" aria-hidden="true" />{aiPreview.items.length > 0 ? <div className="fridge-ai-items">{aiPreview.items.map((item, index) => <AiBasketItem key={item.id} item={item} editing={editingAiId === item.id} onToggleEdit={() => setEditingAiId(current => current === item.id ? null : item.id)} onUpdate={patch => updateAiItem(item.id, patch)} onFindMatches={() => void findAiMatches(item.id)} onSelectMatch={candidate => updateAiItem(item.id, { mappingCandidate: candidate, mappingSource: "user", mappingAiConfidence: null, mappingError: null })} style={basketItemStyle(index, aiPreview.items.length)} />)}</div> : <p className="fridge-ai-basket-empty">No food items were identified. Try a more specific fridge note.</p>}</div>{aiPreview.items.length > 0 && <div className="fridge-ai-quantities" aria-label="Detected food quantities">{aiPreview.items.map(item => <div className={`fridge-ai-quantity-item${item.selected ? "" : " is-excluded"}`} key={item.id} aria-label={`${item.name}: ${item.quantity ?? "quantity needed"} ${item.unit ?? "unit needed"}`}><AiFoodArt name={item.name} catalogKey={item.catalogKey} assetUrl={item.assetUrl} /><span>{item.quantity == null || !item.unit ? "Set amount" : `× ${item.quantity} ${item.unit}`}</span></div>)}</div>}<div className="fridge-editor-actions"><button type="button" className="fridge-secondary-button" onClick={onClose}>Cancel</button><button type="button" className="fridge-primary-button" onClick={confirmAiItems} disabled={!aiPreview.items.length || aiPreview.isSample || aiPreview.items.some(item => item.selected && item.mappingLoading)}>{aiPreview.isSample ? <><Check aria-hidden="true" />Preview only</> : aiPreview.items.some(item => item.selected && item.mappingLoading) ? <><LoaderCircle className="is-spinning" aria-hidden="true" />Matching…</> : <><Check aria-hidden="true" />Add selected</>}</button></div></div>}
      {error && <p className="fridge-form-error" role="alert">{error}</p>}
    </div>}
  </div></dialog>
  return portal ? createPortal(editor, portal) : editor
}

export default function MobileFridgePage({ onLogMeal }: { onLogMeal?: (item: FridgeItem) => void } = {}) {
  const { data, storageError, update, createRemote, adjustRemote, confirmMappingRemote } = useFridgeRecords()
  const [, setAssetRevision] = useState(0)
  const [view, setView] = useState<FridgeView>("overview")
  const [showEditor, setShowEditor] = useState(false)
  const [selectedFoodId, setSelectedFoodId] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const today = dateKey(new Date())
  const items = data.items
  const selectedFood = items.find((item) => item.id === selectedFoodId) ?? null
  const itemWithZones = useMemo(() => items.map((item) => ({ item, zone: shelfFor(item) })), [items])
  const soonCount = items.filter((item) => getFridgeExpiryState(item.bestBefore, today) === "use-soon").length
  const expiringItems = useMemo(() => items
    .filter((item) => getFridgeExpiryState(item.bestBefore, today) === "use-soon")
    .sort((a, b) => parseDate(a.bestBefore).getTime() - parseDate(b.bestBefore).getTime()), [items, today])
  useEffect(() => {
    const controller = new AbortController()
    fetchFoodAssets(controller.signal)
      .then((mappings) => {
        setFridgeFoodAssets(mappings)
        setAssetRevision((revision) => revision + 1)
      })
      .catch(() => {
        // Static asset paths remain a local fallback while Core is unavailable.
      })
    return () => controller.abort()
  }, [])
  useEffect(() => {
    const openEditor = () => setShowEditor(true)
    window.addEventListener("mobile-fridge-add", openEditor)
    return () => window.removeEventListener("mobile-fridge-add", openEditor)
  }, [])
  useEffect(() => {
    function handleBack(event: Event) {
      if (view === "overview") return
      event.preventDefault()
      setSelectedFoodId(null)
      setShowEditor(false)
      setView("overview")
    }
    window.addEventListener("mobile-back", handleBack)
    return () => window.removeEventListener("mobile-back", handleBack)
  }, [view])
  function showNotice(message: string) {
    setNotice(message)
    window.setTimeout(() => setNotice(null), 3200)
  }
  function adjust(item: FridgeItem, delta: number) {
    const idempotencyKey = crypto.randomUUID()
    const snapshot = update((records) => changeFridgeQuantity(records, item.id, delta, new Date().toISOString(), idempotencyKey))
    if (snapshot.storageError) showNotice(snapshot.storageError)
    void adjustRemote(item.id, { quantityDelta: delta, unit: item.unit, idempotencyKey }).catch((error) => {
      showNotice(error instanceof Error ? `Saved on this device. Sync pending: ${error.message}` : "Saved on this device. Sync pending.")
    })
  }
  function addFood(input: FoodInput) {
    const catalog = findFridgeFoodByName(input.name)
    const idempotencyKey = crypto.randomUUID()
    try {
      const localInput = {
        name: input.name,
        ...(input.brand ? { brand: input.brand } : {}),
        quantity: input.quantity,
        unit: input.unit,
        bestBefore: input.bestBefore,
        ...(input.gramsPerUnit !== undefined ? { gramsPerUnit: input.gramsPerUnit } : {}),
        ...(catalog ? { catalogId: catalog.id } : {}),
        ...(input.catalogKey ? { catalogKey: input.catalogKey } : {}),
        ...(input.assetKey ? { assetKey: input.assetKey } : {}),
        ...(input.assetUrl ? { assetUrl: input.assetUrl } : {}),
        ...(input.assetMappingStatus ? { assetMappingStatus: input.assetMappingStatus } : {}),
        ...(input.assetMappingConfidence !== undefined ? { assetMappingConfidence: input.assetMappingConfidence } : {}),
        ...(input.catalogFoodId ? { catalogFoodId: input.catalogFoodId } : {}),
        ...(input.customFoodId ? { customFoodId: input.customFoodId } : {}),
      }
      const snapshot = update((records) => saveFridgeItem(records, localInput, new Date().toISOString(), crypto.randomUUID(), idempotencyKey))
      setShowEditor(false)
      if (snapshot.storageError) showNotice(snapshot.storageError)
      const remoteInput: FridgeAddItemRequest = {
        ...localInput,
        idempotencyKey,
      }
      void createRemote(remoteInput).then((remoteSnapshot) => {
        const added = remoteSnapshot.data.items.find((item) => item.name.trim().toLocaleLowerCase() === input.name.trim().toLocaleLowerCase())
        if (added?.mappingStatus === "needs_mapping" || added?.mappingStatus === "unavailable") {
          showNotice("Food added. Confirm its Food match to calculate nutrition.")
        } else {
          showNotice("Food added to your fridge.")
        }
      }).catch((error) => {
        showNotice(error instanceof Error ? `Saved on this device. Sync pending: ${error.message}` : "Saved on this device. Sync pending.")
      })
    } catch (error) { setNotice(error instanceof Error ? error.message : "Could not add food.") }
  }
  function addFoods(inputs: FoodInput[]) {
    setShowEditor(false)
    inputs.forEach(input => addFood(input))
  }
  return <div className={`mobile-fridge is-${view}`}>
    {storageError && <p className="fridge-alert" role="alert">{storageError}</p>}
    {notice && <p className="fridge-toast" role="status">{notice}</p>}
    {view === "overview" && <>
      <header className="fridge-page-heading"><div className="fridge-heading-row"><div><span className="fridge-eyebrow">PERSONAL COLD STORAGE</span><h1>My fridge</h1></div><button type="button" className="fridge-icon-button" aria-label="View fridge activity" title="View fridge activity" onClick={() => setView("records")}><ClipboardList aria-hidden="true" /></button></div></header>
      <ClosedFridge soonCount={soonCount} />
      <section className="fridge-overview-footer" aria-label="Fridge summary"><div><strong>{items.length}</strong><span>food items</span></div><div><strong className="is-warning">{soonCount}</strong><span>use soon</span></div><button type="button" className="fridge-primary-button fridge-enter-button" onClick={() => setView("inside")}>Open fridge <ArrowRight aria-hidden="true" /></button></section>
      <section className="fridge-expiring-section" aria-labelledby="fridge-expiring-title">
        <div className="fridge-expiring-heading"><div><span className="fridge-expiring-kicker">ATTENTION</span><h2 id="fridge-expiring-title">Use soon</h2></div><span className="fridge-expiring-count" aria-label={`${expiringItems.length} items to use soon`}>{expiringItems.length}</span></div>
        {expiringItems.length ? <div className="fridge-expiring-list">{expiringItems.map((item) => <ExpiringFoodRow key={item.id} item={item} onSelect={() => { setView("inside"); setSelectedFoodId(item.id) }} />)}</div> : <p className="fridge-expiring-empty">Nothing needs using soon.</p>}
      </section>
      <p className="fridge-storage-note">{data.isSample ? "Sample data · saved edits stay on this device" : "On this device · workspace scoped"}</p>
    </>}
    {view === "inside" && <>
      <header className="fridge-inside-heading"><div><span className="fridge-eyebrow">FOOD AT A GLANCE</span><h1>Inside the fridge</h1></div><button type="button" className="fridge-icon-button" aria-label="View fridge activity" title="View fridge activity" onClick={() => setView("records")}><ClipboardList aria-hidden="true" /></button></header>
      <section className="fridge-open-shell" aria-label="Food inside the refrigerator">{shelves.map((shelf) => <div className="fridge-shelf" key={shelf.id}><span className="fridge-shelf-label">{shelf.label}</span><div className="fridge-shelf-items">{itemWithZones.filter(({ zone }) => zone === shelf.id).map(({ item }) => <FridgeCard key={item.id} item={item} today={today} onSelect={() => setSelectedFoodId(item.id)} />)}{!itemWithZones.some(({ zone }) => zone === shelf.id) && <span className="fridge-empty-shelf">Empty shelf</span>}</div></div>)}</section>
    </>}
    {view === "records" && <>
      <header className="fridge-records-heading"><div><span className="fridge-eyebrow">ON THIS DEVICE</span><h1>Fridge activity</h1></div><button type="button" className="fridge-icon-button" aria-label="Back to fridge" title="Back to fridge" onClick={() => setView("overview")}><ArrowLeft aria-hidden="true" /></button></header>
      <section className="fridge-record-list" aria-label="Fridge activity">{data.operations.length ? data.operations.slice(0, 18).map((operation) => <article className="fridge-record-row" key={operation.id}><span className={`fridge-record-icon is-${operation.kind}`} aria-hidden="true">{operation.kind === "remove" ? <Minus /> : operation.kind === "create" ? <Plus /> : <ClipboardList />}</span><div><strong>{operation.itemName} {operation.kind === "create" ? "added" : operation.kind === "remove" ? "used" : "adjusted"}</strong><span>{operation.quantityDelta > 0 ? "+" : ""}{operation.quantityDelta} · {operation.quantityAfter} remaining</span></div><time>{formatWhen(operation.at)}</time></article>) : <div className="fridge-empty-records"><ClipboardList aria-hidden="true" /><strong>No changes yet</strong><span>Add or adjust an item to start your fridge log.</span></div>}</section>
    </>}
    {showEditor && <FoodEditor onClose={() => setShowEditor(false)} onSave={addFood} onSaveMany={addFoods} />}
    {selectedFood && <StockEditor item={selectedFood} today={today} onClose={() => setSelectedFoodId(null)} onChange={(delta) => adjust(selectedFood, delta)} onLogMeal={() => { setSelectedFoodId(null); onLogMeal?.(selectedFood) }} onConfirmMapping={candidate => confirmMappingRemote(selectedFood.id, { candidateId: candidate.id, kind: candidate.kind }).then(() => undefined)} />}
  </div>
}
