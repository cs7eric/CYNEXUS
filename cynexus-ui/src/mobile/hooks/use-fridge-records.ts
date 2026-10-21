import { useCallback, useEffect, useSyncExternalStore } from "react"

import { useWorkspace } from "@/layout/workspace-provider"
import { adjustFridgeItemRemote, confirmFridgeItemMappingRemote, createFridgeItemRemote, getFridgeSnapshot, subscribeFridgeRecords, syncFridgeRecords, updateFridgeRecords } from "@/services/fridge/local-records"
import type { FridgeAddItemRequest, FridgeAdjustRequest, FridgeMappingRequest } from "@/services/fridge/api"
import type { FridgeRecords } from "@/services/fridge/model"

export function useFridgeRecords() {
  const { workspaceId } = useWorkspace()
  const subscribe = useCallback((listener: () => void) => subscribeFridgeRecords(workspaceId, listener), [workspaceId])
  const getSnapshot = useCallback(() => getFridgeSnapshot(workspaceId), [workspaceId])
  const snapshot = useSyncExternalStore(subscribe, getSnapshot, getSnapshot)
  const update = useCallback((change: (data: FridgeRecords) => FridgeRecords) => updateFridgeRecords(workspaceId, change), [workspaceId])
  useEffect(() => {
    const controller = new AbortController()
    void syncFridgeRecords(workspaceId, controller.signal)
    return () => controller.abort()
  }, [workspaceId])
  const createRemote = useCallback((input: FridgeAddItemRequest, signal?: AbortSignal) => createFridgeItemRemote(workspaceId, input, signal), [workspaceId])
  const adjustRemote = useCallback((itemId: string, input: FridgeAdjustRequest, signal?: AbortSignal) => adjustFridgeItemRemote(workspaceId, itemId, input, signal), [workspaceId])
  const confirmMappingRemote = useCallback((itemId: string, input: FridgeMappingRequest, signal?: AbortSignal) => confirmFridgeItemMappingRemote(workspaceId, itemId, input, signal), [workspaceId])
  const refresh = useCallback((signal?: AbortSignal) => syncFridgeRecords(workspaceId, signal), [workspaceId])
  return { ...snapshot, update, createRemote, adjustRemote, confirmMappingRemote, refresh }
}
