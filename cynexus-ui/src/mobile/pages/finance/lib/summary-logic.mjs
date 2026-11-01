function exactFen(row) {
  const amount = row?.amount
  if (typeof amount !== "number" || !Number.isFinite(amount) || amount <= 0) return null
  const [coefficient, exponentText = "0"] = amount.toString().split("e")
  const [whole, fraction = ""] = coefficient.split(".")
  const digits = BigInt(whole + fraction)
  const shift = 2 + Number(exponentText) - fraction.length
  let value
  if (shift >= 0) value = digits * 10n ** BigInt(shift)
  else {
    const divisor = 10n ** BigInt(-shift)
    if (digits % divisor !== 0n) return null
    value = digits / divisor
  }
  return value > 0n && value <= BigInt(Number.MAX_SAFE_INTEGER) ? Number(value) : null
}

export function summarizeRows(rows, { complete = true, type = "all", accountId = null, toFen = exactFen } = {}) {
  if (!complete) return null
  let income = 0
  let expense = 0
  for (const row of rows) {
    if (row.archived || row.currency?.trim().toUpperCase() !== "CNY") continue
    if (accountId !== null && row.accountId !== accountId && row.toAccountId !== accountId) continue
    if (type !== "all" && row.transactionType !== type) continue
    const cents = toFen(row)
    if (cents === null) return null
    if (row.transactionType === "income") income += cents
    if (row.transactionType === "expense") expense += cents
  }
  return { income: income / 100, expense: expense / 100, net: (income - expense) / 100 }
}

export function groupCategories(rows, type = "expense", accountId = null, toFen = exactFen) {
  const values = new Map()
  for (const row of rows) {
    if (row.archived || row.transactionType !== type || row.currency?.trim().toUpperCase() !== "CNY") continue
    if (accountId !== null && row.accountId !== accountId && row.toAccountId !== accountId) continue
    const key = row.categoryId ?? "uncategorized"
    const cents = toFen(row)
    if (cents === null) continue
    const entry = values.get(key) ?? { id: key, label: row.categoryName || "Uncategorized", cents: 0 }
    entry.cents += cents
    values.set(key, entry)
  }
  return [...values.values()].map(({ cents, ...item }) => ({ ...item, amount: cents / 100 })).sort((a, b) => b.amount - a.amount).slice(0, 8)
}

export function activityDateKey(value) {
  const date = new Date(value)
  if (!Number.isFinite(date.getTime())) return null
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`
}

export function groupDays(rows, complete, accountId = null, toFen = exactFen) {
  const days = new Map()
  for (const row of rows) {
    if (row.archived || (accountId !== null && row.accountId !== accountId && row.toAccountId !== accountId)) continue
    const day = activityDateKey(row.occurredAt)
    if (!day) continue
    const bucket = days.get(day) ?? []
    bucket.push(row)
    days.set(day, bucket)
  }
  return [...days.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([key, items]) => ({ key, label: key.slice(-2), ...(summarizeRows(items, { complete, accountId, toFen }) ?? { income: null, expense: null, net: null }) }))
}
