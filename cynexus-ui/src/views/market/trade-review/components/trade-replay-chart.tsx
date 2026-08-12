import { useEffect, useRef, useState, type ReactNode } from "react";
import { useDefaultLayout } from "react-resizable-panels";
import * as echarts from "echarts/core";
import {
  BarChart,
  CandlestickChart,
  CustomChart,
  LineChart,
  ScatterChart,
} from "echarts/charts";
import {
  DataZoomComponent,
  GridComponent,
  LegendComponent,
  MarkLineComponent,
  TooltipComponent,
} from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import type { EChartsOption } from "echarts";

import type {
  StockKlineBar,
  StockIntradayPoint,
} from "@/views/stock-chart/lib/types";
import type { ReplayMarker } from "../lib/replay";
import { barIndexForMarker, epoch, formatTime } from "../lib/replay";
import {
  ResizableHandle,
  ResizablePanel,
  ResizablePanelGroup,
} from "@/components/ui/resizable";

echarts.use([
  BarChart,
  CandlestickChart,
  CustomChart,
  LineChart,
  ScatterChart,
  DataZoomComponent,
  GridComponent,
  LegendComponent,
  MarkLineComponent,
  TooltipComponent,
  CanvasRenderer,
]);

interface TradeReplayChartProps {
  bars: StockKlineBar[];
  timeshare: StockIntradayPoint[];
  /** Previous-session close. Unlike today's open, this is the 0% line. */
  preClose?: number | null;
  overlays: Array<{
    name: string;
    bars: StockKlineBar[];
    color: string;
    mode?: "absolute" | "relative";
    /** Visual hint used to distinguish the context lines at a glance. */
    lineType?: "solid" | "dashed" | "dotted";
    lineWidth?: number;
  }>;
  cursor: number;
  period: "timeshare" | "1m" | "5m" | "15m" | "30m" | "1d" | "1w" | "1mo";
  markers: ReplayMarker[];
  relativeMode: boolean;
  onMarkerSelect: (marker: ReplayMarker) => void;
  /** Context-sensitive content rendered below the two synchronized plots. */
  lowerContent?: ReactNode;
  lowerLabel?: string;
}

function normalized(values: Array<number | null>) {
  const initial = values.find(
    (value): value is number => value != null && Number.isFinite(value),
  );
  return values.map((value) =>
    initial && value != null ? (value / initial - 1) * 100 : null,
  );
}

function label(
  timestamp: number,
  period: TradeReplayChartProps["period"] = "timeshare",
) {
  if (period === "1d" || period === "1w" || period === "1mo") {
    return new Intl.DateTimeFormat("zh-CN", {
      month: "2-digit",
      day: "2-digit",
      timeZone: "Asia/Shanghai",
    }).format(new Date(timestamp));
  }
  return formatTime(timestamp).slice(0, 5);
}

const DEFAULT_PANE_LAYOUT = {
  price: 50,
  context: 50,
};

function played<T>(values: T[], cursor: number) {
  return values.map((value, index) => (index <= cursor ? value : null));
}

/**
 * Candlestick data cannot use `null` for unrevealed items. ECharts' whisker
 * series assumes every item is an array/data item during initialisation and
 * will otherwise try to read `.value` from the null placeholder. Use the
 * documented empty-value marker instead so the category axis stays stable
 * while replayed candles are progressively revealed.
 */
function playedCandles(
  values: Array<[number, number, number, number]>,
  cursor: number,
) {
  return values.map((value, index) => (index <= cursor ? value : "-"));
}

function stableRange(values: Array<number | null>, minAtZero = false) {
  const usable = values.filter(
    (value): value is number => value != null && Number.isFinite(value),
  );
  if (!usable.length) return { min: minAtZero ? 0 : 0, max: 1 };
  const low = minAtZero ? 0 : Math.min(...usable);
  const high = Math.max(...usable);
  const span = Math.max(high - low, Math.abs(high) * 0.01, 0.01);
  return { min: minAtZero ? 0 : low - span * 0.08, max: high + span * 0.08 };
}

/** Keep the y-axis tied to the visible values instead of a fixed/symmetric domain. */
function dynamicRange(values: Array<number | null>, includeBaseline = false) {
  const usable = values.filter(
    (value): value is number => value != null && Number.isFinite(value),
  );
  if (!usable.length) return { min: -1, max: 1 };
  const low = includeBaseline ? Math.min(...usable, 0) : Math.min(...usable);
  const high = includeBaseline ? Math.max(...usable, 0) : Math.max(...usable);
  const span = Math.max(high - low, 0.0001);
  const padding = Math.max(span * 0.12, 0.005);
  return { min: low - padding, max: high + padding };
}

/**
 * A one-price (typically 一字涨停/跌停) intraday chart still needs a useful
 * percentage scale. Keep 0% exactly in the centre instead of putting the
 * flat price at the edge of an otherwise nearly empty domain.
 */
