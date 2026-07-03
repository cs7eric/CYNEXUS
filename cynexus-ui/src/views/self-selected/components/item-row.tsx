import { useState } from "react"
import { Trash2, ChevronRight, FileEdit, Compass, PlusCircle } from "lucide-react"
import { useNavigate } from "react-router-dom"

import type { SelfSelectedItem } from "@/services/market/self-selected"
import type { StockSectorsResponse } from "@/services/market/stock-chart"
import { Button } from "@/components/ui/button"
import { StockSectorClassification } from "@/components/app/stock-sector-classification"
import { cn } from "@/lib/utils"

import { ConfirmDialog } from "@/components/ui/confirm-dialog"
import { getMarketClasses } from "../lib/constants"

/**
 * 维护入口:
 * - 视觉与交互设计先看 design/front/self-selected-item-card.md
 * - 后续改动此卡片样式或信息层级时，请同步更新该文档
 */
interface ItemRowProps {
  item: SelfSelectedItem
  pending: boolean
  systemGroup?: boolean
  sectors?: StockSectorsResponse | null
  sectorsLoading?: boolean
  /** 当前 symbol 已加入「应用分析」targets */
  inAnalysis?: boolean
  canAddToTarget?: boolean
  onAddToTarget?: (item: SelfSelectedItem) => void | Promise<void>
  onDelete: (itemId: string) => void | Promise<void>
  onEdit?: (item: SelfSelectedItem) => void
}

