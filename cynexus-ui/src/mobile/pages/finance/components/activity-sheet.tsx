import { X } from "lucide-react"
import { useEffect, useId, useRef, type ReactNode } from "react"

export function ActivitySheet({ title, onClose, busy = false, children }: { title: string; onClose: () => void; busy?: boolean; children: ReactNode }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  useEffect(() => {
    const element = dialog.current
    element?.showModal()
    return () => { element?.close() }
  }, [])
  return <dialog ref={dialog} className="finance-activity-dialog" aria-labelledby={titleId}
    onCancel={(event) => { event.preventDefault(); event.stopPropagation(); if (!busy) onClose() }}
    onClick={(event) => { if (event.target === event.currentTarget && !busy) onClose() }}>
    <div className="fa-sheet">
      <div className="fa-handle" />
      <div className="fa-sheet-heading"><h2 id={titleId}>{title}</h2><button type="button" className="fa-round" disabled={busy} aria-label={`Close ${title.toLowerCase()}`} title="Close" onClick={onClose}><X aria-hidden="true" /></button></div>
      {children}
    </div>
  </dialog>
}
