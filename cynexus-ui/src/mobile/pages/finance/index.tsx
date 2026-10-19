import { ArrowUpRight, Banknote, ChevronLeft, ChevronRight, CreditCard, Landmark, LoaderCircle, Pencil, Plus, Save, TrendingUp, Wallet, WalletCards, X } from "lucide-react"
import { createPortal } from "react-dom"
import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties, type FormEvent, type PointerEvent } from "react"
import { Link, useLocation, useOutletContext } from "react-router-dom"

import { useWorkspace } from "@/layout/workspace-provider"
import { useMobilePortal } from "@/mobile/hooks/use-mobile-portal"
import { fetchFinanceAccounts, fetchFinanceOverview, saveFinanceAccount, type FinanceAccount, type FinanceOverview } from "@/services/finance/ledger"
import "@/mobile/pages/finance/finance.css"
import { FinanceOverviewView } from "./overview"
import { FinanceActivityView } from "./activity"

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
  const { hash } = useLocation()

  if (hash === "#finance-accounts") return <FinanceAccountsView />
  if (hash === "#finance-activity") return <FinanceActivityView />
  return <FinanceOverviewView />
}

function FinanceAccountsView() {
  const { workspaceId } = useWorkspace()
  const { financeRefreshToken } = useOutletContext<{ financeRefreshToken: number }>()
  const [accountsState, setAccountsState] = useState<FinanceLoadState>("loading")
  const [overviewState, setOverviewState] = useState<FinanceLoadState>("loading")
  const [accounts, setAccounts] = useState<FinanceAccount[]>([])
  const [overview, setOverview] = useState<FinanceOverview | null>(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [accountForm, setAccountForm] = useState<{ mode: "add" | "edit"; account?: FinanceAccount } | null>(null)
  const [focusAccountId, setFocusAccountId] = useState<number | null>(null)
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
    <div id="finance-accounts" className="mobile-finance">
      <header className="mobile-finance-page-head">
        <div><p className="mobile-finance-eyebrow">Finance</p><h2>Your accounts</h2></div>
        <div className="mobile-finance-page-actions"><span>{displayAccounts.length} {displayAccounts.length === 1 ? "account" : "accounts"}</span><button type="button" className="mobile-finance-circle-button" aria-label="Add account" title="Add account" onClick={() => setAccountForm({ mode: "add" })}><Plus aria-hidden="true" /></button></div>
      </header>

      {loading && <FinanceLoading />}
      {unavailable && <FinanceError onRetry={() => setReloadKey((value) => value + 1)} />}
      {!loading && !unavailable && !hasData && <FinanceEmpty onAdd={() => setAccountForm({ mode: "add" })} />}
      {!loading && !unavailable && hasData && (
        <>
          <section className="mobile-finance-accounts" aria-labelledby="mobile-finance-accounts-title">
            {accountsState === "error" ? <div className="mobile-finance-inline-empty">Accounts are unavailable</div> : displayAccounts.length ? <AccountStack accounts={displayAccounts} overview={overview} overviewState={overviewState} focusAccountId={focusAccountId} onFocusHandled={() => setFocusAccountId(null)} onEdit={(account) => setAccountForm({ mode: "edit", account })} onSaved={async (saved) => {
              setAccounts((current) => {
                const source = current.length ? current : displayAccounts
                return source.map((account) => account.id === saved.id ? saved : account)
              })
              await load()
            }} /> : <div className="mobile-finance-inline-empty">No accounts yet</div>}
          </section>

          <section className="mobile-finance-flow" aria-label="Monthly cash flow">
            <div><span>Income</span><strong className="is-income">{formatMoney(overview?.income, currency)}</strong></div>
            <div><span>Expense</span><strong className="is-expense">{formatMoney(overview?.expense, currency)}</strong></div>
            <div><span>Net flow</span><strong>{formatMoney(overview?.net, currency)}</strong></div>
          </section>
          <p className="mobile-finance-derived-note">Balances are derived from opening balances and saved activity.</p>
          {overviewState === "error" && <p className="mobile-finance-stale-note">Monthly totals are unavailable</p>}
        </>
      )}
      {accountForm && <AccountFormSheet mode={accountForm.mode} account={accountForm.account} onClose={() => setAccountForm(null)} onSaved={async (saved) => { setAccountForm(null); setFocusAccountId(saved.id); await load() }} />}
    </div>
  )
}

