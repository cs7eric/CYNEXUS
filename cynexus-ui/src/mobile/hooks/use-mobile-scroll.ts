import { useLayoutEffect, type RefObject } from "react"
import { useLocation, useNavigationType } from "react-router-dom"

const positions = new Map<string, number>()

export function useMobileScroll(ref: RefObject<HTMLElement | null>, workspace: string) {
  const { pathname, search } = useLocation()
  const navigationType = useNavigationType()
  const key = `${workspace}:${pathname}${search}`

  useLayoutEffect(() => {
    const element = ref.current
    if (!element) return
    element.scrollTop = navigationType === "POP" ? positions.get(key) ?? 0 : 0
    return () => { positions.set(key, element.scrollTop) }
  }, [key, navigationType, ref])
}
