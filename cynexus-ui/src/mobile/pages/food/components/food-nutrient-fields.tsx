import { nutrientKeys, nutrientNames } from "@/services/food/model"
import type { Nutrients } from "@/services/food/types"

export function NutrientFields({ values, placeholder = "Unknown", onChange }: { values: Record<keyof Nutrients, string>; placeholder?: string; onChange: (key: keyof Nutrients, value: string) => void }) {
  return <div className="food-field-grid food-nutrient-grid">{nutrientKeys.map(key => <label className={`food-field food-nutrient-${key}`} key={key}><span className="food-nutrient-name">{nutrientNames[key]}</span><span className="food-nutrient-value"><input type="number" inputMode="decimal" min="0" max="10000000" step="any" placeholder={placeholder} value={values[key]} onChange={event => onChange(key, event.target.value)} /><small className="food-nutrient-unit">{key === "caloriesKcal" ? "kcal" : "g"}</small></span></label>)}</div>
}