function AccountStack({ accounts, overview, overviewState, focusAccountId, onFocusHandled, onEdit, onSaved }: { accounts: FinanceAccount[]; overview: FinanceOverview | null; overviewState: FinanceLoadState; focusAccountId: number | null; onFocusHandled: () => void; onEdit: (account: FinanceAccount) => void; onSaved: (account: FinanceAccount) => Promise<void> | void }) {
  const [activeIndex, setActiveIndex] = useState(0)
  const [editingField, setEditingField] = useState<EditableAccountField | null>(null)
  const [savingId, setSavingId] = useState<number | null>(null)
  const [saveError, setSaveError] = useState<string | null>(null)
  const gesture = useRef<{ startX: number; startY: number; moved: boolean } | null>(null)
  const suppressClick = useRef(false)

  const focusIndex = focusAccountId == null ? -1 : accounts.findIndex((account) => account.id === focusAccountId)
  const safeActiveIndex = accounts.length ? (focusIndex >= 0 ? focusIndex : activeIndex % accounts.length) : 0
  const orderedAccounts = accounts.length ? [...accounts.slice(safeActiveIndex), ...accounts.slice(0, safeActiveIndex)] : []
  const visibleAccounts = orderedAccounts.slice(0, Math.min(4, orderedAccounts.length))
  const activeAccount = accounts[safeActiveIndex]

  function selectIndex(index: number) {
    onFocusHandled()
    setActiveIndex(index)
  }

  function move(direction: 1 | -1) {
    if (accounts.length < 2) return
    onFocusHandled()
    setActiveIndex((current) => (current + direction + accounts.length) % accounts.length)
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
    } catch (cause) {
      setSaveError(cause instanceof Error ? cause.message : "Could not save this account.")
    } finally {
      setSavingId(null)
    }
  }

  return <>
    <div className="mobile-finance-stack-wrap">
      <div
        className="mobile-finance-stack"
        role="list"
        aria-label="Finance accounts"
        onPointerDown={startGesture}
        onPointerMove={trackGesture}
        onPointerUp={endGesture}
        onPointerCancel={endGesture}
        style={{ minHeight: `${205 + Math.max(0, visibleAccounts.length - 1) * 18}px` }}
      >
      {visibleAccounts.map((account, index) => {
        const Icon = accountIcon(account.accountType)
        const palette = accountPalette(account, index)
        const balance = account.balance ?? account.openingBalance
        const isActive = index === 0
        const cardStyle = { "--stack-index": index, zIndex: visibleAccounts.length - index } as CSSProperties
        return (
          <article key={account.id} role="listitem" className={`mobile-finance-account-card is-${palette}${isActive ? " is-active" : ""}`} style={cardStyle}>
            <button
              type="button"
              className="mobile-finance-account-summary"
              aria-pressed={isActive}
              onClick={() => {
                if (suppressClick.current) { suppressClick.current = false; return }
                if (!isActive) {
                  selectIndex((safeActiveIndex + index) % accounts.length)
                  setEditingField(null)
                  setSaveError(null)
                }
              }}
              title={isActive ? "Selected account" : "Bring account forward"}
            >
              <span className="mobile-finance-account-top">
                <span className="mobile-finance-account-icon"><Icon aria-hidden="true" /></span>
                <span className="mobile-finance-account-meta"><strong>{account.name}</strong><small>{accountLabel(account.accountType)}</small></span>
                <span className="mobile-finance-account-currency">{account.currency}</span>
              </span>
              <span className="mobile-finance-account-bottom">
                <span>{account.nature === "credit" ? "Current due" : "Balance"}</span>
                <strong>{formatMoney(balance, account.currency)}</strong>
                <span className="mobile-finance-account-action" aria-hidden="true"><ChevronRight /></span>
              </span>
            </button>
          </article>
        )
      })}
      </div>
      {accounts.length > 1 && <div className="mobile-finance-stack-controls"><button type="button" className="mobile-finance-stack-control" onClick={() => move(-1)}><ChevronLeft aria-hidden="true" /><span>Previous</span></button><div className="mobile-finance-dots" aria-label="Select account">{accounts.map((account, index) => <button key={account.id} type="button" className={`mobile-finance-dot${index === safeActiveIndex ? " is-active" : ""}`} aria-label={`Select ${account.name}`} aria-pressed={index === safeActiveIndex} onClick={() => selectIndex(index)} />)}</div><button type="button" className="mobile-finance-stack-control" onClick={() => move(1)}><span>Next</span><ChevronRight aria-hidden="true" /></button></div>}
    </div>
    {accounts.length > 1 && <div className="mobile-finance-account-switcher" aria-label="All accounts">{accounts.map((account, index) => <button key={account.id} type="button" className={`mobile-finance-account-chip${index === safeActiveIndex ? " is-active" : ""}`} aria-pressed={index === safeActiveIndex} onClick={() => { selectIndex(index); setEditingField(null); setSaveError(null) }}>{account.name}</button>)}</div>}
    {activeAccount && <AccountOverviewPanel account={activeAccount} overview={overview} overviewState={overviewState} saving={savingId === activeAccount.id} editingField={editingField} error={saveError} onEdit={() => onEdit(activeAccount)} onBeginEdit={(field) => { setEditingField(field); setSaveError(null) }} onCancel={() => { setEditingField(null); setSaveError(null) }} onSave={(field, value) => save(activeAccount, field, value)} />}
  </>
}

