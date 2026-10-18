/* eslint-disable react-refresh/only-export-components */
import { useEffect, type ReactNode } from "react"
import { useLocation, useNavigate } from "react-router-dom"

import { getWorkspaceForPath, isWorkspaceId, validWorkspaceLocation, workspaces, type WorkspaceId } from "@/config/workspaces"
import { useWorkspaceState } from "@/layout/workspace-state"
import { isMobilePath } from "@/mobile/paths"

export function useWorkspace() {
  const location = useLocation()
  const navigate = useNavigate()
  const storedWorkspace = useWorkspaceState((state) => state.activeWorkspace)
  const owner = getWorkspaceForPath(location.pathname)
  const historyWorkspace: unknown = location.state?.workspace
  const id = owner && owner !== "shared" ? owner : isWorkspaceId(historyWorkspace) ? historyWorkspace : storedWorkspace

  function switchWorkspace(next: WorkspaceId) {
    if (next === id) return
    const destination = isMobilePath(location.pathname)
      ? `${location.pathname}${location.search}${location.hash}`
      : validWorkspaceLocation(useWorkspaceState.getState().lastLocations[next], next)
    // Commit space preferences only after router blockers allow navigation.
    void navigate(destination, { state: { workspace: next } })
  }

  return { workspace: workspaces[id], workspaceId: id, switchWorkspace }
}

export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const location = useLocation()
  const navigate = useNavigate()
  const { workspaceId } = useWorkspace()
  const recordLocation = useWorkspaceState((state) => state.recordLocation)
  const url = `${location.pathname}${location.search}${location.hash}`

  useEffect(() => {
    recordLocation(workspaceId, url)
    if (getWorkspaceForPath(location.pathname) === "shared" && !isWorkspaceId(location.state?.workspace)) {
      void navigate(url, { replace: true, state: { ...location.state, workspace: workspaceId } })
    }
  }, [workspaceId, url, recordLocation, location.pathname, location.state, navigate])

  return children
}
