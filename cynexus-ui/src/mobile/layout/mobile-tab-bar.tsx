import { useEffect, useRef, useState, type CSSProperties } from "react"
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom"
import { BookOpenText, LayoutGrid, Plus, Ruler, Scale, Sparkles, Timer, X } from "lucide-react"

import { cn } from "@/lib/utils"
import { useWorkspace } from "@/layout/workspace-provider"
import { getMobileAppHref, getMobileAppNavigation, mobileNavigation, normalizeMobileAppPath } from "@/mobile/navigation"
import type { MetricEntryMode } from "@/mobile/components/metric-entry"

export function MobileTabBar({ onOpenMetricEntry }: { onOpenMetricEntry: (mode: MetricEntryMode) => void }) {
  const { workspaceId } = useWorkspace()
  const location = useLocation()
  const navigate = useNavigate()
  const appNavigation = getMobileAppNavigation(location.pathname)
  const mobilePath = normalizeMobileAppPath(location.pathname)
  const isFridgePage = mobilePath === "/m/food" && location.hash === "#food-fridge"
  const [quickActionsOpen, setQuickActionsOpen] = useState(false)
  const layout = useRef<HTMLDivElement>(null)
  const addButton = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape" && quickActionsOpen) {
        setQuickActionsOpen(false)
        addButton.current?.focus()
      }
    }

    document.addEventListener("keydown", closeOnEscape)
    return () => {
      document.removeEventListener("keydown", closeOnEscape)
    }
  }, [quickActionsOpen])

  return (
    <nav aria-label={appNavigation ? "App navigation" : "Mobile navigation"} className={`mobile-tab-bar${appNavigation ? " is-app-navigation" : ""}`}>
      {quickActionsOpen && <button type="button" className="mobile-tab-backdrop" aria-label="Close quick actions" onClick={() => setQuickActionsOpen(false)} />}
      <div ref={layout} className="mobile-tab-layout">
        <div className="mobile-tab-capsule" data-actions-open={quickActionsOpen} aria-hidden={quickActionsOpen} style={appNavigation ? { gridTemplateColumns: `repeat(${appNavigation.items.length}, minmax(0, 1fr))` } : undefined}>
          {appNavigation ? appNavigation.items.map(({ href, label, icon: Icon }, index) => {
            const isActive = location.hash === href || (!location.hash && index === 0)
            return <a key={href} href={href} tabIndex={quickActionsOpen ? -1 : 0} aria-current={isActive ? "page" : undefined} className={`mobile-tab${isActive ? " mobile-tab-active" : ""}`} onClick={(event) => {
              event.preventDefault()
              navigate({ pathname: mobilePath, search: location.search, hash: href }, { state: { workspace: workspaceId } })
            }}>
              <Icon className="size-5 shrink-0" aria-hidden="true" />
              <span>{label}</span>
            </a>
          }) : mobileNavigation.map(({ href, label, icon: Icon }) => (
            <NavLink key={href} to={href} state={{ workspace: workspaceId }} end={href === "/m"}
              tabIndex={quickActionsOpen ? -1 : 0}
              className={({ isActive }) => cn("mobile-tab", isActive && "mobile-tab-active")}>
              <Icon className="size-5 shrink-0" aria-hidden="true" />
              <span>{label}</span>
            </NavLink>
          ))}
        </div>
        <div id="mobile-quick-actions" className="mobile-quick-actions" data-open={quickActionsOpen} aria-hidden={!quickActionsOpen}>
          <button type="button" className="mobile-quick-action mobile-quick-action-metric" style={{ "--stack-y": "-70px" } as CSSProperties} tabIndex={quickActionsOpen ? 0 : -1} aria-label="Add weight" title="Add weight" onClick={() => { setQuickActionsOpen(false); onOpenMetricEntry("weight") }}><Scale aria-hidden="true" /><span>Weight</span></button>
          <button type="button" className="mobile-quick-action mobile-quick-action-metric" style={{ "--stack-y": "-122px" } as CSSProperties} tabIndex={quickActionsOpen ? 0 : -1} aria-label="Add body dimensions" title="Add body dimensions" onClick={() => { setQuickActionsOpen(false); onOpenMetricEntry("body") }}><Ruler aria-hidden="true" /><span>Body</span></button>
          <button type="button" className="mobile-quick-action mobile-quick-action-metric" style={{ "--stack-y": "-174px" } as CSSProperties} tabIndex={quickActionsOpen ? 0 : -1} aria-label="Add workout duration" title="Add workout duration" onClick={() => { setQuickActionsOpen(false); onOpenMetricEntry("workout") }}><Timer aria-hidden="true" /><span>Workout</span></button>
          <Link to="/m/apps" state={{ workspace: workspaceId }} className="mobile-quick-action" style={{ "--stack-y": "-226px" } as CSSProperties} tabIndex={quickActionsOpen ? 0 : -1} aria-label="Apps" title="Apps" onClick={() => setQuickActionsOpen(false)}><LayoutGrid aria-hidden="true" /><span>Apps</span></Link>
          <Link to={getMobileAppHref("/knowledge-base/view")} state={{ workspace: workspaceId }} className="mobile-quick-action" style={{ "--stack-y": "-278px" } as CSSProperties} tabIndex={quickActionsOpen ? 0 : -1} aria-label="Knowledge Base" title="Knowledge Base" onClick={() => setQuickActionsOpen(false)}><BookOpenText aria-hidden="true" /><span>Knowledge</span></Link>
          <Link to={getMobileAppHref("/cynexus-agent")} state={{ workspace: workspaceId }} className="mobile-quick-action" style={{ "--stack-y": "-330px" } as CSSProperties} tabIndex={quickActionsOpen ? 0 : -1} aria-label="Cynexus Agent" title="Cynexus Agent" onClick={() => setQuickActionsOpen(false)}><Sparkles aria-hidden="true" /><span>Agent</span></Link>
        </div>
        {!isFridgePage && <button ref={addButton} type="button" className="mobile-tab-add" aria-label={mobilePath === "/m/food" ? "Log food" : mobilePath === "/m/finance" ? "Add a record" : quickActionsOpen ? "Close quick actions" : "Open quick actions"} aria-expanded={quickActionsOpen} aria-controls="mobile-quick-actions" onClick={() => {
          if (mobilePath === "/m/food") {
            setQuickActionsOpen(false)
            navigate({ pathname: mobilePath, search: location.search, hash: "#food-today" }, { state: { mobileAction: "food-entry", requestId: crypto.randomUUID() } })
          } else if (mobilePath === "/m/finance") {
            setQuickActionsOpen(false)
            navigate({ pathname: mobilePath, search: location.search, hash: "#finance-activity" }, { state: { mobileAction: "finance-entry", requestId: crypto.randomUUID() } })
          } else setQuickActionsOpen((open) => !open)
        }}>
          <span>{quickActionsOpen ? <X aria-hidden="true" /> : <Plus aria-hidden="true" />}</span>
        </button>}
      </div>
    </nav>
  )
}