export function ItemRow({
  item,
  pending,
  systemGroup = false,
  sectors = null,
  sectorsLoading = false,
  inAnalysis = false,
  canAddToTarget = false,
  onAddToTarget,
  onDelete,
  onEdit,
}: ItemRowProps) {
  const [confirmOpen, setConfirmOpen] = useState(false)
  const navigate = useNavigate()

  const label = item.name ? `${item.symbol}（${item.name}）` : item.symbol
  const market = (item.market || "").toUpperCase()
  const marketBadge = getMarketClasses(market)
  const sectorCount = (sectors?.industries?.length ?? 0) + (sectors?.concepts?.length ?? 0) + (sectors?.styles?.length ?? 0)

  // 点击卡片任意空白处 → 跳到 application-analysis 页面，并把当前股票以
  // query string 带过去；该页面会按 symbol 自动定位 / 添加到 targets
  const goToChart = () => {
    const params = new URLSearchParams({
      target_type: item.target_type || (market === "HK" ? "hk_stock" : "stock"),
      symbol: item.symbol,
      name: item.name || item.symbol,
      market: market,
    })
    navigate(`/stock-overview/application-analysis?${params.toString()}`)
  }

  // 删除按钮：阻止冒泡，不触发卡片跳转
  const openDelete = (e: React.MouseEvent) => {
    e.stopPropagation()
    if (pending) return
    setConfirmOpen(true)
  }

  const openEdit = (e: React.MouseEvent) => {
    e.stopPropagation()
    if (pending) return
    onEdit?.(item)
  }

  const handleAddToTarget = (e: React.MouseEvent) => {
    e.stopPropagation()
    if (pending || !onAddToTarget) return
    void onAddToTarget(item)
  }

  return (
    <div
      className={cn(
        "group relative flex w-full items-stretch overflow-hidden rounded-2xl border border-border/50 bg-card text-left shadow-[0_10px_30px_-24px_rgba(15,23,42,0.55)]",
        "transition-all duration-200",
        "hover:-translate-y-1 hover:border-border/80 hover:shadow-[0_18px_45px_-28px_rgba(15,23,42,0.7)]",
        pending && "pointer-events-none opacity-60",
      )}
    >
      <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(135deg,rgba(255,255,255,0.05),transparent_42%,rgba(15,23,42,0.04))]" />
      <div className="pointer-events-none absolute inset-x-0 top-0 h-12 bg-[radial-gradient(circle_at_top_left,rgba(255,255,255,0.12),transparent_68%)] opacity-70" />

      <div className="relative z-10 flex min-w-0 flex-1 flex-col gap-2.5 p-3">
        <div className="flex items-start gap-3 pr-28 sm:pr-32">
          <div className="min-w-0 flex-1 space-y-2.5">
            <div className="flex items-start justify-between gap-2">
              <div className="min-w-0 flex-1">
                <div className="flex min-w-0 items-center gap-2 pr-2">
                  {item.name ? (
                    <div className="truncate text-[15px] font-semibold text-foreground">
                      {item.name}
                    </div>
                  ) : (
                    <div className="shrink-0 text-xs italic text-muted-foreground/60">未命名</div>
                  )}
                  <span className="shrink-0 font-mono text-[11px] font-semibold tracking-[0.14em] text-foreground/75">
                    {item.symbol}
                  </span>
                  {market ? (
                    <span
                      className={cn(
                        "inline-flex h-5 shrink-0 items-center rounded-full border px-1.5 font-mono text-[9px] font-bold tracking-[0.18em]",
                        marketBadge,
                      )}
                    >
                      {market}
                    </span>
                  ) : null}
                </div>
                <div className="mt-0.5 text-[10px] text-muted-foreground/75">
                  {sectorsLoading
                    ? "F10 归属加载中..."
                    : sectorCount > 0
                      ? `F10 归属 ${sectorCount} 项`
                      : "点击进入应用分析"}
                </div>
              </div>

              <div className="flex items-start gap-2 pl-1">
                {inAnalysis ? (
                  <span
                    className="inline-flex h-7 shrink-0 items-center gap-1.5 rounded-full border border-emerald-500/35 bg-emerald-500/12 px-2.5 text-[10px] font-semibold tracking-wide text-emerald-700 shadow-sm shadow-emerald-950/5"
                    title="已加入应用分析 targets"
                  >
                    <Compass className="size-3" />
                    已加入
                  </span>
                ) : null}
                {systemGroup ? (
                  <span
                    className="inline-flex h-7 shrink-0 items-center rounded-full border border-amber-500/35 bg-amber-500/12 px-2.5 text-[10px] font-semibold tracking-wide text-amber-700 shadow-sm shadow-amber-950/5"
                    title="系统 target 分组镜像项"
                  >
                    系统镜像
                  </span>
                ) : null}
              </div>
            </div>
          </div>
        </div>

        <StockSectorClassification sectors={sectors} loading={sectorsLoading} />

        {item.notes ? (
          <div className="rounded-xl border border-border/40 bg-background/55 px-2.5 py-2 text-[11px] leading-5 text-muted-foreground shadow-inner shadow-black/[0.03] backdrop-blur-sm">
            <span className="line-clamp-2 block">{item.notes}</span>
          </div>
        ) : null}

        {canAddToTarget ? (
          <div className="mt-auto flex items-center justify-end gap-2 border-t border-border/40 pt-2">
            {canAddToTarget ? (
              <button
                type="button"
                onClick={handleAddToTarget}
                className="inline-flex items-center gap-1 rounded-full border border-amber-500/30 bg-amber-500/10 px-2.5 py-1 text-[11px] font-medium text-amber-700 transition-colors hover:border-amber-500/50 hover:bg-amber-500/15"
                aria-label="加入 target 分组"
              >
                <PlusCircle className="size-3.5" />
                加入 target
              </button>
            ) : null}
          </div>
        ) : null}
      </div>

      <div className="absolute right-3 top-3 z-20 flex items-center gap-1.5">
        <Button
          size="icon-sm"
          variant="ghost"
          className="size-8 rounded-full border border-border/50 bg-background/90 text-muted-foreground shadow-sm backdrop-blur-sm hover:bg-muted hover:text-foreground"
          onClick={goToChart}
          disabled={pending}
          aria-label={`分析 ${item.symbol} ${item.name ?? ""} 的应用面`}
          title="进入分析"
        >
          <ChevronRight className="size-3.5 transition-transform duration-200 group-hover:translate-x-0.5" />
        </Button>
        {onEdit ? (
          <Button
            size="icon-sm"
            variant="ghost"
            className="size-8 rounded-full border border-border/50 bg-background/90 text-muted-foreground shadow-sm backdrop-blur-sm hover:bg-muted hover:text-foreground"
            onClick={openEdit}
            disabled={pending}
            aria-label="编辑备注"
            title="编辑备注"
          >
            <FileEdit className="size-3.5" />
          </Button>
        ) : null}
        <Button
          size="icon-sm"
          variant="ghost"
          className="size-8 rounded-full border border-border/50 bg-background/90 text-muted-foreground shadow-sm backdrop-blur-sm hover:bg-destructive/10 hover:text-destructive"
          onClick={openDelete}
          disabled={pending}
          aria-label="删除"
          title="删除"
        >
          <Trash2 className="size-3.5" />
        </Button>
      </div>

      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title="从分类中移除"
        description={
          <>
            确定要把 <span className="font-mono font-semibold text-foreground">{label}</span>{" "}
            从这个分类中移除吗？
          </>
        }
        confirmText="移除"
        destructive
        pending={pending}
        onConfirm={() => onDelete(item.id)}
      />
    </div>
  )
}
