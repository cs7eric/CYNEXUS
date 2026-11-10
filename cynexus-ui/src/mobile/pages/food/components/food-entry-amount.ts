import type { Food } from "@/services/food/types"

export type FoodAmountUnit = "g" | "ml" | "pcs"

type FridgeFood = Pick<Food, "fridgeItemId" | "availableQuantity" | "availableUnit" | "availableGrams">

function hasFridgeStock(food: FridgeFood) {
  return !!food.fridgeItemId && food.availableQuantity != null && food.availableQuantity > 0
}

function totalStockGrams(food: FridgeFood) {
  if (!hasFridgeStock(food)) return null
  if (food.availableGrams != null && food.availableGrams >= 0) return food.availableGrams
  if (food.availableUnit === "g") return food.availableQuantity!
  if (food.availableUnit === "kg") return food.availableQuantity! * 1000
  if (food.availableUnit === "mg") return food.availableQuantity! / 1000
  return null
}

function gramsPerStockUnit(food: FridgeFood) {
  if (!hasFridgeStock(food)) return null
  if (food.availableUnit === "g") return 1
  if (food.availableUnit === "kg") return 1000
  if (food.availableUnit === "mg") return 0.001
  const grams = totalStockGrams(food)
  return grams != null && grams > 0 ? grams / food.availableQuantity! : null
}

export function foodAmountUnits(food?: Food): FoodAmountUnit[] {
  if (!food?.fridgeItemId) return ["g"]
  const units: FoodAmountUnit[] = []
  if (food.availableUnit === "g" || food.availableUnit === "kg" || food.availableUnit === "mg" || (food.availableGrams != null && food.availableGrams > 0)) units.push("g")
  if (food.availableUnit === "ml" || (food.availableUnit === "L" && gramsPerStockUnit(food) != null)) units.push("ml")
  if (food.availableUnit === "pcs") units.push("pcs")
  return units
}

export function initialFoodAmountUnit(food?: Food): FoodAmountUnit {
  const units = foodAmountUnits(food)
  if ((food?.availableUnit === "ml" || food?.availableUnit === "L") && units.includes("ml")) return "ml"
  if (food?.availableUnit === "pcs" && units.includes("pcs")) return "pcs"
  return "g"
}

export function gramsPerFoodAmountUnit(food: Food | undefined, unit: FoodAmountUnit, pcsGrams?: number | null) {
  if (unit === "g") return 1
  if (!food?.fridgeItemId) return null
  if (unit === "pcs" && pcsGrams != null && Number.isFinite(pcsGrams) && pcsGrams > 0) return pcsGrams
  const stockGramsPerUnit = gramsPerStockUnit(food)
  if (stockGramsPerUnit == null) return null
  if (unit === "ml" && food.availableUnit === "ml") return stockGramsPerUnit
  if (unit === "ml" && food.availableUnit === "L") return stockGramsPerUnit / 1000
  if (unit === "pcs" && food.availableUnit === "pcs") return stockGramsPerUnit
  return null
}

export function availableFoodAmount(food: Food | undefined, unit: FoodAmountUnit, pcsGrams?: number | null) {
  if (!food?.fridgeItemId) return null
  if (unit === "ml" && food.availableUnit === "ml") return food.availableQuantity ?? null
  if (unit === "ml" && food.availableUnit === "L") {
    const gramsPerMl = gramsPerFoodAmountUnit(food, unit)
    const grams = totalStockGrams(food)
    return gramsPerMl != null && grams != null ? grams / gramsPerMl : null
  }
  if (unit === "pcs" && food.availableUnit === "pcs") return food.availableQuantity ?? null
  if (unit === "g" && (food.availableUnit === "g" || food.availableUnit === "kg" || food.availableUnit === "mg")) return totalStockGrams(food)
  const gramsPerUnit = gramsPerFoodAmountUnit(food, unit, pcsGrams)
  const stockGrams = totalStockGrams(food)
  return gramsPerUnit != null && stockGrams != null ? stockGrams / gramsPerUnit : null
}

export function initialFoodAmount(food?: Food) {
  if (!food?.fridgeItemId) return 100
  const unit = initialFoodAmountUnit(food)
  const available = availableFoodAmount(food, unit)
  const base = unit === "pcs" ? 1 : 100
  return available == null ? (foodAmountUnits(food).length ? base : 0) : Math.min(base, available)
}

export function foodAmountInGrams(food: Food | undefined, amount: number, unit: FoodAmountUnit, pcsGrams?: number | null) {
  if (!food?.fridgeItemId) return unit === "g" ? amount : null
  const gramsPerUnit = gramsPerFoodAmountUnit(food, unit, pcsGrams)
  return gramsPerUnit == null ? null : amount * gramsPerUnit
}

/**
 * Build the quantity/unit pair accepted by the fridge meal endpoint.
 * Piece counts stay in the inventory unit, with an optional per-piece weight
 * used to calculate consumed nutrition when the user supplies one.
 */
export function fridgeMealQuantityAndUnit(food: Food, amount: number, unit: FoodAmountUnit, pcsGrams?: number | null) {
  if (!Number.isFinite(amount) || amount <= 0) return null
  if (unit === "pcs" && food.availableUnit === "pcs") {
    return { quantity: amount, unit: food.availableUnit, ...(pcsGrams != null ? { gramsPerUnit: pcsGrams } : {}) }
  }
  if (unit === food.availableUnit) return { quantity: amount, unit: food.availableUnit }
  const grams = foodAmountInGrams(food, amount, unit, pcsGrams)
  return grams == null || grams <= 0 ? null : { quantity: grams, unit: "g" }
}
