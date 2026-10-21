import { createEmptyFridgeRecords, decodeFridgeRecords, type FridgeRecords } from "./model"
import { addFridgeItem, adjustFridgeItem, confirmFridgeItemMapping, fetchFridgeInventory, normalizeFridgeInventory, type FridgeAddItemRequest, type FridgeAdjustRequest, type FridgeMappingRequest } from "./api"

export type FridgeSnapshot = { data: FridgeRecords; storageError: string | null }

const cache = new Map<string, FridgeSnapshot>()
const listeners = new Map<string, Set<() => void>>()
const prefix = "cynexus.fridge.records.v1."
const outboxPrefix = "cynexus.fridge.outbox.v1."
const outboxItemPrefix = `${outboxPrefix}item/`
const pendingCache = new Map<string, PendingWrite[]>()
const writeTails = new Map<string, Promise<void>>()
const localRevisions = new Map<string, number>()
const refreshRevisions = new Map<string, number>()

type PendingWrite =
  | { key: string; kind: "add"; input: FridgeAddItemRequest }
  | { key: string; kind: "adjust"; itemId: string; input: FridgeAdjustRequest }
  | { key: string; kind: "mapping"; itemId: string; input: FridgeMappingRequest }

class FridgeMappingTargetNotFoundError extends Error {}

function isPendingWrite(value: unknown): value is PendingWrite {
  if (!value || typeof value !== "object") return false
  const entry = value as Partial<PendingWrite>
  if (typeof entry.key !== "string" || !entry.key) return false
  if (entry.kind === "add") return !!entry.input && typeof entry.input === "object"
  if (entry.kind === "adjust" || entry.kind === "mapping") return typeof entry.itemId === "string" && !!entry.input && typeof entry.input === "object"
  return false
}

function itemStorageKey(scope: string, key: string) {
  return `${outboxItemPrefix}${encodeURIComponent(scope)}/${encodeURIComponent(key)}`
}

function legacyStorageKey(scope: string) {
  return outboxPrefix + scope
}

function pendingWrites(scope: string) {
  const cached = pendingCache.get(scope)
  if (cached) return cached

  const storage = typeof window === "undefined" ? null : window.localStorage
  const byKey = new Map<string, PendingWrite>()
  let legacyWrites: PendingWrite[] = []
  if (storage) {
    try {
      const raw = storage.getItem(legacyStorageKey(scope))
      const parsed: unknown = raw === null ? null : JSON.parse(raw)
      if (Array.isArray(parsed)) legacyWrites = parsed.filter(isPendingWrite)
    } catch { /* Continue with per-operation entries when old storage is malformed. */ }

    const scopedPrefix = `${outboxItemPrefix}${encodeURIComponent(scope)}/`
    try {
      for (let index = 0; index < storage.length; index++) {
        const key = storage.key(index)
        if (!key?.startsWith(scopedPrefix)) continue
        const raw = storage.getItem(key)
        if (raw === null) continue
        const parsed: unknown = JSON.parse(raw)
        if (isPendingWrite(parsed)) byKey.set(parsed.key, parsed)
      }
    } catch { /* In-memory writes remain available for this session. */ }

    let migrated = true
    for (const write of legacyWrites) {
      if (byKey.has(write.key)) continue
      byKey.set(write.key, write)
      try {
        storage.setItem(itemStorageKey(scope, write.key), JSON.stringify(write))
      } catch {
        migrated = false
      }
    }
    if (legacyWrites.length && migrated) {
      try { storage.removeItem(legacyStorageKey(scope)) } catch { /* Legacy entries are still read on the next load. */ }
    }
  } else {
    legacyWrites = []
  }

  for (const write of legacyWrites) {
    if (!byKey.has(write.key)) byKey.set(write.key, write)
  }
  const queue = [...byKey.values()]
  pendingCache.set(scope, queue)
  return queue
}

function persistPendingWrite(scope: string, write: PendingWrite) {
  const current = pendingWrites(scope)
  if (current.some(candidate => candidate.key === write.key)) return null
  pendingCache.set(scope, [...current, write])
  if (current.length >= 200) {
    pendingCache.set(scope, current)
    throw new Error("Fridge sync queue is full. Reconnect to sync pending changes first.")
  }
  try {
    if (typeof window === "undefined") throw new Error("Browser storage is unavailable.")
    window.localStorage.setItem(itemStorageKey(scope, write.key), JSON.stringify(write))
    return null
  } catch {
    return "Pending fridge changes could not be saved for retry after this session."
  }
}

function removeLegacyWrite(scope: string, key: string) {
  if (typeof window === "undefined") return
  const storageKey = legacyStorageKey(scope)
  try {
    const raw = window.localStorage.getItem(storageKey)
    if (raw === null) return
    const parsed: unknown = JSON.parse(raw)
    if (!Array.isArray(parsed)) return
    const remaining = parsed.filter((candidate) => !isPendingWrite(candidate) || candidate.key !== key)
    if (remaining.length) window.localStorage.setItem(storageKey, JSON.stringify(remaining))
    else window.localStorage.removeItem(storageKey)
  } catch { /* A stale legacy entry is safe to retry because writes are idempotent. */ }
}

