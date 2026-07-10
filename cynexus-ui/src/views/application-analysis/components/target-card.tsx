import { useEffect, useMemo, useState } from "react"
import { ChevronDown, ChevronRight, Clock, Plus, Power, RefreshCw, Save, Search, Target, Trash2 } from "lucide-react"

import AnimatedList from "@/components/AnimatedList"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { searchStockChart } from "@/services/market/stock-chart"
import type { ApplicationAnalysisSchedulerStatus, ApplicationAnalysisTarget } from "@/services/ai/application-analysis"
import type { StockSearchItem } from "../../stock-chart/lib/types"

export type HorizonPatch = Partial<Record<"days" | "segments" | "monthly_keep" | "weekly_keep", number>>

type ListItem =
  | { kind: "target"; data: ApplicationAnalysisTarget }
  | { kind: "search"; data: StockSearchItem }

function formatIntervalLabel(minutes: number) {
  if (minutes < 60) return `${minutes} min`
  if (minutes % 1440 === 0) {
    const days = minutes / 1440
    return days === 1 ? "1 day" : `${days} days`
  }
  if (minutes % 60 === 0) {
    const hours = minutes / 60
    return hours === 1 ? "1 hour" : `${hours} hours`
  }
  return `${minutes} min`
}

export function TargetCard({
  targets,
  searchKeyword,
  setSearchKeyword,
  selectedId,
  setSelectedId,
  expandedId,
  setExpandedId,
  collapsed,
  setCollapsed,
  horizon,
  onHorizonChange,
  scheduler,
  saving,
  onAddFromSearch,
  onRemove,
  onUpdateTarget,
  onTriggerTarget,
  onSave,
  onToggleScheduler,
  onRefreshAll,
  compactMobile = false,
}: {
  targets: ApplicationAnalysisTarget[]
  searchKeyword: string
  setSearchKeyword: (value: string) => void
  selectedId: string | null
  setSelectedId: (id: string) => void
  expandedId: string | null
  setExpandedId: React.Dispatch<React.SetStateAction<string | null>>
  collapsed: boolean
  setCollapsed: React.Dispatch<React.SetStateAction<boolean>>
  horizon: Record<string, number>
  onHorizonChange: (patch: HorizonPatch) => void
  scheduler: ApplicationAnalysisSchedulerStatus | null
  saving: boolean
  onAddFromSearch: (item: StockSearchItem) => void
  onRemove: (id: string) => void
  onUpdateTarget: (id: string, patch: Partial<ApplicationAnalysisTarget>) => void
  onTriggerTarget: (targetId: string) => void
  onSave: () => void
  onToggleScheduler: () => void
  onRefreshAll: () => void
  compactMobile?: boolean
}) {
  const [stockSearchResults, setStockSearchResults] = useState<StockSearchItem[]>([])

  useEffect(() => {
    const keyword = searchKeyword.trim()
    if (!keyword) {
      setStockSearchResults([])
      return
    }
    let active = true
    const timer = window.setTimeout(() => {
      void searchStockChart(keyword)
        .then((items) => {
          if (active) setStockSearchResults(items)
        })
        .catch(() => {
          if (active) setStockSearchResults([])
        })
    }, 200)
    return () => {
      active = false
      window.clearTimeout(timer)
    }
  }, [searchKeyword])

  const targetIdSet = useMemo(
    () => new Set(targets.map((t) => `${t.target_type}-${t.symbol}`)),
    [targets],
  )

  const mergedList = useMemo<ListItem[]>(() => {
    const keyword = searchKeyword.trim().toLowerCase()
    const matchedTargets: ListItem[] = targets
      .filter((t) => {
        if (!keyword) return true
        const haystack = `${t.id} ${t.symbol} ${t.name} ${t.target_type} ${(t.tags || []).join(" ")}`.toLowerCase()
        return haystack.includes(keyword)
      })
      .map((t) => ({ kind: "target" as const, data: t }))

    const searchOnly: ListItem[] = keyword
      ? stockSearchResults
          .filter((item) => !targetIdSet.has(`${item.target_type}-${item.symbol}`))
          .map((item) => ({ kind: "search" as const, data: item }))
      : []

    return [...matchedTargets, ...searchOnly]
  }, [targets, stockSearchResults, searchKeyword, targetIdSet])

  return (
    <Card className="w-full min-w-0 max-w-full gap-0 overflow-hidden rounded-3xl border-0 bg-muted/40 py-0 shadow-sm">
      <CardHeader className="px-3 pb-3 pt-4 sm:px-4 2xl:px-6">
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <CardTitle className="flex items-center gap-2 text-base">
              <Target className="size-4 text-slate-600" />
              Watchlist
            </CardTitle>
            <CardDescription className="hidden break-words sm:block">Postgres application analysis targets + self-selected target sync</CardDescription>
          </div>
          <div className="flex items-center gap-2">
            <Button
              className="rounded-xl"
              size="icon-sm"
              variant="ghost"
              onClick={() => setCollapsed((value) => !value)}
              aria-label={collapsed ? "Expand Watchlist" : "Collapse Watchlist"}
            >
              {collapsed ? <ChevronRight className="size-4" /> : <ChevronDown className="size-4" />}
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-3 px-3 pb-4 sm:px-4 2xl:px-6">
        <div className="space-y-2">
          <div className="relative">
            <Search className="pointer-events-none absolute left-2 top-1/2 size-3.5 -translate-y-1/2 text-slate-400" />
            <input
              type="search"
              value={searchKeyword}
              onChange={(event) => setSearchKeyword(event.target.value)}
              placeholder="Search targets / stocks · name / symbol"
              className="w-full rounded-xl border border-slate-200 bg-white pl-7 pr-3 py-1.5 text-xs text-slate-700 outline-none focus:border-slate-400 focus:ring-2 focus:ring-slate-200"
            />
          </div>
          {collapsed && !searchKeyword.trim() ? null : (
          <AnimatedList
            items={mergedList}
            selectedIndex={mergedList.findIndex((item) => item.kind === "target" && item.data.id === selectedId)}
            onItemSelect={(item) => {
              if (item.kind === "target") {
                setSelectedId(item.data.id)
                if (!collapsed) {
                  setExpandedId((current) => (current === item.data.id ? null : item.data.id))
                }
              } else {
                onAddFromSearch(item.data)
              }
            }}
            renderItem={(item) => {
              if (item.kind === "target") {
                const target = item.data
                const isExpanded = !collapsed && expandedId === target.id
                const isSelected = target.id === selectedId
                return (
                  <div
                    className={`overflow-hidden rounded-xl transition ${
                      isSelected
                        ? "bg-background shadow-sm ring-1 ring-border"
                        : "bg-background/90 shadow-[0_1px_2px_rgba(15,23,42,0.04)] hover:bg-background hover:shadow-sm"
                    }`}
                  >
                    <div className="flex w-full flex-col gap-2 px-3 py-2.5 text-left sm:flex-row sm:items-center sm:justify-between">
                      <div className="flex min-w-0 items-start gap-2 sm:items-center">
                        {!collapsed ? (
                          isExpanded ? <ChevronDown className="mt-0.5 size-3.5 shrink-0 text-slate-500 sm:mt-0" /> : <ChevronRight className="mt-0.5 size-3.5 shrink-0 text-slate-500 sm:mt-0" />
                        ) : null}
                        <div className="min-w-0">
                          <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-sm font-semibold text-slate-800">
                            <span className="min-w-0 break-words">{target.name}</span>
                            <span className="shrink-0 text-slate-400">· {target.symbol}</span>
                          </div>
                          <div className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-slate-500">
                            <Clock className="size-3 shrink-0" />Every {formatIntervalLabel(target.interval_minutes)}
                            {target.last_updated_at ? <span>· Last {new Date(target.last_updated_at).toLocaleString()}</span> : null}
                          </div>
                        </div>
                      </div>
                      {target.enabled ? (
                        <Badge className={`w-fit rounded-md border-emerald-200 bg-emerald-50 px-1.5 text-[11px] text-emerald-700 ${compactMobile ? "max-lg:hidden" : ""}`} variant="outline">Enabled</Badge>
                      ) : (
                        <Badge className={`w-fit rounded-md border-border bg-muted px-1.5 text-[11px] text-muted-foreground ${compactMobile ? "max-lg:hidden" : ""}`} variant="outline">Disabled</Badge>
                      )}
                    </div>
                    {isExpanded ? (
                      <div className="space-y-2 bg-muted/45 px-3 pb-2.5 pt-2 ring-1 ring-inset ring-border/60" onClick={(event) => event.stopPropagation()}>
                        <div className="flex w-full min-w-0 items-center gap-2 text-xs">
                          <label className="flex min-w-0 flex-1 items-center gap-1.5">
                            <span className="shrink-0 text-[10px] font-medium text-muted-foreground">Adjust</span>
                            <Select value={target.adjust} onValueChange={(value) => onUpdateTarget(target.id, { adjust: value })}>
                              <SelectTrigger className={`h-5 min-w-0 flex-1 rounded-md border-0 bg-muted px-1.5 py-0 shadow-none ring-0 [&_svg]:size-3 ${compactMobile ? "px-1 text-[10px]" : "text-[11px]"}`}><SelectValue /></SelectTrigger>
                              <SelectContent>
                                <SelectItem value="qfq">QFQ</SelectItem>
                                <SelectItem value="none">Unadjusted</SelectItem>
                                <SelectItem value="hfq">HFQ</SelectItem>
                              </SelectContent>
                            </Select>
                          </label>
                          <div className="h-3 w-px shrink-0 bg-border" />
                          <label className="flex min-w-0 flex-1 items-center gap-1.5">
                            <span className="shrink-0 text-[10px] font-medium text-muted-foreground">Interval</span>
                            <Select
                              value={target.interval_minutes.toString()}
                              onValueChange={(value) => onUpdateTarget(target.id, { interval_minutes: Number(value) })}
                            >
                              <SelectTrigger className={`h-5 min-w-0 flex-1 rounded-md border-0 bg-muted px-1.5 py-0 shadow-none ring-0 [&_svg]:size-3 ${compactMobile ? "px-1 text-[10px]" : "text-[11px]"}`}><SelectValue /></SelectTrigger>
                              <SelectContent>
                                <SelectItem value="15">15 min</SelectItem>
                                <SelectItem value="30">30 min</SelectItem>
                                <SelectItem value="60">1 hour</SelectItem>
                                <SelectItem value="120">2 hours</SelectItem>
                                <SelectItem value="240">4 hours</SelectItem>
                                <SelectItem value="1440">1 day</SelectItem>
                              </SelectContent>
                            </Select>
                          </label>
                        </div>
                        <div className="grid grid-cols-[1fr_1.35fr_1fr] gap-2 text-xs">
                          <Button
                            size="sm"
                            variant="secondary"
                            className="h-7 w-full rounded-lg px-2 text-[11px]"
                            onClick={() => onUpdateTarget(target.id, { enabled: !target.enabled })}
                          >
                            <Power className={`mr-1 size-3 shrink-0 ${target.enabled ? "text-slate-400" : "text-emerald-600"}`} />
                            {target.enabled ? "Disable" : "Enable"}
                          </Button>
                          <Button
                            size="sm"
                            variant="secondary"
                            className="h-7 w-full rounded-lg px-2 text-[11px]"
                            onClick={() => onTriggerTarget(target.id)}
                          >
                            <RefreshCw className="mr-1 size-3 shrink-0 text-sky-600" />
                            <span>Run Now</span>
                          </Button>
                          <Button
                            size="sm"
                            variant="ghost"
                            className="h-7 w-full rounded-lg px-2 text-[11px] text-muted-foreground hover:bg-rose-50 hover:text-rose-700"
                            onClick={() => onRemove(target.id)}
                          >
                            <Trash2 className="mr-1 size-3 shrink-0" />
                            <span>Delete</span>
                          </Button>
                        </div>
                      </div>
                    ) : null}
                  </div>
                )
              }

              const searchItem = item.data
              return (
                <div
                  className="rounded-xl bg-muted/70 shadow-[0_1px_2px_rgba(15,23,42,0.04)] transition hover:bg-muted"
                  onClick={(event) => event.stopPropagation()}
                >
                  <div className="flex w-full flex-col gap-2 px-3 py-2.5 text-left sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex min-w-0 items-start gap-2 sm:items-center">
                      <Plus className="mt-0.5 size-3.5 shrink-0 text-slate-400 sm:mt-0" />
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-sm font-semibold text-slate-800">
                          <span className="min-w-0 break-words">{searchItem.name}</span>
                          <span className="shrink-0 text-slate-400">· {searchItem.symbol}</span>
                        </div>
                        <div className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-slate-500">
                          <Clock className="size-3 shrink-0" />Every 1 hour
                          <Badge className="rounded-md border-border bg-background px-1.5 text-muted-foreground" variant="outline">Disabled</Badge>
                          <span>· Not added</span>
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center justify-between gap-2 sm:justify-end">
                      <Badge className={`rounded-md border-border bg-background px-1.5 text-[11px] text-muted-foreground ${compactMobile ? "max-lg:hidden" : ""}`} variant="outline">{searchItem.target_type}</Badge>
                      <button
                        type="button"
                        aria-label="Add to targets"
                        className="inline-flex size-7 items-center justify-center rounded-lg bg-background text-muted-foreground shadow-sm ring-1 ring-border transition hover:bg-background hover:text-foreground"
                        onClick={() => onAddFromSearch(searchItem)}
                      >
                        <Plus className="size-3.5" />
                      </button>
                    </div>
                  </div>
                </div>
              )
            }}
            emptyMessage={
              searchKeyword.trim()
                ? "No matching targets or stocks."
                : targets.length === 0
                  ? "No targets yet. Search by keyword to add a stock."
                  : "No matching targets."
            }
            maxHeight={collapsed ? "max-h-[24vh]" : "max-h-[36vh] lg:max-h-[40vh]"}
            className=""
            itemClassName=""
          />
          )}
        </div>
        {!collapsed ? (
          <div className="space-y-2 rounded-2xl border border-slate-200 bg-background p-3">
            <div className="text-xs font-semibold text-slate-600">Data Horizon</div>
            <div className={compactMobile ? "grid grid-cols-4 gap-1 text-[10px]" : "grid grid-cols-1 gap-2 text-xs sm:grid-cols-2"}>
              <label className={`flex flex-col gap-1.5 rounded-lg border border-slate-200 bg-white px-2 py-1 ${compactMobile ? "items-stretch gap-1 px-1 py-1" : "sm:flex-row sm:items-center sm:justify-between sm:gap-2"}`}>
                Days<input
                  type="number"
                  min={30}
                  value={horizon.days}
                  onChange={(event) => onHorizonChange({ days: Number(event.target.value) || 120 })}
                  className={`w-full rounded-md border border-slate-200 px-2 text-left ${compactMobile ? "h-7 px-1 text-[10px]" : "sm:w-16 sm:text-right"}`}
                />
              </label>
              <label className={`flex flex-col gap-1.5 rounded-lg border border-slate-200 bg-white px-2 py-1 ${compactMobile ? "items-stretch gap-1 px-1 py-1" : "sm:flex-row sm:items-center sm:justify-between sm:gap-2"}`}>
                Segments<input
                  type="number"
                  min={1}
                  value={horizon.segments}
                  onChange={(event) => onHorizonChange({ segments: Number(event.target.value) || 4 })}
                  className={`w-full rounded-md border border-slate-200 px-2 text-left ${compactMobile ? "h-7 px-1 text-[10px]" : "sm:w-16 sm:text-right"}`}
                />
              </label>
              <label className={`flex flex-col gap-1.5 rounded-lg border border-slate-200 bg-white px-2 py-1 ${compactMobile ? "items-stretch gap-1 px-1 py-1" : "sm:flex-row sm:items-center sm:justify-between sm:gap-2"}`}>
                Monthly K<input
                  type="number"
                  min={1}
                  value={horizon.monthly_keep}
                  onChange={(event) => onHorizonChange({ monthly_keep: Number(event.target.value) || 6 })}
                  className={`w-full rounded-md border border-slate-200 px-2 text-left ${compactMobile ? "h-7 px-1 text-[10px]" : "sm:w-16 sm:text-right"}`}
                />
              </label>
              <label className={`flex flex-col gap-1.5 rounded-lg border border-slate-200 bg-white px-2 py-1 ${compactMobile ? "items-stretch gap-1 px-1 py-1" : "sm:flex-row sm:items-center sm:justify-between sm:gap-2"}`}>
                Weekly K<input
                  type="number"
                  min={1}
                  value={horizon.weekly_keep}
                  onChange={(event) => onHorizonChange({ weekly_keep: Number(event.target.value) || 12 })}
                  className={`w-full rounded-md border border-slate-200 px-2 text-left ${compactMobile ? "h-7 px-1 text-[10px]" : "sm:w-16 sm:text-right"}`}
                />
              </label>
            </div>
            <div className={compactMobile ? "grid grid-cols-3 gap-2" : "grid grid-cols-1 gap-2 sm:flex sm:flex-wrap sm:items-center"}>
              <Button className="w-full rounded-xl sm:w-auto" size="sm" disabled={saving} onClick={onSave}>
                <Save className="mr-1 size-3.5" />{saving ? "Saving" : "Save Config"}
              </Button>
              <Button
                className="w-full rounded-xl sm:w-auto"
                size="sm"
                variant={scheduler?.running ? "destructive" : "default"}
                onClick={onToggleScheduler}
              >
                {scheduler?.running ? "Stop Scheduler" : "Start Scheduler"}
              </Button>
              <Button className="w-full rounded-xl sm:w-auto" size="sm" variant="outline" onClick={onRefreshAll}>
                Refresh All
              </Button>
            </div>
            <div className="text-xs text-slate-500">
              {scheduler
                ? `Scheduler ${scheduler.running ? "running" : "stopped"} · ${scheduler.runs_count ?? 0} runs · enabled ${scheduler.enabled_target_count ?? 0}/${scheduler.total_target_count ?? 0}`
                : "Scheduler status unavailable"}
            </div>
          </div>
        ) : null}
      </CardContent>
    </Card>
  )
}
