import { Button } from "@heroui/react/button"
import { Dropdown } from "@heroui/react/dropdown"
import { ChevronDown, Search } from "lucide-react"
import { useId, useState, type ReactNode } from "react"

import { useMobilePortal } from "@/mobile/hooks/use-mobile-portal"

interface EntryChoiceOption {
  id: string
  label: string
  description?: string
  disabled?: boolean
}

export function ActivityEntryChoice({ label, icon, value, options, onChange, disabled = false, searchable = false, boxed = false }: {
  label: string
  icon?: ReactNode
  value: string
  options: EntryChoiceOption[]
  onChange: (value: string) => void
  disabled?: boolean
  searchable?: boolean
  boxed?: boolean
}) {
  const portal = useMobilePortal()
  const labelId = useId()
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState("")
  const selected = options.find((option) => option.id === value)
  const search = query.trim().toLocaleLowerCase()
  const filtered = options.filter((option) => `${option.label} ${option.description ?? ""}`.toLocaleLowerCase().includes(search))

  return <div className={`fa-entry-choice${boxed ? " is-boxed" : ""}`}>
    <span className="fa-entry-choice-label" id={labelId}>{icon}{label}</span>
    <Dropdown isOpen={open} onOpenChange={(next) => { setOpen(next); if (!next) setQuery("") }}>
      <Button type="button" variant="ghost" className="fa-entry-choice-trigger" isDisabled={disabled || !options.length}
        aria-labelledby={`${labelId} ${labelId}-value`}>
        <span id={`${labelId}-value`}>{selected?.label ?? `Choose ${label.toLowerCase()}`}</span><ChevronDown aria-hidden="true" />
      </Button>
      <Dropdown.Popover UNSTABLE_portalContainer={portal} className="fa-entry-choice-popover" placement="bottom start" offset={4}>
        {searchable && <label className="fa-entry-choice-search"><Search aria-hidden="true" /><input type="search" aria-label={`Search ${label.toLowerCase()}`} value={query}
          placeholder={`Search ${label.toLowerCase()}…`} onChange={(event) => setQuery(event.target.value)} /></label>}
        <Dropdown.Menu aria-labelledby={labelId} selectionMode="single" disallowEmptySelection selectedKeys={value ? [value] : []}
          disabledKeys={options.filter((option) => option.disabled).map((option) => option.id)}
          onAction={(key) => { if (!disabled) onChange(String(key)); setOpen(false) }}>
          {filtered.map((option) => <Dropdown.Item key={option.id} id={option.id} textValue={`${option.label} ${option.description ?? ""}`}>
            <span className="fa-entry-choice-copy"><span>{option.label}</span>{option.description && <small>{option.description}</small>}</span>
            <Dropdown.ItemIndicator />
          </Dropdown.Item>)}
        </Dropdown.Menu>
        {!filtered.length && <p className="fa-entry-choice-empty" role="status">No matching options.</p>}
      </Dropdown.Popover>
    </Dropdown>
  </div>
}
