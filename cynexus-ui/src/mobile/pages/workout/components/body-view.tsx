import { useState } from "react"
import { Plus, Pencil, ChevronDown } from "lucide-react"
import { BODY_METRICS, dateKey, formatDate as shortDate, getMetric, latestObservation, parseDate, type BodyMetricId, type BodyObservation, type BodyObservationInput, type BodySide } from "../lib/model"
import { measurementTrend } from "../lib/body-geometry"
import { BodyDiagram } from "./body-diagram"
import { MeasurementEditor } from "./measurement-editor"
import "./body-view.css"

type Props = { today: Date; observations: readonly BodyObservation[]; isSample: boolean; onSaveObservation: (input: BodyObservationInput) => void; onDeleteObservation: (id: string) => void; onStartLog: () => void }
type Period = "Week" | "Month" | "Year"
const formatDate = (value: string) => `${shortDate(value)}, ${value.slice(0, 4)}`

function startOfRange(end: string, period: Period) {
  const date = parseDate(end)
  date.setDate(date.getDate() - (period === "Week" ? 6 : period === "Month" ? 29 : 364))
  return dateKey(date)
}

export function WorkoutBodyView({ today, observations, isSample, onSaveObservation, onDeleteObservation, onStartLog }: Props) {
  const todayKey = dateKey(today)
  const [asOf, setAsOf] = useState(todayKey)
  const [rangeEnd, setRangeEnd] = useState(todayKey)
  const [metric, setMetric] = useState<BodyMetricId>("waist")
  const [side, setSide] = useState<BodySide>(null)
  const [view, setView] = useState<"front" | "back">("front")
  const [period, setPeriod] = useState<Period>("Month")
  const [compare, setCompare] = useState(false)
  const [compareDate, setCompareDate] = useState(startOfRange(todayKey, "Month"))
  const [editor, setEditor] = useState<{ observation?: BodyObservation } | null>(null)
  const [allRecords, setAllRecords] = useState(false)
  const effectiveCompareDate = compareDate > asOf ? asOf : compareDate
  const definition = getMetric(metric)
  const current = latestObservation(observations, metric, side, asOf)
  const weight = latestObservation(observations, "weight", null, asOf)
  const height = latestObservation(observations, "height", null, asOf)
  const bmiHeight = weight ? latestObservation(observations, "height", null, weight.measuredAt) : null
  const bmi = weight && bmiHeight ? weight.value / ((bmiHeight.value / 100) ** 2) : null
  const historical = latestObservation(observations, metric, side, effectiveCompareDate)
  const preceding = observations.filter((entry) => entry.metric === metric && entry.side === side && current && entry.measuredAt < current.measuredAt).sort((a, b) => b.measuredAt.localeCompare(a.measuredAt))[0]
  const baseline = compare ? historical : preceding
  const change = current && baseline && current.id !== baseline.id ? current.value - baseline.value : null
  const label = `${definition.paired ? `${side === "left" ? "Left" : "Right"} ` : ""}${definition.label}`
  // Selecting a point preserves the range; date browsing can open older history.
  const rangeStart = startOfRange(rangeEnd, period)
  const trend = measurementTrend(observations, metric, side, rangeStart, rangeEnd)
  const records = observations.filter((entry) => entry.metric === metric && entry.side === side && entry.measuredAt <= asOf).sort((a, b) => b.measuredAt.localeCompare(a.measuredAt) || b.id.localeCompare(a.id))
  const shownRecords = allRecords ? records : records.slice(0, 3)
  const low = trend.length ? Math.min(...trend.map((entry) => entry.value)) : 0
  const high = trend.length ? Math.max(...trend.map((entry) => entry.value)) : 0
  const rangeDays = period === "Week" ? 6 : period === "Month" ? 29 : 364
  const chartPoints = trend.map((entry) => {
    const date = parseDate(entry.measuredAt)
    const begin = parseDate(rangeStart)
    const days = Math.round((Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) - Date.UTC(begin.getFullYear(), begin.getMonth(), begin.getDate())) / 86400000)
    return { entry, x: 13 + days / rangeDays * 274, y: high === low ? 48 : 77 - (entry.value - low) / (high - low) * 57 }
  })
  function selectMetric(next: BodyMetricId, nextSide?: BodySide) {
    setMetric(next)
    setSide(getMetric(next).paired ? nextSide ?? "left" : null)
    setAllRecords(false)
  }
  function editSummaryMetric(nextMetric: "weight" | "height", observation?: BodyObservation | null) {
    setMetric(nextMetric)
    setSide(null)
    setEditor({ observation: isSample ? undefined : observation ?? undefined })
  }
  return <section className="workout-body-view" aria-label="Body measurements">
    <div className="workout-body-title"><h1>Your body</h1><div className="workout-body-title-actions"><span className="workout-body-source">{isSample ? "Sample data" : "Account sync enabled"}</span>{isSample && <button className="workout-body-start" onClick={onStartLog}>Start my log</button>}</div></div>
    <div className="workout-body-summary">
      <button type="button" className="workout-body-summary-cell" onClick={() => editSummaryMetric("weight", weight)} aria-label={weight ? "Edit weight" : "Record weight"}>
        <span>Weight</span><strong>{weight ? weight.value.toFixed(1) : "—"}<small>kg</small></strong><time>{weight ? formatDate(weight.measuredAt) : "Not logged"}</time><Pencil className="workout-body-summary-edit" size={13} aria-hidden="true" />
      </button>
      <button type="button" className="workout-body-summary-cell" onClick={() => editSummaryMetric("height", height)} aria-label={height ? "Edit height" : "Record height"}>
        <span>Height</span><strong>{height ? height.value.toFixed(1) : "—"}<small>cm</small></strong><time>{height ? formatDate(height.measuredAt) : "Not logged"}</time><Pencil className="workout-body-summary-edit" size={13} aria-hidden="true" />
      </button>
      <div><span>BMI</span><strong>{bmi !== null ? bmi.toFixed(1) : "—"}</strong><span className="workout-body-summary-note">{bmi !== null ? <>Weight {shortDate(weight!.measuredAt)}<br />Height {shortDate(bmiHeight!.measuredAt)}</> : "Needs height & weight"}</span></div>
    </div>
    <div className="workout-body-date-row"><label>As of<input type="date" value={asOf} max={todayKey} onChange={(event) => { if (event.target.value && event.target.value <= todayKey) { setAsOf(event.target.value); setRangeEnd(event.target.value) } }} /></label><button aria-pressed={compare} onClick={() => setCompare(!compare)}>Compare {compare ? "on" : "off"}</button></div>
    {compare && <label className="workout-body-compare-date">Dashed outline · compare with<input type="date" value={effectiveCompareDate} max={asOf} onChange={(event) => { if (event.target.value && event.target.value <= asOf) setCompareDate(event.target.value) }} /></label>}
    <div className="workout-body-figure-toolbar"><div className="workout-body-switch" aria-label="Body view">{(["front", "back"] as const).map((entry) => <button key={entry} aria-pressed={view === entry} onClick={() => setView(entry)}>{entry === "front" ? "Front" : "Back"}</button>)}</div><span>Illustration</span></div>
    <div className="workout-body-figure"><BodyDiagram observations={observations} asOf={asOf} compareDate={compare ? effectiveCompareDate : null} metric={metric} side={side} view={view} onSelect={selectMetric} /></div>
    <div className="workout-body-picker"><span>Tap a body area</span><label><span className="sr-only">All measurements</span><select value={metric} onChange={(event) => selectMetric(event.target.value as BodyMetricId)}>{BODY_METRICS.map((entry) => <option value={entry.id} key={entry.id}>{entry.label}</option>)}</select><ChevronDown size={13} aria-hidden="true" /></label></div>
    {definition.paired && <div className="workout-body-sides workout-body-switch" aria-label="Measurement side">{(["left", "right"] as const).map((entry) => <button key={entry} aria-pressed={side === entry} onClick={() => setSide(entry)}>{entry === "left" ? "Left" : "Right"}</button>)}</div>}
    <section className="workout-body-measurement" aria-label={`${label} trend`}>
      <div className="workout-body-measurement-heading"><h2>{label}</h2>{change !== null && <span>{change > 0 ? "+" : change < 0 ? "−" : ""}{Math.abs(change).toFixed(1)} {definition.unit}</span>}</div>
      <div className="workout-body-value">{current ? current.value.toFixed(1) : "—"}<small>{definition.unit}</small><button onClick={() => setEditor({})}><Plus size={16} /> Record</button></div>
      <p className="workout-body-log-date" aria-live="polite">{current ? `Measured ${formatDate(current.measuredAt)}` : "No measurement on or before this date"}{baseline && change !== null ? ` · vs ${formatDate(baseline.measuredAt)}` : ""}</p>
      <div className="workout-body-period" aria-label="Trend period">{(["Week", "Month", "Year"] as const).map((entry) => <button key={entry} aria-pressed={period === entry} onClick={() => setPeriod(entry)}>{entry}</button>)}</div>
      {trend.length ? <><svg className="workout-body-trend" viewBox="0 0 300 98" role="group" aria-label={`${label} observed measurements. Select a point to view that date.`}>
        <path d="M12 20H288 M12 49H288 M12 78H288" fill="none" stroke="#ffffff15" />
        {chartPoints.length > 1 && <polyline points={chartPoints.map(({ x, y }) => `${x},${y}`).join(" ")} fill="none" stroke="#d9f387" strokeWidth="1.5" strokeOpacity=".8" strokeLinejoin="round" aria-label="Straight guides between recorded measurements" />}
        {chartPoints.map(({ entry, x, y }) => <g key={entry.id}><circle className="workout-body-trend-hit" cx={x} cy={y} r="24" role="button" tabIndex={0} aria-label={`${formatDate(entry.measuredAt)}: ${entry.value} ${definition.unit}`} aria-pressed={current?.id === entry.id} onClick={() => setAsOf(entry.measuredAt)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); setAsOf(entry.measuredAt) } }} /><circle className="workout-body-trend-point" cx={x} cy={y} r={current?.id === entry.id ? 4.5 : 3} aria-hidden="true" /></g>)}
      </svg><div className="workout-body-chart-labels"><span>{shortDate(rangeStart)}</span><span>Measurements</span><span>{shortDate(rangeEnd)}</span></div></> : <p className="workout-body-no-trend">No measurements in this {period.toLowerCase()}. Record one to begin.</p>}
    </section>
    <div className="workout-body-records-heading"><h2>Recent measurements</h2>{records.length > 3 && <button onClick={() => setAllRecords(!allRecords)}>{allRecords ? "Show less" : `View all (${records.length})`}</button>}</div>
    {shownRecords.length ? shownRecords.map((entry) => <div className="workout-body-record" key={entry.id}><button className="workout-body-record-date" onClick={() => setAsOf(entry.measuredAt)}><strong>{formatDate(entry.measuredAt)}</strong><span>{entry.note ?? label}</span></button><b>{entry.value.toFixed(1)} <small>{definition.unit}</small></b>{!isSample && <button className="workout-body-edit" onClick={() => setEditor({ observation: entry })} aria-label={`Edit ${label} from ${formatDate(entry.measuredAt)}`}><Pencil size={16} /></button>}</div>) : <p className="workout-body-empty">Nothing recorded yet. Measurements appear here after you save them.</p>}
    <p className="workout-body-disclaimer">Illustration based on recorded measurements, with a neutral shape where data is missing. It does not estimate body fat, muscle or your actual appearance. Dates may differ by measurement.</p>
    {editor && <MeasurementEditor today={todayKey} initialMetric={editor.observation?.metric ?? metric} initialSide={editor.observation?.side ?? side} initialDate={asOf} observation={editor.observation} isSample={isSample} onSave={onSaveObservation} onDelete={onDeleteObservation} onClose={() => setEditor(null)} />}
  </section>
}
