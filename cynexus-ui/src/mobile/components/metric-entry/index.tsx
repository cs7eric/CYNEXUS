import { ChevronUp, Plus, Repeat2, X } from "lucide-react"
import type { CSSProperties, MouseEvent as ReactMouseEvent, PointerEvent as ReactPointerEvent } from "react"
import { useEffect, useLayoutEffect, useMemo, useRef, useState } from "react"

import { getMetric } from "@/services/workout/model"

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

function descendingValues(min: number, max: number, pad = false) {
  return Array.from({ length: max - min + 1 }, (_, index) => pad ? String(max - index).padStart(2, "0") : String(max - index))
}

const weight = getMetric("weight")
const waist = getMetric("waist")
const metricConfigs: MetricEntryConfig[] = [
  {
    mode: "weight",
    label: "Weight",
    unit: "kg",
    leftValues: descendingValues(weight.min, weight.max),
    rightValues: descendingValues(0, 9),
    initialLeft: weight.max - 67,
    initialRight: 4,
  },
  {
    mode: "body",
    label: "Waist circumference",
    unit: "cm",
    leftValues: descendingValues(waist.min, waist.max),
    rightValues: descendingValues(0, 9),
    initialLeft: waist.max - 80,
    initialRight: 9,
  },
  {
    mode: "workout",
    label: "Workout duration",
    unit: "hr · min",
    leftValues: descendingValues(0, 24, true),
    rightValues: descendingValues(0, 59, true),
    initialLeft: 23,
    initialRight: 34,
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
  cycles = false,
  onSelect,
}: {
  values: string[]
  selectedIndex: number
  label: string
  side: "left" | "right"
  cycles?: boolean
  onSelect: (index: number) => void
}) {
  const column = useRef<HTMLDivElement>(null)
  const [fraction, setFraction] = useState(0)
  const [dragging, setDragging] = useState(false)
  const drag = useRef<{ pointerId: number; startY: number; startIndex: number; rowSpacing: number; moved: boolean; targetIndex: number | null; hadFocus: boolean } | null>(null)
  const suppressClick = useRef(false)
  const wheelDelta = useRef(0)
  const restoreFocus = useRef(false)

  useLayoutEffect(() => {
    if (!restoreFocus.current) return
    column.current?.querySelector<HTMLButtonElement>("button.is-selected")?.focus({ preventScroll: true })
    restoreFocus.current = false
  }, [selectedIndex])

  function boundIndex(index: number) {
    return cycles ? ((index % values.length) + values.length) % values.length : Math.max(0, Math.min(values.length - 1, index))
  }

  function focusSelected() {
    column.current?.querySelector<HTMLButtonElement>("button.is-selected")?.focus({ preventScroll: true })
  }

  useEffect(() => {
    const element = column.current
    if (!element) return
    function handleWheel(event: WheelEvent) {
      event.preventDefault()
      if (drag.current) return
      const delta = event.deltaY * (event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? element!.clientHeight : 1)
      if (Math.sign(wheelDelta.current) !== Math.sign(delta)) wheelDelta.current = 0
      wheelDelta.current += delta
      const steps = Math.trunc(wheelDelta.current / 40)
      if (!steps) return
      wheelDelta.current -= steps * 40
      const next = selectedIndex + steps
      restoreFocus.current = element!.contains(document.activeElement)
      onSelect(cycles ? ((next % values.length) + values.length) % values.length : Math.max(0, Math.min(values.length - 1, next)))
    }
    element.addEventListener("wheel", handleWheel, { passive: false })
    return () => element.removeEventListener("wheel", handleWheel)
  }, [cycles, onSelect, selectedIndex, values.length])

  function handlePointerDown(event: ReactPointerEvent<HTMLDivElement>) {
    if (event.button !== 0 || drag.current) return
    const target = (event.target as Element).closest<HTMLButtonElement>("button[data-index]")
    const canvasWidth = event.currentTarget.getBoundingClientRect().width * 2
    suppressClick.current = false
    wheelDelta.current = 0
    drag.current = {
      pointerId: event.pointerId, startY: event.clientY, startIndex: selectedIndex,
      rowSpacing: 66 * canvasWidth / 390, moved: false, targetIndex: target ? Number(target.dataset.index) : null,
      hadFocus: event.currentTarget.contains(document.activeElement),
    }
    event.currentTarget.setPointerCapture(event.pointerId)
  }

  function handlePointerMove(event: ReactPointerEvent<HTMLDivElement>) {
    const activeDrag = drag.current
    if (!activeDrag || activeDrag.pointerId !== event.pointerId) return

    const deltaY = event.clientY - activeDrag.startY
    if (Math.abs(deltaY) > 5) {
      activeDrag.moved = true
      setDragging(true)
    }
    if (!activeDrag.moved) return

    const rawPosition = activeDrag.startIndex - deltaY / activeDrag.rowSpacing
    const position = cycles ? rawPosition : Math.max(0, Math.min(values.length - 1, rawPosition))
    const index = Math.round(position)
    setFraction(position - index)
    restoreFocus.current = activeDrag.hadFocus
    onSelect(boundIndex(index))
  }

  function handlePointerEnd(event: ReactPointerEvent<HTMLDivElement>) {
    const activeDrag = drag.current
    if (!activeDrag || activeDrag.pointerId !== event.pointerId) return
    suppressClick.current = true
    drag.current = null
    setDragging(false)
    setFraction(0)
    if (event.type === "pointerup" && !activeDrag.moved && activeDrag.targetIndex != null) onSelect(activeDrag.targetIndex)
    if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId)
    if (activeDrag.hadFocus) focusSelected()
  }

  function handleClick(index: number, event: ReactMouseEvent<HTMLButtonElement>) {
    if (event.detail !== 0 && suppressClick.current) {
      event.preventDefault()
      suppressClick.current = false
      return
    }
    onSelect(index)
  }

  return (
    <div
      ref={column}
      className={`mobile-metric-entry-wheel-column is-${side}`}
      role="group"
      aria-label={label}
      data-dragging={dragging}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerEnd}
      onPointerCancel={handlePointerEnd}
      onLostPointerCapture={handlePointerEnd}
      onKeyDown={(event) => {
        if (!["ArrowUp", "ArrowDown", "Home", "End"].includes(event.key)) return
        event.preventDefault()
        const next = event.key === "Home" ? 0 : event.key === "End" ? values.length - 1 : boundIndex(selectedIndex + (event.key === "ArrowUp" ? -1 : 1))
        restoreFocus.current = true
        onSelect(next)
      }}
    >
      {Array.from({ length: values.length === 1 ? 1 : 9 }, (_, position) => {
        const offset = values.length === 1 ? 0 : position - 4
        const rawIndex = selectedIndex + offset
        if (!cycles && (rawIndex < 0 || rawIndex >= values.length)) return null
        const index = boundIndex(rawIndex)
        const value = values[index]
        const angle = (offset - fraction) * Math.PI / 9
        const x = side === "left" ? -72 + 192 * Math.cos(angle) : 267 - 192 * Math.cos(angle)
        const y = 270 + 192 * Math.sin(angle)

        return (
          <button
            key={index}
            type="button"
            className={index === selectedIndex ? "is-selected" : ""}
            data-index={index}
            tabIndex={index === selectedIndex ? 0 : -1}
            style={{ "--wheel-x": `${x / 390 * 100}cqw`, "--wheel-y": `${y / 390 * 100}cqw`, "--wheel-distance": Math.abs(offset - fraction) } as CSSProperties}
            aria-label={`${label} ${value}`}
            aria-pressed={index === selectedIndex}
            onClick={(event) => handleClick(index, event)}
          >
            {value}
          </button>
        )
      })}
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
  const upperLimit = leftIndex === 0
  const rightValues = upperLimit ? [config.rightValues.at(-1)!] : config.rightValues
  const zeroDuration = mode === "workout" && Number(config.leftValues[leftIndex]) === 0 && Number(config.rightValues[rightIndex]) === 0
  useEffect(() => {
    const element = dialog.current
    const trigger = document.activeElement instanceof HTMLElement ? document.activeElement : null
    element?.showModal()
    element?.focus({ preventScroll: true })
    return () => { element?.close(); trigger?.focus() }
  }, [])

  function switchMetric() {
    const next = nextMode(mode)
    const nextConfig = configFor(next)
    setMode(next)
    setLeftIndex(nextConfig.initialLeft)
    setRightIndex(nextConfig.initialRight)
    setError("")
  }

  function selectLeft(index: number) {
    setLeftIndex(index)
    if (index === 0) setRightIndex(config.rightValues.length - 1)
    setError("")
  }

  function selectRight(index: number) {
    setRightIndex(index)
    setError("")
  }

  function addMetric() {
    try {
      onAdd({ mode, left: config.leftValues[leftIndex], right: config.rightValues[rightIndex], unit: config.unit })
      onClose()
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Could not save record.") }
  }

  function moveSelectorUp() {
    selectLeft(Math.max(0, leftIndex - 1))
  }

  return (
    <dialog ref={dialog} className="mobile-metric-entry" aria-labelledby="mobile-metric-entry-title" onCancel={onClose}>
      <div className="mobile-metric-entry-canvas">
        <h1 id="mobile-metric-entry-title" className="mobile-metric-entry-title" aria-live="polite" aria-atomic="true">{config.label}</h1>
        {error && <p className="mobile-metric-entry-error" role="alert">{error}</p>}

        <button type="button" className="mobile-metric-entry-top" aria-label="Move selector up" title="Move selector up" onClick={moveSelectorUp}>
          <ChevronUp aria-hidden="true" />
        </button>

        <div className="mobile-metric-entry-selector">
          <div className="mobile-metric-entry-circles" aria-hidden="true">
            <span className="mobile-metric-entry-circle is-left" />
            <span className="mobile-metric-entry-circle is-right" />
          </div>

          <div className="mobile-metric-entry-wheels">
            <WheelColumn key={`${mode}-left`} values={config.leftValues} selectedIndex={leftIndex} label={`${config.label} first value`} side="left" onSelect={selectLeft} />
            <span className="mobile-metric-entry-colon" aria-hidden="true">:</span>
            <WheelColumn key={`${mode}-right-${upperLimit}`} values={rightValues} selectedIndex={upperLimit ? 0 : rightIndex} cycles={!upperLimit} label={`${config.label} second value`} side="right" onSelect={upperLimit ? () => {} : selectRight} />
          </div>

          <div className="mobile-metric-entry-marker" aria-hidden="true" />
          <div className="mobile-metric-entry-unit" aria-live="polite">{config.unit}</div>
        </div>

        <button type="button" className="mobile-metric-entry-switch" aria-label={`Switch from ${config.label}`} title="Switch metric" onClick={switchMetric}>
          <Repeat2 aria-hidden="true" />
        </button>
        {zeroDuration && <p className="mobile-metric-entry-error">Choose at least 1 minute.</p>}
        <button type="button" className="mobile-metric-entry-add" disabled={zeroDuration} onClick={addMetric}>
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
