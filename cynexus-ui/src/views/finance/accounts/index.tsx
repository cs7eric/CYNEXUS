import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  Archive,
  ArchiveRestore,
  CreditCard,
  Landmark,
  Plus,
  Trash2,
  Wallet,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Input } from "@/components/ui/input";
import { notification } from "@/components/ui/notification";
import { Skeleton } from "@/components/ui/skeleton";
import { WorkspaceShell } from "@/layout/workspace-shell";
import {
  archiveFinanceAccount,
  archiveFinanceCategory,
  fetchFinanceAccounts,
  fetchFinanceCategories,
  restoreFinanceAccount,
  saveFinanceAccount,
  saveFinanceCategory,
  type FinanceAccount,
  type FinanceCategory,
} from "@/services/finance/ledger";
import { FinanceAccountDrawer } from "../components/account-drawer";
import { FinancePageHeader } from "../components/page-header";
import { accountTypeLabel, formatMoney } from "../lib/format";

const accountTypes = [
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
const creditAccountTypes = new Set([
  "huabei",
  "baitiao",
  "douyin_monthly",
  "credit_card",
]);

export default function FinanceAccountsPage() {
  const [accounts, setAccounts] = useState<FinanceAccount[]>([]);
  const [categories, setCategories] = useState<FinanceCategory[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [account, setAccount] = useState({
    name: "",
    accountType: "bank",
    openingBalance: "",
    creditLimit: "",
    billingDay: "",
    dueDay: "",
  });
  const [category, setCategory] = useState({ name: "", direction: "expense" });
  const [categoryToArchive, setCategoryToArchive] =
    useState<FinanceCategory | null>(null);
  const [selectedAccountId, setSelectedAccountId] = useState<number | null>(
    null,
  );

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [nextAccounts, nextCategories] = await Promise.all([
        fetchFinanceAccounts(true),
        fetchFinanceCategories(true),
      ]);
      setAccounts(nextAccounts);
      setCategories(nextCategories);
    } catch (cause) {
      notification.danger({
        title: "账户数据加载失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void load();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  async function addAccount(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    try {
      const credit = creditAccountTypes.has(account.accountType);
      await saveFinanceAccount({
        name: account.name,
        accountType: account.accountType as FinanceAccount["accountType"],
        nature: credit ? "credit" : "asset",
        openingBalance: account.openingBalance
          ? Number(account.openingBalance)
          : 0,
        creditLimit: account.creditLimit ? Number(account.creditLimit) : null,
        billingDay: account.billingDay ? Number(account.billingDay) : null,
        dueDay: account.dueDay ? Number(account.dueDay) : null,
        currency: "CNY",
      });
      setAccount({
        name: "",
        accountType: "bank",
        openingBalance: "",
        creditLimit: "",
        billingDay: "",
        dueDay: "",
      });
      notification.success({ title: "账户已创建" });
      await load();
    } catch (cause) {
      notification.danger({
        title: "账户创建失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    } finally {
      setSaving(false);
    }
  }

  async function addCategory(event: FormEvent) {
    event.preventDefault();
    try {
      await saveFinanceCategory({
        name: category.name,
        direction: category.direction as FinanceCategory["direction"],
      });
      setCategory({ name: "", direction: "expense" });
      notification.success({ title: "分类已创建" });
      await load();
    } catch (cause) {
      notification.danger({
        title: "分类创建失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    }
  }

  async function archive(id: number) {
    try {
      await archiveFinanceAccount(id);
      notification.success({ title: "账户已归档" });
      await load();
    } catch (cause) {
      notification.danger({
        title: "账户归档失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    }
  }

  async function removeCategory() {
    if (!categoryToArchive) return;
    try {
      await archiveFinanceCategory(categoryToArchive.id);
      setCategoryToArchive(null);
      notification.success({ title: "分类已归档" });
      await load();
    } catch (cause) {
      notification.danger({
        title: "分类归档失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    }
  }

  async function restore(id: number) {
    try {
      await restoreFinanceAccount(id);
      notification.success({ title: "账户已恢复" });
      await load();
    } catch (cause) {
      notification.danger({
        title: "账户恢复失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    }
  }

  const activeAccounts = accounts.filter((item) => !item.archived);
  const assetTotal = activeAccounts
    .filter((item) => item.nature === "asset")
    .reduce((sum, item) => sum + (item.balance ?? 0), 0);
  const creditTotal = activeAccounts
    .filter((item) => item.nature === "credit")
    .reduce((sum, item) => sum + (item.balance ?? 0), 0);
  const netAssets = assetTotal - creditTotal;

  return (
    <WorkspaceShell sectionLabel="Finance" pageTitle="Accounts">
      <FinancePageHeader
        current="accounts"
        description="管理资金账户和信用账户，余额会随账目自动更新。"
        onRefresh={() => void load()}
        loading={loading}
        onManualEntrySaved={() => void load()}
      />
      <section className="grid grid-cols-2 overflow-hidden rounded-xl border border-border/70 bg-card sm:grid-cols-4 sm:divide-x sm:divide-border/60">
        <AccountMetric
          label="资产余额"
          value={assetTotal}
          tone="text-emerald-600 dark:text-emerald-400"
          detail={`${activeAccounts.filter((item) => item.nature === "asset").length} 个账户`}
        />
        <AccountMetric
          label="负债余额"
          value={creditTotal}
          tone="text-rose-600 dark:text-rose-400"
          detail="信用账户当前待还"
        />
        <AccountMetric
          label="净资产"
          value={netAssets}
          tone={
            netAssets >= 0
              ? "text-indigo-600 dark:text-indigo-400"
              : "text-amber-600 dark:text-amber-400"
          }
          detail="资产 - 负债"
        />
        <AccountMetric
          label="账户总数"
          value={activeAccounts.length}
          detail="包含资产与信用"
          count
        />
      </section>
      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_340px]">
        <section className="min-w-0 overflow-hidden rounded-xl border border-border/70 bg-card">
          <div className="flex items-center justify-between gap-3 border-b border-border/60 px-4 py-3">
            <div>
              <h2 className="text-sm font-semibold">账户列表</h2>
              <p className="mt-0.5 text-xs text-muted-foreground">
                启用账户 {activeAccounts.length} 个
              </p>
            </div>
            <span className="text-xs text-muted-foreground">CNY</span>
          </div>
          {loading ? (
            <div className="space-y-3 p-4">
              <Skeleton className="h-16" />
              <Skeleton className="h-16" />
              <Skeleton className="h-16" />
            </div>
          ) : accounts.length ? (
            <div className="divide-y divide-border/60 px-4">
              {accounts.map((item) => (
                <AccountRow
                  key={item.id}
                  item={item}
                  onOpen={() => setSelectedAccountId(item.id)}
                  onArchive={archive}
                  onRestore={restore}
                />
              ))}
            </div>
          ) : (
            <div className="px-4 py-14 text-center">
              <Wallet className="mx-auto size-8 text-muted-foreground/50" />
              <p className="mt-3 text-sm font-medium">还没有账户</p>
              <p className="mt-1 text-xs text-muted-foreground">
                从右侧添加你的第一个资金账户。
              </p>
            </div>
          )}
        </section>
        <section className="min-w-0 rounded-xl border border-border/70 bg-muted/25 p-4">
          <div className="mb-4 flex items-start gap-3">
            <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary">
              <Plus className="size-4" />
            </span>
            <div>
              <h2 className="text-sm font-semibold">新增账户</h2>
              <p className="mt-0.5 text-xs text-muted-foreground">
                花呗、白条、抖音月付和信用卡会自动记为信用性质。
              </p>
            </div>
          </div>
          <form onSubmit={addAccount} className="space-y-3">
            <Field label="名称">
              <Input
                required
                value={account.name}
                onChange={(event) =>
                  setAccount({ ...account, name: event.target.value })
                }
                placeholder="例如：招商银行信用卡"
              />
            </Field>
            <Field label="类型">
              <select
                value={account.accountType}
                onChange={(event) =>
                  setAccount({ ...account, accountType: event.target.value })
                }
                className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm outline-none focus-visible:ring-1 focus-visible:ring-ring"
              >
                {accountTypes.map((item) => (
                  <option key={item.value} value={item.value}>
                    {item.label}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="性质">
              <div className="flex h-9 items-center rounded-md border border-input bg-muted/40 px-3 text-sm text-muted-foreground">
                {creditAccountTypes.has(account.accountType) ? "信用" : "资产"}
              </div>
            </Field>
            <Field label="期初余额">
              <Input
                type="number"
                min="0"
                step="0.01"
                value={account.openingBalance}
                onChange={(event) =>
                  setAccount({ ...account, openingBalance: event.target.value })
                }
                placeholder="信用账户填写当前待还金额"
              />
            </Field>
            {creditAccountTypes.has(account.accountType) ? (
              <div className="grid grid-cols-3 gap-2">
                <Field label="额度">
                  <Input
                    type="number"
                    min="0"
                    step="0.01"
                    value={account.creditLimit}
                    onChange={(event) =>
                      setAccount({
                        ...account,
                        creditLimit: event.target.value,
                      })
                    }
                    placeholder="可选"
                  />
                </Field>
                <Field label="账单日">
                  <Input
                    type="number"
                    min="1"
                    max="31"
                    value={account.billingDay}
                    onChange={(event) =>
                      setAccount({ ...account, billingDay: event.target.value })
                    }
                    placeholder="日"
                  />
                </Field>
                <Field label="还款日">
                  <Input
                    type="number"
                    min="1"
                    max="31"
                    value={account.dueDay}
                    onChange={(event) =>
                      setAccount({ ...account, dueDay: event.target.value })
                    }
                    placeholder="日"
                  />
                </Field>
              </div>
            ) : null}
            <Button
              type="submit"
              disabled={saving}
              className="mt-2 w-full gap-2"
            >
              <Plus className="size-4" />
              {saving ? "创建中" : "创建账户"}
            </Button>
          </form>
        </section>
      </div>
      <section className="rounded-xl border border-border/70 bg-card p-4">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <h2 className="text-sm font-semibold">记账分类</h2>
            <p className="mt-0.5 text-xs text-muted-foreground">
              已补充餐饮、交通、住房、健康、社交、保险和常见收入分类；还可以添加自定义分类。
            </p>
          </div>
          <form
            onSubmit={addCategory}
            className="flex flex-wrap items-end gap-2"
          >
            <Field label="分类名称" compact>
              <Input
                required
                value={category.name}
                onChange={(event) =>
                  setCategory({ ...category, name: event.target.value })
                }
                placeholder="例如：宠物"
              />
            </Field>
            <Field label="方向" compact>
              <select
                value={category.direction}
                onChange={(event) =>
                  setCategory({ ...category, direction: event.target.value })
                }
                className="h-9 rounded-md border border-input bg-background px-3 text-sm"
              >
                <option value="expense">支出</option>
                <option value="income">收入</option>
                <option value="both">收支</option>
              </select>
            </Field>
            <Button type="submit" variant="outline" className="gap-2">
              <Plus className="size-4" />
              添加
            </Button>
          </form>
        </div>
        <div className="mt-4 grid gap-2 border-t border-border/60 pt-4 sm:grid-cols-2 xl:grid-cols-3">
          {categories
            .filter((item) => !item.archived)
            .map((item) => (
              <div
                key={item.id}
                className="flex items-center justify-between gap-2 rounded-md border border-border/70 bg-muted/25 px-2.5 py-1.5 text-xs"
              >
                <span className="truncate">
                  {item.name}
                  <span className="ml-1.5 text-muted-foreground">
                    {item.direction === "income"
                      ? "收入"
                      : item.direction === "expense"
                        ? "支出"
                        : "收支"}
                  </span>
                </span>
                {item.system ? (
                  <span className="shrink-0 text-[10px] text-muted-foreground">
                    系统
                  </span>
                ) : (
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon-sm"
                    title="归档分类"
                    aria-label={`归档分类 ${item.name}`}
                    onClick={() => setCategoryToArchive(item)}
                  >
                    <Trash2 className="size-3.5 text-muted-foreground" />
                  </Button>
                )}
              </div>
            ))}
        </div>
      </section>
      <ConfirmDialog
        open={categoryToArchive != null}
        onOpenChange={(open) => {
          if (!open) setCategoryToArchive(null);
        }}
        title="归档这个分类？"
        description="历史账目仍会保留，但新账目将不再默认显示这个分类。"
        confirmText="归档分类"
        destructive
        onConfirm={removeCategory}
      />
      <FinanceAccountDrawer
        account={
          selectedAccountId == null
            ? null
            : accounts.find((item) => item.id === selectedAccountId) ?? null
        }
        accounts={accounts}
        onClose={() => setSelectedAccountId(null)}
        onChanged={() => void load()}
      />
    </WorkspaceShell>
  );
}

function AccountMetric({
  label,
  value,
  tone = "text-foreground",
  detail,
  count = false,
}: {
  label: string;
  value: number;
  tone?: string;
  detail: string;
  count?: boolean;
}) {
  return (
    <div className="px-4 py-4">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div
        className={`mt-1.5 text-xl font-semibold tabular-nums tracking-tight ${tone}`}
      >
        {count ? value : formatMoney(value)}
      </div>
      <div className="mt-1 text-xs text-muted-foreground">{detail}</div>
    </div>
  );
}

function AccountRow({
  item,
  onOpen,
  onArchive,
  onRestore,
}: {
  item: FinanceAccount;
  onOpen: () => void;
  onArchive: (id: number) => Promise<void>;
  onRestore: (id: number) => Promise<void>;
}) {
  const Icon =
    item.nature === "credit"
      ? CreditCard
      : item.accountType === "bank"
        ? Landmark
        : Wallet;
  return (
    <div
      className={`flex items-center justify-between gap-3 py-3.5 ${item.archived ? "opacity-50" : ""}`}
    >
      <button
        type="button"
        className="flex min-w-0 flex-1 items-center gap-3 rounded-md text-left outline-none focus-visible:ring-1 focus-visible:ring-ring"
        onClick={onOpen}
        title="查看账户详情"
      >
        <span
          className={`flex size-9 shrink-0 items-center justify-center rounded-md ${item.nature === "credit" ? "bg-rose-500/10 text-rose-600 dark:text-rose-400" : "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"}`}
        >
          <Icon className="size-4" />
        </span>
        <div className="min-w-0">
          <div className="truncate text-sm font-medium">{item.name}</div>
          <div className="mt-0.5 text-xs text-muted-foreground">
            {accountTypeLabel(item.accountType)} ·{" "}
            {item.nature === "credit" ? "信用" : "资产"}
            {item.archived ? " · 已归档" : ""}
          </div>
          {item.notes ? (
            <div
              className="mt-1 max-w-[34rem] truncate text-[11px] text-muted-foreground"
              title={item.notes}
            >
              {item.notes}
            </div>
          ) : null}
        </div>
      </button>
      <div className="flex items-center gap-2">
        <div className="text-right">
          <span
            className={`font-mono text-sm tabular-nums ${item.nature === "credit" ? "text-rose-600 dark:text-rose-400" : "text-foreground"}`}
          >
            {formatMoney(item.balance)}
          </span>
          {item.nature === "credit" && item.creditLimit != null ? (
            <div className="text-[11px] text-muted-foreground">
              额度 {formatMoney(item.creditLimit)}
            </div>
          ) : null}
        </div>
        {item.archived ? (
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            title="恢复账户"
            aria-label={`恢复 ${item.name}`}
            onClick={() => void onRestore(item.id)}
          >
            <ArchiveRestore className="size-4" />
          </Button>
        ) : (
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            title="归档账户"
            aria-label={`归档 ${item.name}`}
            onClick={() => void onArchive(item.id)}
          >
            <Archive className="size-4" />
          </Button>
        )}
      </div>
    </div>
  );
}

function Field({
  label,
  compact = false,
  children,
}: {
  label: string;
  compact?: boolean;
  children: React.ReactNode;
}) {
  return (
    <label
      className={`block space-y-1.5 text-xs font-medium ${compact ? "min-w-[120px]" : ""}`}
    >
      <span>{label}</span>
      {children}
    </label>
  );
}
