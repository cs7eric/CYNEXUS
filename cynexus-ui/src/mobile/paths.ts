export const MOBILE_ROOT = "/m"

export function isMobilePath(url: string) {
  const pathname = url.split(/[?#]/)[0]
  return pathname === MOBILE_ROOT || pathname.startsWith(`${MOBILE_ROOT}/`)
}
