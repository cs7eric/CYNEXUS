import { ChevronRight, type LucideIcon } from "lucide-react"
import { Link } from "react-router-dom"

import { useWorkspace } from "@/layout/workspace-provider"

export function DesktopAppLink({ href, title, icon: Icon }: { href: string; title: string; icon: LucideIcon }) {
  const { workspaceId } = useWorkspace()
  return (
    <Link to={href} state={{ workspace: workspaceId }} className="mobile-apps-row">
      <Icon className="mobile-apps-row-icon" aria-hidden="true" />
      <span>{title}</span>
      <ChevronRight className="mobile-apps-row-chevron" aria-hidden="true" />
    </Link>
  )
}
