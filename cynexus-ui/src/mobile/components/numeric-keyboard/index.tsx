import { Check, Delete } from "lucide-react"
import { useEffect, useRef, useState } from "react"

import { applyNumericKey } from "./model"
import "./styles.css"

type NumericInput = HTMLInputElement & { inputMode: string }

function isNumericInput(target: EventTarget | null): target is NumericInput {
  return target instanceof HTMLInputElement
    && (target.type === "number" || ["decimal", "numeric", "tel"].includes(target.inputMode))
}

function setInputValue(input: HTMLInputElement, value: string) {
  const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value")?.set
  setter?.call(input, value)
  input.dispatchEvent(new Event("input", { bubbles: true }))
}

export function MobileNumericKeyboard() {
  const dialog = useRef<HTMLDialogElement>(null)
  const input = useRef<NumericInput | null>(null)
  const [isOpen, setIsOpen] = useState(false)
  const [draft, setDraft] = useState("")
  const [decimal, setDecimal] = useState(false)
  const [maxLength, setMaxLength] = useState<number | null>(null)

  useEffect(() => {
    function openForInput(event: PointerEvent) {
      if (!isNumericInput(event.target) || event.target.disabled || event.target.readOnly) return
      event.preventDefault()
      event.target.readOnly = true
      input.current = event.target
      setDraft(event.target.value)
      setDecimal(event.target.inputMode === "decimal" || (event.target.type === "number" && !["", "1"].includes(event.target.step)))
      setMaxLength(event.target.maxLength > 0 ? event.target.maxLength : null)
      setIsOpen(true)
    }

    document.addEventListener("pointerdown", openForInput, true)
    return () => document.removeEventListener("pointerdown", openForInput, true)
  }, [])

  useEffect(() => {
    if (!isOpen) return
    const element = dialog.current
    if (element && !element.open) element.showModal()
    return () => { if (element?.open) element.close() }
  }, [isOpen])

  function close(commit: boolean) {
    const activeInput = input.current
    if (commit && activeInput) setInputValue(activeInput, draft)
    if (activeInput) activeInput.readOnly = false
    input.current = null
    dialog.current?.close()
    setIsOpen(false)
  }

  function press(key: string) {
    setDraft(value => applyNumericKey(value, key, { decimal, maxLength }))
  }

  return <dialog ref={dialog} className="mobile-numeric-keyboard" aria-label="Numeric keypad"
    onCancel={event => { event.preventDefault(); close(false) }}
    onKeyDown={event => {
      if (event.key === "Escape") { event.preventDefault(); close(false) }
      else if (event.key === "Enter") { event.preventDefault(); close(true) }
      else if (/^\d$/.test(event.key)) { event.preventDefault(); press(event.key) }
      else if (event.key === "." || event.key === ",") { event.preventDefault(); press(".") }
      else if (event.key === "Backspace") { event.preventDefault(); press("backspace") }
    }}>
    <div className="mobile-numeric-keyboard-grip" aria-hidden="true" />
    <output className="mobile-numeric-keyboard-value" aria-live="polite">{draft || "0"}</output>
    <div className="mobile-numeric-keyboard-grid">
      {["1", "2", "3", "4", "5", "6", "7", "8", "9"].map(key => <button type="button" key={key} onClick={() => press(key)}>{key}</button>)}
      <button type="button" className="mobile-numeric-keyboard-action" onClick={() => press("clear")}>Clear</button>
      <button type="button" onClick={() => press("0")}>0</button>
      <button type="button" className="mobile-numeric-keyboard-action" aria-label="Delete last digit" onClick={() => press("backspace")}><Delete aria-hidden="true" /></button>
      {decimal && <button type="button" onClick={() => press(".")}>.</button>}
      <button type="button" className={`mobile-numeric-keyboard-done${decimal ? " is-decimal" : ""}`} onClick={() => close(true)}><Check aria-hidden="true" /><span>Done</span></button>
    </div>
  </dialog>
}
