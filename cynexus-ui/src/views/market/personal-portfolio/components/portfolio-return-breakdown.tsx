import { ArrowDownRight, ArrowUpRight, CircleHelp, Equal, ReceiptText } from "lucide-react"
import type { PortfolioOverview } from "@/services/market/portfolio/types"

interface PortfolioReturnBreakdownProps {
  overview: PortfolioOverview | null
}

function amount(value: number | null | undefined) {
  if (value == null) return "--"
  const numericValue = Number(value)
  return `${numericValue > 0 ? "+" : ""}${numericValue.toFixed(2)}`
}

function tone(value: number | null | undefined) {
  if (value == null || value === 0) return "text-muted-foreground"
  // A-share convention: red indicates a gain, green indicates a loss.
  return value > 0 ? "text-rose-600" : "text-emerald-600"
}

function DirectionIcon({ value }: { value: number | null | undefined }) {
  if (value == null || value === 0) return <Equal className="size-3.5" aria-hidden="true" />
  return value > 0
    ? <ArrowUpRight className="size-3.5" aria-hidden="true" />
    : <ArrowDownRight className="size-3.5" aria-hidden="true" />
}

export function PortfolioReturnBreakdown({ overview }: PortfolioReturnBreakdownProps) {
  const hasTrades = (overview?.tradeCount ?? 0) > 0
  const total = overview?.totalPnl ?? 0
  const realized = overview?.realizedPnl ?? 0
  const unrealized = overview?.unrealizedPnl ?? 0

  return (
    <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="return-breakdown-heading">
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/60 px-4 py-3.5">
        <div>
          <div className="flex items-center gap-2">
            <ReceiptText className="size-4 text-muted-foreground" aria-hidden="true" />
            <h2 id="return-breakdown-heading" className="text-base font-semibold">盈亏概览</h2>
          </div>
          <p className="mt-1 text-xs text-muted-foreground">把账户结果拆成已落袋和当前持仓两部分。</p>
        </div>
        <span className="rounded-full bg-muted px-2.5 py-1 text-xs text-muted-foreground">
          {hasTrades ? `${overview?.tradeCount ?? 0} 笔已确认交易` : "暂无交易记录"}
        </span>
      </div>

      {hasTrades ? (
        <div className="p-4">
          <div className="flex flex-wrap items-end justify-between gap-4 rounded-md bg-muted/35 px-4 py-4">
            <div>
              <p className="text-xs text-muted-foreground">当前总盈亏</p>
              <p className={`mt-1 font-mono text-3xl font-semibold tracking-tight tabular-nums ${tone(total)}`}>{amount(overview?.totalPnl)}</p>
            </div>
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <span className="inline-flex size-6 items-center justify-center rounded-full border border-border/70 bg-background" aria-hidden="true"><DirectionIcon value={total} /></span>
              <span>已实现盈亏 + 当前持仓盈亏</span>
            </div>
          </div>

          <dl className="mt-3 divide-y divide-border/60 rounded-md border border-border/60">
            <ReturnRow label="当前持仓盈亏" value={unrealized} description="当前市值 − 当前持仓成本" />
            <ReturnRow label="已实现盈亏" value={realized} description="已卖出部分；按当前基线重新累计" />
          </dl>

          <div className="mt-3 flex items-start gap-2 text-xs leading-5 text-muted-foreground">
            <CircleHelp className="mt-0.5 size-3.5 shrink-0" aria-hidden="true" />
            <p>当前总盈亏只反映已确认交易。现金余额、未确认的 AI 识别结果不会计入。</p>
          </div>
        </div>
      ) : (
        <div className="flex min-h-[210px] items-center justify-center px-4 py-10 text-center">
          <div>
            <p className="text-sm font-medium">确认交易后，这里会显示账户表现</p>
            <p className="mt-1 max-w-md text-xs leading-5 text-muted-foreground">导入截图并确认识别结果后，系统会按交易流水计算已实现盈亏、持仓盈亏和当前总盈亏。</p>
          </div>
        </div>
      )}
    </section>
  )
}

function ReturnRow({ label, value, description }: { label: string; value: number; description: string }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
      <div className="min-w-0">
        <dt className="text-sm font-medium">{label}</dt>
        <dd className="mt-0.5 text-xs text-muted-foreground">{description}</dd>
      </div>
      <dd className={`inline-flex items-center gap-1 font-mono text-sm font-semibold tabular-nums ${tone(value)}`}>
        <DirectionIcon value={value} />
        {amount(value)}
      </dd>
    </div>
  )
}
