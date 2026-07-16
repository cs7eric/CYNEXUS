import { useCallback, useEffect, useMemo, useState } from "react"
import {
  AlertTriangle,
  BrainCircuit,
  CheckCircle2,
  Database,
  GitBranch,
  Plus,
  RefreshCw,
  Route,
  Save,
  Trash2,
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
import {
  type AiBindingItem,
  type AiCapability,
  type AiProviderItem,
  type AiProviderType,
  createAiProvider,
  deleteAiProvider,
  fetchAiBindings,
  fetchAiCapabilities,
  fetchAiProviderTypes,
  fetchAiProviders,
  updateAiProvider,
  upsertAiBinding,
} from "@/services/ai/provider"
import { cn } from "@/lib/utils"

// AI Provider frontend docs:
//   design/frontend/ai-provider.md
// Keep that document in sync when changing this settings UI, route behavior, or payload shape.

type ProviderDraft = {
  id?: string
  code: string
  name: string
  provider_type: string
  base_url: string
  default_model: string
  models: string[]
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
}

type BindingDraft = {
  providerId: string
  modelOverride: string
}

const emptyDraft: ProviderDraft = {
  code: "",
  name: "",
  provider_type: "openai_compatible",
  base_url: "",
  default_model: "",
  models: [],
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
}

function draftFromProvider(provider: AiProviderItem): ProviderDraft {
  return {
    id: provider.id,
    code: provider.code,
    name: provider.name,
    provider_type: provider.provider_type,
    base_url: provider.base_url,
    default_model: provider.default_model,
    models: provider.models || [],
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
  }
}

export default function AiProviderSettingsPage() {
  const [providers, setProviders] = useState<AiProviderItem[]>([])
  const [providerTypes, setProviderTypes] = useState<AiProviderType[]>([])
  const [bindings, setBindings] = useState<AiBindingItem[]>([])
  const [capabilities, setCapabilities] = useState<AiCapability[]>([])
  const [draft, setDraft] = useState<ProviderDraft>(emptyDraft)
  const [selectedProviderId, setSelectedProviderId] = useState("")
  const [dialogOpen, setDialogOpen] = useState(false)
  const [dialogMode, setDialogMode] = useState<"create" | "edit">("create")
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [savingBinding, setSavingBinding] = useState("")
  const [error, setError] = useState("")
  const [newModelInput, setNewModelInput] = useState("")
  const [bindingDrafts, setBindingDrafts] = useState<Record<string, BindingDraft>>({})
  const [bindingSaveTimers, setBindingSaveTimers] = useState<Record<string, number>>({})

  const selectedProvider = useMemo(
    () => providers.find((item) => item.id === selectedProviderId) || null,
    [providers, selectedProviderId],
  )

  const reload = useCallback(async () => {
    setLoading(true)
    setError("")
    try {
      const [nextCapabilities, nextProviders, nextBindings] = await Promise.all([
        fetchAiCapabilities(),
        fetchAiProviders(),
        fetchAiBindings(),
      ])
      const nextProviderTypes = await fetchAiProviderTypes()
      setCapabilities(nextCapabilities)
      setProviderTypes(nextProviderTypes)
      setProviders(nextProviders)
      setBindings(nextBindings)
      setBindingDrafts(
        Object.fromEntries(
          nextBindings.map((binding) => [
            binding.capability,
            {
              providerId: binding.provider_id || "",
              modelOverride: binding.model_override || "",
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
      const effectiveModel = modelOverride || provider?.default_model || ""
      const issues = routeIssues(provider, effectiveModel)
      const dirty = providerId !== (binding?.provider_id || "") || modelOverride !== (binding?.model_override || "")
      return { capability, binding, providerId, provider, modelOverride, effectiveModel, issues, dirty }
    }),
    [bindingByCapability, bindingDrafts, capabilities, providerById],
  )

  const issueCount = routeRows.filter((row) => row.issues.length > 0).length
  const activeBindingCount = routeRows.filter((row) => row.binding?.is_enabled).length

  async function handleSaveProvider() {
    if (!draft.code.trim() || !draft.name.trim()) {
      notification.danger({ title: "Provider 未保存", description: "Code 和名称不能为空" })
      return
    }
    setSaving(true)
    try {
      const payload = {
        ...draft,
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
    setDialogOpen(true)
  }

  function openEditDialog(provider: AiProviderItem) {
    setDialogMode("edit")
    setSelectedProviderId(provider.id)
    setDraft(draftFromProvider(provider))
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

  function scheduleBindingSave(capability: AiCapability, providerId: string, modelOverride: string) {
    const previous = bindingSaveTimers[capability.code]
    if (previous) window.clearTimeout(previous)
    const timer = window.setTimeout(() => {
      void handleBindingChange(capability, providerId, modelOverride)
      setBindingSaveTimers((current) => {
        const next = { ...current }
        delete next[capability.code]
        return next
      })
    }, 250)
    setBindingSaveTimers((current) => ({ ...current, [capability.code]: timer }))
  }

  async function handleBindingChange(capability: AiCapability, providerId: string, modelOverride: string) {
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
        is_enabled: true,
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
        },
      }))
      notification.success({ title: "绑定已保存", description: capability.label })
    } catch (err) {
      notification.danger({ title: "绑定保存失败", description: err instanceof Error ? err.message : "保存失败" })
    } finally {
      setSavingBinding("")
    }
  }

  return (
    <WorkspaceShell>
      <div className="flex w-full flex-col gap-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
              <BrainCircuit className="size-4" />
              Settings
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
          <section className="rounded-lg bg-[#f9f9f9] p-3">
            <div className="grid gap-2 md:grid-cols-4">
              <RouteStat icon={Route} label="Runtime bindings" value={`${activeBindingCount}/${capabilities.length}`} />
              <RouteStat icon={Database} label="Providers" value={String(providers.length)} />
              <RouteStat icon={AlertTriangle} label="Needs attention" value={String(issueCount)} />
              <RouteStat icon={GitBranch} label="Dynamic route" value="DB read" />
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
            <div className="overflow-hidden rounded-lg bg-[#f9f9f9]">
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

          <section className="rounded-lg bg-[#f9f9f9] p-3">
            <div className="mb-3 flex items-center justify-between gap-3">
              <div className="flex items-center gap-2 text-sm font-semibold">
                <CheckCircle2 className="size-4" />
                Capability routing
              </div>
              <Badge variant="secondary">{capabilities.length}</Badge>
            </div>
            <div className="overflow-x-auto rounded-lg bg-white/70">
              <div className="sticky top-0 z-10 grid min-w-[1120px] grid-cols-[minmax(240px,1.35fr)_minmax(170px,0.9fr)_minmax(210px,1fr)_minmax(210px,1fr)_minmax(190px,1fr)_108px] gap-3 border-b border-border/50 bg-white/95 px-3 py-2 text-xs font-medium text-muted-foreground">
                <div>Capability</div>
                <div>Group</div>
                <div>Provider</div>
                <div>Model</div>
                <div>Runtime / entry</div>
                <div className="text-right">Action</div>
              </div>
              <div className="divide-y divide-border/50">
                {routeRows.map((row) => {
                  const { capability, binding, providerId, provider, modelOverride, issues, dirty } = row
                  const providerModels = provider?.models || []
                  const availableModels = uniq([provider?.default_model || "", ...providerModels, modelOverride].filter(Boolean))
                  return (
                    <div key={capability.code} className="grid min-w-[1120px] grid-cols-[minmax(240px,1.35fr)_minmax(170px,0.9fr)_minmax(210px,1fr)_minmax(210px,1fr)_minmax(190px,1fr)_108px] items-start gap-3 px-3 py-3">
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
                      <div className="min-w-0">
                        <div className="truncate text-sm">{capability.group}</div>
                        <div className="mt-1 truncate text-xs text-muted-foreground">{capability.description}</div>
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
                            scheduleBindingSave(capability, nextProviderId, nextModel)
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
                            scheduleBindingSave(capability, providerId, value)
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
                          onBlur={(event) => scheduleBindingSave(capability, providerId, event.target.value)}
                          placeholder={provider?.default_model || "provider default"}
                        />
                      )}
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
                        <Badge variant={binding?.is_enabled ? "outline" : "secondary"}>
                          {binding?.is_enabled ? "active" : "fallback"}
                        </Badge>
                        {savingBinding === capability.code ? <RefreshCw className="size-3.5 animate-spin text-muted-foreground" /> : dirty ? <span className="text-[11px] text-amber-600">Saving on blur…</span> : <span className="text-[11px] text-emerald-600">Saved</span>}
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
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
                  <Input value={draft.default_model} onChange={(e) => setDraft({ ...draft, default_model: e.target.value })} placeholder="gpt-4.1-mini" />
                </div>
                <div className="space-y-2 md:col-span-2 xl:col-span-4">
                  <Label>Models</Label>
                  <div className="flex flex-wrap gap-2">
                    {draft.models.map((model) => (
                      <Badge key={model} variant="secondary" className="gap-1 pr-1">
                        {model}
                        <button
                          type="button"
                          className="ml-1 rounded-full p-0.5 hover:bg-muted-foreground/20"
                          onClick={() => setDraft({ ...draft, models: draft.models.filter((m) => m !== model) })}
                        >
                          <X className="size-3" />
                        </button>
                      </Badge>
                    ))}
                  </div>
                  <div className="flex gap-2">
                    <Input
                      value={newModelInput}
                      onChange={(e) => setNewModelInput(e.target.value)}
                      placeholder="Add model name"
                      onKeyDown={(e) => {
                        if (e.key === "Enter") {
                          e.preventDefault()
                          const val = newModelInput.trim()
                          if (val && !draft.models.includes(val)) {
                            setDraft({ ...draft, models: [...draft.models, val] })
                          }
                          setNewModelInput("")
                        }
                      }}
                    />
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => {
                        const val = newModelInput.trim()
                        if (val && !draft.models.includes(val)) {
                          setDraft({ ...draft, models: [...draft.models, val] })
                        }
                        setNewModelInput("")
                      }}
                    >
                      Add
                    </Button>
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
  const knownModels = uniq([provider.default_model || "", ...(provider.models || [])].filter(Boolean))
  if (model.trim() && knownModels.length > 0 && !knownModels.includes(model.trim())) {
    issues.push({ level: "error", message: "Model is not listed on this provider." })
  }
  return issues
}

function uniq(items: string[]) {
  return Array.from(new Set(items.map((item) => item.trim()).filter(Boolean)))
}
