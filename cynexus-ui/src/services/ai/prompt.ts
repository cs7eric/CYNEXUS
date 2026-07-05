import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

export type AiPromptStatus = "draft" | "active" | "archived"

export interface AiPromptItem {
  id: string
  prompt_key: string
  name: string
  scenario_code: string
  prompt_version: string
  status: AiPromptStatus
  system_prompt: string
  user_prompt_template: string
  output_schema: string
  variables: string
  tags: string
  extra: string
  remark: string
  created_at?: string | null
  updated_at?: string | null
}

export interface AiPromptFilters {
  keyword?: string
  scenario_code?: string
  status?: AiPromptStatus | "all"
}

interface JavaAiPromptDTO {
  id?: string | number | null
  promptKey?: string | null
  name?: string | null
  scenarioCode?: string | null
  promptVersion?: string | null
  status?: string | null
  systemPrompt?: string | null
  userPromptTemplate?: string | null
  outputSchema?: string | null
  variables?: string | null
  tags?: string | null
  extra?: string | null
  remark?: string | null
  createdAt?: string | null
  updatedAt?: string | null
  deletedAt?: string | null
}

function normalizeJson(value: string | undefined | null, fallback: string) {
  const text = value?.trim()
  return text ? text : fallback
}

function toAiPromptItem(dto: JavaAiPromptDTO): AiPromptItem {
  return {
    id: String(dto.id || ""),
    prompt_key: dto.promptKey || "",
    name: dto.name || "",
    scenario_code: dto.scenarioCode || "",
    prompt_version: dto.promptVersion || "v1",
    status: (dto.status || "draft") as AiPromptStatus,
    system_prompt: dto.systemPrompt || "",
    user_prompt_template: dto.userPromptTemplate || "",
    output_schema: dto.outputSchema || "",
    variables: normalizeJson(dto.variables, "[]"),
    tags: normalizeJson(dto.tags, "[]"),
    extra: normalizeJson(dto.extra, "{}"),
    remark: dto.remark || "",
    created_at: dto.createdAt || null,
    updated_at: dto.updatedAt || null,
  }
}

function toJavaAiPromptPayload(payload: Partial<AiPromptItem>): Partial<JavaAiPromptDTO> {
  return {
    id: payload.id,
    promptKey: payload.prompt_key,
    name: payload.name,
    scenarioCode: payload.scenario_code || null,
    promptVersion: payload.prompt_version || "v1",
    status: payload.status || "draft",
    systemPrompt: payload.system_prompt,
    userPromptTemplate: payload.user_prompt_template || null,
    outputSchema: payload.output_schema?.trim() || null,
    variables: normalizeJson(payload.variables, "[]"),
    tags: normalizeJson(payload.tags, "[]"),
    extra: normalizeJson(payload.extra, "{}"),
    remark: payload.remark || null,
  }
}

function buildPromptQuery(filters?: AiPromptFilters) {
  const params = new URLSearchParams()
  if (filters?.keyword?.trim()) params.set("keyword", filters.keyword.trim())
  if (filters?.scenario_code?.trim()) params.set("scenarioCode", filters.scenario_code.trim())
  if (filters?.status && filters.status !== "all") params.set("status", filters.status)
  const query = params.toString()
  return query ? `?${query}` : ""
}

async function getAiPrompt(id: string): Promise<AiPromptItem> {
  const dto = await cynexusResult<JavaAiPromptDTO>(
    await fetchWithRetry(`${SERVICE_PREFIX.ai}/prompts/${encodeURIComponent(id)}`, { cache: "no-store" }),
  )
  return toAiPromptItem(dto)
}

export async function fetchAiPrompts(filters?: AiPromptFilters): Promise<AiPromptItem[]> {
  const items = await cynexusResult<JavaAiPromptDTO[]>(
    await fetchWithRetry(`${SERVICE_PREFIX.ai}/prompts${buildPromptQuery(filters)}`, { cache: "no-store" }),
  )
  return (items || []).filter((item) => !item.deletedAt).map(toAiPromptItem)
}

export async function createAiPrompt(payload: Partial<AiPromptItem>): Promise<AiPromptItem> {
  const id = await cynexusResult<string | number>(
    await fetchWithRetry(`${SERVICE_PREFIX.ai}/prompts`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(toJavaAiPromptPayload(payload)),
    }),
  )
  return getAiPrompt(String(id))
}

export async function updateAiPrompt(id: string, payload: Partial<AiPromptItem>): Promise<AiPromptItem> {
  await cynexusResult<boolean>(
    await fetchWithRetry(`${SERVICE_PREFIX.ai}/prompts`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(toJavaAiPromptPayload({ id, ...payload })),
    }),
  )
  return getAiPrompt(id)
}

export async function deleteAiPrompt(id: string): Promise<void> {
  await cynexusResult<boolean>(
    await fetchWithRetry(`${SERVICE_PREFIX.ai}/prompts/${encodeURIComponent(id)}`, { method: "DELETE" }),
  )
}
