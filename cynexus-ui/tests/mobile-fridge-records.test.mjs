import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import test from "node:test"
import vm from "node:vm"
import ts from "typescript"

const module = { exports: {} }
const source = readFileSync(new URL("../src/services/fridge/model.ts", import.meta.url), "utf8")
vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 } }).outputText, { module, exports: module.exports, Date })
const fridge = module.exports
const today = "2026-10-19"
const now = `${today}T08:00:00.000Z`
const sample = () => fridge.createSampleFridgeRecords(fridge.parseDate(today))

test("expiry state uses calendar dates and marks the next three days for use soon", () => {
  assert.equal(fridge.getFridgeExpiryState("2026-10-18", today), "expired")
  assert.equal(fridge.getFridgeExpiryState(today, today), "use-soon")
  assert.equal(fridge.getFridgeExpiryState("2026-10-22", today), "use-soon")
  assert.equal(fridge.getFridgeExpiryState("2026-10-23", today), "fresh")
  assert.equal(fridge.isDateKey("2026-02-30"), false)
  assert.throws(() => fridge.getFridgeExpiryState("2026-02-30", today))
})

test("sample fridge rows are editable and do not add fixture history to personal records", () => {
  const fixture = sample()
  assert.equal(fixture.isSample, true)
  assert.equal(fixture.items.length, 19)
  assert.equal(fixture.operations.length, 0)
  assert.equal(fixture.items.find((item) => item.name === "Apple").catalogId, "food-071")
  assert.equal(fixture.items.find((item) => item.name === "Salmon").catalogId, "food-061")
  assert.equal(fixture.items.find((item) => item.name === "Broccoli").unit, "head")

  const changed = fridge.changeFridgeQuantity(fixture, "sample-chicken-breast", -1, now, "op-1")
  assert.equal(changed.isSample, false)
  assert.equal(changed.items.find((item) => item.id === "sample-chicken-breast").quantity, 449)
  assert.equal(changed.operations.length, 1)
  assert.equal(changed.operations[0].kind, "remove")
  assert.equal(changed.operations[0].quantityDelta, -1)
})

test("fridge starts empty instead of showing demo stock and caches the authoritative inventory", async () => {
  const browser = createStorageHarness()
  const response = deferred()
  const chicken = { id: "1234567890123456789", name: "Chicken breast", quantity: 450, unit: "g", bestBefore: "2026-10-29", createdAt: now, updatedAt: now }
  const thigh = { ...chicken, id: "1234567890123456790", name: "Chicken thigh" }
  const store = loadFridgeStore(browser.createWindow(), { fetchFridgeInventory: () => response.promise })

  const initial = store.getFridgeSnapshot("workspace")
  assert.equal(initial.data.isSample, false)
  assert.equal(initial.data.items.length, 0)

  const sync = store.syncFridgeRecords("workspace")
  response.resolve(inventory(chicken, thigh))
  await sync

  assert.deepEqual(store.getFridgeSnapshot("workspace").data.items.map((item) => item.name), ["Chicken breast", "Chicken thigh"])
  assert.deepEqual(JSON.parse(browser.values.get("cynexus.fridge.records.v1.workspace")).items.map((item) => item.name), ["Chicken breast", "Chicken thigh"])

  const reloaded = loadFridgeStore(browser.createWindow())
  assert.equal(JSON.stringify(reloaded.getFridgeSnapshot("workspace").data.items.map((item) => item.name)), JSON.stringify(["Chicken breast", "Chicken thigh"]))
})

