export type CurrencyAssetId =
  | "yuan-100"
  | "yuan-50"
  | "yuan-20"
  | "yuan-10"
  | "yuan-5"
  | "yuan-1"
  | "jiao-5"
  | "jiao-1"
  | "fen-1"

export interface AssetGroup {
  id: CurrencyAssetId
  valueInFen: number
  count: number
  kind: "bundle" | "note" | "coin"
}

export interface AssetDecomposition {
  groups: AssetGroup[]
  loose: AssetGroup[]
}

export interface PilePiece {
  key: string
  group: AssetGroup
  /** The number of this group represented by the visible piece and its badge. */
  representedCount: number
  x: number
  y: number
  rotation: number
  width: number
}

export const PILE_STAGE_WIDTH = 380
export const PILE_STAGE_HEIGHT = 330

const MAX_SAFE_FEN = BigInt(Number.MAX_SAFE_INTEGER)
const DENOMINATIONS: ReadonlyArray<readonly [CurrencyAssetId, number]> = [
  ["yuan-100", 10_000],
  ["yuan-50", 5_000],
  ["yuan-20", 2_000],
  ["yuan-10", 1_000],
  ["yuan-5", 500],
  ["yuan-1", 100],
  ["jiao-5", 50],
  ["jiao-1", 10],
  ["fen-1", 1],
]

const PIECE_WIDTHS: Record<CurrencyAssetId, number> = {
  "yuan-100": 177,
  "yuan-50": 168,
  "yuan-20": 164,
  "yuan-10": 160,
  "yuan-5": 156,
  "yuan-1": 151,
  "jiao-5": 32,
  "jiao-1": 30,
  "fen-1": 31,
}

const BUNDLE_SLOTS = [
  [140, 78, -12], [231, 93, 11], [104, 122, -18], [210, 133, 6],
  [152, 155, -6], [260, 164, 17], [109, 190, -14], [207, 195, 4],
  [158, 218, -8], [253, 216, 13], [102, 228, -16], [210, 245, 7],
] as const

const NOTE_SLOTS = [
  [145, 117, -15], [233, 142, 13], [139, 174, -6], [207, 196, 8],
  [111, 224, -16], [241, 229, 15], [150, 253, -9], [225, 267, 9],
  [163, 271, -6], [246, 263, 14], [113, 256, -13], [193, 265, 5],
] as const

const FRONT_NOTE_SLOTS = [
  [168, 169, -9], [254, 184, 14], [115, 205, -16], [191, 221, 7],
  [250, 234, 15], [112, 255, -12], [224, 265, 10], [170, 270, -5],
  [254, 261, 9], [111, 260, -10], [170, 258, 2], [230, 271, -8],
] as const

function isSafeFen(fen: number): boolean {
  return Number.isSafeInteger(fen)
}

/** Round the number's decimal representation to fen, with ties away from zero. */
export function moneyToFen(amount: number): number | null {
  if (!Number.isFinite(amount)) return null

  const [coefficient, exponentText] = Math.abs(amount).toString().split("e")
  const [whole, fraction = ""] = coefficient.split(".")
  const exponent = Number(exponentText ?? 0)
  const digits = BigInt(whole + fraction)
  const shift = 2 + exponent - fraction.length
  let fen: bigint

  if (shift >= 0) {
    fen = digits * 10n ** BigInt(shift)
  } else {
    const divisor = 10n ** BigInt(-shift)
    fen = digits / divisor
    if ((digits % divisor) * 2n >= divisor) fen += 1n
  }

  if (fen > MAX_SAFE_FEN) return null
  return fen === 0n ? 0 : Number(fen) * (amount < 0 ? -1 : 1)
}

export function formatCny(fen: number): string {
  if (!isSafeFen(fen)) return "Unavailable"
  const absolute = Math.abs(fen)
  const whole = Math.floor(absolute / 100).toLocaleString("en-US")
  const fraction = String(absolute % 100).padStart(2, "0")
  return `${fen < 0 ? "−" : ""}¥${whole}.${fraction}`
}

export function compactCny(fen: number): string {
  if (!isSafeFen(fen)) return "Unavailable"
  const absolute = Math.abs(fen)
  for (const [unit, suffix] of [[1e14, "t"], [1e11, "b"], [1e8, "m"], [1e5, "k"]] as const) {
    if (absolute >= unit) return `${fen < 0 ? "−" : ""}¥${absolute / unit}${suffix}`
  }
  return `${fen < 0 ? "−" : ""}¥${absolute / 100}`
}

