import { useCallback, useEffect, useMemo, useState } from "react"
import { Link, useSearchParams } from "react-router-dom"
import { ArrowLeft, BookOpenText, Check, Loader2, Pencil, Plus, Save, Search, Sparkles } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { notification } from "@/components/ui/notification"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { AiBindingProviderSelect } from "@/components/ai/ai-binding-provider-select"
import {
  type KnowledgeCategory,
  type KnowledgeDocumentStatus,
  type KnowledgeTag,
  createKnowledgeCategory,
  createKnowledgeDocument,
  createKnowledgeTag,
  fetchKnowledgeCategories,
  fetchKnowledgeDocument,
  fetchKnowledgeTags,
  updateKnowledgeDocument,
} from "@/services/knowledge-base/documents"
import { generateKnowledgeBaseDraft } from "@/services/knowledge-base/ai-generate"
import { KnowledgeBaseMdxEditor } from "./components/knowledge-base-mdx-editor"

type MetadataField = "categories" | "tags" | null
type HeaderField = "title" | "description" | null

interface MetadataMultiSelectProps {
  active: boolean
  label: string
  options: string[]
  values: string[]
  onActivate: () => void
  onDeactivate: () => void
  onChange: (values: string[]) => void
}

function MetadataMultiSelect({ active, label, options, values, onActivate, onDeactivate, onChange }: MetadataMultiSelectProps) {
  const [keyword, setKeyword] = useState("")
  const normalizedKeyword = keyword.trim().toLowerCase()
  const mergedOptions = Array.from(new Set([...options, ...values])).filter(Boolean)
  const filteredOptions = mergedOptions
    .filter((option) => option.toLowerCase().includes(normalizedKeyword))
    .sort((left, right) => {
      const leftSelected = values.includes(left)
      const rightSelected = values.includes(right)
      if (leftSelected !== rightSelected) return leftSelected ? -1 : 1

      const leftStartsWith = normalizedKeyword ? left.toLowerCase().startsWith(normalizedKeyword) : false
      const rightStartsWith = normalizedKeyword ? right.toLowerCase().startsWith(normalizedKeyword) : false
      if (leftStartsWith !== rightStartsWith) return leftStartsWith ? -1 : 1

      return left.localeCompare(right, "en", { sensitivity: "base" })
    })
  const canCreate = keyword.trim() && !mergedOptions.some((option) => option.toLowerCase() === normalizedKeyword)

  const toggleValue = (value: string, checked: boolean) => {
    onChange(checked ? Array.from(new Set([...values, value])) : values.filter((item) => item !== value))
  }

  const createValue = () => {
    const value = keyword.trim()
    if (!value) return
    onChange(Array.from(new Set([...values, value])))
    setKeyword("")
  }

  const selectKeyword = () => {
    const value = keyword.trim()
    if (!value) return

    const matchedOption = mergedOptions.find((option) => option.toLowerCase() === normalizedKeyword)
    if (matchedOption) {
      if (!values.includes(matchedOption)) {
        onChange(Array.from(new Set([...values, matchedOption])))
      }
      setKeyword("")
      return
    }

    createValue()
  }

  return (
    <div
      className="group rounded-lg bg-muted/40 px-4 py-2 transition hover:bg-muted/55"
      role="button"
      tabIndex={0}
      onClick={onActivate}
      onKeyDown={(event) => {
        if (event.key === "Enter") onActivate()
      }}
      onBlur={(event) => {
        const nextFocused = event.relatedTarget
        if (active && (!nextFocused || !event.currentTarget.contains(nextFocused as Node))) {
          onDeactivate()
        }
      }}
    >
      <div className="flex min-h-7 items-center justify-between gap-3">
        <div className="flex min-w-0 flex-wrap items-center gap-x-3 gap-y-2">
          <span className="text-xs font-semibold uppercase tracking-wide text-foreground/80">{label}</span>
          {!active ? (
            <div className="flex min-w-0 flex-wrap items-center gap-2">
              {values.length ? (
                values.map((value) => (
                  <Badge key={value} variant="secondary" className="px-3">
                    {value}
                  </Badge>
                ))
              ) : (
                <span className="text-sm text-muted-foreground">No {label.toLowerCase()}</span>
              )}
            </div>
          ) : null}
        </div>
        {!active ? <Pencil className="size-3.5 text-muted-foreground opacity-0 transition group-hover:opacity-100" /> : null}
      </div>

      {active ? (
        <div
          className="mt-3 flex min-h-12 flex-nowrap items-center gap-1.5 overflow-x-auto overflow-y-hidden rounded-md bg-background p-2 transition"
          onClick={(event) => event.stopPropagation()}
        >
          <div className="flex h-7 w-40 shrink-0 items-center gap-2 rounded-md bg-muted/35 px-2 text-muted-foreground">
            <Search className="size-3.5 shrink-0" />
            <input
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault()
                  selectKeyword()
                }
              }}
              className="h-7 min-w-0 flex-1 bg-transparent text-xs text-foreground outline-none placeholder:text-muted-foreground"
              placeholder={`Search ${label.toLowerCase()}`}
              autoFocus
            />
          </div>
          {filteredOptions.length ? (
            filteredOptions.map((option) => {
              const selected = values.includes(option)

              return (
                <button
                  key={option}
                  type="button"
                  className={[
                    "inline-flex h-7 shrink-0 items-center gap-1.5 rounded-full border px-3 text-[11px] font-medium transition",
                    selected
                      ? "border-neutral-400 bg-neutral-100 text-neutral-900 shadow-[inset_0_0_0_1px_rgba(24,24,27,0.08)]"
                      : "border-transparent bg-muted/55 text-muted-foreground hover:bg-muted hover:text-foreground",
                  ].join(" ")}
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => toggleValue(option, !selected)}
                >
                  {selected ? <Check className="size-3.5" /> : null}
                  <span>{option}</span>
                </button>
              )
            })
          ) : (
            <span className="px-1.5 text-xs text-muted-foreground">No matched {label.toLowerCase()}</span>
          )}
          {canCreate ? (
            <button
              type="button"
              className="inline-flex h-7 shrink-0 items-center gap-1.5 rounded-full border border-dashed bg-background px-3 text-[11px] font-medium text-muted-foreground transition hover:bg-muted hover:text-foreground"
              onMouseDown={(event) => event.preventDefault()}
              onClick={createValue}
            >
              <Plus className="size-3" />
              Add "{keyword.trim()}"
            </button>
          ) : null}
        </div>
      ) : null}
    </div>
  )
}

