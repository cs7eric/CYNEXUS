import { useCallback, useSyncExternalStore } from "react"
import { useWorkspace } from "@/layout/workspace-provider"
import { getWorkoutSnapshot, subscribeWorkoutRecords, updateWorkoutRecords } from "@/services/workout/local-records"
import type { WorkoutRecords } from "@/services/workout/model"

export function useWorkoutRecords() {
  const { workspaceId } = useWorkspace()
  const subscribe = useCallback((listener: () => void) => subscribeWorkoutRecords(workspaceId, listener), [workspaceId])
  const getSnapshot = useCallback(() => getWorkoutSnapshot(workspaceId), [workspaceId])
  const snapshot = useSyncExternalStore(subscribe, getSnapshot, getSnapshot)
  const update = useCallback((change: (data: WorkoutRecords) => WorkoutRecords) => updateWorkoutRecords(workspaceId, change), [workspaceId])
  return { ...snapshot, update }
}
