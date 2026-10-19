import { ArrowDownLeft, ArrowLeftRight, ArrowUpRight, BriefcaseBusiness, CalendarClock, ChevronDown, ChevronLeft, ChevronRight, House, RefreshCw, Search, ShoppingBag, ShoppingBasket, TrainFront, Utensils, Wallet, X } from "lucide-react"
import { useEffect, useMemo, useRef, useState } from "react"
import { useLocation, useNavigate, useOutletContext } from "react-router-dom"

import { useWorkspace } from "@/layout/workspace-provider"
import type { FinanceTransaction } from "@/services/finance/ledger"
import { ActivityAmount, ActivityCash, ActivityCashBreakdown } from "./components/activity-cash"
import { ActivityEntry } from "./components/activity-entry"
import { ActivityInstallments } from "./components/activity-installments"
import { ActivitySheet } from "./components/activity-sheet"
import { useFinanceActivity } from "./hooks/use-finance-activity"
import { activityDateKey, activityMonthKey, activityTransactionFen, currentActivityMonth, filterActivityTransactions, groupActivityTransactions, shiftActivityMonth, summarizeActivityTransactions, type ActivityFilterType, type ActivityMonth } from "./lib/activity"
import { formatCny } from "./lib/asset-pile"
import "./overview.css"
import "./activity.css"

