import { Lock } from "lucide-react"

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

export function NotApplicableCard({
  title,
  description,
}: {
  title: string
  description: string
}) {
  return (
    <Card className="rounded-none border-0 bg-muted/50 shadow-none sm:rounded-2xl">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <Lock className="size-4 text-slate-400" />
          {title}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="rounded-none bg-background/80 p-6 text-sm text-slate-500 sm:rounded-2xl">
          {description}
        </div>
      </CardContent>
    </Card>
  )
}
