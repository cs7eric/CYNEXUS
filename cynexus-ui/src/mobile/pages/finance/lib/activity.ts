import type { FinanceTransaction } from "@/services/finance/ledger"

export interface ActivityMonth {
  year: number
  /** Calendar month, from 0 (January) to 11 (December). */
  month: number
}

export type ActivityFilterType = "all" | FinanceTransaction["transactionType"]

export interface ActivityFilters {
  type?: ActivityFilterType
  query?: string
  /** Omit to show records from every currency without combining their amounts. */
  currency?: string
  accountId?: number | null
  categoryId?: number | null
}

export interface ActivitySummary {
  currency: string
  incomeFen: number | null
  expenseFen: number | null
  netFen: number | null
  incomeCount: number
  expenseCount: number
  transferCount: number
  invalidCount: number
  excludedCurrencyCount: number
  complete: boolean
}

export interface ActivityDayGroup {
  date: string
  transactions: FinanceTransaction[]
  summary: ActivitySummary
}

export interface ActivityTransactionPage {
  transactions: FinanceTransaction[]
  complete: boolean
  /** Dates whose response reached the server's hard cap. */
  incompleteDays: string[]
  failedDays: string[]
}

export type ActivityTransactionFetcher = (params: { start: string; end: string; limit: number }) => Promise<FinanceTransaction[]>

export const ACTIVITY_TRANSACTION_LIMIT = 200
const MAX_SAFE_FEN = BigInt(Number.MAX_SAFE_INTEGER)

function pad(value: number) { return String(value).padStart(2, "0") }
function normalizedCurrency(value: unknown): string { return typeof value === "string" ? value.trim().toUpperCase() : "" }

function monthDate(month: ActivityMonth): Date {
  if (!Number.isInteger(month.year) || month.year < 1 || month.year > 9999 || !Number.isInteger(month.month) || month.month < 0 || month.month > 11) {
    throw new RangeError("Choose a valid calendar month")
  }
  const date = new Date(0)
  date.setFullYear(month.year, month.month, 1)
  date.setHours(12, 0, 0, 0)
  return date
}

export function currentActivityMonth(date = new Date()): ActivityMonth {
  return { year: date.getFullYear(), month: date.getMonth() }
}

export function activityMonthKey(month: ActivityMonth): string {
  monthDate(month)
  return `${String(month.year).padStart(4, "0")}-${pad(month.month + 1)}`
}

export function activityMonthBounds(month: ActivityMonth): { start: string; end: string } {
  const date = monthDate(month)
  date.setMonth(date.getMonth() + 1, 0)
  const key = activityMonthKey(month)
  return { start: `${key}-01`, end: `${key}-${pad(date.getDate())}` }
}

export function shiftActivityMonth(month: ActivityMonth, delta: number): ActivityMonth {
  if (!Number.isInteger(delta)) throw new RangeError("Month shift must be a whole number")
  const date = monthDate(month)
  date.setMonth(date.getMonth() + delta)
  const next = currentActivityMonth(date)
  monthDate(next)
  return next
}

