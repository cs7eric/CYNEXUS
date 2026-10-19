import { CalendarClock, Check, Plus } from "lucide-react"
import { useEffect, useRef, useState, type FormEvent } from "react"
import { Link } from "react-router-dom"

import { fetchFinanceInstallmentPlans, saveFinanceInstallmentPlan, saveFinanceTransaction, updateFinanceTransaction, type FinanceAccount, type FinanceCategory, type FinanceInstallmentPlan, type FinanceTransaction } from "@/services/finance/ledger"
import { activityTransactionFen, type ActivityMonth } from "../lib/activity"
import { activityDateTimePayload, defaultActivityEntryDate, localActivityDateTime, parseActivityAmount } from "../lib/activity-entry"
import { buildActivityInstallmentTerms } from "../lib/activity-installments"
import { compactCny, decomposeAssets, formatCny } from "../lib/asset-pile"
import { ActivityCash } from "./activity-cash"
import { ActivityCategoryPicker } from "./activity-category-picker"
import { ActivitySheet } from "./activity-sheet"

type EntryType = FinanceTransaction["transactionType"]
type PendingPlan = { purchase: FinanceTransaction; payload: Parameters<typeof saveFinanceInstallmentPlan>[0] }
interface ActivityEntryProps {
  record?: FinanceTransaction
  initialType?: EntryType
  month: ActivityMonth
  accounts: FinanceAccount[]
  categories: FinanceCategory[]
  available: boolean
  loading: boolean
  onClose: () => void
  onSaved: (record: FinanceTransaction, message?: string) => void
  onPosted: () => void
  onOpenInstallments: () => void
}

