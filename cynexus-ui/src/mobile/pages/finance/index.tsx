import { ArrowUpRight, Banknote, ChevronDown, ChevronUp, CreditCard, Landmark, LoaderCircle, Pencil, Save, TrendingUp, Wallet, WalletCards, X } from "lucide-react"
import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties, type PointerEvent } from "react"
import { Link, useOutletContext } from "react-router-dom"

import { useWorkspace } from "@/layout/workspace-provider"
import { fetchFinanceAccounts, fetchFinanceOverview, saveFinanceAccount, type FinanceAccount, type FinanceOverview } from "@/services/finance/ledger"
import "@/mobile/pages/finance/finance.css"

type FinanceLoadState = "loading" | "ready" | "error"

const accountPalettes = ["coral", "gold", "sky", "mint", "violet"] as const
const currencySymbols: Record<string, string> = { CNY: "¥", USD: "$", EUR: "€", GBP: "£", JPY: "¥", HKD: "HK$" }
const accountTypeOptions: Array<{ value: FinanceAccount["accountType"]; label: string }> = [
  { value: "cash", label: "Cash" },
  { value: "bank", label: "Bank account" },
  { value: "e_wallet", label: "E-wallet" },
  { value: "huabei", label: "Credit account" },
  { value: "baitiao", label: "Credit account" },
  { value: "douyin_monthly", label: "Credit account" },
  { value: "credit_card", label: "Credit card" },
  { value: "investment", label: "Investment" },
  { value: "other", label: "Account" },
]
const creditAccountTypes = new Set<FinanceAccount["accountType"]>(["huabei", "baitiao", "douyin_monthly", "credit_card"])

function monthBounds() {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), 1)
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 0)
  const localDate = (value: Date) => `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`
  return { start: localDate(start), end: localDate(end) }
}

function formatMoney(value: number | null | undefined, currency = "CNY") {
  if (value == null || !Number.isFinite(value)) return "—"
  const amount = Math.abs(value).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return `${value < 0 ? "-" : ""}${currencySymbols[currency] ?? currency + " "}${amount}`
}

function accountLabel(accountType: FinanceAccount["accountType"]) {
  return ({
    cash: "Cash",
    bank: "Bank account",
    e_wallet: "E-wallet",
    huabei: "Credit account",
    baitiao: "Credit account",
    douyin_monthly: "Credit account",
    credit_card: "Credit card",
    investment: "Investment",
    other: "Account",
  } satisfies Record<FinanceAccount["accountType"], string>)[accountType]
}

function accountIcon(accountType: FinanceAccount["accountType"]) {
  if (accountType === "bank") return Landmark
  if (accountType === "credit_card" || accountType === "huabei" || accountType === "baitiao" || accountType === "douyin_monthly") return CreditCard
  if (accountType === "investment") return TrendingUp
  if (accountType === "cash") return Banknote
  if (accountType === "e_wallet") return WalletCards
  return Wallet
}

function accountPalette(account: FinanceAccount, index: number) {
  if (account.accountType === "e_wallet") return "coral"
  if (account.accountType === "bank" || account.accountType === "cash") return "gold"
  if (account.accountType === "investment") return "mint"
  if (account.accountType === "credit_card" || account.accountType === "huabei" || account.accountType === "baitiao" || account.accountType === "douyin_monthly") return "sky"
  return accountPalettes[index % accountPalettes.length]
}

