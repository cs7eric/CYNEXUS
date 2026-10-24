import { ArrowRight, Check, ChevronRight, Dumbbell, Flame, Ruler, Save, Settings2 } from "lucide-react"
import { useMemo, useState, type ChangeEvent, type FormEvent } from "react"
import { Link } from "react-router-dom"

import { useWorkspace } from "@/layout/workspace-provider"
import { useNutritionConfig } from "@/mobile/hooks/use-nutrition-config"
import { useWorkoutRecords } from "@/mobile/hooks/use-workout-records"
import { dateKey, latestObservation } from "@/services/workout/model"
import {
  ACTIVITY_LEVELS,
  CALORIE_DEFICITS,
  calculateNutritionTargets,
  formatNutritionValue,
  type NutritionConfig,
  type NutritionSex,
} from "@/services/food/nutrition-config"

import "@/mobile/pages/configuration/configuration.css"

function numericValue(value: string) {
  return value.trim() === "" ? null : Number(value)
}

function formatMultiplier(value: number) {
  return value.toLocaleString("en-US", { maximumFractionDigits: 2 })
}

function NumberField({ label, value, unit, min, max, step, onChange }: {
  label: string
  value: number | null
  unit: string
  min: number
  max: number
  step: number
  onChange: (event: ChangeEvent<HTMLInputElement>) => void
}) {
  return <label className="mobile-configuration-field">
    <span>{label}</span>
    <span className="mobile-configuration-input-wrap"><input type="number" inputMode="decimal" min={min} max={max} step={step} value={value == null ? "" : value} onChange={onChange} /><small>{unit}</small></span>
  </label>
}

