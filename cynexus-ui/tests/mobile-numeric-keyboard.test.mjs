import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import test from "node:test"
import ts from "typescript"
import vm from "node:vm"

const model = {}
const code = ts.transpileModule(readFileSync(new URL("../src/mobile/components/numeric-keyboard/model.ts", import.meta.url), "utf8"), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 },
}).outputText
vm.runInNewContext(code, { exports: model })

test("numeric keypad handles digits, decimals, clear, delete, and maximum length", () => {
  const options = { decimal: true, maxLength: 5 }
  assert.equal(model.applyNumericKey("", "1", options), "1")
  assert.equal(model.applyNumericKey("1", ".", options), "1.")
  assert.equal(model.applyNumericKey("1.", "2", options), "1.2")
  assert.equal(model.applyNumericKey("1.2", ".", options), "1.2")
  assert.equal(model.applyNumericKey("1.2", "backspace", options), "1.")
  assert.equal(model.applyNumericKey("1.2", "clear", options), "")
  assert.equal(model.applyNumericKey("12345", "6", options), "12345")
})

test("numeric mode rejects decimal keys", () => {
  assert.equal(model.applyNumericKey("12", ".", { decimal: false }), "12")
})

test("mobile numeric controls are redirected before native focus", () => {
  const keyboard = readFileSync(new URL("../src/mobile/components/numeric-keyboard/index.tsx", import.meta.url), "utf8")
  assert.match(keyboard, /document\.addEventListener\("pointerdown", openForInput, true\)/)
  assert.match(keyboard, /event\.preventDefault\(\)/)
  assert.match(keyboard, /event\.target\.readOnly = true/)
  assert.match(keyboard, /input\.dispatchEvent\(new Event\("input", \{ bubbles: true \}\)\)/)
})
