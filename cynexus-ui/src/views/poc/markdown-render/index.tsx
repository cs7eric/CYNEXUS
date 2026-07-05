import { useMemo, useState } from "react"
import { Link } from "react-router-dom"
import { ArrowLeft, FileText, PanelRightOpen } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { MdxEditorPoc } from "./components/mdx-editor-poc"
import { defaultMarkdown, markdownSamples } from "./lib/samples"

function getMarkdownStats(markdown: string) {
  const normalized = markdown.trim()
  const headings = normalized.match(/^#{1,6}\s+/gm)?.length ?? 0
  const tables = normalized.match(/^\|.+\|$/gm)?.length ?? 0
  const codeBlocks = normalized.match(/```/g)?.length ?? 0

  return {
    lines: normalized ? normalized.split(/\r?\n/).length : 0,
    chars: normalized.length,
    headings,
    tables: tables > 1 ? 1 : 0,
    codeBlocks: Math.floor(codeBlocks / 2),
  }
}

export default function MarkdownRenderPocPage() {
  const [markdown, setMarkdown] = useState(defaultMarkdown)
  const [editorKey, setEditorKey] = useState(0)
  const stats = useMemo(() => getMarkdownStats(markdown), [markdown])

  const applySample = (value: string) => {
    setMarkdown(value)
    setEditorKey((current) => current + 1)
  }

  return (
    <WorkspaceShell sectionLabel="POC" sectionUrl="/poc" pageTitle="Markdown Render">
      <div className="space-y-5">
        <div className="flex flex-col gap-4 rounded-lg bg-muted/35 p-5 lg:flex-row lg:items-end lg:justify-between">
          <div className="space-y-3">
            <div className="flex flex-wrap items-center gap-2">
              <Badge variant="secondary" className="rounded-md">
                <FileText className="size-3.5" />
                MDXEditor
              </Badge>
              <Badge variant="outline" className="rounded-md bg-background">
                WYSIWYG markdown
              </Badge>
            </div>
            <div className="space-y-2">
              <h1 className="text-3xl font-semibold tracking-tight text-foreground sm:text-4xl">
                Markdown Render
              </h1>
              <p className="max-w-3xl text-sm leading-7 text-muted-foreground sm:text-base">
                用 MDXEditor 验证 Typora 风格的实时编辑渲染，覆盖标题、列表、表格、代码块和 Markdown 输出。
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

        <div className="grid gap-4 xl:grid-cols-[minmax(0,1.35fr)_minmax(320px,0.65fr)]">
          <section className="flex min-h-[640px] flex-col rounded-lg bg-muted/35 p-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="space-y-1">
                <h2 className="text-sm font-semibold text-foreground">Editor</h2>
                <p className="text-xs text-muted-foreground">直接编辑渲染后的内容，Markdown 快捷语法仍然可用。</p>
              </div>
              <div className="flex flex-wrap gap-2">
                {markdownSamples.map((sample) => (
                  <Button
                    key={sample.label}
                    type="button"
                    size="sm"
                    variant="secondary"
                    onClick={() => applySample(sample.value)}
                  >
                    {sample.label}
                  </Button>
                ))}
              </div>
            </div>

            <div className="mt-4 min-h-[590px] flex-1">
              <MdxEditorPoc key={editorKey} markdown={markdown} onChange={setMarkdown} />
            </div>

            <div className="mt-3 grid grid-cols-2 gap-2 text-xs sm:grid-cols-5">
              <div className="rounded-md bg-background px-3 py-2">
                <p className="text-muted-foreground">Lines</p>
                <p className="font-semibold text-foreground">{stats.lines}</p>
              </div>
              <div className="rounded-md bg-background px-3 py-2">
                <p className="text-muted-foreground">Chars</p>
                <p className="font-semibold text-foreground">{stats.chars}</p>
              </div>
              <div className="rounded-md bg-background px-3 py-2">
                <p className="text-muted-foreground">Headings</p>
                <p className="font-semibold text-foreground">{stats.headings}</p>
              </div>
              <div className="rounded-md bg-background px-3 py-2">
                <p className="text-muted-foreground">Tables</p>
                <p className="font-semibold text-foreground">{stats.tables}</p>
              </div>
              <div className="rounded-md bg-background px-3 py-2">
                <p className="text-muted-foreground">Code</p>
                <p className="font-semibold text-foreground">{stats.codeBlocks}</p>
              </div>
            </div>
          </section>

          <section className="min-h-[640px] rounded-lg bg-muted/35 p-4">
            <div className="mb-4 flex items-center justify-between gap-3">
              <div className="space-y-1">
                <h2 className="text-sm font-semibold text-foreground">Markdown Output</h2>
                <p className="text-xs text-muted-foreground">MDXEditor 变更后输出的 Markdown 字符串。</p>
              </div>
              <div className="flex size-9 items-center justify-center rounded-md bg-background text-muted-foreground shadow-xs">
                <PanelRightOpen className="size-4" />
              </div>
            </div>
            <textarea
              value={markdown}
              readOnly
              spellCheck={false}
              className="min-h-[590px] w-full resize-none rounded-lg border border-border/60 bg-background p-4 font-mono text-xs leading-6 text-foreground outline-none placeholder:text-muted-foreground"
            />
          </section>
        </div>
      </div>
    </WorkspaceShell>
  )
}
