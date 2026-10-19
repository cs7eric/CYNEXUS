import { Check, ChevronDown, Search } from "lucide-react"
import { useId, useMemo, useState } from "react"
import { createPortal } from "react-dom"

import { useMobilePortal } from "@/mobile/hooks/use-mobile-portal"
import type { FinanceCategory } from "@/services/finance/ledger"
import { ActivitySheet } from "./activity-sheet"

function categoryPath(category: FinanceCategory, categories: FinanceCategory[]): string[] {
  const parents: string[] = []
  const visited = new Set([category.id])
  let parentId = category.parentId
  while (parentId != null && !visited.has(parentId)) {
    visited.add(parentId)
    const parent = categories.find((item) => item.id === parentId)
    if (!parent) break
    parents.unshift(parent.name)
    parentId = parent.parentId
  }
  return parents
}

export function ActivityCategoryPicker({ categories, value, onChange }: { categories: FinanceCategory[]; value: string; onChange: (value: string) => void }) {
  const portal = useMobilePortal()
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState("")
  const id = useId()
  const options = useMemo(() => categories.map((category) => ({ category, path: categoryPath(category, categories) })), [categories])
  const selected = options.find((item) => String(item.category.id) === value)
  const normalizedQuery = query.trim().toLocaleLowerCase()
  const filtered = options.filter((item) => [...item.path, item.category.name].join(" ").toLocaleLowerCase().includes(normalizedQuery))

  const sheet = open && <ActivitySheet title="Choose a category" onClose={() => setOpen(false)}>
      <div className="fa-category-search"><Search aria-hidden="true" /><input type="search" aria-label="Search categories" placeholder="Search categories…" autoComplete="off" value={query} onChange={(event) => setQuery(event.target.value)} /></div>
      <p className="fa-category-count" role="status">{filtered.length} {filtered.length === 1 ? "category" : "categories"}</p>
      <div className="fa-category-list" role="group" aria-label="Available categories">{filtered.map(({ category, path }) => <button type="button" key={category.id} aria-pressed={String(category.id) === value} onClick={() => { onChange(String(category.id)); setOpen(false) }}>
        <span><strong>{category.name}</strong>{path.length > 0 && <small>{path.join(" / ")}</small>}</span>{String(category.id) === value && <Check aria-hidden="true" />}
      </button>)}</div>
      {!filtered.length && <div className="fa-empty"><h3>No matching categories.</h3><p>Try a category name or its parent category.</p>{query && <button type="button" className="fa-text-button" onClick={() => setQuery("")}>Clear search</button>}</div>}
    </ActivitySheet>

  return <div className="fa-category-field">
    <span className="fa-field-label" id={id}>Category</span>
    <button type="button" className="fa-category-trigger" aria-labelledby={`${id} ${id}-value`} aria-haspopup="dialog" onClick={() => { setQuery(""); setOpen(true) }}>
      <span><strong id={`${id}-value`}>{selected?.category.name ?? "Choose a category"}</strong>{!!selected?.path.length && <small>{selected.path.join(" / ")}</small>}</span><ChevronDown aria-hidden="true" />
    </button>
    {!categories.length && <p className="fa-message">No categories available. Add one in desktop Finance, or choose another record type.</p>}
    {portal ? createPortal(sheet, portal) : sheet}
  </div>
}
