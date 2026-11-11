import type { FinanceAccount, FinanceTransaction } from "@/services/finance/ledger"
import { ACTIVITY_TRANSACTION_LIMIT, activityDateKey, type ActivityTransactionFetcher, type ActivityTransactionPage } from "./activity"

export interface AccountActivityRange {
  start: string
  end: string
  /** Local midnight on the first included day. */
  from: number
  /** The request's current instant; future entries today are excluded too. */
  through: number
}

export interface RankedAccountActivity {
  account: FinanceAccount
  count: number
  lastOccurredAt: number | null
}

/** Today and the preceding 29 local calendar days, including DST transitions. */
export function accountActivityRange(now = new Date()): AccountActivityRange {
  if (!Number.isFinite(now.getTime())) throw new RangeError("Choose a valid activity date")
  const first = new Date(now)
  first.setHours(0, 0, 0, 0)
  first.setDate(first.getDate() - 29)
  return { start: activityDateKey(first.toISOString())!, end: activityDateKey(now.toISOString())!, from: first.getTime(), through: now.getTime() }
}

function recentTransactions(rows: FinanceTransaction[], range: AccountActivityRange): FinanceTransaction[] {
  const seen = new Set<number>()
  return rows.filter((row) => {
    const time = Date.parse(row.occurredAt)
    if (row.archived || !Number.isFinite(time) || time < range.from || time > range.through || seen.has(row.id)) return false
    if (!["expense", "income", "transfer"].includes(row.transactionType)) return false
    seen.add(row.id)
    return true
  })
}

/** Transfers count once for each participating account; ties preserve the input order. */
export function rankAccountsByRecentActivity(accounts: FinanceAccount[], transactions: FinanceTransaction[], range: AccountActivityRange): RankedAccountActivity[] {
  const activity = new Map(accounts.map((account) => [account.id, { count: 0, lastOccurredAt: null as number | null }]))
  for (const row of recentTransactions(transactions, range)) {
    const participants = new Set([row.accountId])
    if (row.transactionType === "transfer" && row.toAccountId != null) participants.add(row.toAccountId)
    const time = Date.parse(row.occurredAt)
    for (const id of participants) {
      const value = activity.get(id)
      if (!value) continue
      value.count++
      value.lastOccurredAt = Math.max(value.lastOccurredAt ?? -Infinity, time)
    }
  }
  return accounts.map((account, index) => ({ account, ...activity.get(account.id)!, index }))
    .toSorted((left, right) => right.count - left.count || (right.lastOccurredAt ?? -Infinity) - (left.lastOccurredAt ?? -Infinity) || left.index - right.index)
    .map(({ account, count, lastOccurredAt }) => ({ account, count, lastOccurredAt }))
}

function validateRows(rows: FinanceTransaction[]): FinanceTransaction[] {
  if (!Array.isArray(rows) || rows.some((row) => row === null || typeof row !== "object")) throw new Error("Transactions unavailable")
  return rows
}

/** Fetch the exact 30-day range; split capped responses into at most 30 daily reads. */
export async function loadAccountActivityTransactions(range: AccountActivityRange, fetcher: ActivityTransactionFetcher, isCurrent = () => true): Promise<ActivityTransactionPage> {
  const initial = validateRows(await fetcher({ start: range.start, end: range.end, limit: ACTIVITY_TRANSACTION_LIMIT }))
  if (!isCurrent()) throw new Error("Account activity request superseded")
  if (initial.length < ACTIVITY_TRANSACTION_LIMIT) return { transactions: recentTransactions(initial, range), complete: true, incompleteDays: [], failedDays: [] }
  const dates: string[] = []
  const date = new Date(range.from)
  date.setHours(12, 0, 0, 0)
  for (let index = 0; index < 30; index++) {
    dates.push(activityDateKey(date.toISOString())!)
    date.setDate(date.getDate() + 1)
  }
  const results = new Map<string, FinanceTransaction[]>()
  const incompleteDays: string[] = []
  const failedDays: string[] = []
  let next = 0
  async function worker() {
    while (next < dates.length && isCurrent()) {
      const day = dates[next++]
      try {
        const rows = validateRows(await fetcher({ start: day, end: day, limit: ACTIVITY_TRANSACTION_LIMIT }))
        results.set(day, rows)
        if (rows.length >= ACTIVITY_TRANSACTION_LIMIT) incompleteDays.push(day)
      } catch { failedDays.push(day) }
    }
  }
  await Promise.all(Array.from({ length: 4 }, () => worker()))
  if (!isCurrent()) throw new Error("Account activity request superseded")
  const byId = new Map<number, FinanceTransaction>()
  for (const rows of results.values()) for (const row of rows) byId.set(row.id, row)
  for (const row of initial) if (failedDays.includes(activityDateKey(row.occurredAt) ?? "")) byId.set(row.id, row)
  return {
    transactions: recentTransactions([...byId.values()], range),
    complete: incompleteDays.length === 0 && failedDays.length === 0,
    incompleteDays: incompleteDays.sort(), failedDays: failedDays.sort(),
  }
}
