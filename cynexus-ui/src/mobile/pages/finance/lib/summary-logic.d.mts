import type { FinanceTransaction } from "@/services/finance/ledger"

export function summarizeRows(rows: FinanceTransaction[], options?: { complete?: boolean; type?: "all" | "expense" | "income"; accountId?: number | null; toFen?: (row: FinanceTransaction) => number | null }): { income: number; expense: number; net: number } | null
export function groupCategories(rows: FinanceTransaction[], type?: "expense" | "income", accountId?: number | null, toFen?: (row: FinanceTransaction) => number | null): Array<{ id: number | "uncategorized"; label: string; amount: number }>
export function activityDateKey(value: string): string | null
export function groupDays(rows: FinanceTransaction[], complete: boolean, accountId?: number | null, toFen?: (row: FinanceTransaction) => number | null): Array<{ key: string; label: string; income: number | null; expense: number | null; net: number | null }>
