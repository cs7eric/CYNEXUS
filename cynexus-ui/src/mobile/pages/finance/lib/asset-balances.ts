import type { FinanceAccount } from "@/services/finance/ledger"
import { moneyToFen } from "./asset-pile"

export interface CnyAccountSummary {
  assetFen: number | null
  liabilityFen: number | null
  netFen: number | null
  excludedCount: number
  missingAssetCount: number
  missingCreditCount: number
  cnyCount: number
}

const MAX_SAFE_FEN = BigInt(Number.MAX_SAFE_INTEGER)

function safeTotal(value: bigint): number | null {
  return value > MAX_SAFE_FEN || value < -MAX_SAFE_FEN ? null : Number(value)
}

/** Account balances are never combined across currencies without an explicit exchange rate. */
export function summarizeCnyAccounts(accounts: FinanceAccount[]): CnyAccountSummary {
  let assets = 0n
  let liabilities = 0n
  let excludedCount = 0
  let missingAssetCount = 0
  let missingCreditCount = 0
  let cnyCount = 0

  for (const account of accounts) {
    if (account.archived) continue
    if (typeof account.currency !== "string" || account.currency.trim().toUpperCase() !== "CNY") {
      excludedCount++
      continue
    }

    cnyCount++
    if (account.nature !== "asset" && account.nature !== "credit") {
      // An unclassified CNY balance could belong to either side of the summary.
      missingAssetCount++
      missingCreditCount++
      continue
    }
    const fen = typeof account.balance === "number" ? moneyToFen(account.balance) : null
    if (account.nature === "asset") {
      if (fen === null) missingAssetCount++
      else assets += BigInt(fen)
    } else {
      if (fen === null) missingCreditCount++
      else liabilities += BigInt(fen)
    }
  }

  const assetFen = missingAssetCount ? null : safeTotal(assets)
  const liabilityFen = missingCreditCount ? null : safeTotal(liabilities)
  const netFen = assetFen === null || liabilityFen === null ? null : safeTotal(assets - liabilities)
  return { assetFen, liabilityFen, netFen, excludedCount, missingAssetCount, missingCreditCount, cnyCount }
}
