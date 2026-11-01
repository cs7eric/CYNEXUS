import test from "node:test"
import assert from "node:assert/strict"
import { groupCategories, groupDays, summarizeRows } from "./lib/summary-logic.mjs"

const records = [
  { id: 1, transactionType: "expense", categoryId: 7, categoryName: "Food", accountId: 2, toAccountId: null, amount: 12.5, currency: "CNY", occurredAt: "2026-05-02T04:00:00Z", archived: false },
  { id: 2, transactionType: "income", categoryId: 7, categoryName: "Salary", accountId: 2, toAccountId: null, amount: 100, currency: "CNY", occurredAt: "2026-05-02T12:00:00Z", archived: false },
  { id: 3, transactionType: "transfer", categoryId: null, categoryName: null, accountId: 2, toAccountId: 3, amount: 20, currency: "CNY", occurredAt: "2026-05-03T12:00:00Z", archived: false },
]

test("income and expense category IDs remain separate and transfers are excluded", () => {
  assert.deepEqual(groupCategories(records, "expense"), [{ id: 7, label: "Food", amount: 12.5 }])
  assert.deepEqual(groupCategories(records, "income"), [{ id: 7, label: "Salary", amount: 100 }])
})

test("account totals include either side of a transfer only as ledger records, never cash flow", () => {
  assert.deepEqual(summarizeRows(records, { accountId: 2 }), { income: 100, expense: 12.5, net: 87.5 })
  assert.deepEqual(summarizeRows(records, { accountId: 3 }), { income: 0, expense: 0, net: 0 })
})

test("incomplete coverage suppresses totals and daily series use local calendar dates", () => {
  assert.equal(summarizeRows(records, { complete: false }), null)
  assert.deepEqual(groupDays(records, true).map(({ label, income, expense }) => ({ label, income, expense })), [
    { label: "02", income: 100, expense: 12.5 },
    { label: "03", income: 0, expense: 0 },
  ])
})

test("invalid sub-cent and unsafe amounts do not enter summary totals", () => {
  const invalid = [{ ...records[0], amount: 0.001 }]
  assert.equal(summarizeRows(invalid), null)
  assert.equal(summarizeRows([records[0], { ...records[1], amount: 0.001 }]), null)
  assert.deepEqual(groupCategories(invalid, "expense"), [])
})
