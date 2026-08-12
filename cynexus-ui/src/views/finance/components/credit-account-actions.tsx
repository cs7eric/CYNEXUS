import { notification } from "@/components/ui/notification";
import {
  saveFinanceTransaction,
  type FinanceAccount,
  type FinanceTransaction,
} from "@/services/finance/ledger";
import { CreditInstallmentPanel } from "./credit-installment-panel";
import { CreditRepaymentForm } from "./credit-repayment-form";

export function CreditAccountActions({
  account,
  accounts,
  onChanged,
}: {
  account: FinanceAccount;
  accounts: FinanceAccount[];
  onChanged?: () => Promise<void> | void;
}) {
  async function saveRepayment(payload: Record<string, unknown>) {
    try {
      await saveFinanceTransaction(
        payload as unknown as Partial<FinanceTransaction>,
      );
      notification.success({ title: "还款已登记" });
      await onChanged?.();
    } catch (cause) {
      notification.danger({
        title: "信用账户还款失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
      throw cause;
    }
  }

  return (
    <div className="border-t border-border/60 px-4 sm:px-6">
      <CreditRepaymentForm
        accounts={accounts}
        creditAccountId={account.id}
        embedded
        onSaved={saveRepayment}
      />
      <CreditInstallmentPanel
        accounts={accounts}
        accountId={account.id}
        embedded
        onChanged={onChanged}
      />
    </div>
  );
}
