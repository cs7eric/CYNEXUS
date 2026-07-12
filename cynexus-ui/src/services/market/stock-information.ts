import {
  fetchStockAnnouncements,
  fetchStockCompanyNews,
  fetchStockNews,
  fetchStockRoadshows,
  type StockAnnouncementItem,
  type StockCompanyNewsItem,
  type StockNewsItem,
  type StockRoadshowItem,
} from "@/services/market/f10"

export type StockInformationKind = "announcement" | "news" | "research" | "roadshow"

export interface StockInformationItem {
  id: string
  kind: StockInformationKind
  title: string
  occurredAt: string | null
  source: string | null
  detail: string | null
  href: string | null
}

export interface StockInformationFeed {
  symbol: string
  items: StockInformationItem[]
  counts: Record<StockInformationKind, number>
  sources: string[]
  failures: Partial<Record<StockInformationKind, string>>
}

function text(value: string | null | undefined): string | null {
  const normalized = value?.trim()
  return normalized || null
}

function announcementItem(item: StockAnnouncementItem, index: number): StockInformationItem | null {
  const title = text(item.title)
  if (!title) return null
  return {
    id: `announcement:${item.recId ?? item.url ?? index}`,
    kind: "announcement",
    title,
    occurredAt: text(item.issueDate) ?? text(item.redistime),
    source: text(item.source),
    detail: text(item.typename),
    href: text(item.url),
  }
}

function newsItem(item: StockNewsItem, index: number): StockInformationItem | null {
  const title = text(item.title)
  if (!title) return null
  return {
    id: `news:${item.recId ?? index}`,
    kind: "news",
    title,
    occurredAt: text(item.issueDate) ?? text(item.redistime),
    source: text(item.source),
    detail: text(item.relatecolumn),
    href: null,
  }
}

function researchItem(item: StockCompanyNewsItem, index: number): StockInformationItem | null {
  const title = text(item.title)
  if (!title) return null
  return {
    id: `research:${item.recId ?? item.docHash ?? index}`,
    kind: "research",
    title,
    occurredAt: text(item.issueDate),
    source: "eltdx",
    detail: [text(item.rating), text(item.analysts)].filter(Boolean).join(" · ") || null,
    href: null,
  }
}

function roadshowItem(item: StockRoadshowItem, index: number): StockInformationItem | null {
  const title = text(item.title)
  if (!title) return null
  return {
    id: `roadshow:${item.url ?? item.startDate ?? index}`,
    kind: "roadshow",
    title,
    occurredAt: text(item.startDate),
    source: "eltdx",
    detail: [text(item.roadshowType), text(item.startTime), text(item.endTime)].filter(Boolean).join(" · ") || text(item.summary),
    href: text(item.url),
  }
}

function chronologicalValue(value: string | null): number {
  if (!value) return 0
  const timestamp = Date.parse(value.replace(/\./g, "-"))
  return Number.isFinite(timestamp) ? timestamp : 0
}

export async function fetchStockInformation(symbol: string): Promise<StockInformationFeed> {
  const results = await Promise.allSettled([
    fetchStockAnnouncements(symbol),
    fetchStockNews(symbol),
    fetchStockCompanyNews(symbol, { section: "gsyj" }),
    fetchStockRoadshows(symbol),
  ])
  const [announcements, news, research, roadshows] = results
  const failures: Partial<Record<StockInformationKind, string>> = {}
  const sources = new Set<string>()
  const items: StockInformationItem[] = []

  if (announcements.status === "fulfilled") {
    if (announcements.value.source) sources.add(announcements.value.source)
    items.push(...announcements.value.items.map(announcementItem).filter((item): item is StockInformationItem => Boolean(item)))
  } else {
    failures.announcement = announcements.reason instanceof Error ? announcements.reason.message : "Announcement source unavailable"
  }

  if (news.status === "fulfilled") {
    if (news.value.source) sources.add(news.value.source)
    items.push(...news.value.items.map(newsItem).filter((item): item is StockInformationItem => Boolean(item)))
  } else {
    failures.news = news.reason instanceof Error ? news.reason.message : "News source unavailable"
  }

  if (research.status === "fulfilled") {
    if (research.value.source) sources.add(research.value.source)
    items.push(...research.value.items.map(researchItem).filter((item): item is StockInformationItem => Boolean(item)))
  } else {
    failures.research = research.reason instanceof Error ? research.reason.message : "Research source unavailable"
  }

  if (roadshows.status === "fulfilled") {
    if (roadshows.value.source) sources.add(roadshows.value.source)
    items.push(...roadshows.value.items.map(roadshowItem).filter((item): item is StockInformationItem => Boolean(item)))
  } else {
    failures.roadshow = roadshows.reason instanceof Error ? roadshows.reason.message : "Roadshow source unavailable"
  }

  const deduped = Array.from(new Map(items.map((item) => [`${item.kind}:${item.title}:${item.occurredAt ?? ""}`, item])).values())
    .sort((left, right) => chronologicalValue(right.occurredAt) - chronologicalValue(left.occurredAt))

  return {
    symbol,
    items: deduped,
    counts: {
      announcement: deduped.filter((item) => item.kind === "announcement").length,
      news: deduped.filter((item) => item.kind === "news").length,
      research: deduped.filter((item) => item.kind === "research").length,
      roadshow: deduped.filter((item) => item.kind === "roadshow").length,
    },
    sources: Array.from(sources),
    failures,
  }
}
