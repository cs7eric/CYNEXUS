import { ArrowRight, BookOpen, Camera, WalletCards } from "lucide-react"
import { Link } from "react-router-dom"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"

export function FinancePageHeader({ current, description, updatedAt, onRefresh, loading }: { current: string; description: string; updatedAt?: string | null; onRefresh?: () => void; loading?: boolean }) {
  return (
    <div className="flex min-w-0 flex-col gap-4 overflow-hidden md:flex-row md:items-end md:justify-between">
      <div className="min-w-0 space-y-2">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="outline" className="bg-emerald-50 text-emerald-700 dark:bg-emerald-950/30 dark:text-emerald-300"><WalletCards className="size-3" /> finance</Badge>
          <span className="text-xs text-muted-foreground">{current}</span>
          {updatedAt ? <span className="text-xs text-muted-foreground">更新 {new Date(updatedAt).toLocaleString("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" })}</span> : null}
        </div>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-foreground md:text-3xl">个人账本</h1>
          <p className="mt-1 max-w-3xl text-sm text-muted-foreground">{description}</p>
        </div>
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <Button asChild variant={current === "ledger" ? "secondary" : "ghost"} size="sm" className="gap-1.5"><Link to="/finance/ledger"><BookOpen className="size-3.5" />账本</Link></Button>
        <Button asChild variant={current === "accounts" ? "secondary" : "ghost"} size="sm" className="gap-1.5"><Link to="/finance/accounts"><WalletCards className="size-3.5" />账户</Link></Button>
        <Button asChild variant={current === "ai-capture" ? "secondary" : "ghost"} size="sm" className="gap-1.5"><Link to="/finance/ai-capture"><Camera className="size-3.5" />图片记账<ArrowRight className="size-3" /></Link></Button>
        {onRefresh ? <Button type="button" variant="outline" size="sm" onClick={onRefresh} disabled={loading}>刷新</Button> : null}
      </div>
    </div>
  )
}
