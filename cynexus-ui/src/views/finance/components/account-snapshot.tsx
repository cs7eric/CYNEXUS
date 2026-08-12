import { useState, type FormEvent, type ReactNode } from "react";
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
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { notification } from "@/components/ui/notification";
import {
  saveFinanceAccount,
  type FinanceAccount,
} from "@/services/finance/ledger";
import { cn } from "@/lib/utils";
import { useIsMobile } from "@/hooks/use-mobile";
import { accountTypeLabel, formatMoney } from "../lib/format";
import { CreditAccountActions } from "./credit-account-actions";

const accountTypes: Array<{
  value: FinanceAccount["accountType"];
  label: string;
}> = [
  { value: "cash", label: "现金" },
  { value: "bank", label: "银行卡" },
  { value: "e_wallet", label: "电子钱包" },
  { value: "huabei", label: "花呗" },
  { value: "baitiao", label: "白条" },
  { value: "douyin_monthly", label: "抖音月付" },
  { value: "credit_card", label: "信用卡" },
  { value: "investment", label: "投资账户" },
  { value: "other", label: "其他" },
];

const creditAccountTypes = new Set<FinanceAccount["accountType"]>([
  "huabei",
  "baitiao",
  "douyin_monthly",
  "credit_card",
]);
export function AccountSnapshot({
  accounts,
  onChanged,
}: {
  accounts: FinanceAccount[];
  onChanged?: () => Promise<void> | void;
}) {
  const isMobile = useIsMobile();
  const [overrides, setOverrides] = useState<Record<number, FinanceAccount>>(
    {},
  );
  const [selected, setSelected] = useState<FinanceAccount | null>(null);
  const [editing, setEditing] = useState<FinanceAccount | null>(null);
  const [saving, setSaving] = useState(false);
  const active = accounts
    .filter((item) => !item.archived)
    .map((item) => overrides[item.id] ?? item);
  const assets = active.filter((item) => item.nature === "asset");
  const creditAccounts = active.filter((item) => item.nature === "credit");
  const assetTotal = assets.reduce((sum, item) => sum + (item.balance ?? 0), 0);
  const creditTotal = creditAccounts.reduce(
    (sum, item) => sum + (item.balance ?? 0),
    0,
  );

  function openDetails(account: FinanceAccount) {
    setSelected(account);
    setEditing(null);
  }

  function beginEdit(account: FinanceAccount) {
    setSelected(account);
    setEditing(account);
  }

  function closeDrawer(open: boolean) {
    if (!open) {
      setSelected(null);
      setEditing(null);
    }
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editing) return;
    const form = new FormData(event.currentTarget);
    const accountType = String(
      form.get("accountType"),
    ) as FinanceAccount["accountType"];
    const name = String(form.get("name") ?? "").trim();
    const openingBalance = Number(form.get("openingBalance"));
    if (!name || !Number.isFinite(openingBalance) || openingBalance < 0) {
      notification.danger({
        title: "请补全账户信息",
        description: "账户名称和期初余额必须填写正确。",
      });
      return;
    }
    const credit = creditAccountTypes.has(accountType);
    const payload = {
      id: editing.id,
      name,
      accountType,
      nature: credit ? "credit" : "asset",
      currency: editing.currency,
      openingBalance,
      creditLimit: credit ? numberOrNull(form.get("creditLimit")) : null,
      billingDay: credit ? integerOrNull(form.get("billingDay")) : null,
      dueDay: credit ? integerOrNull(form.get("dueDay")) : null,
      color: editing.color,
      notes: editing.notes,
    } satisfies Partial<FinanceAccount>;
    setSaving(true);
    try {
      const saved = await saveFinanceAccount(payload);
      setOverrides((current) => ({ ...current, [saved.id]: saved }));
      setSelected(saved);
      setEditing(null);
      notification.success({ title: "账户已更新" });
    } catch (cause) {
      notification.danger({
        title: "账户更新失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="min-w-0 overflow-hidden rounded-xl border border-border/70 bg-card">
      <div className="border-b border-border/60 px-4 py-3">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="text-sm font-semibold">账户余额</h2>
            <p className="mt-0.5 text-xs text-muted-foreground">
              资产与信用账户分开看，信用余额代表当前待还。
            </p>
          </div>
          <span className="shrink-0 text-xs text-muted-foreground">
            {active.length} 个启用
          </span>
        </div>
      </div>
      {active.length ? (
        <>
          <div className="grid grid-cols-2 divide-x divide-border/60 border-b border-border/60">
            <div className="px-4 py-3">
              <div className="text-[11px] text-muted-foreground">资产余额</div>
              <div className="mt-1 font-mono text-sm font-semibold tabular-nums text-emerald-700 dark:text-emerald-300">
                {formatMoney(assetTotal)}
              </div>
            </div>
            <div className="px-4 py-3">
              <div className="text-[11px] text-muted-foreground">信用待还</div>
              <div className="mt-1 font-mono text-sm font-semibold tabular-nums text-rose-600 dark:text-rose-400">
                {formatMoney(creditTotal)}
              </div>
            </div>
          </div>
          <div className="space-y-5 px-4 pb-4 pt-4">
            {assets.length ? (
              <AccountGroup
                label="资产账户"
                count={assets.length}
                accounts={assets}
                onOpen={openDetails}
                onEdit={beginEdit}
              />
            ) : null}
            {creditAccounts.length ? (
              <AccountGroup
                label="信用账户"
                count={creditAccounts.length}
                accounts={creditAccounts}
                credit
                onOpen={openDetails}
                onEdit={beginEdit}
              />
            ) : null}
          </div>
        </>
      ) : (
        <div className="py-10 text-center text-xs text-muted-foreground">
          还没有账户。先建立一个账户，记账后余额会自动更新。
        </div>
      )}
      <Drawer
        open={selected != null}
        onOpenChange={closeDrawer}
        direction={isMobile ? "bottom" : "right"}
      >
        {selected ? (
          <DrawerContent className="data-[vaul-drawer-direction=right]:w-full data-[vaul-drawer-direction=right]:sm:max-w-xl data-[vaul-drawer-direction=bottom]:max-h-[88vh]">
            <DrawerHeader className="border-b border-border/60">
              <div className="flex items-start gap-3">
                <AccountIcon account={selected} size="lg" />
                <div className="min-w-0">
                  <DrawerTitle className="truncate text-base">
                    {editing ? "编辑账户" : selected.name}
                  </DrawerTitle>
                  <DrawerDescription className="mt-1">
                    {editing
                      ? "保存后会立即更新账户余额和资产结构。"
                      : `${accountTypeLabel(selected.accountType)} · ${selected.nature === "credit" ? "信用账户" : "资产账户"}`}
                  </DrawerDescription>
                </div>
              </div>
            </DrawerHeader>
            <ScrollArea className="min-h-0 flex-1">
              {editing ? (
                <AccountEditForm account={editing} onSubmit={save} />
              ) : (
                <>
                  <AccountDetails
                    account={selected}
                    onEdit={() => beginEdit(selected)}
                  />
                  {selected.nature === "credit" ? (
                    <CreditAccountActions
                      account={selected}
                      accounts={active}
                      onChanged={onChanged}
                    />
                  ) : null}
                </>
              )}
            </ScrollArea>
            <DrawerFooter className="border-t border-border/60 bg-background/95 sm:flex-row sm:justify-end">
              {editing ? (
                <>
                  <Button
                    type="button"
                    variant="ghost"
                    onClick={() => setEditing(null)}
                    disabled={saving}
                  >
                    取消
                  </Button>
                  <Button
                    type="submit"
                    form="finance-account-edit"
                    disabled={saving}
                  >
                    {saving ? "保存中" : "保存修改"}
                  </Button>
                </>
              ) : (
                <DrawerClose asChild>
                  <Button type="button" variant="ghost">
                    关闭
                  </Button>
                </DrawerClose>
              )}
            </DrawerFooter>
          </DrawerContent>
        ) : null}
      </Drawer>
    </section>
  );
}

function AccountGroup({
  label,
  count,
  accounts,
  credit = false,
  onOpen,
  onEdit,
}: {
  label: string;
  count: number;
  accounts: FinanceAccount[];
  credit?: boolean;
  onOpen: (account: FinanceAccount) => void;
  onEdit: (account: FinanceAccount) => void;
}) {
  return (
    <div>
      <div className="mb-2 flex items-center justify-between gap-3">
        <div className="flex items-center gap-2 text-[11px] font-medium text-muted-foreground">
          <span
            className={cn(
              "size-1.5 rounded-full",
              credit ? "bg-rose-500" : "bg-emerald-500",
            )}
          />
          {label}
        </div>
        <span className="text-[10px] tabular-nums text-muted-foreground">
          {count} 个
        </span>
      </div>
      <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
        {accounts.map((account) => (
          <AccountCard
            key={account.id}
            account={account}
            onOpen={onOpen}
            onEdit={onEdit}
          />
        ))}
      </div>
    </div>
  );
}

function AccountCard({
  account,
  onOpen,
  onEdit,
}: {
  account: FinanceAccount;
  onOpen: (account: FinanceAccount) => void;
  onEdit: (account: FinanceAccount) => void;
}) {
  const credit = account.nature === "credit";
  const utilization =
    credit && account.creditLimit
      ? Math.min(
          100,
          Math.max(0, ((account.balance ?? 0) / account.creditLimit) * 100),
        )
      : null;
  return (
    <button
      type="button"
      className={cn(
        "group min-w-0 rounded-lg border p-3 text-left transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring",
        credit
          ? "border-rose-200/80 bg-rose-50/35 hover:border-rose-300 hover:bg-rose-50/70 dark:border-rose-900/50 dark:bg-rose-950/10 dark:hover:bg-rose-950/25"
          : "border-border/70 bg-background/60 hover:border-emerald-300 hover:bg-emerald-50/35 dark:hover:border-emerald-800 dark:hover:bg-emerald-950/15",
      )}
      onClick={() => onOpen(account)}
      onDoubleClick={() => onEdit(account)}
      title="点击查看详情，双击编辑"
    >
      <div className="flex min-w-0 items-center justify-between gap-3">
        <div className="flex min-w-0 items-center gap-2.5">
          <AccountIcon account={account} />
          <div className="min-w-0">
            <div className="truncate text-xs font-medium">{account.name}</div>
            <div className="mt-0.5 truncate text-[10px] text-muted-foreground">
              {accountTypeLabel(account.accountType)}
            </div>
          </div>
        </div>
        <span
          className={cn(
            "shrink-0 text-[10px]",
            credit
              ? "text-rose-600 dark:text-rose-400"
              : "text-emerald-700 dark:text-emerald-300",
          )}
        >
          {credit ? "待还" : account.currency}
        </span>
      </div>
      <div className="mt-4 flex items-end justify-between gap-2">
        <span
          className={cn(
            "font-mono text-base font-semibold tabular-nums",
            credit ? "text-rose-600 dark:text-rose-400" : "text-foreground",
          )}
        >
          {formatMoney(account.balance)}
        </span>
        {credit && account.creditLimit != null ? (
          <span className="text-[10px] text-muted-foreground">
            额度 {formatMoney(account.creditLimit)}
          </span>
        ) : null}
      </div>
      {utilization != null ? (
        <div className="mt-2 h-1 overflow-hidden rounded-full bg-rose-100 dark:bg-rose-950/60">
          <div
            className="h-full rounded-full bg-rose-400 transition-[width] dark:bg-rose-500"
            style={{ width: `${utilization}%` }}
          />
        </div>
      ) : null}
    </button>
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
      className={cn(
        "flex shrink-0 items-center justify-center rounded-md",
        size === "lg" ? "size-10" : "size-8",
        credit
          ? "bg-rose-500/10 text-rose-600 dark:text-rose-400"
          : "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400",
      )}
    >
      <Icon className={size === "lg" ? "size-5" : "size-4"} />
    </span>
  );
}

function AccountDetails({
  account,
  onEdit,
}: {
  account: FinanceAccount;
  onEdit: () => void;
}) {
  const credit = account.nature === "credit";
  const utilization =
    credit && account.creditLimit
      ? ((account.balance ?? 0) / account.creditLimit) * 100
      : null;
  return (
    <div className="cursor-default px-4 py-5 sm:px-6" onDoubleClick={onEdit}>
      <div className="grid gap-x-8 gap-y-4 sm:grid-cols-2">
        <DetailField
          label="当前余额"
          value={
            <span
              className={cn(
                "font-mono text-base font-semibold tabular-nums",
                credit
                  ? "text-rose-600 dark:text-rose-400"
                  : "text-emerald-700 dark:text-emerald-300",
              )}
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
              <DetailField
                label="额度使用率"
                value={`${utilization.toFixed(1)}%`}
              />
            ) : null}
            {account.billingDay != null ? (
              <DetailField
                label="账单日"
                value={`每月 ${account.billingDay} 日`}
              />
            ) : null}
            {account.dueDay != null ? (
              <DetailField label="还款日" value={`每月 ${account.dueDay} 日`} />
            ) : null}
          </>
        ) : null}
        <DetailField label="币种" value={account.currency} />
      </div>
      {account.notes ? (
        <section className="mt-6 border-t border-border/60 pt-4">
          <h3 className="text-xs font-medium text-muted-foreground">备注</h3>
          <p className="mt-2 whitespace-pre-wrap text-sm leading-6">
            {account.notes}
          </p>
        </section>
      ) : null}
    </div>
  );
}

