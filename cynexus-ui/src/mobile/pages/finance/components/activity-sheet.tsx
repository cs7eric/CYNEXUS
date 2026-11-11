import { X } from "lucide-react"
import { useEffect, useId, useRef, useState, type ReactNode } from "react"

import { MobilePortalContext } from "@/mobile/hooks/use-mobile-portal"

export function ActivitySheet({ title, onClose, busy = false, className, children }: { title: string; onClose: () => void; busy?: boolean; className?: string; children: ReactNode }) {
  const [dialog, setDialog] = useState<HTMLDialogElement | null>(null)
  const backdropPress = useRef(false)
  const titleId = useId()
  useEffect(() => {
    dialog?.showModal()
    return () => { dialog?.close() }
  }, [dialog])
  return <dialog ref={setDialog} className={`finance-activity-dialog${className ? ` ${className}` : ""}`} aria-labelledby={titleId}
    onCancel={(event) => { event.preventDefault(); event.stopPropagation(); if (!busy) onClose() }}
    onPointerDown={(event) => {
      const bounds = event.currentTarget.getBoundingClientRect()
      backdropPress.current = event.target === event.currentTarget && (
        event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom
      )
    }}
    onClick={(event) => {
      // Opening a popover can retarget the trigger's click to the native dialog.
      const dismiss = backdropPress.current && event.target === event.currentTarget
      backdropPress.current = false
      if (dismiss && !busy) onClose()
    }}>
    <div className="fa-sheet">
      <div className="fa-handle" />
      <div className="fa-sheet-heading"><h2 id={titleId}>{title}</h2><button type="button" className="fa-round" disabled={busy} aria-label={`Close ${title.toLowerCase()}`} title="Close" onClick={onClose}><X aria-hidden="true" /></button></div>
      <MobilePortalContext value={dialog}>{children}</MobilePortalContext>
    </div>
  </dialog>
}
