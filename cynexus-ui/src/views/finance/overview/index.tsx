import { useCallback, useEffect, useState } from "react";
import {
  ArrowDownLeft,
  ArrowUpRight,
  CalendarRange,
  RefreshCw,
  Scale,
  WalletCards,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { DatePicker } from "@/components/ui/date-picker";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { toLocalIso } from "@/lib/date-utils";
import { WorkspaceShell } from "@/layout/workspace-shell";
import {
  fetchFinanceOverview,
  type FinanceOverview,
} from "@/services/finance/ledger";
import { notification } from "@/components/ui/notification";
import { cn } from "@/lib/utils";
import { DailyActivityHeatmap } from "../components/daily-activity-heatmap";
import { CashFlowChart } from "../components/cash-flow-chart";
import { ExpenseHeatmap } from "../components/expense-heatmap";
import { FinanceOverviewInsights } from "../components/finance-overview-insights";
import { AccountSnapshot } from "../components/account-snapshot";
import { FinancePageHeader } from "../components/page-header";
import { formatMoney, monthBounds, trailingDaysBounds } from "../lib/format";

const PERIODS = [
  { value: "month", label: "本月" },
  { value: "365", label: "过去 12 个月", days: 365 },
  { value: "180", label: "过去 6 个月", days: 180 },
  { value: "90", label: "过去 3 个月", days: 90 },
] as const;

export default function FinanceOverviewPage() {
  const [period, setPeriod] = useState("month");
  const [range, setRange] = useState(() => monthBounds());
  const [activityRange] = useState(() => trailingDaysBounds(365));
  const [overview, setOverview] = useState<FinanceOverview | null>(null);
  const [activityOverview, setActivityOverview] =
    useState<FinanceOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [activityLoading, setActivityLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setOverview(await fetchFinanceOverview(range));
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : String(cause);
      setError(message);
      notification.danger({ title: "财务概览加载失败", description: message });
    } finally {
      setLoading(false);
    }
  }, [range]);

  const loadActivity = useCallback(async () => {
    setActivityLoading(true);
    try {
      setActivityOverview(await fetchFinanceOverview(activityRange));
    } catch (cause) {
      notification.danger({
        title: "日活脉搏加载失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    } finally {
      setActivityLoading(false);
    }
  }, [activityRange]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void load();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadActivity();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [loadActivity]);

  function changePeriod(value: string) {
    setPeriod(value);
    if (value === "month") {
      setRange(monthBounds());
      return;
    }
    const selectedPeriod = PERIODS.find((item) => item.value === value);
    const days =
      selectedPeriod && "days" in selectedPeriod ? selectedPeriod.days : 365;
    setRange(trailingDaysBounds(days));
  }

  function changeDate(part: "start" | "end", date: Date | null) {
    if (!date) return;
    const nextDate = toLocalIso(date);
    setPeriod("custom");
    setRange((current) => {
      if (part === "start") {
        return {
          start: nextDate,
          end: nextDate > current.end ? nextDate : current.end,
        };
      }
      return {
        start: nextDate < current.start ? nextDate : current.start,
        end: nextDate,
      };
    });
  }

  return (
    <WorkspaceShell sectionLabel="Finance" pageTitle="Overview">
      <FinancePageHeader
        current="overview"
        description="从记账活跃度、现金流和账户结构，快速看清这一段时间的财务节奏。"
        onRefresh={() => void load()}
        loading={loading}
      />
      <section className="rounded-xl border border-border/70 bg-muted/25 px-3 py-3 sm:px-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center sm:justify-between">
          <div className="flex items-center gap-2 text-xs font-medium">
            <CalendarRange className="size-4 text-primary" />
            <span>统计周期</span>
            <span className="hidden text-muted-foreground sm:inline">
              {range.start} 至 {range.end}
            </span>
          </div>
          <div className="grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-2 sm:flex sm:flex-wrap sm:justify-end">
            <span className="col-span-3 text-xs text-muted-foreground sm:hidden">
              {range.start} 至 {range.end}
            </span>
            <Select value={period} onValueChange={changePeriod}>
              <SelectTrigger
                size="sm"
                className="col-span-3 w-full border-0 bg-muted/45 px-2 text-xs shadow-none hover:bg-muted/70 sm:col-span-1 sm:w-24"
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="month">本月</SelectItem>
                <SelectItem value="365">过去 12 个月</SelectItem>
                <SelectItem value="180">过去 6 个月</SelectItem>
                <SelectItem value="90">过去 3 个月</SelectItem>
                <SelectItem value="custom">自定义</SelectItem>
              </SelectContent>
            </Select>
            <DatePicker
              value={range.start}
              onChange={(date) => changeDate("start", date)}
              aria-label="开始日期"
              disableWeekends={false}
              triggerClassName="w-full sm:w-[132px]"
            />
            <span className="text-center text-xs text-muted-foreground">
              至
            </span>
            <DatePicker
              value={range.end}
              onChange={(date) => changeDate("end", date)}
              aria-label="结束日期"
              disableWeekends={false}
              triggerClassName="w-full sm:w-[132px]"
            />
          </div>
        </div>
      </section>
      {loading ? (
        <OverviewSkeleton />
      ) : error ? (
        <ErrorState message={error} onRetry={() => void load()} />
      ) : overview ? (
        <OverviewContent
          overview={overview}
          activityOverview={activityOverview}
          activityLoading={activityLoading}
        />
      ) : (
        <EmptyState />
      )}
    </WorkspaceShell>
  );
}

function OverviewContent({
  overview,
  activityOverview,
  activityLoading,
}: {
  overview: FinanceOverview;
  activityOverview: FinanceOverview | null;
  activityLoading: boolean;
}) {
  const activityCount = overview.daily.reduce(
    (sum, item) => sum + (item.activityCount ?? 0),
    0,
  );
  const activeDays = overview.daily.filter(
    (item) => (item.activityCount ?? 0) > 0,
  ).length;
  const dailyExpense = activeDays ? overview.expense / activeDays : 0;
  const savingsRate = overview.income
    ? (overview.net / overview.income) * 100
    : null;

  return (
    <>
      <section className="overflow-hidden rounded-xl border border-border/70 bg-card">
        <div className="flex items-start justify-between gap-3 border-b border-border/60 px-4 py-3">
          <div>
            <h2 className="text-sm font-semibold">财务概览</h2>
            <p className="mt-0.5 text-xs text-muted-foreground">
              资产看当前余额，收入与支出看选定周期。
            </p>
          </div>
          <span className="text-xs text-muted-foreground">CNY</span>
        </div>
        <div className="grid divide-y divide-border/60 md:grid-cols-4 md:divide-x md:divide-y-0">
          <OverviewMetric
            label="净资产"
            value={formatMoney(overview.netAssets)}
            detail={
              "资产 " +
              formatMoney(overview.assetTotal) +
              " · 待还 " +
              formatMoney(overview.liabilityTotal)
            }
            icon={WalletCards}
            tone={
              overview.netAssets >= 0
                ? "text-sky-600 dark:text-sky-400"
                : "text-amber-600 dark:text-amber-400"
            }
          />
          <OverviewMetric
            label="周期结余"
            value={formatMoney(overview.net)}
            detail={
              "收入 " +
              formatMoney(overview.income) +
              " · 支出 " +
              formatMoney(overview.expense)
            }
            icon={Scale}
            tone={
              overview.net >= 0
                ? "text-emerald-600 dark:text-emerald-400"
                : "text-rose-600 dark:text-rose-400"
            }
          />
          <OverviewMetric
            label="记账活跃"
            value={activityCount + " 笔"}
            detail={
              activeDays + " 个活跃日 · 日均支出 " + formatMoney(dailyExpense)
            }
            icon={CalendarRange}
            tone="text-violet-600 dark:text-violet-400"
          />
          <OverviewMetric
            label="收入结余率"
            value={savingsRate == null ? "—" : savingsRate.toFixed(1) + "%"}
            detail={
              savingsRate == null
                ? "暂无收入数据"
                : savingsRate >= 0
                  ? "当前收入能够覆盖支出"
                  : "支出已经超过收入"
            }
            icon={
              savingsRate != null && savingsRate >= 0
                ? ArrowDownLeft
                : ArrowUpRight
            }
            tone={
              savingsRate != null && savingsRate >= 0
                ? "text-emerald-600 dark:text-emerald-400"
                : "text-rose-600 dark:text-rose-400"
            }
          />
        </div>
      </section>
      <DailyActivityHeatmap
        overview={activityOverview}
        loading={activityLoading}
      />
      <div className="grid min-w-0 items-start gap-4 lg:grid-cols-2">
        <CashFlowChart overview={overview} />
        <ExpenseHeatmap overview={overview} />
      </div>
      <div className="grid min-w-0 items-start gap-4 lg:grid-cols-[minmax(0,0.85fr)_minmax(0,1.15fr)]">
        <FinanceOverviewInsights overview={overview} />
        <AccountSnapshot accounts={overview.accounts} />
      </div>
      <div className="flex items-start gap-2 border-t border-border/60 pt-3 text-xs leading-5 text-muted-foreground">
        <RefreshCw className="mt-0.5 size-3.5 shrink-0" />
        分析口径：只统计已确认且未归档的收入、支出和转账；净资产取当前账户余额，转账不计入收入或支出。
      </div>
    </>
  );
}

function OverviewMetric({
  label,
  value,
  detail,
  icon: Icon,
  tone,
}: {
  label: string;
  value: string;
  detail: string;
  icon: typeof Scale;
  tone: string;
}) {
  return (
    <div className="flex items-start gap-3 px-4 py-4 md:py-5">
      <Icon className={cn("mt-0.5 size-4 shrink-0", tone)} />
      <div className="min-w-0">
        <div className="text-xs text-muted-foreground">{label}</div>
        <div className={cn("mt-1 text-xl font-semibold tabular-nums", tone)}>
          {value}
        </div>
        <div className="mt-1 truncate text-[11px] text-muted-foreground">
          {detail}
        </div>
      </div>
    </div>
  );
}

function OverviewSkeleton() {
  return (
    <div className="space-y-4">
      <Skeleton className="h-36" />
      <Skeleton className="h-52" />
      <div className="grid gap-4 lg:grid-cols-2">
        <Skeleton className="h-[340px]" />
        <Skeleton className="h-[340px]" />
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <Skeleton className="h-[280px]" />
        <Skeleton className="h-[280px]" />
      </div>
    </div>
  );
}

function ErrorState({
  message,
  onRetry,
}: {
  message: string;
  onRetry: () => void;
}) {
  return (
    <div className="flex min-h-56 flex-col items-center justify-center rounded-xl border border-rose-200 bg-rose-50 px-4 text-center dark:border-rose-900/50 dark:bg-rose-950/20">
      <p className="text-sm font-medium text-rose-700 dark:text-rose-300">
        财务概览加载失败
      </p>
      <p className="mt-1 max-w-lg text-xs leading-5 text-rose-700/80 dark:text-rose-300/80">
        {message}
      </p>
      <Button
        type="button"
        variant="outline"
        size="sm"
        className="mt-4"
        onClick={onRetry}
      >
        重新加载
      </Button>
    </div>
  );
}

function EmptyState() {
  return (
    <div className="flex min-h-56 items-center justify-center rounded-xl border border-dashed border-border px-4 text-center text-sm text-muted-foreground">
      暂无财务数据，完成第一笔记账后这里会形成趋势。
    </div>
  );
}
