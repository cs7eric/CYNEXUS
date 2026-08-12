import { ArrowDownToLine, Check, CreditCard } from "lucide-react";
import { useMemo, useState, type FormEvent, type ReactNode } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { notification } from "@/components/ui/notification";
import type { FinanceAccount } from "@/services/finance/ledger";
import { formatMoney } from "../lib/format";

function localDateTime(value = new Date()) {
  const pad = (part: number) => String(part).padStart(2, "0");
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}`;
}

export function CreditRepaymentForm({
  accounts,
  creditAccountId: targetCreditAccountId,
  embedded = false,
  onSaved,
}: {
  accounts: FinanceAccount[];
  creditAccountId?: number;
  embedded?: boolean;
  onSaved: (payload: Record<string, unknown>) => Promise<void>;
}) {
  const creditAccounts = useMemo(
    () =>
      accounts.filter(
        (item) =>
          !item.archived &&
          item.nature === "credit" &&
          (targetCreditAccountId == null || item.id === targetCreditAccountId),
      ),
    [accounts, targetCreditAccountId],
  );
  const paymentAccounts = useMemo(
    () => accounts.filter((item) => !item.archived && item.nature === "asset"),
    [accounts],
  );
  const [creditAccountId, setCreditAccountId] = useState("");
  const [paymentAccountId, setPaymentAccountId] = useState("");
  const [amount, setAmount] = useState("");
  const [occurredAt, setOccurredAt] = useState(localDateTime());
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);

  const selectedCreditAccountId = creditAccounts.some(
    (item) => String(item.id) === creditAccountId,
  )
    ? creditAccountId
    : creditAccounts[0]
      ? String(creditAccounts[0].id)
      : "";
  const selectedPaymentAccountId = paymentAccounts.some(
    (item) => String(item.id) === paymentAccountId,
  )
    ? paymentAccountId
    : paymentAccounts[0]
      ? String(paymentAccounts[0].id)
      : "";

  const creditAccount = creditAccounts.find(
    (item) => String(item.id) === selectedCreditAccountId,
  );

  function fillBalance() {
    if (creditAccount?.balance != null && creditAccount.balance > 0) {
      setAmount(creditAccount.balance.toFixed(2));
    }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const numericAmount = Number(amount);
    if (
      !selectedCreditAccountId ||
      !selectedPaymentAccountId ||
      !Number.isFinite(numericAmount) ||
      numericAmount <= 0 ||
      !occurredAt
    ) {
      notification.danger({
        title: "请补全还款信息",
        description: "信用账户、付款账户、金额和发生时间必须填写。",
      });
      return;
    }
    setSaving(true);
    try {
      await onSaved({
        transactionType: "transfer",
        accountId: selectedPaymentAccountId,
        toAccountId: selectedCreditAccountId,
        categoryId: null,
        amount: numericAmount,
        currency: creditAccount?.currency || "CNY",
        occurredAt: `${occurredAt}:00+08:00`,
        merchant: creditAccount?.name || "信用账户还款",
        note: note.trim() || "信用账户还款",
      });
      setAmount("");
      setNote("");
      setOccurredAt(localDateTime());
    } catch (cause) {
      notification.danger({
        title: "信用账户还款失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    } finally {
      setSaving(false);
    }
  }

  if (!creditAccounts.length) return null;

  return (
    <section
      className={
        embedded
          ? "min-w-0 border-b border-border/60 py-5"
          : "rounded-xl border border-border/70 bg-card p-4"
      }
    >
      <div className="flex items-start gap-3">
        <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-rose-500/10 text-rose-600 dark:text-rose-400">
          <ArrowDownToLine className="size-4" />
        </span>
        <div>
          <h2 className="text-sm font-semibold">信用账户还款</h2>
          <p className="mt-0.5 text-xs leading-5 text-muted-foreground">
            从资产账户转入信用账户，记录为转账，不重复计入支出。分期计划请在下方逐期登记。
          </p>
        </div>
      </div>
      <form
        onSubmit={submit}
        className={
          embedded
            ? "mt-4 grid min-w-0 gap-3 sm:grid-cols-2"
            : "mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-5"
        }
      >
        <Field label="信用账户">
          <select
            required
            value={selectedCreditAccountId}
            onChange={(event) => setCreditAccountId(event.target.value)}
            className="h-9 min-w-0 w-full rounded-md border border-input bg-background px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            {creditAccounts.map((account) => (
              <option key={String(account.id)} value={String(account.id)}>
                {account.name} · 待还 {formatMoney(account.balance)}
              </option>
            ))}
          </select>
        </Field>
        <Field label="付款账户">
          <select
            required
            value={selectedPaymentAccountId}
            onChange={(event) => setPaymentAccountId(event.target.value)}
            className="h-9 min-w-0 w-full rounded-md border border-input bg-background px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <option value="">选择资产账户</option>
            {paymentAccounts.map((account) => (
              <option key={String(account.id)} value={String(account.id)}>
                {account.name} · 余额 {formatMoney(account.balance)}
              </option>
            ))}
          </select>
        </Field>
        <Field label="还款金额">
          <div className="flex min-w-0 gap-2">
            <Input
              required
              min="0.01"
              step="0.01"
              type="number"
              inputMode="decimal"
              placeholder="0.00"
              value={amount}
              className="min-w-0"
              onChange={(event) => setAmount(event.target.value)}
            />
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="shrink-0"
              onClick={fillBalance}
              title="填入当前待还余额"
            >
              全额
            </Button>
          </div>
        </Field>
        <Field label="发生时间">
          <Input
            required
            type="datetime-local"
            value={occurredAt}
            onChange={(event) => setOccurredAt(event.target.value)}
          />
        </Field>
        <Field label="备注">
          <Input
            placeholder="可选"
            value={note}
            onChange={(event) => setNote(event.target.value)}
          />
        </Field>
        <div
          className={
            embedded
              ? "flex items-end justify-end sm:col-span-2"
              : "flex items-end sm:col-span-2 xl:col-span-5 xl:justify-end"
          }
        >
          <Button
            type="submit"
            disabled={saving || !paymentAccounts.length}
            className="w-full gap-2 sm:w-auto"
          >
            <CreditCard className="size-4" />
            {saving ? "保存中" : <><Check className="size-4" />登记还款</>}
          </Button>
        </div>
      </form>
    </section>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block min-w-0 space-y-1.5 text-xs font-medium">
      <span>{label}</span>
      {children}
    </label>
  );
}
