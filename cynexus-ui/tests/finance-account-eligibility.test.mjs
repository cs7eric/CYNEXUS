import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import { test } from "node:test"
import vm from "node:vm"
import ts from "typescript"

const module = { exports: {} }
const code = ts.transpileModule(readFileSync(new URL("../src/services/finance/account-eligibility.ts", import.meta.url), "utf8"), { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
vm.runInNewContext(code, { module, exports: module.exports })
const { canUseFinanceAccountForTransaction } = module.exports

test("securities accounts only support transfers and cannot record income or expenses", () => {
  const account = { accountType: "investment" }
  assert.equal(canUseFinanceAccountForTransaction(account, "expense"), false)
  assert.equal(canUseFinanceAccountForTransaction(account, "transfer"), true)
  assert.equal(canUseFinanceAccountForTransaction(account, "income"), false)
})

test("ordinary asset and credit accounts keep their transaction eligibility", () => {
  for (const accountType of ["cash", "bank", "e_wallet", "huabei", "baitiao", "douyin_monthly", "credit_card", "other"]) {
    for (const type of ["expense", "income", "transfer"]) assert.equal(canUseFinanceAccountForTransaction({ accountType }, type), true, `${accountType}/${type}`)
  }
})
