import { Activity } from "lucide-react";

import type { PortfolioTrade } from "@/services/market/portfolio/types";
import { cn } from "@/lib/utils";

export function TradeActivityHeatmap({ trades }: { trades: PortfolioTrade[] }) {
  const byDate = new Map<string, number>();
  for (const trade of trades) {
    const date = localDate(trade.tradedAt);
    byDate.set(date, (byDate.get(date) ?? 0) + 1);
  }

  const latestDate = [...byDate.keys()].sort().at(-1);
  if (!latestDate) {
    return (
      <section className="min-w-0 overflow-hidden rounded-lg border border-border/70 bg-card" aria-label="交易活跃度">
        <HeatmapHeader total={0} activeDays={0} />
        <p className="px-4 py-5 text-xs text-muted-foreground">暂无交易活跃度数据。</p>
      </section>
    );
  }

  const end = parseLocalDate(latestDate);
  end.setDate(end.getDate() + 6 - end.getDay());
  const start = new Date(end);
  start.setDate(start.getDate() - 52 * 7);
  const weeks = Array.from({ length: 53 }, (_, weekIndex) =>
    Array.from({ length: 7 }, (_, dayIndex) => {
      const date = new Date(start);
      date.setDate(start.getDate() + weekIndex * 7 + dayIndex);
      const key = formatDate(date);
      return { date, key, count: byDate.get(key) ?? 0 };
    }),
  );
  const maxCount = Math.max(...weeks.flat().map((item) => item.count), 1);
  const total = trades.length;
  const activeDays = [...byDate.values()].filter((count) => count > 0).length;
  const monthLabels = weeks.map((week, index) => {
    const date = week[0].date;
    return index === 0 || date.getDate() <= 7 ? `${date.getMonth() + 1}月` : "";
  });

  return (
    <section className="min-w-0 overflow-hidden rounded-lg border border-border/70 bg-card" aria-label="交易活跃度">
      <HeatmapHeader total={total} activeDays={activeDays} />
      <div className="px-4 py-3">
        <div className="grid grid-cols-[24px_minmax(0,1fr)] gap-x-2">
          <div />
          <div className="grid h-4 gap-1 text-[10px] text-muted-foreground" style={{ gridTemplateColumns: "repeat(53, minmax(0, 1fr))" }}>
            {monthLabels.map((label, index) => <span key={`${label}-${index}`} className="truncate">{label}</span>)}
          </div>
          <div className="grid grid-rows-7 gap-1 pr-1 text-[10px] leading-[10px] text-muted-foreground">
            <span>日</span><span /><span>二</span><span /><span>四</span><span /><span>六</span>
          </div>
          <div role="grid" aria-label="过去一年交易活跃度" className="grid grid-flow-col grid-rows-7 gap-1" style={{ gridTemplateColumns: "repeat(53, minmax(0, 1fr))" }}>
            {weeks.flatMap((week) => week.map((item) => (
              <span
                key={item.key}
                role="gridcell"
                aria-label={`${item.key} ${item.count} 笔交易`}
                title={`${item.key} · ${item.count} 笔交易`}
                className={cn("min-h-2 min-w-0 rounded-[3px] border border-transparent bg-muted/60 sm:min-h-3", activityTone(item.count, maxCount), !item.count && "opacity-35")}
              />
            )))}
          </div>
        </div>
        <div className="mt-3 flex items-center justify-between gap-2 text-[10px] text-muted-foreground">
          <div className="flex items-center gap-1.5"><Activity className="size-3" aria-hidden="true" /><span>交易活跃度 · 过去 53 周</span></div>
          <div className="flex items-center gap-1.5"><span>少</span><span className="size-3 rounded-[3px] bg-muted/60" /><span className="size-3 rounded-[3px] bg-red-100" /><span className="size-3 rounded-[3px] bg-red-300" /><span className="size-3 rounded-[3px] bg-red-500" /><span className="size-3 rounded-[3px] bg-red-600" /><span>多</span></div>
        </div>
      </div>
    </section>
  );
}

function HeatmapHeader({ total, activeDays }: { total: number; activeDays: number }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border/60 px-4 py-2.5">
      <div className="flex min-w-0 items-center gap-2"><Activity className="size-4 text-muted-foreground" aria-hidden="true" /><h2 className="text-sm font-semibold">交易活跃度</h2></div>
      <div className="text-[11px] text-muted-foreground"><span className="font-mono font-medium text-foreground tabular-nums">{total}</span> 笔交易 · <span className="font-mono font-medium text-foreground tabular-nums">{activeDays}</span> 个活跃日</div>
    </div>
  );
}

function activityTone(count: number, max: number) {
  if (!count) return "bg-muted/60";
  const ratio = count / Math.max(max, 1);
  if (ratio <= 0.25) return "bg-red-100";
  if (ratio <= 0.5) return "bg-red-300";
  if (ratio <= 0.75) return "bg-red-500";
  return "bg-red-600";
}

function localDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value.slice(0, 10);
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Shanghai" }).format(date);
}

function parseLocalDate(value: string) {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, (month || 1) - 1, day || 1);
}

function formatDate(value: Date) {
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`;
}