function normalizeName(value: string) {
  return value.trim().toLowerCase()
}

function stableHash(value: string) {
  let hash = 0
  for (let index = 0; index < value.length; index += 1) {
    hash = (hash * 31 + value.charCodeAt(index)) >>> 0
  }
  return hash.toString(36)
}

function toOptionCode(value: string, prefix: string) {
  const code = value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "")

  return code || `${prefix}_${stableHash(value)}`
}

export default function KnowledgeBaseWritingPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const documentId = searchParams.get("id") || ""
  const [markdown, setMarkdown] = useState("")
  const [title, setTitle] = useState("")
  const [description, setDescription] = useState("")
  const [categories, setCategories] = useState<string[]>([])
  const [tags, setTags] = useState<string[]>([])
  const [editingField, setEditingField] = useState<MetadataField>(null)
  const [editingHeaderField, setEditingHeaderField] = useState<HeaderField>(null)
  const [documentStatus, setDocumentStatus] = useState<KnowledgeDocumentStatus>("draft")
  const [availableTags, setAvailableTags] = useState<KnowledgeTag[]>([])
  const [availableCategories, setAvailableCategories] = useState<KnowledgeCategory[]>([])
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [savedAt, setSavedAt] = useState<string | null>(null)
  const [editorRevision, setEditorRevision] = useState(0)
  const [aiGenerateOpen, setAiGenerateOpen] = useState(false)
  const [aiGenerateText, setAiGenerateText] = useState("")
  const [aiGenerating, setAiGenerating] = useState(false)
  const editorKey = documentId ? `document-${documentId}-${editorRevision}` : `new-document-${editorRevision}`
  const categoryOptions = useMemo(
    () => Array.from(new Set([...availableCategories.map((category) => category.category_name), ...categories])).filter(Boolean),
    [availableCategories, categories],
  )
  const tagOptions = useMemo(
    () => Array.from(new Set([...availableTags.map((tag) => tag.tag_name), ...tags])).filter(Boolean),
    [availableTags, tags],
  )
  const documentStats = useMemo(() => {
    const plainText = markdown
      .replace(/```[\s\S]*?```/g, "")
      .replace(/`[^`]*`/g, "")
      .replace(/!\[[^\]]*]\([^)]*\)/g, "")
      .replace(/\[[^\]]*]\([^)]*\)/g, "")
      .replace(/[#>*_~|`-]/g, " ")
      .trim()
    const characters = plainText.replace(/\s/g, "").length
    const readingUnits = plainText.match(/[\u4e00-\u9fff]|[A-Za-z0-9]+/g)?.length ?? 0
    const minutes = Math.max(1, Math.ceil(readingUnits / 360))
    const headings = markdown.match(/^#{1,6}\s+\S+/gm)?.length ?? 0
    const codeBlocks = Math.floor((markdown.match(/```/g)?.length ?? 0) / 2)
    const tables = markdown.match(/^\s*\|?[\s:-]+\|[\s|:-]+\|?\s*$/gm)?.length ?? 0

    return [
      { label: "字数", value: characters.toLocaleString() },
      { label: "阅读时间", value: `${minutes} min` },
      { label: "标题", value: headings.toString() },
      { label: "代码块", value: codeBlocks.toString() },
      { label: "表格", value: tables.toString() },
    ]
  }, [markdown])

  const reloadMetadata = useCallback(async () => {
    const [nextTags, nextCategories] = await Promise.all([
      fetchKnowledgeTags(),
      fetchKnowledgeCategories(),
    ])
    setAvailableTags(nextTags)
    setAvailableCategories(nextCategories)
    return { tags: nextTags, categories: nextCategories }
  }, [])

  useEffect(() => {
    let cancelled = false

    async function loadWritingContext() {
      setLoading(true)
      try {
        const metadataPromise = reloadMetadata()
        const documentPromise = documentId ? fetchKnowledgeDocument(documentId) : Promise.resolve(null)
        const [, document] = await Promise.all([metadataPromise, documentPromise])
        if (cancelled) return

        if (document) {
          setTitle(document.title || "Untitled")
          setDescription(document.description || "")
          setMarkdown(document.content || "")
          setDocumentStatus(document.status || "draft")
          setTags(document.tags.map((tag) => tag.tag_name).filter(Boolean))
          setCategories(document.categories.map((category) => category.category_name).filter(Boolean))
          setSavedAt(document.updated_at || document.created_at || null)
          setEditorRevision((revision) => revision + 1)
        } else {
          setTitle("")
          setDescription("")
          setMarkdown("")
          setDocumentStatus("draft")
          setTags([])
          setCategories([])
          setSavedAt(null)
          setEditorRevision((revision) => revision + 1)
        }
      } catch (err) {
        notification.danger({ title: "Knowledge base 加载失败", description: err instanceof Error ? err.message : "加载失败" })
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void loadWritingContext()

    return () => {
      cancelled = true
    }
  }, [documentId, reloadMetadata])

  const ensureTagIds = useCallback(
    async (selectedNames: string[]) => {
      const normalizedSelected = Array.from(new Set(selectedNames.map((name) => name.trim()).filter(Boolean)))
      let knownTags = availableTags
      const createdTags: KnowledgeTag[] = []

      for (const name of normalizedSelected) {
        const existing = [...knownTags, ...createdTags].find((tag) => normalizeName(tag.tag_name) === normalizeName(name))
        if (existing) continue

        const id = await createKnowledgeTag({
          tag_name: name,
          tag_code: toOptionCode(name, "tag"),
          description: "",
          color: "",
        })
        createdTags.push({ id, tag_name: name, tag_code: toOptionCode(name, "tag"), description: "", color: "" })
      }

      if (createdTags.length) {
        knownTags = [...knownTags, ...createdTags]
        setAvailableTags(knownTags)
      }

      return normalizedSelected
        .map((name) => knownTags.find((tag) => normalizeName(tag.tag_name) === normalizeName(name))?.id)
        .filter(Boolean) as string[]
    },
    [availableTags],
  )

  const ensureCategoryIds = useCallback(
    async (selectedNames: string[]) => {
      const normalizedSelected = Array.from(new Set(selectedNames.map((name) => name.trim()).filter(Boolean)))
      let knownCategories = availableCategories
      const createdCategories: KnowledgeCategory[] = []

      for (const name of normalizedSelected) {
        const existing = [...knownCategories, ...createdCategories].find(
          (category) => normalizeName(category.category_name) === normalizeName(name),
        )
        if (existing) continue

        const id = await createKnowledgeCategory({
          category_name: name,
          category_code: toOptionCode(name, "category"),
          description: "",
          sort_order: knownCategories.length + createdCategories.length,
        })
        createdCategories.push({
          id,
          category_name: name,
          category_code: toOptionCode(name, "category"),
          description: "",
          sort_order: knownCategories.length + createdCategories.length,
        })
      }

      if (createdCategories.length) {
        knownCategories = [...knownCategories, ...createdCategories]
        setAvailableCategories(knownCategories)
      }

      return normalizedSelected
        .map((name) => knownCategories.find((category) => normalizeName(category.category_name) === normalizeName(name))?.id)
        .filter(Boolean) as string[]
    },
    [availableCategories],
  )

  const saveDocument = useCallback(
    async (nextStatus: KnowledgeDocumentStatus = documentStatus) => {
      const normalizedTitle = title.trim()
      if (!normalizedTitle) {
        notification.warn({ title: "标题不能为空" })
        return
      }

      setSaving(true)
      try {
        const [tagIds, categoryIds] = await Promise.all([
          ensureTagIds(tags),
          ensureCategoryIds(categories),
        ])
        const payload = {
          title: normalizedTitle,
          description: description.trim(),
          content: markdown,
          status: nextStatus,
          extra: "{}",
          tag_ids: tagIds,
          category_ids: categoryIds,
        }

        let nextDocumentId = documentId
        if (documentId) {
          await updateKnowledgeDocument(documentId, payload)
        } else {
          nextDocumentId = await createKnowledgeDocument(payload)
          setSearchParams({ id: nextDocumentId }, { replace: true })
        }

        setDocumentStatus(nextStatus)
        setSavedAt(new Date().toISOString())
        await reloadMetadata()
        notification.success({ title: "Knowledge base 已保存", description: nextStatus === "published" ? "文档已发布" : "文档已保存为草稿" })
      } catch (err) {
        notification.danger({ title: "Knowledge base 保存失败", description: err instanceof Error ? err.message : "保存失败" })
      } finally {
        setSaving(false)
      }
    },
    [
      categories,
      description,
      documentId,
      documentStatus,
      ensureCategoryIds,
      ensureTagIds,
      markdown,
      reloadMetadata,
      setSearchParams,
      tags,
      title,
    ],
  )

  const generateDraftFromAi = useCallback(async () => {
    const sourceText = aiGenerateText.trim()
    if (!sourceText) {
      notification.warn({ title: "请输入文本" })
      return
    }

    setAiGenerating(true)
    try {
      const draft = await generateKnowledgeBaseDraft({ source_text: sourceText })
      setTitle(draft.title)
      setDescription(draft.description)
      setTags(draft.tags)
      setCategories(draft.categories)
      setMarkdown(draft.content)
      setDocumentStatus("draft")
      setSavedAt(null)
      setEditorRevision((revision) => revision + 1)
      setAiGenerateOpen(false)
      notification.success({ title: "AI draft generated" })
    } catch (err) {
      notification.danger({ title: "AI Generate 失败", description: err instanceof Error ? err.message : "生成失败" })
    } finally {
      setAiGenerating(false)
    }
  }, [aiGenerateText])

  return (
    <WorkspaceShell sectionLabel="Knowledge Base" sectionUrl="/knowledge-base/view" pageTitle="Writing">
      <div className="space-y-5">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
              <BookOpenText className="size-4" />
              Knowledge Base
            </div>

            <div
              className="group w-fit max-w-4xl rounded-md"
              role="button"
              tabIndex={0}
              onClick={() => setEditingHeaderField("title")}
              onKeyDown={(event) => {
                if (event.key === "Enter") setEditingHeaderField("title")
              }}
              onBlur={(event) => {
                const nextFocused = event.relatedTarget
                if (editingHeaderField === "title" && (!nextFocused || !event.currentTarget.contains(nextFocused as Node))) {
                  setEditingHeaderField(null)
                }
              }}
            >
              {editingHeaderField === "title" ? (
                <Input
                  value={title}
                  onClick={(event) => event.stopPropagation()}
                  onChange={(event) => setTitle(event.target.value)}
                  className="h-9 min-w-[24rem] border-0 bg-transparent px-0 py-0 text-2xl font-semibold leading-9 tracking-tight text-foreground shadow-none focus-visible:ring-0 md:text-2xl"
                  autoFocus
                />
              ) : (
                <h1 className="m-0 flex h-9 items-center gap-2 text-2xl font-semibold leading-9 tracking-tight text-foreground">
                  {title || "Untitled"}
                  <Pencil className="size-4 text-muted-foreground opacity-0 transition group-hover:opacity-100" />
                </h1>
              )}
            </div>

            <div
              className="group w-fit max-w-3xl rounded-md"
              role="button"
              tabIndex={0}
              onClick={() => setEditingHeaderField("description")}
              onKeyDown={(event) => {
                if (event.key === "Enter") setEditingHeaderField("description")
              }}
              onBlur={(event) => {
                const nextFocused = event.relatedTarget
                if (editingHeaderField === "description" && (!nextFocused || !event.currentTarget.contains(nextFocused as Node))) {
                  setEditingHeaderField(null)
                }
              }}
            >
              {editingHeaderField === "description" ? (
                <textarea
                  value={description}
                  onClick={(event) => event.stopPropagation()}
                  onChange={(event) => setDescription(event.target.value)}
                  className="min-h-16 w-[min(48rem,calc(100vw-20rem))] resize-y rounded-md border-0 bg-transparent px-0 py-0 text-sm leading-6 outline-none focus:ring-0"
                  autoFocus
                />
              ) : (
                <p className="flex items-center gap-2 text-sm leading-6 text-muted-foreground">
                  {description || "No description"}
                  <Pencil className="size-3.5 shrink-0 opacity-0 transition group-hover:opacity-100" />
                </p>
              )}
            </div>
            <div className="flex flex-wrap items-center gap-2 pt-1">
              <span className="inline-flex h-7 items-center gap-1.5 rounded-full bg-muted/55 px-3 text-xs text-muted-foreground">
                <span>状态</span>
                <span className="font-semibold text-foreground/80">{documentStatus === "published" ? "published" : "draft"}</span>
              </span>
              {documentStats.map((stat) => (
                <span
                  key={stat.label}
                  className="inline-flex h-7 items-center gap-1.5 rounded-full bg-muted/55 px-3 text-xs text-muted-foreground"
                >
                  <span>{stat.label}</span>
                  <span className="font-semibold text-foreground/80">{stat.value}</span>
                </span>
              ))}
              {savedAt ? (
                <span className="inline-flex h-7 items-center gap-1.5 rounded-full bg-muted/55 px-3 text-xs text-muted-foreground">
                  <span>上次更新时间</span>
                  <span className="font-semibold text-foreground/80">{new Date(savedAt).toLocaleString()}</span>
                </span>
              ) : null}
            </div>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <div className="flex items-center gap-1 text-xs text-muted-foreground"><span className="hidden sm:inline">AI route</span><AiBindingProviderSelect capability="knowledge_base_document_generate" /></div>
            <div className="inline-flex items-center gap-0.5 rounded-md bg-muted/55 p-0.5">
              <Button
                size="sm"
                variant="ghost"
                className="h-7 rounded-sm px-2.5 text-[11px] font-medium text-muted-foreground hover:bg-background hover:text-foreground"
                type="button"
                onClick={() => setAiGenerateOpen(true)}
              >
                <Sparkles className="size-3.5" />
                AI Generate
              </Button>
              <Button
                size="sm"
                variant="ghost"
                className="h-7 rounded-sm px-2.5 text-[11px] font-medium text-muted-foreground hover:bg-background hover:text-foreground"
                onClick={() => void saveDocument("draft")}
                disabled={saving || loading}
              >
                {saving ? <Loader2 className="size-3.5 animate-spin" /> : <Save className="size-3.5" />}
                Save
              </Button>
              <Button
                size="sm"
                variant="ghost"
                className="h-7 rounded-sm px-2.5 text-[11px] font-semibold text-foreground hover:bg-background"
                onClick={() => void saveDocument("published")}
                disabled={saving || loading}
              >
                {saving ? <Loader2 className="size-3.5 animate-spin" /> : <Check className="size-3.5" />}
                Publish
              </Button>
            </div>
            <Button asChild size="sm" variant="secondary" className="h-7 px-2.5 text-[11px] font-medium">
              <Link to={documentId ? `/knowledge-base/view?id=${encodeURIComponent(documentId)}` : "/knowledge-base/view"}>
                <ArrowLeft className="size-3.5" />
                Back to view
              </Link>
            </Button>
          </div>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <MetadataMultiSelect
            active={editingField === "categories"}
            label="Categories"
            options={categoryOptions}
            values={categories}
            onActivate={() => setEditingField("categories")}
            onDeactivate={() => setEditingField(null)}
            onChange={setCategories}
          />
          <MetadataMultiSelect
            active={editingField === "tags"}
            label="Tags"
            options={tagOptions}
            values={tags}
            onActivate={() => setEditingField("tags")}
            onDeactivate={() => setEditingField(null)}
            onChange={setTags}
          />
        </div>

        <div className="grid gap-4">
          <section className="flex min-h-[640px] flex-col rounded-lg bg-muted/35">
            <div className="min-h-[590px] flex-1">
              <KnowledgeBaseMdxEditor key={editorKey} markdown={markdown} onChange={setMarkdown} />
            </div>
          </section>
        </div>

        <Dialog open={aiGenerateOpen} onOpenChange={setAiGenerateOpen}>
          <DialogContent className="sm:max-w-2xl">
            <DialogHeader>
              <DialogTitle>AI Generate</DialogTitle>
              <DialogDescription>输入一段文本，后续会基于这段内容生成知识库 Markdown。</DialogDescription>
            </DialogHeader>
            <textarea
              value={aiGenerateText}
              onChange={(event) => setAiGenerateText(event.target.value)}
              className="min-h-40 w-full resize-y rounded-md bg-muted/40 px-3 py-2 text-sm leading-6 outline-none focus:bg-muted/55"
              placeholder="Paste source text or describe what you want to generate..."
              autoFocus
            />
            <DialogFooter>
              <DialogClose asChild>
                <Button variant="secondary" size="sm" disabled={aiGenerating}>Cancel</Button>
              </DialogClose>
              <Button size="sm" onClick={() => void generateDraftFromAi()} disabled={aiGenerating}>
                {aiGenerating ? <Loader2 className="size-3.5 animate-spin" /> : <Sparkles className="size-3.5" />}
                Generate
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </WorkspaceShell>
  )
}
