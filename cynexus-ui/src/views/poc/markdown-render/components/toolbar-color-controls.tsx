import { $patchStyleText } from "@lexical/selection"
import { $createTextNode, $getSelection, $insertNodes, $isRangeSelection, $setSelection } from "lexical"
import { Button, activeEditor$, currentSelection$, useCellValue } from "@mdxeditor/editor"
import { Baseline, ChevronDown, Highlighter } from "lucide-react"
import type { LucideIcon } from "lucide-react"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import type { CSSProperties, MouseEvent, PointerEvent } from "react"

export type ColorActionKind = "font" | "background"

export interface ColorAction {
  label: string
  kind: ColorActionKind
  color: string
  patch: Record<string, string>
  insertText: string
  style: string
}

export const colorActions: ColorAction[] = [
  {
    label: "红字",
    kind: "font",
    color: "#dc2626",
    patch: { color: "#dc2626" },
    insertText: "红色文字",
    style: "color: #dc2626",
  },
  {
    label: "绿字",
    kind: "font",
    color: "#16a34a",
    patch: { color: "#16a34a" },
    insertText: "绿色文字",
    style: "color: #16a34a",
  },
  {
    label: "黄字",
    kind: "font",
    color: "#ca8a04",
    patch: { color: "#ca8a04" },
    insertText: "黄色文字",
    style: "color: #ca8a04",
  },
  {
    label: "红底",
    kind: "background",
    color: "#fee2e2",
    patch: { "background-color": "#fee2e2", color: "#991b1b" },
    insertText: "红色背景",
    style: "background-color: #fee2e2; color: #991b1b",
  },
  {
    label: "绿底",
    kind: "background",
    color: "#dcfce7",
    patch: { "background-color": "#dcfce7", color: "#166534" },
    insertText: "绿色背景",
    style: "background-color: #dcfce7; color: #166534",
  },
  {
    label: "黄底",
    kind: "background",
    color: "#fef3c7",
    patch: { "background-color": "#fef3c7", color: "#92400e" },
    insertText: "黄色背景",
    style: "background-color: #fef3c7; color: #92400e",
  },
]

export const colorActionGroups = [
  {
    kind: "font",
    label: "文字颜色",
    icon: Baseline,
    actions: colorActions.filter((action) => action.kind === "font"),
  },
  {
    kind: "background",
    label: "背景颜色",
    icon: Highlighter,
    actions: colorActions.filter((action) => action.kind === "background"),
  },
] satisfies Array<{
  kind: ColorActionKind
  label: string
  icon: LucideIcon
  actions: ColorAction[]
}>

export function useApplyMdxColor() {
  const activeEditor = useCellValue(activeEditor$)
  const currentSelection = useCellValue(currentSelection$)

  return (action: ColorAction) => {
    if (!activeEditor) {
      return
    }

    activeEditor.update(
      () => {
        let selection = $getSelection()

        if (!$isRangeSelection(selection) && currentSelection) {
          const restoredSelection = currentSelection.clone()
          $setSelection(restoredSelection)
          selection = restoredSelection
        }

        if (
          $isRangeSelection(selection) &&
          selection.isCollapsed() &&
          currentSelection &&
          !currentSelection.isCollapsed()
        ) {
          const restoredSelection = currentSelection.clone()
          $setSelection(restoredSelection)
          selection = restoredSelection
        }

        if ($isRangeSelection(selection) && !selection.isCollapsed()) {
          $patchStyleText(selection, action.patch)
          return
        }

        if (!$isRangeSelection(selection)) {
          return
        }

        const node = $createTextNode(action.insertText)
        node.setStyle(action.style)
        $insertNodes([node])
      },
      { discrete: true }
    )
    activeEditor.focus()
  }
}

export function ToolbarColorControls() {
  const applyColor = useApplyMdxColor()

  const handlePointerDown = (event: PointerEvent) => {
    event.preventDefault()
  }

  const handleMouseDown = (event: MouseEvent) => {
    event.preventDefault()
  }

  return (
    <div className="cynexus-mdx-color-tools">
      {colorActionGroups.map(({ actions, icon: Icon, kind, label }) => (
        <DropdownMenu key={kind}>
          <DropdownMenuTrigger asChild>
            <Button type="button" title={label} className="cynexus-mdx-color-button cynexus-mdx-color-trigger">
              <span className="cynexus-mdx-color-icon" data-kind={kind}>
                <Icon aria-hidden="true" />
              </span>
              <ChevronDown aria-hidden="true" className="cynexus-mdx-color-chevron" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start" className="cynexus-mdx-color-dropdown">
            {actions.map((action) => (
              <DropdownMenuItem
                key={action.label}
                className="cynexus-mdx-color-dropdown-item"
                onPointerDown={handlePointerDown}
                onMouseDown={handleMouseDown}
                onSelect={() => applyColor(action)}
              >
                <span
                  className="cynexus-mdx-color-icon"
                  data-kind={action.kind}
                  style={{ "--swatch-color": action.color } as CSSProperties}
                >
                  {action.kind === "font" ? <Baseline aria-hidden="true" /> : <Highlighter aria-hidden="true" />}
                </span>
                <span>{action.label}</span>
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      ))}
    </div>
  )
}
