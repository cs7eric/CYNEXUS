import { useCallback, useEffect, useMemo, useState } from "react"
import { AlertCircle, Cpu, Loader2, Sparkles } from "lucide-react"

import { cn } from "@/lib/utils"
import { notification } from "@/components/ui/notification"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { fetchAiBindings, fetchAiProviders, updateAiBindingModel, updateAiBindingProvider, AI_BINDING_CHANGED_EVENT, type AiBindingItem, type AiProviderItem } from "@/services/ai/provider"

interface AiBindingProviderSelectProps {
  capability: string
  className?: string
  label?: string
  modelLabel?: string
}

function providerLabel(provider: AiProviderItem) {
  return provider.name || provider.code || "Unnamed provider"
}

function providerModels(provider: AiProviderItem | null | undefined, selectedModel: string) {
  if (!provider) return selectedModel ? [selectedModel] : []
  return Array.from(new Set([
    ...provider.model_configs.map((item) => item.model),
    ...provider.models,
    provider.default_model,
    selectedModel,
  ].map((model) => model.trim()).filter(Boolean)))
}

export function AiBindingProviderSelect({ capability, className, label = "Provider", modelLabel = "Model" }: AiBindingProviderSelectProps) {
  const [binding, setBinding] = useState<AiBindingItem | null>(null)
  const [providers, setProviders] = useState<AiProviderItem[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [nextBindings, nextProviders] = await Promise.all([fetchAiBindings(), fetchAiProviders()])
      setBinding(nextBindings.find((item) => item.capability === capability) || null)
      setProviders(nextProviders)
      setError(null)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause))
    } finally {
      setLoading(false)
    }
  }, [capability])

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0)
    return () => window.clearTimeout(timer)
  }, [load])

  useEffect(() => {
    const onBindingChanged = (event: Event) => {
      const next = (event as CustomEvent<AiBindingItem>).detail
      if (next?.capability === capability) setBinding(next)
    }
    const onVisibilityChange = () => {
      if (!document.hidden) void load()
    }
    window.addEventListener(AI_BINDING_CHANGED_EVENT, onBindingChanged)
    document.addEventListener("visibilitychange", onVisibilityChange)
    return () => {
      window.removeEventListener(AI_BINDING_CHANGED_EVENT, onBindingChanged)
      document.removeEventListener("visibilitychange", onVisibilityChange)
    }
  }, [capability, load])

  const currentProvider = binding?.provider
  const availableModels = useMemo(
    () => providerModels(currentProvider, binding?.model_override || ""),
    [binding?.model_override, currentProvider],
  )
  const availableProviders = useMemo(() => {
    const enabled = providers.filter((provider) => provider.is_enabled)
    if (currentProvider && !enabled.some((provider) => provider.id === currentProvider.id)) {
      return [currentProvider, ...enabled]
    }
    return enabled
  }, [currentProvider, providers])

  async function handleChange(value: string) {
    if (!binding && value === "auto") return
    setSaving(true)
    try {
      const nextProvider = value === "auto" ? null : providers.find((provider) => provider.id === value) || null
      const next = await updateAiBindingProvider(capability, value === "auto" ? "" : value, nextProvider?.default_model || "")
      setBinding(next)
      setError(null)
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : String(cause)
      setError(message)
      notification.danger({ title: "Provider route update failed", description: message })
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <span className={cn("inline-flex h-7 min-w-24 items-center gap-1.5 rounded-md bg-muted/40 px-2 text-[11px] text-muted-foreground", className)} aria-label={`${label} loading`}><Loader2 className="size-3 animate-spin" />Loading</span>
  }

  if (error && !binding) {
    return <span className={cn("inline-flex h-7 max-w-52 items-center gap-1.5 truncate rounded-md bg-rose-500/10 px-2 text-[11px] text-rose-700 dark:text-rose-300", className)} title={error}><AlertCircle className="size-3 shrink-0" />{label} unavailable</span>
  }

  return <div className={cn("inline-flex min-w-0 items-center", className)} title={error || undefined}>
    <Select value={binding?.provider_id || "auto"} onValueChange={(value) => void handleChange(value)} disabled={saving || !availableProviders.length}>
      <SelectTrigger size="sm" aria-label={`${label} for ${capability}`} className="h-7 min-w-0 max-w-[10.5rem] gap-1.5 border-0 bg-transparent px-2 text-xs shadow-none">
        {saving ? <Loader2 className="size-3.5 animate-spin" /> : <Cpu className="size-3.5" />}
        <SelectValue placeholder={label} />
      </SelectTrigger>
      <SelectContent align="start">
        <SelectItem value="auto">Default provider</SelectItem>
        {availableProviders.map((provider) => <SelectItem key={provider.id} value={provider.id}>{providerLabel(provider)}{provider.is_enabled ? "" : " (disabled)"}</SelectItem>)}
      </SelectContent>
    </Select>
    <Select
      value={binding?.model_override || currentProvider?.default_model || ""}
      onValueChange={(value) => {
        setSaving(true)
        void updateAiBindingModel(capability, value)
          .then((next) => {
            setBinding(next)
            setError(null)
          })
          .catch((cause) => {
            const message = cause instanceof Error ? cause.message : String(cause)
            setError(message)
            notification.danger({ title: "Model route update failed", description: message })
          })
          .finally(() => setSaving(false))
      }}
      disabled={saving || !currentProvider || !availableModels.length}
    >
      <SelectTrigger size="sm" aria-label={`${modelLabel} for ${capability}`} className="h-7 min-w-0 max-w-[11rem] gap-1.5 border-0 bg-transparent px-2 text-xs shadow-none">
        {saving ? <Loader2 className="size-3.5 animate-spin" /> : <Sparkles className="size-3.5" />}
        <SelectValue placeholder={modelLabel} />
      </SelectTrigger>
      <SelectContent align="start">
        {availableModels.map((model) => <SelectItem key={model} value={model}>{model}</SelectItem>)}
      </SelectContent>
    </Select>
    {error ? <AlertCircle className="ml-1 size-3.5 shrink-0 text-rose-600" aria-label={error} /> : null}
  </div>
}
