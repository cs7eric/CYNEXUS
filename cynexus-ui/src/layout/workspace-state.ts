import { create } from "zustand"

import { isWorkspaceId, validWorkspaceLocation, workspaces, type WorkspaceId } from "@/config/workspaces"

const STORAGE_KEY = "cynexus-workspaces-v1"

interface WorkspacePreferences {
  activeWorkspace: WorkspaceId
  lastLocations: Record<WorkspaceId, string>
}

function readPreferences(): WorkspacePreferences {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? "null")
    return {
      activeWorkspace: isWorkspaceId(saved?.activeWorkspace) ? saved.activeWorkspace : "personal",
      lastLocations: {
        personal: validWorkspaceLocation(saved?.lastLocations?.personal, "personal"),
        development: validWorkspaceLocation(saved?.lastLocations?.development, "development"),
      },
    }
  } catch {
    return {
      activeWorkspace: "personal",
      lastLocations: { personal: workspaces.personal.defaultPath, development: workspaces.development.defaultPath },
    }
  }
}

export const useWorkspaceState = create<WorkspacePreferences & {
  recordLocation: (workspace: WorkspaceId, url: string) => void
}>((set) => ({
  ...readPreferences(),
  recordLocation: (workspace, url) => set((current) => {
    if (current.activeWorkspace === workspace && current.lastLocations[workspace] === url) return current
    const next: WorkspacePreferences = {
      activeWorkspace: workspace,
      lastLocations: { ...current.lastLocations, [workspace]: url },
    }
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    } catch {
      // Navigation remains usable when browser storage is unavailable.
    }
    return next
  }),
}))
