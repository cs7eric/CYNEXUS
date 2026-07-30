import { useEffect, useMemo, useState } from "react"
import { ArrowDown, ArrowUp, BookmarkPlus, Check, Loader2, Users } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { createSelfSelectedItem, fetchSelfSelectedGroups, type SelfSelectedGroup } from "@/services/market/self-selected"
import { fetchFuturesPositionHistory, fetchFuturesPositionMembers, type FuturesPositionHistory, type FuturesPositionInstrument, type FuturesPositionMember, type FuturesRankType, type FuturesScopeType } from "@/services/market/futures-position"
import { formatDate, formatPosition, formatRatio, formatSigned, toneForValue } from "../lib/format"

interface PositionDrawerProps {
  row: FuturesPositionInstrument | null
  scopeType: FuturesScopeType
  exchangeCode: string
  top: number
  tradeDate?: string | null
  onClose: () => void
}

export function PositionDrawer({ row, scopeType, exchangeCode, top, tradeDate, onClose }: PositionDrawerProps) {
  const [members, setMembers] = useState<FuturesPositionMember[]>([])
  const [history, setHistory] = useState<FuturesPositionHistory[]>([])
  const [activeRank, setActiveRank] = useState<FuturesRankType>("LONG")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [groups, setGroups] = useState<SelfSelectedGroup[]>([])
  const [groupId, setGroupId] = useState("")
  const [savingWatchlist, setSavingWatchlist] = useState(false)
  const [watchlistSaved, setWatchlistSaved] = useState(false)
  const [watchlistError, setWatchlistError] = useState<string | null>(null)

  useEffect(() => {
    if (!row || !exchangeCode) return
    let alive = true
    setLoading(true)
    setError(null)
    setGroups([])
    setGroupId("")
    setWatchlistSaved(false)
    setWatchlistError(null)
    void fetchSelfSelectedGroups().then((response) => {
      if (!alive) return
      setGroups(response.items)
      setGroupId(response.items[0]?.id ?? "")
    }).catch((cause) => {
      if (alive) setWatchlistError(cause instanceof Error ? cause.message : String(cause))
    })
    Promise.all([
      fetchFuturesPositionMembers({ tradeDate: tradeDate ?? undefined, scopeType, exchangeCode, instrumentCode: row.instrumentCode, top }),
      fetchFuturesPositionHistory({ scopeType, exchangeCode, instrumentCode: row.instrumentCode, top, limit: 20 }),
    ]).then(([nextMembers, nextHistory]) => {
      if (!alive) return
      setMembers(nextMembers)
      setHistory(nextHistory)
    }).catch((cause) => {
      if (alive) setError(cause instanceof Error ? cause.message : String(cause))
    }).finally(() => { if (alive) setLoading(false) })
    return () => { alive = false }
  }, [exchangeCode, row, scopeType, top, tradeDate])

  const saveToWatchlist = async () => {
    if (!row || !groupId || savingWatchlist) return
    setSavingWatchlist(true)
    setWatchlistError(null)
    try {
      await createSelfSelectedItem({
        group_id: groupId,
        symbol: `${scopeType}:${exchangeCode}:${row.instrumentCode}`,
        market: exchangeCode,
        name: row.instrumentCode,
        notes: "期货会员持仓监控",
        target_type: "future_instrument",
      })
      setWatchlistSaved(true)
    } catch (cause) {
      setWatchlistError(cause instanceof Error ? cause.message : String(cause))
    } finally {
      setSavingWatchlist(false)
    }
  }

  const groupedMembers = useMemo(() => members.filter((member) => member.rankType === activeRank), [activeRank, members])
  const maxHistory = useMemo(() => Math.max(1, ...history.flatMap((item) => [item.longPosition ?? 0, item.shortPosition ?? 0])), [history])

  return <Sheet open={row != null} onOpenChange={(open) => { if (!open) onClose() }}>
    <SheetContent side="right" className="w-[min(100vw,560px)] gap-0 p-0 sm:max-w-xl">
      <SheetHeader className="border-b border-border/60 pr-12">
        <div className="flex flex-wrap items-center gap-2"><Badge variant="outline" className="font-mono">{exchangeCode}</Badge><Badge variant="secondary">Top {top}</Badge></div>
        <SheetTitle className="font-mono text-lg">{row?.instrumentCode ?? "期货标的"}</SheetTitle>
        <SheetDescription>交易所会员排名明细与近 {history.length || 20} 个有效交易日的结构变化。</SheetDescription>
      </SheetHeader>
      <div className="min-h-0 flex-1 overflow-y-auto">
        {row ? <div className="space-y-4 p-4">
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
            {[{ label: "成交量", value: row.volume, tone: "text-foreground" }, { label: "多头", value: row.longPosition, tone: "text-rose-600 dark:text-rose-400" }, { label: "空头", value: row.shortPosition, tone: "text-emerald-600 dark:text-emerald-400" }, { label: "多空比", value: row.longShortRatio, tone: "text-foreground", ratio: true }].map((item) => <div key={item.label} className="rounded-md bg-muted/45 px-3 py-2"><div className="text-[11px] text-muted-foreground">{item.label}</div><div className={`mt-1 font-mono text-sm font-semibold tabular-nums ${item.tone}`}>{item.ratio ? formatRatio(item.value) : formatPosition(item.value)}</div></div>)}
          </div>
          <div className="flex flex-wrap items-center gap-2 rounded-md border border-border/60 bg-muted/25 px-3 py-2">
            <BookmarkPlus className="size-4 text-muted-foreground" />
            <span className="text-xs text-muted-foreground">加入自选监控</span>
            {groups.length > 0 ? <>
              <Select value={groupId} onValueChange={setGroupId}>
                <SelectTrigger size="sm" className="h-7 min-w-[130px] text-xs"><SelectValue placeholder="选择分类" /></SelectTrigger>
                <SelectContent>{groups.map((group) => <SelectItem key={group.id} value={group.id}>{group.name}</SelectItem>)}</SelectContent>
              </Select>
              <Button type="button" size="sm" variant="outline" onClick={() => void saveToWatchlist()} disabled={savingWatchlist || !groupId} className="h-7 gap-1 text-xs">
                {savingWatchlist ? <Loader2 className="size-3 animate-spin" /> : watchlistSaved ? <Check className="size-3 text-emerald-600" /> : <BookmarkPlus className="size-3" />}
                {watchlistSaved ? "已加入" : "加入观察"}
              </Button>
            </> : <span className="text-xs text-muted-foreground">暂无自选分类，请先在自选页面创建。</span>}
            {watchlistError ? <span className="basis-full text-xs text-rose-600 dark:text-rose-400">自选操作失败：{watchlistError}</span> : null}
          </div>
          {error ? <div className="rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700 dark:border-rose-900/50 dark:bg-rose-950/20 dark:text-rose-300">明细加载失败：{error}</div> : null}
          <section className="rounded-lg border border-border/70" aria-label="会员排名">
            <div className="border-b border-border/60 px-3 py-3"><div className="flex items-center gap-2 text-sm font-semibold"><Users className="size-4 text-muted-foreground" />会员排名</div><p className="mt-1 text-xs text-muted-foreground">三类榜单相互独立，排名相同不代表同一会员。</p></div>
            <Tabs value={activeRank} onValueChange={(value) => setActiveRank(value as FuturesRankType)}>
              <TabsList variant="line" className="w-full justify-start rounded-none border-b border-border/60 px-2"><TabsTrigger value="LONG" className="text-xs">多头</TabsTrigger><TabsTrigger value="SHORT" className="text-xs">空头</TabsTrigger><TabsTrigger value="VOLUME" className="text-xs">成交量</TabsTrigger></TabsList>
              <TabsContent value={activeRank} className="m-0">
                {loading ? <div className="space-y-2 p-3">{Array.from({ length: 5 }, (_, index) => <Skeleton key={index} className="h-7 w-full" />)}</div> : groupedMembers.length > 0 ? <div className="divide-y divide-border/60">{groupedMembers.map((member) => <div key={`${member.rankType}-${member.rankNo}-${member.memberName}`} className="grid grid-cols-[36px_minmax(0,1fr)_96px_72px] items-center gap-2 px-3 py-2 text-xs"><span className="font-mono text-muted-foreground">{String(member.rankNo).padStart(2, "0")}</span><span className="truncate font-medium">{member.memberName || "未披露"}</span><span className="text-right font-mono tabular-nums">{formatPosition(member.rankValue)}</span><span className={`flex items-center justify-end gap-1 font-mono tabular-nums ${toneForValue(member.rankChange)}`}>{member.rankChange != null && member.rankChange > 0 ? <ArrowUp className="size-3" /> : member.rankChange != null && member.rankChange < 0 ? <ArrowDown className="size-3" /> : null}{formatSigned(member.rankChange)}</span></div>)}</div> : <div className="px-3 py-10 text-center text-sm text-muted-foreground">当前榜单没有会员明细</div>}
              </TabsContent>
            </Tabs>
          </section>
          <section className="rounded-lg border border-border/70" aria-label="历史结构">
            <div className="border-b border-border/60 px-3 py-3"><h3 className="text-sm font-semibold">结构历史</h3><p className="mt-1 text-xs text-muted-foreground">按当前 Top-N 口径汇总，日期顺序从旧到新。</p></div>
            {history.length > 0 ? <div className="divide-y divide-border/60">{history.map((item) => <div key={item.tradeDate} className="grid grid-cols-[72px_minmax(0,1fr)_76px] items-center gap-3 px-3 py-2.5 text-xs"><span className="font-mono text-muted-foreground">{formatDate(item.tradeDate)}</span><div className="space-y-1.5"><div className="flex items-center gap-2"><span className="w-7 shrink-0 text-[10px] text-rose-600 dark:text-rose-400">多</span><span className="h-1.5 rounded-full bg-rose-500/75" style={{ width: `${Math.max(3, ((item.longPosition ?? 0) / maxHistory) * 100)}%` }} /><span className="w-16 text-right font-mono tabular-nums">{formatPosition(item.longPosition)}</span></div><div className="flex items-center gap-2"><span className="w-7 shrink-0 text-[10px] text-emerald-600 dark:text-emerald-400">空</span><span className="h-1.5 rounded-full bg-emerald-500/75" style={{ width: `${Math.max(3, ((item.shortPosition ?? 0) / maxHistory) * 100)}%` }} /><span className="w-16 text-right font-mono tabular-nums">{formatPosition(item.shortPosition)}</span></div></div><span className={`text-right font-mono tabular-nums ${toneForValue(item.netPosition)}`}>{formatSigned(item.netPosition)}</span></div>)}</div> : <div className="px-3 py-10 text-center text-sm text-muted-foreground">暂无历史结构数据</div>}
          </section>
        </div> : null}
      </div>
      {loading ? <div className="pointer-events-none absolute right-12 top-5"><Loader2 className="size-4 animate-spin text-muted-foreground" /></div> : null}
    </SheetContent>
  </Sheet>
}
