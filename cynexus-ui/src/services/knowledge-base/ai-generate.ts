import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

export interface KnowledgeBaseGenerateDraftRequest {
  source_text: string
  preferred_model_code?: string
  tenant_code?: string
  routing_quality?: string
  max_cost_tier?: string
}

export interface KnowledgeBaseGeneratedDraft {
  title: string
  description: string
  tags: string[]
  categories: string[]
  content: string
}

interface JavaKnowledgeBaseGenerateDraftRequest {
  sourceText: string
  preferredModelCode?: string
  tenantCode?: string
  routingQuality?: string
  maxCostTier?: string
}

interface JavaKnowledgeBaseGeneratedDraft {
  title?: string | null
  description?: string | null
  tags?: string[] | null
  categories?: string[] | null
  content?: string | null
}

function toJavaPayload(payload: KnowledgeBaseGenerateDraftRequest): JavaKnowledgeBaseGenerateDraftRequest {
  return {
    sourceText: payload.source_text,
    preferredModelCode: payload.preferred_model_code,
    tenantCode: payload.tenant_code,
    routingQuality: payload.routing_quality,
    maxCostTier: payload.max_cost_tier,
  }
}

function toDraft(dto: JavaKnowledgeBaseGeneratedDraft): KnowledgeBaseGeneratedDraft {
  return {
    title: dto.title || "",
    description: dto.description || "",
    tags: dto.tags || [],
    categories: dto.categories || [],
    content: dto.content || "",
  }
}

export async function generateKnowledgeBaseDraft(
  payload: KnowledgeBaseGenerateDraftRequest,
): Promise<KnowledgeBaseGeneratedDraft> {
  const result = await cynexusResult<JavaKnowledgeBaseGeneratedDraft>(
    await fetchWithRetry(`${SERVICE_PREFIX.ai}/knowledge-base/document-draft`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(toJavaPayload(payload)),
    }),
  )
  return toDraft(result)
}