const filters = [{ type: "all", label: "All" }, { type: "expense", label: "Expenses" }, { type: "income", label: "Income" }, { type: "transfer", label: "Transfers" }] as const
function titleFor(record: FinanceTransaction) { return record.merchant || record.categoryName || (record.transactionType === "transfer" ? "Transfer" : "Record") }
function signed(fen: number) { return `${fen > 0 ? "+" : fen < 0 ? "−" : ""}${formatCny(Math.abs(fen))}` }
function recordAmount(record: FinanceTransaction) {
  const fen = activityTransactionFen(record)
  if (fen === null) return "Unavailable"
  const currency = record.currency?.trim().toUpperCase()
  const label = currency === "CNY" ? formatCny(fen) : `${currency || "Unknown currency"} ${(fen / 100).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
  return `${record.transactionType === "income" ? "+" : record.transactionType === "expense" ? "−" : "↔ "}${label}`
}
function dayLabel(day: string) {
  if (day === "unknown") return "Date unavailable"
  const now = new Date()
  if (day === activityDateKey(now.toISOString())) return "Today"
  now.setDate(now.getDate() - 1)
  return day === activityDateKey(now.toISOString()) ? "Yesterday" : new Date(`${day}T12:00:00`).toLocaleDateString("en-US", { weekday: "long" })
}
function recordIcon(record: FinanceTransaction) {
  if (record.transactionType === "transfer") return ArrowLeftRight
  if (record.transactionType === "income") return BriefcaseBusiness
  const name = `${record.categoryName ?? ""}`.toLowerCase()
  if (/food|dining|餐|饮/.test(name)) return Utensils
  if (/grocer|日用|超市/.test(name)) return ShoppingBasket
  if (/transport|travel|交通|出行/.test(name)) return TrainFront
  if (/home|rent|housing|房|居/.test(name)) return House
  if (/shop|购物/.test(name)) return ShoppingBag
  return Wallet
}

export function FinanceActivityView() {
  const { workspaceId } = useWorkspace()
  return <FinanceActivity key={workspaceId} workspaceId={workspaceId} />
}

function FinanceActivity({ workspaceId }: { workspaceId: string }) {
  const { financeRefreshToken } = useOutletContext<{ financeRefreshToken: number }>()
  const location = useLocation()
  const navigate = useNavigate()
  const [month, setMonth] = useState(currentActivityMonth)
  const [filter, setFilter] = useState<ActivityFilterType>("all")
  const [query, setQuery] = useState("")
  const [searching, setSearching] = useState(false)
  const [pickerYear, setPickerYear] = useState<number | null>(null)
  const [moneyType, setMoneyType] = useState<"income" | "expense" | null>(null)
  const [detail, setDetail] = useState<FinanceTransaction | null>(null)
  const [showInstallments, setShowInstallments] = useState(false)
  const [entry, setEntry] = useState<{ record?: FinanceTransaction; type: FinanceTransaction["transactionType"] } | null>(null)
  const [notice, setNotice] = useState("")
  const searchInput = useRef<HTMLInputElement>(null)
  const load = useFinanceActivity({ month, workspaceId, refreshToken: financeRefreshToken })
  const { snapshot } = load
  const summary = useMemo(() => summarizeActivityTransactions(snapshot?.transactions ?? [], { complete: !!snapshot?.complete }), [snapshot])
  const filtered = useMemo(() => filterActivityTransactions(snapshot?.transactions ?? [], { type: filter, query }), [snapshot, filter, query])
  const groups = useMemo(() => groupActivityTransactions(filtered, { complete: !!snapshot?.complete }), [filtered, snapshot?.complete])
  const monthText = new Date(month.year, month.month, 1).toLocaleDateString("en-US", { month: "long", year: "numeric" })
  const isCurrent = activityMonthKey(month) === activityMonthKey(currentActivityMonth())
  const entryAvailable = !!snapshot?.accountsAvailable && !!snapshot?.categoriesAvailable
  const action = location.state as { mobileAction?: string; requestId?: string } | null

  useEffect(() => {
    if (action?.mobileAction !== "finance-entry" || !action.requestId) return
    const timer = window.setTimeout(() => {
      setEntry({ type: "expense" })
      navigate({ hash: "#finance-activity" }, { replace: true, state: null })
    }, 0)
    return () => window.clearTimeout(timer)
  }, [action?.mobileAction, action?.requestId, navigate])
  useEffect(() => {
    if (!notice) return
    const timer = window.setTimeout(() => setNotice(""), 4500)
    return () => window.clearTimeout(timer)
  }, [notice])

  function chooseMonth(next: ActivityMonth) {
    setMonth(next); setPickerYear(null); setDetail(null); setMoneyType(null)
    document.getElementById("mobile-content")?.scrollTo({ top: 0 })
  }
  function saved(record: FinanceTransaction, message?: string) {
    setEntry(null); setFilter("all"); setQuery("")
    const date = new Date(record.occurredAt)
    if (Number.isFinite(date.getTime())) chooseMonth(currentActivityMonth(date))
    load.refresh()
    setNotice(message ?? "Record saved. Refreshing monthly activity…")
  }
  const cashAmount = moneyType === "income" ? summary.incomeFen : summary.expenseFen

  return <section className="mobile-finance-activity" aria-label="Finance activity" aria-busy={load.status === "loading"}>
    <div className="fa-heading"><h2>Activity</h2><div><button type="button" className="fa-round" aria-label="Search records" title="Search records" aria-expanded={searching} onClick={() => { setSearching(!searching); if (!searching) requestAnimationFrame(() => searchInput.current?.focus()); else setQuery("") }}><Search aria-hidden="true" /></button><button type="button" className="fa-round" aria-label="Refresh activity" title="Refresh activity" disabled={load.status === "loading"} onClick={load.refresh}><RefreshCw aria-hidden="true" /></button></div></div>
    <div className="fa-period" aria-label="Month navigation">
      <button type="button" className="fa-round" aria-label="Previous month" title="Previous month" disabled={month.year === 1 && month.month === 0} onClick={() => chooseMonth(shiftActivityMonth(month, -1))}><ChevronLeft aria-hidden="true" /></button>
      <button type="button" className="fa-period-label" aria-label={`Choose month, ${monthText}`} aria-haspopup="dialog" onClick={() => setPickerYear(month.year)}>{isCurrent && <span className="fa-current-mark" />}{monthText}<ChevronDown aria-hidden="true" /></button>
      <button type="button" className="fa-round" aria-label="Next month" title="Next month" disabled={month.year === 9999 && month.month === 11} onClick={() => chooseMonth(shiftActivityMonth(month, 1))}><ChevronRight aria-hidden="true" /></button>
    </div>
    {searching && <div className="fa-search"><Search aria-hidden="true" /><input ref={searchInput} type="search" aria-label="Search merchant, category, account or note" placeholder="Search merchant, category, account…" value={query} onChange={(event) => setQuery(event.target.value)} /><button type="button" className="fa-round" aria-label="Close search" onClick={() => { setSearching(false); setQuery("") }}><X aria-hidden="true" /></button></div>}
    <div className="fa-flow-grid" aria-label="CNY totals for the selected month">{(["income", "expense"] as const).map((type) => {
      const amount = type === "income" ? summary.incomeFen : summary.expenseFen
      const count = type === "income" ? summary.incomeCount : summary.expenseCount
      const Icon = type === "income" ? ArrowDownLeft : ArrowUpRight
      return <button type="button" key={type} className={`fa-flow-card is-${type}`} disabled={amount === null} onClick={() => setMoneyType(type)} aria-haspopup="dialog" aria-label={`View ${type} cash breakdown${amount === null ? ", unavailable" : `, ${formatCny(amount)}`}`}>
        <span className="fa-flow-label"><Icon aria-hidden="true" />{type === "income" ? "Income" : "Expenses"}</span><ActivityAmount amount={amount} /><ActivityCash amount={amount ?? 0} /><span className="fa-flow-foot"><span>{snapshot ? `${snapshot.complete ? "" : "At least "}${count} ${count === 1 ? "record" : "records"}` : load.status === "loading" ? "Loading…" : "Unavailable"}</span><ChevronRight aria-hidden="true" /></span>
      </button>
    })}</div>
    <div className="fa-net"><span>Net cash flow · CNY</span><strong>{summary.netFen === null ? "—" : signed(summary.netFen)}</strong></div>
    {(load.error || summary.invalidCount > 0 || snapshot && (summary.incomeFen === null || summary.expenseFen === null)) && <div className="fa-alert" role="status"><p>{load.stale ? "Could not refresh. Showing previously loaded records." : load.error || "Some amounts are unavailable. Monthly totals cannot be calculated."}</p><button type="button" className="fa-text-button" disabled={load.status === "loading"} onClick={load.refresh}>Try again</button></div>}
    <div className="fa-filters" role="group" aria-label="Filter record type">{filters.map((item) => <button type="button" key={item.type} aria-pressed={filter === item.type} onClick={() => setFilter(item.type)}>{item.label}</button>)}</div>
    <div className="fa-ledger-heading"><h3>Transactions</h3><span role="status">{snapshot ? `${snapshot.complete ? "" : "At least "}${filtered.length} ${filtered.length === 1 ? "record" : "records"}` : load.status === "loading" ? "Loading…" : "Unavailable"}</span></div>
    <button type="button" className="fa-installments-link" disabled={!snapshot?.accountsAvailable} onClick={() => setShowInstallments(true)}><CalendarClock aria-hidden="true" /><span>Installment plans</span><ChevronRight aria-hidden="true" /></button>
    {!snapshot ? <div className="fa-empty" role="status"><Wallet aria-hidden="true" /><h3>{load.status === "loading" ? "Gathering your activity…" : "Activity unavailable"}</h3><p>{load.status === "loading" ? "Loading the selected month." : "Your transactions could not be loaded."}</p>{load.status === "error" && <button type="button" className="fa-text-button" onClick={load.refresh}>Try again</button>}</div> : !filtered.length ? <div className="fa-empty"><Wallet aria-hidden="true" /><h3>{query ? "No matching records." : !snapshot.transactions.length && snapshot.complete ? "A fresh page for this month." : "No records to show."}</h3><p>{query ? "Try another merchant, category, account or note." : !snapshot.complete ? "Some records could not be loaded. Try refreshing this month." : "Add an expense, income or transfer. Your monthly flow will appear above."}</p><button type="button" className="fa-text-button" onClick={() => query ? setQuery("") : setEntry({ type: filter === "all" ? "expense" : filter })}>{query ? "Clear search" : "Add a record"}</button></div> : groups.map((group) => <section className="fa-day" key={group.date} aria-label={group.date}>
      <div className="fa-date-heading"><strong>{dayLabel(group.date)}{group.date !== "unknown" && <time dateTime={group.date}>{new Date(`${group.date}T12:00:00`).toLocaleDateString("en-US", { month: "short", day: "numeric" })}</time>}</strong><span>{filter === "transfer" || group.summary.excludedCurrencyCount > 0 || group.summary.netFen === null ? `${group.transactions.length} records` : signed(group.summary.netFen)}</span></div>
      <ul className="fa-record-list">{group.transactions.map((record) => { const Icon = recordIcon(record); return <li key={record.id}><button type="button" className="fa-record" onClick={() => setDetail(record)} aria-haspopup="dialog"><span className={`fa-record-icon is-${record.transactionType}`}><Icon aria-hidden="true" /></span><span className="fa-record-copy"><strong>{titleFor(record)}</strong><span>{record.transactionType === "transfer" ? `${record.accountName ?? "Account"} → ${record.toAccountName ?? "Account"}` : `${record.categoryName ?? "Uncategorized"} · ${record.accountName ?? "Account"}`}</span></span><span className={`fa-record-amount is-${record.transactionType}`}>{recordAmount(record)}</span></button></li> })}</ul>
    </section>)}
    <p className="fa-footnote">Monthly cash piles show CNY only. Transfers and credit repayments are excluded from income and expenses.{summary.excludedCurrencyCount > 0 && " Other currencies remain individually labeled in the list."}{snapshot && <><br />{load.status === "loading" ? "Refreshing…" : `Updated ${snapshot.updatedAt.toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" })}`}</>}</p>
    <div className="fa-notice" role="status">{notice}</div>
    {pickerYear !== null && <ActivitySheet title="Choose a month" onClose={() => setPickerYear(null)}><div className="fa-year-switch"><button type="button" className="fa-round" aria-label="Previous year" disabled={pickerYear <= 1} onClick={() => setPickerYear(pickerYear - 1)}><ChevronLeft aria-hidden="true" /></button><strong>{pickerYear}</strong><button type="button" className="fa-round" aria-label="Next year" disabled={pickerYear >= 9999} onClick={() => setPickerYear(pickerYear + 1)}><ChevronRight aria-hidden="true" /></button></div><div className="fa-month-grid">{Array.from({ length: 12 }, (_, index) => <button type="button" key={index} aria-pressed={month.year === pickerYear && month.month === index} onClick={() => chooseMonth({ year: pickerYear, month: index })}>{new Date(2000, index, 1).toLocaleDateString("en-US", { month: "short" })}</button>)}</div><button type="button" className="fa-primary" onClick={() => chooseMonth(currentActivityMonth())}>Back to this month</button></ActivitySheet>}
    {moneyType && cashAmount !== null && <ActivitySheet title={moneyType === "income" ? "Income, in cash" : "Expenses, in cash"} onClose={() => setMoneyType(null)}><p className="fa-detail-amount">{formatCny(cashAmount)}</p><p className="fa-message">{monthText} · CNY transactions</p><ActivityCash amount={cashAmount} size="detail" /><ActivityCashBreakdown amount={cashAmount} /><p className="fa-footnote">Cash equivalents of recorded flows, not an account balance.</p></ActivitySheet>}
    {detail && <ActivitySheet title={titleFor(detail)} onClose={() => setDetail(null)}><p className="fa-detail-type">{detail.transactionType === "transfer" ? "Transfer · excluded from income and expenses" : detail.transactionType === "income" ? "Income" : "Expense"}</p><p className="fa-detail-amount">{recordAmount(detail)}</p>{detail.currency.toUpperCase() === "CNY" && <ActivityCash amount={activityTransactionFen(detail) ?? 0} size="detail" />}<dl className="fa-detail-grid">{(detail.transactionType === "transfer" ? [["From", detail.accountName], ["To", detail.toAccountName]] : [["Category", detail.categoryName], ["Account", detail.accountName]]).concat([["Date", Number.isFinite(Date.parse(detail.occurredAt)) ? new Date(detail.occurredAt).toLocaleString("en-GB") : "Unavailable"], ["Currency", detail.currency], ...(detail.note ? [["Note", detail.note]] : [])]).map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value || "—"}</dd></div>)}</dl>{detail.currency.toUpperCase() === "CNY" && !detail.archived && <button type="button" className="fa-primary" disabled={!entryAvailable} onClick={() => { setEntry({ record: detail, type: detail.transactionType }); setDetail(null) }}>Edit record</button>}{detail.currency.toUpperCase() !== "CNY" && <p className="fa-message">Edit this currency in desktop Finance.</p>}</ActivitySheet>}
    {entry && <ActivityEntry record={entry.record} initialType={entry.type} month={month} accounts={snapshot?.accounts ?? []} categories={snapshot?.categories ?? []} available={entryAvailable} loading={load.status === "loading"} onClose={() => setEntry(null)} onSaved={saved} onPosted={load.refresh} onOpenInstallments={() => { setEntry(null); setShowInstallments(true) }} />}
    {showInstallments && <ActivityInstallments accounts={snapshot?.accounts ?? []} onClose={() => setShowInstallments(false)} onChanged={load.refresh} />}
  </section>
}
