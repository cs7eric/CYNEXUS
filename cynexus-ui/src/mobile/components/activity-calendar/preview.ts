import { activityDateKey, shiftActivityDate, type DailyActivity, type DailyNutritionTargets } from "./model"

// UI preview fixtures only. Never merge these into real health records.
const sampleTargets: DailyNutritionTargets = { calories: 2200, carbsGrams: 250, proteinGrams: 120, fatGrams: 80 }

const samples: Omit<DailyActivity, "date" | "nutritionTargets">[] = [
  { nutrition: { calories: 1840, meals: 3, carbsGrams: 220, proteinGrams: 105, fatGrams: 60 }, workout: { minutes: 45, sessions: 1, label: "Strength" } },
  { nutrition: { calories: 2120, meals: 4, carbsGrams: 250, proteinGrams: 145, fatGrams: 60 }, workout: { minutes: 30, sessions: 1, label: "Running" } },
  { nutrition: { calories: 1960, meals: 3, carbsGrams: 240, proteinGrams: 115, fatGrams: 60 }, workout: { minutes: 0, sessions: 0, label: "Rest day" } },
  { nutrition: null, workout: { minutes: 50, sessions: 1, label: "Strength" } },
  { nutrition: { calories: 1780, meals: 3, carbsGrams: 190.5, proteinGrams: null, fatGrams: 50 }, workout: null },
  { nutrition: { calories: 2050, meals: 3, carbsGrams: 240, proteinGrams: 115, fatGrams: 70 }, workout: { minutes: 25, sessions: 1, label: "Mobility" } },
  { nutrition: null, workout: null },
  { nutrition: { calories: 1890, meals: 3, carbsGrams: 210, proteinGrams: 105, fatGrams: 70 }, workout: { minutes: 60, sessions: 2, label: "Strength + walk" } },
  { nutrition: { calories: 2210, meals: 4, carbsGrams: 260, proteinGrams: 135, fatGrams: 70 }, workout: { minutes: 40, sessions: 1, label: "Cycling" } },
  { nutrition: { calories: 1940, meals: 3, carbsGrams: 235, proteinGrams: 115, fatGrams: 60 }, workout: { minutes: 0, sessions: 0, label: "Rest day" } },
]

export function createActivityPreview(today: Date): DailyActivity[] {
  return Array.from({ length: 30 }, (_, offset) => ({
    date: activityDateKey(shiftActivityDate(today, -offset)),
    nutritionTargets: sampleTargets,
    ...samples[offset % samples.length],
  }))
}
