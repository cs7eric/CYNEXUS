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
  activeEditor$,
  addEditorWrapper$,
  applyBlockType$,
  applyFormat$,
  applyListType$,
  codeBlockPlugin,
  currentSelection$,
  headingsPlugin,
  insertCodeBlock$,
  insertMarkdown$,
  insertTable$,
  insertThematicBreak$,
  linkDialogPlugin,
  linkPlugin,
  listsPlugin,
  markdownShortcutPlugin,
  quotePlugin,
  realmPlugin,
  tablePlugin,
  thematicBreakPlugin,
  toolbarPlugin,
  useCellValue,
  usePublisher,
} from "@mdxeditor/editor"
import {
  ContextMenu,
  ContextMenuContent,
  ContextMenuItem,
  ContextMenuLabel,
  ContextMenuSeparator,
  ContextMenuSub,
  ContextMenuSubContent,
  ContextMenuSubTrigger,
  ContextMenuTrigger,
} from "@/components/ui/context-menu"
import {
  Baseline,
  Bold,
  Code2,
  Heading1,
  Heading2,
  Highlighter,
  Italic,
  Link,
  List,
  ListChecks,
  ListOrdered,
  Minus,
  Pilcrow,
  Quote,
  Table,
  Underline,
} from "lucide-react"
import { useEffect, useState } from "react"
import type { ReactNode } from "react"
import type { CSSProperties } from "react"
import { $createParagraphNode, $getRoot, $setSelection } from "lexical"
import "@mdxeditor/editor/style.css"
import "./mdx-editor-poc.css"
import { ToolbarColorControls, colorActionGroups, useApplyMdxColor } from "./toolbar-color-controls"

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
  realmPlugin({
    init: (realm) => {
      realm.pub(addEditorWrapper$, MdxEditorContextMenu)
    },
  })(),
  toolbarPlugin({
    toolbarClassName: "cynexus-mdx-toolbar",
    toolbarContents: () => <MdxEditorControls />,
  }),
]

function MdxEditorControls() {
  return (
    <>
      <UndoRedo />
      <Separator />
      <BlockTypeSelect />
      <Separator />
      <BoldItalicUnderlineToggles />
      <CodeToggle />
      <ToolbarColorControls />
      <Separator />
      <ListsToggle options={["bullet", "number", "check"]} />
      <Separator />
      <CreateLink />
      <InsertTable />
      <InsertCodeBlock />
      <InsertThematicBreak />
    </>
  )
}

function MdxEditorContextMenu({ children }: { children: ReactNode }) {
  const activeEditor = useCellValue(activeEditor$)
  const [hasTrailingEmptyParagraph, setHasTrailingEmptyParagraph] = useState(false)

  useEffect(() => {
    if (!activeEditor) {
      return
    }

    const readTrailingState = () => {
      activeEditor.getEditorState().read(() => {
        const lastChild = $getRoot().getLastChild()
        setHasTrailingEmptyParagraph(lastChild?.getType() === "paragraph" && lastChild.getTextContent() === "")
      })
    }

    readTrailingState()
    return activeEditor.registerUpdateListener(readTrailingState)
  }, [activeEditor])

  const focusAppendParagraph = () => {
    activeEditor?.update(
      () => {
        const root = $getRoot()
        const lastChild = root.getLastChild()
        if (lastChild?.getType() === "paragraph" && lastChild.getTextContent() === "") {
          lastChild.selectEnd()
          return
        }

        const paragraph = $createParagraphNode()
        root.append(paragraph)
        paragraph.selectEnd()
      },
      { discrete: true }
    )
    activeEditor?.focus()
  }

  return (
    <ContextMenu>
      <ContextMenuTrigger asChild>
        <div>
          {children}
          {!hasTrailingEmptyParagraph ? (
            <button
              type="button"
              aria-label="Continue writing after the document"
              className="cynexus-mdx-append-target"
              onMouseDown={(event) => event.preventDefault()}
              onClick={focusAppendParagraph}
            >
              <span>Click to continue writing after the document</span>
            </button>
          ) : null}
        </div>
      </ContextMenuTrigger>
      <ContextMenuContent className="cynexus-mdx-context-menu" onCloseAutoFocus={(event) => event.preventDefault()}>
        <MdxEditorContextMenuControls />
      </ContextMenuContent>
    </ContextMenu>
  )
}

