export function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value))
}

const TEMPERATURE_COLOR_STOPS: [number, number, number, number][] = [
  [0, 0, 40, 230],
  [10, 0, 70, 242],
  [20, 15, 100, 250],
  [30, 45, 128, 253],
  [40, 85, 158, 255],
  [50, 245, 245, 245],
  [60, 255, 153, 153],
  [70, 255, 102, 102],
  [80, 255, 0, 0],
  [85, 229, 0, 0],
  [90, 203, 0, 0],
  [95, 178, 0, 0],
  [100, 152, 0, 0],
]

export function scoreToRgb(score: number): [number, number, number] {
  const s = clamp(score, 0, 100)
  const stops = TEMPERATURE_COLOR_STOPS
  if (s <= stops[0][0]) return [stops[0][1], stops[0][2], stops[0][3]]
  for (let i = 0; i < stops.length - 1; i++) {
    const [v0, r0, g0, b0] = stops[i]
    const [v1, r1, g1, b1] = stops[i + 1]
    if (s >= v0 && s <= v1) {
      const t = (s - v0) / (v1 - v0)
      return [
        Math.round(r0 + (r1 - r0) * t),
        Math.round(g0 + (g1 - g0) * t),
        Math.round(b0 + (b1 - b0) * t),
      ]
    }
  }
  const last = stops[stops.length - 1]
  return [last[1], last[2], last[3]]
}

export function scoreToColor(score: number): string {
  const [r, g, b] = scoreToRgb(score)
  return `rgb(${r},${g},${b})`
}
