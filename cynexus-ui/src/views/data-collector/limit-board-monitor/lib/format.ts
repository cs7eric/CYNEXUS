import type { LimitBoardSessionSummary, LimitBoardTrendFeatures } from "@/services/market/indicator"

const SHANGHAI = "Asia/Shanghai"
const HALF_HOUR_MS = 30 * 60 * 1000
const CLOCK = { hour: "2-digit", minute: "2-digit", hourCycle: "h23", timeZone: SHANGHAI } as const
const CLOCK_WITH_SECONDS = { ...CLOCK, second: "2-digit" } as const

export type SessionWindow = { start: number; end: number }
export type TrendPoint = { at: number; amount: number; clock: string }

export function amount(value?: number | null) {
  if (value == null) return "-"
  if (Math.abs(value) >= 100000000) return `${(value / 100000000).toFixed(2)} 亿`
  if (Math.abs(value) >= 10000) return `${(value / 10000).toFixed(1)} 万`
  return value.toFixed(0)
}

export function duration(value?: number | null) {
  if (!value) return "-"
  return `${Math.floor(value / 60)}分${value % 60}秒`
}

export function percent(value?: number | null) {
  return value == null ? "-" : `${(value * 100).toFixed(1)}%`
}

export function sealedTurnoverRatio(row: LimitBoardSessionSummary) {
  return row.max_sealed_turnover_ratio ?? row.last_sealed_turnover_ratio ?? null
}

export function time(value?: string | null) {
  if (!value) return "-"
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? "-" : clockLabel(parsed.getTime())
}

export function clockLabel(value: number, withSeconds = false) {
  const parts = new Intl.DateTimeFormat("en-GB", withSeconds ? CLOCK_WITH_SECONDS : CLOCK).formatToParts(new Date(value))
  const hour = parts.find((part) => part.type === "hour")?.value ?? "00"
  const minute = parts.find((part) => part.type === "minute")?.value ?? "00"
  if (!withSeconds) return `${hour}:${minute}`
  const second = parts.find((part) => part.type === "second")?.value ?? "00"
  return `${hour}:${minute}:${second}`
}

export function trendLabel(row: LimitBoardSessionSummary) {
  if (row.trend_direction === "increasing") return "封单增强"
  if (row.trend_direction === "decreasing") return "封单走弱"
  if (row.trend_direction === "flat") return "封单平稳"
  return "样本不足"
}

export function parseTrendFeatures(value: LimitBoardSessionSummary["trend_features"]): LimitBoardTrendFeatures {
  if (!value) return {}
  if (typeof value === "string") {
    try {
      const parsed: unknown = JSON.parse(value)
      return parsed && typeof parsed === "object" ? parsed as LimitBoardTrendFeatures : {}
    } catch {
      return {}
    }
  }
  return typeof value === "object" ? value as LimitBoardTrendFeatures : {}
}

export function calendarDay(value?: string | Date | null) {
  if (value instanceof Date && !Number.isNaN(value.getTime())) {
    return value.toLocaleDateString("en-CA", { timeZone: "Asia/Shanghai" })
  }
  if (typeof value !== "string" || !value) return null
  const match = /^(\d{4}-\d{2}-\d{2})/.exec(value)
  return match ? match[1] : null
}

export function sessionWindow(row: LimitBoardSessionSummary): SessionWindow | null {
  const day = calendarDay(row.trade_date)
  if (!day) return null
  const afternoon = row.trade_session === "afternoon"
  return {
    start: shanghaiAt(day, afternoon ? 13 : 9, afternoon ? 0 : 30).getTime(),
    end: shanghaiAt(day, afternoon ? 15 : 11, afternoon ? 0 : 30).getTime(),
  }
}

export function sessionTicks(window: SessionWindow): number[] {
  const ticks: number[] = []
  for (let at = window.start; at <= window.end; at += HALF_HOUR_MS) ticks.push(at)
  return ticks
}

export function sessionAxisRange(session?: string | null) {
  return session === "afternoon" ? { min: 120, max: 240 } : { min: 0, max: 120 }
}

export function sessionAxisTicks(session?: string | null) {
  const { min, max } = sessionAxisRange(session)
  const ticks: number[] = []
  for (let value = min; value <= max; value += 30) ticks.push(value)
  return ticks
}

export function sessionMinute(at: number) {
  const [hour, minute] = clockLabel(at).split(":").map(Number)
  const minutes = (hour ?? 0) * 60 + (minute ?? 0)
  if (minutes < 13 * 60) return clamp(minutes - (9 * 60 + 30), 0, 120)
  return clamp(120 + minutes - 13 * 60, 120, 240)
}

export function sessionTickLabel(value: number) {
  const minutes = value <= 120 ? 9 * 60 + 30 + value : 13 * 60 + (value - 120)
  return `${String(Math.floor(minutes / 60)).padStart(2, "0")}:${String(minutes % 60).padStart(2, "0")}`
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value))
}

export function trendPoints(row: LimitBoardSessionSummary): TrendPoint[] {
  const origin = originMillis(row)
  if (origin == null) return []
  const offsets = parseNumberSequence(row.trend_time_offsets)
  const amounts = parseNumberSequence(row.trend_sealed_amounts)
  if (offsets.length && offsets.length === amounts.length) {
    return offsets.map((elapsed, index) => pointAt(origin + elapsed * 1000, amounts[index]))
  }
  const features = parseTrendFeatures(row.trend_features)
  return (features.sealed_amount_points ?? []).flatMap((item) => {
    if (!Array.isArray(item) || item.length < 2 || !Number.isFinite(Number(item[0])) || !Number.isFinite(Number(item[1]))) return []
    return [pointAt(origin + Number(item[0]) * 1000, Number(item[1]))]
  })
}

function originMillis(row: LimitBoardSessionSummary) {
  for (const value of [row.trend_series_origin_at, row.first_sealed_at, row.first_touch_at]) {
    if (!value) continue
    const parsed = new Date(value)
    if (!Number.isNaN(parsed.getTime())) return parsed.getTime()
  }
  return sessionWindow(row)?.start ?? null
}

function pointAt(at: number, amountValue: number): TrendPoint {
  return { at, amount: amountValue, clock: clockLabel(at, true) }
}

function shanghaiAt(day: string, hour: number, minute: number) {
  return new Date(`${day}T${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}:00+08:00`)
}

function parseNumberSequence(value: string | number[] | null | undefined): number[] {
  if (Array.isArray(value)) return value.map(Number).filter(Number.isFinite)
  if (typeof value !== "string") return []
  try {
    const parsed: unknown = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.map(Number).filter(Number.isFinite) : []
  } catch {
    return []
  }
}
