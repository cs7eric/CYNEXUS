import { useEffect, useMemo, useRef, useState } from "react"
import * as echarts from "echarts/core"
import { BarChart, CandlestickChart, LineChart } from "echarts/charts"
import {
  DataZoomComponent,
  GridComponent,
  LegendComponent,
  MarkLineComponent,
  TooltipComponent,
} from "echarts/components"
import { CanvasRenderer } from "echarts/renderers"
import type { EChartsOption } from "echarts"

import type { IndustryApplicationTdxFundFlowPoint } from "@/services/market/industry-application"

echarts.use([
  BarChart,
  CandlestickChart,
  LineChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  DataZoomComponent,
  MarkLineComponent,
  CanvasRenderer,
])

type KlinePoint = {
  tradeDate: string
  open: number | null
  high: number | null
  low: number | null
  close: number | null
  amount?: number | null
}

type Props = {
  history: IndustryApplicationTdxFundFlowPoint[]
  klineRows?: KlinePoint[]
  fundOnly?: boolean
}

function formatYi(value: number | null | undefined) {
  if (value == null || !Number.isFinite(value)) return "—"
  const yi = value / 1e8
  if (Math.abs(yi) >= 1000) return `${(yi / 1000).toFixed(2)}万亿`
  return `${yi.toFixed(2)}亿`
}

function formatPrice(value: number | null | undefined) {
  if (value == null || !Number.isFinite(value)) return "—"
  return value.toFixed(2)
}

function formatAxisYi(value: number) {
  const yi = value / 1e8
  if (Math.abs(yi) >= 1000) return `${(yi / 1000).toFixed(2)}万亿`
  if (Math.abs(yi) >= 100) return `${yi.toFixed(0)}亿`
  if (Math.abs(yi) >= 10) return `${yi.toFixed(1)}亿`
  return `${yi.toFixed(2)}亿`
}

function pickNiceAmountStepYi(valueYi: number) {
  if (valueYi >= 5000) return 500
  if (valueYi >= 1000) return 100
  if (valueYi >= 500) return 50
  if (valueYi >= 100) return 10
  if (valueYi >= 20) return 5
  if (valueYi >= 10) return 2
  return 1
}