function centeredTimeshareRange(values: Array<number | null>) {
  const usable = values.filter(
    (value): value is number => value != null && Number.isFinite(value),
  );
  if (!usable.length) return { min: -1, max: 1 };
  const low = Math.min(...usable);
  const high = Math.max(...usable);
  const epsilon = Math.max(Math.abs(low), Math.abs(high), 1) * 0.000_001;
  const isOnePrice = Math.abs(high - low) <= epsilon;
  if (!isOnePrice) return dynamicRange([...usable, 0], true);

  const priceDelta = usable[0];
  // Preserve a legible domain for a flat-at-yesterday-close instrument too.
  const halfSpan = Math.max(Math.abs(priceDelta) * 1.08, 0.5);
  return { min: -halfSpan, max: halfSpan };
}

type SignedSegment = {
  side: "up" | "down";
  values: [number, number, number, number];
  startIntensity: number;
  endIntensity: number;
};

type SignedSliceRenderApi = {
  value: (dimension: number) => unknown;
  coord: (data: [number, number]) => [number, number];
};

type ReplayMarkerRenderApi = {
  value: (dimension: number) => unknown;
  coord: (data: [number, number]) => [number, number];
};

type ReplayMarkerRenderParams = {
  coordSys?: { y: number; height: number };
};

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}

function renderReplayMarker(
  params: ReplayMarkerRenderParams,
  api: ReplayMarkerRenderApi,
) {
  const x = Number(api.value(0));
  const price = Number(api.value(1));
  const guidePrice = Number(api.value(2));
  const [pointX, pointY] = api.coord([x, price]);
  const [, guideY] = api.coord([x, Number.isFinite(guidePrice) ? guidePrice : price]);
  // Keep rendering metadata in numeric dimensions. ECharts custom-series
  // params.data may be normalized to the value array and drop extra object
  // fields, which previously made every trade fall back to the blue "观".
  const markerKind = Number(api.value(3));
  const markerSide = Number(api.value(4));
  const isTrade = markerKind === 1;
  const label = isTrade ? (markerSide === 1 ? "B" : "S") : "观";
  const color = isTrade
    ? markerSide === 1
      ? "#dc2626"
      : "#16a34a"
    : "#2563eb";
  const badgeWidth = isTrade ? 24 : 28;
  const badgeHeight = 21;
  const guideLength = isTrade ? 24 : 18;
  const preferredDirection = isTrade && label === "S" ? 1 : -1;
  const top = params.coordSys?.y ?? 0;
  const bottom = top + (params.coordSys?.height ?? Number.POSITIVE_INFINITY);
  const preferredCenterY = guideY + preferredDirection * guideLength;
  const direction =
    preferredCenterY - badgeHeight / 2 < top ||
    preferredCenterY + badgeHeight / 2 > bottom
      ? preferredDirection * -1
      : preferredDirection;
  const badgeCenterY = guideY + direction * guideLength;

  return {
    type: "group",
    children: [
      {
        type: "line",
        shape: { x1: pointX, y1: pointY, x2: pointX, y2: badgeCenterY },
        style: { stroke: color, lineWidth: 1.5, lineDash: [5, 4], opacity: 0.95 },
      },
      {
        type: "circle",
        shape: { cx: pointX, cy: pointY, r: isTrade ? 3.5 : 3 },
        style: { fill: color, stroke: "#ffffff", lineWidth: 1.5 },
      },
      {
        type: "rect",
        shape: {
          x: pointX - badgeWidth / 2,
          y: badgeCenterY - badgeHeight / 2,
          width: badgeWidth,
          height: badgeHeight,
          r: 4,
        },
        style: {
          fill: color,
          stroke: "#ffffff",
          lineWidth: 1.5,
          shadowBlur: 4,
          shadowColor: "rgba(15, 23, 42, 0.35)",
        },
      },
      {
        type: "text",
        style: {
          x: pointX,
          y: badgeCenterY + 4,
          text: label,
          fill: "#ffffff",
          font: isTrade ? "800 13px sans-serif" : "700 11px sans-serif",
          textAlign: "center",
          textVerticalAlign: "middle",
        },
      },
    ],
  };
}

/** Split a price delta series at the 0%/昨收 line so the fill never crosses sides. */
function buildSignedSegments(values: Array<number | null>, maxAbsDelta: number) {
  const up: SignedSegment[] = [];
  const down: SignedSegment[] = [];
  const append = (side: "up" | "down", start: [number, number], end: [number, number]) => {
    if (start[0] === end[0] || (start[1] === 0 && end[1] === 0)) return;
    const segment: SignedSegment = {
      side,
      values: [start[0], start[1], end[0], end[1]],
      startIntensity: clamp(Math.abs(start[1]) / Math.max(maxAbsDelta, 0.01), 0, 1),
      endIntensity: clamp(Math.abs(end[1]) / Math.max(maxAbsDelta, 0.01), 0, 1),
    };
    (side === "up" ? up : down).push(segment);
  };

  for (let index = 1; index < values.length; index += 1) {
    const previous = values[index - 1];
    const current = values[index];
    if (previous == null || current == null || !Number.isFinite(previous) || !Number.isFinite(current)) continue;

    if (previous >= 0 && current >= 0 && (previous > 0 || current > 0)) {
      append("up", [index - 1, previous], [index, current]);
      continue;
    }
    if (previous <= 0 && current <= 0 && (previous < 0 || current < 0)) {
      append("down", [index - 1, previous], [index, current]);
      continue;
    }
    if (previous === current) continue;
    const ratio = previous / (previous - current);
    const cross: [number, number] = [index - 1 + clamp(ratio, 0, 1), 0];
    const firstSide = previous > 0 ? "up" : "down";
    append(firstSide, [index - 1, previous], cross);
    append(firstSide === "up" ? "down" : "up", cross, [index, current]);
  }
  return { up, down } as const;
}