export default function MobileFinancePage() {
  const { workspaceId } = useWorkspace()
  const { financeRefreshToken } = useOutletContext<{ financeRefreshToken: number }>()
  const [accountsState, setAccountsState] = useState<FinanceLoadState>("loading")
  const [overviewState, setOverviewState] = useState<FinanceLoadState>("loading")
  const [accounts, setAccounts] = useState<FinanceAccount[]>([])
  const [overview, setOverview] = useState<FinanceOverview | null>(null)
  const [reloadKey, setReloadKey] = useState(0)
  const bounds = useMemo(() => monthBounds(), [])

  const load = useCallback(async () => {
    setAccountsState("loading")
    setOverviewState("loading")
    const [accountsResult, overviewResult] = await Promise.allSettled([
      fetchFinanceAccounts(false),
      fetchFinanceOverview(bounds),
    ])
    if (accountsResult.status === "fulfilled") {
      setAccounts(accountsResult.value.filter((account) => !account.archived))
      setAccountsState("ready")
    } else {
      setAccountsState("error")
    }
    if (overviewResult.status === "fulfilled") {
      setOverview(overviewResult.value)
      setOverviewState("ready")
    } else {
      setOverviewState("error")
    }
  }, [bounds])

  useEffect(() => {
    const timer = window.setTimeout(() => { void load() }, 0)
    return () => window.clearTimeout(timer)
  }, [load, reloadKey, workspaceId, financeRefreshToken])

  const currency = "CNY"
  const displayAccounts = accounts.length ? accounts : overview?.accounts.filter((account) => !account.archived) ?? []
  const loading = accountsState === "loading" || overviewState === "loading"
  const unavailable = accountsState === "error" && overviewState === "error"
  const hasData = displayAccounts.length > 0 || Boolean(overview && (overview.income !== 0 || overview.expense !== 0 || overview.assetTotal !== 0 || overview.liabilityTotal !== 0 || overview.recentTransactions.length > 0))

  return (
    <div className="mobile-finance">
      <p className="mobile-finance-eyebrow">{new Intl.DateTimeFormat("en-US", { month: "long", year: "numeric" }).format(new Date())}</p>

      {loading && <FinanceLoading />}
      {unavailable && <FinanceError onRetry={() => setReloadKey((value) => value + 1)} />}
      {!loading && !unavailable && !hasData && <FinanceEmpty />}
      {!loading && !unavailable && hasData && (
        <>
          <section id="finance-overview" className="mobile-finance-totals" aria-label="Monthly totals">
            <div><span>Net assets</span><strong>{formatMoney(overview?.netAssets, currency)}</strong></div>
            <div><span>This month</span><strong>{formatMoney(overview?.expense, currency)}</strong><small>spent</small></div>
          </section>

          <section id="finance-accounts" className="mobile-finance-accounts" aria-labelledby="mobile-finance-accounts-title">
            <div className="mobile-finance-section-heading"><h3 id="mobile-finance-accounts-title">Your accounts</h3><span>{displayAccounts.length} {displayAccounts.length === 1 ? "account" : "accounts"}</span></div>
            {accountsState === "error" ? <div className="mobile-finance-inline-empty">Accounts are unavailable</div> : displayAccounts.length ? <AccountStack accounts={displayAccounts} onSaved={async (saved) => {
              setAccounts((current) => {
                const source = current.length ? current : displayAccounts
                return source.map((account) => account.id === saved.id ? saved : account)
              })
              await load()
            }} /> : <div className="mobile-finance-inline-empty">No accounts yet</div>}
          </section>

          <section id="finance-activity" className="mobile-finance-flow" aria-label="Monthly cash flow">
            <div><span>Income</span><strong className="is-income">{formatMoney(overview?.income, currency)}</strong></div>
            <div><span>Expense</span><strong className="is-expense">{formatMoney(overview?.expense, currency)}</strong></div>
            <div><span>Net</span><strong>{formatMoney(overview?.net, currency)}</strong></div>
          </section>
          {overviewState === "error" && <p className="mobile-finance-stale-note">Monthly totals are unavailable</p>}
        </>
      )}
    </div>
  )
}

