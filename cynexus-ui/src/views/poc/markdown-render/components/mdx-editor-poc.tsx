import {
  BlockTypeSelect,
  BoldItalicUnderlineToggles,
  CodeToggle,
  CodeMirrorEditor,
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
  codeMirrorPlugin,
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
import type { CodeBlockEditorDescriptor, CodeBlockEditorProps } from "@mdxeditor/editor"
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
  AlignCenter,
  AlignLeft,
  AlignRight,
  Baseline,
  BetweenHorizontalEnd,
  BetweenHorizontalStart,
  BetweenVerticalEnd,
  BetweenVerticalStart,
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
  Trash2,
  Underline,
} from "lucide-react"
import { useEffect, useLayoutEffect, useRef, useState } from "react"
import type { ReactNode } from "react"
import type { CSSProperties } from "react"
import { createPortal } from "react-dom"
import { $createParagraphNode, $getRoot, $setSelection } from "lexical"
import "@mdxeditor/editor/style.css"
import "./mdx-editor-poc.css"
import { ToolbarColorControls, colorActionGroups, useApplyMdxColor } from "./toolbar-color-controls"

interface MdxEditorPocProps {
  markdown: string
  onChange: (markdown: string) => void
}

type TableMenuKind = "column" | "row"

interface TableMenuState {
  button: HTMLButtonElement
  kind: TableMenuKind
  left: number
  top: number
}

const copyIconSvg = `
  <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
    stroke-linecap="round" stroke-linejoin="round">
    <rect width="14" height="14" x="8" y="8" rx="2" ry="2"></rect>
    <path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"></path>
  </svg>
`

function CodeMirrorEditorWithCopy(props: CodeBlockEditorProps) {
  const wrapperRef = useRef<HTMLDivElement>(null)
  const codeRef = useRef(props.code)

  codeRef.current = props.code

  useLayoutEffect(() => {
    const wrapper = wrapperRef.current
    if (!wrapper) {
      return
    }

    const syncCopyButton = () => {
      const toolbar = wrapper.querySelector<HTMLElement>('[class*="_codeMirrorToolbar_"]')
      if (!toolbar) {
        return
      }

      const deleteButton = Array.from(toolbar.querySelectorAll<HTMLButtonElement>("button")).find((button) =>
        button.title.toLowerCase().includes("delete")
      )
      let copyButton = toolbar.querySelector<HTMLButtonElement>(".cynexus-mdx-code-copy-button")

      if (!copyButton) {
        copyButton = document.createElement("button")
        copyButton.type = "button"
        copyButton.title = "Copy code"
        copyButton.setAttribute("aria-label", "Copy code")
        copyButton.className = "cynexus-mdx-code-copy-button"
        copyButton.innerHTML = copyIconSvg
      }

      copyButton.onclick = (event) => {
        event.preventDefault()
        event.stopPropagation()
        void navigator.clipboard?.writeText(codeRef.current)
      }

      if (deleteButton && copyButton.nextSibling !== deleteButton) {
        toolbar.insertBefore(copyButton, deleteButton)
      } else if (copyButton.parentElement !== toolbar) {
        toolbar.appendChild(copyButton)
      }
    }

    syncCopyButton()
    const animationFrame = window.requestAnimationFrame(syncCopyButton)
    const observer = new MutationObserver(syncCopyButton)
    observer.observe(wrapper, { childList: true, subtree: true })

    return () => {
      window.cancelAnimationFrame(animationFrame)
      observer.disconnect()
    }
  }, [])

  return (
    <div ref={wrapperRef} className="cynexus-mdx-code-block-editor">
      <CodeMirrorEditor {...props} />
    </div>
  )
}

const codeBlockEditorDescriptors: CodeBlockEditorDescriptor[] = [
  {
    priority: 100,
    match: () => true,
    Editor: CodeMirrorEditorWithCopy,
  },
]

