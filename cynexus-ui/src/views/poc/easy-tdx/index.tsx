import { useCallback, useEffect, useMemo, useState } from "react"
import { Activity, BarChart3, Blocks, Database, LineChart, RefreshCw, Search, Waves } from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { cn } from "@/lib/utils"
import {
  fetchEasyTdxBoardMembers,
  fetchEasyTdxBoardRanking,
  fetchEasyTdxBoards,
  fetchEasyTdxBoardSummary,
  fetchEasyTdxFundFlow,
  fetchEasyTdxKline,
  fetchEasyTdxTimeshare,
  type EasyTdxRow,
} from "@/services/market/easy-tdx"

function compact(value: unknown): string {
  if (value === null || value === undefined || value === "") return "-"
  if (typeof value === "object") return JSON.stringify(value)
  return String(value)
}

function columnsFor(rows: EasyTdxRow[]): string[] {
  const keys: string[] = []
  for (const row of rows) {
    for (const key of Object.keys(row)) if (!keys.includes(key)) keys.push(key)
  }
  return keys.slice(0, 12)
}

function DataTable({ rows, loading, empty = "暂无数据" }: { rows: EasyTdxRow[]; loading?: boolean; empty?: string }) {
  const columns = useMemo(() => columnsFor(rows), [rows])
  return (
    <div className="overflow-hidden rounded-md bg-muted/45">
      <div className="max-h-[390px] overflow-auto">
        <Table className="min-w-[760px]">
          <TableHeader className="sticky top-0 z-10 bg-muted/95 [&_th]:h-9">
            <TableRow>{columns.map((column) => <TableHead key={column} className="font-mono text-[11px]">{column}</TableHead>)}</TableRow>
          </TableHeader>
          <TableBody>
            {rows.length ? rows.map((row, index) => (
              <TableRow key={index}>
                {columns.map((column) => <TableCell key={column} className="max-w-[240px] align-top font-mono text-xs" title={compact(row[column])}>{compact(row[column])}</TableCell>)}
              </TableRow>
            )) : <TableRow><TableCell colSpan={Math.max(columns.length, 1)} className="h-28 text-center text-sm text-muted-foreground">{loading ? "正在读取..." : empty}</TableCell></TableRow>}
          </TableBody>
        </Table>
      </div>
    </div>
  )
}

function MiniLine({ rows, field }: { rows: EasyTdxRow[]; field: string }) {
  const values = rows.map((row) => Number(row[field])).filter(Number.isFinite)
  if (values.length < 2) return <div className="flex h-20 items-center justify-center text-xs text-muted-foreground">暂无可绘制数值</div>
  const min = Math.min(...values)
  const max = Math.max(...values)
  const points = values.map((value, index) => `${(index / (values.length - 1)) * 100},${92 - ((value - min) / Math.max(max - min, 1)) * 84}`).join(" ")
  return <svg viewBox="0 0 100 100" preserveAspectRatio="none" className="h-20 w-full text-primary" aria-label={`${field} trend`}><polyline points={points} fill="none" stroke="currentColor" strokeWidth="2" vectorEffect="non-scaling-stroke" /></svg>
}

