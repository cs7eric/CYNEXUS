// Shared by the silhouette, surface details and measurement overlays. The waist
// divides the illustrated height into 33% above and 67% below it.
const anchors = [
  [10, 10], [55, 50], [76, 67], [146, 118.24], [191, 158],
  [209, 174], [264, 244], [315, 316], [338, 338],
] as const

/** Project an original illustration coordinate while preserving its ordering. */
export function bodyY(y: number): number {
  let segment = 0
  while (segment < anchors.length - 2 && y > anchors[segment + 1][0]) segment++
  const [sourceStart, targetStart] = anchors[segment]
  const [sourceEnd, targetEnd] = anchors[segment + 1]
  return targetStart + (y - sourceStart) * (targetEnd - targetStart) / (sourceEnd - sourceStart)
}

/** Project absolute M/L/C/Q/H/V/Z paths without changing horizontal dimensions. */
export function bodyPath(path: string): string {
  let command = ""
  let coordinate = 0
  return (path.match(/[A-Za-z]|[-+]?(?:\d*\.\d+|\d+\.?\d*)(?:[eE][-+]?\d+)?/g) ?? []).map((token) => {
    if (/^[A-Za-z]$/.test(token)) {
      if (!/^[MLCQHVZ]$/.test(token)) throw new Error(`Unsupported body path command: ${token}`)
      command = token
      coordinate = 0
      return token
    }
    const isY = command === "V" || (command !== "H" && coordinate % 2 === 1)
    coordinate++
    return isY ? String(Number(bodyY(Number(token)).toFixed(3))) : token
  }).join(" ")
}