/** Integer fen is the source of truth; bundles contain notes of ¥100. */
export function decomposeAssets(fen: number): AssetDecomposition {
  const groups: AssetGroup[] = []
  const loose: AssetGroup[] = []
  if (!isSafeFen(fen) || fen <= 0) return { groups, loose }

  let remaining = fen
  let unit = 100_000
  while (unit <= Math.floor(fen / 10)) unit *= 10
  for (; unit >= 100_000; unit /= 10) {
    const count = Math.floor(remaining / unit)
    if (!count) continue
    groups.push({ id: "yuan-100", valueInFen: unit, count, kind: "bundle" })
    remaining -= count * unit
  }

  for (const [id, valueInFen] of DENOMINATIONS) {
    const count = Math.floor(remaining / valueInFen)
    if (!count) continue
    loose.push({ id, valueInFen, count, kind: valueInFen < 100 ? "coin" : "note" })
    remaining -= count * valueInFen
  }

  return { groups, loose }
}

function stableJitter(key: string): [number, number] {
  let hash = 2166136261
  for (const character of key) hash = Math.imul(hash ^ character.charCodeAt(0), 16777619)
  return [((hash >>> 0) % 701) / 100 - 3.5, ((hash >>> 12) % 501) / 100 - 2.5]
}

function makePiece(group: AssetGroup, index: number, slot: readonly number[], representedCount: number): PilePiece {
  const key = `${group.kind}-${group.valueInFen}-${index}`
  const [jitterX, jitterY] = stableJitter(key)
  const width = PIECE_WIDTHS[group.id]
  const rotation = slot[2]
  // All note canvases are shorter than 0.53 × width; coins fit within 1.04 × width.
  // Include the bundle's paper edges and count badge in the safe stage margins.
  const height = width * (group.kind === "coin" ? 1.04 : 0.53)
  const angle = Math.abs(rotation) * Math.PI / 180
  const radiusX = (Math.abs(Math.cos(angle)) * width + Math.abs(Math.sin(angle)) * height) / 2 + 12
  const radiusY = (Math.abs(Math.sin(angle)) * width + Math.abs(Math.cos(angle)) * height) / 2 + 12
  return {
    key,
    group,
    representedCount,
    x: Math.min(PILE_STAGE_WIDTH - radiusX, Math.max(radiusX, slot[0] + jitterX)),
    y: Math.min(PILE_STAGE_HEIGHT - radiusY, Math.max(radiusY, slot[1] + jitterY)),
    rotation,
    width,
  }
}

/** Large bundles stay separate; repeated ¥1k bundles, loose notes and coins may share a badge. */
export function getPilePieces(fen: number): PilePiece[] {
  const { groups, loose } = decomposeAssets(fen)
  const pieces: PilePiece[] = []
  let bundleIndex = 0
  let noteIndex = 0
  let coinIndex = 0

  for (const group of [...groups, ...loose]) {
    const compressed = group.count > 4 && (group.kind !== "bundle" || group.valueInFen === 100_000)
    const visibleCount = compressed ? 1 : group.count
    const representedCount = compressed ? group.count : 1
    for (let index = 0; index < visibleCount; index++) {
      let slot: readonly number[]
      if (group.kind === "bundle") {
        const base = BUNDLE_SLOTS[bundleIndex % BUNDLE_SLOTS.length]
        const layer = Math.floor(bundleIndex++ / BUNDLE_SLOTS.length)
        slot = layer ? [base[0] + (layer % 2 ? -1 : 1) * Math.min(layer * 7, 28), Math.max(65, base[1] - Math.min(layer * 11, 55)), base[2] + (layer % 3 - 1) * 2] : base
      } else if (group.kind === "note") {
        const slots = groups.length ? FRONT_NOTE_SLOTS : NOTE_SLOTS
        slot = slots[noteIndex++ % slots.length]
      } else {
        slot = [284 + coinIndex % 3 * 19, 264 + Math.floor(coinIndex / 3) * 19 + coinIndex % 2 * 9, -20 + coinIndex * 31]
        coinIndex++
      }
      pieces.push(makePiece(group, index, slot, representedCount))
    }
  }

  return pieces
}
