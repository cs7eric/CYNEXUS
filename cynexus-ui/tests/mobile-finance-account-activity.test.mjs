import assert from "node:assert/strict"
import { execFileSync } from "node:child_process"
import { readFileSync } from "node:fs"
import { test } from "node:test"
import vm from "node:vm"
import ts from "typescript"

function load(path, imports = {}, globals = {}) {
  const model = { exports: {} }
  const code = ts.transpileModule(readFileSync(new URL(path, import.meta.url), "utf8"), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 } }).outputText
  vm.runInNewContext(code, { module: model, exports: model.exports, require: id => { assert.ok(id in imports, id); return imports[id] }, ...globals })
  return model.exports
}
const activity = load("../src/mobile/pages/finance/lib/activity.ts")
const model = load("../src/mobile/pages/finance/lib/account-activity.ts", { "./activity": activity })
const plain = value => JSON.parse(JSON.stringify(value))
const now = new Date(2026, 10, 10, 12)
const range = model.accountActivityRange(now)
const row = (id, overrides = {}) => ({ id, accountId: 1, toAccountId: null, transactionType: "expense", occurredAt: new Date(2026, 10, 9, 12).toISOString(), archived: false, ...overrides })
const accounts = [1, 2, 3, 4, 5].map(id => ({ id, name: `Account ${id}` }))

test("activity window includes exactly30 local calendar days across year and DST boundaries", () => {
  const year = model.accountActivityRange(new Date(2027, 0, 1, 12))
  assert.equal(year.start, "2026-12-03")
  assert.equal(year.end, "2027-01-01")
  const script = `import assert from "node:assert/strict"; import {readFileSync} from "node:fs"; import vm from "node:vm"; import ts from "typescript";
    function read(path, imports = {}) { const module = {exports:{}}; const code = ts.transpileModule(readFileSync(path,"utf8"),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2023}}).outputText;
      vm.runInNewContext(code,{module,exports:module.exports,require:id=>imports[id]}); return module.exports; }
    const activity = read("src/mobile/pages/finance/lib/activity.ts");
    const model = read("src/mobile/pages/finance/lib/account-activity.ts",{"./activity":activity});
    const dst = model.accountActivityRange(new Date(2026,2,10,12));
    assert.equal(dst.start,"2026-02-09"); assert.equal(new Date(dst.from).getHours(),0); assert.equal((dst.through-dst.from)/3600000,707);`
  execFileSync(process.execPath, ["--input-type=module", "-e", script], { cwd: new URL("..", import.meta.url), env: { ...process.env, TZ: "America/New_York" } })
  assert.throws(() => model.accountActivityRange(new Date(NaN)), /valid/)
})

test("accounts rank by record count, last transaction, then original order without mutating inputs", () => {
  const rows = [row(1), row(2, { accountId: 2 }), row(3, { accountId: 2 }), row(4, { accountId: 3, occurredAt: new Date(2026, 10, 10, 11).toISOString() })]
  const before = JSON.stringify({ accounts, rows })
  const ranked = model.rankAccountsByRecentActivity(accounts, rows, range)
  assert.deepEqual(plain(ranked.map(item => item.account.id)), [2, 3, 1, 4, 5])
  assert.deepEqual(plain(ranked.map(item => item.count)), [2, 1, 1, 0, 0])
  assert.equal(JSON.stringify({ accounts, rows }), before)
})

test("transfers count on both sides once and duplicate, archived, old, future and invalid records do not count", () => {
  const transfer = row(1, { transactionType: "transfer", toAccountId: 2 })
  const rows = [transfer, transfer, row(2, { transactionType: "transfer", accountId: 2, toAccountId: 2 }),
    row(3, { archived: true }), row(4, { occurredAt: new Date(range.from - 1).toISOString() }),
    row(5, { occurredAt: new Date(now.getTime() + 1).toISOString() }), row(6, { occurredAt: "invalid" }),
    row(7, { occurredAt: new Date(range.from).toISOString(), transactionType: "income", toAccountId: 3 }),
    row(8, { accountId: 99 })]
  const counts = new Map(model.rankAccountsByRecentActivity(accounts, rows, range).map(item => [item.account.id, item.count]))
  assert.equal(counts.get(1), 2)
  assert.equal(counts.get(2), 2)
  assert.equal(counts.get(3), 0)
})

test("uncapped window uses one bounded read and excludes records outside the requested instant range", async () => {
  const requests = []
  const result = await model.loadAccountActivityTransactions(range, async query => {
    requests.push(query)
    return [row(1), row(2, { occurredAt: new Date(now.getTime() + 1).toISOString() })]
  })
  assert.deepEqual(plain(requests), [{ start: "2026-10-12", end: "2026-11-10", limit: 200 }])
  assert.equal(result.complete, true)
  assert.deepEqual(plain(result.transactions.map(item => item.id)), [1])
})

