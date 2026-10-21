export type MealType = "breakfast" | "lunch" | "dinner" | "snack"
export type FoodKind = "catalog" | "custom"
export type NutrientKey = "caloriesKcal" | "carbsG" | "proteinG" | "fatG"
export type Nutrients = Record<NutrientKey, number | null>
export interface Food {
  id: string
  kind: FoodKind
  name: string
  brand: string | null
  nutrients: Nutrients
  source: string | null
  license?: string | null
  usageScope?: string | null
  isComplete?: boolean
  favoriteId?: string | null
  assetKey?: string | null
  assetUrl?: string | null
  /** Present when this search result comes from current fridge inventory. */
  fridgeItemId?: string | null
  availableQuantity?: number | null
  availableUnit?: string | null
  availableGrams?: number | null
  fridgeMealReady?: boolean
}
export interface FoodAssetMapping {
  /** Stable UI catalog key, for example food-201. */
  catalogKey: string
  catalogFoodId?: string | null
  assetKey: string
  assetUrl: string
  sourceAtlas?: string | null
  row?: number | null
  column?: number | null
  printedLabel?: string | null
}
export interface DiaryItem {
  id: string
  mealType: MealType
  entryKind: FoodKind | "quick"
  foodId: string | null
  foodName: string
  brand: string | null
  quantity: number | null
  unit: string | null
  gramsPerUnit: number | null
  consumedGrams: number | null
  per100g: Nutrients
  nutrients: Nutrients
  assetKey?: string | null
  sourceFridgeItemId?: string | null
  sourceFridgeQuantity?: number | null
  sourceFridgeUnit?: string | null
}
export interface FoodDay {
  date: string
  status: "open" | "complete"
  timezone: string
  targets: Nutrients
  totals: Nutrients
  knownTotals: Nutrients
  missingCounts: Record<NutrientKey, number>
  items: DiaryItem[]
}
export interface FoodGoal {
  id: string
  effectiveFrom: string
  nutrients: Nutrients
}
export interface EntryInput {
  idempotencyKey: string
  timezone: string
  mealType: MealType
  entryKind: FoodKind | "quick"
  foodId?: string
  foodName?: string
  quantity?: number
  unit?: string
  gramsPerUnit?: number
  nutrients?: Nutrients
}
export interface EditInput {
  mealType?: MealType
  quantity?: number
  unit?: string
  gramsPerUnit?: number
  nutrients?: Nutrients
}