function AccountStack({ accounts, onSaved }: { accounts: FinanceAccount[]; onSaved: (account: FinanceAccount) => Promise<void> | void }) {
  const [activeIndex, setActiveIndex] = useState(0)
  const [expandedId, setExpandedId] = useState<number | null>(null)
  const [editingField, setEditingField] = useState<EditableAccountField | null>(null)
  const [savingId, setSavingId] = useState<number | null>(null)
  const [saveError, setSaveError] = useState<string | null>(null)
  const gesture = useRef<{ startX: number; startY: number; moved: boolean } | null>(null)
  const suppressClick = useRef(false)

  const safeActiveIndex = accounts.length ? activeIndex % accounts.length : 0
  const orderedAccounts = accounts.length ? [...accounts.slice(safeActiveIndex), ...accounts.slice(0, safeActiveIndex)] : []
  const visibleAccounts = orderedAccounts.slice(0, Math.min(4, orderedAccounts.length))
  const expanded = expandedId != null

  function move(direction: 1 | -1) {
    if (accounts.length < 2) return
    setActiveIndex((current) => (current + direction + accounts.length) % accounts.length)
    setExpandedId(null)
    setEditingField(null)
    setSaveError(null)
  }

  function startGesture(event: PointerEvent<HTMLDivElement>) {
    if ((event.target as HTMLElement).closest("input, select, textarea, form, .mobile-finance-account-data-row")) return
    gesture.current = { startX: event.clientX, startY: event.clientY, moved: false }
    event.currentTarget.setPointerCapture(event.pointerId)
  }

  function trackGesture(event: PointerEvent<HTMLDivElement>) {
    if (!gesture.current) return
    if (Math.abs(event.clientX - gesture.current.startX) > 8 || Math.abs(event.clientY - gesture.current.startY) > 8) gesture.current.moved = true
  }

  function endGesture(event: PointerEvent<HTMLDivElement>) {
    const current = gesture.current
    gesture.current = null
    if (!current) return
    if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId)
    const deltaX = event.clientX - current.startX
    const deltaY = event.clientY - current.startY
    if (current.moved && Math.abs(deltaX) > 42 && Math.abs(deltaX) > Math.abs(deltaY) * 1.2) {
      suppressClick.current = true
      move(deltaX < 0 ? 1 : -1)
    }
  }

  async function save(account: FinanceAccount, field: EditableAccountField, rawValue: string) {
    const value = rawValue.trim()
    const accountType = field === "accountType" ? rawValue as FinanceAccount["accountType"] : account.accountType
    const name = field === "name" ? value : account.name
    const openingBalance = field === "openingBalance" ? parseNonNegative(rawValue) : account.openingBalance ?? 0
    const creditLimit = field === "creditLimit" ? parseNonNegativeOrNull(rawValue) : account.creditLimit
    const billingDay = field === "billingDay" ? parseDayOrNull(rawValue) : account.billingDay
    const dueDay = field === "dueDay" ? parseDayOrNull(rawValue) : account.dueDay
    if (!name || openingBalance == null || creditLimit === undefined || billingDay === undefined || dueDay === undefined) {
      setSaveError("Enter a valid value before saving.")
      return
    }
    const credit = creditAccountTypes.has(accountType)
    setSavingId(account.id)
    setSaveError(null)
    try {
      const saved = await saveFinanceAccount({
        id: account.id,
        name,
        accountType,
        nature: credit ? "credit" : "asset",
        currency: account.currency,
        openingBalance,
        creditLimit: credit ? creditLimit : null,
        billingDay: credit ? billingDay : null,
        dueDay: credit ? dueDay : null,
        color: account.color,
        notes: field === "notes" ? value || null : account.notes,
      })
      await onSaved(saved)
      setEditingField(null)
      setExpandedId(saved.id)
    } catch (cause) {
      setSaveError(cause instanceof Error ? cause.message : "Could not save this account.")
    } finally {
      setSavingId(null)
    }
  }

  return (
    <div
      className={`mobile-finance-stack${expanded ? " is-expanded" : ""}`}
      role="list"
      aria-label="Finance accounts"
      onPointerDown={startGesture}
      onPointerMove={trackGesture}
      onPointerUp={endGesture}
      onPointerCancel={endGesture}
      style={{ minHeight: `${expanded ? (editingField != null ? 420 : 330) : 164 + Math.max(0, visibleAccounts.length - 1) * 18}px` }}
    >
      {visibleAccounts.map((account, index) => {
        const Icon = accountIcon(account.accountType)
        const palette = accountPalette(account, index)
        const balance = account.balance ?? account.openingBalance
        const isActive = index === 0
        const isExpanded = isActive && expandedId === account.id
        const cardStyle = { "--stack-index": index, zIndex: visibleAccounts.length - index } as CSSProperties
        return (
          <article key={account.id} role="listitem" className={`mobile-finance-account-card is-${palette}${isActive ? " is-active" : ""}${isExpanded ? " is-open" : ""}`} style={cardStyle}>
            <button
              type="button"
              className="mobile-finance-account-summary"
              aria-expanded={isExpanded}
              aria-controls={`mobile-finance-account-edit-${account.id}`}
              onClick={() => {
                if (suppressClick.current) { suppressClick.current = false; return }
                if (!isActive) {
                  setActiveIndex((current) => (current + index) % accounts.length)
                  setExpandedId(account.id)
                  setEditingField(null)
                  setSaveError(null)
                  return
                }
                const isOpen = expandedId === account.id
                setExpandedId(isOpen ? null : account.id)
                setEditingField(null)
                setSaveError(null)
              }}
              title={isExpanded ? "Close account details" : "Open account details"}
            >
              <span className="mobile-finance-account-top">
                <span className="mobile-finance-account-icon"><Icon aria-hidden="true" /></span>
                <span className="mobile-finance-account-meta"><strong>{account.name}</strong><small>{accountLabel(account.accountType)}</small></span>
                <span className="mobile-finance-account-currency">{account.currency}</span>
              </span>
              <span className="mobile-finance-account-bottom">
                <span>{account.nature === "credit" ? "Current due" : "Balance"}</span>
                <strong>{formatMoney(balance, account.currency)}</strong>
                <span className="mobile-finance-account-action">{isExpanded ? <ChevronUp aria-hidden="true" /> : <ChevronDown aria-hidden="true" />}</span>
              </span>
            </button>
            {isExpanded && <div id={`mobile-finance-account-edit-${account.id}`} className="mobile-finance-account-expanded">
              <AccountExpandedDetails account={account} editingField={editingField} saving={savingId === account.id} error={saveError} onBeginEdit={(field) => { setEditingField(field); setSaveError(null) }} onCancel={() => { setEditingField(null); setSaveError(null) }} onSave={(field, value) => save(account, field, value)} />
            </div>}
          </article>
        )
      })}
      {accounts.length > 1 && <div className="mobile-finance-stack-hint" aria-live="polite"><span>{safeActiveIndex + 1} of {accounts.length}</span><span>Swipe to switch</span></div>}
    </div>
  )
}

