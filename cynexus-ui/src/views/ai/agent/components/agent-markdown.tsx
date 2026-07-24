import {
  CodeMirrorEditor,
  MDXEditor,
  codeBlockPlugin,
  codeMirrorPlugin,
  headingsPlugin,
  linkPlugin,
  listsPlugin,
  markdownShortcutPlugin,
  quotePlugin,
  tablePlugin,
  thematicBreakPlugin,
} from "@mdxeditor/editor"
import type { CodeBlockEditorDescriptor } from "@mdxeditor/editor"
import "@mdxeditor/editor/style.css"
import "./agent-markdown.css"

interface AgentMarkdownProps {
  markdown: string
  className?: string
}

const codeBlockEditorDescriptors: CodeBlockEditorDescriptor[] = [
  {
    priority: 100,
    match: () => true,
    Editor: CodeMirrorEditor,
  },
]

const readonlyPlugins = [
  headingsPlugin(),
  listsPlugin(),
  quotePlugin(),
  thematicBreakPlugin(),
  linkPlugin(),
  tablePlugin(),
  codeBlockPlugin({ defaultCodeBlockLanguage: "markdown", codeBlockEditorDescriptors }),
  codeMirrorPlugin({
    codeBlockLanguages: {
      json: "JSON",
      js: "JavaScript",
      ts: "TypeScript",
      tsx: "TypeScript (React)",
      jsx: "JavaScript (React)",
      css: "CSS",
      sql: "SQL",
      java: "Java",
      python: "Python",
      bash: "Bash",
      markdown: "Markdown",
    },
  }),
  markdownShortcutPlugin(),
]

export function AgentMarkdown({ markdown, className }: AgentMarkdownProps) {
  return (
    <MDXEditor
      key={markdown}
      markdown={markdown}
      readOnly
      plugins={readonlyPlugins}
      className={["cynexus-agent-markdown", className].filter(Boolean).join(" ")}
      contentEditableClassName="cynexus-agent-markdown-content"
      spellCheck={false}
    />
  )
}
