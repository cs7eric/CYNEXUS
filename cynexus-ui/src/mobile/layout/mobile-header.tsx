import { Dropdown } from "@heroui/react/dropdown"
import { Check } from "lucide-react"
import { useState } from "react"

import { sidebarConfig } from "@/config/sidebar"
import { workspaces, type WorkspaceId } from "@/config/workspaces"
import { useWorkspace } from "@/layout/workspace-provider"
import { MobileAppSearch } from "@/mobile/components/app-search"
import { useMobilePortal } from "@/mobile/hooks/use-mobile-portal"
import { MobileButton } from "@/mobile/ui/button"

export function MobileHeader() {
  const { workspace, workspaceId, switchWorkspace } = useWorkspace()
  const portal = useMobilePortal()
  const [workspaceMenuOpen, setWorkspaceMenuOpen] = useState(false)
  const WorkspaceIcon = workspace.icon

  const workspaceControl = (
    <Dropdown isOpen={workspaceMenuOpen} onOpenChange={setWorkspaceMenuOpen}>
      <MobileButton isIconOnly variant="ghost" className="mobile-header-control" aria-label={`Switch workspace: ${workspace.name}`}>
        <span className="inline-flex" title={`Switch workspace: ${workspace.name}`}><WorkspaceIcon aria-hidden="true" /></span>
      </MobileButton>
      <Dropdown.Popover UNSTABLE_portalContainer={portal} className="mobile-space-menu" placement="bottom start" offset={10}>
        <p className="mobile-space-caption" aria-hidden="true">Workspace</p>
        <Dropdown.Menu aria-label="Choose workspace" selectionMode="single" selectedKeys={[workspaceId]}
          onAction={(key) => { switchWorkspace(key as WorkspaceId); setWorkspaceMenuOpen(false) }}>
          {(Object.keys(workspaces) as WorkspaceId[]).map((id) => {
            const option = workspaces[id]
            const Icon = option.icon
            return <Dropdown.Item key={id} id={id} textValue={option.name} className="mobile-space-option">
              <span className="mobile-space-icon"><Icon aria-hidden="true" /></span>
              <span className="mobile-space-name">{option.name}</span>
              <span className="mobile-space-selection" aria-hidden="true">{workspaceId === id && <Check />}</span>
            </Dropdown.Item>
          })}
        </Dropdown.Menu>
      </Dropdown.Popover>
    </Dropdown>
  )

  return (
    <header className="mobile-header">
      <div className="mobile-header-top mx-auto w-full max-w-2xl px-4">
        <div className="mobile-header-leading">
          {workspaceControl}
        </div>
        <div className="mobile-header-greeting"><h1>Hello, {sidebarConfig.user.name}</h1><p>Today {new Date().toLocaleDateString("en-GB", { day: "numeric", month: "short" })}.</p></div>
        <div className="mobile-header-actions">
          <MobileAppSearch />
        </div>
      </div>
    </header>
  )
}
