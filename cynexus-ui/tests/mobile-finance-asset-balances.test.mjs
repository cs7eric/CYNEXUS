import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import { test } from "node:test"
import vm from "node:vm"
import ts from "typescript"

function loadModel(path, imports = {}) {
  const model = { exports: {} }
  const code = ts.transpileModule(readFileSync(new URL(path, import.meta.url), "utf8"), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 },
  }).outputText
  vm.runInNewContext(code, {
    module: model,
    exports: model.exports,
    require: (id) => {
      assert.ok(id in imports, `Unexpected runtime dependency: ${id}`)
      return imports[id]
    },
  })
  return model.exports
}

const pile = loadModel("../src/mobile/pages/finance/lib/asset-pile.ts")
const { summarizeCnyAccounts } = loadModel("../src/mobile/pages/finance/lib/asset-balances.ts", { "./asset-pile": pile })
const account = (balance, overrides = {}) => ({ id: 1, currency: "CNY", nature: "asset", archived: false, balance, ...overrides })
const comparable = (value) => JSON.parse(JSON.stringify(value))

test("CNY balances are aggregated by nature while other currencies are counted separately", () => {
  const summary = summarizeCnyAccounts([
    account(100.25),
    account(20.1, { currency: " cny " }),
    account(50.01, { nature: "credit" }),
    account(9999, { currency: "USD" }),
    account(200, { currency: "HKD" }),
    account(100, { currency: "" }),
    account(100, { currency: null }),
  ])
  assert.deepEqual(comparable(summary), { assetFen: 12035, liabilityFen: 5001, netFen: 7034, excludedCount: 4, missingAssetCount: 0, missingCreditCount: 0, cnyCount: 3 })
})

test("an unclassified current CNY account makes both sides incomplete", () => {
  const summary = summarizeCnyAccounts([account(100), account(20, { nature: "credit" }), account(50, { nature: "unknown" })])
  assert.deepEqual(comparable(summary), { assetFen: null, liabilityFen: null, netFen: null, excludedCount: 0, missingAssetCount: 1, missingCreditCount: 1, cnyCount: 3 })
})

test("negative assets and credit overpayments retain their signs", () => {
  const summary = summarizeCnyAccounts([account(-50.5), account(-10.25, { nature: "credit" })])
  assert.equal(summary.assetFen, -5050)
  assert.equal(summary.liabilityFen, -1025)
  assert.equal(summary.netFen, -4025)
})

test("unknown account balances invalidate only the corresponding subtotal and never become zero", () => {
  const assets = summarizeCnyAccounts([account(100), account(null), account(NaN), account(Infinity), account(1e20), account(25, { nature: "credit" })])
  assert.equal(assets.assetFen, null)
  assert.equal(assets.liabilityFen, 2500)
  assert.equal(assets.netFen, null)
  assert.equal(assets.missingAssetCount, 4)
  assert.equal(assets.cnyCount, 6)

  const credit = summarizeCnyAccounts([account(100), account(undefined, { nature: "credit" }), account("50", { nature: "credit" })])
  assert.equal(credit.assetFen, 10000)
  assert.equal(credit.liabilityFen, null)
  assert.equal(credit.netFen, null)
  assert.equal(credit.missingCreditCount, 2)
})

test("archived accounts are ignored and no current CNY accounts produce zero totals", () => {
  const expected = { assetFen: 0, liabilityFen: 0, netFen: 0, excludedCount: 0, missingAssetCount: 0, missingCreditCount: 0, cnyCount: 0 }
  assert.deepEqual(comparable(summarizeCnyAccounts([])), expected)
  assert.deepEqual(comparable(summarizeCnyAccounts([account(null, { archived: true }), account(500, { archived: true, nature: "credit" }), account(999, { archived: true, currency: "USD" })])), expected)
})

test("subtotals and net assets reject unsafe sums without losing exact cents or depending on account order", () => {
  const large = 50_000_000_000_000
  const unsafeAssets = summarizeCnyAccounts([account(large), account(large), account(0, { nature: "credit" })])
  assert.equal(unsafeAssets.assetFen, null)
  assert.equal(unsafeAssets.liabilityFen, 0)
  assert.equal(unsafeAssets.netFen, null)
  assert.equal(unsafeAssets.missingAssetCount, 0)

  const unsafeCredits = summarizeCnyAccounts([account(1), account(large, { nature: "credit" }), account(large, { nature: "credit" })])
  assert.equal(unsafeCredits.assetFen, 100)
  assert.equal(unsafeCredits.liabilityFen, null)
  assert.equal(unsafeCredits.netFen, null)

  const unsafeNet = summarizeCnyAccounts([account(large), account(-large, { nature: "credit" })])
  assert.equal(unsafeNet.assetFen, 5_000_000_000_000_000)
  assert.equal(unsafeNet.liabilityFen, -5_000_000_000_000_000)
  assert.equal(unsafeNet.netFen, null)

  const cancellation = [account(large), account(large), account(-large), account(1.005)]
  const summary = summarizeCnyAccounts(cancellation)
  assert.equal(summary.assetFen, 5_000_000_000_000_101)
  assert.deepEqual(comparable(summary), comparable(summarizeCnyAccounts(cancellation.toReversed())))
})