const mdxEditorPlugins = [
  headingsPlugin(),
  listsPlugin(),
  quotePlugin(),
  thematicBreakPlugin(),
  linkPlugin(),
  linkDialogPlugin(),
  tablePlugin(),
  codeBlockPlugin({ defaultCodeBlockLanguage: "json", codeBlockEditorDescriptors }),
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
  const wrapperRef = useRef<HTMLDivElement>(null)
  const tableMenuRef = useRef<HTMLDivElement>(null)
  const allowNativeTableMenuRef = useRef(false)
  const [hasTrailingEmptyParagraph, setHasTrailingEmptyParagraph] = useState(false)
  const [tableMenu, setTableMenu] = useState<TableMenuState | null>(null)

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

  useEffect(() => {
    const wrapper = wrapperRef.current
    if (!wrapper) {
      return
    }

    const tableMenuButtonSelector =
      'button[title="Column menu"], button[title="Row menu"], button[class*="_tableColumnEditorTrigger_"]'

    let closeTimer: number | null = null
    const cancelClose = () => {
      if (closeTimer !== null) {
        window.clearTimeout(closeTimer)
        closeTimer = null
      }
    }

    const findTableMenuButton = (event: PointerEvent) => {
      const target = event.target
      const directButton = target instanceof Element ? target.closest<HTMLButtonElement>(tableMenuButtonSelector) : null

      if (directButton) {
        return directButton
      }

      return document
        .elementsFromPoint(event.clientX, event.clientY)
        .map((element) => element.closest<HTMLButtonElement>(tableMenuButtonSelector))
        .find((button): button is HTMLButtonElement => Boolean(button))
    }

    const closeTableMenu = () => {
      cancelClose()
      setTableMenu(null)
    }

    const scheduleCloseTableMenu = () => {
      if (closeTimer !== null) {
        return
      }

      closeTimer = window.setTimeout(() => {
        closeTimer = null
        setTableMenu(null)
      }, 80)
    }

    const isOverFloatingTableMenu = (event: PointerEvent) => {
      return tableMenuRef.current?.contains(event.target as Node) ?? false
    }

    const showTableMenu = (button: HTMLButtonElement) => {
      const rect = button.getBoundingClientRect()
      const kind: TableMenuKind = button.title === "Row menu" ? "row" : "column"
      const estimatedWidth = kind === "column" ? 222 : 138
      const estimatedHeight = 40
      const left = Math.max(8, Math.min(window.innerWidth - estimatedWidth - 8, rect.right + 8))
      const top = Math.max(8, Math.min(window.innerHeight - estimatedHeight - 8, rect.top + rect.height / 2 - estimatedHeight / 2))

      cancelClose()
      setTableMenu((current) => {
        if (current?.button === button && current.left === left && current.top === top) {
          return current
        }

        return { button, kind, left, top }
      })
    }

    const openTableMenuOnHover = (event: PointerEvent) => {
      const button = findTableMenuButton(event)

      if (!button) {
        if (!isOverFloatingTableMenu(event)) {
          scheduleCloseTableMenu()
        }
        return
      }

      showTableMenu(button)
    }

    const closeTableMenuWhenUnhovered = (event: PointerEvent) => {
      if (findTableMenuButton(event) || isOverFloatingTableMenu(event)) {
        cancelClose()
        return
      }

      scheduleCloseTableMenu()
    }

    const handleNativeTableMenuClick = (event: MouseEvent) => {
      if (allowNativeTableMenuRef.current) {
        return
      }

      const target = event.target
      const button = target instanceof Element ? target.closest<HTMLButtonElement>(tableMenuButtonSelector) : null

      if (!button) {
        return
      }

      event.preventDefault()
      event.stopPropagation()
      showTableMenu(button)
    }

    wrapper.addEventListener("pointerover", openTableMenuOnHover)
    wrapper.addEventListener("pointermove", openTableMenuOnHover)
    wrapper.addEventListener("click", handleNativeTableMenuClick, true)
    document.addEventListener("pointermove", closeTableMenuWhenUnhovered)
    window.addEventListener("scroll", closeTableMenu, true)
    window.addEventListener("resize", closeTableMenu)
    return () => {
      wrapper.removeEventListener("pointerover", openTableMenuOnHover)
      wrapper.removeEventListener("pointermove", openTableMenuOnHover)
      wrapper.removeEventListener("click", handleNativeTableMenuClick, true)
      document.removeEventListener("pointermove", closeTableMenuWhenUnhovered)
      window.removeEventListener("scroll", closeTableMenu, true)
      window.removeEventListener("resize", closeTableMenu)
      cancelClose()
    }
  }, [])

  const runNativeTableMenuAction = (titleIncludes: string) => {
    const trigger = tableMenu?.button
    if (!trigger) {
      return
    }

    const contentSelector = '[class*="_tableColumnEditorPopoverContent_"]'
    const cleanup = () => {
      document.body.classList.remove("cynexus-mdx-hide-native-table-menu")
      if (trigger.dataset.state === "open") {
        allowNativeTableMenuRef.current = true
        trigger.click()
        allowNativeTableMenuRef.current = false
      }
      setTableMenu(null)
    }

    document.body.classList.add("cynexus-mdx-hide-native-table-menu")
    allowNativeTableMenuRef.current = true
    trigger.click()
    allowNativeTableMenuRef.current = false

    window.setTimeout(() => {
      const contents = Array.from(document.querySelectorAll<HTMLElement>(contentSelector))
      const content = contents.find((item) => item.dataset.state === "open") ?? contents.at(-1)
      const actionButton = Array.from(content?.querySelectorAll<HTMLButtonElement>("button") ?? []).find((button) =>
        button.title.toLowerCase().includes(titleIncludes.toLowerCase())
      )

      actionButton?.click()
      window.setTimeout(cleanup, 0)
    }, 0)
  }

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
        <div ref={wrapperRef}>
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
      {tableMenu
        ? createPortal(
            <div
              ref={tableMenuRef}
              className="cynexus-mdx-table-floating-menu"
              style={{ left: tableMenu.left, top: tableMenu.top }}
              onPointerEnter={() => undefined}
              onPointerLeave={() => setTableMenu(null)}
            >
              {tableMenu.kind === "column" ? (
                <>
                  <button type="button" title="Align left" onClick={() => runNativeTableMenuAction("align left")}>
                    <AlignLeft aria-hidden="true" />
                  </button>
                  <button type="button" title="Align center" onClick={() => runNativeTableMenuAction("align center")}>
                    <AlignCenter aria-hidden="true" />
                  </button>
                  <button type="button" title="Align right" onClick={() => runNativeTableMenuAction("align right")}>
                    <AlignRight aria-hidden="true" />
                  </button>
                  <span className="cynexus-mdx-table-floating-separator" />
                  <button type="button" title="Insert column left" onClick={() => runNativeTableMenuAction("left of this")}>
                    <BetweenHorizontalStart aria-hidden="true" />
                  </button>
                  <button type="button" title="Insert column right" onClick={() => runNativeTableMenuAction("right of this")}>
                    <BetweenHorizontalEnd aria-hidden="true" />
                  </button>
                  <button type="button" title="Delete column" onClick={() => runNativeTableMenuAction("delete this column")}>
                    <Trash2 aria-hidden="true" />
                  </button>
                </>
              ) : (
                <>
                  <button type="button" title="Insert row above" onClick={() => runNativeTableMenuAction("above this")}>
                    <BetweenVerticalStart aria-hidden="true" />
                  </button>
                  <button type="button" title="Insert row below" onClick={() => runNativeTableMenuAction("below this")}>
                    <BetweenVerticalEnd aria-hidden="true" />
                  </button>
                  <button type="button" title="Delete row" onClick={() => runNativeTableMenuAction("delete this row")}>
                    <Trash2 aria-hidden="true" />
                  </button>
                </>
              )}
            </div>,
            document.body
          )
        : null}
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
