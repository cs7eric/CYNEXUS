"use client"

import * as React from "react"

import { sidebarConfig } from "@/config/sidebar"
import { sharedNavigation } from "@/config/workspaces"
import { useWorkspace } from "@/layout/workspace-provider"
import { NavMain } from "@/layout/nav-main"
import { NavUser } from "@/layout/nav-user"
import { WorkspaceSwitcher } from "@/layout/workspace-switcher"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarRail,
} from "@/components/ui/sidebar"

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  const { workspace, workspaceId } = useWorkspace()
  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarHeader>
        <WorkspaceSwitcher />
      </SidebarHeader>
      <SidebarContent className="scrollbar-none">
        <NavMain key={`${workspaceId}-shared`} items={[{ title: "首页", url: "/", icon: workspace.icon }, ...sharedNavigation.items]} label={sharedNavigation.title} storageKey={`${workspaceId}-shared`} />
        {workspace.groups.map((group) => (
          <NavMain key={`${workspaceId}-${group.id}`} items={group.items} label={group.title} storageKey={`${workspaceId}-${group.id}`} />
        ))}
      </SidebarContent>
      <SidebarFooter>
        <NavUser user={sidebarConfig.user} />
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}
