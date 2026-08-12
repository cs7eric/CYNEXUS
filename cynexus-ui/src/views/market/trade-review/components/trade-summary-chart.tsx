import { useEffect, useRef } from "react";
import * as echarts from "echarts/core";
import { BarChart, LineChart, ScatterChart } from "echarts/charts";
import {
  GridComponent,
  MarkLineComponent,
  TooltipComponent,
} from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import type { EChartsOption } from "echarts";

import type {
  StockIntradayPoint,
  StockKlineBar,
} from "@/views/stock-chart/lib/types";
import type { PortfolioTrade } from "@/services/market/portfolio/types";
import { epoch, formatTime } from "../lib/replay";

echarts.use([
  BarChart,
  LineChart,
  ScatterChart,
  GridComponent,
  MarkLineComponent,
  TooltipComponent,
  CanvasRenderer,
]);

interface TradeSummaryChartProps {
  points: StockIntradayPoint[];
  trades: PortfolioTrade[];
  preClose: number | null;
  name: string;
}

function toTimeline(points: StockIntradayPoint[]) {
  const byTimestamp = new Map<number, StockIntradayPoint>();
  for (const point of points) {
    const timestamp = epoch(point.timestamp);
    if (timestamp > 0 && Number.isFinite(point.price)) {
      byTimestamp.set(timestamp, { ...point, timestamp });
    }
  }
  return [...byTimestamp.values()].sort((left, right) => left.timestamp - right.timestamp);
}

function nearestIndex(bars: StockKlineBar[], timestamp: number) {
  let best = 0;
  let distance = Number.POSITIVE_INFINITY;
  bars.forEach((bar, index) => {
    const nextDistance = Math.abs(bar.timestamp - timestamp);
    if (nextDistance < distance) {
      best = index;
      distance = nextDistance;
    }
  });
  return best;
}

