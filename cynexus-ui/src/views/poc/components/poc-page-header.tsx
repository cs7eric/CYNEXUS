import { Beaker } from "lucide-react"

export function PocPageHeader() {
  return (
    <div className="space-y-3">
      <div className="inline-flex items-center gap-2 rounded-full bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">
        <Beaker className="size-3.5" />
        POC
      </div>
      <div className="space-y-2">
        <h1 className="text-3xl font-semibold tracking-tight text-foreground sm:text-4xl">
          Proof of Concept
        </h1>
        <p className="max-w-3xl text-sm leading-7 text-muted-foreground sm:text-base">
          Isolated experiments for ideas that should not leak into the main product surface yet.
        </p>
      </div>
    </div>
  )
}
