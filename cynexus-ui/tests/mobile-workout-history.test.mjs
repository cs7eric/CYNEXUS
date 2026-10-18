import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import { test } from "node:test"
import vm from "node:vm"
import ts from "typescript"

const model = { exports: {} }
const code = ts.transpileModule(readFileSync(new URL("../src/mobile/pages/workout/lib/history.ts", import.meta.url), "utf8"), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 },
}).outputText
vm.runInNewContext(code, { module: model, exports: model.exports, Date })
const { historyKey, historyDate, historyBounds, historyData, shiftHistoryPeriod } = model.exports
const session = (date, category, minutes, split = null) => ({ id: date, date, category, minutes, split })

test("Monday weeks cross years and leap months retain every day", () => {
  const bounds = historyBounds(historyDate("2026-01-01"), "week")
  assert.equal(historyKey(bounds.start), "2025-12-29")
  assert.equal(historyKey(bounds.end), "2026-01-04")
  for (const [year, days] of [[2024, 29], [2025, 28]]) {
    const result = historyData([], historyDate(`${year}-02-10`), "month", "All", historyDate("2026-10-18"))
    assert.equal(result.buckets.length, days)
    assert.equal(result.days, days)
    assert.equal(result.buckets.at(-1).key, `${year}-02-${days}`)
  }
})

test("period navigation clamps to period starts instead of overflowing short months", () => {
  assert.equal(historyKey(shiftHistoryPeriod(historyDate("2026-01-31"), "month", 1)), "2026-02-01")
  assert.equal(historyKey(shiftHistoryPeriod(historyDate("2026-01-31"), "month", -1)), "2025-12-01")
  assert.equal(historyKey(shiftHistoryPeriod(historyDate("2024-02-29"), "year", 1)), "2025-01-01")
})

test("one filtered log drives buckets, total minutes, sessions and active days; future records are excluded", () => {
  const sessions = [session("2026-10-12", "Strength", 50, "Chest + Triceps"), session("2026-10-14", "Cardio", 40), session("2026-10-18", "Strength", 45, "Shoulders"), session("2026-10-19", "Strength", 60), session("2026-09-30", "Cardio", 30)]
  for (const [filter, minutes, count] of [["All", 135, 3], ["Strength", 95, 2], ["Cardio", 40, 1]]) {
    const data = historyData(sessions, historyDate("2026-10-18"), "month", filter, historyDate("2026-10-18"))
    assert.equal(data.minutes, minutes)
    assert.equal(data.records.length, count)
    assert.equal(data.activeDays, count)
    assert.equal(data.buckets.reduce((sum, bucket) => sum + bucket.minutes, 0), minutes)
    assert.equal(data.buckets[18].future, true)
    assert.equal(data.buckets[18].minutes, 0)
    assert.equal(data.buckets[12].minutes, 0)
  }
  assert.equal(sessions[0].split, "Chest + Triceps")
})

test("year includes twelve real monthly totals without fabricating missing or future training", () => {
  const data = historyData([session("2024-02-29", "Strength", 50), session("2024-03-10", "Cardio", 30), session("2024-03-11", "Strength", 40)], historyDate("2024-03-10"), "year", "All", historyDate("2024-03-10"))
  assert.equal(data.buckets.length, 12)
  assert.equal(data.days, 366)
  assert.equal(data.buckets[1].minutes, 50)
  assert.equal(data.buckets[2].minutes, 30)
  assert.equal(data.buckets[3].future, true)
  assert.equal(data.buckets[0].minutes, 0)
  assert.equal(data.minutes, 80)
})

test("local calendar bucketing remains consecutive across DST transitions", () => {
  const previous = process.env.TZ
  process.env.TZ = "America/New_York"
  try {
    for (const key of ["2026-03-08", "2026-11-01"]) {
      const data = historyData([], historyDate(key), "week", "All", historyDate("2026-12-31"))
      assert.equal(data.days, 7)
      assert.equal(data.buckets.length, 7)
      assert.equal(new Set(data.buckets.map(bucket => bucket.key)).size, 7)
      assert.equal(data.buckets[0].date.getDay(), 1)
      assert.equal(data.buckets[6].date.getDay(), 0)
    }
  } finally {
    if (previous === undefined) delete process.env.TZ
    else process.env.TZ = previous
  }
})
