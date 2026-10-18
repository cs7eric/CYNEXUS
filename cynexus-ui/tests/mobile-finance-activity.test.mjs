import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import { test } from "node:test"
import vm from "node:vm"
import ts from "typescript"

function loadModel(path, imports = {}, globals = {}) {
  const model = { exports: {} }
  const code = ts.transpileModule(readFileSync(new URL(path, import.meta.url), "utf8"), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 },
  }).outputText
  vm.runInNewContext(code, {
    module: model, exports: model.exports,
    require: (id) => { assert.ok(id in imports, `Unexpected runtime dependency: ${id}`); return imports[id] },
    ...globals,
  })
  return model.exports
}

const activity = loadModel("../src/mobile/pages/finance/lib/activity.ts")
const entry = loadModel("../src/mobile/pages/finance/lib/activity-entry.ts", { "./activity": activity })
const plain = (value) => JSON.parse(JSON.stringify(value))
const october = { year: 2026, month: 9 }
const transaction = (id, overrides = {}) => ({
  id, transactionType: "expense", amount: 1, currency: "CNY", occurredAt: "2026-10-18T12:00:00",
  accountId: 1, accountName: "Cash", toAccountId: null, toAccountName: null,
  categoryId: 1, categoryName: "Food", merchant: "Cafe", note: null, archived: false,
  ...overrides,
})

test("local calendar month bounds and shifts handle leap years and December boundaries", () => {
  assert.deepEqual(plain(activity.activityMonthBounds({ year: 2024, month: 1 })), { start: "2024-02-01", end: "2024-02-29" })
  assert.deepEqual(plain(activity.activityMonthBounds({ year: 2026, month: 1 })), { start: "2026-02-01", end: "2026-02-28" })
  assert.deepEqual(plain(activity.shiftActivityMonth({ year: 2026, month: 0 }, -1)), { year: 2025, month: 11 })
  assert.deepEqual(plain(activity.shiftActivityMonth({ year: 2026, month: 11 }, 1)), { year: 2027, month: 0 })
  assert.equal(activity.activityMonthKey(october), "2026-10")
  assert.throws(() => activity.activityMonthBounds({ year: 2026, month: 12 }))
})

test("income/expense totals retain exact cents, exclude transfers and archived records, and isolate currencies", () => {
  const rows = [
    transaction(1, { transactionType: "income", amount: 100.1 }), transaction(2, { amount: 0.2 }),
    transaction(3, { amount: 0.1 }), transaction(4, { transactionType: "transfer", amount: 50 }),
    transaction(5, { amount: 999, archived: true }), transaction(6, { currency: "USD", amount: 12.34 }),
  ]
  assert.deepEqual(plain(activity.summarizeActivityTransactions(rows)), {
    currency: "CNY", incomeFen: 10010, expenseFen: 30, netFen: 9980,
    incomeCount: 1, expenseCount: 2, transferCount: 1, invalidCount: 0, excludedCurrencyCount: 1, complete: true,
  })
  const usd = activity.summarizeActivityTransactions(rows, { currency: "usd" })
  assert.equal(usd.expenseFen, 1234)
  assert.equal(usd.netFen, -1234)
})

test("malformed, negative, zero, subcent and unsafe transaction amounts never become valid totals", () => {
  for (const amount of [null, undefined, "2", NaN, Infinity, -5, 0, 1.005, 1e-8, 1e20]) {
    const row = transaction(1, { amount })
    assert.equal(activity.activityTransactionFen(row), null)
    const total = activity.summarizeActivityTransactions([row])
    assert.equal(total.expenseFen, null)
    assert.equal(total.incomeFen, null)
    assert.equal(total.netFen, null)
    assert.equal(total.invalidCount, 1)
  }
  assert.equal(activity.activityTransactionFen(transaction(1, { amount: 0.01 })), 1)
  assert.equal(activity.activityTransactionFen(transaction(1, { amount: 1e3 })), 100000)
  assert.equal(activity.summarizeActivityTransactions([transaction(1, { transactionType: "refund" })]).netFen, null)
})

