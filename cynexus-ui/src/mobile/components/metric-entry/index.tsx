import { ArrowUp, Plus, Repeat2, X } from "lucide-react"
import type { CSSProperties } from "react"
import { useEffect, useMemo, useRef, useState } from "react"

import "./styles.css"

export type MetricEntryMode = "weight" | "body" | "workout"
export type MetricEntryValue = {
  mode: MetricEntryMode
  left: string
  right: string
  unit: string
}

type MetricEntryConfig = {
  mode: MetricEntryMode
  label: string
  unit: string
  leftValues: string[]
  rightValues: string[]
  initialLeft: number
  initialRight: number
}

const metricConfigs: MetricEntryConfig[] = [
  {
    mode: "weight",
    label: "Weight",
    unit: "kg",
    leftValues: ["70", "69", "68", "67", "66", "65", "64"],
    rightValues: ["0", "1", "2", "3", "4", "5", "6"],
    initialLeft: 3,
    initialRight: 5,
  },
  {
    mode: "body",
    label: "Waist circumference",
    unit: "cm",
    leftValues: ["95", "90", "85", "80", "75", "70", "65"],
    rightValues: ["0", "1", "2", "3", "4", "5", "6"],
    initialLeft: 3,
    initialRight: 0,
  },
  {
    mode: "workout",
    label: "Workout duration",
    unit: "hr · min",
    leftValues: ["04", "03", "02", "01", "00", "12", "11"],
    rightValues: ["40", "35", "30", "25", "20", "15", "10"],
    initialLeft: 3,
    initialRight: 3,
  },
]

function configFor(mode: MetricEntryMode) {
  return metricConfigs.find((config) => config.mode === mode) ?? metricConfigs[0]
}

function nextMode(mode: MetricEntryMode): MetricEntryMode {
  const index = metricConfigs.findIndex((config) => config.mode === mode)
  return metricConfigs[(index + 1) % metricConfigs.length].mode
}

function WheelColumn({
  values,
  selectedIndex,
  label,
  side,
  onSelect,
}: {
  values: string[]
  selectedIndex: number
  label: string
  side: "left" | "right"
  onSelect: (index: number) => void
}) {
  return (
    <div className={`mobile-metric-entry-wheel-column is-${side}`} aria-label={label}>
      {values.map((value, index) => (
        <button
          key={`${label}-${value}-${index}`}
          type="button"
          className={index === selectedIndex ? "is-selected" : ""}
          style={{ "--wheel-top": `${((index + 0.5) / values.length) * 100}%` } as CSSProperties}
          aria-label={`${label} ${value}`}
          aria-pressed={index === selectedIndex}
          onClick={() => onSelect(index)}
        >
          {value}
        </button>
      ))}
    </div>
  )
}

export function MobileMetricEntry({ initialMode, onClose, onAdd }: { initialMode: MetricEntryMode; onClose: () => void; onAdd: (entry: MetricEntryValue) => void }) {
  const [mode, setMode] = useState(initialMode)
  const config = useMemo(() => configFor(mode), [mode])
  const [leftIndex, setLeftIndex] = useState(config.initialLeft)
  const [rightIndex, setRightIndex] = useState(config.initialRight)
  const [error, setError] = useState("")
  const dialog = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const element = dialog.current
    const trigger = document.activeElement instanceof HTMLElement ? document.activeElement : null
    element?.showModal()
    return () => { element?.close(); trigger?.focus() }
  }, [])

  function switchMetric() {
    const next = nextMode(mode)
    const nextConfig = configFor(next)
    setMode(next)
    setLeftIndex(nextConfig.initialLeft)
    setRightIndex(nextConfig.initialRight)
  }

  function addMetric() {
    try {
      onAdd({ mode, left: config.leftValues[leftIndex], right: config.rightValues[rightIndex], unit: config.unit })
      onClose()
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Could not save record.") }
  }

  function moveSelectorUp() {
    setLeftIndex((index) => (index + config.leftValues.length - 1) % config.leftValues.length)
  }

  return (
    <dialog ref={dialog} className="mobile-metric-entry" aria-labelledby="mobile-metric-entry-title" onCancel={onClose}>
      <div className="mobile-metric-entry-canvas">
        <h1 id="mobile-metric-entry-title" className="sr-only">Add {config.label}</h1>
        {error && <p className="mobile-metric-entry-error" role="alert">{error}</p>}

        <button type="button" className="mobile-metric-entry-top" aria-label="Move selector up" title="Move selector up" onClick={moveSelectorUp}>
          <ArrowUp aria-hidden="true" />
        </button>

        <svg className="mobile-metric-entry-arcs" viewBox="0 0 540 620" preserveAspectRatio="none" aria-hidden="true">
          <path d="M 0 0 A 242 310 0 0 1 0 620" />
          <path d="M 540 0 A 242 310 0 0 0 540 620" />
        </svg>

        <div className="mobile-metric-entry-wheels">
          <WheelColumn values={config.leftValues} selectedIndex={leftIndex} label={`${config.label} first value`} side="left" onSelect={setLeftIndex} />
          <span className="mobile-metric-entry-colon" aria-hidden="true">:</span>
          <WheelColumn values={config.rightValues} selectedIndex={rightIndex} label={`${config.label} second value`} side="right" onSelect={setRightIndex} />
        </div>

        <div className="mobile-metric-entry-marker" aria-hidden="true" />
        <div className="mobile-metric-entry-unit" aria-live="polite">{config.unit}</div>

        <button type="button" className="mobile-metric-entry-switch" aria-label={`Switch from ${config.label}`} title="Switch metric" onClick={switchMetric}>
          <Repeat2 aria-hidden="true" />
        </button>
        <button type="button" className="mobile-metric-entry-add" onClick={addMetric}>
          <Plus aria-hidden="true" />
          <span>Add</span>
        </button>
        <button type="button" className="mobile-metric-entry-cancel" aria-label="Cancel this add" title="Cancel this add" onClick={onClose}>
          <X aria-hidden="true" />
        </button>
      </div>
    </dialog>
  )
}