type EditableAccountField = "name" | "accountType" | "openingBalance" | "creditLimit" | "billingDay" | "dueDay" | "notes"

function AccountExpandedDetails({ account, editingField, saving, error, onBeginEdit, onCancel, onSave }: { account: FinanceAccount; editingField: EditableAccountField | null; saving: boolean; error: string | null; onBeginEdit: (field: EditableAccountField) => void; onCancel: () => void; onSave: (field: EditableAccountField, value: string) => Promise<void> }) {
  return <div className="mobile-finance-account-details">
    <p className="mobile-finance-account-details-hint">Double-click a value to edit</p>
    <EditableDataField key={`name-${account.name}`} field="name" label="Name" value={account.name} editingField={editingField} saving={saving} error={error} onBeginEdit={onBeginEdit} onCancel={onCancel} onSave={onSave} />
    <EditableDataField key={`type-${account.accountType}`} field="accountType" label="Type" value={accountLabel(account.accountType)} inputValue={account.accountType} inputKind="select" editingField={editingField} saving={saving} error={error} onBeginEdit={onBeginEdit} onCancel={onCancel} onSave={onSave} />
    <div className="mobile-finance-account-data-row"><span>Balance</span><strong>{formatMoney(account.balance ?? account.openingBalance, account.currency)}</strong></div>
    <EditableDataField key={`opening-${account.openingBalance}`} field="openingBalance" label="Opening balance" value={formatMoney(account.openingBalance, account.currency)} inputValue={String(account.openingBalance ?? 0)} inputKind="number" editingField={editingField} saving={saving} error={error} onBeginEdit={onBeginEdit} onCancel={onCancel} onSave={onSave} />
    {account.nature === "credit" && <>
      <EditableDataField key={`limit-${account.creditLimit}`} field="creditLimit" label="Credit limit" value={account.creditLimit == null ? "Not set" : formatMoney(account.creditLimit, account.currency)} inputValue={account.creditLimit == null ? "" : String(account.creditLimit)} inputKind="number" editingField={editingField} saving={saving} error={error} onBeginEdit={onBeginEdit} onCancel={onCancel} onSave={onSave} />
      <EditableDataField key={`billing-${account.billingDay}`} field="billingDay" label="Billing day" value={account.billingDay == null ? "Not set" : String(account.billingDay)} inputValue={account.billingDay == null ? "" : String(account.billingDay)} inputKind="day" editingField={editingField} saving={saving} error={error} onBeginEdit={onBeginEdit} onCancel={onCancel} onSave={onSave} />
      <EditableDataField key={`due-${account.dueDay}`} field="dueDay" label="Due day" value={account.dueDay == null ? "Not set" : String(account.dueDay)} inputValue={account.dueDay == null ? "" : String(account.dueDay)} inputKind="day" editingField={editingField} saving={saving} error={error} onBeginEdit={onBeginEdit} onCancel={onCancel} onSave={onSave} />
    </>}
    <EditableDataField key={`notes-${account.notes}`} field="notes" label="Notes" value={account.notes || "Not set"} inputValue={account.notes ?? ""} inputKind="text" editingField={editingField} saving={saving} error={error} onBeginEdit={onBeginEdit} onCancel={onCancel} onSave={onSave} wide />
  </div>
}

