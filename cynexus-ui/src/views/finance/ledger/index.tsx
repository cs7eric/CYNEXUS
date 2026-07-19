import { useCallback, useEffect, useState } from "react"
import { Link } from "react-router-dom"
import { ArrowRight, CalendarDays } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { notification } from "@/components/ui/notification"
import { Skeleton } from "@/components/ui/skeleton"
import { WorkspaceShell } from "@/layout/workspace-shell"
import {
  archiveFinanceTransaction,
  fetchFinanceAccounts,
  fetchFinanceCategories,
  fetchFinanceOverview,
  type FinanceAccount,
  type FinanceCategory,
  type FinanceOverview,
} from "@/services/finance/ledger"
import { FinancePageHeader } from "../components/page-header"
import { AccountSnapshot } from "../components/account-snapshot"
import { CategoryBreakdown } from "../components/category-breakdown"
import { QuickEntryForm } from "../components/quick-entry-form"
import { FinanceSummaryStrip } from "../components/summary-strip"
import { TransactionTable } from "../components/transaction-table"
import { monthBounds } from "../lib/format"
import { saveFinanceTransaction } from "@/services/finance/ledger"

function LoadingLedger() {
  return <div className="space-y-4"><Skeleton className="h-28" /><div className="grid gap-4 xl:grid-cols-[minmax(0,1.15fr)_minmax(360px,0.85fr)]"><Skeleton className="h-[430px]" /><Skeleton className="h-[430px]" /></div><Skeleton className="h-72" /></div>
}

export default function FinanceLedgerPage() {
  const bounds = monthBounds()
  const [range, setRange] = useState(bounds)
  const [overview, setOverview] = useState<FinanceOverview | null>(null)
  const [accounts, setAccounts] = useState<FinanceAccount[]>([])
  const [categories, setCategories] = useState<FinanceCategory[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const [nextOverview, nextAccounts, nextCategories] = await Promise.all([
        fetchFinanceOverview(range), fetchFinanceAccounts(), fetchFinanceCategories(),
      ])
      setOverview(nextOverview); setAccounts(nextAccounts); setCategories(nextCategories)
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : String(cause)
      setError(message); notification.danger({ title: "账本加载失败", description: message })
    } finally { setLoading(false) }
  }, [range])

  useEffect(() => { const timer = window.setTimeout(() => { void load() }, 0); return () => window.clearTimeout(timer) }, [load])

  async function save(payload: Record<string, unknown>) {
    await saveFinanceTransaction(payload)
    notification.success({ title: "已入账", description: "这笔账已加入当前账本。" })
    await load()
  }

  async function archive(id: number) {
    try { await archiveFinanceTransaction(id); notification.success({ title: "已归档", description: "账目仍保留在历史数据中。" }); await load() }
    catch (cause) { notification.danger({ title: "归档失败", description: cause instanceof Error ? cause.message : String(cause) }) }
  }

  return <WorkspaceShell sectionLabel="Finance" pageTitle="Ledger">
    <FinancePageHeader current="ledger" description="把日常收支、转账和账户余额放在同一个可回看的账本里。" onRefresh={() => void load()} loading={loading} />
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border/70 bg-muted/25 px-4 py-3">
      <div className="flex items-center gap-2 text-xs font-medium"><CalendarDays className="size-4 text-primary" />统计周期</div>
      <div className="flex flex-wrap items-center gap-2"><Input aria-label="开始日期" type="date" value={range.start} onChange={(event) => setRange((value) => ({ ...value, start: event.target.value }))} className="w-auto bg-background" /><span className="text-xs text-muted-foreground">至</span><Input aria-label="结束日期" type="date" value={range.end} onChange={(event) => setRange((value) => ({ ...value, end: event.target.value }))} className="w-auto bg-background" /><Button type="button" variant="outline" size="sm" onClick={() => void load()} disabled={loading}>应用</Button></div>
    </div>
    {error && !loading ? <div className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:border-rose-900/50 dark:bg-rose-950/20 dark:text-rose-300">{error}</div> : null}
    {loading ? <LoadingLedger /> : overview ? <>
      <FinanceSummaryStrip overview={overview} />
      <div className="grid min-w-0 gap-4 xl:grid-cols-[minmax(0,1.15fr)_minmax(360px,0.85fr)]"><QuickEntryForm accounts={accounts} categories={categories} onSaved={save} /><div className="space-y-4"><AccountSnapshot accounts={accounts} /><CategoryBreakdown items={overview.categories} /></div></div>
      <TransactionTable rows={overview.recentTransactions} onArchive={archive} />
      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border/60 pt-3 text-xs text-muted-foreground"><span>AI 识别账单会先进入草稿，不会绕过审核直接写入账本。</span><Button asChild variant="link" size="sm" className="gap-1"><Link to="/finance/ai-capture">去图片记账 <ArrowRight className="size-3.5" /></Link></Button></div>
    </> : <div className="rounded-lg border border-dashed border-border px-4 py-12 text-center text-sm text-muted-foreground">暂无账本数据</div>}
  </WorkspaceShell>
}
