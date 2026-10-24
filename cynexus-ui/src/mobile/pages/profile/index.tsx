import { ArrowUpRight, BookOpen, ChevronRight, Fingerprint, LayoutGrid, ListTodo, Monitor, Settings, Settings2, Sparkles } from "lucide-react"
import { useRef } from "react"
import { Link } from "react-router-dom"

import { sidebarConfig } from "@/config/sidebar"
import { sharedNavigation, workspaces } from "@/config/workspaces"
import { useWorkspace } from "@/layout/workspace-provider"
import { useWorkspaceState } from "@/layout/workspace-state"
import { getMobileAppGroups } from "@/mobile/navigation"
import { MobileButton } from "@/mobile/ui/button"
import { MobileLogoutDialog } from "@/mobile/ui/confirm-dialog"
import { logoutOwner } from "@/services/common/owner-auth"
import "@/mobile/pages/profile/profile.css"

export default function MobileProfilePage() {
  const { workspaceId, workspace } = useWorkspace()
  const desktopPath = useWorkspaceState((state) => state.lastLocations[workspaceId])
  const security = useRef<HTMLDetailsElement>(null)
  const { name, email } = sidebarConfig.user
  const initials = name.slice(0, 2).toUpperCase()
  const appCount = getMobileAppGroups(workspaceId).reduce((total, group) => total + group.items.length, 0)
  const summaries = [
    { label: "Personal", count: workspaces.personal.groups.reduce((total, group) => total + group.items.length, 0), workspace: "personal" },
    { label: "Development", count: workspaces.development.groups.reduce((total, group) => total + group.items.length, 0), workspace: "development" },
    { label: "Shared", count: sharedNavigation.items.length, workspace: workspaceId },
  ] as const
  const links = [
    { title: "Your apps", detail: `${appCount} apps in ${workspace.name}`, href: "/m/apps", icon: LayoutGrid },
    { title: "Async tasks", detail: "Track background work and retry failures", href: "/m/tasks", icon: ListTodo },
    { title: "Configuration center", detail: "Daily targets and basic data", href: "/m/configuration", icon: Settings2 },
    { title: "Cynexus Agent", detail: "Plan, ask and explore", href: "/cynexus-agent", icon: Sparkles },
    { title: "Knowledge Base", detail: "Notes, files and collections", href: "/knowledge-base/view", icon: BookOpen },
    { title: "Desktop workspace", detail: `Continue in ${workspace.name}`, href: desktopPath, icon: Monitor },
  ]

  function openSecurity() {
    const section = security.current
    if (!section) return
    section.open = true
    section.scrollIntoView({ block: "nearest" })
    section.querySelector("summary")?.focus({ preventScroll: true })
  }

  return (
    <div className="mobile-profile">
      <div className="mobile-profile-heading">
        <h2>Profile</h2>
        <MobileButton isIconOnly variant="ghost" className="mobile-profile-settings" aria-label="Open security settings" onPress={openSecurity}>
          <span title="Open security settings"><Settings aria-hidden="true" /></span>
        </MobileButton>
      </div>

      <section className="mobile-profile-identity" aria-label="Your profile">
        <span className="mobile-profile-avatar" aria-hidden="true">{initials}</span>
        <div><h3>{name}</h3><p>{email}</p></div>
      </section>

      <div className="mobile-profile-context">
        <dl><div><dt>Workspace</dt><dd>{workspace.name}</dd></div><div><dt>Access</dt><dd>Owner</dd></div></dl>
        <Link to={desktopPath} state={{ workspace: workspaceId }} className="mobile-profile-desktop" aria-label="Open desktop workspace" title="Open desktop workspace"><ArrowUpRight aria-hidden="true" /></Link>
      </div>

      <nav className="mobile-profile-summaries" aria-label="Workspace apps">
        {summaries.map((summary) => (
          <Link key={summary.label} to="/m/apps" state={{ workspace: summary.workspace }} className="mobile-profile-summary" aria-label={`${summary.label}: ${summary.count} apps`}>
            <span>{summary.label}</span><strong>{summary.count}<small>apps</small></strong>
          </Link>
        ))}
      </nav>

      <div className="mobile-profile-links">
        {links.map(({ title, detail, href, icon: Icon }) => (
          <Link key={title} to={href} state={{ workspace: workspaceId }} className="mobile-profile-row">
            <Icon className="mobile-profile-row-icon" aria-hidden="true" />
            <span><strong>{title}</strong><small>{detail}</small></span>
            <ChevronRight className="mobile-profile-chevron" aria-hidden="true" />
          </Link>
        ))}
        <details ref={security} className="mobile-profile-security">
          <summary className="mobile-profile-row">
            <Fingerprint className="mobile-profile-row-icon" aria-hidden="true" />
            <span><strong>Sign-in &amp; security</strong><small>Devices and sign-in</small></span>
            <ChevronRight className="mobile-profile-chevron" aria-hidden="true" />
          </summary>
          <div className="mobile-profile-security-actions">
            <MobileLogoutDialog onConfirm={() => logoutOwner()} />
            <MobileLogoutDialog allDevices onConfirm={() => logoutOwner(true)} />
          </div>
        </details>
      </div>
    </div>
  )
}
