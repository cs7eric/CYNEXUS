export type NutritionSex = "male" | "female"

export type NutritionConfig = {
  sex: NutritionSex | null
  bodyFatPct: number | null
  ageYears: number | null
  activityFactor: number | null
  calorieDeficit: number | null
  carbsMultiplier: number
  proteinMultiplier: number
  fatMultiplier: number
}

export type NutritionCalculation = {
  weightKg: number | null
  carbsG: number | null
  proteinG: number | null
  fatG: number | null
  bmrKcal: number | null
  tdeeKcal: number | null
  caloriesKcal: number | null
  missing: string[]
}

export const ACTIVITY_LEVELS = [
  { value: 1.2, label: "Sedentary · little or no exercise" },
  { value: 1.375, label: "Light activity · low intensity exercise" },
  { value: 1.55, label: "Moderate activity · regular exercise" },
  { value: 1.725, label: "High activity · strength and cardio" },
  { value: 1.9, label: "Very high activity · physical work and exercise" },
] as const

export const CALORIE_DEFICITS = [
  { value: 500, label: "Slow fat loss · 500 kcal deficit" },
  { value: 800, label: "Fast fat loss · 800 kcal deficit" },
] as const

export const DEFAULT_NUTRITION_CONFIG: NutritionConfig = {
  sex: null,
  bodyFatPct: null,
  ageYears: null,
  activityFactor: null,
  calorieDeficit: 500,
  carbsMultiplier: 3,
  proteinMultiplier: 1.7,
  fatMultiplier: 0.9,
}

const MAX_MULTIPLIER = 20
const API = "/api/core/food/nutrition-config"

function finiteOrNull(value: unknown) {
  return typeof value === "number" && Number.isFinite(value) ? value : null
}

function boundedNumber(value: unknown, fallback: number, min: number, max: number) {
  const parsed = finiteOrNull(value)
  return parsed != null && parsed >= min && parsed <= max ? parsed : fallback
}

function normalizeConfig(raw: unknown): NutritionConfig {
  const source = raw && typeof raw === "object" ? raw as Partial<NutritionConfig> : {}
  return {
    sex: source.sex === "male" || source.sex === "female" ? source.sex : DEFAULT_NUTRITION_CONFIG.sex,
    bodyFatPct: source.bodyFatPct == null ? null : boundedNumber(source.bodyFatPct, 0, 0, 100),
    ageYears: source.ageYears == null ? null : boundedNumber(source.ageYears, 1, 1, 120),
    activityFactor: source.activityFactor == null ? null : boundedNumber(source.activityFactor, 1.2, 1.1, 2.5),
    calorieDeficit: source.calorieDeficit == null ? DEFAULT_NUTRITION_CONFIG.calorieDeficit : boundedNumber(source.calorieDeficit, 500, 0, 5000),
    carbsMultiplier: boundedNumber(source.carbsMultiplier, DEFAULT_NUTRITION_CONFIG.carbsMultiplier, 0, MAX_MULTIPLIER),
    proteinMultiplier: boundedNumber(source.proteinMultiplier, DEFAULT_NUTRITION_CONFIG.proteinMultiplier, 0, MAX_MULTIPLIER),
    fatMultiplier: boundedNumber(source.fatMultiplier, DEFAULT_NUTRITION_CONFIG.fatMultiplier, 0, MAX_MULTIPLIER),
  }
}

export async function fetchNutritionConfig(signal?: AbortSignal): Promise<NutritionConfig> {
  const response = await fetch(API, { method: "GET", cache: "no-store", signal })
  const body = await response.text()
  let payload: { data?: unknown; message?: string; success?: boolean } | null = null
  try { payload = body ? JSON.parse(body) as { data?: unknown; message?: string; success?: boolean } : null } catch { /* handled below */ }
  if (!response.ok || !payload || payload.success === false) throw new Error(payload?.message || `Unable to load nutrition configuration (HTTP ${response.status})`)
  return normalizeConfig(payload.data)
}

export async function persistNutritionConfig(config: NutritionConfig, signal?: AbortSignal): Promise<NutritionConfig> {
  const normalized = normalizeConfig(config)
  const response = await fetch(API, {
    method: "PUT",
    cache: "no-store",
    signal,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(normalized),
  })
  const body = await response.text()
  let payload: { data?: unknown; message?: string; success?: boolean } | null = null
  try { payload = body ? JSON.parse(body) as { data?: unknown; message?: string; success?: boolean } : null } catch { /* handled below */ }
  if (!response.ok || !payload || payload.success === false) throw new Error(payload?.message || `Unable to save nutrition configuration (HTTP ${response.status})`)
  return normalizeConfig(payload.data)
}

export function calculateNutritionTargets(weightKg: number | null, config: NutritionConfig): NutritionCalculation {
  const weight = finiteOrNull(weightKg)
  const validWeight = weight != null && weight > 0 && weight <= 600 ? weight : null
  const carbsG = validWeight == null ? null : validWeight * config.carbsMultiplier
  const proteinG = validWeight == null ? null : validWeight * config.proteinMultiplier
  const fatG = validWeight == null ? null : validWeight * config.fatMultiplier
  const missing: string[] = []
  if (validWeight == null) missing.push("weight")
  if (config.sex == null) missing.push("sex")
  if (config.bodyFatPct == null) missing.push("body fat")
  if (config.ageYears == null) missing.push("age")
  if (config.activityFactor == null) missing.push("activity")
  if (config.calorieDeficit == null) missing.push("calorie deficit")
  if (validWeight == null || config.sex == null || config.bodyFatPct == null || config.ageYears == null || config.activityFactor == null || config.calorieDeficit == null) {
    return { weightKg: validWeight, carbsG, proteinG, fatG, bmrKcal: null, tdeeKcal: null, caloriesKcal: null, missing }
  }
  const leanMass = validWeight * (1 - config.bodyFatPct / 100)
  const fatMass = validWeight * (config.bodyFatPct / 100)
  const bmrKcal = 13.587 * leanMass + 9.613 * fatMass + (config.sex === "male" ? 198 : 0) - 3.351 * config.ageYears + 674
  const tdeeKcal = bmrKcal * config.activityFactor
  return { weightKg: validWeight, carbsG, proteinG, fatG, bmrKcal, tdeeKcal, caloriesKcal: Math.max(0, tdeeKcal - config.calorieDeficit), missing }
}

export function formatNutritionValue(value: number | null, fractionDigits = 0) {
  return value == null ? "—" : value.toLocaleString("en-US", { maximumFractionDigits: fractionDigits, minimumFractionDigits: fractionDigits })
}