function overlayValues(
  overlay: TradeReplayChartProps["overlays"][number],
  categories: string[],
  period: TradeReplayChartProps["period"],
) {
  const byMinute = new Map<string, number>();
  for (const bar of overlay.bars) {
    const value = Number(bar.close);
    if (Number.isFinite(value)) byMinute.set(label(bar.timestamp, period), value);
  }
  const values = categories.map((time) => byMinute.get(time) ?? null);
  return overlay.mode === "absolute" ? values : normalized(values);
}

/** Format share/lot volume with compact Chinese units for chart labels. */
function formatVolume(value: number) {
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

function formatAxisValue(value: unknown, percent = false) {
  const number = typeof value === "number" ? value : Number(value);
  if (!Number.isFinite(number)) return value == null ? "" : String(value);
  const normalized = Object.is(number, -0) ? 0 : number;
  const formatted = normalized.toFixed(2).replace(/\.?(0+)$/, "");
  return `${formatted}${percent ? "%" : ""}`;
}

function formatTooltipValue(
  seriesName: string | undefined,
  value: unknown,
  relativeMode: boolean,
) {
  const raw = Array.isArray(value) ? value.at(-1) : value;
  const number = typeof raw === "number" ? raw : Number(raw);
  if (!Number.isFinite(number)) return raw == null ? "-" : String(raw);
  if (seriesName === "成交量") {
    return formatVolume(number);
  }
  const percent =
    seriesName?.includes("行业") ||
    (relativeMode && (seriesName === "价格" || seriesName === "均价"));
  return `${number.toFixed(2)}${percent ? "%" : ""}`;
}

export function TradeReplayChart({
  bars,
  timeshare,
  preClose,
  overlays,
  cursor,
  period,
  markers,
  relativeMode,
  onMarkerSelect,
  lowerContent,
  lowerLabel,
}: TradeReplayChartProps) {
  const rootRef = useRef<HTMLDivElement | null>(null);
  const chartRef = useRef<echarts.ECharts | null>(null);
  const zoomWindowRef = useRef({ start: 0, end: 100 });
  const zoomKeyRef = useRef<string | null>(null);
  const markersRef = useRef(markers);
  const onMarkerSelectRef = useRef(onMarkerSelect);
  const chartHeightLayout = useDefaultLayout({
    id: "cynexus.trade-review.chart-height",
    onlySaveAfterUserInteractions: true,
  });
  const chartPaneLayout = useDefaultLayout({
    id: "cynexus.trade-review.chart-panes",
    onlySaveAfterUserInteractions: true,
  });
  const [paneLayout, setPaneLayout] =
    useState<Record<string, number>>(
      () => chartPaneLayout.defaultLayout ?? DEFAULT_PANE_LAYOUT,
    );
  useEffect(() => {
    markersRef.current = markers;
    onMarkerSelectRef.current = onMarkerSelect;
  }, [markers, onMarkerSelect]);

  useEffect(() => {
    const element = rootRef.current;
    if (!element) return;
    const chart = echarts.init(element, undefined, { renderer: "canvas" });
    chartRef.current = chart;
    const observer = new ResizeObserver(() => chart.resize());
    observer.observe(element);
    chart.on("click", (params) => {
      const markerId = (params.data as { markerId?: string } | undefined)
        ?.markerId;
      if (markerId) {
        const marker = markersRef.current.find((item) => item.id === markerId);
        if (marker) onMarkerSelectRef.current(marker);
      }
    });
    return () => {
      observer.disconnect();
      chart.dispose();
      chartRef.current = null;
    };
  }, []);

  useEffect(() => {
    const chart = chartRef.current;
    if (!chart) return;
    const categories = bars.map((bar) => label(bar.timestamp, period));
    const averageByTimestamp = new Map(
      timeshare.map((point) => [
        epoch(point.timestamp),
        point.avg_price ?? null,
      ]),
    );
    const averages = bars.map(
      (bar) => averageByTimestamp.get(bar.timestamp) ?? null,
    );
    const baseClose = bars.map((bar) => bar.close);
    const isTimeshare = period === "timeshare";
    const firstTimesharePoint = timeshare[0] ?? null;
    const firstReplayBar = bars[0] ?? null;
    const firstTimesharePrice = firstTimesharePoint?.price;
    const referenceClose =
      isTimeshare && preClose != null && Number.isFinite(preClose) && preClose > 0
        ? preClose
        : isTimeshare && firstTimesharePoint?.open != null && firstTimesharePoint.open > 0
        ? firstTimesharePoint.open
        : isTimeshare && firstTimesharePrice != null && firstTimesharePrice > 0
          ? firstTimesharePrice
          : isTimeshare && firstReplayBar && firstReplayBar.open > 0
            ? firstReplayBar.open
            : isTimeshare && firstReplayBar && firstReplayBar.close > 0
              ? firstReplayBar.close
              : null;
    const displayClose = isTimeshare
      ? baseClose.map((value) =>
          referenceClose != null
            ? relativeMode
              ? (value / referenceClose - 1) * 100
              : value - referenceClose
            : value,
        )
      : relativeMode
        ? normalized(baseClose)
        : baseClose;
    const displayAverages = isTimeshare
      ? averages.map((value) =>
          value == null || referenceClose == null
            ? value
            : relativeMode
              ? (value / referenceClose - 1) * 100
              : value - referenceClose,
        )
      : relativeMode
        ? normalized(averages)
        : averages;
    const timeshareMaxAbsDelta = Math.max(
      ...displayClose
        .filter((value): value is number => value != null && Number.isFinite(value))
        .map((value) => Math.abs(value)),
      0.01,
    );
    const replayDisplayClose = displayClose.map((value, index) =>
      index <= cursor ? value : null,
    );
    const signedSegments = isTimeshare
      ? buildSignedSegments(replayDisplayClose, timeshareMaxAbsDelta)
      : { up: [], down: [] };
    const firstClose = baseClose[0];
    const markerReferenceClose = isTimeshare ? referenceClose : firstClose;
    const displayMarkerValue = (value: number) => {
      if (markerReferenceClose == null) return value;
      if (relativeMode) return (value / markerReferenceClose - 1) * 100;
      return isTimeshare ? value - markerReferenceClose : value;
    };
    const markerData = markers.flatMap((marker) => {
      if (marker.kind === "event" || marker.price == null) return [];
      const barIndex = barIndexForMarker(marker.timestamp, bars, period);
      if (barIndex < 0 || barIndex > cursor) return [];
      const markerPrice = displayMarkerValue(marker.price);
      const bar = bars[barIndex];
      const guideRawPrice =
        marker.kind === "trade"
          ? marker.label === "B"
            ? bar.high
            : bar.low
          : marker.price;
      const markerKind = marker.kind === "trade" ? 1 : 2;
      const markerSide =
        marker.kind === "trade" ? (marker.label === "B" ? 1 : 2) : 0;
      return [
        {
          value: [
            barIndex,
            markerPrice,
            displayMarkerValue(guideRawPrice),
            markerKind,
            markerSide,
          ],
          markerId: marker.id,
          detail: marker.detail,
          markerKind: marker.kind,
        },
      ];
    });
    const overlaySeries = overlays.map((overlay) => {
      const values = overlayValues(overlay, categories, period);
      return {
        name: overlay.name,
        type: "line" as const,
        xAxisIndex: 1,
        yAxisIndex: overlay.mode === "absolute" ? 3 : 2,
        data: played(values, cursor),
        symbol: "circle",
        showSymbol: true,
        symbolSize: (_value: unknown, params: { dataIndex: number }) => params.dataIndex === cursor ? 5 : 0,
        connectNulls: true,
        lineStyle: {
          // Keep the two context series visually distinct even when their
          // colours are close or the chart is viewed in a compact pane:
          // industry is a solid trend line while the broad-market index is
          // rendered as a dashed line. Callers can override this per series.
          width:
            overlay.lineWidth ??
            (overlay.mode === "absolute" ? 2.2 : 2.4),
          type:
            overlay.lineType ??
            (overlay.mode === "absolute" ? "dashed" : "solid"),
          color: overlay.color,
        },
        itemStyle: { color: overlay.color },
      };
    });
    const relativeOverlayValues = overlays
      .filter((overlay) => overlay.mode !== "absolute")
      .flatMap((overlay) => overlayValues(overlay, categories, period));
    const absoluteOverlayValues = overlays
      .filter((overlay) => overlay.mode === "absolute")
      .flatMap((overlay) => overlayValues(overlay, categories, period));
    const timelineMax = Math.max(0, categories.length - 1);
    const historicalPeriod =
      period === "1d" || period === "1w" || period === "1mo";
    const preferredWindow =
      period === "1d" ? 60 : period === "1w" ? 80 : 60;
    const initialZoomStart =
      historicalPeriod && categories.length > preferredWindow
        ? ((categories.length - preferredWindow) / categories.length) * 100
        : 0;
    const zoomKey = `${period}:${bars[0]?.timestamp ?? 0}:${bars.at(-1)?.timestamp ?? 0}:${bars.length}`;
    if (zoomKeyRef.current !== zoomKey) {
      zoomKeyRef.current = zoomKey;
      zoomWindowRef.current = { start: initialZoomStart, end: 100 };
    }
    const zoomStart = zoomWindowRef.current.start;
    const zoomEnd = zoomWindowRef.current.end;
    const rangesForZoom = (startPercent: number, endPercent: number) => {
      const startIndex = Math.max(
        0,
        Math.floor((startPercent / 100) * Math.max(0, categories.length - 1)),
      );
      const endIndex = Math.min(
        Math.max(0, categories.length - 1),
        Math.ceil((endPercent / 100) * Math.max(0, categories.length - 1)),
      );
      const visibleIndices = Array.from(
        { length: Math.max(0, endIndex - startIndex + 1) },
        (_, offset) => startIndex + offset,
      ).filter((index) => index <= Math.max(0, cursor));
      const effectiveIndices = visibleIndices.length
        ? visibleIndices
        : [Math.min(Math.max(0, cursor), Math.max(0, bars.length - 1))];
      const valuesAtZoom = <T,>(values: T[]) =>
        effectiveIndices.map((index) => values[index]).filter((value) => value != null);
      const visibleBars = effectiveIndices
        .map((index) => bars[index])
        .filter((bar): bar is (typeof bars)[number] => Boolean(bar));
      const visibleAverages = valuesAtZoom(
        isTimeshare || relativeMode ? displayAverages : averages,
      ) as Array<number | null>;
      const visibleRelativeOverlayValues = valuesAtZoom(relativeOverlayValues) as Array<
        number | null
      >;
      const visibleAbsoluteOverlayValues = valuesAtZoom(absoluteOverlayValues) as Array<
        number | null
      >;
      const priceValues = [
        ...(valuesAtZoom(displayClose) as Array<number | null>),
        ...visibleAverages,
      ];
      return {
        price: isTimeshare
          ? centeredTimeshareRange(priceValues)
          : stableRange(
              relativeMode
                ? [
                    ...(valuesAtZoom(displayClose) as Array<number | null>),
                    ...visibleAverages,
                  ]
                : [
                    ...visibleBars.flatMap((bar) => [bar.low, bar.high]),
                    ...visibleAverages,
                  ],
            ),
        volume: stableRange(
          visibleBars.map((bar) => bar.volume),
          true,
        ),
        relative: stableRange(visibleRelativeOverlayValues),
        absolute: stableRange(visibleAbsoluteOverlayValues),
      };
    };
    const visibleRanges = rangesForZoom(zoomStart, zoomEnd);
    const priceSize = paneLayout.price ?? 50;
    const contextLeft = Math.min(72, priceSize + 2);
    // The price and context panes share one time index but are displayed
    // side-by-side. Volume remains a compact band inside the price pane.
    const volumeAxisMax = Math.max(visibleRanges.volume.max / 0.22, 1);
    const renderSignedSlice = (side: "up" | "down") =>
      (_params: unknown, api: SignedSliceRenderApi) => {
        const x1 = Number(api.value(0));
        const y1 = Number(api.value(1));
        const x2 = Number(api.value(2));
        const y2 = Number(api.value(3));
        const startIntensity = clamp(Number(api.value(4)) || 0, 0, 1);
        const endIntensity = clamp(Number(api.value(5)) || 0, 0, 1);
        const start = api.coord([x1, y1]);
        const end = api.coord([x2, y2]);
        const baseStart = api.coord([x1, 0]);
        const baseEnd = api.coord([x2, 0]);
        const isUp = side === "up";
        const color = isUp ? "220, 38, 38" : "22, 163, 74";
        const lineColor = isUp ? "220, 23, 23" : "5, 148, 71";
        const fill = {
          type: "linear" as const,
          x: start[0],
          y: 0,
          x2: end[0],
          y2: 0,
          global: true,
          colorStops: [
            { offset: 0, color: `rgba(${color}, ${0.08 + startIntensity * 0.3})` },
            { offset: 1, color: `rgba(${color}, ${0.08 + endIntensity * 0.3})` },
          ],
        };
        const stroke = {
          type: "linear" as const,
          x: start[0],
          y: 0,
          x2: end[0],
          y2: 0,
          global: true,
          colorStops: [
            { offset: 0, color: `rgba(${lineColor}, ${0.7 + startIntensity * 0.3})` },
            { offset: 1, color: `rgba(${lineColor}, ${0.7 + endIntensity * 0.3})` },
          ],
        };
        return {
          type: "group",
          children: [
            {
              type: "polygon",
              shape: { points: [baseStart, start, end, baseEnd] },
              style: { fill, stroke: "none" },
              silent: true,
            },
            {
              type: "polyline",
              shape: { points: [start, end] },
              style: {
                fill: "none",
                stroke,
                lineWidth: 2.6,
                lineCap: "butt",
                lineJoin: "round",
              },
              silent: true,
            },
          ],
        };
      };
    const option: EChartsOption = {
      animation: false,
      backgroundColor: "transparent",
      legend: [
        {
          top: 4,
          left: 8,
          data: ["价格", "均价", "BS", "观察点"],
          itemWidth: 12,
          itemHeight: 2,
          textStyle: { color: "#666", fontSize: 11 },
          selectedMode: false,
        },
        {
          // Keep the context legend attached to the resizable context pane.
          top: 4,
          left: `${Math.min(78, contextLeft + 1)}%`,
          data: overlays.map((overlay) => overlay.name),
          // A slightly longer legend swatch makes the solid-vs-dashed
          // distinction readable in the compact right-hand pane.
          itemWidth: 22,
          itemHeight: 3,
          itemGap: 12,
          textStyle: { color: "#666", fontSize: 10 },
          selectedMode: false,
        },
      ],
      tooltip: {
        trigger: "axis",
        axisPointer: {
          type: "cross",
          lineStyle: {
            color: "rgba(71, 85, 105, 0.22)",
            width: 6,
            type: "solid",
          },
          crossStyle: {
            color: "rgba(71, 85, 105, 0.12)",
            width: 1,
            type: "solid",
          },
          label: {
            show: true,
            backgroundColor: "#475569",
            color: "#fff",
            fontSize: 10,
            formatter: (params) => {
              const axisIndex =
                typeof params.axisIndex === "number" ? params.axisIndex : -1;
              const percent =
                axisIndex === 2 || (axisIndex === 0 && relativeMode);
              return formatAxisValue(params.value, percent);
            },
          },
        },
        borderColor: "#e5e7eb",
        textStyle: { color: "#252525", fontSize: 11 },
        formatter: (items: unknown) => {
          const rows = Array.isArray(items)
            ? (items as Array<{
                axisValueLabel?: string;
                seriesName?: string;
                value?: unknown;
                data?: { detail?: string | null };
              }>)
            : [];
          const first = rows[0];
          const lines = rows
            .slice(0, 5)
            .map(
              (row) => {
                const raw = Array.isArray(row.value) ? row.value.at(-1) : row.value;
                const numeric = typeof raw === "number" ? raw : Number(raw);
                const displayValue =
                  isTimeshare &&
                  !relativeMode &&
                  referenceClose != null &&
                  (row.seriesName === "价格" || row.seriesName === "均价") &&
                  Number.isFinite(numeric)
                    ? numeric + referenceClose
                    : row.value;
                return `${row.seriesName}: ${formatTooltipValue(row.seriesName, displayValue, relativeMode)}`;
              },
            );
          const note = rows.find((row) => row.data?.detail)?.data?.detail;
          return `<b>${first?.axisValueLabel ?? ""}</b><br/>${lines.join("<br/>")}${note ? `<br/><span style='color:#666'>${note}</span>` : ""}`;
        },
      },
      grid: [
        // Keep every pane's plot rectangle identical. The wider margins make
        // room for the longest left/right tick labels without changing the
        // actual time-axis geometry between panes.
        {
          left: "4%",
          width: `${Math.max(25, priceSize - 6)}%`,
          top: "10%",
          bottom: 30,
          containLabel: false,
        },
        {
          left: `${contextLeft}%`,
          right: "3%",
          top: "10%",
          bottom: 30,
          containLabel: false,
        },
      ],
      dataZoom: [
        {
          type: "inside",
          xAxisIndex: [0, 1],
          filterMode: "none",
          start: zoomStart,
          end: zoomEnd,
          zoomOnMouseWheel: true,
          moveOnMouseMove: true,
          moveOnMouseWheel: true,
          preventDefaultMouseMove: true,
          throttle: 50,
        },
        ...(historicalPeriod
          ? [
              {
                type: "slider" as const,
                xAxisIndex: [0, 1],
                filterMode: "none" as const,
                start: zoomStart,
                end: zoomEnd,
                bottom: 2,
                height: 14,
                showDetail: false,
                brushSelect: false,
                borderColor: "transparent",
                backgroundColor: "rgba(148, 163, 184, 0.12)",
                fillerColor: "rgba(100, 116, 139, 0.2)",
                handleStyle: { color: "#64748b" },
                dataBackground: {
                  lineStyle: { color: "rgba(100, 116, 139, 0.35)" },
                  areaStyle: { color: "rgba(148, 163, 184, 0.12)" },
                },
              },
            ]
          : []),
      ],
      axisPointer: {
        link: [{ xAxisIndex: [0, 1] }],
        lineStyle: {
          color: "rgba(71, 85, 105, 0.22)",
          width: 6,
          type: "solid",
        },
      },
      xAxis: [
        {
          type: "category",
          data: categories,
          min: 0,
          max: timelineMax,
          boundaryGap: true,
          axisLabel: {
            color: "#888",
            fontSize: 10,
            interval: Math.max(0, Math.floor(categories.length / 6)),
          },
          axisTick: { show: false },
          axisLine: { lineStyle: { color: "#ddd" } },
          axisPointer: {
            show: true,
            lineStyle: {
              color: "rgba(71, 85, 105, 0.22)",
              width: 6,
              type: "solid",
            },
          },
        },
        {
          type: "category",
          gridIndex: 1,
          data: categories,
          min: 0,
          max: timelineMax,
          boundaryGap: true,
          axisLabel: {
            color: "#888",
            fontSize: 10,
            interval: Math.max(0, Math.floor(categories.length / 6)),
          },
          axisTick: { show: false },
          axisLine: { lineStyle: { color: "#ddd" } },
          axisPointer: {
            show: true,
            lineStyle: {
              color: "rgba(71, 85, 105, 0.22)",
              width: 6,
              type: "solid",
            },
          },
        },
      ],
      yAxis: [
        {
          type: "value",
          min: visibleRanges.price.min,
          max: visibleRanges.price.max,
          axisLabel: {
            color: "#777",
            fontSize: 10,
            formatter: (value: number) =>
              isTimeshare && !relativeMode && referenceClose != null
                ? formatAxisValue(value + referenceClose)
                : formatAxisValue(value, relativeMode),
          },
          splitNumber: 5,
          splitLine: { lineStyle: { color: "#eee" } },
        },
        {
          type: "value",
          gridIndex: 0,
          min: 0,
          max: volumeAxisMax,
          show: false,
        },
        {
          type: "value",
          gridIndex: 1,
          min: visibleRanges.relative.min,
          max: visibleRanges.relative.max,
          position: "left",
          axisLabel: {
            show: relativeOverlayValues.length > 0,
            color: "#777",
            fontSize: 10,
            formatter: (value: number) => formatAxisValue(value, true),
          },
          splitNumber: 4,
          splitLine: { lineStyle: { color: "#eee" } },
        },
        {
          type: "value",
          gridIndex: 1,
          min: visibleRanges.absolute.min,
          max: visibleRanges.absolute.max,
          position: "right",
          axisLabel: {
            show: absoluteOverlayValues.length > 0,
            color: "#777",
            fontSize: 10,
            formatter: (value: number) => formatAxisValue(value),
          },
          splitNumber: 4,
          splitLine: { show: false },
        },
      ],
      series: [
        period !== "timeshare" && !relativeMode
          ? {
              name: "价格",
              type: "candlestick",
              data: playedCandles(
                bars.map((bar) => [bar.open, bar.close, bar.low, bar.high]),
                cursor,
              ),
              itemStyle: {
                color: "#dc2626",
                color0: "#16a34a",
                borderColor: "#dc2626",
                borderColor0: "#16a34a",
              },
              markLine: {
                silent: true,
                symbol: "none",
                lineStyle: { color: "#475569", type: "dashed" },
                data: [{ xAxis: categories[Math.max(0, cursor)] }],
              },
            }
          : {
              name: "价格",
              type: "line",
              data: played(displayClose, cursor),
              symbol: "none",
              lineStyle: isTimeshare
                ? { color: "#334155", width: 0, opacity: 0 }
                : { color: "#334155", width: 2 },
              markLine: {
                silent: true,
                symbol: "none",
                data: isTimeshare
                  ? [
                      {
                        xAxis: categories[Math.max(0, cursor)],
                        lineStyle: { color: "#475569", type: "dashed", width: 1 },
                      },
                    ]
                  : [
                      {
                        xAxis: categories[Math.max(0, cursor)],
                        lineStyle: { color: "#475569", type: "dashed" },
                      },
                    ],
              },
            },
        ...(isTimeshare
          ? [
              {
                name: "上涨区域",
                type: "custom" as const,
                coordinateSystem: "cartesian2d" as const,
                clip: true,
                renderItem: renderSignedSlice("up"),
                data: signedSegments.up
                  .map((segment) => ({
                    value: [
                      ...segment.values,
                      segment.startIntensity,
                      segment.endIntensity,
                    ],
                  })),
                tooltip: { show: false },
                z: 4,
              },
              {
                name: "下跌区域",
                type: "custom" as const,
                coordinateSystem: "cartesian2d" as const,
                clip: true,
                renderItem: renderSignedSlice("down"),
                data: signedSegments.down
                  .map((segment) => ({
                    value: [
                      ...segment.values,
                      segment.startIntensity,
                      segment.endIntensity,
                    ],
                  })),
                tooltip: { show: false },
                z: 5,
              },
              {
                name: relativeMode ? "0%" : "昨收 / 0%",
                type: "line" as const,
                data: categories.map((_, index) => [index, 0]),
                showSymbol: false,
                silent: true,
                lineStyle: { color: "#cbd5e1", width: 1, type: "dashed" as const },
                tooltip: { show: false },
                z: 6,
              },
            ]
          : []),
        {
          name: "均价",
          type: "line",
          data: played(displayAverages, cursor),
          symbol: "none",
          connectNulls: true,
          lineStyle: { color: "#d97706", width: 2 },
          itemStyle: { color: "#d97706" },
          z: 5,
        },
        {
          name: "BS",
          type: "custom",
          coordinateSystem: "cartesian2d",
          renderItem: renderReplayMarker,
          data: markerData.filter((marker) => marker.markerKind === "trade"),
          silent: false,
          z: 20,
        },
        {
          name: "观察点",
          type: "custom",
          coordinateSystem: "cartesian2d",
          renderItem: renderReplayMarker,
          data: markerData.filter((marker) => marker.markerKind === "observation"),
          silent: false,
          z: 20,
        },
        {
          name: "成交量",
          type: "bar",
          xAxisIndex: 0,
          yAxisIndex: 1,
          data: played(
            bars.map((bar, index) => ({
              value: bar.volume,
              itemStyle: {
                color:
                  bar.close >=
                  (isTimeshare ? bars[index - 1]?.close ?? bar.open : bar.open)
                    ? "#dc262680"
                    : "#16a34a80",
              },
            })),
            cursor,
          ),
          barMaxWidth: 8,
          z: 1,
        },
        ...overlaySeries,
      ] as EChartsOption["series"],
    };
    const handleDataZoom = (rawParams: unknown) => {
      const params = rawParams as {
        start?: unknown;
        end?: unknown;
        batch?: Array<{ start?: unknown; end?: unknown }>;
      };
      const source = params.batch?.[0] ?? params;
      const nextStart = Number(source.start);
      const nextEnd = Number(source.end);
      if (!Number.isFinite(nextStart) || !Number.isFinite(nextEnd)) return;
      zoomWindowRef.current = {
        start: Math.max(0, Math.min(100, nextStart)),
        end: Math.max(0, Math.min(100, nextEnd)),
      };
      const nextRanges = rangesForZoom(
        zoomWindowRef.current.start,
        zoomWindowRef.current.end,
      );
      chart.setOption(
        {
          yAxis: [
            { min: nextRanges.price.min, max: nextRanges.price.max },
            { min: 0, max: Math.max(nextRanges.volume.max / 0.22, 1) },
            {
              min: nextRanges.relative.min,
              max: nextRanges.relative.max,
            },
            {
              min: nextRanges.absolute.min,
              max: nextRanges.absolute.max,
            },
          ],
        },
        { lazyUpdate: true },
      );
    };
    chart.off("dataZoom");
    chart.on("dataZoom", handleDataZoom);
    chart.setOption(option, { notMerge: true, lazyUpdate: true });
    return () => {
      chart.off("dataZoom", handleDataZoom);
    };
  }, [
    bars,
    cursor,
    markers,
    overlays,
    period,
    relativeMode,
    timeshare,
    paneLayout,
    preClose,
  ]);

  return (
    <div
      // The replay chart must never introduce a second browser scrollbar.
      // Let the resizable panels shrink below their preferred height/width
      // and let ECharts resize to the available rectangle instead.
      className="relative h-full min-h-0 min-w-0 w-full overflow-hidden"
      aria-label="个股盘中价格、均价、板块和大盘叠加图"
    >
      <ResizablePanelGroup
        id="trade-review-chart-height"
        orientation="vertical"
        defaultLayout={
          chartHeightLayout.defaultLayout ?? { chart: 64, spacer: 36 }
        }
        onLayoutChanged={chartHeightLayout.onLayoutChanged}
      >
        <ResizablePanel
          id="chart-pane"
          defaultSize="64%"
          minSize="30%"
          className="min-h-0 min-w-0 overflow-hidden"
        >
          <div className="relative h-full min-h-0 min-w-0 w-full overflow-hidden">
            <div ref={rootRef} className="absolute inset-0 z-0" />
            <div className="pointer-events-none absolute inset-0 z-10">
              <ResizablePanelGroup
                id="trade-review-chart-panes"
                orientation="horizontal"
                defaultLayout={chartPaneLayout.defaultLayout ?? DEFAULT_PANE_LAYOUT}
                onLayoutChanged={(layout, meta) => {
                  setPaneLayout(layout);
                  chartPaneLayout.onLayoutChanged(layout, meta);
                }}
                className="pointer-events-none min-h-0 min-w-0 overflow-hidden"
              >
                <ResizablePanel
                  id="price"
                  defaultSize="50%"
                  minSize="30%"
                  className="pointer-events-none min-w-0 overflow-hidden"
                />
                <ResizableHandle
                  withHandle
                  aria-label="调整标的与行业大盘图宽度"
                  className="pointer-events-auto z-20 h-full w-px min-w-px cursor-col-resize border-0 bg-border transition-colors hover:bg-primary [&>div]:h-3 [&>div]:w-2 [&>div]:border-border [&>div]:bg-muted"
                />
                <ResizablePanel
                  id="context"
                  defaultSize="50%"
                  minSize="30%"
                  className="pointer-events-none min-w-0 overflow-hidden"
                />
              </ResizablePanelGroup>
            </div>
          </div>
        </ResizablePanel>
        <ResizableHandle
          withHandle
          aria-label="调整图表与下方空白区域高度"
          className="z-20 h-px min-h-px cursor-row-resize border-0 bg-border transition-colors hover:bg-primary [&>div]:h-3 [&>div]:w-2 [&>div]:border-border [&>div]:bg-muted"
        />
        <ResizablePanel
          id="spacer"
          defaultSize="36%"
          minSize="20%"
          className="min-h-0 overflow-hidden"
        >
          <section className="flex h-full min-h-0 flex-col border-t border-border/60 bg-muted/10">
            <div className="flex h-8 shrink-0 items-center justify-between border-b border-border/60 px-3 text-[11px]">
              <span className="font-medium text-foreground">
                {lowerLabel ?? "当前时刻数据"}
              </span>
              <span className="text-muted-foreground">与上方时间轴同步</span>
            </div>
            <div className="min-h-0 flex-1 overflow-hidden">
              {lowerContent ?? (
                <div className="flex h-full items-center justify-center text-xs text-muted-foreground">
                  选择右侧模块查看对应的复盘数据
                </div>
              )}
            </div>
          </section>
        </ResizablePanel>
      </ResizablePanelGroup>
    </div>
  );
}
