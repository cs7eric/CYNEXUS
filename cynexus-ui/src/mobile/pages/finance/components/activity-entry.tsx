import { Check, Plus } from "lucide-react"
import { useRef, useState, type FormEvent } from "react"
import { Link } from "react-router-dom"

import { saveFinanceTransaction, updateFinanceTransaction, type FinanceAccount, type FinanceCategory, type FinanceTransaction } from "@/services/finance/ledger"
import { type ActivityMonth } from "../lib/activity"
import { activityDateTimePayload, defaultActivityEntryDate, localActivityDateTime, parseActivityAmount } from "../lib/activity-entry"
import { compactCny, decomposeAssets } from "../lib/asset-pile"
import { ActivityCash } from "./activity-cash"
import { ActivitySheet } from "./activity-sheet"

type EntryType = FinanceTransaction["transactionType"]

export function ActivityEntry({ record, initialType = "expense", month, accounts, categories, available, loading, onClose, onSaved }: {
  record?: FinanceTransaction; initialType?: EntryType; month: ActivityMonth; accounts: FinanceAccount[]; categories: FinanceCategory[];
  available: boolean; loading: boolean; onClose: () => void; onSaved: (record: FinanceTransaction) => void
}) {
  return <EntryForm record={record} initialType={initialType} month={month} accounts={accounts} categories={categories} available={available} loading={loading} onClose={onClose} onSaved={onSaved} />
}

