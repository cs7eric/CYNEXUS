import { ArrowUpRight, X } from "lucide-react"
import { useId, useRef } from "react"

import { compactCny, decomposeAssets, formatCny } from "../lib/asset-pile"
import { AssetPile } from "./asset-pile"

export function PileBreakdown({ amountInFen }: { amountInFen: number }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  const { groups, loose } = decomposeAssets(amountInFen)
  const notes = loose.filter((group) => group.kind === "note").reduce((sum, group) => sum + group.count, 0)
  const coins = loose.filter((group) => group.kind === "coin").reduce((sum, group) => sum + group.count, 0)
  const looseLabel = [notes ? `${notes} ${notes === 1 ? "note" : "notes"}` : "", coins ? `${coins} ${coins === 1 ? "coin" : "coins"}` : ""].filter(Boolean).join(" · ")
  function open() { dialog.current?.showModal() }

  return (
    <>
      <button type="button" className="finance-asset-pile-trigger" onClick={open} aria-label="View banknote and coin breakdown" aria-haspopup="dialog">
        <AssetPile amountInFen={amountInFen} />
      </button>
      <button type="button" className="finance-asset-composition" onClick={open} aria-haspopup="dialog">
        <span>
          {groups.length > 0 && <span className="finance-asset-bundle-key">{groups.map((group) => `${group.count} ${group.count === 1 ? "bundle" : "bundles"} of ${compactCny(group.valueInFen)}`).join(" · ")}</span>}
          {looseLabel && <span>{looseLabel}</span>}
          <span className="finance-asset-details-label">View breakdown <ArrowUpRight size={12} aria-hidden="true" /></span>
        </span>
      </button>
      <dialog ref={dialog} className="finance-pile-dialog" aria-labelledby={titleId} onClick={(event) => { if (event.target === event.currentTarget) dialog.current?.close() }}>
        <div className="finance-pile-sheet">
          <div className="finance-pile-sheet-heading">
            <h2 id={titleId}>Inside your pile</h2>
            <button type="button" className="finance-asset-icon-button" aria-label="Close breakdown" title="Close breakdown" onClick={() => dialog.current?.close()}><X aria-hidden="true" /></button>
          </div>
          <p>The pile represents your CNY asset balances. Bundles group notes of ¥100; the remainder uses individual notes and coins.</p>
          <ul className="finance-pile-rows">
            {[...groups, ...loose].map((group) => (
              <li key={`${group.kind}-${group.valueInFen}`}>
                <img src={`/mobile/finance/currency/single/${group.id}.webp`} alt="" className={group.kind === "coin" ? "is-coin" : undefined} />
                <div><span>{group.kind === "bundle" ? `${compactCny(group.valueInFen)} group` : formatCny(group.valueInFen)} × {group.count}</span><small>{group.kind === "bundle" ? `${(group.valueInFen / 10_000).toLocaleString("en-US")} notes of ¥100 per group` : `Individual ${group.kind === "coin" ? "coins" : "notes"}`}</small></div>
                <strong>{formatCny(group.valueInFen * group.count)}</strong>
              </li>
            ))}
          </ul>
          <div className="finance-pile-total"><span>Total assets</span><strong>{formatCny(amountInFen)}</strong></div>
        </div>
      </dialog>
    </>
  )
}
