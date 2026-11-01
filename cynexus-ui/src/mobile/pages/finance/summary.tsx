import { RefreshCw } from "lucide-react"
import { useEffect, useMemo, useState } from "react"
import { useOutletContext } from "react-router-dom"

import { useWorkspace } from "@/layout/workspace-provider"
import { fetchFinanceAccounts, fetchFinanceTransactions, type FinanceAccount, type FinanceTransaction } from "@/services/finance/ledger"
import { activityMonthKey, activityTransactionFen, currentActivityMonth, loadActivityTransactions, shiftActivityMonth, type ActivityMonth } from "./lib/activity"
import { groupCategories, groupDays, summarizeRows } from "./lib/summary-logic.mjs"
import "./summary.css"

type Period = "month" | "year"
interface MonthData { month: ActivityMonth; complete: boolean; transactions: FinanceTransaction[]; failure: boolean }
interface Snapshot { scope: string; months: MonthData[]; accounts: FinanceAccount[]; accountsAvailable: boolean; updatedAt: Date }
interface State { status: "loading" | "ready" | "error"; snapshot?: Snapshot }

function monthRange(year: number): ActivityMonth[] { return Array.from({ length: 12 }, (_, month) => ({ year, month })) }
function money(value: number | null, currency = "CNY") { return value === null ? "Unavailable" : new Intl.NumberFormat("en-US", { style: "currency", currency, maximumFractionDigits: 2 }).format(value) }
function monthLabel(month: ActivityMonth, options: Intl.DateTimeFormatOptions = { month: "short" }) { return new Date(month.year, month.month, 1).toLocaleDateString("en-US", options) }

async function concurrent<T, R>(items: T[], limit: number, run: (item: T) => Promise<R>): Promise<R[]> {
  const output = new Array<R>(items.length)
  let cursor = 0
  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (cursor < items.length) { const index = cursor++; output[index] = await run(items[index]) }
  }))
  return output
}

async function loadMonth(month: ActivityMonth, active: () => boolean): Promise<MonthData> {
  const activity = await loadActivityTransactions(month, fetchFinanceTransactions, active).then((value) => ({ value, failed: false }), () => ({ value: null, failed: true }))
  return {
    month,
    complete: activity.value?.complete ?? false,
    transactions: activity.value?.transactions ?? [],
    failure: activity.failed,
  }
}

export function FinanceSummaryView() {
  const { workspaceId } = useWorkspace()
  return <FinanceSummary key={workspaceId} />
}

