import { cynexusResult } from "@/services/common/cynexus-result"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"
import type { FridgeItem, FridgeItemInput, FridgeOperation, FridgeRecords, FridgeUnit } from "./model"

/**
 * The fridge API is deliberately kept separate from the local fixture store.
 * This lets mobile use the same optimistic UI while the workspace API is
 * unavailable (for example while the user is offline or during rollout).
 */
const API = `${SERVICE_PREFIX.core}/food/fridge`

export type FridgeMappingStatus = "matched" | "needs_mapping" | "user_confirmed" | "unavailable"
export type FridgeNutrition = {
  caloriesKcal: number | null
  carbsG: number | null
  proteinG: number | null
  fatG: number | null
}

export type FridgeApiItem = FridgeItem & {
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

export type FridgeMappingCandidate = {
  id: string
  kind: "catalog" | "custom"
  name: string
  brand?: string | null
  confidence?: number | null
  nutrition?: FridgeNutrition | null
}

export type FridgeApiInventory = {
  version?: number
  isSample?: boolean
  items: readonly FridgeApiItem[]
  operations?: readonly FridgeOperation[]
}

export type FridgeAddItemRequest = FridgeItemInput & {
  idempotencyKey: string
  brand?: string
  catalogFoodId?: string
  customFoodId?: string
  catalogKey?: string
  gramsPerUnit?: number
}

export type FridgeAiPreviewItem = {
  name: string
  brand?: string | null
  quantity: number | null
  unit: FridgeUnit | null
  bestBefore?: string | null
  confidence: number
  notes: readonly string[]
  catalogKey?: string | null
  assetKey?: string | null
  assetUrl?: string | null
  assetMappingStatus?: FridgeMappingStatus | null
  assetMappingConfidence?: number | null
}

export type FridgeAiPreview = {
  provider: string
  model: string
  inputMode: "text" | "voice"
  items: readonly FridgeAiPreviewItem[]
  warnings: readonly string[]
}

export type FridgeAdjustRequest = {
  quantityDelta: number
  unit?: FridgeUnit
  idempotencyKey: string
  source?: "manual" | "meal"
  mealItemId?: string
}

export type FridgeMappingRequest = {
  candidateId: string
  kind: "catalog" | "custom"
}

export type FridgeMappingSearch = {
  query: string
  candidates: readonly FridgeMappingCandidate[]
}

export type FridgeNutritionMatchInput = {
  name: string
  brand?: string | null
}

export type FridgeNutritionMatch = {
  name: string
  brand?: string | null
  status: "exact_match" | "ai_suggested" | "needs_review" | "unmatched"
  selectedId: string | null
  confidence: number | null
  candidates: readonly FridgeMappingCandidate[]
  rankedIds: readonly string[]
  requiresConfirmation: boolean
}

export type FridgeNutritionMatchPreview = {
  provider: string | null
  model: string | null
  items: readonly FridgeNutritionMatch[]
}

async function request<T>(path: string, method = "GET", body?: unknown, signal?: AbortSignal): Promise<T> {
  return cynexusResult<T>(await fetch(`${API}${path}`, {
    method,
    cache: "no-store",
    signal,
    ...(body === undefined ? {} : { headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) }),
  }))
}

function unwrapInventory(value: FridgeApiInventory | { inventory: FridgeApiInventory } | FridgeRecords): FridgeApiInventory {
  if ("inventory" in value && value.inventory) return value.inventory
  return value as FridgeApiInventory
}

export async function fetchFridgeInventory(signal?: AbortSignal) {
  return unwrapInventory(await request<FridgeApiInventory | { inventory: FridgeApiInventory }>("/inventory", "GET", undefined, signal))
}

export async function addFridgeItem(data: FridgeAddItemRequest, signal?: AbortSignal) {
  return unwrapInventory(await request<FridgeApiInventory | { inventory: FridgeApiInventory }>("/items", "POST", data, signal))
}

export async function previewFridgeInventory(input: string, inputMode: "text" | "voice", signal?: AbortSignal) {
  return request<FridgeAiPreview>("/ai/preview", "POST", { input, inputMode }, signal)
}

export async function adjustFridgeItem(itemId: string, data: FridgeAdjustRequest, signal?: AbortSignal) {
  return unwrapInventory(await request<FridgeApiInventory | { inventory: FridgeApiInventory }>(`/items/${encodeURIComponent(itemId)}/adjust`, "POST", data, signal))
}

export async function confirmFridgeItemMapping(itemId: string, data: FridgeMappingRequest, signal?: AbortSignal) {
  return unwrapInventory(await request<FridgeApiInventory | { inventory: FridgeApiInventory }>(`/items/${encodeURIComponent(itemId)}/mapping`, "POST", data, signal))
}

export function searchFridgeMappings(query: string, signal?: AbortSignal) {
  return request<FridgeMappingSearch>(`/mapping/search?${new URLSearchParams({ q: query.trim() })}`, "GET", undefined, signal)
}

export function previewFridgeNutritionMatches(items: readonly FridgeNutritionMatchInput[], signal?: AbortSignal) {
  return request<FridgeNutritionMatchPreview>("/ai/mapping-preview", "POST", { items }, signal)
}

/** Convert API inventory into the shape consumed by the existing mobile view. */
export function normalizeFridgeInventory(inventory: FridgeApiInventory): FridgeRecords {
  return {
    version: 1,
    isSample: inventory.isSample === true,
    items: inventory.items.map((item) => ({
      ...item,
      ...(isVisualCatalogKey(item.assetCatalogKey) ? { assetCatalogKey: item.assetCatalogKey } : { assetCatalogKey: null }),
      mappingStatus: item.mappingStatus,
      mappingCandidates: item.mappingCandidates,
      mappingConfidence: item.mappingConfidence,
      grams: item.grams,
      gramsPerUnit: item.gramsPerUnit,
      nutrition: item.nutrition,
      nutritionStatus: item.nutritionStatus,
      stackCount: item.stackCount,
      overflowCount: item.overflowCount,
    })),
    operations: [...(inventory.operations ?? [])],
  }
}

function isVisualCatalogKey(value: string | null | undefined): value is string {
  return typeof value === "string" && /^food-\d{3}$/.test(value)
}
