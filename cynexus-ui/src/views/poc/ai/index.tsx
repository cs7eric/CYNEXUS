import { Link } from "react-router-dom"
import { ArrowLeft, Beaker, ChevronRight } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { CapabilityCard } from "./components/capability-card"
import { FoundationPanel } from "./components/foundation-panel"
import { aiCapabilities } from "./lib/capabilities"

export default function AiPocPage() {
  return (
    <WorkspaceShell sectionLabel="POC" sectionUrl="/poc" pageTitle="AI POC">
      <div className="space-y-5">
        <div className="flex flex-col gap-4 rounded-lg bg-muted/35 p-5 md:flex-row md:items-end md:justify-between">
          <div className="space-y-3">
            <div className="flex flex-wrap items-center gap-2">
              <Badge variant="secondary" className="rounded-md">
                <Beaker className="size-3.5" />
                POC
              </Badge>
              <ChevronRight className="size-3.5 text-muted-foreground" />
              <Badge variant="outline" className="rounded-md bg-background">
                AI capability lab
              </Badge>
            </div>
            <div className="space-y-2">
              <h1 className="text-3xl font-semibold tracking-tight text-foreground sm:text-4xl">
                AI POC
              </h1>
              <p className="max-w-3xl text-sm leading-7 text-muted-foreground sm:text-base">
                集中验证 ChatClient、Prompt、Memory、SSE、Function Calling、RAG、多模型路由和结构化输出，
                所有 POC 页面相关代码都隔离在 poc 目录下。
              </p>
            </div>
          </div>
          <Button asChild variant="secondary" className="w-fit">
            <Link to="/poc">
              <ArrowLeft className="size-4" />
              Back to POC
            </Link>
          </Button>
        </div>

        <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_320px]">
          <section className="grid gap-3 sm:grid-cols-2 2xl:grid-cols-4">
            {aiCapabilities.map((capability) => (
              <CapabilityCard key={capability.title} capability={capability} />
            ))}
          </section>
          <FoundationPanel />
        </div>
      </div>
    </WorkspaceShell>
  )
}
