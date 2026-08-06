import { CalendarDays } from "lucide-react"

import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import type { FuturesPositionInstrument, FuturesScopeType } from "@/services/market/futures-position"
import { varietyLabel } from "../lib/variety"

interface FuturesPositionFiltersProps {
  tradeDate: string
  scopeType: FuturesScopeType
  exchangeCode: string
  varietyCode: string
  varietyOptions: FuturesPositionInstrument[]
  top: string
  onTradeDateChange: (value: string) => void
  onScopeTypeChange: (value: FuturesScopeType) => void
  onExchangeChange: (value: string) => void
  onVarietyChange: (value: string) => void
  onTopChange: (value: string) => void
}

const exchanges = [
  { value: "ALL", label: "全部交易所" },
  { value: "CFFEX", label: "CFFEX · 中金所" },
  { value: "SHFE", label: "SHFE · 上期所" },
  { value: "CZCE", label: "CZCE · 郑商所" },
  { value: "DCE", label: "DCE · 大商所" },
  { value: "GFEX", label: "GFEX · 广期所" },
  { value: "INE", label: "INE · 上期能源" },
]

export function FuturesPositionFilters(props: FuturesPositionFiltersProps) {
  const varietyOptions = [...new Map(props.varietyOptions.map((item) => [item.instrumentCode, item])).values()]
    .sort((a, b) => a.instrumentCode.localeCompare(b.instrumentCode))

  return (
    <section className="flex min-w-0 flex-wrap items-center gap-x-3 gap-y-2 rounded-lg border border-border/70 bg-card px-3 py-2" aria-label="期货持仓筛选">
        <label className="flex min-w-0 items-center gap-2 text-[11px] text-muted-foreground">
          <span className="shrink-0">交易所</span>
          <Select value={props.exchangeCode} onValueChange={props.onExchangeChange}>
            <SelectTrigger size="sm" className="h-7 w-[148px] text-xs"><SelectValue /></SelectTrigger>
            <SelectContent className="max-h-[min(60vh,360px)]">{exchanges.map((exchange) => <SelectItem key={exchange.value} value={exchange.value}>{exchange.label}</SelectItem>)}</SelectContent>
          </Select>
        </label>
        <label className="flex min-w-0 items-center gap-2 text-[11px] text-muted-foreground">
          <span className="shrink-0">期货品种</span>
          <Select value={props.varietyCode || "ALL"} onValueChange={(value) => props.onVarietyChange(value === "ALL" ? "" : value)}>
            <SelectTrigger size="sm" className="h-7 w-[220px] text-xs"><SelectValue placeholder="全部品种" /></SelectTrigger>
            <SelectContent className="overflow-y-auto" style={{ maxHeight: "min(360px, calc(100dvh - 180px))" }}>
              <SelectItem value="ALL">全部品种</SelectItem>
              {varietyOptions.map((item) => <SelectItem key={item.instrumentCode} value={item.instrumentCode}>{varietyLabel(item.varietyCode || item.instrumentCode)}</SelectItem>)}
            </SelectContent>
          </Select>
        </label>
        <label className="flex min-w-0 items-center gap-2 text-[11px] text-muted-foreground">
          <span className="shrink-0">交易日</span>
          <span className="relative block min-w-0">
            <CalendarDays className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
            <Input type="date" value={props.tradeDate} onChange={(event) => props.onTradeDateChange(event.target.value)} className="h-7 w-[168px] pl-8 text-xs" />
          </span>
        </label>
        <div className="flex min-w-0 items-center gap-2 text-[11px] text-muted-foreground">
          <span id="futures-scope-label">分析层级</span>
          <Tabs value={props.scopeType} onValueChange={(value) => props.onScopeTypeChange(value as FuturesScopeType)} className="gap-0" aria-labelledby="futures-scope-label">
            <TabsList className="h-7 w-[128px] rounded-md p-0.5">
              <TabsTrigger value="CONTRACT" className="text-xs">合约</TabsTrigger>
              <TabsTrigger value="VARIETY" className="text-xs">品种</TabsTrigger>
            </TabsList>
          </Tabs>
        </div>
        <label className="flex min-w-0 items-center gap-2 text-[11px] text-muted-foreground">
          <span className="shrink-0">会员口径</span>
          <Select value={props.top} onValueChange={props.onTopChange}>
            <SelectTrigger size="sm" className="h-7 w-[96px] text-xs"><SelectValue /></SelectTrigger>
            <SelectContent className="max-h-[min(60vh,360px)]">{[5, 10, 15, 20].map((value) => <SelectItem key={value} value={String(value)}>前 {value} 名</SelectItem>)}</SelectContent>
          </Select>
        </label>
    </section>
  )
}
