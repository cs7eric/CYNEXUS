import { useEffect, useRef, type ReactNode } from "react"
import { createPortal } from "react-dom"
import { useMobilePortal } from "@/mobile/hooks/use-mobile-portal"
import "./body-view.css"

export function WorkoutFormDialog({ children, onClose }: { children: ReactNode; onClose: () => void }) {
  const portal = useMobilePortal()
  const dialog = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const element = dialog.current
    const trigger = document.activeElement instanceof HTMLElement ? document.activeElement : null
    element?.showModal()
    return () => { element?.close(); trigger?.focus() }
  }, [])
  const content = <dialog ref={dialog} className="workout-measurement-dialog" aria-label="Record workout" onCancel={onClose} onClick={(event) => { if (event.target === event.currentTarget) onClose() }}><div className="workout-measurement-sheet">{children}</div></dialog>
  return portal ? createPortal(content, portal) : content
}