function QueryHeader({ code, setCode, tradeDate, setTradeDate, onRefresh, loading }: { code: string; setCode: (value: string) => void; tradeDate: string; setTradeDate: (value: string) => void; onRefresh: () => void; loading: boolean }) {
  return <div className="flex flex-col gap-3 border-b border-border/50 pb-4 lg:flex-row lg:items-end lg:justify-between">
    <div><div className="flex items-center gap-2"><Badge variant="secondary" className="rounded-md"><Database className="size-3.5" />easy_tdx</Badge><span className="font-mono text-xs text-muted-foreground">TDX / MAC online</span></div><h1 className="mt-2 text-2xl font-semibold tracking-tight">数据源能力工作台</h1><p className="mt-1 max-w-3xl text-sm leading-6 text-muted-foreground">直接读取标准 TDX/MAC 客户端能力，查看线上 1 分钟、复权、分时、资金流和板块返回结果。</p></div>
    <div className="flex flex-wrap items-center gap-2"><label className="sr-only" htmlFor="easy-tdx-code">股票代码</label><Input id="easy-tdx-code" value={code} onChange={(event) => setCode(event.target.value)} className="w-32 font-mono" placeholder="600519" /><label className="sr-only" htmlFor="easy-tdx-date">交易日</label><Input id="easy-tdx-date" type="date" value={tradeDate} onChange={(event) => setTradeDate(event.target.value)} className="w-40" /><Button variant="outline" onClick={onRefresh} disabled={loading}><RefreshCw className={cn("size-4", loading && "animate-spin")} />刷新当前面板</Button></div>
  </div>
}

