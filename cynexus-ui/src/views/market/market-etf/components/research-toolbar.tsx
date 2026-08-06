import { Search, SlidersHorizontal } from "lucide-react"

import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"

export type EtfView = "overview" | "broad" | "sector"
export type EtfPreset = "all" | "active" | "inflow" | "premium"

export function EtfResearchToolbar({
  view,
  preset,
  query,
  resultCount,
  onViewChange,
  onPresetChange,
  onQueryChange,
}: {
  view: EtfView
  preset: EtfPreset
  query: string
  resultCount: number
  onViewChange: (value: EtfView) => void
  onPresetChange: (value: EtfPreset) => void
  onQueryChange: (value: string) => void
}) {
  return (
    <section className="flex min-w-0 flex-col gap-3 rounded-lg border border-border/70 bg-card p-3 sm:p-4">
      <div className="flex min-w-0 flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <Tabs value={view} onValueChange={(value) => onViewChange(value as EtfView)}>
          <TabsList variant="line" className="w-full justify-start overflow-x-auto sm:w-fit">
            <TabsTrigger value="overview" className="flex-none px-3 text-xs">总览</TabsTrigger>
            <TabsTrigger value="broad" className="flex-none px-3 text-xs">宽基 ETF</TabsTrigger>
            <TabsTrigger value="sector" className="flex-none px-3 text-xs">行业 ETF</TabsTrigger>
          </TabsList>
        </Tabs>
        <div className="flex min-w-0 items-center gap-2 text-xs text-muted-foreground">
          <SlidersHorizontal className="size-3.5 shrink-0" />
          <span className="shrink-0">研究筛选</span>
          <span className="font-mono tabular-nums">{resultCount} 只</span>
        </div>
      </div>
      <div className="flex min-w-0 flex-col gap-2 sm:flex-row sm:items-center">
        <div className="relative min-w-0 flex-1 sm:max-w-sm">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input value={query} onChange={(event) => onQueryChange(event.target.value)} placeholder="搜索代码、名称或主题" className="h-8 pl-9 text-xs" aria-label="搜索 ETF" />
        </div>
        <Select value={preset} onValueChange={(value) => onPresetChange(value as EtfPreset)}>
          <SelectTrigger size="sm" className="w-full text-xs sm:w-40"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">全部 ETF</SelectItem>
            <SelectItem value="active">成交活跃</SelectItem>
            <SelectItem value="inflow">净流入优先</SelectItem>
            <SelectItem value="premium">折溢价异常</SelectItem>
          </SelectContent>
        </Select>
      </div>
    </section>
  )
}
