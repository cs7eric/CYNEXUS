import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import test from "node:test"
import vm from "node:vm"
import ts from "typescript"

function harness({ failOnSessionCall = 0, blockFirstSession = false, readError = null, writeError = null } = {}) {
  const storage = new Map()
  const remote = { version: 1, isSample: false, cycleStart: "2026-10-20", sessions: [], observations: [] }
  const calls = { session: 0, plan: 0, read: 0, keys: [] }
  const timers = new Map()
  const events = new Map()
  let failOnce = failOnSessionCall
  let releaseFirstSession
  let signalFirstSession
  const firstSessionStarted = new Promise((resolve) => { signalFirstSession = resolve })
  const firstSessionGate = new Promise((resolve) => { releaseFirstSession = resolve })
  const clone = (value) => JSON.parse(JSON.stringify(value))
  const sessionIndex = (value) => remote.sessions.findIndex((item) => item.date === value.date)
  const api = {
    fetchWorkoutSnapshot: async () => { calls.read++; if (readError) throw readError; return clone(remote) },
    normalizeWorkoutSnapshot: (value) => value,
    saveWorkoutPlan: async ({ cycleStart }) => { calls.plan++; remote.cycleStart = cycleStart; return clone(remote) },
    saveWorkoutSession: async (value) => {
      calls.session++
      calls.keys.push(value.idempotencyKey)
      if (blockFirstSession && value.minutes === 30) { signalFirstSession(); await firstSessionGate; blockFirstSession = false }
      if (calls.session === failOnce) { failOnce = -1; throw writeError ?? new Error("temporary network failure") }
      const existing = sessionIndex(value)
      const session = { id: String(value.id ?? `remote-${calls.session}`), date: value.date, category: value.category, minutes: value.minutes, split: value.split ?? null }
      if (existing < 0) remote.sessions.push(session); else remote.sessions[existing] = session
      return clone(remote)
    },
    saveWorkoutMeasurement: async (value) => {
      const index = remote.observations.findIndex((item) => item.metric === value.metric && item.side === value.side && item.measuredAt === value.measuredAt)
      const observation = { ...value, id: String(value.id ?? "remote-observation") }
      if (index < 0) remote.observations.push(observation); else remote.observations[index] = observation
      return clone(remote)
    },
    deleteWorkoutSession: async (id) => { remote.sessions = remote.sessions.filter((item) => item.id !== String(id)); return clone(remote) },
    deleteWorkoutMeasurement: async (id) => { remote.observations = remote.observations.filter((item) => item.id !== id); return clone(remote) },
  }
  const modelModule = { exports: {} }
  const modelCode = ts.transpileModule(readFileSync(new URL("../src/services/workout/model.ts", import.meta.url), "utf8"), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 } }).outputText
  vm.runInNewContext(modelCode, { module: modelModule, exports: modelModule.exports, Date })
  const module = { exports: {} }
  const context = {
    module, exports: module.exports, Date, DOMException, Error,
    setTimeout: (callback, delay) => { const id = Symbol(); timers.set(id, { callback, delay }); return id },
    clearTimeout: (id) => timers.delete(id),
    crypto: { randomUUID: (() => { let id = 0; return () => `request-${++id}` })() },
    window: {
      localStorage: {
        getItem: (key) => storage.get(key) ?? null,
        setItem: (key, value) => storage.set(key, String(value)),
        removeItem: (key) => storage.delete(key),
      },
      addEventListener: (event, callback) => events.set(event, callback),
    },
    require: (name) => {
      if (name === "./model") return modelModule.exports
      if (name === "./api") return api
      throw new Error(`Unexpected module: ${name}`)
    },
  }
  const source = readFileSync(new URL("../src/services/workout/local-records.ts", import.meta.url), "utf8")
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 } }).outputText, context)
  return { records: context.module.exports, storage, remote, calls, firstSessionStarted, releaseFirstSession, timers, events, recoverRead: () => { readError = null } }
}

