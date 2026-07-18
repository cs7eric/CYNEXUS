import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import type { ReactNode } from "react"
import { BriefcaseBusiness, CalendarDays, FileChartColumn, Newspaper, RefreshCw, WalletCards } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { fetchEasyTdxBelongBoard, type EasyTdxBelongBoardRow, type EasyTdxResponse } from "@/services/market/easy-tdx"
import {
  fetchStockAnnouncements,
  fetchStockBusinessComposition,
  fetchStockCompanyNews,
  fetchStockFinanceReport,
  fetchStockNews,
  fetchStockProfitForecast,
  fetchStockRoadshows,
  fetchStockValuation,
  type StockAnnouncementsResponse,
  type StockBusinessCompositionResponse,
  type StockCompanyNewsResponse,
  type StockFinanceReportResponse,
  type StockNewsResponse,
  type StockProfitForecastResponse,
  type StockRoadshowsResponse,
  type StockValuationResponse,
} from "@/services/market/f10"

type ProfileView = "overview" | "finance" | "operations" | "events"
type FinanceReportType = "zcfzb" | "lrb" | "xjllb"

interface ProfileData {
  valuation: StockValuationResponse | null
  belongBoard: EasyTdxResponse<EasyTdxBelongBoardRow> | null
  finance: Partial<Record<FinanceReportType, StockFinanceReportResponse>>
  business: StockBusinessCompositionResponse | null
  forecast: StockProfitForecastResponse | null
  announcements: StockAnnouncementsResponse | null
  news: StockNewsResponse | null
  roadshows: StockRoadshowsResponse | null
  companyNews: StockCompanyNewsResponse | null
}

type ProfileErrors = Partial<Record<keyof ProfileData | FinanceReportType, string>>

const EMPTY_DATA: ProfileData = {
  valuation: null,
  belongBoard: null,
  finance: {},
  business: null,
  forecast: null,
  announcements: null,
  news: null,
  roadshows: null,
  companyNews: null,
}

const FINANCE_REPORTS: Array<{ value: FinanceReportType; label: string }> = [
  { value: "zcfzb", label: "资产负债表" },
  { value: "lrb", label: "利润表" },
  { value: "xjllb", label: "现金流量表" },
]

function displayValue(value: unknown, fractionDigits = 2): string {
  if (value === null || value === undefined || value === "") return "—"
  if (typeof value === "number" && Number.isFinite(value)) {
    return value.toLocaleString("zh-CN", { maximumFractionDigits: fractionDigits })
  }
  return String(value)
}

function formatDate(value: string | null | undefined): string {
  if (!value) return "—"
  const compact = value.match(/^(\d{4})(\d{2})(\d{2})/)
  return compact ? `${compact[1]}-${compact[2]}-${compact[3]}` : value.slice(0, 10)
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "数据源请求失败"
}

function SectionHeading({ icon: Icon, title, detail }: { icon: typeof BriefcaseBusiness; title: string; detail?: string }) {
  return (
    <div className="flex min-w-0 items-center gap-2">
      <span className="flex size-7 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-slate-600">
        <Icon className="size-3.5" />
      </span>
      <div className="min-w-0">
        <h2 className="text-sm font-semibold text-foreground">{title}</h2>
        {detail ? <p className="truncate text-[11px] text-muted-foreground">{detail}</p> : null}
      </div>
    </div>
  )
}

function EmptyState({ label }: { label: string }) {
  return <div className="flex min-h-24 items-center justify-center rounded-xl border border-dashed border-slate-200 bg-white px-4 py-6 text-center text-xs text-slate-500">{label}</div>
}

function ErrorNote({ message }: { message?: string }) {
  return message ? <div className="rounded-lg border border-amber-200/80 bg-amber-50/70 px-3 py-2 text-[11px] leading-5 text-amber-800">{message}</div> : null
}

