import { useCallback, useEffect, useRef, useState, useSyncExternalStore } from "react"

import { useWorkspace } from "@/layout/workspace-provider"
import { adjustFridgeItemRemote, confirmFridgeItemMappingRemote, createFridgeItemRemote, getFridgeSnapshot, subscribeFridgeRecords, syncFridgeRecords, updateFridgeRecords } from "@/services/fridge/local-records"
import type { FridgeAddItemRequest, FridgeAdjustRequest, FridgeMappingRequest } from "@/services/fridge/api"
import type { FridgeRecords } from "@/services/fridge/model"

export function useFridgeRecords() {
  const { workspaceId } = useWorkspace()
  const mounted = useRef(true)
  const [syncState, setSyncState] = useState<{ syncing: boolean; error: string | null }>({ syncing: true, error: null })
  const subscribe = useCallback((listener: () => void) => subscribeFridgeRecords(workspaceId, listener), [workspaceId])
  const getSnapshot = useCallback(() => getFridgeSnapshot(workspaceId), [workspaceId])
  const snapshot = useSyncExternalStore(subscribe, getSnapshot, getSnapshot)
  const update = useCallback((change: (data: FridgeRecords) => FridgeRecords) => updateFridgeRecords(workspaceId, change), [workspaceId])
  const refresh = useCallback(async (signal?: AbortSignal) => {
    if (mounted.current) setSyncState({ syncing: true, error: null })
    try {
      const result = await syncFridgeRecords(workspaceId, signal)
      if (mounted.current) setSyncState({ syncing: false, error: result.error })
      return result
    } catch (cause) {
      if (cause instanceof Error && cause.name === "AbortError") throw cause
      const message = cause instanceof Error ? cause.message : "Fridge service is unavailable."
      if (mounted.current) setSyncState({ syncing: false, error: message })
      throw cause
    }
  }, [workspaceId])
  useEffect(() => {
    mounted.current = true
    const controller = new AbortController()
    void refresh(controller.signal).catch(() => undefined)
    return () => {
      controller.abort()
      mounted.current = false
    }
  }, [refresh])
  const createRemote = useCallback((input: FridgeAddItemRequest, signal?: AbortSignal) => createFridgeItemRemote(workspaceId, input, signal), [workspaceId])
  const adjustRemote = useCallback((itemId: string, input: FridgeAdjustRequest, signal?: AbortSignal) => adjustFridgeItemRemote(workspaceId, itemId, input, signal), [workspaceId])
  const confirmMappingRemote = useCallback((itemId: string, input: FridgeMappingRequest, signal?: AbortSignal) => confirmFridgeItemMappingRemote(workspaceId, itemId, input, signal), [workspaceId])
  return { ...snapshot, update, createRemote, adjustRemote, confirmMappingRemote, refresh, syncing: syncState.syncing, syncError: syncState.error }
}
