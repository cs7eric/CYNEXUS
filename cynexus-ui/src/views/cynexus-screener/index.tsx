import {
  lazy,
  Suspense,
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { CartesianGrid, Line, LineChart, XAxis } from "recharts";
import {
  AlertTriangle,
  BarChart3,
  BookmarkPlus,
  Bot,
  CandlestickChart,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Database,
  FlaskConical,
  History,
  ClipboardCheck,
  ListFilter,
  Loader2,
  Play,
  RefreshCw,
  Radio,
  RotateCcw,
  Save,
  SlidersHorizontal,
  Sparkles,
  TrendingUp,
  Workflow,
  X,
} from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { AiBindingProviderSelect } from "@/components/ai/ai-binding-provider-select";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { notification } from "@/components/ui/notification";
import {
  ResizableHandle,
  ResizablePanel,
  ResizablePanelGroup,
} from "@/components/ui/resizable";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";
import { WorkspaceShell } from "@/layout/workspace-shell";
import { FactorObservatoryTab } from "./factor-observatory-tab";
import { FactorResearchTab } from "./factor-research-tab";
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart";
import {
  fetchMarketIndicators,
  type MarketIndicatorEvent,
} from "@/services/market/indicator";
import {
  fetchStockScreener,
  fetchScreenerClassificationOptions,
  fetchScreenerStrategies,
  fetchScreenerStrategyHealth,
  fetchStockSelectionWatchlist,
  fetchStockSelectionWatchlistPerformance,
  fetchStockSelectionWatchlistPerformanceHistory,
  addStockSelectionWatchlistItem,
  reviewStockSelectionWatchlistItem,
  fetchScreenerStrategyBacktest,
  runScreenerStrategyBacktest,
  runScreenerStrategy,
  runStockSelectionAiScreening,
  parseStockSelectionAiIntent,
  createScreenerStrategy,
  updateScreenerStrategy,
  type ScreenerBoard,
  type ScreenerSort,
  type ScreenerSortDirection,
  type ScreenerUniverse,
  type ScreenerClassificationOptions,
  type StockScreenerItem,
  type StockScreenerParams,
  type StockScreenerResponse,
  type StockSelectionStrategy,
  type SaveStockSelectionStrategyRequest,
  type StockSelectionStrategyCandidate,
  type StockSelectionStrategyBacktestResponse,
  type StockSelectionStrategyTargetSymbol,
  type StockSelectionStrategyHealthResponse,
  type StockSelectionStrategyRunResponse,
  type StockSelectionWatchlistDecision,
  type StockSelectionWatchlistItem,
  type StockSelectionWatchlistPerformance,
  type StockSelectionWatchlistPerformanceHistoryResponse,
  type StockSelectionAiScreeningResponse,
  type StockSelectionAiIntentPlanResponse,
} from "@/services/market/screener";

const TradeReviewPanel = lazy(() =>
  import("../market/trade-review").then((module) => ({
    default: module.TradeReviewPanel,
  })),
);

const PAGE_SIZE = 50;

const BOARD_OPTIONS: Array<{ value: ScreenerBoard; label: string }> = [
  { value: "ALL", label: "全部板块" },
  { value: "main_sh", label: "沪市主板" },
  { value: "main_sz", label: "深市主板" },
  { value: "chinext", label: "创业板" },
  { value: "star", label: "科创板" },
  { value: "bse", label: "北交所" },
  { value: "unknown", label: "未分类" },
];

const BOARD_LABELS = new Map(
  BOARD_OPTIONS.map((option) => [option.value, option.label]),
);

const SORT_COLUMNS: Array<{
  key: ScreenerSort;
  label: string;
  align?: "right";
}> = [
  { key: "price", label: "现价", align: "right" },
  { key: "change_pct", label: "当日涨跌", align: "right" },
  { key: "amplitude_pct", label: "振幅", align: "right" },
  { key: "return_5d", label: "5日涨幅", align: "right" },
  { key: "return_10d", label: "10日涨幅", align: "right" },
  { key: "return_30d", label: "30日涨幅", align: "right" },
  { key: "return_60d", label: "60日涨幅", align: "right" },
  { key: "return_252d", label: "252日涨幅", align: "right" },
  { key: "main_net", label: "主力净流入", align: "right" },
  { key: "main_net_5d", label: "5日主力净流入", align: "right" },
  { key: "amount", label: "成交额", align: "right" },
  { key: "market_cap", label: "总市值", align: "right" },
  { key: "turnover_pct", label: "换手率", align: "right" },
  { key: "volume_ratio", label: "量比", align: "right" },
  { key: "pe", label: "市盈率", align: "right" },
];

type Criteria = {
  tradeDate: string;
  universe: ScreenerUniverse;
  board: ScreenerBoard;
  excludeSt: boolean;
  excludeChiNext: boolean;
  excludeStar: boolean;
  industry: string;
  tdxIndustryCode: string;
  thsIndustryCode: string;
  conceptSectorId: string;
  styleSectorId: string;
  minPrice: string;
  maxPrice: string;
  minChangePct: string;
  maxChangePct: string;
  minAmplitudePct: string;
  maxAmplitudePct: string;
  minReturnPct5d: string;
  maxReturnPct5d: string;
  minReturnPct10d: string;
  maxReturnPct10d: string;
  minReturnPct30d: string;
  maxReturnPct30d: string;
  minReturnPct60d: string;
  maxReturnPct60d: string;
  minReturnPct252d: string;
  maxReturnPct252d: string;
  minAmount: string;
  minMarketCap: string;
  maxMarketCap: string;
  minMainNet: string;
  minMainNet5d: string;
  minTurnoverPct: string;
  maxTurnoverPct: string;
  minVolumeRatio: string;
  maxVolumeRatio: string;
  volumeContractionDays: string;
  minBreakoutVolumeMultiple: string;
  maxBreakoutVolumeMultiple: string;
  minBullishBodyPct: string;
  minCloseAboveMa20Pct: string;
  minMa5AboveMa20Pct: string;
  breakoutHighDays: string;
  maxAtrPct20: string;
  minConsecutiveBullishDays: string;
  minVolumeVsMa5Multiple: string;
  maxVolumeVsMa5Multiple: string;
  candlestickPattern: string;
  minPe: string;
  maxPe: string;
  sort: ScreenerSort;
  sortDir: ScreenerSortDirection;
};

const DEFAULT_CRITERIA: Criteria = {
  tradeDate: "",
  universe: "ALL",
  board: "ALL",
  excludeSt: true,
  excludeChiNext: true,
  excludeStar: true,
  industry: "",
  tdxIndustryCode: "",
  thsIndustryCode: "",
  conceptSectorId: "",
  styleSectorId: "",
  minPrice: "",
  maxPrice: "",
  minChangePct: "",
  maxChangePct: "",
  minAmplitudePct: "",
  maxAmplitudePct: "",
  minReturnPct5d: "",
  maxReturnPct5d: "",
  minReturnPct10d: "",
  maxReturnPct10d: "",
  minReturnPct30d: "",
  maxReturnPct30d: "",
  minReturnPct60d: "",
  maxReturnPct60d: "",
  minReturnPct252d: "",
  maxReturnPct252d: "",
  minAmount: "",
  minMarketCap: "",
  maxMarketCap: "",
  minMainNet: "",
  minMainNet5d: "",
  minTurnoverPct: "",
  maxTurnoverPct: "",
  minVolumeRatio: "",
  maxVolumeRatio: "",
  volumeContractionDays: "",
  minBreakoutVolumeMultiple: "",
  maxBreakoutVolumeMultiple: "",
  minBullishBodyPct: "",
  minCloseAboveMa20Pct: "",
  minMa5AboveMa20Pct: "",
  breakoutHighDays: "",
  maxAtrPct20: "",
  minConsecutiveBullishDays: "",
  minVolumeVsMa5Multiple: "",
  maxVolumeVsMa5Multiple: "",
  candlestickPattern: "",
  minPe: "",
  maxPe: "",
  sort: "amount",
  sortDir: "desc",
};

function textValue(value: unknown): string {
  return value == null ? "" : String(value);
}

function asNumber(value: unknown): number | undefined {
  const normalized = textValue(value).trim();
  if (!normalized) return undefined;
  const parsed = Number(normalized);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function toQuery(criteria: Criteria, page: number): StockScreenerParams {
  // AI plans and persisted strategy definitions are partial by design. Merge
  // them with the complete UI shape before reading optional string fields.
  const normalized = { ...DEFAULT_CRITERIA, ...criteria };
  return {
    tradeDate: normalized.tradeDate || undefined,
    universe: normalized.universe,
    board: normalized.board === "ALL" ? undefined : normalized.board,
    excludeSt: normalized.excludeSt,
    excludeChiNext: normalized.excludeChiNext,
    excludeStar: normalized.excludeStar,
    industry: textValue(normalized.industry).trim() || undefined,
    tdxIndustryCode: normalized.tdxIndustryCode || undefined,
    thsIndustryCode: normalized.thsIndustryCode || undefined,
    conceptSectorId: normalized.conceptSectorId || undefined,
    styleSectorId: normalized.styleSectorId || undefined,
    minPrice: asNumber(normalized.minPrice),
    maxPrice: asNumber(normalized.maxPrice),
    minChangePct: asNumber(normalized.minChangePct),
    maxChangePct: asNumber(normalized.maxChangePct),
    minAmplitudePct: asNumber(normalized.minAmplitudePct),
    maxAmplitudePct: asNumber(normalized.maxAmplitudePct),
    minReturnPct5d: asNumber(normalized.minReturnPct5d),
    maxReturnPct5d: asNumber(normalized.maxReturnPct5d),
    minReturnPct10d: asNumber(normalized.minReturnPct10d),
    maxReturnPct10d: asNumber(normalized.maxReturnPct10d),
    minReturnPct30d: asNumber(normalized.minReturnPct30d),
    maxReturnPct30d: asNumber(normalized.maxReturnPct30d),
    minReturnPct60d: asNumber(normalized.minReturnPct60d),
    maxReturnPct60d: asNumber(normalized.maxReturnPct60d),
    minReturnPct252d: asNumber(normalized.minReturnPct252d),
    maxReturnPct252d: asNumber(normalized.maxReturnPct252d),
    minAmount: asNumber(normalized.minAmount),
    minMarketCap: asNumber(normalized.minMarketCap),
    maxMarketCap: asNumber(normalized.maxMarketCap),
    minMainNet: asNumber(normalized.minMainNet),
    minMainNet5d: asNumber(normalized.minMainNet5d),
    minTurnoverPct: asNumber(normalized.minTurnoverPct),
    maxTurnoverPct: asNumber(normalized.maxTurnoverPct),
    minVolumeRatio: asNumber(normalized.minVolumeRatio),
    maxVolumeRatio: asNumber(normalized.maxVolumeRatio),
    volumeContractionDays: asNumber(normalized.volumeContractionDays),
    minBreakoutVolumeMultiple: asNumber(normalized.minBreakoutVolumeMultiple),
    maxBreakoutVolumeMultiple: asNumber(normalized.maxBreakoutVolumeMultiple),
    minBullishBodyPct: asNumber(normalized.minBullishBodyPct),
    minCloseAboveMa20Pct: asNumber(normalized.minCloseAboveMa20Pct),
    minMa5AboveMa20Pct: asNumber(normalized.minMa5AboveMa20Pct),
    breakoutHighDays: asNumber(normalized.breakoutHighDays),
    maxAtrPct20: asNumber(normalized.maxAtrPct20),
    minConsecutiveBullishDays: asNumber(normalized.minConsecutiveBullishDays),
    minVolumeVsMa5Multiple: asNumber(normalized.minVolumeVsMa5Multiple),
    maxVolumeVsMa5Multiple: asNumber(normalized.maxVolumeVsMa5Multiple),
    candlestickPattern: (normalized.candlestickPattern || undefined) as StockScreenerParams["candlestickPattern"],
    minPe: asNumber(normalized.minPe),
    maxPe: asNumber(normalized.maxPe),
    sort: normalized.sort,
    sortDir: normalized.sortDir,
    page,
    pageSize: PAGE_SIZE,
  };
}

function strategyCriteria(criteria: Criteria): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(toQuery(criteria, 1)).filter(
      ([key, value]) => !["tradeDate", "page", "pageSize"].includes(key) && value !== undefined && value !== "",
    ),
  )
}

function formatNumber(value: number | null | undefined, digits = 2): string {
  if (value == null || !Number.isFinite(value)) return "—";
  return new Intl.NumberFormat("zh-CN", {
    maximumFractionDigits: digits,
    minimumFractionDigits: digits,
  }).format(value);
}

function formatAmount(value: number | null | undefined): string {
  if (value == null || !Number.isFinite(value)) return "—";
  if (Math.abs(value) >= 1e8) return `${formatNumber(value / 1e8, 2)} 亿`;
  if (Math.abs(value) >= 1e4) return `${formatNumber(value / 1e4, 1)} 万`;
  return formatNumber(value, 0);
}

function formatPercent(value: number | null | undefined): string {
  if (value == null || !Number.isFinite(value)) return "—";
  return `${value > 0 ? "+" : ""}${formatNumber(value)}%`;
}

function marketValueClass(value: number | null | undefined): string {
  if (value == null || value === 0) return "text-foreground";
  return value > 0 ? "text-[#c8332c]" : "text-[#14804a]";
}

function RangeInputs({
  criteria,
  start,
  end,
  label,
  unit,
  onChange,
}: {
  criteria: Criteria;
  start: keyof Criteria;
  end: keyof Criteria;
  label: string;
  unit?: string;
  onChange: <K extends keyof Criteria>(key: K, value: Criteria[K]) => void;
}) {
  return (
    <div className="grid grid-cols-[minmax(0,1fr)_minmax(0,1fr)] gap-2">
      <label className="min-w-0 space-y-1">
        <span className="block truncate text-[11px] text-muted-foreground">
          {label} 下限{unit ? `（${unit}）` : ""}
        </span>
        <Input
          type="number"
          step="any"
          value={criteria[start] as string}
          placeholder="不限"
          className="h-8 bg-muted/55 text-xs"
          onChange={(event) =>
            onChange(start, event.target.value as Criteria[typeof start])
          }
        />
      </label>
      <label className="min-w-0 space-y-1">
        <span className="block truncate text-[11px] text-muted-foreground">
          {label} 上限{unit ? `（${unit}）` : ""}
        </span>
        <Input
          type="number"
          step="any"
          value={criteria[end] as string}
          placeholder="不限"
          className="h-8 bg-muted/55 text-xs"
          onChange={(event) =>
            onChange(end, event.target.value as Criteria[typeof end])
          }
        />
      </label>
    </div>
  );
}

function FilterSection({
  icon,
  title,
  children,
}: {
  icon: ReactNode;
  title: string;
  children: ReactNode;
}) {
  return (
    <Collapsible
      defaultOpen
      className="group border-b border-border/60 pb-5 last:border-b-0 last:pb-0"
    >
      <CollapsibleTrigger asChild>
        <button
          type="button"
          className="flex w-full items-center justify-between gap-3 rounded-sm py-0.5 text-left text-xs font-semibold text-foreground outline-none hover:text-primary focus-visible:ring-2 focus-visible:ring-ring"
        >
          <span className="flex min-w-0 items-center gap-2">
            {icon}
            <span className="truncate">{title}</span>
          </span>
          <ChevronDown
            className="size-3.5 shrink-0 text-muted-foreground transition-transform duration-150 group-data-[state=closed]:-rotate-90"
            aria-hidden="true"
          />
        </button>
      </CollapsibleTrigger>
      <CollapsibleContent>
        <div className="space-y-3 pt-3">{children}</div>
      </CollapsibleContent>
    </Collapsible>
  );
}

function aiCriteria(
  criteria: Criteria,
): Omit<StockScreenerParams, "page" | "pageSize"> {
  const query = toQuery(criteria, 1);
  delete query.page;
  delete query.pageSize;
  return query;
}

type AiCandidateSelection = {
  code: string;
  name?: string | null;
  tradeDate?: string | null;
};

type AiScreeningState = {
  result: StockSelectionAiScreeningResponse | null;
  loading: boolean;
  error: string | null;
};

const EXECUTABLE_CRITERIA_LABELS: Record<string, string> = {
  universe: "市场", board: "板块", excludeSt: "剔除 ST", tdxIndustryCode: "通达信行业大类",
  thsIndustryCode: "同花顺行业", conceptSectorId: "通达信概念", styleSectorId: "通达信风格",
  minPrice: "收盘价下限", maxPrice: "收盘价上限", minChangePct: "当日涨幅下限",
  maxChangePct: "当日涨幅上限", minAmplitudePct: "振幅下限", maxAmplitudePct: "振幅上限",
  minReturnPct5d: "5日涨幅下限", maxReturnPct5d: "5日涨幅上限", minReturnPct10d: "10日涨幅下限", maxReturnPct10d: "10日涨幅上限",
  minReturnPct30d: "30日涨幅下限", maxReturnPct30d: "30日涨幅上限", minReturnPct60d: "60日涨幅下限", maxReturnPct60d: "60日涨幅上限",
  minReturnPct252d: "252日涨幅下限", maxReturnPct252d: "252日涨幅上限", minAmount: "成交额下限", minMarketCap: "市值下限", maxMarketCap: "市值上限",
  minMainNet: "主力净流入下限", minMainNet5d: "5日主力净流入下限", minTurnoverPct: "换手率下限", maxTurnoverPct: "换手率上限",
  minVolumeRatio: "量比下限", maxVolumeRatio: "量比上限",
  volumeContractionDays: "连续缩量天数", minBreakoutVolumeMultiple: "放量倍数下限", maxBreakoutVolumeMultiple: "放量倍数上限",
  minBullishBodyPct: "阳线实体下限", minCloseAboveMa20Pct: "收盘价高于 MA20", minMa5AboveMa20Pct: "MA5 高于 MA20",
  breakoutHighDays: "突破近高回看天数", maxAtrPct20: "ATR20 上限", minConsecutiveBullishDays: "连续阳线天数",
  minVolumeVsMa5Multiple: "成交量相对 MA5 下限", maxVolumeVsMa5Multiple: "成交量相对 MA5 上限", candlestickPattern: "K 线形态", minPe: "市盈率下限", maxPe: "市盈率上限", sort: "排序字段", sortDir: "排序方向",
};