function AccountEditForm({
  account,
  onSubmit,
}: {
  account: FinanceAccount;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}) {
  const [accountType, setAccountType] = useState(account.accountType);
  const credit = creditAccountTypes.has(accountType);
  return (
    <form
      id="finance-account-edit"
      onSubmit={onSubmit}
      className="space-y-4 px-4 py-5 sm:px-6"
    >
      <Field label="账户名称">
        <Input name="name" required defaultValue={account.name} />
      </Field>
      <Field label="账户类型">
        <Select
          name="accountType"
          value={accountType}
          onValueChange={(value) =>
            setAccountType(value as FinanceAccount["accountType"])
          }
        >
          <SelectTrigger className="w-full bg-muted/60">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {accountTypes.map((item) => (
              <SelectItem key={item.value} value={item.value}>
                {item.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </Field>
      <Field label="账户性质">
        <div className="flex h-9 items-center rounded-md bg-muted/40 px-3 text-sm text-muted-foreground">
          {credit ? "信用" : "资产"}（根据账户类型自动确定）
        </div>
      </Field>
      <Field label="期初余额">
        <Input
          name="openingBalance"
          required
          type="number"
          min="0"
          step="0.01"
          defaultValue={account.openingBalance ?? 0}
        />
      </Field>
      {credit ? (
        <div className="grid grid-cols-3 gap-2">
          <Field label="额度">
            <Input
              name="creditLimit"
              type="number"
              min="0"
              step="0.01"
              defaultValue={account.creditLimit ?? ""}
            />
          </Field>
          <Field label="账单日">
            <Input
              name="billingDay"
              type="number"
              min="1"
              max="31"
              defaultValue={account.billingDay ?? ""}
            />
          </Field>
          <Field label="还款日">
            <Input
              name="dueDay"
              type="number"
              min="1"
              max="31"
              defaultValue={account.dueDay ?? ""}
            />
          </Field>
        </div>
      ) : null}
    </form>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block space-y-1.5 text-xs font-medium">
      <span>{label}</span>
      {children}
    </label>
  );
}

function DetailField({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="min-w-0 border-b border-border/40 pb-3">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="mt-1 truncate text-sm font-medium">{value}</div>
    </div>
  );
}

function numberOrNull(value: FormDataEntryValue | null) {
  if (value == null || String(value).trim() === "") return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : null;
}

function integerOrNull(value: FormDataEntryValue | null) {
  const parsed = numberOrNull(value);
  return parsed == null ? null : Math.round(parsed);
}
