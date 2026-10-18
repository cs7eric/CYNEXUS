export interface DailyNutritionTargets {
  calories: number | null
  carbsGrams: number | null
  proteinGrams: number | null
  fatGrams: number | null
}

export interface DailyActivity {
  date: string
  nutritionTargets: DailyNutritionTargets | null
  nutrition: {
    calories: number
    meals: number
    carbsGrams: number | null
    proteinGrams: number | null
    fatGrams: number | null
  } | null
  workout: { minutes: number; sessions: number; label: string } | null
}

export interface BodyMeasurementSnapshot {
  recordedAt: string
  heightCm: number | null
  weightKg: number | null
  chestCm: number | null
  waistCm: number | null
  upperArmCm: number | null
  forearmCm: number | null
}

export type FinanceSummarySnapshot =
  | { status: "loading" }
  | { status: "error"; error?: string }
  | { status: "empty" }
  | { status: "ready"; netAssets: number; income: number; expense: number; net: number; currency: string | null }

export function calculateBmi(weightKg: number | null, heightCm: number | null) {
  if (weightKg == null || heightCm == null || !Number.isFinite(weightKg) || !Number.isFinite(heightCm) || weightKg <= 0 || heightCm <= 0) return null
  const heightMeters = heightCm / 100
  return weightKg / (heightMeters * heightMeters)
}

export function activityDateKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`
}

export function activityDateFromKey(key: string) {
  const [year, month, day] = key.split("-").map(Number)
  return new Date(year, month - 1, day, 12)
}

export function shiftActivityDate(date: Date, offset: number) {
  const next = new Date(date)
  next.setHours(12, 0, 0, 0)
  next.setDate(next.getDate() + offset)
  return next
}

export function activityMonthStart(date: Date) {
  const first = shiftActivityDate(date, 0)
  first.setDate(1)
  return first
}

export function shiftActivityMonth(date: Date, offset: number) {
  const next = activityMonthStart(date)
  next.setMonth(next.getMonth() + offset)
  return next
}

export function activityCalendarDays(today: Date, anchor: Date, expanded: boolean) {
  const first = expanded ? activityMonthStart(anchor) : anchor
  const last = expanded ? shiftActivityDate(shiftActivityMonth(anchor, 1), -1) : anchor
  const start = shiftActivityDate(first, -first.getDay())
  const end = shiftActivityDate(last, 6 - last.getDay())
  const todayEnd = shiftActivityDate(today, 0)
  const days = []
  for (let date = start; date <= end; date = shiftActivityDate(date, 1)) {
    days.push({ date, key: activityDateKey(date), available: date <= todayEnd && (!expanded || (date >= first && date <= last)) })
  }
  return days
}
