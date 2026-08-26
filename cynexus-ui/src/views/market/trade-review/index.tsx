/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useDefaultLayout } from "react-resizable-panels";
import {
  Activity,
  ArrowDownUp,
  CalendarDays,
  Check,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Clock3,
  ClipboardCheck,
  Database,
  ExternalLink,
  FileChartColumn,
  Gauge,
  Landmark,
  Layers3,
  ListOrdered,
  Loader2,
  Pause,
  Play,
  Plus,
  RefreshCw,
  SkipBack,
  SkipForward,
  SlidersHorizontal,
  X,
} from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";

import { WorkspaceShell } from "@/layout/workspace-shell";
import { cn } from "@/lib/utils";
import { StockDetailDialog } from "@/components/stock-detail-dialog";
import DogLoader from "@/components/loader/dog-loader";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import {
  Command,
  CommandEmpty,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Textarea } from "@/components/ui/textarea";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Slider } from "@/components/ui/slider";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  ResizableHandle,
  ResizablePanel,
  ResizablePanelGroup,
} from "@/components/ui/resizable";
import { notification } from "@/components/ui/notification";
import { fetchPortfolioTrades } from "@/services/market/portfolio/portfolio";
import {
  fetchPortfolioReviewAnchors,
  savePortfolioReviewAnchor,
} from "@/services/market/portfolio/review-anchors";
import type {
  PortfolioReviewAnchor,
  PortfolioTrade,
} from "@/services/market/portfolio/types";
import {
  fetchStockFundFlow,
  fetchStockIntraday,
  fetchStockKlines,
  fetchStockMeta,
  fetchStockSectors,
  fetchSectorStocks,
  searchStockChart,
} from "@/services/market/stock-chart";
import {
  fetchEasyTdxBoardMembers,
  fetchEasyTdxBoardSummary,
  fetchEasyTdxBelongBoard,
  type EasyTdxBelongBoardRow,
  type EasyTdxBoardSummary,
  type EasyTdxResponse,
} from "@/services/market/easy-tdx";
import {
  fetchIndustryApplicationKline,
  fetchIndustryApplicationTargetCodes,
  type IndustryApplicationTdxFundFlowResponse,
  type IndustryFundFlowRow,
} from "@/services/market/industry-application";
import type {
  IndustryApplicationIndexBar,
  IndustryApplicationTargetCode,
} from "@/views/industry-application/lib/types";
import {
  fetchEasyTdxTimeshare,
  fetchEasyTdxTransactions,
  type HistoricalTradeTick,
} from "@/services/market/easy-tdx";
import {
  fetchMarginStockHistory,
  type MarginStockDetail,
} from "@/services/market/margin";
import {
  fetchStockBusinessComposition,
  fetchStockCompanyNews,
  fetchStockFinanceReportWithFallback,
  fetchStockProfitForecast,
  fetchStockValuation,
  type StockBusinessCompositionResponse,
  type StockProfitForecastResponse,
  type StockFinanceReportResponse,
  type StockCompanyNewsResponse,
  type StockValuationResponse,
} from "@/services/market/f10";
import {
  fetchMarketIndicatorIndustryDetail,
  fetchMarketIndicatorReplayTimeline,
  type MarketIndicatorIndustryDetail,
  type MarketIndicatorEvent,
  type MarketIndicatorReplayTimelineResponse,
} from "@/services/market/indicator";
import {
  fetchTradeReviewMaterializedDates,
  fetchTradeReviewMaterializationStatus,
  triggerLocalTdxMinuteLoad,
  triggerTradeReviewMarketStateMaterialization,
  triggerTradeReviewTimeshareLoad,
  type TradeReviewMaterializationStatus,
} from "@/services/market/trade-review";
import type {
  StockIntradayPoint,
  StockIntradayResponse,
  StockKlineBar,
  StockSearchItem,
  StockTargetType,
} from "@/views/stock-chart/lib/types";
import {
  barsForPeriod,
  aggregateDailyBars,
  barIndexForMarker,
  contextBarsForPeriod,
  easyTdxTimesharePoints,
  epoch,
  formatTime,
  markerList,
  nearestIndex,
  normalizeStockIntraday,
  isHistoricalReplayPeriod,
  type ReplayMarker,
  type ReplayPeriod,
} from "./lib/replay";
import { TradeReplayChart } from "./components/trade-replay-chart";
import { TradeSummaryChart } from "./components/trade-summary-chart";
import { TradeActivityHeatmap } from "./components/trade-activity-heatmap";
import { MarketStateMaterializedSlot } from "./components/market-state-materialized-slot";
import { MarketStateTab as MarketStateTabContent } from "./components/market-state-tab";
import {
  IndustryCapitalDialog,
  IndustryFundFlowDialog,
} from "./components/industry-capability-dialogs";

const INDEX_SYMBOL = "000001";
const INDEX_NAME = "上证指数";
const lastTradeReviewTargetStorageKey = "cynexus.trade-review.last-target";
const tradeReviewHealthCacheTtlMs = 8 * 60 * 60 * 1_000;
const tradeReviewHealthCacheVersion = 1;
const tradeReviewPanelTabs = [
  "交易与复盘",
  "逐笔成交",
  "两融",
  "财务",
  "资金流",
  "行业",
  "市场状态",
] as const;

function historicalBarsKey(targetType: StockTargetType, symbol: string) {
  return `${targetType}:${symbol.trim().toUpperCase()}`;
}

function historicalAdjust(targetType: StockTargetType) {
  // Stock daily history is served from ClickHouse's adjusted table. The raw
  // TDX fallback does not expose the full historical range for `none`.
  return targetType === "stock" ? "qfq" : "none";
}

function industryApplicationDailyBars(items: IndustryApplicationIndexBar[]): StockKlineBar[] {
  return items.flatMap((item) => {
    const normalizedTime = item.time.includes("T")
      ? item.time
      : item.time.replace(" ", "T");
    const timestamp = Date.parse(normalizedTime);
    if (!Number.isFinite(timestamp)) return [];
    return [{
      timestamp,
      trade_date: normalizedTime.slice(0, 10),
      open: Number(item.open),
      high: Number(item.high),
      low: Number(item.low),
      close: Number(item.close),
      volume: Number(item.volume_lots ?? 0),
      turnover: Number(item.amount ?? 0),
    }];
  });
}

type TradeReviewHealthCache = {
  version: number;
  checkedAt: number;
  stock: StockIntradayResponse;
  indicator: MarketIndicatorReplayTimelineResponse | null;
};

function tradeReviewHealthCacheKey(
  instrumentType: "STOCK" | "ETF",
  symbol: string,
  tradeDate: string,
) {
  return `cynexus.trade-review.health.${instrumentType}.${symbol.toUpperCase()}.${tradeDate}`;
}

function readTradeReviewHealthCache(
  instrumentType: "STOCK" | "ETF",
  symbol: string,
  tradeDate: string,
): TradeReviewHealthCache | null {
  if (typeof window === "undefined" || !symbol || !tradeDate) return null;
  try {
    const raw = window.localStorage.getItem(
      tradeReviewHealthCacheKey(instrumentType, symbol, tradeDate),
    );
    if (!raw) return null;
    const cached = JSON.parse(raw) as Partial<TradeReviewHealthCache>;
    if (
      cached.version !== tradeReviewHealthCacheVersion ||
      !cached.checkedAt ||
      Date.now() - cached.checkedAt > tradeReviewHealthCacheTtlMs ||
      !cached.stock
    ) {
      return null;
    }
    return cached as TradeReviewHealthCache;
  } catch {
    return null;
  }
}

function writeTradeReviewHealthCache(
  instrumentType: "STOCK" | "ETF",
  symbol: string,
  tradeDate: string,
  stock: StockIntradayResponse,
  indicator: MarketIndicatorReplayTimelineResponse | null,
) {
  if (typeof window === "undefined" || !symbol || !tradeDate) return;
  try {
    const payload: TradeReviewHealthCache = {
      version: tradeReviewHealthCacheVersion,
      checkedAt: Date.now(),
      stock,
      indicator,
    };
    window.localStorage.setItem(
      tradeReviewHealthCacheKey(instrumentType, symbol, tradeDate),
      JSON.stringify(payload),
    );
  } catch {
    // A full/blocked localStorage must not prevent the health check from working.
  }
}

type TradeReviewTargetPreference = {
  symbol: string;
  instrumentType: "STOCK" | "ETF";
};

function readLastTradeReviewTarget(): TradeReviewTargetPreference | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(lastTradeReviewTargetStorageKey);
  if (!raw) return null;
  try {
    const value = JSON.parse(raw) as Partial<TradeReviewTargetPreference>;
    if (!value.symbol) return null;
    return {
      symbol: value.symbol.toUpperCase(),
      instrumentType: value.instrumentType === "ETF" ? "ETF" : "STOCK",
    };
  } catch {
    return null;
  }
}

function writeLastTradeReviewTarget(
  symbol: string,
  instrumentType: "STOCK" | "ETF",
) {
  if (typeof window === "undefined" || !symbol) return;
  window.localStorage.setItem(
    lastTradeReviewTargetStorageKey,
    JSON.stringify({ symbol: symbol.toUpperCase(), instrumentType }),
  );
}

type SectorContext = {
  symbol: string;
  name: string;
  targetType: "index" | "sector";
  kind?: IndustryApplicationTargetCode["kind"];
};

type IndustryPickerOption = IndustryApplicationTargetCode & {
  sourceLabel: string;
};

// Easy-TDX has returned both the legacy 12/0/1 codes and the current MAC
// industry code 2 across environments. Keep all known industry codes here;
// concept (4), region (3), and style (5) boards remain separate.
const TDX_INDUSTRY_BOARD_TYPES = new Set([0, 1, 2, 12]);

function isTdxIndustryBoard(row: Pick<EasyTdxBelongBoardRow, "board_type">) {
  return TDX_INDUSTRY_BOARD_TYPES.has(row.board_type);
}

const manualSectorStorageKey = (instrumentType: "STOCK" | "ETF", symbol: string) =>
  `cynexus.trade-review.manual-sector.${instrumentType}.${symbol}`;

function readManualSectorContext(
  instrumentType: "STOCK" | "ETF",
  symbol: string,
): SectorContext | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(manualSectorStorageKey(instrumentType, symbol));
  if (!raw) return null;
  try {
    const value = JSON.parse(raw) as Partial<SectorContext>;
    if (!value.symbol || !value.name || !value.targetType) return null;
    return {
      symbol: value.symbol,
      name: value.name,
      targetType: value.targetType === "sector" ? "sector" : "index",
      kind: value.kind === "concept" || value.kind === "industry" || value.kind === "index"
        ? value.kind
        : undefined,
    };
  } catch {
    return null;
  }
}

function writeManualSectorContext(
  instrumentType: "STOCK" | "ETF",
  symbol: string,
  context: SectorContext | null,
) {
  if (typeof window === "undefined") return;
  const key = manualSectorStorageKey(instrumentType, symbol);
  if (context) window.localStorage.setItem(key, JSON.stringify(context));
  else window.localStorage.removeItem(key);
}

function resolveSectorContext(
  meta: Awaited<ReturnType<typeof fetchStockMeta>> | null,
  sectors: Awaited<ReturnType<typeof fetchStockSectors>> | null,
  tdxBoards: EasyTdxResponse<EasyTdxBelongBoardRow> | null,
  flowRows: IndustryFundFlowRow[],
  securityName: string | null | undefined,
): SectorContext | null {
  if (meta?.sectorIndexSymbol) {
    return {
      symbol: meta.sectorIndexSymbol,
      name: meta.sectorIndexName || meta.sectorIndexSymbol,
      targetType: /^\d+$/.test(meta.sectorIndexSymbol) ? "index" : "sector",
      kind: "industry",
    };
  }

  // Stock-sector serving can derive the canonical TDX 56 code from the THS
  // constituent reverse mapping. Prefer that code for both the intraday
  // overlay and the Industry Application slot; passing the THS code here
  // makes the TDX fund-flow endpoint reject an otherwise valid stock sector.
  const mappedIndustry = (sectors?.industries ?? []).find(
    (item) =>
      item.source?.includes("ths_industry_constituent_snapshot") &&
      Boolean(item.name?.trim()) &&
      Boolean(item.topic_id?.trim() || item.sector_id?.trim()),
  );
  if (mappedIndustry) {
    return {
      symbol: mappedIndustry.topic_id?.trim() || mappedIndustry.sector_id!.trim(),
      name: mappedIndustry.name!.trim(),
      targetType: "index",
      kind: "industry",
    };
  }

  const tdxIndustry = (tdxBoards?.items ?? []).find(
    (item) => isTdxIndustryBoard(item) && Boolean(item.board_code?.trim()),
  );
  if (tdxIndustry) {
    return {
      symbol: tdxIndustry.board_code.trim(),
      name: tdxIndustry.board_name?.trim() || tdxIndustry.board_code.trim(),
      targetType: "index",
      kind: "industry",
    };
  }

  const sectorNames = [
    meta?.industry,
    ...(sectors?.industries ?? []).map((item) => item.name),
    ...(sectors?.concepts ?? []).map((item) => item.name),
  ].filter((item): item is string => Boolean(item?.trim()));
  const instrumentText = `${securityName ?? ""} ${meta?.name ?? ""}`;
  const row = flowRows.find((item) => {
    const name = String(item["行业"] ?? "").trim();
    return (
      name &&
      (sectorNames.some((candidate) => candidate === name) ||
        instrumentText.includes(name))
    );
  });
  if (!row?.code) return null;
  return {
    symbol: String(row.code),
    name: String(row["行业"] ?? row.code),
    targetType: "index",
    kind: "industry",
  };
}
const PERIODS: Array<{ value: ReplayPeriod; label: string }> = [
  { value: "timeshare", label: "分时" },
  { value: "1m", label: "1m" },
  { value: "5m", label: "5m" },
  { value: "15m", label: "15m" },
  { value: "30m", label: "30m" },
  { value: "1d", label: "日K" },
  { value: "1w", label: "周K" },
  { value: "1mo", label: "月K" },
];

function periodLabel(period: ReplayPeriod) {
  return PERIODS.find((item) => item.value === period)?.label ?? period;
}

function localDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value.slice(0, 10)
    : new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Shanghai" }).format(
        date,
      );
}

function calendarMonthDate(value: string) {
  const [year, month] = value.split("-").map(Number);
  return new Date(year || new Date().getFullYear(), (month || 1) - 1, 1);
}

function calendarMonthDays(month: Date) {
  const start = new Date(month.getFullYear(), month.getMonth(), 1);
  start.setDate(start.getDate() - start.getDay());
  const end = new Date(month.getFullYear(), month.getMonth() + 1, 0);
  end.setDate(end.getDate() + (6 - end.getDay()));
  const days: Date[] = [];
  for (const cursor = new Date(start); cursor <= end; cursor.setDate(cursor.getDate() + 1)) {
    days.push(new Date(cursor));
  }
  return days;
}

function addCalendarMonths(value: Date, offset: number) {
  const next = new Date(value.getFullYear(), value.getMonth() + offset, 1);
  return `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, "0")}`;
}