function EditableDataField({ field, label, value, inputValue = value, inputKind = "text", editingField, saving, error, onBeginEdit, onCancel, onSave, wide = false }: { field: EditableAccountField; label: string; value: string; inputValue?: string; inputKind?: "text" | "number" | "day" | "select"; editingField: EditableAccountField | null; saving: boolean; error: string | null; onBeginEdit: (field: EditableAccountField) => void; onCancel: () => void; onSave: (field: EditableAccountField, value: string) => Promise<void>; wide?: boolean }) {
  const [draft, setDraft] = useState(inputValue)
  const editing = editingField === field

  if (editing) {
    return <form key={`${field}-editor`} className={`mobile-finance-account-data-row is-editing${wide ? " is-wide" : ""}`} onSubmit={(event) => { event.preventDefault(); void onSave(field, draft) }}>
      <label><span>{label}</span>{inputKind === "select" ? <select value={draft} onChange={(event) => setDraft(event.target.value)}>{accountTypeOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select> : <input autoFocus inputMode={inputKind === "number" ? "decimal" : inputKind === "day" ? "numeric" : undefined} type={inputKind === "number" || inputKind === "day" ? "number" : "text"} min={inputKind === "number" ? "0" : inputKind === "day" ? "1" : undefined} max={inputKind === "day" ? "31" : undefined} step={inputKind === "number" ? "0.01" : undefined} value={draft} onChange={(event) => setDraft(event.target.value)} />}</label>
      <div className="mobile-finance-inline-actions"><button type="button" onClick={onCancel} disabled={saving}><X aria-hidden="true" /></button><button type="submit" disabled={saving}>{saving ? <LoaderCircle aria-hidden="true" className="is-spinning" /> : <Save aria-hidden="true" />}</button></div>
      {error && <p className="mobile-finance-editor-error" role="alert">{error}</p>}
    </form>
  }

  return <button type="button" className={`mobile-finance-account-data-row${wide ? " is-wide" : ""}`} title={`Double-click to edit ${label}`} onDoubleClick={() => onBeginEdit(field)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); onBeginEdit(field) } }}><span>{label}</span><strong>{value}</strong><Pencil aria-hidden="true" /></button>
}

function parseNonNegative(value: string) {
  if (!value.trim()) return null
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : null
}

function parseNonNegativeOrNull(value: string) {
  if (!value.trim()) return null
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : undefined
}

function parseDayOrNull(value: string) {
  if (!value.trim()) return null
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed >= 1 && parsed <= 31 ? parsed : undefined
}

function FinanceLoading() {
  return <div className="mobile-finance-state" aria-busy="true"><div className="mobile-finance-skeleton mobile-finance-skeleton-large" /><div className="mobile-finance-skeleton mobile-finance-skeleton-card" /><div className="mobile-finance-skeleton mobile-finance-skeleton-card is-short" /></div>
}

function FinanceError({ onRetry }: { onRetry: () => void }) {
  return <div className="mobile-finance-message"><WalletCards aria-hidden="true" /><strong>Finance is unavailable</strong><p>We could not load your accounts.</p><button type="button" onClick={onRetry}>Try again</button></div>
}

function FinanceEmpty() {
  return <div className="mobile-finance-message"><Wallet aria-hidden="true" /><strong>Start with an account</strong><p>Add an account from the Finance workspace to see it here.</p><Link to="/finance/overview">Open Finance workspace <ArrowUpRight aria-hidden="true" /></Link></div>
}
