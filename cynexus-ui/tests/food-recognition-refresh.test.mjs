import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import test from "node:test"
import vm from "node:vm"
import ts from "typescript"

function observer(responses) {
  const effects = [], timers = new Map(), messages = [], listeners = new Map()
  let timerId = 0
  let changes = 0
  const module = { exports: {} }
  const source = readFileSync(new URL("../src/mobile/pages/food/hooks/use-food-recognition-tasks.ts", import.meta.url), "utf8")
  const output = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 } }).outputText
  const dependencies = {
    react: { useCallback: callback => callback, useRef: current => ({ current }), useEffect: effect => effects.push(effect) },
    "@/services/food/api": { announceFoodChange: () => changes++ },
    "@/services/food/recognition": { listFoodRecognitionTasks: async () => {
      const response = responses.shift()
      if (response instanceof Error) throw response
      return await response
    } },
  }
  vm.runInNewContext(output, {
    module, exports: module.exports, require: name => dependencies[name],
    window: {
      clearTimeout: id => timers.delete(id),
      setTimeout: (callback, delay) => { const id = ++timerId; timers.set(id, { callback, delay }); return id },
    },
    document: {
      addEventListener: (type, callback) => listeners.set(type, callback),
      removeEventListener: type => listeners.delete(type),
      visibilityState: "visible",
    },
  })
  const hook = module.exports.useFoodRecognitionTasks(message => messages.push(message))
  const cleanups = effects.map(effect => effect())
  return {
    messages, changes: () => changes,
    add: hook.add,
    resume: () => listeners.get("visibilitychange")?.(),
    delays: () => [...timers.values()].map(timer => timer.delay),
    nextPoll: () => { const [id, timer] = timers.entries().next().value; timers.delete(id); timer.callback() },
    close: () => cleanups.forEach(cleanup => cleanup?.()),
  }
}

const settled = () => new Promise(resolve => setImmediate(resolve))

test("first observed completed task refreshes My foods and All foods without an old toast", async () => {
  const h = observer([[{ id: "42", status: "succeeded", foodName: "Yogurt" }]])
  await settled()
  assert.equal(h.changes(), 1)
  assert.deepEqual(h.messages, [])
  h.close()
})

test("a completion missed during network failure still refreshes the food lists", async () => {
  const h = observer([new Error("offline"), [{ id: "42", status: "succeeded" }]])
  await settled()
  h.nextPoll()
  await settled()
  assert.equal(h.changes(), 1)
  h.close()
})

test("running to succeeded refreshes once and reports the saved food", async () => {
  const h = observer([
    [{ id: "42", status: "running" }],
    [{ id: "42", status: "succeeded", foodName: "Yogurt" }],
    [{ id: "42", status: "succeeded", foodName: "Yogurt" }],
  ])
  await settled()
  h.nextPoll()
  await settled()
  h.nextPoll()
  await settled()
  assert.equal(h.changes(), 1)
  assert.deepEqual(h.messages, ["Yogurt was added to My foods."])
  h.close()
})

test("a new submission wakes idle polling and keeps polling until saved", async () => {
  const h = observer([[], [{ id: "42", status: "queued" }], [{ id: "42", status: "succeeded" }]])
  await settled()
  assert.deepEqual(h.delays(), [8_000])
  h.add({ id: "42", status: "queued" })
  await settled()
  assert.deepEqual(h.delays(), [2_500])
  h.nextPoll()
  await settled()
  assert.equal(h.changes(), 1)
  assert.deepEqual(h.delays(), [8_000])
  h.close()
  assert.deepEqual(h.delays(), [])
})

test("submitting during an in-flight history request schedules an immediate follow-up", async () => {
  let finishHistory
  const pending = new Promise(resolve => { finishHistory = resolve })
  const h = observer([pending, [{ id: "42", status: "succeeded" }]])
  h.add({ id: "42", status: "queued" })
  finishHistory([])
  await settled()
  assert.deepEqual(h.delays(), [0])
  h.nextPoll()
  await settled()
  assert.equal(h.changes(), 1)
  h.close()
})

test("failed recognition leaves the library unchanged and reports the error once", async () => {
  const failed = { id: "42", status: "failed", errorMessage: "Unreadable nutrition label" }
  const h = observer([[{ id: "42", status: "running" }], [failed], [failed]])
  await settled()
  h.nextPoll()
  await settled()
  h.nextPoll()
  await settled()
  assert.equal(h.changes(), 0)
  assert.deepEqual(h.messages, ["Unreadable nutrition label"])
  h.close()
})

test("returning to the page immediately recovers completed food data", async () => {
  const h = observer([[{ id: "42", status: "running" }], [{ id: "42", status: "succeeded" }]])
  await settled()
  h.resume()
  await settled()
  assert.equal(h.changes(), 1)
  h.close()
})

test("an unmounted observer ignores late success responses", async () => {
  let finishHistory
  const h = observer([new Promise(resolve => { finishHistory = resolve })])
  h.close()
  finishHistory([{ id: "42", status: "succeeded" }])
  await settled()
  assert.equal(h.changes(), 0)
  assert.deepEqual(h.delays(), [])
})
