import { formatNutrition, nutrientKeys } from "@/services/food/model"
import type { DiaryItem } from "@/services/food/types"

export function foodGroupKey(item: DiaryItem) {
  if (!item.foodId || item.entryKind === "quick" || !(item.consumedGrams != null && item.consumedGrams > 0)) return `entry:${item.id}`
  return JSON.stringify([item.entryKind, item.foodId, item.foodName, item.brand, item.unit, item.gramsPerUnit, ...nutrientKeys.map(key => item.per100g[key])])
}

export function groupFoods(items: DiaryItem[]) {
  const groups = new Map<string, DiaryItem[]>()
  for (const item of items) {
    const key = foodGroupKey(item)
    const group = groups.get(key)
    if (group) group.push(item)
    else groups.set(key, [item])
  }
  return [...groups].map(([key, entries]) => ({ key, entries }))
}

export function foodAmount(entries: DiaryItem[]) {
  const first = entries[0]
  if (entries.every(item => item.unit && item.unit !== "g" && item.unit === first.unit && item.quantity != null && item.quantity > 0)) {
    return `${formatNutrition(entries.reduce((sum, item) => sum + item.quantity!, 0), 1)} ${first.unit}`
  }
  if (entries.every(item => item.consumedGrams != null)) return `${formatNutrition(entries.reduce((sum, item) => sum + item.consumedGrams!, 0), 1)} g`
  return entries.length === 1 ? "Quick entry" : `${entries.length} entries`
}
