import { bodyPath } from "../lib/body-proportions"
import type { bodyGeometry } from "../lib/body-geometry"

type Props = {
  id: string
  shape: ReturnType<typeof bodyGeometry>
  front: boolean
  leftTransform?: string
  rightTransform?: string
}

/** Soft muscle planes sit below the skin highlights and share the body clip. */
export function BodyMuscles({ id, shape, front, leftTransform, rightTransform }: Props) {
  const chest = 170 + (shape.widths.chest ?? 31)
  const waist = 170 + (shape.widths.waist ?? 26)
  const hips = 170 + (shape.widths.hips ?? 34)
  const mirror = "translate(340 0) scale(-1 1)"
  const pec = `M173 86 C182 84 ${chest - 7} 84 ${chest - 4} 91 C${chest} 97 ${chest - 4} 107 ${chest - 10} 109 C184 113 177 111 173 108 C171 101 172 92 173 86Z`
  const lat = `M175 91 C183 86 ${chest - 1} 88 ${chest} 99 C${chest - 2} 115 ${waist - 3} 134 ${waist - 7} 145 C183 140 177 125 175 113Z`
  const glute = `M174 179 C182 173 ${hips - 7} 176 ${hips - 4} 187 C${hips - 2} 199 183 203 174 195Z`

  return <g className="workout-body-muscles" aria-hidden="true" pointerEvents="none">
    <defs>
      <linearGradient id={`${id}-muscle`} x1="0" y1="0" x2="1" y2=".3">
        <stop stopColor="#f8e2e4" /><stop offset=".55" stopColor="#edbec3" /><stop offset="1" stopColor="#f4d2d5" />
      </linearGradient>
      <linearGradient id={`${id}-muscle-soft`} x1="0" y1="0" x2="1" y2=".3">
        <stop stopColor="#f9e8e9" /><stop offset=".55" stopColor="#f1cdd1" /><stop offset="1" stopColor="#f8e4e6" />
      </linearGradient>
    </defs>
    <g fill={`url(#${id}-muscle)`} opacity=".85">
      {front ? <>
        <path d={bodyPath(pec)} /><path d={bodyPath(pec)} transform={mirror} />
        {[0, 1, 2].map((row) => <g key={row}>
          <path d={bodyPath(`M173 ${117 + row * 14} Q183 ${115 + row * 14} 182 ${123 + row * 14} Q182 ${130 + row * 14} 173 ${128 + row * 14}Z`)} />
          <path d={bodyPath(`M167 ${117 + row * 14} Q157 ${115 + row * 14} 158 ${123 + row * 14} Q158 ${130 + row * 14} 167 ${128 + row * 14}Z`)} />
        </g>)}
      </> : <>
        <path d={bodyPath(lat)} /><path d={bodyPath(lat)} transform={mirror} />
        <path d={bodyPath(glute)} /><path d={bodyPath(glute)} transform={mirror} />
        <path d={bodyPath("M174 68 Q186 73 191 82 Q181 92 174 103Z M166 68 Q154 73 149 82 Q159 92 166 103Z")} />
      </>}
      {(["left", "right"] as const).map((side) => {
        const upper = shape.limbWidth("upperArm", side)
        const fore = shape.limbWidth("forearm", side)
        const thigh = shape.limbWidth("thigh", side)
        const calf = shape.limbWidth("calf", side)
        const shoulder = shape.limbCenter("upperArm")
        return <g key={side} transform={side === "left" ? leftTransform : rightTransform}>
          <path d={bodyPath(`M${shoulder - 8} 80 C${shoulder + 6} 76 ${shoulder + upper - 1} 85 ${shoulder + upper - 2} 96 Q${shoulder + 3} 104 ${shoulder - 6} 97Z`)} />
          <path d={bodyPath(`M${shoulder + 1} 103 C${shoulder + upper} 107 ${221 + upper * .2} 120 220 128 Q214 130 212 120Z`)} />
          <path d={bodyPath(`M219 140 Q${225 + fore * .7} 149 ${232 + fore * .3} 175 Q228 177 226 168Z`)} />
          <path d={bodyPath(`M${190 - thigh * .58} 205 C${190 - thigh * .2} 196 ${190 + thigh * .7} 205 ${190 + thigh * .7} 220 C${190 + thigh * .7} 234 194 248 192 253 Q188 251 186 242 C${190 - thigh * .5} 229 ${190 - thigh * .7} 215 ${190 - thigh * .58} 205Z`)} />
          <path d={bodyPath(`M188 271 Q${190 - calf * .65} 279 ${190 - calf * .6} 286 Q${190 - calf * .3} 299 189 306 Q${190 + calf * .7} 289 ${190 + calf * .5} 280 Q194 271 192 270Z`)} />
        </g>
      })}
    </g>
    {front && <g fill={`url(#${id}-muscle-soft)`}>
      <path d={bodyPath(`M${waist - 3} 128 Q${waist - 4} 147 ${hips - 14} 164 Q185 155 185 142Z`)} />
      <path d={bodyPath(`M${waist - 3} 128 Q${waist - 4} 147 ${hips - 14} 164 Q185 155 185 142Z`)} transform={mirror} />
    </g>}
  </g>
}
