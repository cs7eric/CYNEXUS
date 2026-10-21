import { Activity, Apple, ArrowLeftRight, ChartNoAxesCombined, Dumbbell, House, LayoutGrid, ReceiptText, Refrigerator, Ruler, UserRound, WalletCards } from "lucide-react"

import { sharedNavigation, workspaces, type WorkspaceId } from "@/config/workspaces"

const workoutNavigationItem = { href: "/m/workout", label: "Workout", icon: Dumbbell }
const foodNavigationItem = { href: "/m/food", label: "Food", icon: Apple }

export const mobileNavigation = [
  { href: "/m", label: "Home", icon: House },
  { href: "/m/apps", label: "Apps", icon: LayoutGrid },
  { href: "/m/finance", label: "Finance", icon: WalletCards },
  workoutNavigationItem,
  foodNavigationItem,
  { href: "/m/me", label: "Profile", icon: UserRound },
] as const

export const mobileFeatureApps = [workoutNavigationItem, foodNavigationItem] as const

export const mobileAppNavigation = {
  "/m/finance": {
    mode: "views",
    items: [
      { href: "#finance-overview", label: "Overview", icon: ChartNoAxesCombined },
      { href: "#finance-accounts", label: "Accounts", icon: WalletCards },
      { href: "#finance-activity", label: "Activity", icon: ArrowLeftRight },
    ],
  },
  "/m/workout": {
    mode: "views",
    items: [
      { href: "#workout-overview", label: "Overview", icon: Activity },
      { href: "#workout-activity", label: "Activity", icon: Dumbbell },
      { href: "#workout-body", label: "Body", icon: Ruler },
      { href: "#workout-history", label: "History", icon: ReceiptText },
    ],
  },
  "/m/food": {
    mode: "views",
    items: [
      { href: "#food-today", label: "Today", icon: Apple },
      { href: "#food-fridge", label: "Fridge", icon: Refrigerator },
      { href: "#food-foods", label: "Foods", icon: LayoutGrid },
      { href: "#food-history", label: "History", icon: ChartNoAxesCombined },
      { href: "#food-goals", label: "Goals", icon: Activity },
    ],
  },
} as const

export function normalizeMobileAppPath(pathname: string) {
  const normalizedPath = pathname.replace(/\/+$/, "") || "/m"
  return normalizedPath === "/m/fridge" ? "/m/food" : normalizedPath
}

export function getMobileAppNavigation(pathname: string) {
  const normalizedPath = normalizeMobileAppPath(pathname)
  return mobileAppNavigation[normalizedPath as keyof typeof mobileAppNavigation]
}

export function getMobileTitle(pathname: string) {
  const normalizedPath = normalizeMobileAppPath(pathname)
  return [...mobileNavigation, ...mobileFeatureApps].find((item) => item.href === normalizedPath)?.label ?? "Page not found"
}

export function getMobileAppGroups(workspace: WorkspaceId) {
  return [sharedNavigation, ...workspaces[workspace].groups]
}
