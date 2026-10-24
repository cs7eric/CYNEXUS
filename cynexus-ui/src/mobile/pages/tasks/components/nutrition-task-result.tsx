import { Check, LoaderCircle, Plus, RefreshCw } from "lucide-react"
import { useEffect, useState } from "react"

import { fetchVisionTask } from "@/services/ai/vision"
import { announceFoodChange, createCustomFood } from "@/services/food/api"
import { formatNutrition, nutrientKeys, nutrientNames } from "@/services/food/model"
import { nutritionLabelPer100g, parseNutritionLabelRecognition, type NutritionLabelRecognition } from "@/services/food/nutrition-label"

export function NutritionTaskResult({ taskId, savingDraft }: { taskId: string; savingDraft: boolean }) {
  const [label, setLabel] = useState<NutritionLabelRecognition | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reload, setReload] = useState(0)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [name, setName] = useState("")

  useEffect(() => {
    let cancelled = false
    fetchVisionTask({ processId: taskId, status: "succeeded" }).then(snapshot => {
      const content = typeof snapshot.result === "string" ? snapshot.result : snapshot.content ?? snapshot.result?.content
      if (!content) throw new Error("No recognition result is available.")
      const result = parseNutritionLabelRecognition(content)
      if (!cancelled) { setLabel(result); setName(result.foodName ?? "") }
    }).catch(cause => {
      if (!cancelled) setError(cause instanceof Error ? cause.message : "Unable to load the result.")
    }).finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [taskId, reload])

  const nutrients = label ? nutritionLabelPer100g(label) : null
  const canSave = !!label?.recognized && !!name.trim() && nutrients && nutrientKeys.some(key => nutrients[key] != null)

  async function save() {
    if (!canSave || !nutrients || saving || savingDraft) return
    setSaving(true)
    setError(null)
    try {
      await createCustomFood({ idempotencyKey: `vision-task:${taskId}`, name: name.trim(), brand: label?.brand?.trim() || undefined, nutrients })
      setSaved(true)
      announceFoodChange()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Unable to save this food.")
    } finally { setSaving(false) }
  }

  return <div className="mobile-task-result" aria-label="Nutrition recognition result">
    {loading ? <p role="status"><LoaderCircle aria-hidden="true" className="is-spinning" />Loading result...</p> : label && <>
      <label>Food name<input maxLength={200} value={name} disabled={saved || saving || savingDraft} onChange={event => setName(event.target.value)} /></label>
      {label.brand && <p>{label.brand}</p>}
      <p>Per 100 g · {label.confidence} confidence</p>
      <dl>{nutrientKeys.map(key => <div key={key}><dt>{nutrientNames[key]}</dt><dd>{formatNutrition(nutrients?.[key] ?? null, 1)} {key === "caloriesKcal" ? "kcal" : "g"}</dd></div>)}</dl>
      {!canSave && <p>No complete food name and usable nutrition basis were recognized.</p>}
      {canSave && <button type="button" className="mobile-task-save" disabled={saved || saving || savingDraft} onClick={() => void save()}>
        {saved ? <Check aria-hidden="true" /> : saving || savingDraft ? <LoaderCircle aria-hidden="true" className="is-spinning" /> : <Plus aria-hidden="true" />}
        {saved ? "Saved to My foods" : saving || savingDraft ? "Saving food..." : "Save to My foods"}
      </button>}
    </>}
    {error && <p className="mobile-task-failure" role="alert">{error}</p>}
    {!loading && !label && <button type="button" className="mobile-task-save" onClick={() => { setLoading(true); setError(null); setReload(value => value + 1) }}><RefreshCw aria-hidden="true" />Reload result</button>}
  </div>
}