test("unsafe sums and incomplete lists withhold totals instead of presenting a partial sum as complete", () => {
  const rows = [transaction(1, { amount: 50_000_000_000_000 }), transaction(2, { amount: 50_000_000_000_000 })]
  assert.equal(activity.summarizeActivityTransactions(rows).expenseFen, null)
  assert.equal(activity.summarizeActivityTransactions(rows).netFen, null)
  const incomplete = activity.summarizeActivityTransactions([transaction(1)], { complete: false })
  assert.equal(incomplete.expenseFen, null)
  assert.equal(incomplete.incomeFen, null)
  assert.equal(incomplete.netFen, null)
  assert.equal(incomplete.expenseCount, 1)
  assert.equal(incomplete.complete, false)
})

test("filters include both transfer accounts, exclude archived rows and safely search descriptive fields", () => {
  const rows = [transaction(1), transaction(2, { transactionType: "transfer", accountId: 2, toAccountId: 3, toAccountName: "Savings", merchant: null }), transaction(3, { currency: "USD" }), transaction(4, { archived: true }), transaction(5, { categoryId: 2, merchant: "Train", note: "Weekend ticket" })]
  assert.deepEqual(plain(activity.filterActivityTransactions(rows, { accountId: 3 })).map(r => r.id), [2])
  assert.deepEqual(plain(activity.filterActivityTransactions(rows, { query: " SAVINGS " })).map(r => r.id), [2])
  assert.deepEqual(plain(activity.filterActivityTransactions(rows, { currency: " cny ", type: "expense", categoryId: 2, query: "weekend" })).map(r => r.id), [5])
  assert.equal(activity.filterActivityTransactions(rows).length, 4)
})

test("day grouping sorts actual instants, preserves stable id ordering and does not mutate input", () => {
  const rows = [transaction(1, { occurredAt: "2026-10-17T12:00:00" }), transaction(2), transaction(3, { transactionType: "income", amount: 4 })]
  const groups = plain(activity.groupActivityTransactions(rows))
  assert.deepEqual(groups.map(g => g.date), ["2026-10-18", "2026-10-17"])
  assert.deepEqual(groups[0].transactions.map(r => r.id), [3, 2])
  assert.equal(groups[0].summary.netFen, 300)
  assert.deepEqual(rows.map(r => r.id), [1, 2, 3])
  assert.equal(activity.activityDateKey("not a date"), null)
  assert.equal(plain(activity.groupActivityTransactions([transaction(4, { occurredAt: "invalid" })]))[0].date, "unknown")
})

test("a month below the server cap requires just one request", async () => {
  const calls = []
  const result = await activity.loadActivityTransactions(october, async (params) => { calls.push(plain(params)); return [transaction(1)] })
  assert.deepEqual(calls, [{ start: "2026-10-01", end: "2026-10-31", limit: 200 }])
  assert.equal(result.complete, true)
  assert.equal(result.transactions.length, 1)
})

test("capped months are split into dates with bounded concurrency and exact larger totals", async () => {
  let concurrent = 0
  let maxConcurrent = 0
  let calls = 0
  const result = await activity.loadActivityTransactions(october, async ({ start, end }) => {
    calls++
    if (start !== end) return Array.from({ length: 200 }, (_, index) => transaction(index))
    concurrent++; maxConcurrent = Math.max(maxConcurrent, concurrent)
    await new Promise(resolve => setImmediate(resolve))
    concurrent--
    const day = Number(start.slice(-2))
    return Array.from({ length: 10 }, (_, index) => transaction(day * 100 + index, { occurredAt: `${start}T12:00:00` }))
  })
  assert.equal(calls, 32)
  assert.equal(maxConcurrent, 4)
  assert.equal(result.complete, true)
  assert.equal(result.transactions.length, 310)
  assert.equal(activity.summarizeActivityTransactions(result.transactions).expenseFen, 31000)
})

