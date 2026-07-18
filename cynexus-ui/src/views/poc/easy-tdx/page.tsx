import { WorkspaceShell } from "@/layout/workspace-shell"

import EasyTdxPoc from "."

export default function EasyTdxPocPage() {
  return (
    <WorkspaceShell sectionLabel="POC" sectionUrl="/poc" pageTitle="easy_tdx Data Lab">
      <EasyTdxPoc />
    </WorkspaceShell>
  )
}
