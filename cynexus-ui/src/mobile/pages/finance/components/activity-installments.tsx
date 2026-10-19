import { ArrowLeft, CalendarClock, Check, ChevronRight, RefreshCw } from "lucide-react"
import { useCallback, useEffect, useRef, useState } from "react"

import { fetchFinanceInstallmentPlans, payFinanceInstallment, type FinanceAccount, type FinanceInstallmentPlan } from "@/services/finance/ledger"
import { activityTransactionFen } from "../lib/activity"
import { getCnyAssetPaymentAccounts, getCnyCreditInstallmentPlans } from "../lib/activity-installments"
import { formatCny } from "../lib/asset-pile"
import { ActivitySheet } from "./activity-sheet"

interface PlanLoadState {
  status: "loading" | "ready" | "error"
  plans?: FinanceInstallmentPlan[]
}

function money(amount: number) {
  const fen = amount === 0 ? 0 : activityTransactionFen({ amount })
  return fen === null ? "Unavailable" : formatCny(fen)
}
function paymentKey(planId: number, installmentNo: number) { return `${planId}:${installmentNo}` }

export function ActivityInstallments({ accounts, onClose, onChanged }: {
  accounts: FinanceAccount[]; onClose: () => void; onChanged: () => Promise<void> | void
}) {
  const [load, setLoad] = useState<PlanLoadState>({ status: "loading" })
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [installmentNo, setInstallmentNo] = useState<number | null>(null)
  const [paymentAccountId, setPaymentAccountId] = useState("")
  const [note, setNote] = useState("")
  const [error, setError] = useState("")
  const [notice, setNotice] = useState("")
  const [saving, setSaving] = useState(false)
  const [confirmedPayments, setConfirmedPayments] = useState<string[]>([])
  const active = useRef(true)
  const requestId = useRef(0)
  const savingRef = useRef(false)
  const errorRef = useRef<HTMLParagraphElement>(null)
  const { plans, excludedCount } = getCnyCreditInstallmentPlans(load.plans ?? [], accounts)
  const paymentAccounts = getCnyAssetPaymentAccounts(accounts)
  const accountId = paymentAccounts.some((account) => String(account.id) === paymentAccountId) ? paymentAccountId : String(paymentAccounts[0]?.id ?? "")
  const selected = plans.find((plan) => plan.id === selectedId)
  const rows = selected?.installments?.toSorted((left, right) => left.installmentNo - right.installmentNo) ?? []
  const isPaid = (planId: number, row: { installmentNo: number; status: string }) => row.status === "paid" || confirmedPayments.includes(paymentKey(planId, row.installmentNo))
  const pending = selected ? rows.filter((row) => row.status === "pending" && !isPaid(selected.id, row)) : []
  const selectedRow = pending.find((row) => row.installmentNo === installmentNo) ?? pending[0]
  const creditAccount = selected ? accounts.find((account) => account.id === selected.accountId && !account.archived && account.nature === "credit" && account.currency.trim().toUpperCase() === "CNY") : undefined

  const reload = useCallback(async () => {
    const request = ++requestId.current
    if (active.current) setLoad((previous) => ({ ...previous, status: "loading" }))
    try {
      const value = await fetchFinanceInstallmentPlans()
      if (!Array.isArray(value)) throw new Error("Installment plans unavailable")
      if (active.current && request === requestId.current) {
        setLoad({ status: "ready", plans: value })
        setConfirmedPayments([])
      }
      return true
    } catch {
      if (active.current && request === requestId.current) setLoad((previous) => ({ ...previous, status: "error" }))
      return false
    }
  }, [])
  const invalidateRequests = useCallback(() => { requestId.current++ }, [])

  useEffect(() => {
    active.current = true
    const timer = window.setTimeout(() => { void reload() }, 0)
    return () => { active.current = false; invalidateRequests(); window.clearTimeout(timer) }
  }, [reload, invalidateRequests])

  function selectPlan(plan: FinanceInstallmentPlan) {
    setSelectedId(plan.id); setInstallmentNo(null); setError(""); setNotice(""); setNote("")
  }
  function fail(message: string) { setError(message); requestAnimationFrame(() => errorRef.current?.focus()) }

  async function saveRepayment() {
    if (savingRef.current) return
    if (!selected || !selectedRow || !creditAccount) return fail("Choose a pending installment on an active CNY credit account.")
    const paymentAccount = paymentAccounts.find((account) => String(account.id) === accountId)
    if (!paymentAccount || paymentAccount.id === selected.accountId) return fail("Choose an active CNY asset account for this repayment.")
    if (activityTransactionFen({ amount: selectedRow.amount }) === null) return fail("The installment amount is unavailable. Refresh this plan before paying.")
    savingRef.current = true; setSaving(true); setError(""); setNotice("")
    try {
      const updated = await payFinanceInstallment(selected.id, selectedRow.installmentNo, { paymentAccountId: paymentAccount.id, note: note.trim() || undefined })
      if (!active.current) return
      setConfirmedPayments((previous) => [...previous, paymentKey(selected.id, selectedRow.installmentNo)])
      setLoad((previous) => ({ status: "ready", plans: (previous.plans ?? []).map((plan) => plan.id === selected.id ? updated : plan) }))
      setNote("")
      const reloaded = await reload()
      let changed = true
      try { await onChanged() } catch { changed = false }
      if (active.current) {
        setNotice("Repayment saved.")
        if (!reloaded || !changed) setError("Repayment saved. Some data could not refresh; showing the last available schedule.")
      }
    } catch {
      if (active.current) fail("Could not save this repayment. Your details are still here; please try again.")
    } finally {
      savingRef.current = false
      if (active.current) setSaving(false)
    }
  }

  return <ActivitySheet title="Installments" onClose={onClose} busy={saving}>
    <div className="fa-plan-toolbar">
      {selected ? <button type="button" className="fa-text-button" disabled={saving} onClick={() => { setSelectedId(null); setError(""); setNotice("") }}><ArrowLeft aria-hidden="true" />All plans</button> : <span className="fa-plan-count">{load.plans ? `${plans.length} CNY ${plans.length === 1 ? "plan" : "plans"}` : "CNY credit plans"}</span>}
      <button type="button" className="fa-round" aria-label="Refresh installment plans" title="Refresh installment plans" disabled={saving || load.status === "loading"} onClick={() => { void reload() }}><RefreshCw aria-hidden="true" /></button>
    </div>
    {load.status === "error" && <div className="fa-alert" role="status"><p>{load.plans ? "Could not refresh. Showing the last loaded plans." : "Installment plans could not be loaded."}</p><button type="button" className="fa-text-button" disabled={saving} onClick={() => { void reload() }}>Try again</button></div>}
    {load.status === "loading" && <p className="fa-message" role="status">{load.plans ? "Refreshing plans…" : "Loading installment plans…"}</p>}
    {!selected && load.plans && !plans.length && <div className="fa-empty" role="status"><CalendarClock aria-hidden="true" /><h3>No CNY installment plans.</h3><p>Installment purchases on your CNY credit accounts will appear here.</p></div>}
    {!selected && !!plans.length && <div className="fa-plan-list">{plans.map((plan) => {
      const schedule = Array.isArray(plan.installments) ? plan.installments : []
      const paid = schedule.filter((row) => isPaid(plan.id, row)).length
      const next = schedule.find((row) => row.status === "pending" && !isPaid(plan.id, row))
      return <button type="button" key={plan.id} className="fa-plan-card" onClick={() => selectPlan(plan)}>
        <span className="fa-plan-card-heading"><strong>{plan.merchant || "Installment purchase"}</strong><ChevronRight aria-hidden="true" /></span>
        <span className="fa-plan-card-meta">{plan.accountName || "Credit account"} · {money(plan.totalAmount)} · {plan.installmentCount} installments</span>
        <span className="fa-plan-progress">{paid} of {plan.installmentCount} paid · {plan.status === "cancelled" ? "Cancelled" : paid === plan.installmentCount || plan.status === "completed" ? "Completed" : "Active"}</span>
        {next && <span className="fa-plan-next">Next {next.dueDate} · {money(next.amount)}</span>}
      </button>
    })}</div>}
    {selected && <>
      <div className="fa-plan-detail-heading"><h3>{selected.merchant || "Installment purchase"}</h3><p>{selected.accountName || "Credit account"} · CNY</p></div>
      <dl className="fa-plan-summary"><div><dt>Purchase</dt><dd>{money(selected.totalAmount)}</dd></div><div><dt>Total fees</dt><dd>{money(selected.feeTotal)}</dd></div><div><dt>Schedule</dt><dd>{selected.installmentCount} installments · every {selected.intervalMonths} {selected.intervalMonths === 1 ? "month" : "months"}</dd></div></dl>
      <div className="fa-plan-schedule" role="group" aria-label="Installment schedule">{rows.map((row) => {
        const paid = isPaid(selected.id, row)
        const status = paid ? "paid" : row.status
        return <button type="button" key={row.id} className={`fa-plan-row is-${status}${selectedRow?.installmentNo === row.installmentNo ? " is-selected" : ""}`} disabled={saving || paid || row.status !== "pending" || selected.status === "cancelled"} aria-pressed={!paid && selectedRow?.installmentNo === row.installmentNo} onClick={() => { setInstallmentNo(row.installmentNo); setError(""); setNotice("") }}>
          <span className="fa-plan-row-copy"><strong>Installment {row.installmentNo} · {paid ? "Paid" : row.status === "cancelled" ? "Cancelled" : "Pending"}</strong><span>{row.dueDate} · Principal {money(row.principalAmount)} · Fee {money(row.feeAmount)}</span></span><span className="fa-plan-row-amount">{money(row.amount)}{paid && <Check aria-hidden="true" />}</span>
        </button>
      })}</div>
      {selectedRow && selected.status !== "cancelled" && <div className="fa-plan-payment">
        <h3>Repay installment {selectedRow.installmentNo}</h3><p className="fa-message">{money(selectedRow.amount)} · recorded now as a transfer.</p>
        <label className="fa-field"><span>Pay from</span><select aria-label="Repayment account" disabled={saving || !paymentAccounts.length || !creditAccount} value={accountId} onChange={(event) => setPaymentAccountId(event.target.value)}><option value="">Choose CNY asset account</option>{paymentAccounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}</select></label>
        <label className="fa-field"><span>Note (optional)</span><input type="text" maxLength={300} disabled={saving} value={note} placeholder="Anything you want to remember" onChange={(event) => setNote(event.target.value)} /></label>
        {(!paymentAccounts.length || !creditAccount) && <p className="fa-message">An active CNY asset account and an active CNY credit account are required to repay.</p>}
        <button type="button" className="fa-primary" disabled={saving || !accountId || !creditAccount || load.status === "loading"} onClick={() => { void saveRepayment() }}><Check aria-hidden="true" />{saving ? "Saving…" : "Save repayment"}</button>
      </div>}
      {!pending.length && rows.length > 0 && <p className="fa-message">{selected.status === "cancelled" ? "This plan is cancelled." : "No pending installments."}</p>}
    </>}
    <p className="fa-form-error" ref={errorRef} tabIndex={-1} role={error ? "alert" : undefined}>{error}</p>
    <p className="fa-plan-payment-status" role="status">{notice}</p>
    <p className="fa-footnote">CNY credit plans only.{excludedCount > 0 && ` ${excludedCount} ${excludedCount === 1 ? "plan in another or unavailable account is" : "plans in other or unavailable accounts are"} excluded.`} Repayments are transfers and do not count as a new expense.</p>
  </ActivitySheet>
}
