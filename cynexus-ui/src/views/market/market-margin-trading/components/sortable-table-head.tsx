import { ArrowDown, ArrowUp, ArrowUpDown } from "lucide-react"

import type { MarginRankingSort, MarginSortDirection } from "@/services/market/margin"
import { cn } from "@/lib/utils"

interface SortableTableHeadProps {
  label: string
  column: MarginRankingSort
  activeSort: MarginRankingSort
  direction: MarginSortDirection
  align?: "left" | "right"
  className?: string
  onSort: (column: MarginRankingSort) => void
}

export function SortableTableHead({
  label,
  column,
  activeSort,
  direction,
  align = "left",
  className,
  onSort,
}: SortableTableHeadProps) {
  const active = activeSort === column
  const Icon = active ? (direction === "asc" ? ArrowUp : ArrowDown) : ArrowUpDown

  return (
    <button
      type="button"
      aria-sort={active ? (direction === "asc" ? "ascending" : "descending") : "none"}
      onClick={() => onSort(column)}
      className={cn(
        "inline-flex w-full items-center gap-1.5 text-xs font-medium text-muted-foreground transition hover:text-foreground",
        align === "right" && "justify-end",
        className,
      )}
    >
      <span>{label}</span>
      <Icon className={cn("size-3.5", active ? "text-foreground" : "text-muted-foreground/70")} />
    </button>
  )
}
