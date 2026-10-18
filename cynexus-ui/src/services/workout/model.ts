export type WorkoutCategory = "Strength" | "Cardio"
export type SplitDay = "Chest + Triceps" | "Back + Biceps" | "Shoulders" | "Rest"
export type WorkoutSession = { id: string | number; date: string; category: WorkoutCategory; minutes: number; split: SplitDay | null }
export type BodyMetricId = "height" | "weight" | "head" | "neck" | "shoulders" | "shoulderWidth" | "chest" | "underChest" | "waist" | "abdomen" | "hips" | "upperArm" | "forearm" | "wrist" | "thigh" | "calf" | "ankle"
export type BodySide = "left" | "right" | null
export type BodyObservation = { id: string; metric: BodyMetricId; side: BodySide; value: number; measuredAt: string; note?: string }
export type BodyObservationInput = Omit<BodyObservation, "id"> & { id?: string }
export type BodyMetric = { id: BodyMetricId; label: string; shortLabel: string; unit: "cm" | "kg"; paired: boolean; min: number; max: number; step: number }
export type WorkoutRecords = { version: 1; isSample: boolean; cycleStart: string; sessions: WorkoutSession[]; observations: BodyObservation[] }

export const WORKOUT_CYCLE: readonly SplitDay[] = ["Chest + Triceps", "Back + Biceps", "Shoulders", "Rest"]
export const BODY_METRICS: readonly BodyMetric[] = [
  { id: "weight", label: "Weight", shortLabel: "Weight", unit: "kg", paired: false, min: 1, max: 600, step: .1 },
  { id: "height", label: "Height", shortLabel: "Height", unit: "cm", paired: false, min: 40, max: 260, step: .1 },
  ...([
    ["head", "Head circumference", "Head", false, 20, 90],
    ["neck", "Neck circumference", "Neck", false, 10, 90],
    ["shoulders", "Shoulder circumference", "Shoulders", false, 40, 220],
    ["shoulderWidth", "Shoulder width", "Shoulder width", false, 10, 100],
    ["chest", "Chest circumference", "Chest", false, 30, 250],
    ["underChest", "Under-chest circumference", "Under chest", false, 30, 250],
    ["waist", "Waist circumference", "Waist", false, 30, 250],
    ["abdomen", "Abdomen circumference", "Abdomen", false, 30, 250],
    ["hips", "Hip circumference", "Hips", false, 30, 250],
    ["upperArm", "Upper-arm circumference", "Upper arm", true, 5, 100],
    ["forearm", "Forearm circumference", "Forearm", true, 5, 80],
    ["wrist", "Wrist circumference", "Wrist", true, 5, 50],
    ["thigh", "Thigh circumference", "Thigh", true, 10, 150],
    ["calf", "Calf circumference", "Calf", true, 5, 100],
    ["ankle", "Ankle circumference", "Ankle", true, 5, 60],
  ] as const).map(([id, label, shortLabel, paired, min, max]) => ({ id, label, shortLabel, unit: "cm" as const, paired, min, max, step: .1 })),
]

