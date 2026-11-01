import { createSampleWorkoutRecords, dateKey, decodeWorkoutRecords, type BodyObservationInput, type WorkoutRecords, type WorkoutSession } from "./model"
import { deleteWorkoutMeasurement, deleteWorkoutSession, fetchWorkoutSnapshot, normalizeWorkoutSnapshot, saveWorkoutMeasurement, saveWorkoutPlan, saveWorkoutSession } from "./api"

export type WorkoutSnapshot = { data: WorkoutRecords; storageError: string | null; syncing: boolean; syncError: string | null; pendingCount: number; retryable: boolean }
type SessionWrite = Omit<WorkoutSession, "id"> & { id?: string | number }
type PendingWrite =
  | { requestId: string; idempotencyKey: string; kind: "plan"; cycleStart: string }
  | { requestId: string; idempotencyKey: string; kind: "session"; cycleStart: string; session: SessionWrite }
  | { requestId: string; idempotencyKey: string; kind: "measurement"; cycleStart: string; observation: BodyObservationInput }
  | { requestId: string; idempotencyKey: string; kind: "delete-session"; id: string | number }
  | { requestId: string; idempotencyKey: string; kind: "delete-measurement"; id: string }
type RemoteSnapshot = Awaited<ReturnType<typeof fetchWorkoutSnapshot>>

const cache = new Map<string, WorkoutSnapshot>()
const listeners = new Map<string, Set<() => void>>()
const flushChains = new Map<string, Promise<void>>()
const prefix = "cynexus.workout.records.v1."
const pendingPrefix = "cynexus.workout.pending.v1."
const migrationPrefix = "cynexus.workout.core-migration.v1."
const retries = new Map<string, ReturnType<typeof setTimeout>>()
const attempts = new Map<string, number>()
const syncs = new Map<string, Promise<SyncResult>>()
const revisions = new Map<string, number>()
type SyncResult = { snapshot: WorkoutSnapshot; synced: boolean; error: string | null }

function notify(scope: string, patch: Partial<WorkoutSnapshot>) {
  const snapshot = { ...getWorkoutSnapshot(scope), ...patch, pendingCount: readPending(scope).length }
  cache.set(scope, snapshot)
  listeners.get(scope)?.forEach((listener) => listener())
  return snapshot
}

function scheduleRetry(scope: string) {
  if (!listeners.get(scope)?.size || retries.has(scope) || !getWorkoutSnapshot(scope).retryable) return
  const attempt = attempts.get(scope) ?? 0
  if (attempt >= 6) return
  attempts.set(scope, attempt + 1)
  retries.set(scope, setTimeout(() => { retries.delete(scope); void syncWorkoutRecords(scope) }, Math.min(10_000 * 2 ** attempt, 60_000)))
}

function failed(scope: string, error: unknown) {
  if (error instanceof Error && error.name === "AbortError") return notify(scope, { syncing: false })
  const message = error instanceof Error && !/[<>]/.test(error.message) ? error.message : "Workout sync is unavailable. Your changes are saved on this device."
  const retryable = !(error instanceof Error && "retryable" in error) || Boolean((error as { retryable: unknown }).retryable)
  if (!retryable) { clearTimeout(retries.get(scope)); retries.delete(scope) }
  const snapshot = notify(scope, { syncing: false, syncError: message, retryable })
  scheduleRetry(scope)
  return snapshot
}

export function getWorkoutSnapshot(scope: string): WorkoutSnapshot {
  const previous = cache.get(scope)
  if (previous) return previous
  let data = createSampleWorkoutRecords(new Date()), storageError: string | null = null
  try {
    const raw = typeof window === "undefined" ? null : window.localStorage.getItem(prefix + scope)
    if (raw !== null) data = decodeWorkoutRecords(raw, dateKey(new Date()))
  } catch { storageError = "Saved records could not be loaded. Sample data is shown. Changes can be saved again from this device." }
  const snapshot: WorkoutSnapshot = { data, storageError, syncing: false, syncError: null, pendingCount: readPending(scope).length, retryable: false }
  cache.set(scope, snapshot)
  return snapshot
}

export function subscribeWorkoutRecords(scope: string, listener: () => void) {
  const group = listeners.get(scope) ?? new Set<() => void>()
  group.add(listener); listeners.set(scope, group)
  if (group.size === 1) { attempts.delete(scope); void syncWorkoutRecords(scope) }
  return () => { group.delete(listener); if (!group.size) { clearTimeout(retries.get(scope)); retries.delete(scope); attempts.delete(scope) } }
}

