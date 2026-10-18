import { activityMonthBounds, activityMonthKey, activityTransactionFen, type ActivityMonth } from "./activity"

function pad(value: number) { return String(value).padStart(2, "0") }

/** Accept normal decimal input without rounding away fractions of a cent. */
export function parseActivityAmount(text: string): number | null {
  const value = text.trim()
  if (!/^\d+(?:\.\d{1,2})?$/.test(value)) return null
  const [whole, fraction = ""] = value.split(".")
  // Reject excessively long inputs before constructing a BigInt.
  if (whole.replace(/^0+/, "").length > 14) return null
  const fen = BigInt(whole) * 100n + BigInt(fraction.padEnd(2, "0"))
  if (fen <= 0n || fen > BigInt(Number.MAX_SAFE_INTEGER)) return null
  const integerFen = Number(fen)
  // The shared API sends a number of yuan; safe integer fen alone does not
  // guarantee that its yuan representation survives JSON without losing a cent.
  return activityTransactionFen({ amount: integerFen / 100 }) === integerFen ? integerFen : null
}

export function localActivityDateTime(date = new Date()): string {
  if (!Number.isFinite(date.getTime())) return ""
  return `${String(date.getFullYear()).padStart(4, "0")}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** Preserve the browser's local wall time and its date-specific UTC offset. */
export function activityDateTimePayload(input: string): string | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/.exec(input)
  if (!match) return null
  const [, yearText, monthText, dayText, hourText, minuteText, secondText = "00"] = match
  const [year, month, day, hour, minute, second] = [yearText, monthText, dayText, hourText, minuteText, secondText].map(Number)
  if (year < 1 || year > 9999 || month < 1 || month > 12 || day < 1 || day > 31 || hour > 23 || minute > 59 || second > 59) return null
  const date = new Date(0)
  date.setFullYear(year, month - 1, day)
  date.setHours(hour, minute, second, 0)
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day || date.getHours() !== hour || date.getMinutes() !== minute || date.getSeconds() !== second) return null
  const offset = -date.getTimezoneOffset()
  const zone = `${offset >= 0 ? "+" : "-"}${pad(Math.floor(Math.abs(offset) / 60))}:${pad(Math.abs(offset) % 60)}`
  return `${yearText}-${monthText}-${dayText}T${hourText}:${minuteText}:${secondText}${zone}`
}

/** Backdated entry starts in the viewed month, with the day clamped to that month. */
export function defaultActivityEntryDate(month: ActivityMonth, date = new Date()): string {
  if (!Number.isFinite(date.getTime())) return ""
  const lastDay = Number(activityMonthBounds(month).end.slice(-2))
  return `${activityMonthKey(month)}-${pad(Math.min(date.getDate(), lastDay))}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}
