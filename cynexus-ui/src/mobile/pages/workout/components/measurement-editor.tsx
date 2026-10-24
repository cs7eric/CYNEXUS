import { useEffect, useRef, useState, type FormEvent } from "react"
import { createPortal } from "react-dom"
import { Trash2, X } from "lucide-react"
import { useMobilePortal } from "@/mobile/hooks/use-mobile-portal"
import { BODY_METRICS, getMetric, type BodyMetricId, type BodyObservation, type BodyObservationInput, type BodySide } from "../lib/model"
import "./body-view.css"

const instructions: Partial<Record<BodyMetricId, string>> = {
  upperArm: "Keep your arm relaxed at your side. Measure midway between shoulder and elbow, without flexing.",
  forearm: "Keep your arm relaxed. Measure around the widest part of your forearm.",
  waist: "Measure midway between your lowest rib and hip bone after a normal breath out.",
  abdomen: "Measure at your navel, keeping the tape level and your abdomen relaxed.",
  hips: "Stand with feet together. Measure around the fullest part of your hips.",
  chest: "Keep the tape level around the fullest part of your chest, with arms relaxed.",
  shoulderWidth: "Measure the straight distance between the outer shoulder points. This is a length, not a circumference.",
  shoulders: "Measure around both shoulders at their widest point, arms relaxed.",
  thigh: "Stand relaxed and measure around the fullest part of the selected thigh.",
  calf: "Measure around the widest part of the selected calf.",
  weight: "Use the same scale and similar conditions for more consistent records.",
  height: "Stand barefoot, upright against a flat wall. Record height in centimeters.",
}

type Props = { today: string; initialMetric: BodyMetricId; initialSide: BodySide; initialDate: string; observation?: BodyObservation; isSample?: boolean; onSave: (input: BodyObservationInput) => void; onDelete: (id: string) => void; onClose: () => void }

export function MeasurementEditor({ today, initialMetric, initialSide, initialDate, observation, isSample = false, onSave, onDelete, onClose }: Props) {
  const portal = useMobilePortal()
  const dialog = useRef<HTMLDialogElement>(null)
  const [metric, setMetric] = useState(initialMetric)
  const [side, setSide] = useState<BodySide>(initialSide ?? "left")
  const [value, setValue] = useState(observation ? String(observation.value) : "")
  const [date, setDate] = useState(observation?.measuredAt ?? initialDate)
  const [note, setNote] = useState(observation?.note ?? "")
  const [error, setError] = useState("")
  const [confirmDelete, setConfirmDelete] = useState(false)
  const definition = getMetric(metric)
  useEffect(() => {
    const element = dialog.current
    const trigger = document.activeElement instanceof HTMLElement ? document.activeElement : null
    element?.showModal()
    return () => { element?.close(); trigger?.focus() }
  }, [])
  function save(event: FormEvent) {
    event.preventDefault()
    const numeric = Number(value)
    const actualDate = new Date(`${date}T12:00:00`)
    if (!date || !Number.isFinite(actualDate.getTime()) || actualDate.getFullYear() !== Number(date.slice(0, 4)) || actualDate.getMonth() + 1 !== Number(date.slice(5, 7)) || actualDate.getDate() !== Number(date.slice(8, 10)) || date > today) { setError("Choose a valid date on or before today."); return }
    if (!value.trim() || !Number.isFinite(numeric) || numeric < definition.min || numeric > definition.max) { setError(`Enter a value from ${definition.min} to ${definition.max} ${definition.unit}.`); return }
    try {
      onSave({ ...(observation ? { id: observation.id } : {}), metric, side: definition.paired ? side : null, value: numeric, measuredAt: date, ...(note.trim() ? { note: note.trim() } : {}) })
      onClose()
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Could not save this measurement. Please try again.") }
  }
  const content = <dialog ref={dialog} className="workout-measurement-dialog" aria-labelledby="measurement-editor-title" onCancel={onClose} onClick={(event) => { if (event.target === event.currentTarget) onClose() }}>
    <div className="workout-measurement-sheet">
      <header><h2 id="measurement-editor-title">{observation ? "Edit measurement" : "Record measurement"}</h2><button type="button" onClick={onClose} aria-label="Close measurement editor"><X size={21} /></button></header>
      <p className="workout-body-muted">Account sync enabled</p>
      {isSample && <p className="workout-measurement-guide">Saving starts your personal log. Sample measurements and workouts will be excluded.</p>}
      <form onSubmit={save}>
        <label>Measurement<select value={metric} onChange={(event) => { setMetric(event.target.value as BodyMetricId); setError(""); setConfirmDelete(false) }}>{BODY_METRICS.map((entry) => <option key={entry.id} value={entry.id}>{entry.label} ({entry.unit})</option>)}</select></label>
        {definition.paired && <label>Side<select value={side ?? "left"} onChange={(event) => setSide(event.target.value as BodySide)}><option value="left">Left</option><option value="right">Right</option></select></label>}
        <p className="workout-measurement-guide">{instructions[metric] ?? "Keep the tape level and snug without compressing the skin. Use the same position each time."}</p>
        <div className="workout-measurement-fields"><label>Value ({definition.unit})<input autoFocus type="number" inputMode="decimal" required min={definition.min} max={definition.max} step={definition.step} value={value} onChange={(event) => setValue(event.target.value)} /></label><label>Measured on<input type="date" required max={today} value={date} onChange={(event) => setDate(event.target.value)} /></label></div>
        <label>Note <span className="workout-body-muted">(optional)</span><textarea rows={2} maxLength={240} value={note} onChange={(event) => setNote(event.target.value)} placeholder="Position or conditions" /></label>
        {error && <p role="alert" className="workout-measurement-error">{error}</p>}
        <button className="workout-measurement-save" type="submit">Save measurement</button>
      </form>
      {observation && <div className="workout-measurement-delete">{confirmDelete ? <><p>Delete this measurement? This cannot be undone.</p><div><button type="button" onClick={() => setConfirmDelete(false)}>Keep measurement</button><button className="is-destructive" type="button" onClick={() => { try { onDelete(observation.id); onClose() } catch (cause) { setError(cause instanceof Error ? cause.message : "Could not delete this measurement. Please try again.") } }}>Delete permanently</button></div></> : <button type="button" onClick={() => setConfirmDelete(true)}><Trash2 size={16} /> Delete measurement</button>}</div>}
    </div>
  </dialog>
  return portal ? createPortal(content, portal) : content
}
