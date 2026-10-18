import { Dumbbell, House, LayoutGrid, UserRound, WalletCards } from "lucide-react"

import { sharedNavigation, workspaces, type WorkspaceId } from "@/config/workspaces"

export const mobileNavigation = [
  { href: "/m", label: "Home", icon: House },
  { href: "/m/apps", label: "Apps", icon: LayoutGrid },
  { href: "/m/finance", label: "Finance", icon: WalletCards },
  { href: "/m/me", label: "Profile", icon: UserRound },
] as const

export const mobileFeatureApps = [
  { href: "/m/workout", label: "Workout", icon: Dumbbell },
] as const

export function getMobileTitle(pathname: string) {
  const normalizedPath = pathname.replace(/\/+$/, "") || "/m"
  return [...mobileNavigation, ...mobileFeatureApps].find((item) => item.href === normalizedPath)?.label ?? "Page not found"
}

export function getMobileAppGroups(workspace: WorkspaceId) {
  return [sharedNavigation, ...workspaces[workspace].groups]
}