function ConfigurationContent() {
  const { data: records, storageError } = useWorkoutRecords()
  const remote = useNutritionConfig()
  const today = useMemo(() => dateKey(new Date()), [])
  const [draft, setDraft] = useState<NutritionConfig | null>(null)
  const config = draft ?? remote.config
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const latestWeight = useMemo(() => records.isSample ? null : latestObservation(records.observations, "weight", null, today), [records.isSample, records.observations, today])
  const weightKg = latestWeight?.value ?? null
  const calculation = useMemo(() => calculateNutritionTargets(weightKg, config), [weightKg, config])

  function updateNumber(key: keyof NutritionConfig, value: string) {
    setSaved(false)
    setDraft((current) => ({ ...(current ?? remote.config), [key]: numericValue(value) }))
  }

  function updateMultiplier(key: "carbsMultiplier" | "proteinMultiplier" | "fatMultiplier", value: string) {
    setSaved(false)
    setDraft((current) => ({ ...(current ?? remote.config), [key]: Number(value) }))
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    try {
      const next = await remote.save(config)
      setDraft(next)
      setSaved(true)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Could not save your configuration.")
    }
  }

  return <div className="mobile-configuration">
    <header className="mobile-configuration-heading">
      <div className="mobile-configuration-heading-icon" aria-hidden="true"><Settings2 /></div>
      <div><h2>Configuration center</h2><p>Set the rules behind your daily nutrition targets.</p></div>
    </header>

    <form className="mobile-configuration-form" onSubmit={save}>
      <section className="mobile-configuration-section" aria-labelledby="mobile-configuration-profile-title">
        <div className="mobile-configuration-section-heading"><h3 id="mobile-configuration-profile-title">Personal inputs</h3><span>Used for calories</span></div>
        <div className="mobile-configuration-list mobile-configuration-profile-list">
          <div className="mobile-configuration-readonly">
            <span className="mobile-configuration-row-icon is-sky" aria-hidden="true"><Ruler /></span>
            <span className="mobile-configuration-row-copy"><strong>Current weight</strong><small>{weightKg == null ? (records.isSample ? "No personal weight recorded" : "No weight recorded") : `${formatNutritionValue(weightKg, 1)} kg · Latest measurement`}</small></span>
            <Link to="/m/workout#workout-body" className="mobile-configuration-inline-link" title="Update body measurements">Update</Link>
          </div>
          <label className="mobile-configuration-field"><span>Sex</span><select value={config.sex ?? ""} onChange={(event) => { setSaved(false); setDraft((current) => ({ ...(current ?? remote.config), sex: (event.target.value || null) as NutritionSex | null })) }}><option value="">Choose</option><option value="male">Male</option><option value="female">Female</option></select></label>
          <NumberField label="Body fat" value={config.bodyFatPct} unit="%" min={0} max={100} step={0.1} onChange={(event) => updateNumber("bodyFatPct", event.target.value)} />
          <NumberField label="Age" value={config.ageYears} unit="years" min={1} max={120} step={1} onChange={(event) => updateNumber("ageYears", event.target.value)} />
          <label className="mobile-configuration-field"><span>Activity level</span><select value={config.activityFactor == null ? "" : String(config.activityFactor)} onChange={(event) => updateNumber("activityFactor", event.target.value)}><option value="">Choose</option>{ACTIVITY_LEVELS.map((level) => <option key={level.value} value={level.value}>{level.label}</option>)}</select></label>
          <label className="mobile-configuration-field"><span>Fat-loss pace</span><select value={config.calorieDeficit == null ? "" : String(config.calorieDeficit)} onChange={(event) => updateNumber("calorieDeficit", event.target.value)}><option value="">Choose</option>{CALORIE_DEFICITS.map((deficit) => <option key={deficit.value} value={deficit.value}>{deficit.label}</option>)}</select></label>
        </div>
      </section>

      <section className="mobile-configuration-section" aria-labelledby="mobile-configuration-macro-title">
        <div className="mobile-configuration-section-heading"><h3 id="mobile-configuration-macro-title">Macro multipliers</h3><span>Per kg body weight</span></div>
        <div className="mobile-configuration-list">
          <NumberField label="Carbohydrates" value={config.carbsMultiplier} unit="g/kg" min={0} max={20} step={0.1} onChange={(event) => updateMultiplier("carbsMultiplier", event.target.value)} />
          <NumberField label="Protein" value={config.proteinMultiplier} unit="g/kg" min={0} max={20} step={0.1} onChange={(event) => updateMultiplier("proteinMultiplier", event.target.value)} />
          <NumberField label="Fat" value={config.fatMultiplier} unit="g/kg" min={0} max={20} step={0.1} onChange={(event) => updateMultiplier("fatMultiplier", event.target.value)} />
        </div>
        <p className="mobile-configuration-help">Targets recalculate from the latest recorded weight. Defaults: 3 g/kg carbs, 1.7 g/kg protein and 0.9 g/kg fat.</p>
      </section>

      <section className="mobile-configuration-results" aria-labelledby="mobile-configuration-results-title">
        <div className="mobile-configuration-results-heading"><span className="mobile-configuration-results-icon" aria-hidden="true"><Flame /></span><div><h3 id="mobile-configuration-results-title">Daily target</h3><p>Calculated from your current inputs</p></div></div>
        <div className="mobile-configuration-calorie-result"><span>Calories</span><strong>{formatNutritionValue(calculation.caloriesKcal)} <small>kcal</small></strong></div>
        <div className="mobile-configuration-macro-results"><div><span>Carbs</span><strong>{formatNutritionValue(calculation.carbsG, 1)}<small>g</small></strong><em>{formatMultiplier(config.carbsMultiplier)}x weight</em></div><div><span>Protein</span><strong>{formatNutritionValue(calculation.proteinG, 1)}<small>g</small></strong><em>{formatMultiplier(config.proteinMultiplier)}x weight</em></div><div><span>Fat</span><strong>{formatNutritionValue(calculation.fatG, 1)}<small>g</small></strong><em>{formatMultiplier(config.fatMultiplier)}x weight</em></div></div>
        <div className="mobile-configuration-equation"><span>BMR</span><strong>{formatNutritionValue(calculation.bmrKcal)} kcal</strong><span>TDEE</span><strong>{formatNutritionValue(calculation.tdeeKcal)} kcal</strong><span>Deficit</span><strong>{config.calorieDeficit == null ? "—" : `${formatNutritionValue(config.calorieDeficit)} kcal`}</strong></div>
        {calculation.missing.length > 0 && <p className="mobile-configuration-missing" role="status">Add {calculation.missing.join(", ")} to calculate daily calories. Macro targets only need a recorded weight.</p>}
      </section>

      {remote.loading && <p className="mobile-configuration-status" role="status">Loading saved configuration…</p>}
      {remote.stale && remote.error && <p className="mobile-configuration-error" role="alert">{remote.error} <button type="button" onClick={remote.reload}>Try again</button></p>}
      {storageError && <p className="mobile-configuration-error" role="alert">{storageError}</p>}
      {error && <p className="mobile-configuration-error" role="alert">{error}</p>}
      {saved && <p className="mobile-configuration-success" role="status"><Check />Configuration saved.</p>}
      <button type="submit" className="mobile-configuration-save" disabled={remote.loading || remote.saving}><Save aria-hidden="true" />{remote.saving ? "Saving…" : "Save configuration"}</button>
    </form>

    <section className="mobile-configuration-section" aria-labelledby="mobile-configuration-basics-title">
      <div className="mobile-configuration-section-heading"><h3 id="mobile-configuration-basics-title">Personal basics</h3><span>More settings over time</span></div>
      <div className="mobile-configuration-list">
        <Link to="/m/workout#workout-body" className="mobile-configuration-row"><span className="mobile-configuration-row-icon is-sky" aria-hidden="true"><Ruler /></span><span className="mobile-configuration-row-copy"><strong>Body measurements</strong><small>Update weight and measurements</small></span><ChevronRight className="mobile-configuration-row-chevron" aria-hidden="true" /></Link>
        <Link to="/m/workout" className="mobile-configuration-row"><span className="mobile-configuration-row-icon is-lilac" aria-hidden="true"><Dumbbell /></span><span className="mobile-configuration-row-copy"><strong>Workout cycle</strong><small>Training split and cycle start</small></span><ChevronRight className="mobile-configuration-row-chevron" aria-hidden="true" /></Link>
      </div>
    </section>
    <p className="mobile-configuration-note"><ArrowRight aria-hidden="true" /> Update your weight in Body measurements, then return here to recalculate.</p>
  </div>
}

export default function MobileConfigurationPage() {
  const { workspaceId } = useWorkspace()
  return <ConfigurationContent key={workspaceId} />
}
