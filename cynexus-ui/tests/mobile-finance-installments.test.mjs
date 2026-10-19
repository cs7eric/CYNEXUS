import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import { test } from "node:test"
import vm from "node:vm"
import ts from "typescript"

function load(path, imports = {}) {
  const model = { exports: {} }
  const code = ts.transpileModule(readFileSync(new URL(path, import.meta.url), "utf8"), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 } }).outputText
  vm.runInNewContext(code, { module: model, exports: model.exports, require: id => { assert.ok(id in imports, id); return imports[id] } })
  return model.exports
}
const activity = load("../src/mobile/pages/finance/lib/activity.ts")
const entry = load("../src/mobile/pages/finance/lib/activity-entry.ts", { "./activity": activity })
const model = load("../src/mobile/pages/finance/lib/activity-installments.ts", { "./activity": activity, "./activity-entry": entry })
const input = overrides => ({ totalFen: 10000, feeText: "0", countText: "3", firstDueDate: "2026-01-31", intervalText: "1", ...overrides })
const plain = value => JSON.parse(JSON.stringify(value))

test("preview allocates principal and fee remainders to the final installment like the backend", () => {
  const result = model.buildActivityInstallmentTerms(input({ feeText: "1.00" })).value
  assert.deepEqual(plain(result.schedule), [
    { dueDate: "2026-01-31", principalFen: 3333, feeFen: 33, amountFen: 3366 },
    { dueDate: "2026-02-28", principalFen: 3333, feeFen: 33, amountFen: 3366 },
    { dueDate: "2026-03-31", principalFen: 3334, feeFen: 34, amountFen: 3368 },
  ])
  assert.equal(result.schedule.reduce((sum, row) => sum + row.principalFen, 0), 10000)
  assert.equal(result.schedule.reduce((sum, row) => sum + row.feeFen, 0), 100)
})

test("schedule dates stay anchored to the first due day and honor leap years and intervals", () => {
  const leap = model.buildActivityInstallmentTerms(input({ firstDueDate: "2024-01-31", countText: "3" })).value
  assert.deepEqual(plain(leap.schedule).map(row => row.dueDate), ["2024-01-31", "2024-02-29", "2024-03-31"])
  const yearly = model.buildActivityInstallmentTerms(input({ firstDueDate: "2024-02-29", intervalText: "12", countText: "5" })).value
  assert.deepEqual(plain(yearly.schedule).map(row => row.dueDate), ["2024-02-29", "2025-02-28", "2026-02-28", "2027-02-28", "2028-02-29"])
})

test("each principal is at least one cent, including the database constraint missed by backend policy", () => {
  assert.match(model.buildActivityInstallmentTerms(input({ totalFen: 2, countText: "3" })).error, /at least/)
  assert.deepEqual(plain(model.buildActivityInstallmentTerms(input({ totalFen: 2, countText: "2" })).value.schedule).map(row => row.principalFen), [1, 1])
})

test("count and interval must be bounded whole numbers", () => {
  for (const countText of ["", "1", "61", "2.5", "2e1", "-2"]) assert.ok(model.buildActivityInstallmentTerms(input({ countText })).error, countText)
  for (const intervalText of ["", "0", "13", "1.5", "1e1", "-1"]) assert.ok(model.buildActivityInstallmentTerms(input({ intervalText })).error, intervalText)
  assert.equal(model.buildActivityInstallmentTerms(input({ countText: "60", intervalText: "12" })).value.schedule.length, 60)
})

test("total fees accept explicit zero or blank and reject unsafe, negative or subcent values", () => {
  for (const feeText of ["", "0", "0.0", "0.00", " 000.00 "]) assert.equal(model.buildActivityInstallmentTerms(input({ feeText })).value.feeFen, 0)
  for (const feeText of ["-1", "0.001", "1e2", "90071992547409.91"]) assert.ok(model.buildActivityInstallmentTerms(input({ feeText })).error, feeText)
  assert.ok(model.buildActivityInstallmentTerms(input({ totalFen: 9007199254740990, feeText: "0.02" })).error)
  for (const totalFen of [0, -1, NaN, Infinity, 1.5, Number.MAX_SAFE_INTEGER + 1, Number.MAX_SAFE_INTEGER]) assert.ok(model.buildActivityInstallmentTerms(input({ totalFen })).error)
})

test("invalid first dates and schedules beyond supported years are rejected", () => {
  for (const firstDueDate of ["", "2026-02-29", "2026-04-31", "2026-13-01", "0000-01-01", "2026-01-01T00:00"]) assert.ok(model.buildActivityInstallmentTerms(input({ firstDueDate })).error, firstDueDate)
  assert.ok(model.buildActivityInstallmentTerms(input({ firstDueDate: "9999-12-31" })).error)
})

test("plans are scoped via CNY credit account ids rather than inferred from a currency-less plan", () => {
  const accounts = [{ id: 1, nature: "credit", currency: "CNY" }, { id: 2, nature: "credit", currency: "USD" }, { id: 3, nature: "asset", currency: "CNY" }, { id: 4, nature: "credit", currency: " cny " }]
  const result = model.getCnyCreditInstallmentPlans([{ id: 10, accountId: 1 }, { id: 11, accountId: 2 }, { id: 12, accountId: 3 }, { id: 13, accountId: 4 }, { id: 14, accountId: 99 }], accounts)
  assert.deepEqual(plain(result.plans).map(plan => plan.id), [10, 13])
  assert.equal(result.excludedCount, 3)
})

test("repayment account choices are active CNY assets only", () => {
  const accounts = [{ id: 1, nature: "asset", currency: "CNY", archived: false }, { id: 2, nature: "credit", currency: "CNY" }, { id: 3, nature: "asset", currency: "USD" }, { id: 4, nature: "asset", currency: "CNY", archived: true }]
  assert.deepEqual(plain(model.getCnyAssetPaymentAccounts(accounts)).map(account => account.id), [1])
})
