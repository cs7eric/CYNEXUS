import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, Camera, CalendarDays } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { notification } from "@/components/ui/notification";
import { Skeleton } from "@/components/ui/skeleton";
import { WorkspaceShell } from "@/layout/workspace-shell";
import {
  archiveFinanceTransaction,
  fetchFinanceOverview,
  type FinanceOverview,
} from "@/services/finance/ledger";
import { FinancePageHeader } from "../components/page-header";
import { AccountSnapshot } from "../components/account-snapshot";
import { CategoryBreakdown } from "../components/category-breakdown";
import { ConsumptionAnalysis } from "../components/consumption-analysis";
import { FinanceSummaryStrip } from "../components/summary-strip";
import { TransactionTable } from "../components/transaction-table";
import { monthBounds } from "../lib/format";

function LoadingLedger() {
  return (
    <div className="space-y-4">
      <Skeleton className="h-28" />
      <div className="grid gap-4 xl:grid-cols-[minmax(0,1.35fr)_minmax(320px,0.65fr)]">
        <Skeleton className="h-[430px]" />
        <Skeleton className="h-[330px]" />
      </div>
      <Skeleton className="h-72" />
    </div>
  );
}

export default function FinanceLedgerPage() {
  const bounds = monthBounds();
  const [range, setRange] = useState(bounds);
  const [overview, setOverview] = useState<FinanceOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setOverview(await fetchFinanceOverview(range));
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : String(cause);
      setError(message);
      notification.danger({ title: "账本加载失败", description: message });
    } finally {
      setLoading(false);
    }
  }, [range]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void load();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  async function archive(id: number) {
    try {
      await archiveFinanceTransaction(id);
      notification.success({
        title: "已归档",
        description: "账目仍保留在历史数据中。",
      });
      await load();
    } catch (cause) {
      notification.danger({
        title: "归档失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    }
  }

  return (
    <WorkspaceShell sectionLabel="Finance" pageTitle="Ledger">
      <FinancePageHeader
        current="ledger"
        description="查看消费节奏、支出结构和账户变化，所有数据来自 AI 解析后确认的账单。"
        onRefresh={() => void load()}
        loading={loading}
      />
      <div className="rounded-xl border border-border/70 bg-muted/25 px-3 py-3 sm:px-4">
        <div className="flex items-center gap-2 text-xs font-medium">
          <CalendarDays className="size-4 text-primary" />
          统计周期
        </div>
        <div className="grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-2 sm:flex sm:flex-wrap">
          <Input
            aria-label="开始日期"
            type="date"
            value={range.start}
            onChange={(event) =>
              setRange((value) => ({ ...value, start: event.target.value }))
            }
            className="w-full bg-background sm:w-auto"
          />
          <span className="text-center text-xs text-muted-foreground">至</span>
          <Input
            aria-label="结束日期"
            type="date"
            value={range.end}
            onChange={(event) =>
              setRange((value) => ({ ...value, end: event.target.value }))
            }
            className="w-full bg-background sm:w-auto"
          />
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="col-span-3 sm:col-span-1"
            onClick={() => void load()}
            disabled={loading}
          >
            应用
          </Button>
        </div>
      </div>
      {error && !loading ? (
        <div className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:border-rose-900/50 dark:bg-rose-950/20 dark:text-rose-300">
          {error}
        </div>
      ) : null}
      {loading ? (
        <LoadingLedger />
      ) : overview ? (
        <>
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-primary/20 bg-primary/[0.04] px-4 py-3">
            <div className="flex min-w-0 items-center gap-3">
              <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary">
                <Camera className="size-4" />
              </span>
              <div className="min-w-0">
                <div className="text-sm font-medium">用图片快速入账</div>
                <div className="truncate text-xs text-muted-foreground">
                  上传一张小票，AI 自动解析并匹配账户、分类和收支类型。
                </div>
              </div>
            </div>
            <Button asChild size="sm" className="shrink-0 gap-1.5">
              <Link to="/finance/ai-capture">
                上传图片 <ArrowRight className="size-3.5" />
              </Link>
            </Button>
          </div>
          <FinanceSummaryStrip overview={overview} />
          <div className="grid min-w-0 gap-4 xl:grid-cols-[minmax(0,1.35fr)_minmax(320px,0.65fr)]">
            <ConsumptionAnalysis overview={overview} />
            <CategoryBreakdown items={overview.categories} />
          </div>
          <div className="grid min-w-0 gap-4 xl:grid-cols-[minmax(0,0.85fr)_minmax(0,1.15fr)]">
            <AccountSnapshot accounts={overview.accounts} />
            <TransactionTable
              rows={overview.recentTransactions}
              onArchive={archive}
            />
          </div>
          <div className="border-t border-border/60 pt-3 text-xs leading-5 text-muted-foreground">
            分析口径：只统计已确认入账的收入和支出；转账不会计入消费。AI
            识别结果始终先进入审核草稿。
          </div>
        </>
      ) : (
        <div className="rounded-lg border border-dashed border-border px-4 py-12 text-center text-sm text-muted-foreground">
          暂无账本数据
        </div>
      )}
    </WorkspaceShell>
  );
}
