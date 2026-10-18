import { useId } from "react"
import { bodyGeometry } from "../lib/body-geometry"
import { bodyPath, bodyY } from "../lib/body-proportions"
import { getMetric, latestObservation, type BodyMetricId, type BodyObservation, type BodySide } from "../lib/model"
import { BodyMuscles } from "./body-muscles"

type Props = { observations: readonly BodyObservation[]; asOf: string; compareDate: string | null; metric: BodyMetricId; side: BodySide; view: "front" | "back"; onSelect: (metric: BodyMetricId, side: BodySide) => void }

const positions: Partial<Record<BodyMetricId, number>> = { neck: 62, shoulders: 79, shoulderWidth: 73, chest: 103, underChest: 123, waist: 146, abdomen: 167, hips: 191, upperArm: 108, forearm: 163, wrist: 193, thigh: 233, calf: 280, ankle: 315 }

export function BodyDiagram({ observations, asOf, compareDate, metric, side, view, onSelect }: Props) {
  const id = useId().replaceAll(":", "")
  const shape = bodyGeometry(observations, asOf)
  const previous = compareDate ? bodyGeometry(observations, compareDate) : null
  const front = view === "front"
  const mirror = "translate(340 0) scale(-1 1)"
  const anatomicalLeft = front ? undefined : mirror
  const anatomicalRight = front ? mirror : undefined
  const paths = (geometry: ReturnType<typeof bodyGeometry>) => <>
    <path d={geometry.leftLeg} transform={anatomicalLeft} /><path d={geometry.rightLeg} transform={anatomicalRight} />
    <path d={geometry.torso} /><path d={geometry.leftArm} transform={anatomicalLeft} /><path d={geometry.rightArm} transform={anatomicalRight} />
  </>
  const bandY = positions[metric] === undefined ? undefined : bodyY(positions[metric])
  const limb = getMetric(metric).paired
  const bandWidth = limb ? shape.limbWidth(metric, side) : shape.widths[metric] ?? 30
  const bandCenter = limb ? shape.limbCenter(metric) : 170
  const bandTransform = limb ? side === "left" ? anatomicalLeft : anatomicalRight : undefined
  const labelOnLeft = limb && (side === "left" ? !front : front)
  const displayCenter = labelOnLeft ? 340 - bandCenter : bandCenter
  const anchorX = displayCenter + (labelOnLeft ? -bandWidth : bandWidth)
  const labelY = Math.max(36, Math.min(302, bandY ?? 146))
  const labelX = labelOnLeft ? 10 : 260
  const lineEnd = labelOnLeft ? 84 : 252
  const observation = latestObservation(observations, metric, side, asOf)
  const definition = getMetric(metric)
  const oval = (cx: number, cy: number, rx: number, ry: number) => ({ cx, cy: bodyY(cy), rx, ry: (bodyY(cy + ry) - bodyY(cy - ry)) / 2 })
  const hit = (part: BodyMetricId, x: number, y: number, width: number, height: number, partSide: BodySide = null, transform?: string) => <rect key={`${part}-${partSide}`} className="workout-body-hit" x={x} y={bodyY(y)} width={width} height={bodyY(y + height) - bodyY(y)} rx="10" transform={transform} role="button" tabIndex={0} aria-label={`${partSide ? `${partSide} ` : ""}${getMetric(part).label}`} aria-pressed={metric === part && side === partSide} onClick={() => onSelect(part, partSide)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); onSelect(part, partSide) } }} />
  return <svg className="workout-body-diagram" viewBox="0 36 340 316" role="group" aria-label={`${view} body illustration. Select a measurement region.`}>
    <defs>
      <clipPath id={`${id}-body`} clipPathUnits="userSpaceOnUse">{paths(shape)}</clipPath>
      <linearGradient id={`${id}-skin`} gradientUnits="userSpaceOnUse" x1="127" y1="70" x2="216" y2="196">
        <stop stopColor="#e6dcda" /><stop offset=".35" stopColor="#fffdfb" /><stop offset=".66" stopColor="#fbf4f2" /><stop offset="1" stopColor="#e5dad8" />
      </linearGradient>
      <radialGradient id={`${id}-light`}><stop stopColor="#fffdf9" stopOpacity=".8" /><stop offset="1" stopColor="#fffdf9" stopOpacity="0" /></radialGradient>
      <radialGradient id={`${id}-shade`}><stop stopColor="#938589" stopOpacity=".18" /><stop offset="1" stopColor="#938589" stopOpacity="0" /></radialGradient>
      <radialGradient id={`${id}-ambient`}><stop stopColor="#f6f2f3" stopOpacity=".45" /><stop offset="1" stopColor="#faf8f8" stopOpacity="0" /></radialGradient>
      <radialGradient id={`${id}-shadow`}><stop stopColor="#8d8186" stopOpacity=".14" /><stop offset="1" stopColor="#8d8186" stopOpacity="0" /></radialGradient>
      <filter id={`${id}-material`} x="-10%" y="-5%" width="120%" height="110%" colorInterpolationFilters="sRGB">
        <feGaussianBlur in="SourceAlpha" stdDeviation="2.2" result="soft" />
        <feComposite in="SourceAlpha" in2="soft" operator="out" result="edge" />
        <feFlood floodColor="#9c8e91" floodOpacity=".22" result="ink" />
        <feComposite in="ink" in2="edge" operator="in" result="contour" />
        <feMerge><feMergeNode in="SourceGraphic" /><feMergeNode in="contour" /></feMerge>
      </filter>
      {previous && <mask id={`${id}-comparison`} maskUnits="userSpaceOnUse" x="0" y="0" width="340" height="352">
        <g fill="white" stroke="white" strokeWidth="2" strokeDasharray="3 3">{paths(previous)}</g>
        <g fill="black">{paths(previous)}</g>
      </mask>}
    </defs>
    <ellipse cx="170" cy="174" rx="100" ry="162" fill={`url(#${id}-ambient)`} aria-hidden="true" />
    <ellipse cx="170" cy="341" rx="51" ry="7" fill={`url(#${id}-shadow)`} aria-hidden="true" />
    <g filter={`url(#${id}-material)`} aria-hidden="true">
      <g clipPath={`url(#${id}-body)`}>
        <rect x="35" y="0" width="270" height="342" fill={`url(#${id}-skin)`} />
        <BodyMuscles id={id} shape={shape} front={front} leftTransform={anatomicalLeft} rightTransform={anatomicalRight} />
        <g fill={`url(#${id}-light)`}>
          <ellipse {...oval(158, 102, 24, 23)} /><ellipse {...oval(184, 102, 24, 23)} />
          <ellipse {...oval(168, 145, 22, 45)} />
          {([anatomicalLeft, anatomicalRight] as const).map((transform, index) => <g key={index} transform={transform}>
            <ellipse {...oval(211, 96, 14, 24)} transform={`rotate(-10 211 ${bodyY(96)})`} />
            <ellipse {...oval(225, 154, 8, 30)} transform={`rotate(-16 225 ${bodyY(154)})`} />
            <ellipse {...oval(188, 232, 15, 37)} /><ellipse {...oval(188, 286, 10, 24)} />
          </g>)}
        </g>
        <g fill={`url(#${id}-shade)`}>
          <ellipse {...oval(170, 62, 14, 15)} />
          <ellipse {...oval(170, 197, 6, 24)} />
          <ellipse {...oval(170, 113, 4, 22)} />
          {([anatomicalLeft, anatomicalRight] as const).map((transform, index) => <g key={index} transform={transform}>
            <ellipse {...oval(201, 126, 8, 21)} /><ellipse {...oval(198, 260, 6, 12)} />
            <ellipse {...oval(204, 232, 6, 35)} /><ellipse {...oval(197, 304, 5, 20)} />
          </g>)}
        </g>
        <g className="workout-body-anatomy" fill="none" stroke="#b18b93" strokeWidth=".7" strokeOpacity=".21" strokeLinecap="round">
          {front ? <>
            <path d={bodyPath("M151 77 Q161 76 167 81 M173 81 Q181 76 189 77 M170 88 L170 109 M147 105 Q155 112 165 107 M175 107 Q185 112 193 105 M169 145 Q170 147 171 145 M150 177 Q161 184 166 193 M190 177 Q179 184 174 193")} />
            <path d={bodyPath("M151 220 Q155 240 153 253 M189 220 Q185 240 187 253 M149 266 Q154 269 159 266 M181 266 Q186 269 191 266 M150 283 L151 306 M190 283 L189 306")} />
          </> : <path d={bodyPath("M170 70 Q167 106 170 154 M148 88 Q150 105 160 113 M192 88 Q190 105 180 113 M146 179 Q153 191 166 187 M194 179 Q187 191 174 187 M170 174 L170 194 M151 224 Q155 244 151 257 M189 224 Q185 244 189 257 M151 275 Q146 285 151 296 M189 275 Q194 285 189 296")} />}
        </g>
        {bandY !== undefined && <rect x={bandCenter - bandWidth - 2} y={bandY - 9} width={bandWidth * 2 + 4} height="19" transform={bandTransform} fill="#6ebca9" opacity=".17" />}
      </g>
    </g>
    {previous && <rect x="0" y="0" width="340" height="352" fill="#a3959b" opacity=".7" mask={`url(#${id}-comparison)`} aria-label={`Comparison outline ${compareDate}`} />}
    {bandY !== undefined && <g transform={bandTransform} aria-hidden="true">
      <path d={`M${bandCenter - bandWidth} ${bandY} Q${bandCenter} ${bandY - 5} ${bandCenter + bandWidth} ${bandY}`} fill="none" stroke="#2f8978" strokeOpacity=".4" strokeWidth=".8" strokeDasharray="2 2" />
      <path className="workout-body-band" d={`M${bandCenter - bandWidth} ${bandY} Q${bandCenter} ${bandY + (metric === "shoulderWidth" ? 0 : 6)} ${bandCenter + bandWidth} ${bandY}`} />
    </g>}
    <g className="workout-body-callout" pointerEvents="none">
      {bandY !== undefined && <>
        <path d={`M${anchorX} ${bandY} H${labelOnLeft ? lineEnd + 6 : lineEnd - 6} L${lineEnd} ${labelY}`} fill="none" stroke="#7baca5" strokeWidth=".7" />
        <circle cx={anchorX} cy={bandY} r="2.2" fill="#167965" stroke="white" strokeWidth="1" />
      </>}
      <text className="workout-body-callout-name" x={labelX} y={labelY - 6}>{definition.paired ? `${side === "left" ? "L" : "R"} · ` : ""}{definition.shortLabel}</text>
      <text className="workout-body-callout-value" x={labelX} y={labelY + 12}>{observation ? observation.value.toFixed(1) : "—"}<tspan className="workout-body-callout-unit" dx="3">{definition.unit}</tspan></text>
    </g>
    {hit("neck", 148, 49, 44, 25)}{hit("shoulders", 128, 71, 84, 22)}
    {hit("chest", 139, 94, 62, 25)}{hit("waist", 140, 133, 60, 25)}{hit("abdomen", 139, 159, 62, 22)}{hit("hips", 132, 182, 76, 25)}
    {(["left", "right"] as const).map((partSide) => <g key={partSide}>
      {hit("upperArm", 200, 91, 40, 44, partSide, partSide === "left" ? anatomicalLeft : anatomicalRight)}
      {hit("forearm", 214, 144, 40, 44, partSide, partSide === "left" ? anatomicalLeft : anatomicalRight)}
      {hit("wrist", 221, 188, 40, 34, partSide, partSide === "left" ? anatomicalLeft : anatomicalRight)}
      {hit("thigh", 172, 211, 38, 46, partSide, partSide === "left" ? anatomicalLeft : anatomicalRight)}
      {hit("calf", 174, 264, 36, 43, partSide, partSide === "left" ? anatomicalLeft : anatomicalRight)}
      {hit("ankle", 174, 308, 36, 34, partSide, partSide === "left" ? anatomicalLeft : anatomicalRight)}
    </g>)}
  </svg>
}