function FinanceSummary() {
  const { financeRefreshToken } = useOutletContext<{ financeRefreshToken: number }>()
  const now = currentActivityMonth()
  const [period, setPeriod] = useState<Period>("month")
  const [cursor, setCursor] = useState<ActivityMonth>(now)
  const [accountId, setAccountId] = useState("all")
  const [categoryType, setCategoryType] = useState<"expense" | "income">("expense")
  const [selectedTrend, setSelectedTrend] = useState(0)
  const [reload, setReload] = useState(0)
  const [state, setState] = useState<State>({ status: "loading" })

  useEffect(() => {
    let active = true
    const months = period === "month" ? [cursor] : monthRange(cursor.year)
    const scope = period === "month" ? activityMonthKey(cursor) : String(cursor.year)
    const timer = window.setTimeout(() => {
      setState((previous) => ({ ...previous, status: "loading" }))
      void Promise.all([
        concurrent(months, 3, (month) => loadMonth(month, () => active)),
        fetchFinanceAccounts(false).catch(() => null),
      ]).then(([results, accounts]) => {
        if (active) setState({ status: "ready", snapshot: { scope, months: results, accounts: accounts ?? [], accountsAvailable: accounts !== null, updatedAt: new Date() } })
      }).catch(() => { if (active) setState((previous) => ({ ...previous, status: "error" })) })
    }, 0)
    return () => { active = false; window.clearTimeout(timer) }
  }, [period, cursor, financeRefreshToken, reload])

  const scope = period === "month" ? activityMonthKey(cursor) : String(cursor.year)
  const current = state.snapshot?.scope === scope ? state.snapshot : undefined
  const selectedId = accountId === "all" ? null : Number(accountId)
  const filteredTransactions = useMemo(() => (current?.months ?? []).map((item) => ({ ...item, transactions: selectedId === null ? item.transactions : item.transactions.filter((row) => row.accountId === selectedId || row.toAccountId === selectedId) })), [current, selectedId])
  const selectedComplete = filteredTransactions.length > 0 && filteredTransactions.every((item) => item.complete && !item.failure)
  const allRows = useMemo(() => filteredTransactions.flatMap((item) => item.transactions), [filteredTransactions])
  const selectedTotals = useMemo(() => summarizeRows(allRows, { complete: selectedComplete, accountId: selectedId, toFen: activityTransactionFen }), [allRows, selectedComplete, selectedId])
  const totals = selectedTotals
  const totalsComplete = selectedTotals !== null
  const categories = useMemo(() => groupCategories(allRows, categoryType, selectedId, activityTransactionFen), [allRows, categoryType, selectedId])
  const trend = useMemo(() => period === "month"
    ? groupDays(allRows, filteredTransactions[0]?.complete && !filteredTransactions[0]?.failure, selectedId, activityTransactionFen)
    : filteredTransactions.map((item) => {
      const stat = summarizeRows(item.transactions, { complete: item.complete && !item.failure, accountId: selectedId, toFen: activityTransactionFen })
      return { key: activityMonthKey(item.month), label: monthLabel(item.month), income: stat?.income ?? null, expense: stat?.expense ?? null, net: stat?.net ?? null }
    }), [allRows, filteredTransactions, period, selectedId])
  const safeSelectedTrend = Math.min(selectedTrend, Math.max(0, trend.length - 1))
  const selectedDay = trend[safeSelectedTrend]
  const maxTrend = Math.max(1, ...trend.flatMap((item) => [item.income ?? 0, item.expense ?? 0, Math.abs(item.net ?? 0)]))
  const loading = state.status === "loading"
  const stale = loading && !!current
  const hasData = current?.months.some((item) => item.transactions.length)
  const incompleteCount = current?.months.filter((item) => !item.complete || item.failure).length ?? 0
  const allMonthsFailed = !!current?.months.length && current.months.every((item) => item.failure)
  const canGoForward = period === "month" ? cursor.year < now.year || (cursor.year === now.year && cursor.month < now.month) : cursor.year < now.year
  const canGoBackward = period === "month" ? cursor.year > 1 : cursor.year > 1
  function shiftPeriod(delta: number) {
    setSelectedTrend(0)
    setCursor(period === "month" ? shiftActivityMonth(cursor, delta) : { year: Math.max(1, cursor.year + delta), month: cursor.month })
  }

  return <section className="mobile-finance-summary" aria-label="Finance summary" aria-busy={loading}>
    <header className="mfs-head"><div><p className="mobile-finance-eyebrow">Finance</p><h2>Summary</h2></div><button className="finance-asset-icon-button" type="button" aria-label="Refresh summary" title="Refresh summary" disabled={loading} onClick={() => setReload((value) => value + 1)}><RefreshCw size={16} aria-hidden="true" /></button></header>
    <div className="mfs-controls"><div className="mfs-segment" role="group" aria-label="Summary period"><button type="button" aria-pressed={period === "month"} onClick={() => { setPeriod("month"); setSelectedTrend(0) }}>Month</button><button type="button" aria-pressed={period === "year"} onClick={() => { setPeriod("year"); setSelectedTrend(0) }}>Year</button></div><div className="mfs-period"><button type="button" aria-label={`Previous ${period}`} disabled={!canGoBackward} onClick={() => shiftPeriod(-1)}>‹</button><strong>{period === "month" ? `${monthLabel(cursor, { month: "long" })} ${cursor.year}` : cursor.year}</strong><button type="button" aria-label={`Next ${period}`} disabled={!canGoForward} onClick={() => shiftPeriod(1)}>›</button></div></div>
    <label className="mfs-account">Account<select value={accountId} disabled={!current?.accountsAvailable} onChange={(event) => setAccountId(event.target.value)}><option value="all">All accounts</option>{current?.accounts.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
    {loading && !current && <div className="mfs-state" role="status">Loading finance summary…</div>}
    {state.status === "error" && !current && <div className="mfs-state" role="alert">Summary could not be loaded.<button type="button" onClick={() => setReload((value) => value + 1)}>Retry</button></div>}
    {current && allMonthsFailed && !loading && <div className="mfs-state" role="alert">Transactions could not be loaded.<button type="button" onClick={() => setReload((value) => value + 1)}>Retry</button></div>}
    {current && !hasData && !allMonthsFailed && !loading && <div className="mfs-state">No finance data for this period.</div>}
    {current && hasData && <>
      <section className="mfs-totals" aria-label="Income and expenses"><div><span>Income</span><strong>{totalsComplete ? money(totals?.income ?? null) : "Partial data"}</strong></div><div><span>Expenses</span><strong>{totalsComplete ? money(totals?.expense ?? null) : "Partial data"}</strong></div><div><span>Net</span><strong>{totalsComplete && totals ? money(totals.income - totals.expense) : "Unavailable"}</strong></div></section>
      <section className="mfs-section"><div className="mobile-finance-section-heading"><h3>{period === "month" ? "Daily trend" : "Monthly trend"}</h3><span>{period === "year" ? "Calendar year" : "Recorded days"}</span></div><div className="mfs-chart">{trend.map((item, index) => <button type="button" className={`mfs-bar-group${index === safeSelectedTrend ? " is-selected" : ""}`} key={item.key} aria-label={`${item.label}: income ${item.income === null ? "unavailable" : money(item.income)}, expenses ${item.expense === null ? "unavailable" : money(item.expense)}`} aria-pressed={index === safeSelectedTrend} onClick={() => setSelectedTrend(index)}><div className="mfs-bars"><i className="is-income" style={{ height: `${item.income === null ? 0 : Math.max(3, item.income / maxTrend * 100)}%` }} /><i className="is-expense" style={{ height: `${item.expense === null ? 0 : Math.max(3, item.expense / maxTrend * 100)}%` }} />{period === "month" && item.net !== null && <i className={`is-net${item.net < 0 ? " is-negative" : ""}`} style={{ height: `${Math.max(3, Math.abs(item.net) / maxTrend * 100)}%` }} />}</div><span>{item.label}</span></button>)}</div><input className="mfs-slider" type="range" min="0" max={Math.max(0, trend.length - 1)} value={safeSelectedTrend} aria-label={period === "month" ? "Select trend day" : "Select trend month"} onChange={(event) => setSelectedTrend(Number(event.target.value))} disabled={!trend.length} /><div className="mfs-trend-readout" aria-live="polite"><strong>{selectedDay?.key ?? "No recorded activity"}</strong><span>Income {selectedDay?.income == null ? "Unavailable" : money(selectedDay.income)}</span><span>Expenses {selectedDay?.expense == null ? "Unavailable" : money(selectedDay.expense)}</span>{period === "month" && <span>Net {selectedDay?.net == null ? "Unavailable" : money(selectedDay.net)}</span>}</div><div className="mfs-legend"><span><i className="is-income" />Income</span><span><i className="is-expense" />Expenses</span>{period === "month" && <span><i className="is-net" />Net</span>}</div></section>
      <section className="mfs-section"><div className="mobile-finance-section-heading"><h3>Categories</h3><span>{totalsComplete ? "CNY · recorded transactions" : "Partial data"}</span></div><div className="mfs-segment" role="group" aria-label="Category type"><button type="button" aria-pressed={categoryType === "expense"} onClick={() => setCategoryType("expense")}>Expenses</button><button type="button" aria-pressed={categoryType === "income"} onClick={() => setCategoryType("income")}>Income</button></div>{!totalsComplete ? <p className="mfs-muted">Category totals are hidden because the summary data is incomplete.</p> : categories.length ? <ul className="mfs-categories">{categories.map((item) => <li key={String(item.id)}><span>{item.label}</span><strong>{money(item.amount)}</strong></li>)}</ul> : <p className="mfs-muted">No {categoryType} records in this period.</p>}</section>
      <p className="mfs-note" role="status">{incompleteCount ? `Coverage incomplete for ${incompleteCount} ${incompleteCount === 1 ? "month" : "months"}. Transaction-based category and account totals may omit records.` : "Transaction coverage is complete."}{stale ? " Refreshing; showing the last loaded data." : ` Updated ${current.updatedAt.toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" })}.`}</p>
    </>}
    {state.status === "error" && current && <p className="mfs-note" role="alert">Refresh failed. Showing the last loaded summary.</p>}
    {!current?.accountsAvailable && current && <p className="mfs-note" role="status">Account list unavailable; account filtering is disabled.</p>}
  </section>
}