test("a saturated day or failed daily request makes totals explicitly unavailable while keeping available records", async () => {
  const result = await activity.loadActivityTransactions(october, async ({ start, end }) => {
    if (start !== end) return Array.from({ length: 200 }, (_, index) => transaction(index, { occurredAt: "2026-10-03T12:00:00" }))
    if (start === "2026-10-03") throw new Error("Temporary outage")
    if (start === "2026-10-02") return Array.from({ length: 200 }, (_, index) => transaction(1000 + index, { occurredAt: `${start}T12:00:00` }))
    return []
  })
  assert.deepEqual(plain(result.incompleteDays), ["2026-10-02"])
  assert.deepEqual(plain(result.failedDays), ["2026-10-03"])
  assert.equal(result.complete, false)
  assert.equal(result.transactions.length, 400)
  assert.equal(activity.summarizeActivityTransactions(result.transactions, { complete: result.complete }).expenseFen, null)
})

test("superseded capped requests stop issuing daily work", async () => {
  let active = true
  let calls = 0
  await assert.rejects(activity.loadActivityTransactions(october, async () => { calls++; active = false; return Array.from({ length: 200 }, (_, index) => transaction(index)) }, () => active), /superseded/)
  assert.equal(calls, 1)
})

test("manual amount input accepts exact cents and rejects signs, exponents, precision loss and unsafe limits", () => {
  assert.equal(entry.parseActivityAmount(" 12.30 "), 1230)
  assert.equal(entry.parseActivityAmount("000.01"), 1)
  assert.equal(entry.parseActivityAmount("90071992547409.90"), 9007199254740990)
  for (const value of ["", "0", "-1", "+1", "1e3", "1.234", "1.", ".50", "NaN", "90071992547409.91", "90071992547409.92", "10000000000000000000000000000000"]) assert.equal(entry.parseActivityAmount(value), null)
})

test("accepted manual amounts survive the number-based API's JSON round trip without losing cents", () => {
  for (const input of ["0.01", "0.29", "96.50", "999999999.99", "50000000000000.01", "90071992547409.90"]) {
    const fen = entry.parseActivityAmount(input)
    assert.notEqual(fen, null, input)
    const payload = JSON.parse(JSON.stringify({ amount: fen / 100 }))
    assert.equal(activity.activityTransactionFen(payload), fen, input)
  }
  assert.equal(entry.parseActivityAmount("90071992547409.91"), null)
})

test("entry local date payload preserves wall time with the date's actual timezone offset and rejects rolled dates", () => {
  const date = new Date(2026, 9, 18, 14, 23)
  assert.equal(entry.localActivityDateTime(date), "2026-10-18T14:23")
  const payload = entry.activityDateTimePayload("2026-10-18T14:23")
  assert.match(payload, /^2026-10-18T14:23:00[+-]\d{2}:\d{2}$/)
  assert.equal(new Date(payload).getTime(), date.getTime())
  for (const value of ["2026-02-29T12:00", "2026-04-31T12:00", "2026-13-01T12:00", "2026-01-01T24:00", "2026-01-01T12:60", "2026-01-01", "0000-01-01T12:00", "2026-01-01T12:00Z"]) assert.equal(entry.activityDateTimePayload(value), null)
  assert.equal(entry.activityDateTimePayload("2024-02-29T12:00")?.startsWith("2024-02-29T12:00:00"), true)
})

test("backdated default entries stay in the selected month and clamp the day", () => {
  assert.equal(entry.defaultActivityEntryDate({ year: 2026, month: 1 }, new Date(2026, 9, 31, 18, 9)), "2026-02-28T18:09")
  assert.equal(entry.defaultActivityEntryDate({ year: 2024, month: 1 }, new Date(2026, 9, 31, 18, 9)), "2024-02-29T18:09")
})

function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}

