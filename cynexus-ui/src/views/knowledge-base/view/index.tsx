import { useCallback, useEffect, useMemo, useState } from "react"
import { Link, useSearchParams } from "react-router-dom"
import { PenLine, Search } from "lucide-react"

import DogLoader from "@/components/loader/dog-loader"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { notification } from "@/components/ui/notification"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { cn } from "@/lib/utils"
import {
  type KnowledgeCategory,
  type KnowledgeDocument,
  type KnowledgeDocumentStatus,
  type KnowledgeTag,
  fetchKnowledgeCategories,
  fetchKnowledgeDocuments,
  fetchKnowledgeTags,
} from "@/services/knowledge-base/documents"
import { KnowledgeBaseMarkdownViewer } from "../components/knowledge-base-markdown-viewer"
import { estimateReadingMinutes, formatKnowledgeDate } from "../lib/format"

function DocumentListSkeleton() {
  return (
    <div className="space-y-2 p-2">
      {Array.from({ length: 7 }).map((_, index) => (
        <div key={index} className="rounded-md px-3 py-2">
          <div className="flex items-center justify-between gap-3">
            <Skeleton className="h-4 w-48 bg-foreground/10" />
            <Skeleton className="h-5 w-16 rounded-full bg-foreground/10" />
          </div>
          <Skeleton className="mt-2 h-3 w-4/5 bg-foreground/10" />
          <Skeleton className="mt-1.5 h-3 w-2/3 bg-foreground/10" />
        </div>
      ))}
    </div>
  )
}

