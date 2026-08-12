import type { MarketIndicatorEvent } from "@/services/market/indicator";
import type {
  PortfolioReviewAnchor,
  PortfolioTrade,
} from "@/services/market/portfolio/types";
import type {
  StockIntradayPoint,
  StockIntradayResponse,
  StockKlineBar,
} from "@/views/stock-chart/lib/types";

export type ReplayPeriod =
  | "timeshare"
  | "1m"
  | "5m"
  | "15m"
  | "30m"
  | "1d"
  | "1w"
  | "1mo";

export function isHistoricalReplayPeriod(
  period: ReplayPeriod,
): period is "1d" | "1w" | "1mo" {
  return period === "1d" || period === "1w" || period === "1mo";
}

export interface ReplayMarker {
  id: string;
  kind: "trade" | "observation" | "event";
  timestamp: number;
  price: number | null;
  label: string;
  detail?: string | null;
  trade?: PortfolioTrade;
  anchor?: PortfolioReviewAnchor;
  event?: MarketIndicatorEvent;
}

export function epoch(value: number | string | null | undefined) {
  if (typeof value === "number")
    return value < 100_000_000_000 ? value * 1_000 : value;
  const parsed = value ? Date.parse(value) : Number.NaN;
  return Number.isFinite(parsed) ? parsed : 0;
}

function isTradingMinute(value: string) {
  const match = /^(\d{2}):(\d{2})$/.exec(value);
  if (!match) return false;
  const minutes = Number(match[1]) * 60 + Number(match[2]);
  return (
    (minutes >= 9 * 60 + 30 && minutes <= 11 * 60 + 30) ||
    (minutes >= 13 * 60 && minutes <= 15 * 60)
  );
}

function chinaTimestamp(tradeDate: string, time: string) {
  return epoch(`${tradeDate}T${time}:00+08:00`);
}

/**
 * The market API historically serialized ClickHouse DateTime values as UTC
 * epochs even though the date/time fields represent China market time. Keep
 * replay cursors inside the two A-share sessions when that legacy encoding is
 * encountered, while leaving correctly encoded timestamps unchanged.
 */
