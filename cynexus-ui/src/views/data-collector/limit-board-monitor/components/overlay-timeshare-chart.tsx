import { useEffect, useMemo, useRef } from "react"
import * as echarts from "echarts/core"
import { LineChart } from "echarts/charts"
import { GridComponent, TooltipComponent } from "echarts/components"
import { CanvasRenderer } from "echarts/renderers"

import { amount, sessionAxisRange, sessionAxisTicks, sessionMinute, sessionTickLabel, type TrendPoint } from "../lib/format"
import type { TimesharePoint } from "../lib/timeshare"

echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const UP = "#d85b47"
const DOWN = "#059669"
const SEALED = "#0071e3"

export function OverlayTimeshareChart({
  timeshare,
  sealed,
  session,
  down,
  label,
}: {
  timeshare: TimesharePoint[]
  sealed: TrendPoint[]
  session?: string | null
  down?: boolean
  label: string
}) {
  const ref = useRef<HTMLDivElement>(null)
  const chartRef = useRef<echarts.ECharts | null>(null)
  const priceColor = down ? DOWN : UP
  const axis = sessionAxisRange(session)
  const ticks = sessionAxisTicks(session)

  const option = useMemo<echarts.EChartsCoreOption>(() => {
    const priceByMinute = new Map(timeshare.map((point) => [sessionMinute(point.at), point]))
    const sealedByMinute = new Map(sealed.map((point) => [sessionMinute(point.at), point]))
    return {
      animation: false,
      grid: { left: 2, right: 2, top: 18, bottom: 0, containLabel: true },
      tooltip: {
        trigger: "axis",
        axisPointer: { type: "line", lineStyle: { color: "#a3a3a3", width: 1, type: "dashed" } },
        backgroundColor: "rgba(20,20,20,0.92)",
        borderWidth: 0,
        padding: [8, 10],
        textStyle: { color: "#f5f5f5", fontSize: 12 },
        formatter: (items: unknown) => {
          const rows = Array.isArray(items) ? items : []
          const first = rows[0] as { axisValue?: number; value?: [number, number] } | undefined
          const minute = Number(first?.axisValue ?? first?.value?.[0])
          if (!Number.isFinite(minute)) return ""
          const price = priceByMinute.get(minute)
          const seal = sealedByMinute.get(minute) ?? nearestSealed(sealed, minute)
          return `<div style="min-width:140px;font-size:12px">
            <div style="opacity:.7;font-size:11px">盘中时间</div>
            <div style="font-family:ui-monospace,Menlo,monospace;margin-top:2px">${price?.clock || sessionTickLabel(minute)}</div>
            <div style="margin-top:8px;opacity:.7;font-size:11px">分时价</div>
            <div style="font-family:ui-monospace,Menlo,monospace;margin-top:2px">${price ? price.price.toFixed(2) : "-"}</div>
            <div style="margin-top:8px;opacity:.7;font-size:11px">封单额</div>
            <div style="font-family:ui-monospace,Menlo,monospace;margin-top:2px">${seal ? amount(seal.amount) : "-"}</div>
          </div>`
        },
      },
      xAxis: {
        type: "value",
        min: axis.min,
        max: axis.max,
        interval: 30,
        axisLine: { lineStyle: { color: "#ebebeb" } },
        axisTick: { show: false },
        axisLabel: {
          color: "#757575",
          fontSize: 10,
          fontFamily: "ui-monospace, Menlo, monospace",
          customValues: ticks,
          formatter: (value: number) => sessionTickLabel(value),
        },
        splitLine: { show: false },
      },
      yAxis: [
        {
          type: "value",
          scale: true,
          splitNumber: 3,
          axisLine: { show: false },
          axisTick: { show: false },
          axisLabel: {
            color: "#757575",
            fontSize: 10,
            fontFamily: "ui-monospace, Menlo, monospace",
            formatter: (value: number) => value.toFixed(2),
          },
          splitLine: { lineStyle: { color: "#f2f2f2", type: "dashed" } },
        },
        {
          type: "value",
          scale: true,
          splitNumber: 3,
          axisLine: { show: false },
          axisTick: { show: false },
          axisLabel: {
            color: SEALED,
            fontSize: 10,
            fontFamily: "ui-monospace, Menlo, monospace",
            formatter: (value: number) => amount(value),
          },
          splitLine: { show: false },
        },
      ],
      series: [
        {
          name: "分时",
          type: "line",
          yAxisIndex: 0,
          data: timeshare.map((point) => [sessionMinute(point.at), point.price]),
          showSymbol: false,
          symbol: "circle",
          symbolSize: 4,
          sampling: "lttb",
          lineStyle: { width: 1.6, color: priceColor },
          itemStyle: { color: priceColor },
          areaStyle: { color: down ? "rgba(5,150,105,0.10)" : "rgba(216,91,71,0.10)" },
        },
        {
          name: "封单额",
          type: "line",
          yAxisIndex: 1,
          data: sealed.map((point) => [sessionMinute(point.at), point.amount]),
          showSymbol: false,
          symbol: "circle",
          symbolSize: 4,
          sampling: "lttb",
          lineStyle: { width: 1.4, color: SEALED },
          itemStyle: { color: SEALED },
        },
      ],
    }
  }, [axis.max, axis.min, down, priceColor, sealed, ticks, timeshare])

  useEffect(() => {
    const node = ref.current
    if (!node) return
    const chart = echarts.init(node, undefined, { renderer: "canvas" })
    chartRef.current = chart
    const observer = new ResizeObserver(() => chart.resize())
    observer.observe(node)
    return () => {
      observer.disconnect()
      chart.dispose()
      chartRef.current = null
    }
  }, [])

  useEffect(() => {
    chartRef.current?.setOption(option, true)
  }, [option])

  return <div ref={ref} className="h-[148px] w-full min-w-0" role="img" aria-label={`${label}分时与封单额叠加`} />
}

function nearestSealed(points: TrendPoint[], minute: number) {
  if (!points.length) return null
  return points.reduce((best, point) => {
    const delta = Math.abs(sessionMinute(point.at) - minute)
    const bestDelta = Math.abs(sessionMinute(best.at) - minute)
    return delta < bestDelta ? point : best
  })
}
