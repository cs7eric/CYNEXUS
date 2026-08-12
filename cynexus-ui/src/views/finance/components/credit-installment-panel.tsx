import { useCallback, useEffect, useMemo, useState } from "react";
import { CalendarClock, Check, CreditCard, LoaderCircle } from "lucide-react";

import { Button } from "@/components/ui/button";
import { notification } from "@/components/ui/notification";
import { Skeleton } from "@/components/ui/skeleton";
import {
  fetchFinanceInstallmentPlans,
  payFinanceInstallment,
  type FinanceAccount,
  type FinanceInstallmentPlan,
} from "@/services/finance/ledger";
import { formatMoney } from "../lib/format";

export function CreditInstallmentPanel({
  accounts,
  accountId,
  embedded = false,
  onChanged,
}: {
  accounts: FinanceAccount[];
  accountId?: number;
  embedded?: boolean;
  onChanged?: () => Promise<void> | void;
}) {
  const [plans, setPlans] = useState<FinanceInstallmentPlan[]>([]);
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState<string | null>(null);
  const creditAccounts = useMemo(
    () =>
      accounts.filter(
        (item) =>
          !item.archived &&
          item.nature === "credit" &&
          (accountId == null || item.id === accountId),
      ),
    [accountId, accounts],
  );
  const paymentAccounts = useMemo(
    () => accounts.filter((item) => !item.archived && item.nature === "asset"),
    [accounts],
  );

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setPlans(await fetchFinanceInstallmentPlans(accountId));
    } catch (cause) {
      notification.danger({
        title: "分期计划加载失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    } finally {
      setLoading(false);
    }
  }, [accountId]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void load();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  async function pay(
    plan: FinanceInstallmentPlan,
    installmentNo: number,
    paymentAccountId: number,
  ) {
    const key = `${plan.id}-${installmentNo}`;
    setPaying(key);
    try {
      await payFinanceInstallment(plan.id, installmentNo, { paymentAccountId });
      notification.success({ title: "分期已还款" });
      await load();
      await onChanged?.();
    } catch (cause) {
      notification.danger({
        title: "分期还款失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    } finally {
      setPaying(null);
    }
  }

  if (!creditAccounts.length && !plans.length) return null;

  return (
    <section
      className={
        embedded
          ? "min-w-0 py-5"
          : "rounded-xl border border-border/70 bg-card p-4"
      }
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-start gap-3">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-rose-500/10 text-rose-600 dark:text-rose-400">
            <CalendarClock className="size-4" />
          </span>
          <div>
            <h2 className="text-sm font-semibold">信用分期</h2>
            <p className="mt-0.5 text-xs leading-5 text-muted-foreground">
              查看该信用账户的分期计划，并登记下一期还款。
            </p>
          </div>
        </div>
        <span className="text-xs text-muted-foreground">
          {plans.length} 个计划
        </span>
      </div>
      {loading ? (
        <div className="mt-4 space-y-3">
          <Skeleton className="h-20" />
          <Skeleton className="h-20" />
        </div>
      ) : plans.length ? (
        <div className={embedded ? "mt-4" : "mt-4 space-y-3"}>
          {plans.map((plan) => (
            <InstallmentPlanRow
              key={plan.id}
              plan={plan}
              paymentAccounts={paymentAccounts}
              paying={paying}
              embedded={embedded}
              onPay={pay}
            />
          ))}
        </div>
      ) : (
        <div className="mt-4 rounded-lg border border-dashed border-border/70 px-4 py-8 text-center text-xs text-muted-foreground">
          这个信用账户还没有分期计划。
        </div>
      )}
    </section>
  );
}

function InstallmentPlanRow({
  plan,
  paymentAccounts,
  paying,
  embedded,
  onPay,
}: {
  plan: FinanceInstallmentPlan;
  paymentAccounts: FinanceAccount[];
  paying: string | null;
  embedded: boolean;
  onPay: (
    plan: FinanceInstallmentPlan,
    installmentNo: number,
    paymentAccountId: number,
  ) => Promise<void>;
}) {
  const [paymentAccountId, setPaymentAccountId] = useState(
    paymentAccounts[0]?.id ? String(paymentAccounts[0].id) : "",
  );
  const next = plan.installments.find((item) => item.status === "pending");
  const paid = plan.installments.filter((item) => item.status === "paid").length;

  return (
    <div
      className={
        embedded
          ? "border-b border-border/60 py-3 last:border-b-0"
          : "rounded-lg border border-border/70 bg-background p-3"
      }
    >
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <CreditCard className="size-4 text-rose-600 dark:text-rose-400" />
            <span className="truncate text-sm font-medium">
              {plan.merchant || "信用消费"}
            </span>
            <span className="rounded bg-muted px-1.5 py-0.5 text-[11px] text-muted-foreground">
              {plan.accountName}
            </span>
          </div>
          <div className="mt-1 text-xs text-muted-foreground">
            {formatMoney(plan.totalAmount)} · {plan.installmentCount} 期 · 已还 {paid} 期 ·{" "}
            {plan.status === "completed" ? "已完成" : "进行中"}
          </div>
        </div>
        {next ? (
          <div className="text-right">
            <div className="text-xs text-muted-foreground">
              下一期 {next.dueDate}
            </div>
            <div className="mt-0.5 font-mono text-sm tabular-nums">
              {formatMoney(next.amount)}
            </div>
          </div>
        ) : (
          <span className="text-xs text-emerald-600 dark:text-emerald-400">
            全部完成
          </span>
        )}
      </div>
      {next ? (
        <div className="mt-3 flex flex-wrap items-center justify-end gap-2 border-t border-border/60 pt-3">
          <select
            aria-label="还款账户"
            value={paymentAccountId}
            onChange={(event) => setPaymentAccountId(event.target.value)}
            className="h-9 min-w-44 rounded-md border border-input bg-background px-2 text-sm"
          >
            <option value="">选择资产账户</option>
            {paymentAccounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
          <Button
            type="button"
            size="sm"
            disabled={!paymentAccountId || paying != null}
            onClick={() => void onPay(plan, next.installmentNo, Number(paymentAccountId))}
            className="gap-1.5"
          >
            {paying === `${plan.id}-${next.installmentNo}` ? (
              <LoaderCircle className="size-3.5 animate-spin" />
            ) : (
              <Check className="size-3.5" />
            )}
            {paying === `${plan.id}-${next.installmentNo}`
              ? "处理中"
              : "登记还款"}
          </Button>
        </div>
      ) : null}
    </div>
  );
}
