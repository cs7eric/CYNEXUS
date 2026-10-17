import { ArrowLeft, Beaker, Boxes, Database, GitBranch, Maximize2, Network, Workflow } from "lucide-react"
import { Link } from "react-router-dom"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { WorkspaceShell } from "@/layout/workspace-shell"
import {
  AgentArchitecture,
  ExecutionComponents,
  Mp4RagSequence,
  SystemTopology,
} from "./components/architecture-map"
import { UnifiedArchitectureCanvas } from "./components/unified-architecture"

function UnifiedArchitectureDialog() {
  return <section className="rounded-lg border border-border bg-card p-5"><div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"><div className="min-w-0"><h2 className="text-base font-semibold">Unified architecture canvas</h2><p className="mt-1 max-w-3xl text-xs leading-5 text-muted-foreground">Open the full technical map in a near-fullscreen workspace. The canvas keeps every connected component and connector visible without constraining it to the page column.</p></div><Dialog><DialogTrigger asChild><Button variant="secondary" size="sm" className="w-fit"><Maximize2 className="size-4" />Open architecture map</Button></DialogTrigger><DialogContent className="flex h-[calc(100dvh-2rem)] max-w-none flex-col gap-0 overflow-hidden p-0 sm:max-w-none"><DialogHeader className="shrink-0 border-b border-border px-5 py-4 pr-12"><DialogTitle>Unified AI Architecture</DialogTitle><DialogDescription>System topology, normal execution, fallback, RAG, Agent governance and external infrastructure.</DialogDescription></DialogHeader><div className="min-h-0 flex-1 overflow-auto p-4"><UnifiedArchitectureCanvas /></div></DialogContent></Dialog></div></section>
}

export default function AiPocPage() {
  return (
    <WorkspaceShell sectionLabel="POC" sectionUrl="/poc" pageTitle="AI Architecture">
      <div className="mx-auto flex w-full max-w-[1500px] flex-col gap-5 pb-8">
        <header className="flex flex-col gap-4 border-b border-border/70 pb-5 lg:flex-row lg:items-end lg:justify-between">
          <div className="min-w-0 space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <Badge variant="secondary" className="rounded-md"><Beaker className="size-3.5" />POC</Badge>
              <Badge variant="outline" className="rounded-md">Implementation map</Badge>
            </div>
            <div className="space-y-1">
              <h1 className="text-2xl font-semibold text-foreground">Cynexus AI Architecture</h1>
              <p className="max-w-4xl text-sm leading-6 text-muted-foreground">
                当前代码的 AI 控制面、执行路径、知识库数据流和能力封装边界。实线表示已接入运行时，灰色隔离区表示尚未接管生产链路的集成。
              </p>
            </div>
          </div>
          <Button asChild variant="secondary" size="sm" className="w-fit">
            <Link to="/poc"><ArrowLeft className="size-4" />Back to POC</Link>
          </Button>
        </header>

        <Tabs defaultValue="unified" className="w-full">
          <TabsList variant="line" className="h-9 w-full justify-start overflow-x-auto border-b border-border/70 p-0">
            <TabsTrigger value="unified" className="h-9 shrink-0 rounded-none px-3 text-xs"><Network className="size-3.5" />Unified architecture</TabsTrigger>
            <TabsTrigger value="topology" className="h-9 shrink-0 rounded-none px-3 text-xs"><Network className="size-3.5" />System topology</TabsTrigger>
            <TabsTrigger value="components" className="h-9 shrink-0 rounded-none px-3 text-xs"><Workflow className="size-3.5" />AI components</TabsTrigger>
            <TabsTrigger value="sequence" className="h-9 shrink-0 rounded-none px-3 text-xs"><Database className="size-3.5" />MP4 and RAG sequence</TabsTrigger>
            <TabsTrigger value="agent" className="h-9 shrink-0 rounded-none px-3 text-xs"><Boxes className="size-3.5" />Agent architecture</TabsTrigger>
          </TabsList>
          <TabsContent value="unified" className="mt-5"><UnifiedArchitectureDialog /></TabsContent>
          <TabsContent value="topology" className="mt-5"><SystemTopology /></TabsContent>
          <TabsContent value="components" className="mt-5"><ExecutionComponents /></TabsContent>
          <TabsContent value="sequence" className="mt-5"><Mp4RagSequence /></TabsContent>
          <TabsContent value="agent" className="mt-5"><AgentArchitecture /></TabsContent>
        </Tabs>

        <footer className="grid gap-3 border-t border-border/70 pt-4 text-xs leading-5 text-muted-foreground md:grid-cols-3">
          <p><Database className="mr-1 inline size-3.5" />The database selects route and prompt; it does not contain business decisions.</p>
          <p><GitBranch className="mr-1 inline size-3.5" />Frameworks execute requests; Cynexus owns validation, persistence, and audit.</p>
          <p><Network className="mr-1 inline size-3.5" />AgentScope remains an isolated adapter, not an owner of existing Agent runs.</p>
        </footer>
      </div>
    </WorkspaceShell>
  )
}