function MdxEditorContextMenuControls() {
  const activeEditor = useCellValue(activeEditor$)
  const currentSelection = useCellValue(currentSelection$)
  const applyBlockType = usePublisher(applyBlockType$)
  const applyFormat = usePublisher(applyFormat$)
  const applyListType = usePublisher(applyListType$)
  const insertCodeBlock = usePublisher(insertCodeBlock$)
  const insertMarkdown = usePublisher(insertMarkdown$)
  const insertTable = usePublisher(insertTable$)
  const insertThematicBreak = usePublisher(insertThematicBreak$)
  const applyColor = useApplyMdxColor()

  const restoreSelection = () => {
    activeEditor?.update(
      () => {
        if (currentSelection) {
          $setSelection(currentSelection.clone())
        }
      },
      { discrete: true }
    )
  }

  const runWithSelection = (action: () => void) => {
    restoreSelection()
    action()
    activeEditor?.focus()
  }

  return (
    <>
      <ContextMenuLabel className="cynexus-mdx-context-label">Markdown</ContextMenuLabel>
      <ContextMenuItem onSelect={() => runWithSelection(() => applyBlockType("paragraph"))}>
        <Pilcrow />
        <span>Paragraph</span>
      </ContextMenuItem>
      <ContextMenuItem onSelect={() => runWithSelection(() => applyBlockType("h1"))}>
        <Heading1 />
        <span>Heading 1</span>
      </ContextMenuItem>
      <ContextMenuItem onSelect={() => runWithSelection(() => applyBlockType("h2"))}>
        <Heading2 />
        <span>Heading 2</span>
      </ContextMenuItem>
      <ContextMenuItem onSelect={() => runWithSelection(() => applyBlockType("quote"))}>
        <Quote />
        <span>Quote</span>
      </ContextMenuItem>

      <ContextMenuSeparator />

      <ContextMenuItem onSelect={() => runWithSelection(() => applyFormat("bold"))}>
        <Bold />
        <span>Bold</span>
      </ContextMenuItem>
      <ContextMenuItem onSelect={() => runWithSelection(() => applyFormat("italic"))}>
        <Italic />
        <span>Italic</span>
      </ContextMenuItem>
      <ContextMenuItem onSelect={() => runWithSelection(() => applyFormat("underline"))}>
        <Underline />
        <span>Underline</span>
      </ContextMenuItem>
      <ContextMenuItem onSelect={() => runWithSelection(() => applyFormat("code"))}>
        <Code2 />
        <span>Inline Code</span>
      </ContextMenuItem>

      {colorActionGroups.map(({ actions, icon: Icon, kind, label }) => (
        <ContextMenuSub key={kind}>
          <ContextMenuSubTrigger>
            <Icon />
            <span>{label === "文字颜色" ? "Text Color" : "Highlight Color"}</span>
          </ContextMenuSubTrigger>
          <ContextMenuSubContent className="cynexus-mdx-context-submenu">
            {actions.map((action) => (
              <ContextMenuItem key={action.label} onSelect={() => runWithSelection(() => applyColor(action))}>
                <span
                  className="cynexus-mdx-color-icon"
                  data-kind={action.kind}
                  style={{ "--swatch-color": action.color } as CSSProperties}
                >
                  {action.kind === "font" ? <Baseline aria-hidden="true" /> : <Highlighter aria-hidden="true" />}
                </span>
                <span>
                  {action.kind === "font"
                    ? action.label.replace("字", " Text").replace("红", "Red").replace("绿", "Green").replace("黄", "Yellow")
                    : action.label.replace("底", " Highlight").replace("红", "Red").replace("绿", "Green").replace("黄", "Yellow")}
                </span>
              </ContextMenuItem>
            ))}
          </ContextMenuSubContent>
        </ContextMenuSub>
      ))}

      <ContextMenuSeparator />

      <ContextMenuItem onSelect={() => runWithSelection(() => applyListType("bullet"))}>
        <List />
        <span>Bullet List</span>
      </ContextMenuItem>
      <ContextMenuItem onSelect={() => runWithSelection(() => applyListType("number"))}>
        <ListOrdered />
        <span>Numbered List</span>
      </ContextMenuItem>
      <ContextMenuItem onSelect={() => runWithSelection(() => applyListType("check"))}>
        <ListChecks />
        <span>Task List</span>
      </ContextMenuItem>

      <ContextMenuSeparator />

      <ContextMenuItem onSelect={() => runWithSelection(() => insertMarkdown("[Link](https://example.com)"))}>
        <Link />
        <span>Insert Link</span>
      </ContextMenuItem>
      <ContextMenuItem onSelect={() => runWithSelection(() => insertTable({ rows: 3, columns: 3 }))}>
        <Table />
        <span>Insert Table</span>
      </ContextMenuItem>
      <ContextMenuItem onSelect={() => runWithSelection(() => insertCodeBlock({}))}>
        <Code2 />
        <span>Code Block</span>
      </ContextMenuItem>
      <ContextMenuItem onSelect={() => runWithSelection(() => insertThematicBreak())}>
        <Minus />
        <span>Thematic Break</span>
      </ContextMenuItem>
    </>
  )
}

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
    />
  )
}
