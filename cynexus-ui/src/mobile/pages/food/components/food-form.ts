import { emptyNutrition, nutrientKeys } from "@/services/food/model"
import type { Nutrients } from "@/services/food/types"

export function readNutrients(values: Record<keyof Nutrients, string>): Nutrients {
  const result = Object.fromEntries(nutrientKeys.map(key => [key, values[key].trim() === "" ? null : Number(values[key])])) as Nutrients
  if (nutrientKeys.some(key => result[key] != null && (!Number.isFinite(result[key]) || result[key]! < 0 || result[key]! > 10000000))) throw new Error("Enter valid non-negative nutrition values.")
  if (nutrientKeys.every(key => result[key] == null)) throw new Error("Enter at least one nutrition value.")
  return result
}

export function draftNutrients(values: Nutrients = emptyNutrition()) {
  return Object.fromEntries(nutrientKeys.map(key => [key, values[key] == null ? "" : String(values[key])])) as Record<keyof Nutrients, string>
}