// Exercise the hook's observable async behavior with controlled React effects and requests.
function activityHookHarness(serviceOverrides = {}) {
  const slots = []
  let cursor = 0
  let effects = []
  let timerId = 0
  const timers = new Map()
  const requests = []
  const sameDependencies = (left, right) => left && left.length === right.length && left.every((value, index) => Object.is(value, right[index]))
  const react = {
    useState(initial) {
      const index = cursor++
      slots[index] ??= { value: typeof initial === "function" ? initial() : initial }
      return [slots[index].value, update => { slots[index].value = typeof update === "function" ? update(slots[index].value) : update }]
    },
    useCallback(callback, dependencies) {
      const index = cursor++
      if (!sameDependencies(slots[index]?.dependencies, dependencies)) slots[index] = { callback, dependencies }
      return slots[index].callback
    },
    useEffect(effect, dependencies) {
      const index = cursor++
      if (!sameDependencies(slots[index]?.dependencies, dependencies)) effects.push(() => {
        slots[index]?.cleanup?.()
        slots[index] = { dependencies, cleanup: effect() }
      })
    },
  }
  const services = {
    fetchFinanceTransactions: () => { const request = deferred(); requests.push(request); return request.promise },
    fetchFinanceAccounts: async () => [{ id: 1, currency: "CNY", archived: false }],
    fetchFinanceCategories: async () => [{ id: 1, direction: "expense", archived: false }],
    ...serviceOverrides,
  }
  const { useFinanceActivity } = loadModel("../src/mobile/pages/finance/hooks/use-finance-activity.ts", {
    react, "@/services/finance/ledger": services, "../lib/activity": activity,
  }, { window: { setTimeout: callback => { const id = ++timerId; timers.set(id, callback); return id }, clearTimeout: id => timers.delete(id) } })
  return {
    requests,
    render(props) { cursor = 0; effects = []; const result = useFinanceActivity(props); for (const effect of effects) effect(); return result },
    runTimers() { const callbacks = [...timers.values()]; timers.clear(); callbacks.forEach(callback => callback()) },
    async settle() { await new Promise(resolve => setImmediate(resolve)) },
  }
}

test("hook scope switches immediately hide old data and discard late responses from another workspace or month", async () => {
  const harness = activityHookHarness()
  const first = { month: october, workspaceId: "personal" }
  harness.render(first); harness.runTimers()
  harness.requests[0].resolve([transaction(1)])
  await harness.settle()
  let state = harness.render(first)
  assert.equal(state.snapshot.transactions[0].id, 1)
  state.refresh(); harness.render(first); harness.runTimers()
  const second = { month: { year: 2026, month: 8 }, workspaceId: "work" }
  state = harness.render(second)
  assert.equal(state.snapshot, undefined)
  assert.equal(state.status, "loading")
  harness.runTimers()
  harness.requests[2].resolve([transaction(3)])
  await harness.settle()
  assert.equal(harness.render(second).snapshot.transactions[0].id, 3)
  harness.requests[1].resolve([transaction(2)])
  await harness.settle()
  assert.equal(harness.render(second).snapshot.transactions[0].id, 3)
  const third = { ...second, month: october }
  assert.equal(harness.render(third).snapshot, undefined)
})

test("hook refresh errors preserve the last same-scope data and a retry can replace it", async () => {
  const harness = activityHookHarness()
  const props = { month: october, workspaceId: "personal" }
  harness.render(props); harness.runTimers()
  harness.requests[0].resolve([transaction(1)])
  await harness.settle()
  let state = harness.render(props)
  state.refresh(); harness.render(props); harness.runTimers()
  assert.equal(harness.render(props).snapshot.transactions[0].id, 1)
  harness.requests[1].reject(new Error("Offline"))
  await harness.settle()
  state = harness.render(props)
  assert.equal(state.status, "error")
  assert.equal(state.stale, true)
  assert.equal(state.snapshot.transactions[0].id, 1)
  state.refresh(); harness.render(props); harness.runTimers()
  harness.requests[2].resolve([transaction(2)])
  await harness.settle()
  state = harness.render(props)
  assert.equal(state.status, "ready")
  assert.equal(state.stale, false)
  assert.equal(state.snapshot.transactions[0].id, 2)
})

test("lookup failures preserve a readable complete ledger but explicitly block unavailable entry metadata", async () => {
  const harness = activityHookHarness({ fetchFinanceCategories: async () => { throw new Error("Offline") } })
  const props = { month: october, workspaceId: "personal" }
  harness.render(props); harness.runTimers()
  harness.requests[0].resolve([transaction(1)])
  await harness.settle()
  const state = harness.render(props)
  assert.equal(state.status, "ready")
  assert.equal(state.partial, true)
  assert.equal(state.snapshot.complete, true)
  assert.equal(state.snapshot.accountsAvailable, true)
  assert.equal(state.snapshot.categoriesAvailable, false)
  assert.equal(state.snapshot.categories.length, 0)
  assert.match(state.error, /Categories could not be loaded/)
})
