/**
 * POC 页面 — Tabs 容器
 *
 * 如何新增 POC item:
 *   1. 在 poc/ 下新建目录，如 poc/my-idea/
 *   2. 该目录的 index.tsx 即为 POC 页面内容组件 (default export)
 *   3. 在 TABS_CONFIG 中加入一项即可出现在 Tab 列表
 */
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { PocPageHeader } from "./components/poc-page-header"
import { POC_TABS } from "./lib/tabs"

export default function PocPage() {
  return (
    <WorkspaceShell sectionLabel="POC" pageTitle="Proof of Concept">
      <PocPageHeader />

      <Tabs defaultValue={POC_TABS[0]?.key} className="w-full">
        <TabsList className="inline-flex h-fit w-fit max-w-full flex-wrap items-center gap-1 rounded-xl border border-border/30 bg-muted/35 p-1.5">
          {POC_TABS.map((tab) => (
            <TabsTrigger
              key={tab.key}
              value={tab.key}
              className="inline-flex h-8 min-w-[100px] items-center justify-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition-all data-[state=active]:bg-background data-[state=active]:shadow-sm"
            >
              {tab.label}
            </TabsTrigger>
          ))}
        </TabsList>

        {POC_TABS.map((tab) => {
          const Content = tab.component
          return (
            <TabsContent key={tab.key} value={tab.key} className="mt-4 space-y-4">
              <Content />
            </TabsContent>
          )
        })}
      </Tabs>
    </WorkspaceShell>
  )
}
