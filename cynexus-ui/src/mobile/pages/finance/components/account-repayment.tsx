import { Button } from "@heroui/react/button"
import { CalendarDays, CalendarClock, Check, CreditCard, Wallet } from "lucide-react"
import { useRef, useState, type FormEvent } from "react"

import { saveFinanceTransaction, type FinanceAccount, type FinanceTransaction } from "@/services/finance/ledger"
import { accountRepaymentDueFen, accountRepaymentSources, buildAccountRepayment } from "../lib/account-repayment"
import { localActivityDateTime } from "../lib/activity-entry"
import { formatCny } from "../lib/asset-pile"
import { ActivityEntryChoice } from "./activity-entry-choice"
import { ActivitySheet } from "./activity-sheet"
import "../activity.css"
import "./account-repayment.css"

function money(fen: number | null, currency: string) {
  if (fen === null) return "Unavailable"
  if (currency === "CNY") return formatCny(fen)
  return /^[A-Z]{3}$/.test(currency) ? new Intl.NumberFormat("en-US", { style: "currency", currency }).format(fen / 100) : (fen / 100).toFixed(2)
}

export function AccountRepaymentSheet({ account, accounts, available, onClose, onSaved, onOpenInstallments }: {
  account: FinanceAccount
  accounts: FinanceAccount[]
  available: boolean
  onClose: () => void
  onSaved: (transaction: FinanceTransaction) => Promise<void> | void
  onOpenInstallments: () => void
}) {
  const dueFen = accountRepaymentDueFen(account)
  const sources = accountRepaymentSources(account, accounts)
  const [paymentId, setPaymentId] = useState(String(sources[0]?.id ?? ""))
  const [amount, setAmount] = useState(dueFen ? (dueFen / 100).toFixed(2) : "")
  const [occurredAt, setOccurredAt] = useState(() => localActivityDateTime())
  const [note, setNote] = useState("")
  const [error, setError] = useState("")
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const savingRef = useRef(false)
  const savedRef = useRef(false)
  const errorRef = useRef<HTMLParagraphElement>(null)
  const currency = account.currency.trim().toUpperCase()
  const locked = saving || saved || !available
  const unavailable = !available || !sources.length || dueFen === null || dueFen === 0 || account.archived || account.nature !== "credit"

  function fail(message: string) {
    setError(message)
    requestAnimationFrame(() => errorRef.current?.focus())
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (savingRef.current || savedRef.current) return
    if (!available) return fail("Your accounts are unavailable. Close this sheet and refresh before repaying.")
    const payload = buildAccountRepayment({ accounts, creditAccountId: account.id, paymentAccountId: paymentId, amount, occurredAt, note })
    if (!payload.value) return fail(payload.error!)
    savingRef.current = true; setSaving(true); setError("")
    let transaction: FinanceTransaction
    try {
      transaction = await saveFinanceTransaction(payload.value)
    } catch {
      savingRef.current = false; setSaving(false)
      return fail("Could not save this repayment. Your details are still here; please try again.")
    }
    // A refresh failure must never allow posting the confirmed repayment again.
    savedRef.current = true; setSaved(true)
    try { await onSaved(transaction) }
    catch { fail("Repayment saved. Close this sheet and refresh to update the balances.") }
    finally { savingRef.current = false; setSaving(false) }
  }

  return <ActivitySheet title={`Repay ${account.name}`} className="fa-entry-sheet fa-repayment-sheet" busy={saving} onClose={onClose}>
    <form className="fa-entry-form" onSubmit={submit} noValidate aria-busy={saving}>
      <div className="fa-entry-scroll">
        <div className="fa-repayment-due"><CreditCard aria-hidden="true" /><div><span>Current due</span><strong>{money(dueFen, currency)}</strong></div><span>{currency}</span></div>
        <fieldset disabled={locked}>
          <div className="fa-entry-amount-block">
            <div className="fa-entry-amount-heading"><label htmlFor="fa-repayment-amount">Repayment amount · {currency}</label>
              <Button type="button" variant="ghost" size="sm" isDisabled={locked || !dueFen} onPress={() => { setAmount((dueFen! / 100).toFixed(2)); setError("") }}>Full amount</Button></div>
            <div className="fa-entry-amount-line"><input id="fa-repayment-amount" type="text" inputMode="decimal" autoComplete="off"
              aria-label="Repayment amount" aria-describedby="fa-repayment-error" placeholder="0.00" value={amount} onChange={(event) => { setAmount(event.target.value); setError("") }} /></div>
          </div>
          <div className="fa-repayment-source"><ActivityEntryChoice label="Pay from" icon={<Wallet aria-hidden="true" />} disabled={locked || !sources.length}
            value={paymentId} options={sources.map((source) => ({ id: String(source.id), label: source.name }))}
            onChange={(value) => { setPaymentId(value); setError("") }} /></div>
          <label className="fa-entry-row"><CalendarDays aria-hidden="true" /><span>Date &amp; time</span><input type="datetime-local" aria-label="Repayment date and time" value={occurredAt}
            min="0001-01-01T00:00" max="9999-12-31T23:59" onChange={(event) => { setOccurredAt(event.target.value); setError("") }} /></label>
          <label className="fa-repayment-note"><span>Note · Optional</span><textarea aria-label="Repayment note" value={note} maxLength={300} rows={2} placeholder="Anything you want to remember" onChange={(event) => setNote(event.target.value)} /></label>
        </fieldset>
        {!sources.length && <p className="fa-message" role="status">Add an active {currency} asset account to repay from.</p>}
        {dueFen === 0 && <p className="fa-message" role="status">There is no outstanding balance to repay.</p>}
        {dueFen === null && <p className="fa-message" role="status">The current amount due is unavailable. Close this sheet and refresh.</p>}
        {currency === "CNY" && <div className="fa-repayment-installments"><p>For scheduled payments, repay the matching installment.</p>
          <Button type="button" variant="ghost" isDisabled={locked} onPress={onOpenInstallments}><CalendarClock aria-hidden="true" />Installment plans</Button></div>}
      </div>
      <footer className="fa-entry-submit">
        <p id="fa-repayment-error" ref={errorRef} tabIndex={-1} className="fa-form-error" role={error ? "alert" : undefined}>{error}</p>
        <Button type="submit" variant="primary" className="fa-entry-save" isDisabled={locked || unavailable}><Check aria-hidden="true" />{saving ? "Saving…" : saved ? "Repayment saved" : "Save repayment"}</Button>
        <p className="fa-entry-caption">Records a transfer and reduces your debt. No new expense.</p>
      </footer>
    </form>
  </ActivitySheet>
}