function AccountOverviewPanel({ account, overview, overviewState, editingField, saving, error, onEdit, onBeginEdit, onCancel, onSave }: {
  account: FinanceAccount
  overview: FinanceOverview | null
  overviewState: FinanceLoadState
  editingField: EditableAccountField | null
  saving: boolean
  error: string | null
  onEdit: () => void
  onBeginEdit: (field: EditableAccountField) => void
  onCancel: () => void
  onSave: (field: EditableAccountField, value: string) => Promise<void>
}) {
  return <section className="mobile-finance-account-overview" aria-label="Account summary and details">
    <div className="mobile-finance-totals" aria-label="Monthly totals">
      <div><span>Net assets</span><strong>{formatMoney(overview?.netAssets)}</strong><small>{overview ? "Assets " + formatMoney(overview.assetTotal) + " · Credit " + formatMoney(overview.liabilityTotal) : "Unavailable"}</small></div>
      <div><span>This month</span><strong className="is-negative">{formatMoney(overview?.expense)}</strong><small>spent so far</small></div>
    </div>
    <div className="mobile-finance-details">
      <div className="mobile-finance-details-head"><h3>Selected account</h3><button type="button" className="mobile-finance-text-button" onClick={onEdit}><Pencil aria-hidden="true" />Edit account</button></div>
      <AccountExpandedDetails account={account} editingField={editingField} saving={saving} error={error} onBeginEdit={onBeginEdit} onCancel={onCancel} onSave={onSave} />
      {overviewState === "error" && <p className="mobile-finance-inline-note">Monthly totals are unavailable.</p>}
    </div>
  </section>
}

type EditableAccountField = "name" | "accountType" | "openingBalance" | "creditLimit" | "billingDay" | "dueDay" | "notes"