export default function EasyTdxPoc() {
  const [code, setCode] = useState("600519")
  const [tradeDate, setTradeDate] = useState(new Date().toISOString().slice(0, 10))
  const [loading, setLoading] = useState(false)
  const [flowLoading, setFlowLoading] = useState(false)
  const [error, setError] = useState("")
  const [kline, setKline] = useState<EasyTdxRow[]>([])
  const [qfqKline, setQfqKline] = useState<EasyTdxRow[]>([])
  const [hfqKline, setHfqKline] = useState<EasyTdxRow[]>([])
  const [timeshare, setTimeshare] = useState<EasyTdxRow[]>([])
  const [currentFlow, setCurrentFlow] = useState<EasyTdxRow[]>([])
  const [historyFlow, setHistoryFlow] = useState<EasyTdxRow[]>([])
  const [boards, setBoards] = useState<EasyTdxRow[]>([])
  const [members, setMembers] = useState<EasyTdxRow[]>([])
  const [summary, setSummary] = useState<EasyTdxRow[]>([])
  const [ranking, setRanking] = useState<EasyTdxRow[]>([])
  const [changeRanking, setChangeRanking] = useState<EasyTdxRow[]>([])
  const [boardType, setBoardType] = useState("hy")
  const [boardSymbol, setBoardSymbol] = useState("")
  const [activeTab, setActiveTab] = useState("flow")

  const run = useCallback(async (task: () => Promise<void>) => { setLoading(true); setError(""); try { await task() } catch (err) { setError(err instanceof Error ? err.message : "easy_tdx 数据读取失败") } finally { setLoading(false) } }, [])
  const loadMarket = useCallback(() => run(async () => {
    const [minute, qfq, hfq] = await Promise.all([
      fetchEasyTdxKline({ code, period: "1m", adjust: "none", limit: 120 }),
      fetchEasyTdxKline({ code, period: "day", adjust: "qfq", limit: 120 }),
      fetchEasyTdxKline({ code, period: "day", adjust: "hfq", limit: 120 }),
    ])
    setKline(minute.items)
    setQfqKline(qfq.items)
    setHfqKline(hfq.items)
    setSummary([])
    setRanking([])
    setChangeRanking([])
  }), [code, run])

  const loadTimeshare = () => run(async () => { const data = await fetchEasyTdxTimeshare({ code, tradeDate }); setTimeshare(data.items) })
  const loadFlow = useCallback(async () => {
    setFlowLoading(true)
    try {
      const [current, history] = await Promise.allSettled([
        fetchEasyTdxFundFlow({ code, history: false, count: 1 }),
        fetchEasyTdxFundFlow({ code, history: true, count: 120 }),
      ])
      if (current.status === "fulfilled") setCurrentFlow(current.value.items)
      if (history.status === "fulfilled") setHistoryFlow(history.value.items)
      if (current.status === "rejected" && history.status === "rejected") {
        throw history.reason
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "资金流数据读取失败")
    } finally {
      setFlowLoading(false)
    }
  }, [code])
  const loadBoards = () => run(async () => { const [list, rank, change] = await Promise.all([fetchEasyTdxBoards({ boardType, count: 100 }), fetchEasyTdxBoardRanking({ boardType, topN: 30, change: false }), fetchEasyTdxBoardRanking({ boardType, topN: 30, change: true })]); setBoards(list.items); setRanking(rank.items); setChangeRanking(change.items); if (list.items[0]) { const symbol = String(list.items[0].symbol ?? list.items[0].code ?? ""); setBoardSymbol(symbol); const [boardSummary, boardMembers] = await Promise.all([fetchEasyTdxBoardSummary(symbol), fetchEasyTdxBoardMembers({ boardSymbol: symbol, count: 100 })]); setSummary(boardSummary.items || [boardSummary as unknown as EasyTdxRow]); setMembers(boardMembers.items) } })
  const refresh = () => void loadMarket()

  useEffect(() => { void loadMarket() }, [])
  useEffect(() => { void loadFlow() }, [loadFlow])

  return <div className="space-y-5">
    <QueryHeader code={code} setCode={setCode} tradeDate={tradeDate} setTradeDate={setTradeDate} onRefresh={refresh} loading={loading} />
    {error ? <Alert variant="destructive"><AlertTitle>数据源请求失败</AlertTitle><AlertDescription>{error}</AlertDescription></Alert> : null}
    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4"><div className="rounded-md bg-muted/45 p-3"><LineChart className="size-4 text-primary" /><p className="mt-3 text-sm font-medium">线上 1 分钟</p><p className="mt-1 text-xs text-muted-foreground">MAC 直接取数，不依赖本地文件</p></div><div className="rounded-md bg-muted/45 p-3"><Activity className="size-4 text-emerald-600" /><p className="mt-3 text-sm font-medium">复权日线</p><p className="mt-1 text-xs text-muted-foreground">qfq / hfq 可直接切换验证</p></div><div className="rounded-md bg-muted/45 p-3"><Waves className="size-4 text-sky-600" /><p className="mt-3 text-sm font-medium">历史分时</p><p className="mt-1 text-xs text-muted-foreground">标准 TDX 历史 timeshare</p></div><div className="rounded-md bg-muted/45 p-3"><Blocks className="size-4 text-orange-600" /><p className="mt-3 text-sm font-medium">板块与资金流</p><p className="mt-1 text-xs text-muted-foreground">MAC 板块查询 + TDX 资金流</p></div></div>
    <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
      <TabsList className="h-fit w-full justify-start gap-1 overflow-x-auto rounded-md bg-muted/60 p-1"><TabsTrigger value="market">K 线 / 复权</TabsTrigger><TabsTrigger value="timeshare">历史分时</TabsTrigger><TabsTrigger value="flow">个股资金流</TabsTrigger><TabsTrigger value="boards">板块能力</TabsTrigger></TabsList>
      <TabsContent value="market" className="mt-4 grid gap-4 xl:grid-cols-2"><Card><CardHeader className="pb-3"><CardTitle className="flex items-center gap-2 text-base"><LineChart className="size-4" />在线 1 分钟 K 线</CardTitle><CardDescription>MAC · {code} · none · {kline.length} rows</CardDescription></CardHeader><CardContent><MiniLine rows={kline} field="close" /><DataTable rows={kline.slice(-12)} loading={loading} /></CardContent></Card><Card><CardHeader className="pb-3"><CardTitle className="flex items-center gap-2 text-base"><BarChart3 className="size-4" />前复权日线</CardTitle><CardDescription>MAC · {code} · qfq · {qfqKline.length} rows</CardDescription></CardHeader><CardContent><MiniLine rows={qfqKline} field="close" /><DataTable rows={qfqKline.slice(-12)} loading={loading} /></CardContent></Card><Card><CardHeader className="pb-3"><CardTitle className="flex items-center gap-2 text-base"><BarChart3 className="size-4" />后复权日线</CardTitle><CardDescription>MAC · {code} · hfq · {hfqKline.length} rows</CardDescription></CardHeader><CardContent><MiniLine rows={hfqKline} field="close" /><DataTable rows={hfqKline.slice(-12)} loading={loading} /></CardContent></Card></TabsContent>
      <TabsContent value="timeshare" className="mt-4"><Card><CardHeader className="flex-row items-end justify-between pb-3"><div><CardTitle className="flex items-center gap-2 text-base"><Waves className="size-4" />历史分时</CardTitle><CardDescription>TDX · {code} · {tradeDate}</CardDescription></div><Button onClick={() => void loadTimeshare()} disabled={loading}><Search className="size-4" />读取分时</Button></CardHeader><CardContent><MiniLine rows={timeshare} field="price" /><DataTable rows={timeshare} loading={loading} empty="点击读取分时" /></CardContent></Card></TabsContent>
      <TabsContent value="flow" className="mt-4 grid gap-4 xl:grid-cols-[minmax(0,0.8fr)_minmax(0,1.2fr)]"><Card><CardHeader className="pb-3"><CardTitle className="flex items-center gap-2 text-base"><Activity className="size-4" />当前资金流</CardTitle><CardDescription>TDX 分类资金流快照</CardDescription></CardHeader><CardContent><Button className="mb-3" onClick={() => void loadFlow()} disabled={flowLoading}><Search className="size-4" />刷新当前与历史</Button><DataTable rows={currentFlow} loading={flowLoading} empty="正在等待资金流数据" /></CardContent></Card><Card><CardHeader className="pb-3"><CardTitle className="text-base">历史资金流 <span className="font-mono text-xs font-normal text-muted-foreground">{historyFlow.length} rows</span></CardTitle><CardDescription>标准 TDX 历史日级资金流，最多 120 条</CardDescription></CardHeader><CardContent><MiniLine rows={historyFlow} field="main_net" /><DataTable rows={historyFlow} loading={flowLoading} empty="正在等待资金流数据" /></CardContent></Card></TabsContent>
      <TabsContent value="boards" className="mt-4 space-y-4"><div className="flex flex-wrap items-end gap-2"><div><label className="mb-1 block text-xs text-muted-foreground">板块类型</label><Select value={boardType} onValueChange={setBoardType}><SelectTrigger className="w-36"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="hy">行业</SelectItem><SelectItem value="gn">概念</SelectItem><SelectItem value="fg">风格</SelectItem><SelectItem value="dq">地区</SelectItem><SelectItem value="all">全部</SelectItem></SelectContent></Select></div><div><label className="mb-1 block text-xs text-muted-foreground">板块代码</label><Input value={boardSymbol} onChange={(event) => setBoardSymbol(event.target.value)} placeholder="点击列表后自动填充" className="w-48 font-mono" /></div><Button onClick={() => void loadBoards()} disabled={loading}><Search className="size-4" />读取板块全套能力</Button></div><div className="grid gap-4 xl:grid-cols-2"><Card><CardHeader className="pb-3"><CardTitle className="text-base">板块列表 / 排名</CardTitle><CardDescription>MAC · list / ranking / change-ranking</CardDescription></CardHeader><CardContent className="space-y-3"><DataTable rows={boards.slice(0, 20)} loading={loading} empty="点击读取板块" /><DataTable rows={ranking.slice(0, 15)} loading={loading} /></CardContent></Card><Card><CardHeader className="pb-3"><CardTitle className="text-base">板块详情</CardTitle><CardDescription>summary / members / 20 日变化排名</CardDescription></CardHeader><CardContent className="space-y-3"><DataTable rows={summary} loading={loading} empty="点击读取板块" /><DataTable rows={members.slice(0, 30)} loading={loading} /><DataTable rows={changeRanking.slice(0, 15)} loading={loading} /></CardContent></Card></div></TabsContent>
    </Tabs>
  </div>
}
