import { ArrowDownLeft, ArrowLeftRight, ArrowUpRight, CalendarDays, CalendarClock, Check, CreditCard, ListTree, Plus, Store, Wallet } from "lucide-react"
import { Button, ButtonGroup } from "@heroui/react"
import { useEffect, useRef, useState, type FormEvent } from "react"
import { Link } from "react-router-dom"

import { fetchFinanceInstallmentPlans, saveFinanceInstallmentPlan, saveFinanceTransaction, updateFinanceTransaction, type FinanceAccount, type FinanceCategory, type FinanceInstallmentPlan, type FinanceTransaction } from "@/services/finance/ledger"
import { canUseFinanceAccountForTransaction } from "@/services/finance/account-eligibility"
import { activityTransactionFen, type ActivityMonth } from "../lib/activity"
import { activityDateTimePayload, defaultActivityEntryDate, localActivityDateTime, parseActivityAmount } from "../lib/activity-entry"
import { buildActivityInstallmentTerms } from "../lib/activity-installments"
import { formatCny } from "../lib/asset-pile"
import { ActivityEntryChoice } from "./activity-entry-choice"
import { ActivityEntryPlan } from "./activity-entry-plan"
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
  const initialAccountId = record?.accountId ?? activeAccounts.find((account) => canUseFinanceAccountForTransaction(account, initialType))?.id
  const [type, setType] = useState<EntryType>(record?.transactionType ?? initialType)
  const [amount, setAmount] = useState(record ? record.amount.toFixed(2) : "")
  const [accountId, setAccountId] = useState(String(initialAccountId ?? ""))
  const [toAccountId, setToAccountId] = useState(String(record?.toAccountId ?? activeAccounts.find((account) => account.id !== initialAccountId)?.id ?? ""))
  const [categoryId, setCategoryId] = useState(String(record?.categoryId ?? categories.find((category) => !category.archived && (category.direction === initialType || category.direction === "both"))?.id ?? ""))
  const [occurredAt, setOccurredAt] = useState(record ? localActivityDateTime(new Date(record.occurredAt)) : defaultActivityEntryDate(month))
  const [merchant, setMerchant] = useState(record?.merchant ?? "")
  const [note, setNote] = useState(record?.note ?? "")
  const [showNote, setShowNote] = useState(Boolean(record?.note))
  const [installments, setInstallments] = useState(false)
  const [countText, setCountText] = useState("12")
  const [feeText, setFeeText] = useState("0.00")
  const [firstDueDate, setFirstDueDate] = useState("")
  const intervalText = "1"
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
  const entryAccounts = installments ? creditAccounts : activeAccounts.filter((account) => canUseFinanceAccountForTransaction(account, type))
  const terms = buildActivityInstallmentTerms({ totalFen: fen ?? 0, feeText, countText, firstDueDate, intervalText })
  const checkExistingPlan = !!record && record.transactionType === "expense" && accounts.some((account) => account.id === record.accountId && account.nature === "credit")
  const canEdit = (!record || record.currency.toUpperCase() === "CNY" && !record.archived) && (!checkExistingPlan || planLookup === "ready")
  const baseLocked = saving || !canEdit || !available || !!pendingPlan || installments && !!record
  const controlsLocked = saving || !canEdit || !available || !!pendingPlan
  const planLocked = saving || !canEdit || !available || !!pendingPlan
  const previewTerms = buildActivityInstallmentTerms({ totalFen: fen ?? 0, feeText, countText, firstDueDate: firstDueDate || occurredAt.slice(0, 10), intervalText }).value
  const categoryOptions = compatibleCategories.map((category) => {
    const path: string[] = []
    const visited = new Set([category.id])
    let parentId = category.parentId
    while (parentId != null && !visited.has(parentId)) {
      visited.add(parentId)
      const parent = categories.find((item) => item.id === parentId)
      if (!parent) break
      path.unshift(parent.name)
      parentId = parent.parentId
    }
    return { id: String(category.id), label: category.name, description: path.join(" / ") }
  })

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
    const nextAccounts = activeAccounts.filter((account) => canUseFinanceAccountForTransaction(account, next))
    const nextAccountId = nextAccounts.some((account) => String(account.id) === accountId) ? accountId : String(nextAccounts[0]?.id ?? "")
    setAccountId(nextAccountId)
    if (next === "transfer" && (!activeAccounts.some((account) => String(account.id) === toAccountId) || toAccountId === nextAccountId)) {
      setToAccountId(String(activeAccounts.find((account) => String(account.id) !== nextAccountId)?.id ?? ""))
    }
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
      if (!canUseFinanceAccountForTransaction(account, type)) return fail("Investment accounts only support transfers.")
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

  return <ActivitySheet title={record ? "Edit record" : "New record"} onClose={onClose} busy={saving} className="fa-entry-sheet">
    {!activeAccounts.length && !pendingPlan ? <div className="fa-empty" role="status">
      <h3>{loading ? "Loading your accounts…" : !available ? "Entry unavailable" : "Add a CNY account first"}</h3>
      <p>{!available ? "Your accounts and categories must be available before recording a transaction." : "Choose an account for your income, expenses and transfers."}</p>
      <Link to="#finance-accounts" className="fa-text-button" onClick={onClose}>View accounts</Link>
    </div> : <form className="fa-entry-form" onSubmit={submit} noValidate aria-busy={saving}>
      <div className="fa-entry-scroll">
        {!available && !pendingPlan && <p className="fa-message" role="status">Accounts or categories unavailable. Your entry is preserved; close this sheet and refresh before saving.</p>}
        {checkExistingPlan && planLookup !== "ready" && <div className="fa-alert" role="status">
          <p>{planLookup === "loading" ? "Checking installment details…" : "Installment details unavailable. Refresh them before editing this expense."}</p>
          {planLookup === "error" && <Button type="button" variant="ghost" onPress={() => setLookupRefresh((value) => value + 1)}>Try again</Button>}
        </div>}
        <fieldset disabled={controlsLocked}>
          <ButtonGroup className="fa-entry-types" variant="tertiary" aria-label="Record type">
            {(["expense", "income", "transfer"] as const).map((value) => {
              const Icon = value === "expense" ? ArrowUpRight : value === "income" ? ArrowDownLeft : ArrowLeftRight
              return <Button key={value} type="button" variant={type === value ? "primary" : "tertiary"}
                isDisabled={controlsLocked || installments && !!record && value !== "expense"}
                aria-pressed={type === value} onPress={() => changeType(value)}>
                <Icon aria-hidden="true" />{value[0].toUpperCase() + value.slice(1)}
              </Button>
            })}
          </ButtonGroup>
          <div className="fa-entry-amount-block">
            <div className="fa-entry-amount-heading"><label htmlFor="fa-entry-amount">Amount</label><span>CNY</span></div>
            <div className="fa-entry-amount-line"><span aria-hidden="true">¥</span><input id="fa-entry-amount" type="text"
              inputMode="decimal" autoComplete="off" placeholder="0.00" aria-label="Amount in Chinese yuan"
              readOnly={installments && !!record} value={amount} aria-describedby="fa-entry-error"
              onChange={(event) => { setAmount(event.target.value); setError("") }} /></div>
          </div>
        </fieldset>
        <fieldset disabled={baseLocked}>
          <div className="fa-entry-paired">
            {type !== "transfer" && <ActivityEntryChoice label="Category" icon={<ListTree aria-hidden="true" />} disabled={baseLocked}
              value={selectedCategory} options={categoryOptions} searchable onChange={(value) => { setCategoryId(value); setError("") }} />}
            <ActivityEntryChoice label={type === "transfer" ? "From" : "Account"} icon={<Wallet aria-hidden="true" />} disabled={baseLocked}
              value={accountId} options={entryAccounts.map((account) => ({ id: String(account.id), label: account.name }))}
              onChange={(value) => {
                setAccountId(value); setError("")
                if (value === toAccountId) setToAccountId(String(activeAccounts.find((account) => String(account.id) !== value)?.id ?? ""))
              }} />
            {type === "transfer" && <ActivityEntryChoice label="To" icon={<ArrowLeftRight aria-hidden="true" />} disabled={baseLocked}
              value={toAccountId} options={activeAccounts.map((account) => ({ id: String(account.id), label: account.name, disabled: String(account.id) === accountId }))}
              onChange={(value) => { setToAccountId(value); setError("") }} />}
          </div>
          {type !== "transfer" && !compatibleCategories.length && <p className="fa-message">No categories available. Add one in desktop Finance, or choose another record type.</p>}
          {type !== "transfer" && activeAccounts.some((account) => account.accountType === "investment") && <p className="fa-entry-account-hint">Investment accounts only support transfers.</p>}
          {installments && !creditAccounts.length && <p className="fa-message">Installments need an active CNY credit account. <Link to="#finance-accounts" onClick={onClose}>View accounts</Link>.</p>}
        </fieldset>
        {type === "expense" && (!record || record.transactionType === "expense" && accounts.some((account) => account.id === record.accountId && account.nature === "credit")) && <fieldset disabled={controlsLocked}>
          <div className="fa-entry-row fa-entry-payment"><CreditCard aria-hidden="true" /><span>Payment</span>
            <ButtonGroup variant="tertiary" aria-label="Payment mode" isDisabled={controlsLocked}>
              <Button type="button" variant={installments ? "ghost" : "tertiary"} aria-pressed={!installments} onPress={() => changePaymentMode(false)}>One payment</Button>
              <Button type="button" variant={installments ? "tertiary" : "ghost"} aria-pressed={installments} onPress={() => changePaymentMode(true)}>Installments</Button>
            </ButtonGroup>
          </div>
        </fieldset>}
        <fieldset disabled={baseLocked}>
          <label className="fa-entry-row"><CalendarDays aria-hidden="true" /><span>Date &amp; time</span><input type="datetime-local" aria-label="Date and time"
            min="0001-01-01T00:00" max="9999-12-31T23:59" value={occurredAt} onChange={(event) => { setOccurredAt(event.target.value); setError("") }} /></label>
          <label className="fa-entry-row"><Store aria-hidden="true" /><span>{type === "income" ? "Source" : type === "transfer" ? "Title" : "Merchant"}</span>
            <input type="text" maxLength={80} placeholder="Add a name" value={merchant} onChange={(event) => setMerchant(event.target.value)} /></label>
        </fieldset>
        {installments && <ActivityEntryPlan count={countText} fees={feeText} firstDueDate={firstDueDate} preview={previewTerms} terms={terms.value}
          error={terms.error} disabled={planLocked} existingPurchase={!!record}
          onCountChange={(value) => { setCountText(value); setError("") }}
          onFeesChange={(value) => { setFeeText(value); setError("") }}
          onDateChange={(value) => { setFirstDueDate(value); setError("") }} />}
        <fieldset disabled={baseLocked}>
          <Button type="button" variant="ghost" className="fa-entry-note-toggle" isDisabled={baseLocked}
            aria-expanded={showNote} aria-controls="fa-entry-note" onPress={() => setShowNote(!showNote)}>
            <Plus aria-hidden="true" /><span>{showNote ? "Hide note" : "Add a note"}</span><small>Optional</small>
          </Button>
          {showNote && <label className="fa-entry-note"><span className="sr-only">Note</span><textarea id="fa-entry-note" maxLength={300}
            placeholder="Anything you want to remember" value={note} onChange={(event) => setNote(event.target.value)} /></label>}
        </fieldset>
      </div>
      <footer className="fa-entry-submit">
        {pendingPlan && <p className="fa-message" role="status">Expense saved · installment setup pending. Retry to finish, or close and set up installments from this expense later.</p>}
        <p ref={errorRef} tabIndex={-1} id="fa-entry-error" className="fa-form-error" role={error ? "alert" : undefined}>{error}</p>
        <Button type="submit" variant="primary" className="fa-entry-save" isDisabled={saving || !pendingPlan && (!canEdit || !available || !entryAccounts.some((account) => String(account.id) === accountId))}>
          <Check aria-hidden="true" />{saving ? "Saving…" : pendingPlan ? "Retry installment plan" : installments ? record ? "Create installment plan" : "Add installment purchase" : record ? "Save changes" : type === "transfer" ? "Save transfer" : `Add ${type}`}
        </Button>
        <p className="fa-entry-caption">{installments ? "Records one purchase and creates its payment plan." : type === "transfer" ? "Moves money between accounts. Not an expense." : "Updates your account balance and activity."}</p>
      </footer>
    </form>}
  </ActivitySheet>
}