function ProfileSkeleton() {
  return (
    <div className="space-y-3">
      <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-5">
        {[0, 1, 2, 3, 4].map((item) => <Skeleton key={item} className="h-[76px] rounded-none border-b border-slate-100 bg-slate-50/70 last:col-span-2 sm:border-b-0 sm:border-r sm:last:col-span-1 sm:last:border-r-0" />)}
      </div>
      <div className="grid gap-3 xl:grid-cols-2">
        {[0, 1].map((item) => (
          <div key={item} className="space-y-3 rounded-xl bg-white/70 p-3">
            <Skeleton className="h-4 w-36" />
            {[0, 1, 2, 3].map((row) => <Skeleton key={row} className="h-8 rounded-lg" />)}
          </div>
        ))}
      </div>
    </div>
  )
}

function Metric({ label, value, tone, className }: { label: string; value: string; tone?: string; className?: string }) {
  return (
    <div className={`min-w-0 rounded-xl bg-white/70 px-3 py-2.5 ${className ?? ""}`}>
      <div className="text-[10px] font-medium uppercase tracking-[0.06em] text-slate-500">{label}</div>
      <div className={`mt-1 truncate text-[15px] font-semibold tabular-nums ${tone ?? "text-slate-900"}`}>{value}</div>
    </div>
  )
}

function ValuationSection({ data, error }: { data: StockValuationResponse | null; error?: string }) {
  const latest = data?.rows?.[0]
  return (
    <section className="min-w-0 space-y-2 rounded-2xl bg-muted/50 p-3">
      <div className="flex items-center justify-between gap-3">
        <SectionHeading icon={WalletCards} title="估值快照" detail={latest?.date ? `最新数据 ${formatDate(latest.date)}` : "eltdx F10 valuation"} />
        <span className="text-[11px] text-muted-foreground">{data?.rows?.length ?? 0} 条</span>
      </div>
      <ErrorNote message={error} />
      {latest ? (
        <div className="grid gap-2 sm:grid-cols-2 2xl:grid-cols-5">
          <Metric label="PE TTM" value={displayValue(latest.peTtm)} />
          <Metric label="PB MRQ" value={displayValue(latest.pbMrq)} />
          <Metric label="PS TTM" value={displayValue(latest.psTtm)} />
          <Metric label="PEG" value={displayValue(latest.peg)} />
          <Metric label="流通市值" value={displayValue(latest.aliqMarketCap, 0)} className="col-span-2 2xl:col-span-1" />
        </div>
      ) : <EmptyState label={error ? "估值数据暂不可用" : "暂无估值数据"} />}
    </section>
  )
}

