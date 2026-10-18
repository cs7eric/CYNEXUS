import { useLayoutEffect, type RefObject } from "react"

/** Keep application chrome inside Safari's visible viewport, including its keyboard. */
export function useMobileViewport(ref: RefObject<HTMLElement | null>) {
  useLayoutEffect(() => {
    const element = ref.current
    if (!element) return

    const viewport = window.visualViewport
    const standalone = window.matchMedia("(display-mode: standalone)").matches
      || ("standalone" in navigator && Boolean((navigator as Navigator & { standalone?: boolean }).standalone))
    element.dataset.mobileStandalone = standalone ? "true" : "false"
    element.style.setProperty("--mobile-viewport-position", standalone ? "absolute" : "relative")
    if (standalone) element.style.setProperty("--mobile-shell-height", "var(--mobile-viewport-height, 100dvh)")
    let frame = 0

    function update() {
      if (!element) return
      // Pinch zoom must magnify the existing layout, rather than reflowing the app.
      if (viewport && Math.abs(viewport.scale - 1) > 0.01) return
      const top = Math.max(0, viewport?.offsetTop ?? 0)
      const left = Math.max(0, viewport?.offsetLeft ?? 0)
      const height = viewport?.height ?? window.innerHeight
      const width = viewport?.width ?? window.innerWidth
      if (height <= 0 || width <= 0) return

      element.style.setProperty("--mobile-viewport-top", `${top}px`)
      element.style.setProperty("--mobile-viewport-left", `${left}px`)
      element.style.setProperty("--mobile-viewport-height", `${height}px`)
      element.style.setProperty("--mobile-viewport-width", `${width}px`)
    }

    function schedule() {
      cancelAnimationFrame(frame)
      frame = requestAnimationFrame(update)
    }

    update()
    viewport?.addEventListener("resize", schedule)
    viewport?.addEventListener("scroll", schedule)
    window.addEventListener("resize", schedule)
    window.addEventListener("pageshow", schedule)
    return () => {
      cancelAnimationFrame(frame)
      viewport?.removeEventListener("resize", schedule)
      viewport?.removeEventListener("scroll", schedule)
      window.removeEventListener("resize", schedule)
      window.removeEventListener("pageshow", schedule)
      for (const property of ["top", "left", "height", "width"]) {
        element.style.removeProperty(`--mobile-viewport-${property}`)
      }
      element.style.removeProperty("--mobile-viewport-position")
      element.style.removeProperty("--mobile-shell-height")
      delete element.dataset.mobileStandalone
    }
  }, [ref])
}
