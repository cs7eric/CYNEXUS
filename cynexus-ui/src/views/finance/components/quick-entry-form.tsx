import { ArrowDownLeft, ArrowLeftRight, ArrowUpRight, Check, Plus } from "lucide-react";
import { useMemo, useState, type FormEvent, type ReactNode } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { FinanceAccount, FinanceCategory } from "@/services/finance/ledger";

type EntryType = "expense" | "income" | "transfer";

function localDateTime(value = new Date()) {
  const pad = (part: number) => String(part).padStart(2, "0");
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}`;
}

export function QuickEntryForm({
  accounts,
  categories,
  onSaved,
  embedded = false,
}: {
  accounts: FinanceAccount[];
  categories: FinanceCategory[];
  onSaved: (payload: Record<string, unknown>) => Promise<void>;
  embedded?: boolean;
}) {
  const [type, setType] = useState<EntryType>("expense");
  const [amount, setAmount] = useState("");
  const [accountId, setAccountId] = useState("");
  const [toAccountId, setToAccountId] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [occurredAt, setOccurredAt] = useState(localDateTime());
  const [merchant, setMerchant] = useState("");
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);

  const activeAccounts = useMemo(
    () => accounts.filter((item) => !item.archived),
    [accounts],
  );
  const activeCategories = useMemo(
    () =>
      categories.filter(
        (item) =>
          !item.archived &&
          (item.direction === type || item.direction === "both"),
      ),
    [categories, type],
  );

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const numericAmount = Number(amount);
    if (
      !Number.isFinite(numericAmount) ||
      numericAmount <= 0 ||
      !accountId ||
      !occurredAt ||
      (type === "transfer" ? !toAccountId : !categoryId) ||
      (type === "transfer" && accountId === toAccountId)
    ) {
      return;
    }
    setSaving(true);
    try {
      await onSaved({
        transactionType: type,
        amount: numericAmount,
        accountId,
        toAccountId: type === "transfer" ? toAccountId : null,
        categoryId: type === "transfer" ? null : categoryId,
        occurredAt: `${occurredAt}:00+08:00`,
        merchant: merchant.trim() || null,
        note: note.trim() || null,
      });
      setAmount("");
      setMerchant("");
      setNote("");
      setOccurredAt(localDateTime());
    } catch {
      // The parent owns the request error notification.
    } finally {
      setSaving(false);
    }
  }

  const types = [
    { value: "expense" as const, label: "支出", icon: ArrowUpRight },
    { value: "income" as const, label: "收入", icon: ArrowDownLeft },
    { value: "transfer" as const, label: "转账", icon: ArrowLeftRight },
  ];

  return (
    <section
      className={
        embedded
          ? "min-w-0"
          : "min-w-0 rounded-xl border border-border/70 bg-card p-4 shadow-sm shadow-black/[0.02]"
      }
    >
      {!embedded ? (
        <div className="mb-4 flex items-start gap-3">
          <span className="flex size-7 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary">
            <Plus className="size-4" />
          </span>
          <div>
            <h2 className="text-base font-semibold">手动记账</h2>
            <p className="mt-1 text-xs leading-5 text-muted-foreground">
              记录收入、支出或账户之间的转账；保存后会立即更新余额和统计。
            </p>
          </div>
        </div>
      ) : null}
      <form onSubmit={submit} className="space-y-3">
        <div className="grid grid-cols-3 gap-1 rounded-lg bg-muted/60 p-1">
          {types.map(({ value, label, icon: Icon }) => (
            <button
              key={value}
              type="button"
              onClick={() => {
                setType(value);
                if (value === "transfer") setCategoryId("");
                else setToAccountId("");
              }}
              className={`flex items-center justify-center gap-1.5 rounded-md px-2 py-2 text-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring ${type === value ? "bg-background font-medium text-foreground shadow-sm" : "text-muted-foreground hover:bg-background/60 hover:text-foreground"}`}
              aria-pressed={type === value}
            >
              <Icon className="size-3.5" />
              {label}
            </button>
          ))}
        </div>
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <Field label="金额">
            <Input
              required
              min="0.01"
              step="0.01"
              type="number"
              inputMode="decimal"
              placeholder="0.00"
              value={amount}
              onChange={(event) => setAmount(event.target.value)}
            />
          </Field>
          <Field label="发生时间">
            <Input
              required
              type="datetime-local"
              value={occurredAt}
              onChange={(event) => setOccurredAt(event.target.value)}
            />
          </Field>
          <Field label={type === "transfer" ? "转出账户" : "资金账户"}>
            <select
              required
              value={accountId}
              onChange={(event) => setAccountId(event.target.value)}
              className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="">选择账户</option>
              {activeAccounts.map((item) => (
                <option key={String(item.id)} value={String(item.id)}>
                  {item.name} · {item.nature === "credit" ? "信用" : "资产"}
                </option>
              ))}
            </select>
          </Field>
          {type === "transfer" ? (
            <Field label="转入账户">
              <select
                required
                value={toAccountId}
                onChange={(event) => setToAccountId(event.target.value)}
                className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                <option value="">选择账户</option>
                {activeAccounts
                  .filter((item) => String(item.id) !== accountId)
                  .map((item) => (
                    <option key={String(item.id)} value={String(item.id)}>
                      {item.name}
                    </option>
                  ))}
              </select>
            </Field>
          ) : (
            <Field label="分类">
              <select
                required
                value={categoryId}
                onChange={(event) => setCategoryId(event.target.value)}
                className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                <option value="">选择分类</option>
                {activeCategories.map((item) => (
                  <option key={String(item.id)} value={String(item.id)}>
                    {item.name}
                  </option>
                ))}
              </select>
            </Field>
          )}
          <Field label="商户 / 来源">
            <Input
              placeholder="例如：便利店"
              value={merchant}
              onChange={(event) => setMerchant(event.target.value)}
            />
          </Field>
          <Field label="备注">
            <Input
              placeholder="可选"
              value={note}
              onChange={(event) => setNote(event.target.value)}
            />
          </Field>
        </div>
        <div className="flex justify-end">
          <Button
            type="submit"
            disabled={saving || !activeAccounts.length}
            className="gap-2"
          >
            {saving ? "保存中" : <><Check className="size-4" />入账</>}
          </Button>
        </div>
      </form>
    </section>
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
