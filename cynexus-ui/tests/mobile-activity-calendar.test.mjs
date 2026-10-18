import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import { test } from "node:test"
import vm from "node:vm"
import ts from "typescript"

const model = { exports: {} }
const filename = new URL("../src/mobile/components/activity-calendar/model.ts", import.meta.url)
const code = ts.transpileModule(readFileSync(filename, "utf8"), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 },
}).outputText
vm.runInNewContext(code, { module: model, exports: model.exports })
const { activityDateKey, activityDateFromKey, shiftActivityDate, activityMonthStart, shiftActivityMonth, activityCalendarDays, calculateBmi } = model.exports

test("BMI is derived from valid metric weight and height only", () => {
  assert.equal(calculateBmi(72.3, 178), 72.3 / (1.78 * 1.78))
  assert.equal(calculateBmi(null, 178), null)
  assert.equal(calculateBmi(72.3, null), null)
  assert.equal(calculateBmi(72.3, 0), null)
  assert.equal(calculateBmi(-1, 178), null)
  assert.equal(calculateBmi(Number.NaN, 178), null)
})

test("month view covers the whole calendar month, aligned to weeks, across leap years and year boundaries", () => {
  for (const [anchor, lastDay, count] of [
    ["2026-10-17", "2026-10-31", 31],
    ["2026-01-03", "2026-01-31", 31],
    ["2025-12-31", "2025-12-31", 31],
    ["2024-02-29", "2024-02-29", 29],
    ["2025-02-28", "2025-02-28", 28],
    ["2026-02-15", "2026-02-28", 28],
    ["2026-08-31", "2026-08-31", 31],
    ["2026-04-15", "2026-04-30", 30],
  ]) {
    const days = activityCalendarDays(activityDateFromKey("2026-12-31"), activityDateFromKey(anchor), true)
    const selectable = days.filter((day) => day.available)
    assert.equal(selectable.length, count, anchor)
    assert.equal(selectable[0].key, `${anchor.slice(0, 7)}-01`)
    assert.equal(selectable.at(-1).key, lastDay)
    assert.equal(new Set(selectable.map((day) => day.key)).size, count)
    assert.equal(days[0].date.getDay(), 0)
    assert.equal(days.at(-1).date.getDay(), 6)
    assert.equal(days.length % 7, 0)
  }
})

test("weeks follow historical selections and future dates stay disabled in both views", () => {
  const sunday = activityDateFromKey("2026-10-18")
  const current = activityCalendarDays(sunday, sunday, false)
  assert.equal(current.length, 7)
  assert.equal(current[0].key, "2026-10-18")
  assert.equal(current.filter((day) => day.available).length, 1)

  const earlier = activityCalendarDays(activityDateFromKey("2026-10-17"), activityDateFromKey("2026-09-18"), false)
  assert.equal(earlier[0].key, "2026-09-13")
  assert.equal(earlier.at(-1).key, "2026-09-19")
  assert.equal(earlier.filter((day) => day.available).length, 7)

  const today = activityDateFromKey("2026-10-17")
  const currentMonth = activityCalendarDays(today, today, true)
  assert.equal(currentMonth.filter((day) => day.available).length, 17)
  const futureMonth = activityCalendarDays(today, activityDateFromKey("2026-11-01"), true)
  assert.equal(futureMonth.filter((day) => day.available).length, 0)
  const oldWeek = activityCalendarDays(today, activityDateFromKey("2024-02-29"), false)
  assert.equal(oldWeek.filter((day) => day.available).length, 7)
})

test("month navigation starts on day one and does not overflow on the 29th, 30th or 31st", () => {
  for (const [start, offset, expected] of [
    ["2026-01-31", 1, "2026-02-01"],
    ["2024-03-31", -1, "2024-02-01"],
    ["2024-02-29", 12, "2025-02-01"],
    ["2026-12-31", 1, "2027-01-01"],
    ["2026-01-30", -1, "2025-12-01"],
  ]) {
    const date = activityDateFromKey(start)
    const before = date.getTime()
    assert.equal(activityDateKey(shiftActivityMonth(date, offset)), expected)
    assert.equal(activityDateKey(activityMonthStart(date)), `${start.slice(0, 7)}-01`)
    assert.equal(date.getTime(), before)
  }
})

test("local date keys survive DST changes without skipping dates or mutating inputs", () => {
  for (const date of [new Date(2024, 2, 10, 23, 30), new Date(2024, 10, 3, 0, 30)]) {
    const before = date.getTime()
    const days = activityCalendarDays(activityDateFromKey("2026-12-31"), date, true).filter((day) => day.available)
    assert.equal(date.getTime(), before)
    assert.equal(days.length, date.getMonth() === 2 ? 31 : 30)
    assert.equal(days[0].date.getDate(), 1)
    for (let index = 1; index < days.length; index++) {
      assert.equal(activityDateKey(shiftActivityDate(days[index - 1].date, 1)), days[index].key)
    }
  }
})
