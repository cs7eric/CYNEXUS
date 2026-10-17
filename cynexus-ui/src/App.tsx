import { Outlet } from "react-router-dom";

import { WorkspaceProvider, useWorkspace } from "@/layout/workspace-provider";
import { OwnerAuthBoundary } from "@/components/owner-auth-boundary";

function App() {
  const { workspaceId } = useWorkspace();
  return (
    <OwnerAuthBoundary>
      <WorkspaceProvider>
        <Outlet key={workspaceId} />
      </WorkspaceProvider>
    </OwnerAuthBoundary>
  );
}

export default App;