export function normalizeTradingTimestamp(
  timestamp: number,
  tradeDate?: string | null,
  timeLabel?: string | null,
) {
  if (!tradeDate) return timestamp;
  const explicitTime = timeLabel?.match(/^\d{2}:\d{2}$/)?.[0];
  if (explicitTime && isTradingMinute(explicitTime))
    return chinaTimestamp(tradeDate, explicitTime);
  const date = new Date(timestamp);
  if (Number.isNaN(date.getTime())) return timestamp;
  const chinaTime = date.toLocaleTimeString("en-GB", {
    timeZone: "Asia/Shanghai",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
  if (isTradingMinute(chinaTime)) return timestamp;
  const utcTime = `${String(date.getUTCHours()).padStart(2, "0")}:${String(date.getUTCMinutes()).padStart(2, "0")}`;
  return isTradingMinute(utcTime)
    ? chinaTimestamp(tradeDate, utcTime)
    : timestamp;
}

function normalizeBar(
  bar: StockKlineBar,
  fallbackDate?: string | null,
): StockKlineBar {
  const tradeDate = bar.trade_date ?? fallbackDate;
  return {
    ...bar,
    timestamp: normalizeTradingTimestamp(bar.timestamp, tradeDate),
  };
}

/** Normalize all intraday series used by the replay, including overlays. */
export function normalizeStockIntraday(
  response: StockIntradayResponse,
  fallbackDate?: string,
) {
  const tradeDate =
    response.trade_date ?? response.requested_trade_date ?? fallbackDate;
  return {
    ...response,
    timeshare: response.timeshare
      .map((point) => ({
        ...point,
        timestamp: normalizeTradingTimestamp(
          point.timestamp,
          point.trade_date ?? tradeDate,
          point.time_label,
        ),
      }))
      .filter((point) => isTradingMinute(point.time_label)),
    minute_bars: Object.fromEntries(
      Object.entries(response.minute_bars).map(([period, bars]) => [
        period,
        bars?.map((bar) => normalizeBar(bar, tradeDate)) ?? [],
      ]),
    ),
  } as StockIntradayResponse;
}

/** Normalize the historical TDX timeshare API into the chart point shape. */
export function easyTdxTimesharePoints(
  items: Array<Record<string, unknown>>,
  tradeDate: string,
): StockIntradayPoint[] {
  let cumulativeVolume = 0;
  let cumulativeTurnover = 0;
  return items
    .map((item) => {
      const timeLabel = String(item.time_label ?? "09:30");
      const rawTime = String(item.trade_time ?? "");
      const timestamp = epoch(
        /^\d{4}-\d{2}-\d{2}T/.test(rawTime)
          ? /[zZ]|[+-]\d{2}:?\d{2}$/.test(rawTime)
            ? rawTime
            : `${rawTime}+08:00`
          : `${tradeDate}T${timeLabel.length === 5 ? `${timeLabel}:00` : timeLabel}+08:00`,
      );
      const price = Number(item.price ?? 0);
      const volume = Number(item.volume ?? 0);
      const turnover = Number(item.turnover ?? 0);
      cumulativeVolume += Number.isFinite(volume) ? volume : 0;
      cumulativeTurnover +=
        Number.isFinite(turnover) && turnover > 0
          ? turnover
          : price * Math.max(volume, 0);
      return {
        timestamp,
        trade_date: String(item.trade_date ?? tradeDate),
        time_label: timeLabel,
        price,
        open: price,
        avg_price:
          cumulativeVolume > 0 ? cumulativeTurnover / cumulativeVolume : price,
        volume,
        turnover: Number.isFinite(turnover) ? turnover : null,
        turnover_rate:
          typeof item.turnover_rate === "number" ? item.turnover_rate : null,
      };
    })
    .filter((item) => item.timestamp > 0 && Number.isFinite(item.price));
}

export function formatTime(timestamp: number | null | undefined) {
  if (!timestamp) return "-";
  return new Date(timestamp).toLocaleTimeString("zh-CN", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

export function barsForPeriod(
  response: {
    timeshare: StockIntradayPoint[];
    minute_bars: Partial<Record<"1m" | "5m" | "15m" | "30m", StockKlineBar[]>>;
  } | null,
  period: ReplayPeriod,
) {
  if (!response) return [] as StockKlineBar[];
  if (period === "timeshare") {
    // Some ClickHouse timeshare snapshots contain the same minute twice
    // (the source is captured at a finer cadence but serialized with a
    // minute timestamp). Collapse those duplicates before using the series
    // as the replay timeline, otherwise the x-axis allocates two slots to
    // one market minute and context indices appear to drift.
    const byTimestamp = new Map<number, StockKlineBar>();
    for (const point of response.timeshare) {
      const timestamp = epoch(point.timestamp);
      byTimestamp.set(timestamp, {
        timestamp,
        trade_date: point.trade_date ?? undefined,
        open: point.open ?? point.price,
        high: point.price,
        low: point.price,
        close: point.price,
        volume: point.volume,
        turnover: point.turnover ?? undefined,
      });
    }
    return [...byTimestamp.values()].sort((a, b) => a.timestamp - b.timestamp);
  }
  if (isHistoricalReplayPeriod(period)) return [];
  return response.minute_bars[period] ?? [];
}

function chinaDate(timestamp: number) {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Shanghai",
  }).format(new Date(timestamp));
}

function periodKey(date: string, period: ReplayPeriod) {
  if (period === "1d") return date;
  if (period === "1w") return weekStart(date);
  if (period === "1mo") return date.slice(0, 7);
  return date;
}

/** Map a trade/observation timestamp onto the chart's actual category slot. */
export function barIndexForMarker(
  timestamp: number,
  bars: StockKlineBar[],
  period: ReplayPeriod,
) {
  if (!timestamp || !bars.length) return -1;
  if (isHistoricalReplayPeriod(period)) {
    const markerKey = periodKey(chinaDate(timestamp), period);
    return bars.findIndex((bar) => {
      const date = bar.trade_date?.slice(0, 10) || chinaDate(epoch(bar.timestamp));
      return periodKey(date, period) === markerKey;
    });
  }
  return nearestIndex(bars, timestamp);
}

function barTradeDate(bar: StockKlineBar) {
  if (bar.trade_date) return bar.trade_date.slice(0, 10);
  const value = new Date(epoch(bar.timestamp));
  return Number.isNaN(value.getTime())
    ? ""
    : new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Shanghai" }).format(
        value,
      );
}

function weekStart(date: string) {
  const value = new Date(`${date}T00:00:00+08:00`);
  if (Number.isNaN(value.getTime())) return date;
  const day = value.getUTCDay();
  value.setUTCDate(value.getUTCDate() + (day === 0 ? -6 : 1 - day));
  return value.toISOString().slice(0, 10);
}

/** Aggregate historical daily bars for the non-replayable higher periods. */
export function aggregateDailyBars(
  dailyBars: StockKlineBar[],
  period: "1d" | "1w" | "1mo",
  asOfDate?: string | null,
) {
  const filtered = dailyBars
    .filter((bar) => {
      const date = barTradeDate(bar);
      return date && (!asOfDate || date <= asOfDate);
    })
    .sort((a, b) => a.timestamp - b.timestamp);
  if (period === "1d") return filtered;

  const groups = new Map<string, StockKlineBar[]>();
  for (const bar of filtered) {
    const date = barTradeDate(bar);
    const key = period === "1w" ? weekStart(date) : date.slice(0, 7);
    const group = groups.get(key);
    if (group) group.push(bar);
    else groups.set(key, [bar]);
  }

  return [...groups.values()].map((group) => {
    const first = group[0];
    const last = group[group.length - 1];
    return {
      ...last,
      trade_date: barTradeDate(last),
      open: first.open,
      high: Math.max(...group.map((item) => item.high)),
      low: Math.min(...group.map((item) => item.low)),
      close: last.close,
      volume: group.reduce((sum, item) => sum + (item.volume || 0), 0),
      turnover: group.some((item) => item.turnover != null)
        ? group.reduce((sum, item) => sum + (item.turnover || 0), 0)
        : undefined,
    };
  });
}

/** Keep context indices on the same one-minute sampling as a timeshare replay. */
export function contextBarsForPeriod(
  response: {
    timeshare: StockIntradayPoint[];
    minute_bars: Partial<Record<"1m" | "5m" | "15m" | "30m", StockKlineBar[]>>;
  } | null,
  period: ReplayPeriod,
) {
  if (period === "timeshare" && response?.minute_bars["1m"]?.length) {
    return response.minute_bars["1m"];
  }
  if (isHistoricalReplayPeriod(period)) return [];
  return barsForPeriod(response, period);
}

export function markerList(
  trades: PortfolioTrade[],
  anchors: PortfolioReviewAnchor[],
  events: MarketIndicatorEvent[],
) {
  const tradeAnchors = new Map(
    anchors
      .filter((item) => item.tradeId != null)
      .map((item) => [item.tradeId!, item]),
  );
  const tradeMarkers: ReplayMarker[] = trades.map((trade) => {
    const anchor = tradeAnchors.get(trade.id);
    return {
      id: `trade-${trade.id}`,
      kind: "trade",
      timestamp: epoch(trade.tradedAt),
      price: trade.price,
      label: trade.side === "BUY" ? "B" : "S",
      detail: anchor?.note ?? null,
      trade,
      anchor,
    };
  });
  const observations: ReplayMarker[] = anchors
    .filter((item) => item.anchorType === "OBSERVATION")
    .map((anchor) => ({
      id: `observation-${anchor.id ?? epoch(anchor.occurredAt)}`,
      kind: "observation",
      timestamp: epoch(anchor.occurredAt),
      price: anchor.price,
      label: "观",
      detail: anchor.note ?? null,
      anchor,
    }));
  const indicatorEvents: ReplayMarker[] = events.map((event, index) => ({
    id: `event-${event.id ?? `${event.code}-${index}`}`,
    kind: "event",
    timestamp: epoch(event.eventAt),
    price: event.lastPrice ?? null,
    label: "I",
    detail: `${event.name ?? event.code} · ${event.eventType}`,
    event,
  }));
  return [...tradeMarkers, ...observations, ...indicatorEvents]
    .filter((item) => item.timestamp > 0)
    .sort((a, b) => a.timestamp - b.timestamp);
}

export function nearestIndex<T extends { timestamp: number }>(
  items: T[],
  timestamp: number,
) {
  if (!items.length) return 0;
  let best = 0;
  let distance = Math.abs(items[0].timestamp - timestamp);
  for (let index = 1; index < items.length; index += 1) {
    const nextDistance = Math.abs(items[index].timestamp - timestamp);
    if (nextDistance < distance) {
      best = index;
      distance = nextDistance;
    }
  }
  return best;
}

export function asOf<
  T extends {
    tradeDate?: string | null;
    rq?: string | null;
    date?: string | null;
  },
>(rows: T[], date: string) {
  return rows.filter(
    (row) => (row.tradeDate ?? row.rq ?? row.date ?? "") <= date,
  );
}