test("item creation and custom ingredients work without catalog images and retain real operations", () => {
  const item = { name: "Free-range eggs", quantity: 12, unit: "pcs", bestBefore: "2026-10-29" }
  const created = fridge.saveFridgeItem(sample(), item, now, "egg-id", "create-op")
  assert.equal(created.isSample, false)
  assert.equal(created.items.find((candidate) => candidate.id === "egg-id").catalogId, undefined)
  assert.equal(created.operations[0].kind, "create")
  assert.equal(created.operations[0].quantityDelta, 12)

  const updated = fridge.saveFridgeItem(created, { ...item, id: "egg-id", quantity: 8, bestBefore: "2026-10-28" }, `${today}T09:00:00.000Z`, "unused-new-id", "update-op")
  assert.equal(updated.items.length, created.items.length)
  assert.equal(updated.items.find((candidate) => candidate.id === "egg-id").quantity, 8)
  assert.equal(updated.operations[0].kind, "update")
  assert.equal(updated.operations[0].quantityDelta, -4)
})

test("item creation keeps unit conversion and brand in the local record", () => {
  const created = fridge.saveFridgeItem(sample(), {
    name: "Whole milk",
    brand: "Farm",
    quantity: 500,
    unit: "ml",
    gramsPerUnit: 1.03,
    bestBefore: "2026-10-29",
  }, now, "milk-id", "milk-op")
  const item = created.items.find((candidate) => candidate.id === "milk-id")
  assert.equal(item.brand, "Farm")
  assert.equal(item.gramsPerUnit, 1.03)
  assert.equal(fridge.decodeFridgeRecords(JSON.stringify(created)).items.find((candidate) => candidate.id === "milk-id").gramsPerUnit, 1.03)
  assert.throws(() => fridge.saveFridgeItem(sample(), {
    name: "Milk", quantity: 500, unit: "ml", gramsPerUnit: 0, bestBefore: "2026-10-29",
  }, now, "invalid-milk", "invalid-milk-op"))
})

test("quantity changes clamp at zero and invalid dates or values are rejected", () => {
  const data = fridge.changeFridgeQuantity(sample(), "sample-chicken-breast", -1000, now, "op-1")
  assert.equal(data.items.find((item) => item.id === "sample-chicken-breast").quantity, 0)
  assert.equal(data.operations[0].quantityDelta, -450)
  assert.throws(() => fridge.saveFridgeItem(sample(), { name: "Eggs", quantity: -1, unit: "pcs", bestBefore: today }, now, "i", "o"))
  assert.throws(() => fridge.saveFridgeItem(sample(), { name: "Eggs", quantity: 2, unit: "pcs", bestBefore: "2026-02-30" }, now, "i", "o"))
  assert.throws(() => fridge.saveFridgeItem(sample(), { name: "Eggs", quantity: Infinity, unit: "pcs", bestBefore: today }, now, "i", "o"))
})

test("stored records reject unsupported schemas, duplicate ids and malformed operations", () => {
  const data = sample()
  assert.deepEqual(fridge.decodeFridgeRecords(JSON.stringify(data)), data)
  assert.throws(() => fridge.decodeFridgeRecords('{"version":2,"isSample":false,"items":[],"operations":[]}'))
  assert.throws(() => fridge.decodeFridgeRecords(JSON.stringify({ ...data, items: [data.items[0], data.items[0]] })))
  assert.throws(() => fridge.decodeFridgeRecords(JSON.stringify({ ...data, operations: [{ id: "bad" }] })))
})

test("local record updates report storage failures while retaining the session snapshot", () => {
  const storageModule = { exports: {} }
  const storageSource = readFileSync(new URL("../src/services/fridge/local-records.ts", import.meta.url), "utf8")
  const unavailableStorage = {
    addEventListener() {},
    localStorage: {
      getItem() { return null },
      setItem() { throw new Error("Quota exceeded") },
    },
  }
  vm.runInNewContext(ts.transpileModule(storageSource, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 } }).outputText, {
    module: storageModule,
    exports: storageModule.exports,
    require: () => fridge,
    window: unavailableStorage,
    Date,
    JSON,
    Map,
    Set,
  })
  const store = storageModule.exports
  let notified = false
  store.subscribeFridgeRecords("test", () => { notified = true })

  const snapshot = store.updateFridgeRecords("test", (records) => ({ ...records, isSample: false }))

  assert.match(snapshot.storageError, /Could not save/)
  assert.equal(store.getFridgeSnapshot("test"), snapshot)
  assert.equal(notified, true)
})

