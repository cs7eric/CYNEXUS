import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"
import { fetchAiPrompts, type AiPromptItem } from "@/services/ai/prompt"

export interface AiCapability {
  code: string
  label: string
  group: string
  description: string
  entry: string
  prompt_key?: string
  reads_binding: boolean
  runtime: string
  aliases?: string[]
}

export interface AiProviderType {
  code: string
  label: string
  default_base_url: string
  default_model: string
  api_key_env: string
  group_id_env: string
}

export interface AiProviderItem {
  id: string
  code: string
  name: string
  provider_type: string
  base_url: string
  default_model: string
  models: string[]
  api_key?: string
  api_key_masked: string
  api_key_env: string
  group_id: string
  group_id_env: string
  is_enabled: boolean
  timeout_seconds: number | null
  extra: Record<string, unknown>
  input_price_per_million: number
  output_price_per_million: number
  pricing_currency: string
  remark: string
  created_at?: string | null
  updated_at?: string | null
}

export interface AiBindingItem {
  id: string
  capability: string
  label: string
  provider_id: string
  provider: AiProviderItem | null
  prompt_id: string
  prompt: AiPromptItem | null
  model_override: string
  is_enabled: boolean
  params: Record<string, unknown>
  remark: string
}

interface JavaAiProviderDTO {
  id: string
  code?: string | null
  name?: string | null
  providerType?: string | null
  baseUrl?: string | null
  defaultModel?: string | null
  apiKey?: string | null
  apiKeyMasked?: string | null
  apiKeyEnv?: string | null
  groupId?: string | null
  groupIdEnv?: string | null
  enabled?: boolean | null
  timeoutSeconds?: number | null
  extra?: string | null
  remark?: string | null
  createdAt?: string | null
  updatedAt?: string | null
  deletedAt?: string | null
  models?: string | null
  inputPricePerMillion?: number | null
  outputPricePerMillion?: number | null
  pricingCurrency?: string | null
}

interface JavaAiUsageBindingDTO {
  id: string
  capability?: string | null
  label?: string | null
  providerId?: string | null
  promptId?: string | null
  modelOverride?: string | null
  enabled?: boolean | null
  params?: string | null
  remark?: string | null
  createdAt?: string | null
  updatedAt?: string | null
  deletedAt?: string | null
}

