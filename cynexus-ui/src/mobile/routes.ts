import type { RouteObject } from "react-router-dom"

export const mobileRoutes: RouteObject[] = [{
  path: "m",
  lazy: async () => ({ Component: (await import("./layout/mobile-layout")).default }),
  children: [
    { index: true, lazy: async () => ({ Component: (await import("./pages/home")).default }) },
    { path: "apps", lazy: async () => ({ Component: (await import("./pages/apps")).default }) },
    { path: "me", lazy: async () => ({ Component: (await import("./pages/profile")).default }) },
    { path: "*", lazy: async () => ({ Component: (await import("./pages/not-found")).default }) },
  ],
}]