const NUMERIC_EXECUTABLE_CRITERIA = new Set([
  "minPrice", "maxPrice", "minChangePct", "maxChangePct", "minAmplitudePct", "maxAmplitudePct",
  "minReturnPct5d", "maxReturnPct5d", "minReturnPct10d", "maxReturnPct10d", "minReturnPct30d", "maxReturnPct30d",
  "minReturnPct60d", "maxReturnPct60d", "minReturnPct252d", "maxReturnPct252d", "minAmount", "minMarketCap", "maxMarketCap",
  "minMainNet", "minMainNet5d", "minTurnoverPct", "maxTurnoverPct", "minVolumeRatio", "maxVolumeRatio",
  "volumeContractionDays", "minBreakoutVolumeMultiple", "maxBreakoutVolumeMultiple", "minBullishBodyPct", "minCloseAboveMa20Pct", "minMa5AboveMa20Pct",
  "breakoutHighDays", "maxAtrPct20", "minConsecutiveBullishDays", "minVolumeVsMa5Multiple", "maxVolumeVsMa5Multiple", "minPe", "maxPe",
]);

type EditablePlanCriteria = Record<string, unknown>;

function PlanCriterionEditor({
  criterionKey,
  value,
  onValueChange,
}: {
  criterionKey: string;
  value: unknown;
  onValueChange: (value: string | boolean) => void;
}) {
  const label = EXECUTABLE_CRITERIA_LABELS[criterionKey] || criterionKey;
  const valueText = value == null ? "" : String(value);
  const selectOptions = criterionKey === "universe"
    ? [
        { value: "ALL", label: "全部 A 股" },
        { value: "SH", label: "上海" },
        { value: "SZ", label: "深圳" },
        { value: "BJ", label: "北京" },
      ]
    : criterionKey === "board"
      ? BOARD_OPTIONS
      : criterionKey === "sort"
        ? SORT_COLUMNS.map(({ key, label: optionLabel }) => ({ value: key, label: optionLabel }))
        : criterionKey === "sortDir"
          ? [
              { value: "desc", label: "降序" },
              { value: "asc", label: "升序" },
            ]
          : null;

  return (
    <label className="min-w-0 rounded-md bg-background/60 px-2 py-1.5">
      <span className="block truncate text-[10px] text-muted-foreground">{label}</span>
      {criterionKey === "excludeSt" ? (
        <span className="mt-1 flex h-8 items-center gap-2 text-[11px] text-foreground">
          <Checkbox
            checked={value === true}
            onCheckedChange={(checked) => onValueChange(checked === true)}
          />
          剔除 ST
        </span>
      ) : selectOptions ? (
        <Select value={valueText} onValueChange={onValueChange}>
          <SelectTrigger size="sm" className="mt-1 h-8 w-full bg-muted/55 text-[11px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {selectOptions.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      ) : (
        <Input
          type={NUMERIC_EXECUTABLE_CRITERIA.has(criterionKey) ? "number" : "text"}
          step={NUMERIC_EXECUTABLE_CRITERIA.has(criterionKey) ? "any" : undefined}
          value={valueText}
          className="mt-1 h-8 bg-muted/55 font-mono text-[11px]"
          onChange={(event) => onValueChange(event.target.value)}
        />
      )}
    </label>
  );
}

function AiScreeningPanel({
  criteria,
  loading,
  onRun,
  onApplyCriteria,
  screeningState,
  onScreeningStateChange,
}: {
  criteria: Criteria;
  loading: boolean;
  onRun: () => Promise<StockScreenerResponse | null>;
  onApplyCriteria: (criteria: StockSelectionAiIntentPlanResponse["criteria"]) => void;
  screeningState: AiScreeningState;
  onScreeningStateChange: (state: AiScreeningState) => void;
}) {
  const [intent, setIntent] = useState("");
  const [maxCandidates, setMaxCandidates] = useState("12");
  const [includeAiReview, setIncludeAiReview] = useState(false);
  const [planningError, setPlanningError] = useState<string | null>(null);
  const [plan, setPlan] = useState<StockSelectionAiIntentPlanResponse | null>(null);
  const [draftCriteria, setDraftCriteria] = useState<EditablePlanCriteria | null>(null);
  const [planning, setPlanning] = useState(false);
  const [planApplied, setPlanApplied] = useState(false);
  const run = useCallback(async () => {
    const screenResult = await onRun();
    if (!includeAiReview || !screenResult?.ok || screenResult.items.length === 0) {
      return;
    }
    onScreeningStateChange({ result: null, loading: true, error: null });
    try {
      const nextResult = await runStockSelectionAiScreening({
        researchIntent: intent.trim() || undefined,
        maxCandidates: Number(maxCandidates),
        criteria: aiCriteria(criteria),
      });
      onScreeningStateChange({ result: nextResult, loading: false, error: null });
    } catch (caught) {
      onScreeningStateChange({
        result: null,
        loading: false,
        error: caught instanceof Error ? caught.message : "Unable to run AI screening.",
      });
    }
  }, [criteria, includeAiReview, intent, maxCandidates, onRun, onScreeningStateChange]);
  const interpret = useCallback(async () => {
    setPlanning(true);
    setPlanningError(null);
    try {
      const resolvedPlan = await parseStockSelectionAiIntent({
        researchIntent: intent.trim() || undefined,
        criteria: aiCriteria(criteria),
      });
      setPlan(resolvedPlan);
      setDraftCriteria({ ...resolvedPlan.criteria });
      setPlanApplied(false);
    } catch (caught) {
      setPlan(null);
      setDraftCriteria(null);
      setPlanningError(caught instanceof Error ? caught.message : "Unable to interpret the screening intent.");
    } finally {
      setPlanning(false);
    }
  }, [criteria, intent]);
  return (
    <div className="min-h-0 flex-1 overflow-y-auto p-4">
      <div className="space-y-4">
        <section className="rounded-lg border border-border/60 bg-muted/25 p-3">
          <div className="flex items-start gap-2">
            <Bot className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
            <div>
              <h3 className="text-xs font-semibold">Bounded AI research</h3>
              <p className="mt-1 text-[11px] leading-5 text-muted-foreground">
                Supported intent rules are resolved into the daily-close
                screener before the model receives candidates. The model then
                ranks research follow-ups; it does not create a trade decision.
              </p>
            </div>
          </div>
        </section>
        <label className="block space-y-1">
          <span className="text-[11px] text-muted-foreground">
            Research intent
          </span>
          <Textarea
            value={intent}
            maxLength={600}
            placeholder="For example: prioritize momentum candidates with liquid participation and explain what to verify next."
            className="min-h-24 resize-y bg-muted/55 text-xs"
            onChange={(event) => setIntent(event.target.value)}
          />
        </label>
        <label className="block space-y-1">
          <span className="text-[11px] text-muted-foreground">
            Candidates to review
          </span>
          <Input
            type="number"
            min={1}
            max={30}
            value={maxCandidates}
            className="h-8 bg-muted/55 font-mono text-xs"
            onChange={(event) => setMaxCandidates(event.target.value)}
          />
        </label>
        <Button
          type="button"
          variant="default"
          className="w-full"
          onClick={() => void interpret()}
          disabled={planning || !intent.trim()}
        >
          {planning ? <Loader2 className="animate-spin" /> : <ClipboardCheck />}
          {planning ? "Converting intent" : "Convert to executable indicators"}
        </Button>
        <div className="rounded-md bg-muted/45 p-3">
          <div className="flex items-start gap-2">
            <Checkbox
              id="screener-ai-review"
              checked={includeAiReview}
              onCheckedChange={(checked) => setIncludeAiReview(checked === true)}
            />
            <div className="grid gap-1 leading-none">
              <label htmlFor="screener-ai-review" className="text-xs font-medium text-foreground">
                完成选股后进行 AI 研判
              </label>
              <p className="text-[11px] leading-5 text-muted-foreground">
                仅对确定性筛选出的候选生成研究逻辑、风险与后续观察项，不改变入选结果。
              </p>
            </div>
          </div>
        </div>
        <Button
          type="button"
          variant="default"
          className="w-full"
          onClick={() => void run()}
          disabled={loading || screeningState.loading || !planApplied}
        >
          {loading || screeningState.loading ? <Loader2 className="animate-spin" /> : includeAiReview ? <Sparkles /> : <Play />}
          {loading
            ? "正在选股"
            : screeningState.loading
              ? "正在进行 AI 研判"
              : includeAiReview
                ? "执行选股并进行 AI 研判"
                : "执行选股"}
        </Button>
        {plan && draftCriteria ? (
          <section className="rounded-lg border border-border/60 bg-muted/25 p-3">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="text-xs font-semibold">Verified screening plan</p>
                <p className="mt-1 text-[11px] leading-5 text-muted-foreground">
                  Adjust supported rules here. Changes remain local until they are applied.
                </p>
              </div>
              <Button variant="secondary" size="sm" onClick={() => {
                onApplyCriteria(draftCriteria as StockSelectionAiIntentPlanResponse["criteria"]);
                setPlanApplied(true);
              }}>
                Apply changes
              </Button>
            </div>
            <div className="mt-3 grid grid-cols-2 gap-2 border-y border-border/60 py-2">
              {Object.entries(draftCriteria).map(([key, value]) => (
                <PlanCriterionEditor
                  key={key}
                  criterionKey={key}
                  value={value}
                  onValueChange={(nextValue) => {
                    setDraftCriteria((current) => current ? { ...current, [key]: nextValue } : current);
                    setPlanApplied(false);
                  }}
                />
              ))}
            </div>
            {plan.appliedRules.length ? (
              <ul className="mt-2 space-y-1 text-[11px] leading-5 text-foreground">
                {plan.appliedRules.map((rule) => <li key={rule}>• {rule}</li>)}
              </ul>
            ) : null}
            {plan.assumptions.length ? (
              <div className="mt-2 border-t border-border/60 pt-2">
                <p className="text-[11px] font-medium text-muted-foreground">Editable assumptions</p>
                {plan.assumptions.map((item) => <p key={item} className="mt-1 text-[11px] leading-5 text-foreground">• {item}</p>)}
              </div>
            ) : null}
            {(plan.proposedConditions ?? []).length ? (
              <div className="mt-2 border-t border-border/60 pt-2">
                <p className="text-[11px] font-medium text-muted-foreground">Research conditions preserved by AI</p>
                <div className="mt-2 space-y-2">
                  {(plan.proposedConditions ?? []).map((condition, index) => (
                    <div key={`${condition.label}-${index}`} className="rounded-md bg-muted/55 px-2.5 py-2">
                      <div className="flex flex-wrap items-center gap-1.5">
                        <p className="text-[11px] font-medium text-foreground">{condition.label}</p>
                        <Badge variant="secondary" className="text-[10px]">{condition.category}</Badge>
                        <Badge variant={condition.executionStatus === "EXECUTABLE" ? "default" : "secondary"} className="text-[10px]">
                          {condition.executionStatus === "EXECUTABLE" ? "Executable" : condition.executionStatus === "NEEDS_DATA" ? "Needs data" : "Research only"}
                        </Badge>
                      </div>
                      <p className="mt-1 text-[11px] leading-5 text-muted-foreground">{condition.definition}</p>
                      {condition.sourcePhrase ? <p className="mt-1 text-[10px] text-muted-foreground">From: {condition.sourcePhrase}</p> : null}
                      {condition.dataRequirements.length ? <p className="mt-1 text-[10px] text-warning">Data: {condition.dataRequirements.join("; ")}</p> : null}
                    </div>
                  ))}
                </div>
              </div>
            ) : null}
            {plan.unsupportedPhrases.map((item) => (
              <p key={item} className="mt-2 text-[11px] leading-5 text-warning">{item}</p>
            ))}
            {plan.warnings.map((warning) => (
              <p key={warning} className="mt-2 text-[11px] leading-5 text-muted-foreground">{warning}</p>
            ))}
            {plan.providerCode || plan.modelCode ? (
              <p className="mt-2 text-[10px] text-muted-foreground">Route: {plan.providerCode || "provider unavailable"} / {plan.modelCode || "model unavailable"}{plan.promptVersion ? ` / ${plan.promptVersion}` : ""}</p>
            ) : null}
          </section>
        ) : null}
        {planningError ? (
          <div className="flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-xs text-destructive">
            <AlertTriangle className="mt-0.5 size-3.5 shrink-0" />
            {planningError}
          </div>
        ) : null}
      </div>
    </div>
  );
}

function strategyParameters(
  strategy: StockSelectionStrategy,
): Partial<Criteria> {
  try {
    const values = JSON.parse(strategy.activeVersion.parameterSchema) as Record<
      string,
      unknown
    >;
    const mapped: Partial<Criteria> = {};
    const parameterMap: Record<string, unknown> = mapped;
    for (const [key, value] of Object.entries(values)) {
      if (key in DEFAULT_CRITERIA && key !== "requiresTdxIndustry") {
        parameterMap[key] = typeof value === "boolean" ? value : String(value);
      }
    }
    return mapped;
  } catch {
    return {};
  }
}

