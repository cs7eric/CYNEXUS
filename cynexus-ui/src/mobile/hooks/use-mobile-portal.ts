import { createContext, useContext } from "react"

export const MobilePortalContext = createContext<HTMLElement | null>(null)

export function useMobilePortal() {
  return useContext(MobilePortalContext) ?? undefined
}
