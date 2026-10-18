import { AlertDialog } from "@heroui/react/alert-dialog"
import { Loader2, LogOut } from "lucide-react"
import { useState } from "react"

import { useMobilePortal } from "@/mobile/hooks/use-mobile-portal"
import { MobileButton } from "@/mobile/ui/button"

export function MobileLogoutDialog({ allDevices, onConfirm }: { allDevices?: boolean; onConfirm: () => Promise<void> }) {
  const portal = useMobilePortal()
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState("")
  const title = allDevices ? "Sign out of all devices" : "Sign out of this device"

  async function confirm() {
    if (busy) return
    setBusy(true)
    setError("")
    try { await onConfirm(); setOpen(false) }
    catch (failure) { setError(failure instanceof Error ? failure.message : "Sign-out failed. Please try again.") }
    finally { setBusy(false) }
  }

  return (
    <AlertDialog isOpen={open} onOpenChange={(value) => { if (!busy) { setOpen(value); setError("") } }}>
      <MobileButton variant="ghost" className="w-full justify-start" onPress={() => setOpen(true)}><LogOut aria-hidden="true" />{title}</MobileButton>
      <AlertDialog.Backdrop UNSTABLE_portalContainer={portal} variant="opaque">
        <AlertDialog.Container placement="center" size="sm">
          <AlertDialog.Dialog className="rounded-lg">
            <AlertDialog.Header><AlertDialog.Heading>{title}?</AlertDialog.Heading></AlertDialog.Header>
            <AlertDialog.Body><p>You will need your password to sign in again.</p>{error && <p role="alert" className="mt-3 text-destructive">{error}</p>}</AlertDialog.Body>
            <AlertDialog.Footer>
              <MobileButton slot="close" isDisabled={busy}>Cancel</MobileButton>
              <MobileButton variant="primary" isPending={busy} onPress={() => void confirm()}>
                {busy ? <Loader2 className="size-4 animate-spin" aria-hidden="true" /> : <LogOut className="size-4" aria-hidden="true" />}Sign out
              </MobileButton>
            </AlertDialog.Footer>
          </AlertDialog.Dialog>
        </AlertDialog.Container>
      </AlertDialog.Backdrop>
    </AlertDialog>
  )
}
