/**
 * Recharts 折线图 (替代内联 SVG sparkline, 60px 高 + hover tooltip)
 */
import { useEffect } from "react"
import { Area, AreaChart, ResponsiveContainer, Tooltip, type TooltipContentProps } from "recharts"
import type { SparkPoint } from "../lib/spark"

interface SparklineProps {
  data: SparkPoint[]
  /** 线条颜色: 默认按末值 vs 首值 (涨红跌绿). 传 null 用中性. */
  color?: "auto" | "neutral" | "inverse"
  height?: number | string
  /** inverse: 末值越大越红 (适用 vol, score) */
  formatter?: (v: number) => string
  showEdgeDates?: boolean
  dateTickStep?: number
  tooltipLabel?: string
  tooltipValueClassName?: (point: SparkPoint) => string
  tooltipExtras?: (point: SparkPoint) => Array<{ label: string; value: string; valueClassName?: string }>
  onHoverPointChange?: (point: SparkPoint | null) => void
}

export function Sparkline({
  data,
  color = "auto",
  height = 60,
  formatter,
  showEdgeDates = false,
  dateTickStep = 0,
  tooltipLabel,
  tooltipValueClassName,
  tooltipExtras,
  onHoverPointChange,
}: SparklineProps) {
  if (!data || data.length < 2) {
    return (
      <div
        style={{ height }}
        className="flex items-center justify-end text-[10px] text-slate-300"
      >
        —
      </div>
    )
  }
  const first = data[0].value
  const last = data[data.length - 1].value
  let stroke: string
  if (color === "neutral") {
    stroke = "#64748b"
  } else if (color === "inverse") {
    // 末值越大越红 (vol, risk score 这类)
    stroke = last > first ? "#dc2626" : last < first ? "#059669" : "#64748b"
  } else {
    // auto: 末值 vs 首值 (涨红跌绿)
    stroke = last > first ? "#dc2626" : last < first ? "#059669" : "#64748b"
  }
  const tickIndexes =
    dateTickStep > 0
      ? (() => {
          const raw = data.reduce<number[]>((acc, _item, index) => {
            if (index === 0 || index === data.length - 1 || index % dateTickStep === 0) {
              acc.push(index)
            }
            return acc
          }, [])
          const unique = Array.from(new Set(raw)).sort((a, b) => a - b)
          if (unique.length >= 2) {
            const last = unique[unique.length - 1]
            const prev = unique[unique.length - 2]
            if (last - prev <= 1) {
              unique.splice(unique.length - 2, 1)
            }
          }
          return unique
        })()
      : []

  return (
    <div className="flex h-full w-full flex-col">
      <div style={{ height, width: "100%" }} className="min-h-0 flex-1">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart
            data={data}
            margin={{ top: 4, right: 4, bottom: 4, left: 4 }}
            onMouseLeave={() => onHoverPointChange?.(null)}
          >
            <defs>
              <linearGradient id={`spark-grad-${stroke.replace("#", "")}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={stroke} stopOpacity={0.28} />
                <stop offset="100%" stopColor={stroke} stopOpacity={0} />
              </linearGradient>
            </defs>
            <Tooltip
              cursor={{ stroke: "#94a3b8", strokeDasharray: "2 2" }}
              content={((props: TooltipContentProps<number, string>) => (
                <SparklineTooltip
                  {...props}
                  formatter={formatter}
                  tooltipLabel={tooltipLabel}
                  tooltipValueClassName={tooltipValueClassName}
                  tooltipExtras={tooltipExtras}
                  onHoverPointChange={onHoverPointChange}
                />
              )) as never}
            />
            <Area
              type="monotone"
              dataKey="value"
              stroke={stroke}
              strokeWidth={1.5}
              fill={`url(#spark-grad-${stroke.replace("#", "")})`}
              dot={false}
              activeDot={{ r: 3, fill: stroke, stroke: "#fff", strokeWidth: 1.5 }}
              isAnimationActive={false}
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
      {showEdgeDates ? (
        <div className="mt-1 flex items-center justify-between text-[10px] text-slate-400">
          <span>{data[0]?.date ?? "—"}</span>
          <span>{data[data.length - 1]?.date ?? "—"}</span>
        </div>
      ) : dateTickStep > 0 ? (
        <div className="relative mt-1 h-4">
          {tickIndexes.map((index) => {
            const left = data.length <= 1 ? 0 : (index / (data.length - 1)) * 100
            const alignClass =
              index === 0 ? "left-0 -translate-x-0" : index === data.length - 1 ? "left-full -translate-x-full" : "-translate-x-1/2"
            return (
              <span
                key={`${data[index]?.date ?? index}-${index}`}
                className={`absolute top-0 whitespace-nowrap text-[10px] text-slate-400 ${alignClass}`}
                style={index === 0 ? undefined : index === data.length - 1 ? undefined : { left: `${left}%` }}
              >
                {data[index]?.date ?? "—"}
              </span>
            )
          })}
        </div>
      ) : null}
    </div>
  )
}

function SparklineTooltip({
  active,
  payload,
  formatter,
  tooltipLabel,
  tooltipValueClassName,
  tooltipExtras,
  onHoverPointChange,
}: {
  active?: boolean
  payload?: ReadonlyArray<{ payload?: unknown }>
  formatter?: (v: number) => string
  tooltipLabel?: string
  tooltipValueClassName?: (point: SparkPoint) => string
  tooltipExtras?: (point: SparkPoint) => Array<{ label: string; value: string; valueClassName?: string }>
  onHoverPointChange?: (point: SparkPoint | null) => void
}) {
  const point = active && payload?.length ? (payload[0].payload as SparkPoint) : null

  useEffect(() => {
    onHoverPointChange?.(point)
  }, [onHoverPointChange, point])

  if (!point) return null

  return (
    <div className="rounded-md border border-border/50 bg-background px-2 py-1 text-[11px] shadow-md">
      <div className="text-muted-foreground">{point.date}</div>
      <div className="space-y-1">
        <div className="flex items-center justify-between gap-3">
          <span className="text-muted-foreground">{tooltipLabel ?? "数值"}</span>
          <span className={`font-mono font-semibold tabular-nums ${tooltipValueClassName?.(point) ?? "text-foreground"}`}>
            {formatter ? formatter(point.value) : point.value.toFixed(2)}
          </span>
        </div>
        {(tooltipExtras?.(point) ?? []).map((item) => (
          <div key={`${point.date}-${item.label}`} className="flex items-center justify-between gap-3">
            <span className="text-muted-foreground">{item.label}</span>
            <span className={`font-mono font-semibold tabular-nums ${item.valueClassName ?? "text-foreground"}`}>{item.value}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
