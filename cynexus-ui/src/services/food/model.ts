import type { DiaryItem, FoodDay, MealType, NutrientKey, Nutrients } from "./types"

export const nutrientKeys: NutrientKey[] = ["caloriesKcal", "carbsG", "proteinG", "fatG"]
export const macroKeys: NutrientKey[] = ["carbsG", "proteinG", "fatG"]
export const nutrientNames: Record<NutrientKey, string> = { caloriesKcal: "Energy", carbsG: "Carbs", proteinG: "Protein", fatG: "Fat" }
export const meals = [
  { id: "breakfast", name: "Breakfast", icon: "sun" },
  { id: "lunch", name: "Lunch", icon: "bowl" },
  { id: "dinner", name: "Dinner", icon: "moon" },
  { id: "snack", name: "Snacks", icon: "apple" },
] as const
/** Pick a useful meal bucket for a new entry without forcing the user to decide first. */
export function defaultMealType(date = new Date()): MealType {
  const hour = date.getHours()
  if (hour >= 5 && hour < 11) return "breakfast"
  if (hour >= 11 && hour < 15) return "lunch"
  if (hour >= 15 && hour < 18) return "snack"
  if (hour >= 18 && hour < 22) return "dinner"
  return "snack"
}
export const emptyNutrition = (): Nutrients => ({ caloriesKcal: null, carbsG: null, proteinG: null, fatG: null })
export function formatNutrition(value: number | null | undefined, decimals = 0) { return value == null || !Number.isFinite(value) ? "—" : value.toLocaleString("en-US", { maximumFractionDigits: decimals }) }
export function dateKey(date: Date) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}` }
export function parseDate(key: string) { return new Date(`${key}T12:00:00`) }
export function addDays(key: string, n: number) { const date = parseDate(key); date.setDate(date.getDate() + n); return dateKey(date) }
export function readableDate(key: string, full = false) { return parseDate(key).toLocaleDateString("en-GB", full ? { weekday: "long", day: "numeric", month: "long" } : { day: "numeric", month: "short" }) }
export function sumNutrition(items: DiaryItem[]) {
  const knownTotals = emptyNutrition(), totals = emptyNutrition(), missingCounts = {} as FoodDay["missingCounts"]
  for (const key of nutrientKeys) {
    const known = items.filter(i => i.nutrients[key] != null)
    missingCounts[key] = items.length - known.length
    knownTotals[key] = !items.length ? 0 : known.length ? known.reduce((sum, i) => sum + i.nutrients[key]!, 0) : null
    totals[key] = missingCounts[key] ? null : knownTotals[key]
  }
  return { totals, knownTotals, missingCounts }
}
export function emptyDay(date: string): FoodDay { return { date, status: "open", timezone: Intl.DateTimeFormat().resolvedOptions().timeZone, targets: emptyNutrition(), ...sumNutrition([]), items: [] } }
export function scaledNutrition(values: Nutrients, grams: number): Nutrients { return Object.fromEntries(nutrientKeys.map(k => [k, values[k] == null ? null : values[k]! * grams / 100])) as Nutrients }
export type HistoryPeriod = "week" | "month" | "year"
export function historyBounds(anchor: string, period: HistoryPeriod) {
  const d = parseDate(anchor)
  if (period === "week") return { start: addDays(anchor, -6), end: anchor }
  return period === "month" ? { start: dateKey(new Date(d.getFullYear(), d.getMonth(), 1)), end: dateKey(new Date(d.getFullYear(), d.getMonth() + 1, 0)) }
    : { start: `${d.getFullYear()}-01-01`, end: `${d.getFullYear()}-12-31` }
}
