import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { RouterProvider } from "react-router-dom";
import Prism from "prismjs";
import "./index.css";
import { router } from "./router";

// MDXEditor plugins expect Prism on the global object in production bundles.
(globalThis as typeof globalThis & { Prism: typeof Prism }).Prism = Prism;

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>
);
