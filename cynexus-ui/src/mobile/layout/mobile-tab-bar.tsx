import { useEffect, useRef, useState, type CSSProperties } from "react"
import { Link, NavLink } from "react-router-dom"
import { BookOpenText, LayoutGrid, Plus, Sparkles, X } from "lucide-react"

import { cn } from "@/lib/utils"
import { mobileNavigation } from "@/mobile/navigation"

export function MobileTabBar() {
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
    <nav aria-label="Mobile navigation" className="mobile-tab-bar">
      {quickActionsOpen && <button type="button" className="mobile-tab-backdrop" aria-label="Close quick actions" onClick={() => setQuickActionsOpen(false)} />}
      <div ref={layout} className="mobile-tab-layout">
        <div className="mobile-tab-capsule" data-actions-open={quickActionsOpen} aria-hidden={quickActionsOpen}>
          {mobileNavigation.map(({ href, label, icon: Icon }) => (
            <NavLink key={href} to={href} end={href === "/m"}
              tabIndex={quickActionsOpen ? -1 : 0}
              className={({ isActive }) => cn("mobile-tab", isActive && "mobile-tab-active")}>
              <Icon className="size-5 shrink-0" aria-hidden="true" />
              <span>{label}</span>
            </NavLink>
          ))}
        </div>
        <div id="mobile-quick-actions" className="mobile-quick-actions" data-open={quickActionsOpen} aria-hidden={!quickActionsOpen}>
          <Link to="/m/apps" className="mobile-quick-action" style={{ "--stack-y": "-70px" } as CSSProperties} tabIndex={quickActionsOpen ? 0 : -1} aria-label="Apps" title="Apps" onClick={() => setQuickActionsOpen(false)}><LayoutGrid aria-hidden="true" /><span>Apps</span></Link>
          <Link to="/knowledge-base/view" className="mobile-quick-action" style={{ "--stack-y": "-122px" } as CSSProperties} tabIndex={quickActionsOpen ? 0 : -1} aria-label="Knowledge Base" title="Knowledge Base" onClick={() => setQuickActionsOpen(false)}><BookOpenText aria-hidden="true" /><span>Knowledge</span></Link>
          <Link to="/cynexus-agent" className="mobile-quick-action" style={{ "--stack-y": "-174px" } as CSSProperties} tabIndex={quickActionsOpen ? 0 : -1} aria-label="Cynexus Agent" title="Cynexus Agent" onClick={() => setQuickActionsOpen(false)}><Sparkles aria-hidden="true" /><span>Agent</span></Link>
        </div>
        <button ref={addButton} type="button" className="mobile-tab-add" aria-label={quickActionsOpen ? "Close quick actions" : "Open quick actions"} aria-expanded={quickActionsOpen} aria-controls="mobile-quick-actions" onClick={() => setQuickActionsOpen((open) => !open)}>
          <span>{quickActionsOpen ? <X aria-hidden="true" /> : <Plus aria-hidden="true" />}</span>
        </button>
      </div>
    </nav>
  )
}