function StrategyBacktestDialog({
  strategy,
  open,
  onOpenChange,
}: {
  strategy: StockSelectionStrategy | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const [asOfDate, setAsOfDate] = useState("");
  const [lookbackSessions, setLookbackSessions] = useState("60");
  const [signalIntervalSessions, setSignalIntervalSessions] = useState("5");
  const [holdingSessions, setHoldingSessions] = useState("10");
  const [maxCandidatesPerSignal, setMaxCandidatesPerSignal] = useState("20");
  const [transactionCostBpsPerSide, setTransactionCostBpsPerSide] =
    useState("10");
  const [stockCode, setStockCode] = useState("");
  const [stockMarket, setStockMarket] = useState<"SH" | "SZ" | "BJ">("SZ");
  const [targetSymbolsInput, setTargetSymbolsInput] = useState("");
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] =
    useState<StockSelectionStrategyBacktestResponse | null>(null);
  const backtestId = result?.id;
  const pending = result?.status === "QUEUED" || result?.status === "RUNNING";
  const singleStockMode = stockCode.trim().length > 0;
  const targetListMode = targetSymbolsInput.trim().length > 0;

  useEffect(() => {
    if (!open || !backtestId || !pending) return;
    let cancelled = false;
    const poll = async () => {
      try {
        const next = await fetchScreenerStrategyBacktest(backtestId);
        if (!cancelled) setResult(next);
      } catch (pollError) {
        if (!cancelled)
          setError(
            pollError instanceof Error
              ? pollError.message
              : "Unable to refresh the backtest status",
          );
      }
    };
    void poll();
    const interval = window.setInterval(() => void poll(), 2_000);
    return () => {
      cancelled = true;
      window.clearInterval(interval);
    };
  }, [backtestId, open, pending]);

  const run = useCallback(async () => {
    if (!strategy) return;
    if (singleStockMode && targetListMode) {
      setError("Use either one target stock or a target list, not both.");
      return;
    }
    setRunning(true);
    setError(null);
    try {
      const resolvedTargetSymbols = targetListMode
        ? parseBacktestTargetSymbols(targetSymbolsInput)
        : [];
      setResult(
        await runScreenerStrategyBacktest(strategy.code, {
          asOfDate: asOfDate || undefined,
          lookbackSessions: Number(lookbackSessions),
          signalIntervalSessions: Number(signalIntervalSessions),
          holdingSessions: Number(holdingSessions),
          maxCandidatesPerSignal: Number(maxCandidatesPerSignal),
          transactionCostBpsPerSide: Number(transactionCostBpsPerSide),
          stockCode: singleStockMode ? stockCode.trim() : undefined,
          stockMarket: singleStockMode ? stockMarket : undefined,
          targetSymbols: resolvedTargetSymbols.length ? resolvedTargetSymbols : undefined,
        }),
      );
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : "Unable to complete strategy backtest.",
      );
    } finally {
      setRunning(false);
    }
  }, [
    asOfDate,
    holdingSessions,
    lookbackSessions,
    maxCandidatesPerSignal,
    signalIntervalSessions,
    singleStockMode,
    stockCode,
    stockMarket,
    strategy,
    targetListMode,
    targetSymbolsInput,
    transactionCostBpsPerSide,
  ]);

  const fields = [
    {
      label: "Lookback sessions",
      value: lookbackSessions,
      setValue: setLookbackSessions,
      min: 5,
      max: 260,
    },
    {
      label: "Signal interval",
      value: signalIntervalSessions,
      setValue: setSignalIntervalSessions,
      min: 1,
      max: 30,
    },
    {
      label: "Holding sessions",
      value: holdingSessions,
      setValue: setHoldingSessions,
      min: 1,
      max: 60,
    },
    {
      label: "Candidates / signal",
      value: maxCandidatesPerSignal,
      setValue: setMaxCandidatesPerSignal,
      min: 1,
      max: 20,
    },
    {
      label: "Cost / side (bps)",
      value: transactionCostBpsPerSide,
      setValue: setTransactionCostBpsPerSide,
      min: 0,
      max: 100,
    },
  ];

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="flex max-h-[92vh] w-[calc(100%-2rem)] max-w-[980px] flex-col gap-0 overflow-hidden p-0">
        <DialogTitle className="border-b border-border/60 px-5 py-4 text-base">
          {strategy ? `${strategy.name} backtest` : "Strategy backtest"}
        </DialogTitle>
        <DialogDescription className="sr-only">
          Replay the governed strategy against historical daily-close data and
          inspect its performance.
        </DialogDescription>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <section className="grid gap-3 border-b border-border/60 bg-muted/25 p-5 sm:grid-cols-3">
            <label className="space-y-1">
              <span className="text-[11px] text-muted-foreground">
                As of date
              </span>
              <Input
                type="date"
                value={asOfDate}
                className="h-8 bg-background text-xs"
                onChange={(event) => setAsOfDate(event.target.value)}
              />
            </label>
            <label className="space-y-1">
              <span className="text-[11px] text-muted-foreground">
                Target stock (optional)
              </span>
              <Input
                value={stockCode}
                maxLength={6}
                inputMode="numeric"
                placeholder="000001"
                className="h-8 bg-background font-mono text-xs"
                onChange={(event) =>
                  setStockCode(event.target.value.replace(/\D/g, ""))
                }
              />
            </label>
            <label className="space-y-1">
              <span className="text-[11px] text-muted-foreground">
                Target market
              </span>
              <Select
                value={stockMarket}
                onValueChange={(value) =>
                  setStockMarket(value as "SH" | "SZ" | "BJ")
                }
              >
                <SelectTrigger className="h-8 bg-background text-xs">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="SH">SH</SelectItem>
                  <SelectItem value="SZ">SZ</SelectItem>
                  <SelectItem value="BJ">BJ</SelectItem>
                </SelectContent>
              </Select>
            </label>
            <label className="space-y-1 sm:col-span-3">
              <span className="text-[11px] text-muted-foreground">
                Target list (optional, max 50)
              </span>
              <Textarea
                value={targetSymbolsInput}
                placeholder="SZ:000001, SH:600519, BJ:430047"
                className="min-h-[60px] bg-background font-mono text-xs"
                onChange={(event) => setTargetSymbolsInput(event.target.value)}
              />
            </label>
            {fields.map((field) => (
              <label key={field.label} className="space-y-1">
                <span className="text-[11px] text-muted-foreground">
                  {field.label}
                </span>
                <Input
                  type="number"
                  min={field.min}
                  max={field.max}
                  value={field.value}
                  className="h-8 bg-background font-mono text-xs"
                  onChange={(event) => field.setValue(event.target.value)}
                />
              </label>
            ))}
            <p className="sm:col-span-3 text-[11px] leading-5 text-muted-foreground">
              A target symbol or list is checked against the same historical
              strategy conditions on every signal date. List mode evaluates
              every supplied symbol rather than taking the top-ranked screen
              results; candidates-per-signal is not used.
            </p>
          </section>
          {error ? (
            <div className="m-5 flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-xs text-destructive">
              <AlertTriangle className="mt-0.5 size-3.5 shrink-0" />
              {error}
            </div>
          ) : null}
          {result ? (
            <div className="space-y-5 p-5">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <div className="text-sm font-semibold">
                    Performance summary
                  </div>
                  <div className="mt-1 text-xs text-muted-foreground">
                    Snapshot {result.actualAsOfDate || "unavailable"} ·
                    benchmark {result.benchmark} · v{result.strategyVersionNo}
                    {result.config.stockCode
                      ? ` · target ${result.config.stockMarket}:${result.config.stockCode}`
                      : result.config.targetSymbols.length
                        ? ` · ${result.config.targetSymbols.length} target symbols`
                        : ""}
                  </div>
                </div>
                <Badge variant="secondary">{result.status}</Badge>
              </div>
              <div className="grid grid-cols-2 gap-px overflow-hidden rounded-lg border border-border/60 bg-border sm:grid-cols-4">
                <div className="bg-background p-3">
                  <div className="text-[11px] text-muted-foreground">
                    Win rate
                  </div>
                  <div className="mt-1 font-mono text-lg tabular-nums">
                    {formatPercent(result.summary.winRatePct)}
                  </div>
                </div>
                <div className="bg-background p-3">
                  <div className="text-[11px] text-muted-foreground">
                    Avg net return
                  </div>
                  <div
                    className={`mt-1 font-mono text-lg tabular-nums ${marketValueClass(result.summary.averageNetReturnPct)}`}
                  >
                    {formatPercent(result.summary.averageNetReturnPct)}
                  </div>
                </div>
                <div className="bg-background p-3">
                  <div className="text-[11px] text-muted-foreground">
                    Avg excess
                  </div>
                  <div
                    className={`mt-1 font-mono text-lg tabular-nums ${marketValueClass(result.summary.averageExcessReturnPct)}`}
                  >
                    {formatPercent(result.summary.averageExcessReturnPct)}
                  </div>
                </div>
                <div className="bg-background p-3">
                  <div className="text-[11px] text-muted-foreground">
                    Evaluated trades
                  </div>
                  <div className="mt-1 font-mono text-lg tabular-nums">
                    {result.summary.evaluatedTradeCount}
                  </div>
                </div>
              </div>
              {result.warnings.length > 0 ? (
                <div className="space-y-1 rounded-md bg-muted/45 p-3 text-xs text-muted-foreground">
                  {result.warnings.map((warning) => (
                    <div key={warning} className="flex items-start gap-2">
                      <AlertTriangle className="mt-0.5 size-3 shrink-0 text-warning" />
                      {warning}
                    </div>
                  ))}
                </div>
              ) : null}
              <section>
                <div className="flex items-center justify-between gap-3">
                  <h3 className="text-sm font-semibold">Executed trades</h3>
                  <span className="text-[11px] text-muted-foreground">
                    T+1 entry · {result.config.holdingSessions} sessions held ·{" "}
                    {result.config.transactionCostBpsPerSide} bps / side
                    {result.config.stockCode
                      ? " · single-symbol"
                      : result.config.targetSymbols.length
                        ? " · target-list"
                        : ""}
                  </span>
                </div>
                {result.trades.length === 0 ? (
                  <div className="mt-3 rounded-md bg-muted/45 p-4 text-xs text-muted-foreground">
                    No trade had complete entry, exit, and benchmark closes in
                    this window.
                  </div>
                ) : (
                  <div className="mt-3 overflow-x-auto rounded-lg border border-border/60">
                    <table className="w-full min-w-[820px] text-left text-xs">
                      <thead className="bg-muted/60 text-muted-foreground">
                        <tr>
                          <th className="px-3 py-2 font-medium">Security</th>
                          <th className="px-3 py-2 font-medium">Signal</th>
                          <th className="px-3 py-2 font-medium">
                            Entry / exit
                          </th>
                          <th className="px-3 py-2 text-right font-medium">
                            Net return
                          </th>
                          <th className="px-3 py-2 text-right font-medium">
                            Benchmark
                          </th>
                          <th className="px-3 py-2 text-right font-medium">
                            Excess
                          </th>
                          <th className="px-3 py-2 font-medium">Outcome</th>
                        </tr>
                      </thead>
                      <tbody>
                        {result.trades.map((trade) => (
                          <tr
                            key={trade.id}
                            className="border-t border-border/60"
                          >
                            <td className="px-3 py-2.5">
                              <div className="font-medium">
                                {trade.stockName || trade.stockCode}
                              </div>
                              <div className="font-mono text-[10px] text-muted-foreground">
                                {trade.market} {trade.stockCode}
                              </div>
                            </td>
                            <td className="px-3 py-2.5 font-mono tabular-nums">
                              {trade.signalDate}
                            </td>
                            <td className="px-3 py-2.5 font-mono tabular-nums text-muted-foreground">
                              {trade.entryDate} / {trade.exitDate}
                            </td>
                            <td
                              className={`px-3 py-2.5 text-right font-mono tabular-nums ${marketValueClass(trade.netReturnPct)}`}
                            >
                              {formatPercent(trade.netReturnPct)}
                            </td>
                            <td className="px-3 py-2.5 text-right font-mono tabular-nums">
                              {formatPercent(trade.benchmarkReturnPct)}
                            </td>
                            <td
                              className={`px-3 py-2.5 text-right font-mono tabular-nums ${marketValueClass(trade.excessReturnPct)}`}
                            >
                              {formatPercent(trade.excessReturnPct)}
                            </td>
                            <td className="px-3 py-2.5">
                              <Badge variant="secondary">{trade.outcome}</Badge>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>
            </div>
          ) : (
            <div className="flex min-h-[260px] flex-col items-center justify-center gap-2 px-6 text-center">
              <History className="size-5 text-muted-foreground" />
              <p className="text-sm font-medium">Replay a governed strategy</p>
              <p className="max-w-md text-xs leading-5 text-muted-foreground">
                Signals are evaluated on their historical daily-close snapshot.
                Trades enter on the next session close and exit after the
                selected holding period.
              </p>
            </div>
          )}
        </div>
        <div className="flex shrink-0 items-center justify-between gap-3 border-t border-border/60 px-5 py-3">
          <span className="text-[11px] text-muted-foreground">
            The result is saved to your workspace and compared with CSI 300.
          </span>
          <div className="flex items-center gap-2">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => onOpenChange(false)}
            >
              Close
            </Button>
            <Button
              variant="default"
              size="sm"
              onClick={() => void run()}
              disabled={running || !strategy}
            >
              {running ? <Loader2 className="animate-spin" /> : <History />}
              {running ? "Running" : "Run backtest"}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

function parseBacktestTargetSymbols(
  value: string,
): StockSelectionStrategyTargetSymbol[] {
  const parsed = value
    .split(/[\s,;]+/)
    .filter(Boolean)
    .map((item) => {
      const match = item
        .trim()
        .toUpperCase()
        .match(/^(SH|SZ|BJ)[:.]?(\d{6})$/);
      if (!match)
        throw new Error(`Invalid target symbol: ${item}. Use SZ:000001.`);
      return { market: match[1], code: match[2] } as StockSelectionStrategyTargetSymbol;
    });
  const unique = Array.from(
    new Map(parsed.map((item) => [`${item.market}:${item.code}`, item])).values(),
  );
  if (unique.length > 50) throw new Error("Target lists support at most 50 symbols.");
  return unique;
}

function healthBadgeClass(
  status: StockSelectionStrategyHealthResponse["status"] | undefined,
): string {
  if (status === "VALID")
    return "border-emerald-500/30 bg-emerald-50 text-emerald-700";
  if (status === "INVALID")
    return "border-destructive/30 bg-destructive/5 text-destructive";
  if (status === "NOT_ASSESSED")
    return "border-border bg-muted text-muted-foreground";
  return "border-warning/30 bg-warning/5 text-warning";
}

function StrategySaveDialog({
  open,
  onOpenChange,
  criteria,
  strategy,
  onSaved,
}: {
  open: boolean;
  onOpenChange: (next: boolean) => void;
  criteria: Record<string, unknown>;
  strategy: StockSelectionStrategy | null;
  onSaved: (strategy: StockSelectionStrategy) => void;
}) {
  const [name, setName] = useState(strategy?.name || "");
  const [description, setDescription] = useState(strategy?.description || "");
  const [changeSummary, setChangeSummary] = useState("");
  const [saving, setSaving] = useState(false);
  const isUpdate = strategy?.type === "USER_SCREENER";
  const submit = async () => {
    if (!name.trim() || !changeSummary.trim()) {
      notification.danger({ title: "Name and change summary are required." });
      return;
    }
    setSaving(true);
    try {
      const request: SaveStockSelectionStrategyRequest = {
        name: name.trim(),
        description: description.trim() || undefined,
        criteria,
        changeSummary: changeSummary.trim(),
        expectedDefinitionVersion: isUpdate
          ? strategy.activeVersion.definitionVersion
          : undefined,
      };
      const saved = isUpdate
        ? await updateScreenerStrategy(strategy.code, request)
        : await createScreenerStrategy(request);
      notification.success({ title: isUpdate ? "Strategy updated." : "Strategy saved." });
      onSaved(saved);
      onOpenChange(false);
    } catch (caught) {
      notification.danger({ title: caught instanceof Error ? caught.message : "Unable to save strategy." });
    } finally {
      setSaving(false);
    }
  };
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogTitle>{isUpdate ? "Update screener strategy" : "Save screener strategy"}</DialogTitle>
        <DialogDescription>
          The current multi-factor conditions become an immutable governed version.
        </DialogDescription>
        <div className="space-y-4 py-2">
          <label className="block space-y-1.5">
            <span className="text-xs font-medium">Name</span>
            <Input value={name} maxLength={128} onChange={(event) => setName(event.target.value)} placeholder="Breakout with volume confirmation" />
          </label>
          <label className="block space-y-1.5">
            <span className="text-xs font-medium">Description <span className="font-normal text-muted-foreground">(optional)</span></span>
            <Textarea value={description} maxLength={1000} className="min-h-20 resize-y" onChange={(event) => setDescription(event.target.value)} placeholder="What this screen is intended to identify" />
          </label>
          <label className="block space-y-1.5">
            <span className="text-xs font-medium">Change summary</span>
            <Textarea value={changeSummary} maxLength={1000} className="min-h-20 resize-y" onChange={(event) => setChangeSummary(event.target.value)} placeholder={isUpdate ? "What changed and why" : "Initial version rationale"} />
          </label>
          <div className="rounded-md border border-border/60 bg-muted/35 px-3 py-2 text-xs text-muted-foreground">
            {Object.keys(criteria).length} executable criteria will be validated against the governed Screener vocabulary.
          </div>
        </div>
        <div className="flex justify-end gap-2 border-t border-border/60 pt-4">
          <Button variant="secondary" onClick={() => onOpenChange(false)} disabled={saving}>Cancel</Button>
          <Button variant="default" onClick={() => void submit()} disabled={saving}>
            {saving ? <Loader2 className="animate-spin" /> : <Save />}
            {saving ? "Saving" : isUpdate ? "Update strategy" : "Save strategy"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}

function StrategyCatalogPanel({
  catalog,
  loading,
  error,
  runningCode,
  healthByCode,
  healthLoading,
  onRetry,
  onPreview,
  onEdit,
  onRun,
  onBacktest,
}: {
  catalog: StockSelectionStrategy[] | null;
  loading: boolean;
  error: string | null;
  runningCode: string | null;
  healthByCode: Record<string, StockSelectionStrategyHealthResponse>;
  healthLoading: boolean;
  onRetry: () => void;
  onPreview: (strategy: StockSelectionStrategy) => void;
  onEdit: (strategy: StockSelectionStrategy) => void;
  onRun: (strategy: StockSelectionStrategy) => void;
  onBacktest: (strategy: StockSelectionStrategy) => void;
}) {
  if (loading)
    return (
      <div className="min-h-0 flex-1 space-y-3 overflow-y-auto p-4">
        {Array.from({ length: 3 }, (_, index) => (
          <div
            key={index}
            className="space-y-2 rounded-lg border border-border/60 p-4"
          >
            <Skeleton className="h-4 w-32" />
            <Skeleton className="h-3 w-full" />
            <Skeleton className="h-8 w-28" />
          </div>
        ))}
      </div>
    );
  if (error)
    return (
      <div className="flex min-h-0 flex-1 flex-col items-center justify-center gap-3 p-6 text-center">
        <AlertTriangle className="size-5 text-warning" />
        <div>
          <p className="text-sm font-medium">Strategy catalog unavailable</p>
          <p className="mt-1 max-w-sm text-xs leading-5 text-muted-foreground">
            {error}
          </p>
        </div>
        <Button variant="secondary" size="sm" onClick={onRetry}>
          Retry
        </Button>
      </div>
    );
  if (catalog?.length === 0)
    return (
      <div className="flex min-h-0 flex-1 flex-col items-center justify-center gap-2 p-6 text-center">
        <Workflow className="size-5 text-muted-foreground" />
        <p className="text-sm font-medium">No active strategy definitions</p>
        <p className="max-w-sm text-xs leading-5 text-muted-foreground">
          An administrator must publish a governed strategy version before it
          can be previewed here.
        </p>
      </div>
    );
  return (
    <div className="min-h-0 flex-1 space-y-3 overflow-y-auto p-4">
      {catalog?.map((strategy) => {
        const isRunning = runningCode === strategy.code;
        const health = healthByCode[strategy.code];
        return (
          <section
            key={strategy.code}
            className="rounded-lg border border-border/60 bg-card p-4"
          >
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <h3 className="truncate text-sm font-semibold">
                  {strategy.name}
                </h3>
                <p className="mt-1 text-xs leading-5 text-muted-foreground">
                  {strategy.description}
                </p>
              </div>
              <Badge variant="secondary" className="shrink-0">
                v{strategy.activeVersion.versionNo}
              </Badge>
            </div>
            <div className="mt-3 rounded-md border border-border/60 bg-muted/25 p-3">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div>
                  <div className="flex items-center gap-2 text-xs font-semibold">
                    <ClipboardCheck className="size-3.5 text-muted-foreground" />
                    Today logic still valid?
                  </div>
                  <p className="mt-1 text-[11px] leading-5 text-muted-foreground">
                    {healthLoading && !health
                      ? "Assessing the latest daily snapshot…"
                      : health?.reason || "Health assessment unavailable."}
                  </p>
                </div>
                <Badge
                  variant="outline"
                  className={`shrink-0 ${healthBadgeClass(health?.status)}`}
                >
                  {health?.status ||
                    (healthLoading ? "Checking" : "Unavailable")}
                </Badge>
              </div>
              {health ? (
                <div className="mt-3 grid grid-cols-2 gap-2 border-t border-border/60 pt-3 text-[11px] sm:grid-cols-4">
                  <div>
                    <div className="text-muted-foreground">Candidates</div>
                    <div className="mt-0.5 font-mono tabular-nums">
                      {health.candidateCount.toLocaleString()}
                    </div>
                  </div>
                  <div>
                    <div className="text-muted-foreground">Density</div>
                    <div className="mt-0.5 font-mono tabular-nums">
                      {formatPercent(health.signalDensityPct)}
                    </div>
                  </div>
                  <div>
                    <div className="text-muted-foreground">Recent hit rate</div>
                    <div className="mt-0.5 font-mono tabular-nums">
                      {health.recentPerformance
                        ? formatPercent(health.recentPerformance.winRatePct)
                        : "—"}
                    </div>
                  </div>
                  <div>
                    <div className="text-muted-foreground">Recent excess</div>
                    <div
                      className={`mt-0.5 font-mono tabular-nums ${marketValueClass(health.recentPerformance?.averageExcessReturnPct)}`}
                    >
                      {health.recentPerformance
                        ? formatPercent(
                            health.recentPerformance.averageExcessReturnPct,
                          )
                        : "—"}
                    </div>
                  </div>
                </div>
              ) : null}
              {health?.recentPerformance ? (
                <p className="mt-2 text-[11px] text-muted-foreground">
                  Recent performance:{" "}
                  {health.recentPerformance.evaluatedTradeCount} trades through{" "}
                  {health.recentPerformance.asOfDate || "—"}.
                </p>
              ) : null}
              {health?.recommendedAction ? (
                <p className="mt-2 border-t border-border/60 pt-2 text-[11px] leading-5 text-muted-foreground">
                  {health.recommendedAction}
                </p>
              ) : null}
              {health?.warnings[0] ? (
                <p className="mt-2 flex items-start gap-1 text-[11px] leading-5 text-warning">
                  <AlertTriangle className="mt-0.5 size-3 shrink-0" />
                  {health.warnings[0]}
                </p>
              ) : null}
            </div>
            <p className="mt-3 border-t border-border/60 pt-3 text-[11px] leading-5 text-muted-foreground">
              {strategy.activeVersion.changeSummary}
            </p>
            <div className="mt-3 flex flex-wrap items-center justify-between gap-2">
              <span className="text-[11px] text-muted-foreground">
                Effective {strategy.activeVersion.effectiveFrom}
              </span>
              <div className="flex items-center gap-1">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => onPreview(strategy)}
                  disabled={isRunning}
                >
                  Preview rules
                </Button>
                {strategy.type === "USER_SCREENER" ? (
                  <Button variant="ghost" size="sm" onClick={() => onEdit(strategy)} disabled={isRunning}>
                    Edit rules
                  </Button>
                ) : null}
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => onBacktest(strategy)}
                  disabled={isRunning}
                >
                  <History />
                  Backtest
                </Button>
                <Button
                  variant="default"
                  size="sm"
                  onClick={() => onRun(strategy)}
                  disabled={runningCode !== null}
                >
                  {isRunning ? <Loader2 className="animate-spin" /> : <Play />}
                  {isRunning ? "Running" : "Run strategy"}
                </Button>
              </div>
            </div>
          </section>
        );
      })}
    </div>
  );
}

function CriteriaPanel({
  criteria,
  classifications,
  loading,
  runningStrategyCode,
  onChange,
  onReset,
  onRun,
  onApplyStrategy,
  onRunStrategy,
  onBacktestStrategy,
  aiScreening,
  onAiScreeningChange,
  onApplyObservatoryHints,
  onSelectObservatoryEvent,
  mode,
  onModeChange,
}: {
  criteria: Criteria;
  classifications: ScreenerClassificationOptions;
  loading: boolean;
  runningStrategyCode: string | null;
  onChange: <K extends keyof Criteria>(key: K, value: Criteria[K]) => void;
  onReset: () => void;
  onRun: () => Promise<StockScreenerResponse | null>;
  onApplyStrategy: (strategy: StockSelectionStrategy) => void;
  onRunStrategy: (strategy: StockSelectionStrategy) => void;
  onBacktestStrategy: (strategy: StockSelectionStrategy) => void;
  aiScreening: AiScreeningState;
  onAiScreeningChange: (state: AiScreeningState) => void;
  onApplyObservatoryHints: (hints: Array<{ criterion: string; value: string }>) => void;
  onSelectObservatoryEvent: (event: { code: string; name: string }, tradeDate: string) => void;
  mode: string;
  onModeChange: (mode: string) => void;
}) {
  const [strategyCatalog, setStrategyCatalog] = useState<
    StockSelectionStrategy[] | null
  >(null);
  const [strategyLoading, setStrategyLoading] = useState(false);
  const [strategyError, setStrategyError] = useState<string | null>(null);
  const [strategyHealth, setStrategyHealth] = useState<
    Record<string, StockSelectionStrategyHealthResponse>
  >({});
  const [strategyHealthLoading, setStrategyHealthLoading] = useState(false);
  const [editingStrategy, setEditingStrategy] = useState<StockSelectionStrategy | null>(null);
  const [saveStrategyOpen, setSaveStrategyOpen] = useState(false);
  const title =
    mode === "indicators"
      ? "多因子选股"
      : mode === "strategies"
        ? "Strategy screen"
          : mode === "observatory"
            ? "事件因子观察"
            : mode === "research"
              ? "Factor research"
          : "AI screen";
  const loadStrategies = useCallback(async () => {
    setStrategyLoading(true);
    setStrategyError(null);
    try {
      const response = await fetchScreenerStrategies();
      setStrategyCatalog(response.items);
      setStrategyHealthLoading(true);
      const healthResults = await Promise.all(
        response.items.map(async (strategy) => {
          try {
            return [
              strategy.code,
              await fetchScreenerStrategyHealth(
                strategy.code,
                criteria.tradeDate || undefined,
              ),
            ] as const;
          } catch {
            return null;
          }
        }),
      );
      setStrategyHealth(
        Object.fromEntries(
          healthResults.filter(
            (
              item,
            ): item is readonly [
              string,
              StockSelectionStrategyHealthResponse,
            ] => item !== null,
          ),
        ),
      );
    } catch (caught) {
      setStrategyCatalog(null);
      setStrategyError(
        caught instanceof Error
          ? caught.message
          : "Unable to load strategy definitions.",
      );
    } finally {
      setStrategyHealthLoading(false);
      setStrategyLoading(false);
    }
  }, [criteria.tradeDate]);
  const changeMode = useCallback(
    (nextMode: string) => {
      onModeChange(nextMode);
      if (
        nextMode === "strategies" &&
        strategyCatalog === null &&
        !strategyLoading &&
        strategyError === null
      )
        void loadStrategies();
    },
    [loadStrategies, onModeChange, strategyCatalog, strategyError, strategyLoading],
  );
  return (
    <section className="flex h-full min-h-0 flex-col bg-card">
      <div className="flex flex-wrap items-start justify-between gap-3 px-4 py-3">
        <div>
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <SlidersHorizontal className="size-3.5" />
            筛选条件
          </div>
          <h2 className="mt-1 text-sm font-semibold">{title}</h2>
        </div>
        {mode === "ai" ? (
          <div className="flex min-w-0 items-center gap-1 text-[11px] text-muted-foreground">
            <span className="shrink-0 font-medium">AI route</span>
            <AiBindingProviderSelect
              capability={["stock_selection_ai_intent_planning", "stock_selection_ai_screening"]}
              label="Provider"
              modelLabel="Model"
            />
          </div>
        ) : (
          <Button
            variant="ghost"
            size="icon-sm"
            aria-label="重置指标筛选条件"
            title="重置指标筛选条件"
            onClick={() => {
              onReset();
              setEditingStrategy(null);
            }}
            disabled={mode !== "indicators"}
          >
            <RotateCcw className="size-3.5" />
          </Button>
        )}
      </div>
      <Tabs
        value={mode}
        onValueChange={changeMode}
        className="flex min-h-0 flex-1 flex-col"
      >
        <TabsList
          variant="line"
          className="grid h-9 w-full shrink-0 grid-cols-5 rounded-none border-y border-border/70 px-3"
        >
          <TabsTrigger value="indicators" className="gap-1.5 px-2 text-[11px]">
            <BarChart3 className="size-3.5" aria-hidden="true" />
            指标选股
          </TabsTrigger>
          <TabsTrigger value="strategies" className="gap-1.5 px-2 text-[11px]">
            <Workflow className="size-3.5" aria-hidden="true" />
            Strategies
          </TabsTrigger>
          <TabsTrigger value="ai" className="gap-1.5 px-2 text-[11px]">
            <Bot className="size-3.5" aria-hidden="true" />
            AI screening
          </TabsTrigger>
          <TabsTrigger value="observatory" className="gap-1.5 px-2 text-[11px]">
            <Radio className="size-3.5" aria-hidden="true" />
            因子观察
          </TabsTrigger>
          <TabsTrigger value="research" className="gap-1.5 px-2 text-[11px]">
            <FlaskConical className="size-3.5" aria-hidden="true" />
            Research
          </TabsTrigger>
        </TabsList>
        <TabsContent
          value="indicators"
          className="m-0 flex min-h-0 flex-1 flex-col"
        >
          <div className="min-h-0 flex-1 space-y-5 overflow-y-auto p-4">
            <FilterSection
              icon={<Database className="size-3.5 text-muted-foreground" />}
              title="范围与分类"
            >
              <div className="grid grid-cols-2 gap-x-2 gap-y-2.5">
                <label className="min-w-0 space-y-1">
                  <span className="text-[11px] text-muted-foreground">
                    市场
                  </span>
                  <Select
                    value={criteria.universe}
                    onValueChange={(value) =>
                      onChange("universe", value as ScreenerUniverse)
                    }
                  >
                    <SelectTrigger size="sm" className="w-full">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ALL">全部 A 股</SelectItem>
                      <SelectItem value="SH">上海</SelectItem>
                      <SelectItem value="SZ">深圳</SelectItem>
                      <SelectItem value="BJ">北京</SelectItem>
                    </SelectContent>
                  </Select>
                </label>
                <label className="min-w-0 space-y-1">
                  <span className="text-[11px] text-muted-foreground">
                    板块
                  </span>
                  <Select
                    value={criteria.board}
                    onValueChange={(value) => {
                      const board = value as ScreenerBoard;
                      onChange("board", board);
                      if (board === "chinext") onChange("excludeChiNext", false);
                      if (board === "star") onChange("excludeStar", false);
                    }}
                  >
                    <SelectTrigger size="sm" className="w-full">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {BOARD_OPTIONS.map((option) => (
                        <SelectItem key={option.value} value={option.value}>
                          {option.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </label>
                <label className="min-w-0 space-y-1">
                  <span className="block truncate text-[11px] text-muted-foreground" title="通达信行业大类（THS 映射）">
                    通达信行业大类（THS 映射）
                  </span>
                  <Select
                    value={criteria.tdxIndustryCode || "ALL"}
                    onValueChange={(value) =>
                      onChange("tdxIndustryCode", value === "ALL" ? "" : value)
                    }
                  >
                    <SelectTrigger size="sm" className="w-full">
                      <SelectValue placeholder="全部行业大类" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ALL">全部通达信行业大类</SelectItem>
                      {classifications.tdxIndustryGroups.map((option) => (
                        <SelectItem key={option.code} value={option.code}>
                          {option.name} ({option.stockCount.toLocaleString()})
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </label>
                <label className="min-w-0 space-y-1">
                  <span className="block truncate text-[11px] text-muted-foreground" title="同花顺行业细分">
                    同花顺行业细分
                  </span>
                  <Select
                    value={criteria.thsIndustryCode || "ALL"}
                    onValueChange={(value) =>
                      onChange("thsIndustryCode", value === "ALL" ? "" : value)
                    }
                  >
                    <SelectTrigger size="sm" className="w-full">
                      <SelectValue placeholder="全部同花顺行业" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ALL">全部同花顺行业</SelectItem>
                      {classifications.thsIndustries.map((option) => (
                        <SelectItem key={option.code} value={option.code}>
                          {option.name} ({option.stockCount.toLocaleString()})
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </label>
                <label className="min-w-0 space-y-1">
                  <span className="text-[11px] text-muted-foreground">
                    通达信概念
                  </span>
                  <Select
                    value={criteria.conceptSectorId || "ALL"}
                    onValueChange={(value) =>
                      onChange("conceptSectorId", value === "ALL" ? "" : value)
                    }
                  >
                    <SelectTrigger size="sm" className="w-full">
                      <SelectValue placeholder="全部概念" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ALL">全部概念</SelectItem>
                      {classifications.concepts.map((option) => (
                        <SelectItem key={option.code} value={option.code}>
                          {option.name} ({option.stockCount.toLocaleString()})
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </label>
                <label className="min-w-0 space-y-1">
                  <span className="text-[11px] text-muted-foreground">
                    通达信风格
                  </span>
                  <Select
                    value={criteria.styleSectorId || "ALL"}
                    onValueChange={(value) =>
                      onChange("styleSectorId", value === "ALL" ? "" : value)
                    }
                  >
                    <SelectTrigger size="sm" className="w-full">
                      <SelectValue placeholder="全部风格" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ALL">全部风格</SelectItem>
                      {classifications.styles.map((option) => (
                        <SelectItem key={option.code} value={option.code}>
                          {option.name} ({option.stockCount.toLocaleString()})
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </label>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <label className="space-y-1">
                  <span className="text-[11px] text-muted-foreground">
                    截止日期
                  </span>
                  <Input
                    type="date"
                    value={criteria.tradeDate}
                    className="h-8 bg-muted/55 text-xs"
                    onChange={(event) =>
                      onChange("tradeDate", event.target.value)
                    }
                  />
                </label>
                <div className="grid grid-cols-3 gap-2">
                <label className="flex items-center gap-2 rounded-md bg-muted/45 px-2.5 py-2 text-xs">
                  <Checkbox
                    checked={criteria.excludeSt}
                    onCheckedChange={(checked) =>
                      onChange("excludeSt", checked === true)
                    }
                  />
                  剔除 ST
                </label>
                <label className="flex items-center gap-2 rounded-md bg-muted/45 px-2.5 py-2 text-xs">
                  <Checkbox checked={criteria.excludeChiNext} onCheckedChange={(checked) => onChange("excludeChiNext", checked === true)} />
                  剔除创业板
                </label>
                <label className="flex items-center gap-2 rounded-md bg-muted/45 px-2.5 py-2 text-xs">
                  <Checkbox checked={criteria.excludeStar} onCheckedChange={(checked) => onChange("excludeStar", checked === true)} />
                  剔除科创板
                </label>
                </div>
              </div>
            </FilterSection>
            <FilterSection
              icon={<BarChart3 className="size-3.5 text-muted-foreground" />}
              title="价格与波动"
            >
              <RangeInputs
                criteria={criteria}
                start="minPrice"
                end="maxPrice"
                label="收盘价"
                unit="元"
                onChange={onChange}
              />
              <RangeInputs
                criteria={criteria}
                start="minChangePct"
                end="maxChangePct"
                label="当日涨跌幅"
                unit="%"
                onChange={onChange}
              />
              <RangeInputs
                criteria={criteria}
                start="minAmplitudePct"
                end="maxAmplitudePct"
                label="振幅"
                unit="%"
                onChange={onChange}
              />
            </FilterSection>
            <FilterSection
              icon={<BarChart3 className="size-3.5 text-muted-foreground" />}
              title="量价形态"
            >
              <div className="rounded-md border border-border/60 bg-muted/30 p-2.5 text-[11px] leading-5 text-muted-foreground">
                前序交易日成交量须持续缩量，当日成交量与前序均量比较；阳线实体为
                <span className="font-mono">（收盘价 − 开盘价）/ 昨收</span>。
              </div>
              <div className="grid grid-cols-2 gap-2">
                <label className="min-w-0 space-y-1">
                  <span className="block text-[11px] text-muted-foreground">连续缩量天数</span>
                  <Input type="number" min={2} max={10} step={1} value={criteria.volumeContractionDays}
                    placeholder="例如 3" className="h-8 bg-muted/55 text-xs"
                    onChange={(event) => onChange("volumeContractionDays", event.target.value)} />
                </label>
                <label className="min-w-0 space-y-1">
                  <span className="block text-[11px] text-muted-foreground">放量倍数下限</span>
                  <Input type="number" min={1} max={20} step="any" value={criteria.minBreakoutVolumeMultiple}
                    placeholder="例如 3" className="h-8 bg-muted/55 text-xs"
                    onChange={(event) => onChange("minBreakoutVolumeMultiple", event.target.value)} />
                </label>
                <label className="min-w-0 space-y-1">
                  <span className="block text-[11px] text-muted-foreground">放量倍数上限</span>
                  <Input type="number" min={1} max={20} step="any" value={criteria.maxBreakoutVolumeMultiple}
                    placeholder="例如 4.5" className="h-8 bg-muted/55 text-xs"
                    onChange={(event) => onChange("maxBreakoutVolumeMultiple", event.target.value)} />
                </label>
                <label className="min-w-0 space-y-1">
                  <span className="block text-[11px] text-muted-foreground">阳线实体下限</span>
                  <Input type="number" min={0} max={30} step="any" value={criteria.minBullishBodyPct}
                    placeholder="例如 5" className="h-8 bg-muted/55 text-xs"
                    onChange={(event) => onChange("minBullishBodyPct", event.target.value)} />
                </label>
              </div>
              <p className="text-[11px] leading-5 text-muted-foreground">
                放量使用区间匹配。示例：连续缩量至少 3 日，放量在 2.1 至 4.5 倍之间，且阳线实体不低于 5%。
              </p>
            </FilterSection>
            <FilterSection
              icon={<TrendingUp className="size-3.5 text-muted-foreground" />}
              title="技术趋势与突破"
            >
              <div className="grid grid-cols-2 gap-2">
                <label className="min-w-0 space-y-1">
                  <span className="block text-[11px] text-muted-foreground">收盘价高于 MA20</span>
                  <Input type="number" min={-100} max={500} step="any" value={criteria.minCloseAboveMa20Pct}
                    placeholder="例如 3" className="h-8 bg-muted/55 text-xs"
                    onChange={(event) => onChange("minCloseAboveMa20Pct", event.target.value)} />
                </label>
                <label className="min-w-0 space-y-1">
                  <span className="block text-[11px] text-muted-foreground">MA5 高于 MA20</span>
                  <Input type="number" min={-100} max={300} step="any" value={criteria.minMa5AboveMa20Pct}
                    placeholder="例如 1" className="h-8 bg-muted/55 text-xs"
                    onChange={(event) => onChange("minMa5AboveMa20Pct", event.target.value)} />
                </label>
                <label className="min-w-0 space-y-1">
                  <span className="block text-[11px] text-muted-foreground">突破近高回看天数</span>
                  <Input type="number" min={5} max={20} step={1} value={criteria.breakoutHighDays}
                    placeholder="例如 20" className="h-8 bg-muted/55 text-xs"
                    onChange={(event) => onChange("breakoutHighDays", event.target.value)} />
                </label>
                <label className="min-w-0 space-y-1">
                  <span className="block text-[11px] text-muted-foreground">ATR20 上限</span>
                  <Input type="number" min={0.01} max={50} step="any" value={criteria.maxAtrPct20}
                    placeholder="例如 4" className="h-8 bg-muted/55 text-xs"
                    onChange={(event) => onChange("maxAtrPct20", event.target.value)} />
                </label>
                <label className="min-w-0 space-y-1">
                  <span className="block text-[11px] text-muted-foreground">连续阳线天数</span>
                  <Input type="number" min={1} max={10} step={1} value={criteria.minConsecutiveBullishDays}
                    placeholder="例如 2" className="h-8 bg-muted/55 text-xs"
                    onChange={(event) => onChange("minConsecutiveBullishDays", event.target.value)} />
                </label>
                <label className="min-w-0 space-y-1">
                  <span className="block text-[11px] text-muted-foreground">成交量相对 MA5 下限</span>
                  <Input type="number" min={0.1} max={20} step="any" value={criteria.minVolumeVsMa5Multiple}
                    placeholder="例如 1.5" className="h-8 bg-muted/55 text-xs"
                    onChange={(event) => onChange("minVolumeVsMa5Multiple", event.target.value)} />
                </label>
                <label className="min-w-0 space-y-1">
                  <span className="block text-[11px] text-muted-foreground">成交量相对 MA5 上限</span>
                  <Input type="number" min={0.1} max={20} step="any" value={criteria.maxVolumeVsMa5Multiple}
                    placeholder="例如 4.5" className="h-8 bg-muted/55 text-xs"
                    onChange={(event) => onChange("maxVolumeVsMa5Multiple", event.target.value)} />
                </label>
              </div>
              <p className="text-[11px] leading-5 text-muted-foreground">
                仅使用可用的日线 OHLCV 历史数据，启用的条件将同时作为硬性筛选。
              </p>
            </FilterSection>
            <FilterSection
              icon={<CandlestickChart className="size-3.5 text-muted-foreground" />}
              title="K 线形态"
            >
              <label className="space-y-1">
                <span className="block text-[11px] text-muted-foreground">日线形态</span>
                <Select value={criteria.candlestickPattern || "ALL"} onValueChange={(value) => onChange("candlestickPattern", value === "ALL" ? "" : value)}>
                  <SelectTrigger size="sm"><SelectValue placeholder="不限形态" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ALL">不限形态</SelectItem>
                    <SelectItem value="bullish_engulfing">多头吞没</SelectItem>
                    <SelectItem value="bearish_engulfing">空头吞没</SelectItem>
                    <SelectItem value="hammer">锤头</SelectItem>
                    <SelectItem value="shooting_star">射击之星</SelectItem>
                    <SelectItem value="doji">十字星</SelectItem>
                    <SelectItem value="gap_up">向上跳空</SelectItem>
                    <SelectItem value="gap_down">向下跳空</SelectItem>
                    <SelectItem value="inside_bar">内包线</SelectItem>
                  </SelectContent>
                </Select>
              </label>
              <p className="text-[11px] leading-5 text-muted-foreground">形态与量价、均线条件可叠加；只使用与结果快照同一交易日的 OHLCV 日线事实。</p>
            </FilterSection>
            <FilterSection
              icon={<TrendingUp className="size-3.5 text-muted-foreground" />}
              title="动量表现"
            >
              <RangeInputs
                criteria={criteria}
                start="minReturnPct5d"
                end="maxReturnPct5d"
                label="5日涨幅"
                unit="%"
                onChange={onChange}
              />
              <RangeInputs
                criteria={criteria}
                start="minReturnPct10d"
                end="maxReturnPct10d"
                label="10日涨幅"
                unit="%"
                onChange={onChange}
              />
              <RangeInputs
                criteria={criteria}
                start="minReturnPct30d"
                end="maxReturnPct30d"
                label="30日涨幅"
                unit="%"
                onChange={onChange}
              />
              <RangeInputs
                criteria={criteria}
                start="minReturnPct60d"
                end="maxReturnPct60d"
                label="60日涨幅"
                unit="%"
                onChange={onChange}
              />
              <RangeInputs
                criteria={criteria}
                start="minReturnPct252d"
                end="maxReturnPct252d"
                label="252日涨幅"
                unit="%"
                onChange={onChange}
              />
            </FilterSection>
            <FilterSection
              icon={<Database className="size-3.5 text-muted-foreground" />}
              title="流动性、市值与资金"
            >
              <label className="space-y-1">
                <span className="text-[11px] text-muted-foreground">
                  最低成交额（元）
                </span>
                <Input
                  type="number"
                  step="any"
                  value={criteria.minAmount}
                  placeholder="不限"
                  className="h-8 bg-muted/55 text-xs"
                  onChange={(event) =>
                    onChange("minAmount", event.target.value)
                  }
                />
              </label>
              <RangeInputs
                criteria={criteria}
                start="minTurnoverPct"
                end="maxTurnoverPct"
                label="换手率"
                unit="%"
                onChange={onChange}
              />
              <RangeInputs
                criteria={criteria}
                start="minVolumeRatio"
                end="maxVolumeRatio"
                label="量比"
                onChange={onChange}
              />
              <RangeInputs
                criteria={criteria}
                start="minMarketCap"
                end="maxMarketCap"
                label="市值"
                unit="元"
                onChange={onChange}
              />
              <label className="space-y-1">
                <span className="text-[11px] text-muted-foreground">
                  主力净流入下限（元）
                </span>
                <Input
                  type="number"
                  step="any"
                  value={criteria.minMainNet}
                  placeholder="不限；支持负值"
                  className="h-8 bg-muted/55 text-xs"
                  onChange={(event) =>
                    onChange("minMainNet", event.target.value)
                  }
                />
              </label>
              <label className="space-y-1">
                <span className="text-[11px] text-muted-foreground">
                  5日主力净流入下限（元）
                </span>
                <Input
                  type="number"
                  step="any"
                  value={criteria.minMainNet5d}
                  placeholder="不限；支持负值"
                  className="h-8 bg-muted/55 text-xs"
                  onChange={(event) =>
                    onChange("minMainNet5d", event.target.value)
                  }
                />
              </label>
            </FilterSection>
            <FilterSection
              icon={<Database className="size-3.5 text-muted-foreground" />}
              title="估值（部分覆盖）"
            >
              <RangeInputs
                criteria={criteria}
                start="minPe"
                end="maxPe"
                label="市盈率（PE）"
                onChange={onChange}
              />
              <div className="text-[11px] leading-5 text-muted-foreground">
                日线 THS 快照约覆盖 4,105 只股票的市盈率。ROE、盈利增长等财务因子仍需等待全市场财报覆盖后提供。
              </div>
            </FilterSection>
          </div>
          <div className="flex gap-2 border-t border-border p-4">
            <Button
              type="button"
              variant="secondary"
              className="min-w-0 flex-1"
              onClick={() => setSaveStrategyOpen(true)}
              disabled={loading}
            >
              <Save />
              {editingStrategy ? "更新策略" : "保存为策略"}
            </Button>
            <Button
              type="button"
              variant="default"
              className="min-w-0 flex-1"
              onClick={onRun}
              disabled={loading}
            >
              {loading ? <Loader2 className="animate-spin" /> : <Play />}
              {loading ? "正在选股" : "执行选股"}
            </Button>
          </div>
        </TabsContent>
        <TabsContent value="strategies" className="m-0 flex min-h-0 flex-1">
          <StrategyCatalogPanel
            catalog={strategyCatalog}
            loading={strategyLoading}
            error={strategyError}
            runningCode={runningStrategyCode}
            healthByCode={strategyHealth}
            healthLoading={strategyHealthLoading}
            onRetry={() => void loadStrategies()}
            onPreview={(strategy) => {
              onApplyStrategy(strategy);
              setEditingStrategy(null);
              onModeChange("indicators");
            }}
            onEdit={(strategy) => {
              onApplyStrategy(strategy);
              setEditingStrategy(strategy);
              onModeChange("indicators");
            }}
            onRun={onRunStrategy}
            onBacktest={onBacktestStrategy}
          />
        </TabsContent>
        <TabsContent value="ai" className="m-0 flex min-h-0 flex-1">
          <AiScreeningPanel
            criteria={criteria}
            loading={loading}
            onRun={onRun}
            screeningState={aiScreening}
            onScreeningStateChange={onAiScreeningChange}
            onApplyCriteria={(resolved) => {
              Object.entries(resolved).forEach(([key, value]) => {
                if (!(key in DEFAULT_CRITERIA) || value === undefined || value === null) return;
                const field = key as keyof Criteria;
                const next = typeof DEFAULT_CRITERIA[field] === "boolean" ? value === true : String(value);
                onChange(field, next as Criteria[typeof field]);
              });
            }}
          />
        </TabsContent>
        <TabsContent value="observatory" className="m-0 flex min-h-0 flex-1">
          <FactorObservatoryTab
            tradeDate={criteria.tradeDate}
            onTradeDateChange={(value) => onChange("tradeDate", value)}
            onApplyHints={onApplyObservatoryHints}
            onSelectEvent={onSelectObservatoryEvent}
          />
        </TabsContent>
        <TabsContent value="research" className="m-0 flex min-h-0 flex-1" />
      </Tabs>
      {saveStrategyOpen ? (
        <StrategySaveDialog
          key={`${editingStrategy?.code || "new"}-${Object.keys(strategyCriteria(criteria)).join("-")}`}
          open={saveStrategyOpen}
          onOpenChange={setSaveStrategyOpen}
          criteria={strategyCriteria(criteria)}
          strategy={editingStrategy}
          onSaved={() => {
            setEditingStrategy(null);
            onModeChange("strategies");
            void loadStrategies();
          }}
        />
      ) : null}
    </section>
  );
}

function ResultsSkeleton() {
  return (
    <div className="space-y-3 p-4">
      {Array.from({ length: 10 }, (_, index) => (
        <div
          key={index}
          className="grid grid-cols-[1.2fr_repeat(6,minmax(72px,1fr))] gap-3"
        >
          <Skeleton className="h-9" />
          {Array.from({ length: 6 }, (_, cell) => (
            <Skeleton key={cell} className="h-9" />
          ))}
        </div>
      ))}
    </div>
  );
}

function TradeReviewPanelLoading({
  onOpenChange,
}: {
  onOpenChange: (open: boolean) => void;
}) {
  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className="!h-[96vh] !w-[96vw] !max-w-[96vw] gap-0 overflow-hidden rounded-xl border-border p-0">
        <DialogTitle className="sr-only">Loading Trade Review</DialogTitle>
        <DialogDescription className="sr-only">
          Loading the selected security's trade review workspace.
        </DialogDescription>
        <div className="flex h-full min-h-0 flex-col bg-background">
          <div className="flex min-h-[52px] items-center border-b border-border/70 px-4">
            <div>
              <div className="text-sm font-semibold">Trade Review</div>
              <div className="mt-1 text-xs text-muted-foreground">
                Loading the selected security's replay workspace…
              </div>
            </div>
          </div>
          <div className="grid min-h-0 flex-1 grid-cols-[minmax(0,2fr)_minmax(300px,1fr)] gap-px bg-border">
            <div className="bg-background p-4">
              <Skeleton className="h-full w-full" />
            </div>
            <div className="space-y-3 bg-background p-4">
              <Skeleton className="h-8 w-full" />
              {Array.from({ length: 6 }, (_, index) => (
                <Skeleton key={index} className="h-16 w-full" />
              ))}
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

function formatEvidenceValue(value: unknown): string {
  if (value == null) return "—";
  if (typeof value === "number") return formatNumber(value, 4);
  if (typeof value === "boolean") return value ? "Yes" : "No";
  return String(value);
}

function StrategyEvidenceSheet({
  candidate,
  open,
  onOpenChange,
}: {
  candidate: StockSelectionStrategyCandidate | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side="right"
        className="flex h-full w-full flex-col gap-0 p-0 sm:max-w-xl"
      >
        <SheetHeader className="border-b border-border/60 px-5 py-4 pr-12">
          <SheetTitle className="text-base">
            {candidate
              ? `${candidate.stockName || candidate.stockCode} · selection evidence`
              : "Selection evidence"}
          </SheetTitle>
          <SheetDescription>
            {candidate
              ? `Rank ${candidate.rank} · Score ${formatNumber(candidate.score)} · all values are frozen with this strategy run.`
              : ""}
          </SheetDescription>
        </SheetHeader>
        {candidate ? (
          <div className="min-h-0 flex-1 space-y-5 overflow-y-auto p-5">
            <section className="rounded-md bg-muted/45 p-4">
              <div className="text-xs font-medium text-muted-foreground">
                Why this security matched
              </div>
              <p className="mt-2 text-sm leading-6">
                {candidate.reasonSummary}
              </p>
            </section>
            <section>
              <h3 className="text-sm font-semibold">Rule evidence</h3>
              <div className="mt-3 divide-y divide-border/60 rounded-lg border border-border/60">
                <div className="grid grid-cols-[minmax(0,1fr)_80px_88px] gap-3 bg-muted/45 px-3 py-2 text-[11px] font-medium text-muted-foreground">
                  <span>Rule</span>
                  <span className="text-right">Expected</span>
                  <span className="text-right">Actual</span>
                </div>
                {candidate.evidence.map((item) => (
                  <div
                    key={item.code}
                    className="grid grid-cols-[minmax(0,1fr)_80px_88px] gap-3 px-3 py-3 text-xs"
                  >
                    <div className="min-w-0">
                      <div className="truncate font-medium">{item.label}</div>
                      <div className="mt-1 font-mono text-[10px] text-muted-foreground">
                        {item.operator}
                      </div>
                    </div>
                    <div className="text-right font-mono tabular-nums">
                      {formatEvidenceValue(item.expectedValue)}
                    </div>
                    <div className="text-right">
                      <span
                        className={`font-mono tabular-nums ${item.matched ? "text-emerald-700" : "text-destructive"}`}
                      >
                        {formatEvidenceValue(item.actualValue)}
                      </span>
                      <span className="mt-1 block text-[10px] text-muted-foreground">
                        {item.matched ? "Matched" : "Not matched"}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </section>
            <section>
              <h3 className="text-sm font-semibold">Frozen market snapshot</h3>
              <div className="mt-3 grid grid-cols-2 gap-3 text-xs">
                {Object.entries(candidate.snapshot).map(([key, value]) => (
                  <div key={key} className="min-w-0 rounded-md bg-muted/45 p-3">
                    <div className="truncate text-[11px] text-muted-foreground">
                      {key}
                    </div>
                    <div className="mt-1 truncate font-mono tabular-nums">
                      {formatEvidenceValue(value)}
                    </div>
                  </div>
                ))}
              </div>
            </section>
          </div>
        ) : null}
      </SheetContent>
    </Sheet>
  );
}

function AddWatchlistDialog({
  candidate,
  onOpenChange,
  onSaved,
}: {
  candidate: StockSelectionStrategyCandidate | null;
  onOpenChange: (open: boolean) => void;
  onSaved: (item: StockSelectionWatchlistItem) => void;
}) {
  const [watchlistName, setWatchlistName] = useState("My watchlist");
  const [attentionReason, setAttentionReason] = useState("");
  const [invalidationCondition, setInvalidationCondition] = useState("");
  const [nextReviewDate, setNextReviewDate] = useState("");
  const [saving, setSaving] = useState(false);
  const save = async () => {
    if (!candidate) return;
    setSaving(true);
    try {
      const item = await addStockSelectionWatchlistItem({
        candidateId: candidate.id,
        watchlistName,
        attentionReason,
        invalidationCondition,
        nextReviewDate,
      });
      onSaved(item);
      notification.success({
        title: "Added to watchlist",
        description: `${candidate.stockName || candidate.stockCode} is now being tracked.`,
      });
      onOpenChange(false);
      setWatchlistName("My watchlist");
      setAttentionReason("");
      setInvalidationCondition("");
      setNextReviewDate("");
    } catch (caught) {
      notification.danger({
        title: "Unable to add watchlist item",
        description: caught instanceof Error ? caught.message : String(caught),
      });
    } finally {
      setSaving(false);
    }
  };
  return (
    <Dialog open={candidate !== null} onOpenChange={onOpenChange}>
      <DialogContent className="gap-0 overflow-hidden p-0 sm:max-w-lg">
        <DialogTitle className="border-b border-border/60 px-5 py-4 text-base">
          Add to watchlist
        </DialogTitle>
        <DialogDescription className="sr-only">
          Record the reason, invalidation condition, and next review date for
          this strategy candidate.
        </DialogDescription>
        {candidate ? (
          <form
            className="space-y-4 p-5"
            onSubmit={(event) => {
              event.preventDefault();
              void save();
            }}
          >
            <div className="rounded-md bg-muted/45 px-3 py-2">
              <div className="text-sm font-medium">
                {candidate.stockName || candidate.stockCode}
              </div>
              <div className="mt-0.5 font-mono text-[11px] text-muted-foreground">
                {candidate.stockCode} · strategy candidate #{candidate.rank}
              </div>
            </div>
            <label className="block space-y-1.5">
              <span className="text-xs font-medium">Watchlist name</span>
              <Input
                value={watchlistName}
                onChange={(event) => setWatchlistName(event.target.value)}
                placeholder="My watchlist"
                required
                maxLength={128}
                className="h-9 bg-muted/55 text-sm"
              />
            </label>
            <label className="block space-y-1.5">
              <span className="text-xs font-medium">Attention reason</span>
              <Textarea
                value={attentionReason}
                onChange={(event) => setAttentionReason(event.target.value)}
                placeholder="What keeps this security in scope?"
                required
                maxLength={1000}
                className="min-h-20 resize-y text-sm"
              />
            </label>
            <label className="block space-y-1.5">
              <span className="text-xs font-medium">
                Invalidation condition
              </span>
              <Textarea
                value={invalidationCondition}
                onChange={(event) =>
                  setInvalidationCondition(event.target.value)
                }
                placeholder="What would invalidate the current thesis?"
                maxLength={1000}
                className="min-h-20 resize-y text-sm"
              />
            </label>
            <label className="block space-y-1.5">
              <span className="text-xs font-medium">Next review date</span>
              <Input
                type="date"
                value={nextReviewDate}
                onChange={(event) => setNextReviewDate(event.target.value)}
                className="h-9 bg-muted/55 text-sm"
              />
            </label>
            <div className="flex justify-end gap-2 border-t border-border/60 pt-4">
              <Button
                type="button"
                variant="secondary"
                onClick={() => onOpenChange(false)}
                disabled={saving}
              >
                Cancel
              </Button>
              <Button type="submit" variant="default" disabled={saving}>
                {saving ? (
                  <Loader2 className="animate-spin" />
                ) : (
                  <BookmarkPlus />
                )}
                {saving ? "Adding" : "Add to watchlist"}
              </Button>
            </div>
          </form>
        ) : null}
      </DialogContent>
    </Dialog>
  );
}

function WatchlistReviewDialog({
  item,
  onOpenChange,
  onSaved,
}: {
  item: StockSelectionWatchlistItem | null;
  onOpenChange: (open: boolean) => void;
  onSaved: (item: StockSelectionWatchlistItem) => void;
}) {
  const [decision, setDecision] =
    useState<StockSelectionWatchlistDecision>("CONTINUE");
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);
  const save = async () => {
    if (!item) return;
    setSaving(true);
    try {
      const updated = await reviewStockSelectionWatchlistItem(
        item.id,
        decision,
        note,
      );
      onSaved(updated);
      notification.success({
        title: "Watchlist review saved",
        description: `${item.stockName || item.stockCode} is now ${updated.status}.`,
      });
      onOpenChange(false);
      setNote("");
    } catch (caught) {
      notification.danger({
        title: "Unable to save watchlist review",
        description: caught instanceof Error ? caught.message : String(caught),
      });
    } finally {
      setSaving(false);
    }
  };
  return (
    <Dialog open={item !== null} onOpenChange={onOpenChange}>
      <DialogContent className="gap-0 overflow-hidden p-0 sm:max-w-md">
        <DialogTitle className="border-b border-border/60 px-5 py-4 text-base">
          Review watchlist item
        </DialogTitle>
        <DialogDescription className="sr-only">
          Record the next decision for this watchlist item.
        </DialogDescription>
        {item ? (
          <form
            className="space-y-4 p-5"
            onSubmit={(event) => {
              event.preventDefault();
              void save();
            }}
          >
            <div>
              <div className="text-sm font-medium">
                {item.stockName || item.stockCode}
              </div>
              <div className="mt-0.5 font-mono text-[11px] text-muted-foreground">
                {item.stockCode} · current status {item.status}
              </div>
            </div>
            <label className="block space-y-1.5">
              <span className="text-xs font-medium">Decision</span>
              <Select
                value={decision}
                onValueChange={(value) =>
                  setDecision(value as StockSelectionWatchlistDecision)
                }
              >
                <SelectTrigger className="h-9">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="CONTINUE">Continue watching</SelectItem>
                  <SelectItem value="PAUSE">Pause</SelectItem>
                  <SelectItem value="INVALIDATE">Invalidate thesis</SelectItem>
                  <SelectItem value="ARCHIVE">Archive</SelectItem>
                </SelectContent>
              </Select>
            </label>
            <label className="block space-y-1.5">
              <span className="text-xs font-medium">Review note</span>
              <Textarea
                value={note}
                onChange={(event) => setNote(event.target.value)}
                placeholder="What changed since the last review?"
                maxLength={1000}
                className="min-h-20 resize-y text-sm"
              />
            </label>
            <div className="flex justify-end gap-2 border-t border-border/60 pt-4">
              <Button
                type="button"
                variant="secondary"
                onClick={() => onOpenChange(false)}
                disabled={saving}
              >
                Cancel
              </Button>
              <Button type="submit" variant="default" disabled={saving}>
                {saving ? (
                  <Loader2 className="animate-spin" />
                ) : (
                  <ClipboardCheck />
                )}
                {saving ? "Saving" : "Save review"}
              </Button>
            </div>
          </form>
        ) : null}
      </DialogContent>
    </Dialog>
  );
}

const WATCHLIST_PERFORMANCE_CHART_CONFIG = {
  returnSinceEntryPct: { label: "Since entry", color: "var(--chart-1)" },
  benchmarkReturnPct: { label: "Benchmark", color: "var(--chart-2)" },
  excessReturnPct: { label: "Excess", color: "var(--chart-4)" },
} satisfies ChartConfig;

function WatchlistPerformanceDialog({
  item,
  onOpenChange,
}: {
  item: StockSelectionWatchlistItem | null;
  onOpenChange: (open: boolean) => void;
}) {
  const [history, setHistory] =
    useState<StockSelectionWatchlistPerformanceHistoryResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    if (!item) return;
    let active = true;
    void fetchStockSelectionWatchlistPerformanceHistory(item.id)
      .then((response) => {
        if (active) setHistory(response);
      })
      .catch((caught) => {
        if (active) {
          setError(
            caught instanceof Error
              ? caught.message
              : "Unable to load return observation.",
          );
        }
      })
    return () => {
      active = false;
    };
  }, [item]);
  const loading = item !== null && history === null && error === null;
  const chartData = useMemo(
    () =>
      history?.points.map((point) => ({
        date: point.tradeDate.slice(5),
        returnSinceEntryPct: point.returnSinceEntryPct,
        benchmarkReturnPct: point.benchmarkReturnPct,
        excessReturnPct: point.excessReturnPct,
      })) || [],
    [history],
  );
  return (
    <Dialog open={item !== null} onOpenChange={onOpenChange}>
      <DialogContent className="gap-0 overflow-hidden p-0 sm:max-w-4xl">
        <DialogTitle className="border-b border-border/60 px-5 py-4 text-base">
          Return observation
        </DialogTitle>
        <DialogDescription className="sr-only">
          Daily return and benchmark excess since this candidate entered the watchlist.
        </DialogDescription>
        {item ? (
          <div className="space-y-4 p-5">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="min-w-0">
                <div className="truncate text-sm font-semibold">
                  {item.stockName || item.stockCode}
                </div>
                <div className="mt-0.5 font-mono text-[11px] text-muted-foreground">
                  {item.market ? `${item.market}:` : ""}
                  {item.stockCode} · entry {history?.entryTradeDate || item.entryTradeDate || "—"} · {history?.entryPrice ?? item.entryPrice ?? "—"}
                </div>
              </div>
              <Badge variant="secondary">SH:000300 benchmark</Badge>
            </div>
            {loading ? (
              <Skeleton className="h-64 w-full" />
            ) : error ? (
              <div className="flex min-h-64 flex-col items-center justify-center gap-2 rounded-md border border-dashed border-border/70 p-5 text-center">
                <AlertTriangle className="size-5 text-warning" />
                <p className="text-sm font-medium">Return observation unavailable</p>
                <p className="text-xs text-muted-foreground">{error}</p>
              </div>
            ) : chartData.length === 0 ? (
              <div className="flex min-h-64 flex-col items-center justify-center gap-2 rounded-md border border-dashed border-border/70 p-5 text-center">
                <BarChart3 className="size-5 text-muted-foreground" />
                <p className="text-sm font-medium">No historical observation</p>
                <p className="text-xs text-muted-foreground">
                  Daily closes are not available for this frozen entry context.
                </p>
              </div>
            ) : (
              <ChartContainer config={WATCHLIST_PERFORMANCE_CHART_CONFIG} className="h-64 w-full aspect-auto">
                <LineChart data={chartData} margin={{ left: 0, right: 8, top: 8, bottom: 0 }}>
                  <CartesianGrid vertical={false} />
                  <XAxis dataKey="date" tickLine={false} axisLine={false} tickMargin={8} minTickGap={28} />
                  <ChartTooltip content={<ChartTooltipContent labelKey="date" />} />
                  <Line type="monotone" dataKey="returnSinceEntryPct" stroke="var(--color-returnSinceEntryPct)" strokeWidth={2} dot={false} />
                  <Line type="monotone" dataKey="benchmarkReturnPct" stroke="var(--color-benchmarkReturnPct)" strokeWidth={1.5} dot={false} />
                  <Line type="monotone" dataKey="excessReturnPct" stroke="var(--color-excessReturnPct)" strokeWidth={1.5} strokeDasharray="4 3" dot={false} />
                </LineChart>
              </ChartContainer>
            )}
            {history?.warnings.length ? (
              <div className="rounded-md bg-muted/45 px-3 py-2 text-xs text-muted-foreground">
                {history.warnings.join(" · ")}
              </div>
            ) : null}
            <div className="flex items-center justify-between border-t border-border/60 pt-3 text-[11px] text-muted-foreground">
              <span>{history?.points.length || 0} trading sessions</span>
              <span>{history?.dataSource || "daily close source unavailable"}</span>
            </div>
          </div>
        ) : null}
      </DialogContent>
    </Dialog>
  );
}

function WatchlistSheet({
  open,
  items,
  performance,
  performanceError,
  performanceWarnings,
  performanceSource,
  loading,
  error,
  onOpenChange,
  onRefresh,
  onSelect,
  onUpdated,
}: {
  open: boolean;
  items: StockSelectionWatchlistItem[];
  performance: Record<string, StockSelectionWatchlistPerformance>;
  performanceError: string | null;
  performanceWarnings: string[];
  performanceSource: string | null;
  loading: boolean;
  error: string | null;
  onOpenChange: (open: boolean) => void;
  onRefresh: () => void;
  onSelect: (item: StockSelectionWatchlistItem) => void;
  onUpdated: (item: StockSelectionWatchlistItem) => void;
}) {
  const [reviewing, setReviewing] =
    useState<StockSelectionWatchlistItem | null>(null);
  const [observing, setObserving] =
    useState<StockSelectionWatchlistItem | null>(null);
  const groupedItems = useMemo(() => {
    const groups = new Map<string, StockSelectionWatchlistItem[]>();
    for (const item of items) {
      const name = item.watchlistName || "My watchlist";
      groups.set(name, [...(groups.get(name) || []), item]);
    }
    return Array.from(groups.entries());
  }, [items]);
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side="right"
        className="flex h-full w-full flex-col gap-0 p-0 sm:max-w-xl"
      >
        <SheetHeader className="border-b border-border/60 px-5 py-4 pr-12">
          <SheetTitle className="text-base">Watchlist</SheetTitle>
          <SheetDescription>
            Human-reviewed candidates with explicit reasons, invalidation
            conditions, and lifecycle status.
          </SheetDescription>
        </SheetHeader>
        <div className="min-h-0 flex-1 overflow-y-auto">
          {loading ? (
            <div className="space-y-3 p-5">
              {Array.from({ length: 5 }, (_, index) => (
                <Skeleton key={index} className="h-24 w-full" />
              ))}
            </div>
          ) : error ? (
            <div className="flex h-full flex-col items-center justify-center gap-3 p-6 text-center">
              <AlertTriangle className="size-5 text-warning" />
              <p className="text-sm font-medium">Watchlist unavailable</p>
              <p className="text-xs text-muted-foreground">{error}</p>
              <Button variant="secondary" size="sm" onClick={onRefresh}>
                Retry
              </Button>
            </div>
          ) : items.length === 0 ? (
            <div className="flex h-full flex-col items-center justify-center gap-2 p-6 text-center">
              <BookmarkPlus className="size-5 text-muted-foreground" />
              <p className="text-sm font-medium">No tracked candidates</p>
              <p className="max-w-sm text-xs leading-5 text-muted-foreground">
                Run a governed strategy, then add a candidate with its attention
                reason and invalidation condition.
              </p>
            </div>
          ) : (
            <div className="divide-y divide-border/60">
              {performanceError ? (
                <div className="bg-muted/35 px-5 py-2 text-xs text-muted-foreground">
                  Return observation unavailable: {performanceError}
                </div>
              ) : null}
              {performanceWarnings.map((warning) => (
                <div key={warning} className="bg-muted/35 px-5 py-2 text-xs text-muted-foreground">
                  {warning}
                </div>
              ))}
              {groupedItems.map(([watchlistName, group]) => (
                <section key={watchlistName}>
                  <div className="flex items-center justify-between border-b border-border/60 bg-muted/35 px-5 py-2">
                    <span className="text-xs font-semibold">{watchlistName}</span>
                    <Badge variant="secondary">{group.length}</Badge>
                  </div>
                  {group.map((item) => {
                    const observation = performance[String(item.id)];
                    return (
                <article key={item.id} className="space-y-3 px-5 py-4">
                  <div className="flex items-start justify-between gap-3">
                    <button
                      type="button"
                      className="min-w-0 text-left hover:underline"
                      onClick={() => onSelect(item)}
                    >
                      <div className="truncate text-sm font-semibold">
                        {item.stockName || item.stockCode}
                      </div>
                      <div className="mt-0.5 font-mono text-[11px] text-muted-foreground">
                        {item.market ? `${item.market}:` : ""}
                        {item.stockCode}
                      </div>
                    </button>
                    <Badge variant="secondary" className="shrink-0">
                      {item.status}
                    </Badge>
                  </div>
                  <div className="space-y-2 text-xs">
                    <div>
                      <span className="font-medium">Attention: </span>
                      <span className="text-muted-foreground">
                        {item.attentionReason}
                      </span>
                    </div>
                    {item.invalidationCondition ? (
                      <div>
                        <span className="font-medium">Invalidate: </span>
                        <span className="text-muted-foreground">
                          {item.invalidationCondition}
                        </span>
                      </div>
                    ) : null}
                    <div className="flex flex-wrap gap-x-3 gap-y-1 text-[11px] text-muted-foreground">
                      <span>
                        Entry {item.entryTradeDate || "—"} · {item.entryPrice ?? "—"}
                      </span>
                      <span>
                        Next review {item.nextReviewDate || "not scheduled"}
                      </span>
                      <span>
                        Updated{" "}
                        {item.updatedAt
                          ? new Date(item.updatedAt).toLocaleString()
                          : "—"}
                      </span>
                    </div>
                    {observation ? (
                      <div className="grid grid-cols-3 gap-2 rounded-md bg-muted/45 px-3 py-2 text-[11px]">
                        <div>
                          <div className="text-muted-foreground">Since entry</div>
                          <div className={`mt-0.5 font-mono tabular-nums ${marketValueClass(observation.returnSinceEntryPct)}`}>
                            {formatPercent(observation.returnSinceEntryPct)}
                          </div>
                        </div>
                        <div>
                          <div className="text-muted-foreground">Excess</div>
                          <div className={`mt-0.5 font-mono tabular-nums ${marketValueClass(observation.excessReturnPct)}`}>
                            {formatPercent(observation.excessReturnPct)}
                          </div>
                        </div>
                        <div>
                          <div className="text-muted-foreground">Latest close</div>
                          <div className="mt-0.5 font-mono tabular-nums">
                            {observation.latestClose ?? "—"} {observation.latestTradeDate || ""}
                          </div>
                        </div>
                      </div>
                    ) : null}
                  </div>
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-[11px] text-muted-foreground">
                      {item.reviews.length} recorded review
                      {item.reviews.length === 1 ? "" : "s"}
                    </span>
                    <div className="flex items-center gap-2">
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => setObserving(item)}
                      >
                        <BarChart3 className="size-3.5" />
                        Observe
                      </Button>
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => setReviewing(item)}
                      >
                        <ClipboardCheck className="size-3.5" />
                        Review
                      </Button>
                    </div>
                  </div>
                </article>
                    );
                  })}
                </section>
              ))}
            </div>
          )}
        </div>
        <div className="border-t border-border/60 px-5 py-3">
          <Button
            variant="secondary"
            size="sm"
            onClick={onRefresh}
            disabled={loading}
          >
            <RefreshCw className={loading ? "animate-spin" : ""} />
            Refresh watchlist
          </Button>
          {performanceSource ? (
            <span className="ml-2 text-[11px] text-muted-foreground">{performanceSource}</span>
          ) : null}
        </div>
        <WatchlistReviewDialog
          item={reviewing}
          onOpenChange={(next) => {
            if (!next) setReviewing(null);
          }}
          onSaved={(item) => {
            onUpdated(item);
            setReviewing(null);
          }}
        />
        <WatchlistPerformanceDialog
          key={observing?.id || "closed"}
          item={observing}
          onOpenChange={(next) => {
            if (!next) setObserving(null);
          }}
        />
      </SheetContent>
    </Sheet>
  );
}

function OpportunitySignalsSheet({
  open,
  events,
  tradeDate,
  loading,
  error,
  onOpenChange,
  onRefresh,
  onSelect,
}: {
  open: boolean;
  events: MarketIndicatorEvent[];
  tradeDate?: string | null;
  loading: boolean;
  error: string | null;
  onOpenChange: (open: boolean) => void;
  onRefresh: () => void;
  onSelect: (event: MarketIndicatorEvent) => void;
}) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side="right"
        className="flex h-full w-full flex-col gap-0 p-0 sm:max-w-xl"
      >
        <SheetHeader className="border-b border-border/60 px-5 py-4 pr-12">
          <SheetTitle className="text-base">Opportunity signals</SheetTitle>
          <SheetDescription>
            Deduplicated intraday events, frozen by event minute. Signals are
            evidence for review, not automatic trade instructions.
          </SheetDescription>
        </SheetHeader>
        <div className="min-h-0 flex-1 overflow-y-auto">
          {loading ? (
            <div className="space-y-3 p-5">
              {Array.from({ length: 6 }, (_, index) => (
                <Skeleton key={index} className="h-20 w-full" />
              ))}
            </div>
          ) : error ? (
            <div className="flex h-full flex-col items-center justify-center gap-3 p-6 text-center">
              <AlertTriangle className="size-5 text-warning" />
              <p className="text-sm font-medium">Signals unavailable</p>
              <p className="text-xs text-muted-foreground">{error}</p>
              <Button variant="secondary" size="sm" onClick={onRefresh}>
                Retry
              </Button>
            </div>
          ) : events.length === 0 ? (
            <div className="flex h-full flex-col items-center justify-center gap-2 p-6 text-center">
              <Radio className="size-5 text-muted-foreground" />
              <p className="text-sm font-medium">No opportunity signals</p>
              <p className="max-w-sm text-xs leading-5 text-muted-foreground">
                No current market-indicator event was recorded for this data
                date. Daily-close screening remains available.
              </p>
            </div>
          ) : (
            <div className="divide-y divide-border/60">
              {events.map((event, index) => (
                <article
                  key={`${event.id ?? "event"}:${event.code}:${event.eventAt}:${index}`}
                  className="px-5 py-4"
                >
                  <div className="flex items-start justify-between gap-3">
                    <button
                      type="button"
                      className="min-w-0 text-left hover:underline"
                      onClick={() => onSelect(event)}
                    >
                      <div className="truncate text-sm font-semibold">
                        {event.name || event.code}
                      </div>
                      <div className="mt-0.5 font-mono text-[11px] text-muted-foreground">
                        {event.market || "—"} {event.code}
                      </div>
                    </button>
                    <Badge variant="secondary" className="shrink-0">
                      {event.eventType}
                    </Badge>
                  </div>
                  <div className="mt-3 grid grid-cols-2 gap-3 text-xs">
                    <div>
                      <div className="text-[11px] text-muted-foreground">
                        Triggered
                      </div>
                      <div className="mt-1 font-mono tabular-nums">
                        {event.eventAt
                          ? new Date(event.eventAt).toLocaleString()
                          : "—"}
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-[11px] text-muted-foreground">
                        Signal value
                      </div>
                      <div className="mt-1 font-mono tabular-nums">
                        {formatNumber(event.signalValue, 4)}
                      </div>
                    </div>
                    <div>
                      <div className="text-[11px] text-muted-foreground">
                        Price
                      </div>
                      <div className="mt-1 font-mono tabular-nums">
                        {formatNumber(event.lastPrice)}
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-[11px] text-muted-foreground">
                        Change
                      </div>
                      <div
                        className={`mt-1 font-mono tabular-nums ${marketValueClass(event.changePct)}`}
                      >
                        {formatPercent(event.changePct)}
                      </div>
                    </div>
                  </div>
                  <div className="mt-3 flex justify-between gap-3 text-[11px] text-muted-foreground">
                    <span className="truncate">
                      Source {event.source || "—"}
                    </span>
                    <span>{event.limitStatus || "normal"}</span>
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>
        <div className="flex items-center justify-between border-t border-border/60 px-5 py-3">
          <span className="text-[11px] text-muted-foreground">
            Data date {tradeDate || "—"}
          </span>
          <Button
            variant="secondary"
            size="sm"
            onClick={onRefresh}
            disabled={loading}
          >
            <RefreshCw className={loading ? "animate-spin" : ""} />
            Refresh signals
          </Button>
        </div>
      </SheetContent>
    </Sheet>
  );
}

function StrategyRunResults({
  run,
  onSelect,
  onAddToWatchlist,
}: {
  run: StockSelectionStrategyRunResponse;
  onSelect: (candidate: StockSelectionStrategyCandidate) => void;
  onAddToWatchlist: (candidate: StockSelectionStrategyCandidate) => void;
}) {
  const [evidenceCandidate, setEvidenceCandidate] =
    useState<StockSelectionStrategyCandidate | null>(null);
  return (
    <>
      <div className="min-h-0 flex-1 overflow-auto">
        {run.candidates.length === 0 ? (
          <div className="flex h-full min-h-[360px] flex-col items-center justify-center gap-2 px-6 text-center">
            <Workflow className="size-5 text-muted-foreground" />
            <p className="text-sm font-medium">This run found no candidates</p>
            <p className="max-w-md text-xs leading-5 text-muted-foreground">
              The run is still recorded with its evaluated universe and data
              snapshot. Review the governed rules or use Indicators to broaden
              research.
            </p>
          </div>
        ) : (
          <table className="w-full min-w-[980px] border-collapse text-left text-xs">
            <thead className="sticky top-0 z-10 bg-muted/95 text-muted-foreground">
              <tr className="border-b border-border/60">
                <th className="px-4 py-2.5 font-medium">Rank</th>
                <th className="px-3 py-2.5 font-medium">Security</th>
                <th className="px-3 py-2.5 text-right font-medium">Score</th>
                <th className="px-3 py-2.5 font-medium">Why selected</th>
                <th className="px-3 py-2.5 text-right font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {run.candidates.map((candidate) => (
                <tr
                  key={candidate.id}
                  className="border-b border-border/60 hover:bg-muted/50"
                >
                  <td className="px-4 py-3 font-mono tabular-nums text-muted-foreground">
                    #{candidate.rank}
                  </td>
                  <td className="px-3 py-3">
                    <button
                      type="button"
                      className="text-left hover:underline"
                      onClick={() => onSelect(candidate)}
                    >
                      <span className="block font-medium">
                        {candidate.stockName || candidate.stockCode}
                      </span>
                      <span className="font-mono text-[11px] text-muted-foreground">
                        {candidate.stockCode}
                      </span>
                    </button>
                  </td>
                  <td className="px-3 py-3 text-right font-mono tabular-nums font-medium">
                    {formatNumber(candidate.score)}
                  </td>
                  <td className="max-w-[420px] truncate px-3 py-3 text-muted-foreground">
                    {candidate.reasonSummary}
                  </td>
                  <td className="px-3 py-3 text-right">
                    <div className="flex justify-end gap-1">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setEvidenceCandidate(candidate)}
                      >
                        <Sparkles className="size-3.5" />
                        Evidence
                      </Button>
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => onAddToWatchlist(candidate)}
                      >
                        <BookmarkPlus className="size-3.5" />
                        Watch
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      <div className="flex flex-wrap items-center justify-between gap-2 border-t border-border px-4 py-2.5 text-[11px] text-muted-foreground">
        <div className="min-w-0">
          <span className="font-medium text-foreground">Run {run.id}</span>
          <span className="ml-2">v{run.strategyVersionNo}</span>
          <span className="ml-2">
            Snapshot {run.actualTradeDate || "unavailable"}
          </span>
          <span className="ml-2">
            {run.universeCount.toLocaleString()} evaluated
          </span>
          <span className="ml-2">
            {run.candidates.length} retained /{" "}
            {run.matchedCount.toLocaleString()} matched
          </span>
        </div>
        {run.warnings[0] ? (
          <span className="inline-flex items-center gap-1 text-warning">
            <AlertTriangle className="size-3" />
            {run.warnings[0]}
          </span>
        ) : null}
      </div>
      <StrategyEvidenceSheet
        candidate={evidenceCandidate}
        open={evidenceCandidate !== null}
        onOpenChange={(open) => {
          if (!open) setEvidenceCandidate(null);
        }}
      />
    </>
  );
}

function AiScreeningResults({
  screeningState,
  onSelect,
}: {
  screeningState: AiScreeningState;
  onSelect: (candidate: AiCandidateSelection) => void;
}) {
  if (screeningState.loading) {
    return (
      <div className="min-h-0 flex-1 overflow-auto p-4">
        <div className="space-y-3">
          <Skeleton className="h-28 w-full" />
          {Array.from({ length: 3 }, (_, index) => (
            <Skeleton key={index} className="h-44 w-full" />
          ))}
        </div>
      </div>
    );
  }
  if (screeningState.error) {
    return (
      <div className="flex min-h-0 flex-1 flex-col items-center justify-center gap-3 px-6 text-center">
        <AlertTriangle className="size-5 text-warning" />
        <p className="text-sm font-medium">AI screen unavailable</p>
        <p className="max-w-md text-xs text-muted-foreground">
          {screeningState.error}
        </p>
      </div>
    );
  }

  const result = screeningState.result;
  const analysis = result?.analysis_result;
  if (!result || !analysis) return null;

  return (
    <div className="min-h-0 flex-1 overflow-auto">
      <div className="space-y-4 p-4">
        <section className="rounded-lg border border-border/60 bg-muted/25 p-4">
          <p className="text-sm font-semibold">Research summary</p>
          <p className="mt-1 text-xs leading-5 text-muted-foreground">
            {analysis.summary}
          </p>
          <p className="mt-3 border-t border-border/60 pt-3 text-[11px] leading-5 text-muted-foreground">
            {analysis.market_context}
          </p>
          <p className="mt-2 text-[11px] leading-5 text-muted-foreground">
            {analysis.data_quality}
          </p>
        </section>
        <div className="flex flex-wrap items-center justify-between gap-2 text-[11px] text-muted-foreground">
          <span>
            {result.trade_date || "Snapshot date unavailable"} · {result.candidate_count} candidates
          </span>
          <span className="font-mono">
            {result.provider_code || "Provider"} / {result.model_code || "model"}
          </span>
        </div>
        {result.warnings.map((warning) => (
          <p key={warning} className="flex items-start gap-1 text-[11px] leading-5 text-warning">
            <AlertTriangle className="mt-0.5 size-3 shrink-0" />
            {warning}
          </p>
        ))}
        {analysis.candidates.map((candidate) => (
          <section key={candidate.code} className="rounded-lg border border-border/60 bg-card p-4">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <button
                  type="button"
                  className="text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  onClick={() => onSelect({
                    code: candidate.code,
                    name: candidate.name || candidate.code,
                    tradeDate: result.trade_date || undefined,
                  })}
                >
                  <span className="block font-mono text-sm font-semibold text-primary hover:underline">
                    {candidate.code} <span className="font-sans text-foreground">{candidate.name || candidate.code}</span>
                  </span>
                </button>
                <p className="mt-1 text-xs leading-5 text-muted-foreground">{candidate.thesis}</p>
              </div>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => onSelect({
                  code: candidate.code,
                  name: candidate.name || candidate.code,
                  tradeDate: result.trade_date || undefined,
                })}
              >
                Review
                <ChevronRight className="size-3.5" />
              </Button>
            </div>
            <div className="mt-4 grid gap-4 border-t border-border/60 pt-3 text-[11px] leading-5 md:grid-cols-3">
              <div>
                <span className="font-medium">Evidence</span>
                {candidate.evidence.length ? (
                  <ul className="mt-1 space-y-1 text-muted-foreground">
                    {candidate.evidence.map((item, index) => <li key={`${candidate.code}-e-${index}`}>• {item}</li>)}
                  </ul>
                ) : <p className="mt-1 text-muted-foreground">No model evidence supplied.</p>}
              </div>
              <div>
                <span className="font-medium">Watch</span>
                {candidate.watch.length ? (
                  <ul className="mt-1 space-y-1 text-muted-foreground">
                    {candidate.watch.map((item, index) => <li key={`${candidate.code}-w-${index}`}>• {item}</li>)}
                  </ul>
                ) : <p className="mt-1 text-muted-foreground">No follow-up items supplied.</p>}
              </div>
              <div>
                <span className="font-medium">Risks</span>
                {candidate.risks.length ? (
                  <ul className="mt-1 space-y-1 text-muted-foreground">
                    {candidate.risks.map((item, index) => <li key={`${candidate.code}-r-${index}`}>• {item}</li>)}
                  </ul>
                ) : <p className="mt-1 text-muted-foreground">No model risks supplied.</p>}
              </div>
            </div>
          </section>
        ))}
        <p className="text-[11px] leading-5 text-muted-foreground">{analysis.disclaimer}</p>
      </div>
    </div>
  );
}

function ResultsPanel({
  researchMode,
  result,
  strategyRun,
  aiScreening,
  loading,
  error,
  hasRun,
  onRefresh,
  onSort,
  onPage,
  onSelect,
  onSelectStrategyCandidate,
  onSelectAiCandidate,
  onOpenWatchlist,
  onOpenSignals,
  onAddToWatchlist,
}: {
  researchMode: boolean;
  result: StockScreenerResponse | null;
  strategyRun: StockSelectionStrategyRunResponse | null;
  aiScreening: AiScreeningState;
  loading: boolean;
  error: string | null;
  hasRun: boolean;
  onRefresh: () => void;
  onSort: (sort: ScreenerSort) => void;
  onPage: (page: number) => void;
  onSelect: (item: StockScreenerItem) => void;
  onSelectStrategyCandidate: (
    candidate: StockSelectionStrategyCandidate,
  ) => void;
  onSelectAiCandidate: (candidate: AiCandidateSelection) => void;
  onOpenWatchlist: () => void;
  onOpenSignals: () => void;
  onAddToWatchlist: (candidate: StockSelectionStrategyCandidate) => void;
}) {
  if (researchMode) return <div className="flex h-full min-h-0 flex-col overflow-hidden"><FactorResearchTab /></div>;
  const showingAiScreening = aiScreening.loading || aiScreening.result !== null;
  const pageCount = result
    ? Math.max(1, Math.ceil(result.count / result.pageSize))
    : 1;
  const quality = result?.dataQuality;
  const qualityClass =
    quality?.status === "READY"
      ? "border-emerald-500/30 bg-emerald-50 text-emerald-700"
      : quality?.status === "STALE"
        ? "border-destructive/30 bg-destructive/5 text-destructive"
        : "border-amber-500/30 bg-amber-50 text-amber-700";
  return (
    <section className="flex h-full min-h-0 flex-col bg-background">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border px-4 py-3">
        <div>
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            {showingAiScreening ? <Bot className="size-3.5" /> : <ListFilter className="size-3.5" />}
            {showingAiScreening ? "AI screening" : "Result set"}
          </div>
          <h2 className="mt-1 text-sm font-semibold">
            {showingAiScreening ? "AI research candidates" : "Matched securities"}
          </h2>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="secondary" size="sm" onClick={onOpenSignals}>
            <Radio className="size-3.5" />
            Signals
          </Button>
          <Button variant="secondary" size="sm" onClick={onOpenWatchlist}>
            <BookmarkPlus className="size-3.5" />
            Watchlist
          </Button>
          <Badge variant="secondary">
            {showingAiScreening
              ? aiScreening.result
                ? `${aiScreening.result.candidate_count} candidates`
                : "AI review"
              : result
              ? `${result.count.toLocaleString()} matches`
              : "Daily close"}
          </Badge>
          {!showingAiScreening ? <Button
            variant="ghost"
            size="icon-sm"
            aria-label="Refresh screen results"
            title="Refresh screen results"
            onClick={onRefresh}
            disabled={loading || !hasRun}
          >
            <RefreshCw
              className={loading ? "size-3.5 animate-spin" : "size-3.5"}
            />
          </Button> : null}
        </div>
      </div>
      {!showingAiScreening && aiScreening.error ? (
        <div className="flex items-start gap-2 border-b border-warning/30 bg-warning/5 px-4 py-2 text-xs text-warning">
          <AlertTriangle className="mt-0.5 size-3.5 shrink-0" />
          <span>AI 研判未完成：{aiScreening.error}</span>
        </div>
      ) : null}
      {!showingAiScreening && quality ? (
        <div className="flex flex-wrap items-center gap-x-4 gap-y-2 border-b border-border/60 bg-muted/25 px-4 py-2.5 text-[11px]">
          <div className="flex items-center gap-1.5 font-medium">
            <Database className="size-3.5 text-muted-foreground" />
            Snapshot quality{" "}
            <Badge variant="outline" className={qualityClass}>
              {quality.status}
            </Badge>
          </div>
          <span className="font-mono tabular-nums text-muted-foreground">
            {quality.dailySnapshotCount.toLocaleString()} /{" "}
            {quality.eligibleStockCount.toLocaleString()} daily closes
          </span>
          <span className="text-muted-foreground">
            {quality.freshnessLagDays} calendar days old
          </span>
          <div className="flex min-w-0 flex-wrap gap-x-3 gap-y-1 text-muted-foreground">
            {quality.fieldCoverage.map((coverage) => (
              <span key={coverage.field}>
                <span className="font-medium text-foreground">
                  {coverage.field.replace(/_/g, " ")}
                </span>{" "}
                {coverage.coveragePct.toFixed(1)}%
              </span>
            ))}
          </div>
          {quality.warnings[0] ? (
            <span className="flex min-w-0 items-center gap-1 text-warning">
              <AlertTriangle className="size-3 shrink-0" />
              {quality.warnings[0]}
            </span>
          ) : null}
        </div>
      ) : null}
      {showingAiScreening ? (
        <AiScreeningResults screeningState={aiScreening} onSelect={onSelectAiCandidate} />
      ) : loading ? (
        <div className="min-h-0 flex-1 overflow-auto">
          <ResultsSkeleton />
        </div>
      ) : error ? (
        <div className="flex min-h-0 flex-1 flex-col items-center justify-center gap-3 px-6 text-center">
          <AlertTriangle className="size-5 text-warning" />
          <p className="text-sm font-medium">Screen unavailable</p>
          <p className="max-w-md text-xs text-muted-foreground">{error}</p>
          <Button variant="secondary" size="sm" onClick={onRefresh}>
            Retry
          </Button>
        </div>
      ) : strategyRun ? (
        <StrategyRunResults
          run={strategyRun}
          onSelect={onSelectStrategyCandidate}
          onAddToWatchlist={onAddToWatchlist}
        />
      ) : (
        <>
          <div className="min-h-0 flex-1 overflow-auto">
            {!hasRun ? (
              <div className="flex h-full min-h-[360px] flex-col items-center justify-center gap-2 px-6 text-center">
                <ListFilter className="size-5 text-muted-foreground" />
                <p className="text-sm font-medium">
                  Build criteria, then run the screen
                </p>
                <p className="max-w-md text-xs leading-5 text-muted-foreground">
                  Daily close data is available across real industry membership,
                  technical, liquidity, size, valuation coverage, and fund-flow
                  dimensions.
                </p>
              </div>
            ) : !result?.ok || result.items.length === 0 ? (
              <div className="flex h-full min-h-[360px] flex-col items-center justify-center gap-2 px-6 text-center">
                <ListFilter className="size-5 text-warning" />
                <p className="text-sm font-medium">No stocks matched</p>
                <p className="max-w-md text-xs leading-5 text-muted-foreground">
                  {result?.error ||
                    "Broaden one condition or reset the screen to inspect the available daily snapshot."}
                </p>
              </div>
            ) : (
              <table className="w-full min-w-[1840px] border-collapse text-left text-xs">
                <thead className="sticky top-0 z-10 bg-muted/95 text-muted-foreground">
                  <tr className="border-b border-border/60">
                    <th className="sticky left-0 z-10 bg-muted/95 px-4 py-2.5 font-medium">
                      Security
                    </th>
                    <th className="px-3 py-2.5 font-medium">Board</th>
                    {SORT_COLUMNS.map((column) => (
                      <th
                        key={column.key}
                        className={`px-3 py-2.5 font-medium ${column.align === "right" ? "text-right" : ""}`}
                      >
                        <button
                          type="button"
                          className="inline-flex items-center gap-1 hover:text-foreground"
                          onClick={() => onSort(column.key)}
                        >
                          {column.label}
                          {result.sort === column.key && (
                            <ChevronDown
                              className={`size-3 ${result.sortDir === "asc" ? "rotate-180" : ""}`}
                            />
                          )}
                        </button>
                      </th>
                    ))}
                    <th className="px-3 py-2.5 font-medium">THS industry</th>
                  </tr>
                </thead>
                <tbody>
                  {result.items.map((item) => (
                    <tr
                      key={`${item.market}:${item.code}`}
                      className="border-b border-border/60 hover:bg-muted/50"
                    >
                      <td className="sticky left-0 bg-background px-4 py-2.5 hover:bg-muted/50">
                        <button
                          type="button"
                          className="min-w-0 text-left hover:underline"
                          onClick={() => onSelect(item)}
                        >
                          <span className="block font-medium text-foreground">
                            {item.name || item.code}
                            {item.st && (
                              <span className="ml-1 text-[10px] text-muted-foreground">
                                ST
                              </span>
                            )}
                          </span>
                          <span className="font-mono text-[11px] text-muted-foreground">
                            {item.market} {item.code}
                          </span>
                        </button>
                      </td>
                      <td className="px-3 py-2.5">
                        <Badge variant="secondary" className="font-normal">
                          {BOARD_LABELS.get(item.board ?? "unknown") ||
                            "Unknown"}
                        </Badge>
                      </td>
                      <td className="px-3 py-2.5 text-right font-mono tabular-nums">
                        {formatNumber(item.price)}
                      </td>
                      <td
                        className={`px-3 py-2.5 text-right font-mono tabular-nums ${marketValueClass(item.changePct)}`}
                      >
                        {formatPercent(item.changePct)}
                      </td>
                      <td className="px-3 py-2.5 text-right font-mono tabular-nums">
                        {formatPercent(item.amplitudePct)}
                      </td>
                      <td
                        className={`px-3 py-2.5 text-right font-mono tabular-nums ${marketValueClass(item.returnPct5d)}`}
                      >
                        {formatPercent(item.returnPct5d)}
                      </td>
                      <td
                        className={`px-3 py-2.5 text-right font-mono tabular-nums ${marketValueClass(item.returnPct10d)}`}
                      >
                        {formatPercent(item.returnPct10d)}
                      </td>
                      <td
                        className={`px-3 py-2.5 text-right font-mono tabular-nums ${marketValueClass(item.returnPct30d)}`}
                      >
                        {formatPercent(item.returnPct30d)}
                      </td>
                      <td
                        className={`px-3 py-2.5 text-right font-mono tabular-nums ${marketValueClass(item.returnPct60d)}`}
                      >
                        {formatPercent(item.returnPct60d)}
                      </td>
                      <td
                        className={`px-3 py-2.5 text-right font-mono tabular-nums ${marketValueClass(item.returnPct252d)}`}
                      >
                        {formatPercent(item.returnPct252d)}
                      </td>
                      <td
                        className={`px-3 py-2.5 text-right font-mono tabular-nums ${marketValueClass(item.mainNet)}`}
                      >
                        {formatAmount(item.mainNet)}
                      </td>
                      <td
                        className={`px-3 py-2.5 text-right font-mono tabular-nums ${marketValueClass(item.mainNet5d)}`}
                      >
                        {formatAmount(item.mainNet5d)}
                      </td>
                      <td className="px-3 py-2.5 text-right font-mono tabular-nums">
                        {formatAmount(item.amount)}
                      </td>
                      <td className="px-3 py-2.5 text-right font-mono tabular-nums">
                        {formatAmount(item.totalMarketCap)}
                      </td>
                      <td className="px-3 py-2.5 text-right font-mono tabular-nums">
                        {formatPercent(item.turnoverPct)}
                      </td>
                      <td className="px-3 py-2.5 text-right font-mono tabular-nums">
                        {formatNumber(item.volumeRatio)}
                      </td>
                      <td className="px-3 py-2.5 text-right font-mono tabular-nums">
                        {formatNumber(item.pe)}
                      </td>
                      <td className="max-w-44 truncate px-3 py-2.5 text-muted-foreground">
                        {item.thsIndustryName || item.industry || "—"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
          <div className="flex flex-wrap items-center justify-between gap-2 border-t border-border px-4 py-2.5 text-[11px] text-muted-foreground">
            <div className="min-w-0">
              <span className="font-medium text-foreground">
                {result?.tradeDate
                  ? result.requestedTradeDate && result.requestedTradeDate !== result.tradeDate
                    ? `请求 ${result.requestedTradeDate}，实际快照 ${result.tradeDate}`
                    : `实际快照 ${result.tradeDate}`
                  : "Daily close snapshot"}
              </span>
              {result && (
                <span className="ml-2">
                  {result.universeCount.toLocaleString()} in universe
                </span>
              )}
              {result?.source && (
                <span className="ml-2 truncate">{result.source}</span>
              )}
              {result?.warnings?.[0] && (
                <span className="ml-2 inline-flex items-center gap-1 text-warning">
                  <AlertTriangle className="size-3" />
                  {result.warnings[0]}
                </span>
              )}
            </div>
            <div className="flex items-center gap-1">
              <Button
                variant="ghost"
                size="icon-sm"
                aria-label="Previous page"
                disabled={!result || result.page <= 1}
                onClick={() => result && onPage(result.page - 1)}
              >
                <ChevronLeft className="size-3.5" />
              </Button>
              <span className="font-mono">
                Page {result?.page ?? 1} / {pageCount}
              </span>
              <Button
                variant="ghost"
                size="icon-sm"
                aria-label="Next page"
                disabled={!result || result.page >= pageCount}
                onClick={() => result && onPage(result.page + 1)}
              >
                <ChevronRight className="size-3.5" />
              </Button>
            </div>
          </div>
        </>
      )}
    </section>
  );
}

function ScreenerPanel({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const [workspaceMode, setWorkspaceMode] = useState(() => new URLSearchParams(window.location.search).get("researchView") ? "research" : "indicators");
  const [criteria, setCriteria] = useState<Criteria>(DEFAULT_CRITERIA);
  const [result, setResult] = useState<StockScreenerResponse | null>(null);
  const [strategyRun, setStrategyRun] =
    useState<StockSelectionStrategyRunResponse | null>(null);
  const [aiScreening, setAiScreening] = useState<AiScreeningState>({
    result: null,
    loading: false,
    error: null,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [runningStrategyCode, setRunningStrategyCode] = useState<string | null>(
    null,
  );
  const [backtestStrategy, setBacktestStrategy] =
    useState<StockSelectionStrategy | null>(null);
  const [hasRun, setHasRun] = useState(false);
  const [selected, setSelected] = useState<{
    code: string;
    name: string;
    tradeDate: string;
  } | null>(null);
  const [classifications, setClassifications] =
    useState<ScreenerClassificationOptions>({
      tdxIndustryGroups: [],
      thsIndustries: [],
      concepts: [],
      styles: [],
    });
  const [watchlistOpen, setWatchlistOpen] = useState(false);
  const [watchlist, setWatchlist] = useState<StockSelectionWatchlistItem[]>([]);
  const [watchlistLoading, setWatchlistLoading] = useState(false);
  const [watchlistError, setWatchlistError] = useState<string | null>(null);
  const [watchlistPerformance, setWatchlistPerformance] = useState<Record<string, StockSelectionWatchlistPerformance>>({});
  const [watchlistPerformanceError, setWatchlistPerformanceError] = useState<string | null>(null);
  const [watchlistPerformanceWarnings, setWatchlistPerformanceWarnings] = useState<string[]>([]);
  const [watchlistPerformanceSource, setWatchlistPerformanceSource] = useState<string | null>(null);
  const [watchlistCandidate, setWatchlistCandidate] =
    useState<StockSelectionStrategyCandidate | null>(null);
  const [signalsOpen, setSignalsOpen] = useState(false);
  const [signals, setSignals] = useState<MarketIndicatorEvent[]>([]);
  const [signalsTradeDate, setSignalsTradeDate] = useState<string | null>(null);
  const [signalsLoading, setSignalsLoading] = useState(false);
  const [signalsError, setSignalsError] = useState<string | null>(null);
  useEffect(() => {
    void fetchScreenerClassificationOptions(criteria.tradeDate || undefined)
      .then(setClassifications)
      .catch(() =>
        setClassifications({
          tdxIndustryGroups: [],
          thsIndustries: [],
          concepts: [],
          styles: [],
        }),
      );
  }, [criteria.tradeDate]);
  const loadWatchlist = useCallback(async () => {
    setWatchlistLoading(true);
    setWatchlistError(null);
    setWatchlistPerformanceError(null);
    try {
      const [listResult, performanceResult] = await Promise.allSettled([
        fetchStockSelectionWatchlist(),
        fetchStockSelectionWatchlistPerformance(),
      ]);
      if (listResult.status === "rejected") {
        throw listResult.reason;
      }
      setWatchlist(listResult.value.items);
      if (performanceResult.status === "fulfilled") {
        setWatchlistPerformance(
          Object.fromEntries(
            performanceResult.value.items.map((item) => [
              String(item.watchlistItemId),
              item,
            ]),
          ),
        );
        setWatchlistPerformanceWarnings(performanceResult.value.warnings || []);
        setWatchlistPerformanceSource(performanceResult.value.dataSource || null);
      } else {
        setWatchlistPerformance({});
        setWatchlistPerformanceWarnings([]);
        setWatchlistPerformanceSource(null);
        setWatchlistPerformanceError(
          performanceResult.reason instanceof Error
            ? performanceResult.reason.message
            : "Unable to calculate return observation.",
        );
      }
    } catch (caught) {
      setWatchlistError(
        caught instanceof Error ? caught.message : "Unable to load watchlist.",
      );
    } finally {
      setWatchlistLoading(false);
    }
  }, []);
  const loadSignals = useCallback(async () => {
    setSignalsLoading(true);
    setSignalsError(null);
    try {
      const response = await fetchMarketIndicators({
        tradeDate: criteria.tradeDate || undefined,
        status: "all",
        limit: 100,
      });
      setSignals(response.events);
      setSignalsTradeDate(response.tradeDate || criteria.tradeDate || null);
    } catch (caught) {
      setSignalsError(
        caught instanceof Error
          ? caught.message
          : "Unable to load opportunity signals.",
      );
    } finally {
      setSignalsLoading(false);
    }
  }, [criteria.tradeDate]);
  const mergeWatchlistItem = useCallback(
    (item: StockSelectionWatchlistItem) =>
      setWatchlist((current) => {
        const index = current.findIndex((value) => value.id === item.id);
        return index === -1
          ? [item, ...current]
          : current.map((value) => (value.id === item.id ? item : value));
      }),
    [],
  );
  const updateCriteria = useCallback(
    <K extends keyof Criteria>(key: K, value: Criteria[K]) =>
      setCriteria((current) => ({ ...current, [key]: value })),
    [],
  );
  const runScreen = useCallback(
    async (page = 1, nextCriteria = criteria) => {
      setLoading(true);
      setError(null);
      setHasRun(true);
      setStrategyRun(null);
      setAiScreening({ result: null, loading: false, error: null });
      try {
        const nextResult = await fetchStockScreener(toQuery(nextCriteria, page));
        setResult(nextResult);
        return nextResult;
      } catch (caught) {
        setResult(null);
        setError(
          caught instanceof Error
            ? caught.message
            : "Unable to load screener results.",
        );
        return null;
      } finally {
        setLoading(false);
      }
    },
    [criteria],
  );
  const runStrategy = useCallback(
    async (strategy: StockSelectionStrategy) => {
      setLoading(true);
      setError(null);
      setHasRun(true);
      setRunningStrategyCode(strategy.code);
      setResult(null);
      setAiScreening({ result: null, loading: false, error: null });
      try {
        setStrategyRun(
          await runScreenerStrategy(
            strategy.code,
            criteria.tradeDate || undefined,
          ),
        );
      } catch (caught) {
        setStrategyRun(null);
        setError(
          caught instanceof Error
            ? caught.message
            : "Unable to run the governed strategy.",
        );
      } finally {
        setLoading(false);
        setRunningStrategyCode(null);
      }
    },
    [criteria.tradeDate],
  );
  const activeCriteriaCount = useMemo(
    () =>
      Object.entries(criteria).filter(
        ([key, value]) =>
          ![
            "tradeDate",
            "universe",
            "board",
            "excludeSt",
            "excludeChiNext",
            "excludeStar",
            "sort",
            "sortDir",
          ].includes(key) && value !== "",
      ).length +
      (criteria.board === "ALL" ? 0 : 1) +
      (criteria.universe === "ALL" ? 0 : 1) +
      (criteria.excludeSt ? 1 : 0) +
      (criteria.excludeChiNext ? 1 : 0) +
      (criteria.excludeStar ? 1 : 0),
    [criteria],
  );
  const changeSort = useCallback(
    (sort: ScreenerSort) => {
      const next = {
        ...criteria,
        sort,
        sortDir:
          criteria.sort === sort && criteria.sortDir === "desc"
            ? "asc"
            : "desc",
      } satisfies Criteria;
      setCriteria(next);
      void runScreen(1, next);
    },
    [criteria, runScreen],
  );
  const closeTradeReview = (next: boolean) => {
    if (!next) setSelected(null);
  };
  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent
          showCloseButton={false}
          className="!h-[100dvh] !w-full !max-w-none gap-0 overflow-hidden rounded-none border-2 border-border bg-background p-0 shadow-2xl sm:!h-[96vh] sm:!w-[96vw] sm:!max-w-[96vw] sm:rounded-xl"
          onPointerDownOutside={(event) => event.preventDefault()}
          onFocusOutside={(event) => event.preventDefault()}
          onInteractOutside={(event) => event.preventDefault()}
        >
          <DialogTitle className="sr-only">Cynexus Screener</DialogTitle>
          <DialogDescription className="sr-only">
            Build a multi-dimensional stock screen and review the matching
            daily-close snapshot.
          </DialogDescription>
          <div className="flex h-full min-h-0 flex-col">
            <header className="flex min-h-[58px] shrink-0 flex-wrap items-center justify-between gap-3 border-b-2 border-border px-4 py-2">
              <div className="flex min-w-0 items-center gap-3">
                <div className="grid size-8 shrink-0 place-items-center rounded-md bg-foreground text-background">
                  <ListFilter className="size-4" />
                </div>
                <div className="min-w-0">
                  <div className="truncate text-sm font-semibold">
                    Cynexus Screener
                  </div>
                  <div className="truncate text-xs text-muted-foreground">
                    Daily-close multi-factor research workspace
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Badge variant="secondary">Daily close</Badge>
                <Badge variant="secondary">{activeCriteriaCount} active</Badge>
                <Button
                  variant="ghost"
                  size="icon-sm"
                  aria-label="Close screener panel"
                  title="Close screener panel"
                  onClick={() => onOpenChange(false)}
                >
                  <X />
                </Button>
              </div>
            </header>
            <div className="min-h-0 flex-1 overflow-hidden">
              <ResizablePanelGroup orientation="horizontal" className="h-full">
                <ResizablePanel
                  id="criteria"
                  defaultSize="31%"
                  minSize="27%"
                  maxSize="42%"
                >
                  <CriteriaPanel
                    mode={workspaceMode}
                    onModeChange={setWorkspaceMode}
                    criteria={criteria}
                    classifications={classifications}
                    loading={loading}
                    runningStrategyCode={runningStrategyCode}
                    onChange={updateCriteria}
                    onReset={() => setCriteria(DEFAULT_CRITERIA)}
                    onRun={() => runScreen()}
                    onApplyStrategy={(strategy) =>
                      setCriteria({
                        ...DEFAULT_CRITERIA,
                        ...strategyParameters(strategy),
                      })
                    }
                    onRunStrategy={(strategy) => void runStrategy(strategy)}
                    onBacktestStrategy={setBacktestStrategy}
                    aiScreening={aiScreening}
                    onAiScreeningChange={setAiScreening}
                    onApplyObservatoryHints={(hints) => {
                      hints.forEach((hint) => {
                        if (!(hint.criterion in DEFAULT_CRITERIA)) return;
                        const key = hint.criterion as keyof Criteria;
                        updateCriteria(key, hint.value as Criteria[typeof key]);
                      });
                    }}
                    onSelectObservatoryEvent={(event, tradeDate) =>
                      setSelected({ code: event.code, name: event.name || event.code, tradeDate })
                    }
                  />
                </ResizablePanel>
                <ResizableHandle
                  withHandle
                  className="w-1 border-x border-border bg-muted/50"
                />
                <ResizablePanel id="results" defaultSize="69%" minSize="54%">
                  <ResultsPanel
                    researchMode={workspaceMode === "research"}
                    result={result}
                    strategyRun={strategyRun}
                    aiScreening={aiScreening}
                    loading={loading}
                    error={error}
                    hasRun={hasRun}
                    onRefresh={() =>
                      strategyRun
                        ? void runStrategy({
                            code: strategyRun.strategyCode,
                          } as StockSelectionStrategy)
                        : void runScreen(result?.page ?? 1)
                    }
                    onSort={changeSort}
                    onPage={(page) => void runScreen(page)}
                    onSelect={(item) =>
                      setSelected({
                        code: item.code,
                        name: item.name,
                        tradeDate: result?.tradeDate || criteria.tradeDate,
                      })
                    }
                    onSelectStrategyCandidate={(candidate) =>
                      setSelected({
                        code: candidate.stockCode,
                        name: candidate.stockName,
                        tradeDate:
                          strategyRun?.actualTradeDate || criteria.tradeDate,
                      })
                    }
                    onSelectAiCandidate={(candidate) =>
                      setSelected({
                        code: candidate.code,
                        name: candidate.name || candidate.code,
                        tradeDate: candidate.tradeDate || criteria.tradeDate,
                      })
                    }
                    onOpenWatchlist={() => {
                      setWatchlistOpen(true);
                      void loadWatchlist();
                    }}
                    onOpenSignals={() => {
                      setSignalsOpen(true);
                      void loadSignals();
                    }}
                    onAddToWatchlist={setWatchlistCandidate}
                  />
                </ResizablePanel>
              </ResizablePanelGroup>
            </div>
          </div>
        </DialogContent>
      </Dialog>
      {backtestStrategy ? (
        <StrategyBacktestDialog
          key={backtestStrategy.code}
          strategy={backtestStrategy}
          open
          onOpenChange={(next) => {
            if (!next) setBacktestStrategy(null);
          }}
        />
      ) : null}
      <AddWatchlistDialog
        candidate={watchlistCandidate}
        onOpenChange={(next) => {
          if (!next) setWatchlistCandidate(null);
        }}
        onSaved={(item) => {
          mergeWatchlistItem(item);
          void loadWatchlist();
        }}
      />
      <WatchlistSheet
        open={watchlistOpen}
        items={watchlist}
        performance={watchlistPerformance}
        performanceError={watchlistPerformanceError}
        performanceWarnings={watchlistPerformanceWarnings}
        performanceSource={watchlistPerformanceSource}
        loading={watchlistLoading}
        error={watchlistError}
        onOpenChange={setWatchlistOpen}
        onRefresh={() => void loadWatchlist()}
        onSelect={(item) => {
          setWatchlistOpen(false);
          setSelected({
            code: item.stockCode,
            name: item.stockName || item.stockCode,
            tradeDate: criteria.tradeDate,
          });
        }}
        onUpdated={(item) => {
          mergeWatchlistItem(item);
          void loadWatchlist();
        }}
      />
      <OpportunitySignalsSheet
        open={signalsOpen}
        events={signals}
        tradeDate={signalsTradeDate}
        loading={signalsLoading}
        error={signalsError}
        onOpenChange={setSignalsOpen}
        onRefresh={() => void loadSignals()}
        onSelect={(event) => {
          setSignalsOpen(false);
          setSelected({
            code: event.code,
            name: event.name || event.code,
            tradeDate: signalsTradeDate || criteria.tradeDate,
          });
        }}
      />
      {selected ? (
        <Suspense
          fallback={<TradeReviewPanelLoading onOpenChange={closeTradeReview} />}
        >
          <TradeReviewPanel
            open
            onOpenChange={closeTradeReview}
            symbol={selected.code}
            instrumentType="STOCK"
            tradeDate={selected.tradeDate}
            onTradeDateChange={(tradeDate) =>
              setSelected((current) =>
                current ? { ...current, tradeDate } : null,
              )
            }
            securityName={selected.name}
            symbols={[]}
            availableTradeDates={selected.tradeDate ? [selected.tradeDate] : []}
            onSymbolChange={(symbol) =>
              setSelected((current) =>
                current ? { ...current, code: symbol, name: symbol } : null,
              )
            }
            trades={[]}
          />
        </Suspense>
      ) : null}
    </>
  );
}

export default function CynexusScreenerPage() {
  const [panelOpen, setPanelOpen] = useState(true);
  return (
    <WorkspaceShell
      sectionLabel="Cynexus Screener"
      pageTitle="Cynexus Screener"
      contentScrollable={false}
    >
      <div className="flex h-full min-h-0 items-center justify-center">
        <section className="max-w-xl rounded-lg border border-dashed border-border/70 bg-muted/20 p-6 text-center">
          <ListFilter className="mx-auto size-5 text-muted-foreground" />
          <h1 className="mt-3 text-base font-semibold">Cynexus Screener</h1>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">
            The multi-factor panel is closed. Reopen it to continue the current
            stock screen.
          </p>
          <Button
            variant="secondary"
            className="mt-4"
            onClick={() => setPanelOpen(true)}
          >
            Open screener panel
          </Button>
        </section>
      </div>
      <ScreenerPanel open={panelOpen} onOpenChange={setPanelOpen} />
    </WorkspaceShell>
  );
}