export function getMetric(id: BodyMetricId) { return BODY_METRICS.find((metric) => metric.id === id)! }
export function dateKey(date: Date) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}` }
export function parseDate(value: string) { const [year, month, day] = value.split("-").map(Number); return new Date(year, month - 1, day, 12) }
export function isDateKey(value: unknown): value is string { return typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value) && dateKey(parseDate(value)) === value }
export function addDays(date: Date, days: number) { const next = new Date(date); next.setHours(12, 0, 0, 0); next.setDate(next.getDate() + days); return next }
export function dayDifference(start: Date, end: Date) { return Math.round((Date.UTC(end.getFullYear(), end.getMonth(), end.getDate()) - Date.UTC(start.getFullYear(), start.getMonth(), start.getDate())) / 86400000) }
export function formatDate(value: string) { return parseDate(value).toLocaleDateString("en-US", { month: "short", day: "numeric" }) }
export function formatMinutes(minutes: number) { const h = Math.floor(minutes / 60), m = minutes % 60; return h ? m ? `${h}h ${m}m` : `${h}h` : `${m} min` }
export function splitOnDate(cycleStart: string, date: string): SplitDay | null { const offset = dayDifference(parseDate(cycleStart), parseDate(date)); return offset < 0 ? null : WORKOUT_CYCLE[offset % 4] }

export function latestObservation(observations: readonly BodyObservation[], metric: BodyMetricId, side: BodySide, asOf: string) {
  return observations.filter((item) => item.metric === metric && item.side === side && item.measuredAt <= asOf).sort((a, b) => b.measuredAt.localeCompare(a.measuredAt) || b.id.localeCompare(a.id))[0] ?? null
}

export function bodySummary(observations: readonly BodyObservation[], asOf: string) {
  const value = (metric: BodyMetricId, side: BodySide = null) => latestObservation(observations, metric, side, asOf)?.value ?? null
  const recordedAt = observations.filter((item) => item.measuredAt <= asOf).sort((a, b) => b.measuredAt.localeCompare(a.measuredAt))[0]?.measuredAt ?? asOf
  return { recordedAt, heightCm: value("height"), weightKg: value("weight"), chestCm: value("chest"), waistCm: value("waist"), upperArmCm: value("upperArm", "left"), forearmCm: value("forearm", "left") }
}

export function validObservation(input: BodyObservationInput, today: string) {
  const metric = BODY_METRICS.find((item) => item.id === input.metric)
  return Boolean(metric && isDateKey(input.measuredAt) && input.measuredAt <= today && Number.isFinite(input.value) && input.value >= metric.min && input.value <= metric.max && (metric.paired ? input.side === "left" || input.side === "right" : input.side === null))
}

export function saveObservation(data: WorkoutRecords, input: BodyObservationInput, today: string, newId: string): WorkoutRecords {
  if (!validObservation(input, today)) throw new Error("Enter a valid measurement and date.")
  const observations = data.isSample ? [] : data.observations
  const duplicate = observations.find((item) => item.metric === input.metric && item.side === input.side && item.measuredAt === input.measuredAt && item.id !== input.id)
  const id = input.id && observations.some((item) => item.id === input.id) ? input.id : duplicate?.id ?? newId
  const observation: BodyObservation = { ...input, id, value: Math.round(input.value * 10) / 10 }
  return { ...data, isSample: false, sessions: data.isSample ? [] : data.sessions, observations: [...observations.filter((item) => item.id !== id && item.id !== duplicate?.id), observation] }
}

export function saveSession(data: WorkoutRecords, input: Omit<WorkoutSession, "id" | "split">, today: string, newId: string): WorkoutRecords {
  if (!isDateKey(input.date) || input.date > today || !["Strength", "Cardio"].includes(input.category) || !Number.isInteger(input.minutes) || input.minutes < 1 || input.minutes > 1440) throw new Error("Enter a duration from 1 to 1,440 minutes and a valid date.")
  const sessions = data.isSample ? [] : data.sessions
  const previous = sessions.find((item) => item.date === input.date)
  const session = { ...input, id: previous?.id ?? newId, split: input.category === "Strength" ? previous?.category === "Strength" ? previous.split : splitOnDate(data.cycleStart, input.date) : null }
  return { ...data, isSample: false, observations: data.isSample ? [] : data.observations, sessions: [session, ...sessions.filter((item) => item.date !== input.date)] }
}

export function startPersonalLog(data: WorkoutRecords): WorkoutRecords { return { ...data, isSample: false, sessions: [], observations: [] } }

export function createSampleWorkoutRecords(today: Date): WorkoutRecords {
  const cycleStart = dateKey(addDays(today, -120))
  const sessions: WorkoutSession[] = []
  for (let offset = -112; offset <= 0; offset++) {
    const date = dateKey(addDays(today, offset)), split = splitOnDate(cycleStart, date)
    if (split === "Rest" || offset % 11 === 0) continue
    const category = offset % 9 === 0 ? "Cardio" : "Strength"
    sessions.push({ id: `sample-${date}`, date, category, minutes: category === "Cardio" ? 30 + Math.abs(offset % 4) * 5 : 45 + Math.abs(offset % 5) * 4, split: category === "Strength" ? split : null })
  }
  const observations: BodyObservation[] = []
  const baseline: Partial<Record<BodyMetricId, number>> = { height: 178, weight: 72.4, head: 57, neck: 37, shoulders: 112, shoulderWidth: 44, chest: 98.2, underChest: 87, waist: 80, abdomen: 84, hips: 96.4, upperArm: 32, forearm: 27.1, wrist: 17, thigh: 55.8, calf: 37.2, ankle: 22.5 }
  for (let week = 0; week < 17; week++) {
    const measuredAt = dateKey(addDays(today, -week * 7))
    for (const metric of BODY_METRICS) for (const side of metric.paired ? ["left", "right"] as const : [null]) {
      const base = baseline[metric.id]!, trend = ["waist", "abdomen", "weight", "hips"].includes(metric.id) ? .3 : ["height", "head", "shoulderWidth", "wrist", "ankle"].includes(metric.id) ? 0 : -.12
      observations.push({ id: `sample-${metric.id}-${side}-${week}`, metric: metric.id, side, measuredAt, value: Math.round((base + week * trend + (side === "right" ? .3 : 0)) * 10) / 10 })
    }
  }
  return { version: 1, isSample: true, cycleStart, sessions, observations }
}

export function decodeWorkoutRecords(raw: string, today: string): WorkoutRecords {
  const data = JSON.parse(raw) as WorkoutRecords
  if (data.version !== 1 || typeof data.isSample !== "boolean" || !isDateKey(data.cycleStart) || !Array.isArray(data.sessions) || !Array.isArray(data.observations)) throw new Error("Unsupported workout records.")
  if (data.observations.some((item) => !item || typeof item.id !== "string" || !validObservation(item, today))) throw new Error("Invalid measurement records.")
  if (data.sessions.some((item) => !item || !["string", "number"].includes(typeof item.id) || !isDateKey(item.date) || item.date > today || !["Strength", "Cardio"].includes(item.category) || !Number.isInteger(item.minutes) || item.minutes < 1 || item.minutes > 1440 || !(item.split === null || WORKOUT_CYCLE.includes(item.split)))) throw new Error("Invalid workout records.")
  return data
}
