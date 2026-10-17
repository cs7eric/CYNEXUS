import { useCallback } from "react"
import { useBeforeUnload, useBlocker } from "react-router-dom"
import { ArrowLeft, LogOut } from "lucide-react"

import { isWorkspaceId } from "@/config/workspaces"
import { useWorkspace } from "@/layout/workspace-provider"
import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent,
  AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle,
} from "@/components/ui/alert-dialog"

export function UnsavedChangesGuard({ when, saving = false }: { when: boolean; saving?: boolean }) {
  const { workspaceId } = useWorkspace()
  const blocker = useBlocker(({ currentLocation, nextLocation }) => (when || saving) && (
    currentLocation.pathname !== nextLocation.pathname ||
    (!saving && currentLocation.search !== nextLocation.search) ||
    (isWorkspaceId(nextLocation.state?.workspace) && nextLocation.state.workspace !== workspaceId)
  ))

  useBeforeUnload(useCallback((event) => {
    if (when || saving) {
      event.preventDefault()
      event.returnValue = ""
    }
  }, [when, saving]))

  return (
    <AlertDialog open={blocker.state === "blocked"} onOpenChange={(open) => {
      if (!open && blocker.state === "blocked") blocker.reset()
    }}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{saving ? "正在保存" : "离开当前页面？"}</AlertDialogTitle>
          <AlertDialogDescription>
            {saving ? "请等待保存完成后再离开。" : "还有未保存的修改，离开后这些修改将丢失。"}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel variant="secondary" onClick={(event) => {
            event.preventDefault()
            if (blocker.state === "blocked") blocker.reset()
          }}>
            <ArrowLeft className="size-4" />继续编辑
          </AlertDialogCancel>
          <AlertDialogAction variant="destructive" disabled={saving} onClick={(event) => {
            event.preventDefault()
            if (blocker.state === "blocked") blocker.proceed()
          }}>
            <LogOut className="size-4" />放弃修改并离开
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