function EntryForm({ record, initialType, month, accounts, categories, available, loading, onClose, onSaved }: {
  record?: FinanceTransaction; initialType: EntryType; month: ActivityMonth; accounts: FinanceAccount[]; categories: FinanceCategory[];
  available: boolean; loading: boolean; onClose: () => void; onSaved: (record: FinanceTransaction) => void
}) {
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
  const [error, setError] = useState("")
  const [saving, setSaving] = useState(false)
  const savingRef = useRef(false)
  const errorRef = useRef<HTMLParagraphElement>(null)
  const fen = parseActivityAmount(amount)
  const compatibleCategories = categories.filter((category) => !category.archived && (category.direction === type || category.direction === "both"))
  const selectedCategory = compatibleCategories.some((category) => String(category.id) === categoryId) ? categoryId : ""
  const { groups, loose } = decomposeAssets(fen ?? 0)
  const canEdit = !record || record.currency.toUpperCase() === "CNY" && !record.archived

  function changeType(next: EntryType) {
    setType(next)
    setCategoryId(String(categories.find((category) => !category.archived && (category.direction === next || category.direction === "both"))?.id ?? ""))
    setError("")
  }
  function fail(message: string) { setError(message); requestAnimationFrame(() => errorRef.current?.focus()) }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (savingRef.current) return
    if (!available) return fail("Your accounts and categories must be available before saving. Close this sheet and refresh to try again.")
    if (!canEdit) return fail("This record cannot be edited here.")
    if (!fen) return fail("Enter an amount greater than zero, with up to two decimal places.")
    const account = activeAccounts.find((item) => String(item.id) === accountId)
    if (!account) return fail("Choose an active CNY account.")
    const destination = activeAccounts.find((item) => String(item.id) === toAccountId)
    if (type === "transfer" && (!destination || destination.id === account.id)) return fail("Choose two different active CNY accounts.")
    if (type !== "transfer" && !selectedCategory) return fail("Choose a category for this record.")
    const date = activityDateTimePayload(occurredAt)
    if (!date) return fail("Choose a valid date and time.")
    const payload: Partial<FinanceTransaction> = {
      transactionType: type, amount: fen / 100, currency: "CNY", accountId: account.id,
      toAccountId: type === "transfer" ? destination!.id : null,
      categoryId: type === "transfer" ? null : Number(selectedCategory), occurredAt: date,
      merchant: merchant.trim() || null, note: note.trim() || null,
    }
    savingRef.current = true
    setSaving(true)
    setError("")
    try {
      const saved = record ? await updateFinanceTransaction(record.id, payload) : await saveFinanceTransaction(payload)
      onSaved(saved)
    } catch {
      fail("Could not save this record. Your entry is still here; please try again.")
    } finally { savingRef.current = false; setSaving(false) }
  }

  return <ActivitySheet title={record ? "Edit record" : "New record"} onClose={onClose} busy={saving}>
    {!activeAccounts.length ? <div className="fa-empty" role="status"><h3>{loading ? "Loading your accounts…" : !available ? "Entry unavailable" : "Add a CNY account first"}</h3><p>{!available ? "Your accounts and categories must be available before recording a transaction." : "Choose an account for your income, expenses and transfers."}</p><Link to="#finance-accounts" className="fa-text-button" onClick={onClose}>View accounts</Link></div> :
    <form className="fa-entry-form" onSubmit={submit} noValidate aria-busy={saving}>
      {!available && <p className="fa-message" role="status">Accounts or categories unavailable. Your entry is preserved; close this sheet and refresh before saving.</p>}
      <fieldset disabled={saving || !canEdit || !available}>
        <div className="fa-type-switch" role="group" aria-label="Record type">{(["expense", "income", "transfer"] as const).map((value) => <button key={value} type="button" aria-pressed={type === value} onClick={() => changeType(value)}>{value[0].toUpperCase() + value.slice(1)}</button>)}</div>
        <div className="fa-amount-entry"><label htmlFor="fa-entry-amount">CNY</label><input id="fa-entry-amount" type="text" inputMode="decimal" autoComplete="off" placeholder="0.00" value={amount} aria-describedby="fa-entry-composition fa-entry-error" onChange={(event) => { setAmount(event.target.value); setError("") }} /></div>
        <ActivityCash amount={fen ?? 0} size="entry" />
        <p className="fa-denominations" id="fa-entry-composition">{fen ? [...groups, ...loose].map((group) => `${group.count} × ${compactCny(group.valueInFen)}`).join(" · ") : "Enter an amount to see it in cash."}</p>
        {type !== "transfer" && <div className="fa-category-field"><span className="fa-field-label" id="fa-category-label">Category</span><div className="fa-category-options" role="group" aria-labelledby="fa-category-label">{compatibleCategories.map((category) => <button type="button" key={category.id} aria-pressed={selectedCategory === String(category.id)} onClick={() => setCategoryId(String(category.id))}>{category.name}</button>)}</div>{!compatibleCategories.length && <p className="fa-message">No categories available for {type}. Add one in desktop Finance, or choose another record type.</p>}</div>}
        <div className="fa-form-row">
          <label className="fa-field"><span>{type === "transfer" ? "From account" : "Account"}</span><select aria-label={type === "transfer" ? "From account" : "Account"} value={accountId} onChange={(event) => setAccountId(event.target.value)}><option value="">Choose account</option>{activeAccounts.map((account) => <option value={account.id} key={account.id}>{account.name}</option>)}</select></label>
          {type === "transfer" && <label className="fa-field"><span>To account</span><select aria-label="To account" value={toAccountId} onChange={(event) => setToAccountId(event.target.value)}><option value="">Choose account</option>{activeAccounts.map((account) => <option value={account.id} key={account.id}>{account.name}</option>)}</select></label>}
          <label className={`fa-field${type === "transfer" ? " is-full" : ""}`}><span>Date &amp; time</span><input type="datetime-local" aria-label="Date and time" value={occurredAt} onChange={(event) => setOccurredAt(event.target.value)} /></label>
        </div>
        <label className="fa-field"><span>{type === "income" ? "Source / title" : "Merchant / title"}</span><input type="text" maxLength={80} placeholder={type === "income" ? "e.g. Monthly salary" : type === "transfer" ? "e.g. Credit card repayment" : "e.g. Lunch at the corner café"} value={merchant} onChange={(event) => setMerchant(event.target.value)} /></label>
        <button type="button" className="fa-notes-toggle" aria-expanded={showNote} onClick={() => setShowNote(!showNote)}><Plus aria-hidden="true" />{showNote ? "Hide note" : "Add a note"}</button>
        {showNote && <label className="fa-field"><span>Note</span><textarea maxLength={300} placeholder="Anything you want to remember" value={note} onChange={(event) => setNote(event.target.value)} /></label>}
      </fieldset>
      <p ref={errorRef} tabIndex={-1} id="fa-entry-error" className="fa-form-error" role={error ? "alert" : undefined}>{error}</p>
      <button type="submit" className="fa-primary" disabled={saving || !canEdit || !available}><Check aria-hidden="true" />{saving ? "Saving…" : record ? "Save changes" : "Add record"}</button>
      <p className="fa-footnote">{type === "transfer" ? "Transfers and credit repayments are excluded from income and expenses." : "Saving updates your account balance and monthly activity."}</p>
    </form>}
  </ActivitySheet>
}