const AI_CAPABILITIES: AiCapability[] = [
  {
    code: "text_polish",
    label: "MP4 text polish",
    group: "MP4 to Text",
    description: "Transcript polish stage after media transcription.",
    entry: "MP4 to Text / AI Polish",
    reads_binding: true,
    runtime: "MediaTranscriptionAiAppService",
    prompt_key: "mp4_text_polish",
  },
  {
    code: "text_summary",
    label: "MP4 summary",
    group: "MP4 to Text",
    description: "Summary generation for polished transcript content.",
    entry: "MP4 to Text / AI Summary",
    reads_binding: true,
    runtime: "MediaTranscriptionAiAppService",
    prompt_key: "mp4_text_summary",
  },
  {
    code: "post_metadata",
    label: "Markdown metadata",
    group: "MP4 to Text",
    description: "Title, tags, and categories for archived markdown export.",
    entry: "MP4 to Text / History export",
    reads_binding: true,
    runtime: "MediaTranscriptionAiAppService",
    prompt_key: "post_metadata",
  },
  {
    code: "mp4_qa",
    label: "MP4 Ask AI",
    group: "MP4 to Text",
    description: "Question answering over transcript, polish, and summary.",
    entry: "MP4 to Text / Ask",
    reads_binding: true,
    runtime: "MediaTranscriptionAiAppService",
    prompt_key: "mp4_qa",
  },
  {
    code: "application_analysis",
    label: "Stock application analysis",
    group: "Stock AI",
    description: "Legacy application-analysis overlay route.",
    entry: "Stock Overview / Application Analysis",
    reads_binding: true,
    runtime: "ApplicationAnalysisModelRouter",
    prompt_key: "application_analysis_annotation",
    aliases: ["Application Analysis"],
  },
  {
    code: "application_recent30",
    label: "AI Direction recent 30-day",
    group: "Stock AI",
    description: "AI Direction / 当日 AI 整体判断，基于最近 K 线写入 daily snapshots.",
    entry: "Stock Overview / Application Analysis / AI Direction",
    reads_binding: true,
    runtime: "ApplicationAnalysisRecent30Executor",
    prompt_key: "application_recent30_short_term_daily",
    aliases: ["AI Direction", "当日 AI 整体判断", "Recent 30-day analysis"],
  },
  {
    code: "accounting_receipt_parse",
    label: "Accounting receipt recognition",
    group: "Finance AI",
    description: "Parse one receipt image into expense or income items, account/category candidates, and installment details.",
    entry: "Finance / AI Capture",
    reads_binding: true,
    runtime: "ScenarioExecutionAppService.executeVision",
    prompt_key: "accounting_receipt_parse",
    aliases: ["AI 记账", "小票识别", "Receipt recognition"],
  },
  {
    code: "auction_analysis",
    label: "Auction AI analysis",
    group: "Stock AI",
    description: "Opening auction analysis prompt chain.",
    entry: "Auction analysis jobs",
    reads_binding: true,
    runtime: "Auction AI service",
    prompt_key: "auction_analysis",
  },
  {
    code: "market_information_event_analysis",
    label: "Market Information event analysis",
    group: "Market Information",
    description: "Structured event analysis for prioritized market information clusters.",
    entry: "Information / Event analysis",
    reads_binding: true,
    runtime: "MarketInformationAiWorker",
    prompt_key: "market_information_event_analysis",
    aliases: ["Market Information", "Event analysis"],
  },
  {
    code: "market_information_daily_brief",
    label: "Market Information daily brief",
    group: "Market Information",
    description: "Daily market brief assembled from analyzed information events.",
    entry: "Information / Daily brief",
    reads_binding: true,
    runtime: "MarketInformationAiWorker",
    prompt_key: "market_information_daily_brief",
    aliases: ["Market brief", "Daily brief"],
  },
  {
    code: "knowledge_base_document_generate",
    label: "Knowledge Base document generation",
    group: "Knowledge Base",
    description: "Generate structured knowledge-base drafts with prompt version and fallback chain.",
    entry: "Knowledge Base / Writing / AI Generate",
    reads_binding: true,
    runtime: "KnowledgeBaseDocumentGenerationAppService",
    prompt_key: "knowledge_base_document_generate",
  },
  {
    code: "workspace_chat",
    label: "Workspace chat",
    group: "Workspace AI",
    description: "Conversational AI entry available from the global workspace header.",
    entry: "Workspace / Global AI Chat",
    reads_binding: true,
    runtime: "ScenarioExecutionAppService.execute",
    prompt_key: "workspace_chat",
    aliases: ["Cynexus AI", "Workspace assistant"],
  },
]

const AI_PROVIDER_TYPES: AiProviderType[] = [
  {
    code: "minimax",
    label: "MiniMax",
    default_base_url: "https://api.minimaxi.com",
    default_model: "MiniMax-M2.5",
    api_key_env: "MINIMAX_API_KEY",
    group_id_env: "MINIMAX_GROUP_ID",
  },
  {
    code: "openai_compatible",
    label: "OpenAI compatible",
    default_base_url: "https://api.openai.com",
    default_model: "",
    api_key_env: "OPENAI_API_KEY",
    group_id_env: "",
  },
  {
    code: "deepseek",
    label: "DeepSeek",
    default_base_url: "https://api.deepseek.com",
    default_model: "deepseek-v4-flash",
    api_key_env: "DEEPSEEK_API_KEY",
    group_id_env: "",
  },
  {
    code: "anthropic_compatible",
    label: "Anthropic compatible",
    default_base_url: "",
    default_model: "",
    api_key_env: "",
    group_id_env: "",
  },
]