function FinanceReportSection({ data, error, reportType, onReportTypeChange }: {
  data: StockFinanceReportResponse | null
  error?: string
  reportType: FinanceReportType
  onReportTypeChange: (value: FinanceReportType) => void
}) {
  const rows = useMemo(() => data?.rows ?? [], [data?.rows])
  const fields = useMemo(() => {
    const keys = new Set<string>()
    for (const row of rows.slice(0, 4)) {
      for (const key of Object.keys(row.fields)) {
        if (key !== "rq") keys.add(key)
      }
    }
    return Array.from(keys).slice(0, 16)
  }, [rows])

  return (
    <section className="min-w-0 space-y-2 rounded-2xl bg-muted/50 p-3">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <SectionHeading icon={FileChartColumn} title="财务报表" detail="通达信 F10 原始字段，按报告期展示" />
        <div className="flex w-full gap-1 overflow-x-auto pb-1 sm:w-auto">
          {FINANCE_REPORTS.map((item) => (
            <button
              key={item.value}
              type="button"
              className={`h-7 shrink-0 rounded-md px-2.5 text-[11px] font-medium outline-none transition focus-visible:ring-2 focus-visible:ring-slate-400/60 ${reportType === item.value ? "bg-foreground text-background" : "bg-muted/70 text-muted-foreground hover:bg-muted hover:text-foreground"}`}
              onClick={() => onReportTypeChange(item.value)}
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>
      <ErrorNote message={error} />
      {rows.length && fields.length ? (
        <div className="overflow-x-auto rounded-xl bg-white/70">
          <table className="min-w-[760px] w-full border-collapse text-left text-[11px]">
            <thead className="border-b border-border/70 bg-white/80 text-muted-foreground">
              <tr>
                <th className="sticky left-0 z-10 bg-slate-50/95 px-3 py-2 font-medium">报告期</th>
                {fields.map((field) => <th key={field} className="whitespace-nowrap px-3 py-2 font-medium">{field}</th>)}
              </tr>
            </thead>
            <tbody className="divide-y divide-border/60">
              {rows.slice(0, 8).map((row, index) => (
                <tr key={`${row.rq ?? "row"}-${index}`} className="text-slate-700 transition-colors hover:bg-slate-50/80">
                  <td className="sticky left-0 bg-white/95 px-3 py-2 font-medium text-foreground">{formatDate(row.rq)}</td>
                  {fields.map((field) => <td key={field} className="whitespace-nowrap px-3 py-2 tabular-nums">{displayValue(row.fields[field])}</td>)}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : <EmptyState label={error ? "财务报表暂不可用" : "暂无该类财务报表"} />}
      {rows.length > 8 ? <div className="text-right text-[11px] text-muted-foreground">已展示最近 8 个报告期，共 {rows.length} 个</div> : null}
    </section>
  )
}

function BusinessSection({ data, error }: { data: StockBusinessCompositionResponse | null; error?: string }) {
  return (
    <section className="min-w-0 space-y-2 rounded-2xl bg-muted/50 p-3">
      <div className="flex items-center justify-between gap-3">
        <SectionHeading icon={BriefcaseBusiness} title="主营构成" detail={data?.reportDate ? `报告期 ${formatDate(data.reportDate)}` : "营业收入分项"} />
        <span className="text-[11px] text-muted-foreground">{data?.items?.length ?? 0} 项</span>
      </div>
      <ErrorNote message={error} />
      {data?.items?.length ? (
        <div className="overflow-x-auto rounded-xl bg-white/70">
          <table className="min-w-[620px] w-full text-left text-[11px]">
            <thead className="border-b border-border/70 bg-white/80 text-muted-foreground"><tr><th className="px-3 py-2 font-medium">分类</th><th className="px-3 py-2 font-medium">业务名称</th><th className="px-3 py-2 text-right font-medium">收入</th><th className="px-3 py-2 text-right font-medium">占比</th></tr></thead>
            <tbody className="divide-y divide-border/60">
              {data.items.slice(0, 12).map((item, index) => (
                <tr key={`${item.name ?? "item"}-${index}`} className="text-slate-700 transition-colors hover:bg-slate-50/80">
                  <td className="px-3 py-2">{item.category ?? item.subType ?? "—"}</td>
                  <td className="px-3 py-2 font-medium text-foreground">{item.name ?? "—"}</td>
                  <td className="px-3 py-2 text-right tabular-nums">{displayValue(item.revenue, 0)}</td>
                  <td className="px-3 py-2 text-right tabular-nums">{item.ratio === null ? "—" : `${displayValue(item.ratio)}%`}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : <EmptyState label={error ? "主营构成暂不可用" : "暂无主营构成数据"} />}
    </section>
  )
}

function ForecastSection({ data, error }: { data: StockProfitForecastResponse | null; error?: string }) {
  return (
    <section className="min-w-0 space-y-2 rounded-2xl bg-muted/50 p-3">
      <div className="flex items-center justify-between gap-3">
        <SectionHeading icon={FileChartColumn} title="业绩预测" detail="F10 预测数据" />
        <span className="text-[11px] text-muted-foreground">{data?.items?.length ?? 0} 期</span>
      </div>
      <ErrorNote message={error} />
      {data?.items?.length ? (
        <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-4">
          {data.items.slice(0, 4).map((item, index) => (
            <div key={`${item.year || "period"}-${index}`} className="rounded-xl bg-white/70 px-3 py-2.5">
              <div className="flex items-center justify-between gap-2"><span className="text-xs font-semibold text-foreground">{item.year || "—"}</span><span className="text-[10px] text-muted-foreground">预测</span></div>
              <dl className="mt-2 grid grid-cols-2 gap-x-3 gap-y-1 text-[11px]">
                <dt className="text-muted-foreground">净利润</dt><dd className="text-right tabular-nums">{displayValue(item.netProfit, 0)}</dd>
                <dt className="text-muted-foreground">EPS</dt><dd className="text-right tabular-nums">{displayValue(item.eps)}</dd>
                <dt className="text-muted-foreground">营收</dt><dd className="text-right tabular-nums">{displayValue(item.revenue, 0)}</dd>
              </dl>
            </div>
          ))}
        </div>
      ) : <EmptyState label={error ? "业绩预测暂不可用" : "暂无业绩预测数据"} />}
    </section>
  )
}

function BoardMembershipSection({ data, error }: { data: EasyTdxResponse<EasyTdxBelongBoardRow> | null; error?: string }) {
  // get_belong_board uses its own server-side category codes: 12=industry, 4=concept.
  const industries = (data?.items ?? []).filter((item) => item.board_type === 12 || item.board_type === 0 || item.board_type === 1)
  const concepts = (data?.items ?? []).filter((item) => item.board_type === 4)

  const renderGroup = (label: string, items: EasyTdxBelongBoardRow[], tone: string) => (
    <div className="min-w-0 rounded-xl bg-white/70 px-3 py-2.5">
      <div className="flex items-center justify-between gap-2">
        <span className="text-xs font-semibold text-foreground">{label}</span>
        <span className="text-[11px] text-muted-foreground">{items.length} 个</span>
      </div>
      {items.length ? (
        <div className="mt-2 flex flex-wrap gap-1.5">
          {items.map((item) => <span key={`${item.board_type}-${item.board_code}`} className={`rounded-md px-2 py-1 text-[11px] font-medium ${tone}`} title={item.board_code}>{item.board_name || item.board_code}</span>)}
        </div>
      ) : <p className="mt-2 text-[11px] text-muted-foreground">暂无数据</p>}
    </div>
  )

  return (
    <section className="min-w-0 space-y-2 rounded-2xl bg-muted/50 p-3">
      <div className="flex items-center justify-between gap-3">
        <SectionHeading icon={BriefcaseBusiness} title="行业 / 概念板块" detail="easy_tdx MAC 个股所属板块" />
        <span className="text-[11px] text-muted-foreground">{data?.items?.length ?? 0} 个</span>
      </div>
      <ErrorNote message={error} />
      {data || error ? (
        <div className="grid gap-2 sm:grid-cols-2">
          {renderGroup("行业", industries, "bg-amber-50 text-amber-700")}
          {renderGroup("概念", concepts, "bg-sky-50 text-sky-700")}
        </div>
      ) : <EmptyState label="暂无板块归属数据" />}
    </section>
  )
}

function EventList({ title, icon: Icon, items, error, renderItem }: {
  title: string
  icon: typeof Newspaper
  items: Array<{ key: string; date?: string | null; title?: string | null; detail?: string | null }>
  error?: string
  renderItem?: (item: { key: string; date?: string | null; title?: string | null; detail?: string | null }) => ReactNode
}) {
  return (
    <section className="min-w-0 space-y-2 rounded-2xl bg-muted/50 p-3">
      <div className="flex items-center justify-between gap-3"><SectionHeading icon={Icon} title={title} /><span className="text-[11px] text-muted-foreground">{items.length} 条</span></div>
      <ErrorNote message={error} />
      {items.length ? (
        <div className="divide-y divide-border/70 rounded-xl bg-white/70 px-3">
          {items.slice(0, 8).map((item) => renderItem ? renderItem(item) : (
            <div key={item.key} className="flex min-w-0 items-start gap-3 py-2.5">
              <span className="w-20 shrink-0 font-mono text-[10px] text-slate-500">{formatDate(item.date)}</span>
              <span className="min-w-0 flex-1 break-words text-xs text-slate-900">{item.title ?? "—"}</span>
              {item.detail ? <span className="max-w-28 shrink-0 truncate text-[10px] text-slate-500">{item.detail}</span> : null}
            </div>
          ))}
        </div>
      ) : <EmptyState label={error ? `${title}暂不可用` : `暂无${title}`} />}
    </section>
  )
}

function ProfileContent({ data, errors, view, reportType, onReportTypeChange }: {
  data: ProfileData
  errors: ProfileErrors
  view: ProfileView
  reportType: FinanceReportType
  onReportTypeChange: (value: FinanceReportType) => void
}) {
  const financeData = data.finance[reportType] ?? null
  if (view === "finance") return <FinanceReportSection data={financeData} error={errors[reportType]} reportType={reportType} onReportTypeChange={onReportTypeChange} />
  if (view === "operations") return <div className="space-y-4"><BusinessSection data={data.business} error={errors.business} /><ForecastSection data={data.forecast} error={errors.forecast} /></div>
  if (view === "events") return (
    <div className="grid gap-3 xl:grid-cols-2">
      <EventList title="公告" icon={Newspaper} error={errors.announcements} items={(data.announcements?.items ?? []).map((item, index) => ({ key: `${item.recId ?? "announcement"}-${index}`, date: item.issueDate, title: item.title, detail: item.typename }))} />
      <EventList title="公司新闻" icon={Newspaper} error={errors.news} items={(data.news?.items ?? []).map((item, index) => ({ key: `${item.recId ?? "news"}-${index}`, date: item.issueDate, title: item.title, detail: item.relatecolumn }))} />
      <EventList title="机构调研" icon={CalendarDays} error={errors.roadshows} items={(data.roadshows?.items ?? []).map((item, index) => ({ key: `${item.startDate ?? "roadshow"}-${index}`, date: item.startDate, title: item.title, detail: item.roadshowType }))} />
      <EventList title="公司研究" icon={FileChartColumn} error={errors.companyNews} items={(data.companyNews?.items ?? []).map((item, index) => ({ key: `${item.recId ?? "research"}-${index}`, date: item.issueDate, title: item.title, detail: item.rating }))} />
    </div>
  )
  return (
    <div className="space-y-3">
      <div className="grid gap-3 xl:grid-cols-[minmax(0,1.1fr)_minmax(300px,0.9fr)]">
        <ValuationSection data={data.valuation} error={errors.valuation} />
        <BoardMembershipSection data={data.belongBoard} error={errors.belongBoard} />
      </div>
      <div className="grid gap-3 xl:grid-cols-2">
        <BusinessSection data={data.business} error={errors.business} />
        <ForecastSection data={data.forecast} error={errors.forecast} />
      </div>
    </div>
  )
}

export function ProfileTab({ symbol, name }: { symbol: string; name?: string | null }) {
  const [view, setView] = useState<ProfileView>("overview")
  const [reportType, setReportType] = useState<FinanceReportType>("zcfzb")
  const [data, setData] = useState<ProfileData>(EMPTY_DATA)
  const [errors, setErrors] = useState<ProfileErrors>({})
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const requestIdRef = useRef(0)

  const load = useCallback(async (refresh = false) => {
    const requestId = ++requestIdRef.current
    setLoading(true)
    if (refresh) setRefreshing(true)
    setData(EMPTY_DATA)
    setErrors({})
    const jobs = [
      ["valuation", fetchStockValuation(symbol, { limit: 120 })],
      ["belongBoard", fetchEasyTdxBelongBoard(symbol)],
      ["finance:zcfzb", fetchStockFinanceReport(symbol, "zcfzb")],
      ["business", fetchStockBusinessComposition(symbol, { limit: 20 })],
      ["forecast", fetchStockProfitForecast(symbol)],
      ["announcements", fetchStockAnnouncements(symbol)],
      ["news", fetchStockNews(symbol)],
      ["roadshows", fetchStockRoadshows(symbol)],
      ["companyNews", fetchStockCompanyNews(symbol)],
    ] as const
    const results = await Promise.allSettled(jobs.map(([, promise]) => promise))
    if (requestId !== requestIdRef.current) return
    const nextData: ProfileData = { ...EMPTY_DATA, finance: {} }
    const nextErrors: ProfileErrors = {}
    results.forEach((result, index) => {
      const key = jobs[index][0]
      if (result.status === "fulfilled") {
        if (key === "finance:zcfzb") nextData.finance.zcfzb = result.value as StockFinanceReportResponse
        else nextData[key as Exclude<keyof ProfileData, "finance">] = result.value as never
      } else {
        const errorKey = key === "finance:zcfzb" ? "zcfzb" : key as keyof ProfileData
        nextErrors[errorKey] = errorMessage(result.reason)
      }
    })
    setData(nextData)
    setErrors(nextErrors)
    setLoading(false)
    setRefreshing(false)
  }, [symbol])

  useEffect(() => {
    setView("overview")
    setReportType("zcfzb")
    void load()
    return () => { requestIdRef.current += 1 }
  }, [load])

  const loadFinanceReport = useCallback(async (nextReportType: FinanceReportType) => {
    setReportType(nextReportType)
    if (data.finance[nextReportType] || errors[nextReportType]) return
    const requestId = ++requestIdRef.current
    try {
      const result = await fetchStockFinanceReport(symbol, nextReportType)
      if (requestId !== requestIdRef.current) return
      setData((current) => ({ ...current, finance: { ...current.finance, [nextReportType]: result } }))
    } catch (error) {
      if (requestId !== requestIdRef.current) return
      setErrors((current) => ({ ...current, [nextReportType]: errorMessage(error) }))
    }
  }, [data.finance, errors, symbol])

  const fetchedAt = data.valuation?.fetchedAt ?? data.finance.zcfzb?.fetchedAt ?? null
  const viewItems: Array<{ value: ProfileView; label: string }> = [
    { value: "overview", label: "Overview" },
    { value: "finance", label: "Finance" },
    { value: "operations", label: "Operations" },
    { value: "events", label: "Events" },
  ]

  return (
    <section className="min-w-0 space-y-3">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <span className="flex size-7 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-slate-600"><BriefcaseBusiness className="size-3.5" /></span>
            <h2 className="text-sm font-semibold text-foreground">Profile</h2>
            <Badge variant="outline" className="rounded-full bg-white px-2 py-0 text-[10px]">eltdx F10</Badge>
          </div>
          <p className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-[11px] text-muted-foreground">
            <span className="font-medium text-foreground">{name ?? symbol}</span>
            <span className="font-mono">{symbol}</span>
            <span>行业、概念、估值、财务和公司资讯</span>
            {fetchedAt ? <span>· 更新 {formatDate(fetchedAt)}</span> : null}
          </p>
        </div>
        <Button type="button" size="sm" variant="secondary" className="h-7 shrink-0 gap-1.5 px-2.5 text-[11px]" onClick={() => void load(true)} disabled={loading || refreshing}>
          <RefreshCw className={`size-3.5 ${refreshing ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </div>

      <div className="flex items-center gap-1 overflow-x-auto pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {viewItems.map((item) => (
          <button key={item.value} type="button" aria-pressed={view === item.value} className={`h-7 shrink-0 rounded-md px-2.5 text-[11px] font-medium outline-none transition focus-visible:ring-2 focus-visible:ring-slate-400/60 ${view === item.value ? "bg-foreground text-background" : "bg-muted/70 text-muted-foreground hover:bg-muted hover:text-foreground"}`} onClick={() => setView(item.value)}>{item.label}</button>
        ))}
      </div>

      {loading ? <ProfileSkeleton /> : <ProfileContent data={data} errors={errors} view={view} reportType={reportType} onReportTypeChange={(value) => void loadFinanceReport(value)} />}
    </section>
  )
}
