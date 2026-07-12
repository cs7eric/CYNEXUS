import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

const STOCK_INFORMATION_API_BASE = `${SERVICE_PREFIX.market}/stock-information`

export type StockInformationKind = "announcement" | "news" | "research" | "interaction" | "roadshow"

export interface StockInformationItem {
  id: string
  kind: StockInformationKind
  title: string
  occurredAt: string | null
  source: string | null
  detail: string | null
  content: string | null
  href: string | null
}

export interface StockInformationFeed {
  symbol: string
  items: StockInformationItem[]
  counts: Record<StockInformationKind, number>
}

export interface StockInformationContent {
  kind: StockInformationKind
  source: string
  itemId: string
  title: string | null
  sourceUrl: string | null
  resolvedUrl: string | null
  contentType: string | null
  status: "not_fetched" | "ready" | "needs_ocr" | "unsupported" | "failed"
  text: string | null
  contentHash: string | null
  message: string | null
  extractedAt: string | null
}

export interface StockInformationAnalysis {
  status: "content_required" | "not_requested" | "ready" | "failed"
  resultJson: string | null
  modelCode: string | null
  errorMessage: string | null
  analyzedAt: string | null
}

interface StockInformationApiFeed {
  symbol: string
  announcements?: Array<{ id?: string | null; title?: string | null; category?: string | null; publishedAt?: string | null; source?: string | null; url?: string | null }>
  news?: Array<{ id?: string | null; title?: string | null; summary?: string | null; mediaName?: string | null; publishedAt?: string | null; source?: string | null; url?: string | null }>
  research?: Array<{ id?: string | null; title?: string | null; organization?: string | null; author?: string | null; rating?: string | null; publishedAt?: string | null; source?: string | null; url?: string | null }>
  investorInteractions?: Array<{ id?: string | null; question?: string | null; answer?: string | null; answerer?: string | null; askedAt?: string | null; source?: string | null }>
  roadshows?: Array<{ title?: string | null; type?: string | null; startAt?: string | null; source?: string | null; url?: string | null }>
}

function text(value: string | null | undefined): string | null {
  const normalized = value?.trim()
  return normalized || null
}

function chronologicalValue(value: string | null): number {
  if (!value) return 0
  const timestamp = Date.parse(value)
  return Number.isFinite(timestamp) ? timestamp : 0
}

export async function fetchStockInformation(symbol: string, limit = 50): Promise<StockInformationFeed> {
  const query = new URLSearchParams({ symbol, limit: String(limit) })
  const response = await fetchWithRetry(`${STOCK_INFORMATION_API_BASE}?${query.toString()}`, { cache: "no-store" })
  const data = await cynexusResult<StockInformationApiFeed>(response)
  const items: StockInformationItem[] = [
    ...(data.announcements ?? []).flatMap((item, index) => {
      const title = text(item.title)
      const href = text(item.url)
      return title && href ? [{
        id: `announcement:${item.id ?? index}`,
        kind: "announcement" as const,
        title,
        occurredAt: text(item.publishedAt),
        source: text(item.source),
        detail: text(item.category),
        content: null,
        href,
      }] : []
    }),
    ...(data.news ?? []).flatMap((item, index) => {
      const title = text(item.title)
      const href = text(item.url)
      return title && href ? [{
        id: `news:${item.id ?? index}`,
        kind: "news" as const,
        title,
        occurredAt: text(item.publishedAt),
        source: text(item.source),
        detail: text(item.mediaName),
        content: text(item.summary),
        href,
      }] : []
    }),
    ...(data.research ?? []).flatMap((item, index) => {
      const title = text(item.title)
      const href = text(item.url)
      return title && href ? [{
        id: `research:${item.id ?? index}`,
        kind: "research" as const,
        title,
        occurredAt: text(item.publishedAt),
        source: text(item.source),
        detail: [text(item.rating), text(item.organization), text(item.author)].filter(Boolean).join(" · ") || null,
        content: null,
        href,
      }] : []
    }),
    ...(data.investorInteractions ?? []).flatMap((item, index) => {
      const question = text(item.question)
      const answer = text(item.answer)
      return question || answer ? [{
        id: `interaction:${item.id ?? index}`,
        kind: "interaction" as const,
        title: question ?? "Investor interaction",
        occurredAt: text(item.askedAt),
        source: text(item.source),
        detail: text(item.answerer),
        content: answer,
        href: null,
      }] : []
    }),
    ...(data.roadshows ?? []).flatMap((item, index) => {
      const title = text(item.title)
      const href = text(item.url)
      return title && href ? [{
        id: `roadshow:${href ?? index}`,
        kind: "roadshow" as const,
        title,
        occurredAt: text(item.startAt),
        source: text(item.source),
        detail: text(item.type),
        content: null,
        href,
      }] : []
    }),
  ]

  const deduped = Array.from(new Map(items.map((item) => [`${item.kind}:${item.title}:${item.occurredAt ?? ""}`, item])).values())
    .sort((left, right) => chronologicalValue(right.occurredAt) - chronologicalValue(left.occurredAt))

  return {
    symbol: data.symbol || symbol,
    items: deduped,
    counts: {
      announcement: deduped.filter((item) => item.kind === "announcement").length,
      news: deduped.filter((item) => item.kind === "news").length,
      research: deduped.filter((item) => item.kind === "research").length,
      interaction: deduped.filter((item) => item.kind === "interaction").length,
      roadshow: deduped.filter((item) => item.kind === "roadshow").length,
    },
  }
}

function informationItemQuery(symbol: string, item: Pick<StockInformationItem, "kind" | "source" | "id">): string {
  const query = new URLSearchParams({
    symbol,
    kind: item.kind,
    source: item.source || "",
    itemId: item.id.replace(`${item.kind}:`, ""),
  })
  return query.toString()
}

export async function fetchStockInformationContent(symbol: string, item: StockInformationItem): Promise<StockInformationContent> {
  const query = informationItemQuery(symbol, item)
  return cynexusResult<StockInformationContent>(
    await fetchWithRetry(`${STOCK_INFORMATION_API_BASE}/items/content/fetch?${query}`, { method: "POST" }),
  )
}

export async function fetchStockInformationAnalysis(symbol: string, item: StockInformationItem): Promise<StockInformationAnalysis> {
  const query = informationItemQuery(symbol, item)
  return cynexusResult<StockInformationAnalysis>(
    await fetchWithRetry(`${STOCK_INFORMATION_API_BASE}/items/analysis?${query}`, { cache: "no-store" }),
  )
}

export async function runStockInformationAnalysis(symbol: string, item: StockInformationItem): Promise<StockInformationAnalysis> {
  const query = informationItemQuery(symbol, item)
  return cynexusResult<StockInformationAnalysis>(
    await fetchWithRetry(`${STOCK_INFORMATION_API_BASE}/items/analysis/run?${query}`, { method: "POST" }),
  )
}
