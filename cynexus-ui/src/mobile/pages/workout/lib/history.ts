import type { WorkoutSession } from "./model"

export type HistoryPeriod = "week" | "month" | "year"
export type HistoryFilter = "All" | "Strength" | "Cardio"

export function historyKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`
}

export function historyDate(key: string) {
  const [year, month, day] = key.split("-").map(Number)
  return new Date(year, month - 1, day, 12)
}

export function historyShift(date: Date, days: number) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days, 12)
}

export function historyBounds(anchor: Date, period: HistoryPeriod) {
  const year = anchor.getFullYear(), month = anchor.getMonth()
  const start = period === "year" ? new Date(year, 0, 1, 12)
    : period === "month" ? new Date(year, month, 1, 12)
      : historyShift(anchor, -((anchor.getDay() + 6) % 7))
  const end = period === "year" ? new Date(year, 11, 31, 12)
    : period === "month" ? new Date(year, month + 1, 0, 12) : historyShift(start, 6)
  return { start, end }
}

export function shiftHistoryPeriod(anchor: Date, period: HistoryPeriod, direction: number) {
  const { start } = historyBounds(anchor, period)
  if (period === "week") return historyShift(start, direction * 7)
  return new Date(start.getFullYear() + (period === "year" ? direction : 0), start.getMonth() + (period === "month" ? direction : 0), 1, 12)
}

export function historyData(sessions: readonly WorkoutSession[], anchor: Date, period: HistoryPeriod, filter: HistoryFilter, today: Date) {
  const { start, end } = historyBounds(anchor, period)
  const startKey = historyKey(start), endKey = historyKey(end), todayKey = historyKey(today)
  const records = sessions.filter(session => session.date >= startKey && session.date <= endKey && session.date <= todayKey
    && (filter === "All" || session.category === filter) && Number.isFinite(session.minutes) && session.minutes > 0)
    .sort((a, b) => b.date.localeCompare(a.date))
  const days = Math.round((Date.UTC(end.getFullYear(), end.getMonth(), end.getDate()) - Date.UTC(start.getFullYear(), start.getMonth(), start.getDate())) / 86400000) + 1
  const buckets = Array.from({ length: period === "year" ? 12 : days }, (_, index) => {
    const date = period === "year" ? new Date(start.getFullYear(), index, 1, 12) : historyShift(start, index)
    const key = historyKey(date)
    const items = records.filter(session => period === "year" ? session.date.slice(0, 7) === key.slice(0, 7) : session.date === key)
    return { key, date, future: key > todayKey, minutes: items.reduce((sum, session) => sum + session.minutes, 0), sessions: items }
  })
  return { start, end, days, records, buckets, minutes: records.reduce((sum, session) => sum + session.minutes, 0), activeDays: new Set(records.map(session => session.date)).size }
}
