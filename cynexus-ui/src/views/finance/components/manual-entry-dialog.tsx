import { Plus, RotateCw } from "lucide-react";
import { useCallback, useState } from "react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { notification } from "@/components/ui/notification";
import {
  fetchFinanceAccounts,
  fetchFinanceCategories,
  saveFinanceTransaction,
  type FinanceAccount,
  type FinanceCategory,
  type FinanceTransaction,
} from "@/services/finance/ledger";
import { QuickEntryForm } from "./quick-entry-form";

export function ManualEntryDialog({
  onSaved,
}: {
  onSaved?: () => Promise<void> | void;
}) {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [accounts, setAccounts] = useState<FinanceAccount[]>([]);
  const [categories, setCategories] = useState<FinanceCategory[]>([]);

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const [nextAccounts, nextCategories] = await Promise.all([
        fetchFinanceAccounts(false),
        fetchFinanceCategories(true),
      ]);
      setAccounts(nextAccounts);
      setCategories(nextCategories);
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : String(cause);
      setLoadError(message);
      notification.danger({
        title: "记账数据加载失败",
        description: message,
      });
    } finally {
      setLoading(false);
    }
  }, []);

  function handleOpenChange(nextOpen: boolean) {
    setOpen(nextOpen);
    if (nextOpen) void load();
  }

  async function save(payload: Record<string, unknown>) {
    await saveFinanceTransaction(
      payload as unknown as Partial<FinanceTransaction>,
    );
    notification.success({ title: "账目已入账" });
    setOpen(false);
    await onSaved?.();
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="min-h-9 shrink-0 gap-1.5 px-2.5 sm:px-3"
        >
          <Plus className="size-3.5" />
          手动入账
        </Button>
      </DialogTrigger>
      <DialogContent className="max-h-[calc(100vh-2rem)] max-w-3xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>手动入账</DialogTitle>
          <DialogDescription>
            记录收入、支出或账户之间的转账；保存后会立即更新余额和统计。
          </DialogDescription>
        </DialogHeader>
        {loading ? (
          <div className="space-y-3">
            <Skeleton className="h-10 w-full" />
            <div className="grid gap-3 sm:grid-cols-2">
              <Skeleton className="h-16" />
              <Skeleton className="h-16" />
              <Skeleton className="h-16" />
              <Skeleton className="h-16" />
            </div>
          </div>
        ) : loadError ? (
          <div className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-6 text-center dark:border-rose-900/50 dark:bg-rose-950/20">
            <p className="text-sm font-medium text-rose-700 dark:text-rose-300">
              记账所需的账户和分类加载失败
            </p>
            <p className="mt-1 text-xs leading-5 text-rose-700/80 dark:text-rose-300/80">
              {loadError}
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
        ) : (
          <QuickEntryForm
            embedded
            accounts={accounts}
            categories={categories}
            onSaved={save}
          />
        )}
      </DialogContent>
    </Dialog>
  );
}
