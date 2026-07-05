import {
  BlockTypeSelect,
  BoldItalicUnderlineToggles,
  CodeToggle,
  CreateLink,
  InsertCodeBlock,
  InsertTable,
  InsertThematicBreak,
  ListsToggle,
  MDXEditor,
  Separator,
  UndoRedo,
  codeBlockPlugin,
  headingsPlugin,
  linkDialogPlugin,
  linkPlugin,
  listsPlugin,
  markdownShortcutPlugin,
  quotePlugin,
  tablePlugin,
  thematicBreakPlugin,
  toolbarPlugin,
} from "@mdxeditor/editor"
import "@mdxeditor/editor/style.css"
import "./mdx-editor-poc.css"

interface MdxEditorPocProps {
  markdown: string
  onChange: (markdown: string) => void
}

const mdxEditorPlugins = [
  headingsPlugin(),
  listsPlugin(),
  quotePlugin(),
  thematicBreakPlugin(),
  linkPlugin(),
  linkDialogPlugin(),
  tablePlugin(),
  codeBlockPlugin({ defaultCodeBlockLanguage: "json" }),
  markdownShortcutPlugin(),
  toolbarPlugin({
    toolbarClassName: "cynexus-mdx-toolbar",
    toolbarContents: () => (
      <>
        <UndoRedo />
        <Separator />
        <BlockTypeSelect />
        <Separator />
        <BoldItalicUnderlineToggles />
        <CodeToggle />
        <Separator />
        <ListsToggle options={["bullet", "number", "check"]} />
        <Separator />
        <CreateLink />
        <InsertTable />
        <InsertCodeBlock />
        <InsertThematicBreak />
      </>
    ),
  }),
]

export function MdxEditorPoc({ markdown, onChange }: MdxEditorPocProps) {
  return (
    <MDXEditor
      markdown={markdown}
      onChange={(value) => onChange(value)}
      plugins={mdxEditorPlugins}
      className="cynexus-mdx-editor"
      contentEditableClassName="cynexus-mdx-content"
      placeholder="输入 Markdown，或者直接像 Typora 一样编辑内容..."
      spellCheck={false}
      suppressHtmlProcessing
    />
  )
}
