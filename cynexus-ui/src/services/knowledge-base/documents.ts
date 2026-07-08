import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

export type KnowledgeDocumentStatus = "draft" | "published" | "archived"

export interface KnowledgeTag {
  id: string
  tag_code: string
  tag_name: string
  description: string
  color: string
}

export interface KnowledgeCategory {
  id: string
  category_code: string
  category_name: string
  description: string
  sort_order: number
}

export interface KnowledgeDocument {
  id: string
  title: string
  description: string
  content: string
  content_format: "markdown"
  status: KnowledgeDocumentStatus
  extra: string
  tag_ids: string[]
  category_ids: string[]
  tags: KnowledgeTag[]
  categories: KnowledgeCategory[]
  created_at?: string | null
  updated_at?: string | null
}

export interface KnowledgeDocumentFilters {
  keyword?: string
  status?: KnowledgeDocumentStatus | "all"
  tag_id?: string
  category_id?: string
}

interface JavaKnowledgeTagDTO {
  id?: string | number | null
  tagCode?: string | null
  tagName?: string | null
  description?: string | null
  color?: string | null
}

interface JavaKnowledgeCategoryDTO {
  id?: string | number | null
  categoryCode?: string | null
  categoryName?: string | null
  description?: string | null
  sortOrder?: number | null
}

interface JavaKnowledgeDocumentDTO {
  id?: string | number | null
  title?: string | null
  description?: string | null
  content?: string | null
  contentFormat?: string | null
  status?: string | null
  extra?: string | null
  tagIds?: Array<string | number> | null
  categoryIds?: Array<string | number> | null
  tags?: JavaKnowledgeTagDTO[] | null
  categories?: JavaKnowledgeCategoryDTO[] | null
  createdAt?: string | null
  updatedAt?: string | null
  deletedAt?: string | null
}

function toIdList(values?: Array<string | number> | null) {
  return (values || []).map(String).filter(Boolean)
}

function toTag(dto: JavaKnowledgeTagDTO): KnowledgeTag {
  return {
    id: String(dto.id || ""),
    tag_code: dto.tagCode || "",
    tag_name: dto.tagName || "",
    description: dto.description || "",
    color: dto.color || "",
  }
}

function toCategory(dto: JavaKnowledgeCategoryDTO): KnowledgeCategory {
  return {
    id: String(dto.id || ""),
    category_code: dto.categoryCode || "",
    category_name: dto.categoryName || "",
    description: dto.description || "",
    sort_order: dto.sortOrder || 0,
  }
}

function toDocument(dto: JavaKnowledgeDocumentDTO): KnowledgeDocument {
  return {
    id: String(dto.id || ""),
    title: dto.title || "",
    description: dto.description || "",
    content: dto.content || "",
    content_format: "markdown",
    status: (dto.status || "draft") as KnowledgeDocumentStatus,
    extra: dto.extra || "{}",
    tag_ids: toIdList(dto.tagIds),
    category_ids: toIdList(dto.categoryIds),
    tags: (dto.tags || []).map(toTag),
    categories: (dto.categories || []).map(toCategory),
    created_at: dto.createdAt || null,
    updated_at: dto.updatedAt || null,
  }
}

function toJavaDocumentPayload(payload: Partial<KnowledgeDocument>): Partial<JavaKnowledgeDocumentDTO> {
  return {
    id: payload.id,
    title: payload.title,
    description: payload.description || null,
    content: payload.content || "",
    contentFormat: "markdown",
    status: payload.status || "draft",
    extra: payload.extra || "{}",
    tagIds: (payload.tag_ids || []).filter(Boolean),
    categoryIds: (payload.category_ids || []).filter(Boolean),
  }
}

function toJavaTagPayload(payload: Partial<KnowledgeTag>): Partial<JavaKnowledgeTagDTO> {
  return {
    id: payload.id,
    tagCode: payload.tag_code,
    tagName: payload.tag_name,
    description: payload.description || null,
    color: payload.color || null,
  }
}

