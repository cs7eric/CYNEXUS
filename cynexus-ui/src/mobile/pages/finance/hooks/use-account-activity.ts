import { useCallback, useEffect, useState } from "react"

import { fetchFinanceTransactions } from "@/services/finance/ledger"
import type { ActivityTransactionPage } from "../lib/activity"
import { accountActivityRange, loadAccountActivityTransactions, type AccountActivityRange } from "../lib/account-activity"

export interface AccountActivitySnapshot extends ActivityTransactionPage {
  range: AccountActivityRange
  updatedAt: Date
}

interface AccountActivityState {
  key: string
  status: "loading" | "ready" | "error"
  snapshot?: AccountActivitySnapshot
  error?: string
}

export function useAccountActivity({ workspaceId, refreshToken = 0 }: { workspaceId: string; refreshToken?: number }) {
  const range = accountActivityRange()
  const key = `${workspaceId}:${range.start}:${range.end}`
  const [refreshCount, setRefreshCount] = useState(0)
  const [state, setState] = useState<AccountActivityState>({ key, status: "loading" })
  const refresh = useCallback(() => setRefreshCount((value) => value + 1), [])

  useEffect(() => {
    let active = true
    const timer = window.setTimeout(() => {
      const requestRange = accountActivityRange()
      setState((previous) => ({ ...(previous.key === key ? previous : {}), key, status: "loading", error: undefined }))
      void loadAccountActivityTransactions(requestRange, fetchFinanceTransactions, () => active).then((result) => {
        if (!active) return
        setState({ key, status: "ready", snapshot: { ...result, range: requestRange, updatedAt: new Date() },
          error: result.complete ? undefined : "Recent activity is incomplete. Account order is unavailable." })
      }).catch(() => {
        if (!active) return
        setState((previous) => ({ ...(previous.key === key ? previous : {}), key, status: "error", error: "Recent account activity could not be loaded." }))
      })
    }, 0)
    return () => { active = false; window.clearTimeout(timer) }
  }, [key, refreshToken, refreshCount])

  // Hide another workspace or day's data before its replacement effect runs.
  const current: AccountActivityState = state.key === key ? state : { key, status: "loading" }
  return { status: current.status, snapshot: current.snapshot, error: current.error,
    stale: current.status === "error" && !!current.snapshot, partial: !!current.snapshot && !current.snapshot.complete, refresh }
}
