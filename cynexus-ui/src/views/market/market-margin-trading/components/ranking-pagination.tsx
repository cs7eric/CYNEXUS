import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { RANKING_PAGE_SIZE_OPTIONS } from "../lib/constants"

interface RankingPaginationProps {
  page: number
  totalPages: number
  pageSize: number
  loading?: boolean
  onPageChange: (page: number) => void
  onPageSizeChange: (pageSize: number) => void
}

export function RankingPagination({
  page,
  totalPages,
  pageSize,
  loading = false,
  onPageChange,
  onPageSizeChange,
}: RankingPaginationProps) {
  const safePage = Math.min(Math.max(page, 1), totalPages)
  const canPrevious = !loading && safePage > 1
  const canNext = !loading && safePage < totalPages

  return (
    <div className="mt-3 flex flex-wrap items-center justify-between gap-3 border-t border-border/50 pt-3">
      <div className="flex flex-1 items-center gap-4 text-xs text-muted-foreground">
        <div className="flex items-center gap-2">
          <span>每页</span>
          <Select
            value={String(pageSize)}
            onValueChange={(value) => {
              onPageSizeChange(Number(value))
              onPageChange(1)
            }}
          >
            <SelectTrigger size="sm" className="h-7 w-[72px] border-0 bg-muted/45 px-2 text-xs shadow-none" aria-label="每页条数">
              <SelectValue />
            </SelectTrigger>
            <SelectContent side="top">
              {RANKING_PAGE_SIZE_OPTIONS.map((option) => (
                <SelectItem key={option} value={String(option)}>{option}</SelectItem>
              ))}
            </SelectContent>
          </Select>
          <span>条</span>
        </div>
        <span className="tabular-nums">第 {safePage} / {totalPages} 页</span>
      </div>
      <div className="flex items-center gap-1.5">
        <Button type="button" variant="outline" size="icon" className="hidden size-8 sm:inline-flex" disabled={!canPrevious} onClick={() => onPageChange(1)} aria-label="第一页">
          <ChevronsLeft className="size-4" />
        </Button>
        <Button type="button" variant="outline" size="icon" className="size-8" disabled={!canPrevious} onClick={() => onPageChange(safePage - 1)} aria-label="上一页">
          <ChevronLeft className="size-4" />
        </Button>
        <Button type="button" variant="outline" size="icon" className="size-8" disabled={!canNext} onClick={() => onPageChange(safePage + 1)} aria-label="下一页">
          <ChevronRight className="size-4" />
        </Button>
        <Button type="button" variant="outline" size="icon" className="hidden size-8 sm:inline-flex" disabled={!canNext} onClick={() => onPageChange(totalPages)} aria-label="最后一页">
          <ChevronsRight className="size-4" />
        </Button>
      </div>
    </div>
  )
}
