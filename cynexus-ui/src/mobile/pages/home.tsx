import { ArrowUpRight, LayoutGrid, Sparkles, UserRound } from "lucide-react"
import { Link } from "react-router-dom"

import { useWorkspace } from "@/layout/workspace-provider"
import { MobileActivityCalendar } from "@/mobile/components/activity-calendar"
import { createActivityPreview } from "@/mobile/components/activity-calendar/preview"
import { getMobileAppGroups } from "@/mobile/navigation"
import "@/mobile/pages/home.css"

export default function MobileHomePage() {
  const { workspace, workspaceId } = useWorkspace()
  const groups = getMobileAppGroups(workspaceId)
  const primaryApp = groups.find((group) => group.id !== "shared")?.items[0]
  const today = new Date()
  const PrimaryIcon = primaryApp?.icon ?? LayoutGrid

  return (
    <div className="mobile-home">
      <section className="mobile-home-greeting" aria-labelledby="mobile-home-greeting-title">
        <h2 id="mobile-home-greeting-title">Let’s Make<br />Today Productive</h2>
      </section>
      <MobileActivityCalendar today={today} records={createActivityPreview(today)} isSample showWeek={false} />

      <section className="mobile-home-plan" aria-labelledby="mobile-home-plan-title">
        <h2 id="mobile-home-plan-title">Your plan</h2>
        <div className="mobile-home-plan-grid">
          <Link to={primaryApp?.url ?? "/m/apps"} state={{ workspace: workspaceId }} className="mobile-home-primary-card">
            <span className="mobile-home-card-tag">{workspace.name}</span>
            <div className="mobile-home-primary-copy">
              <h3>{primaryApp?.title ?? "Your apps"}</h3>
              <p>{workspaceId === "personal" ? <>Your money,<br />at a glance.</> : <>A clear view of<br />your workspace.</>}</p>
              <span className="mobile-home-card-action">Open workspace <ArrowUpRight size={14} aria-hidden="true" /></span>
            </div>
            <div className="mobile-home-card-owner">
              <span className="mobile-home-owner-icon"><PrimaryIcon aria-hidden="true" /></span>
              <span><small>Workspace</small><strong>{workspace.name}</strong></span>
            </div>
          </Link>

          <Link to="/knowledge-base/view" state={{ workspace: workspaceId }} className="mobile-home-library-card">
            <span className="mobile-home-card-tag">Shared</span>
            <h3>Knowledge<br />Base</h3>
            <p>Your ideas,<br />connected.</p>
            <img src="/mobile/home/balance-sculpture.webp" alt="" width="117" height="153" />
          </Link>

          <div className="mobile-home-tools-card" role="group" aria-label="Quick access">
            <Link to="/cynexus-agent" state={{ workspace: workspaceId }} aria-label="Cynexus Agent" title="Cynexus Agent"><Sparkles aria-hidden="true" /></Link>
            <Link to="/m/apps" aria-label="App directory" title="App directory"><LayoutGrid aria-hidden="true" /></Link>
            <Link to="/m/me" aria-label="Profile" title="Profile"><UserRound aria-hidden="true" /></Link>
          </div>
        </div>
      </section>
    </div>
  )
}