export default function KnowledgeBaseViewPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedDocumentId = searchParams.get("id") || ""
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([])
  const [tags, setTags] = useState<KnowledgeTag[]>([])
  const [categories, setCategories] = useState<KnowledgeCategory[]>([])
  const [keyword, setKeyword] = useState("")
  const [status, setStatus] = useState<KnowledgeDocumentStatus | "all">(requestedDocumentId ? "all" : "published")
  const [tagId, setTagId] = useState("all")
  const [categoryId, setCategoryId] = useState("all")
  const [activeId, setActiveId] = useState("")
  const [loading, setLoading] = useState(true)
  const selectItemClassName = "min-h-7 py-1 text-xs hover:bg-white focus:bg-white focus:text-foreground data-[highlighted]:bg-white"
  const statusBadgeClassName = (nextStatus: KnowledgeDocumentStatus) =>
    nextStatus === "published"
      ? "bg-emerald-50 text-emerald-700 hover:bg-emerald-50"
      : "bg-amber-50 text-amber-700 hover:bg-amber-50"

  const reload = useCallback(async () => {
    setLoading(true)
    try {
      const [nextDocuments, nextTags, nextCategories] = await Promise.all([
        fetchKnowledgeDocuments({
          keyword,
          status,
          tag_id: tagId === "all" ? undefined : tagId,
          category_id: categoryId === "all" ? undefined : categoryId,
        }),
        fetchKnowledgeTags(),
        fetchKnowledgeCategories(),
      ])
      setDocuments(nextDocuments)
      setTags(nextTags)
      setCategories(nextCategories)
      setActiveId((current) => {
        if (requestedDocumentId && nextDocuments.some((item) => item.id === requestedDocumentId)) return requestedDocumentId
        return nextDocuments.some((item) => item.id === current) ? current : nextDocuments[0]?.id || ""
      })
    } catch (err) {
      notification.danger({ title: "Knowledge base 加载失败", description: err instanceof Error ? err.message : "加载失败" })
    } finally {
      setLoading(false)
    }
  }, [categoryId, keyword, requestedDocumentId, status, tagId])

  useEffect(() => {
    const timer = window.setTimeout(() => void reload(), 180)
    return () => window.clearTimeout(timer)
  }, [reload])

  const activeDocument = useMemo(() => documents.find((item) => item.id === activeId), [activeId, documents])

  return (
    <WorkspaceShell sectionLabel="Knowledge Base" sectionUrl="/knowledge-base/view" pageTitle="Knowledge Base Content List" contentScrollable={false}>
      <div className="flex h-full min-h-0 w-full flex-col gap-6">
        <section className="grid min-h-0 flex-1 gap-4 xl:grid-cols-[420px_minmax(0,1fr)]">
          <aside className="flex min-h-0 flex-col overflow-hidden rounded-lg bg-muted/35">
            <div className="space-y-2 border-b p-3">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input className="pl-9" value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="Search documents" />
              </div>
              <div className="flex items-center justify-between gap-2">
                <Select value={status} onValueChange={(value) => setStatus(value as KnowledgeDocumentStatus | "all")}>
                  <SelectTrigger className="h-8 border-0 bg-muted/45 px-2.5 py-1 text-xs shadow-none"><SelectValue /></SelectTrigger>
                  <SelectContent className="border-0 bg-muted/95 shadow-none">
                    <SelectItem className={selectItemClassName} value="all">All</SelectItem>
                    <SelectItem className={selectItemClassName} value="published">Published</SelectItem>
                    <SelectItem className={selectItemClassName} value="draft">Draft</SelectItem>
                    <SelectItem className={selectItemClassName} value="archived">Archived</SelectItem>
                  </SelectContent>
                </Select>
                <div className="flex items-center gap-2">
                  <Select value={tagId} onValueChange={setTagId}>
                    <SelectTrigger className="h-8 border-0 bg-muted/45 px-2.5 py-1 text-xs shadow-none"><SelectValue placeholder="Tag" /></SelectTrigger>
                    <SelectContent className="border-0 bg-muted/95 shadow-none">
                      <SelectItem className={selectItemClassName} value="all">All tags</SelectItem>
                      {tags.map((tag) => <SelectItem className={selectItemClassName} key={tag.id} value={tag.id}>{tag.tag_name}</SelectItem>)}
                    </SelectContent>
                  </Select>
                  <Select value={categoryId} onValueChange={setCategoryId}>
                    <SelectTrigger className="h-8 border-0 bg-muted/45 px-2.5 py-1 text-xs shadow-none"><SelectValue placeholder="Category" /></SelectTrigger>
                    <SelectContent className="border-0 bg-muted/95 shadow-none">
                      <SelectItem className={selectItemClassName} value="all">All categories</SelectItem>
                      {categories.map((category) => <SelectItem className={selectItemClassName} key={category.id} value={category.id}>{category.category_name}</SelectItem>)}
                    </SelectContent>
                  </Select>
                </div>
              </div>
            </div>
            <div className="flex-1 overflow-auto">
              {loading ? (
                <DocumentListSkeleton />
              ) : (
                <div className="p-2">
                  {documents.map((document) => (
                    <button
                      key={document.id}
                      type="button"
                      className={cn(
                        "w-full rounded-md px-3 py-2 text-left transition hover:bg-background",
                        activeId === document.id && "bg-white",
                      )}
                      onClick={() => {
                        setActiveId(document.id)
                        setSearchParams({ id: document.id }, { replace: true })
                      }}
                    >
                      <div className="flex items-center justify-between gap-3">
                        <div className="truncate text-sm font-medium">{document.title}</div>
                        <Badge variant="secondary" className={statusBadgeClassName(document.status)}>{document.status}</Badge>
                      </div>
                      <div className="mt-1 truncate text-xs leading-5 text-muted-foreground">{document.description || "No description"}</div>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </aside>

          <article className="flex min-h-0 min-w-0 flex-col overflow-hidden rounded-lg bg-muted/35 p-5">
            {loading ? (
              <div className="flex min-h-0 flex-1 items-center justify-center rounded-lg bg-background">
                <DogLoader size={45} label="Loading knowledge content..." />
              </div>
            ) : activeDocument ? (
              <div className="flex min-h-0 min-w-0 flex-1 flex-col">
                <div className="mb-5 flex shrink-0 flex-col gap-3 border-b pb-5 lg:flex-row lg:items-start lg:justify-between">
                  <div>
                    <h2 className="text-2xl font-semibold tracking-tight">{activeDocument.title}</h2>
                    <p className="mt-2 text-sm leading-6 text-muted-foreground">{activeDocument.description}</p>
                    <div className="mt-3 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                      <span>{formatKnowledgeDate(activeDocument.updated_at)}</span>
                      <span>{estimateReadingMinutes(activeDocument.content)} min read</span>
                      {activeDocument.categories.length ? (
                        <span className="inline-flex min-h-7 items-center gap-1.5 rounded-full bg-background/70 px-2.5">
                          <span className="text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">Categories</span>
                          {activeDocument.categories.map((category) => (
                            <Badge key={category.id} variant="outline" className="h-5 rounded-full px-2 text-[11px] font-medium">
                              {category.category_name}
                            </Badge>
                          ))}
                        </span>
                      ) : null}
                      {activeDocument.tags.length ? (
                        <span className="inline-flex min-h-7 items-center gap-1.5 rounded-full bg-background/70 px-2.5">
                          <span className="text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">Tags</span>
                          {activeDocument.tags.map((tag) => (
                            <Badge key={tag.id} variant="secondary" className="h-5 rounded-full px-2 text-[11px] font-medium">
                              {tag.tag_name}
                            </Badge>
                          ))}
                        </span>
                      ) : null}
                    </div>
                  </div>
                  <div className="flex shrink-0 gap-2">
                    <Button asChild variant="secondary" size="sm">
                      <Link to={`/knowledge-base/writing?id=${encodeURIComponent(activeDocument.id)}`}>
                        <PenLine className="size-4" />
                        Writing
                      </Link>
                    </Button>
                  </div>
                </div>
                <div className="min-h-0 min-w-0 flex-1 overflow-auto rounded-lg bg-background">
                  <KnowledgeBaseMarkdownViewer key={activeDocument.id} markdown={activeDocument.content || ""} />
                </div>
              </div>
            ) : (
              <div className="flex min-h-0 flex-1 items-center justify-center text-sm text-muted-foreground">No documents</div>
            )}
          </article>
        </section>
      </div>
    </WorkspaceShell>
  )
}