function AccountExpandedDetails({ account, editingField, saving, error, onBeginEdit, onCancel, onSave }: { account: FinanceAccount; editingField: EditableAccountField | null; saving: boolean; error: string | null; onBeginEdit: (field: EditableAccountField) => void; onCancel: () => void; onSave: (field: EditableAccountField, value: string) => Promise<void> }) {
  return <div className="mobile-finance-account-details">
    <p className="mobile-finance-account-details-hint">Tap a value to edit</p>
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

  return <button type="button" className={`mobile-finance-account-data-row${wide ? " is-wide" : ""}`} title={`Edit ${label}`} onClick={() => onBeginEdit(field)}><span>{label}</span><strong>{value}</strong><Pencil aria-hidden="true" /></button>
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

function AccountFormSheet({ mode, account, onClose, onSaved }: { mode: "add" | "edit"; account?: FinanceAccount; onClose: () => void; onSaved: (account: FinanceAccount) => Promise<void> | void }) {
  const portal = useMobilePortal()
  const dialog = useRef<HTMLDialogElement>(null)
  const [accountType, setAccountType] = useState<FinanceAccount["accountType"]>(account?.accountType ?? "bank")
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const title = mode === "add" ? "Add account" : "Edit account"

  useEffect(() => {
    const element = dialog.current
    const trigger = document.activeElement instanceof HTMLElement ? document.activeElement : null
    element?.showModal()
    return () => { element?.close(); trigger?.focus() }
  }, [])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (saving) return
    const form = new FormData(event.currentTarget)
    const name = String(form.get("name") ?? "").trim()
    const openingBalance = parseNonNegative(String(form.get("openingBalance") ?? ""))
    const credit = creditAccountTypes.has(accountType)
    const creditLimit = parseNonNegativeOrNull(String(form.get("creditLimit") ?? ""))
    const billingDay = parseDayOrNull(String(form.get("billingDay") ?? ""))
    const dueDay = parseDayOrNull(String(form.get("dueDay") ?? ""))
    if (!name || openingBalance == null || creditLimit === undefined || billingDay === undefined || dueDay === undefined) {
      setError("Enter valid account details before saving.")
      return
    }
    setSaving(true)
    setError(null)
    try {
      const saved = await saveFinanceAccount({
        ...(account ? { id: account.id } : {}),
        name,
        accountType,
        nature: credit ? "credit" : "asset",
        currency: account?.currency ?? String(form.get("currency") ?? "CNY"),
        openingBalance,
        creditLimit: credit ? creditLimit : null,
        billingDay: credit ? billingDay : null,
        dueDay: credit ? dueDay : null,
        color: account?.color ?? null,
        notes: String(form.get("notes") ?? "").trim() || null,
      })
      await onSaved(saved)
      onClose()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Could not save this account.")
    } finally {
      setSaving(false)
    }
  }

  const content = <dialog ref={dialog} className="mobile-finance-dialog" aria-labelledby="mobile-finance-dialog-title" onCancel={(event) => { event.preventDefault(); if (!saving) onClose() }} onClick={(event) => { if (event.target === event.currentTarget && !saving) onClose() }}>
    <form className="mobile-finance-dialog-inner" onSubmit={(event) => void submit(event)}>
      <div className="mobile-finance-dialog-head"><div><h2 id="mobile-finance-dialog-title">{title}</h2><p>{mode === "add" ? "Set up an account once. Its balance will follow your saved activity." : "Update account details. Current balance remains derived from activity."}</p></div><button type="button" className="mobile-finance-circle-button" aria-label="Close account form" onClick={onClose} disabled={saving}><X aria-hidden="true" /></button></div>
      <label className="mobile-finance-form-field"><span>Account name</span><input required name="name" defaultValue={account?.name ?? ""} maxLength={80} autoComplete="off" /></label>
      <label className="mobile-finance-form-field"><span>Account type</span><select name="accountType" value={accountType} onChange={(event) => setAccountType(event.target.value as FinanceAccount["accountType"])}>{accountTypeOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label>
      <div className="mobile-finance-form-field"><span>Nature</span><div className="mobile-finance-readonly">{creditAccountTypes.has(accountType) ? "Credit account" : "Asset account"} · derived from type</div></div>
      {mode === "add" ? <label className="mobile-finance-form-field"><span>Currency</span><select name="currency" defaultValue="CNY"><option>CNY</option><option>USD</option><option>EUR</option><option>GBP</option><option>JPY</option><option>HKD</option></select></label> : <div className="mobile-finance-form-field"><span>Currency</span><div className="mobile-finance-readonly">{account?.currency ?? "CNY"}</div></div>}
      <label className="mobile-finance-form-field"><span>Opening balance</span><input required name="openingBalance" type="number" min="0" step="0.01" defaultValue={account?.openingBalance ?? 0} inputMode="decimal" /><small>Current balance is updated by activity records.</small></label>
      {creditAccountTypes.has(accountType) && <div className="mobile-finance-editor-grid">
        <label className="mobile-finance-form-field"><span>Credit limit</span><input name="creditLimit" type="number" min="0" step="0.01" defaultValue={account?.creditLimit ?? ""} inputMode="decimal" /></label>
        <label className="mobile-finance-form-field"><span>Billing day</span><input name="billingDay" type="number" min="1" max="31" defaultValue={account?.billingDay ?? ""} inputMode="numeric" /></label>
        <label className="mobile-finance-form-field"><span>Due day</span><input name="dueDay" type="number" min="1" max="31" defaultValue={account?.dueDay ?? ""} inputMode="numeric" /></label>
      </div>}
      <label className="mobile-finance-form-field"><span>Notes</span><textarea name="notes" defaultValue={account?.notes ?? ""} rows={3} /></label>
      {error && <p className="mobile-finance-dialog-error" role="alert">{error}</p>}
      <div className="mobile-finance-dialog-actions"><button type="button" className="mobile-finance-form-cancel" onClick={onClose} disabled={saving}>Cancel</button><button type="submit" className="mobile-finance-form-save" disabled={saving}>{saving ? <LoaderCircle aria-hidden="true" className="is-spinning" /> : <Save aria-hidden="true" />}{saving ? "Saving" : mode === "add" ? "Add account" : "Save changes"}</button></div>
    </form>
  </dialog>
  return portal ? createPortal(content, portal) : content
}

function FinanceEmpty({ onAdd }: { onAdd: () => void }) {
  return <div className="mobile-finance-message"><Wallet aria-hidden="true" /><strong>Start with an account</strong><p>Add an account to see its balance and monthly activity here.</p><button type="button" onClick={onAdd}>Add account</button><Link to="/finance/overview">Open Finance workspace <ArrowUpRight aria-hidden="true" /></Link></div>
}
