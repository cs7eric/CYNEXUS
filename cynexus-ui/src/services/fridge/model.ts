export type FridgeUnit = "pcs" | "kg" | "g" | "mg" | "L" | "ml" | "pack" | "head" | "bunch" | "bottle" | "carton"
export type FridgeExpiryState = "expired" | "use-soon" | "fresh"
export type FridgeOperationKind = "create" | "add" | "remove" | "update"
export type FridgeMappingStatus = "matched" | "needs_mapping" | "user_confirmed" | "unavailable"
export type FridgeNutrition = {
  caloriesKcal: number | null
  carbsG: number | null
  proteinG: number | null
  fatG: number | null
}
export type FridgeMappingCandidate = {
  id: string
  kind: "catalog" | "custom"
  name: string
  brand?: string | null
  confidence?: number | null
  nutrition?: FridgeNutrition | null
}

export type FridgeItem = {
  id: string
  catalogId?: string
  brand?: string | null
  name: string
  quantity: number
  unit: FridgeUnit
  bestBefore: string
  createdAt: string
  updatedAt: string
  catalogFoodId?: string | null
  customFoodId?: string | null
  assetCatalogKey?: string | null
  assetKey?: string | null
  assetUrl?: string | null
  assetMappingStatus?: FridgeMappingStatus
  assetMappingConfidence?: number | null
  mappingStatus?: FridgeMappingStatus
  mappingCandidates?: readonly FridgeMappingCandidate[]
  mappingConfidence?: number | null
  grams?: number | null
  gramsPerUnit?: number | null
  nutrition?: FridgeNutrition | null
  nutritionStatus?: "complete" | "partial" | "unavailable"
  stackCount?: number
  overflowCount?: number
}

export type FridgeItemInput = {
  id?: string
  catalogId?: string
  catalogFoodId?: string
  customFoodId?: string
  brand?: string | null
  catalogKey?: string
  assetKey?: string
  assetUrl?: string
  assetMappingStatus?: FridgeMappingStatus
  assetMappingConfidence?: number | null
  name: string
  quantity: number
  unit: FridgeUnit
  bestBefore: string
  gramsPerUnit?: number
}

export type FridgeOperation = {
  id: string
  itemId: string
  itemName: string
  kind: FridgeOperationKind
  quantityDelta: number
  quantityAfter: number
  at: string
}

export type FridgeRecords = {
  version: 1
  isSample: boolean
  items: FridgeItem[]
  operations: FridgeOperation[]
}

const FRIDGE_UNITS: readonly FridgeUnit[] = ["pcs", "kg", "g", "mg", "L", "ml", "pack", "head", "bunch", "bottle", "carton"]
const OPERATION_KINDS: readonly FridgeOperationKind[] = ["create", "add", "remove", "update"]