function parseJsonObject(value?: string | null): Record<string, unknown> {
  if (!value) return {}
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed as Record<string, unknown> : {}
  } catch {
    return {}
  }
}

function parseJsonStringList(value?: string | null): string[] {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.map((item) => String(item).trim()).filter(Boolean) : []
  } catch {
    return []
  }
}

function maskSecret(value?: string | null): string {
  if (!value) return ""
  if (value.length <= 8) return "****"
  return `${value.slice(0, 4)}...${value.slice(-4)}`
}

function toAiProviderItem(dto: JavaAiProviderDTO): AiProviderItem {
  return {
    id: String(dto.id),
    code: dto.code || "",
    name: dto.name || "",
    provider_type: dto.providerType || "",
    base_url: dto.baseUrl || "",
    default_model: dto.defaultModel || "",
    models: parseJsonStringList(dto.models),
    api_key: "",
    api_key_masked: dto.apiKeyMasked || maskSecret(dto.apiKey),
    api_key_env: dto.apiKeyEnv || "",
    group_id: dto.groupId || "",
    group_id_env: dto.groupIdEnv || "",
    is_enabled: dto.enabled ?? true,
    timeout_seconds: dto.timeoutSeconds ?? null,
    extra: parseJsonObject(dto.extra),
    input_price_per_million: dto.inputPricePerMillion ?? 0,
    output_price_per_million: dto.outputPricePerMillion ?? 0,
    pricing_currency: dto.pricingCurrency || "USD",
    remark: dto.remark || "",
    created_at: dto.createdAt || null,
    updated_at: dto.updatedAt || null,
  }
}

function toJavaAiProviderPayload(payload: Partial<AiProviderItem>): Partial<JavaAiProviderDTO> {
  const value: Partial<JavaAiProviderDTO> = {
    id: payload.id,
    code: payload.code,
    name: payload.name,
    providerType: payload.provider_type,
    baseUrl: payload.base_url,
    defaultModel: payload.default_model,
    apiKeyEnv: payload.api_key_env,
    groupId: payload.group_id,
    groupIdEnv: payload.group_id_env,
    enabled: payload.is_enabled,
    timeoutSeconds: payload.timeout_seconds,
    extra: JSON.stringify(payload.extra || {}),
    remark: payload.remark,
    models: JSON.stringify(payload.models || []),
    inputPricePerMillion: payload.input_price_per_million,
    outputPricePerMillion: payload.output_price_per_million,
    pricingCurrency: payload.pricing_currency,
  }
  if (payload.api_key && payload.api_key.trim()) {
    value.apiKey = payload.api_key.trim()
  }
  return value
}

function toAiBindingItem(
  dto: JavaAiUsageBindingDTO,
  providerById: Map<string, AiProviderItem> = new Map(),
  promptById: Map<string, AiPromptItem> = new Map(),
): AiBindingItem {
  const providerId = dto.providerId ? String(dto.providerId) : ""
  const promptId = dto.promptId ? String(dto.promptId) : ""
  return {
    id: String(dto.id),
    capability: dto.capability || "",
    label: dto.label || dto.capability || "",
    provider_id: providerId,
    provider: providerById.get(providerId) || null,
    prompt_id: promptId,
    prompt: promptById.get(promptId) || null,
    model_override: dto.modelOverride || "",
    is_enabled: dto.enabled ?? true,
    params: parseJsonObject(dto.params),
    remark: dto.remark || "",
  }
}

function toJavaAiBindingPayload(payload: Partial<AiBindingItem>): Partial<JavaAiUsageBindingDTO> {
  return {
    id: payload.id,
    capability: payload.capability,
    label: payload.label,
    providerId: payload.provider_id || null,
    promptId: payload.prompt_id || null,
    modelOverride: payload.model_override,
    enabled: payload.is_enabled,
    params: JSON.stringify(payload.params || {}),
    remark: payload.remark,
  }
}

