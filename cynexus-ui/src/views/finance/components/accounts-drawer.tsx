import { useCallback, useState } from "react";
import {
  ArrowDownToLine,
  CalendarClock,
  CreditCard,
  Landmark,
  RotateCw,
  Wallet,
  WalletCards,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  Drawer,
  DrawerContent,
  DrawerDescription,
  DrawerHeader,
  DrawerTitle,
  DrawerTrigger,
} from "@/components/ui/drawer";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import { ScrollArea } from "@/components/ui/scroll-area";
import { notification } from "@/components/ui/notification";
import { Skeleton } from "@/components/ui/skeleton";
import { useIsMobile } from "@/hooks/use-mobile";
import {
  fetchFinanceAccounts,
  saveFinanceTransaction,
  type FinanceAccount,
  type FinanceTransaction,
} from "@/services/finance/ledger";
import { CreditInstallmentPanel } from "./credit-installment-panel";
import { CreditRepaymentForm } from "./credit-repayment-form";
import { accountTypeLabel, formatMoney } from "../lib/format";

export function AccountsDrawer({
  onChanged,
}: {
  onChanged?: () => Promise<void> | void;
}) {
  const isMobile = useIsMobile();
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [accounts, setAccounts] = useState<FinanceAccount[]>([]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setAccounts(await fetchFinanceAccounts(false));
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : String(cause);
      setError(message);
      notification.danger({ title: "账户加载失败", description: message });
    } finally {
      setLoading(false);
    }
  }, []);

  function handleOpenChange(nextOpen: boolean) {
    setOpen(nextOpen);
    if (nextOpen) void load();
  }

  async function refreshAfterChange() {
    await load();
    await onChanged?.();
  }

  const activeAccounts = accounts.filter((account) => !account.archived);

  return (
    <Drawer
      open={open}
      onOpenChange={handleOpenChange}
      direction={isMobile ? "bottom" : "right"}
    >
      <DrawerTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="min-h-9 shrink-0 gap-1.5 px-2.5 sm:px-3"
        >
          <WalletCards className="size-3.5" />
          账户
        </Button>
      </DrawerTrigger>
      <DrawerContent className="data-[vaul-drawer-direction=right]:w-full data-[vaul-drawer-direction=right]:sm:max-w-2xl data-[vaul-drawer-direction=bottom]:max-h-[88vh]">
        <DrawerHeader className="border-b border-border/60 px-6 py-5 sm:px-8">
          <DrawerTitle>账户</DrawerTitle>
          <DrawerDescription>
            展开账户查看余额、额度与信用账户的还款和分期计划。
          </DrawerDescription>
        </DrawerHeader>
        <ScrollArea className="min-h-0 flex-1">
          {loading ? (
            <div className="space-y-3 px-4 py-5 sm:px-6">
              <Skeleton className="h-14" />
              <Skeleton className="h-14" />
              <Skeleton className="h-14" />
            </div>
          ) : error ? (
            <div className="px-4 py-12 text-center sm:px-6">
              <p className="text-sm font-medium">账户暂时无法加载</p>
              <p className="mt-1 text-xs leading-5 text-muted-foreground">
                {error}
              </p>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="mt-4 gap-1.5"
                onClick={() => void load()}
              >
                <RotateCw className="size-3.5" />
                重新加载
              </Button>
            </div>
          ) : activeAccounts.length ? (
            <Accordion type="single" collapsible className="px-4 sm:px-6">
              {activeAccounts.map((account) => (
                <AccountCollapsible
                  key={account.id}
                  account={account}
                  accounts={activeAccounts}
                  onChanged={refreshAfterChange}
                />
              ))}
            </Accordion>
          ) : (
            <div className="px-4 py-12 text-center text-sm text-muted-foreground sm:px-6">
              还没有可用账户。
            </div>
          )}
        </ScrollArea>
      </DrawerContent>
    </Drawer>
  );
}

