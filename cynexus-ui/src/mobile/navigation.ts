import { House, LayoutGrid, UserRound } from "lucide-react"

import { sharedNavigation, workspaces, type WorkspaceId } from "@/config/workspaces"

export const mobileNavigation = [
  { href: "/m", label: "Home", icon: House },
  { href: "/m/apps", label: "Apps", icon: LayoutGrid },
  { href: "/m/me", label: "Profile", icon: UserRound },
] as const

export function getMobileTitle(pathname: string) {
  return mobileNavigation.find((item) => item.href === pathname.replace(/\/+$/, ""))?.label ?? "Page not found"
}

export function getMobileAppGroups(workspace: WorkspaceId) {
  return [sharedNavigation, ...workspaces[workspace].groups]
}
