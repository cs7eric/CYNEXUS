import { useCallback, useEffect, useMemo, useState } from "react"
import {
  AlertTriangle,
  BrainCircuit,
  CheckCircle2,
  Database,
  Plus,
  RefreshCw,
  Route,
  Save,
  Search,
  Trash2,
  Wrench,
  X,
} from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { notification } from "@/components/ui/notification"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { UnsavedChangesGuard } from "@/components/unsaved-changes-guard"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
  type AiBindingItem,
  type AiCapability,
  type AiProviderItem,
  type AiReasoningEffort,
  type AiProviderType,
  AI_REASONING_EFFORT_PARAM,
  AI_REASONING_EFFORTS,
  DEFAULT_AI_REASONING_EFFORT,
  createAiProvider,
  deleteAiProvider,
  fetchAiBindings,
  fetchAiProviderTypes,
  fetchAiProviders,
  buildAiCapabilitiesFromBindings,
  normalizeAiReasoningEffort,
  updateAiProvider,
  upsertAiBinding,
} from "@/services/ai/provider"
import {
  fetchAgentCapabilities,
  fetchAgentTools,
  type AgentCapability,
  type AgentToolDefinition,
  updateAgentCapabilityGovernance,
} from "@/services/ai/agent"
import { cn } from "@/lib/utils"
import { fetchAiPrompts, type AiPromptItem } from "@/services/ai/prompt"

/**
 * Maintenance Notice:
 * Before modifying this page, review the related documentation under:
 * - design/front/infra/index.md
 * - design/front/infra/ai-provider.detail.md
 * - design/front/reuse/reuse.md
 *
 * If this page's route, data source, API usage, component structure,
 * or reusable logic changes, update the corresponding design documents.
 */

// Keep the AI Provider page documentation in sync when changing this UI, route behavior, or payload shape.

type ProviderDraft = {
  id?: string
  code: string
  name: string
  provider_type: string
  base_url: string
  default_model: string
  model_configs: ProviderModelDraft[]
  api_key: string
  api_key_env: string
  group_id: string
  group_id_env: string
  timeout_seconds: string
  is_enabled: boolean
  remark: string
  input_price_per_million: string
  output_price_per_million: string
  pricing_currency: string
  extra: Record<string, unknown>
  reasoning_effort: string
}

type ProviderModelDraft = {
  model: string
  input_price_per_million: string
  output_price_per_million: string
}

type BindingDraft = {
  providerId: string
  modelOverride: string
  promptId: string
  routeGroup: string
  params: Record<string, unknown>
  reasoningEffort: AiReasoningEffort
}

type AgentRoutingRow = {
  kind: "capability" | "tool" | "both"
  code: string
  name: string
  module: string
  description: string
  routeGroup: string
  runtimeEntry: string
  readOnly: boolean
  requiresConfirmation: boolean
  availability: string
  source: string
  freshness: string
}

function agentCapabilityRow(item: AgentCapability): AgentRoutingRow {
  return {
    kind: "capability",
    code: item.capabilityCode,
    name: item.name,
    module: item.module,
    description: item.description,
    routeGroup: item.routeGroup || `Agent / ${item.module}`,
    runtimeEntry: item.runtimeEntry || "Agent runtime",
    readOnly: item.readOnly,
    requiresConfirmation: item.requiresConfirmation,
    availability: item.availability || "UNKNOWN",
    source: item.source || "registry",
    freshness: item.freshness || "unknown",
  }
}

function agentToolRow(item: AgentToolDefinition): AgentRoutingRow {
  return {
    kind: "tool",
    code: item.code,
    name: item.name,
    module: item.module,
    description: item.description,
    routeGroup: item.routeGroup || `Agent / ${item.module}`,
    runtimeEntry: item.runtimeEntry || "Agent runtime",
    readOnly: item.readOnly,
    requiresConfirmation: item.requiresConfirmation,
    availability: item.availability || "UNKNOWN",
    source: item.source || "catalog",
    freshness: item.freshness || "unknown",
  }
}

function mergeAgentRoutingRows(agentCapabilities: AgentCapability[], agentTools: AgentToolDefinition[]): AgentRoutingRow[] {
  const rows = new Map<string, AgentRoutingRow>()
  agentCapabilities.forEach((item) => rows.set(item.capabilityCode, agentCapabilityRow(item)))
  agentTools.forEach((item) => {
    const existing = rows.get(item.code)
    if (!existing) {
      rows.set(item.code, agentToolRow(item))
      return
    }
    rows.set(item.code, {
      ...existing,
      kind: "both",
      runtimeEntry: item.runtimeEntry || existing.runtimeEntry,
      availability: item.availability || existing.availability,
      source: item.source || existing.source,
      freshness: item.freshness || existing.freshness,
    })
  })
  return [...rows.values()].sort((a, b) => a.routeGroup.localeCompare(b.routeGroup) || a.name.localeCompare(b.name))
}

function normalizeBindingReasoning(params: Record<string, unknown>): AiReasoningEffort {
  return normalizeAiReasoningEffort(params[AI_REASONING_EFFORT_PARAM]) || DEFAULT_AI_REASONING_EFFORT
}

function firstBindingText(params: Record<string, unknown>, keys: string[]): string {
  for (const key of keys) {
    const value = params[key]
    if (typeof value === "string" && value.trim()) return value.trim()
  }
  return ""
}

const emptyDraft: ProviderDraft = {
  code: "",
  name: "",
  provider_type: "openai_compatible",
  base_url: "",
  default_model: "",
  model_configs: [],
  api_key: "",
  api_key_env: "",
  group_id: "",
  group_id_env: "",
  timeout_seconds: "120",
  is_enabled: true,
  remark: "",
  input_price_per_million: "0",
  output_price_per_million: "0",
  pricing_currency: "USD",
  extra: {},
  reasoning_effort: "",
}

function modelDrafts(provider: AiProviderItem): ProviderModelDraft[] {
  const configs = provider.model_configs?.length
    ? provider.model_configs
    : (provider.models || []).map((model) => ({
      model,
      input_price_per_million: provider.input_price_per_million,
      output_price_per_million: provider.output_price_per_million,
    }))
  return configs.map((config) => ({
    model: config.model,
    input_price_per_million: String(config.input_price_per_million ?? 0),
    output_price_per_million: String(config.output_price_per_million ?? 0),
  }))
}

function providerModelNames(provider: AiProviderItem | null): string[] {
  if (!provider) return []
  return uniq([
    ...(provider.model_configs || []).map((item) => item.model),
    ...(provider.models || []),
    provider.default_model || "",
  ])
}

