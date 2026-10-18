import { latestObservation, type BodyMetricId, type BodyObservation, type BodySide } from "./model"
import { bodyPath } from "./body-proportions"

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value))

/** Bounded illustration dimensions, never a body-composition or appearance estimate. */
export function bodyGeometry(observations: readonly BodyObservation[], asOf: string) {
  const dimension = (metric: BodyMetricId, neutral: number, reference: number, side: BodySide = null) => {
    const entry = latestObservation(observations, metric, side, asOf)
    return entry ? neutral * clamp(entry.value / reference, 0.72, 1.35) : neutral
  }
  const head = dimension("head", 17, 56)
  const neck = dimension("neck", 9, 37)
  const shoulder = dimension("shoulderWidth", dimension("shoulders", 35, 110), 43)
  const chest = dimension("chest", 31, 96)
  const underChest = dimension("underChest", 29, 84)
  const waist = dimension("waist", 26, 80)
  const abdomen = dimension("abdomen", 29, 86)
  const hips = dimension("hips", 34, 98)
  // The neck, shoulder caps and upper thighs overlap inside the silhouette.
  // Their union has a continuous contour rather than visible joint boundaries.
  const torso = `M${170 - neck} 47 C${170 - neck + 1} 54 ${170 - neck + 1} 59 ${170 - neck - 2} 64 C${170 - neck - 7} 68 ${170 - shoulder + 5} 70 ${170 - shoulder} 76 C${170 - shoulder - 3} 82 ${170 - chest - 1} 94 ${170 - chest} 105 C${170 - chest + 1} 120 ${170 - underChest} 128 ${170 - waist} 146 C${170 - waist - 1} 160 ${170 - abdomen - 2} 171 ${170 - hips} 185 C${170 - hips - 1} 197 ${170 - hips + 5} 208 150 213 C159 216 164 210 170 207 C176 210 181 216 190 213 C${170 + hips - 5} 208 ${170 + hips + 1} 197 ${170 + hips} 185 C${170 + abdomen + 2} 171 ${170 + waist + 1} 160 ${170 + waist} 146 C${170 + underChest} 128 ${170 + chest - 1} 120 ${170 + chest} 105 C${170 + chest + 1} 94 ${170 + shoulder + 3} 82 ${170 + shoulder} 76 C${170 + shoulder - 5} 70 ${170 + neck + 7} 68 ${170 + neck + 2} 64 C${170 + neck - 1} 59 ${170 + neck - 1} 54 ${170 + neck} 47Z`
  const arms = (side: "left" | "right") => {
    const upper = dimension("upperArm", 10, 31, side)
    const fore = dimension("forearm", 8, 26, side)
    const wrist = dimension("wrist", 5, 17, side)
    const shoulderX = 170 + shoulder + 4
    return `M${shoulderX - 12} 76 C${shoulderX - 4} 71 ${shoulderX + upper - 1} 75 ${shoulderX + upper + 1} 89 C${shoulderX + upper + 3} 103 ${217 + upper - 2} 119 ${217 + fore - 1} 132 C${221 + fore} 143 ${229 + fore - 1} 162 ${237 + wrist - 2} 182 C${237 + wrist - 1} 188 ${241 + wrist} 196 ${242 + wrist} 203 C${243 + wrist} 208 ${241 + wrist} 212 ${239 + wrist} 210 L${234 + wrist} 199 C${235 + wrist} 204 ${233 + wrist} 204 ${232 + wrist} 201 L${230 + wrist} 192 C${229 + wrist} 188 ${237 - wrist} 186 ${237 - wrist} 181 C${229 - fore} 165 ${217 - fore + 4} 146 ${217 - fore + 1} 135 C${215 - upper + 1} 119 ${shoulderX - 8} 106 ${shoulderX - 12} 96Z`
  }
  const legs = (side: "left" | "right") => {
    const thigh = dimension("thigh", 17, 55, side)
    const calf = dimension("calf", 10, 36, side)
    const ankle = dimension("ankle", 6, 22, side)
    const knee = Math.max(calf * .78, ankle)
    // A continuous thigh taper meets a softly defined knee. The calf's fullness
    // starts high, then follows one long taper down to the ankle.
    return `M174 183 C186 179 ${170 + hips - 2} 182 ${170 + hips} 194 C${170 + hips + 1} 206 ${190 + thigh * .9} 218 ${190 + thigh * .82} 231 C${190 + thigh * .74} 246 ${190 + knee} 254 ${190 + knee} 264 C${190 + knee} 270 ${190 + calf} 276 ${190 + calf} 282 C${190 + calf} 291 ${190 + ankle} 305 ${190 + ankle} 315 C${190 + ankle - 1} 320 ${190 + ankle - 1} 322 ${190 + ankle + 2} 325 C${190 + ankle + 5} 329 207 332 206 335 C204 339 192 338 ${190 - ankle - 1} 334 C${190 - ankle - 3} 331 ${190 - ankle} 323 ${190 - ankle} 315 C${190 - ankle} 304 ${190 - calf} 290 ${190 - calf} 282 C${190 - calf} 276 ${190 - knee} 270 ${190 - knee} 264 C${190 - knee} 254 ${190 - thigh * .62} 245 ${190 - thigh * .7} 231 C${190 - thigh * .8} 220 172 215 170 209Z`
  }
  const widths: Partial<Record<BodyMetricId, number>> = { head: head * .89, neck, shoulders: shoulder, shoulderWidth: shoulder, chest, underChest, waist, abdomen, hips }
  return { head: bodyPath(`M170 10 C${170 - head * .58} 10 ${170 - head * .91} 16 ${170 - head * .91} 25 C${170 - head * .91} 27 ${170 - head * .88} 29 ${170 - head * .88} 31 C${170 - head * .96} 30 ${170 - head * .95} 37 ${170 - head * .84} 39 C${170 - head * .81} 42 ${170 - head * .72} 45 ${170 - head * .62} 48 C${170 - head * .49} 51 ${170 - head * .34} 54 ${170 - head * .18} 55 L${170 + head * .18} 55 C${170 + head * .34} 54 ${170 + head * .49} 51 ${170 + head * .62} 48 C${170 + head * .72} 45 ${170 + head * .81} 42 ${170 + head * .84} 39 C${170 + head * .95} 37 ${170 + head * .96} 30 ${170 + head * .88} 31 C${170 + head * .88} 29 ${170 + head * .91} 27 ${170 + head * .91} 25 C${170 + head * .91} 16 ${170 + head * .58} 10 170 10Z`), torso: bodyPath(torso), leftArm: bodyPath(arms("left")), rightArm: bodyPath(arms("right")), leftLeg: bodyPath(legs("left")), rightLeg: bodyPath(legs("right")), widths,
    limbCenter(metric: BodyMetricId) {
      // The upper arm follows the shoulder; lower arm and leg anchors stay fixed.
      const centers: Partial<Record<BodyMetricId, number>> = { upperArm: 213 + (shoulder - 35) * .5, forearm: 229, wrist: 237, thigh: 190, calf: 190, ankle: 190 }
      return centers[metric] ?? 170
    },
    limbWidth(metric: BodyMetricId, side: BodySide) {
      const definitions: Partial<Record<BodyMetricId, [number, number]>> = { upperArm: [10, 31], forearm: [8, 26], wrist: [5, 17], thigh: [17 * .76, 55], calf: [10, 36], ankle: [6, 22] }
      const def = definitions[metric]
      return def ? dimension(metric, def[0], def[1], side) : 0
    },
  }
}

export function measurementTrend(observations: readonly BodyObservation[], metric: BodyMetricId, side: BodySide, start: string, end: string) {
  return observations.filter((entry) => entry.metric === metric && entry.side === side && entry.measuredAt >= start && entry.measuredAt <= end).sort((a, b) => a.measuredAt.localeCompare(b.measuredAt) || a.id.localeCompare(b.id))
}
