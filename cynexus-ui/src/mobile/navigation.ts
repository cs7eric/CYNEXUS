import { Activity, ArrowLeftRight, ChartNoAxesCombined, Dumbbell, House, LayoutGrid, ReceiptText, Ruler, UserRound, WalletCards } from "lucide-react"

import { sharedNavigation, workspaces, type WorkspaceId } from "@/config/workspaces"

const workoutNavigationItem = { href: "/m/workout", label: "Workout", icon: Dumbbell }

export const mobileNavigation = [
  { href: "/m", label: "Home", icon: House },
  { href: "/m/apps", label: "Apps", icon: LayoutGrid },
  { href: "/m/finance", label: "Finance", icon: WalletCards },
  workoutNavigationItem,
  { href: "/m/me", label: "Profile", icon: UserRound },
] as const

export const mobileFeatureApps = [workoutNavigationItem] as const

export const mobileAppNavigation = {
  "/m/finance": [
    { href: "#finance-overview", label: "Overview", icon: ChartNoAxesCombined },
    { href: "#finance-accounts", label: "Accounts", icon: WalletCards },
    { href: "#finance-activity", label: "Activity", icon: ArrowLeftRight },
  ],
  "/m/workout": [
    { href: "#workout-overview", label: "Overview", icon: Activity },
    { href: "#workout-activity", label: "Activity", icon: Dumbbell },
    { href: "#workout-body", label: "Body", icon: Ruler },
    { href: "#workout-history", label: "History", icon: ReceiptText },
  ],
} as const

export function getMobileAppNavigation(pathname: string) {
  const normalizedPath = pathname.replace(/\/+$/, "") || "/m"
  return mobileAppNavigation[normalizedPath as keyof typeof mobileAppNavigation]
}

export function getMobileTitle(pathname: string) {
  const normalizedPath = pathname.replace(/\/+$/, "") || "/m"
  return [...mobileNavigation, ...mobileFeatureApps].find((item) => item.href === normalizedPath)?.label ?? "Page not found"
}

export function getMobileAppGroups(workspace: WorkspaceId) {
  return [sharedNavigation, ...workspaces[workspace].groups]
}