function createStorageHarness() {
  const values = new Map()
  const contexts = []
  function createWindow() {
    const handlers = new Set()
    const localStorage = {
      get length() { return values.size },
      key(index) { return [...values.keys()][index] ?? null },
      getItem(key) { return values.get(key) ?? null },
      setItem(key, value) {
        const oldValue = values.get(key) ?? null
        const newValue = String(value)
        values.set(key, newValue)
        dispatch(key, oldValue, newValue, thisWindow)
      },
      removeItem(key) {
        const oldValue = values.get(key) ?? null
        if (oldValue === null) return
        values.delete(key)
        dispatch(key, oldValue, null, thisWindow)
      },
    }
    const thisWindow = {
      localStorage,
      addEventListener(type, listener) { if (type === "storage") handlers.add(listener) },
    }
    contexts.push({ window: thisWindow, handlers })
    return thisWindow
  }
  function dispatch(key, oldValue, newValue, source) {
    for (const context of contexts) {
      if (context.window === source) continue
      for (const handler of context.handlers) handler({ key, oldValue, newValue, storageArea: context.window.localStorage })
    }
  }
  return { createWindow, values }
}

function loadFridgeStore(window, api = {}) {
  const storageModule = { exports: {} }
  const storageSource = readFileSync(new URL("../src/services/fridge/local-records.ts", import.meta.url), "utf8")
  const apiModule = {
    addFridgeItem: async () => { throw new Error("Fridge service is unavailable.") },
    adjustFridgeItem: async () => { throw new Error("Fridge service is unavailable.") },
    confirmFridgeItemMapping: async () => { throw new Error("Fridge service is unavailable.") },
    fetchFridgeInventory: async () => ({ isSample: false, items: [], operations: [] }),
    normalizeFridgeInventory: (inventory) => inventory,
    ...api,
  }
  vm.runInNewContext(ts.transpileModule(storageSource, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 } }).outputText, {
    module: storageModule,
    exports: storageModule.exports,
    require: (name) => name === "./model" ? fridge : apiModule,
    window,
    Date,
    JSON,
    Map,
    Set,
    DOMException,
  })
  return storageModule.exports
}

function deferred() {
  let resolve
  let reject
  const promise = new Promise((accept, fail) => { resolve = accept; reject = fail })
  return { promise, resolve, reject }
}

async function waitUntil(predicate) {
  for (let attempt = 0; attempt < 20; attempt++) {
    if (predicate()) return
    await new Promise((resolve) => setImmediate(resolve))
  }
  assert.fail("Condition did not become true")
}

const inventory = (...items) => ({ version: 1, isSample: false, items, operations: [] })
const fridgeInput = (idempotencyKey) => ({ idempotencyKey, name: `Food ${idempotencyKey}`, quantity: 1, unit: "pcs", bestBefore: "2026-10-29" })

test("remote fridge writes are serialized and the final inventory contains every confirmed item", async () => {
  const browser = createStorageHarness()
  const first = deferred()
  const second = deferred()
  const calls = []
  const store = loadFridgeStore(browser.createWindow(), {
    addFridgeItem(input) {
      calls.push(input.idempotencyKey)
      return calls.length === 1 ? first.promise : second.promise
    },
    fetchFridgeInventory: async () => inventory({ id: "item-a" }, { id: "item-b" }),
  })

  const firstWrite = store.createFridgeItemRemote("workspace", fridgeInput("a"))
  const secondWrite = store.createFridgeItemRemote("workspace", fridgeInput("b"))
  await waitUntil(() => calls.length === 1)
  assert.deepEqual(calls, ["a"])

  first.resolve(inventory({ id: "item-a" }))
  await waitUntil(() => calls.length === 2)
  second.resolve(inventory({ id: "item-a" }, { id: "item-b" }))
  await Promise.all([firstWrite, secondWrite])

  assert.deepEqual(store.getFridgeSnapshot("workspace").data.items.map((item) => item.id), ["item-a", "item-b"])
})