export function dateKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`
}

export function parseDate(value: string) {
  const [year, month, day] = value.split("-").map(Number)
  return new Date(year, month - 1, day, 12)
}

export function isDateKey(value: unknown): value is string {
  return typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value) && dateKey(parseDate(value)) === value
}

export function addDays(date: Date, days: number) {
  const next = new Date(date)
  next.setHours(12, 0, 0, 0)
  next.setDate(next.getDate() + days)
  return next
}

export function dayDifference(start: Date, end: Date) {
  return Math.round((Date.UTC(end.getFullYear(), end.getMonth(), end.getDate()) - Date.UTC(start.getFullYear(), start.getMonth(), start.getDate())) / 86400000)
}

export function getFridgeExpiryState(bestBefore: string, today = dateKey(new Date())): FridgeExpiryState {
  if (!isDateKey(bestBefore) || !isDateKey(today)) throw new Error("Enter a valid date.")
  const remaining = dayDifference(parseDate(today), parseDate(bestBefore))
  if (remaining < 0) return "expired"
  if (remaining <= 3) return "use-soon"
  return "fresh"
}

function isTimestamp(value: unknown): value is string {
  return typeof value === "string" && value.trim().length > 0 && Number.isFinite(Date.parse(value))
}

function normalizedName(value: unknown) {
  return typeof value === "string" ? value.trim().replace(/\s+/g, " ") : ""
}

function normalizedQuantity(value: unknown) {
  return typeof value === "number" && Number.isFinite(value) && value >= 0 && value <= 100000
}

export function validateFridgeItem(input: FridgeItemInput) {
  const name = normalizedName(input.name)
  return Boolean(
    name &&
    name.length <= 120 &&
    normalizedQuantity(input.quantity) &&
    FRIDGE_UNITS.includes(input.unit) &&
    isDateKey(input.bestBefore) &&
    (input.id === undefined || (typeof input.id === "string" && input.id.trim().length > 0)) &&
    (input.catalogId === undefined || (typeof input.catalogId === "string" && input.catalogId.trim().length > 0)) &&
    (input.brand === undefined || input.brand === null || (typeof input.brand === "string" && input.brand.trim().length <= 120)) &&
    (input.gramsPerUnit === undefined || (Number.isFinite(input.gramsPerUnit) && input.gramsPerUnit > 0)),
  )
}

function timestamp(value: string) {
  return isTimestamp(value) ? value : new Date().toISOString()
}

function materialize(data: FridgeRecords): FridgeRecords {
  // Fixture rows remain visible on first edit, but fixture history never becomes user history.
  return data.isSample ? { ...data, isSample: false, operations: [] } : data
}

function operation(data: FridgeRecords, item: FridgeItem, kind: FridgeOperationKind, delta: number, at: string, id: string): FridgeRecords {
  if (!id || !Number.isFinite(delta)) return data
  const entry: FridgeOperation = {
    id,
    itemId: item.id,
    itemName: item.name,
    kind,
    quantityDelta: Math.round(delta * 1000) / 1000,
    quantityAfter: item.quantity,
    at,
  }
  return { ...data, operations: [entry, ...data.operations].slice(0, 200) }
}

export function saveFridgeItem(data: FridgeRecords, input: FridgeItemInput, now: string, newItemId: string, newOperationId: string): FridgeRecords {
  if (!validateFridgeItem(input)) throw new Error("Enter a name, quantity, unit, and valid best-before date.")
  if (!newItemId || !newOperationId) throw new Error("Item identifiers are required.")
  const nextData = materialize(data)
  const at = timestamp(now)
  const existingIndex = input.id ? nextData.items.findIndex((item) => item.id === input.id) : -1
  const existing = existingIndex >= 0 ? nextData.items[existingIndex] : null
  const id = existing?.id ?? newItemId
  if (!existing && nextData.items.some((item) => item.id === id)) throw new Error("Item identifier already exists.")
  if (nextData.operations.some((entry) => entry.id === newOperationId)) throw new Error("Operation identifier already exists.")
  const item: FridgeItem = {
    id,
    ...(input.catalogId ? { catalogId: input.catalogId.trim() } : {}),
    ...(input.brand ? { brand: input.brand.trim() } : {}),
    ...(input.catalogKey ? { assetCatalogKey: input.catalogKey.trim() } : {}),
    ...(input.assetKey ? { assetKey: input.assetKey.trim() } : {}),
    ...(input.assetUrl ? { assetUrl: input.assetUrl } : {}),
    ...(input.assetMappingStatus ? { assetMappingStatus: input.assetMappingStatus } : {}),
    ...(input.assetMappingConfidence !== undefined ? { assetMappingConfidence: input.assetMappingConfidence } : {}),
    ...(input.gramsPerUnit !== undefined ? { gramsPerUnit: input.gramsPerUnit } : {}),
    name: normalizedName(input.name),
    quantity: Math.round(input.quantity * 1000) / 1000,
    unit: input.unit,
    bestBefore: input.bestBefore,
    createdAt: existing?.createdAt ?? at,
    updatedAt: at,
  }
  const items = existingIndex >= 0
    ? nextData.items.map((candidate, index) => index === existingIndex ? item : candidate)
    : [item, ...nextData.items]
  const updated: FridgeRecords = { ...nextData, items }
  const delta = item.quantity - (existing?.quantity ?? 0)
  return operation(updated, item, existing ? "update" : "create", delta, at, newOperationId)
}

export function changeFridgeQuantity(data: FridgeRecords, itemId: string, delta: number, now: string, newOperationId: string): FridgeRecords {
  if (!itemId || !Number.isFinite(delta) || delta === 0) return data
  const index = data.items.findIndex((item) => item.id === itemId)
  if (index < 0) return data
  if (!newOperationId) throw new Error("Operation identifier is required.")
  if (data.operations.some((entry) => entry.id === newOperationId)) throw new Error("Operation identifier already exists.")
  const nextData = materialize(data)
  const current = nextData.items[index]
  const quantity = Math.round(Math.max(0, Math.min(100000, current.quantity + delta)) * 1000) / 1000
  const actualDelta = quantity - current.quantity
  if (actualDelta === 0) return nextData
  const item = { ...current, quantity, updatedAt: timestamp(now) }
  const updated = { ...nextData, items: nextData.items.map((candidate, itemIndex) => itemIndex === index ? item : candidate) }
  return operation(updated, item, actualDelta > 0 ? "add" : "remove", actualDelta, item.updatedAt, newOperationId)
}

export function removeFridgeItem(data: FridgeRecords, itemId: string, now: string, newOperationId: string): FridgeRecords {
  const item = data.items.find((candidate) => candidate.id === itemId)
  if (!item) return data
  if (!newOperationId || data.operations.some((entry) => entry.id === newOperationId)) throw new Error("Operation identifier is required and must be unique.")
  const nextData = materialize(data)
  const updated = { ...nextData, items: nextData.items.filter((candidate) => candidate.id !== itemId) }
  return operation(updated, { ...item, quantity: 0 }, "remove", -item.quantity, timestamp(now), newOperationId)
}

function validStoredItem(value: unknown): value is FridgeItem {
  if (!value || typeof value !== "object") return false
  const item = value as Partial<FridgeItem>
  return Boolean(
    typeof item.id === "string" && item.id.trim() &&
    typeof item.name === "string" && normalizedName(item.name).length <= 120 && normalizedName(item.name).length > 0 &&
    normalizedQuantity(item.quantity) && typeof item.unit === "string" && FRIDGE_UNITS.includes(item.unit as FridgeUnit) &&
    isDateKey(item.bestBefore) && isTimestamp(item.createdAt) && isTimestamp(item.updatedAt) &&
    (item.catalogId === undefined || (typeof item.catalogId === "string" && item.catalogId.trim().length > 0))
    && (item.brand === undefined || item.brand === null || (typeof item.brand === "string" && item.brand.trim().length <= 120))
    && (item.gramsPerUnit === undefined || item.gramsPerUnit === null || (Number.isFinite(item.gramsPerUnit) && item.gramsPerUnit > 0))
    && (item.assetCatalogKey === undefined || item.assetCatalogKey === null || (typeof item.assetCatalogKey === "string" && /^food-\d{3}$/.test(item.assetCatalogKey)))
  )
}

function validStoredOperation(value: unknown): value is FridgeOperation {
  if (!value || typeof value !== "object") return false
  const entry = value as Partial<FridgeOperation>
  return Boolean(
    typeof entry.id === "string" && entry.id.trim() && typeof entry.itemId === "string" && entry.itemId.trim() &&
    typeof entry.itemName === "string" && normalizedName(entry.itemName).length > 0 &&
    typeof entry.kind === "string" && OPERATION_KINDS.includes(entry.kind as FridgeOperationKind) &&
    typeof entry.quantityDelta === "number" && Number.isFinite(entry.quantityDelta) &&
    normalizedQuantity(entry.quantityAfter) && isTimestamp(entry.at),
  )
}

export function decodeFridgeRecords(raw: string): FridgeRecords {
  const value: unknown = JSON.parse(raw)
  if (!value || typeof value !== "object") throw new Error("Unsupported fridge records.")
  const data = value as Partial<FridgeRecords>
  if (data.version !== 1 || typeof data.isSample !== "boolean" || !Array.isArray(data.items) || !Array.isArray(data.operations)) throw new Error("Unsupported fridge records.")
  if (data.items.some((item) => !validStoredItem(item))) throw new Error("Invalid fridge items.")
  const ids = new Set(data.items.map((item) => item.id))
  if (ids.size !== data.items.length) throw new Error("Duplicate fridge item IDs.")
  if (data.operations.some((entry) => !validStoredOperation(entry))) throw new Error("Invalid fridge operations.")
  const operationIds = new Set(data.operations.map((entry) => entry.id))
  if (operationIds.size !== data.operations.length) throw new Error("Duplicate fridge operation IDs.")
  return { version: 1, isSample: data.isSample, items: data.items, operations: data.operations }
}

export function createEmptyFridgeRecords(): FridgeRecords {
  return { version: 1, isSample: false, items: [], operations: [] }
}

export function createSampleFridgeRecords(today: Date): FridgeRecords {
  const createdAt = today.toISOString()
  const item = (id: string, catalogId: string | undefined, name: string, quantity: number, unit: FridgeUnit, days: number): FridgeItem => ({
    id,
    ...(catalogId ? { catalogId } : {}),
    name,
    quantity,
    unit,
    bestBefore: dateKey(addDays(today, days)),
    createdAt,
    updatedAt: createdAt,
  })
  return {
    version: 1,
    isSample: true,
    items: [
      item("sample-apples", "food-071", "Apple", 2, "pcs", 6),
      item("sample-strawberries", "food-079", "Strawberries", 1, "pack", -1),
      item("sample-blueberries", "food-089", "Blueberries", 1, "pack", 6),
      item("sample-broccoli", "food-011", "Broccoli", 1, "head", 3),
      item("sample-avocado", "food-100", "Avocado", 1, "pcs", 3),
      item("sample-mushrooms", "food-037", "Mushrooms", 1, "pack", 5),
      item("sample-chicken-breast", "food-052", "Chicken breast", 450, "g", 4),
      item("sample-salmon", "food-061", "Salmon", 2, "pcs", 2),
      item("sample-steak", "food-046", "Steak", 1, "pack", 2),
      item("sample-pork-ribs", "food-042", "Pork ribs", 1, "pack", 4),
      item("sample-potatoes", "food-025", "Potatoes", 2, "pcs", 11),
      item("sample-onion", "food-031", "Onion", 1, "pcs", 20),
      item("sample-pumpkin", "food-022", "Pumpkin", 1, "pcs", 10),
      item("sample-lettuce", "food-003", "Lettuce", 1, "head", 2),
      item("sample-cucumber", "food-013", "Cucumber", 2, "pcs", 5),
      item("sample-tomatoes", "food-015", "Tomatoes", 3, "pcs", 6),
      item("sample-carrots", "food-023", "Carrots", 4, "pcs", 8),
      item("sample-lemon", "food-084", "Lemon", 2, "pcs", 3),
      item("sample-ginger", "food-033", "Ginger", 1, "pcs", 14),
    ],
    operations: [],
  }
}