export function ActivityEntry({ record, initialType = "expense", month, accounts, categories, available, loading, onClose, onSaved, onPosted, onOpenInstallments }: ActivityEntryProps) {
  const activeAccounts = accounts.filter((account) => !account.archived && account.currency.toUpperCase() === "CNY")
  const [type, setType] = useState<EntryType>(record?.transactionType ?? initialType)
  const [amount, setAmount] = useState(record ? record.amount.toFixed(2) : "")
  const [accountId, setAccountId] = useState(String(record?.accountId ?? activeAccounts[0]?.id ?? ""))
  const [toAccountId, setToAccountId] = useState(String(record?.toAccountId ?? activeAccounts.find((account) => account.id !== (record?.accountId ?? activeAccounts[0]?.id))?.id ?? ""))
  const [categoryId, setCategoryId] = useState(String(record?.categoryId ?? categories.find((category) => !category.archived && (category.direction === initialType || category.direction === "both"))?.id ?? ""))
  const [occurredAt, setOccurredAt] = useState(record ? localActivityDateTime(new Date(record.occurredAt)) : defaultActivityEntryDate(month))
  const [merchant, setMerchant] = useState(record?.merchant ?? "")
  const [note, setNote] = useState(record?.note ?? "")
  const [showNote, setShowNote] = useState(Boolean(record?.note))
  const [installments, setInstallments] = useState(false)
  const [countText, setCountText] = useState("6")
  const [feeText, setFeeText] = useState("0.00")
  const [firstDueDate, setFirstDueDate] = useState("")
  const [intervalText, setIntervalText] = useState("1")
  const [pendingPlan, setPendingPlan] = useState<PendingPlan | null>(null)
  const pendingRef = useRef<PendingPlan | null>(null)
  const [linkedPlan, setLinkedPlan] = useState<FinanceInstallmentPlan | null>(null)
  const [planLookup, setPlanLookup] = useState<"loading" | "ready" | "error">("loading")
  const [lookupRefresh, setLookupRefresh] = useState(0)
  const [error, setError] = useState("")
  const [saving, setSaving] = useState(false)
  const savingRef = useRef(false)
  const errorRef = useRef<HTMLParagraphElement>(null)
  const fen = parseActivityAmount(amount)
  const compatibleCategories = categories.filter((category) => !category.archived && (category.direction === type || category.direction === "both"))
  const selectedCategory = compatibleCategories.some((category) => String(category.id) === categoryId) ? categoryId : ""
  const creditAccounts = activeAccounts.filter((account) => account.nature === "credit")
  const entryAccounts = installments ? creditAccounts : activeAccounts
  const terms = buildActivityInstallmentTerms({ totalFen: fen ?? 0, feeText, countText, firstDueDate, intervalText })
  const { groups, loose } = decomposeAssets(fen ?? 0)
  const checkExistingPlan = !!record && record.transactionType === "expense" && accounts.some((account) => account.id === record.accountId && account.nature === "credit")
  const canEdit = (!record || record.currency.toUpperCase() === "CNY" && !record.archived) && (!checkExistingPlan || planLookup === "ready")
  const baseLocked = saving || !canEdit || !available || !!pendingPlan || installments && !!record

  useEffect(() => {
    if (!record || !checkExistingPlan) return
    let active = true
    const timer = window.setTimeout(() => {
      setPlanLookup("loading")
      void fetchFinanceInstallmentPlans(record.accountId).then((plans) => {
        if (!active) return
        setLinkedPlan(plans.find((plan) => plan.purchaseTransactionId === record.id) ?? null)
        setPlanLookup("ready")
      }).catch(() => { if (active) setPlanLookup("error") })
    }, 0)
    return () => { active = false; window.clearTimeout(timer) }
  }, [record, checkExistingPlan, lookupRefresh])

  function changeType(next: EntryType) {
    setType(next); setInstallments(false)
    setCategoryId(String(categories.find((category) => !category.archived && (category.direction === next || category.direction === "both"))?.id ?? ""))
    setError("")
  }
  function changePaymentMode(value: boolean) {
    setInstallments(value); setError("")
    if (record && value) {
      setAmount(record.amount.toFixed(2)); setAccountId(String(record.accountId)); setCategoryId(String(record.categoryId ?? ""))
      setOccurredAt(localActivityDateTime(new Date(record.occurredAt))); setMerchant(record.merchant ?? ""); setNote(record.note ?? "")
    } else if (value && !creditAccounts.some((account) => String(account.id) === accountId)) setAccountId(String(creditAccounts[0]?.id ?? ""))
  }
  function fail(message: string) { setError(message); requestAnimationFrame(() => errorRef.current?.focus()) }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (savingRef.current) return
    if (!pendingRef.current) {
      if (!available) return fail("Your accounts and categories must be available before saving. Close this sheet and refresh to try again.")
      if (!canEdit || linkedPlan) return fail("This record cannot be edited here. View its installment plan instead.")
      if (!fen) return fail("Enter an amount greater than zero, with up to two decimal places.")
      const account = entryAccounts.find((item) => String(item.id) === accountId)
      if (!account) return fail(installments ? "Choose an active CNY credit account for installments." : "Choose an active CNY account.")
      const destination = activeAccounts.find((item) => String(item.id) === toAccountId)
      if (type === "transfer" && (!destination || destination.id === account.id)) return fail("Choose two different active CNY accounts.")
      if (!(installments && record)) {
        if (type !== "transfer" && !selectedCategory) return fail("Choose a category for this record.")
        if (!activityDateTimePayload(occurredAt)) return fail("Choose a valid date and time.")
      }
      if (installments && !terms.value) return fail(terms.error ?? "Check your installment details.")
    }
    savingRef.current = true
    setSaving(true); setError("")
    try {
      if (installments || pendingRef.current) {
        if (!pendingRef.current) {
          // Keep the posted expense so retries only create the linked plan.
          const purchase = record ?? await saveFinanceTransaction(transactionPayload())
          const value = terms.value!
          const pending: PendingPlan = { purchase, payload: {
            accountId: purchase.accountId, purchaseTransactionId: purchase.id, totalAmount: purchase.amount,
            merchant: purchase.merchant ?? undefined, note: purchase.note ?? undefined,
            feeTotal: value.feeFen / 100, installmentCount: value.count, firstDueDate: value.firstDueDate, intervalMonths: value.intervalMonths,
          } }
          pendingRef.current = pending; setPendingPlan(pending)
          if (!record) onPosted()
        }
        await saveFinanceInstallmentPlan(pendingRef.current.payload)
        onSaved(pendingRef.current.purchase, "Installment plan saved.")
      } else {
        const saved = record ? await updateFinanceTransaction(record.id, transactionPayload()) : await saveFinanceTransaction(transactionPayload())
        onSaved(saved)
      }
    } catch {
      fail(pendingRef.current ? "Your expense is saved. The installment plan could not be created. Retry the plan to finish; another expense will not be added." : "Could not save this record. Your entry is still here; please try again.")
    } finally { savingRef.current = false; setSaving(false) }
  }

  function transactionPayload(): Partial<FinanceTransaction> {
    return {
      transactionType: type, amount: fen! / 100, currency: "CNY", accountId: Number(accountId),
      toAccountId: type === "transfer" ? Number(toAccountId) : null, categoryId: type === "transfer" ? null : Number(selectedCategory),
      occurredAt: record && occurredAt === localActivityDateTime(new Date(record.occurredAt)) ? record.occurredAt : activityDateTimePayload(occurredAt)!,
      merchant: merchant.trim() || null, note: note.trim() || null,
    }
  }

  if (linkedPlan) return <ActivitySheet title="Installment purchase" onClose={onClose}>
    <p className="fa-detail-amount">{formatCny(activityTransactionFen({ amount: linkedPlan.totalAmount }) ?? NaN)}</p>
    <p className="fa-message">{linkedPlan.merchant || "Purchase"} · {linkedPlan.installmentCount} payments</p>
    <p className="fa-message">This expense is linked to a repayment plan. View its schedule and repayments to manage it.</p>
    <button type="button" className="fa-primary" onClick={onOpenInstallments}><CalendarClock aria-hidden="true" />View installment plans</button>
  </ActivitySheet>

  return <ActivitySheet title={record ? "Edit record" : "New record"} onClose={onClose} busy={saving}>
    {!activeAccounts.length && !pendingPlan ? <div className="fa-empty" role="status"><h3>{loading ? "Loading your accounts…" : !available ? "Entry unavailable" : "Add a CNY account first"}</h3><p>{!available ? "Your accounts and categories must be available before recording a transaction." : "Choose an account for your income, expenses and transfers."}</p><Link to="#finance-accounts" className="fa-text-button" onClick={onClose}>View accounts</Link></div> :
    <form className="fa-entry-form" onSubmit={submit} noValidate aria-busy={saving}>
      {!available && !pendingPlan && <p className="fa-message" role="status">Accounts or categories unavailable. Your entry is preserved; close this sheet and refresh before saving.</p>}
      {checkExistingPlan && planLookup !== "ready" && <div className="fa-alert" role="status"><p>{planLookup === "loading" ? "Checking installment details…" : "Installment details unavailable. Refresh them before editing this expense."}</p>{planLookup === "error" && <button type="button" className="fa-text-button" onClick={() => setLookupRefresh((value) => value + 1)}>Try again</button>}</div>}
      <fieldset disabled={saving || !canEdit || !available || !!pendingPlan}>
        <div className="fa-type-switch" role="group" aria-label="Record type">{(["expense", "income", "transfer"] as const).map((value) => <button key={value} type="button" disabled={installments && !!record && value !== "expense"} aria-pressed={type === value} onClick={() => changeType(value)}>{value[0].toUpperCase() + value.slice(1)}</button>)}</div>
        <div className="fa-amount-entry"><label htmlFor="fa-entry-amount">CNY</label><input id="fa-entry-amount" type="text" inputMode="decimal" autoComplete="off" placeholder="0.00" readOnly={installments && !!record} value={amount} aria-describedby="fa-entry-composition fa-entry-error" onChange={(event) => { setAmount(event.target.value); setError("") }} /></div>
        {fen !== null && <ActivityCash amount={fen} size="entry" />}
        <p className={`fa-denominations${fen === null ? " is-empty" : ""}`} id="fa-entry-composition">{fen ? [...groups, ...loose].map((group) => `${group.count} × ${compactCny(group.valueInFen)}`).join(" · ") : "Enter an amount to see it in cash."}</p>
        {type === "expense" && (!record || record.transactionType === "expense" && accounts.some((account) => account.id === record.accountId && account.nature === "credit")) && <div className="fa-payment-mode"><span className="fa-field-label">Payment</span><div role="group" aria-label="Payment mode"><button type="button" aria-pressed={!installments} onClick={() => changePaymentMode(false)}>One payment</button><button type="button" aria-pressed={installments} onClick={() => changePaymentMode(true)}><CalendarClock aria-hidden="true" />Installments</button></div></div>}
      </fieldset>
      <fieldset disabled={baseLocked}>
        <label className="fa-field"><span>{installments ? "Credit account" : type === "transfer" ? "From account" : "Account"}</span><select aria-label={installments ? "Credit account" : type === "transfer" ? "From account" : "Account"} value={accountId} onChange={(event) => setAccountId(event.target.value)}><option value="">Choose account</option>{entryAccounts.map((account) => <option value={account.id} key={account.id}>{account.name}</option>)}</select></label>
        {installments && !creditAccounts.length && <p className="fa-message">Installments need an active CNY credit account. <Link to="#finance-accounts" onClick={onClose}>View accounts</Link>.</p>}
        {type !== "transfer" && <ActivityCategoryPicker categories={compatibleCategories} value={selectedCategory} onChange={setCategoryId} />}
        {type === "transfer" && <label className="fa-field"><span>To account</span><select aria-label="To account" value={toAccountId} onChange={(event) => setToAccountId(event.target.value)}><option value="">Choose account</option>{activeAccounts.map((account) => <option value={account.id} key={account.id}>{account.name}</option>)}</select></label>}
        <label className="fa-field"><span>Date &amp; time</span><input type="datetime-local" aria-label="Date and time" value={occurredAt} onChange={(event) => setOccurredAt(event.target.value)} /></label>
        <label className="fa-field"><span>{type === "income" ? "Source / title" : "Merchant / title"}</span><input type="text" maxLength={80} placeholder={type === "income" ? "e.g. Monthly salary" : type === "transfer" ? "e.g. Credit card repayment" : "e.g. Lunch at the corner café"} value={merchant} onChange={(event) => setMerchant(event.target.value)} /></label>
        <button type="button" className="fa-notes-toggle" aria-expanded={showNote} onClick={() => setShowNote(!showNote)}><Plus aria-hidden="true" />{showNote ? "Hide note" : "Add a note"}</button>
        {showNote && <label className="fa-field"><span>Note</span><textarea maxLength={300} placeholder="Anything you want to remember" value={note} onChange={(event) => setNote(event.target.value)} /></label>}
      </fieldset>
      {installments && <fieldset className="fa-installment-fields" disabled={saving || !available || !!pendingPlan}>
        <div className="fa-installment-heading"><CalendarClock aria-hidden="true" /><h3>Repayment plan</h3></div>
        <div className="fa-form-row"><label className="fa-field"><span>Number of payments</span><input aria-label="Number of payments" type="number" inputMode="numeric" min={2} max={60} step={1} value={countText} onChange={(event) => { setCountText(event.target.value); setError("") }} /></label><label className="fa-field"><span>Total fees · CNY</span><input aria-label="Total fees" inputMode="decimal" placeholder="0.00" value={feeText} onChange={(event) => { setFeeText(event.target.value); setError("") }} /></label></div>
        <label className="fa-field"><span>First due date</span><input aria-label="First due date" type="date" value={firstDueDate} onChange={(event) => { setFirstDueDate(event.target.value); setError("") }} /></label>
        <label className="fa-field"><span>Payment frequency</span><select aria-label="Payment frequency" value={intervalText} onChange={(event) => { setIntervalText(event.target.value); setError("") }}>{Array.from({ length: 12 }, (_, index) => <option key={index + 1} value={index + 1}>{index === 0 ? "Monthly" : `Every ${index + 1} months`}</option>)}</select></label>
        {terms.value ? <div className="fa-installment-preview"><strong>First payment {formatCny(terms.value.schedule[0].amountFen)}</strong><span>{terms.value.count} payments · Last payment {formatCny(terms.value.schedule.at(-1)!.amountFen)}</span><details><summary>Preview repayment schedule</summary><ol>{terms.value.schedule.map((payment, index) => <li key={index}><span>{index + 1}. {payment.dueDate}</span><strong>{formatCny(payment.amountFen)}</strong></li>)}</ol></details></div> : <p className="fa-message">{!firstDueDate ? "Choose a first due date to preview the repayment schedule." : terms.error}</p>}
        <p className="fa-message">{record ? "This expense is already recorded. Only a repayment plan will be added." : "The full purchase is recorded once. Repayments are transfers, so the purchase is not counted twice."}</p>
      </fieldset>}
      <div className="fa-entry-submit">
        {pendingPlan && <p className="fa-message" role="status">Expense saved · installment setup pending. Retry to finish, or close and set up installments from this expense later.</p>}
        <p ref={errorRef} tabIndex={-1} id="fa-entry-error" className="fa-form-error" role={error ? "alert" : undefined}>{error}</p>
        <button type="submit" className="fa-primary" disabled={saving || !pendingPlan && (!canEdit || !available)}><Check aria-hidden="true" />{saving ? "Saving…" : pendingPlan ? "Retry installment plan" : installments ? record ? "Create installment plan" : "Add installment purchase" : record ? "Save changes" : "Add record"}</button>
        {!installments && <p className="fa-footnote">{type === "transfer" ? "Transfers and credit repayments are excluded from income and expenses." : "Saving updates your account balance and monthly activity."}</p>}
      </div>
    </form>}
  </ActivitySheet>
}
