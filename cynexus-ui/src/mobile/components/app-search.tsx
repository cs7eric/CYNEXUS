import { Search, X } from "lucide-react"
import { useRef, useState } from "react"
import { Link } from "react-router-dom"

import { useWorkspace } from "@/layout/workspace-provider"
import { getMobileAppGroups, mobileFeatureApps } from "@/mobile/navigation"
import { MobileButton } from "@/mobile/ui/button"

export function MobileAppSearch() {
  const dialog = useRef<HTMLDialogElement>(null)
  const input = useRef<HTMLInputElement>(null)
  const [query, setQuery] = useState("")
  const { workspaceId } = useWorkspace()
  const search = query.trim().toLocaleLowerCase("en-US")
  const apps = [...mobileFeatureApps.map((item) => ({ url: item.href, title: item.label, icon: item.icon })), ...getMobileAppGroups(workspaceId).flatMap((group) => group.items)]
    .filter((item) => item.title.toLocaleLowerCase("en-US").includes(search))

  function open() {
    dialog.current?.showModal()
    input.current?.focus()
  }

  return (
    <>
      <MobileButton isIconOnly variant="ghost" className="mobile-header-control" aria-label="Search apps" onPress={open}>
        <Search aria-hidden="true" />
      </MobileButton>
      <dialog ref={dialog} className="mobile-app-search" aria-labelledby="mobile-app-search-title" onClose={() => setQuery("")}
        onKeyDown={(event) => {
          if (event.key === "Escape") {
            event.preventDefault()
            event.stopPropagation()
            dialog.current?.close()
          }
        }}>
        <div className="mobile-app-search-heading">
          <h2 id="mobile-app-search-title">Find an app</h2>
          <button type="button" aria-label="Close search" title="Close search" onClick={() => dialog.current?.close()}><X aria-hidden="true" /></button>
        </div>
        <label className="mobile-app-search-field"><Search size={18} aria-hidden="true" /><input ref={input} value={query} onChange={(event) => setQuery(event.target.value)} type="search" aria-label="Search apps" placeholder="Search your apps" autoComplete="off" /></label>
        <nav className="mobile-app-search-results" aria-label="Search results">
          {apps.map(({ title, url, icon: Icon }) => (
            <Link key={url} to={url} state={{ workspace: workspaceId }} onClick={() => dialog.current?.close()}><Icon size={20} aria-hidden="true" />{title}</Link>
          ))}
          {apps.length === 0 && <p role="status">No apps found. Try another name.</p>}
        </nav>
      </dialog>
    </>
  )
}