test("a later failed write does not make an earlier confirmed write appear failed", async () => {
  const browser = createStorageHarness()
  const first = deferred()
  const second = deferred()
  let calls = 0
  const store = loadFridgeStore(browser.createWindow(), {
    addFridgeItem() {
      calls++
      return calls === 1 ? first.promise : second.promise
    },
  })

  const firstWrite = store.createFridgeItemRemote("workspace", fridgeInput("confirmed"))
  const secondWrite = store.createFridgeItemRemote("workspace", fridgeInput("pending"))
  await waitUntil(() => calls === 1)
  first.resolve(inventory({ id: "confirmed-item" }))
  await firstWrite
  await waitUntil(() => calls === 2)
  second.reject(new Error("offline"))
  await assert.rejects(secondWrite, /offline/)
})

test("a failed fridge write stays in the durable outbox and sync retries it", async () => {
  const browser = createStorageHarness()
  let attempts = 0
  const store = loadFridgeStore(browser.createWindow(), {
    addFridgeItem: async () => {
      attempts++
      if (attempts === 1) throw new Error("offline")
      return inventory({ id: "server-item" })
    },
    fetchFridgeInventory: async () => inventory({ id: "server-item" }),
  })

  await assert.rejects(store.createFridgeItemRemote("workspace", fridgeInput("retry-me")), /offline/)
  assert.equal([...browser.values.keys()].some((key) => key.startsWith("cynexus.fridge.outbox.v1.item/")), true)

  const result = await store.syncFridgeRecords("workspace")
  assert.equal(result.synced, true)
  assert.equal(attempts, 2)
  assert.deepEqual(result.snapshot.data.items.map((item) => item.id), ["server-item"])
  assert.equal([...browser.values.keys()].some((key) => key.startsWith("cynexus.fridge.outbox.v1.item/")), false)
})

test("concurrent browser tabs retain distinct failed outbox operations", async () => {
  const browser = createStorageHarness()
  const first = loadFridgeStore(browser.createWindow(), { addFridgeItem: async () => { throw new Error("offline") } })
  const second = loadFridgeStore(browser.createWindow(), { addFridgeItem: async () => { throw new Error("offline") } })

  const writes = await Promise.allSettled([
    first.createFridgeItemRemote("workspace", fridgeInput("tab-a")),
    second.createFridgeItemRemote("workspace", fridgeInput("tab-b")),
  ])
  assert.equal(writes.every((result) => result.status === "rejected"), true)
  assert.equal([...browser.values.keys()].filter((key) => key.startsWith("cynexus.fridge.outbox.v1.item/")).length, 2)

  let attempts = 0
  const reloaded = loadFridgeStore(browser.createWindow(), {
    addFridgeItem: async (input) => {
      attempts++
      return inventory({ id: `server-${input.idempotencyKey}` })
    },
    fetchFridgeInventory: async () => inventory({ id: "server-tab-a" }, { id: "server-tab-b" }),
  })
  const result = await reloaded.syncFridgeRecords("workspace")
  assert.equal(result.synced, true)
  assert.equal(attempts, 2)
  assert.deepEqual(result.snapshot.data.items.map((item) => item.id), ["server-tab-a", "server-tab-b"])
})

