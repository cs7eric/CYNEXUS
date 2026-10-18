import { ArrowUpRight, LayoutGrid, Sparkles, UserRound } from "lucide-react"
import { useEffect, useState } from "react"
import { Link } from "react-router-dom"

import { useWorkspace } from "@/layout/workspace-provider"
import { MobileActivityCalendar } from "@/mobile/components/activity-calendar"
import { activityDateKey, activityMonthStart, shiftActivityDate, shiftActivityMonth, type FinanceSummarySnapshot } from "@/mobile/components/activity-calendar/model"
import { createActivityPreview, createBodyMeasurementPreview } from "@/mobile/components/activity-calendar/preview"
import { getMobileAppGroups } from "@/mobile/navigation"
import { fetchFinanceOverview } from "@/services/finance/ledger"
import "@/mobile/pages/home/home.css"

export default function MobileHomePage() {
  const { workspace, workspaceId } = useWorkspace()
  const groups = getMobileAppGroups(workspaceId)
  const primaryApp = groups.find((group) => group.id !== "shared")?.items[0]
  const financeAvailable = groups.some((group) => group.items.some((item) => item.url === "/finance/overview"))
  const today = new Date()
  const financeStart = activityDateKey(activityMonthStart(today))
  const financeEnd = activityDateKey(shiftActivityDate(shiftActivityMonth(today, 1), -1))
  const PrimaryIcon = primaryApp?.icon ?? LayoutGrid
  const [finance, setFinance] = useState<FinanceSummarySnapshot>(() => financeAvailable ? { status: "loading" } : { status: "empty" })
  const [financeRetry, setFinanceRetry] = useState(0)

  useEffect(() => {
    if (!financeAvailable) return
    let cancelled = false
    const timer = window.setTimeout(() => {
      if (cancelled) return
      setFinance({ status: "loading" })
      fetchFinanceOverview({ start: financeStart, end: financeEnd }).then((overview) => {
        if (cancelled) return
        const hasData = overview.accounts.length > 0 || overview.recentTransactions.length > 0 || overview.daily.some((item) => item.activityCount > 0) || overview.income !== 0 || overview.expense !== 0 || overview.assetTotal !== 0 || overview.liabilityTotal !== 0
        if (!hasData) {
          setFinance({ status: "empty" })
          return
        }
        const currencies = [...overview.accounts.map((account) => account.currency), ...overview.recentTransactions.map((transaction) => transaction.currency)].filter((currency, index, values) => currency && values.indexOf(currency) === index)
        setFinance({ status: "ready", netAssets: overview.netAssets, income: overview.income, expense: overview.expense, net: overview.net, currency: currencies.length === 1 ? currencies[0] : null })
      }).catch(() => {
        if (!cancelled) setFinance({ status: "error" })
      })
    }, 0)
    return () => { cancelled = true; window.clearTimeout(timer) }
  }, [financeAvailable, financeStart, financeEnd, financeRetry, workspaceId])

  return (
    <div className="mobile-home">
      <section className="mobile-home-greeting" aria-labelledby="mobile-home-greeting-title">
        <h2 id="mobile-home-greeting-title">Let’s Make<br />Today Productive</h2>
      </section>
      <MobileActivityCalendar today={today} records={createActivityPreview(today)} bodyMeasurements={createBodyMeasurementPreview(today)} finance={finance} showFinance={financeAvailable} onFinanceRetry={() => setFinanceRetry((value) => value + 1)} isSample showWeek={false} />

      <section className="mobile-home-plan" aria-labelledby="mobile-home-plan-title">
        <h2 id="mobile-home-plan-title">Your plan</h2>
        <div className="mobile-home-plan-grid">
          <Link to={primaryApp?.url ?? "/m/apps"} state={{ workspace: workspaceId }} className="mobile-home-primary-card">
            <span className="mobile-home-card-tag">{workspace.name}</span>
            <div className="mobile-home-primary-copy">
              <h3>{primaryApp?.title ?? "Your apps"}</h3>
              <p>{workspaceId === "personal" ? <>Your money,<br />at a glance.</> : <>A clear view of<br />your workspace.</>}</p>
              <span className="mobile-home-card-action">Open workspace <ArrowUpRight size={14} aria-hidden="true" /></span>
            </div>
            <div className="mobile-home-card-owner">
              <span className="mobile-home-owner-icon"><PrimaryIcon aria-hidden="true" /></span>
              <span><small>Workspace</small><strong>{workspace.name}</strong></span>
            </div>
          </Link>

          <Link to="/knowledge-base/view" state={{ workspace: workspaceId }} className="mobile-home-library-card">
            <span className="mobile-home-card-tag">Shared</span>
            <h3>Knowledge<br />Base</h3>
            <p>Your ideas,<br />connected.</p>
            <img src="/mobile/home/balance-sculpture.webp" alt="" width="117" height="153" />
          </Link>

          <div className="mobile-home-tools-card" role="group" aria-label="Quick access">
            <Link to="/cynexus-agent" state={{ workspace: workspaceId }} aria-label="Cynexus Agent" title="Cynexus Agent"><Sparkles aria-hidden="true" /></Link>
            <Link to="/m/apps" aria-label="App directory" title="App directory"><LayoutGrid aria-hidden="true" /></Link>
            <Link to="/m/me" aria-label="Profile" title="Profile"><UserRound aria-hidden="true" /></Link>
          </div>
        </div>
      </section>
    </div>
  )
}
