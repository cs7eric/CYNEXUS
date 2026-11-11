import { CalendarClock } from "lucide-react"

import type { ActivityInstallmentTerms } from "../lib/activity-installments"
import { formatCny } from "../lib/asset-pile"
import { ActivityEntryChoice } from "./activity-entry-choice"

export function ActivityEntryPlan({ count, fees, firstDueDate, preview, terms, error, disabled, existingPurchase, onCountChange, onFeesChange, onDateChange }: {
  count: string
  fees: string
  firstDueDate: string
  preview?: ActivityInstallmentTerms
  terms?: ActivityInstallmentTerms
  error?: string
  disabled: boolean
  existingPurchase: boolean
  onCountChange: (value: string) => void
  onFeesChange: (value: string) => void
  onDateChange: (value: string) => void
}) {
  const principal = preview?.schedule.reduce((total, row) => total + row.principalFen, 0)
  return <fieldset className="fa-entry-plan" disabled={disabled}>
    <div className="fa-entry-plan-heading"><CalendarClock aria-hidden="true" /><div>
      <h3>Repayment plan</h3><p>The purchase amount above is split across these payments.</p>
    </div></div>
    <div className="fa-entry-plan-summary" aria-label="Repayment summary">
      <div><span>Total to repay</span><strong>{preview && principal !== undefined ? formatCny(principal + preview.feeFen) : "—"}</strong></div>
      <div><span>Each payment</span><strong>{preview ? formatCny(preview.schedule[0].amountFen) : "—"}</strong></div>
    </div>
    <div className="fa-entry-plan-breakdown"><span>Purchase <strong>{principal !== undefined ? formatCny(principal) : "—"}</strong></span><span>Fees <strong>{preview ? formatCny(preview.feeFen) : "—"}</strong></span></div>
    <div className="fa-entry-plan-fields">
      <ActivityEntryChoice label="Number of payments" boxed disabled={disabled} value={count}
        options={["12", "6", "3", "1", "24"].map((id) => ({ id, label: `${id} ${id === "1" ? "payment" : "payments"}` }))} onChange={onCountChange} />
      <div className="fa-entry-plan-field"><span>Payment frequency</span><output aria-label="Payment frequency">Every month</output></div>
      <label className="fa-entry-plan-field"><span>First payment date</span><input aria-label="First payment date" type="date" min="0001-01-01" max="9999-12-31" value={firstDueDate} onChange={(event) => onDateChange(event.target.value)} /></label>
      <label className="fa-entry-plan-field"><span>Total fees · CNY</span><input aria-label="Total fees" type="text" inputMode="decimal" placeholder="0.00" value={fees} onChange={(event) => onFeesChange(event.target.value)} /></label>
    </div>
    <p className="fa-entry-plan-hint">{preview ? "The final payment may differ by a few cents so the total settles exactly." : error ?? "Enter the purchase amount above to calculate the plan."}</p>
    {terms ? <details className="fa-entry-plan-schedule"><summary>View {terms.count} payment dates</summary><ol>
      {terms.schedule.map((payment, index) => <li key={payment.dueDate}><span>{index + 1} · {payment.dueDate}</span><strong>{formatCny(payment.amountFen)}</strong></li>)}
    </ol></details> : <p className="fa-entry-plan-hint">{firstDueDate ? error : "Choose a first payment date to preview the schedule."}</p>}
    {existingPurchase && <p className="fa-entry-plan-hint">This expense is already recorded. Only a repayment plan will be added.</p>}
  </fieldset>
}