function draftFromProvider(provider: AiProviderItem): ProviderDraft {
  return {
    id: provider.id,
    code: provider.code,
    name: provider.name,
    provider_type: provider.provider_type,
    base_url: provider.base_url,
    default_model: provider.default_model && modelDrafts(provider).some((item) => item.model === provider.default_model)
      ? provider.default_model
      : modelDrafts(provider)[0]?.model || "",
    model_configs: modelDrafts(provider),
    api_key: "",
    api_key_env: provider.api_key_env,
    group_id: provider.group_id,
    group_id_env: provider.group_id_env,
    timeout_seconds: String(provider.timeout_seconds || 120),
    is_enabled: provider.is_enabled,
    remark: provider.remark,
    input_price_per_million: String(provider.input_price_per_million ?? 0),
    output_price_per_million: String(provider.output_price_per_million ?? 0),
    pricing_currency: provider.pricing_currency || "USD",
    extra: provider.extra || {},
    reasoning_effort: typeof provider.extra?.model_reasoning_effort === "string" ? provider.extra.model_reasoning_effort : "",
  }
}

export default function AiProviderSettingsPage() {
  const [providers, setProviders] = useState<AiProviderItem[]>([])
  const [providerTypes, setProviderTypes] = useState<AiProviderType[]>([])
  const [bindings, setBindings] = useState<AiBindingItem[]>([])
  const [prompts, setPrompts] = useState<AiPromptItem[]>([])
  const [capabilities, setCapabilities] = useState<AiCapability[]>([])
  const [agentCapabilities, setAgentCapabilities] = useState<AgentCapability[]>([])
  const [agentTools, setAgentTools] = useState<AgentToolDefinition[]>([])
  const [draft, setDraft] = useState<ProviderDraft>(emptyDraft)
  const [initialDraft, setInitialDraft] = useState<ProviderDraft>(emptyDraft)
  const [selectedProviderId, setSelectedProviderId] = useState("")
  const [dialogOpen, setDialogOpen] = useState(false)
  const [dialogMode, setDialogMode] = useState<"create" | "edit">("create")
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [savingBinding, setSavingBinding] = useState("")
  const [error, setError] = useState("")
  const [newModelInput, setNewModelInput] = useState("")
  const [selectedModelIndex, setSelectedModelIndex] = useState(0)
  const [bindingDrafts, setBindingDrafts] = useState<Record<string, BindingDraft>>({})
  const [bindingSaveTimers, setBindingSaveTimers] = useState<Record<string, number>>({})
  const [savingGovernance, setSavingGovernance] = useState("")
  const [routingView, setRoutingView] = useState<"ai" | "tools">("ai")
  const [routingQuery, setRoutingQuery] = useState("")
  const [routingGroup, setRoutingGroup] = useState("all")
  const [routingScope, setRoutingScope] = useState<"all" | "attention">("all")

  const selectedProvider = useMemo(
    () => providers.find((item) => item.id === selectedProviderId) || null,
    [providers, selectedProviderId],
  )

  const reload = useCallback(async () => {
    setLoading(true)
    setError("")
    try {
      const [nextProviders, nextBindings, nextPrompts, nextAgentCapabilities, nextTools] = await Promise.all([
        fetchAiProviders(),
        fetchAiBindings(),
        fetchAiPrompts({ status: "active" }),
        fetchAgentCapabilities().catch(() => []),
        fetchAgentTools().catch(() => []),
      ])
      const nextProviderTypes = await fetchAiProviderTypes()
      setCapabilities(buildAiCapabilitiesFromBindings(nextBindings))
      setProviderTypes(nextProviderTypes)
      setProviders(nextProviders)
      setBindings(nextBindings)
      setPrompts(nextPrompts)
      setAgentCapabilities(nextAgentCapabilities)
      setAgentTools(nextTools)
      const nextCapabilities = buildAiCapabilitiesFromBindings(nextBindings)
      const capabilityGroups = new Map(nextCapabilities.map((item) => [item.code, item.group]))
      setBindingDrafts(
        Object.fromEntries(
          nextBindings.map((binding) => [
            binding.capability,
            {
              providerId: binding.provider_id || "",
              modelOverride: binding.model_override || "",
              promptId: binding.prompt_id || "",
              routeGroup: firstBindingText(binding.params || {}, ["route_group", "routeGroup", "group"]) || capabilityGroups.get(binding.capability) || "AI / Other",
              params: binding.params,
              reasoningEffort: normalizeBindingReasoning(binding.params),
            },
          ]),
        ),
      )
      if (!selectedProviderId && nextProviders[0]) {
        setSelectedProviderId(nextProviders[0].id)
        setDraft(draftFromProvider(nextProviders[0]))
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "加载 AI Provider 设置失败")
    } finally {
      setLoading(false)
    }
  }, [selectedProviderId])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void reload()
    }, 0)
    return () => window.clearTimeout(timer)
  }, [reload])

  const bindingByCapability = useMemo(() => {
    const map = new Map<string, AiBindingItem>()
    bindings.forEach((item) => map.set(item.capability, item))
    return map
  }, [bindings])

  const providerById = useMemo(() => {
    const map = new Map<string, AiProviderItem>()
    providers.forEach((item) => map.set(item.id, item))
    return map
  }, [providers])

  const routeRows = useMemo(
    () => capabilities.map((capability) => {
      const binding = bindingByCapability.get(capability.code)
      const draftRoute = bindingDrafts[capability.code]
      const providerId = draftRoute?.providerId ?? binding?.provider_id ?? ""
      const provider = providerById.get(providerId) || null
      const modelOverride = draftRoute?.modelOverride ?? binding?.model_override ?? ""
      const promptId = draftRoute?.promptId ?? binding?.prompt_id ?? ""
      const routeGroup = draftRoute?.routeGroup ?? (firstBindingText(binding?.params || {}, ["route_group", "routeGroup", "group"]) || capability.group)
      const reasoningEffort = draftRoute?.reasoningEffort ?? normalizeBindingReasoning(binding?.params || {})
      const effectiveModel = modelOverride || provider?.default_model || ""
      const issues = routeIssues(provider, effectiveModel)
      const persistedRouteGroup = firstBindingText(binding?.params || {}, ["route_group", "routeGroup", "group"]) || capability.group
      const dirty = providerId !== (binding?.provider_id || "") || modelOverride !== (binding?.model_override || "") || promptId !== (binding?.prompt_id || "") || routeGroup !== persistedRouteGroup || reasoningEffort !== normalizeBindingReasoning(binding?.params || {})
      return { capability, binding, providerId, provider, modelOverride, promptId, routeGroup, reasoningEffort, effectiveModel, issues, dirty }
    }).sort((a, b) => a.routeGroup.localeCompare(b.routeGroup) || a.capability.label.localeCompare(b.capability.label)),
    [bindingByCapability, bindingDrafts, capabilities, providerById],
  )

  const issueCount = routeRows.filter((row) => row.issues.length > 0).length
  const activeBindingCount = routeRows.filter((row) => row.binding?.is_enabled).length

  const agentRoutingRows = useMemo(
    () => mergeAgentRoutingRows(agentCapabilities, agentTools),
    [agentCapabilities, agentTools],
  )

  const routingGroups = useMemo(
    () => (routingView === "tools"
      ? uniq(agentRoutingRows.map((tool) => tool.routeGroup))
      : uniq(routeRows.map((row) => row.routeGroup))
    ).sort((a, b) => a.localeCompare(b)),
    [agentRoutingRows, routeRows, routingView],
  )

  const normalizedRoutingQuery = routingQuery.trim().toLowerCase()
  const filteredRouteRows = useMemo(
    () => routeRows.filter(({ capability, routeGroup, issues }) => {
      const matchesGroup = routingGroup === "all" || routeGroup === routingGroup
      const haystack = [capability.label, capability.code, routeGroup, capability.description, ...(capability.aliases || [])]
        .join(" ")
        .toLowerCase()
      const matchesQuery = !normalizedRoutingQuery || haystack.includes(normalizedRoutingQuery)
      const matchesAttention = routingView !== "ai" || routingScope === "all" || issues.length > 0
      return matchesGroup && matchesQuery && matchesAttention
    }),
    [normalizedRoutingQuery, routeRows, routingGroup, routingScope, routingView],
  )

  const filteredAgentTools = useMemo(
    () => agentRoutingRows.filter((tool) => {
      const matchesGroup = routingGroup === "all" || tool.routeGroup === routingGroup
      const haystack = [tool.name, tool.code, tool.routeGroup, tool.module, tool.description, tool.runtimeEntry]
        .join(" ")
        .toLowerCase()
      return matchesGroup && (!normalizedRoutingQuery || haystack.includes(normalizedRoutingQuery))
    }),
    [agentRoutingRows, normalizedRoutingQuery, routingGroup],
  )

  async function handleSaveProvider() {
    if (!draft.code.trim() || !draft.name.trim()) {
      notification.danger({ title: "Provider 未保存", description: "Code 和名称不能为空" })
      return
    }
    setSaving(true)
    try {
      const modelConfigs = draft.model_configs
        .map((item) => ({
          model: item.model.trim(),
          input_price_per_million: Number(item.input_price_per_million || 0),
          output_price_per_million: Number(item.output_price_per_million || 0),
        }))
        .filter((item) => item.model)
      const extra = { ...draft.extra }
      if (draft.reasoning_effort.trim()) {
        extra.model_reasoning_effort = draft.reasoning_effort.trim()
      } else {
        delete extra.model_reasoning_effort
      }
      const payload = {
        ...draft,
        default_model: draft.default_model && modelConfigs.some((item) => item.model === draft.default_model)
          ? draft.default_model
          : modelConfigs[0]?.model || "",
        model_configs: modelConfigs,
        extra,
        timeout_seconds: Number(draft.timeout_seconds || 120),
        input_price_per_million: Number(draft.input_price_per_million || 0),
        output_price_per_million: Number(draft.output_price_per_million || 0),
        pricing_currency: draft.pricing_currency.trim().toUpperCase() || "USD",
      }
      const saved = draft.id
        ? await updateAiProvider(draft.id, payload)
        : await createAiProvider(payload)
      notification.success({ title: "Provider 已保存", description: saved.name })
      setSelectedProviderId(saved.id)
      setDialogOpen(false)
      await reload()
    } catch (err) {
      notification.danger({ title: "Provider 保存失败", description: err instanceof Error ? err.message : "保存失败" })
    } finally {
      setSaving(false)
    }
  }

  async function handleDeleteProvider() {
    if (!draft.id) return
    setSaving(true)
    try {
      await deleteAiProvider(draft.id)
      notification.success({ title: "Provider 已删除", description: draft.name })
      setDraft(emptyDraft)
      setSelectedProviderId("")
      setDialogOpen(false)
      await reload()
    } catch (err) {
      notification.danger({ title: "删除失败", description: err instanceof Error ? err.message : "删除失败" })
    } finally {
      setSaving(false)
    }
  }

  function openCreateDialog() {
    setDialogMode("create")
    setDraft(emptyDraft)
    setInitialDraft(emptyDraft)
    setSelectedModelIndex(0)
    setDialogOpen(true)
  }

  function openEditDialog(provider: AiProviderItem) {
    setDialogMode("edit")
    setSelectedProviderId(provider.id)
    setDraft(draftFromProvider(provider))
    setInitialDraft(draftFromProvider(provider))
    setSelectedModelIndex(0)
    setDialogOpen(true)
  }

  function handleProviderTypeChange(providerTypeCode: string) {
    const providerType = providerTypes.find((item) => item.code === providerTypeCode)
    setDraft((current) => ({
      ...current,
      provider_type: providerTypeCode,
      base_url: current.base_url || providerType?.default_base_url || "",
      default_model: current.default_model || providerType?.default_model || "",
      api_key_env: current.api_key_env || providerType?.api_key_env || "",
      group_id_env: current.group_id_env || providerType?.group_id_env || "",
    }))
  }

  function updateBindingDraft(capabilityCode: string, patch: Partial<BindingDraft>) {
    setBindingDrafts((current) => {
      const binding = bindingByCapability.get(capabilityCode)
      const existing = current[capabilityCode] || {
        providerId: binding?.provider_id || "",
        modelOverride: binding?.model_override || "",
        promptId: binding?.prompt_id || "",
        routeGroup: firstBindingText(binding?.params || {}, ["route_group", "routeGroup", "group"]) || "AI / Other",
        params: binding?.params || {},
        reasoningEffort: normalizeBindingReasoning(binding?.params || {}),
      }
      return {
        ...current,
        [capabilityCode]: {
          ...existing,
          ...patch,
        },
      }
    })
  }

  function scheduleBindingSave(capability: AiCapability, providerId: string, modelOverride: string, promptId: string, routeGroup: string, reasoningEffort: AiReasoningEffort) {
    const previous = bindingSaveTimers[capability.code]
    if (previous) window.clearTimeout(previous)
    const timer = window.setTimeout(() => {
      void handleBindingChange(capability, providerId, modelOverride, promptId, routeGroup, reasoningEffort)
      setBindingSaveTimers((current) => {
        const next = { ...current }
        delete next[capability.code]
        return next
      })
    }, 250)
    setBindingSaveTimers((current) => ({ ...current, [capability.code]: timer }))
  }

  async function handleBindingChange(capability: AiCapability, providerId: string, modelOverride: string, promptId: string, routeGroup: string, reasoningEffort: AiReasoningEffort) {
    const binding = bindingByCapability.get(capability.code) || null
    const provider = providerById.get(providerId) || null
    const normalizedModel = modelOverride.trim() || provider?.default_model || ""
    const issues = routeIssues(provider, normalizedModel)
    if (issues.some((issue) => issue.level === "error")) {
      notification.danger({
        title: "绑定未保存",
        description: issues.find((issue) => issue.level === "error")?.message || "Provider 和模型不匹配",
      })
      return
    }
    setSavingBinding(capability.code)
    try {
      const saved = await upsertAiBinding({
        capability: capability.code,
        label: capability.label,
        provider_id: providerId,
        model_override: normalizedModel,
        prompt_id: promptId,
        is_enabled: true,
          params: {
            ...(bindingDrafts[capability.code]?.params || binding?.params || {}),
            route_group: routeGroup.trim() || capability.group,
            [AI_REASONING_EFFORT_PARAM]: reasoningEffort,
        },
      })
      setBindings((current) => {
        const rest = current.filter((item) => item.capability !== saved.capability)
        return [...rest, saved].sort((a, b) => a.capability.localeCompare(b.capability))
      })
      setBindingDrafts((current) => ({
        ...current,
        [saved.capability]: {
          providerId: saved.provider_id || "",
          modelOverride: saved.model_override || "",
          promptId: saved.prompt_id || "",
          routeGroup: firstBindingText(saved.params || {}, ["route_group", "routeGroup", "group"]) || capability.group,
          params: saved.params,
          reasoningEffort: normalizeBindingReasoning(saved.params),
        },
      }))
      notification.success({ title: "绑定已保存", description: capability.label })
    } catch (err) {
      notification.danger({ title: "绑定保存失败", description: err instanceof Error ? err.message : "保存失败" })
    } finally {
      setSavingBinding("")
    }
  }

  async function handleGovernanceChange(capability: AiCapability, enabled: boolean, availability: string, priority: number) {
    if (!capability.governed) return
    setSavingGovernance(capability.code)
    try {
      await updateAgentCapabilityGovernance(capability.code, { enabled, availability, priority })
      notification.success({ title: "Capability governance saved", description: capability.code })
      await reload()
    } catch (err) {
      notification.danger({ title: "Capability governance failed", description: err instanceof Error ? err.message : "保存失败" })
    } finally {
      setSavingGovernance("")
    }
  }

  const selectedModelConfig = draft.model_configs[selectedModelIndex]

  return (
    <WorkspaceShell>
      <UnsavedChangesGuard when={(dialogOpen && JSON.stringify(draft) !== JSON.stringify(initialDraft)) || routeRows.some((row) => row.dirty)} saving={saving || Boolean(savingBinding) || Boolean(savingGovernance)} />
      <div className="flex w-full flex-col gap-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
              <BrainCircuit className="size-4" />
              AI
            </div>
            <h1 className="text-2xl font-semibold tracking-tight text-foreground">AI Provider</h1>
            <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
              管理模型连接和功能绑定。OpenAI-compatible endpoint 可以直接通过配置接入。
            </p>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => void reload()} disabled={loading}>
              <RefreshCw className={cn("size-4", loading && "animate-spin")} />
              Refresh
            </Button>
            <Button
              variant="secondary"
              onClick={openCreateDialog}
            >
              <Plus className="size-4" />
              New provider
            </Button>
          </div>
        </div>

        {error ? (
          <Alert variant="destructive">
            <AlertTitle>AI Provider 设置加载失败</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        ) : null}

        <div className="space-y-4">
          <section className="rounded-lg bg-muted/40 p-3">
            <div className="grid gap-2 md:grid-cols-4">
              <RouteStat icon={BrainCircuit} label="AI capabilities" value={`${activeBindingCount}/${capabilities.length}`} />
              <RouteStat icon={Wrench} label="Agent tools" value={String(agentRoutingRows.length)} />
              <RouteStat icon={Database} label="Providers" value={String(providers.length)} />
              <RouteStat icon={AlertTriangle} label="Needs attention" value={String(issueCount)} />
            </div>
          </section>

          <section className="space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-sm font-semibold">
                <Database className="size-4" />
                Provider switchboard
              </div>
              <Badge variant="secondary">{providers.length}</Badge>
            </div>
            <div className="overflow-hidden rounded-lg bg-muted/40">
              {providers.length ? (
                <div className="grid grid-cols-[minmax(180px,1.2fr)_minmax(150px,0.85fr)_minmax(160px,1fr)_100px_96px] gap-3 px-3 py-2 text-xs font-medium text-muted-foreground">
                  <div>Provider</div>
                  <div>Type</div>
                  <div>Model</div>
                  <div>Timeout</div>
                  <div className="text-right">Action</div>
                </div>
              ) : null}
              {providers.map((provider) => (
                <div
                  key={provider.id}
                  onClick={() => {
                    setSelectedProviderId(provider.id)
                    setDraft(draftFromProvider(provider))
                  }}
                  onKeyDown={(event) => {
                    if (event.key === "Enter" || event.key === " ") {
                      setSelectedProviderId(provider.id)
                      setDraft(draftFromProvider(provider))
                    }
                  }}
                  role="button"
                  tabIndex={0}
                  className={cn(
                    "grid w-full cursor-pointer grid-cols-[minmax(180px,1.2fr)_minmax(150px,0.85fr)_minmax(160px,1fr)_100px_96px] items-center gap-3 px-3 py-3 text-left transition hover:bg-white",
                    selectedProviderId === provider.id && "bg-background",
                  )}
                >
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className={cn(
                        "size-2 rounded-full",
                        provider.is_enabled ? "bg-emerald-500" : "bg-muted-foreground/30",
                      )} />
                      <span className="truncate font-medium">{provider.name}</span>
                    </div>
                    <div className="truncate pl-4 text-xs text-muted-foreground">{provider.code}</div>
                  </div>
                  <div className="truncate text-sm text-muted-foreground">{provider.provider_type}</div>
                  <div className="truncate text-sm text-muted-foreground">{provider.default_model || "no model"}</div>
                  <div className="text-sm text-muted-foreground">{provider.timeout_seconds || 120}s</div>
                  <div className="flex justify-end">
                    <Button
                      size="sm"
                      variant="ghost"
                      className="h-8 px-2"
                      onClick={(event) => {
                        event.stopPropagation()
                        openEditDialog(provider)
                      }}
                    >
                      Edit
                    </Button>
                  </div>
                </div>
              ))}
              {!providers.length && !loading ? (
                <div className="p-4 text-sm text-muted-foreground">
                  暂无 provider，先新建一个。
                </div>
              ) : null}
            </div>
          </section>

          <section className="rounded-lg border border-border/60 bg-card p-3">
            <div className="mb-3 flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
              <div>
                <div className="flex items-center gap-2 text-sm font-semibold">
                  <CheckCircle2 className="size-4" />
                  AI routing registry
                </div>
                <p className="mt-1 text-xs text-muted-foreground">
                  AI 能力负责选择模型和 Prompt；Agent Tools 只是可被 Agent 调用的工具，两者分开管理。
                </p>
              </div>
              <Tabs value={routingView} onValueChange={(value) => {
                setRoutingView(value as "ai" | "tools")
                setRoutingGroup("all")
                setRoutingScope("all")
              }}>
                <TabsList>
                  <TabsTrigger value="ai">
                    <BrainCircuit className="size-3.5" />
                    AI capabilities
                    <Badge variant="secondary" className="ml-1 px-1.5 py-0 text-[10px]">{capabilities.length}</Badge>
                  </TabsTrigger>
                  <TabsTrigger value="tools">
                    <Wrench className="size-3.5" />
                    Agent tools & capabilities
                    <Badge variant="secondary" className="ml-1 px-1.5 py-0 text-[10px]">{agentRoutingRows.length}</Badge>
                  </TabsTrigger>
                </TabsList>
              </Tabs>
            </div>
            <div className="mb-3 flex flex-col gap-2 sm:flex-row sm:items-center">
              <div className="relative min-w-0 flex-1">
                <Search className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
                <Input
                  value={routingQuery}
                  onChange={(event) => setRoutingQuery(event.target.value)}
                  placeholder={routingView === "ai" ? "Search capability, code, group or entry" : "Search tool, code, module or runtime"}
                  className="h-8 pl-8 text-xs"
                />
              </div>
              <Select value={routingGroup} onValueChange={setRoutingGroup}>
                <SelectTrigger className="h-8 w-full text-xs sm:w-[220px]"><SelectValue placeholder="All groups" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All groups</SelectItem>
                  {routingGroups.map((group) => <SelectItem key={group} value={group}>{group}</SelectItem>)}
                </SelectContent>
              </Select>
              {routingView === "ai" ? (
                <Select value={routingScope} onValueChange={(value) => setRoutingScope(value as "all" | "attention")}>
                  <SelectTrigger className="h-8 w-full text-xs sm:w-[160px]"><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All routes</SelectItem>
                    <SelectItem value="attention">Needs attention</SelectItem>
                  </SelectContent>
                </Select>
              ) : null}
            </div>
              <Tabs value={routingView} onValueChange={(value) => setRoutingView(value as "ai" | "tools")}>
                <TabsContent value="ai">
                <div className="overflow-x-auto rounded-lg bg-muted/25">
              <div className="sticky top-0 z-10 grid min-w-[1480px] grid-cols-[minmax(240px,1.2fr)_minmax(160px,0.8fr)_minmax(190px,0.9fr)_minmax(190px,0.9fr)_minmax(130px,0.6fr)_minmax(220px,1fr)_minmax(190px,0.9fr)_minmax(220px,1fr)] gap-3 border-b border-border/50 bg-background/95 px-3 py-2 text-xs font-medium text-muted-foreground">
                <div>AI capability</div>
                <div>Group</div>
                <div>Provider</div>
                <div>Model</div>
                <div>Reasoning</div>
                <div>Prompt</div>
                <div>Runtime / entry</div>
                <div className="text-right">Governance</div>
              </div>
              <div className="divide-y divide-border/50">
                {filteredRouteRows.map((row) => {
                  const { capability, providerId, provider, modelOverride, promptId, routeGroup, reasoningEffort, issues, dirty } = row
                  const availableModels = uniq([...providerModelNames(provider), modelOverride].filter(Boolean))
                  return (
                    <div key={capability.code} className="grid min-w-[1480px] grid-cols-[minmax(240px,1.2fr)_minmax(160px,0.8fr)_minmax(190px,0.9fr)_minmax(190px,0.9fr)_minmax(130px,0.6fr)_minmax(220px,1fr)_minmax(190px,0.9fr)_minmax(220px,1fr)] items-start gap-3 px-3 py-3">
                      <div className="min-w-0">
                        <div className="flex min-w-0 items-center gap-2">
                          <span className="truncate font-medium">{capability.label}</span>
                          {issues.length ? <AlertTriangle className="size-3.5 shrink-0 text-amber-600" /> : null}
                        </div>
                        <div className="truncate text-xs text-muted-foreground">{capability.code}</div>
                        {capability.aliases?.length ? (
                          <div className="mt-1 flex flex-wrap gap-1">
                            {capability.aliases.map((alias) => (
                              <span key={alias} className="rounded bg-muted px-1.5 py-0.5 text-[11px] text-muted-foreground">{alias}</span>
                            ))}
                          </div>
                        ) : null}
                      </div>
                      <div className="min-w-0 space-y-1">
                        <Select
                          value={routeGroup}
                          onValueChange={(value) => {
                            updateBindingDraft(capability.code, { routeGroup: value })
                            scheduleBindingSave(capability, providerId, modelOverride, promptId, value, reasoningEffort)
                          }}
                        >
                          <SelectTrigger className="h-8 text-xs"><SelectValue /></SelectTrigger>
                          <SelectContent>
                            {uniq([...routingGroups, routeGroup]).map((group) => <SelectItem key={group} value={group}>{group}</SelectItem>)}
                          </SelectContent>
                        </Select>
                        <div className="truncate text-xs text-muted-foreground">{capability.description}</div>
                      </div>
                      <Select
                        value={providerId || "none"}
                          onValueChange={(value) => {
                            const nextProviderId = value === "none" ? "" : value
                            const nextProvider = providerById.get(nextProviderId)
                            const nextModel = nextProvider?.default_model || ""
                              updateBindingDraft(capability.code, {
                              providerId: nextProviderId,
                              modelOverride: nextModel,
                            })
                            scheduleBindingSave(capability, nextProviderId, nextModel, promptId, routeGroup, reasoningEffort)
                          }}
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="none">No provider</SelectItem>
                          {providers.map((provider) => (
                            <SelectItem key={provider.id} value={provider.id}>{provider.name}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      {availableModels.length > 0 ? (
                        <Select
                          value={modelOverride || provider?.default_model || ""}
                          onValueChange={(value) => {
                            updateBindingDraft(capability.code, { modelOverride: value })
                            scheduleBindingSave(capability, providerId, value, promptId, routeGroup, reasoningEffort)
                          }}
                        >
                          <SelectTrigger>
                            <SelectValue placeholder={provider?.default_model || "provider default"} />
                          </SelectTrigger>
                          <SelectContent>
                            {availableModels.map((model) => (
                              <SelectItem key={model} value={model}>{model}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      ) : (
                        <Input
                          value={modelOverride}
                          onChange={(event) => updateBindingDraft(capability.code, { modelOverride: event.target.value })}
                           onBlur={(event) => scheduleBindingSave(capability, providerId, event.target.value, promptId, routeGroup, reasoningEffort)}
                          placeholder={provider?.default_model || "provider default"}
                        />
                      )}
                      <Select
                        value={reasoningEffort}
                        onValueChange={(value) => {
                          const nextReasoningEffort = value as AiReasoningEffort
                          updateBindingDraft(capability.code, { reasoningEffort: nextReasoningEffort })
                          scheduleBindingSave(capability, providerId, modelOverride, promptId, routeGroup, nextReasoningEffort)
                        }}
                      >
                        <SelectTrigger><SelectValue /></SelectTrigger>
                        <SelectContent>
                          {AI_REASONING_EFFORTS.map((effort) => (
                            <SelectItem key={effort} value={effort}>{effort}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <Select
                        value={promptId || "none"}
                        onValueChange={(value) => {
                          const nextPromptId = value === "none" ? "" : value
                          updateBindingDraft(capability.code, { promptId: nextPromptId })
                           scheduleBindingSave(capability, providerId, modelOverride, nextPromptId, routeGroup, reasoningEffort)
                        }}
                      >
                        <SelectTrigger><SelectValue placeholder="No prompt" /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="none">No prompt</SelectItem>
                          {prompts.filter((prompt) => prompt.prompt_key === (capability.prompt_key || capability.code)).map((prompt) => (
                            <SelectItem key={prompt.id} value={prompt.id}>{prompt.name} · {prompt.prompt_version}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <div className="min-w-0">
                        <div className="truncate text-xs text-muted-foreground">{capability.runtime}</div>
                        <div className="mt-1 truncate text-xs text-muted-foreground">{capability.entry}</div>
                        {issues.length ? (
                          <div className="mt-1 text-xs text-amber-700">{issues[0].message}</div>
                        ) : (
                          <div className="mt-1 text-xs text-emerald-700">Reads binding before execution</div>
                        )}
                      </div>
                      <div className="flex flex-col items-end gap-2">
                        <div className="flex items-center gap-2">
                          <Badge variant={capability.availability === "AVAILABLE" ? "outline" : "secondary"}>
                            {capability.governed ? `${capability.availability || "UNKNOWN"} · v${capability.version || "?"}` : "STATIC"}
                          </Badge>
                          {capability.governed ? (
                            <Button
                              size="sm"
                              variant={capability.enabled ? "outline" : "secondary"}
                              disabled={savingGovernance === capability.code}
                              onClick={() => void handleGovernanceChange(capability, !capability.enabled, capability.governance_availability || "AVAILABLE", capability.priority ?? 100)}
                            >
                              {capability.enabled ? "Enabled" : "Disabled"}
                            </Button>
                          ) : null}
                        </div>
                        {savingBinding === capability.code ? <RefreshCw className="size-3.5 animate-spin text-muted-foreground" /> : dirty ? <span className="text-[11px] text-amber-600">Saving on blur…</span> : <span className="text-[11px] text-emerald-600">Saved</span>}
                        {capability.governed ? <span className="text-[11px] text-muted-foreground">priority {capability.priority ?? 100} · {capability.freshness || "unknown freshness"}</span> : null}
                      </div>
                    </div>
                  )
                })}
                {!filteredRouteRows.length && !loading ? (
                  <div className="p-6 text-center text-sm text-muted-foreground">
                    No AI capabilities match the current filters.
                  </div>
                ) : null}
              </div>
                </div>
              </TabsContent>
              <TabsContent value="tools">
                <div className="overflow-x-auto rounded-lg bg-muted/25">
                  <div className="min-w-[980px]">
                    <div className="grid grid-cols-[minmax(210px,1.1fr)_minmax(150px,0.8fr)_minmax(280px,1.6fr)_minmax(160px,0.8fr)_minmax(190px,0.9fr)] gap-3 border-b border-border/50 px-3 py-2 text-xs font-medium text-muted-foreground">
                      <div>Agent tool</div>
                      <div>Group</div>
                      <div>Description</div>
                      <div>Safety</div>
                      <div>Runtime / source</div>
                    </div>
                    <div className="divide-y divide-border/50">
                      {filteredAgentTools.map((tool) => {
                        return (
                          <div key={`${tool.kind}-${tool.code}`} className="grid grid-cols-[minmax(210px,1.1fr)_minmax(150px,0.8fr)_minmax(280px,1.6fr)_minmax(160px,0.8fr)_minmax(190px,0.9fr)] items-start gap-3 px-3 py-3">
                            <div className="min-w-0">
                              <div className="flex items-center gap-2">
                                <span className="truncate font-medium">{tool.name}</span>
                                <Badge variant="outline" className="shrink-0 text-[10px]">
                                  {tool.kind === "both" ? "Tool · governed" : tool.kind === "capability" ? "Capability" : "Tool"}
                                </Badge>
                              </div>
                              <div className="truncate font-mono text-xs text-muted-foreground">{tool.code}</div>
                            </div>
                            <div className="min-w-0">
                              <div className="truncate text-sm">{tool.routeGroup}</div>
                              <div className="truncate text-xs text-muted-foreground">{tool.module}</div>
                            </div>
                            <div className="min-w-0 text-sm leading-5 text-muted-foreground">{tool.description}</div>
                            <div className="flex flex-wrap gap-1">
                              <Badge variant={tool.readOnly ? "outline" : "secondary"}>{tool.readOnly ? "Read only" : "Writes data"}</Badge>
                              {tool.requiresConfirmation ? <Badge variant="secondary">Confirmation</Badge> : null}
                              <Badge variant={tool.availability === "AVAILABLE" ? "outline" : "secondary"}>{tool.availability || "UNKNOWN"}</Badge>
                            </div>
                            <div className="min-w-0 text-xs text-muted-foreground">
                              <div className="truncate">{tool.runtimeEntry || "Agent runtime"}</div>
                              <div className="mt-1 truncate">{tool.source}</div>
                              <div className="mt-1">Freshness: {tool.freshness || "unknown"}</div>
                            </div>
                          </div>
                        )
                      })}
                      {!filteredAgentTools.length && !loading ? (
                          <div className="p-6 text-center text-sm text-muted-foreground">
                          No Agent tools or capabilities match the current filters.
                        </div>
                      ) : null}
                    </div>
                  </div>
                </div>
              </TabsContent>
            </Tabs>
          </section>

          <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
            <DialogContent className="w-[calc(100vw-2rem)] sm:!max-w-[88rem] max-h-[calc(100vh-2rem)] overflow-y-auto">
              <DialogHeader>
                <DialogTitle>{dialogMode === "create" ? "New provider" : "Edit provider"}</DialogTitle>
                <DialogDescription>
                  Configure the model endpoint and credentials used by AI capabilities.
                </DialogDescription>
              </DialogHeader>

              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                <div className="space-y-2">
                  <Label>Code</Label>
                  <Input value={draft.code} onChange={(e) => setDraft({ ...draft, code: e.target.value })} placeholder="openai-main" />
                </div>
                <div className="space-y-2">
                  <Label>Name</Label>
                  <Input value={draft.name} onChange={(e) => setDraft({ ...draft, name: e.target.value })} placeholder="OpenAI Main" />
                </div>
                <div className="space-y-2">
                  <Label>Provider type</Label>
                  <Select value={draft.provider_type} onValueChange={handleProviderTypeChange}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {providerTypes.map((providerType) => (
                        <SelectItem key={providerType.code} value={providerType.code}>
                          {providerType.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>Default model</Label>
                  <Select
                    value={draft.default_model || "none"}
                    onValueChange={(value) => setDraft({ ...draft, default_model: value === "none" ? "" : value })}
                    disabled={!draft.model_configs.length}
                  >
                    <SelectTrigger><SelectValue placeholder="Add a model first" /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="none">No default model</SelectItem>
                      {uniq(draft.model_configs.map((item) => item.model)).filter(Boolean).map((model) => (
                        <SelectItem key={model} value={model}>{model}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2 md:col-span-2 xl:col-span-4">
                  <div className="flex items-center justify-between gap-3">
                    <Label>Models</Label>
                    <Badge variant="secondary" className="font-mono text-[11px] tabular-nums">
                      {draft.model_configs.length}
                    </Badge>
                  </div>
                  <div className="overflow-hidden rounded-lg border bg-card">
                    <div className="grid lg:grid-cols-[minmax(0,1.1fr)_minmax(20rem,0.9fr)]">
                      <div className="min-w-0 border-b lg:border-b-0 lg:border-r">
                        <div className="flex items-center justify-between border-b bg-muted/40 px-3 py-2 text-[11px] font-medium text-muted-foreground">
                          <span>Model</span>
                          <span>Input / 1M · Output / 1M</span>
                        </div>
                        <div className="max-h-64 overflow-y-auto p-1.5">
                          {draft.model_configs.length ? draft.model_configs.map((config, index) => (
                            <div key={`${config.model}-${index}`} className="flex items-center gap-1">
                              <button
                                type="button"
                                className={cn(
                                  "flex min-w-0 flex-1 items-center justify-between gap-3 rounded-md px-2.5 py-2 text-left text-sm transition-colors",
                                  selectedModelIndex === index ? "bg-muted font-medium text-foreground" : "text-muted-foreground hover:bg-muted/60 hover:text-foreground",
                                )}
                                aria-pressed={selectedModelIndex === index}
                                onClick={() => setSelectedModelIndex(index)}
                              >
                                <span className="min-w-0 truncate">{config.model || "Unnamed model"}</span>
                                <span className="shrink-0 font-mono text-[11px] tabular-nums text-muted-foreground">
                                  {config.input_price_per_million} · {config.output_price_per_million}
                                </span>
                              </button>
                              <Button
                                type="button"
                                variant="ghost"
                                size="icon-sm"
                                className="shrink-0"
                                aria-label={`Remove ${config.model || "model"}`}
                                title="Remove model"
                                onClick={() => {
                                  const nextModels = draft.model_configs.filter((_, itemIndex) => itemIndex !== index)
                                  setDraft({
                                    ...draft,
                                    model_configs: nextModels,
                                    default_model: config.model === draft.default_model ? nextModels[0]?.model || "" : draft.default_model,
                                  })
                                  setSelectedModelIndex((currentIndex) => {
                                    if (!nextModels.length) return 0
                                    if (currentIndex > index) return currentIndex - 1
                                    return Math.min(currentIndex, nextModels.length - 1)
                                  })
                                }}
                              >
                                <X className="size-4" />
                              </Button>
                            </div>
                          )) : (
                            <div className="px-2.5 py-5 text-sm text-muted-foreground">No models configured</div>
                          )}
                        </div>
                      </div>
                      <div className="min-w-0 p-3 sm:p-4">
                        {selectedModelConfig ? (
                          <div className="space-y-3">
                            <div className="flex items-center justify-between gap-3">
                              <div className="min-w-0">
                                <p className="text-[11px] font-medium text-muted-foreground">Selected model</p>
                                <p className="truncate text-sm font-medium">{selectedModelConfig.model || "Unnamed model"}</p>
                              </div>
                              {selectedModelConfig.model === draft.default_model ? <Badge variant="secondary">Default</Badge> : null}
                            </div>
                            <div className="space-y-2">
                              <Label htmlFor={`provider-model-${selectedModelIndex}`}>Model</Label>
                              <Input
                                id={`provider-model-${selectedModelIndex}`}
                                className="h-8 min-w-0"
                                value={selectedModelConfig.model}
                                onChange={(event) => setDraft((current) => ({
                                  ...current,
                                  default_model: current.default_model === selectedModelConfig.model ? event.target.value : current.default_model,
                                  model_configs: current.model_configs.map((item, itemIndex) => itemIndex === selectedModelIndex ? { ...item, model: event.target.value } : item),
                                }))}
                                placeholder="Model name"
                              />
                            </div>
                            <div className="grid gap-3 sm:grid-cols-2">
                              <div className="space-y-2">
                                <Label htmlFor={`provider-input-price-${selectedModelIndex}`}>Input / 1M</Label>
                                <Input
                                  id={`provider-input-price-${selectedModelIndex}`}
                                  className="h-8 font-mono tabular-nums"
                                  inputMode="decimal"
                                  value={selectedModelConfig.input_price_per_million}
                                  onChange={(event) => setDraft((current) => ({
                                    ...current,
                                    model_configs: current.model_configs.map((item, itemIndex) => itemIndex === selectedModelIndex ? { ...item, input_price_per_million: event.target.value } : item),
                                  }))}
                                  placeholder="0"
                                />
                              </div>
                              <div className="space-y-2">
                                <Label htmlFor={`provider-output-price-${selectedModelIndex}`}>Output / 1M</Label>
                                <Input
                                  id={`provider-output-price-${selectedModelIndex}`}
                                  className="h-8 font-mono tabular-nums"
                                  inputMode="decimal"
                                  value={selectedModelConfig.output_price_per_million}
                                  onChange={(event) => setDraft((current) => ({
                                    ...current,
                                    model_configs: current.model_configs.map((item, itemIndex) => itemIndex === selectedModelIndex ? { ...item, output_price_per_million: event.target.value } : item),
                                  }))}
                                  placeholder="0"
                                />
                              </div>
                            </div>
                          </div>
                        ) : (
                          <div className="flex min-h-36 items-center justify-center text-sm text-muted-foreground">Add a model to configure pricing</div>
                        )}
                      </div>
                    </div>
                    <div className="flex gap-2 border-t bg-muted/20 p-2">
                      <Input
                        className="h-8 min-w-0"
                        value={newModelInput}
                        onChange={(e) => setNewModelInput(e.target.value)}
                        placeholder="Add model name"
                        onKeyDown={(e) => {
                          if (e.key === "Enter") {
                            e.preventDefault()
                            const val = newModelInput.trim()
                            if (val && !draft.model_configs.some((item) => item.model === val)) {
                              setDraft({
                                ...draft,
                                model_configs: [...draft.model_configs, { model: val, input_price_per_million: draft.input_price_per_million, output_price_per_million: draft.output_price_per_million }],
                                default_model: draft.default_model || val,
                              })
                              setSelectedModelIndex(draft.model_configs.length)
                            }
                            setNewModelInput("")
                          }
                        }}
                      />
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        className="shrink-0"
                        onClick={() => {
                          const val = newModelInput.trim()
                          if (val && !draft.model_configs.some((item) => item.model === val)) {
                            setDraft({
                              ...draft,
                              model_configs: [...draft.model_configs, { model: val, input_price_per_million: draft.input_price_per_million, output_price_per_million: draft.output_price_per_million }],
                              default_model: draft.default_model || val,
                            })
                            setSelectedModelIndex(draft.model_configs.length)
                          }
                          setNewModelInput("")
                        }}
                      >
                        <Plus className="size-4" />
                        Add
                      </Button>
                    </div>
                  </div>
                </div>
                <div className="space-y-2 md:col-span-2 xl:col-span-4">
                  <Label>Base URL</Label>
                  <Input value={draft.base_url} onChange={(e) => setDraft({ ...draft, base_url: e.target.value })} placeholder="https://api.openai.com" />
                </div>
                <div className="space-y-2">
                  <Label>API key env</Label>
                  <Input value={draft.api_key_env} onChange={(e) => setDraft({ ...draft, api_key_env: e.target.value })} placeholder="OPENAI_API_KEY" />
                </div>
                <div className="space-y-2">
                  <Label>API key</Label>
                  <Input
                    type="password"
                    value={draft.api_key}
                    onChange={(e) => setDraft({ ...draft, api_key: e.target.value })}
                    placeholder={dialogMode === "edit" ? selectedProvider?.api_key_masked || "leave blank to keep" : "optional if env is set"}
                  />
                </div>
                <div className="space-y-2">
                  <Label>Group ID env</Label>
                  <Input value={draft.group_id_env} onChange={(e) => setDraft({ ...draft, group_id_env: e.target.value })} placeholder="MINIMAX_GROUP_ID" />
                </div>
                <div className="space-y-2">
                  <Label>Timeout seconds</Label>
                  <Input value={draft.timeout_seconds} onChange={(e) => setDraft({ ...draft, timeout_seconds: e.target.value })} />
                </div>
                <div className="space-y-2">
                  <Label>Input price / 1M tokens</Label>
                  <Input inputMode="decimal" value={draft.input_price_per_million} onChange={(e) => setDraft({ ...draft, input_price_per_million: e.target.value })} placeholder="0.00" />
                </div>
                <div className="space-y-2">
                  <Label>Output price / 1M tokens</Label>
                  <Input inputMode="decimal" value={draft.output_price_per_million} onChange={(e) => setDraft({ ...draft, output_price_per_million: e.target.value })} placeholder="0.00" />
                </div>
                <div className="space-y-2">
                  <Label>Currency</Label>
                  <Input value={draft.pricing_currency} onChange={(e) => setDraft({ ...draft, pricing_currency: e.target.value.toUpperCase() })} placeholder="USD" />
                </div>
                <div className="space-y-2">
                  <Label>Model reasoning effort</Label>
                  <Select
                    value={draft.reasoning_effort || "none"}
                    onValueChange={(value) => setDraft({ ...draft, reasoning_effort: value === "none" ? "" : value })}
                  >
                    <SelectTrigger><SelectValue placeholder="Provider default" /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="none">Provider default</SelectItem>
                      {(["low", "medium", "high"] as const).map((effort) => (
                        <SelectItem key={effort} value={effort}>{effort}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <DialogFooter>
                {dialogMode === "edit" && draft.id ? (
                  <Button variant="destructive" onClick={() => void handleDeleteProvider()} disabled={saving}>
                    <Trash2 className="size-4" />
                    Delete
                  </Button>
                ) : null}
                <Button onClick={() => void handleSaveProvider()} disabled={saving}>
                  <Save className="size-4" />
                  Save provider
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>
      </div>
    </WorkspaceShell>
  )
}

function RouteStat({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof Route
  label: string
  value: string
}) {
  return (
    <div className="rounded-md bg-white px-3 py-2">
      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        <Icon className="size-3.5" />
        {label}
      </div>
      <div className="mt-1 text-base font-semibold">{value}</div>
    </div>
  )
}

function routeIssues(provider: AiProviderItem | null, model: string) {
  const issues: { level: "warning" | "error"; message: string }[] = []
  if (!provider) {
    issues.push({ level: "warning", message: "No provider selected; runtime will fall back." })
    return issues
  }
  if (!provider.is_enabled) {
    issues.push({ level: "error", message: "Provider is disabled." })
  }
  if (!provider.api_key_masked && !provider.api_key_env) {
    issues.push({ level: "warning", message: "No API key or env configured." })
  }
  if (!model.trim()) {
    issues.push({ level: "error", message: "Model is empty." })
  }
  const knownModels = providerModelNames(provider)
  if (model.trim() && knownModels.length > 0 && !knownModels.includes(model.trim())) {
    issues.push({ level: "error", message: "Model is not listed on this provider." })
  }
  return issues
}

function uniq(items: string[]) {
  return Array.from(new Set(items.map((item) => item.trim()).filter(Boolean)))
}
