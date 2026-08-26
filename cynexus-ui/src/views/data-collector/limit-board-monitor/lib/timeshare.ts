import { fetchEasyTdxTimeshare } from "@/services/market/easy-tdx"

import { clockLabel } from "./format"

export type TimesharePoint = {
  at: number
  price: number
  clock: string
  volume: number
}

export async function loadTimeshare(code: string, tradeDate: string): Promise<TimesharePoint[]> {
  const response = await fetchEasyTdxTimeshare({ code, tradeDate })
  return parseTimeshareRows(response.items, tradeDate)
}

export function parseTimeshareRows(items: Array<Record<string, unknown>>, tradeDate: string): TimesharePoint[] {
  return items.flatMap((item) => {
    const at = pointTimestamp(item, tradeDate)
    const price = Number(item.price ?? item.last_price ?? item.close)
    const volume = Number(item.volume ?? 0)
    if (!at || !Number.isFinite(price) || price <= 0) return []
    return [{
      at,
      price,
      clock: String(item.time_label ?? "") || clockLabel(at),
      volume: Number.isFinite(volume) ? volume : 0,
    }]
  }).sort((left, right) => left.at - right.at)
}

function pointTimestamp(row: Record<string, unknown>, tradeDate: string) {
  const raw = row.trade_time ?? row.datetime ?? row.timestamp
  if (typeof raw === "number" && Number.isFinite(raw)) return raw < 10_000_000_000 ? raw * 1000 : raw
  const text = String(raw ?? "")
  if (/^\d{4}-\d{2}-\d{2}[T\s]/.test(text)) {
    const parsed = Date.parse(/[zZ]|[+-]\d{2}:?\d{2}$/.test(text) ? text : `${text.replace(" ", "T")}+08:00`)
    if (Number.isFinite(parsed)) return parsed
  }
  const clock = String(row.time_label ?? row.time ?? "").match(/(\d{2}:\d{2})/)?.[1]
  return clock ? Date.parse(`${tradeDate}T${clock}:00+08:00`) : 0
}