test("capped windows split into30 exact dates with bounded concurrency and preserve all accounts' counts", async () => {
  let pending = 0
  let peak = 0
  const requests = []
  const result = await model.loadAccountActivityTransactions(range, async query => {
    requests.push(query)
    if (query.start !== query.end) return Array.from({ length: 200 }, (_, index) => row(index))
    pending++; peak = Math.max(peak, pending)
    await new Promise(resolve => setImmediate(resolve))
    pending--
    const day = Number(query.start.replaceAll("-", ""))
    return [row(day, { accountId: day % 2 + 1, occurredAt: `${query.start}T00:00:00` })]
  })
  assert.equal(requests.length, 31)
  assert.equal(requests[1].start, range.start)
  assert.equal(requests.at(-1).end, range.end)
  assert.ok(peak > 1 && peak <= 4)
  assert.equal(result.complete, true)
  assert.equal(result.transactions.length, 30)
  assert.equal(model.rankAccountsByRecentActivity(accounts, result.transactions, range).reduce((sum, item) => sum + item.count, 0), 30)
})

test("capped or failed days remain explicitly incomplete, retaining available failed-day records", async () => {
  const result = await model.loadAccountActivityTransactions(range, async query => {
    if (query.start !== query.end) return Array.from({ length: 200 }, (_, index) => row(index))
    if (query.start === "2026-11-09") throw new Error("offline")
    if (query.start === "2026-11-08") return Array.from({ length: 200 }, (_, index) => row(1000 + index, { occurredAt: "2026-11-08T12:00:00" }))
    return []
  })
  assert.equal(result.complete, false)
  assert.deepEqual(plain(result.failedDays), ["2026-11-09"])
  assert.deepEqual(plain(result.incompleteDays), ["2026-11-08"])
  assert.equal(result.transactions.length, 400)
})

test("superseded loads stop issuing daily requests and reject instead of publishing partial ranking", async () => {
  let active = true
  let requests = 0
  await assert.rejects(model.loadAccountActivityTransactions(range, async () => {
    requests++
    if (requests === 1) return Array.from({ length: 200 }, (_, index) => row(index))
    active = false
    return []
  }, () => active), /superseded/)
  assert.ok(requests <= 5)
})

function deferred() { let resolve; let reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }

function hookHarness() {
  const slots = []
  let cursor = 0
  let effects = []
  let timerId = 0
  const timers = new Map()
  const requests = []
  const same = (left, right) => left && left.length === right.length && left.every((value, index) => Object.is(value, right[index]))
  const react = {
    useState(initial) { const index = cursor++; slots[index] ??= { value: initial }; return [slots[index].value, update => { slots[index].value = typeof update === "function" ? update(slots[index].value) : update }] },
    useCallback(callback, dependencies) { const index = cursor++; if (!same(slots[index]?.dependencies, dependencies)) slots[index] = { callback, dependencies }; return slots[index].callback },
    useEffect(effect, dependencies) { const index = cursor++; if (!same(slots[index]?.dependencies, dependencies)) effects.push(() => { slots[index]?.cleanup?.(); slots[index] = { dependencies, cleanup: effect() } }) },
  }
  const { useAccountActivity } = load("../src/mobile/pages/finance/hooks/use-account-activity.ts", {
    react, "@/services/finance/ledger": { fetchFinanceTransactions: () => { const request = deferred(); requests.push(request); return request.promise } },
    "../lib/account-activity": { ...model, accountActivityRange: () => range },
  }, { window: { setTimeout: callback => { const id = ++timerId; timers.set(id, callback); return id }, clearTimeout: id => timers.delete(id) } })
  return {
    requests,
    render(props) { cursor = 0; effects = []; const state = useAccountActivity(props); effects.forEach(effect => effect()); return state },
    runTimers() { const callbacks = [...timers.values()]; timers.clear(); callbacks.forEach(callback => callback()) },
    async settle() { await new Promise(resolve => setImmediate(resolve)) },
  }
}

test("workspace switches immediately hide prior rankings and reject late responses", async () => {
  const hook = hookHarness()
  const first = { workspaceId: "personal" }
  hook.render(first); hook.runTimers(); hook.requests[0].resolve([row(1)]); await hook.settle()
  let state = hook.render(first)
  assert.equal(state.snapshot.transactions[0].id, 1)
  state.refresh(); hook.render(first); hook.runTimers()
  const second = { workspaceId: "work" }
  state = hook.render(second)
  assert.equal(state.snapshot, undefined)
  hook.runTimers(); hook.requests[2].resolve([row(3)]); await hook.settle()
  hook.requests[1].resolve([row(2)]); await hook.settle()
  assert.equal(hook.render(second).snapshot.transactions[0].id, 3)
})

test("refresh failures retain same-scope data as stale and refreshToken retries it", async () => {
  const hook = hookHarness()
  const props = { workspaceId: "personal", refreshToken: 0 }
  hook.render(props); hook.runTimers(); hook.requests[0].resolve([row(1)]); await hook.settle()
  hook.render({ ...props, refreshToken: 1 }); hook.runTimers(); hook.requests[1].reject(new Error("offline")); await hook.settle()
  let state = hook.render({ ...props, refreshToken: 1 })
  assert.equal(state.status, "error")
  assert.equal(state.stale, true)
  assert.equal(state.snapshot.transactions[0].id, 1)
  hook.render({ ...props, refreshToken: 2 }); hook.runTimers(); hook.requests[2].resolve([row(2)]); await hook.settle()
  state = hook.render({ ...props, refreshToken: 2 })
  assert.equal(state.status, "ready")
  assert.equal(state.stale, false)
  assert.equal(state.snapshot.transactions[0].id, 2)
})
