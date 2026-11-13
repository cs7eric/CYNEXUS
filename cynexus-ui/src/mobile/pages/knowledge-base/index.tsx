import { Check, ChevronRight, FileText, LoaderCircle, PenLine, Plus, Search, X } from "lucide-react"
import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react"

import { KnowledgeBaseMarkdownViewer } from "@/views/knowledge-base/components/knowledge-base-markdown-viewer"
import { estimateReadingMinutes, formatKnowledgeDate } from "@/views/knowledge-base/lib/format"
import {
  type KnowledgeCategory,
  type KnowledgeDocument,
  type KnowledgeDocumentStatus,
  type KnowledgeTag,
  createKnowledgeCategory,
  createKnowledgeDocument,
  createKnowledgeTag,
  fetchKnowledgeCategories,
  fetchKnowledgeDocuments,
  fetchKnowledgeTags,
  updateKnowledgeDocument,
} from "@/services/knowledge-base/documents"
import "./knowledge-base.css"

type StatusFilter = "all" | KnowledgeDocumentStatus
type PageMode = "list" | "reader" | "editor"

interface DocumentDraft {
  id?: string
  title: string
  description: string
  content: string
  status: KnowledgeDocumentStatus
  tags: string[]
  categories: string[]
}

const statusTabs: Array<{ value: StatusFilter; label: string }> = [
  { value: "all", label: "All" },
  { value: "published", label: "Published" },
  { value: "draft", label: "Drafts" },
  { value: "archived", label: "Archived" },
]

function toDraft(document?: KnowledgeDocument): DocumentDraft {
  return {
    id: document?.id,
    title: document?.title ?? "",
    description: document?.description ?? "",
    content: document?.content ?? "",
    status: document?.status ?? "draft",
    tags: document?.tags.map((tag) => tag.tag_name) ?? [],
    categories: document?.categories.map((category) => category.category_name) ?? [],
  }
}

function normalizeName(value: string) { return value.trim().toLocaleLowerCase() }

function optionCode(value: string, prefix: string) {
  const slug = value.trim().toLocaleLowerCase().replace(/[^\p{L}\p{N}]+/gu, "-").replace(/^-|-$/g, "")
  return `${prefix}-${slug || crypto.randomUUID()}`
}

function parseNames(value: string) {
  const seen = new Set<string>()
  return value.split(",").map((name) => name.trim()).filter((name) => {
    const key = normalizeName(name)
    if (!key || seen.has(key)) return false
    seen.add(key)
    return true
  })
}

function DocumentMetadata({ document }: { document: KnowledgeDocument }) {
  const updated = document.updated_at || document.created_at
  return <div className="mobile-kb-meta"><span>{document.categories[0]?.category_name || "Uncategorized"}</span><span className="mobile-kb-meta-separator" /><span>{updated ? formatKnowledgeDate(updated).split(" ")[0] : "No date"}</span><span className="mobile-kb-meta-separator" /><span>{estimateReadingMinutes(document.content)} min read</span></div>
}