function removePendingWrite(scope: string, write: PendingWrite) {
  pendingCache.set(scope, pendingWrites(scope).filter(candidate => candidate.key !== write.key))
  try {
    if (typeof window === "undefined") throw new Error("Browser storage is unavailable.")
    window.localStorage.removeItem(itemStorageKey(scope, write.key))
  } catch { /* A stale outbox entry is safe to retry because writes are idempotent. */ }
  removeLegacyWrite(scope, write.key)
}

function serializeWrite<T>(scope: string, action: () => Promise<T>) {
  const previous = writeTails.get(scope) ?? Promise.resolve()
  const result = previous.then(action, action)
  writeTails.set(scope, result.then(() => undefined, () => undefined))
  return result
}

function isServerResourceId(value: string) {
  return /^[1-9]\d*$/.test(value)
}

function normalizeFoodIdentity(value: string | null | undefined) {
  return (value ?? "").trim().replace(/\s+/g, " ").toLocaleLowerCase()
}

async function sendPendingWrite(scope: string, write: PendingWrite, signal?: AbortSignal) {
  if (write.kind === "add") return addFridgeItem(write.input, signal)
  if (write.kind === "adjust") return adjustFridgeItem(write.itemId, write.input, signal)

  let itemId = write.itemId
  if (!isServerResourceId(itemId)) {
    const localItem = getFridgeSnapshot(scope).data.items.find((item) => item.id === itemId)
    if (!localItem) throw new FridgeMappingTargetNotFoundError("This fridge item is no longer available. Refresh the fridge and try again.")

    // Local-first items use UUIDs until their add request is accepted by Core.
    // Resolve that optimistic ID after earlier queued writes have drained.
    const inventory = await fetchFridgeInventory(signal)
    const name = normalizeFoodIdentity(localItem.name)
    const brand = normalizeFoodIdentity(localItem.brand)
    const matches = inventory.items.filter((item) =>
      normalizeFoodIdentity(item.name) === name && normalizeFoodIdentity(item.brand) === brand,
    )
    if (matches.length !== 1 || !isServerResourceId(matches[0]?.id ?? "")) {
      throw new FridgeMappingTargetNotFoundError("This food has not synced to the fridge service yet. Refresh the fridge and try again.")
    }
    itemId = matches[0].id
  }
  return confirmFridgeItemMapping(itemId, write.input, signal)
}

async function drainPendingWrites(scope: string, signal?: AbortSignal, stopAfterKey?: string) {
  let latestInventory: Awaited<ReturnType<typeof sendPendingWrite>> | null = null
  while (pendingWrites(scope).length) {
    const write = pendingWrites(scope)[0]
    try {
      latestInventory = await sendPendingWrite(scope, write, signal)
    } catch (error) {
      if (error instanceof FridgeMappingTargetNotFoundError) removePendingWrite(scope, write)
      throw error
    }
    removePendingWrite(scope, write)
    if (write.key === stopAfterKey) break
  }
  if (latestInventory && pendingWrites(scope).length === 0) publish(scope, normalizeFridgeInventory(latestInventory))
  return latestInventory
}

export function getFridgeSnapshot(scope: string): FridgeSnapshot {
  const previous = cache.get(scope)
  if (previous) return previous
  let data = createEmptyFridgeRecords()
  let storageError: string | null = null
  try {
    const raw = typeof window === "undefined" ? null : window.localStorage.getItem(prefix + scope)
    if (raw !== null) data = decodeFridgeRecords(raw)
  } catch {
    storageError = "Saved fridge data could not be loaded. The fridge is empty until you add food or sync again."
  }
  const snapshot = { data, storageError }
  cache.set(scope, snapshot)
  return snapshot
}

export function subscribeFridgeRecords(scope: string, listener: () => void) {
  const group = listeners.get(scope) ?? new Set<() => void>()
  group.add(listener)
  listeners.set(scope, group)
  return () => {
    group.delete(listener)
    if (group.size === 0) listeners.delete(scope)
  }
}

export function updateFridgeRecords(scope: string, update: (current: FridgeRecords) => FridgeRecords) {
  const data = update(getFridgeSnapshot(scope).data)
  localRevisions.set(scope, (localRevisions.get(scope) ?? 0) + 1)
  let storageError: string | null = null
  try {
    if (typeof window === "undefined") throw new Error("Browser storage is unavailable.")
    window.localStorage.setItem(prefix + scope, JSON.stringify(data))
  } catch {
    storageError = "Could not save to this browser. Changes are kept for this session only."
  }
  const snapshot = { data, storageError }
  cache.set(scope, snapshot)
  listeners.get(scope)?.forEach((listener) => listener())
  return snapshot
}