const today = new Date()
const todayKey = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`
const priorDay = new Date(today)
priorDay.setDate(priorDay.getDate() - 1)
const priorKey = `${priorDay.getFullYear()}-${String(priorDay.getMonth() + 1).padStart(2, "0")}-${String(priorDay.getDate()).padStart(2, "0")}`

function personalRecords(sessions) {
  return { version: 1, isSample: false, cycleStart: "2026-10-01", sessions, observations: [] }
}

test("interrupted legacy migration resumes missing records without duplicating completed writes", async () => {
  const h = harness({ failOnSessionCall: 2 })
  h.storage.set("cynexus.workout.records.v1.owner-a", JSON.stringify(personalRecords([
    { id: "legacy-a", date: priorKey, category: "Strength", minutes: 40, split: "Chest + Triceps" },
    { id: "legacy-b", date: todayKey, category: "Cardio", minutes: 25, split: null },
  ])))

  const first = await h.records.syncWorkoutRecords("owner-a")
  assert.equal(first.synced, false)
  assert.equal(h.remote.sessions.length, 1)
  const retry = await h.records.syncWorkoutRecords("owner-a")

  assert.equal(retry.synced, true)
  assert.deepEqual(h.remote.sessions.map((item) => item.date).sort(), [priorKey, todayKey].sort())
  assert.equal(h.calls.session, 3, "the completed first write is skipped on retry")
  assert.equal(h.storage.get("cynexus.workout.core-migration.v1.owner-a"), "complete")
})

test("failed remote writes stay queued and sync after connectivity returns", async () => {
  const h = harness({ failOnSessionCall: 1 })
  const records = personalRecords([{ id: "local-session", date: todayKey, category: "Strength", minutes: 45, split: "Shoulders" }])
  h.storage.set("cynexus.workout.records.v1.owner-b", JSON.stringify(records))
  await assert.rejects(h.records.saveWorkoutSessionRemote("owner-b", records.sessions[0]), /temporary network failure/)
  assert.equal(JSON.parse(h.storage.get("cynexus.workout.pending.v1.owner-b")).length, 1)

  const synced = await h.records.syncWorkoutRecords("owner-b")

  assert.equal(synced.synced, true)
  assert.equal(h.remote.sessions.length, 1)
  assert.equal(h.remote.sessions[0].minutes, 45)
  assert.equal(JSON.parse(h.storage.get("cynexus.workout.pending.v1.owner-b")).length, 0)
})

test("concurrent saves keep later queued writes while an earlier request is in flight", async () => {
  const h = harness({ blockFirstSession: true })
  const records = personalRecords([])
  h.storage.set("cynexus.workout.records.v1.owner-c", JSON.stringify(records))
  const first = h.records.saveWorkoutSessionRemote("owner-c", { date: todayKey, category: "Strength", minutes: 30, split: "Chest + Triceps" })
  await h.firstSessionStarted
  const second = h.records.saveWorkoutSessionRemote("owner-c", { date: todayKey, category: "Strength", minutes: 50, split: "Chest + Triceps" })
  h.releaseFirstSession()
  await Promise.all([first, second])

  assert.equal(h.remote.sessions.length, 1)
  assert.equal(h.remote.sessions[0].minutes, 50)
  assert.equal(JSON.parse(h.storage.get("cynexus.workout.pending.v1.owner-c")).length, 0)
})

test("transient 502 keeps optimistic records and stable queued keys, then shared retry persists recovery", async () => {
  const h = harness({ failOnSessionCall: 1, writeError: Object.assign(new Error("Workout sync is temporarily unavailable."), { retryable: true }) })
  const scope = "retry-owner"
  const records = personalRecords([{ id: "local", date: todayKey, category: "Strength", minutes: 45, split: null }])
  h.storage.set(`cynexus.workout.records.v1.${scope}`, JSON.stringify(records))
  h.storage.set(`cynexus.workout.core-migration.v1.${scope}`, "complete")
  const stop = h.records.subscribeWorkoutRecords(scope, () => {})
  const stopSecond = h.records.subscribeWorkoutRecords(scope, () => {})
  await h.records.syncWorkoutRecords(scope)
  h.records.updateWorkoutRecords(scope, () => records)
  await assert.rejects(h.records.saveWorkoutSessionRemote(scope, records.sessions[0]))
  const failed = h.records.getWorkoutSnapshot(scope)
  assert.equal(failed.pendingCount, 1)
  assert.equal(failed.storageError, null)
  assert.equal(failed.data.sessions[0].minutes, 45)
  assert.equal(failed.retryable, true)
  assert.equal(h.timers.size, 1, "subscribers share a single retry")
  const [timerId, timer] = [...h.timers][0]
  assert.equal(timer.delay, 10_000)
  h.timers.delete(timerId); timer.callback()
  await h.records.syncWorkoutRecords(scope)
  const recovered = h.records.getWorkoutSnapshot(scope)
  assert.equal(recovered.syncError, null)
  assert.equal(recovered.pendingCount, 0)
  assert.equal(recovered.syncing, false)
  assert.equal(h.calls.session, 2)
  assert.equal(h.calls.keys[0], h.calls.keys[1])
  assert.equal(JSON.parse(h.storage.get(`cynexus.workout.records.v1.${scope}`)).sessions[0].minutes, 45)
  stop(); stopSecond()
  assert.equal(h.timers.size, 0)
})

test("initial read failure is observable and focus recovery clears it", async () => {
  const h = harness({ readError: Object.assign(new Error("Workout sync is temporarily unavailable."), { retryable: true }) })
  const stop = h.records.subscribeWorkoutRecords("initial", () => {})
  const first = await h.records.syncWorkoutRecords("initial")
  assert.equal(first.synced, false)
  assert.match(first.snapshot.syncError, /temporarily unavailable/)
  assert.equal(first.snapshot.storageError, null)
  h.recoverRead(); h.events.get("focus")()
  await h.records.syncWorkoutRecords("initial")
  assert.equal(h.records.getWorkoutSnapshot("initial").syncError, null)
  stop()
})

test("abort resolves without a banner or an automatic retry", async () => {
  const h = harness({ readError: new DOMException("Aborted", "AbortError") })
  const stop = h.records.subscribeWorkoutRecords("abort", () => {})
  const result = await h.records.syncWorkoutRecords("abort")
  assert.equal(result.synced, false)
  assert.equal(result.snapshot.syncError, null)
  assert.equal(result.snapshot.syncing, false)
  assert.equal(h.timers.size, 0)
  stop()
})

test("terminal 4xx preserves queued writes without automatic replay", async () => {
  const h = harness({ failOnSessionCall: 1, writeError: Object.assign(new Error("Minutes must be valid."), { retryable: false }) })
  h.storage.set("cynexus.workout.core-migration.v1.terminal", "complete")
  const stop = h.records.subscribeWorkoutRecords("terminal", () => {})
  await h.records.syncWorkoutRecords("terminal")
  await assert.rejects(h.records.saveWorkoutSessionRemote("terminal", { date: todayKey, category: "Strength", minutes: 45 }), /Minutes must be valid/)
  assert.equal(h.records.getWorkoutSnapshot("terminal").retryable, false)
  assert.equal(h.records.getWorkoutSnapshot("terminal").pendingCount, 1)
  h.events.get("online")(); h.events.get("focus")()
  assert.equal(h.calls.session, 1)
  assert.equal(h.timers.size, 0)
  stop()
})

test("a remote response cannot replace an optimistic edit made while its save is in flight", async () => {
  const h = harness({ blockFirstSession: true })
  h.storage.set("cynexus.workout.core-migration.v1.edit", "complete")
  const session = { id: "local", date: todayKey, category: "Strength", minutes: 30, split: null }
  h.records.updateWorkoutRecords("edit", () => personalRecords([session]))
  const pending = h.records.saveWorkoutSessionRemote("edit", session)
  await h.firstSessionStarted
  h.records.updateWorkoutRecords("edit", () => personalRecords([{ ...session, minutes: 55 }]))
  h.releaseFirstSession(); await pending
  assert.equal(h.records.getWorkoutSnapshot("edit").data.sessions[0].minutes, 55)
})

function apiHarness(responses) {
  const paths = []
  const compile = (path, require) => {
    const module = { exports: {} }
    const code = ts.transpileModule(readFileSync(new URL(path, import.meta.url), "utf8"), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 } }).outputText
    vm.runInNewContext(code, { module, exports: module.exports, require, Error, DOMException, fetch: async (url) => { paths.push(url); const next = responses.shift(); if (next instanceof Error) throw next; return next } })
    return module.exports
  }
  const result = compile("../src/services/common/cynexus-result.ts")
  const api = compile("../src/services/workout/api.ts", (name) => name.endsWith("cynexus-result") ? result : { SERVICE_PREFIX: { core: "/api/core" } })
  return { api, paths }
}

test("workout API suppresses proxy HTML and does not fallback after 502", async () => {
  const h = apiHarness([new Response("<html><style>body {color:red}</style>Cloudflare Bad gateway</html>", { status: 502 })])
  await assert.rejects(h.api.fetchWorkoutSnapshot(), (error) => {
    assert.equal(error.status, 502)
    assert.equal(error.retryable, true)
    assert.doesNotMatch(error.message, /Cloudflare|style|color|<|>/)
    return true
  })
  assert.deepEqual(h.paths, ["/api/core/workout"])
})

test("workout API falls back only for unavailable legacy route and preserves validation details", async () => {
  const h = apiHarness([new Response("missing", { status: 404 }), new Response(JSON.stringify({ success: true, data: { sessions: [], observations: [] } }))])
  await h.api.fetchWorkoutSnapshot()
  assert.deepEqual(h.paths, ["/api/core/workout", "/api/core/workout/snapshot"])
  const invalid = apiHarness([new Response(JSON.stringify({ message: "Minutes must be between 1 and 1440" }), { status: 400 })])
  await assert.rejects(invalid.api.saveWorkoutSession({}), (error) => error.retryable === false && error.message === "Minutes must be between 1 and 1440")
})

test("workout API preserves abort and normalizes a network failure", async () => {
  const abort = apiHarness([new DOMException("Aborted", "AbortError")])
  await assert.rejects(abort.api.fetchWorkoutSnapshot(), (error) => error.name === "AbortError")
  assert.equal(abort.paths.length, 1)
  const offline = apiHarness([new TypeError("Failed to fetch")])
  await assert.rejects(offline.api.fetchWorkoutSnapshot(), (error) => error.retryable && /saved on this device/.test(error.message))
})

test("remount restarts the bounded recovery budget without duplicating retry timers", async () => {
  const h = harness({ readError: Object.assign(new Error("Temporary outage"), { retryable: true }) })
  const stop = h.records.subscribeWorkoutRecords("remount", () => {})
  await h.records.syncWorkoutRecords("remount")
  for (let attempt = 0; attempt < 6; attempt++) {
    assert.equal(h.timers.size, 1)
    const [id, timer] = [...h.timers][0]
    h.timers.delete(id); timer.callback()
    await h.records.syncWorkoutRecords("remount")
  }
  assert.equal(h.timers.size, 0, "automatic retry budget is exhausted")
  stop()
  const stopNew = h.records.subscribeWorkoutRecords("remount", () => {})
  const stopSecond = h.records.subscribeWorkoutRecords("remount", () => {})
  await h.records.syncWorkoutRecords("remount")
  assert.equal(h.timers.size, 1)
  assert.equal([...h.timers.values()][0].delay, 10_000)
  stopNew(); stopSecond()
  assert.equal(h.timers.size, 0)
})

test("workout API preserves aborts during successful and error response body reads", async () => {
  const bodyAbort = new DOMException("Body read aborted", "AbortError")
  const success = apiHarness([{ ok: true, text: async () => { throw bodyAbort } }])
  await assert.rejects(success.api.fetchWorkoutSnapshot(), (error) => error === bodyAbort)
  assert.equal(success.paths.length, 1, "body abort must not trigger legacy fallback")
  const failure = apiHarness([{ ok: false, status: 502, clone: () => ({ json: async () => { throw bodyAbort } }) }])
  await assert.rejects(failure.api.fetchWorkoutSnapshot(), (error) => error === bodyAbort)
  assert.equal(failure.paths.length, 1)
})
