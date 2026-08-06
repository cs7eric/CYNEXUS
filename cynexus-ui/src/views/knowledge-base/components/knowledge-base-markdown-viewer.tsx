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
import { useEffect, useRef, useState } from "react"
import "@mdxeditor/editor/style.css"
import "./knowledge-base-markdown-viewer.css"
import type { MDXEditorMethods } from "@mdxeditor/editor"

interface KnowledgeBaseMarkdownViewerProps {
  markdown: string
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

function normalizeKnowledgeMarkdown(markdown: string) {
  return markdown
    .replace(/\r\n?/g, "\n")
    .replace(/<((?:https?|mailto):[^>\s]+)>/g, "[$1]($1)")
    .trim()
}

export function KnowledgeBaseMarkdownViewer({ markdown }: KnowledgeBaseMarkdownViewerProps) {
  const editorRef = useRef<MDXEditorMethods>(null)
  const normalizedMarkdown = normalizeKnowledgeMarkdown(markdown)
  const [parseError, setParseError] = useState<string | null>(null)

  useEffect(() => {
    setParseError(null)
    editorRef.current?.setMarkdown(normalizedMarkdown)
  }, [normalizedMarkdown])

  return (
    <>
      <MDXEditor
        ref={editorRef}
        markdown={normalizedMarkdown}
        readOnly
        onError={(payload) => setParseError(payload.error)}
        plugins={readonlyPlugins}
        className="cynexus-kb-markdown-viewer"
        contentEditableClassName="cynexus-kb-markdown-content"
        spellCheck={false}
      />
      {parseError ? (
        <pre className="cynexus-kb-markdown-fallback" title={parseError}>
          {normalizedMarkdown}
        </pre>
      ) : null}
    </>
  )
}