export function TradeSummaryChart({
  points,
  trades,
  preClose,
  name,
}: TradeSummaryChartProps) {
  const rootRef = useRef<HTMLDivElement | null>(null);
  const chartRef = useRef<echarts.ECharts | null>(null);

  useEffect(() => {
    const element = rootRef.current;
    if (!element) return;
    const chart = echarts.init(element, undefined, { renderer: "canvas" });
    chartRef.current = chart;
    const observer = new ResizeObserver(() => chart.resize());
    observer.observe(element);
    return () => {
      observer.disconnect();
      chart.dispose();
      chartRef.current = null;
    };
  }, []);

  useEffect(() => {
    const chart = chartRef.current;
    if (!chart) return;
    const timeline = toTimeline(points);
    if (!timeline.length) {
      chart.clear();
      return;
    }

    const bars: StockKlineBar[] = timeline.map((point) => ({
      timestamp: point.timestamp,
      trade_date: point.trade_date ?? undefined,
      open: point.open ?? point.price,
      high: point.price,
      low: point.price,
      close: point.price,
      volume: Number.isFinite(point.volume) ? point.volume : 0,
      turnover: point.turnover ?? undefined,
    }));
    const first = timeline[0];
    const referenceClose =
      preClose != null && Number.isFinite(preClose) && preClose > 0
        ? preClose
        : first.open != null && first.open > 0
          ? first.open
          : first.price;
    const displayValue = (value: number | null | undefined) =>
      value == null || !Number.isFinite(value) ? null : value - referenceClose;
    const priceValues = bars.map((bar) => displayValue(bar.close));
    const averageValues = timeline.map((point) => displayValue(point.avg_price));
    const maxVolume = Math.max(...bars.map((bar) => bar.volume), 1);
    const categories = timeline.map((point) => point.time_label || formatTime(point.timestamp).slice(0, 5));
    const markerData = trades.map((trade) => {
      const index = nearestIndex(bars, epoch(trade.tradedAt));
      return {
        value: [index, displayValue(trade.price)],
        itemStyle: {
          color: trade.side === "BUY" ? "#dc2626" : "#16a34a",
        },
        label: {
          show: true,
          formatter: trade.side === "BUY" ? "B" : "S",
          color: trade.side === "BUY" ? "#dc2626" : "#16a34a",
          fontSize: 10,
          fontWeight: 700,
          position: (trade.side === "BUY" ? "top" : "bottom") as "top" | "bottom",
        },
      };
    });
    const option: EChartsOption = {
      animation: false,
      grid: [
        { left: 8, right: 8, top: 10, height: 72 },
        { left: 8, right: 8, bottom: 18, height: 18 },
      ],
      tooltip: {
        trigger: "axis",
        confine: true,
        formatter: (params) => {
          const rows = Array.isArray(params) ? params : [params];
          const index = Number(rows[0]?.dataIndex ?? 0);
          const point = timeline[index];
          if (!point) return "";
          const tradeRows = trades.filter(
            (trade) => nearestIndex(bars, epoch(trade.tradedAt)) === index,
          );
          return [
            `${categories[index]} · ${point.price.toFixed(2)}`,
            point.avg_price != null ? `均价 ${point.avg_price.toFixed(2)}` : "",
            tradeRows.map((trade) => `${trade.side === "BUY" ? "B" : "S"} ${trade.price.toFixed(2)}`).join(" · "),
          ].filter(Boolean).join("<br />");
        },
      },
      xAxis: [
        {
          type: "category",
          gridIndex: 0,
          data: categories,
          boundaryGap: false,
          axisLine: { lineStyle: { color: "#d4d4d8" } },
          axisTick: { show: false },
          axisLabel: { show: false },
        },
        {
          type: "category",
          gridIndex: 1,
          data: categories,
          boundaryGap: true,
          axisLine: { lineStyle: { color: "#d4d4d8" } },
          axisTick: { show: false },
          axisLabel: {
            color: "#737373",
            fontSize: 9,
            interval: Math.max(0, Math.ceil(categories.length / 4) - 1),
          },
        },
      ],
      yAxis: [
        {
          type: "value",
          gridIndex: 0,
          scale: true,
          axisLabel: { show: false },
          axisLine: { show: false },
          axisTick: { show: false },
          splitLine: { lineStyle: { color: "#f0f0f0", type: "dashed" } },
        },
        {
          type: "value",
          gridIndex: 1,
          max: maxVolume,
          axisLabel: { show: false },
          axisLine: { show: false },
          axisTick: { show: false },
          splitLine: { show: false },
        },
      ],
      series: [
        {
          name: "价格",
          type: "line",
          xAxisIndex: 0,
          yAxisIndex: 0,
          data: priceValues,
          showSymbol: false,
          connectNulls: false,
          lineStyle: { color: priceValues.at(-1)! >= 0 ? "#dc2626" : "#16a34a", width: 1.5 },
          itemStyle: { color: priceValues.at(-1)! >= 0 ? "#dc2626" : "#16a34a" },
          markLine: {
            silent: true,
            symbol: "none",
            lineStyle: { color: "#a1a1aa", type: "dashed", width: 1 },
            data: [{ yAxis: 0 }],
          },
        },
        {
          name: "均价",
          type: "line",
          xAxisIndex: 0,
          yAxisIndex: 0,
          data: averageValues,
          showSymbol: false,
          connectNulls: false,
          lineStyle: { color: "#d97706", width: 1, type: "dashed" },
        },
        {
          name: "成交量",
          type: "bar",
          xAxisIndex: 1,
          yAxisIndex: 1,
          data: bars.map((bar, index) => ({
            value: bar.volume,
            itemStyle: {
              color: index === 0 || bar.close >= bars[index - 1].close ? "#dc2626" : "#16a34a",
              opacity: 0.28,
            },
          })),
          barMaxWidth: 5,
        },
        {
          name: "B/S",
          type: "scatter",
          xAxisIndex: 0,
          yAxisIndex: 0,
          data: markerData,
          symbolSize: 7,
          z: 5,
        },
      ],
    };
    chart.setOption(option, true);
  }, [name, points, preClose, trades]);

  return <div ref={rootRef} className="h-[122px] w-full" role="img" aria-label={`${name} 分时与 B/S 标记`} />;
}
