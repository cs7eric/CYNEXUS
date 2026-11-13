import { ArrowUpRight, BookOpenText, ChevronRight } from "lucide-react"
import { Link } from "react-router-dom"

import { useWorkspace } from "@/layout/workspace-provider"
import { DesktopAppLink } from "@/mobile/components/desktop-app-link"
import { getMobileAppGroups, getMobileAppHref, mobileFeatureApps } from "@/mobile/navigation"
import "@/mobile/pages/apps/apps.css"

export default function MobileAppsPage() {
  const { workspaceId, workspace } = useWorkspace()
  const groups = getMobileAppGroups(workspaceId)
  const count = groups.reduce((total, group) => total + group.items.length, 0) + mobileFeatureApps.length
  const featuredCount: number = mobileFeatureApps.length
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
            {shared.items.map(({ url, title, icon }) => {
              const Icon = url === "/knowledge-base/view" ? BookOpenText : icon
              return (
                <Link key={url} to={getMobileAppHref(url)} state={{ workspace: workspaceId }} className="mobile-apps-feature">
                  <span className="mobile-apps-feature-top"><Icon aria-hidden="true" /><span className="mobile-apps-feature-arrow"><ArrowUpRight aria-hidden="true" /></span></span>
                  <strong>{title.split(" ").map((word, index) => <span key={`${word}-${index}`}>{word}{" "}</span>)}</strong>
                </Link>
              )
            })}
          </div>
        </section>
      )}

      <section aria-labelledby="mobile-group-featured">
        <div className="mobile-apps-section-heading"><h3 id="mobile-group-featured">Personal apps</h3><span>{featuredCount} {featuredCount === 1 ? "app" : "apps"}</span></div>
        <div className="mobile-apps-personal">
          {mobileFeatureApps.map(({ href, label, icon: Icon }) => (
            <Link key={href} to={href} state={{ workspace: workspaceId }} className="mobile-apps-personal-link">
              <Icon aria-hidden="true" />
              <strong>{label}</strong>
              <ChevronRight className="mobile-apps-personal-chevron" aria-hidden="true" />
            </Link>
          ))}
        </div>
      </section>

      {groups.filter((group) => group.id !== "shared").map((group) => (
        <section key={group.id} className="mobile-apps-group" aria-labelledby={`mobile-group-${group.id}`}>
          <div className="mobile-apps-section-heading"><h3 id={`mobile-group-${group.id}`}>{group.title}</h3><span>{group.items.length} {group.items.length === 1 ? "app" : "apps"}</span></div>
          <div className="mobile-apps-list">
            {group.items.map((item) => <DesktopAppLink key={item.url} href={getMobileAppHref(item.url)} title={item.title} icon={item.icon} />)}
          </div>
        </section>
      ))}
    </div>
  )
}