async function getAiProvider(id: string): Promise<AiProviderItem> {
  const dto = await cynexusResult<JavaAiProviderDTO>(
    await fetchWithRetry(`${SERVICE_PREFIX.aiConfig}/providers/${encodeURIComponent(id)}`, { cache: "no-store" }),
  )
  return toAiProviderItem(dto)
}

export async function fetchAiCapabilities(): Promise<AiCapability[]> {
  return AI_CAPABILITIES
}

export async function fetchAiProviderTypes(): Promise<AiProviderType[]> {
  return AI_PROVIDER_TYPES
}

export async function fetchAiProviders(): Promise<AiProviderItem[]> {
  const items = await cynexusResult<JavaAiProviderDTO[]>(
    await fetchWithRetry(`${SERVICE_PREFIX.aiConfig}/providers`, { cache: "no-store" }),
  )
  return (items || [])
    .filter((item) => !item.deletedAt)
    .map(toAiProviderItem)
}

export async function createAiProvider(payload: Partial<AiProviderItem>): Promise<AiProviderItem> {
  const id = await cynexusResult<string>(
    await fetchWithRetry(`${SERVICE_PREFIX.aiConfig}/providers`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(toJavaAiProviderPayload(payload)),
    }),
  )
  return getAiProvider(String(id))
}

export async function updateAiProvider(id: string, payload: Partial<AiProviderItem>): Promise<AiProviderItem> {
  await cynexusResult<boolean>(
    await fetchWithRetry(`${SERVICE_PREFIX.aiConfig}/providers`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(toJavaAiProviderPayload({ id, ...payload })),
    }),
  )
  return getAiProvider(id)
}

export async function deleteAiProvider(id: string): Promise<void> {
  await cynexusResult<boolean>(
    await fetchWithRetry(`${SERVICE_PREFIX.aiConfig}/providers/${encodeURIComponent(id)}`, { method: "DELETE" }),
  )
}

export async function fetchAiBindings(): Promise<AiBindingItem[]> {
  const [providers, prompts, bindings] = await Promise.all([
    fetchAiProviders(),
    fetchAiPrompts({ status: "active" }),
    cynexusResult<JavaAiUsageBindingDTO[]>(
      await fetchWithRetry(`${SERVICE_PREFIX.aiConfig}/usage-bindings`, { cache: "no-store" }),
    ),
  ])
  const providerById = new Map(providers.map((provider) => [provider.id, provider]))
  const promptById = new Map(prompts.map((prompt) => [prompt.id, prompt]))
  return (bindings || [])
    .filter((item) => !item.deletedAt)
    .map((item) => toAiBindingItem(item, providerById, promptById))
    .sort((a, b) => a.capability.localeCompare(b.capability))
}

export async function upsertAiBinding(payload: Partial<AiBindingItem>): Promise<AiBindingItem> {
  const existing = (await fetchAiBindings()).find((item) => item.capability === payload.capability)
  const method = existing ? "PUT" : "POST"
  const savedPayload = existing ? { ...payload, id: existing.id } : payload
  const result = await cynexusResult<string | boolean>(
    await fetchWithRetry(`${SERVICE_PREFIX.aiConfig}/usage-bindings`, {
      method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(toJavaAiBindingPayload(savedPayload)),
    }),
  )
  const id = existing?.id || String(result)
  const dto = await cynexusResult<JavaAiUsageBindingDTO>(
    await fetchWithRetry(`${SERVICE_PREFIX.aiConfig}/usage-bindings/${encodeURIComponent(id)}`, { cache: "no-store" }),
  )
  const [providers, prompts] = await Promise.all([fetchAiProviders(), fetchAiPrompts({ status: "active" })])
  return toAiBindingItem(dto, new Map(providers.map((provider) => [provider.id, provider])), new Map(prompts.map((prompt) => [prompt.id, prompt])))
}
