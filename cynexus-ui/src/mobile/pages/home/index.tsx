import { ArrowUpRight, LayoutGrid, Sparkles, UserRound } from "lucide-react"
import { useEffect, useState } from "react"
import { Link } from "react-router-dom"

import { useWorkspace } from "@/layout/workspace-provider"
import { MobileActivityCalendar } from "@/mobile/components/activity-calendar"
import { activityDateKey, activityMonthStart, shiftActivityDate, shiftActivityMonth, type FinanceSummarySnapshot } from "@/mobile/components/activity-calendar/model"
import { createActivityPreview, createBodyMeasurementPreview } from "@/mobile/components/activity-calendar/preview"
import { getMobileAppGroups } from "@/mobile/navigation"
import { useFoodDay } from "@/mobile/pages/food/hooks/use-food-data"
import { useNutritionConfig } from "@/mobile/hooks/use-nutrition-config"
import { useWorkoutRecords } from "@/mobile/hooks/use-workout-records"
import { calculateNutritionTargets } from "@/services/food/nutrition-config"
import { bodySummary, latestObservation } from "@/services/workout/model"
import { fetchFinanceOverview } from "@/services/finance/ledger"
import "@/mobile/pages/home/home.css"

export default function MobileHomePage() {
  const { workspace, workspaceId } = useWorkspace()
  const { data: workoutRecords } = useWorkoutRecords()
  const groups = getMobileAppGroups(workspaceId)
  const primaryApp = groups.find((group) => group.id !== "shared")?.items[0]
  const financeAvailable = groups.some((group) => group.items.some((item) => item.url === "/finance/overview"))
  const today = new Date()
  const todayKey = activityDateKey(today)
  const foodDay = useFoodDay(todayKey)
  const nutrition = useNutritionConfig()
  const latestWeight = workoutRecords.isSample ? null : latestObservation(workoutRecords.observations, "weight", null, todayKey)
  const calculatedTargets = calculateNutritionTargets(latestWeight?.value ?? null, nutrition.config)
  const nutritionState = workoutRecords.isSample
    ? "ready"
    : nutrition.loading || (foodDay.loading && !foodDay.data)
      ? "loading"
      : nutrition.error || foodDay.error
        ? "error"
        : "ready"
  const nutritionRecord = foodDay.data ? (() => {
    const day = foodDay.data
    const hasFood = day.items.length > 0
    const total = (key: "caloriesKcal" | "carbsG" | "proteinG" | "fatG") => day.missingCounts[key] > 0 ? day.knownTotals[key] : day.totals[key]
    return {
      nutrition: hasFood ? {
        calories: total("caloriesKcal") ?? 0,
        meals: new Set(day.items.map((item) => item.mealType)).size,
        carbsGrams: total("carbsG"),
        proteinGrams: total("proteinG"),
        fatGrams: total("fatG"),
      } : null,
      nutritionTargets: {
        calories: calculatedTargets.caloriesKcal ?? day.targets.caloriesKcal,
        carbsGrams: calculatedTargets.carbsG ?? day.targets.carbsG,
        proteinGrams: calculatedTargets.proteinG ?? day.targets.proteinG,
        fatGrams: calculatedTargets.fatG ?? day.targets.fatG,
      },
    }
  })() : null
  const activityRecords = workoutRecords.isSample
    ? createActivityPreview(today)
    : (() => {
      const sessions = workoutRecords.sessions.map((session) => ({ date: session.date, nutrition: null, nutritionTargets: null, workout: { minutes: session.minutes, sessions: 1, label: session.category === "Cardio" ? "Cardio" : session.split ?? "Strength" } }))
      const todaySession = sessions.find((session) => session.date === todayKey)
      const todayActivity = { date: todayKey, nutrition: nutritionRecord?.nutrition ?? null, nutritionTargets: nutritionRecord?.nutritionTargets ?? null, workout: todaySession?.workout ?? null }
      return [todayActivity, ...sessions.filter((session) => session.date !== todayKey)]
    })()
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
      <MobileActivityCalendar today={today} records={activityRecords} bodyMeasurements={workoutRecords.isSample ? createBodyMeasurementPreview(today) : bodySummary(workoutRecords.observations, todayKey)} finance={finance} showFinance={financeAvailable} onFinanceRetry={() => setFinanceRetry((value) => value + 1)} isSample={workoutRecords.isSample} nutritionState={nutritionState} showWeek={false} />

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
