import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import test from "node:test"
import vm from "node:vm"
import ts from "typescript"

function taskPage(visionResponses, workflowStatus = "SUCCEEDED") {
  const slots = [], effects = [], timers = new Map()
  let cursor = 0, timerId = 0, dirty = false, rendered, visionCalls = 0
  const same = (left, right) => left && right && left.length === right.length && left.every((value, index) => Object.is(value, right[index]))
  const memo = (factory, dependencies) => {
    const index = cursor++
    if (!same(slots[index]?.dependencies, dependencies)) slots[index] = { value: factory(), dependencies }
    return slots[index].value
  }
  const react = {
    useState(initial) {
      const index = cursor++
      slots[index] ??= { value: initial, set(value) {
        const next = typeof value === "function" ? value(slots[index].value) : value
        if (!Object.is(next, slots[index].value)) { slots[index].value = next; dirty = true }
      } }
      return [slots[index].value, slots[index].set]
    },
    useRef: initial => memo(() => ({ current: initial }), []),
    useMemo: memo,
    useCallback: (callback, dependencies) => memo(() => callback, dependencies),
    useEffect(effect, dependencies) {
      const index = cursor++
      if (!same(slots[index]?.dependencies, dependencies)) {
        effects.push(() => { slots[index]?.cleanup?.(); slots[index] = { dependencies, cleanup: effect() } })
      }
    },
  }
  const addTimer = (callback, delay, interval) => {
    const id = ++timerId
    timers.set(id, { callback, delay, interval })
    return id
  }
  const jsx = (type, props) => ({ type: typeof type === "string" ? type : "component", props })
  const dependencies = {
    react,
    "react/jsx-runtime": { jsx, jsxs: jsx },
    "lucide-react": new Proxy({}, { get: () => () => null }),
    "@/services/core/api-workflows": { listApiWorkflows: async () => ({ items: [{ id: "terminal", title: "Available history", workflowType: "MEDIA_TRANSCRIPTION", status: workflowStatus, events: [] }] }) },
    "@/services/ai/vision": { listVisionTasks: async () => {
      visionCalls++
      const response = visionResponses.shift()
      if (response instanceof Error) throw response
      return response
    } },
    "@/services/food/recognition": { listFoodRecognitionTasks: async () => [], isFoodRecognitionVisionTask: () => false },
    "@/mobile/pages/food/hooks/use-food-recognition-tasks": { useFoodRecognitionTasks() {} },
    "./components/nutrition-task-result": {},
    "@/mobile/pages/tasks/tasks.css": {},
  }
  const source = readFileSync(new URL("../src/mobile/pages/tasks/index.tsx", import.meta.url), "utf8")
  const output = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023, jsx: ts.JsxEmit.ReactJSX } }).outputText
  const module = { exports: {} }
  vm.runInNewContext(output, { module, exports: module.exports, require: name => dependencies[name], window: {
    setTimeout: (callback, delay) => addTimer(callback, delay, false),
    clearTimeout: id => timers.delete(id),
    setInterval: (callback, delay) => addTimer(callback, delay, true),
    clearInterval: id => timers.delete(id),
  } })
  const render = () => { cursor = 0; dirty = false; rendered = module.exports.default(); effects.splice(0).forEach(effect => effect()) }
  render()
  return {
    async settle() {
      await new Promise(resolve => setImmediate(resolve))
      while (dirty) render()
    },
    fire(delay) {
      const entry = [...timers.entries()].find(([, timer]) => timer.delay === delay)
      assert.ok(entry, `Expected a ${delay}ms timer`)
      if (!entry[1].interval) timers.delete(entry[0])
      entry[1].callback()
    },
    snapshot: () => JSON.stringify(rendered),
    delays: () => [...timers.values()].map(timer => timer.delay),
    calls: () => visionCalls,
    close: () => slots.forEach(slot => slot?.cleanup?.()),
  }
}

test("terminal partial history retries unavailable image analysis and clears its warning on recovery", async () => {
  const page = taskPage([new Error("temporary source failure"), [{ id: "recovered", scenarioCode: "food_fridge_inventory", status: "SUCCEEDED" }]])
  page.fire(0)
  await page.settle()
  assert.equal(page.calls(), 1)
  assert.match(page.snapshot(), /Available history/)
  assert.match(page.snapshot(), /Image analysis/)
  assert.deepEqual(page.delays(), [10_000])
  page.fire(10_000)
  assert.match(page.snapshot(), /Available history/)
  await page.settle()
  assert.equal(page.calls(), 2)
  assert.match(page.snapshot(), /Fridge inventory scan/)
  assert.doesNotMatch(page.snapshot(), /Some task sources are unavailable/)
  assert.deepEqual(page.delays(), [])
  page.close()
})

test("active tasks keep four second polling and unmount clears the timer", async () => {
  const page = taskPage([new Error("temporary source failure")], "RUNNING")
  page.fire(0)
  await page.settle()
  assert.deepEqual(page.delays(), [4_000])
  page.close()
  assert.deepEqual(page.delays(), [])
})
