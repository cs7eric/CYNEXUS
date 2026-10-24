import { useCallback, useEffect, useSyncExternalStore } from "react"
import { useWorkspace } from "@/layout/workspace-provider"
import { deleteWorkoutMeasurementRemote, deleteWorkoutSessionRemote, getWorkoutSnapshot, saveWorkoutMeasurementRemote, saveWorkoutPlanRemote, saveWorkoutSessionRemote, subscribeWorkoutRecords, syncWorkoutRecords, updateWorkoutRecords } from "@/services/workout/local-records"
import type { BodyObservationInput, WorkoutRecords, WorkoutSession } from "@/services/workout/model"

export function useWorkoutRecords() {
  const { workspaceId } = useWorkspace()
  const subscribe = useCallback((listener: () => void) => subscribeWorkoutRecords(workspaceId, listener), [workspaceId])
  const getSnapshot = useCallback(() => getWorkoutSnapshot(workspaceId), [workspaceId])
  const snapshot = useSyncExternalStore(subscribe, getSnapshot, getSnapshot)
  const update = useCallback((change: (data: WorkoutRecords) => WorkoutRecords) => updateWorkoutRecords(workspaceId, change), [workspaceId])
  useEffect(() => {
    const controller = new AbortController()
    void syncWorkoutRecords(workspaceId, controller.signal)
    return () => controller.abort()
  }, [workspaceId])
  const sync = useCallback((signal?: AbortSignal) => syncWorkoutRecords(workspaceId, signal), [workspaceId])
  const savePlanRemote = useCallback((cycleStart: string, signal?: AbortSignal) => saveWorkoutPlanRemote(workspaceId, cycleStart, signal), [workspaceId])
  const saveSessionRemote = useCallback((session: Omit<WorkoutSession, "id"> & { id?: string | number }, signal?: AbortSignal) => saveWorkoutSessionRemote(workspaceId, session, signal), [workspaceId])
  const saveMeasurementRemote = useCallback((observation: BodyObservationInput, signal?: AbortSignal) => saveWorkoutMeasurementRemote(workspaceId, observation, signal), [workspaceId])
  const deleteSessionRemote = useCallback((id: string | number, signal?: AbortSignal) => deleteWorkoutSessionRemote(workspaceId, id, signal), [workspaceId])
  const deleteMeasurementRemote = useCallback((id: string, signal?: AbortSignal) => deleteWorkoutMeasurementRemote(workspaceId, id, signal), [workspaceId])
  return { ...snapshot, update, sync, savePlanRemote, saveSessionRemote, saveMeasurementRemote, deleteSessionRemote, deleteMeasurementRemote }
}
