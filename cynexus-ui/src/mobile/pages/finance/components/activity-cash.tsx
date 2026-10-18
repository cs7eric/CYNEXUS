import { useEffect, useRef } from "react"

import { compactCny, decomposeAssets, formatCny } from "../lib/asset-pile"
import { AssetPile } from "./asset-pile"

export function ActivityCash({ amount, size = "summary" }: { amount: number; size?: "summary" | "entry" | "detail" }) {
  return <div className={`fa-cash is-${size}`} aria-hidden="true">{amount > 0 ? <AssetPile amountInFen={amount} /> : <span className="fa-cash-empty" />}</div>
}

export function ActivityAmount({ amount }: { amount: number | null }) {
  const frame = useRef<HTMLSpanElement>(null)
  const figure = useRef<HTMLElement>(null)
  const label = amount === null ? "—" : formatCny(amount)
  useEffect(() => {
    const container = frame.current
    const content = figure.current
    if (!container || !content) return
    let active = true
    function fit() {
      if (!active || !container || !content) return
      content.style.fontSize = ""
      const width = content.getBoundingClientRect().width
      const size = Number.parseFloat(getComputedStyle(content).fontSize)
      if (width > container.clientWidth) content.style.fontSize = `${size * container.clientWidth / width}px`
    }
    const observer = new ResizeObserver(fit)
    observer.observe(container)
    fit()
    void document.fonts.ready.then(fit)
    return () => { active = false; observer.disconnect() }
  }, [label])
  const [whole, cents] = label.split(".")
  return <span className="fa-amount-frame" ref={frame}><strong className="fa-flow-amount" ref={figure}>{whole}{cents && <small>.{cents}</small>}</strong></span>
}

export function ActivityCashBreakdown({ amount }: { amount: number }) {
  const { groups, loose } = decomposeAssets(amount)
  return <ul className="fa-money-list">{[...groups, ...loose].map((group) => <li key={`${group.kind}-${group.valueInFen}`}>
    <img src={`/mobile/finance/currency/single/${group.id}.webp`} alt="" />
    <span>{group.count.toLocaleString("en-US")} × {compactCny(group.valueInFen)}{group.kind === "bundle" ? " bundle" : ""}</span><strong>{formatCny(group.count * group.valueInFen)}</strong>
  </li>)}{amount === 0 && <li>No cash flow recorded this month.</li>}</ul>
}
