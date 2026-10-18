import { useRef, useState } from "react"
import { I18nProvider } from "react-aria-components/I18nProvider"
import { Outlet } from "react-router-dom"

import { useWorkspace } from "@/layout/workspace-provider"
import { MobilePortalContext } from "@/mobile/hooks/use-mobile-portal"
import { useMobileScroll } from "@/mobile/hooks/use-mobile-scroll"
import { useMobileViewport } from "@/mobile/hooks/use-mobile-viewport"
import { useWorkoutRecords } from "@/mobile/hooks/use-workout-records"
import { MobileHeader } from "@/mobile/layout/mobile-header"
import { MobileTabBar } from "@/mobile/layout/mobile-tab-bar"
import { MobileMetricEntry, type MetricEntryMode, type MetricEntryValue } from "@/mobile/components/metric-entry"
import { dateKey, saveObservation, saveSession } from "@/services/workout/model"
import "@/mobile/styles.css"

export default function MobileLayout() {
  const { workspaceId } = useWorkspace()
  const theme = useRef<HTMLDivElement>(null)
  const main = useRef<HTMLElement>(null)
  const [portal, setPortal] = useState<HTMLDivElement | null>(null)
  const [financeRefreshToken, setFinanceRefreshToken] = useState(0)
  const [metricEntryMode, setMetricEntryMode] = useState<MetricEntryMode | null>(null)
  const { update: updateWorkout } = useWorkoutRecords()
  function addMetric(entry: MetricEntryValue) {
    const date = dateKey(new Date())
    updateWorkout((data) => entry.mode === "workout"
      ? saveSession(data, { date, category: "Strength", minutes: Number(entry.left) * 60 + Number(entry.right) }, date, crypto.randomUUID())
      : saveObservation(data, { metric: entry.mode === "weight" ? "weight" : "waist", side: null, value: Number(`${entry.left}.${entry.right}`), measuredAt: date }, date, crypto.randomUUID()))
  }
  useMobileScroll(main, workspaceId)
  useMobileViewport(theme)

  return (
    <I18nProvider locale="en-US">
      <MobilePortalContext.Provider value={portal}>
        <div ref={theme} className="mobile-theme" lang="en">
          <div className="mobile-shell">
            <MobileHeader onFinanceRefresh={() => setFinanceRefreshToken((value) => value + 1)} />
            <main ref={main} id="mobile-content" className="mobile-content">
              <div className="mobile-route-container mx-auto w-full max-w-2xl px-5 pb-6"><Outlet context={{ financeRefreshToken }} /></div>
            </main>
            <MobileTabBar onOpenMetricEntry={setMetricEntryMode} />
            {metricEntryMode && <MobileMetricEntry initialMode={metricEntryMode} onClose={() => setMetricEntryMode(null)} onAdd={addMetric} />}
          </div>
          <div ref={setPortal} />
        </div>
      </MobilePortalContext.Provider>
    </I18nProvider>
  )
}