function AccountCollapsible({
  account,
  accounts,
  onChanged,
}: {
  account: FinanceAccount;
  accounts: FinanceAccount[];
  onChanged: () => Promise<void>;
}) {
  const credit = account.nature === "credit";
  const Icon = credit
    ? CreditCard
    : account.accountType === "bank"
      ? Landmark
      : Wallet;

  return (
    <AccordionItem value={String(account.id)} className="border-border/60">
      <AccordionTrigger className="gap-3 py-3 hover:no-underline">
        <span
          className={`flex size-8 shrink-0 items-center justify-center rounded-md ${credit ? "bg-rose-500/10 text-rose-600 dark:text-rose-400" : "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"}`}
        >
          <Icon className="size-4" />
        </span>
        <span className="min-w-0 flex-1 text-left">
          <span className="block truncate text-sm font-medium">
            {account.name}
          </span>
          <span className="mt-0.5 block truncate text-xs text-muted-foreground">
            {accountTypeLabel(account.accountType)} · {credit ? "信用" : "资产"}
          </span>
        </span>
        <span
          className={`shrink-0 font-mono text-sm tabular-nums ${credit ? "text-rose-600 dark:text-rose-400" : "text-foreground"}`}
        >
          {formatMoney(account.balance)}
        </span>
      </AccordionTrigger>
      <AccordionContent className="pb-4 pt-1">
        <AccountDetails account={account} />
        {credit ? (
          <div className="mt-4 flex flex-wrap gap-2 border-t border-border/60 pt-3">
            <RepaymentDialog
              account={account}
              accounts={accounts}
              onChanged={onChanged}
            />
            <InstallmentsDialog
              account={account}
              accounts={accounts}
              onChanged={onChanged}
            />
          </div>
        ) : null}
      </AccordionContent>
    </AccordionItem>
  );
}

function AccountDetails({ account }: { account: FinanceAccount }) {
  const credit = account.nature === "credit";
  const utilization =
    credit && account.creditLimit
      ? ((account.balance ?? 0) / account.creditLimit) * 100
      : null;
  return (
    <div className="grid gap-x-6 gap-y-3 sm:grid-cols-2">
      <Detail label={credit ? "当前待还" : "当前余额"} value={formatMoney(account.balance)} />
      <Detail label="账户类型" value={accountTypeLabel(account.accountType)} />
      <Detail label="期初余额" value={formatMoney(account.openingBalance)} />
      <Detail label="币种" value={account.currency} />
      {credit && account.creditLimit != null ? (
        <Detail label="信用额度" value={formatMoney(account.creditLimit)} />
      ) : null}
      {utilization != null ? (
        <Detail label="额度使用率" value={`${utilization.toFixed(1)}%`} />
      ) : null}
      {credit && account.billingDay != null ? (
        <Detail label="账单日" value={`每月 ${account.billingDay} 日`} />
      ) : null}
      {credit && account.dueDay != null ? (
        <Detail label="还款日" value={`每月 ${account.dueDay} 日`} />
      ) : null}
    </div>
  );
}

function RepaymentDialog({
  account,
  accounts,
  onChanged,
}: {
  account: FinanceAccount;
  accounts: FinanceAccount[];
  onChanged: () => Promise<void>;
}) {
  const [open, setOpen] = useState(false);

  async function save(payload: Record<string, unknown>) {
    await saveFinanceTransaction(
      payload as unknown as Partial<FinanceTransaction>,
    );
    notification.success({ title: "还款已登记" });
    setOpen(false);
    await onChanged();
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button type="button" size="sm" variant="outline" className="gap-1.5">
          <ArrowDownToLine className="size-3.5" />
          还款
        </Button>
      </DialogTrigger>
      <DialogContent className="max-h-[calc(100vh-2rem)] max-w-3xl overflow-y-auto sm:p-7">
        <DialogHeader>
          <DialogTitle>{account.name} · 还款</DialogTitle>
          <DialogDescription>
            资产账户转入信用账户，按转账处理，不计入支出。
          </DialogDescription>
        </DialogHeader>
        <CreditRepaymentForm
          accounts={accounts}
          creditAccountId={account.id}
          embedded
          onSaved={save}
        />
      </DialogContent>
    </Dialog>
  );
}

function InstallmentsDialog({
  account,
  accounts,
  onChanged,
}: {
  account: FinanceAccount;
  accounts: FinanceAccount[];
  onChanged: () => Promise<void>;
}) {
  const [open, setOpen] = useState(false);

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button type="button" size="sm" variant="outline" className="gap-1.5">
          <CalendarClock className="size-3.5" />
          分期计划
        </Button>
      </DialogTrigger>
      <DialogContent className="max-h-[calc(100vh-2rem)] max-w-3xl overflow-y-auto sm:p-7">
        <DialogHeader>
          <DialogTitle>{account.name} · 分期计划</DialogTitle>
          <DialogDescription>
            查看该信用账户下的分期，并登记下一期还款。
          </DialogDescription>
        </DialogHeader>
        {open ? (
          <CreditInstallmentPanel
            accounts={accounts}
            accountId={account.id}
            embedded
            onChanged={onChanged}
          />
        ) : null}
      </DialogContent>
    </Dialog>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0 border-b border-border/40 pb-2">
      <div className="text-[11px] text-muted-foreground">{label}</div>
      <div className="mt-1 truncate text-sm font-medium">{value}</div>
    </div>
  );
}
