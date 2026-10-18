import { ArrowUpRight } from "lucide-react"
import { Link } from "react-router-dom"

import { useWorkspace } from "@/layout/workspace-provider"
import { DesktopAppLink } from "@/mobile/components/desktop-app-link"
import { getMobileAppGroups } from "@/mobile/navigation"
import "@/mobile/pages/apps.css"

export default function MobileAppsPage() {
  const { workspaceId, workspace } = useWorkspace()
  const groups = getMobileAppGroups(workspaceId)
  const count = groups.reduce((total, group) => total + group.items.length, 0)
  const shared = groups.find((group) => group.id === "shared")

  return (
    <div className="mobile-apps">
      <header className="mobile-apps-heading">
        <div><h2>Apps</h2><p>{workspace.name} workspace</p></div>
        <span>{count} {count === 1 ? "app" : "apps"}</span>
      </header>

      {shared && (
        <section aria-labelledby="mobile-group-shared">
          <div className="mobile-apps-section-heading"><h3 id="mobile-group-shared">{shared.title}</h3><span>Both workspaces</span></div>
          <div className="mobile-apps-shared">
            {shared.items.map(({ url, title, icon: Icon }) => (
              <Link key={url} to={url} state={{ workspace: workspaceId }} className="mobile-apps-feature">
                <span className="mobile-apps-feature-top"><Icon aria-hidden="true" /><ArrowUpRight aria-hidden="true" /></span>
                <strong>{title}</strong>
              </Link>
            ))}
          </div>
        </section>
      )}

      {groups.filter((group) => group.id !== "shared").map((group) => (
        <section key={group.id} aria-labelledby={`mobile-group-${group.id}`}>
          <div className="mobile-apps-section-heading"><h3 id={`mobile-group-${group.id}`}>{group.title}</h3><span>{group.items.length} {group.items.length === 1 ? "app" : "apps"}</span></div>
          <div className="mobile-apps-list">
            {group.items.map((item) => <DesktopAppLink key={item.url} href={item.url} title={item.title} icon={item.icon} />)}
          </div>
        </section>
      ))}
    </div>
  )
}
