import { nutrientKeys, nutrientNames } from "@/services/food/model"
import type { Nutrients } from "@/services/food/types"

export function NutrientFields({ values, onChange }: { values: Record<keyof Nutrients, string>; onChange: (key: keyof Nutrients, value: string) => void }) {
  return <div className="food-field-grid">{nutrientKeys.map(key => <label className="food-field" key={key}><span>{nutrientNames[key]} <small>{key === "caloriesKcal" ? "kcal" : "g"}</small></span><input type="number" inputMode="decimal" min="0" max="10000000" step="any" placeholder="Unknown" value={values[key]} onChange={event => onChange(key, event.target.value)} /></label>)}</div>
}
