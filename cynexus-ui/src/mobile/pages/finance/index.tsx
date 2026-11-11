import { ArrowUpRight, Banknote, ChevronLeft, ChevronRight, CreditCard, Landmark, LoaderCircle, Pencil, Save, TrendingUp, Wallet, WalletCards, X } from "lucide-react"
import { Button } from "@heroui/react/button"
import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties, type PointerEvent } from "react"
import { Link, useLocation, useOutletContext } from "react-router-dom"

import { useWorkspace } from "@/layout/workspace-provider"
import { fetchFinanceAccounts, fetchFinanceOverview, saveFinanceAccount, type FinanceAccount, type FinanceOverview } from "@/services/finance/ledger"
import "@/mobile/pages/finance/finance.css"
import "@/mobile/pages/finance/account-fit.css"
import { FinanceOverviewView } from "./overview"
import { FinanceActivityView } from "./activity"
import { FinanceSummaryView } from "./summary"
import { AccountFormSheet } from "./components/account-form"
import { AccountRepaymentSheet } from "./components/account-repayment"
import { ActivityInstallments } from "./components/activity-installments"
import { useAccountActivity } from "./hooks/use-account-activity"
import { rankAccountsByRecentActivity } from "./lib/account-activity"
import { accountRepaymentDueFen } from "./lib/account-repayment"

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
  const { workspaceId } = useWorkspace()
  const fixedFinancePage = hash === "#finance-summary" || hash === "#finance-activity"

  useEffect(() => {
    const content = document.getElementById("mobile-content")
    if (!content) return
    content.classList.toggle("is-finance-fixed", fixedFinancePage)
    return () => content.classList.remove("is-finance-fixed")
  }, [fixedFinancePage])

  if (hash === "#finance-accounts") return <FinanceAccountsView key={workspaceId} />
  if (hash === "#finance-activity") return <FinanceActivityView />
  if (hash === "#finance-summary") return <FinanceSummaryView />
  return <FinanceOverviewView />
}

