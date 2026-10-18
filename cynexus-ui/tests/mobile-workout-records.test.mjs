import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import test from "node:test"
import vm from "node:vm"
import ts from "typescript"

const module = { exports: {} }
vm.runInNewContext(ts.transpileModule(readFileSync(new URL("../src/services/workout/model.ts", import.meta.url), "utf8"), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 } }).outputText, { module, exports: module.exports, Date })
const m = module.exports
const today = "2026-10-18"
const empty = () => ({ version: 1, isSample: false, cycleStart: "2026-10-12", sessions: [], observations: [] })
const observation = (overrides = {}) => ({ metric: "upperArm", side: "left", value: 32, measuredAt: today, ...overrides })

test("personal measurements exclude fixtures, distinguish sides and replace same-day readings", () => {
  const sample = m.createSampleWorkoutRecords(m.parseDate(today))
  let data = m.saveObservation(sample, observation(), today, "a")
  assert.equal(data.isSample, false)
  assert.equal(data.sessions.length, 0)
  assert.equal(data.observations.length, 1)
  data = m.saveObservation(data, observation({ side: "right", value: 33 }), today, "b")
  data = m.saveObservation(data, observation({ value: 34 }), today, "c")
  assert.equal(data.observations.length, 2)
  assert.equal(m.latestObservation(data.observations, "upperArm", "left", today).value, 34)
  assert.equal(m.latestObservation(data.observations, "upperArm", "right", today).value, 33)
})

test("historical measurement selection never uses a future reading or another side", () => {
  const data = [{ id: "a", ...observation({ measuredAt: "2026-10-01", value: 30 }) }, { id: "b", ...observation() }]
  assert.equal(m.latestObservation(data, "upperArm", "left", "2026-10-10").value, 30)
  assert.equal(m.latestObservation(data, "upperArm", "left", "2026-09-30"), null)
  assert.equal(m.latestObservation(data, "upperArm", "right", today), null)
})

test("the three-on one-off cycle keeps historical split snapshots when its start changes", () => {
  let data = empty()
  data = m.saveSession(data, { date: "2026-10-12", category: "Strength", minutes: 45 }, today, "w")
  assert.equal(data.sessions[0].split, "Chest + Triceps")
  data = m.saveSession({ ...data, cycleStart: "2026-10-10" }, { date: "2026-10-12", category: "Strength", minutes: 55 }, today, "new")
  assert.equal(data.sessions.length, 1)
  assert.equal(data.sessions[0].split, "Chest + Triceps")
  assert.equal(data.sessions[0].minutes, 55)
  assert.equal(m.splitOnDate("2026-10-12", "2026-10-15"), "Rest")
  assert.equal(m.splitOnDate("2026-10-12", "2026-10-16"), "Chest + Triceps")
  assert.equal(m.splitOnDate("2026-10-12", "2026-10-11"), null)
})

test("invalid dates, missing sides, future values and impossible durations are rejected", () => {
  for (const bad of [observation({ side: null }), observation({ measuredAt: "2026-02-30" }), observation({ measuredAt: "2026-10-19" }), observation({ value: NaN }), observation({ metric: "waist", side: "left" })]) assert.throws(() => m.saveObservation(empty(), bad, today, "x"))
  for (const minutes of [0, -1, 1.5, 1441, Infinity]) assert.throws(() => m.saveSession(empty(), { date: today, category: "Strength", minutes }, today, "x"))
  assert.throws(() => m.decodeWorkoutRecords('{"version":2}', today))
  assert.equal(m.decodeWorkoutRecords(JSON.stringify(empty()), today).version, 1)
})

test("leap dates and cross-year cycle calculations use calendar days", () => {
  assert.equal(m.isDateKey("2024-02-29"), true)
  assert.equal(m.isDateKey("2026-02-29"), false)
  assert.equal(m.dateKey(m.addDays(m.parseDate("2024-02-28"), 1)), "2024-02-29")
  assert.equal(m.dayDifference(m.parseDate("2025-12-31"), m.parseDate("2026-01-01")), 1)
})
