import { createElement } from "react"
import { Navigate, type RouteObject } from "react-router-dom"

export const mobileRoutes: RouteObject[] = [{
  path: "m",
  lazy: async () => ({ Component: (await import("./layout/mobile-layout")).default }),
  children: [
    { index: true, lazy: async () => ({ Component: (await import("./pages/home/index")).default }) },
    { path: "apps", lazy: async () => ({ Component: (await import("./pages/apps/index")).default }) },
    { path: "finance", lazy: async () => ({ Component: (await import("./pages/finance/index")).default }) },
    { path: "food", lazy: async () => ({ Component: (await import("./pages/food/index")).default }) },
    { path: "fridge", element: createElement(Navigate, { to: "/m/food#food-fridge", replace: true }) },
    { path: "workout", lazy: async () => ({ Component: (await import("./pages/workout/index")).default }) },
    { path: "me", lazy: async () => ({ Component: (await import("./pages/profile/index")).default }) },
    { path: "configuration", lazy: async () => ({ Component: (await import("./pages/configuration/index")).default }) },
    { path: "*", lazy: async () => ({ Component: (await import("./pages/not-found/index")).default }) },
  ],
}]