export function IndustryTdxFundOverlayChart({ history, klineRows = [], fundOnly = false }: Props) {
  const ref = useRef<HTMLDivElement | null>(null)
  const chartRef = useRef<echarts.ECharts | null>(null)
  const [chartWidth, setChartWidth] = useState(1200)

  const chartData = useMemo(() => {
    const klineByDate = new Map(klineRows.map((item) => [item.tradeDate, item]))
    return history.map((item) => {
      const tradeDate = item.trade_date ?? ""
      const kline = klineByDate.get(tradeDate) ?? null
      return {
        tradeDate,
        label: tradeDate.slice(5),
        mainNet: item.main_net ?? 0,
        open: kline?.open ?? null,
        high: kline?.high ?? null,
        low: kline?.low ?? null,
        close: kline?.close ?? null,
        amount: kline?.amount ?? null,
      }
    })
  }, [history, klineRows])

  const option = useMemo(() => {
    const dates = chartData.map((item) => item.label)
    const isCompact = chartWidth < 720
    const fg = "#555"
    const fgLight = "#64748b"
    const axisLine = "#ccd3dd"
    const splitLine = "#d9dee8"

    const maxAbsFund = Math.max(1, ...chartData.map((item) => Math.abs(item.mainNet)))
    const positiveFlowValues = chartData.map((item) => (item.mainNet > 0 ? Math.abs(item.mainNet) : 0))
    const negativeFlowValues = chartData.map((item) => (item.mainNet < 0 ? Math.abs(item.mainNet) : 0))
    const priceValues = chartData.flatMap((item) =>
      [item.open, item.high, item.low, item.close].filter((value): value is number => value != null && Number.isFinite(value)),
    )
    const amountValues = chartData.map((item) => item.amount).filter((value): value is number => value != null && Number.isFinite(value))

    const priceMin = priceValues.length ? Math.min(...priceValues) : 0
    const priceMax = priceValues.length ? Math.max(...priceValues) : 1
    const pricePadding = Math.max((priceMax - priceMin) * 0.08, priceMax * 0.01, 1)
    const amountMax = amountValues.length ? Math.max(...amountValues) : 1
    const amountMin = amountValues.length ? Math.min(...amountValues) : 0

    const amountMaxYi = amountMax / 1e8
    const amountMinYi = amountMin / 1e8
    const amountRangeYi = Math.max(0, amountMaxYi - amountMinYi)
    const amountCompressionFloorYiRaw =
      amountMinYi > 0
        ? Math.max(
            0,
            amountMinYi * 0.7,
            amountMinYi - amountRangeYi * 0.25,
          )
        : 0
    const amountCompressionStepYi = pickNiceAmountStepYi(amountCompressionFloorYiRaw)
    const amountCompressionFloorYi =
      amountCompressionFloorYiRaw > 0
        ? Math.floor(amountCompressionFloorYiRaw / amountCompressionStepYi) * amountCompressionStepYi
        : 0
    const enableAmountCompression =
      amountCompressionFloorYi > 0 &&
      amountMinYi > amountCompressionFloorYi &&
      amountRangeYi > 0 &&
      amountMinYi / Math.max(amountMaxYi, 1) >= 0.28

    const AMOUNT_LOW_SCALE = 0.18
    const amountToVisual = (amountYuan: number | null | undefined) => {
      if (amountYuan == null || !Number.isFinite(amountYuan)) return null
      const amountYi = amountYuan / 1e8
      if (!enableAmountCompression) return amountYuan
      if (amountYi <= amountCompressionFloorYi) {
        return amountYi * AMOUNT_LOW_SCALE * 1e8
      }
      return (amountCompressionFloorYi * AMOUNT_LOW_SCALE + (amountYi - amountCompressionFloorYi)) * 1e8
    }

    const visualToAmount = (visualYuan: number) => {
      if (!enableAmountCompression) return visualYuan
      const visualYi = visualYuan / 1e8
      const compressedFloorVisualYi = amountCompressionFloorYi * AMOUNT_LOW_SCALE
      if (visualYi <= compressedFloorVisualYi) {
        return (visualYi / AMOUNT_LOW_SCALE) * 1e8
      }
      return (amountCompressionFloorYi + (visualYi - compressedFloorVisualYi)) * 1e8
    }

    const amountVisualValues = chartData.map((item) => amountToVisual(item.amount))
    const amountVisualMax = Math.max(
      1,
      ...amountVisualValues.filter((value): value is number => value != null && Number.isFinite(value)),
    )
    const amountCompressionMarkLine = amountToVisual(amountCompressionFloorYi * 1e8)
    const maxFundValue = Math.max(1, ...chartData.map((item) => Math.abs(item.mainNet)))

    if (fundOnly) {
      return {
        backgroundColor: "#fff",
        animation: false,
        tooltip: {
          trigger: "axis",
          axisPointer: {
            type: "cross",
            animation: false,
            label: { backgroundColor: "#505765" },
          },
          backgroundColor: "rgba(255,255,255,0.96)",
          borderColor: "#d9dee8",
          borderWidth: 1,
          padding: [8, 10],
          textStyle: { color: "#333", fontSize: 12 },
          formatter: (params: unknown) => {
            const items = params as Array<{
              axisValueLabel: string
              dataIndex: number
            }>
            if (!items?.length) return ""
            const idx = items[0].dataIndex
            const row = chartData[idx]
            return [
              `<div style="font-weight:600;margin-bottom:6px;">${row?.tradeDate || items[0].axisValueLabel}</div>`,
              `<div style="display:flex;justify-content:space-between;gap:16px;"><span style="color:#64748b;">资金净流</span><span>${formatYi(row?.mainNet ?? null)}</span></div>`,
            ].join("")
          },
        },
        grid: {
          left: isCompact ? 18 : 72,
          right: isCompact ? 18 : 48,
          top: 20,
          bottom: 48,
          containLabel: false,
        },
        xAxis: {
          type: "category",
          boundaryGap: false,
          data: dates,
          axisLine: { lineStyle: { color: axisLine } },
          axisTick: { show: false },
          axisLabel: { color: fgLight, fontSize: 11, margin: 8 },
        },
        yAxis: {
          type: "value",
          min: -maxFundValue * 1.12,
          max: maxFundValue * 1.12,
          splitNumber: 4,
          axisLabel: {
            color: fg,
            fontSize: isCompact ? 10 : 11,
            formatter: (value: number) => formatAxisYi(value),
          },
          splitLine: { lineStyle: { color: splitLine } },
          axisLine: { show: false },
          axisTick: { show: false },
        },
        dataZoom: [
          {
            show: true,
            realtime: true,
            xAxisIndex: [0],
            start: chartData.length > 40 ? Math.max(0, 100 - (40 / chartData.length) * 100) : 0,
            end: 100,
            height: isCompact ? 18 : 24,
            bottom: 4,
            borderColor: "#dbe3f3",
            backgroundColor: "#f5f7fc",
            fillerColor: "rgba(151, 171, 232, 0.35)",
            handleStyle: { color: "#aab8df" },
            moveHandleStyle: { color: "#aab8df" },
            textStyle: { color: fgLight, fontSize: isCompact ? 9 : 10 },
            filterMode: "none",
          },
          {
            type: "inside",
            realtime: true,
            xAxisIndex: [0],
            start: chartData.length > 40 ? Math.max(0, 100 - (40 / chartData.length) * 100) : 0,
            end: 100,
            filterMode: "none",
          },
        ],
        series: [
          {
            name: "资金净流",
            type: "line",
            data: chartData.map((item) => item.mainNet),
            symbol: "none",
            smooth: 0.2,
            lineStyle: { width: 2, color: "#2563eb" },
            areaStyle: { color: "rgba(37, 99, 235, 0.16)" },
            markLine: {
              symbol: "none",
              silent: true,
              lineStyle: {
                type: "dashed",
                color: "rgba(148,163,184,0.65)",
                width: 1,
              },
              data: [{ yAxis: 0 }],
            },
            z: 3,
          },
        ],
      } satisfies EChartsOption
    }

    const gridSide = isCompact ? 16 : 96
    const sideOffset = isCompact ? 10 : 42

    const MIDDLE_OVERLAY_RATIO = 0.3
    const MIDDLE_GRID_TOP = 22
    const MIDDLE_GRID_HEIGHT = 50
    const EXTRA_OVERLAY_HEIGHT = MIDDLE_GRID_HEIGHT * MIDDLE_OVERLAY_RATIO

    const FLOW_GRID_TOP = 8
    const FLOW_GRID_BASE_HEIGHT = 22
    const FLOW_GRID_HEIGHT = FLOW_GRID_BASE_HEIGHT + EXTRA_OVERLAY_HEIGHT

    const AMOUNT_GRID_BASE_TOP = 64
    const AMOUNT_GRID_BOTTOM = 86
    const AMOUNT_GRID_TOP = AMOUNT_GRID_BASE_TOP - EXTRA_OVERLAY_HEIGHT
    const AMOUNT_GRID_HEIGHT = AMOUNT_GRID_BOTTOM - AMOUNT_GRID_TOP

    const zoomStart = chartData.length > 40 ? Math.max(0, 100 - (40 / chartData.length) * 100) : 0

    return {
      backgroundColor: "#fff",
      animation: false,
      legend: {
        top: 0,
        left: 8,
        itemWidth: isCompact ? 12 : 18,
        itemHeight: isCompact ? 8 : 10,
        textStyle: { color: fg, fontSize: isCompact ? 10 : 12 },
        data: ["成交额", "资金净流", "日K"],
      },
      grid: [
        {
          left: gridSide,
          right: gridSide,
          top: `${FLOW_GRID_TOP}%`,
          height: `${FLOW_GRID_HEIGHT}%`,
          containLabel: false,
        },
        {
          left: gridSide,
          right: gridSide,
          top: `${MIDDLE_GRID_TOP}%`,
          height: `${MIDDLE_GRID_HEIGHT}%`,
          containLabel: false,
        },
        {
          left: gridSide,
          right: gridSide,
          top: `${AMOUNT_GRID_TOP}%`,
          height: `${AMOUNT_GRID_HEIGHT}%`,
          containLabel: false,
        },
      ],
      tooltip: {
        trigger: "axis",
        axisPointer: {
          type: "cross",
          animation: false,
          label: { backgroundColor: "#505765" },
        },
        backgroundColor: "rgba(255,255,255,0.96)",
        borderColor: "#d9dee8",
        borderWidth: 1,
        padding: [8, 10],
        textStyle: { color: "#333", fontSize: 12 },
        formatter: (params: unknown) => {
          const items = params as Array<{
            axisValueLabel: string
            dataIndex: number
          }>
          if (!items?.length) return ""
          const idx = items[0].dataIndex
          const row = chartData[idx]
          return [
            `<div style="font-weight:600;margin-bottom:6px;">${row?.tradeDate || items[0].axisValueLabel}</div>`,
            `<div style="display:flex;justify-content:space-between;gap:16px;"><span style="color:#64748b;">资金净流</span><span>${formatYi(row?.mainNet ?? null)}</span></div>`,
            `<div style="display:flex;justify-content:space-between;gap:16px;"><span style="color:#64748b;">开</span><span>${formatPrice(row?.open ?? null)}</span></div>`,
            `<div style="display:flex;justify-content:space-between;gap:16px;"><span style="color:#64748b;">高</span><span>${formatPrice(row?.high ?? null)}</span></div>`,
            `<div style="display:flex;justify-content:space-between;gap:16px;"><span style="color:#64748b;">低</span><span>${formatPrice(row?.low ?? null)}</span></div>`,
            `<div style="display:flex;justify-content:space-between;gap:16px;"><span style="color:#64748b;">收</span><span>${formatPrice(row?.close ?? null)}</span></div>`,
            `<div style="display:flex;justify-content:space-between;gap:16px;"><span style="color:#64748b;">成交额</span><span>${formatYi(row?.amount ?? null)}</span></div>`,
          ].join("")
        },
      },
      axisPointer: {
        link: [{ xAxisIndex: [0, 1, 2] }],
      },
      xAxis: [
        {
          type: "category",
          gridIndex: 0,
          boundaryGap: true,
          data: dates,
          axisLine: { show: false },
          axisTick: { show: false },
          axisLabel: { show: false },
        },
        {
          type: "category",
          gridIndex: 1,
          boundaryGap: true,
          data: dates,
          axisLine: { show: false },
          axisTick: { show: false },
          axisLabel: { show: false },
        },
        {
          type: "category",
          gridIndex: 2,
          boundaryGap: true,
          data: dates,
          axisLine: { onZero: false, lineStyle: { color: axisLine } },
          axisTick: { show: false },
          axisLabel: { color: fgLight, fontSize: 11, margin: 8 },
        },
      ],
      yAxis: [
        {
          type: "value",
          name: "资金净流",
          nameTextStyle: { color: "#047857", fontSize: isCompact ? 10 : 12 },
          gridIndex: 0,
          min: 0,
          max: maxAbsFund * 1.12,
          inverse: true,
          splitNumber: 4,
          position: "right",
          offset: sideOffset,
          axisLabel: {
            color: "#047857",
            fontSize: isCompact ? 9 : 11,
            formatter: (value: number) => formatAxisYi(value),
            margin: 4,
          },
          splitLine: { lineStyle: { color: "rgba(217,222,232,0.38)" } },
          axisLine: { show: true, lineStyle: { color: "#86efac" } },
          axisTick: { show: false },
        },
        {
          type: "value",
          name: "价格",
          nameTextStyle: { color: fg, fontSize: isCompact ? 10 : 12 },
          gridIndex: 1,
          min: priceMin - pricePadding,
          max: priceMax + pricePadding,
          scale: true,
          splitNumber: 4,
          position: "right",
          axisLabel: { color: fg, fontSize: isCompact ? 10 : 11, formatter: (value: number) => value.toFixed(0) },
          splitLine: { lineStyle: { color: splitLine } },
          axisLine: { show: true, lineStyle: { color: axisLine } },
          axisTick: { show: false },
        },
        {
          type: "value",
          name: "成交额",
          nameTextStyle: { color: "#5470c6", fontSize: isCompact ? 10 : 12 },
          gridIndex: 2,
          min: 0,
          max: amountVisualMax * 1.08,
          splitNumber: 4,
          position: "left",
          offset: sideOffset,
          axisLabel: {
            color: "#5470c6",
            fontSize: isCompact ? 9 : 11,
            formatter: (value: number) => formatAxisYi(visualToAmount(value)),
            margin: 4,
          },
          splitLine: { lineStyle: { color: "rgba(217,222,232,0.38)" } },
          axisLine: { show: true, lineStyle: { color: "#5470c6" } },
          axisTick: { show: false },
        },
      ],
      dataZoom: [
        {
          show: true,
          realtime: true,
          xAxisIndex: [0, 1, 2],
          start: zoomStart,
          end: 100,
          height: isCompact ? 18 : 24,
          bottom: isCompact ? 0 : 4,
          borderColor: "#dbe3f3",
          backgroundColor: "#f5f7fc",
          fillerColor: "rgba(151, 171, 232, 0.35)",
          handleStyle: { color: "#aab8df" },
          moveHandleStyle: { color: "#aab8df" },
          textStyle: { color: fgLight, fontSize: isCompact ? 9 : 10 },
          filterMode: "none",
        },
        {
          type: "inside",
          realtime: true,
          xAxisIndex: [0, 1, 2],
          start: zoomStart,
          end: 100,
          filterMode: "none",
        },
      ],
      series: [
        {
          name: "资金净流",
          type: "line",
          xAxisIndex: 0,
          yAxisIndex: 0,
          data: negativeFlowValues,
          symbol: "none",
          smooth: 0.22,
          connectNulls: true,
          lineStyle: {
            width: 1.6,
            color: "#86efac",
          },
          areaStyle: {
            color: "rgba(134, 239, 172, 0.38)",
          },
          z: 3,
          markLine: {
            symbol: "none",
            silent: true,
            lineStyle: {
              type: "dashed",
              color: "rgba(148,163,184,0.65)",
              width: 1,
            },
            data: [{ yAxis: 0 }],
          },
        },
        {
          name: "资金净流",
          type: "line",
          xAxisIndex: 0,
          yAxisIndex: 0,
          data: positiveFlowValues,
          symbol: "none",
          smooth: 0.22,
          connectNulls: true,
          lineStyle: {
            width: 1.6,
            color: "#fda4af",
          },
          areaStyle: {
            color: "rgba(252, 165, 165, 0.34)",
          },
          z: 4,
        },
        {
          name: "日K",
          type: "candlestick",
          xAxisIndex: 1,
          yAxisIndex: 1,
          data: chartData.map((item) =>
            item.open == null || item.high == null || item.low == null || item.close == null
              ? null
              : [item.open, item.close, item.low, item.high],
          ),
          itemStyle: {
            color: "#ef4444",
            color0: "#10b981",
            borderColor: "#ef4444",
            borderColor0: "#10b981",
          },
          barMaxWidth: isCompact ? 10 : 14,
          z: 4,
        },
        {
          name: "成交额",
          type: "line",
          xAxisIndex: 2,
          yAxisIndex: 2,
          data: amountVisualValues,
          symbol: "none",
          smooth: 0.22,
          connectNulls: false,
          lineStyle: {
            width: 1.5,
            color: "#5470c6",
          },
          areaStyle: {
            color: "rgba(84, 112, 198, 0.62)",
          },
          markLine: enableAmountCompression && amountCompressionMarkLine != null
            ? {
                symbol: "none",
                silent: true,
                label: {
                  color: "#5470c6",
                  fontSize: 10,
                  formatter: `${formatAxisYi(amountCompressionFloorYi * 1e8)}压缩线`,
                  position: "insideEndTop",
                  backgroundColor: "rgba(255,255,255,0.72)",
                  padding: [2, 5],
                  borderRadius: 3,
                },
                lineStyle: {
                  type: "dashed",
                  color: "rgba(84,112,198,0.45)",
                  width: 1,
                },
                data: [{ yAxis: amountCompressionMarkLine }],
              }
            : undefined,
          z: 3,
        },
      ],
    } satisfies EChartsOption
  }, [chartData, chartWidth, fundOnly])

  useEffect(() => {
    if (!ref.current) return

    if (!chartRef.current) {
      chartRef.current = echarts.init(ref.current, undefined, { renderer: "canvas" })
    }

    const chart = chartRef.current
    chart.setOption(option, { notMerge: true, lazyUpdate: true })

    const syncSize = () => {
      chart.resize()
      const nextWidth = ref.current?.clientWidth ?? chart.getWidth?.() ?? 1200
      setChartWidth((prev) => (Math.abs(prev - nextWidth) > 2 ? nextWidth : prev))
    }

    syncSize()

    const observer = new ResizeObserver(syncSize)
    observer.observe(ref.current)
    window.addEventListener("resize", syncSize)

    return () => {
      observer.disconnect()
      window.removeEventListener("resize", syncSize)
    }
  }, [option])

  useEffect(() => {
    return () => {
      chartRef.current?.dispose()
      chartRef.current = null
    }
  }, [])

  return <div ref={ref} className="h-full min-h-[260px] w-full" />
}
