import { BookOpenText } from "lucide-react"

interface KnowledgeBaseHeaderProps {
  title: string
  description: string
}

export function KnowledgeBaseHeader({ title, description }: KnowledgeBaseHeaderProps) {
  return (
    <div className="flex flex-col gap-4">
      <div className="space-y-2">
        <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
          <BookOpenText className="size-4" />
          Knowledge Base
        </div>
        <h1 className="text-2xl font-semibold tracking-tight text-foreground">{title}</h1>
        <p className="max-w-3xl text-sm leading-6 text-muted-foreground">{description}</p>
      </div>
    </div>
  )
}
