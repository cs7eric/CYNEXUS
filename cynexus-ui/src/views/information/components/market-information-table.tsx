import { Fragment, useEffect, useRef } from "react"
import { Database, ExternalLink } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { cn } from "@/lib/utils"
import type { MarketInformationNews, MarketInformationNewsPage } from "@/services/information"

interface MarketInformationTableProps {
  items: MarketInformationNews[]
  pagination?: MarketInformationNewsPage
  loading?: boolean
  onPageChange: (page: number) => void
  onSelectEvent: (clusterId: number) => void
}

function formatPublishedAt(value?: string | null) {
  if (!value) return "-"
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString([], { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" })
}

function sourceLabel(value?: string | null) {
  const labels: Record<string, string> = {
    "cls.telegraph": "CLS",
    "eastmoney.7x24": "EastMoney",
    "gov.cn.policy": "State Council",
    "pboc.news": "PBoC",
    "stats.gov.cn.release": "NBS",
    "mof.policy": "MOF",
    "ndrc.news": "NDRC",
    "csrc.news": "CSRC",
    "szse.disclosure": "SZSE",
    "xinhua.finance": "Xinhua",
    "cctv.finance": "CCTV Finance",
    "caam.industry": "CAAM",
    "caam.policy": "CAAM",
    "chinania.news": "China Nonferrous",
    "chinania.stats": "China Nonferrous",
    "chinania.policy": "China Nonferrous",
    "cninfo.market": "CNInfo",
    "cninfo.regulation": "CNInfo",
  }
  return value ? labels[value] ?? value : "Unknown"
}

function categoryClass(category?: string | null) {
  if (category === "Macro") return "bg-sky-500/10 text-sky-700 dark:text-sky-300"
  if (category === "Policy") return "bg-amber-500/10 text-amber-700 dark:text-amber-300"
  if (category === "Regulation") return "bg-rose-500/10 text-rose-700 dark:text-rose-300"
  if (category === "Industry") return "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300"
  return "bg-muted text-foreground"
}

function impactDotClass(status?: string | null) {
  if (status === "Direct") return "bg-emerald-500"
  if (status === "Inferred") return "bg-amber-500"
  return "bg-muted-foreground/45"
}

function analysisStatusClass(status?: string | null) {
  if (status === "SUCCEEDED") return "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300"
  if (status === "FAILED") return "bg-rose-500/10 text-rose-700 dark:text-rose-300"
  if (status === "PENDING" || status === "RUNNING") return "bg-amber-500/10 text-amber-700 dark:text-amber-300"
  return "bg-muted text-muted-foreground"
}

function pageItems(page: number, totalPages: number) {
  return [...new Set([1, totalPages, page - 1, page, page + 1].filter((item) => item >= 1 && item <= totalPages))]
    .sort((left, right) => left - right)
}

export function MarketInformationTable({ items, pagination, loading = false, onPageChange, onSelectEvent }: MarketInformationTableProps) {
  const sectionRef = useRef<HTMLElement>(null)
  const previousPageRef = useRef(pagination?.page)
  const pages = pageItems(pagination?.page ?? 1, pagination?.totalPages ?? 1)
  const start = pagination?.total ? (pagination.page - 1) * pagination.pageSize + 1 : 0
  const end = pagination ? Math.min(pagination.page * pagination.pageSize, pagination.total) : items.length

  useEffect(() => {
    if (previousPageRef.current !== undefined && previousPageRef.current !== pagination?.page) {
      sectionRef.current?.scrollIntoView({ block: "start" })
    }
    previousPageRef.current = pagination?.page
  }, [pagination?.page])

  return (
    <section ref={sectionRef} className="scroll-mt-20 overflow-hidden rounded-xl bg-card ring-1 ring-border/50" aria-busy={loading}>
      <div className="flex items-center justify-between gap-4 border-b border-border/50 px-4 py-3">
        <div className="flex items-center gap-2">
          <Database className="size-4 text-primary" />
          <h2 className="text-sm font-semibold">Information feed</h2>
        </div>
        <span className="text-xs tabular-nums text-muted-foreground">{pagination?.total ?? items.length} records</span>
      </div>

      <Table className="table-fixed">
        <colgroup>
          <col className="w-[34%]" />
          <col className="w-28" />
          <col className="w-20" />
          <col className="w-32" />
          <col className="w-28" />
          <col className="w-24" />
          <col className="w-12" />
        </colgroup>
        <TableHeader className="bg-muted/35">
          <TableRow className="hover:bg-transparent">
            <TableHead className="w-[34%] px-4">Information</TableHead>
            <TableHead className="w-28">Published</TableHead>
            <TableHead className="w-20">Type</TableHead>
            <TableHead className="w-32">Impact</TableHead>
            <TableHead className="w-28">AI status</TableHead>
            <TableHead className="w-24">Source</TableHead>
            <TableHead className="w-12"><span className="sr-only">Open</span></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody className={cn(loading && "opacity-60")}>
          {items.map((item) => (
            <TableRow key={`${item.source}-${item.id}`} className={cn(item.clusterId && "cursor-pointer")} onClick={() => item.clusterId && onSelectEvent(item.clusterId)}>
              <TableCell className="max-w-0 whitespace-normal px-4 align-top">
                <div className="font-medium leading-5 text-foreground">{item.title}</div>
                {item.summary ? <p className="mt-1 line-clamp-2 text-xs leading-5 text-muted-foreground">{item.summary}</p> : null}
              </TableCell>
              <TableCell className="align-top text-xs tabular-nums text-muted-foreground">{formatPublishedAt(item.publishedAt)}</TableCell>
              <TableCell className="align-top">
                <Badge variant="secondary" className={cn("border-0", categoryClass(item.category))}>{item.category || "Market Wire"}</Badge>
              </TableCell>
              <TableCell className="max-w-0 whitespace-normal align-top">
                {(item.impactIndustries ?? []).length ? <div className="min-w-0"><div className="flex min-w-0 items-center gap-1.5"><span className={cn("size-1.5 shrink-0 rounded-full", impactDotClass(item.impactStatus))} /><span className="truncate text-xs font-medium" title={(item.impactIndustries ?? []).map((industry) => industry.tdxName).join(", ")}>{(item.impactIndustries ?? [])[0]?.tdxName}{(item.impactIndustries ?? []).length > 1 ? ` +${(item.impactIndustries ?? []).length - 1}` : ""}</span></div><div className="mt-0.5 pl-3 text-[10px] text-muted-foreground">{item.impactStatus ?? "Unmapped"} mapping</div></div> : <div className="flex items-center gap-1.5 text-xs text-muted-foreground"><span className={cn("size-1.5 shrink-0 rounded-full", impactDotClass(item.impactStatus))} />No map</div>}
              </TableCell>
              <TableCell className="max-w-0 whitespace-normal align-top">
                <div className="flex min-w-0 items-center gap-2">
                  <Badge variant="secondary" className={cn("border-0 capitalize", analysisStatusClass(item.analysisStatus))}>{(item.analysisStatus || "Not requested").replaceAll("_", " ").toLowerCase()}</Badge>
                  {item.marketDirection && item.marketDirection !== "unknown" || item.materiality && item.materiality !== "unknown" ? <span className="truncate text-[11px] capitalize text-muted-foreground">{[item.marketDirection !== "unknown" ? item.marketDirection : null, item.materiality !== "unknown" ? item.materiality : null].filter(Boolean).join(" · ")}</span> : null}
                </div>
              </TableCell>
              <TableCell className="max-w-0 whitespace-normal align-top">
                <div className="flex flex-wrap gap-1">
                  <div className="text-xs font-medium">{sourceLabel(item.source)}</div>
                  <div className="w-full truncate text-[11px] text-muted-foreground" title={item.source ?? undefined}>{item.source}</div>
                </div>
              </TableCell>
              <TableCell className="align-top">
                {item.url ? (
                  <Button variant="ghost" size="icon" className="size-8" asChild>
                    <a href={item.url} target="_blank" rel="noreferrer" aria-label="Open source" onClick={(event) => event.stopPropagation()}><ExternalLink className="size-3.5" /></a>
                  </Button>
                ) : <span className="px-2 text-muted-foreground">-</span>}
              </TableCell>
            </TableRow>
          ))}
          {!items.length ? (
            <TableRow><TableCell colSpan={7} className="h-36 text-center text-sm text-muted-foreground">No market information available.</TableCell></TableRow>
          ) : null}
        </TableBody>
      </Table>

      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border/50 px-4 py-3">
        <span className="text-xs tabular-nums text-muted-foreground">Showing {start}-{end} of {pagination?.total ?? items.length}</span>
        {pagination && pagination.totalPages > 1 ? (
          <Pagination className="mx-0 w-auto justify-end">
            <PaginationContent>
              <PaginationItem><PaginationPrevious className="[&>span]:hidden" disabled={loading || !pagination.hasPrevious} onClick={() => onPageChange(Math.max(1, pagination.page - 1))} /></PaginationItem>
              {pages.map((item, index) => {
                const previous = pages[index - 1]
                return (
                  <Fragment key={item}>
                    {previous != null && item - previous > 1 ? <PaginationItem><PaginationEllipsis /></PaginationItem> : null}
                    <PaginationItem><PaginationLink disabled={loading} isActive={item === pagination.page} onClick={() => onPageChange(item)}>{item}</PaginationLink></PaginationItem>
                  </Fragment>
                )
              })}
              <PaginationItem><PaginationNext className="[&>span]:hidden" disabled={loading || !pagination.hasNext} onClick={() => onPageChange(Math.min(pagination.totalPages, pagination.page + 1))} /></PaginationItem>
            </PaginationContent>
          </Pagination>
        ) : null}
      </div>
    </section>
  )
}