function publish(scope: string, data: FridgeRecords, storageError: string | null = null) {
  try {
    if (typeof window === "undefined") throw new Error("Browser storage is unavailable.")
    window.localStorage.setItem(prefix + scope, JSON.stringify(data))
  } catch {
    storageError ??= "Could not save the latest fridge snapshot to this browser. It is available for this session only."
  }
  const snapshot = { data, storageError }
  cache.set(scope, snapshot)
  listeners.get(scope)?.forEach((listener) => listener())
  return snapshot
}

async function refreshAfterStorageChange(scope: string) {
  const refreshRevision = (refreshRevisions.get(scope) ?? 0) + 1
  refreshRevisions.set(scope, refreshRevision)
  const localRevision = localRevisions.get(scope) ?? 0
  try {
    const inventory = await fetchFridgeInventory()
    if (refreshRevisions.get(scope) !== refreshRevision || (localRevisions.get(scope) ?? 0) !== localRevision) return
    if (pendingWrites(scope).length) return
    publish(scope, normalizeFridgeInventory(inventory))
  } catch { /* Keep the last local snapshot until the next explicit sync. */ }
}

/** Pull workspace inventory into the local cache; successful server snapshots also refresh the offline cache. */
export async function syncFridgeRecords(scope: string, signal?: AbortSignal) {
  return serializeWrite(scope, async () => {
    try {
      await drainPendingWrites(scope, signal)
      const localRevision = localRevisions.get(scope) ?? 0
      const inventory = await fetchFridgeInventory(signal)
      if ((localRevisions.get(scope) ?? 0) !== localRevision) {
        return { snapshot: getFridgeSnapshot(scope), synced: true, error: null }
      }
      return { snapshot: publish(scope, normalizeFridgeInventory(inventory)), synced: true, error: null }
    } catch (error) {
      if (typeof DOMException !== "undefined" && error instanceof DOMException && error.name === "AbortError") throw error
      const message = error instanceof Error ? error.message : "Fridge service is unavailable."
      return { snapshot: getFridgeSnapshot(scope), synced: false, error: message }
    }
  })
}

/** Queue writes durably before sending them so failed requests can be retried on the next sync. */
function queueWrite(scope: string, write: PendingWrite, signal?: AbortSignal) {
  const persistenceError = persistPendingWrite(scope, write)
  return serializeWrite(scope, async () => {
    let latestInventory: Awaited<ReturnType<typeof sendPendingWrite>> | null = null
    try {
      latestInventory = await drainPendingWrites(scope, signal, write.key)
    } catch (error) {
      if (!persistenceError) throw error
      const message = error instanceof Error ? error.message : "Fridge service is unavailable."
      throw new Error(`${message} ${persistenceError}`, { cause: error })
    }
    if (pendingWrites(scope).length) return getFridgeSnapshot(scope)
    if (latestInventory) return publish(scope, normalizeFridgeInventory(latestInventory))
    try {
      const inventory = await fetchFridgeInventory(signal)
      return publish(scope, normalizeFridgeInventory(inventory))
    } catch {
      return getFridgeSnapshot(scope)
    }
  })
}

export function createFridgeItemRemote(scope: string, input: FridgeAddItemRequest, signal?: AbortSignal) {
  return queueWrite(scope, { key: `add:${input.idempotencyKey}`, kind: "add", input }, signal)
}

export function adjustFridgeItemRemote(scope: string, itemId: string, input: FridgeAdjustRequest, signal?: AbortSignal) {
  return queueWrite(scope, { key: `adjust:${input.idempotencyKey}`, kind: "adjust", itemId, input }, signal)
}

export function confirmFridgeItemMappingRemote(scope: string, itemId: string, input: FridgeMappingRequest, signal?: AbortSignal) {
  return queueWrite(scope, { key: `mapping:${itemId}:${input.kind}:${input.candidateId}`, kind: "mapping", itemId, input }, signal)
}

function decodeOutboxScope(key: string) {
  if (!key.startsWith(outboxItemPrefix)) return null
  const separator = key.indexOf("/", outboxItemPrefix.length)
  if (separator < 0) return null
  try { return decodeURIComponent(key.slice(outboxItemPrefix.length, separator)) } catch { return null }
}

if (typeof window !== "undefined") window.addEventListener("storage", (event) => {
  if (event.key !== null && !event.key.startsWith(prefix) && !event.key.startsWith(outboxPrefix)) return
  const scopes = event.key === null
    ? [...new Set([...cache.keys(), ...pendingCache.keys()])]
    : event.key.startsWith(outboxItemPrefix)
      ? [decodeOutboxScope(event.key)].filter((scope): scope is string => scope !== null)
      : [event.key.slice(event.key.startsWith(outboxPrefix) ? outboxPrefix.length : prefix.length)]

  scopes.forEach((scope) => {
    cache.delete(scope)
    pendingCache.delete(scope)
    listeners.get(scope)?.forEach((listener) => listener())
    const outboxCleared = event.key === null || (event.key.startsWith(outboxPrefix) && event.newValue === null)
    if (outboxCleared && pendingWrites(scope).length === 0) void refreshAfterStorageChange(scope)
  })
})
