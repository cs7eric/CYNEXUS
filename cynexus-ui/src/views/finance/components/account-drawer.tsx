import { CreditCard, Landmark, Wallet } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Drawer,
  DrawerClose,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer";
import { ScrollArea } from "@/components/ui/scroll-area";
import { useIsMobile } from "@/hooks/use-mobile";
import type { FinanceAccount } from "@/services/finance/ledger";
import { accountTypeLabel, formatMoney } from "../lib/format";
import { CreditAccountActions } from "./credit-account-actions";

export function FinanceAccountDrawer({
  account,
  accounts,
  onClose,
  onChanged,
}: {
  account: FinanceAccount | null;
  accounts: FinanceAccount[];
  onClose: () => void;
  onChanged?: () => Promise<void> | void;
}) {
  const isMobile = useIsMobile();

  return (
    <Drawer
      open={account != null}
      onOpenChange={(open) => {
        if (!open) onClose();
      }}
      direction={isMobile ? "bottom" : "right"}
    >
      {account ? (
        <DrawerContent className="data-[vaul-drawer-direction=right]:w-full data-[vaul-drawer-direction=right]:sm:max-w-xl data-[vaul-drawer-direction=bottom]:max-h-[88vh]">
          <DrawerHeader className="border-b border-border/60">
            <div className="flex items-start gap-3">
              <AccountIcon account={account} size="lg" />
              <div className="min-w-0">
                <DrawerTitle className="truncate text-base">
                  {account.name}
                </DrawerTitle>
                <DrawerDescription className="mt-1">
                  {accountTypeLabel(account.accountType)} ·{" "}
                  {account.nature === "credit" ? "信用账户" : "资产账户"}
                </DrawerDescription>
              </div>
            </div>
          </DrawerHeader>
          <ScrollArea className="min-h-0 flex-1">
            <AccountDetails account={account} />
            {account.nature === "credit" ? (
              <CreditAccountActions
                account={account}
                accounts={accounts}
                onChanged={onChanged}
              />
            ) : (
              <div className="border-t border-border/60 px-4 py-5 sm:px-6">
                <div className="rounded-lg bg-muted/45 px-4 py-3 text-xs leading-5 text-muted-foreground">
                  资产账户可作为信用账户还款的付款账户。打开信用账户即可登记还款和查看分期计划。
                </div>
              </div>
            )}
          </ScrollArea>
          <DrawerFooter className="border-t border-border/60 bg-background/95 sm:flex-row sm:justify-end">
            <DrawerClose asChild>
              <Button type="button" variant="ghost">
                关闭
              </Button>
            </DrawerClose>
          </DrawerFooter>
        </DrawerContent>
      ) : null}
    </Drawer>
  );
}

function AccountDetails({ account }: { account: FinanceAccount }) {
  const credit = account.nature === "credit";
  const utilization =
    credit && account.creditLimit
      ? ((account.balance ?? 0) / account.creditLimit) * 100
      : null;

  return (
    <div className="grid gap-x-8 gap-y-4 px-4 py-5 sm:grid-cols-2 sm:px-6">
      <DetailField
        label={credit ? "当前待还" : "当前余额"}
        value={
          <span
            className={`font-mono text-base font-semibold tabular-nums ${credit ? "text-rose-600 dark:text-rose-400" : "text-emerald-700 dark:text-emerald-300"}`}
          >
            {formatMoney(account.balance)} {account.currency}
          </span>
        }
      />
      <DetailField
        label="账户性质"
        value={credit ? "信用账户" : "资产账户"}
      />
      <DetailField
        label="账户类型"
        value={accountTypeLabel(account.accountType)}
      />
      <DetailField
        label="期初余额"
        value={`${formatMoney(account.openingBalance)} ${account.currency}`}
      />
      {credit ? (
        <>
          <DetailField
            label="信用额度"
            value={
              account.creditLimit == null
                ? "未设置"
                : `${formatMoney(account.creditLimit)} ${account.currency}`
            }
          />
          {utilization != null ? (
            <DetailField label="额度使用率" value={`${utilization.toFixed(1)}%`} />
          ) : null}
          {account.billingDay != null ? (
            <DetailField label="账单日" value={`每月 ${account.billingDay} 日`} />
          ) : null}
          {account.dueDay != null ? (
            <DetailField label="还款日" value={`每月 ${account.dueDay} 日`} />
          ) : null}
        </>
      ) : null}
      <DetailField label="币种" value={account.currency} />
      {account.notes ? (
        <div className="sm:col-span-2">
          <DetailField label="备注" value={account.notes} />
        </div>
      ) : null}
    </div>
  );
}

function DetailField({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="min-w-0 border-b border-border/40 pb-3">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="mt-1 truncate text-sm font-medium">{value}</div>
    </div>
  );
}

function AccountIcon({
  account,
  size = "sm",
}: {
  account: FinanceAccount;
  size?: "sm" | "lg";
}) {
  const Icon =
    account.nature === "credit"
      ? CreditCard
      : account.accountType === "bank"
        ? Landmark
        : Wallet;
  const credit = account.nature === "credit";
  return (
    <span
      className={`flex shrink-0 items-center justify-center rounded-md ${size === "lg" ? "size-10" : "size-8"} ${credit ? "bg-rose-500/10 text-rose-600 dark:text-rose-400" : "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"}`}
    >
      <Icon className={size === "lg" ? "size-5" : "size-4"} />
    </span>
  );
}
