import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import test from "node:test"
import vm from "node:vm"
import ts from "typescript"

function harness({ failOnSessionCall = 0, blockFirstSession = false } = {}) {
  const storage = new Map()
  const remote = { version: 1, isSample: false, cycleStart: "2026-10-20", sessions: [], observations: [] }
  const calls = { session: 0, plan: 0 }
  let failOnce = failOnSessionCall
  let releaseFirstSession
  let signalFirstSession
  const firstSessionStarted = new Promise((resolve) => { signalFirstSession = resolve })
  const firstSessionGate = new Promise((resolve) => { releaseFirstSession = resolve })
  const clone = (value) => JSON.parse(JSON.stringify(value))
  const sessionIndex = (value) => remote.sessions.findIndex((item) => item.date === value.date)
  const api = {
    fetchWorkoutSnapshot: async () => clone(remote),
    normalizeWorkoutSnapshot: (value) => value,
    saveWorkoutPlan: async ({ cycleStart }) => { calls.plan++; remote.cycleStart = cycleStart; return clone(remote) },
    saveWorkoutSession: async (value) => {
      calls.session++
      if (blockFirstSession && value.minutes === 30) { signalFirstSession(); await firstSessionGate; blockFirstSession = false }
      if (calls.session === failOnce) { failOnce = -1; throw new Error("temporary network failure") }
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
    module, exports: module.exports, Date, DOMException,
    crypto: { randomUUID: (() => { let id = 0; return () => `request-${++id}` })() },
    window: {
      localStorage: {
        getItem: (key) => storage.get(key) ?? null,
        setItem: (key, value) => storage.set(key, String(value)),
        removeItem: (key) => storage.delete(key),
      },
      addEventListener: () => {},
    },
    require: (name) => {
      if (name === "./model") return modelModule.exports
      if (name === "./api") return api
      throw new Error(`Unexpected module: ${name}`)
    },
  }
  const source = readFileSync(new URL("../src/services/workout/local-records.ts", import.meta.url), "utf8")
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 } }).outputText, context)
  return { records: context.module.exports, storage, remote, calls, firstSessionStarted, releaseFirstSession }
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
