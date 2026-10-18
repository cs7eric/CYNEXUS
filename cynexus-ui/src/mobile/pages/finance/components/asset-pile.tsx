import type { CSSProperties } from "react"

import { compactCny, getPilePieces, PILE_STAGE_HEIGHT, PILE_STAGE_WIDTH } from "../lib/asset-pile"

const BUNDLE_NOTE_LAYERS = [0, 1, 2]

export function AssetPile({ amountInFen }: { amountInFen: number }) {
  return (
    <span className="finance-asset-scene" aria-hidden="true">
      <span className="finance-asset-ground" />
      {getPilePieces(amountInFen).map((piece, index) => {
        const { group } = piece
        const source = `/mobile/finance/currency/single/${group.id}.webp`
        const thinBundle = group.kind === "bundle" && group.valueInFen === 100_000
        // Compress the exposed paper edge to suit the tilted face and overlapping pile.
        const bundleDepth = group.valueInFen >= 10_000_000 ? 8 : group.valueInFen >= 1_000_000 ? 4 : 0.8
        const style = {
          left: `${piece.x / PILE_STAGE_WIDTH * 100}%`,
          top: `${piece.y / PILE_STAGE_HEIGHT * 100}%`,
          width: `${piece.width / PILE_STAGE_WIDTH * 100}%`,
          "--piece-angle": `${piece.rotation}deg`,
          "--piece-delay": `${Math.min(index * 16, 300)}ms`,
          "--bundle-depth": `${bundleDepth}px`,
          zIndex: (group.kind === "bundle" ? 200 : group.kind === "coin" ? 400 : 0) + index + 1,
        } as CSSProperties
        return (
          <span key={piece.key} className={`finance-asset-piece is-${group.kind}${thinBundle ? " is-thin-bundle" : ""}`} style={style}>
            {group.kind === "bundle" && <span className="finance-asset-paper-stack" />}
            {group.kind === "bundle" && (
              <span className="finance-asset-note-layers">
                {BUNDLE_NOTE_LAYERS.slice(0, thinBundle ? 2 : 3).map((layer) => (
                  <img key={layer} src={source} alt="" draggable={false} className="finance-asset-note-layer" />
                ))}
              </span>
            )}
            <img src={source} alt="" draggable={false} className="finance-asset-face" />
            {group.kind === "bundle" && <><span className="finance-asset-band-fold" /><span className="finance-asset-band"><span>{compactCny(group.valueInFen)}</span></span></>}
            {piece.representedCount > 1 && (
              <span className="finance-asset-count">{group.kind === "bundle" ? `${compactCny(group.valueInFen)} ` : ""}×{piece.representedCount}</span>
            )}
          </span>
        )
      })}
    </span>
  )
}
