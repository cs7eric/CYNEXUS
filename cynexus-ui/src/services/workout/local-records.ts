import { createSampleWorkoutRecords, dateKey, decodeWorkoutRecords, type WorkoutRecords } from "./model"

export type WorkoutSnapshot = { data: WorkoutRecords; storageError: string | null }
const cache = new Map<string, WorkoutSnapshot>()
const listeners = new Map<string, Set<() => void>>()
const prefix = "cynexus.workout.records.v1."

export function getWorkoutSnapshot(scope: string): WorkoutSnapshot {
  const previous = cache.get(scope)
  if (previous) return previous
  let data = createSampleWorkoutRecords(new Date()), storageError: string | null = null
  try {
    const raw = window.localStorage.getItem(prefix + scope)
    if (raw !== null) data = decodeWorkoutRecords(raw, dateKey(new Date()))
  } catch { storageError = "Saved records could not be loaded. Sample data is shown. Changes can be saved again from this device." }
  const snapshot = { data, storageError }
  cache.set(scope, snapshot)
  return snapshot
}

export function subscribeWorkoutRecords(scope: string, listener: () => void) {
  const group = listeners.get(scope) ?? new Set<() => void>()
  group.add(listener); listeners.set(scope, group)
  return () => { group.delete(listener) }
}

export function updateWorkoutRecords(scope: string, update: (current: WorkoutRecords) => WorkoutRecords) {
  const data = update(getWorkoutSnapshot(scope).data)
  let storageError: string | null = null
  try { window.localStorage.setItem(prefix + scope, JSON.stringify(data)) }
  catch { storageError = "Could not save to this browser. Changes are kept for this session only." }
  cache.set(scope, { data, storageError })
  listeners.get(scope)?.forEach((listener) => listener())
}

if (typeof window !== "undefined") window.addEventListener("storage", (event) => {
  if (event.key !== null && !event.key.startsWith(prefix)) return
  const scopes = event.key === null ? [...cache.keys()] : [event.key.slice(prefix.length)]
  scopes.forEach((scope) => { cache.delete(scope); listeners.get(scope)?.forEach((listener) => listener()) })
})
