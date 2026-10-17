"use client"

import { Check, ChevronsUpDown } from "lucide-react"

import { workspaces } from "@/config/workspaces"
import { useWorkspace } from "@/layout/workspace-provider"

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar"

export function WorkspaceSwitcher() {
  const { isMobile, setOpenMobile } = useSidebar()
  const { workspace, workspaceId, switchWorkspace } = useWorkspace()

  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <SidebarMenuButton
              size="lg"
              aria-label={`切换空间：${workspace.name}`}
              tooltip={workspace.name}
              className="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground"
            >
              <div className="flex aspect-square size-8 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground">
                <workspace.icon className="size-4" />
              </div>
              <div className="grid flex-1 text-left text-sm leading-tight">
                <span className="truncate font-medium">{workspace.name}</span>
                <span className="truncate text-xs text-muted-foreground">{workspace.subtitle}</span>
              </div>
              <ChevronsUpDown className="ml-auto" />
            </SidebarMenuButton>
          </DropdownMenuTrigger>
          <DropdownMenuContent
            className="w-(--radix-dropdown-menu-trigger-width) min-w-56 rounded-lg"
            align="start"
            side={isMobile ? "bottom" : "right"}
            sideOffset={4}
          >
            <DropdownMenuLabel className="text-xs text-muted-foreground">
              空间
            </DropdownMenuLabel>
            {Object.values(workspaces).map((space) => (
              <DropdownMenuItem
                key={space.id}
                onSelect={() => {
                  switchWorkspace(space.id)
                  if (isMobile) setOpenMobile(false)
                }}
                className="gap-2 p-2"
              >
                <div className="flex size-6 items-center justify-center rounded-md border">
                  <space.icon className="size-3.5 shrink-0" />
                </div>
                <span className="flex-1">{space.name}</span>
                {space.id === workspaceId ? <Check className="size-4" aria-label="当前空间" /> : null}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      </SidebarMenuItem>
    </SidebarMenu>
  )
}
