import { cynexusResult } from "@/services/common/cynexus-result"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"
import type { EditInput, EntryInput, Food, FoodAssetMapping, FoodDay, FoodGoal, FoodKind, MealType, Nutrients } from "./types"

const API = `${SERVICE_PREFIX.core}/food`
export const FOOD_CHANGED = "cynexus:food-changed"
async function request<T>(path: string, method = "GET", body?: unknown, signal?: AbortSignal): Promise<T> {
  return cynexusResult<T>(await fetch(`${API}${path}`, {
    method, cache: "no-store", signal,
    ...(body === undefined ? {} : { headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) }),
  }))
}
export const fetchDay = (date: string, signal?: AbortSignal) => request<FoodDay>(`/days/${date}`, "GET", undefined, signal)
export const fetchCatalog = (query: string, signal?: AbortSignal) => request<Food[]>(`/catalog?${new URLSearchParams({ q: query, limit: "30" })}`, "GET", undefined, signal)
export const fetchFoodAssets = (signal?: AbortSignal) => request<FoodAssetMapping[]>("/assets", "GET", undefined, signal)
export const fetchCustomFoods = async (query: string, signal?: AbortSignal) => (await request<Food[]>("/custom-foods", "GET", undefined, signal)).filter(food => `${food.name} ${food.brand ?? ""}`.toLocaleLowerCase().includes(query.trim().toLocaleLowerCase()))
export const fetchFavorites = (signal?: AbortSignal) => request<Food[]>("/favorites", "GET", undefined, signal)
export const fetchGoals = (signal?: AbortSignal) => request<FoodGoal[]>("/goals", "GET", undefined, signal)
export const fetchHistory = (start: string, end: string, signal?: AbortSignal) => request<FoodDay[]>(`/history?${new URLSearchParams({ start, end })}`, "GET", undefined, signal)
export const createItem = (date: string, data: EntryInput) => request<FoodDay>(`/days/${date}/items`, "POST", data)
export type FridgeMealInput = {
  idempotencyKey: string
  timezone: string
  mealType: MealType
  fridgeItemId: string
  quantity: number
  unit: string
}
export const createFridgeMeal = (date: string, data: FridgeMealInput) => request<FoodDay>(`/days/${date}/fridge-items`, "POST", data)
export const updateItem = (date: string, id: string, data: EditInput) => request<FoodDay>(`/days/${date}/items/${encodeURIComponent(id)}`, "PUT", data)
export const deleteItem = (date: string, id: string) => request<FoodDay>(`/days/${date}/items/${encodeURIComponent(id)}`, "DELETE")
export const setDayStatus = (date: string, complete: boolean) => request<FoodDay>(`/days/${date}/${complete ? "complete" : "reopen"}`, "POST", { timezone: Intl.DateTimeFormat().resolvedOptions().timeZone })
export const createCustomFood = (data: { idempotencyKey: string; name: string; brand?: string; nutrients: Nutrients; assetKey?: string; assetImageDataUrl?: string }) => request<Food>("/custom-foods", "POST", data)
export const createGoal = (data: { idempotencyKey: string; effectiveFrom: string; nutrients: Nutrients }) => request<FoodGoal[]>("/goals", "POST", data)
export const addFavorite = (kind: FoodKind, foodId: string, idempotencyKey: string) => request<Food>("/favorites", "POST", { kind, foodId, idempotencyKey })
export const deleteFavorite = (id: string) => request<void>(`/favorites/${encodeURIComponent(id)}`, "DELETE")
export function announceFoodChange() { window.dispatchEvent(new Event(FOOD_CHANGED)) }