export function updateWorkoutRecords(scope: string, update: (current: WorkoutRecords) => WorkoutRecords) {
  const data = update(getWorkoutSnapshot(scope).data)
  let storageError: string | null = null
  try { window.localStorage.setItem(prefix + scope, JSON.stringify(data)) }
  catch { storageError = "Could not save to this browser. Changes are kept for this session only." }
  revisions.set(scope, (revisions.get(scope) ?? 0) + 1)
  return notify(scope, { data, storageError })
}

function publish(scope: string, data: WorkoutRecords, storageError: string | null = null) {
  try { window.localStorage.setItem(prefix + scope, JSON.stringify(data)) }
  catch { storageError = "Could not save to this browser. Changes are kept for this session only." }
  clearTimeout(retries.get(scope)); retries.delete(scope); attempts.delete(scope)
  return notify(scope, { data, storageError, syncing: false, syncError: null, retryable: false })
}

function hasPersonalRecords(data: WorkoutRecords) {
  return !data.isSample && (data.sessions.length > 0 || data.observations.length > 0 || Boolean(data.cycleStart))
}

function hasRemoteRecords(data: WorkoutRecords) {
  return data.sessions.length > 0 || data.observations.length > 0
}

function readPending(scope: string): PendingWrite[] {
  try {
    const raw = typeof window === "undefined" ? null : window.localStorage.getItem(pendingPrefix + scope)
    if (!raw) return []
    const parsed: unknown = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed as PendingWrite[] : []
  } catch { return [] }
}

function writePending(scope: string, writes: PendingWrite[]) {
  if (typeof window !== "undefined") window.localStorage.setItem(pendingPrefix + scope, JSON.stringify(writes))
}

async function applyPending(write: PendingWrite, signal?: AbortSignal): Promise<RemoteSnapshot> {
  switch (write.kind) {
    case "plan":
      return saveWorkoutPlan({ cycleStart: write.cycleStart, idempotencyKey: write.idempotencyKey }, signal)
    case "session":
      return saveWorkoutSession({ ...write.session, cycleStart: write.cycleStart, idempotencyKey: write.idempotencyKey }, signal)
    case "measurement":
      return saveWorkoutMeasurement({ ...write.observation, cycleStart: write.cycleStart, idempotencyKey: write.idempotencyKey }, signal)
    case "delete-session":
      return deleteWorkoutSession(write.id, write.idempotencyKey, signal)
    case "delete-measurement":
      return deleteWorkoutMeasurement(write.id, write.idempotencyKey, signal)
  }
}

function flushPending(scope: string, signal?: AbortSignal): Promise<RemoteSnapshot | null> {
  const previous = flushChains.get(scope) ?? Promise.resolve()
  let last: RemoteSnapshot | null = null
  const current = previous.then(async () => {
    while (true) {
      const [write] = readPending(scope)
      if (!write) return last
      last = await applyPending(write, signal)
      writePending(scope, readPending(scope).filter((item) => item.requestId !== write.requestId))
    }
  })
  flushChains.set(scope, current.then(() => undefined, () => undefined))
  return current
}

async function queueWrite(scope: string, write: PendingWrite, signal?: AbortSignal) {
  const revision = revisions.get(scope) ?? 0
  try { writePending(scope, [...readPending(scope), write]) }
  catch (error) {
    notify(scope, { storageError: "Could not queue account sync in this browser. Your changes are kept for this session only." })
    throw error
  }
  notify(scope, { syncing: true })
  try {
    const snapshot = await flushPending(scope, signal) ?? await fetchWorkoutSnapshot(signal)
    if ((revisions.get(scope) ?? 0) !== revision || readPending(scope).length) return publish(scope, getWorkoutSnapshot(scope).data)
    return publishRemote(scope, snapshot)
  } catch (error) {
    failed(scope, error)
    throw error
  }
}

function isMigrationComplete(scope: string) {
  try { return typeof window !== "undefined" && window.localStorage.getItem(migrationPrefix + scope) === "complete" }
  catch { return false }
}

function markMigrationComplete(scope: string) {
  try { if (typeof window !== "undefined") window.localStorage.setItem(migrationPrefix + scope, "complete") }
  catch { /* Migration remains repeatable when storage is unavailable. */ }
}

function sessionIdentity(session: WorkoutSession) { return session.date }
function observationIdentity(observation: BodyObservationInput) { return `${observation.metric}:${observation.side ?? "none"}:${observation.measuredAt}` }

/** Replay offline writes before merging legacy records and publishing Core state. */
export function syncWorkoutRecords(scope: string, signal?: AbortSignal): Promise<SyncResult> {
  const existing = syncs.get(scope)
  if (existing) return existing
  const task = performSync(scope, signal).finally(() => syncs.delete(scope))
  syncs.set(scope, task)
  return task
}