/** Group the recorded instant in the user's local calendar, rather than slicing a UTC timestamp. */
export function activityDateKey(occurredAt: string): string | null {
  if (typeof occurredAt !== "string" || !occurredAt.trim()) return null
  const date = new Date(occurredAt)
  if (!Number.isFinite(date.getTime())) return null
  return `${String(date.getFullYear()).padStart(4, "0")}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** Ledger amounts must be strictly positive, exact to a cent, and safe in integer arithmetic. */
export function activityTransactionFen(transaction: Pick<FinanceTransaction, "amount">): number | null {
  const amount = transaction.amount
  if (typeof amount !== "number" || !Number.isFinite(amount) || amount <= 0) return null
  const [coefficient, exponentText = "0"] = amount.toString().split("e")
  const [whole, fraction = ""] = coefficient.split(".")
  const digits = BigInt(whole + fraction)
  const shift = 2 + Number(exponentText) - fraction.length
  let value: bigint
  if (shift >= 0) value = digits * 10n ** BigInt(shift)
  else {
    const divisor = 10n ** BigInt(-shift)
    if (digits % divisor !== 0n) return null
    value = digits / divisor
  }
  return value > 0n && value <= MAX_SAFE_FEN ? Number(value) : null
}

function safeFen(value: bigint): number | null {
  return value < -MAX_SAFE_FEN || value > MAX_SAFE_FEN ? null : Number(value)
}

export function summarizeActivityTransactions(
  transactions: FinanceTransaction[],
  { currency = "CNY", complete = true }: { currency?: string; complete?: boolean } = {},
): ActivitySummary {
  const code = normalizedCurrency(currency)
  let income = 0n
  let expense = 0n
  let incomeCount = 0
  let expenseCount = 0
  let transferCount = 0
  let invalidCount = 0
  let excludedCurrencyCount = 0
  for (const transaction of transactions) {
    if (transaction.archived) continue
    if (!code || normalizedCurrency(transaction.currency) !== code) { excludedCurrencyCount++; continue }
    const value = activityTransactionFen(transaction)
    if (value === null) invalidCount++
    if (transaction.transactionType === "income") {
      incomeCount++
      if (value !== null) income += BigInt(value)
    } else if (transaction.transactionType === "expense") {
      expenseCount++
      if (value !== null) expense += BigInt(value)
    } else if (transaction.transactionType === "transfer") transferCount++
    else invalidCount++
  }
  const valid = complete && !!code && invalidCount === 0
  return {
    currency: code, incomeFen: valid ? safeFen(income) : null, expenseFen: valid ? safeFen(expense) : null,
    netFen: valid ? safeFen(income - expense) : null, incomeCount, expenseCount, transferCount,
    invalidCount, excludedCurrencyCount, complete,
  }
}

export function filterActivityTransactions(transactions: FinanceTransaction[], filters: ActivityFilters = {}): FinanceTransaction[] {
  const query = filters.query?.trim().toLocaleLowerCase() ?? ""
  const currency = filters.currency === undefined ? null : normalizedCurrency(filters.currency)
  return transactions.filter((transaction) => {
    if (transaction.archived) return false
    if (filters.type && filters.type !== "all" && transaction.transactionType !== filters.type) return false
    if (currency !== null && normalizedCurrency(transaction.currency) !== currency) return false
    if (filters.accountId != null && transaction.accountId !== filters.accountId && transaction.toAccountId !== filters.accountId) return false
    if (filters.categoryId != null && transaction.categoryId !== filters.categoryId) return false
    return !query || [transaction.merchant, transaction.categoryName, transaction.accountName, transaction.toAccountName, transaction.note]
      .filter((value): value is string => typeof value === "string").join(" ").toLocaleLowerCase().includes(query)
  })
}

function sortTransactions(transactions: FinanceTransaction[]): FinanceTransaction[] {
  return transactions.toSorted((left, right) => {
    const leftTime = Date.parse(left.occurredAt)
    const rightTime = Date.parse(right.occurredAt)
    return (Number.isFinite(rightTime) ? rightTime : -Infinity) - (Number.isFinite(leftTime) ? leftTime : -Infinity) || right.id - left.id
  })
}

export function groupActivityTransactions(
  transactions: FinanceTransaction[],
  options: { currency?: string; complete?: boolean } = {},
): ActivityDayGroup[] {
  const groups = new Map<string, FinanceTransaction[]>()
  for (const transaction of sortTransactions(transactions)) {
    if (transaction.archived) continue
    const day = activityDateKey(transaction.occurredAt) ?? "unknown"
    const group = groups.get(day)
    if (group) group.push(transaction)
    else groups.set(day, [transaction])
  }
  return [...groups.entries()].map(([date, rows]) => ({ date, transactions: rows, summary: summarizeActivityTransactions(rows, options) }))
}

function validateRows(rows: FinanceTransaction[]): FinanceTransaction[] {
  if (!Array.isArray(rows) || rows.some((row) => row === null || typeof row !== "object")) throw new Error("Transactions unavailable")
  return rows
}

/**
 * The server has an inclusive date range and a 200-row hard cap, with no cursor.
 * Split a capped month into days. A capped day remains explicitly incomplete;
 * its records are useful to browse, but cannot establish an exact monthly total.
 */
export async function loadActivityTransactions(month: ActivityMonth, fetcher: ActivityTransactionFetcher, isCurrent = () => true): Promise<ActivityTransactionPage> {
  const bounds = activityMonthBounds(month)
  const initial = validateRows(await fetcher({ ...bounds, limit: ACTIVITY_TRANSACTION_LIMIT }))
  if (initial.length < ACTIVITY_TRANSACTION_LIMIT) return { transactions: sortTransactions(initial), complete: true, incompleteDays: [], failedDays: [] }
  const days = Number(bounds.end.slice(-2))
  const monthKey = activityMonthKey(month)
  const results = new Map<string, FinanceTransaction[]>()
  const incompleteDays: string[] = []
  const failedDays: string[] = []
  let nextDay = 1
  async function worker() {
    while (nextDay <= days && isCurrent()) {
      const date = `${monthKey}-${pad(nextDay++)}`
      try {
        const rows = validateRows(await fetcher({ start: date, end: date, limit: ACTIVITY_TRANSACTION_LIMIT }))
        results.set(date, rows)
        if (rows.length >= ACTIVITY_TRANSACTION_LIMIT) incompleteDays.push(date)
      } catch {
        failedDays.push(date)
      }
    }
  }
  await Promise.all(Array.from({ length: 4 }, () => worker()))
  if (!isCurrent()) throw new Error("Activity request superseded")
  const byId = new Map<number, FinanceTransaction>()
  for (const rows of results.values()) for (const row of rows) byId.set(row.id, row)
  // A failed request keeps only the original month's available rows for that day.
  for (const row of initial) {
    if (failedDays.includes(activityDateKey(row.occurredAt) ?? "")) byId.set(row.id, row)
  }
  return {
    transactions: sortTransactions([...byId.values()]),
    complete: incompleteDays.length === 0 && failedDays.length === 0,
    incompleteDays: incompleteDays.sort(), failedDays: failedDays.sort(),
  }
}
