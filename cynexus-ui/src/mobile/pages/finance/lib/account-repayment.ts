import type { FinanceAccount, FinanceTransaction } from "@/services/finance/ledger"
import { activityDateTimePayload, parseActivityAmount } from "./activity-entry"

const currencyOf = (account: FinanceAccount) => account.currency.trim().toUpperCase()

export function accountRepaymentDueFen(account: FinanceAccount): number | null {
  if (account.balance == null || !Number.isFinite(account.balance)) return null
  if (account.balance <= 0) return 0
  return parseActivityAmount(String(account.balance))
}

export function accountRepaymentSources(creditAccount: FinanceAccount, accounts: FinanceAccount[]) {
  const currency = currencyOf(creditAccount)
  return accounts.filter((account) => !account.archived && account.nature === "asset" && account.id !== creditAccount.id && currency !== "" && currencyOf(account) === currency)
}

export function buildAccountRepayment({ accounts, creditAccountId, paymentAccountId, amount, occurredAt, note }: {
  accounts: FinanceAccount[]
  creditAccountId: number
  paymentAccountId: string
  amount: string
  occurredAt: string
  note: string
}): { value?: Partial<FinanceTransaction>; error?: string } {
  const creditAccount = accounts.find((account) => account.id === creditAccountId && !account.archived && account.nature === "credit")
  if (!creditAccount) return { error: "This credit account is unavailable. Refresh your accounts before repaying." }
  const paymentAccount = accountRepaymentSources(creditAccount, accounts).find((account) => String(account.id) === paymentAccountId)
  if (!paymentAccount) return { error: "Choose an active asset account in the same currency." }
  const dueFen = accountRepaymentDueFen(creditAccount)
  if (dueFen === null) return { error: "The current amount due is unavailable. Refresh your accounts before repaying." }
  if (dueFen === 0) return { error: "There is no outstanding balance to repay." }
  const amountFen = parseActivityAmount(amount)
  if (amountFen === null) return { error: "Enter a repayment greater than zero, with up to two decimal places." }
  if (amountFen > dueFen) return { error: "The repayment cannot exceed the current amount due." }
  const dateTime = activityDateTimePayload(occurredAt)
  if (!dateTime) return { error: "Choose a valid date and time." }
  return { value: {
    transactionType: "transfer", accountId: paymentAccount.id, toAccountId: creditAccount.id,
    categoryId: null, amount: amountFen / 100, currency: currencyOf(creditAccount),
    occurredAt: dateTime, merchant: creditAccount.name, note: note.trim() || "Credit account repayment",
  } }
}
