import { useCallback, useEffect, useState } from "react"
import { fetchDay, fetchGoals, fetchHistory, fetchCatalog, fetchCustomFoods, fetchFavorites, FOOD_CHANGED } from "@/services/food/api"
import { fetchFridgeInventory } from "@/services/fridge/api"
import type { FridgeItem } from "@/services/fridge/model"
import type { Food, FoodDay, FoodGoal } from "@/services/food/types"

export interface Remote<T> { data: T | null; loading: boolean; error: string | null }
export function useFoodRemote<T>(key: string, load: (signal: AbortSignal) => Promise<T>) {
  const [state, setState] = useState<Remote<T> & { key: string }>({ key: "", data: null, loading: true, error: null })
  const [revision, setRevision] = useState(0)
  const reload = useCallback(() => setRevision(n => n + 1), [])
  useEffect(() => {
    const controller = new AbortController()
    const timer = window.setTimeout(() => {
      setState(previous => ({ key, data: previous.key === key ? previous.data : null, loading: true, error: null }))
      load(controller.signal).then(data => { if (!controller.signal.aborted) setState({ key, data, loading: false, error: null }) })
        .catch(error => { if (!controller.signal.aborted) setState(previous => ({ key, data: previous.key === key ? previous.data : null, loading: false, error: error instanceof Error ? error.message : "Unable to load Food. Try again." })) })
    }, 160)
    return () => { controller.abort(); window.clearTimeout(timer) }
  }, [key, load, revision])
  useEffect(() => { window.addEventListener(FOOD_CHANGED, reload); return () => window.removeEventListener(FOOD_CHANGED, reload) }, [reload])
  return { ...(state.key === key ? state : { data: null, loading: true, error: null }), reload }
}
export function useFoodDay(date: string) {
  const load = useCallback((signal: AbortSignal) => fetchDay(date, signal), [date])
  return useFoodRemote<FoodDay>(date, load)
}
export function useFoodGoals() {
  const load = useCallback((signal: AbortSignal) => fetchGoals(signal), [])
  return useFoodRemote<FoodGoal[]>("goals", load)
}
export function useFoodHistory(start: string, end: string) {
  const load = useCallback((signal: AbortSignal) => fetchHistory(start, end, signal), [start, end])
  return useFoodRemote<FoodDay[]>(`${start}/${end}`, load)
}
export type FoodFilter = "all" | "favorites" | "mine"
export function fridgeItemAsFood(item: FridgeItem): Food {
  const stockNutrition = item.nutrition
  const grams = item.grams
  const per100g = (value: number | null | undefined) => grams != null && grams > 0 && value != null ? value * 100 / grams : null
  const nutrients = {
    caloriesKcal: per100g(stockNutrition?.caloriesKcal),
    carbsG: per100g(stockNutrition?.carbsG),
    proteinG: per100g(stockNutrition?.proteinG),
    fatG: per100g(stockNutrition?.fatG),
  }
  return {
    id: item.catalogFoodId ?? item.customFoodId ?? item.id,
    kind: item.customFoodId ? "custom" : "catalog",
    name: item.name,
    brand: null,
    nutrients,
    source: "My fridge",
    isComplete: item.nutritionStatus === "complete",
    assetKey: item.assetKey ?? item.assetCatalogKey,
    assetUrl: item.assetUrl,
    fridgeItemId: item.id,
    availableQuantity: item.quantity,
    availableUnit: item.unit,
    availableGrams: item.grams,
    fridgeMealReady: item.nutritionStatus !== "unavailable" && item.grams != null && item.grams > 0 && Object.values(nutrients).some(value => value != null),
  }
}
export function useFoodSearch(query: string, filter: FoodFilter) {
  const load = useCallback(async (signal: AbortSignal) => {
    const normalizedQuery = query.trim().toLocaleLowerCase()
    // A direct search always starts with in-stock fridge items, regardless of
    // the secondary library filter. This keeps today's available food visible
    // before catalog and saved-food fallbacks.
    const fridgeInventory = normalizedQuery
      ? await fetchFridgeInventory(signal).catch(() => ({ items: [] }))
      : null
    const fridgeFoods = fridgeInventory
      ? fridgeInventory.items
        .filter(item => item.quantity > 0 && `${item.name} ${item.catalogFoodId ?? ""} ${item.customFoodId ?? ""}`.toLocaleLowerCase().includes(normalizedQuery))
        .map(fridgeItemAsFood)
      : []
    if (filter === "mine") {
      const foods = await fetchCustomFoods(query, signal)
      // A saved food remains selectable independently of fridge stock, even
      // when another item has the same printed name.
      return [...fridgeFoods, ...foods]
    }
    if (filter === "favorites") {
      const foods = await fetchFavorites(signal)
      const filtered = foods.filter(f => `${f.name} ${f.brand || ""}`.toLocaleLowerCase().includes(normalizedQuery))
      return [...fridgeFoods, ...filtered.filter(food => !fridgeFoods.some(fridge => fridge.name.toLocaleLowerCase() === food.name.toLocaleLowerCase()))]
    }
    const [catalog, own, inventory] = await Promise.all([
      query.trim() ? fetchCatalog(query.trim(), signal) : fetchFavorites(signal),
      fetchCustomFoods(query, signal),
      fridgeInventory ?? fetchFridgeInventory(signal).catch(() => ({ items: [] })),
    ])
    const allFridgeFoods: Food[] = inventory.items
      .filter(item => !normalizedQuery || `${item.name} ${item.catalogFoodId ?? ""}`.toLocaleLowerCase().includes(normalizedQuery))
      .filter(item => item.quantity > 0)
      .map(fridgeItemAsFood)
    return [...new Map([...fridgeFoods, ...allFridgeFoods, ...own, ...catalog].map(food => [`${food.fridgeItemId ? `fridge/${food.fridgeItemId}` : `${food.kind}/${food.id}`}`, food])).values()]
  }, [query, filter])
  return useFoodRemote<Food[]>(`${filter}/${query}`, load)
}