test("a successful mapping in one tab refreshes the authoritative inventory in another", async () => {
  const browser = createStorageHarness()
  const mapped = inventory({ id: "milk", mappingStatus: "user_confirmed", nutrition: { caloriesKcal: 61 } })
  const first = loadFridgeStore(browser.createWindow(), { confirmFridgeItemMapping: async () => mapped })
  const second = loadFridgeStore(browser.createWindow(), { fetchFridgeInventory: async () => mapped })
  let notified = false
  second.subscribeFridgeRecords("workspace", () => { notified = true })
  second.getFridgeSnapshot("workspace")

  await first.confirmFridgeItemMappingRemote("workspace", "1234567890123456789", { candidateId: "food-1", kind: "catalog" })
  await waitUntil(() => second.getFridgeSnapshot("workspace").data.items[0]?.mappingStatus === "user_confirmed")

  assert.equal(notified, true)
  assert.equal(second.getFridgeSnapshot("workspace").data.items[0].nutrition.caloriesKcal, 61)
})

test("mapping an optimistic local fridge item resolves its server resource ID", async () => {
  const browser = createStorageHarness()
  const mapped = inventory({ id: "9876543210987654321", name: "Chicken thigh", brand: "Farm", mappingStatus: "user_confirmed" })
  let confirmedId = null
  const store = loadFridgeStore(browser.createWindow(), {
    confirmFridgeItemMapping: async (itemId) => {
      confirmedId = itemId
      return mapped
    },
    fetchFridgeInventory: async () => mapped,
  })
  store.updateFridgeRecords("workspace", (records) => ({
    ...records,
    isSample: false,
    items: [{ id: "local-uuid", name: "Chicken thigh", brand: "Farm", quantity: 2, unit: "pcs", bestBefore: "2026-10-29" }],
  }))

  await store.confirmFridgeItemMappingRemote("workspace", "local-uuid", { candidateId: "1234567890123456789", kind: "catalog" })

  assert.equal(confirmedId, "9876543210987654321")
  assert.equal(store.getFridgeSnapshot("workspace").data.items[0].mappingStatus, "user_confirmed")
})

test("mapping a fixture item without a server counterpart does not leave a blocked outbox entry", async () => {
  const browser = createStorageHarness()
  const store = loadFridgeStore(browser.createWindow(), {
    fetchFridgeInventory: async () => inventory(),
  })
  store.updateFridgeRecords("workspace", () => sample())

  await assert.rejects(
    store.confirmFridgeItemMappingRemote("workspace", "sample-chicken-breast", { candidateId: "1234567890123456789", kind: "catalog" }),
    /has not synced to the fridge service/i,
  )
  assert.equal([...browser.values.keys()].some((key) => key.startsWith("cynexus.fridge.outbox.v1.item/")), false)
})

test("a slow sync cannot overwrite a newer optimistic fridge edit", async () => {
  const browser = createStorageHarness()
  const staleFetch = deferred()
  const store = loadFridgeStore(browser.createWindow(), {
    fetchFridgeInventory: () => staleFetch.promise,
    addFridgeItem: async () => { throw new Error("offline") },
  })

  const sync = store.syncFridgeRecords("workspace")
  const local = store.updateFridgeRecords("workspace", (records) => ({
    ...records,
    isSample: false,
    items: [...records.items, { id: "optimistic-item", name: "Food retry-me", quantity: 1, unit: "pcs", bestBefore: "2026-10-29" }],
  }))
  const write = store.createFridgeItemRemote("workspace", fridgeInput("retry-me"))
  staleFetch.resolve(inventory())
  await sync
  await assert.rejects(write, /offline/)

  assert.equal(store.getFridgeSnapshot("workspace").data.items.some((item) => item.id === "optimistic-item"), true)
  assert.equal(local.storageError, null)
})

test("outbox persistence failure is included when a remote write also fails", async () => {
  const window = {
    addEventListener() {},
    localStorage: {
      length: 0,
      key() { return null },
      getItem() { return null },
      setItem(key) {
        if (key.startsWith("cynexus.fridge.outbox.")) throw new Error("Quota exceeded")
      },
      removeItem() {},
    },
  }
  const store = loadFridgeStore(window, { addFridgeItem: async () => { throw new Error("offline") } })

  await assert.rejects(store.createFridgeItemRemote("workspace", fridgeInput("no-storage")), /could not be saved for retry after this session/i)
})
