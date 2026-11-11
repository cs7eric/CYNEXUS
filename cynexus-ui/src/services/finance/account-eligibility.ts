import type { FinanceAccount, FinanceTransaction } from "./ledger"

/** Securities accounts participate only in transfers, never income or expenses. */
export function canUseFinanceAccountForTransaction(account: FinanceAccount, type: FinanceTransaction["transactionType"]) {
  return account.accountType !== "investment" || type === "transfer"
}
