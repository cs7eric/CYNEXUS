import type { FinanceAccount, FinanceInstallmentPlan } from "@/services/finance/ledger"
import { activityTransactionFen } from "./activity"
import { parseActivityAmount } from "./activity-entry"

export interface ActivityInstallmentScheduleRow {
  dueDate: string
  principalFen: number
  feeFen: number
  amountFen: number
}

export interface ActivityInstallmentTerms {
  feeFen: number
  count: number
  firstDueDate: string
  intervalMonths: number
  schedule: ActivityInstallmentScheduleRow[]
}

const normalizedCurrency = (currency: string) => typeof currency === "string" ? currency.trim().toUpperCase() : ""
function pad(value: number) { return String(value).padStart(2, "0") }
function daysInMonth(year: number, month: number) {
  if (month === 2) return year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0) ? 29 : 28
  return [4, 6, 9, 11].includes(month) ? 30 : 31
}

export function getCnyCreditInstallmentPlans(plans: FinanceInstallmentPlan[], accounts: FinanceAccount[]) {
  const eligible = new Set(accounts.filter((account) => account.nature === "credit" && normalizedCurrency(account.currency) === "CNY").map((account) => account.id))
  const visible = plans.filter((plan) => eligible.has(plan.accountId))
  return { plans: visible, excludedCount: plans.length - visible.length }
}

export function getCnyAssetPaymentAccounts(accounts: FinanceAccount[]) {
  return accounts.filter((account) => !account.archived && account.nature === "asset" && normalizedCurrency(account.currency) === "CNY")
}

/** Exact cent allocation matching FinanceInstallmentPolicy: the last row receives every remainder. */
export function buildActivityInstallmentTerms({ totalFen, feeText, countText, firstDueDate, intervalText }: {
  totalFen: number; feeText: string; countText: string; firstDueDate: string; intervalText: string
}): { error?: string; value?: ActivityInstallmentTerms } {
  if (!Number.isSafeInteger(totalFen) || totalFen <= 0 || activityTransactionFen({ amount: totalFen / 100 }) !== totalFen) return { error: "Enter a valid purchase amount first." }
  if (!/^\d+$/.test(countText.trim())) return { error: "Choose a whole number of installments from 2 to 60." }
  const count = Number(countText.trim())
  if (!Number.isInteger(count) || count < 2 || count > 60) return { error: "Choose a whole number of installments from 2 to 60." }
  if (totalFen < count) return { error: "Each installment needs at least ¥0.01 of principal. Reduce the installment count." }
  const feeValue = feeText.trim()
  const feeFen = feeValue === "" || /^0+(?:\.0{1,2})?$/.test(feeValue) ? 0 : parseActivityAmount(feeValue)
  if (feeFen === null) return { error: "Enter a total fee of zero or more, with up to two decimal places." }
  if (!Number.isSafeInteger(totalFen + feeFen)) return { error: "The total repayment amount is too large to calculate accurately." }
  if (!/^\d+$/.test(intervalText.trim())) return { error: "Choose an interval from 1 to 12 months." }
  const intervalMonths = Number(intervalText.trim())
  if (intervalMonths < 1 || intervalMonths > 12) return { error: "Choose an interval from 1 to 12 months." }
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(firstDueDate)
  if (!match) return { error: "Choose a valid first due date." }
  const [year, month, day] = match.slice(1).map(Number)
  if (year < 1 || month < 1 || month > 12 || day < 1 || day > daysInMonth(year, month)) return { error: "Choose a valid first due date." }
  const basePrincipal = Number(BigInt(totalFen) / BigInt(count))
  const baseFee = Number(BigInt(feeFen) / BigInt(count))
  const schedule: ActivityInstallmentScheduleRow[] = []
  for (let index = 0; index < count; index++) {
    const absoluteMonth = year * 12 + month - 1 + index * intervalMonths
    const dueYear = Math.floor(absoluteMonth / 12)
    const dueMonth = absoluteMonth % 12 + 1
    if (dueYear > 9999) return { error: "The schedule extends beyond the supported date range." }
    const principalFen = index === count - 1 ? totalFen - basePrincipal * (count - 1) : basePrincipal
    const rowFeeFen = index === count - 1 ? feeFen - baseFee * (count - 1) : baseFee
    if (activityTransactionFen({ amount: (principalFen + rowFeeFen) / 100 }) !== principalFen + rowFeeFen) return { error: "The installment amount is too large to represent accurately." }
    schedule.push({
      dueDate: `${String(dueYear).padStart(4, "0")}-${pad(dueMonth)}-${pad(Math.min(day, daysInMonth(dueYear, dueMonth)))}`,
      principalFen, feeFen: rowFeeFen, amountFen: principalFen + rowFeeFen,
    })
  }
  return { value: { feeFen, count, firstDueDate, intervalMonths, schedule } }
}
