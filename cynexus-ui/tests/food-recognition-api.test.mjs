import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import test from "node:test"
import vm from "node:vm"
import ts from "typescript"

function apiWith(response) {
  const calls = []
  const module = { exports: {} }
  const source = readFileSync(new URL("../src/services/food/recognition.ts", import.meta.url), "utf8")
  const output = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 } }).outputText
  const dependencies = {
    "@/services/common/cynexus-result": { cynexusResult: async value => value },
    "@/services/common/service-prefix": { SERVICE_PREFIX: { core: "/api/core" } },
  }
  vm.runInNewContext(output, {
    module, exports: module.exports, require: name => dependencies[name],
    fetch: async (url, init) => { calls.push({ url, init }); return response },
  })
  return { api: module.exports, calls }
}

test("food retries use the complete food workflow and normalize the queued task", async () => {
  const h = apiWith({ id: "42", status: "QUEUED", retryable: false })
  const task = await h.api.retryFoodRecognitionTask("42")
  assert.equal(h.calls[0].url, "/api/core/food/recognition-tasks/42/retry")
  assert.equal(h.calls[0].init.method, "POST")
  assert.equal(task.processId, "42")
  assert.equal(task.status, "queued")
})

test("history preserves retry eligibility and hides no workflow failure", async () => {
  const h = apiWith([{ id: "42", status: "failed", retryable: true, errorMessage: "Analysis unavailable" }])
  const [task] = await h.api.listFoodRecognitionTasks()
  assert.equal(task.retryable, true)
  assert.equal(task.errorMessage, "Analysis unavailable")
})

test("internal vision attempts remain owned by the food workflow even if its history is unavailable", () => {
  const { api } = apiWith([])
  assert.equal(api.isFoodRecognitionVisionTask({ idempotencyKey: "food-recognition:901:0" }), true)
  assert.equal(api.isFoodRecognitionVisionTask({ idempotencyKey: "food-recognition:901:2" }), true)
  assert.equal(api.isFoodRecognitionVisionTask({ idempotencyKey: "standalone-label" }), false)
  assert.equal(api.isFoodRecognitionVisionTask({}), false)
})

test("malformed tasks cannot silently become successful history entries", async () => {
  for (const response of [{ id: "42", status: "unknown" }, { id: "invalid", status: "queued" }]) {
    const h = apiWith([response])
    await assert.rejects(h.api.listFoodRecognitionTasks(), /invalid/)
  }
  await assert.rejects(apiWith({}).api.listFoodRecognitionTasks(), /Unable to load/)
})
