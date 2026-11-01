import { cynexusResult } from "@/services/common/cynexus-result"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"
import type { BodyMetricId, BodyObservationInput, SplitDay, WorkoutCategory, WorkoutRecords, WorkoutSession } from "./model"

const API = `${SERVICE_PREFIX.core}/workout`

export class WorkoutApiError extends Error {
  readonly status: number
  constructor(message: string, status = 0) { super(message); this.status = status }
  get retryable() { return this.status === 0 || this.status === 408 || this.status === 429 || this.status >= 500 }
}

export type WorkoutApiSession = {
  id: string | number
  date: string
  category: WorkoutCategory | string
  minutes: number
  split?: SplitDay | string | null
}

export type WorkoutApiObservation = {
  id: string
  metric: BodyMetricId | string
  side?: "left" | "right" | null
  value: number
  measuredAt: string
  note?: string | null
}

export type WorkoutApiSnapshot = {
  version?: number
  isSample?: boolean
  cycleStart?: string | null
  sessions?: readonly WorkoutApiSession[]
  observations?: readonly WorkoutApiObservation[]
  workout?: WorkoutApiSnapshot | null
}

async function request<T>(path: string, method = "GET", body?: unknown, signal?: AbortSignal): Promise<T> {
  let response: Response
  try { response = await fetch(`${API}${path}`, {
    method,
    cache: "no-store",
    signal,
    ...(body === undefined ? {} : { headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) }),
  }) } catch (error) {
    if (error instanceof Error && error.name === "AbortError") throw error
    throw new WorkoutApiError("Workout sync is unavailable. Your changes are saved on this device.")
  }
  if (!response.ok) {
    let message = ""
    try {
      const body = await response.clone().json() as { message?: unknown; error?: unknown }
      const detail = body.message ?? body.error
      if (typeof detail === "string" && !/[<>]/.test(detail)) message = detail
    } catch (error) {
      if (error instanceof Error && error.name === "AbortError") throw error
      /* An HTML proxy response is not a user-facing error message. */
    }
    throw new WorkoutApiError(message || (response.status >= 500 ? "Workout sync is temporarily unavailable. Your changes are saved on this device." : `Workout sync failed (HTTP ${response.status}). Please review your changes and retry.`), response.status)
  }
  try { return await cynexusResult<T>(response) }
  catch (error) {
    if (error instanceof Error && error.name === "AbortError") throw error
    const detail = error instanceof Error ? error.message : ""
    throw new WorkoutApiError(detail && !/[<>]/.test(detail) ? detail : "Workout sync returned an invalid response. Please retry.", 400)
  }
}

function unwrapSnapshot(value: WorkoutApiSnapshot | { snapshot?: WorkoutApiSnapshot | null; workout?: WorkoutApiSnapshot | null } | null | undefined) {
  if (value && "snapshot" in value && value.snapshot) return value.snapshot
  if (value && "workout" in value && value.workout) return value.workout
  return value ?? {}
}

export async function fetchWorkoutSnapshot(signal?: AbortSignal) {
  try {
    return unwrapSnapshot(await request<WorkoutApiSnapshot | { snapshot?: WorkoutApiSnapshot | null; workout?: WorkoutApiSnapshot | null }>("", "GET", undefined, signal))
  } catch (error) {
    // Core releases before the aggregate route was named expose it at /snapshot.
    if (typeof DOMException !== "undefined" && error instanceof DOMException && error.name === "AbortError") throw error
    if (!(error instanceof WorkoutApiError) || (error.status !== 404 && error.status !== 405)) throw error
    return unwrapSnapshot(await request<WorkoutApiSnapshot | { snapshot?: WorkoutApiSnapshot | null; workout?: WorkoutApiSnapshot | null }>("/snapshot", "GET", undefined, signal))
  }
}

async function mutate(path: string, body: unknown, signal?: AbortSignal) {
  const value = await request<WorkoutApiSnapshot | { snapshot?: WorkoutApiSnapshot | null; workout?: WorkoutApiSnapshot | null } | null>(path, "PUT", body, signal)
  const snapshot = unwrapSnapshot(value)
  return Object.keys(snapshot).length > 0 ? snapshot : fetchWorkoutSnapshot(signal)
}

