import { useCallback, useEffect, useState } from "react"

import {
  fetchFinanceAccounts, fetchFinanceCategories, fetchFinanceTransactions,
  type FinanceAccount, type FinanceCategory,
} from "@/services/finance/ledger"
import { activityMonthKey, loadActivityTransactions, type ActivityMonth, type ActivityTransactionPage } from "../lib/activity"

export interface FinanceActivitySnapshot extends ActivityTransactionPage {
  accounts: FinanceAccount[]
  categories: FinanceCategory[]
  accountsAvailable: boolean
  categoriesAvailable: boolean
  updatedAt: Date
}

interface ActivityLoadState {
  key: string
  status: "loading" | "ready" | "error"
  snapshot?: FinanceActivitySnapshot
  error?: string
}

export function useFinanceActivity({ month, workspaceId, refreshToken = 0 }: { month: ActivityMonth; workspaceId: string; refreshToken?: number }) {
  const monthKey = activityMonthKey(month)
  const key = `${workspaceId}:${monthKey}`
  const [refreshCount, setRefreshCount] = useState(0)
  const [state, setState] = useState<ActivityLoadState>({ key, status: "loading" })
  const refresh = useCallback(() => setRefreshCount((value) => value + 1), [])

  useEffect(() => {
    let active = true
    const [year, calendarMonth] = monthKey.split("-").map(Number)
    const timer = window.setTimeout(() => {
      setState((previous) => ({ ...(previous.key === key ? previous : {}), key, status: "loading", error: undefined }))
      void Promise.allSettled([
        loadActivityTransactions({ year, month: calendarMonth - 1 }, fetchFinanceTransactions, () => active),
        fetchFinanceAccounts(false),
        fetchFinanceCategories(true),
      ]).then(([transactions, accounts, categories]) => {
        if (!active) return
        if (transactions.status === "rejected") {
          setState((previous) => ({ ...(previous.key === key ? previous : {}), key, status: "error", error: "Transactions could not be loaded." }))
          return
        }
        const accountsAvailable = accounts.status === "fulfilled" && Array.isArray(accounts.value)
        const categoriesAvailable = categories.status === "fulfilled" && Array.isArray(categories.value)
        const errors: string[] = []
        if (!transactions.value.complete) errors.push("Some transaction dates are incomplete. Monthly totals are unavailable.")
        if (!accountsAvailable) errors.push("Accounts could not be loaded.")
        if (!categoriesAvailable) errors.push("Categories could not be loaded.")
        setState((previous) => {
          const prior = previous.key === key ? previous.snapshot : undefined
          return {
            key, status: "ready", error: errors.length ? errors.join(" ") : undefined,
            snapshot: {
              ...transactions.value,
              accounts: accountsAvailable ? accounts.value : prior?.accounts ?? [],
              categories: categoriesAvailable ? categories.value : prior?.categories ?? [],
              accountsAvailable, categoriesAvailable, updatedAt: new Date(),
            },
          }
        })
      })
    }, 0)
    return () => { active = false; window.clearTimeout(timer) }
  }, [key, monthKey, refreshToken, refreshCount])

  // A workspace/month switch must never show the previous scope, even before effects run.
  const current: ActivityLoadState = state.key === key ? state : { key, status: "loading" }
  return {
    status: current.status, snapshot: current.snapshot, error: current.error,
    stale: current.status === "error" && !!current.snapshot,
    partial: !!current.snapshot && (!current.snapshot.complete || !current.snapshot.accountsAvailable || !current.snapshot.categoriesAvailable),
    refresh,
  }
}