function FinanceAccountsView() {
  const { workspaceId } = useWorkspace()
  const { financeRefreshToken } = useOutletContext<{ financeRefreshToken: number }>()
  const accountsPage = useLocation().hash === "#finance-accounts"
  const [accountsState, setAccountsState] = useState<FinanceLoadState>("loading")
  const [overviewState, setOverviewState] = useState<FinanceLoadState>("loading")
  const [accounts, setAccounts] = useState<FinanceAccount[]>([])
  const [overview, setOverview] = useState<FinanceOverview | null>(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [accountForm, setAccountForm] = useState<{ mode: "add" | "edit"; account?: FinanceAccount } | null>(null)
  const [focusAccountId, setFocusAccountId] = useState<number | null>(null)
  const [repaymentAccount, setRepaymentAccount] = useState<FinanceAccount | null>(null)
  const [installmentAccountId, setInstallmentAccountId] = useState<number | null>(null)
  const [notice, setNotice] = useState("")
  const requestId = useRef(0)
  const activity = useAccountActivity({ workspaceId, refreshToken: reloadKey + financeRefreshToken })
  const bounds = useMemo(() => monthBounds(), [])

  useEffect(() => {
    const openAccountForm = () => setAccountForm((current) => current ?? { mode: "add" })
    window.addEventListener("mobile-finance-account-add", openAccountForm)
    return () => window.removeEventListener("mobile-finance-account-add", openAccountForm)
  }, [])

  const load = useCallback(async () => {
    const request = ++requestId.current
    setAccountsState("loading")
    setOverviewState("loading")
    const [accountsResult, overviewResult] = await Promise.allSettled([
      fetchFinanceAccounts(false),
      fetchFinanceOverview(bounds),
    ])
    if (request !== requestId.current) return false
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
    return accountsResult.status === "fulfilled" && overviewResult.status === "fulfilled"
  }, [bounds])
  const invalidateRequests = useCallback(() => { requestId.current++ }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => { void load() }, 0)
    return () => { window.clearTimeout(timer); invalidateRequests() }
  }, [load, reloadKey, workspaceId, financeRefreshToken, invalidateRequests])

  useEffect(() => {
    const content = document.getElementById("mobile-content")
    if (!content) return
    content.classList.toggle("is-finance-accounts", accountsPage)
    return () => content.classList.remove("is-finance-accounts")
  }, [accountsPage])

  const currency = "CNY"
  const sourceAccounts = accountsState === "ready" ? accounts : accounts.length ? accounts : overview?.accounts.filter((account) => !account.archived) ?? []
  const ranked = activity.snapshot?.complete ? rankAccountsByRecentActivity(sourceAccounts, activity.snapshot.transactions, activity.snapshot.range) : null
  const displayAccounts = ranked?.map((item) => item.account) ?? sourceAccounts
  const loading = accountsState === "loading" || overviewState === "loading"
  const unavailable = accountsState === "error" && overviewState === "error"
  const hasData = displayAccounts.length > 0 || Boolean(overview && (overview.income !== 0 || overview.expense !== 0 || overview.assetTotal !== 0 || overview.liabilityTotal !== 0 || overview.recentTransactions.length > 0))

  return (
    <div id="finance-accounts" className={`mobile-finance${accountsPage ? " is-accounts-page" : ""}`} aria-busy={loading}>
      <div className="mobile-finance-account-order" role="status">
        <span className="mobile-finance-account-order-copy">{activity.snapshot?.complete ? activity.stale ? "Most used · Last available activity" : "Most used · Last 30 days" : activity.status === "loading" ? "Loading recent activity…" : "Recent activity unavailable · Showing account order"}</span>
        <span className="mobile-finance-account-count">{displayAccounts.length} {displayAccounts.length === 1 ? "account" : "accounts"}</span>
        {(activity.status === "error" || activity.snapshot && !activity.snapshot.complete) && <Button type="button" size="sm" variant="ghost" onPress={activity.refresh}>Retry</Button>}
      </div>
      {notice && <p className="mobile-finance-account-notice" role="status">{notice}</p>}

      {loading && !hasData && <FinanceLoading />}
      {unavailable && <FinanceError onRetry={() => setReloadKey((value) => value + 1)} />}
      {!loading && !unavailable && !hasData && <FinanceEmpty onAdd={() => setAccountForm({ mode: "add" })} />}
      {!unavailable && hasData && (
        <>
          <section className="mobile-finance-accounts" aria-label="Accounts">
            {accountsState === "error" ? <div className="mobile-finance-inline-empty">Accounts are unavailable</div> : displayAccounts.length ? <AccountStack accounts={displayAccounts} overview={overview} overviewState={overviewState} available={accountsState === "ready"} focusAccountId={focusAccountId} onFocusHandled={() => setFocusAccountId(null)} onRepay={setRepaymentAccount} onEdit={(account) => setAccountForm({ mode: "edit", account })} onSaved={async (saved) => {
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
      {accountForm && <AccountFormSheet mode={accountForm.mode} account={accountForm.account} onClose={() => setAccountForm(null)} onSaved={async (saved) => { setFocusAccountId(saved.id); await load() }} />}
      {repaymentAccount && <AccountRepaymentSheet key={repaymentAccount.id} account={accounts.find((account) => account.id === repaymentAccount.id) ?? repaymentAccount}
        accounts={accounts} available={accountsState === "ready" && accounts.some((account) => account.id === repaymentAccount.id)} onClose={() => setRepaymentAccount(null)} onOpenInstallments={() => { setInstallmentAccountId(repaymentAccount.id); setRepaymentAccount(null) }}
        onSaved={async (transaction) => {
          setRepaymentAccount(null); setFocusAccountId(transaction.toAccountId); setNotice("Repayment saved.")
          activity.refresh()
          if (!await load()) setNotice("Repayment saved. Some balances could not refresh; use Refresh to update them.")
        }} />}
      {installmentAccountId !== null && <ActivityInstallments accounts={displayAccounts.filter((account) => account.nature === "asset" || account.id === installmentAccountId)}
        onClose={() => setInstallmentAccountId(null)} onChanged={async () => { activity.refresh(); await load() }} />}
    </div>
  )
}

function AccountStack({ accounts, overview, overviewState, available, focusAccountId, onFocusHandled, onEdit, onRepay, onSaved }: { accounts: FinanceAccount[]; overview: FinanceOverview | null; overviewState: FinanceLoadState; available: boolean; focusAccountId: number | null; onFocusHandled: () => void; onEdit: (account: FinanceAccount) => void; onRepay: (account: FinanceAccount) => void; onSaved: (account: FinanceAccount) => Promise<void> | void }) {
  const [activeId, setActiveId] = useState<number | null>(null)
  const [balanceVisible, setBalanceVisible] = useState(false)
  const [editingField, setEditingField] = useState<EditableAccountField | null>(null)
  const [savingId, setSavingId] = useState<number | null>(null)
  const [saveError, setSaveError] = useState<string | null>(null)
  const gesture = useRef<{ startX: number; startY: number; moved: boolean } | null>(null)
  const suppressClick = useRef(false)

  const focusIndex = focusAccountId == null ? -1 : accounts.findIndex((account) => account.id === focusAccountId)
  const selectedIndex = accounts.findIndex((account) => account.id === activeId)
  const safeActiveIndex = Math.max(0, focusIndex >= 0 ? focusIndex : selectedIndex)
  const orderedAccounts = accounts.length ? [...accounts.slice(safeActiveIndex), ...accounts.slice(0, safeActiveIndex)] : []
  const visibleAccounts = orderedAccounts.slice(0, Math.min(4, orderedAccounts.length))
  const activeAccount = accounts[safeActiveIndex]

  function selectIndex(index: number) {
    onFocusHandled()
    setActiveId(accounts[index]?.id ?? null)
  }

  function move(direction: 1 | -1) {
    if (accounts.length < 2) return
    onFocusHandled()
    setActiveId(accounts[(safeActiveIndex + direction + accounts.length) % accounts.length].id)
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
        style={{ minHeight: `${160 + Math.max(0, visibleAccounts.length - 1) * 18}px` }}
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
                <strong>
                  <button
                    type="button"
                    className="mobile-finance-balance-toggle"
                    aria-label={balanceVisible ? "Hide account balance" : "Show account balance"}
                    aria-pressed={balanceVisible}
                    onClick={(event) => { event.stopPropagation(); setBalanceVisible((visible) => !visible) }}
                  >
                    {balanceVisible ? formatMoney(balance, account.currency) : "••••••"}
                  </button>
                </strong>
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
    {activeAccount && <AccountOverviewPanel key={activeAccount.id} account={activeAccount} overview={overview} overviewState={overviewState} available={available} saving={savingId === activeAccount.id} editingField={editingField} error={saveError} onEdit={() => onEdit(activeAccount)} onRepay={() => onRepay(activeAccount)} onBeginEdit={(field) => { setEditingField(field); setSaveError(null) }} onCancel={() => { setEditingField(null); setSaveError(null) }} onSave={(field, value) => save(activeAccount, field, value)} />}
  </>
}

function AccountOverviewPanel({ account, overview, overviewState, available, editingField, saving, error, onEdit, onRepay, onBeginEdit, onCancel, onSave }: {
  account: FinanceAccount
  overview: FinanceOverview | null
  overviewState: FinanceLoadState
  available: boolean
  editingField: EditableAccountField | null
  saving: boolean
  error: string | null
  onEdit: () => void
  onRepay: () => void
  onBeginEdit: (field: EditableAccountField) => void
  onCancel: () => void
  onSave: (field: EditableAccountField, value: string) => Promise<void>
}) {
  return <section className="mobile-finance-account-overview" aria-label="Account summary and details">
    <div className="mobile-finance-totals" aria-label="Monthly totals">
      <div><span>Net assets</span><strong>{formatMoney(overview?.netAssets)}</strong><small>{overview ? "Assets " + formatMoney(overview.assetTotal) + " · Credit " + formatMoney(overview.liabilityTotal) : "Unavailable"}</small></div>
      <div><span>This month</span><strong className="is-negative">{formatMoney(overview?.expense)}</strong><small>spent so far</small></div>
    </div>
    {account.nature === "credit" && <div className="mobile-finance-credit-action"><div><span>Current due</span><strong>{formatMoney(account.balance, account.currency)}</strong></div>
      <Button type="button" variant="primary" isDisabled={!available || saving || accountRepaymentDueFen(account) === null || accountRepaymentDueFen(account) === 0} onPress={onRepay}>
        <CreditCard aria-hidden="true" />{accountRepaymentDueFen(account) === 0 ? "Nothing due" : "Repay"}
      </Button></div>}
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

function FinanceEmpty({ onAdd }: { onAdd: () => void }) {
  return <div className="mobile-finance-message"><Wallet aria-hidden="true" /><strong>Start with an account</strong><p>Add an account to see its balance and monthly activity here.</p><button type="button" onClick={onAdd}>Add account</button><Link to="/finance/overview">Open Finance workspace <ArrowUpRight aria-hidden="true" /></Link></div>
}