async function performSync(scope: string, signal?: AbortSignal): Promise<SyncResult> {
  const local = getWorkoutSnapshot(scope)
  const revision = revisions.get(scope) ?? 0
  notify(scope, { syncing: true })
  try {
    const replayed = await flushPending(scope, signal)
    let data = normalizeWorkoutSnapshot(replayed ?? await fetchWorkoutSnapshot(signal), dateKey(new Date()))
    if (!isMigrationComplete(scope) && hasPersonalRecords(local.data)) {
      if (!hasRemoteRecords(data)) {
        await saveWorkoutPlan({ cycleStart: local.data.cycleStart, idempotencyKey: crypto.randomUUID() }, signal)
      }
      const sessions = new Set(data.sessions.map(sessionIdentity))
      for (const session of local.data.sessions) {
        if (sessions.has(sessionIdentity(session))) continue
        await saveWorkoutSession({ ...session, cycleStart: local.data.cycleStart, idempotencyKey: crypto.randomUUID() }, signal)
        sessions.add(sessionIdentity(session))
      }
      const observations = new Set(data.observations.map(observationIdentity))
      for (const observation of local.data.observations) {
        if (observations.has(observationIdentity(observation))) continue
        await saveWorkoutMeasurement({ ...observation, cycleStart: local.data.cycleStart, idempotencyKey: crypto.randomUUID() }, signal)
        observations.add(observationIdentity(observation))
      }
      data = normalizeWorkoutSnapshot(await fetchWorkoutSnapshot(signal), dateKey(new Date()))
      markMigrationComplete(scope)
    }
    const pending = await flushPending(scope, signal)
    if (pending) data = normalizeWorkoutSnapshot(pending, dateKey(new Date()))
    if ((revisions.get(scope) ?? 0) !== revision || readPending(scope).length) data = getWorkoutSnapshot(scope).data
    return { snapshot: publish(scope, data), synced: true, error: null as string | null }
  } catch (error) {
    const snapshot = failed(scope, error)
    return { snapshot, synced: false, error: snapshot.syncError }
  }
}

function publishRemote(scope: string, raw: RemoteSnapshot) {
  return publish(scope, normalizeWorkoutSnapshot(raw, dateKey(new Date())))
}

function newRequestKey() { return crypto.randomUUID() }

export function saveWorkoutPlanRemote(scope: string, cycleStart: string, signal?: AbortSignal) {
  return queueWrite(scope, { requestId: newRequestKey(), idempotencyKey: newRequestKey(), kind: "plan", cycleStart }, signal)
}

export function saveWorkoutSessionRemote(scope: string, session: SessionWrite, signal?: AbortSignal) {
  return queueWrite(scope, { requestId: newRequestKey(), idempotencyKey: newRequestKey(), kind: "session", session, cycleStart: getWorkoutSnapshot(scope).data.cycleStart }, signal)
}

export function deleteWorkoutSessionRemote(scope: string, id: string | number, signal?: AbortSignal) {
  return queueWrite(scope, { requestId: newRequestKey(), idempotencyKey: newRequestKey(), kind: "delete-session", id }, signal)
}

export function saveWorkoutMeasurementRemote(scope: string, observation: BodyObservationInput, signal?: AbortSignal) {
  return queueWrite(scope, { requestId: newRequestKey(), idempotencyKey: newRequestKey(), kind: "measurement", observation, cycleStart: getWorkoutSnapshot(scope).data.cycleStart }, signal)
}

export function deleteWorkoutMeasurementRemote(scope: string, id: string, signal?: AbortSignal) {
  return queueWrite(scope, { requestId: newRequestKey(), idempotencyKey: newRequestKey(), kind: "delete-measurement", id }, signal)
}

if (typeof window !== "undefined") window.addEventListener("storage", (event) => {
  if (event.key !== null && !event.key.startsWith(prefix)) return
  const scopes = event.key === null ? [...cache.keys()] : [event.key.slice(prefix.length)]
  scopes.forEach((scope) => { cache.delete(scope); listeners.get(scope)?.forEach((listener) => listener()) })
})

if (typeof window !== "undefined") {
  const recover = () => {
    if (typeof document !== "undefined" && document.visibilityState === "hidden") return
    listeners.forEach((group, scope) => { if (group.size && getWorkoutSnapshot(scope).retryable) { attempts.delete(scope); void syncWorkoutRecords(scope) } })
  }
  window.addEventListener("online", recover)
  window.addEventListener("focus", recover)
  if (typeof document !== "undefined") document.addEventListener("visibilitychange", recover)
}
