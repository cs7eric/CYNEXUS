import { CalendarDays, Filter } from "lucide-react"

import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import type { FuturesScopeType } from "@/services/market/futures-position"

interface FuturesPositionFiltersProps {
  tradeDate: string
  scopeType: FuturesScopeType
  exchangeCode: string
  varietyCode: string
  top: string
  onTradeDateChange: (value: string) => void
  onScopeTypeChange: (value: FuturesScopeType) => void
  onExchangeChange: (value: string) => void
  onVarietyChange: (value: string) => void
  onTopChange: (value: string) => void
}

const exchanges = ["ALL", "CFFEX", "SHFE", "CZCE", "DCE", "GFEX", "INE"]

export function FuturesPositionFilters(props: FuturesPositionFiltersProps) {
  return (
    <section className="flex flex-wrap items-end gap-3 rounded-lg bg-muted/35 p-3" aria-label="期货持仓筛选">
      <div className="flex items-center gap-2 pr-1 text-sm font-medium text-foreground"><Filter className="size-4 text-muted-foreground" />筛选</div>
      <label className="grid gap-1 text-xs text-muted-foreground">
        交易日
        <span className="relative">
          <CalendarDays className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input type="date" value={props.tradeDate} onChange={(event) => props.onTradeDateChange(event.target.value)} className="h-8 w-[142px] pl-8 text-xs" />
        </span>
      </label>
      <label className="grid gap-1 text-xs text-muted-foreground">
        统计范围
        <Select value={props.scopeType} onValueChange={(value) => props.onScopeTypeChange(value as FuturesScopeType)}>
          <SelectTrigger size="sm" className="w-[116px] text-xs"><SelectValue /></SelectTrigger>
          <SelectContent><SelectItem value="CONTRACT">合约</SelectItem><SelectItem value="VARIETY">品种</SelectItem></SelectContent>
        </Select>
      </label>
      <label className="grid gap-1 text-xs text-muted-foreground">
        交易所
        <Select value={props.exchangeCode} onValueChange={props.onExchangeChange}>
          <SelectTrigger size="sm" className="w-[116px] text-xs"><SelectValue /></SelectTrigger>
          <SelectContent>{exchanges.map((exchange) => <SelectItem key={exchange} value={exchange}>{exchange === "ALL" ? "全部交易所" : exchange}</SelectItem>)}</SelectContent>
        </Select>
      </label>
      <label className="grid gap-1 text-xs text-muted-foreground">
        品种代码
        <Input value={props.varietyCode} onChange={(event) => props.onVarietyChange(event.target.value.toUpperCase())} placeholder="如 IF / RB" title="IF：沪深300股指期货；RB：螺纹钢" className="h-8 w-[112px] text-xs" />
      </label>
      <label className="grid gap-1 text-xs text-muted-foreground">
        会员 Top-N
        <Select value={props.top} onValueChange={props.onTopChange}>
          <SelectTrigger size="sm" className="w-[98px] text-xs"><SelectValue /></SelectTrigger>
          <SelectContent>{[5, 10, 15, 20].map((value) => <SelectItem key={value} value={String(value)}>Top {value}</SelectItem>)}</SelectContent>
        </Select>
      </label>
      <p className="basis-full text-[11px] text-muted-foreground">品种说明：IF · 沪深300股指期货，IC · 中证500股指期货，RB · 螺纹钢；结果中的代码均会同时显示中文名称。</p>
    </section>
  )
}