function formatCalendarDate(value: Date) {
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`;
}

function calendarWeekStart(value: string) {
  const date = value ? calendarMonthDate(value.slice(0, 7)) : new Date();
  if (value) {
    const [year, month, day] = value.split("-").map(Number);
    date.setFullYear(year, (month || 1) - 1, day || 1);
  }
  date.setDate(date.getDate() - date.getDay());
  return date;
}

function historyCountTone(count: number, grouped: Map<string, PortfolioTrade[]>) {
  const maxCount = Math.max(...[...grouped.values()].map((items) => items.length), count, 1);
  const ratio = count / maxCount;
  if (ratio >= 0.8) return "bg-red-600 text-white hover:bg-red-600";
  if (ratio >= 0.55) return "bg-red-500 text-white hover:bg-red-600";
  if (ratio >= 0.3) return "bg-red-300 text-red-950 hover:bg-red-500";
  if (ratio >= 0.15) return "bg-red-100 text-red-800 hover:bg-red-300";
  return "bg-red-50 text-red-700 hover:bg-red-100";
}

function displayNumber(value: number | null | undefined, digits = 2) {
  return value == null || !Number.isFinite(value)
    ? "-"
    : value.toLocaleString("zh-CN", { maximumFractionDigits: digits });
}

function displayScaled(value: number | null | undefined, digits = 2) {
  if (value == null || !Number.isFinite(value)) return "-";
  const absolute = Math.abs(value);
  if (absolute >= 100_000_000) return `${(value / 100_000_000).toFixed(digits).replace(/\.00$/u, "")} 亿`;
  if (absolute >= 10_000) return `${(value / 10_000).toFixed(digits).replace(/\.00$/u, "")} 万`;
  return displayNumber(value, digits);
}

type ResearchRatingBucket = "buy" | "add" | "neutral" | "reduce" | "sell" | "other";

function researchRatingBucket(value: string | null | undefined): ResearchRatingBucket {
  const rating = value?.replaceAll(" ", "") ?? "";
  if (!rating) return "other";
  if (rating.includes("卖出")) return "sell";
  if (rating.includes("减持") || rating.includes("弱于")) return "reduce";
  if (rating.includes("中性") || rating.includes("持有") || rating.includes("同步")) return "neutral";
  if (rating.includes("增持") || rating.includes("优于") || rating.includes("跑赢")) return "add";
  if (rating.includes("买入") || rating.includes("推荐") || rating.includes("强烈")) return "buy";
  return "other";
}

function researchRatingLabel(bucket: ResearchRatingBucket) {
  return ({ buy: "买入/推荐", add: "增持", neutral: "中性/持有", reduce: "减持", sell: "卖出", other: "其他" })[bucket];
}

type FinancialSignalTone = "positive" | "neutral" | "warning" | "negative";
type FinancialSignal = { label: string; tone: FinancialSignalTone };

function financialSignalClass(tone: FinancialSignalTone) {
  return tone === "positive"
    ? "bg-emerald-50 text-emerald-700"
    : tone === "negative"
      ? "bg-rose-50 text-rose-700"
      : tone === "warning"
        ? "bg-amber-50 text-amber-700"
        : "bg-muted text-muted-foreground";
}

function FinancialSignalBadge({ signal }: { signal?: FinancialSignal | null }) {
  if (!signal) return null;
  return <span className={`shrink-0 rounded-full px-1.5 py-0.5 text-[9px] font-medium ${financialSignalClass(signal.tone)}`}>{signal.label}</span>;
}

function finiteNumber(value: unknown): number | null {
  const parsed = typeof value === "number" ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function displayVolume(value: number | null | undefined) {
  if (value == null || !Number.isFinite(value)) return "-";
  const absolute = Math.abs(value);
  const sign = value < 0 ? "-" : "";
  if (absolute >= 100_000_000)
    return `${sign}${(absolute / 100_000_000)
      .toFixed(2)
      .replace(/\.00$/, "")}亿`;
  if (absolute >= 10_000)
    return `${sign}${(absolute / 10_000).toFixed(2).replace(/\.00$/, "")}万`;
  return Math.round(value).toLocaleString("zh-CN");
}

type TradePerformance = {
  closedTrades: number;
  winningTrades: number;
  winRate: number | null;
  profitLossRatio: number | null;
  grossProfit: number;
  grossLoss: number;
};

function calculateTradePerformance(trades: PortfolioTrade[]): TradePerformance {
  type BuyLot = { quantity: number; price: number; feePerUnit: number };
  const lots: BuyLot[] = [];
  let winningTrades = 0;
  let closedTrades = 0;
  let profit = 0;
  let loss = 0;

  for (const trade of [...trades].sort((left, right) => epoch(left.tradedAt) - epoch(right.tradedAt))) {
    if (trade.side === "BUY") {
      lots.push({
        quantity: trade.quantity,
        price: trade.price,
        feePerUnit: trade.quantity > 0 ? trade.totalFee / trade.quantity : 0,
      });
      continue;
    }

    let remaining = trade.quantity;
    const sellFeePerUnit = trade.quantity > 0 ? trade.totalFee / trade.quantity : 0;
    while (remaining > 0 && lots.length) {
      const lot = lots[0];
      const matched = Math.min(remaining, lot.quantity);
      const pnl = (trade.price - lot.price - lot.feePerUnit - sellFeePerUnit) * matched;
      closedTrades += 1;
      if (pnl > 0) {
        winningTrades += 1;
        profit += pnl;
      } else if (pnl < 0) {
        loss += Math.abs(pnl);
      }
      remaining -= matched;
      lot.quantity -= matched;
      if (lot.quantity <= 1e-9) lots.shift();
    }
  }

  return {
    closedTrades,
    winningTrades,
    winRate: closedTrades ? winningTrades / closedTrades : null,
    profitLossRatio: loss > 0 ? profit / loss : profit > 0 ? Number.POSITIVE_INFINITY : null,
    grossProfit: profit,
    grossLoss: loss,
  };
}

function calculateOverallTradePerformance(trades: PortfolioTrade[]): TradePerformance {
  const groups = new Map<string, PortfolioTrade[]>();
  for (const trade of trades) {
    const key = `${trade.instrumentType}:${trade.symbol}`;
    const group = groups.get(key) ?? [];
    group.push(trade);
    groups.set(key, group);
  }
  const result = [...groups.values()].map(calculateTradePerformance).reduce(
    (total, current) => ({
      closedTrades: total.closedTrades + current.closedTrades,
      winningTrades: total.winningTrades + current.winningTrades,
      winRate: null,
      profitLossRatio: null,
      grossProfit: total.grossProfit + current.grossProfit,
      grossLoss: total.grossLoss + current.grossLoss,
    }),
    { closedTrades: 0, winningTrades: 0, winRate: null, profitLossRatio: null, grossProfit: 0, grossLoss: 0 },
  );
  return {
    ...result,
    winRate: result.closedTrades ? result.winningTrades / result.closedTrades : null,
    profitLossRatio: result.grossLoss > 0 ? result.grossProfit / result.grossLoss : result.grossProfit > 0 ? Number.POSITIVE_INFINITY : null,
  };
}

function dataState(
  stock: StockIntradayResponse | null,
  timeline: MarketIndicatorReplayTimelineResponse | null,
): {
  label: string;
  tone: "positive" | "warning" | "destructive";
  detail: string;
} {
  const hasMinute = Boolean(stock?.minute_bars?.["1m"]?.length);
  const hasTimeshare = Boolean(stock?.timeshare?.length);
  if (!hasMinute && !hasTimeshare)
    return {
      label: "不可用",
      tone: "destructive",
      detail: "1m 与分时都没有覆盖",
    };
  if (!hasMinute || !hasTimeshare)
    return {
      label: "降级",
      tone: "warning",
      detail: `${hasMinute ? "分时" : "1m"}数据缺失`,
    };
  if (timeline?.dataStatus === "partial" || timeline?.dataStatus === "pending")
    return { label: "降级", tone: "warning", detail: "市场指标事件部分覆盖" };
  return {
    label: "可用",
    tone: "positive",
    detail: "价格、均价和成交量可回放",
  };
}

async function loadTradeReviewIntraday(params: {
  targetType: StockTargetType;
  symbol: string;
  name?: string;
  tradeDate: string;
  periods: Array<"1m" | "5m" | "15m" | "30m">;
}): Promise<StockIntradayResponse | null> {
  const raw = await fetchStockIntraday({
    targetType: params.targetType,
    symbol: params.symbol,
    name: params.name,
    adjust: "none",
    tradeDate: params.tradeDate,
    periods: params.periods,
    strictDate: true,
  }).catch(() => null);
  const normalized = raw ? normalizeStockIntraday(raw, params.tradeDate) : null;
  if (
    normalized &&
    normalized.timeshare.length
  ) {
    return normalized;
  }

  // ClickHouse can fail on an individual corrupted part even when the local
  // TDX minute source has the requested trading day. Fall back to that source
  // so one bad part does not make the whole replay target unavailable.
  const tdx = await fetchEasyTdxTimeshare({
    code: params.symbol,
    tradeDate: params.tradeDate,
  }).catch(() => null);
  const rawPoints = easyTdxTimesharePoints(tdx?.items ?? [], params.tradeDate);
  // TDX historical ETF timeshare uses the same ten-times price scale as the
  // raw ETF day files, while the chart API has already normalized ETF minute
  // bars and previous close. Keep the fallback series in that same unit.
  const points = params.targetType === "etf"
    ? rawPoints.map((point) => ({
        ...point,
        price: point.price / 10,
        open: point.open == null ? point.open : point.open / 10,
        avg_price: point.avg_price == null ? point.avg_price : point.avg_price / 10,
        turnover: point.turnover == null ? point.turnover : point.turnover / 10,
      }))
    : rawPoints;
  if (!points.length) return normalized;
  const fallbackMinuteBars = points.map((point) => ({
    timestamp: point.timestamp,
    trade_date: point.trade_date ?? undefined,
    open: point.open ?? point.price,
    high: point.price,
    low: point.price,
    close: point.price,
    volume: point.volume,
    turnover: point.turnover ?? undefined,
  }));
  return {
    ...(normalized ?? {
      ok: true,
      symbol: params.symbol,
      target_type: params.targetType,
      name: params.name ?? params.symbol,
      adjust: "none",
      effective_adjust: "none",
      requested_adjust: "none",
      minute_bars: {},
    }),
    timeshare: points,
    minute_bars: {
      ...(normalized?.minute_bars ?? {}),
      "1m": normalized?.minute_bars["1m"]?.length
        ? normalized.minute_bars["1m"]
        : fallbackMinuteBars,
    },
    trade_date: params.tradeDate,
    requested_trade_date: params.tradeDate,
    source: "easy_tdx.tdx.timeshare",
  };
}

export default function TradeReviewPage() {
  const [searchParams] = useSearchParams();
  const lastTarget = useMemo(() => readLastTradeReviewTarget(), []);
  const [trades, setTrades] = useState<PortfolioTrade[]>([]);
  const [materializedTradeDates, setMaterializedTradeDates] = useState<string[]>([]);
  const [symbol, setSymbol] = useState(
    searchParams.get("symbol")?.toUpperCase() ?? lastTarget?.symbol ?? "",
  );
  const [instrumentType, setInstrumentType] = useState<"STOCK" | "ETF">(
    searchParams.get("type") === "ETF"
      ? "ETF"
      : searchParams.get("symbol")
        ? "STOCK"
        : lastTarget?.instrumentType ?? "STOCK",
  );
  const [tradeDate, setTradeDate] = useState(searchParams.get("date") ?? "");
  const [healthStock, setHealthStock] = useState<StockIntradayResponse | null>(
    null,
  );
  const [summaryStocks, setSummaryStocks] = useState<Record<string, StockIntradayResponse | null>>({});
  const [summaryPage, setSummaryPage] = useState(0);
  const [summaryPrimaryKey, setSummaryPrimaryKey] = useState<string | null>(null);
  const [healthIndicator, setHealthIndicator] =
    useState<MarketIndicatorReplayTimelineResponse | null>(null);
  const [healthLoading, setHealthLoading] = useState(false);
  const [loadBusy, setLoadBusy] = useState(false);
  const [timeshareLoadBusy, setTimeshareLoadBusy] = useState(false);
  const [materializationStatus, setMaterializationStatus] =
    useState<TradeReviewMaterializationStatus | null>(null);
  const [materializationLoading, setMaterializationLoading] = useState(false);
  const [materializationBusy, setMaterializationBusy] = useState(false);
  const [panelOpen, setPanelOpen] = useState(false);
  const [historyMonth, setHistoryMonth] = useState<string | null>(null);
  const [historyView, setHistoryView] = useState<"week" | "month" | "year">("month");
  const [historyYear, setHistoryYear] = useState<number | null>(null);

  useEffect(() => {
    void fetchPortfolioTrades()
      .then(setTrades)
      .catch(() => setTrades([]));
    void fetchTradeReviewMaterializedDates()
      .then(setMaterializedTradeDates)
      .catch(() => setMaterializedTradeDates([]));
  }, []);
  const tradeDates = useMemo(
    () =>
      [...new Set([...trades.map((item) => localDate(item.tradedAt)), ...materializedTradeDates])].sort(
        (a, b) => b.localeCompare(a),
      ),
    [materializedTradeDates, trades],
  );
  const symbols = useMemo(
    () => [...new Map(trades.map((item) => [item.symbol, item])).values()],
    [trades],
  );

  useEffect(() => {
    if (!symbol && trades[0]) {
      setSymbol(trades[0].symbol);
      setInstrumentType(trades[0].instrumentType);
    }
    if (!tradeDate && tradeDates[0]) setTradeDate(tradeDates[0]);
  }, [symbol, tradeDate, tradeDates, trades]);

  useEffect(() => {
    if (searchParams.get("open") !== "1") return;
    const requestedSymbol = searchParams.get("symbol")?.trim().toUpperCase();
    if (requestedSymbol && requestedSymbol !== symbol) setSymbol(requestedSymbol);
    const requestedType = searchParams.get("type") === "ETF" ? "ETF" : "STOCK";
    if (requestedSymbol && requestedType !== instrumentType) setInstrumentType(requestedType);
    const requestedDate = searchParams.get("date") ?? "";
    if (requestedDate && requestedDate !== tradeDate) setTradeDate(requestedDate);
  }, [instrumentType, searchParams, symbol, tradeDate]);

  // The global command palette can deep-link to a symbol and request its panel
  // to open on the latest available trading day.
  useEffect(() => {
    if (searchParams.get("open") !== "1" || !symbol) return;
    if (!tradeDate && tradeDates[0]) setTradeDate(tradeDates[0]);
    if (tradeDate || tradeDates[0]) setPanelOpen(true);
  }, [searchParams, symbol, tradeDate, tradeDates]);

  useEffect(() => {
    if (symbol) writeLastTradeReviewTarget(symbol, instrumentType);
  }, [instrumentType, symbol]);

  const loadHealth = useCallback(async (force = false) => {
    if (!symbol || !tradeDate) return;
    if (!force) {
      const cached = readTradeReviewHealthCache(
        instrumentType,
        symbol,
        tradeDate,
      );
      if (cached) {
        setHealthStock(cached.stock);
        setHealthIndicator(cached.indicator);
        setHealthLoading(false);
        return;
      }
    }
    setHealthLoading(true);
    const [nextStock, indicator] = await Promise.all([
      loadTradeReviewIntraday({
        targetType: instrumentType === "ETF" ? "etf" : "stock",
        symbol,
        tradeDate,
        periods: ["1m"],
      }),
      fetchMarketIndicatorReplayTimeline({
        tradeDate,
        from: "09:30",
        to: "15:00",
        limit: 3000,
      }).catch(() => null),
    ]);
    setHealthStock(nextStock);
    setHealthIndicator(indicator);
    setHealthLoading(false);
    if (
      nextStock &&
      (nextStock.minute_bars["1m"]?.length || nextStock.timeshare.length)
    ) {
      writeTradeReviewHealthCache(
        instrumentType,
        symbol,
        tradeDate,
        nextStock,
        indicator,
      );
    }
  }, [instrumentType, symbol, tradeDate]);

  useEffect(() => {
    void loadHealth();
  }, [loadHealth]);

  const currentDayTrades = useMemo(
    () => trades.filter((item) => localDate(item.tradedAt) === tradeDate),
    [tradeDate, trades],
  );

  useEffect(() => {
    if (!tradeDate || !currentDayTrades.length) {
      setSummaryStocks({});
      setSummaryPage(0);
      setSummaryPrimaryKey(null);
      return;
    }
    let active = true;
    const targets = Array.from(
      new Map(
        currentDayTrades.map((item) => [
          `${item.instrumentType}:${item.symbol}`,
          item,
        ]),
      ).values(),
    );
    setSummaryStocks({});
    setSummaryPage(0);
    setSummaryPrimaryKey(null);
    for (const target of targets) {
      const key = `${target.instrumentType}:${target.symbol}`;
      void loadTradeReviewIntraday({
          targetType: target.instrumentType === "ETF" ? "etf" : "stock",
          symbol: target.symbol,
          name: target.securityName ?? target.symbol,
          tradeDate,
          periods: ["1m"],
        }).then((stock) => {
          if (active) setSummaryStocks((current) => ({ ...current, [key]: stock }));
        });
    }
    return () => {
      active = false;
    };
  }, [currentDayTrades, tradeDate]);

  const loadMaterializationStatus = useCallback(async () => {
    if (!tradeDate) {
      setMaterializationStatus(null);
      return;
    }
    setMaterializationLoading(true);
    try {
      setMaterializationStatus(await fetchTradeReviewMaterializationStatus(tradeDate));
    } catch {
      setMaterializationStatus(null);
    } finally {
      setMaterializationLoading(false);
    }
  }, [tradeDate]);
  useEffect(() => {
    void loadMaterializationStatus();
  }, [loadMaterializationStatus]);
  const state = dataState(healthStock, healthIndicator);
  const selectedTrades = trades.filter(
    (item) => item.symbol === symbol && localDate(item.tradedAt) === tradeDate,
  );
  const summaryStockFor = (trade: PortfolioTrade) =>
    summaryStocks[`${trade.instrumentType}:${trade.symbol}`] ??
    (trade.symbol === symbol && trade.instrumentType === instrumentType ? healthStock : null);
  const summaryGroups = useMemo(() => {
    const groups = new Map<string, PortfolioTrade[]>();
    for (const trade of currentDayTrades) {
      const key = `${trade.instrumentType}:${trade.symbol}`;
      const group = groups.get(key) ?? [];
      group.push(trade);
      groups.set(key, group);
    }
    return [...groups.entries()];
  }, [currentDayTrades]);
  const summaryPageCount = Math.max(1, Math.ceil(summaryGroups.length / 2));
  const overallTradePerformance = useMemo(
    () => calculateOverallTradePerformance(trades),
    [trades],
  );
  const visibleSummaryGroups = useMemo(() => {
    const pageStart = Math.min(summaryPage, summaryPageCount - 1) * 2;
    const pageGroups = summaryGroups.slice(pageStart, pageStart + 2);
    if (!summaryPrimaryKey) return pageGroups;
    const selected = summaryGroups.find(([key]) => key === summaryPrimaryKey);
    if (!selected) return pageGroups;
    return [selected, ...pageGroups.filter(([key]) => key !== summaryPrimaryKey)].slice(0, 2);
  }, [summaryGroups, summaryPage, summaryPageCount, summaryPrimaryKey]);
  const historyTradeDates = useMemo(() => {
    const grouped = new Map<string, PortfolioTrade[]>();
    for (const trade of trades) {
      const date = localDate(trade.tradedAt);
      const group = grouped.get(date) ?? [];
      group.push(trade);
      grouped.set(date, group);
    }
    return grouped;
  }, [trades]);
  const historyMonthKey = historyMonth || tradeDate.slice(0, 7) || tradeDates[0]?.slice(0, 7) || "";
  const historyMonthDate = calendarMonthDate(historyMonthKey);
  const historyDays = calendarMonthDays(historyMonthDate);
  const historyMonthKeys = tradeDates.map((date) => date.slice(0, 7));
  const firstHistoryMonth = historyMonthKeys.at(-1) ?? historyMonthKey;
  const lastHistoryMonth = historyMonthKeys[0] ?? historyMonthKey;
  const historyWeekStart = calendarWeekStart(tradeDate || tradeDates[0] || "");
  const historyWeekDays = Array.from({ length: 7 }, (_, index) => {
    const date = new Date(historyWeekStart);
    date.setDate(date.getDate() + index);
    return date;
  });
  const historyYearValue = historyYear || Number((tradeDate || tradeDates[0] || "").slice(0, 4)) || new Date().getFullYear();
  const historyYearMonths = Array.from({ length: 12 }, (_, index) => new Date(historyYearValue, index, 1));
  const selectedSecurityName =
    selectedTrades.find((item) => item.securityName?.trim())?.securityName?.trim() ??
    symbols.find((item) => item.symbol === symbol)?.securityName?.trim() ??
    healthStock?.name?.trim() ??
    "名称待补充";

  function selectSummarySymbol(key: string) {
    const groupIndex = summaryGroups.findIndex(([groupKey]) => groupKey === key);
    if (groupIndex < 0) return;
    setSummaryPage(Math.floor(groupIndex / 2));
    setSummaryPrimaryKey(key);
  }

  async function loadLocal() {
    if (!symbol || !tradeDate) return;
    setLoadBusy(true);
    try {
      await triggerLocalTdxMinuteLoad();
      notification.success({
        title: "本地 1m 数据已加载",
        description:
          "已按 TDX 本地文件的增量覆盖范围同步，并重新检查当前交易日。",
      });
      await loadHealth(true);
    } catch (error) {
      notification.danger({
        title: "本地 1m 加载失败",
        description: error instanceof Error ? error.message : String(error),
      });
    } finally {
      setLoadBusy(false);
    }
  }

  async function loadTimeshare() {
    if (!symbol || !tradeDate) return;
    setTimeshareLoadBusy(true);
    try {
      await triggerTradeReviewTimeshareLoad({ tradeDate, symbol, instrumentType });
      notification.success({ title: "复盘分时归档任务已触发", description: "任务完成后重新检查即可读取落库/Redis 数据。" });
      await loadHealth(true);
    } catch (error) {
      notification.danger({ title: "复盘分时归档失败", description: error instanceof Error ? error.message : String(error) });
    } finally {
      setTimeshareLoadBusy(false);
    }
  }

  async function materializeMarketState() {
    if (!tradeDate) return;
    setMaterializationBusy(true);
    try {
      const result = await triggerTradeReviewMarketStateMaterialization(tradeDate);
      if (result.accepted === false) {
        throw new Error(result.message ?? "任务当前不可触发，请检查是否已有运行中的物化任务。");
      }
      notification.success({
        title: "市场状态物化任务已触发",
        description: `${tradeDate} 全市场、THS / TDX / union 行业状态将按已落库数据重建${result.runId ? `（任务 ${result.runId}）` : ""}。`,
      });
      // The job is asynchronous. Refresh once immediately so an existing
      // failed/unavailable manifest is not left stale on the entry page.
      await loadMaterializationStatus();
    } catch (error) {
      notification.danger({
        title: "市场状态物化触发失败",
        description: error instanceof Error ? error.message : String(error),
      });
    } finally {
      setMaterializationBusy(false);
    }
  }

  const materializationManifest = materializationStatus?.manifest ?? {};
  const materializationRawStatus = String(materializationManifest.status ?? "").toUpperCase();
  const materializationLabel = materializationLoading
    ? "物化状态检查中"
    : materializationRawStatus === "READY" || materializationStatus?.dataStatus === "ready"
      ? "市场状态已物化"
      : materializationRawStatus === "PARTIAL" || materializationStatus?.dataStatus === "partial"
        ? "市场状态部分物化"
        : materializationRawStatus === "FAILED"
          ? "市场状态物化失败"
          : "市场状态未物化";
  const materializationDetail = materializationRawStatus === "READY"
    ? `${materializationManifest.marketMinuteCount ?? materializationManifest.market_minute_count ?? "-"} 分钟 · ${materializationManifest.contextCount ?? materializationManifest.context_count ?? "-"} 标的`
    : materializationStatus?.missingReasons?.includes("minute_1m_unavailable")
      ? "缺少当日 1m 基线"
      : materializationRawStatus === "FAILED"
        ? String(materializationManifest.errorMessage ?? materializationManifest.error_message ?? "请检查任务运行记录")
        : "可在本地 1m 导入完成后手动重建";

  return (
    <WorkspaceShell sectionLabel="Cynexus Agent" pageTitle="Trade Review">
      <div className="mx-auto w-full max-w-[1680px] space-y-4">
        <header className="flex flex-col gap-3 border-b border-border/60 pb-3 xl:flex-row xl:items-center xl:justify-between">
          <div className="min-w-0">
            <div className="flex items-center gap-2 text-xs font-medium text-muted-foreground">
              <Activity className="size-3.5 text-information" />
              盘后复盘工作台
            </div>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">
              Trade Review
            </h1>
            <p className="mt-1 max-w-3xl text-xs leading-5 text-muted-foreground sm:text-sm">
              先确认数据覆盖，再进入单个交易日的盘面回放。价格、均价、成交量和市场联动是主视图，BS
              点只作为可定位的证据。
            </p>
          </div>
          <div className="flex shrink-0 flex-wrap items-center gap-2 xl:justify-end">
            <div className="flex min-w-0 items-center gap-1.5 px-1 text-xs" title={materializationDetail}>
              <Layers3 className={cn("size-3.5", materializationRawStatus === "READY" ? "text-emerald-600" : materializationRawStatus === "FAILED" ? "text-destructive" : "text-muted-foreground")} aria-hidden="true" />
              <span className="font-medium text-foreground">{materializationLabel}</span>
              <span className="hidden max-w-40 truncate text-muted-foreground 2xl:inline">{materializationDetail}</span>
            </div>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => void materializeMarketState()}
              disabled={!tradeDate || materializationBusy}
            >
              <Layers3 className={materializationBusy ? "animate-pulse" : undefined} />
              {materializationBusy ? "触发中" : "手动物化"}
            </Button>
            <Link to="/personal-portfolio">
              <Button variant="outline" size="sm">
                <ExternalLink />
                交易记录
              </Button>
            </Link>
            <Button
              size="sm"
              onClick={() => setPanelOpen(true)}
              disabled={!symbol || !tradeDate || state.label === "不可用"}
            >
              <Activity />
              打开复盘面板
            </Button>
          </div>
        </header>

        <section
          className="grid gap-3 rounded-lg border border-border/70 bg-card p-3 md:grid-cols-[minmax(0,1.1fr)_minmax(0,1fr)] xl:grid-cols-[minmax(0,1.05fr)_minmax(0,0.95fr)_auto]"
          aria-label="复盘对象选择"
        >
          <label className="space-y-1.5 text-xs">
            <span className="font-medium text-foreground">标的</span>
            <div className="flex gap-2">
              <Input
                list="trade-review-symbols"
                value={symbol}
                onChange={(event) =>
                  setSymbol(event.target.value.toUpperCase())
                }
                placeholder="代码，如 600519 或 510300"
              />
              <datalist id="trade-review-symbols">
                {symbols.map((item) => (
                  <option key={item.symbol} value={item.symbol}>
                    {item.securityName ?? item.symbol}
                  </option>
                ))}
              </datalist>
              <select
                value={instrumentType}
                onChange={(event) =>
                  setInstrumentType(event.target.value as "STOCK" | "ETF")
                }
                className="h-9 rounded-md border border-border bg-muted/40 px-2 text-xs"
              >
                <option value="STOCK">A股</option>
                <option value="ETF">ETF</option>
              </select>
            </div>
          </label>
          <label className="space-y-1.5 text-xs">
            <span className="font-medium text-foreground">交易日</span>
            <div className="flex gap-2">
              <Input
                type="date"
                value={tradeDate}
                onChange={(event) => setTradeDate(event.target.value)}
              />
              <Button
                type="button"
                variant="outline"
                size="icon"
                onClick={() => void loadHealth(true)}
                disabled={healthLoading}
                title="检查数据"
              >
                <RefreshCw
                  className={healthLoading ? "animate-spin" : undefined}
                />
              </Button>
            </div>
          </label>
          <div className="grid items-end gap-2 sm:grid-cols-2 md:col-span-2 xl:col-span-1 xl:flex">
            <Button className="w-full xl:w-auto" variant="outline" onClick={() => void loadLocal()} disabled={loadBusy || !symbol || !tradeDate}>
              <Database />
              {loadBusy ? "正在加载本地 1m" : "Load 本地 1m"}
            </Button>
            <Button className="w-full xl:w-auto" variant="outline" onClick={() => void loadTimeshare()} disabled={timeshareLoadBusy || !symbol || !tradeDate}>
              <RefreshCw className={timeshareLoadBusy ? "animate-spin" : undefined} />
              {timeshareLoadBusy ? "归档中" : "Load 复盘分时"}
            </Button>
          </div>
        </section>

        <section
          className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4"
          aria-label="数据健康状态"
        >
          <HealthCard
            label="复盘数据"
            value={state.label}
            detail={state.detail}
            tone={state.tone}
            loading={healthLoading}
          />
          <HealthCard
            label="1m K 线"
            value={
              healthStock?.minute_bars?.["1m"]?.length
                ? `${healthStock.minute_bars["1m"]!.length} 点`
                : "缺失"
            }
            detail={healthStock?.source ?? "等待检查"}
            tone={
              healthStock?.minute_bars?.["1m"]?.length ? "positive" : "warning"
            }
          />
          <HealthCard
            label="分时与均价"
            value={
              healthStock?.timeshare?.length
                ? `${healthStock.timeshare.length} 点`
                : "缺失"
            }
            detail={(healthStock?.trade_date ?? tradeDate) || "等待检查"}
            tone={healthStock?.timeshare?.length ? "positive" : "warning"}
          />
          <HealthCard
            label="Market Indicator"
            value={healthIndicator?.dataStatus ?? "未覆盖"}
            detail={`${healthIndicator?.eventCount ?? 0} 个事件 · ${healthIndicator?.industryPointCount ?? 0} 条行业状态`}
            tone={
              healthIndicator?.dataStatus === "ready" ? "positive" : "warning"
            }
          />
        </section>

        <section className="grid min-w-0 items-start gap-4 xl:grid-cols-[minmax(0,1.2fr)_minmax(360px,0.8fr)]">
          <div className="min-w-0 space-y-4">
            <TradeActivityHeatmap trades={trades} />
            <div className="min-w-0 overflow-hidden rounded-lg border border-border/70 bg-card">
            <div className="flex items-center justify-between border-b border-border/60 px-4 py-3">
              <div>
                <h2 className="text-sm font-semibold">交易日历史</h2>
                <p className="mt-1 text-xs text-muted-foreground">
                  当前先按单日回放，历史跨度只用于选择，不跨日连续播放。
                </p>
              </div>
              <span className="font-mono text-xs text-muted-foreground">
                {tradeDates.length} 天
              </span>
            </div>
            {tradeDates.length ? (
              <div className="h-[456px] overflow-hidden p-3 sm:p-4">
                <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                  <div className="inline-flex items-center rounded-md bg-muted/60 p-0.5 text-[11px]">
                    {([ ["week", "周视图"], ["month", "月视图"], ["year", "年视图"] ] as const).map(([value, label]) => (
                      <button
                        key={value}
                        type="button"
                        className={cn("rounded px-2 py-1 transition-colors", historyView === value ? "bg-background font-medium text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground")}
                        onClick={() => setHistoryView(value)}
                      >
                        {label}
                      </button>
                    ))}
                  </div>
                  {historyView === "year" ? (
                    <div className="flex items-center gap-1">
                      <button type="button" className="inline-flex size-7 items-center justify-center rounded-md text-muted-foreground hover:bg-muted disabled:pointer-events-none disabled:opacity-35" aria-label="上一年" title="上一年" disabled={!tradeDates.some((date) => Number(date.slice(0, 4)) < historyYearValue)} onClick={() => setHistoryYear(historyYearValue - 1)}><ChevronLeft className="size-4" /></button>
                      <span className="min-w-16 text-center text-sm font-semibold">{historyYearValue}年</span>
                      <button type="button" className="inline-flex size-7 items-center justify-center rounded-md text-muted-foreground hover:bg-muted disabled:pointer-events-none disabled:opacity-35" aria-label="下一年" title="下一年" disabled={!tradeDates.some((date) => Number(date.slice(0, 4)) > historyYearValue)} onClick={() => setHistoryYear(historyYearValue + 1)}><ChevronRight className="size-4" /></button>
                    </div>
                  ) : null}
                {historyView !== "year" ? (
                  <div className="flex items-center gap-2">
                  <button
                    type="button"
                    className="inline-flex size-7 items-center justify-center rounded-md text-muted-foreground hover:bg-muted hover:text-foreground disabled:pointer-events-none disabled:opacity-35"
                    aria-label="上一个月份"
                    title="上一个月份"
                    disabled={historyMonthKey <= firstHistoryMonth}
                    onClick={() => setHistoryMonth(addCalendarMonths(historyMonthDate, -1))}
                  >
                    <ChevronLeft className="size-4" />
                  </button>
                  {historyView === "month" ? <div className="flex items-center gap-2 text-sm font-semibold"><CalendarDays className="size-4 text-muted-foreground" /><span>{historyMonthDate.getFullYear()}年{historyMonthDate.getMonth() + 1}月</span></div> : historyView === "week" ? <div className="flex items-center gap-2 text-sm font-semibold"><CalendarDays className="size-4 text-muted-foreground" /><span>{formatCalendarDate(historyWeekDays[0])} 至 {formatCalendarDate(historyWeekDays[6])}</span></div> : <span className="min-w-16 text-center text-sm font-semibold">{historyYearValue}年</span>}
                  <button
                    type="button"
                    className="inline-flex size-7 items-center justify-center rounded-md text-muted-foreground hover:bg-muted hover:text-foreground disabled:pointer-events-none disabled:opacity-35"
                    aria-label="下一个月份"
                    title="下一个月份"
                    disabled={historyMonthKey >= lastHistoryMonth}
                    onClick={() => setHistoryMonth(addCalendarMonths(historyMonthDate, 1))}
                  >
                    <ChevronRight className="size-4" />
                  </button>
                  </div>
                ) : null}
                </div>
                {historyView !== "year" ? <div className="mb-2 grid grid-cols-7 gap-1 text-center text-[10px] text-muted-foreground">
                  {["日", "一", "二", "三", "四", "五", "六"].map((label) => <span key={label}>{label}</span>)}
                </div> : null}
                {historyView !== "year" ? <div className="grid grid-cols-7 gap-1">
                  {(historyView === "week" ? historyWeekDays : historyDays).map((date) => {
                    const key = formatCalendarDate(date);
                    const dayTrades = historyTradeDates.get(key) ?? [];
                    const inMonth = historyView === "week" || date.getMonth() === historyMonthDate.getMonth();
                    const active = key === tradeDate;
                    const symbolsForDay = [...new Set(dayTrades.map((trade) => trade.symbol))];
                    return (
                      <button
                        key={key}
                        type="button"
                        disabled={!dayTrades.length}
                        aria-pressed={active}
                        aria-label={dayTrades.length ? `${key} ${dayTrades.length} 笔交易` : `${key} 无交易数据`}
                        title={dayTrades.length ? `${key} · ${symbolsForDay.join("、")} · ${dayTrades.length} 笔交易` : key}
                        onClick={() => {
                          if (!dayTrades.length) return;
                          setTradeDate(key);
                          setHistoryMonth(key.slice(0, 7));
                          setSymbol(dayTrades[0].symbol);
                          setInstrumentType(dayTrades[0].instrumentType);
                        }}
                        className={cn(
                          "flex min-h-[58px] min-w-0 flex-col items-center justify-center rounded-sm border border-border/40 px-0.5 py-1 text-center outline-none transition-[box-shadow,background-color,opacity] focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1 disabled:cursor-not-allowed",
                          dayTrades.length ? historyCountTone(dayTrades.length, historyTradeDates) : "bg-muted/20 text-muted-foreground/60",
                          inMonth ? "opacity-100" : "opacity-35",
                          active ? "ring-2 ring-information/30 ring-offset-1" : "",
                        )}
                      >
                        <span className="font-mono text-[10px]">{date.getDate()}</span>
                        {dayTrades.length ? (
                          <>
                            <span className="mt-0.5 font-mono text-[10px] font-semibold tabular-nums">{dayTrades.length} 笔</span>
                            <span className="mt-0.5 max-w-full truncate text-[9px] opacity-80">{symbolsForDay.slice(0, 2).join(" · ")}</span>
                          </>
                        ) : <span className="mt-0.5 text-[10px]">-</span>}
                      </button>
                    );
                  })}
                </div> : <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
                  {historyYearMonths.map((date) => {
                    const prefix = `${historyYearValue}-${String(date.getMonth() + 1).padStart(2, "0")}`;
                    const monthTrades = tradeDates.filter((item) => item.startsWith(prefix)).flatMap((item) => historyTradeDates.get(item) ?? []);
                    const monthCount = monthTrades.length;
                    return <button key={prefix} type="button" disabled={!monthCount} onClick={() => { setHistoryView("month"); setHistoryMonth(prefix); }} className={cn("flex min-h-20 flex-col items-center justify-center rounded-sm border border-border/40 px-2 py-2 text-center", monthCount ? historyCountTone(monthCount, historyTradeDates) : "bg-muted/20 text-muted-foreground/60")}><span className="text-xs font-medium">{date.getMonth() + 1}月</span><span className="mt-1 font-mono text-sm font-semibold tabular-nums">{monthCount ? `${monthCount} 笔` : "-"}</span></button>;
                  })}
                </div>}
              </div>
            ) : (
              <EmptyState text="还没有交易记录，可直接输入任意 A 股或 ETF 代码进行检查。" />
            )}
            </div>
          </div>
          <div className="min-w-0 space-y-3">
            <div className="min-w-0 rounded-lg border border-border/70 bg-card p-3">
            <div className="flex items-start justify-between gap-3 border-b border-border/60 pb-2">
              <div className="min-w-0">
                <h2 className="text-sm font-semibold">当前交易日摘要</h2>
                <p className="mt-1 font-mono text-xs text-muted-foreground">
                  {tradeDate || "尚未选择交易日"}
                </p>
              </div>
              <Badge
                className="shrink-0"
                variant={
                  state.tone === "positive"
                    ? "default"
                    : state.tone === "destructive"
                      ? "destructive"
                      : "secondary"
                }
            >
                {state.label}
              </Badge>
            </div>
            {currentDayTrades.length ? (
              <div className="mt-2 border-y border-border/60 py-1.5">
                <div className="flex items-center justify-between gap-2">
                  <div className="flex min-w-0 items-center gap-2">
                    <span className="shrink-0 text-[10px] font-medium text-muted-foreground">当日交易标的</span>
                    <span className="font-mono text-[9px] text-muted-foreground">{summaryGroups.length} 个</span>
                  </div>
                  <div className="flex items-center gap-2 text-[10px] text-muted-foreground">
                    {summaryPageCount > 1 ? <><span>{summaryPage + 1}/{summaryPageCount}</span><button type="button" aria-label="上一个标的页" title="上一页" className="inline-flex size-5 items-center justify-center rounded text-muted-foreground hover:bg-muted disabled:pointer-events-none disabled:opacity-35" disabled={summaryPage === 0} onClick={() => { setSummaryPage((page) => Math.max(0, page - 1)); setSummaryPrimaryKey(null); }}><ChevronLeft className="size-3" /></button><button type="button" aria-label="下一个标的页" title="下一页" className="inline-flex size-5 items-center justify-center rounded text-muted-foreground hover:bg-muted disabled:pointer-events-none disabled:opacity-35" disabled={summaryPage >= summaryPageCount - 1} onClick={() => { setSummaryPage((page) => Math.min(summaryPageCount - 1, page + 1)); setSummaryPrimaryKey(null); }}><ChevronRight className="size-3" /></button></> : null}
                  </div>
                </div>
                <div className="mt-1 flex flex-wrap gap-1">
                  {summaryGroups.map(([tradeKey, symbolTrades]) => {
                    const selected = visibleSummaryGroups[0]?.[0] === tradeKey;
                    const visible = visibleSummaryGroups.some(([key]) => key === tradeKey);
                    return (
                      <button
                        key={tradeKey}
                        type="button"
                        aria-pressed={selected}
                        onClick={() => selectSummarySymbol(tradeKey)}
                        className={cn("inline-flex min-w-0 max-w-full items-center gap-1 rounded border px-1.5 py-1 text-left transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring", selected ? "border-red-300 bg-red-50" : visible ? "border-border/60 bg-muted/35 hover:bg-muted/50" : "border-border/60 bg-background hover:bg-muted/50")}
                      >
                        <span className="max-w-[100px] truncate text-[10px] font-medium">{symbolTrades.find((trade) => trade.securityName?.trim())?.securityName?.trim() ?? symbolTrades[0].symbol}</span>
                        <span className="shrink-0 font-mono text-[9px] text-muted-foreground">{symbolTrades[0].symbol}</span>
                        <span className="shrink-0 font-mono text-[9px] text-muted-foreground">{symbolTrades.length}笔</span>
                      </button>
                    );
                  })}
                </div>
              </div>
            ) : null}
            {currentDayTrades.length ? (
              visibleSummaryGroups.map(([tradeKey, symbolTrades]) => {
                const summaryStock = summaryStockFor(symbolTrades[0]);
                return (
                <TradeSummaryGroup
                  key={tradeKey}
                  name={symbolTrades.find((trade) => trade.securityName?.trim())?.securityName?.trim() ?? selectedSecurityName}
                  symbol={symbolTrades[0].symbol}
                  instrumentType={symbolTrades[0]?.instrumentType ?? instrumentType}
                  trades={symbolTrades}
                  timeshare={summaryTimeshare(summaryStock)}
                  summaryLoading={summaryStock == null && summaryStocks[tradeKey] === undefined}
                  preClose={summaryStock?.pre_close ?? null}
                  onOpenPanel={() => setPanelOpen(true)}
                />
                );
              })
            ) : (
              <div className="mt-2 rounded-md border border-border/60 bg-background px-3 py-4 text-center text-[11px] text-muted-foreground">
                当前交易日暂无 B/S 记录
              </div>
            )}
            <div className="mt-2 flex items-start gap-2 border-t border-border/60 pt-2 text-[10px] leading-4 text-muted-foreground">
              <ClipboardCheck className="mt-0.5 size-3.5 shrink-0 text-information" aria-hidden="true" />
              <p>
                日级数据按 {tradeDate || "当前日期"} 截断；当天缺失的两融数据只展示最近可用趋势。
              </p>
            </div>
            </div>
            <OverallPerformanceMetrics performance={overallTradePerformance} />
          </div>
        </section>
      </div>
      <TradeReviewPanel
        open={panelOpen}
        onOpenChange={setPanelOpen}
        symbol={symbol}
        instrumentType={instrumentType}
        tradeDate={tradeDate}
        onTradeDateChange={(nextDate) => {
          setTradeDate(nextDate);
          if (nextDate) setHistoryMonth(nextDate.slice(0, 7));
        }}
        securityName={
          symbols.find((item) => item.symbol === symbol)?.securityName ?? undefined
        }
        symbols={symbols}
        availableTradeDates={tradeDates}
        onSymbolChange={(nextSymbol, nextInstrumentType) => {
          setSymbol(nextSymbol);
          setInstrumentType(nextInstrumentType);
        }}
        trades={selectedTrades}
        initialStock={healthStock}
        initialIndicator={healthIndicator}
      />
    </WorkspaceShell>
  );
}

function HealthCard({
  label,
  value,
  detail,
  tone,
  loading,
}: {
  label: string;
  value: string;
  detail: string;
  tone: "positive" | "warning" | "destructive";
  loading?: boolean;
}) {
  return (
    <div className="flex h-[68px] min-w-0 flex-col justify-between rounded-md border border-border/70 bg-card px-3 py-2">
      <div className="flex items-center justify-between gap-2 text-[11px] text-muted-foreground">
        <span>{label}</span>
        {loading ? (
          <Loader2 className="size-3 animate-spin" />
        ) : (
          <span
            className={`size-1.5 rounded-full ${tone === "positive" ? "bg-emerald-500" : tone === "destructive" ? "bg-rose-500" : "bg-amber-500"}`}
          />
        )}
      </div>
      <div className="flex min-w-0 items-baseline justify-between gap-2">
        <p className="shrink-0 text-sm font-semibold">{value}</p>
        <p className="min-w-0 truncate text-right text-[10px] text-muted-foreground">
          {detail}
        </p>
      </div>
    </div>
  );
}

function summaryTimeshare(stock: StockIntradayResponse | null) {
  if (!stock) return [] as StockIntradayPoint[];
  if (stock.timeshare.length > 1) {
    const byTimestamp = new Map<number, StockIntradayPoint>();
    for (const point of stock.timeshare) {
      const timestamp = epoch(point.timestamp);
      if (timestamp > 0 && Number.isFinite(point.price)) {
        byTimestamp.set(timestamp, { ...point, timestamp });
      }
    }
    return [...byTimestamp.values()].sort((left, right) => left.timestamp - right.timestamp);
  }

  const bars = stock.minute_bars["1m"] ?? [];
  let cumulativeVolume = 0;
  let cumulativeTurnover = 0;
  return bars.map((bar) => {
    const volume = Number.isFinite(bar.volume) ? bar.volume : 0;
    const turnover = Number.isFinite(bar.turnover ?? Number.NaN)
      ? bar.turnover ?? 0
      : bar.close * volume;
    cumulativeVolume += volume;
    cumulativeTurnover += turnover;
    return {
      timestamp: bar.timestamp,
      trade_date: bar.trade_date ?? stock.trade_date ?? stock.requested_trade_date,
      time_label: formatTime(bar.timestamp).slice(0, 5),
      price: bar.close,
      open: bar.open,
      avg_price: cumulativeVolume > 0 ? cumulativeTurnover / cumulativeVolume : bar.close,
      volume,
      turnover,
    };
  });
}

function TradeSummaryGroup({
  name,
  symbol,
  instrumentType,
  trades,
  timeshare,
  summaryLoading,
  preClose,
  onOpenPanel,
}: {
  name: string;
  symbol: string;
  instrumentType: "STOCK" | "ETF";
  trades: PortfolioTrade[];
  timeshare: StockIntradayPoint[];
  summaryLoading: boolean;
  preClose: number | null;
  onOpenPanel: () => void;
}) {
  const chartPoints = timeshare.filter((point) => Number.isFinite(point.price));

  return (
    <section className="mt-2 overflow-hidden rounded-md border border-border/60 bg-background">
      <div className="flex items-center justify-between gap-3 border-b border-border/60 bg-muted/25 px-2.5 py-1.5">
        <div className="min-w-0">
          <h3 className="truncate text-xs font-semibold">{name}</h3>
          <p className="mt-0.5 truncate font-mono text-[10px] text-muted-foreground">
            {symbol} · {instrumentType} · {trades.length} 笔 B/S
          </p>
        </div>
        <button type="button" className="shrink-0 text-[10px] text-information hover:underline" onClick={onOpenPanel}>
          打开复盘
        </button>
      </div>
      <div className="px-2.5 pt-1">
        {chartPoints.length ? (
          <TradeSummaryChart points={chartPoints} trades={trades} preClose={preClose} name={`${name} ${symbol}`} />
        ) : summaryLoading ? (
          <div className="flex h-[122px] items-center justify-center text-[11px] text-muted-foreground">正在加载分时数据…</div>
        ) : (
          <div className="flex h-[122px] items-center justify-center text-[11px] text-muted-foreground">
            暂无分时数据，下面保留 B/S 记录
          </div>
        )}
      </div>
      <div className="overflow-x-auto border-t border-border/60">
        <div className="grid min-w-[420px] grid-cols-[38px_58px_58px_58px_68px] items-center gap-2 bg-muted/15 px-2.5 py-1 text-[10px] text-muted-foreground">
          <span>方向</span><span>时间</span><span className="text-right">价格</span><span className="text-right">数量</span><span className="text-right">金额</span>
        </div>
        <div className="min-w-[420px] divide-y divide-border/50">
          {trades.map((trade) => {
            const isBuy = trade.side === "BUY";
              return (
              <button key={trade.id} type="button" className="grid w-full grid-cols-[38px_58px_58px_58px_68px] items-center gap-2 px-2.5 py-1 text-left text-[11px] hover:bg-muted/40" onClick={onOpenPanel}>
                <span className={cn("w-fit rounded px-1.5 py-0.5 font-mono text-[10px] font-bold", isBuy ? "bg-red-500/10 text-red-600" : "bg-emerald-500/10 text-emerald-600")}>{isBuy ? "B" : "S"}</span>
                <span className="font-mono text-muted-foreground">{formatTime(epoch(trade.tradedAt))}</span>
                <span className="text-right font-mono">{displayNumber(trade.price)}</span>
                <span className="text-right font-mono">{displayVolume(trade.quantity)}</span>
                <span className="text-right font-mono">{displayVolume(trade.price * trade.quantity)}</span>
              </button>
            );
          })}
        </div>
      </div>
    </section>
  );
}

function OverallPerformanceMetrics({ performance }: { performance: TradePerformance }) {
  if (!performance.closedTrades) {
    return (
      <section className="min-w-0 rounded-lg border border-border/70 bg-card p-3" aria-label="历史交易表现">
        <div className="flex items-start justify-between gap-3 border-b border-border/60 pb-2">
          <div>
            <h2 className="text-sm font-semibold">历史交易表现</h2>
            <p className="mt-0.5 text-[10px] text-muted-foreground">全部交易日 · 按标的 FIFO 配对</p>
          </div>
          <span className="shrink-0 rounded bg-amber-50 px-1.5 py-0.5 text-[9px] text-amber-700">待完成</span>
        </div>
        <p className="mt-2 text-[10px] text-muted-foreground">暂无已完成的买卖配对，暂时无法计算胜率和盈亏比。</p>
      </section>
    );
  }
  const winRate = `${(performance.winRate! * 100).toFixed(0)}%`;
  const ratio = performance.profitLossRatio == null
    ? "-"
    : Number.isFinite(performance.profitLossRatio)
      ? performance.profitLossRatio.toFixed(2)
      : "∞";
  return (
    <section className="min-w-0 rounded-lg border border-border/70 bg-card p-3" aria-label="历史交易表现">
      <div className="flex items-start justify-between gap-3 border-b border-border/60 pb-2">
        <div>
          <h2 className="text-sm font-semibold">历史交易表现</h2>
          <p className="mt-0.5 text-[10px] text-muted-foreground">全部交易日 · 按标的 FIFO 配对</p>
        </div>
        <span className="shrink-0 rounded bg-muted px-1.5 py-0.5 text-[9px] text-muted-foreground">{performance.closedTrades} 个配对</span>
      </div>
      <div className="mt-2 grid grid-cols-2 gap-2">
        <div className="rounded-md bg-background px-2 py-1.5">
          <span className="block text-[10px] text-muted-foreground">交易胜率</span>
          <strong className="mt-0.5 block font-mono text-sm font-semibold">{winRate}</strong>
        </div>
        <div className="rounded-md bg-background px-2 py-1.5">
          <span className="block text-[10px] text-muted-foreground">盈亏比</span>
          <strong className="mt-0.5 block font-mono text-sm font-semibold">{ratio}</strong>
        </div>
      </div>
      <p className="mt-2 text-[10px] text-muted-foreground">盈利 {performance.winningTrades} 笔 · 亏损 {performance.closedTrades - performance.winningTrades} 笔</p>
    </section>
  );
}

function Summary({ label, value, signal }: { label: string; value: string; signal?: FinancialSignal | null }) {
  return (
    <div className="min-w-0 rounded-md bg-background px-2 py-1.5">
      <div className="flex items-center justify-between gap-1">
        <span className="truncate text-[10px] text-muted-foreground">{label}</span>
        <FinancialSignalBadge signal={signal} />
      </div>
      <span className="mt-1 block truncate font-mono text-xs font-medium">{value}</span>
    </div>
  );
}
function EmptyState({ text }: { text: string }) {
  return (
    <div className="flex min-h-[170px] items-center justify-center px-4 text-center text-xs text-muted-foreground">
      {text}
    </div>
  );
}

interface PanelProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  symbol: string;
  instrumentType: "STOCK" | "ETF";
  tradeDate: string;
  onTradeDateChange: (tradeDate: string) => void;
  securityName?: string;
  symbols: PortfolioTrade[];
  availableTradeDates: string[];
  onSymbolChange: (symbol: string, instrumentType: "STOCK" | "ETF") => void;
  trades: PortfolioTrade[];
  initialStock?: StockIntradayResponse | null;
  initialIndicator?: MarketIndicatorReplayTimelineResponse | null;
}

export function TradeReviewPanel({
  open,
  onOpenChange,
  symbol,
  instrumentType,
  tradeDate,
  onTradeDateChange,
  securityName,
  symbols,
  availableTradeDates,
  onSymbolChange,
  trades,
  initialStock,
  initialIndicator,
}: PanelProps) {
  const [stock, setStock] = useState<StockIntradayResponse | null>(null);
  const [index, setIndex] = useState<StockIntradayResponse | null>(null);
  const [industry, setIndustry] = useState<StockIntradayResponse | null>(null);
  const [industryName, setIndustryName] = useState<string | null>(null);
  const [manualSector, setManualSector] = useState<SectorContext | null>(null);
  const [industryTargets, setIndustryTargets] = useState<IndustryApplicationTargetCode[]>([]);
  const [industryTargetsLoading, setIndustryTargetsLoading] = useState(false);
  const [industryPickerOpen, setIndustryPickerOpen] = useState(false);
  const [capitalDialogOpen, setCapitalDialogOpen] = useState(false);
  const [fundFlowDialogOpen, setFundFlowDialogOpen] = useState(false);
  const [industryFlowRows, setIndustryFlowRows] = useState<IndustryFundFlowRow[]>([]);
  const [industryReplayCapital, setIndustryReplayCapital] =
    useState<IndustryApplicationTdxFundFlowResponse | null>(null);
  const [industryReplayDetail, setIndustryReplayDetail] =
    useState<MarketIndicatorIndustryDetail | null>(null);
  const [industryReplayTimeline, setIndustryReplayTimeline] =
    useState<MarketIndicatorReplayTimelineResponse | null>(null);
  const [industryBoardSummary, setIndustryBoardSummary] =
    useState<EasyTdxBoardSummary | null>(null);
  const [industryReplayLoading, setIndustryReplayLoading] = useState(false);
  const [industryReplayError, setIndustryReplayError] = useState<string | null>(null);
  const [meta, setMeta] = useState<Awaited<
    ReturnType<typeof fetchStockMeta>
  > | null>(null);
  const [metaLoading, setMetaLoading] = useState(false);
  const [sectors, setSectors] = useState<Awaited<
    ReturnType<typeof fetchStockSectors>
  > | null>(null);
  const [sectorsLoading, setSectorsLoading] = useState(false);
  const [tdxBoards, setTdxBoards] = useState<EasyTdxResponse<EasyTdxBelongBoardRow> | null>(null);
  const [tdxBoardsLoading, setTdxBoardsLoading] = useState(false);
  const [timeline, setTimeline] =
    useState<MarketIndicatorReplayTimelineResponse | null>(null);
  const [anchors, setAnchors] = useState<PortfolioReviewAnchor[]>([]);
  const [margin, setMargin] = useState<MarginStockDetail[]>([]);
  const [finance, setFinance] = useState<StockFinanceReportResponse[]>([]);
  const [valuation, setValuation] = useState<StockValuationResponse | null>(
    null,
  );
  const [businessComposition, setBusinessComposition] =
    useState<StockBusinessCompositionResponse | null>(null);
  const [profitForecast, setProfitForecast] =
    useState<StockProfitForecastResponse | null>(null);
  const [companyResearch, setCompanyResearch] =
    useState<StockCompanyNewsResponse | null>(null);
  const [financeDetailsLoading, setFinanceDetailsLoading] = useState(false);
  const [financeDetailsError, setFinanceDetailsError] = useState<string | null>(
    null,
  );
  const [financeLoading, setFinanceLoading] = useState(false);
  const [financeError, setFinanceError] = useState<string | null>(null);
  const [fundFlow, setFundFlow] = useState<Awaited<
    ReturnType<typeof fetchStockFundFlow>
  > | null>(null);
  const [tradeTicks, setTradeTicks] = useState<HistoricalTradeTick[]>([]);
  const [tradeTicksLoading, setTradeTicksLoading] = useState(false);
  const [historicalDailyBars, setHistoricalDailyBars] = useState<
    Record<string, StockKlineBar[]>
  >({});
  const contentLayout = useDefaultLayout({
    id: "cynexus.trade-review.content",
    onlySaveAfterUserInteractions: true,
  });
  const [period, setPeriod] = useState<ReplayPeriod>("timeshare");
  const [historicalWindowDays, setHistoricalWindowDays] = useState(60);
  const [cursor, setCursor] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [speed, setSpeed] = useState(1);
  const [relativeMode, setRelativeMode] = useState(false);
  const [selectedMarker, setSelectedMarker] = useState<ReplayMarker | null>(
    null,
  );
  const [note, setNote] = useState("");
  const [tags, setTags] = useState("");
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(false);
  const [periodLoading, setPeriodLoading] = useState(false);
  const [tab, setTab] = useState("交易与复盘");
  const [targetPickerOpen, setTargetPickerOpen] = useState(false);
  const [targetQuery, setTargetQuery] = useState("");
  const [targetSearchResults, setTargetSearchResults] = useState<StockSearchItem[]>([]);
  const [targetSearchLoading, setTargetSearchLoading] = useState(false);
  const periodLoadAttempts = useRef(new Set<string>());
  const historicalBarsRequested = useRef(new Set<string>());
  const pendingMinuteReplayDate = useRef<string | null>(null);
  const financeDetailsLoadedKey = useRef<string | null>(null);
  const automaticPeriodFallbackKey = useRef<string | null>(null);
  const reviewDateOptions = useMemo(
    () => [...new Set([tradeDate, ...availableTradeDates].filter(Boolean))].sort(
      (left, right) => right.localeCompare(left),
    ),
    [availableTradeDates, tradeDate],
  );

  useEffect(() => {
    if (!open) return;
    let active = true;
    setIndustryTargetsLoading(true);
    void fetchIndustryApplicationTargetCodes()
      .then((result) => {
        if (active) setIndustryTargets(result.items ?? []);
      })
      .catch(() => {
        if (active) setIndustryTargets([]);
      })
      .finally(() => {
        if (active) setIndustryTargetsLoading(false);
      });
    return () => {
      active = false;
    };
  }, [open]);

  useEffect(() => {
    const replayKey = `${symbol}:${tradeDate}`;
    if (
      !open ||
      period !== "timeshare" ||
      automaticPeriodFallbackKey.current === replayKey ||
      stock?.timeshare.length ||
      !stock?.minute_bars["1m"]?.length
    ) {
      return;
    }
    automaticPeriodFallbackKey.current = replayKey;
    setPeriod("1m");
  }, [open, period, stock, symbol, tradeDate]);
  useEffect(() => {
    if (!targetPickerOpen) return;
    const query = targetQuery.trim();
    if (!query) {
      setTargetSearchResults([]);
      setTargetSearchLoading(false);
      return;
    }
    let active = true;
    const timer = window.setTimeout(() => {
      setTargetSearchLoading(true);
      void searchStockChart(query)
        .then((items) => {
          if (active) {
            setTargetSearchResults(
              items.filter(
                (item) => item.target_type === "stock" || item.target_type === "etf",
              ),
            );
          }
        })
        .catch(() => {
          if (active) setTargetSearchResults([]);
        })
        .finally(() => {
          if (active) setTargetSearchLoading(false);
        });
    }, 220);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [targetPickerOpen, targetQuery]);
  const targetOptions = useMemo<StockSearchItem[]>(() => {
    const portfolioOptions = symbols.map((item) => ({
      target_type: item.instrumentType === "ETF" ? ("etf" as const) : ("stock" as const),
      symbol: item.symbol,
      name: item.securityName ?? item.symbol,
    }));
    const merged = [...portfolioOptions, ...targetSearchResults];
    return [...new Map(merged.map((item) => [`${item.target_type}-${item.symbol}`, item])).values()];
  }, [symbols, targetSearchResults]);
  const displayName = useMemo(() => {
    const apiName = stock?.name?.trim();
    const metaName = meta?.name?.trim();
    const recordName =
      securityName?.trim() ||
      trades.find((trade) => trade.securityName?.trim())?.securityName?.trim();
    return [apiName, metaName, recordName].find(
      (name) => name && name !== symbol,
    ) || symbol || "交易日盘面";
  }, [meta?.name, securityName, stock?.name, symbol, trades]);
  const industryTarget = useMemo<IndustryApplicationTargetCode | null>(() => {
    const contextCode =
      manualSector?.symbol ?? industry?.symbol ?? meta?.sectorIndexSymbol ?? "";
    const contextName =
      manualSector?.name ?? industryName ?? meta?.sectorIndexName ?? meta?.industry ?? "";
    const normalizeCode = (value: string) => value.trim().replace(/^(sh|sz)/i, "");
    const normalizedContextCode = normalizeCode(contextCode);
    const matched = industryTargets.find(
      (item) =>
        (!manualSector?.kind || item.kind === manualSector.kind) &&
        ((normalizedContextCode && normalizeCode(item.code) === normalizedContextCode) ||
          (contextName && item.name === contextName)),
    );
    if (matched) return matched;
    if (!contextCode || !contextName) return null;
    return {
      code: contextCode,
      name: contextName,
      kind: manualSector?.kind ?? (/^(880|881)/.test(normalizedContextCode) ? "industry" : "index"),
      tdx_fund_flow_supported: manualSector?.kind === "concept"
        ? false
        : normalizedContextCode.startsWith("880"),
    };
  }, [industry?.symbol, industryName, industryTargets, manualSector, meta]);
  const isConceptTarget = industryTarget?.kind === "concept";

  useEffect(() => {
    if (!open) return;

    const handleShortcut = (event: KeyboardEvent) => {
      if (event.isComposing) return;

      if (event.key === "Escape") {
        event.preventDefault();
        onOpenChange(false);
        return;
      }

      if (
        !event.altKey ||
        event.ctrlKey ||
        event.metaKey ||
        event.shiftKey
      ) {
        return;
      }

      const tabIndex = Number(event.key) - 1;
      const nextTab = tradeReviewPanelTabs[tabIndex];
      if (!nextTab || (nextTab === "资金流" && isConceptTarget)) return;

      event.preventDefault();
      setTab(nextTab);
    };

    window.addEventListener("keydown", handleShortcut, true);
    return () => window.removeEventListener("keydown", handleShortcut, true);
  }, [isConceptTarget, onOpenChange, open]);

  useEffect(() => {
    // Concept boards do not expose historical daily K, but the selected
    // stock/ETF still has its own intraday series. Never force or disable the
    // individual instrument's timeshare view because the sector context is a
    // concept.
    if (isConceptTarget && isHistoricalReplayPeriod(period)) {
      setPeriod("1m");
    }
    if (isConceptTarget && tab === "资金流") setTab("行业");
  }, [isConceptTarget, period, tab]);
  const industryPickerOptions = useMemo<IndustryPickerOption[]>(() => {
    const normalizeCode = (value: string | null | undefined) =>
      String(value ?? "").trim().replace(/^(sh|sz)/i, "");
    const options = new Map<string, IndustryPickerOption>();
    const addOption = (
      codeValue: string | null | undefined,
      name: string | null | undefined,
      sourceLabel: string,
      kind: IndustryApplicationTargetCode["kind"] = "industry",
    ) => {
      const code = normalizeCode(codeValue);
      if (!/^\d{6}$/.test(code) || !name?.trim()) return;
      const canonical = industryTargets.find(
        (item) => normalizeCode(item.code) === code && item.kind === kind,
      );
      const existing = options.get(code);
      const mergedSource = existing?.sourceLabel && existing.sourceLabel !== sourceLabel
        ? [existing.sourceLabel, sourceLabel].filter((value, index, values) => values.indexOf(value) === index).join(" / ")
        : sourceLabel;
      const base: IndustryApplicationTargetCode = canonical ?? {
        code: `sh${code}`,
        name: name.trim(),
        kind,
        tdx_fund_flow_supported: kind === "industry" && code.startsWith("880"),
      };
      options.set(code, { ...base, sourceLabel: mergedSource });
    };

    for (const row of sectors?.industries ?? []) {
      const source = String(row.source ?? "");
      if (source.includes("ths_industry_constituent_snapshot")) {
        addOption(row.topic_id, row.name, "THS");
      }
    }
    for (const row of tdxBoards?.items ?? []) {
      const kind = isTdxIndustryBoard(row)
        ? "industry"
        : row.board_type === 4
          ? "concept"
          : null;
      if (kind) addOption(row.board_code, row.board_name, "TDX", kind);
    }
    if (industryTarget && !options.has(normalizeCode(industryTarget.code))) {
      addOption(industryTarget.code, industryTarget.name, "当前", industryTarget.kind);
    }
    return [...options.values()];
  }, [industryTarget, industryTargets, sectors?.industries, tdxBoards?.items]);
  const industryPickerLoading = sectorsLoading || tdxBoardsLoading || industryTargetsLoading;
  const activeIndustrySource = industryPickerOptions.find(
    (item) => item.code.replace(/^(sh|sz)/i, "") === industryTarget?.code.replace(/^(sh|sz)/i, ""),
  )?.sourceLabel;
  const industryBoardQuote = useMemo(() => {
    const code = industryTarget?.code.replace(/^(sh|sz|bj)/i, "");
    if (!code) return null;
    return tdxBoards?.items.find((item) => item.board_code === code) ?? null;
  }, [industryTarget?.code, tdxBoards?.items]);

  useEffect(() => {
    if (!open || tab !== "行业" || !industryTarget) return;
    let cancelled = false;
    setIndustryReplayLoading(true);
    setIndustryReplayError(null);
    setIndustryReplayCapital(null);
    setIndustryReplayDetail(null);
    setIndustryReplayTimeline(null);
    setIndustryBoardSummary(null);

    const normalizedName = (value: string | null | undefined) =>
      String(value ?? "").trim().replace(/行业$/u, "");

    void (async () => {
      const errors: string[] = [];
      let capital: IndustryApplicationTdxFundFlowResponse | null = null;
      let detail: MarketIndicatorIndustryDetail | null = null;

      if (!capital) {
        // A TDX detail snapshot is preferred. When it is absent, ask Easy-TDX
        // for the board members directly; this works for non-56 TDX boards as
        // well and does not depend on a persisted sector snapshot.
        const code6 = industryTarget.code.replace(/^(sh|sz|bj)/i, "");
        try {
          const [membersResult, summaryResult] = await Promise.allSettled([
            fetchEasyTdxBoardMembers({
              boardSymbol: code6,
              count: 1000,
            }),
            isConceptTarget ? fetchEasyTdxBoardSummary(code6) : Promise.resolve(null),
          ]);
          const tdxMembers = membersResult.status === "fulfilled" ? membersResult.value : null;
          const boardSummary = summaryResult.status === "fulfilled" ? summaryResult.value : null;
          const latestReplayDate = stock?.trade_date ?? stock?.requested_trade_date;
          const canUseLiveQuote = tradeDate === localDate(new Date().toISOString()) || latestReplayDate === tradeDate;
          if (!cancelled && isConceptTarget) setIndustryBoardSummary(canUseLiveQuote ? boardSummary : null);
          if (tdxMembers?.items.length) {
            const numberValue = (row: Record<string, unknown>, keys: string[]) => {
              for (const key of keys) {
                const value = Number(row[key]);
                if (Number.isFinite(value)) return value;
              }
              return null;
            };
            const textValue = (row: Record<string, unknown>, keys: string[]) => {
              for (const key of keys) {
                const value = String(row[key] ?? "").trim();
                if (value) return value;
              }
              return null;
            };
            const baseCapital: IndustryApplicationTdxFundFlowResponse = capital ?? {
              applicable: false,
              ok: false,
              history: [],
              ths_mappings: [],
              constituents: [],
              error: null,
            };
            const latestReplayDate = stock?.trade_date ?? stock?.requested_trade_date;
            const canUseLiveQuote = tradeDate === localDate(new Date().toISOString()) || latestReplayDate === tradeDate;
            capital = {
              ...baseCapital,
              constituents: tdxMembers.items
                .map((row) => {
                  const code = textValue(row, ["code", "symbol"]);
                  if (!code) return null;
                  const amount = numberValue(row, ["amount", "amount_yuan"]);
                  const price = numberValue(row, ["close", "last_price", "price"]);
                  const preClose = numberValue(row, ["pre_close", "preClose"]);
                  const change = numberValue(row, ["change_pct"]) ?? (
                    price != null && preClose != null && preClose !== 0
                      ? ((price - preClose) / preClose) * 100
                      : null
                  );
                  const turnover = numberValue(row, ["turnover", "turnover_rate"]);
                  return {
                    code,
                    name: textValue(row, ["name", "stock_name"]) ?? code,
                    price: canUseLiveQuote ? price : null,
                    change_pct: canUseLiveQuote ? change : null,
                    turnover_pct: canUseLiveQuote ? turnover : null,
                    amount_text:
                      canUseLiveQuote && amount != null ? displayVolume(amount) : null,
                    open: canUseLiveQuote ? numberValue(row, ["open"]) : null,
                    high: canUseLiveQuote ? numberValue(row, ["high"]) : null,
                    low: canUseLiveQuote ? numberValue(row, ["low"]) : null,
                    pre_close: canUseLiveQuote ? preClose : null,
                    volume: canUseLiveQuote ? numberValue(row, ["vol", "volume"]) : null,
                    amount: canUseLiveQuote ? amount : null,
                    vol_ratio: canUseLiveQuote ? numberValue(row, ["vol_ratio", "volume_ratio"]) : null,
                    eps: canUseLiveQuote ? numberValue(row, ["eps"]) : null,
                    net_assets: canUseLiveQuote ? numberValue(row, ["net_assets", "net_asset"]) : null,
                    pe_dynamic: canUseLiveQuote ? numberValue(row, ["pe_dynamic"]) : null,
                    pe_ttm: canUseLiveQuote ? numberValue(row, ["pe_ttm"]) : null,
                    pe_static: canUseLiveQuote ? numberValue(row, ["pe_static"]) : null,
                    main_net_amount: canUseLiveQuote ? numberValue(row, ["main_net_amount", "main_net"]) : null,
                    // board-members is a live membership endpoint. Do not
                    // claim its quote belongs to an older replay date.
                    quote_trade_date: canUseLiveQuote ? tradeDate : null,
                    sources: ["tdx", "easy_tdx.mac"],
                  };
                })
                .filter((item): item is NonNullable<typeof item> => Boolean(item)),
              source: "easy_tdx.mac.board-members",
            };
            if (!cancelled && capital.constituents.length) {
              setIndustryReplayCapital(capital);
              setIndustryReplayLoading(false);
            }
          }

          if (!capital?.constituents.length && isConceptTarget && canUseLiveQuote && boardSummary?.members?.length) {
            const numberValue = (row: Record<string, unknown>, keys: string[]) => {
              for (const key of keys) {
                const value = Number(row[key]);
                if (Number.isFinite(value)) return value;
              }
              return null;
            };
            const textValue = (row: Record<string, unknown>, keys: string[]) => {
              for (const key of keys) {
                const value = String(row[key] ?? "").trim();
                if (value) return value;
              }
              return null;
            };
            capital = {
              applicable: false,
              ok: true,
              history: [],
              ths_mappings: [],
              constituents: boardSummary.members.map((row) => {
                const price = numberValue(row, ["close", "price"]);
                const preClose = numberValue(row, ["pre_close"]);
                return {
                  code: textValue(row, ["code", "symbol"]) ?? "",
                  name: textValue(row, ["name", "stock_name"]) ?? "-",
                  price,
                  change_pct: price != null && preClose ? ((price - preClose) / preClose) * 100 : null,
                  turnover_pct: numberValue(row, ["turnover", "turnover_rate"]),
                  amount_text: displayVolume(numberValue(row, ["amount"])),
                  open: numberValue(row, ["open"]),
                  high: numberValue(row, ["high"]),
                  low: numberValue(row, ["low"]),
                  pre_close: preClose,
                  volume: numberValue(row, ["vol", "volume"]),
                  amount: numberValue(row, ["amount"]),
                  vol_ratio: numberValue(row, ["vol_ratio", "volume_ratio"]),
                  eps: numberValue(row, ["eps"]),
                  net_assets: numberValue(row, ["net_assets"]),
                  pe_dynamic: numberValue(row, ["pe_dynamic"]),
                  pe_ttm: numberValue(row, ["pe_ttm"]),
                  pe_static: numberValue(row, ["pe_static"]),
                  main_net_amount: numberValue(row, ["main_net_amount"]),
                  quote_trade_date: tradeDate,
                  sources: ["tdx", "easy_tdx.mac.board-summary"],
                };
              }).filter((item) => item.code),
              source: "easy_tdx.mac.board-summary",
            };
          }

          if (!capital?.constituents.length) {
            // A TDX detail snapshot is preferred, but the sector relation table
            // is still a valid constituent source. Use it as a fallback so an
            // unavailable snapshot or fund-flow row cannot blank the industry
            // replay slot.
            const [topicResult, nameResult] = await Promise.allSettled([
              fetchSectorStocks({ topicId: code6, limit: 1000 }),
              fetchSectorStocks({ name: industryTarget.name, limit: 1000 }),
            ]);
            const sectorStocks =
              topicResult.status === "fulfilled" && topicResult.value.stocks.length
                ? topicResult.value
                : nameResult.status === "fulfilled"
                  ? nameResult.value
                  : { stocks: [] };
            if (sectorStocks.stocks.length) {
              const baseCapital: IndustryApplicationTdxFundFlowResponse = capital ?? {
                applicable: false,
                ok: false,
                history: [],
                ths_mappings: [],
                constituents: [],
                error: null,
              };
              const constituentSource = code6.startsWith("880") ? "tdx" : "sector_mapping";
              capital = {
                ...baseCapital,
                constituents: sectorStocks.stocks.map((item) => ({
                  code: item.symbol,
                  name: item.name,
                  price: item.last_price,
                  change_pct: item.change_pct,
                  turnover_pct: item.turnover_rate,
                  amount_text: item.amount_wan == null ? null : `${item.amount_wan.toFixed(2)}万`,
                  quote_trade_date: item.trade_date,
                  sources: [constituentSource, item.source].filter(
                    (value): value is string => Boolean(value),
                  ),
                })),
                source: baseCapital.source ?? "sec_stock_sector",
              };
            }
          }
        } catch {
          // Keep the normal empty state when neither snapshot nor mapping data
          // is available. Fund-flow errors remain isolated to Capital.
        }
      }

      if (!cancelled && capital?.constituents.length) {
        setIndustryReplayCapital(capital);
        setIndustryReplayLoading(false);
      }

      const primaryThsCode = isConceptTarget ? null :
        capital?.ths_mappings.find((item) => item.is_primary)?.ths_code ??
        capital?.ths_mappings[0]?.ths_code ??
        industryFlowRows.find(
          (row) => normalizedName(row["行业"]) === normalizedName(industryTarget.name),
        )?.code ??
        null;

      let replayTimeline: MarketIndicatorReplayTimelineResponse | null = null;
      if (primaryThsCode) {
        const [timelineResult, detailResult] = await Promise.allSettled([
          fetchMarketIndicatorReplayTimeline({
            tradeDate,
            from: "09:30",
            to: "15:00",
            industryCode: primaryThsCode,
            limit: 10000,
          }),
          capital?.constituents.length
            ? Promise.resolve(null)
            : fetchMarketIndicatorIndustryDetail({
                industryCode: primaryThsCode,
                tradeDate,
                page: 0,
                pageSize: 200,
              }),
        ]);
        if (timelineResult.status === "fulfilled") {
          replayTimeline = timelineResult.value;
        } else {
          errors.push(
            timelineResult.reason instanceof Error
              ? timelineResult.reason.message
              : String(timelineResult.reason),
          );
        }
        if (detailResult.status === "fulfilled") {
          detail = detailResult.value;
        } else {
          errors.push(
            detailResult.reason instanceof Error
              ? detailResult.reason.message
              : String(detailResult.reason),
          );
        }
      }

      if (cancelled) return;
      setIndustryReplayCapital(capital);
      setIndustryReplayDetail(detail);
      setIndustryReplayTimeline(replayTimeline);
      setIndustryReplayError(
        !capital?.constituents.length && !detail?.constituents.length
          ? errors.filter(Boolean).join("；") || "当前行业暂未返回成分股快照。"
          : null,
      );
      setIndustryReplayLoading(false);
    })();

    return () => {
      cancelled = true;
    };
  }, [industryFlowRows, industryTarget, isConceptTarget, open, stock?.requested_trade_date, stock?.trade_date, tab, tradeDate]);

  useEffect(() => {
    if (!open || !symbol || !tradeDate) return;
    let cancelled = false;
    const reusableStock = initialStock && initialStock.symbol === symbol &&
      (initialStock.trade_date === tradeDate || initialStock.requested_trade_date === tradeDate)
      ? initialStock
      : null;
    setLoading(true);
    setPeriodLoading(false);
    periodLoadAttempts.current.clear();
    historicalBarsRequested.current.clear();
    setHistoricalDailyBars({});
    setMetaLoading(true);
    setSectorsLoading(true);
    setTdxBoardsLoading(true);
    setPlaying(false);
    setSelectedMarker(null);
    setIndustryName(null);
    const storedManualSector = readManualSectorContext(instrumentType, symbol);
    setManualSector(storedManualSector);
    setIndustryFlowRows([]);
    setIndustryReplayCapital(null);
    setIndustryReplayDetail(null);
    setIndustryReplayTimeline(null);
    setIndustryBoardSummary(null);
    setIndustryReplayError(null);
    setMeta(null);
    setSectors(null);
    setTdxBoards(null);
    setTradeTicks([]);
    setTradeTicksLoading(true);
    setFundFlow(null);
    setMargin([]);
    setValuation(null);
    setFinance([]);
    setFinanceLoading(true);
    setFinanceError(null);
    setBusinessComposition(null);
    setProfitForecast(null);
    setCompanyResearch(null);
    setFinanceDetailsError(null);
    financeDetailsLoadedKey.current = null;
    setStock(reusableStock);
    setTimeline(initialIndicator ?? null);
    const targetType: StockTargetType =
      instrumentType === "ETF" ? "etf" : "stock";
    void (async () => {
      const base = reusableStock
        ? Promise.resolve(reusableStock)
        : loadTradeReviewIntraday({
            targetType,
            symbol,
            tradeDate,
            periods: ["1m"],
          });
      // The chart only depends on the primary target. Metadata, portfolio
      // anchors, sector context, and board membership are supplementary and a
      // slow one must never leave the already-rendered replay beneath a modal
      // loading mask.
      void base
        .then((result) => {
          if (cancelled) return;
          setStock(result);
          setLoading(false);
        })
        .catch(() => {
          if (!cancelled) setLoading(false);
        });
      const metaRequest = fetchStockMeta({ targetType, symbol });
      const sectorsRequest = fetchStockSectors(symbol, tradeDate);
      const tdxBoardsRequest = fetchEasyTdxBelongBoard(symbol);
      // Fund-flow data is loaded only when its tab/dialog is opened. It is not
      // needed to render the replay chart or the industry member list.
      const tradeTicksRequest = fetchEasyTdxTransactions({ code: symbol, tradeDate, targetType });
      void metaRequest
        .then((result) => {
          if (cancelled) return;
          setMeta(result);
        })
        .catch(() => undefined)
        .finally(() => {
          if (!cancelled) setMetaLoading(false);
        });
      void sectorsRequest
        .then((result) => {
          if (cancelled) return;
          setSectors(result);
        })
        .catch(() => undefined)
        .finally(() => {
          if (!cancelled) setSectorsLoading(false);
        });
      void tdxBoardsRequest
        .then((result) => {
          if (cancelled) return;
          setTdxBoards(result);
        })
        .catch(() => {
          if (!cancelled) setTdxBoards(null);
        })
        .finally(() => {
          if (!cancelled) setTdxBoardsLoading(false);
        });
      const indicatorRequest = initialIndicator !== undefined
        ? Promise.resolve(initialIndicator)
        : fetchMarketIndicatorReplayTimeline({
            tradeDate,
            from: "09:30",
            to: "15:00",
            limit: 3000,
          });
      const [
        stockResult,
        metaResult,
        anchorResult,
        sectorsResult,
        tdxBoardsResult,
      ] =
        await Promise.allSettled([
          base,
          metaRequest,
          fetchPortfolioReviewAnchors(symbol, tradeDate),
          sectorsRequest,
          tdxBoardsRequest,
        ]);
      if (cancelled) return;
      const nextStock =
        stockResult.status === "fulfilled" ? stockResult.value : null;
      const nextMeta =
        metaResult.status === "fulfilled" ? metaResult.value : null;
      const nextSectors =
        sectorsResult.status === "fulfilled" ? sectorsResult.value : null;
      const nextTdxBoards =
        tdxBoardsResult.status === "fulfilled" ? tdxBoardsResult.value : null;
      const sectorFlowRows: IndustryFundFlowRow[] = [];
      const sectorContext = resolveSectorContext(
        nextMeta,
        nextSectors,
        nextTdxBoards,
        sectorFlowRows,
        `${securityName ?? ""} ${nextStock?.name ?? ""}`,
      );
      const effectiveSectorContext = storedManualSector ?? sectorContext;
      if (cancelled) return;
      setStock(nextStock);
      setAnchors(anchorResult.status === "fulfilled" ? anchorResult.value : []);
      // Indicator events are useful replay context, but their full timeline
      // can be a large response. Load it after the price chart is visible.
      setTimeline(initialIndicator ?? null);
      void indicatorRequest
        .then((result) => {
          if (!cancelled) setTimeline(result);
        })
        .catch(() => {
          if (!cancelled && initialIndicator === undefined) setTimeline(null);
        });
      // The target chart can render from the health-check snapshot before the
      // benchmark overlays finish loading. Keep the slow context requests out
      // of the first-paint critical path.
      setLoading(false);
      const indexRequest = loadTradeReviewIntraday({
        targetType: "index",
        symbol: INDEX_SYMBOL,
        name: INDEX_NAME,
        tradeDate,
        periods: ["1m"],
      });
      const conceptContext = effectiveSectorContext?.kind === "concept" || industryTarget?.kind === "concept";
      const industryRequest = effectiveSectorContext && !conceptContext
        ? loadTradeReviewIntraday({
            targetType: effectiveSectorContext.targetType,
            symbol: effectiveSectorContext.symbol,
            name: effectiveSectorContext.name,
            tradeDate,
            periods: ["1m"],
          })
        : Promise.resolve(null);
      void indexRequest.then((result) => {
        if (!cancelled) setIndex(result);
      });
      void industryRequest.then((result) => {
        if (cancelled) return;
        setIndustry(result);
        setIndustryName(
          conceptContext ? null : effectiveSectorContext?.name ?? null,
        );
      });
      // The replay chart is ready once the target, benchmark and sector data
      // are requested. Do not serialize the independent context fetches: an
      // unavailable industry source must not prevent the index overlay from
      // rendering, or vice versa.
      setLoading(false);
      const [tradeTicksResult] = await Promise.allSettled([tradeTicksRequest]);
      if (cancelled) return;
      setTradeTicks(
        tradeTicksResult.status === "fulfilled" ? tradeTicksResult.value.items : [],
      );
      setTradeTicksLoading(false);
      const financeReportsRequest = Promise.allSettled([
        fetchStockFinanceReportWithFallback(symbol, "zcfzb"),
        fetchStockFinanceReportWithFallback(symbol, "lrb"),
        fetchStockFinanceReportWithFallback(symbol, "xjllb"),
      ]);
      const [
        marginResult,
        valuationResult,
        financeResults,
      ] = await Promise.allSettled([
        fetchMarginStockHistory(symbol, 120),
        fetchStockValuation(symbol, { limit: 120 }),
        financeReportsRequest,
      ]);
      if (cancelled) return;
      setMargin(
        marginResult.status === "fulfilled" ? marginResult.value.items : [],
      );
      setValuation(
        valuationResult.status === "fulfilled" ? valuationResult.value : null,
      );
      setFinance(
        financeResults.status === "fulfilled"
          ? financeResults.value
              .filter((result): result is PromiseFulfilledResult<StockFinanceReportResponse> => result.status === "fulfilled")
              .map((result) => result.value)
          : [],
      );
      const failedReports =
        financeResults.status === "fulfilled"
          ? financeResults.value.filter((result) => result.status === "rejected").length
          : 3;
      setFinanceError(
        valuationResult.status === "rejected" && failedReports === 3
          ? "估值与三大财务报表均暂不可用"
          : valuationResult.status === "rejected"
            ? "估值数据暂不可用"
            : failedReports > 0
              ? `${failedReports} 张财务报表暂不可用`
              : null,
      );
      setFinanceLoading(false);
      setLoading(false);
    })().catch(() => {
      if (!cancelled) {
        setFinanceLoading(false);
        setFinanceError("财务数据请求失败");
        setLoading(false);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [industryTarget?.kind, initialIndicator, initialStock, instrumentType, open, securityName, symbol, tradeDate]);

  useEffect(() => {
    if (!open || tab !== "资金流" || fundFlow || isConceptTarget) return;
    let cancelled = false;
    void fetchStockFundFlow(symbol)
      .then((result) => {
        if (!cancelled) setFundFlow(result);
      })
      .catch(() => {
        if (!cancelled) setFundFlow(null);
      });
    return () => {
      cancelled = true;
    };
  }, [fundFlow, isConceptTarget, open, symbol, tab]);

  useEffect(() => {
    if (!open || tab !== "财务" || !symbol) return;
    const key = `${instrumentType}:${symbol}:${tradeDate}`;
    if (financeDetailsLoadedKey.current === key) return;
    let cancelled = false;
    setFinanceDetailsLoading(true);
    setFinanceDetailsError(null);
    void Promise.allSettled([
      fetchStockBusinessComposition(symbol),
      fetchStockProfitForecast(symbol),
      fetchStockCompanyNews(symbol, { section: "gsyj" }),
    ])
      .then(([businessResult, forecastResult, researchResult]) => {
        if (cancelled) return;
        const errors: string[] = [];
        if (businessResult.status === "fulfilled") {
          setBusinessComposition(businessResult.value);
        } else {
          errors.push("主营构成加载失败");
        }
        if (forecastResult.status === "fulfilled") {
          setProfitForecast(forecastResult.value);
        } else {
          errors.push("业绩预测加载失败");
        }
        if (researchResult.status === "fulfilled") {
          setCompanyResearch(researchResult.value);
        } else {
          errors.push("机构评级加载失败");
        }
        financeDetailsLoadedKey.current = key;
        setFinanceDetailsError(errors.length ? errors.join("；") : null);
      })
      .finally(() => {
        if (!cancelled) setFinanceDetailsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [instrumentType, open, symbol, tab, tradeDate]);

  useEffect(() => {
    if (
      !open ||
      !symbol ||
      !tradeDate ||
      period === "timeshare" ||
      isHistoricalReplayPeriod(period) ||
      !stock
    ) return;
    const key = `${instrumentType}:${symbol}:${tradeDate}:${period}`;
    if (periodLoadAttempts.current.has(key) || stock.minute_bars[period]?.length) return;
    periodLoadAttempts.current.add(key);
    let cancelled = false;
    setPeriodLoading(true);
    void loadTradeReviewIntraday({
      targetType: instrumentType === "ETF" ? "etf" : "stock",
      symbol,
      name: displayName,
      tradeDate,
      periods: [period],
    })
      .then((result) => {
        if (cancelled || !result) return;
        setStock((currentStock) =>
          currentStock
            ? {
                ...currentStock,
                minute_bars: {
                  ...currentStock.minute_bars,
                  [period]: result.minute_bars[period] ?? [],
                },
              }
            : result,
        );
      })
      .finally(() => {
        if (!cancelled) setPeriodLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [displayName, instrumentType, open, period, stock, symbol, tradeDate]);

  useEffect(() => {
    if (!open || !symbol || !tradeDate || !isHistoricalReplayPeriod(period)) return;

    const targets: Array<{
      targetType: StockTargetType;
      symbol: string;
      name?: string;
    }> = [
      {
        targetType: instrumentType === "ETF" ? "etf" : "stock",
        symbol,
        name: displayName,
      },
      { targetType: "index", symbol: INDEX_SYMBOL, name: INDEX_NAME },
    ];
    const historicalIndustrySymbol = industry?.symbol ?? (industryTarget?.kind === "industry" ? industryTarget.code : null);
    if (historicalIndustrySymbol) {
      targets.push({
        targetType: "sector",
        symbol: historicalIndustrySymbol,
        name: industryName ?? industry?.name ?? industryTarget?.name,
      });
    }

    const missing = targets.filter((target) => {
      const key = historicalBarsKey(target.targetType, target.symbol);
      return !historicalBarsRequested.current.has(key);
    });
    if (!missing.length) return;

    for (const target of missing) {
      historicalBarsRequested.current.add(
        historicalBarsKey(target.targetType, target.symbol),
      );
    }
    let cancelled = false;

    const loadTargets = (targetsToLoad: typeof missing, showLoading: boolean) => {
      if (!targetsToLoad.length) return;
      if (showLoading) setPeriodLoading(true);
      void Promise.allSettled(
        targetsToLoad.map(async (target) => {
          const items = target.targetType === "index" || target.targetType === "sector"
            ? industryApplicationDailyBars((await fetchIndustryApplicationKline(
                target.targetType === "index" ? "index" : "industry",
                target.symbol,
                { period: "day", count: 500 },
              )).kline ?? [])
            : (await fetchStockKlines({
                targetType: target.targetType,
                symbol: target.symbol,
                name: target.name,
                period: "1d",
                adjust: historicalAdjust(target.targetType),
              })).items ?? [];
          return [
            historicalBarsKey(target.targetType, target.symbol),
            items,
          ] as const;
        }),
      ).then((results) => {
        if (cancelled) return;
        setHistoricalDailyBars((currentBars) => {
          const nextBars = { ...currentBars };
          results.forEach((result, index) => {
            if (result.status === "fulfilled") {
              nextBars[result.value[0]] = result.value[1];
            } else {
              historicalBarsRequested.current.delete(
                historicalBarsKey(
                  targetsToLoad[index].targetType,
                  targetsToLoad[index].symbol,
                ),
              );
            }
          });
          return nextBars;
        });
      }).finally(() => {
        if (showLoading && !cancelled) setPeriodLoading(false);
      });
    };

    const stockKey = historicalBarsKey(targets[0].targetType, targets[0].symbol);
    loadTargets(
      missing.filter(
        (target) => historicalBarsKey(target.targetType, target.symbol) === stockKey,
      ),
      true,
    );
    // Industry and index history is useful context, but must not block the
    // primary stock chart from becoming visible.
    loadTargets(
      missing.filter(
        (target) => historicalBarsKey(target.targetType, target.symbol) !== stockKey,
      ),
      false,
    );
    return () => {
      cancelled = true;
    };
  }, [
    displayName,
    industryTarget,
    industry?.name,
    industry?.symbol,
    industry?.target_type,
    industryName,
    instrumentType,
    open,
    period,
    symbol,
    tradeDate,
  ]);

  const stockTargetType: StockTargetType = instrumentType === "ETF" ? "etf" : "stock";
  const stockHistoricalKey = historicalBarsKey(stockTargetType, symbol);
  const bars = useMemo(() => {
    if (!isHistoricalReplayPeriod(period)) return barsForPeriod(stock, period);
    const historicalBars = aggregateDailyBars(
      historicalDailyBars[stockHistoricalKey] ?? [],
      period,
      tradeDate,
    );
    return period === "1d"
      ? historicalBars.slice(-historicalWindowDays)
      : historicalBars;
  }, [historicalDailyBars, historicalWindowDays, period, stock, stockHistoricalKey, tradeDate]);
  useEffect(() => {
    setCursor(Math.max(0, bars.length - 1));
  }, [bars.length, period, tradeDate]);
  useEffect(() => {
    const pendingDate = pendingMinuteReplayDate.current;
    if (
      !pendingDate ||
      pendingDate !== tradeDate ||
      period !== "timeshare" ||
      loading ||
      bars.length < 2 ||
      !stock ||
      (stock.trade_date !== pendingDate && stock.requested_trade_date !== pendingDate)
    ) return;
    pendingMinuteReplayDate.current = null;
    setCursor(0);
    setPlaying(true);
  }, [bars.length, loading, period, stock, tradeDate]);
  useEffect(() => {
    if (!playing || bars.length < 2) return;
    const timer = window.setInterval(
      () =>
        setCursor((value) => {
          if (value >= bars.length - 1) {
            setPlaying(false);
            return value;
          }
          return value + 1;
        }),
      Math.max(40, 360 / speed),
    );
    return () => window.clearInterval(timer);
  }, [bars.length, playing, speed]);
  const current = bars[cursor];
  // A daily cursor is the authoritative as-of date for every daily panel.
  // Keep the panel-selected date for intraday replay only; otherwise a user
  // can move the D1/D2 K-line while margin, finance and market state stay on
  // the original date and silently leak future information.
  const dailyReplayActive = period === "1d";
  const activeReplayDate = dailyReplayActive && current?.timestamp
    ? localDate(new Date(current.timestamp).toISOString())
    : tradeDate;
  // Historical K-line periods do not require the intraday payload to be
  // present. The daily/weekly/monthly request can succeed independently while
  // the intraday endpoint is unavailable, so keep a minimal response shape
  // for the lower replay panel instead of hiding a valid historical chart.
  const replayStock = useMemo<StockIntradayResponse>(
    () =>
      stock ?? {
        ok: true,
        symbol,
        target_type: stockTargetType,
        name: displayName,
        adjust: "none",
        effective_adjust: "none",
        requested_adjust: "none",
        trade_date: tradeDate,
        requested_trade_date: tradeDate,
        timeshare: [],
        minute_bars: {},
        source: "historical-kline-only",
      },
    [displayName, stock, stockTargetType, symbol, tradeDate],
  );
  const markerItems = useMemo(
    () => markerList(trades, anchors, timeline?.events ?? []),
    [anchors, timeline?.events, trades],
  );
  const visibleMarkers = markerItems.filter((item) => {
    if (item.kind === "event") return false;
    const barIndex = barIndexForMarker(item.timestamp, bars, period);
    return barIndex >= 0 && barIndex <= cursor;
  });
  const visibleEvents = markerItems.filter(
    (item) =>
      item.kind === "event" &&
      item.timestamp <= (current?.timestamp ?? Number.MAX_SAFE_INTEGER),
  );
  const currentMarker = selectedMarker ?? null;
  const overlays = useMemo(() => {
    const result: Array<{
      name: string;
      bars: typeof bars;
      color: string;
      mode: "absolute" | "relative";
      directional?: boolean;
      lineType: "solid" | "dashed" | "dotted";
      lineWidth: number;
    }> =
      [];
    const historicalPeriod = isHistoricalReplayPeriod(period);
    const historicalIndustrySymbol = industry?.symbol ?? (industryTarget?.kind === "industry" ? industryTarget.code : null);
    if (industry || (historicalPeriod && historicalIndustrySymbol)) {
      const industryBars = historicalPeriod && historicalIndustrySymbol
        ? aggregateDailyBars(
            historicalDailyBars[
              historicalBarsKey("sector", historicalIndustrySymbol)
            ] ?? [],
            period,
            tradeDate,
          )
        : contextBarsForPeriod(industry, period);
      const visibleIndustryBars = period === "1d" ? industryBars.slice(-historicalWindowDays) : industryBars;
      result.push({
        name: `行业${(industryName ?? meta?.sectorIndexName) ? ` · ${industryName ?? meta?.sectorIndexName}` : ""}`,
        bars: visibleIndustryBars,
        color: "#2563eb",
        mode: "absolute",
        lineType: "solid",
        lineWidth: 2.4,
      });
    }
    if (index || historicalPeriod) {
      const indexBars = historicalPeriod
        ? aggregateDailyBars(
            historicalDailyBars[historicalBarsKey("index", INDEX_SYMBOL)] ?? [],
            period,
            tradeDate,
          )
        : contextBarsForPeriod(index, period);
      const visibleIndexBars = period === "1d" ? indexBars.slice(-historicalWindowDays) : indexBars;
      result.push({
        name: `${INDEX_NAME} · ${INDEX_SYMBOL}`,
        bars: visibleIndexBars,
        color: "#dc2626",
        mode: "relative",
        directional: true,
        lineType: "solid",
        lineWidth: 2.4,
      });
    }
    return result;
  }, [
    historicalDailyBars,
    historicalWindowDays,
    index,
    industry,
    industryTarget,
    industryName,
    meta?.sectorIndexName,
    period,
    tradeDate,
  ]);
  const asOfMargin = margin
    .filter((item) => item.tradeDate <= activeReplayDate)
    .sort((a, b) => b.tradeDate.localeCompare(a.tradeDate));
  const asOfFundFlow =
    fundFlow?.history
      .filter((item) => item.trade_date <= activeReplayDate)
      .sort((a, b) => b.trade_date.localeCompare(a.trade_date)) ?? [];
  const asOfValuation =
    valuation?.rows.filter((item) => item.date <= activeReplayDate) ?? [];
  const asOfFinance = finance.map((item) => ({
    ...item,
    rows: item.rows.filter((row) => (row.rq ?? "") <= activeReplayDate),
  }));
  const state = dataState(stock, timeline);
  const currentAverage = stock?.timeshare.find(
    (item) =>
      Math.abs(epoch(item.timestamp) - (current?.timestamp ?? 0)) < 60_000,
  )?.avg_price ?? null;

  useEffect(() => {
    if (currentMarker) {
      setNote(currentMarker.detail ?? "");
      setTags(currentMarker.anchor?.tags ?? "");
    } else {
      setNote("");
      setTags("");
    }
  }, [currentMarker]);
  function jumpTrade(direction: 1 | -1) {
    const tradeMarkers = markerItems.filter((item) => item.kind === "trade");
    const next = tradeMarkers.find((item) =>
      direction === 1
        ? item.timestamp > (current?.timestamp ?? 0)
        : item.timestamp < (current?.timestamp ?? Number.MAX_SAFE_INTEGER),
    );
    const target =
      direction === -1
        ? [...tradeMarkers]
            .reverse()
            .find(
              (item) =>
                item.timestamp <
                (current?.timestamp ?? Number.MAX_SAFE_INTEGER),
            )
        : next;
    if (target) setCursor(nearestIndex(bars, target.timestamp));
  }
  function addObservation() {
    if (!current) return;
    setSelectedMarker({
      id: `draft-${Date.now()}`,
      kind: "observation",
      timestamp: current.timestamp,
      price: current.close,
      label: "观",
      detail: null,
    });
    setTab("交易与复盘");
  }
  async function saveMarker() {
    if (!currentMarker || !currentMarker.price) return;
    setSaving(true);
    try {
      const saved = await savePortfolioReviewAnchor({
        id: currentMarker.anchor?.id,
        symbol,
        instrumentType,
        tradeId: currentMarker.trade?.id ?? null,
        reviewId: currentMarker.anchor?.reviewId ?? null,
        anchorType: currentMarker.kind === "trade" ? "TRADE" : "OBSERVATION",
        side:
          currentMarker.kind === "trade"
            ? (currentMarker.trade?.side ?? "BUY")
            : "OBSERVATION",
        occurredAt: new Date(currentMarker.timestamp).toISOString(),
        price: currentMarker.price,
        note,
        tags,
      });
      setAnchors((items) => [
        ...items.filter(
          (item) => item.id !== saved.id && item.tradeId !== saved.tradeId,
        ),
        saved,
      ]);
      setSelectedMarker((item) =>
        item ? { ...item, anchor: saved, detail: saved.note } : item,
      );
      notification.success({
        title: "复盘记录已保存",
        description: `${formatTime(currentMarker.timestamp)} 已关联到${currentMarker.kind === "trade" ? "交易" : "观察点"}。`,
      });
    } catch (error) {
      notification.danger({
        title: "保存复盘记录失败",
        description: error instanceof Error ? error.message : String(error),
      });
    } finally {
      setSaving(false);
    }
  }

  async function reloadIndustryOverlay(context: SectorContext | null) {
    setLoading(true);
    try {
      const result = context && context.kind !== "concept" && industryTarget?.kind !== "concept"
        ? await loadTradeReviewIntraday({
            targetType: context.targetType,
            symbol: context.symbol,
            name: context.name,
            tradeDate,
            periods: ["1m"],
          })
        : null;
      setIndustry(result);
      setIndustryName(context?.kind === "concept" || industryTarget?.kind === "concept" ? null : context?.name ?? null);
    } finally {
      setLoading(false);
    }
  }

  async function saveManualSector(context: SectorContext) {
    writeManualSectorContext(instrumentType, symbol, context);
    setManualSector(context);
    await reloadIndustryOverlay(context);
    notification.success({
      title: "行业关联已保存",
      description: `${context.name} · ${context.symbol}（仅覆盖当前复盘工作台的自动关联）`,
    });
  }

  async function clearManualSector() {
    writeManualSectorContext(instrumentType, symbol, null);
    setManualSector(null);
    const fallback = resolveSectorContext(
      meta,
      sectors,
      tdxBoards,
      industryFlowRows,
      `${securityName ?? ""} ${stock?.name ?? ""}`,
    );
    await reloadIndustryOverlay(fallback);
    notification.success({
      title: "已恢复自动行业关联",
      description: fallback ? `${fallback.name} · ${fallback.symbol}` : "当前没有可用的自动关联",
    });
  }

  async function selectIndustryTarget(target: IndustryApplicationTargetCode) {
    setIndustryPickerOpen(false);
    await saveManualSector({
      name: target.name,
      symbol: target.code,
      targetType: "index",
      kind: target.kind,
    });
  }

  function selectTarget(option: StockSearchItem) {
    onSymbolChange(option.symbol, option.target_type === "etf" ? "ETF" : "STOCK");
    setTargetPickerOpen(false);
    setTargetQuery("");
    setTargetSearchResults([]);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        showCloseButton={false}
        className="!h-[96vh] !w-[96vw] !max-w-[96vw] gap-0 overflow-hidden rounded-xl border-border p-0"
        onPointerDownOutside={(event) => event.preventDefault()}
        onFocusOutside={(event) => event.preventDefault()}
        onInteractOutside={(event) => event.preventDefault()}
      >
        <DialogTitle className="sr-only">
          {symbol} {tradeDate} 交易复盘
        </DialogTitle>
        <DialogDescription className="sr-only">
          盘中价格、均价、成交量、行业和大盘联动回放。
        </DialogDescription>
        <div className="flex h-full min-h-0 flex-col bg-background">
          <div className="flex min-h-[52px] shrink-0 flex-wrap items-center gap-3 border-b border-border/70 px-4 py-1.5">
            <div className="flex min-w-0 items-center gap-3">
              <div className="flex size-8 shrink-0 items-center justify-center rounded-md bg-foreground text-background">
                <Activity className="size-4" />
              </div>
              <div className="min-w-0">
                <Popover open={targetPickerOpen} onOpenChange={setTargetPickerOpen}>
                  <PopoverTrigger asChild>
                    <button
                      type="button"
                      aria-label="选择或搜索复盘标的"
                      className="flex h-5 min-w-0 max-w-[360px] items-center gap-2 rounded-md px-1 text-left text-sm font-semibold outline-none hover:bg-muted/50 focus-visible:ring-1 focus-visible:ring-ring/50"
                    >
                      <span className="flex min-w-0 items-center gap-2">
                        <span className="truncate font-normal text-muted-foreground">
                          {displayName}
                        </span>
                        <span className="shrink-0 font-mono text-xs">{symbol}</span>
                      </span>
                      <span className="shrink-0 text-[10px] text-muted-foreground">⌄</span>
                    </button>
                  </PopoverTrigger>
                  <PopoverContent
                    align="start"
                    className="w-[min(420px,calc(100vw-2rem))] p-0"
                  >
                    <Command shouldFilter={false}>
                      <CommandInput
                        value={targetQuery}
                        onValueChange={setTargetQuery}
                        placeholder="搜索代码或名称（可选无交易记录标的）"
                      />
                      <CommandList className="max-h-[min(60vh,420px)]">
                        {targetSearchLoading ? (
                          <div className="px-3 py-2 text-xs text-muted-foreground">
                            正在搜索标的…
                          </div>
                        ) : null}
                        {!targetSearchLoading &&
                        targetOptions.filter((item) => {
                          const query = targetQuery.trim().toLowerCase();
                          return (
                            !query ||
                            `${item.symbol} ${item.name}`.toLowerCase().includes(query)
                          );
                        }).length === 0 ? (
                          <CommandEmpty>没有匹配的标的</CommandEmpty>
                        ) : null}
                        {targetOptions
                          .filter((item) => {
                            const query = targetQuery.trim().toLowerCase();
                            return (
                              !query ||
                              `${item.symbol} ${item.name}`.toLowerCase().includes(query)
                            );
                          })
                          .map((item) => {
                            const isPortfolioTarget = symbols.some(
                              (entry) =>
                                entry.symbol === item.symbol &&
                                (entry.instrumentType === "ETF" ? "etf" : "stock") ===
                                  item.target_type,
                            );
                            return (
                              <CommandItem
                                key={`${item.target_type}-${item.symbol}`}
                                value={`${item.target_type}-${item.symbol}`}
                                onSelect={() => selectTarget(item)}
                              >
                                <span className="flex min-w-0 items-center gap-2">
                                  <span className="min-w-0 truncate text-xs">
                                    {item.name || "名称待补充"}
                                  </span>
                                  <span className="shrink-0 font-mono text-xs text-muted-foreground">
                                    {item.symbol}
                                  </span>
                                  <span className="ml-auto shrink-0 text-[10px] text-muted-foreground">
                                    {item.target_type === "etf" ? "ETF" : isPortfolioTarget ? "A股 · 已交易" : "A股 · 可复盘"}
                                  </span>
                                </span>
                              </CommandItem>
                            );
                          })}
                      </CommandList>
                    </Command>
                  </PopoverContent>
                </Popover>
                <div className="-mt-0.5 ml-1 flex min-w-0 items-center gap-2">
                  <Select value={tradeDate} onValueChange={onTradeDateChange}>
                    <SelectTrigger
                      size="sm"
                      className="h-6 w-auto min-w-[86px] shrink-0 border-0 bg-transparent px-0 text-[11px] font-semibold shadow-none hover:bg-transparent"
                      aria-label="选择复盘日期"
                    >
                      <SelectValue placeholder="复盘日期" />
                    </SelectTrigger>
                    <SelectContent align="start">
                      {reviewDateOptions.map((date) => (
                        <SelectItem key={date} value={date} className="font-mono text-xs">
                          {date}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <span className="truncate text-[11px] text-muted-foreground">
                    {state.label} · 游标 {formatTime(current?.timestamp)}
                  </span>
                </div>
              </div>
            </div>
          <div className="flex min-w-0 flex-1 flex-wrap items-center gap-2">
            <div className="flex min-w-0 flex-1 flex-wrap items-center gap-2">
              <Popover open={industryPickerOpen} onOpenChange={setIndustryPickerOpen}>
                <PopoverTrigger asChild>
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    className="h-11 min-h-11 min-w-[152px] max-w-[200px] flex-col items-stretch justify-center gap-0 rounded-md bg-secondary/75 px-2.5 py-1.5 text-left hover:bg-secondary"
                    disabled={industryPickerLoading && !industryTarget}
                    title={industryTarget ? `${industryTarget.name} · ${industryTarget.code}` : "选择行业"}
                  >
                    <span className="flex w-full items-center justify-between gap-2 leading-3">
                      <span className="truncate text-[9px] font-medium uppercase tracking-[0.08em] text-muted-foreground">
                        {activeIndustrySource?.replace(/\s*\/\s*/g, "/") ?? "来源待确认"}
                      </span>
                      <ChevronDown className="size-3 shrink-0 text-muted-foreground" aria-hidden="true" />
                    </span>
                    <span className="max-w-full truncate text-[11px] font-medium leading-4 text-foreground">
                      {industryTarget?.name ? `Industries: ${industryTarget.name}` : "选择行业"}
                    </span>
                  </Button>
                </PopoverTrigger>
                <PopoverContent align="start" className="w-[300px] p-0">
                  <Command>
                    <CommandInput placeholder="搜索行业名称 / 代码" />
                    <CommandList>
                      {industryPickerLoading ? (
                        <div className="flex items-center justify-center gap-2 px-3 py-5 text-xs text-muted-foreground">
                          <Loader2 className="size-3.5 animate-spin" />
                          正在加载行业目录
                        </div>
                      ) : (
                        <>
                          <CommandEmpty>没有匹配的行业板块</CommandEmpty>
                          {industryPickerOptions
                            .map((target) => {
                              const selected = industryTarget?.code === target.code;
                              return (
                                <CommandItem
                                  key={`${target.kind}-${target.code}`}
                                  value={`${target.name} ${target.code}`}
                                  onSelect={() => void selectIndustryTarget(target)}
                                >
                                  <Check className={`size-3.5 ${selected ? "opacity-100" : "opacity-0"}`} />
                                  <span className="min-w-0 flex-1 truncate">{target.name}</span>
                                  <span className={`shrink-0 rounded px-1 text-[9px] ${target.sourceLabel.includes("THS") ? "bg-information/10 text-information" : "bg-muted text-muted-foreground"}`}>
                                    {target.sourceLabel}
                                  </span>
                                  <span className="shrink-0 font-mono text-[10px] text-muted-foreground">{target.code}</span>
                                </CommandItem>
                              );
                            })}
                        </>
                      )}
                    </CommandList>
                  </Command>
               </PopoverContent>
              </Popover>
               <span className="min-w-4 flex-1" aria-hidden="true" />
               <div className="inline-flex shrink-0 items-center gap-0.5 rounded-md border-r border-border/60 bg-muted/60 p-0.5 pr-2">
                 {PERIODS.map((item) => (
                   <Button
                     key={item.value}
                     type="button"
                     variant="ghost"
                     size="sm"
                     aria-pressed={period === item.value}
                     className={cn(
                       "h-7 rounded-sm px-2.5 text-[11px] text-muted-foreground hover:bg-background/70 hover:text-foreground",
                       period === item.value &&
                         "bg-background text-foreground shadow-xs hover:bg-background",
                     )}
                     disabled={isConceptTarget && isHistoricalReplayPeriod(item.value)}
                     title={isConceptTarget && isHistoricalReplayPeriod(item.value) ? "TDX 概念板块不提供板块日 K" : undefined}
                     onClick={() => setPeriod(item.value)}
                   >
                     {item.label}
                   </Button>
                 ))}
               </div>
               <Button
                type="button"
                variant="secondary"
                size="sm"
                className="h-7 px-2.5 text-[11px]"
                onClick={() => setCapitalDialogOpen(true)}
                disabled={!industryTarget || isConceptTarget}
                title={isConceptTarget ? "TDX 概念板块不提供 Capital" : "查看 Capital"}
              >
                <Gauge />
                Capital
              </Button>
              <Button
                type="button"
                variant="secondary"
                size="sm"
                className="h-7 px-2.5 text-[11px]"
                onClick={() => setFundFlowDialogOpen(true)}
                disabled={isConceptTarget}
                title={isConceptTarget ? "TDX 概念板块不提供 Fund Flow" : "查看 Fund Flow"}
              >
                <Activity />
                Fund Flow
              </Button>
              <Button
                type="button"
                variant="secondary"
                size="sm"
                className="h-7 px-2.5 text-[11px]"
                onClick={() => setRelativeMode((value) => !value)}
              >
                <SlidersHorizontal />
                {relativeMode ? "相对涨跌" : "绝对价格"}
              </Button>
              <Button
                type="button"
                variant="secondary"
                size="sm"
                className="h-7 px-2.5 text-[11px]"
                onClick={addObservation}
              >
                <Plus />
                观察点
              </Button>
             </div>
          </div>
          </div>
          <ResizablePanelGroup
            id="trade-review-content"
            orientation="horizontal"
            defaultLayout={contentLayout.defaultLayout ?? { chart: 76, details: 24 }}
            onLayoutChanged={contentLayout.onLayoutChanged}
            className="min-h-0 flex-1"
          >
            <ResizablePanel
              id="chart"
              defaultSize="66%"
              minSize="45%"
              className="min-h-0 min-w-0 overflow-hidden"
            >
              <main className="h-full min-w-0 overflow-hidden border-r border-border/70 p-3">
              <div className="relative h-full min-h-0 min-w-0 overflow-hidden rounded-lg bg-card p-2">
                {loading && !stock ? (
                  <div className="flex h-full items-center justify-center gap-2 text-xs text-muted-foreground">
                    <DogLoader size={32} label="正在加载价格、市场和日级背景…" />
                  </div>
                ) : (stock || isHistoricalReplayPeriod(period)) && bars.length ? (
                  <TradeReplayChart
                    bars={bars}
                    timeshare={replayStock.timeshare}
                    preClose={replayStock.pre_close}
                    overlays={overlays}
                    cursor={cursor}
                    period={period}
                    markers={visibleMarkers}
                    relativeMode={relativeMode}
                    lowerLabel={`${tab} · ${formatTime(current?.timestamp)}`}
                    lowerContent={
                      <ReplayLowerPanel
                        tab={tab}
                        tradeDate={activeReplayDate}
                        period={period}
                        current={current}
                        currentAverage={currentAverage}
                         stock={replayStock}
                        index={index}
                        industry={industry}
                        industryName={industryName}
                        industryTarget={industryTarget}
                        industryCapital={industryReplayCapital}
                        industryBoardSummary={industryBoardSummary}
                        industryBoardQuote={industryBoardQuote}
                        industryDetail={industryReplayDetail}
                        industryLoading={industryReplayLoading}
                        industryError={industryReplayError}
                        markers={visibleMarkers}
                        events={visibleEvents}
                        ticks={tradeTicks}
                        ticksLoading={tradeTicksLoading}
                        onTickSelect={(timestamp) => setCursor(nearestIndex(bars, timestamp))}
                        margin={asOfMargin}
                        finance={asOfFinance}
                        fundFlow={asOfFundFlow}
                        timeline={industryReplayTimeline ?? timeline}
                      />
                    }
                    onMarkerSelect={(marker) => {
                      setSelectedMarker(marker);
                      setTab("交易与复盘");
                    }}
                  />
                ) : (
                  <div className="flex h-full items-center justify-center text-xs text-muted-foreground">
                    当前周期没有可用数据
                  </div>
                )}
                {loading || periodLoading ? (
                  <div className="absolute inset-0 z-30 flex items-center justify-center bg-background/75 backdrop-blur-[1px]">
                    <div className="px-5 py-4">
                      <DogLoader
                        size={36}
                        label={
                          periodLoading
                            ? `正在加载 ${periodLabel(period)} 数据…`
                            : `正在加载 ${displayName} 的分时、1m、行业和大盘数据…`
                        }
                      />
                    </div>
                  </div>
                ) : null}
              </div>
              </main>
            </ResizablePanel>
            <ResizableHandle
              withHandle
              aria-label="调整图表与详情区域宽度"
              className="z-30 w-px min-w-px cursor-col-resize border-0 bg-border transition-colors hover:bg-primary [&>div]:h-3 [&>div]:w-2 [&>div]:border-border [&>div]:bg-muted"
            />
            <ResizablePanel
              id="details"
              defaultSize="34%"
              minSize="28%"
              maxSize="55%"
            >
              <aside className="flex h-full min-w-0 flex-col overflow-hidden bg-muted/15">
              <Tabs
                value={tab}
                onValueChange={setTab}
                className="min-h-0 min-w-0 flex-1 gap-0 overflow-hidden"
              >
                <TabsList
                  variant="line"
                  className="sticky top-0 z-10 grid h-auto w-full shrink-0 grid-cols-7 items-stretch justify-stretch gap-0.5 self-start overflow-hidden rounded-none bg-background px-2 py-1.5"
                >
                  <TabsTrigger
                    value="交易与复盘"
                    title="交易与复盘（Alt+1）"
                    className="min-w-0 justify-center px-1 py-1.5 text-[11px] after:hidden data-[state=active]:rounded-md data-[state=active]:bg-muted data-[state=active]:font-semibold data-[state=active]:text-foreground"
                  >
                    <ClipboardCheck className="size-3 shrink-0" aria-hidden="true" />
                    <span className="truncate">交易复盘</span>
                  </TabsTrigger>
                  <TabsTrigger
                    value="逐笔成交"
                    title="逐笔成交（Alt+2）"
                    className="min-w-0 justify-center px-1 py-1.5 text-[11px] after:hidden data-[state=active]:rounded-md data-[state=active]:bg-muted data-[state=active]:font-semibold data-[state=active]:text-foreground"
                  >
                    <ListOrdered className="size-3 shrink-0" aria-hidden="true" />
                    <span className="truncate">逐笔</span>
                  </TabsTrigger>
                  <TabsTrigger
                    value="两融"
                    title="两融（Alt+3）"
                    className="min-w-0 justify-center px-1 py-1.5 text-[11px] after:hidden data-[state=active]:rounded-md data-[state=active]:bg-muted data-[state=active]:font-semibold data-[state=active]:text-foreground"
                  >
                    <Landmark className="size-3 shrink-0" aria-hidden="true" />
                    两融
                  </TabsTrigger>
                  <TabsTrigger
                    value="财务"
                    title="财务（Alt+4）"
                    className="min-w-0 justify-center px-1 py-1.5 text-[11px] after:hidden data-[state=active]:rounded-md data-[state=active]:bg-muted data-[state=active]:font-semibold data-[state=active]:text-foreground"
                  >
                    <FileChartColumn className="size-3 shrink-0" aria-hidden="true" />
                    财务
                  </TabsTrigger>
                  <TabsTrigger
                    value="资金流"
                    disabled={isConceptTarget}
                    title={isConceptTarget ? "TDX 概念板块绑定后禁用资金流" : "资金流（Alt+5）"}
                    className="min-w-0 justify-center px-1 py-1.5 text-[11px] after:hidden data-[state=active]:rounded-md data-[state=active]:bg-muted data-[state=active]:font-semibold data-[state=active]:text-foreground"
                  >
                    <ArrowDownUp className="size-3 shrink-0" aria-hidden="true" />
                    <span className="truncate">资金</span>
                  </TabsTrigger>
                  <TabsTrigger
                    value="行业"
                    title="行业（Alt+6）"
                    className="min-w-0 justify-center px-1 py-1.5 text-[11px] after:hidden data-[state=active]:rounded-md data-[state=active]:bg-muted data-[state=active]:font-semibold data-[state=active]:text-foreground"
                  >
                    <Layers3 className="size-3 shrink-0" aria-hidden="true" />
                    行业
                  </TabsTrigger>
                  <TabsTrigger
                    value="市场状态"
                    title="市场状态（Alt+7）"
                    className="min-w-0 justify-center px-1 py-1.5 text-[11px] after:hidden data-[state=active]:rounded-md data-[state=active]:bg-muted data-[state=active]:font-semibold data-[state=active]:text-foreground"
                  >
                    <Activity className="size-3 shrink-0" aria-hidden="true" />
                    <span className="truncate">市场</span>
                  </TabsTrigger>
                </TabsList>
                <div className={cn(
                  "min-h-0 min-w-0 flex-1 overflow-auto overscroll-contain p-3",
                  tab === "财务" && "p-2",
                )}>
                  <TabsContent value="交易与复盘" className="m-0 space-y-3">
                    <MarkerInspector
                      marker={currentMarker}
                      note={note}
                      tags={tags}
                      setNote={setNote}
                      setTags={setTags}
                      onSave={() => void saveMarker()}
                      saving={saving}
                      onClear={() => setSelectedMarker(null)}
                    />
                    <ReplayEventFeed events={visibleEvents} />
                    <TradeList
                      trades={trades}
                      current={current?.timestamp ?? 0}
                      onSelect={(trade) =>
                        setCursor(nearestIndex(bars, epoch(trade.tradedAt)))
                      }
                    />
                  </TabsContent>
                  <TabsContent value="逐笔成交" className="m-0">
                    <TradeTickTab
                      rows={tradeTicks}
                      current={current?.timestamp ?? 0}
                      loading={loading}
                      onSelect={(timestamp) => setCursor(nearestIndex(bars, timestamp))}
                    />
                  </TabsContent>
                  <TabsContent value="两融" className="m-0">
                    <MarginTab rows={asOfMargin} tradeDate={activeReplayDate} />
                  </TabsContent>
                  <TabsContent value="财务" className="m-0 min-w-0 max-w-full">
                    <FinanceTab
                      finance={asOfFinance}
                      valuation={asOfValuation}
                      business={businessComposition}
                      forecast={profitForecast}
                      research={companyResearch}
                      loading={financeLoading}
                      error={financeError}
                      detailsLoading={financeDetailsLoading}
                      detailsError={financeDetailsError}
                      tradeDate={activeReplayDate}
                    />
                  </TabsContent>
                  <TabsContent value="资金流" className="m-0">
                    <FundFlowTab rows={asOfFundFlow} />
                  </TabsContent>
                  <TabsContent value="行业" className="m-0">
                    <IndustryTab
                      meta={meta}
                      metaLoading={metaLoading}
                      sectors={sectors}
                      sectorsLoading={sectorsLoading}
                      tdxBoards={tdxBoards}
                      tdxBoardsLoading={tdxBoardsLoading}
                      activeSectorName={manualSector?.name ?? industryName ?? meta?.sectorIndexName ?? meta?.industry ?? null}
                      activeSectorSymbol={manualSector?.symbol ?? industry?.symbol ?? meta?.sectorIndexSymbol ?? null}
                      manualSector={manualSector}
                      targetCodes={industryTargets}
                      targetCodesLoading={industryTargetsLoading}
                      onSaveManual={saveManualSector}
                      onClearManual={() => void clearManualSector()}
                    />
                  </TabsContent>
                  <TabsContent value="市场状态" className="m-0">
                    <MarketStateTabContent
                      key={dailyReplayActive ? "daily" : isHistoricalReplayPeriod(period) ? "aggregate" : "intraday"}
                      tradeDate={activeReplayDate}
                      currentTimestamp={current?.timestamp ?? 0}
                      industryCode={industryTarget?.code.replace(/^(sh|sz|bj)/i, "")}
                      mode={dailyReplayActive ? "daily" : isHistoricalReplayPeriod(period) ? "aggregate" : "intraday"}
                      dailyWindowDays={historicalWindowDays}
                      onDailyWindowDaysChange={setHistoricalWindowDays}
                      onStartMinuteReplay={(replayDate) => {
                        pendingMinuteReplayDate.current = replayDate;
                        setPlaying(false);
                        setCursor(0);
                        setPeriod("timeshare");
                        onTradeDateChange(replayDate);
                      }}
                      onSelectStock={(nextStock) => {
                        const nextSymbol = nextStock.code.trim().toUpperCase();
                        if (!nextSymbol) return;
                        onSymbolChange(nextSymbol, "STOCK");
                        setTab("交易与复盘");
                      }}
                      realEventCount={visibleEvents.length}
                      timeline={timeline}
                    />
                  </TabsContent>
                </div>
              </Tabs>
              </aside>
            </ResizablePanel>
          </ResizablePanelGroup>
          <div className="flex h-[84px] shrink-0 flex-col justify-center gap-2 border-t border-border/70 bg-background px-4">
            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="outline"
                size="icon-sm"
                onClick={() => jumpTrade(-1)}
                title="上一笔交易"
              >
                <SkipBack />
              </Button>
              <Button
                type="button"
                variant="outline"
                size="icon-sm"
                onClick={() => {
                  if (playing) {
                    setPlaying(false);
                    return;
                  }
                  if (cursor >= bars.length - 1) setCursor(0);
                  setPlaying(true);
                }}
                disabled={bars.length < 2}
                title={playing ? "暂停回放" : "播放回放"}
              >
                {playing ? <Pause /> : <Play />}
              </Button>
              <Button
                type="button"
                variant="outline"
                size="icon-sm"
                onClick={() => jumpTrade(1)}
                title="下一笔交易"
              >
                <SkipForward />
              </Button>
              <span className="w-16 font-mono text-xs">
                {formatTime(current?.timestamp)}
              </span>
              <Slider
                value={[cursor]}
                max={Math.max(0, bars.length - 1)}
                step={1}
                onValueChange={(value) => {
                  setPlaying(false);
                  setCursor(value[0] ?? 0);
                }}
                className="min-w-0 flex-1"
                aria-label="回放时间轴"
              />
              <select
                value={speed}
                onChange={(event) => setSpeed(Number(event.target.value))}
                className="h-8 rounded-md border border-border bg-muted/40 px-2 text-xs"
                aria-label="回放速度"
              >
                <option value={0.5}>0.5x</option>
                <option value={1}>1x</option>
                <option value={2}>2x</option>
                <option value={4}>4x</option>
              </select>
              <span className="w-12 text-right font-mono text-[11px] text-muted-foreground">
                {cursor + 1}/{bars.length}
              </span>
            </div>
            <div className="flex items-center justify-between text-[10px] text-muted-foreground">
              <span>{formatTime(bars[0]?.timestamp)}</span>
              <span>
                已显示 {visibleMarkers.length} 个 BS / 观察点 · 已触发{" "}
                {visibleEvents.length} 个 Indicator 事件
              </span>
              <span>{formatTime(bars.at(-1)?.timestamp)}</span>
            </div>
          </div>
          <IndustryCapitalDialog
            open={capitalDialogOpen}
            onOpenChange={setCapitalDialogOpen}
            target={industryTarget}
            tradeDate={tradeDate}
          />
          <IndustryFundFlowDialog
            open={fundFlowDialogOpen}
            onOpenChange={setFundFlowDialogOpen}
            tradeDate={tradeDate}
          />
        </div>
      </DialogContent>
    </Dialog>
  );
}

function MarkerInspector({
  marker,
  note,
  tags,
  setNote,
  setTags,
  onSave,
  saving,
  onClear,
}: {
  marker: ReplayMarker | null;
  note: string;
  tags: string;
  setNote: (value: string) => void;
  setTags: (value: string) => void;
  onSave: () => void;
  saving: boolean;
  onClear: () => void;
}) {
  if (!marker)
    return (
      <div className="rounded-md bg-background p-4 text-xs text-muted-foreground">
        <div className="flex items-center gap-2 text-foreground">
          <Gauge className="size-4" />
          当前时刻盘面
        </div>
        <p className="mt-2 leading-5">
          点击图中的 B、S 或观察点，右侧会切换到对应证据。Indicator
          事件会在游标到达后出现在下方事件区。也可以直接点击“观察点”记录当前价格状态。
        </p>
      </div>
    );
  return (
    <div className="rounded-md bg-background p-3">
      <div className="flex items-start justify-between gap-2">
        <div>
          <div className="flex items-center gap-2">
            <Badge variant={marker.kind === "trade" ? "default" : "secondary"}>
              {marker.label}
            </Badge>
            <span className="text-xs font-medium">
              {marker.kind === "trade"
                ? `${marker.trade?.side === "BUY" ? "买入" : "卖出"}交易`
                : marker.kind === "event"
                  ? "市场指标事件"
                  : "观察点"}
            </span>
          </div>
          <p className="mt-1 font-mono text-[11px] text-muted-foreground">
            {formatTime(marker.timestamp)} · {displayNumber(marker.price)}
          </p>
        </div>
        <Button
          variant="ghost"
          size="icon-sm"
          onClick={onClear}
          title="取消选择"
        >
          <X />
        </Button>
      </div>
      <label className="mt-3 block space-y-1 text-xs">
        <span className="text-muted-foreground">复盘记录</span>
        <Textarea
          value={note}
          onChange={(event) => setNote(event.target.value)}
          placeholder="记录当时看到的盘面、触发条件和执行理由"
          className="min-h-20 rounded-md bg-muted/30 text-xs"
        />
      </label>
      <label className="mt-2 block space-y-1 text-xs">
        <span className="text-muted-foreground">标签</span>
        <Input
          value={tags}
          onChange={(event) => setTags(event.target.value)}
          placeholder="逗号分隔，如 放量,突破"
          className="h-8 text-xs"
        />
      </label>
      <Button
        className="mt-3 w-full"
        size="sm"
        onClick={onSave}
        disabled={saving || marker.kind === "event"}
      >
        {saving ? <Loader2 className="animate-spin" /> : <Check />}
        {saving
          ? "保存中"
          : marker.kind === "event"
            ? "Indicator 事件只读"
            : "保存复盘记录"}
      </Button>
    </div>
  );
}

function ReplayEventFeed({ events }: { events: ReplayMarker[] }) {
  return (
    <section className="overflow-hidden rounded-md bg-background">
      <div className="flex items-center justify-between px-1 py-2">
        <span className="text-xs font-medium">盘中 Indicator</span>
        <span className="font-mono text-[11px] text-muted-foreground">
          {events.length} 个已触发
        </span>
      </div>
      {events.length ? (
        <div className="max-h-44 space-y-1 overflow-y-auto">
          {[...events]
            .slice(-8)
            .reverse()
            .map((event) => (
              <div key={event.id} className="px-3 py-2 text-xs">
                <div className="flex items-center justify-between gap-2">
                  <span className="min-w-0 truncate font-medium">
                    {event.event?.name ?? event.event?.code ?? "市场事件"}
                  </span>
                  <span className="shrink-0 font-mono text-[11px] text-muted-foreground">
                    {formatTime(event.timestamp)}
                  </span>
                </div>
                <p className="mt-0.5 truncate text-[11px] text-muted-foreground">
                  {event.event?.eventType ?? event.detail ?? "已捕捉"}
                </p>
              </div>
            ))}
        </div>
      ) : (
        <p className="px-3 py-4 text-xs text-muted-foreground">
          当前游标之前尚未触发 Market Indicator 事件。
        </p>
      )}
    </section>
  );
}

function TradeTickTab({
  rows,
  current,
  loading,
  onSelect,
}: {
  rows: HistoricalTradeTick[];
  current: number;
  loading: boolean;
  onSelect: (timestamp: number) => void;
}) {
  const [viewMode, setViewMode] = useState<"visual" | "minute" | "detail">("visual");
  const [sideFilter, setSideFilter] = useState<"ALL" | "BUY" | "SELL" | "NEUTRAL">("ALL");
  const visible = rows
    .filter((row) => tickTimestamp(row) <= current)
    .filter((row) => sideFilter === "ALL" || normalizeTickSide(row) === sideFilter)
    .sort((a, b) => tickTimestamp(a) - tickTimestamp(b));
  const latestMinute = visible.at(-1)?.time_label ?? visible.at(-1)?.trade_time;
  const minuteRows = latestMinute
    ? visible.filter((row) => (row.time_label ?? row.trade_time ?? row.time) === latestMinute)
    : [];
  const minuteVolume = minuteRows.reduce((sum, row) => sum + (Number(row.volume) || 0), 0);
  const minuteGroups = Array.from(
    visible.reduce((groups, row) => {
      const label = row.time_label ?? row.trade_time ?? row.time ?? "未知";
      const group = groups.get(label) ?? { label, rows: [], volume: 0, amount: 0, buy: 0, sell: 0, neutral: 0 };
      group.rows.push(row);
      group.volume += Number(row.volume) || 0;
      group.amount += Number(row.amount) || 0;
      const side = normalizeTickSide(row);
      if (side === "BUY") group.buy += 1;
      else if (side === "SELL") group.sell += 1;
      else group.neutral += 1;
      groups.set(label, group);
      return groups;
    }, new Map<string, { label: string; rows: HistoricalTradeTick[]; volume: number; amount: number; buy: number; sell: number; neutral: number }>()),
  );
  return (
    <section className="overflow-hidden rounded-md bg-background">
      <div className="flex items-center justify-between px-3 py-2">
        <div>
          <span className="text-xs font-medium">历史逐笔成交</span>
          <span className="ml-2 text-[11px] text-muted-foreground">TDX / 分钟精度</span>
        </div>
        <span className="font-mono text-[11px] text-muted-foreground">{visible.length}/{rows.length}</span>
      </div>
      <div className="flex flex-wrap items-center gap-1 bg-muted/10 px-2 py-2">
        <span className="mr-1 text-[10px] font-medium text-muted-foreground">视图</span>
        {(["visual", "minute", "detail"] as const).map((mode) => (
          <button key={mode} type="button" onClick={() => setViewMode(mode)} className={`rounded-md px-2 py-1 text-[10px] transition-colors ${viewMode === mode ? "bg-foreground text-background" : "bg-muted/70 text-muted-foreground hover:bg-muted hover:text-foreground"}`}>
            {mode === "visual" ? "密集成交区" : mode === "minute" ? "按分钟汇总" : "原始明细"}
          </button>
        ))}
        <span className="ml-2 mr-1 text-[10px] font-medium text-muted-foreground">方向</span>
        {(["ALL", "BUY", "SELL", "NEUTRAL"] as const).map((side) => (
          <button key={side} type="button" onClick={() => setSideFilter(side)} className={`rounded-md px-2 py-1 text-[10px] ${sideFilter === side ? "bg-muted-foreground/20 font-medium text-foreground" : "text-muted-foreground hover:bg-muted"}`}>
            {side === "ALL" ? "全部" : side === "BUY" ? "买" : side === "SELL" ? "卖" : "中性"}
          </button>
        ))}
      </div>
      {minuteRows.length ? (
        <div className="grid grid-cols-2 gap-2 bg-muted/20 px-2 py-2 text-[11px]">
          <span>当前分钟 {latestMinute} · {minuteRows.length} 笔</span>
          <span className="text-right font-mono">成交量 {displayVolume(minuteVolume)}</span>
        </div>
      ) : null}
      {visible.length ? (
        <div className="overflow-auto">
          {viewMode === "visual" ? (
            <TradeTickVisual rows={visible} onSelect={onSelect} />
          ) : viewMode === "minute" ? (
            <table className="w-full min-w-[420px] text-xs">
              <thead className="sticky top-0 bg-background text-[11px] text-muted-foreground">
                <tr className="text-left">
                  <th className="px-3 py-2 font-medium">分钟</th>
                  <th className="px-2 py-2 text-right font-medium">笔数</th>
                  <th className="px-2 py-2 text-right font-medium">成交量</th>
                  <th className="px-2 py-2 text-right font-medium">金额</th>
                  <th className="px-3 py-2 text-right font-medium">方向</th>
                </tr>
              </thead>
              <tbody>
                {[...minuteGroups].reverse().map(([label, group]) => (
                  <tr key={label} className="cursor-pointer hover:bg-muted/40" onClick={() => onSelect(tickTimestamp(group.rows[0]))} title="跳转到该成交分钟">
                    <td className="px-3 py-1.5 font-mono text-muted-foreground">{label}</td>
                    <td className="px-2 py-1.5 text-right font-mono">{group.rows.length}</td>
                    <td className="px-2 py-1.5 text-right font-mono">{displayVolume(group.volume)}</td>
                    <td className="px-2 py-1.5 text-right font-mono">{displayVolume(group.amount)}</td>
                    <td className="px-3 py-1.5 text-right text-[11px]"><span className="text-red-600">买 {group.buy}</span><span className="mx-1 text-muted-foreground">/</span><span className="text-emerald-600">卖 {group.sell}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
          <table className="w-full min-w-[460px] text-xs">
            <thead className="sticky top-0 bg-background text-[11px] text-muted-foreground">
              <tr className="text-left">
                <th className="px-3 py-2 font-medium">时间</th>
                <th className="px-2 py-2 text-right font-medium">价格</th>
                <th className="px-2 py-2 text-right font-medium">数量</th>
                <th className="px-2 py-2 text-right font-medium">金额</th>
                <th className="px-2 py-2 text-right font-medium">笔数</th>
                <th className="px-3 py-2 text-right font-medium">方向</th>
              </tr>
            </thead>
            <tbody>
              {[...visible].reverse().map((row, index) => {
                const timestamp = tickTimestamp(row);
                const side = normalizeTickSide(row);
                return (
                  <tr
                    key={`${row.index ?? index}-${timestamp}`}
                    className="cursor-pointer hover:bg-muted/40"
                    onClick={() => onSelect(timestamp)}
                    title="跳转到该成交分钟"
                  >
                    <td className="px-3 py-1.5 font-mono text-muted-foreground">{row.time_label ?? row.trade_time ?? row.time ?? "-"}</td>
                    <td className="px-2 py-1.5 text-right font-mono">{displayNumber(Number(row.price))}</td>
                    <td className="px-2 py-1.5 text-right font-mono">{displayVolume(Number(row.volume))}</td>
                    <td className="px-2 py-1.5 text-right font-mono">{displayVolume(Number(row.amount))}</td>
                    <td className="px-2 py-1.5 text-right font-mono">{row.order_count ?? "-"}</td>
                    <td className={`px-3 py-1.5 text-right font-medium ${side === "BUY" ? "text-red-600" : side === "SELL" ? "text-emerald-600" : "text-muted-foreground"}`}>
                      {side === "BUY" ? "买" : side === "SELL" ? "卖" : "—"}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          )}
        </div>
      ) : (
        <p className="px-3 py-8 text-center text-xs text-muted-foreground">
          {loading ? "正在加载历史逐笔成交明细…" : rows.length ? "当前回放时刻尚未出现逐笔成交" : "该交易日暂无历史逐笔成交明细"}
        </p>
      )}
    </section>
  );
}

function TradeTickVisual({
  rows,
  onSelect,
}: {
  rows: HistoricalTradeTick[];
  onSelect: (timestamp: number) => void;
}) {
  const stats = rows.reduce<{
    count: number;
    volume: number;
    amount: number;
    priceVolume: number;
    buyVolume: number;
    sellVolume: number;
    neutralVolume: number;
  }>(
    (result, row) => {
      const volume = Number(row.volume) || 0;
      const amount = Number(row.amount) || 0;
      const price = Number(row.price) || 0;
      result.volume += volume;
      result.amount += amount;
      result.priceVolume += price * volume;
      result.count += 1;
      const side = normalizeTickSide(row);
      if (side === "BUY") result.buyVolume += volume;
      else if (side === "SELL") result.sellVolume += volume;
      else result.neutralVolume += volume;
      return result;
    },
    { count: 0, volume: 0, amount: 0, priceVolume: 0, buyVolume: 0, sellVolume: 0, neutralVolume: 0 },
  );
  const priceLevels = Array.from(
    rows.reduce((levels, row) => {
      const price = Number(row.price);
      if (!Number.isFinite(price)) return levels;
      const precision = price < 10 ? 3 : 2;
      const key = price.toFixed(precision);
      const level = levels.get(key) ?? { price: Number(key), volume: 0, amount: 0, buy: 0, sell: 0, neutral: 0, count: 0 };
      const volume = Number(row.volume) || 0;
      const amount = Number(row.amount) || 0;
      level.volume += volume;
      level.amount += amount;
      level.count += 1;
      const side = normalizeTickSide(row);
      if (side === "BUY") level.buy += volume;
      else if (side === "SELL") level.sell += volume;
      else level.neutral += volume;
      levels.set(key, level);
      return levels;
    }, new Map<string, { price: number; volume: number; amount: number; buy: number; sell: number; neutral: number; count: number }>()).values(),
  ).sort((a, b) => b.price - a.price);
  const maxLevelVolume = Math.max(...priceLevels.map((level) => level.volume), 1);
  // Keep the visual summary compact. The raw-detail view remains available
  // for the complete tick list, while the visual mode focuses on the most
  // useful price bands around the current tape.
  const visiblePriceLevels = priceLevels.slice(0, 18);
  const minuteGroups = Array.from(
    rows.reduce((groups, row) => {
      const label = row.time_label ?? row.trade_time ?? row.time ?? "未知";
      const group = groups.get(label) ?? { label, rows: [], volume: 0, buy: 0, sell: 0, neutral: 0 };
      const volume = Number(row.volume) || 0;
      group.rows.push(row);
      group.volume += volume;
      const side = normalizeTickSide(row);
      if (side === "BUY") group.buy += volume;
      else if (side === "SELL") group.sell += volume;
      else group.neutral += volume;
      groups.set(label, group);
      return groups;
    }, new Map<string, { label: string; rows: HistoricalTradeTick[]; volume: number; buy: number; sell: number; neutral: number }>()).values(),
  );
  const maxMinuteVolume = Math.max(...minuteGroups.map((group) => group.volume), 1);
  const vwap = stats.volume > 0 ? stats.priceVolume / stats.volume : null;
  const buyRatio = stats.volume > 0 ? (stats.buyVolume / stats.volume) * 100 : 0;
  return (
    <div className="space-y-4 p-3">
      <div className="grid grid-cols-2 gap-2">
        <Summary label="累计成交量" value={displayVolume(stats.volume)} />
        <Summary label="成交金额" value={displayVolume(stats.amount)} />
        <Summary label="成交均价" value={displayNumber(vwap)} />
        <Summary label="主动买占比" value={`${buyRatio.toFixed(1)}%`} />
      </div>
      <section className="rounded-md bg-muted/10 p-3">
        <div className="flex items-center justify-between gap-2">
          <div>
            <div className="text-xs font-medium">价格成交密集区</div>
            <div className="mt-0.5 text-[11px] text-muted-foreground">按成交金额聚合，点击价格带可回到对应分钟</div>
          </div>
          <span className="font-mono text-[11px] text-muted-foreground">
            {visiblePriceLevels.length}/{priceLevels.length} 个价位
          </span>
        </div>
        <div className="mt-3 space-y-1">
          {visiblePriceLevels.map((level) => {
            const buyWidth = (level.buy / maxLevelVolume) * 100;
            const sellWidth = (level.sell / maxLevelVolume) * 100;
            const neutralWidth = (level.neutral / maxLevelVolume) * 100;
            const target = rows.find((row) => Number(row.price).toFixed(level.price < 10 ? 3 : 2) === level.price.toFixed(level.price < 10 ? 3 : 2));
            return (
              <button key={level.price} type="button" onClick={() => target && onSelect(tickTimestamp(target))} className="grid w-full grid-cols-[48px_minmax(0,1fr)_64px] items-center gap-2 rounded px-1 py-0.5 text-left hover:bg-muted/40">
                <span className="font-mono text-[11px]">{displayNumber(level.price, level.price < 10 ? 3 : 2)}</span>
                <span className="flex h-2.5 overflow-hidden rounded-sm bg-muted/70">
                  <span className="bg-red-500/75" style={{ width: `${buyWidth}%` }} />
                  <span className="bg-emerald-500/75" style={{ width: `${sellWidth}%` }} />
                  <span className="bg-slate-400/60" style={{ width: `${neutralWidth}%` }} />
                </span>
                <span className="text-right font-mono text-[10px] text-muted-foreground">{displayVolume(level.volume)}</span>
              </button>
            );
          })}
        </div>
        <div className="mt-2 flex items-center justify-between gap-3 text-[10px] text-muted-foreground">
          <div className="flex items-center gap-3">
            <span><i className="mr-1 inline-block size-2 rounded-full bg-red-500/75" />买</span>
            <span><i className="mr-1 inline-block size-2 rounded-full bg-emerald-500/75" />卖</span>
            <span><i className="mr-1 inline-block size-2 rounded-full bg-slate-400/60" />中性</span>
          </div>
          {priceLevels.length > visiblePriceLevels.length ? <span>其余价位请切换原始明细</span> : null}
        </div>
      </section>
      <section className="rounded-md bg-muted/10 p-3">
        <div className="flex items-center justify-between"><span className="text-xs font-medium">分钟成交强弱</span><span className="text-[11px] text-muted-foreground">{stats.count} 条明细</span></div>
        <div className="mt-3 flex min-h-16 items-end gap-px overflow-hidden pb-4">
          {minuteGroups.map((group) => {
            const net = group.buy - group.sell;
            const intensity = Math.max(0.18, group.volume / maxMinuteVolume);
            return <button key={group.label} type="button" title={`${group.label} · 成交量 ${displayVolume(group.volume)}`} onClick={() => onSelect(tickTimestamp(group.rows[0]))} className={`relative h-12 min-w-0 flex-1 rounded-t-sm ${net >= 0 ? "bg-red-500/75 hover:bg-red-500" : "bg-emerald-500/75 hover:bg-emerald-500"}`} style={{ height: `${12 + intensity * 36}px`, opacity: 0.35 + intensity * 0.65 }} />;
          })}
        </div>
      </section>
    </div>
  );
}

function normalizeTickSide(row: HistoricalTradeTick) {
  const text = String(row.side ?? row.buy_sell ?? "").toUpperCase();
  return text === "B" || text === "BUY" ? "BUY" : text === "S" || text === "SELL" ? "SELL" : "NEUTRAL";
}

function tickTimestamp(row: HistoricalTradeTick) {
  const date = row.trade_date;
  const time = row.time_label ?? row.trade_time ?? row.time;
  if (date && time) {
    return epoch(`${date}T${time.length === 5 ? `${time}:00` : time}+08:00`);
  }
  if (row.trade_datetime) {
    const parsed = Date.parse(row.trade_datetime);
    if (Number.isFinite(parsed)) return parsed;
  }
  return 0;
}

function TradeList({
  trades,
  current,
  onSelect,
}: {
  trades: PortfolioTrade[];
  current: number;
  onSelect: (trade: PortfolioTrade) => void;
}) {
  return (
    <div className="rounded-md bg-background">
      <div className="px-3 py-2 text-xs font-medium">
        当日交易{" "}
        <span className="ml-1 font-mono text-muted-foreground">
          {trades.length}
        </span>
      </div>
      {trades.length ? (
        <div className="space-y-1">
          {trades.map((trade) => (
            <button
              key={trade.id}
              type="button"
              onClick={() => onSelect(trade)}
              className={`flex w-full items-center justify-between gap-2 px-3 py-2 text-left text-xs hover:bg-muted/40 ${Math.abs(epoch(trade.tradedAt) - current) < 60_000 ? "bg-muted/60" : ""}`}
            >
              <span>
                <span
                  className={`font-medium ${trade.side === "BUY" ? "text-red-600" : "text-emerald-600"}`}
                >
                  {trade.side === "BUY" ? "B" : "S"}
                </span>
                <span className="ml-2 font-mono">
                  {formatTime(epoch(trade.tradedAt))}
                </span>
              </span>
              <span className="font-mono">{displayNumber(trade.price)}</span>
            </button>
          ))}
        </div>
      ) : (
        <p className="p-3 text-xs text-muted-foreground">
          当天没有已关联交易，可添加观察点。
        </p>
      )}
    </div>
  );
}
function MarginTab({
  rows,
  tradeDate,
}: {
  rows: MarginStockDetail[];
  tradeDate: string;
}) {
  const latest = rows[0];
  return (
    <div className="space-y-3">
      <SectionTitle
        title="两融趋势"
        detail={`截至 ${tradeDate} 可见的最近数据`}
      />
      <div className="grid grid-cols-2 gap-2">
        <Summary
          label="融资余额"
          value={displayNumber(latest?.financingBalance, 0)}
        />
        <Summary
          label="融券余额"
          value={displayNumber(latest?.securitiesLendingBalance, 0)}
        />
        <Summary
          label="融资买入"
          value={displayNumber(latest?.financingBuyAmount, 0)}
        />
        <Summary
          label="两融余额"
          value={displayNumber(latest?.marginBalance, 0)}
        />
      </div>
      <DataRows
        rows={rows
          .slice(0, 12)
          .map((row) => [
            row.tradeDate,
            displayNumber(row.financingBalance, 0),
            displayNumber(row.securitiesLendingBalance, 0),
          ])}
        headers={["日期", "融资余额", "融券余额"]}
        empty="暂无两融历史。"
      />
    </div>
  );
}
function FinanceTab({
  finance,
  valuation,
  business,
  forecast,
  research,
  loading,
  error,
  detailsLoading,
  detailsError,
  tradeDate,
}: {
  finance: StockFinanceReportResponse[];
  valuation: StockValuationResponse["rows"];
  business: StockBusinessCompositionResponse | null;
  forecast: StockProfitForecastResponse | null;
  research: StockCompanyNewsResponse | null;
  loading: boolean;
  error: string | null;
  detailsLoading: boolean;
  detailsError: string | null;
  tradeDate: string;
}) {
  const reportLabels: Record<StockFinanceReportResponse["reportType"], string> = {
    zcfzb: "资产负债表",
    lrb: "利润表",
    xjllb: "现金流量表",
  };
  const fieldLabels: Record<string, string> = {
    货币资金: "货币资金",
    资产总计: "资产总计",
    负债合计: "负债合计",
    股东权益合计: "股东权益",
    流动资产合计: "流动资产",
    流动负债合计: "流动负债",
    营业收入: "营业收入",
    营业总收入: "营业总收入",
    营业利润: "营业利润",
    利润总额: "利润总额",
    净利润: "净利润",
    归属于母公司股东的净利润: "归母净利润",
    基本每股收益: "基本 EPS",
    稀释每股收益: "稀释 EPS",
    经营活动产生的现金流量净额: "经营现金流",
    投资活动产生的现金流量净额: "投资现金流",
    筹资活动产生的现金流量净额: "筹资现金流",
    现金及现金等价物净增加额: "现金净增加",
    营业收入_yoy: "营业收入同比",
    营业总收入_yoy: "营业总收入同比",
    营业成本_yoy: "营业成本同比",
    营业利润_yoy: "营业利润同比",
    净利润_yoy: "净利润同比",
    归属于母公司所有者的净利润_yoy: "归母净利润同比",
    基本每股收益_yoy: "基本 EPS 同比",
    稀释每股收益_yoy: "稀释 EPS 同比",
    研发费用: "研发费用",
    销售费用: "销售费用",
    管理费用: "管理费用",
    财务费用: "财务费用",
    投资收益: "投资收益",
    所得税费用: "所得税费用",
    持续经营净利润: "持续经营净利润",
    少数股东损益: "少数股东损益",
    综合收益总额: "综合收益总额",
  };
  const reportFieldLabels: Record<string, Record<string, string>> = {
    zcfzb: {
      T007: "货币资金",
      T008: "交易性金融资产",
      T009: "应收票据",
      T010: "应收账款",
      T020: "流动资产合计",
      T039: "资产总计",
      T040: "短期借款",
      T042: "应付票据",
      T043: "应付账款",
      T053: "流动负债合计",
      T062: "负债合计",
      T063: "实收资本 / 股本",
      T064: "资本公积",
      T067: "未分配利润",
      T071: "所有者权益合计",
      T072: "负债和股东权益合计",
      T077: "归母权益合计",
    },
    lrb: {},
    xjllb: {
      T008: "销售商品收到的现金",
      T011: "经营活动现金流入小计",
      T012: "购买商品支付的现金",
      T016: "经营活动现金流出小计",
      T017: "经营活动产生的现金流量净额",
      T031: "投资活动产生的现金流量净额",
      T037: "筹资活动产生的现金流量净额",
      T042: "现金及现金等价物净增加额",
    },
  };
  const formatDate = (value: string | null | undefined) => {
    if (!value) return "-";
    const match = value.match(/^(\d{4})[年-]?(\d{2})[月-]?(\d{2})?/);
    return match ? `${match[1]}-${match[2]}${match[3] ? `-${match[3]}` : ""}` : value.slice(0, 10);
  };
  const formatValue = (value: number | string | null | undefined) => {
    if (value == null || value === "") return "-";
    if (typeof value === "number") return displayNumber(value, 2);
    const numeric = Number(value);
    return Number.isFinite(numeric) && value.trim() !== "" ? displayNumber(numeric, 2) : value;
  };
  const formatValuationValue = (value: number | null | undefined, suffix = "") => {
    if (value == null || !Number.isFinite(value)) return "-";
    return `${displayNumber(value, 2)}${suffix}`;
  };
  const valuationSignal = (kind: "pe" | "pb" | "peg" | "ps" | "pcf", value: number | null): FinancialSignal | null => {
    if (value == null || !Number.isFinite(value)) return null;
    if (kind === "pe") return value < 0 ? { label: "亏损", tone: "negative" } : value <= 20 ? { label: "偏低", tone: "positive" } : value <= 40 ? { label: "中性", tone: "neutral" } : { label: "偏高", tone: "warning" };
    if (kind === "pb") return value < 1 ? { label: "低于净资产", tone: "positive" } : value <= 3 ? { label: "中性", tone: "neutral" } : { label: "偏高", tone: "warning" };
    if (kind === "peg") return value < 0 ? { label: "不可用", tone: "warning" } : value <= 1 ? { label: "偏低", tone: "positive" } : value <= 2 ? { label: "中性", tone: "neutral" } : { label: "偏高", tone: "warning" };
    if (kind === "ps") return value <= 2 ? { label: "偏低", tone: "positive" } : value <= 5 ? { label: "中性", tone: "neutral" } : { label: "偏高", tone: "warning" };
    return value <= 10 ? { label: "偏低", tone: "positive" } : value <= 20 ? { label: "中性", tone: "neutral" } : { label: "偏高", tone: "warning" };
  };
  const formatFinanceValue = (value: number | string | null | undefined) => {
    if (value == null || value === "") return "-";
    const numeric = typeof value === "number" ? value : Number(value);
    if (!Number.isFinite(numeric)) return String(value);
    const absolute = Math.abs(numeric);
    if (absolute >= 100_000_000) return `${(numeric / 100_000_000).toFixed(2).replace(/\.00$/u, "")} 亿`;
    if (absolute >= 10_000) return `${(numeric / 10_000).toFixed(2).replace(/\.00$/u, "")} 万`;
    return displayNumber(numeric, 2);
  };
  const formatReportFieldValue = (key: string, value: number | string | null | undefined) => {
    if (/_yoy$/u.test(key)) {
      const numeric = typeof value === "number" ? value : Number(value);
      if (Number.isFinite(numeric)) return `${displayNumber(Math.abs(numeric) <= 1.5 ? numeric * 100 : numeric, 2)}%`;
    }
    return formatFinanceValue(value);
  };
  const fieldLabel = (reportType: StockFinanceReportResponse["reportType"], key: string) => {
    if (reportFieldLabels[reportType]?.[key]) return reportFieldLabels[reportType][key];
    if (fieldLabels[key]) return fieldLabels[key];
    if (/^T\d+$/u.test(key)) return `其他指标 ${key.slice(1)}`;
    return key
      .replaceAll("_", " ")
      .replace(/([a-z])([A-Z])/g, "$1 $2")
      .slice(0, 22);
  };
  const sortedValuation = [...valuation].sort((a, b) => b.date.localeCompare(a.date));
  const latestValuation = sortedValuation[0];
  const sortedReports = (Object.keys(reportLabels) as StockFinanceReportResponse["reportType"][]).map((reportType) => {
    const report = finance.find((item) => item.reportType === reportType);
    return report
      ? {
          ...report,
          rows: [...report.rows].sort((a, b) => (b.rq ?? "").localeCompare(a.rq ?? "")),
        }
      : {
          symbol: "",
          reportType,
          rows: [],
          fetchedAt: null,
          source: "unavailable",
        } satisfies StockFinanceReportResponse;
  });
  const latestReportDate = sortedReports
    .flatMap((report) => report.rows.map((row) => row.rq ?? ""))
    .filter(Boolean)
    .sort((a, b) => b.localeCompare(a))[0];
  const businessIsFuture = Boolean(business?.reportDate && business.reportDate > tradeDate);
  const visibleBusiness = businessIsFuture ? null : business;
  const reportCount = sortedReports.filter((report) => report.rows.length > 0).length;
  const researchItems = (research?.items ?? [])
    .filter((item) => !item.issueDate || item.issueDate.slice(0, 10).replaceAll("-", "") <= tradeDate.replaceAll("-", ""))
    .sort((left, right) => (right.issueDate ?? "").localeCompare(left.issueDate ?? ""));
  const latestRating = researchItems.find((item) => item.rating?.trim())?.rating ?? null;
  const ratingBuckets = researchItems.reduce<Record<ResearchRatingBucket, number>>((counts, item) => {
    const bucket = researchRatingBucket(item.rating);
    counts[bucket] += 1;
    return counts;
  }, { buy: 0, add: 0, neutral: 0, reduce: 0, sell: 0, other: 0 });
  const ratedCount = researchItems.filter((item) => researchRatingBucket(item.rating) !== "other").length;
  const ratingScore = ratedCount
    ? (ratingBuckets.buy * 5 + ratingBuckets.add * 4 + ratingBuckets.neutral * 3 + ratingBuckets.reduce * 2 + ratingBuckets.sell) / ratedCount
    : null;
  const ratingSignal = ratingScore == null ? null : ratingScore >= 4 ? { label: "偏多", tone: "positive" as const } : ratingScore >= 3 ? { label: "中性", tone: "neutral" as const } : { label: "偏空", tone: "negative" as const };
  const latestRows = {
    zcfzb: sortedReports.find((report) => report.reportType === "zcfzb")?.rows[0],
    lrb: sortedReports.find((report) => report.reportType === "lrb")?.rows[0],
    xjllb: sortedReports.find((report) => report.reportType === "xjllb")?.rows[0],
  };
  const numericField = (fields: Record<string, number | string | null> | undefined, ...keys: string[]) => {
    if (!fields) return null;
    for (const key of keys) {
      const value = fields[key];
      if (value == null || value === "") continue;
      const number = typeof value === "number" ? value : Number(value);
      if (Number.isFinite(number)) return number;
    }
    return null;
  };
  const revenue = numericField(latestRows.lrb?.fields, "营业收入", "营业总收入");
  const previousRevenue = numericField(sortedReports.find((report) => report.reportType === "lrb")?.rows[1]?.fields, "营业收入", "营业总收入");
  const cost = numericField(latestRows.lrb?.fields, "营业成本");
  const netProfit = numericField(latestRows.lrb?.fields, "归属于母公司所有者的净利润", "归母净利润", "净利润");
  const previousNetProfit = numericField(sortedReports.find((report) => report.reportType === "lrb")?.rows[1]?.fields, "归属于母公司所有者的净利润", "归母净利润", "净利润");
  const eps = numericField(latestRows.lrb?.fields, "基本每股收益", "稀释每股收益");
  const assets = numericField(latestRows.zcfzb?.fields, "T039", "资产总计", "总资产");
  const liabilities = numericField(latestRows.zcfzb?.fields, "T062", "负债合计", "总负债");
  const equity = numericField(latestRows.zcfzb?.fields, "T071", "所有者权益合计", "股东权益");
  const operatingCashFlow = numericField(latestRows.xjllb?.fields, "经营活动产生的现金流量净额", "T017");
  const previousOperatingCashFlow = numericField(sortedReports.find((report) => report.reportType === "xjllb")?.rows[1]?.fields, "经营活动产生的现金流量净额", "T017");
  const growthSignal = (current: number | null, previous: number | null): FinancialSignal | null => {
    if (current == null || previous == null || previous === 0) return null;
    const growth = ((current - previous) / Math.abs(previous)) * 100;
    return growth >= 10 ? { label: `同比 +${displayNumber(growth, 1)}%`, tone: "positive" } : growth >= 0 ? { label: `同比 +${displayNumber(growth, 1)}%`, tone: "neutral" } : growth > -10 ? { label: `同比 ${displayNumber(growth, 1)}%`, tone: "warning" } : { label: `同比 ${displayNumber(growth, 1)}%`, tone: "negative" };
  };
  const marginSignal = (value: number | null, positiveLabel = "较好"): FinancialSignal | null => value == null ? null : value >= 15 ? { label: positiveLabel, tone: "positive" } : value >= 5 ? { label: "一般", tone: "neutral" } : value >= 0 ? { label: "偏弱", tone: "warning" } : { label: "为负", tone: "negative" };
  const debtRatio = assets && liabilities != null && assets !== 0 ? (liabilities / assets) * 100 : null;
  const grossMargin = revenue && cost != null && revenue !== 0 ? ((revenue - cost) / revenue) * 100 : null;
  const netMargin = revenue && netProfit != null && revenue !== 0 ? (netProfit / revenue) * 100 : null;
  const roe = equity && netProfit != null && equity !== 0 ? (netProfit / equity) * 100 : null;
  const qualityMetrics = [
    { label: "营业收入", value: displayScaled(revenue, 2), signal: growthSignal(revenue, previousRevenue) },
    { label: "归母净利润", value: displayScaled(netProfit, 2), signal: growthSignal(netProfit, previousNetProfit) },
    { label: "EPS", value: formatValue(eps), signal: eps == null ? null : eps > 0 ? { label: "盈利", tone: "positive" as const } : { label: "亏损", tone: "negative" as const } },
    { label: "毛利率", value: grossMargin == null ? "-" : `${displayNumber(grossMargin, 2)}%`, signal: marginSignal(grossMargin) },
    { label: "净利率", value: netMargin == null ? "-" : `${displayNumber(netMargin, 2)}%`, signal: marginSignal(netMargin) },
    { label: "资产负债率", value: debtRatio == null ? "-" : `${displayNumber(debtRatio, 2)}%`, signal: debtRatio == null ? null : debtRatio <= 50 ? { label: "稳健", tone: "positive" as const } : debtRatio <= 70 ? { label: "中性", tone: "neutral" as const } : { label: "偏高", tone: "warning" as const } },
    { label: "ROE（估算）", value: roe == null ? "-" : `${displayNumber(roe, 2)}%`, signal: marginSignal(roe, "较强") },
    { label: "经营现金流", value: displayScaled(operatingCashFlow, 2), signal: growthSignal(operatingCashFlow, previousOperatingCashFlow) },
  ];
  const coreReportFields: Record<StockFinanceReportResponse["reportType"], string[]> = {
    zcfzb: ["T007", "T020", "T039", "T062", "T071", "T077"],
    lrb: ["营业收入", "营业利润", "净利润", "归属于母公司所有者的净利润", "基本每股收益"],
    xjllb: ["经营活动产生的现金流量净额", "投资活动产生的现金流量净额", "筹资活动产生的现金流量净额", "现金及现金等价物净增加额", "T017", "T031"],
  };

  return (
    <div className="min-w-0 max-w-full space-y-4">
      <SectionTitle title="财务研究" detail={`仅展示不晚于 ${tradeDate} 的历史数据；预测单独标注`} />
      {loading ? (
        <div className="space-y-2" role="status" aria-label="财务数据加载中">
          <Skeleton className="h-8 w-full" />
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-36 w-full" />
        </div>
      ) : null}
      {error ? <div className="rounded-md bg-amber-50/70 px-3 py-2 text-[11px] text-amber-800">{error}，已展示当前可用的部分数据。</div> : null}

      <div className="grid grid-cols-2 gap-2 xl:grid-cols-4">
        <Summary label="最新报告期" value={formatDate(latestReportDate)} />
        <Summary label="报表覆盖" value={`${reportCount}/3 张`} />
        <Summary label="估值记录" value={`${valuation.length} 条`} />
        <Summary label="估值日期" value={formatDate(latestValuation?.date)} />
      </div>

      <section className="space-y-2 rounded-md bg-muted/35 p-3">
        <div className="flex items-center justify-between gap-2">
          <SectionTitle title="估值快照" detail="eltdx F10 · 按复盘日前最近记录；标签仅作绝对值初筛，仍需结合行业分位" />
          <span className="text-[10px] text-muted-foreground">{latestValuation ? formatDate(latestValuation.date) : "暂无"}</span>
        </div>
        {latestValuation ? (
          <>
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 xl:grid-cols-5">
              <Summary label="PE TTM" value={formatValuationValue(latestValuation.peTtm, "x")} signal={valuationSignal("pe", latestValuation.peTtm)} />
              <Summary label="PE BFW" value={formatValuationValue(latestValuation.peBfw, "x")} signal={valuationSignal("pe", latestValuation.peBfw)} />
              <Summary label="PB MRQ" value={formatValuationValue(latestValuation.pbMrq, "x")} signal={valuationSignal("pb", latestValuation.pbMrq)} />
              <Summary label="PB BFW" value={formatValuationValue(latestValuation.pbBfw, "x")} signal={valuationSignal("pb", latestValuation.pbBfw)} />
              <Summary label="PS TTM" value={formatValuationValue(latestValuation.psTtm, "x")} signal={valuationSignal("ps", latestValuation.psTtm)} />
              <Summary label="PS BFW" value={formatValuationValue(latestValuation.psBfw, "x")} />
              <Summary label="PCF TTM" value={formatValuationValue(latestValuation.pcfOcfTtm, "x")} />
              <Summary label="PEG" value={formatValuationValue(latestValuation.peg)} signal={valuationSignal("peg", latestValuation.peg)} />
              <Summary label="流通市值" value={displayScaled(latestValuation.aliqMarketCap, 2)} />
              <Summary label="平均市值" value={displayScaled(latestValuation.avgMarketCap, 2)} />
            </div>
            <div className="overflow-x-auto rounded-md bg-background">
              <table className="min-w-[620px] w-full text-[11px]">
                <thead className="border-b border-border/60 text-muted-foreground">
                  <tr><th className="px-2.5 py-2 text-left font-medium">日期</th><th className="px-2.5 py-2 text-right font-medium">PE TTM</th><th className="px-2.5 py-2 text-right font-medium">PE BFW</th><th className="px-2.5 py-2 text-right font-medium">PB MRQ</th><th className="px-2.5 py-2 text-right font-medium">PS TTM</th><th className="px-2.5 py-2 text-right font-medium">PEG</th></tr>
                </thead>
                <tbody className="divide-y divide-border/50">
                  {sortedValuation.slice(0, 8).map((row) => <tr key={row.date}><td className="whitespace-nowrap px-2.5 py-2 font-mono">{formatDate(row.date)}</td><td className="px-2.5 py-2 text-right font-mono tabular-nums">{formatValuationValue(row.peTtm, "x")}</td><td className="px-2.5 py-2 text-right font-mono tabular-nums">{formatValuationValue(row.peBfw, "x")}</td><td className="px-2.5 py-2 text-right font-mono tabular-nums">{formatValuationValue(row.pbMrq, "x")}</td><td className="px-2.5 py-2 text-right font-mono tabular-nums">{formatValuationValue(row.psTtm, "x")}</td><td className="px-2.5 py-2 text-right font-mono tabular-nums">{formatValuationValue(row.peg)}</td></tr>)}
                </tbody>
              </table>
            </div>
          </>
        ) : (
          <div className="rounded-md bg-background px-3 py-3 text-xs text-muted-foreground">暂无复盘日前估值数据。</div>
        )}
      </section>

      <section className="space-y-2 rounded-md bg-muted/35 p-3">
        <SectionTitle title="财务质量快照" detail="由最近一期三大报表计算；ROE、毛利率、净利率为复盘时点的派生指标" />
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          {qualityMetrics.map((metric) => <Summary key={metric.label} label={metric.label} value={metric.value} signal={metric.signal} />)}
        </div>
        <p className="text-[10px] text-muted-foreground">判断原则：收入、利润、经营现金流看同比；毛利率、净利率、ROE 越高通常越好；资产负债率越低通常越稳健。最终仍需结合行业周期和公司阶段。</p>
      </section>

      <section className="space-y-2 rounded-md bg-muted/35 p-3">
        <div className="flex items-center justify-between gap-2">
          <SectionTitle title="机构评级 / 研究覆盖" detail="F10 研报评级；只显示不晚于复盘日的记录；评分为评级倾向映射，不是券商官方综合分" />
          <span className="text-[10px] text-muted-foreground">{researchItems.length} 条</span>
        </div>
        {detailsLoading && !research ? (
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
            <Skeleton className="h-12" /><Skeleton className="h-12" /><Skeleton className="h-12" /><Skeleton className="h-12" />
          </div>
        ) : researchItems.length ? (
          <>
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
              <Summary label="最新评级" value={latestRating ?? "-"} />
              <Summary label="研报倾向评分" value={ratingScore == null ? "-" : `${displayNumber(ratingScore, 1)} / 5`} signal={ratingSignal} />
              <Summary label="买入 / 增持" value={`${ratingBuckets.buy} / ${ratingBuckets.add}`} />
              <Summary label="中性 / 减持" value={`${ratingBuckets.neutral} / ${ratingBuckets.reduce + ratingBuckets.sell}`} />
              <Summary label="研报数量" value={`${researchItems.length} 条`} />
            </div>
            <div className="flex flex-wrap gap-1.5 text-[10px] text-muted-foreground">
              {(Object.keys(ratingBuckets) as ResearchRatingBucket[]).filter((bucket) => ratingBuckets[bucket] > 0).map((bucket) => (
                <span key={bucket} className="rounded-full bg-background px-2 py-1">{researchRatingLabel(bucket)} {ratingBuckets[bucket]}</span>
              ))}
              <span className="rounded-full bg-background px-2 py-1">覆盖 {ratedCount}/{researchItems.length} 条</span>
            </div>
            <div className="overflow-x-auto rounded-md bg-background">
              <table className="min-w-[680px] w-full text-[11px]">
                <thead className="border-b border-border/60 text-muted-foreground"><tr><th className="px-2.5 py-2 text-left font-medium">日期</th><th className="px-2.5 py-2 text-left font-medium">评级</th><th className="px-2.5 py-2 text-left font-medium">分析师</th><th className="px-2.5 py-2 text-left font-medium">研报标题</th></tr></thead>
                <tbody className="divide-y divide-border/50">{researchItems.slice(0, 8).map((item, index) => <tr key={`${item.recId ?? item.title ?? "research"}-${index}`}><td className="whitespace-nowrap px-2.5 py-2 font-mono">{formatDate(item.issueDate)}</td><td className="whitespace-nowrap px-2.5 py-2 font-medium">{item.rating ?? "-"}</td><td className="whitespace-nowrap px-2.5 py-2 text-muted-foreground">{item.analysts ?? "-"}</td><td className="min-w-[360px] px-2.5 py-2">{item.title ?? "-"}</td></tr>)}</tbody>
              </table>
            </div>
          </>
        ) : (
          <div className="rounded-md bg-background px-3 py-4 text-center text-xs text-muted-foreground">暂无复盘日前的机构评级或研报数据。</div>
        )}
      </section>

      <section className="min-w-0 space-y-2 rounded-md bg-muted/35 p-3">
        <div className="flex items-center justify-between gap-2">
          <SectionTitle title="主营构成" detail={visibleBusiness?.reportDate ? `报告期 ${formatDate(visibleBusiness.reportDate)} · 占比用于观察收入集中度` : "营业收入分项 · 占比用于观察收入集中度"} />
          <span className="text-[10px] text-muted-foreground">{visibleBusiness?.items.length ?? 0} 项</span>
        </div>
        {detailsLoading && !business ? (
          <div className="space-y-2"><Skeleton className="h-7 w-full" /><Skeleton className="h-7 w-full" /><Skeleton className="h-7 w-full" /></div>
        ) : visibleBusiness?.items.length ? (
          <div className="rounded-md bg-background p-2">
            <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
              {visibleBusiness.items.slice(0, 16).map((item, index) => {
                const ratio = item.ratio == null ? null : Math.max(0, Math.min(100, item.ratio));
                return (
                  <div key={`${item.name ?? "item"}-${index}`} className="min-w-0 rounded-md bg-muted/45 px-3 py-2.5">
                    <div className="flex items-center justify-between gap-2">
                      <span className="truncate text-[10px] text-muted-foreground">{item.category ?? item.subType ?? "未分类"}</span>
                      {ratio == null ? null : <span className="shrink-0 font-mono text-[10px] tabular-nums text-muted-foreground">{formatValue(item.ratio)}%</span>}
                    </div>
                    <div className="mt-1 truncate text-xs font-semibold" title={item.name ?? "-"}>{item.name ?? "-"}</div>
                    <div className="mt-2 flex items-end justify-between gap-2">
                      <span className="font-mono text-sm font-semibold tabular-nums">{displayScaled(item.revenue, 2)}</span>
                      {item.revenue == null ? null : <span className="text-[10px] text-muted-foreground">收入</span>}
                    </div>
                    {ratio != null ? (
                      <div className="mt-2 h-1 overflow-hidden rounded-full bg-background" aria-label={`收入占比 ${formatValue(item.ratio)}%`}>
                        <div className="h-full rounded-full bg-primary/60" style={{ width: `${ratio}%` }} />
                      </div>
                    ) : null}
                  </div>
                );
              })}
            </div>
          </div>
        ) : (
          <div className="rounded-md bg-background px-3 py-4 text-center text-xs text-muted-foreground">{businessIsFuture ? "复盘日之后的主营构成已隐藏。" : "暂无主营构成数据。"}</div>
        )}
      </section>

      <section className="space-y-2 rounded-md bg-muted/35 p-3">
        <div className="flex items-center justify-between gap-2">
          <SectionTitle title="三大财务报表" detail="保留最近 6 个报告期；核心指标已解析，金额自动换算为万 / 亿；重点观察同比、利润和现金流变化" />
          <span className="text-[10px] text-muted-foreground">{reportCount}/3</span>
        </div>
        <div className="space-y-3">
          {sortedReports.map((report) => {
            const rows = report.rows.slice(0, 6);
            const allFields = Array.from(
              new Set(rows.flatMap((row) => Object.keys(row.fields).filter((key) => key !== "rq"))),
            );
            const preferredFields = [
              ...Object.keys(reportFieldLabels[report.reportType] ?? {}),
              ...Object.keys(fieldLabels),
            ];
            const fields = Array.from(new Set([
              ...preferredFields.filter((field) => allFields.includes(field)),
              ...allFields,
            ])).slice(0, 24);
            const latest = rows[0];
            const coreFields = coreReportFields[report.reportType]
              .filter((field) => latest?.fields[field] != null)
              .slice(0, 6);
            return (
              <div key={report.reportType} className="min-w-0 rounded-md bg-background p-3">
                <div className="flex items-center justify-between gap-2 border-b border-border/60 pb-2">
                  <span className="text-xs font-semibold">{reportLabels[report.reportType]}</span>
                  <span className="text-[10px] text-muted-foreground">
                    {rows.length ? `${rows.length} 期` : report.source === "unavailable" ? "暂不可用" : "暂无记录"}
                  </span>
                </div>
                {latest && coreFields.length ? (
                  <div className="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-3 xl:grid-cols-6">
                    {coreFields.map((field) => (
                      <div key={field} className="min-w-0 rounded-md bg-muted/45 px-2.5 py-2">
                        <div className="truncate text-[10px] text-muted-foreground">{fieldLabel(report.reportType, field)}</div>
                        <div className="mt-1 truncate font-mono text-xs font-medium tabular-nums">{formatReportFieldValue(field, latest.fields[field])}</div>
                      </div>
                    ))}
                  </div>
                ) : null}
                {rows.length && fields.length ? (
                  <div className="mt-3 overflow-x-auto rounded-md border border-border/50">
                    <table className="min-w-[760px] w-full text-[11px]">
                      <thead className="text-muted-foreground">
                        <tr>
                          <th className="sticky left-0 z-10 bg-background px-2.5 py-2 text-left font-medium">报告期</th>
                          {fields.map((field) => <th key={field} className="whitespace-nowrap px-2.5 py-2 text-right font-medium" title={`原始字段 ${field}`}>{fieldLabel(report.reportType, field)}</th>)}
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-border/50">
                        {rows.map((row, index) => (
                          <tr key={`${row.rq ?? "row"}-${index}`}>
                            <td className="sticky left-0 whitespace-nowrap bg-background px-2.5 py-2 font-mono">{formatDate(row.rq)}</td>
                            {fields.map((field) => <td key={field} className="whitespace-nowrap px-2.5 py-2 text-right font-mono tabular-nums">{formatReportFieldValue(field, row.fields[field])}</td>)}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="px-2 py-5 text-center text-xs text-muted-foreground">暂无该类报表。</div>
                )}
              </div>
            );
          })}
        </div>
      </section>

      <section className="min-w-0 space-y-2 rounded-md bg-muted/35 p-3">
          <div className="flex items-center justify-between gap-2">
            <SectionTitle title="业绩预测" detail="F10 前瞻数据，不参与历史截断" />
            <span className="text-[10px] text-muted-foreground">{forecast?.items.length ?? 0} 期</span>
          </div>
          {detailsLoading && !forecast ? (
            <div className="space-y-2"><Skeleton className="h-12 w-full" /><Skeleton className="h-12 w-full" /></div>
          ) : forecast?.items.length ? (
            <div className="grid grid-cols-2 gap-2">{forecast.items.slice(0, 6).map((item, index) => <div key={`${item.year}-${index}`} className="rounded-md bg-background px-2.5 py-2"><div className="flex items-center justify-between gap-2"><span className="text-xs font-semibold">{item.year || "-"}</span><Badge variant="secondary" className="px-1.5 py-0 text-[9px]">预测</Badge></div><div className="mt-2 grid grid-cols-2 gap-x-2 gap-y-1 text-[10px]"><span className="text-muted-foreground">EPS</span><span className="text-right font-mono">{formatValue(item.eps)}</span><span className="text-muted-foreground">净利润</span><span className="text-right font-mono">{formatValue(item.netProfit)}</span><span className="text-muted-foreground">营收</span><span className="text-right font-mono">{formatValue(item.revenue)}</span></div></div>)}</div>
          ) : (
            <div className="rounded-md bg-background px-3 py-4 text-center text-xs text-muted-foreground">暂无业绩预测数据。</div>
          )}
      </section>
      {detailsError ? <div className="rounded-md bg-amber-50/70 px-3 py-2 text-[11px] text-amber-800">{detailsError}，其余财务数据仍可继续查看。</div> : null}
    </div>
  );
}
function FundFlowTab({
  rows,
}: {
  rows: Array<{
    trade_date: string;
    mainNetAmountWan: number | null;
    superNetAmountWan: number | null;
    amountWan: number | null;
    changePct: number | null;
  }>;
}) {
  return (
    <div className="space-y-3">
      <SectionTitle title="日级资金流" detail="只展示游标日期及之前的日数据" />
      <DataRows
        rows={rows
          .slice(0, 15)
          .map((row) => [
            row.trade_date,
            displayNumber(row.mainNetAmountWan),
            displayNumber(row.superNetAmountWan),
            row.changePct == null ? "-" : `${row.changePct.toFixed(2)}%`,
          ])}
        headers={["日期", "主力净流入", "超大单", "涨跌幅"]}
        empty="暂无资金流历史。"
      />
    </div>
  );
}

function InlineFieldLoading({ label }: { label: string }) {
  return (
    <span
      className="inline-flex items-center justify-center text-muted-foreground"
      role="status"
      aria-label={label}
      title={label}
    >
      <Loader2 className="size-3 animate-spin" aria-hidden="true" />
    </span>
  );
}

function IndustryTab({
  meta,
  metaLoading,
  sectors,
  sectorsLoading,
  tdxBoards,
  tdxBoardsLoading,
  activeSectorName,
  activeSectorSymbol,
  manualSector,
  targetCodes,
  targetCodesLoading,
  onSaveManual,
  onClearManual,
}: {
  meta: Awaited<ReturnType<typeof fetchStockMeta>> | null;
  metaLoading: boolean;
  sectors: Awaited<ReturnType<typeof fetchStockSectors>> | null;
  sectorsLoading: boolean;
  tdxBoards: EasyTdxResponse<EasyTdxBelongBoardRow> | null;
  tdxBoardsLoading: boolean;
  activeSectorName: string | null;
  activeSectorSymbol: string | null;
  manualSector: SectorContext | null;
  targetCodes: IndustryApplicationTargetCode[];
  targetCodesLoading: boolean;
  onSaveManual: (context: SectorContext) => Promise<void>;
  onClearManual: () => void;
}) {
  const [targetQuery, setTargetQuery] = useState("");
  const [saving, setSaving] = useState(false);
  const isThsIndustry = (row: { source?: string | null }) =>
    row.source?.includes("ths_industry_constituent_snapshot") ?? false;
  const tdxIndustryRows = (tdxBoards?.items ?? [])
    .filter((item) => isTdxIndustryBoard(item) && Boolean(item.board_code?.trim()))
    .map((item) => ({
      sector_id: `tdx:${item.board_code}`,
      name: item.board_name?.trim() || item.board_code,
      topic_id: item.board_code,
      category_raw: 0,
      changePercent: item.pre_close
        ? ((item.close - item.pre_close) / item.pre_close) * 100
        : null,
      source: "easy_tdx.mac.belong_board",
    }));
  const tdxConceptRows = (tdxBoards?.items ?? [])
    .filter((item) => item.board_type === 4 && Boolean(item.board_code?.trim()))
    .map((item) => ({
      sector_id: `tdx:${item.board_code}`,
      name: item.board_name?.trim() || item.board_code,
      topic_id: item.board_code,
      category_raw: 1,
      changePercent: item.pre_close
        ? ((item.close - item.pre_close) / item.pre_close) * 100
        : null,
      source: "easy_tdx.mac.belong_board",
    }));
  const existingRows = [
    ...(sectors?.industries ?? []),
    ...(sectors?.concepts ?? []),
  ];
  function dedupeRows<T extends { name?: string | null; source?: string | null }>(items: T[]) {
    return Array.from(
      new Map(items.map((row) => [`${row.name ?? ""}:${row.source ?? ""}`, row])).values(),
    );
  }
  const industryRows = dedupeRows([
    ...existingRows.filter(isThsIndustry),
    ...tdxIndustryRows,
    ...existingRows.filter(
      (row) => !isThsIndustry(row) && Number(row.category_raw) === 0,
    ),
  ]).slice(0, 20);
  const conceptRows = dedupeRows(
    [
      ...tdxConceptRows,
      ...existingRows.filter((row) => !isThsIndustry(row) && Number(row.category_raw) !== 0),
    ],
  ).slice(0, 20);
  const rows = industryRows.length || conceptRows.length
    ? [...industryRows, ...conceptRows]
    : meta?.industry
      ? [{ name: meta.industry, source: "stock_basic", changePercent: null }]
      : [];
  const industryDisplayRows = industryRows.length
    ? industryRows
    : conceptRows.length
      ? []
      : rows;
  const industryQuoteRows = industryDisplayRows.filter(
    (row) => row.changePercent != null,
  );
  const industryTagRows = industryDisplayRows.filter(
    (row) => row.changePercent == null,
  );
  const source = sectors?.count
    ? sectors.source
    : tdxBoards?.count
      ? tdxBoards.source
    : meta?.industry
      ? "postgres.sec_stock_basic.industry"
      : sectors?.source || "未返回";
  const automaticIndustry =
    industryRows.find((row) => row.source?.includes("ths_industry_constituent_snapshot"))?.name ||
    industryRows.find((row) => row.source?.includes("easy_tdx.mac.belong_board"))?.name ||
    meta?.industry ||
    industryRows.find((row) => "category_raw" in row && row.category_raw === 0)?.name ||
    industryRows[0]?.name ||
    null;
  const normalizeSectorText = (value: string | null | undefined) =>
    String(value ?? "").trim().replace(/行业$/u, "");
  const normalizeSectorCode = (value: string | null | undefined) =>
    String(value ?? "").trim().replace(/^(sh|sz)/iu, "");
  const selectedName = normalizeSectorText(activeSectorName);
  const selectedCode = normalizeSectorCode(activeSectorSymbol);
  const hasTargetQuery = targetQuery.trim().length > 0;
  const sectorsPending = (sectorsLoading && !sectors) || (tdxBoardsLoading && !tdxBoards);
  const metaPending = metaLoading && !meta;

  const filteredTargets = useMemo(() => {
    const query = targetQuery.trim().toLowerCase();
    const candidates = targetCodes.filter((item) => {
      if (!query) return true;
      return `${item.name} ${item.code} ${item.kind}`.toLowerCase().includes(query);
    });
    return candidates.slice(0, query ? 12 : 8);
  }, [targetCodes, targetQuery]);

  function targetKindLabel(kind: IndustryApplicationTargetCode["kind"]) {
    return kind === "index" ? "指数" : kind === "concept" ? "概念" : "行业";
  }

  async function selectTarget(target: IndustryApplicationTargetCode) {
    setSaving(true);
    try {
      // 880xxx / 881xxx 行业代码在分钟行情表中按指数代码读取。
      await onSaveManual({
        name: target.name,
        symbol: target.code,
        targetType: "index",
      });
      setTargetQuery("");
    } finally {
      setSaving(false);
    }
  }

  function renderSectorRow(
    row: (typeof rows)[number],
    index: number,
    group: "industry" | "industry-tag" | "concept",
  ) {
    const source = row.source ?? "未知来源";
    const topicId = "topic_id" in row ? row.topic_id : null;
    const categoryRaw = "category_raw" in row ? Number(row.category_raw) : null;
    const isDirectTdxIndustry = source.includes("easy_tdx.mac.belong_board") && categoryRaw === 0;
    const isTdxTag = (source.includes("easy_tdx.mac.belong_board") && !isDirectTdxIndustry) || (source === "eltdx" && !/^\d{6}$/.test(String(topicId ?? "")));
    const rowCode = normalizeSectorCode(topicId);
    const isSelected = Boolean(
      (selectedCode && rowCode && selectedCode === rowCode) ||
      (selectedName && normalizeSectorText(row.name) === selectedName),
    );
    const sourceLabel = source.includes("ths_industry_constituent_snapshot")
      ? "THS 行业"
      : source === "stock_basic"
        ? "基础资料"
        : source === "eltdx"
          ? "TDX"
          : source.includes("easy_tdx.mac.belong_board")
            ? "TDX 行业"
            : source;
    const change = isTdxTag || row.changePercent == null
      ? "-"
      : `${row.changePercent > 0 ? "+" : ""}${row.changePercent.toFixed(2)}%`;
    const isThs = isThsIndustry(row);
    const tone = group === "industry"
      ? "bg-information/5 text-foreground"
      : group === "industry-tag"
        ? "bg-muted/45 text-foreground"
        : "bg-amber-500/5 text-foreground";
    const selectedTone = group === "industry"
      ? "ring-1 ring-information/40 bg-information/10"
      : group === "industry-tag"
        ? "ring-1 ring-primary/25 bg-muted/75"
        : "ring-1 ring-amber-500/40 bg-amber-500/10";
    return (
      <div
        key={`${row.name ?? "sector"}-${row.source ?? "source"}-${index}`}
        className={`min-w-0 rounded-md px-3 py-2.5 transition-colors ${isSelected ? selectedTone : tone} ${isThs ? "col-span-full" : ""}`}
        title={`来源：${source}`}
        aria-current={isSelected ? "true" : undefined}
      >
        <div className="flex min-w-0 items-start justify-between gap-2">
          <span className="flex min-w-0 items-center gap-1.5 truncate text-xs font-medium" title={row.name ?? "-"}>
            <span className="min-w-0 truncate">{row.name ?? "-"}</span>
            {isSelected ? <Badge variant="default" className="shrink-0 px-1.5 py-0 text-[9px]">当前绑定</Badge> : null}
          </span>
          <Badge
            variant="secondary"
            className={`shrink-0 px-1.5 py-0 text-[9px] ${group === "industry" ? "bg-information/10 text-information" : group === "industry-tag" ? "bg-muted text-muted-foreground" : "bg-amber-500/10 text-amber-700"}`}
          >
            {sourceLabel}
          </Badge>
        </div>
        {!isTdxTag ? (
          <div className="mt-2 flex items-center justify-between gap-2 text-[10px] text-muted-foreground">
            <span>{isDirectTdxIndustry ? "TDX 最新涨幅" : "当日涨幅"}</span>
            <span className={`font-mono ${row.changePercent == null ? "" : row.changePercent > 0 ? "text-red-600" : row.changePercent < 0 ? "text-emerald-600" : ""}`}>
              {change}
            </span>
          </div>
        ) : null}
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <SectionTitle
        title="行业与板块"
        detail="自动关联保留个股板块关系；复盘覆盖从 Industry Application 目录选择"
      />
      <div className="space-y-2 rounded-md bg-muted/20 p-3 text-xs">
        <div className="flex justify-between gap-3">
          <span className="text-muted-foreground">基础行业</span>
          <span className="flex min-w-0 items-center justify-end gap-1.5 truncate font-medium">
            {metaPending ? (
              <InlineFieldLoading label="正在加载基础行业" />
            ) : (
              automaticIndustry ?? "未返回"
            )}
          </span>
        </div>
        <div className="flex justify-between gap-3">
          <span className="text-muted-foreground">自动来源</span>
          <span className="flex min-w-0 items-center justify-end gap-1.5 truncate font-mono text-[11px]">
            {sectorsPending ? (
              <InlineFieldLoading label="正在加载自动来源" />
            ) : (
              source
            )}
          </span>
        </div>
        <div className="flex justify-between gap-3">
          <span className="text-muted-foreground">自动映射</span>
          <span className="flex items-center justify-end gap-1.5">
            {sectorsPending ? (
              <InlineFieldLoading label="正在加载自动映射" />
            ) : (
              `${rows.length} 个板块`
            )}
          </span>
        </div>
        <div className="flex justify-between gap-3">
          <span className="text-muted-foreground">映射依据</span>
          <span className="flex min-w-0 items-center justify-end gap-1.5 truncate text-right text-[11px]">
            {sectorsPending ? (
              <InlineFieldLoading label="正在加载映射依据" />
            ) : (
              "股票代码 → 行业/概念关系 → 目标目录"
            )}
          </span>
        </div>
        {manualSector ? (
          <div className="flex items-center justify-between gap-3 text-amber-700">
            <span>当前使用复盘覆盖</span>
            <span className="font-mono text-[11px]">{manualSector.symbol}</span>
          </div>
        ) : null}
      </div>
      <div className="space-y-2 rounded-md bg-muted/20 p-3">
        <div className="flex items-center justify-between gap-2">
          <span className="text-xs font-medium">选择复盘关联目标</span>
          {manualSector ? (
            <Button type="button" variant="ghost" size="sm" className="h-7 px-2 text-[11px]" onClick={onClearManual}>
              恢复自动
            </Button>
          ) : automaticIndustry ? (
            <Badge variant="secondary" className="max-w-[58%] truncate text-[10px]">
              当前行业：{automaticIndustry}
            </Badge>
          ) : null}
        </div>
        <Input
          value={targetQuery}
          onChange={(event) => setTargetQuery(event.target.value)}
          placeholder="搜索行业、概念或指数名称 / 代码"
          className="h-8 border-0 bg-background text-xs"
          aria-label="搜索行业、概念或指数"
        />
        {hasTargetQuery ? (
          targetCodesLoading ? (
            <div className="space-y-1.5" aria-label="正在加载目标目录">
              {Array.from({ length: 4 }, (_, index) => (
                <Skeleton key={index} className="h-9 w-full rounded-md" />
              ))}
            </div>
          ) : filteredTargets.length ? (
            <div className="max-h-52 space-y-1 overflow-auto">
              {filteredTargets.map((target) => (
                <button
                  key={`${target.kind}-${target.code}`}
                  type="button"
                  className="flex w-full items-center justify-between gap-2 rounded-md bg-background px-2.5 py-2 text-left text-xs transition hover:bg-muted"
                  onClick={() => void selectTarget(target)}
                  disabled={saving}
                >
                  <span className="min-w-0 truncate font-medium">{target.name}</span>
                  <span className="flex shrink-0 items-center gap-2 text-[10px] text-muted-foreground">
                    <span>{targetKindLabel(target.kind)}</span>
                    <span className="font-mono">{target.code}</span>
                    {target.tdx_fund_flow_supported ? (
                      <Badge variant="secondary" className="h-5 px-1.5 text-[9px]">Capital</Badge>
                    ) : null}
                  </span>
                </button>
              ))}
            </div>
          ) : (
            <div className="px-1 py-2 text-[11px] text-muted-foreground">目标目录中没有匹配项。</div>
          )
        ) : (
          <div className="rounded-md bg-background/70 px-2.5 py-2 text-[11px] text-muted-foreground">
            {manualSector
              ? `当前已使用复盘覆盖：${manualSector.name}`
              : automaticIndustry
                ? `已自动关联当前行业「${automaticIndustry}」，输入关键词可切换目标。`
                : "输入行业、概念或指数名称 / 代码后展开匹配目标。"}
          </div>
        )}
        <p className="text-[10px] leading-4 text-muted-foreground">
          选择后只覆盖当前复盘工作台；名称和代码来自已有目标目录，不再手填未知行业代码。
        </p>
      </div>
      {sectorsPending ? (
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3" aria-label="正在加载板块关联">
          {Array.from({ length: 6 }, (_, index) => (
            <div key={index} className="space-y-2 rounded-md bg-muted/35 px-3 py-2.5">
              <div className="flex items-center justify-between gap-2">
                <Skeleton className="h-3 w-20" />
                <Skeleton className="h-4 w-10 rounded-full" />
              </div>
              <div className="flex items-center justify-between gap-2">
                <Skeleton className="h-3 w-10" />
                <Skeleton className="h-3 w-12" />
              </div>
            </div>
          ))}
        </div>
      ) : rows.length ? (
        <div className="space-y-4" aria-label="自动板块关联">
          {industryQuoteRows.length ? (
            <section className="space-y-2" aria-labelledby="trade-review-industry-quotes">
              <div className="flex items-center gap-2 px-1 py-1">
                <span className="h-4 w-0.5 rounded-full bg-information" aria-hidden="true" />
                <span id="trade-review-industry-quotes" className="text-xs font-semibold text-information">行业板块行情</span>
                <Badge variant="secondary" className="bg-information/10 px-1.5 py-0 text-[9px] text-information">
                  {industryQuoteRows.length}
                </Badge>
                <span className="text-[10px] text-muted-foreground">有涨跌幅数据</span>
              </div>
              <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                {industryQuoteRows.map((row, index) => renderSectorRow(row, index, "industry"))}
              </div>
            </section>
          ) : null}
          {industryTagRows.length ? (
            <Collapsible defaultOpen className="space-y-2" aria-labelledby="trade-review-industry-group">
              <CollapsibleTrigger asChild>
                <button type="button" className="group flex w-full items-center gap-2 rounded-md px-1 py-1 text-left hover:bg-information/5">
                  <ChevronDown className="size-3.5 text-information transition-transform group-data-[state=closed]:-rotate-90" aria-hidden="true" />
                  <span className="h-4 w-0.5 rounded-full bg-information" aria-hidden="true" />
                  <span id="trade-review-industry-group" className="text-xs font-semibold text-information">行业</span>
                  <Badge variant="secondary" className="bg-information/10 px-1.5 py-0 text-[9px] text-information">
                    {industryTagRows.length}
                  </Badge>
                  <span className="ml-auto text-[10px] text-muted-foreground">点击折叠</span>
                </button>
              </CollapsibleTrigger>
              <CollapsibleContent className="grid grid-cols-2 gap-2 pl-5 sm:grid-cols-3">
                {industryTagRows.map((row, index) => renderSectorRow(row, index, "industry-tag"))}
              </CollapsibleContent>
            </Collapsible>
          ) : null}
          {conceptRows.length ? (
            <Collapsible defaultOpen className="space-y-2" aria-labelledby="trade-review-concept-group">
              <CollapsibleTrigger asChild>
                <button type="button" className="group flex w-full items-center gap-2 rounded-md px-1 py-1 text-left hover:bg-amber-500/5">
                  <ChevronDown className="size-3.5 text-amber-600 transition-transform group-data-[state=closed]:-rotate-90" aria-hidden="true" />
                  <span className="h-4 w-0.5 rounded-full bg-amber-500" aria-hidden="true" />
                  <span id="trade-review-concept-group" className="text-xs font-semibold text-amber-700">概念</span>
                  <Badge variant="secondary" className="bg-amber-500/10 px-1.5 py-0 text-[9px] text-amber-700">
                    {conceptRows.length}
                  </Badge>
                  <span className="ml-auto text-[10px] text-muted-foreground">点击折叠</span>
                </button>
              </CollapsibleTrigger>
              <CollapsibleContent className="grid grid-cols-2 gap-2 pl-5 sm:grid-cols-3">
                {conceptRows.map((row, index) => renderSectorRow(row, index, "concept"))}
              </CollapsibleContent>
            </Collapsible>
          ) : null}
        </div>
      ) : (
        <div className="rounded-md bg-muted/25 px-3 py-3 text-[11px] text-muted-foreground">
          {meta?.industry ? "当前有基础行业，但暂无细分板块映射；可从目标目录选择复盘关联。" : "暂无自动板块关联，可从目标目录选择复盘关联。"}
        </div>
      )}
    </div>
  );
}

type ReplayBar = ReturnType<typeof barsForPeriod>[number];
type ReplayFundFlowRow = {
  trade_date: string;
  mainNetAmountWan: number | null;
  superNetAmountWan: number | null;
  amountWan: number | null;
  changePct: number | null;
};

function ReplayLowerPanel({
  tab,
  tradeDate,
  period,
  current,
  currentAverage,
  stock,
  index,
  industry,
  industryName,
  industryTarget,
  industryCapital,
  industryBoardSummary,
  industryBoardQuote,
  industryDetail,
  industryLoading,
  industryError,
  markers,
  events,
  ticks,
  ticksLoading,
  onTickSelect,
  margin,
  finance,
  fundFlow,
  timeline,
}: {
  tab: string;
  tradeDate: string;
  period: ReplayPeriod;
  current?: ReplayBar;
  currentAverage: number | null;
  stock: StockIntradayResponse;
  index: StockIntradayResponse | null;
  industry: StockIntradayResponse | null;
  industryName: string | null;
  industryTarget: IndustryApplicationTargetCode | null;
  industryCapital: IndustryApplicationTdxFundFlowResponse | null;
  industryBoardSummary: EasyTdxBoardSummary | null;
  industryBoardQuote: EasyTdxBelongBoardRow | null;
  industryDetail: MarketIndicatorIndustryDetail | null;
  industryLoading: boolean;
  industryError: string | null;
  markers: ReplayMarker[];
  events: ReplayMarker[];
  ticks: HistoricalTradeTick[];
  ticksLoading: boolean;
  onTickSelect: (timestamp: number) => void;
  margin: MarginStockDetail[];
  finance: StockFinanceReportResponse[];
  fundFlow: ReplayFundFlowRow[];
  timeline: MarketIndicatorReplayTimelineResponse | null;
}) {
  if (tab === "行业") {
    return (
      <IndustryReplayLower
        stock={stock}
        tradeDate={tradeDate}
        industry={industry}
        index={index}
        industryName={industryName}
        target={industryTarget}
        capital={industryCapital}
        boardSummary={industryBoardSummary}
        boardQuote={industryBoardQuote}
        detail={industryDetail}
        timeline={timeline}
        currentTimestamp={current?.timestamp ?? 0}
        loading={industryLoading}
        error={industryError}
      />
    );
  }
  if (tab === "资金流") return <FundFlowReplayLower rows={fundFlow} />;
  if (tab === "两融") return <MarginReplayLower rows={margin} />;
  if (tab === "财务") return <FinanceReplayLower reports={finance} />;
  if (tab === "市场状态") {
    return (
      <MarketStateMaterializedSlot
        tradeDate={tradeDate}
        currentTimestamp={current?.timestamp ?? 0}
        industryName={industryName ?? industryTarget?.name}
        industryCode={industryTarget?.code.replace(/^(sh|sz|bj)/i, "")}
        mode={period === "1d" ? "daily" : isHistoricalReplayPeriod(period) ? "aggregate" : "intraday"}
        realEventCount={events.length}
      />
    );
  }
  if (tab === "逐笔成交") {
    return (
      <TicksReplayLower
        rows={ticks}
        currentTimestamp={current?.timestamp ?? 0}
        loading={ticksLoading}
        onSelect={onTickSelect}
      />
    );
  }
  return (
    <ReplaySummaryLower
      current={current}
      average={currentAverage}
      markers={markers}
      events={events}
    />
  );
}

function LowerMetric({ label, value, tone }: { label: string; value: string; tone?: "up" | "down" | "neutral" }) {
  return (
    <div className="min-w-0 rounded bg-background px-2.5 py-2">
      <span className="block truncate text-[10px] text-muted-foreground">{label}</span>
      <span className={`mt-1 block truncate font-mono text-xs font-medium ${tone === "up" ? "text-red-600" : tone === "down" ? "text-emerald-600" : "text-foreground"}`}>
        {value}
      </span>
    </div>
  );
}

function ReplaySummaryLower({
  current,
  average,
  markers,
  events,
}: {
  current?: ReplayBar;
  average: number | null;
  markers: ReplayMarker[];
  events: ReplayMarker[];
}) {
  return (
    <div className="h-full space-y-2 p-3">
      <div className="grid grid-cols-4 gap-2">
        <LowerMetric label="当前价" value={displayNumber(current?.close)} />
        <LowerMetric label="均价" value={displayNumber(average)} />
        <LowerMetric label="成交量" value={displayVolume(current?.volume)} />
        <LowerMetric label="已出现标记" value={`${markers.length} 个`} />
      </div>
      <div className="flex min-w-0 items-center gap-2 text-[11px] text-muted-foreground">
        <span className="shrink-0">当前游标</span>
        <span className="font-mono text-foreground">{formatTime(current?.timestamp)}</span>
        <span className="ml-auto shrink-0">Indicator {events.length} 个事件</span>
      </div>
    </div>
  );
}

function IndustryReplayLower({
  stock,
  tradeDate,
  industry,
  index,
  industryName,
  target,
  capital,
  boardSummary,
  boardQuote,
  detail,
  timeline,
  currentTimestamp,
  loading,
  error,
}: {
  stock: StockIntradayResponse;
  tradeDate: string;
  industry: StockIntradayResponse | null;
  index: StockIntradayResponse | null;
  industryName: string | null;
  target: IndustryApplicationTargetCode | null;
  capital: IndustryApplicationTdxFundFlowResponse | null;
  boardSummary: EasyTdxBoardSummary | null;
  boardQuote: EasyTdxBelongBoardRow | null;
  detail: MarketIndicatorIndustryDetail | null;
  timeline: MarketIndicatorReplayTimelineResponse | null;
  currentTimestamp: number;
  loading: boolean;
  error: string | null;
}) {
  const [selectedStock, setSelectedStock] = useState<{ code: string; name: string } | null>(null);
  const [stockDialogOpen, setStockDialogOpen] = useState(false);
  const [memberPage, setMemberPage] = useState(0);
  const memberGridElementRef = useRef<HTMLDivElement | null>(null);
  const memberCardElementRef = useRef<HTMLButtonElement | null>(null);
  const [memberGridSize, setMemberGridSize] = useState({ width: 0, height: 0 });
  const [memberCardSize, setMemberCardSize] = useState({ width: 0, height: 0 });

  useEffect(() => {
    const memberGridElement = memberGridElementRef.current;
    if (!memberGridElement) return;
    const memberCardElement = memberCardElementRef.current;

    const updateGridSize = () => {
      const rect = memberGridElement.getBoundingClientRect();
      setMemberGridSize((current) => {
        // ResizeObserver can report tiny fractional changes after the grid
        // recalculates its rows. Round them so measurement does not become a
        // render -> observer -> render loop when switching pages.
        const width = Math.round(rect.width * 100) / 100;
        const height = Math.round(rect.height * 100) / 100;
        return current.width === width && current.height === height ? current : { width, height };
      });
    };

    const updateCardSize = () => {
      if (memberCardElement) {
        const cardRect = memberCardElement.getBoundingClientRect();
        const cardStyle = getComputedStyle(memberCardElement);
        const paddingY = parseFloat(cardStyle.paddingTop) + parseFloat(cardStyle.paddingBottom);
        const contentHeight = Array.from(memberCardElement.children).reduce((total, child) => {
          const childElement = child as HTMLElement;
          const childStyle = getComputedStyle(childElement);
          return (
            total +
            childElement.getBoundingClientRect().height +
            parseFloat(childStyle.marginTop) +
            parseFloat(childStyle.marginBottom)
          );
        }, 0);
        const naturalHeight = Math.ceil(paddingY + contentHeight);
        const width = Math.round(cardRect.width * 100) / 100;
        setMemberCardSize((current) =>
          current.width === width && current.height === naturalHeight
            ? current
            : { width, height: naturalHeight },
        );
      }
    };

    updateGridSize();
    updateCardSize();
    if (typeof ResizeObserver === "undefined") return;
    const observer = new ResizeObserver(updateGridSize);
    observer.observe(memberGridElement);
    return () => observer.disconnect();
  }, [memberPage, target?.code, capital?.constituents?.length, detail?.constituents?.length]);

  const memberGridLayout = useMemo(() => {
    const gap = 6;
    const width = memberGridSize.width || 900;
    const height = memberGridSize.height || 420;
    // Bootstrap with the previous grid geometry until the first real card has
    // been measured. Once it is available, capacity is derived exclusively
    // from the measured card and the available slot size.
    const hasMeasuredCard = memberCardSize.width > 0 && memberCardSize.height > 0;
    const cardWidth = memberCardSize.width || (width - gap * 3) / 4;
    const cardHeight = memberCardSize.height || (height - gap) / 2;
    const columns = hasMeasuredCard
      ? Math.max(1, Math.floor((width + gap) / (cardWidth + gap)))
      : 4;
    const rows = hasMeasuredCard
      ? Math.max(1, Math.floor((height + gap) / (cardHeight + gap)))
      : 2;
    return { columns, rows, pageSize: columns * rows };
  }, [memberCardSize.height, memberCardSize.width, memberGridSize.height, memberGridSize.width]);

  const pageSize = memberGridLayout.pageSize;

  useEffect(() => {
    setMemberPage(0);
  }, [target?.code]);

  const members = useMemo(() => {
    const result = new Map<
      string,
      {
        code: string;
        name: string;
        closePrice: number | null;
        changePct: number | null;
        openPrice: number | null;
        highPrice: number | null;
        lowPrice: number | null;
        preClosePrice: number | null;
        volume: number | null;
        amount: number | null;
        turnoverPct: number | null;
        volRatio: number | null;
        eps: number | null;
        netAssets: number | null;
        pe: number | null;
        mainNetAmount: number | null;
        sources: string[];
      }
    >();
    for (const item of capital?.constituents ?? []) {
      const code = normalizeReplayStockCode(item.code);
      if (!code) continue;
      const quoteDate = item.quote_trade_date ?? item.ths_snapshot_date ?? item.tdx_snapshot_date;
      const isAsOfTradeDate = !quoteDate || quoteDate <= tradeDate;
      result.set(code, {
        code,
      name: item.name?.trim() || code,
        closePrice: isAsOfTradeDate ? finiteNumber(item.price) : null,
        changePct: isAsOfTradeDate ? finiteNumber(item.change_pct) : null,
        openPrice: isAsOfTradeDate ? finiteNumber(item.open) : null,
        highPrice: isAsOfTradeDate ? finiteNumber(item.high) : null,
        lowPrice: isAsOfTradeDate ? finiteNumber(item.low) : null,
        preClosePrice: isAsOfTradeDate ? finiteNumber(item.pre_close) : null,
        volume: isAsOfTradeDate ? finiteNumber(item.volume) : null,
        amount: isAsOfTradeDate ? finiteNumber(item.amount) : null,
        turnoverPct: isAsOfTradeDate ? finiteNumber(item.turnover_pct) : null,
        volRatio: isAsOfTradeDate ? finiteNumber(item.vol_ratio) : null,
        eps: isAsOfTradeDate ? finiteNumber(item.eps) : null,
        netAssets: isAsOfTradeDate ? finiteNumber(item.net_assets) : null,
        pe: isAsOfTradeDate ? finiteNumber(item.pe_ttm ?? item.pe_dynamic ?? item.pe_static) : null,
        mainNetAmount: isAsOfTradeDate ? finiteNumber(item.main_net_amount) : null,
        sources: (item.sources ?? []).filter(Boolean),
      });
    }
    for (const item of detail?.constituents ?? []) {
      const code = normalizeReplayStockCode(item.code);
      if (!code) continue;
      const existing = result.get(code);
      result.set(code, {
        code,
        name: item.name?.trim() || existing?.name || code,
        closePrice: existing?.closePrice ?? finiteNumber(item.lastPrice),
        changePct: existing?.changePct ?? finiteNumber(item.changePct),
        openPrice: existing?.openPrice ?? null,
        highPrice: existing?.highPrice ?? null,
        lowPrice: existing?.lowPrice ?? null,
        preClosePrice: existing?.preClosePrice ?? null,
        volume: existing?.volume ?? null,
        amount: existing?.amount ?? null,
        turnoverPct: existing?.turnoverPct ?? null,
        volRatio: existing?.volRatio ?? null,
        eps: existing?.eps ?? null,
        netAssets: existing?.netAssets ?? null,
        pe: existing?.pe ?? null,
        mainNetAmount: existing?.mainNetAmount ?? null,
        sources: [...new Set([...(existing?.sources ?? []), ...(item.source ? [item.source] : [])])],
      });
    }
    return [...result.values()];
  }, [capital?.constituents, detail?.constituents, tradeDate]);

  const tdxMemberCount = useMemo(
    () => members.filter((member) => member.sources.some((source) => source.toLowerCase() === "tdx")).length,
    [members],
  );

  const memberCodeSet = useMemo(() => new Set(members.map((item) => item.code)), [members]);
  const visibleIndustryEvents = useMemo(
    () =>
      (timeline?.events ?? [])
        .filter((event) => {
          const code = normalizeReplayStockCode(event.code);
          const timestamp = replayEventTimestamp(event);
          return Boolean(code && memberCodeSet.has(code) && timestamp && timestamp <= currentTimestamp);
        })
        .sort((left, right) => replayEventTimestamp(left) - replayEventTimestamp(right)),
    [currentTimestamp, memberCodeSet, timeline?.events],
  );

  const latestEventByCode = useMemo(() => {
    const result = new Map<string, MarketIndicatorEvent>();
    for (const event of visibleIndustryEvents) {
      const code = normalizeReplayStockCode(event.code);
      if (code) result.set(code, event);
    }
    return result;
  }, [visibleIndustryEvents]);

  const firstLimitEventByCode = useMemo(() => {
    const result = new Map<string, MarketIndicatorEvent>();
    for (const event of visibleIndustryEvents) {
      const code = normalizeReplayStockCode(event.code);
      if (!code || result.has(code) || event.eventType !== "limit_up_sealed") continue;
      result.set(code, event);
    }
    return result;
  }, [visibleIndustryEvents]);

  const mappedIndustryCodes = useMemo(
    () =>
      new Set(
        [
          detail?.industryCode,
          target?.code,
          ...(capital?.ths_mappings ?? []).map((item) => item.ths_code),
        ]
          .map((item) => String(item ?? "").trim())
          .filter(Boolean),
      ),
    [capital?.ths_mappings, detail?.industryCode, target?.code],
  );
  const mappedIndustryNames = useMemo(
    () =>
      new Set(
        [detail?.industryName, target?.name, industryName, capital?.tdx_name]
          .map(normalizeReplayIndustryName)
          .filter(Boolean),
      ),
    [capital?.tdx_name, detail?.industryName, industryName, target?.name],
  );

  const activeIndustryPoint = useMemo(() => {
    const candidates = (timeline?.industryPoints ?? []).filter((point) => {
      const timestamp = point.capturedAt ? epoch(point.capturedAt) : 0;
      if (!timestamp || timestamp > currentTimestamp) return false;
      const code = String(point.industryCode ?? "").trim();
      const name = normalizeReplayIndustryName(point.industryName);
      return mappedIndustryCodes.has(code) || mappedIndustryNames.has(name);
    });
    return candidates.sort(
      (left, right) => epoch(left.capturedAt ?? "") - epoch(right.capturedAt ?? ""),
    ).at(-1) ?? null;
  }, [currentTimestamp, mappedIndustryCodes, mappedIndustryNames, timeline?.industryPoints]);

  const orderedMembers = useMemo(
    () =>
      members
        .map((member) => ({
          ...member,
          event: latestEventByCode.get(member.code) ?? null,
          firstLimitEvent: firstLimitEventByCode.get(member.code) ?? null,
        }))
        .sort((left, right) => {
          const leftLimitPriority = industryLimitSortPriority(
            left.event?.eventType,
            left.changePct,
          );
          const rightLimitPriority = industryLimitSortPriority(
            right.event?.eventType,
            right.changePct,
          );
          if (leftLimitPriority !== rightLimitPriority) {
            return leftLimitPriority - rightLimitPriority;
          }
          if (left.changePct != null || right.changePct != null) {
            if (left.changePct == null) return 1;
            if (right.changePct == null) return -1;
            if (left.changePct !== right.changePct) return right.changePct - left.changePct;
          }
          return left.code.localeCompare(right.code);
        }),
    [firstLimitEventByCode, latestEventByCode, members],
  );

  const snapshotLimitMembers = useMemo(() => {
    if (target?.kind !== "concept") return { up: null, down: null };
    // board-members is an end-of-day snapshot. Do not expose it while the
    // replay cursor is still before the close, otherwise the panel would leak
    // information from after the current replay moment.
    const replayClose = stock.timeshare.at(-1)?.timestamp ?? epoch(`${tradeDate}T15:00:00+08:00`);
    if (!canUseReplaySnapshot(tradeDate, currentTimestamp, replayClose)) {
      return { up: null, down: null };
    }
    const limitMembers = members
      .map((member) => ({ member, kind: snapshotLimitKind(member.code, member.changePct) }))
      .filter((item): item is { member: (typeof members)[number]; kind: "up" | "down" } => Boolean(item.kind));
    return {
      up: limitMembers
        .filter((item) => item.kind === "up")
        .sort((left, right) => (right.member.changePct ?? -Infinity) - (left.member.changePct ?? -Infinity))[0]?.member ?? null,
      down: limitMembers
        .filter((item) => item.kind === "down")
        .sort((left, right) => (left.member.changePct ?? Infinity) - (right.member.changePct ?? Infinity))[0]?.member ?? null,
    };
  }, [currentTimestamp, members, stock.timeshare, target?.kind, tradeDate]);

  const pageCount = Math.max(1, Math.ceil(orderedMembers.length / pageSize));
  const safePage = Math.min(memberPage, pageCount - 1);
  const visibleMembers = orderedMembers.slice(safePage * pageSize, (safePage + 1) * pageSize);

  useEffect(() => {
    setMemberPage((current) => Math.min(current, pageCount - 1));
  }, [pageCount]);

  const firstLimitEvent = visibleIndustryEvents.find((event) => event.eventType === "limit_up_sealed") ?? null;
  const firstLimitCode = normalizeReplayStockCode(
    activeIndustryPoint?.firstLimitUpCode ?? firstLimitEvent?.code,
  );
  const firstLimitMember = members.find((item) => item.code === firstLimitCode) ?? null;
  const firstLimitSnapshotMember = snapshotLimitMembers.up;
  const resolvedFirstLimitCode = firstLimitCode ?? firstLimitSnapshotMember?.code ?? null;
  const firstLimitName =
    activeIndustryPoint?.firstLimitUpName ?? firstLimitEvent?.name ?? firstLimitMember?.name ?? firstLimitSnapshotMember?.name ?? null;
  const firstLimitAt = activeIndustryPoint?.firstLimitUpAt ?? firstLimitEvent?.eventAt ?? null;
  const firstLimitChangePct = firstLimitEvent?.changePct ?? firstLimitSnapshotMember?.changePct ?? null;
  const firstLimitDownEvent =
    visibleIndustryEvents.find((event) => event.eventType === "limit_down_sealed") ?? null;
  const firstLimitDownEventCode = normalizeReplayStockCode(firstLimitDownEvent?.code);
  const firstLimitDownSnapshotMember = snapshotLimitMembers.down;
  const firstLimitDownCode = firstLimitDownEventCode ?? firstLimitDownSnapshotMember?.code ?? null;
  const firstLimitDownMember = members.find((item) => item.code === firstLimitDownEventCode) ?? null;
  const firstLimitDownName = firstLimitDownEvent?.name ?? firstLimitDownMember?.name ?? firstLimitDownSnapshotMember?.name ?? null;
  const firstLimitDownAt = firstLimitDownEvent?.eventAt ?? null;
  const firstLimitDownChangePct = firstLimitDownEvent?.changePct ?? firstLimitDownSnapshotMember?.changePct ?? null;
  const recentIndustryEvents = visibleIndustryEvents.slice().reverse().slice(0, 4);

  const stockLast = replayVisiblePrice(stock, currentTimestamp);
  const industryLast = replayVisiblePrice(industry, currentTimestamp);
  const indexLast = replayVisiblePrice(index, currentTimestamp);
  const latestReplayDate = stock.trade_date ?? stock.requested_trade_date;
  const canUseCurrentBoardSnapshot =
    tradeDate === localDate(new Date().toISOString()) || latestReplayDate === tradeDate;
  const boardChangePct = canUseCurrentBoardSnapshot && boardQuote?.pre_close
    ? ((boardQuote.close - boardQuote.pre_close) / boardQuote.pre_close) * 100
    : null;

  function openStock(code: string | null | undefined, name: string | null | undefined) {
    const normalizedCode = normalizeReplayStockCode(code);
    if (!normalizedCode) return;
    setSelectedStock({ code: normalizedCode, name: name?.trim() || normalizedCode });
    setStockDialogOpen(true);
  }

  if (!target) {
    return (
      <div className="flex h-full items-center justify-center px-4 text-center text-xs text-muted-foreground">
        当前标的尚未解析到 Industry Application 行业，请先在右侧行业页选择关联目标。
      </div>
    );
  }

  if (loading && !members.length) {
    return (
      <div className="grid h-full grid-cols-[240px_minmax(0,1fr)] divide-x divide-border/50 p-3">
        <div className="space-y-2 pr-3">
          <div className="h-4 w-28 animate-pulse rounded bg-muted" />
          <div className="h-14 animate-pulse rounded-md bg-muted/70" />
          <div className="grid grid-cols-2 gap-2">
            <div className="h-10 animate-pulse rounded-md bg-muted/60" />
            <div className="h-10 animate-pulse rounded-md bg-muted/60" />
          </div>
        </div>
        <div
          ref={(element) => {
            memberGridElementRef.current = element;
          }}
          className="grid min-h-0 flex-1 gap-2 pl-3"
          style={{
            gridTemplateColumns: `repeat(${memberGridLayout.columns}, minmax(0, 1fr))`,
            gridTemplateRows: `repeat(${memberGridLayout.rows}, minmax(0, 1fr))`,
          }}
        >
          {Array.from({ length: pageSize }, (_, index) => (
            <div key={index} className="animate-pulse rounded-md bg-muted/60" />
          ))}
        </div>
      </div>
    );
  }

  if (!members.length) {
    return (
      <div className="flex h-full items-center justify-center px-4 text-center text-xs text-muted-foreground">
        {error ?? "当前行业暂未返回可用于复盘的成分股快照；资金流不可用不会影响成分股展示。"}
      </div>
    );
  }

  return (
    <>
      <div className="grid h-full min-h-0 grid-cols-[minmax(225px,0.28fr)_minmax(0,1fr)] divide-x divide-border/50">
        <section className="flex min-h-0 flex-col gap-2.5 p-3">
          <div className="flex min-w-0 items-start justify-between gap-2">
            <div className="min-w-0">
              <div className="truncate text-xs font-semibold">{target.name}</div>
              <div className="mt-0.5 font-mono text-[10px] text-muted-foreground">
                {detail?.industryCode ?? target.code} · {members.length} 只标的
              </div>
            </div>
            <span className="shrink-0 rounded bg-muted/60 px-1.5 py-0.5 font-mono text-[9px] text-muted-foreground">
              {formatTime(currentTimestamp)}
            </span>
          </div>

          <div className="grid grid-cols-2 gap-1.5">
            <button
              type="button"
              className="min-w-0 rounded-md bg-background px-2.5 py-2 text-left transition-colors hover:bg-muted/60 disabled:cursor-default disabled:hover:bg-background"
              disabled={!resolvedFirstLimitCode}
              onClick={() => openStock(resolvedFirstLimitCode, firstLimitName)}
            >
              <div className="flex items-center justify-between gap-2 text-[10px] text-muted-foreground">
                <span className="flex items-center gap-1"><Clock3 className="size-3" />最先涨停</span>
                <span className="font-mono">
                  {firstLimitAt ? formatTime(epoch(firstLimitAt)) : firstLimitSnapshotMember ? "收盘快照" : "-"}
                </span>
              </div>
              <div className="mt-1 flex min-w-0 items-center justify-between gap-2">
                <span className="min-w-0 truncate text-[11px] font-medium text-red-600">
                  {firstLimitName ?? "暂无"}
                  {firstLimitChangePct != null ? ` ${firstLimitChangePct > 0 ? "+" : ""}${firstLimitChangePct.toFixed(2)}%` : ""}
                </span>
                <span className="shrink-0 font-mono text-[9px] text-muted-foreground">{resolvedFirstLimitCode ?? "-"}</span>
              </div>
            </button>
            <button
              type="button"
              className="min-w-0 rounded-md bg-background px-2.5 py-2 text-left transition-colors hover:bg-muted/60 disabled:cursor-default disabled:hover:bg-background"
              disabled={!firstLimitDownCode}
              onClick={() => openStock(firstLimitDownCode, firstLimitDownName)}
            >
              <div className="flex items-center justify-between gap-2 text-[10px] text-muted-foreground">
                <span className="flex items-center gap-1"><Clock3 className="size-3" />最先跌停</span>
                <span className="font-mono">
                  {firstLimitDownAt ? formatTime(epoch(firstLimitDownAt)) : firstLimitDownSnapshotMember ? "收盘快照" : "-"}
                </span>
              </div>
              <div className="mt-1 flex min-w-0 items-center justify-between gap-2">
                <span className="min-w-0 truncate text-[11px] font-medium text-emerald-600">
                  {firstLimitDownName ?? "暂无"}
                  {firstLimitDownChangePct != null ? ` ${firstLimitDownChangePct.toFixed(2)}%` : ""}
                </span>
                <span className="shrink-0 font-mono text-[9px] text-muted-foreground">{firstLimitDownCode ?? "-"}</span>
              </div>
            </button>
          </div>

          {target.kind === "concept" ? (
            <div className="grid grid-cols-2 gap-1.5">
              <IndustryReplayMetric
                label="板块涨跌"
                value={boardChangePct == null ? "-" : `${boardChangePct > 0 ? "+" : ""}${boardChangePct.toFixed(2)}%`}
                tone={(boardChangePct ?? 0) < 0 ? "down" : "up"}
              />
              <IndustryReplayMetric label="成分股" value={`${boardSummary?.member_count ?? members.length} 只`} />
              <IndustryReplayMetric
                label="上涨 / 下跌"
                value={`${boardSummary?.up_count ?? "-"} / ${boardSummary?.down_count ?? "-"}`}
                tone="up"
              />
              <IndustryReplayMetric label="成交额" value={displayVolume(boardSummary?.amount)} />
              <IndustryReplayMetric label="成交量" value={displayVolume(boardSummary?.vol)} />
              <IndustryReplayMetric
                label="主力净流入"
                value={displayVolume(boardSummary?.main_net_amount)}
                tone={(boardSummary?.main_net_amount ?? 0) < 0 ? "down" : "up"}
              />
              <IndustryReplayMetric
                label="3 日主力净流入"
                value={displayVolume(boardSummary?.main_net_3d)}
                tone={(boardSummary?.main_net_3d ?? 0) < 0 ? "down" : "up"}
              />
              <IndustryReplayMetric
                label="5 日主力净流入"
                value={displayVolume(boardSummary?.main_net_5d)}
                tone={(boardSummary?.main_net_5d ?? 0) < 0 ? "down" : "up"}
              />
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-1.5">
              <IndustryReplayMetric
                label="涨停 / 跌停"
                value={`${activeIndustryPoint?.limitUpCount ?? 0} / ${activeIndustryPoint?.limitDownCount ?? 0}`}
                tone="up"
              />
              <IndustryReplayMetric
                label="触板 / 触底"
                value={`${activeIndustryPoint?.limitUpTouchCount ?? 0} / ${activeIndustryPoint?.limitDownTouchCount ?? 0}`}
              />
              <IndustryReplayMetric
                label="上涨 / 下跌"
                value={`${activeIndustryPoint?.advancingCount ?? 0} / ${activeIndustryPoint?.decliningCount ?? 0}`}
              />
              <IndustryReplayMetric
                label="快速拉升 / 打压"
                value={`${activeIndustryPoint?.rapidRiseCount ?? 0} / ${activeIndustryPoint?.rapidFallCount ?? 0}`}
              />
              <IndustryReplayMetric
                label="标的 / 行业 / 上证"
                value={`${displayNumber(stockLast)} / ${displayNumber(industryLast)} / ${displayNumber(indexLast)}`}
              />
              <IndustryReplayMetric
                label="日级主力净流入"
                value={displayVolume(capital?.main_net)}
                tone={(capital?.main_net ?? 0) < 0 ? "down" : "up"}
              />
            </div>
          )}

          <div className="mt-0.5 min-h-0 flex-1 border-t border-border/50 pt-2">
            <div className="flex items-center justify-between gap-2 text-[10px]">
              <span className="font-medium">最近行业信号</span>
              <span className="font-mono text-muted-foreground">{visibleIndustryEvents.length} 个</span>
            </div>
            <div className="mt-1 min-h-0 space-y-1 overflow-y-auto pr-0.5">
              {recentIndustryEvents.length ? recentIndustryEvents.map((event) => {
                const code = normalizeReplayStockCode(event.code);
                return (
                  <button
                    key={`${event.id ?? code}-${event.eventAt}`}
                    type="button"
                    className="flex w-full min-w-0 items-center gap-1.5 rounded bg-background px-2 py-1 text-left hover:bg-muted/60"
                    onClick={() => openStock(code, event.name)}
                  >
                    <span className={`shrink-0 rounded px-1 py-0.5 text-[8px] ${industryEventBadgeClass(event.eventType)}`}>
                      {industryEventLabel(event.eventType)}
                    </span>
                    <span className="min-w-0 flex-1 truncate text-[10px]">{event.name ?? code ?? "-"}</span>
                    <span className="shrink-0 font-mono text-[9px] text-muted-foreground">{formatTime(replayEventTimestamp(event))}</span>
                    <span className="shrink-0 font-mono text-[9px]">{event.changePct == null ? "-" : `${event.changePct > 0 ? "+" : ""}${event.changePct.toFixed(2)}%`}</span>
                  </button>
                );
              }) : (
                <div className="px-2 py-1 text-[10px] text-muted-foreground">暂无信号</div>
              )}
            </div>
          </div>
        </section>

        <section className="flex min-h-0 min-w-0 flex-col p-2.5">
          <div className="mb-2 flex h-6 shrink-0 items-center justify-between gap-2 px-0.5">
            <div className="min-w-0">
              <span className="text-[11px] font-medium">行业标的</span>
              <span className="ml-2 text-[10px] text-muted-foreground">
                已触发事件优先，状态只显示到当前游标
                {tdxMemberCount ? ` · TDX 成分 ${tdxMemberCount}` : ""}
              </span>
            </div>
            <div className="flex shrink-0 items-center gap-1">
              <span className="font-mono text-[10px] text-muted-foreground">{safePage + 1}/{pageCount}</span>
              <Button
                type="button"
                variant="ghost"
                size="icon-sm"
                className="size-6"
                disabled={safePage <= 0}
                onClick={() => setMemberPage((value) => Math.max(0, value - 1))}
                aria-label="上一页行业标的"
              >
                <ChevronLeft />
              </Button>
              <Button
                type="button"
                variant="ghost"
                size="icon-sm"
                className="size-6"
                disabled={safePage >= pageCount - 1}
                onClick={() => setMemberPage((value) => Math.min(pageCount - 1, value + 1))}
                aria-label="下一页行业标的"
              >
                <ChevronRight />
              </Button>
            </div>
          </div>
          <div
          ref={(element) => {
            memberGridElementRef.current = element;
          }}
            className="grid min-h-0 flex-1 gap-1.5"
            style={{
              gridTemplateColumns: `repeat(${memberGridLayout.columns}, minmax(0, 1fr))`,
              gridTemplateRows: `repeat(${Math.max(1, Math.ceil(visibleMembers.length / memberGridLayout.columns))}, minmax(0, 1fr))`,
            }}
          >
            {visibleMembers.map((member, index) => {
              const event = member.event;
              const eventType = event?.eventType ?? null;
              const changeTone =
                member.changePct == null
                  ? "text-muted-foreground"
                  : member.changePct > 0
                    ? "text-red-600"
                    : member.changePct < 0
                      ? "text-emerald-600"
                      : "text-foreground";
              return (
                <button
                  key={member.code}
                  ref={
                    index === 0
                      ? (element) => {
                          memberCardElementRef.current = element;
                        }
                      : undefined
                  }
                  type="button"
                  className="group flex min-h-0 min-w-0 flex-col justify-between rounded-md bg-background px-2.5 py-2 text-left transition-colors hover:bg-muted/65 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-inset"
                  onClick={() => openStock(member.code, member.name)}
                  title={`打开 ${member.name} 个股详情`}
                >
                  <div className="min-w-0">
                    <div className="flex min-w-0 items-center gap-1.5">
                      <div className="min-w-0 flex-1 truncate">
                        <span className="text-[11px] font-medium">{member.name}</span>
                        <span className="ml-1.5 font-mono text-[9px] text-muted-foreground">{member.code}</span>
                      </div>
                      <span className={`shrink-0 font-mono text-sm font-semibold tabular-nums ${changeTone}`}>
                        {member.changePct == null
                          ? "-"
                          : `${member.changePct > 0 ? "+" : ""}${member.changePct.toFixed(2)}%`}
                      </span>
                    </div>
                    <div className="mt-0.5 flex min-w-0 items-center gap-1">
                      {member.sources.some((source) => source.toLowerCase() === "tdx") ? (
                        <span className="rounded bg-amber-100 px-1 py-0.5 text-[8px] text-amber-700">TDX</span>
                      ) : null}
                      <span className={`rounded px-1.5 py-0.5 text-[8px] ${industryEventBadgeClass(eventType)}`}>
                        {industryEventLabel(eventType)}
                      </span>
                    </div>
                  </div>
                  <IndustryMemberMiniChart
                    code={member.code}
                    name={member.name}
                    tradeDate={tradeDate || stock.trade_date || stock.requested_trade_date || ""}
                    currentTimestamp={currentTimestamp}
                  />
                  {target.kind === "concept" ? (
                    <div className="mt-1 grid grid-cols-2 gap-x-2 gap-y-0.5 text-[9px] text-muted-foreground">
                      <span className="col-span-2 flex items-baseline justify-between gap-2 border-b border-border/40 pb-0.5">
                        <span>成交额</span>
                        <span className="font-mono text-xs font-semibold tabular-nums text-foreground">{displayVolume(member.amount)}</span>
                      </span>
                      <span>量 {displayVolume(member.volume)}</span>
                      <span>换手 {displayNumber(member.turnoverPct)}%</span>
                      <span>量比 {displayNumber(member.volRatio)}</span>
                      <span>主力 {displayVolume(member.mainNetAmount)}</span>
                      <span>PE {displayNumber(member.pe)}</span>
                      <span>EPS {displayNumber(member.eps)}</span>
                      <span>净资 {displayNumber(member.netAssets)}</span>
                      <span className="col-span-2 truncate">
                        开 {displayNumber(member.openPrice)} · 高 {displayNumber(member.highPrice)} · 低 {displayNumber(member.lowPrice)}
                      </span>
                    </div>
                  ) : null}
                  <div className="mt-1 min-w-0">
                    <div className="flex items-end justify-between gap-2">
                      <span className="truncate text-[9px] text-muted-foreground">
                        日内 {displayNumber(member.lowPrice)} — {displayNumber(member.highPrice)}
                      </span>
                      <span className="shrink-0 font-mono text-[9px] text-muted-foreground">
                        {member.closePrice == null ? "收盘价 -" : `收盘 ${displayNumber(member.closePrice)}`}
                      </span>
                    </div>
                  </div>
                </button>
              );
            })}
          </div>
        </section>
      </div>
      <StockDetailDialog
        open={stockDialogOpen}
        onOpenChange={setStockDialogOpen}
        stockCode={selectedStock?.code ?? null}
        stockName={selectedStock?.name ?? null}
        industryName={target.name}
      />
    </>
  );
}

function IndustryReplayMetric({
  label,
  value,
  tone = "neutral",
}: {
  label: string;
  value: string;
  tone?: "up" | "down" | "neutral";
}) {
  return (
    <div className="min-w-0 rounded-md bg-background px-2 py-1.5">
      <div className="truncate text-[9px] text-muted-foreground">{label}</div>
      <div className={`mt-0.5 truncate font-mono text-[10px] font-medium ${tone === "up" ? "text-red-600" : tone === "down" ? "text-emerald-600" : "text-foreground"}`}>
        {value}
      </div>
    </div>
  );
}

function IndustryMemberMiniChart({
  code,
  name,
  tradeDate,
  currentTimestamp,
}: {
  code: string;
  name: string;
  tradeDate: string;
  currentTimestamp: number;
}) {
  const [points, setPoints] = useState<StockIntradayPoint[]>([]);
  const [status, setStatus] = useState<"loading" | "ready" | "empty">("loading");

  useEffect(() => {
    if (!code || !tradeDate) {
      setPoints([]);
      setStatus("empty");
      return;
    }
    let cancelled = false;
    setPoints([]);
    setStatus("loading");
    void (async () => {
      // Industry cards only need the native TDX timeshare. Avoid requesting
      // the heavier stock-chart aggregate endpoint for every card; fall back
      // to the stored chart data only when TDX has no snapshot.
      const tdx = await fetchEasyTdxTimeshare({ code, tradeDate }).catch(() => null);
      let nextPoints = easyTdxTimesharePoints(tdx?.items ?? [], tradeDate);
      if (!nextPoints.length) {
        nextPoints = await loadTradeReviewIntraday({
          targetType: "stock",
          symbol: code,
          name,
          tradeDate,
          periods: ["1m"],
        }).then((result) => result?.timeshare ?? []).catch(() => []);
      }
      if (cancelled) return;
      setPoints(nextPoints);
      setStatus(nextPoints.length ? "ready" : "empty");
    })();
    return () => {
      cancelled = true;
    };
  }, [code, name, tradeDate]);

  const width = 220;
  const height = 46;
  const padding = { top: 3, right: 1, bottom: 2, left: 1 };
  const chartWidth = width - padding.left - padding.right;
  const chartHeight = height - padding.top - padding.bottom;
  const visible = currentTimestamp
    ? points.filter((point) => point.timestamp <= currentTimestamp)
    : points;

  if (status === "loading") {
    return <div className="mt-1.5 h-[46px] animate-pulse rounded bg-muted/55" aria-label="正在加载分时" />;
  }
  if (!points.length) {
    return (
      <div className="mt-1.5 flex h-[46px] flex-col items-center justify-center rounded bg-muted/25 text-[8px] text-muted-foreground">
        <span>暂无可用分时</span>
        <span className="mt-0.5 text-[7px] text-muted-foreground/70">TDX / 已落库均无</span>
      </div>
    );
  }

  const allValues = points.flatMap((point) => [point.price, point.avg_price ?? point.price]);
  const min = Math.min(...allValues);
  const max = Math.max(...allValues);
  const span = Math.max(max - min, Math.abs(max) * 0.002, 0.01);
  const domainMin = min - span * 0.08;
  const domainMax = max + span * 0.08;
  const volumeMax = Math.max(...points.map((point) => point.volume), 1);
  const baselinePrice = points[0]?.price ?? 0;
  const x = (index: number) =>
    padding.left + (points.length <= 1 ? chartWidth / 2 : (index / (points.length - 1)) * chartWidth);
  const y = (value: number) =>
    padding.top + ((domainMax - value) / (domainMax - domainMin)) * (chartHeight * 0.72);
  const volumeY = (value: number) => padding.top + chartHeight - Math.min(1, value / volumeMax) * chartHeight * 0.22;
  const visibleIndex = new Set(visible.map((point) => point.timestamp));
  const visibleIndexes = visible.map((point) => points.findIndex((item) => item.timestamp === point.timestamp));
  const averagePath = visible
    .map((point) => {
      const index = points.findIndex((item) => item.timestamp === point.timestamp);
      return `${index === 0 ? "M" : "L"}${x(index).toFixed(2)},${y(point.avg_price ?? point.price).toFixed(2)}`;
    })
    .join(" ");
  const lastVisibleIndex = points.findIndex(
    (point) => point.timestamp === visible[visible.length - 1]?.timestamp,
  );

  return (
    <div className="mt-1.5 min-w-0" title="TDX 1 分钟分时（按当前回放时间截断）">
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="h-[46px] w-full overflow-visible"
        preserveAspectRatio="none"
        role="img"
        aria-label="标的当日分时小图"
      >
        <line x1={padding.left} x2={width - padding.right} y1={height - 1} y2={height - 1} stroke="currentColor" strokeOpacity="0.08" />
        {points.map((point, index) => {
           if (!visibleIndex.has(point.timestamp)) return null;
           const barWidth = Math.max(1, chartWidth / Math.max(points.length, 1) - 0.5);
           const previousPrice = points[index - 1]?.price ?? baselinePrice;
           const directionColor = point.price >= previousPrice ? "#dc2626" : "#16a34a";
           return (
             <rect
              key={`${point.timestamp}-volume`}
              x={x(index) - barWidth / 2}
              y={volumeY(point.volume)}
              width={barWidth}
              height={Math.max(1, height - 2 - volumeY(point.volume))}
               fill={directionColor}
               fillOpacity="0.72"
             />
           );
         })}
        <path d={averagePath} fill="none" stroke="#d97706" strokeDasharray="2 2" strokeWidth="1" vectorEffect="non-scaling-stroke" />
        {visibleIndexes.slice(0, -1).map((index, segmentIndex) => {
          const nextIndex = visibleIndexes[segmentIndex + 1];
          if (index < 0 || nextIndex == null || nextIndex < 0) return null;
          const nextPoint = points[nextIndex];
          return (
            <line
              key={`${points[index]?.timestamp ?? index}-price`}
              x1={x(index)}
              y1={y(points[index].price)}
              x2={x(nextIndex)}
              y2={y(nextPoint.price)}
              stroke={nextPoint.price >= baselinePrice ? "#dc2626" : "#16a34a"}
              strokeLinecap="round"
              strokeWidth="1.35"
              vectorEffect="non-scaling-stroke"
            />
          );
        })}
        {visibleIndexes.length === 1 ? (
          <circle
            cx={x(visibleIndexes[0])}
            cy={y(points[visibleIndexes[0]].price)}
            r="1.5"
            fill={points[visibleIndexes[0]].price >= baselinePrice ? "#dc2626" : "#16a34a"}
          />
        ) : null}
        {lastVisibleIndex >= 0 ? (
          <line
            x1={x(lastVisibleIndex)}
            x2={x(lastVisibleIndex)}
            y1={padding.top}
            y2={height - 1}
            stroke="#2563eb"
            strokeOpacity="0.28"
            strokeWidth="1"
            vectorEffect="non-scaling-stroke"
          />
        ) : null}
      </svg>
      <div className="flex items-center justify-between text-[8px] leading-3 text-muted-foreground">
        <span className="flex items-center gap-1"><span className="size-1 rounded-full bg-red-600" /><span className="size-1 rounded-full bg-emerald-600" />价格</span>
        <span className="flex items-center gap-1"><span className="size-1 rounded-full bg-amber-600" />均价</span>
        <span className="font-mono">TDX 1m</span>
      </div>
    </div>
  );
}

function normalizeReplayStockCode(value: string | null | undefined) {
  return String(value ?? "").trim().replace(/^(sh|sz)/i, "").slice(-6);
}

function normalizeReplayIndustryName(value: string | null | undefined) {
  return String(value ?? "").trim().replace(/行业$/u, "");
}

function replayEventTimestamp(event: MarketIndicatorEvent | null | undefined) {
  return event?.eventAt ? epoch(event.eventAt) : 0;
}

function replayVisiblePrice(data: StockIntradayResponse | null, currentTimestamp: number) {
  if (!data || !currentTimestamp) return null;
  return data.timeshare
    .filter((item) => item.timestamp <= currentTimestamp)
    .at(-1)?.price ?? null;
}

function industryEventLabel(eventType: string | null) {
  return {
    limit_up_touch: "触板",
    limit_up_sealed: "封板",
    limit_up_opened: "开板",
    limit_down_touch: "触底",
    limit_down_sealed: "跌停",
    limit_down_opened: "开板",
    rapid_rise: "拉升",
    rapid_fall: "打压",
  }[eventType ?? ""] ?? "等待";
}

function industryEventBadgeClass(eventType: string | null) {
  if (eventType?.startsWith("limit_up")) return "bg-red-500/10 text-red-600";
  if (eventType?.startsWith("limit_down")) return "bg-emerald-500/10 text-emerald-600";
  if (eventType === "rapid_rise") return "bg-amber-500/12 text-amber-700";
  if (eventType === "rapid_fall") return "bg-emerald-500/10 text-emerald-600";
  return "bg-muted text-muted-foreground";
}

function industryLimitSortPriority(
  eventType: string | null | undefined,
  changePct: number | null | undefined,
) {
  switch (eventType) {
    case "limit_up_sealed":
      return 0;
    case "limit_down_sealed":
      return 1;
    case "limit_up_touch":
      return 2;
    case "limit_down_touch":
      return 3;
    case "limit_up_opened":
      return 4;
    case "limit_down_opened":
      return 5;
  }

  // Indicator coverage can miss an individual limit event. Use the closing
  // snapshot as a conservative fallback so 10% and 20% limit-up stocks share
  // the same priority group (and likewise for limit-down stocks).
  if (!eventType && changePct != null) {
    if (isNearLimit(changePct, 10) || isNearLimit(changePct, 20)) return 0;
    if (isNearLimit(changePct, -10) || isNearLimit(changePct, -20)) return 1;
  }
  return 6;
}

function isNearLimit(changePct: number, limitPct: number) {
  return Math.abs(changePct - limitPct) <= 0.75;
}

function inferPriceLimitPct(code: string) {
  const normalized = normalizeReplayStockCode(code);
  // Use the listing board as the source of truth. ST is not given a separate
  // 5% fallback here; its limit follows the board it is listed on.
  if (/^(300|301|688|689)/u.test(normalized)) return 20;
  if (/^(4|8|92)/u.test(normalized)) return 30;
  return 10;
}

function snapshotLimitKind(
  code: string,
  changePct: number | null,
): "up" | "down" | null {
  if (changePct == null) return null;
  const limit = inferPriceLimitPct(code);
  if (isNearLimit(changePct, limit)) return "up";
  if (isNearLimit(changePct, -limit)) return "down";
  return null;
}

function canUseReplaySnapshot(tradeDate: string, currentTimestamp: number, replayClose: number) {
  return Boolean(tradeDate && currentTimestamp && replayClose && currentTimestamp >= replayClose);
}

function TicksReplayLower({
  rows,
  currentTimestamp,
  loading,
  onSelect,
}: {
  rows: HistoricalTradeTick[];
  currentTimestamp: number;
  loading: boolean;
  onSelect: (timestamp: number) => void;
}) {
  const allRows = rows
    .slice()
    .sort((a, b) => tickTimestamp(a) - tickTimestamp(b));
  const visibleRows = allRows.filter((row) => tickTimestamp(row) <= currentTimestamp);
  const groups = Array.from(
    allRows.reduce(
      (map, row) => {
        const label = row.time_label ?? row.trade_time ?? row.time ?? "未知";
        const price = Number(row.price);
        const volume = Number(row.volume) || 0;
        const amount = Number(row.amount) || (Number.isFinite(price) ? price * volume * 100 : 0);
        const side = normalizeTickSide(row);
        const group = map.get(label) ?? {
          label,
          rows: [] as HistoricalTradeTick[],
          count: 0,
          volume: 0,
          amount: 0,
          priceVolume: 0,
          buyVolume: 0,
          sellVolume: 0,
          neutralVolume: 0,
          high: Number.NEGATIVE_INFINITY,
          low: Number.POSITIVE_INFINITY,
        };
        group.rows.push(row);
        group.count += 1;
        group.volume += volume;
        group.amount += amount;
        if (Number.isFinite(price)) {
          group.priceVolume += price * volume;
          group.high = Math.max(group.high, price);
          group.low = Math.min(group.low, price);
        }
        if (side === "BUY") group.buyVolume += volume;
        else if (side === "SELL") group.sellVolume += volume;
        else group.neutralVolume += volume;
        map.set(label, group);
        return map;
      },
      new Map<
        string,
        {
          label: string;
          rows: HistoricalTradeTick[];
          count: number;
          volume: number;
          amount: number;
          priceVolume: number;
          buyVolume: number;
          sellVolume: number;
          neutralVolume: number;
          high: number;
          low: number;
        }
      >(),
    ).values(),
  );
  const visibleGroups = groups.filter(
    (group) => tickTimestamp(group.rows[0]) <= currentTimestamp,
  );
  const latest = visibleGroups.at(-1);
  // Keep the strength strip on the same time span as the replay chart above.
  // Limiting this to the last 18 groups made its left/right ticks drift to a
  // much narrower window than the intraday chart.
  const recent = groups;
  const totalVolume = visibleRows.reduce((sum, row) => sum + (Number(row.volume) || 0), 0);
  const totalAmount = visibleRows.reduce((sum, row) => {
    const price = Number(row.price);
    const volume = Number(row.volume) || 0;
    return sum + (Number(row.amount) || (Number.isFinite(price) ? price * volume * 100 : 0));
  }, 0);
  const totalPriceVolume = visibleRows.reduce((sum, row) => {
    const price = Number(row.price);
    return sum + (Number.isFinite(price) ? price * (Number(row.volume) || 0) : 0);
  }, 0);
  const totalBuyVolume = visibleRows.reduce(
    (sum, row) => sum + (normalizeTickSide(row) === "BUY" ? Number(row.volume) || 0 : 0),
    0,
  );
  const totalSellVolume = visibleRows.reduce(
    (sum, row) => sum + (normalizeTickSide(row) === "SELL" ? Number(row.volume) || 0 : 0),
    0,
  );
  const maxMinuteVolume = Math.max(...recent.map((group) => group.volume), 1);
  const vwap = totalVolume > 0 ? totalPriceVolume / totalVolume : null;
  const buyRatio = totalVolume > 0 ? (totalBuyVolume / totalVolume) * 100 : 0;
  const sellRatio = totalVolume > 0 ? (totalSellVolume / totalVolume) * 100 : 0;
  const latestNet = latest ? latest.buyVolume - latest.sellVolume : 0;
  const latestVwap = latest && latest.volume > 0 ? latest.priceVolume / latest.volume : null;

  if (loading && !rows.length) {
    return (
      <div className="flex h-full items-center justify-center px-3 text-xs text-muted-foreground">
        正在加载逐笔成交明细…
      </div>
    );
  }
  if (!visibleRows.length) {
    return (
      <div className="flex h-full items-center justify-center px-3 text-xs text-muted-foreground">
        {rows.length ? "当前回放时刻尚未出现逐笔成交" : "该交易日暂无历史逐笔成交明细"}
      </div>
    );
  }

  return (
    <div className="h-full min-h-0 overflow-hidden p-3">
      <div className="flex items-center justify-between gap-2 text-[11px]">
        <div className="min-w-0">
          <span className="font-medium">逐笔成交 · 分钟级明细</span>
          <span className="ml-2 text-muted-foreground">买卖方向 / 成交量 / 成交额</span>
        </div>
        <span className="shrink-0 font-mono text-muted-foreground">
          {visibleRows.length}/{rows.length} 笔
        </span>
      </div>

      <div className="mt-2 grid grid-cols-5 gap-1.5">
        <LowerMetric label="成交笔数" value={visibleRows.length.toLocaleString("zh-CN")} />
        <LowerMetric label="成交量" value={displayVolume(totalVolume)} />
        <LowerMetric label="成交额" value={displayVolume(totalAmount)} />
        <LowerMetric label="成交均价" value={displayNumber(vwap)} />
        <LowerMetric label="主动买占比" value={`${buyRatio.toFixed(1)}%`} tone={buyRatio >= sellRatio ? "up" : "down"} />
      </div>

      <div className="mt-2 grid min-h-0 grid-cols-[minmax(0,1.55fr)_minmax(150px,1fr)] gap-2">
        <section className="min-w-0 rounded bg-background/80 px-2 py-1.5">
          <div className="flex items-center justify-between text-[10px] text-muted-foreground">
            <span>分钟成交强弱</span>
            <span>点击柱体跳转</span>
          </div>
          <div className="mt-1 flex h-14 items-end gap-px overflow-hidden">
            {recent.map((group) => {
              const net = group.buyVolume - group.sellVolume;
              const intensity = Math.max(0.12, group.volume / maxMinuteVolume);
              const height = 10 + intensity * 42;
              const future = tickTimestamp(group.rows[0]) > currentTimestamp;
              return (
                <button
                  key={group.label}
                  type="button"
                  className={`min-w-0 flex-1 rounded-t-sm ${future ? "bg-muted-foreground/25 hover:bg-muted-foreground/40" : net >= 0 ? "bg-red-500/75 hover:bg-red-500" : "bg-emerald-500/75 hover:bg-emerald-500"}`}
                  style={{ height: `${height}px`, opacity: future ? 0.5 : 0.35 + intensity * 0.65 }}
                  title={`${group.label} · ${group.count} 笔 · 量 ${displayVolume(group.volume)} · 买 ${displayVolume(group.buyVolume)} · 卖 ${displayVolume(group.sellVolume)}`}
                  onClick={() => onSelect(tickTimestamp(group.rows[0]))}
                />
              );
            })}
          </div>
          <div className="mt-1 flex items-center justify-between text-[10px] text-muted-foreground">
            <span>{recent[0]?.label ?? "-"}</span>
            <span>{recent.at(-1)?.label ?? "-"}</span>
          </div>
        </section>

        <section className="min-w-0 rounded bg-background/80 px-2 py-1.5 text-[10px]">
          <div className="flex items-center justify-between">
            <span className="font-medium">当前分钟</span>
            <span className="font-mono text-muted-foreground">{latest?.label ?? "-"}</span>
          </div>
          <div className="mt-1 grid grid-cols-2 gap-x-2 gap-y-1 text-muted-foreground">
            <span>笔数 <b className="font-mono text-foreground">{latest?.count ?? 0}</b></span>
            <span className="text-right">量 <b className="font-mono text-foreground">{displayVolume(latest?.volume ?? 0)}</b></span>
            <span>均价 <b className="font-mono text-foreground">{displayNumber(latestVwap)}</b></span>
            <span className="text-right">金额 <b className="font-mono text-foreground">{displayVolume(latest?.amount ?? 0)}</b></span>
          </div>
          <div className="mt-2 flex h-2 overflow-hidden rounded-full bg-muted">
            <span className="bg-red-500/80" style={{ width: `${latest?.volume ? (latest.buyVolume / latest.volume) * 100 : 0}%` }} />
            <span className="bg-emerald-500/80" style={{ width: `${latest?.volume ? (latest.sellVolume / latest.volume) * 100 : 0}%` }} />
            <span className="bg-slate-400/70" style={{ width: `${latest?.volume ? (latest.neutralVolume / latest.volume) * 100 : 0}%` }} />
          </div>
          <div className="mt-1 flex items-center justify-between text-muted-foreground">
            <span className="text-red-600">买 {displayVolume(latest?.buyVolume ?? 0)}</span>
            <span className="text-emerald-600">卖 {displayVolume(latest?.sellVolume ?? 0)}</span>
            <span>净 {displayVolume(latestNet)}</span>
          </div>
        </section>
      </div>
    </div>
  );
}

function MarginReplayLower({ rows }: { rows: MarginStockDetail[] }) {
  const latest = rows[0];
  const previous = rows[1];
  const delta = latest?.marginBalance != null && previous?.marginBalance != null
    ? latest.marginBalance - previous.marginBalance
    : null;
  return (
    <div className="h-full space-y-2 p-3">
      <div className="grid grid-cols-4 gap-2">
        <LowerMetric label="两融余额" value={displayVolume(latest?.marginBalance)} />
        <LowerMetric label="融资余额" value={displayVolume(latest?.financingBalance)} />
        <LowerMetric label="融券余额" value={displayVolume(latest?.securitiesLendingBalance)} />
        <LowerMetric label="较前日" value={displayVolume(delta)} tone={delta != null && delta < 0 ? "down" : "up"} />
      </div>
      <p className="text-[10px] text-muted-foreground">两融为日级数据，按交易日前最近可用记录展示，不参与盘中未来数据。</p>
    </div>
  );
}

function FinanceReplayLower({ reports }: { reports: StockFinanceReportResponse[] }) {
  const values = reports.flatMap((report) => Object.entries(report.rows[0]?.fields ?? {}).slice(0, 2));
  return (
    <div className="h-full space-y-2 p-3">
      <div className="flex items-center justify-between text-[11px]">
        <span className="font-medium">最近可用财务快照</span>
        <span className="text-muted-foreground">{reports.filter((item) => item.rows.length).length}/3 张报表</span>
      </div>
      <div className="grid grid-cols-4 gap-2">
        {values.slice(0, 4).map(([key, value]) => <LowerMetric key={key} label={key} value={String(value ?? "-")} />)}
        {!values.length ? <span className="col-span-4 text-xs text-muted-foreground">暂无不晚于当前交易日的财务数据</span> : null}
      </div>
    </div>
  );
}

function FundFlowReplayLower({ rows }: { rows: ReplayFundFlowRow[] }) {
  const recent = rows.slice(0, 14).reverse();
  const max = Math.max(...recent.map((row) => Math.abs(row.mainNetAmountWan ?? 0)), 1);
  return (
    <div className="h-full space-y-2 p-3">
      <div className="flex items-center justify-between text-[11px]">
        <span className="font-medium">日级资金流趋势</span>
        <span className="text-muted-foreground">只显示当前日期及以前</span>
      </div>
      <div className="flex h-14 items-end gap-1">
        {recent.length ? recent.map((row) => {
          const value = row.mainNetAmountWan ?? 0;
          return <div key={row.trade_date} className={`min-w-0 flex-1 rounded-t ${value >= 0 ? "bg-red-500/70" : "bg-emerald-500/70"}`} style={{ height: `${Math.max(10, Math.abs(value) / max * 100)}%` }} title={`${row.trade_date} · 主力 ${displayVolume(value * 10_000)}`} />;
        }) : <span className="self-center text-xs text-muted-foreground">暂无资金流历史</span>}
      </div>
    </div>
  );
}

function SectionTitle({ title, detail }: { title: string; detail: string }) {
  return (
    <div>
      <h3 className="text-xs font-semibold">{title}</h3>
      <p className="mt-1 text-[11px] text-muted-foreground">{detail}</p>
    </div>
  );
}
function DataRows({
  rows,
  headers,
  empty,
}: {
  rows: string[][];
  headers: string[];
  empty: string;
}) {
  return rows.length ? (
    <div className="overflow-hidden rounded-md bg-background">
      <div className="grid grid-cols-3 gap-2 bg-muted/35 px-2 py-1.5 text-[10px] text-muted-foreground">
        {headers.map((header) => (
          <span key={header}>{header}</span>
        ))}
      </div>
        <div className="space-y-1">
        {rows.map((row, index) => (
          <div
            key={`${row[0]}-${index}`}
            className="grid grid-cols-3 gap-2 px-2 py-1.5 text-[11px]"
          >
            <span className="truncate">{row[0]}</span>
            <span className="truncate font-mono">{row[1]}</span>
            <span className="truncate font-mono">{row[2]}</span>
          </div>
        ))}
      </div>
    </div>
  ) : (
    <div className="rounded-md bg-muted/20 p-4 text-center text-xs text-muted-foreground">
      {empty}
    </div>
  );
}
