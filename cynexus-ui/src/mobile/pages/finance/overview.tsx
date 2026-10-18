import { Eye, EyeOff, RefreshCw } from "lucide-react"
import { useEffect, useRef, useState } from "react"
import { Link, useOutletContext } from "react-router-dom"

import { useWorkspace } from "@/layout/workspace-provider"
import { fetchFinanceOverview } from "@/services/finance/ledger"
import { PileBreakdown } from "./components/pile-breakdown"
import { summarizeCnyAccounts, type CnyAccountSummary } from "./lib/asset-balances"
import { formatCny } from "./lib/asset-pile"
import "./overview.css"

interface Snapshot {
  summary: CnyAccountSummary
  updatedAt: Date
}
interface LoadState {
  status: "loading" | "ready" | "error"
  snapshot?: Snapshot
}

function FittedAmount({ amount, hidden, primary = false }: { amount: number | null; hidden: boolean; primary?: boolean }) {
  const frame = useRef<HTMLSpanElement>(null)
  const figure = useRef<HTMLSpanElement>(null)
  const label = hidden ? "Amounts hidden" : amount === null ? "Unavailable" : formatCny(amount)
  useEffect(() => {
    const container = frame.current
    const content = figure.current
    if (!container || !content) return
    let active = true
    function fit() {
      if (!active || !container || !content) return
      content.style.fontSize = ""
      const natural = content.getBoundingClientRect().width
      const size = Number.parseFloat(getComputedStyle(content).fontSize)
      if (natural > container.clientWidth && natural > 0) content.style.fontSize = `${size * container.clientWidth / natural}px`
    }
    const observer = new ResizeObserver(fit)
    observer.observe(container)
    fit()
    void document.fonts.ready.then(fit)
    return () => { active = false; observer.disconnect() }
  }, [label, primary])
  const [integer, cents] = label.split(".")
  return (
    <span ref={frame} className={`finance-asset-figure${primary ? " is-primary" : ""}`}>
      <span ref={figure} aria-hidden="true">{hidden ? "••••••" : primary && amount !== null ? <>{integer}<span className="finance-asset-cents">.{cents}</span></> : label}</span>
      <span className="sr-only">{label}</span>
    </span>
  )
}

export function FinanceOverviewView() {
  const { workspaceId } = useWorkspace()
  return <FinanceAssetOverview key={workspaceId} />
}

function FinanceAssetOverview() {
  const { financeRefreshToken } = useOutletContext<{ financeRefreshToken: number }>()
  const [load, setLoad] = useState<LoadState>({ status: "loading" })
  const [refresh, setRefresh] = useState(0)
  const [hidden, setHidden] = useState(() => {
    try { return sessionStorage.getItem("cynexus.finance.hide-amounts") === "true" } catch { return false }
  })

  function togglePrivacy() {
    const next = !hidden
    setHidden(next)
    try { sessionStorage.setItem("cynexus.finance.hide-amounts", String(next)) } catch { /* Privacy still works when storage is blocked. */ }
  }

  useEffect(() => {
    let active = true
    const timer = window.setTimeout(() => {
      setLoad((previous) => ({ ...previous, status: "loading" }))
      void fetchFinanceOverview().then((overview) => {
        if (!Array.isArray(overview.accounts)) throw new Error("Account balances unavailable")
        const summary = summarizeCnyAccounts(overview.accounts)
        if (active) setLoad({ status: "ready", snapshot: { summary, updatedAt: new Date() } })
      }).catch(() => {
        if (active) setLoad((previous) => ({ ...previous, status: "error" }))
      })
    }, 0)
    return () => { active = false; window.clearTimeout(timer) }
  }, [financeRefreshToken, refresh])

  const { snapshot, status } = load
  const summary = snapshot?.summary
  const unavailable = summary && (summary.assetFen === null || summary.liabilityFen === null || summary.netFen === null)

  return (
    <section className="mobile-finance-overview" aria-label="Finance overview" aria-busy={status === "loading"}>
      <div className="finance-asset-heading"><h2>Your assets</h2><span className="finance-asset-currency">CNY</span></div>
      <div className="finance-asset-amount-line">
        {!snapshot && status === "loading" ? <span className="finance-asset-loading-amount" aria-label="Loading balances" /> : <FittedAmount amount={summary?.assetFen ?? null} hidden={hidden} primary />}
        <button type="button" className="finance-asset-icon-button" onClick={togglePrivacy} aria-label={hidden ? "Show amounts" : "Hide amounts"} aria-pressed={hidden} title={hidden ? "Show amounts" : "Hide amounts"}>{hidden ? <EyeOff aria-hidden="true" /> : <Eye aria-hidden="true" />}</button>
      </div>
      <p className="finance-asset-subtitle">Total assets · Chinese yuan</p>

      {!snapshot ? (
        <div className="finance-asset-empty" role="status">
          {status === "loading" ? <><span className="finance-asset-loading-pile" /><p>Gathering your balances…</p></> : <><strong>Balances unavailable</strong><p>Your accounts could not be loaded.</p><button type="button" className="finance-asset-text-button" onClick={() => setRefresh((value) => value + 1)}>Try again</button></>}
        </div>
      ) : hidden ? (
        <div className="finance-asset-empty"><EyeOff aria-hidden="true" /><p>Amounts hidden</p></div>
      ) : summary && summary.assetFen !== null && summary.assetFen > 0 ? (
        <PileBreakdown amountInFen={summary.assetFen} />
      ) : (
        <div className="finance-asset-empty">
          <strong>{summary?.assetFen === null ? "Balance unavailable" : summary && summary.assetFen < 0 ? "Your balance is below zero." : "A little space to grow."}</strong>
          <p>{summary?.assetFen === null ? "Some CNY balances could not be calculated." : summary && summary.assetFen < 0 ? "The pile appears when total assets are positive." : "Your CNY asset balances will appear here."}</p>
          <Link to="#finance-accounts" className="finance-asset-text-button">View accounts</Link>
        </div>
      )}

      <div className="finance-asset-metrics" aria-label="Net assets and liabilities">
        <div><span>Net assets</span>{!snapshot && status === "loading" ? <span className="finance-asset-loading-metric" aria-label="Loading net assets" /> : <FittedAmount amount={summary?.netFen ?? null} hidden={hidden} />}</div>
        <div><span>Liabilities</span>{!snapshot && status === "loading" ? <span className="finance-asset-loading-metric" aria-label="Loading liabilities" /> : <FittedAmount amount={summary?.liabilityFen ?? null} hidden={hidden} />}</div>
      </div>
      <div className="finance-asset-footer">
        <div role="status">
          {status === "error" ? <p>{snapshot ? "Could not refresh. Showing the last loaded balances." : "Could not load balances."}</p> : <p>{snapshot ? `Updated ${snapshot.updatedAt.toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" })}` : "Current account balances"}{status === "loading" && snapshot ? " · Refreshing…" : ""}</p>}
          {!hidden && summary && summary.excludedCount > 0 && <p>CNY only · Other or unspecified currencies excluded.</p>}
          {!hidden && unavailable && <p>Some CNY totals are unavailable. <Link to="#finance-accounts">Check accounts</Link>.</p>}
        </div>
        <button type="button" className="finance-asset-icon-button" onClick={() => setRefresh((value) => value + 1)} disabled={status === "loading"} aria-label="Refresh asset balances" title="Refresh asset balances"><RefreshCw size={16} aria-hidden="true" className={status === "loading" ? "is-refreshing" : undefined} /></button>
      </div>
    </section>
  )
}
