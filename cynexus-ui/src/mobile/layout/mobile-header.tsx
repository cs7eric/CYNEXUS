import { Dropdown } from "@heroui/react/dropdown"
import { ArrowLeft, Check, RefreshCw } from "lucide-react"
import { useState } from "react"
import { Link, useLocation } from "react-router-dom"

import { sidebarConfig } from "@/config/sidebar"
import { workspaces, type WorkspaceId } from "@/config/workspaces"
import { useWorkspace } from "@/layout/workspace-provider"
import { MobileAppSearch } from "@/mobile/components/app-search"
import { useMobilePortal } from "@/mobile/hooks/use-mobile-portal"
import { getMobileAppNavigation, mobileFeatureApps, mobileNavigation } from "@/mobile/navigation"
import { MobileButton } from "@/mobile/ui/button"

export function MobileHeader({ onFinanceRefresh }: { onFinanceRefresh?: () => void }) {
  const { workspace, workspaceId, switchWorkspace } = useWorkspace()
  const location = useLocation()
  const portal = useMobilePortal()
  const [workspaceMenuOpen, setWorkspaceMenuOpen] = useState(false)
  const WorkspaceIcon = workspace.icon
  const mobilePath = location.pathname.replace(/\/+$/, "") || "/m"
  const currentApp = [...mobileNavigation, ...mobileFeatureApps].find((item) => item.href === mobilePath)
  const CurrentIcon = currentApp?.icon ?? WorkspaceIcon
  const currentLabel = currentApp?.label ?? workspace.name
  const isFinance = mobilePath === "/m/finance" && location.hash === "#finance-accounts"
  const isAppRoute = Boolean(getMobileAppNavigation(mobilePath))

  const workspaceControl = (
    <Dropdown isOpen={workspaceMenuOpen} onOpenChange={setWorkspaceMenuOpen}>
      <MobileButton isIconOnly variant="ghost" className="mobile-header-control" aria-label={`Switch workspace: ${workspace.name}`}>
        <span className="inline-flex" title={`${currentLabel}. Switch workspace`}><CurrentIcon aria-hidden="true" /></span>
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
          {isAppRoute ? <Link to="/m" className="mobile-header-control" aria-label="Back to Home" title="Back to Home"><ArrowLeft aria-hidden="true" /></Link> : workspaceControl}
        </div>
        <div className="mobile-header-greeting"><h1>Hello, {sidebarConfig.user.name}</h1><p>Today {new Date().toLocaleDateString("en-GB", { day: "numeric", month: "short" })}.</p></div>
        <div className="mobile-header-actions">
          {isFinance && onFinanceRefresh && <MobileButton isIconOnly variant="ghost" className="mobile-header-control mobile-header-refresh" aria-label="Refresh finance" onPress={onFinanceRefresh}><span title="Refresh finance"><RefreshCw aria-hidden="true" /></span></MobileButton>}
          <MobileAppSearch />
        </div>
      </div>
    </header>
  )
}
