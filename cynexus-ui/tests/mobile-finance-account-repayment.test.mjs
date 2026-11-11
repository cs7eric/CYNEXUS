import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import { test } from "node:test"
import vm from "node:vm"
import ts from "typescript"

function load(path, imports = {}) {
  const module = { exports: {} }
  const code = ts.transpileModule(readFileSync(new URL(path, import.meta.url), "utf8"), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 } }).outputText
  vm.runInNewContext(code, { module, exports: module.exports, require: id => { assert.ok(id in imports, id); return imports[id] } })
  return module.exports
}
const activity = load("../src/mobile/pages/finance/lib/activity.ts")
const entry = load("../src/mobile/pages/finance/lib/activity-entry.ts", { "./activity": activity })
const model = load("../src/mobile/pages/finance/lib/account-repayment.ts", { "./activity-entry": entry })
const asset = { id: 1, name: "Bank", nature: "asset", currency: "CNY", archived: false, balance: 1000 }
const credit = { id: 2, name: "花呗", nature: "credit", accountType: "huabei", currency: "CNY", archived: false, balance: 123.45 }
const input = overrides => ({ accounts: [asset, credit], creditAccountId: 2, paymentAccountId: "1", amount: "23.45", occurredAt: "2026-11-10T12:30", note: "", ...overrides })

test("repayment posts one asset-to-credit transfer with exact cents and date-specific local offset", () => {
  const result = model.buildAccountRepayment(input({ note: "  Payment  " })).value
  assert.equal(result.transactionType, "transfer")
  assert.equal(result.accountId, 1)
  assert.equal(result.toAccountId, 2)
  assert.equal(result.categoryId, null)
  assert.equal(result.amount, 23.45)
  assert.equal(result.currency, "CNY")
  assert.equal(result.merchant, "花呗")
  assert.equal(result.note, "Payment")
  assert.equal(result.occurredAt, entry.activityDateTimePayload("2026-11-10T12:30"))
  assert.equal(model.buildAccountRepayment(input()).value.note, "Credit account repayment")
})

test("payment sources must be active assets in the credit account's currency", () => {
  const accounts = [asset, credit, { ...asset, id: 3, archived: true }, { ...asset, id: 4, currency: "USD" },
    { ...credit, id: 5 }, { ...asset, id: 6, currency: " cny " }]
  assert.deepEqual(Array.from(model.accountRepaymentSources(credit, accounts), item => item.id), [1, 6])
  for (const paymentAccountId of ["2", "3", "4", "5", "99", ""]) assert.ok(model.buildAccountRepayment(input({ accounts, paymentAccountId })).error)
  const usdCredit = { ...credit, currency: "USD" }
  const usdAsset = { ...asset, currency: "USD" }
  assert.equal(model.buildAccountRepayment(input({ accounts: [usdAsset, usdCredit] })).value.currency, "USD")
})

test("each supported credit product can record a full or partial repayment", () => {
  for (const accountType of ["huabei", "baitiao", "douyin_monthly", "credit_card"]) {
    const accounts = [asset, { ...credit, accountType }]
    assert.equal(model.buildAccountRepayment(input({ accounts, amount: "123.45" })).value.amount, 123.45)
    assert.equal(model.buildAccountRepayment(input({ accounts, amount: "0.01" })).value.amount, 0.01)
  }
})

test("repayment rejects invalid precision, overpayment, unavailable balances and stale account choices", () => {
  for (const amount of ["", "0", "-1", "1e2", "0.001", "123.46", "90071992547409.91"]) assert.ok(model.buildAccountRepayment(input({ amount })).error, amount)
  for (const balance of [null, NaN, Infinity, 0, -10, 0.001]) assert.ok(model.buildAccountRepayment(input({ accounts: [asset, { ...credit, balance }] })).error, String(balance))
  for (const changed of [{ archived: true }, { nature: "asset" }, { id: 99 }]) assert.ok(model.buildAccountRepayment(input({ accounts: [asset, { ...credit, ...changed }] })).error)
  assert.ok(model.buildAccountRepayment(input({ occurredAt: "2026-02-30T12:30" })).error)
  assert.equal(model.accountRepaymentDueFen({ ...credit, balance: null }), null)
  assert.equal(model.accountRepaymentDueFen({ ...credit, balance: -10 }), 0)
  assert.equal(model.accountRepaymentDueFen(credit), 12345)
})
