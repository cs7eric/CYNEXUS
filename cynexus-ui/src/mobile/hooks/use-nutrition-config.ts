import { useCallback, useEffect, useState } from "react"

import {
  DEFAULT_NUTRITION_CONFIG,
  fetchNutritionConfig,
  persistNutritionConfig,
  type NutritionConfig,
} from "@/services/food/nutrition-config"
import { useWorkspace } from "@/layout/workspace-provider"

type NutritionConfigState = {
  config: NutritionConfig
  loading: boolean
  saving: boolean
  error: string | null
  stale: boolean
}

export function useNutritionConfig() {
  const { workspaceId } = useWorkspace()
  const [state, setState] = useState<NutritionConfigState>({
    config: { ...DEFAULT_NUTRITION_CONFIG },
    loading: true,
    saving: false,
    error: null,
    stale: false,
  })
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    void Promise.resolve()
      .then(() => {
        if (controller.signal.aborted) return null
        setState(previous => ({ ...previous, loading: true, error: null, stale: previous.error != null }))
        return fetchNutritionConfig(controller.signal)
      })
      .then(config => {
        if (config && !controller.signal.aborted) setState(previous => ({ ...previous, config, loading: false, error: null, stale: false }))
      })
      .catch(cause => {
        if (!controller.signal.aborted) setState(previous => ({ ...previous, loading: false, error: cause instanceof Error ? cause.message : "Unable to load nutrition configuration.", stale: true }))
      })
    return () => controller.abort()
  }, [workspaceId, reloadToken])

  const reload = useCallback(() => setReloadToken(value => value + 1), [])
  const save = useCallback(async (config: NutritionConfig) => {
    setState(previous => ({ ...previous, saving: true, error: null }))
    try {
      const saved = await persistNutritionConfig(config)
      setState(previous => ({ ...previous, config: saved, saving: false, error: null, stale: false }))
      return saved
    } catch (cause) {
      const error = cause instanceof Error ? cause.message : "Unable to save nutrition configuration."
      setState(previous => ({ ...previous, saving: false, error, stale: true }))
      throw cause
    }
  }, [])

  return { ...state, reload, save }
}
