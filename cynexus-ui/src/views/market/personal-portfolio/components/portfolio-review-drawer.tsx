import { useState, type ReactNode } from "react"
import { CheckCircle2, Save } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { Textarea } from "@/components/ui/textarea"
import { notification } from "@/components/ui/notification"
import { savePortfolioReview } from "@/services/market/portfolio/reviews"
import type { PortfolioReview } from "@/services/market/portfolio/types"

interface PortfolioReviewDrawerProps {
  open: boolean
  symbol?: string | null
  instrumentType: "ETF" | "STOCK"
  onOpenChange: (open: boolean) => void
  onSaved?: (review: PortfolioReview) => void
}

function defaultReview(symbol: string) {
  const today = new Date().toISOString().slice(0, 10)
  return { symbol, periodStart: today, periodEnd: today, status: "DRAFT" } satisfies PortfolioReview
}

export function PortfolioReviewDrawer({ open, symbol, instrumentType, onOpenChange, onSaved }: PortfolioReviewDrawerProps) {
  const [review, setReview] = useState<PortfolioReview>(() => defaultReview(symbol ?? ""))
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function set(key: keyof PortfolioReview, value: string | number | null) {
    setReview((current) => ({ ...current, [key]: value }))
  }

  async function save(status: "DRAFT" | "COMPLETED") {
    if (!review.symbol.trim()) {
      const message = "请先填写要复盘的标的代码。"
      setError(message)
      notification.warn({ title: "无法保存复盘", description: message })
      return
    }
    setSaving(true)
    setError(null)
    try {
      const saved = await savePortfolioReview({
        ...review,
        symbol: review.symbol.trim().toUpperCase(),
        status,
      })
      setReview(saved)
      onSaved?.(saved)
      notification.success({
        title: status === "COMPLETED" ? "复盘已完成" : "复盘草稿已保存",
        description: "复盘内容已关联到你的个人持仓。",
      })
      if (status === "COMPLETED") onOpenChange(false)
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : String(cause)
      setError(message)
      notification.danger({ title: "复盘保存失败", description: message })
    } finally {
      setSaving(false)
    }
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="flex h-full w-full flex-col gap-0 overflow-hidden p-0 md:w-[min(620px,52vw)] md:max-w-none">
        <SheetHeader className="shrink-0 border-b border-border/60 px-5 py-4 pr-14">
          <SheetTitle>新建交易复盘</SheetTitle>
          <SheetDescription>
            记录 {instrumentType === "ETF" ? "ETF" : "股票"} 的交易逻辑、执行偏差和下一步行动。
          </SheetDescription>
        </SheetHeader>

        <div className="min-h-0 flex-1 overflow-y-auto px-5 py-4">
          {error ? <div className="mb-4 break-words rounded-md border border-rose-200 bg-rose-50 px-3 py-2.5 text-xs text-rose-700">{error}</div> : null}
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="标的代码"><Input value={review.symbol} onChange={(event) => set("symbol", event.target.value)} placeholder="例如 159155" /></Field>
            <Field label="复盘状态"><select className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm text-foreground" value={review.status ?? "DRAFT"} onChange={(event) => set("status", event.target.value)}><option value="DRAFT">草稿</option><option value="COMPLETED">已完成</option></select></Field>
            <Field label="开始日期"><Input type="date" value={review.periodStart} onChange={(event) => set("periodStart", event.target.value)} /></Field>
            <Field label="结束日期"><Input type="date" value={review.periodEnd} onChange={(event) => set("periodEnd", event.target.value)} /></Field>
            <Field label="评分（1-5）"><Input type="number" min={1} max={5} value={review.rating ?? ""} onChange={(event) => set("rating", event.target.value ? Number(event.target.value) : null)} /></Field>
            <Field label="标签"><Input value={review.tags ?? ""} onChange={(event) => set("tags", event.target.value)} placeholder="突破、跟随、止损" /></Field>
            <Field className="sm:col-span-2" label="交易逻辑"><Textarea value={review.thesis ?? ""} onChange={(event) => set("thesis", event.target.value)} rows={4} placeholder="为什么买入或卖出？当时观察到什么信号？" /></Field>
            <Field label="交易计划"><Textarea value={review.plan ?? ""} onChange={(event) => set("plan", event.target.value)} rows={5} placeholder="预期、止损位和退出条件是什么？" /></Field>
            <Field label="实际执行"><Textarea value={review.execution ?? ""} onChange={(event) => set("execution", event.target.value)} rows={5} placeholder="实际执行与计划有什么差异？" /></Field>
            <Field label="经验教训"><Textarea value={review.lessons ?? ""} onChange={(event) => set("lessons", event.target.value)} rows={5} placeholder="这次交易应该保留或避免什么？" /></Field>
            <Field label="下一步行动"><Textarea value={review.nextAction ?? ""} onChange={(event) => set("nextAction", event.target.value)} rows={5} placeholder="下一次准备如何执行？" /></Field>
          </div>
          <p className="mt-4 rounded-md bg-muted/35 px-3 py-3 text-xs leading-5 text-muted-foreground">客观收益快照由已确认交易和市场行情自动计算，不会被复盘文字覆盖。</p>
        </div>

        <SheetFooter className="shrink-0 flex-row justify-end border-t border-border/60 px-5 py-3">
          <Button variant="outline" onClick={() => void save("DRAFT")} disabled={saving}><Save className="mr-2 size-4" />{saving ? "保存中..." : "保存草稿"}</Button>
          <Button onClick={() => void save("COMPLETED")} disabled={saving}><CheckCircle2 className="mr-2 size-4" />完成复盘</Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  )
}

function Field({ label, className, children }: { label: string; className?: string; children: ReactNode }) {
  return <label className={`space-y-1 text-xs text-muted-foreground ${className ?? ""}`}>{label}{children}</label>
}
