import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import test from "node:test"
import vm from "node:vm"
import ts from "typescript"

const source = readFileSync(new URL("../src/mobile/pages/tasks/lib/summary.ts", import.meta.url), "utf8")
const output = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 } }).outputText
const module = { exports: {} }
vm.runInNewContext(output, { module, exports: module.exports })
const summary = module.exports

test("current week and month ranges use the local calendar and clamp to today", () => {
  const now = new Date(2026, 9, 29, 17)
  const week = summary.taskSummaryRange("week", 0, now)
  const previousWeek = summary.previousTaskSummaryRange(week, "week")
  const month = summary.taskSummaryRange("month", 0, now)
  const previousMonth = summary.previousTaskSummaryRange(month, "month")

  assert.equal(week.start.toDateString(), new Date(2026, 9, 26, 12).toDateString())
  assert.equal(week.end.toDateString(), new Date(2026, 9, 29, 12).toDateString())
  assert.equal(previousWeek.start.toDateString(), new Date(2026, 9, 19, 12).toDateString())
  assert.equal(previousWeek.end.toDateString(), new Date(2026, 9, 25, 12).toDateString())
  assert.equal(month.end.getDate(), 29)
  assert.equal(previousMonth.end.getDate(), 29)
  assert.equal(summary.taskSummaryDays(month).length, 29)
})

test("a capped source only makes a period incomplete when its oldest returned task overlaps that period", () => {
  const range = summary.taskSummaryRange("month", 0, new Date(2026, 9, 29, 12))
  assert.equal(summary.isTaskSummaryIncomplete([{ name: "Vision", hasMore: false }], range), false)
  assert.equal(summary.isTaskSummaryIncomplete([{ name: "Vision", hasMore: true, oldestAt: "2026-10-15T08:00:00Z" }], range), true)
  assert.equal(summary.isTaskSummaryIncomplete([{ name: "Vision", hasMore: true, oldestAt: "2026-09-30T08:00:00Z" }], range), false)
  assert.equal(summary.isTaskSummaryIncomplete([], range, ["Image analysis"]), true)
})

test("period statistics exclude cancelled tasks from the success rate and use the median timed success", () => {
  const tasks = [
    { status: "SUCCEEDED", createdAt: "2026-10-01T08:00:00Z", startedAt: "2026-10-01T08:00:00Z", completedAt: "2026-10-01T08:00:20Z" },
    { status: "SUCCEEDED", createdAt: "2026-10-02T08:00:00Z", startedAt: "2026-10-02T08:00:00Z", completedAt: "2026-10-02T08:02:00Z" },
    { status: "FAILED", createdAt: "2026-10-03T08:00:00Z" },
    { status: "WAITING_REVIEW", createdAt: "2026-10-04T08:00:00Z" },
    { status: "RUNNING", createdAt: "2026-10-05T08:00:00Z" },
    { status: "CANCELLED", createdAt: "2026-10-06T08:00:00Z" },
  ]
  const stats = summary.summarizeTasks(tasks)

  assert.equal(stats.total, 6)
  assert.equal(stats.closed, 3)
  assert.ok(Math.abs(stats.rate - 200 / 3) < Number.EPSILON * 100)
  assert.equal(stats.review, 1)
  assert.equal(stats.active, 1)
  assert.equal(stats.medianSeconds, 70)
  assert.equal(stats.timedTasks, 2)
})