export default function MobileKnowledgeBasePage() {
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([])
  const [categories, setCategories] = useState<KnowledgeCategory[]>([])
  const [tags, setTags] = useState<KnowledgeTag[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [mode, setMode] = useState<PageMode>("list")
  const [activeId, setActiveId] = useState("")
  const [status, setStatus] = useState<StatusFilter>("all")
  const [categoryId, setCategoryId] = useState("all")
  const [keyword, setKeyword] = useState("")
  const [draft, setDraft] = useState<DocumentDraft>(() => toDraft())
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const reload = useCallback(async () => {
    setLoading(true)
    setLoadError(null)
    try {
      const [nextDocuments, nextTags, nextCategories] = await Promise.all([
        fetchKnowledgeDocuments(), fetchKnowledgeTags(), fetchKnowledgeCategories(),
      ])
      setDocuments(nextDocuments)
      setTags(nextTags)
      setCategories(nextCategories)
      setActiveId((current) => nextDocuments.some((document) => document.id === current) ? current : nextDocuments[0]?.id ?? "")
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : "Documents could not be loaded.")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => { void reload() }, 0)
    return () => window.clearTimeout(timer)
  }, [reload])

  const activeDocument = documents.find((document) => document.id === activeId)
  const filteredDocuments = useMemo(() => {
    const query = keyword.trim().toLocaleLowerCase()
    return documents.filter((document) =>
      (status === "all" || document.status === status)
      && (categoryId === "all" || document.category_ids.includes(categoryId))
      && (!query || `${document.title} ${document.description} ${document.tags.map((tag) => tag.tag_name).join(" ")}`.toLocaleLowerCase().includes(query)),
    ).sort((left, right) => (right.updated_at || right.created_at || "").localeCompare(left.updated_at || left.created_at || ""))
  }, [categoryId, documents, keyword, status])

  useEffect(() => {
    function handleBack(event: Event) {
      if (mode === "list") return
      event.preventDefault()
      setMode(mode === "editor" ? (activeDocument ? "reader" : "list") : "list")
      setSaveError(null)
    }
    window.addEventListener("mobile-back", handleBack)
    return () => window.removeEventListener("mobile-back", handleBack)
  }, [activeDocument, mode])

  function openDocument(document: KnowledgeDocument) {
    setActiveId(document.id)
    setMode("reader")
  }

  function startCreate() {
    setDraft(toDraft())
    setSaveError(null)
    setMode("editor")
  }

  function startEdit() {
    setDraft(toDraft(activeDocument))
    setSaveError(null)
    setMode("editor")
  }

  async function ensureTagIds(names: string[]) {
    const ids: string[] = []
    for (const name of names.map((value) => value.trim()).filter(Boolean)) {
      const existing = tags.find((tag) => normalizeName(tag.tag_name) === normalizeName(name))
      if (existing) { ids.push(existing.id); continue }
      const tagCode = optionCode(name, "tag")
      const id = await createKnowledgeTag({ tag_name: name, tag_code: tagCode, description: "", color: "" })
      setTags((current) => [...current, { id, tag_name: name, tag_code: tagCode, description: "", color: "" }])
      ids.push(id)
    }
    return ids
  }

  async function ensureCategoryIds(names: string[]) {
    const ids: string[] = []
    for (const name of names.map((value) => value.trim()).filter(Boolean)) {
      const existing = categories.find((category) => normalizeName(category.category_name) === normalizeName(name))
      if (existing) { ids.push(existing.id); continue }
      const categoryCode = optionCode(name, "category")
      const sortOrder = categories.length
      const id = await createKnowledgeCategory({ category_name: name, category_code: categoryCode, description: "", sort_order: sortOrder })
      setCategories((current) => [...current, { id, category_name: name, category_code: categoryCode, description: "", sort_order: sortOrder }])
      ids.push(id)
    }
    return ids
  }

  async function saveDocument(nextStatus: KnowledgeDocumentStatus) {
    if (!draft.title.trim()) { setSaveError("Add a title before saving."); return }
    setSaving(true)
    setSaveError(null)
    try {
      const [tagIds, categoryIds] = await Promise.all([ensureTagIds(draft.tags), ensureCategoryIds(draft.categories)])
      const payload = { title: draft.title.trim(), description: draft.description.trim(), content: draft.content, status: nextStatus, tag_ids: tagIds, category_ids: categoryIds }
      let nextId = draft.id
      if (nextId) await updateKnowledgeDocument(nextId, payload)
      else nextId = await createKnowledgeDocument(payload)
      await reload()
      setActiveId(nextId)
      setNotice(nextStatus === "published" ? "Document published" : "Draft saved")
      setMode("reader")
      window.setTimeout(() => setNotice(null), 2200)
    } catch (error) {
      setSaveError(error instanceof Error ? error.message : "Document could not be saved.")
    } finally {
      setSaving(false)
    }
  }

  function updateDraft<K extends keyof DocumentDraft>(key: K, value: DocumentDraft[K]) {
    setDraft((current) => ({ ...current, [key]: value }))
  }

  return (
    <div className="mobile-kb">
      {mode === "list" && <>
        <header className="mobile-kb-heading">
          <div><h2>Knowledge Base</h2><p>Shared workspace</p></div>
          <button type="button" className="mobile-kb-add" aria-label="Create document" title="Create document" onClick={startCreate}><Plus aria-hidden="true" /></button>
        </header>

        <div className="mobile-kb-summary" aria-label="Document summary">
          <div><strong>{documents.length}</strong><span>Documents</span></div>
          <div><strong>{documents.filter((document) => document.status === "published").length}</strong><span>Published</span></div>
          <div><strong>{documents.filter((document) => document.status === "draft").length}</strong><span>Drafts</span></div>
        </div>

        <div className="mobile-kb-tools">
          <label className="mobile-kb-search"><Search aria-hidden="true" /><input type="search" value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="Search documents" aria-label="Search documents" /></label>
          <select className="mobile-kb-category-select" value={categoryId} onChange={(event) => setCategoryId(event.target.value)} aria-label="Filter by category">
            <option value="all">Category</option>
            {categories.map((category) => <option key={category.id} value={category.id}>{category.category_name}</option>)}
          </select>
        </div>

        <div className="mobile-kb-status-tabs" role="group" aria-label="Filter by document status">
          {statusTabs.map((tab) => <button key={tab.value} type="button" aria-pressed={status === tab.value} className={status === tab.value ? "is-selected" : ""} onClick={() => setStatus(tab.value)}>{tab.label}</button>)}
        </div>

        <section className="mobile-kb-document-panel" aria-labelledby="mobile-kb-list-heading" aria-busy={loading}>
          <div className="mobile-kb-section-heading"><h3 id="mobile-kb-list-heading">Recently updated</h3><span>{filteredDocuments.length} {filteredDocuments.length === 1 ? "document" : "documents"}</span></div>
          {loading ? <div className="mobile-kb-state"><LoaderCircle className="is-spinning" aria-hidden="true" /><span>Loading documents</span></div>
            : loadError ? <div className="mobile-kb-state is-error"><p>{loadError}</p><button type="button" onClick={() => void reload()}>Try again</button></div>
              : filteredDocuments.length ? <div className="mobile-kb-document-list">{filteredDocuments.map((document) => <button type="button" className="mobile-kb-document-row" key={document.id} onClick={() => openDocument(document)}>
                <span className="mobile-kb-document-main"><span className="mobile-kb-document-title-line"><span className="mobile-kb-document-title">{document.title}</span><span className={`mobile-kb-status-label is-${document.status}`}>{document.status}</span></span>
                  {document.description && <span className="mobile-kb-document-description">{document.description}</span>}
                  <DocumentMetadata document={document} />
                </span><ChevronRight className="mobile-kb-row-chevron" aria-hidden="true" />
              </button>)}</div>
                : <div className="mobile-kb-state"><FileText aria-hidden="true" /><span>No documents match these filters.</span></div>}
        </section>
      </>}

      {mode === "reader" && activeDocument && <article className="mobile-kb-reader">
        <div className="mobile-kb-reader-toolbar"><button type="button" className="mobile-kb-back" onClick={() => setMode("list")}><ChevronRight aria-hidden="true" />Documents</button><button type="button" className="mobile-kb-edit" onClick={startEdit}><PenLine aria-hidden="true" /><span>Edit</span></button></div>
        <div className="mobile-kb-reader-meta"><span className={`mobile-kb-status-label is-${activeDocument.status}`}>{activeDocument.status}</span><DocumentMetadata document={activeDocument} /></div>
        <h2>{activeDocument.title}</h2>
        {activeDocument.description && <p className="mobile-kb-reader-description">{activeDocument.description}</p>}
        {(activeDocument.categories.length > 0 || activeDocument.tags.length > 0) && <div className="mobile-kb-taxonomy">{activeDocument.categories.map((category) => <span key={category.id}>{category.category_name}</span>)}{activeDocument.tags.map((tag) => <span key={tag.id}>#{tag.tag_name}</span>)}</div>}
        <div className="mobile-kb-markdown"><KnowledgeBaseMarkdownViewer markdown={activeDocument.content || ""} /></div>
      </article>}

      {mode === "editor" && <section className="mobile-kb-editor">
        <header className="mobile-kb-editor-header"><button type="button" className="mobile-kb-back" onClick={() => { setSaveError(null); setMode(activeDocument ? "reader" : "list") }}><X aria-hidden="true" />Cancel</button><h2>{draft.id ? "Edit document" : "New document"}</h2></header>
        <form onSubmit={(event: FormEvent<HTMLFormElement>) => { event.preventDefault(); void saveDocument("published") }}>
          <label className="mobile-kb-field"><span>Title</span><input required maxLength={200} value={draft.title} onChange={(event) => updateDraft("title", event.target.value)} placeholder="Document title" /></label>
          <label className="mobile-kb-field"><span>Description</span><textarea rows={2} maxLength={500} value={draft.description} onChange={(event) => updateDraft("description", event.target.value)} placeholder="A short summary" /></label>
          <label className="mobile-kb-field"><span>Content</span><textarea className="mobile-kb-content-input" value={draft.content} onChange={(event) => updateDraft("content", event.target.value)} placeholder="Write in Markdown" /></label>
          <label className="mobile-kb-field"><span>Categories</span><input value={draft.categories.join(", ")} onChange={(event) => updateDraft("categories", parseNames(event.target.value))} placeholder="Separate with commas" /></label>
          <label className="mobile-kb-field"><span>Tags</span><input value={draft.tags.join(", ")} onChange={(event) => updateDraft("tags", parseNames(event.target.value))} placeholder="Separate with commas" /></label>
          {saveError && <p className="mobile-kb-save-error" role="alert">{saveError}</p>}
          <div className="mobile-kb-editor-actions"><button type="button" className="mobile-kb-save-draft" disabled={saving} onClick={() => void saveDocument("draft")}>{saving ? <LoaderCircle className="is-spinning" aria-hidden="true" /> : null}Save draft</button><button type="submit" className="mobile-kb-publish" disabled={saving}>{saving ? <LoaderCircle className="is-spinning" aria-hidden="true" /> : <Check aria-hidden="true" />}Publish</button></div>
        </form>
      </section>}
      {notice && <div className="mobile-kb-notice" role="status">{notice}</div>}
    </div>
  )
}