function toJavaCategoryPayload(payload: Partial<KnowledgeCategory>): Partial<JavaKnowledgeCategoryDTO> {
  return {
    id: payload.id,
    categoryCode: payload.category_code,
    categoryName: payload.category_name,
    description: payload.description || null,
    sortOrder: payload.sort_order || 0,
  }
}

function buildDocumentQuery(filters?: KnowledgeDocumentFilters) {
  const params = new URLSearchParams()
  if (filters?.keyword?.trim()) params.set("keyword", filters.keyword.trim())
  if (filters?.status && filters.status !== "all") params.set("status", filters.status)
  if (filters?.tag_id) params.set("tagId", filters.tag_id)
  if (filters?.category_id) params.set("categoryId", filters.category_id)
  const query = params.toString()
  return query ? `?${query}` : ""
}

export async function fetchKnowledgeDocuments(filters?: KnowledgeDocumentFilters): Promise<KnowledgeDocument[]> {
  const items = await cynexusResult<JavaKnowledgeDocumentDTO[]>(
    await fetchWithRetry(`${SERVICE_PREFIX.knowledgeBase}/documents${buildDocumentQuery(filters)}`, { cache: "no-store" }),
  )
  return (items || []).filter((item) => !item.deletedAt).map(toDocument)
}

export async function fetchKnowledgeDocument(id: string): Promise<KnowledgeDocument> {
  const item = await cynexusResult<JavaKnowledgeDocumentDTO>(
    await fetchWithRetry(`${SERVICE_PREFIX.knowledgeBase}/documents/${encodeURIComponent(id)}`, { cache: "no-store" }),
  )
  return toDocument(item)
}

export async function createKnowledgeDocument(payload: Partial<KnowledgeDocument>): Promise<string> {
  const id = await cynexusResult<string | number>(
    await fetchWithRetry(`${SERVICE_PREFIX.knowledgeBase}/documents`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(toJavaDocumentPayload(payload)),
    }),
  )
  return String(id)
}

export async function updateKnowledgeDocument(id: string, payload: Partial<KnowledgeDocument>): Promise<void> {
  await cynexusResult<boolean>(
    await fetchWithRetry(`${SERVICE_PREFIX.knowledgeBase}/documents`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(toJavaDocumentPayload({ ...payload, id })),
    }),
  )
}

export async function deleteKnowledgeDocument(id: string): Promise<void> {
  await cynexusResult<boolean>(
    await fetchWithRetry(`${SERVICE_PREFIX.knowledgeBase}/documents/${encodeURIComponent(id)}`, { method: "DELETE" }),
  )
}

export async function fetchKnowledgeTags(): Promise<KnowledgeTag[]> {
  const items = await cynexusResult<JavaKnowledgeTagDTO[]>(
    await fetchWithRetry(`${SERVICE_PREFIX.knowledgeBase}/tags`, { cache: "no-store" }),
  )
  return (items || []).map(toTag)
}

export async function createKnowledgeTag(payload: Partial<KnowledgeTag>): Promise<string> {
  const id = await cynexusResult<string | number>(
    await fetchWithRetry(`${SERVICE_PREFIX.knowledgeBase}/tags`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(toJavaTagPayload(payload)),
    }),
  )
  return String(id)
}

export async function fetchKnowledgeCategories(): Promise<KnowledgeCategory[]> {
  const items = await cynexusResult<JavaKnowledgeCategoryDTO[]>(
    await fetchWithRetry(`${SERVICE_PREFIX.knowledgeBase}/categories`, { cache: "no-store" }),
  )
  return (items || []).map(toCategory)
}

export async function createKnowledgeCategory(payload: Partial<KnowledgeCategory>): Promise<string> {
  const id = await cynexusResult<string | number>(
    await fetchWithRetry(`${SERVICE_PREFIX.knowledgeBase}/categories`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(toJavaCategoryPayload(payload)),
    }),
  )
  return String(id)
}
