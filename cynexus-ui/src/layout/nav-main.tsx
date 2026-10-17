import * as React from "react"
import { Link, useLocation, useNavigate } from "react-router-dom"
import { ChevronRight, type LucideIcon } from "lucide-react"

import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible"
import {
  SidebarGroup,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
  useSidebar,
} from "@/components/ui/sidebar"

const STORAGE_KEY = "app-sidebar-open-groups"

export function NavMain({
  items,
  label = "Applications",
  storageKey = "default",
}: {
  label?: string
  storageKey?: string
  items: {
    title: string
    url: string
    icon?: LucideIcon
    isActive?: boolean
    items?: {
      title: string
      url: string
    }[]
  }[]
}) {
  const { state } = useSidebar()
  const location = useLocation()
  const navigate = useNavigate()
  const persistenceKey = `${STORAGE_KEY}:${storageKey}`

  const getInitialOpenState = React.useCallback(() => {
    if (typeof window === "undefined") {
      return Object.fromEntries(items.map((item) => [item.title, Boolean(item.isActive)]))
    }
    try {
      const raw = window.localStorage.getItem(persistenceKey)
      if (raw) {
        return {
          ...Object.fromEntries(items.map((item) => [item.title, Boolean(item.isActive)])),
          ...JSON.parse(raw),
        }
      }
    } catch {
      return Object.fromEntries(items.map((item) => [item.title, Boolean(item.isActive)]))
    }
    return Object.fromEntries(items.map((item) => [item.title, Boolean(item.isActive)]))
  }, [items, persistenceKey])

  const [openItems, setOpenItems] = React.useState<Record<string, boolean>>(getInitialOpenState)

  React.useEffect(() => {
    try {
      window.localStorage.setItem(persistenceKey, JSON.stringify(openItems))
    } catch {
      // Collapsing groups also works without browser storage.
    }
  }, [openItems, persistenceKey])

  return (
    <SidebarGroup>
      <SidebarGroupLabel>{label}</SidebarGroupLabel>
      <SidebarMenu>
        {items.map((item) => {
          const hasSubItems = Array.isArray(item.items) && item.items.length > 0
          const isActive =
            location.pathname === item.url || location.pathname.startsWith(`${item.url}/`) ||
            item.items?.some((subItem) => subItem.url === location.pathname || location.pathname.startsWith(`${subItem.url}/`))

          if (!hasSubItems) {
            return (
              <SidebarMenuItem key={item.title}>
                <SidebarMenuButton
                  tooltip={item.title}
                  isActive={isActive}
                  onClick={() => navigate(item.url)}
                >
                  {item.icon ? <item.icon /> : null}
                  <span>{item.title}</span>
                </SidebarMenuButton>
              </SidebarMenuItem>
            )
          }

          const isOpen = (openItems[item.title] ?? false) || isActive
          const showChevron = (
            <ChevronRight className="ml-auto transition-transform duration-200 group-data-[state=open]/collapsible:rotate-90 group-data-[collapsible=icon]:hidden" />
          )

          if (state === "collapsed") {
            return (
              <SidebarMenuItem key={item.title}>
                <SidebarMenuButton
                  tooltip={item.title}
                  isActive={isActive}
                  onClick={() => navigate(item.url)}
                >
                  {item.icon ? <item.icon /> : null}
                  <span>{item.title}</span>
                  {showChevron}
                </SidebarMenuButton>
              </SidebarMenuItem>
            )
          }

          return (
            <Collapsible
              key={item.title}
              asChild
              open={isOpen}
              onOpenChange={(open) => {
                setOpenItems((prev) => ({ ...prev, [item.title]: open }))
              }}
              className="group/collapsible"
            >
              <SidebarMenuItem>
                <CollapsibleTrigger asChild>
                  <SidebarMenuButton tooltip={item.title} isActive={isActive}>
                    {item.icon ? <item.icon /> : null}
                    <span>{item.title}</span>
                    {showChevron}
                  </SidebarMenuButton>
                </CollapsibleTrigger>
                <CollapsibleContent>
                  <SidebarMenuSub>
                    {item.items?.map((subItem) => (
                      <SidebarMenuSubItem key={subItem.title}>
                        <SidebarMenuSubButton asChild isActive={location.pathname === subItem.url}>
                          <Link to={subItem.url}>
                            <span>{subItem.title}</span>
                          </Link>
                        </SidebarMenuSubButton>
                      </SidebarMenuSubItem>
                    ))}
                  </SidebarMenuSub>
                </CollapsibleContent>
              </SidebarMenuItem>
            </Collapsible>
          )
        })}
      </SidebarMenu>
    </SidebarGroup>
  )
}
