export type TaskSummaryPeriod = "week" | "month"

export interface TaskSummaryRecord {
  workflowType: string
  status: string
  createdAt?: string | null
  startedAt?: string | null
  completedAt?: string | null
}

export interface TaskSummaryCoverage {
  name: string
  hasMore: boolean
  oldestAt?: string | null
}

export interface TaskSummaryRange {
  start: Date
  end: Date
}

export interface TaskSummaryStats {
  total: number
  success: number
  failed: number
  review: number
  active: number
  closed: number
  rate: number | null
  medianSeconds: number | null
  timedTasks: number
}

function localNoon(year: number, month: number, day: number) {
  return new Date(year, month, day, 12)
}

function localDay(value: Date) {
  return localNoon(value.getFullYear(), value.getMonth(), value.getDate())
}

export function taskSummaryRange(period: TaskSummaryPeriod, offset: number, now = new Date()): TaskSummaryRange {
  const today = localDay(now)
  if (period === "week") {
    const mondayOffset = (today.getDay() + 6) % 7
    const start = localNoon(today.getFullYear(), today.getMonth(), today.getDate() - mondayOffset + offset * 7)
    const end = localNoon(start.getFullYear(), start.getMonth(), start.getDate() + 6)
    return { start, end: end > today ? today : end }
  }

  const start = localNoon(today.getFullYear(), today.getMonth() + offset, 1)
  const lastDay = new Date(start.getFullYear(), start.getMonth() + 1, 0).getDate()
  const end = localNoon(start.getFullYear(), start.getMonth(), lastDay)
  return { start, end: end > today ? today : end }
}

export function previousTaskSummaryRange(range: TaskSummaryRange, period: TaskSummaryPeriod): TaskSummaryRange {
  if (period === "week") {
    const start = localNoon(range.start.getFullYear(), range.start.getMonth(), range.start.getDate() - 7)
    return { start, end: localNoon(start.getFullYear(), start.getMonth(), start.getDate() + 6) }
  }

  const start = localNoon(range.start.getFullYear(), range.start.getMonth() - 1, 1)
  const previousLastDay = new Date(start.getFullYear(), start.getMonth() + 1, 0).getDate()
  const endDay = Math.min(range.end.getDate(), previousLastDay)
  return { start, end: localNoon(start.getFullYear(), start.getMonth(), endDay) }
}

export function taskSummaryRecords<T extends TaskSummaryRecord>(items: T[], range: TaskSummaryRange): T[] {
  const start = localDay(range.start).getTime()
  const end = localDay(range.end).getTime()
  return items.filter((item) => {
    if (!item.createdAt) return false
    const created = new Date(item.createdAt)
    if (!Number.isFinite(created.getTime())) return false
    const timestamp = localDay(created).getTime()
    return timestamp >= start && timestamp <= end
  })
}

export function isTaskSummaryIncomplete(coverage: TaskSummaryCoverage[], range: TaskSummaryRange, unavailableSources: string[] = []) {
  if (unavailableSources.length) return true
  const start = localDay(range.start).getTime()
  return coverage.some((source) => {
    if (!source.hasMore) return false
    if (!source.oldestAt) return true
    const oldest = new Date(source.oldestAt)
    return !Number.isFinite(oldest.getTime()) || localDay(oldest).getTime() >= start
  })
}

export function summarizeTasks(items: TaskSummaryRecord[]): TaskSummaryStats {
  const success = items.filter((item) => item.status === "SUCCEEDED")
  const failed = items.filter((item) => item.status === "FAILED")
  const closed = success.length + failed.length
  const durations = success.flatMap((item) => {
    if (!item.completedAt) return []
    const start = item.startedAt || item.createdAt
    if (!start) return []
    const seconds = (new Date(item.completedAt).getTime() - new Date(start).getTime()) / 1_000
    return Number.isFinite(seconds) && seconds >= 0 ? [seconds] : []
  }).sort((left, right) => left - right)
  const middle = Math.floor(durations.length / 2)
  const medianSeconds = durations.length === 0 ? null
    : durations.length % 2 ? durations[middle] : (durations[middle - 1] + durations[middle]) / 2

  return {
    total: items.length,
    success: success.length,
    failed: failed.length,
    review: items.filter((item) => item.status === "WAITING_REVIEW").length,
    active: items.filter((item) => item.status === "QUEUED" || item.status === "RUNNING").length,
    closed,
    rate: closed ? success.length / closed * 100 : null,
    medianSeconds,
    timedTasks: durations.length,
  }
}

export function taskSummaryDays(range: TaskSummaryRange) {
  const result: Date[] = []
  for (let date = localDay(range.start); date <= range.end; date = localNoon(date.getFullYear(), date.getMonth(), date.getDate() + 1)) {
    result.push(date)
  }
  return result
}

export function formatTaskDuration(seconds: number | null) {
  if (seconds === null) return "—"
  return seconds >= 60 ? `${(seconds / 60).toFixed(1)}m` : `${Math.round(seconds)}s`
}
