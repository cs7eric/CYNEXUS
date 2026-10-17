import { Code2, House } from "lucide-react"

import { sidebarConfig } from "@/config/sidebar"

export type WorkspaceId = "personal" | "development"
export type NavigationItem = (typeof sidebarConfig.navMain)[number]

function menuItems(...urls: string[]): NavigationItem[] {
  return urls.map((url) => {
    const item = sidebarConfig.navMain.find((entry) => entry.url === url)
    if (!item) throw new Error(`Unknown navigation item: ${url}`)
    return item
  })
}

export const sharedNavigation = {
  id: "shared",
  title: "公共入口",
  items: menuItems("/cynexus-agent", "/knowledge-base/view"),
}

export const workspaces = {
  personal: {
    id: "personal",
    name: "个人空间",
    subtitle: "Personal",
    icon: House,
    defaultPath: "/",
    groups: [
      { id: "life", title: "日常生活", items: menuItems("/finance/overview") },
      { id: "media", title: "内容工具", items: menuItems("/mp4-to-word") },
      {
        id: "investing", title: "投资研究",
        items: menuItems("/trade-review", "/cynexus-screener", "/market/overview", "/stock-overview", "/stock-review", "/information"),
      },
      { id: "monitoring", title: "盘中监控", items: menuItems("/data-collector/market-indicator") },
    ],
  },
  development: {
    id: "development",
    name: "开发后台",
    subtitle: "Development",
    icon: Code2,
    defaultPath: "/settings/scheduler",
    groups: [
      { id: "overview", title: "运行总览", items: menuItems("/dashboard") },
      { id: "ai", title: "AI 管理", items: menuItems("/ai") },
      { id: "operations", title: "系统运行", items: menuItems("/settings/scheduler") },
      { id: "experiments", title: "开发实验", items: menuItems("/poc") },
    ],
  },
} as const

export function isWorkspaceId(value: unknown): value is WorkspaceId {
  return value === "personal" || value === "development"
}

function matchesPath(path: string, prefix: string) {
  return path === prefix || path.startsWith(`${prefix}/`)
}

// Submenu routes can live outside their parent's URL (Downloader, Portfolio).
const routeOwners = (Object.keys(workspaces) as WorkspaceId[]).flatMap((id) =>
  workspaces[id].groups.flatMap((group) => group.items.flatMap((item) =>
    [item.url, ...(item.items?.map((child) => child.url) ?? [])].map((url) => ({
      prefix: `/${url.split("/")[1]}`, id,
    })),
  )),
)

export function getWorkspaceForPath(url: string): WorkspaceId | "shared" | undefined {
  const path = url.split(/[?#]/)[0].replace(/\/+$/, "") || "/"
  if (path === "/" || matchesPath(path, "/knowledge-base") || matchesPath(path, "/cynexus-agent") || path === "/ai/agent") return "shared"
  if (path === "/heatmap-demo" || path === "/heatmap-data-debug") return "development"
  return routeOwners.find(({ prefix }) => matchesPath(path, prefix))?.id
}

export function isPathInWorkspace(url: string, workspace: WorkspaceId) {
  const owner = getWorkspaceForPath(url)
  return owner === "shared" || owner === workspace
}

export function validWorkspaceLocation(value: unknown, workspace: WorkspaceId): string {
  return typeof value === "string" && value.startsWith("/") && !value.startsWith("//") && !value.includes("\\") && isPathInWorkspace(value, workspace)
    ? value
    : workspaces[workspace].defaultPath
}
