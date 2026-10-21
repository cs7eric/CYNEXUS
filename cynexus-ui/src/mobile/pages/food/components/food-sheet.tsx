import { X } from "lucide-react"
import { useEffect, useId, useRef, type ReactNode } from "react"
import { createPortal } from "react-dom"
import { useMobilePortal } from "@/mobile/hooks/use-mobile-portal"

export function FoodSheet({ title, busy = false, onClose, children }: { title: string; busy?: boolean; onClose: () => void; children: ReactNode }) {
  const dialog = useRef<HTMLDialogElement>(null), id = useId()
  const portal = useMobilePortal()
  useEffect(() => {
    const previous = document.activeElement instanceof HTMLElement ? document.activeElement : null
    const element = dialog.current
    element?.showModal()
    return () => { element?.close(); previous?.focus({ preventScroll: true }) }
  }, [])
  const content = <dialog ref={dialog} className="food-sheet" aria-labelledby={id} onCancel={event => { event.preventDefault(); event.stopPropagation(); if (!busy) onClose() }} onClick={event => { if (event.target !== event.currentTarget || busy) return; const rect = event.currentTarget.getBoundingClientRect(); if (event.clientY < rect.top || event.clientX < rect.left || event.clientX > rect.right || event.clientY > rect.bottom) onClose() }}>
    <div className="food-sheet-handle" />
    <div className="food-sheet-heading"><h2 id={id}>{title}</h2><button type="button" className="food-round" aria-label="Close" disabled={busy} onClick={onClose}><X aria-hidden="true" /></button></div>
    {children}
  </dialog>
  return portal ? createPortal(content, portal) : content
}