export function saveWorkoutPlan(input: { cycleStart: string; idempotencyKey: string }, signal?: AbortSignal) {
  return mutate("/plan", input, signal)
}

export function saveWorkoutSession(input: { id?: string | number; date: string; category: WorkoutCategory; minutes: number; split?: SplitDay | null; idempotencyKey: string; cycleStart?: string }, signal?: AbortSignal) {
  return mutate("/sessions", input, signal)
}

export async function deleteWorkoutSession(id: string | number, idempotencyKey: string, signal?: AbortSignal) {
  const value = await request<WorkoutApiSnapshot | { snapshot?: WorkoutApiSnapshot | null; workout?: WorkoutApiSnapshot | null } | null>(`/sessions/${encodeURIComponent(String(id))}?${new URLSearchParams({ idempotencyKey })}`, "DELETE", undefined, signal)
  const snapshot = unwrapSnapshot(value)
  return Object.keys(snapshot).length > 0 ? snapshot : fetchWorkoutSnapshot(signal)
}

export function saveWorkoutMeasurement(input: BodyObservationInput & { idempotencyKey: string; cycleStart?: string }, signal?: AbortSignal) {
  return mutate("/measurements", input, signal)
}

export async function deleteWorkoutMeasurement(id: string, idempotencyKey: string, signal?: AbortSignal) {
  const value = await request<WorkoutApiSnapshot | { snapshot?: WorkoutApiSnapshot | null; workout?: WorkoutApiSnapshot | null } | null>(`/measurements/${encodeURIComponent(id)}?${new URLSearchParams({ idempotencyKey })}`, "DELETE", undefined, signal)
  const snapshot = unwrapSnapshot(value)
  return Object.keys(snapshot).length > 0 ? snapshot : fetchWorkoutSnapshot(signal)
}

export function normalizeWorkoutSnapshot(raw: WorkoutApiSnapshot, today: string): WorkoutRecords {
  const source = raw.workout && typeof raw.workout === "object" ? raw.workout : raw
  const sessions = (source.sessions ?? []).map((session) => ({
    id: session.id,
    date: session.date,
    category: normalizeCategory(session.category),
    minutes: Number(session.minutes),
    split: normalizeSplit(session.split),
  })).filter((session) => session.id !== undefined && isDateKey(session.date) && session.date <= today && Number.isInteger(session.minutes) && session.minutes >= 1 && session.minutes <= 1440 && (session.category === "Strength" || session.category === "Cardio"))
  const observations = (source.observations ?? []).map((observation) => ({
    id: String(observation.id),
    metric: normalizeMetric(observation.metric),
    side: observation.side ?? null,
    value: Number(observation.value),
    measuredAt: observation.measuredAt,
    ...(observation.note ? { note: observation.note } : {}),
  })).filter((observation) => observation.id && isDateKey(observation.measuredAt) && observation.measuredAt <= today && Number.isFinite(observation.value))
  const cycleStart = isDateKey(source.cycleStart) ? source.cycleStart : today
  return { version: 1, isSample: source.isSample === true, cycleStart, sessions, observations }
}

function normalizeCategory(category: string): WorkoutCategory {
  return category.toLowerCase() === "cardio" ? "Cardio" : "Strength"
}

function normalizeSplit(split: string | null | undefined): SplitDay | null {
  if (!split) return null
  const value = split.toLowerCase().replace(/[_-]+/g, " ").replace(/\s+/g, " ").trim()
  if (value === "chest + triceps" || value === "chest triceps") return "Chest + Triceps"
  if (value === "back + biceps" || value === "back biceps") return "Back + Biceps"
  if (value === "shoulders" || value === "shoulder") return "Shoulders"
  if (value === "rest") return "Rest"
  return null
}

function normalizeMetric(metric: string): BodyMetricId {
  const value = metric.replace(/[-\s]+/g, "_").replace(/[A-Z]/g, (letter) => `_${letter.toLowerCase()}`).replace(/^_/, "")
  const aliases: Record<string, BodyMetricId> = { shoulder_width: "shoulderWidth", upper_arm: "upperArm", under_chest: "underChest" }
  return aliases[value] ?? metric as BodyMetricId
}

function isDateKey(value: unknown): value is string { return typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value) }

export type { WorkoutRecords, WorkoutSession }
