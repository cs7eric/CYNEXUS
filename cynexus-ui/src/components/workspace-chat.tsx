import { useEffect, useRef, useState, type FormEvent } from "react"
import { createPortal } from "react-dom"
import { ArrowUp, LoaderCircle, MessageCircleDashed, Plus, RotateCw } from "lucide-react"

import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupTextarea,
} from "@/components/ui/input-group"
import { Marker, MarkerContent, MarkerIcon } from "@/components/ui/marker"
import { Button } from "@/components/ui/button"
import { Bubble, BubbleContent } from "@/components/ui/bubble"
import { Message, MessageContent } from "@/components/ui/message"
import {
  MessageScroller,
  MessageScrollerButton,
  MessageScrollerContent,
  MessageScrollerItem,
  MessageScrollerProvider,
  MessageScrollerViewport,
} from "@/components/ui/message-scroller"
import { Popover, PopoverTrigger } from "@/components/ui/popover"
import { executeWorkspaceChat } from "@/services/ai/chat"

type ChatMessage = {
  id: string
  role: "user" | "assistant"
  content: string
  createdAt: string
}

function MessageBubble({ message }: { message: ChatMessage }) {
  const isUser = message.role === "user"

  return (
    <Message align={isUser ? "end" : "start"}>
      <MessageContent>
        <Bubble variant={isUser ? "muted" : "ghost"}>
          <BubbleContent>
            <p className="whitespace-pre-wrap">{message.content}</p>
          </BubbleContent>
        </Bubble>
      </MessageContent>
    </Message>
  )
}

export function WorkspaceChat() {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const inputRef = useRef<HTMLTextAreaElement>(null)
  const panelRef = useRef<HTMLDivElement>(null)
  const triggerRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (!open) return

    function handlePointerDown(event: PointerEvent) {
      const target = event.target
      if (!(target instanceof Node)) return
      if (!panelRef.current?.contains(target) && !triggerRef.current?.contains(target)) {
        setOpen(false)
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setOpen(false)
    }

    document.addEventListener("pointerdown", handlePointerDown)
    document.addEventListener("keydown", handleKeyDown)
    const focusTimer = window.setTimeout(() => inputRef.current?.focus(), 0)

    return () => {
      document.removeEventListener("pointerdown", handlePointerDown)
      document.removeEventListener("keydown", handleKeyDown)
      window.clearTimeout(focusTimer)
    }
  }, [open])

  function resetConversation() {
    if (loading) return
    setMessages([])
    setInput("")
    setError("")
    window.setTimeout(() => inputRef.current?.focus(), 0)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const content = input.trim()
    if (!content || loading) return

    setInput("")
    setError("")
    setMessages((current) => [
      ...current,
      { id: `user-${Date.now()}`, role: "user", content, createdAt: "刚刚" },
    ])
    setLoading(true)

    try {
      const response = await executeWorkspaceChat(content)
      setMessages((current) => [
        ...current,
        { id: `assistant-${Date.now()}`, role: "assistant", content: response, createdAt: "刚刚" },
      ])
    } catch (err) {
      setError(err instanceof Error ? err.message : "AI 暂时不可用，请稍后重试。")
    } finally {
      setLoading(false)
    }
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          ref={triggerRef}
          type="button"
          variant="ghost"
          size="icon"
          aria-label="Open Cynexus AI"
          title="Open Cynexus AI"
        >
          <MessageCircleDashed className="size-4" />
        </Button>
      </PopoverTrigger>
      {open
        ? createPortal(
            <div
              ref={panelRef}
              role="dialog"
              aria-label="Cynexus AI chat"
              className="fixed right-6 bottom-6 z-50 flex h-[min(78vh,760px)] w-[min(calc(100vw-2rem),560px)] flex-col gap-0 overflow-hidden rounded-[min(var(--radius-4xl),24px)] border bg-popover p-0 text-popover-foreground shadow-md max-[640px]:right-4 max-[640px]:bottom-4"
            >
          <div className="grid auto-rows-min items-start gap-1 border-b px-5 py-5">
          <div className="flex items-center justify-between gap-4">
            <div className="min-w-0">
              <h2 className="text-base font-medium">New Chat</h2>
              <p className="text-sm text-muted-foreground">How can I help you today?</p>
            </div>
            <Button
              type="button"
              variant="outline"
              size="icon-sm"
              aria-label="Reset conversation"
              title="Reset conversation"
              onClick={resetConversation}
              disabled={loading}
            >
              <RotateCw />
            </Button>
          </div>
          </div>

          <MessageScrollerProvider autoScroll>
          <MessageScroller className="min-h-0 flex-1">
            <MessageScrollerViewport>
              <MessageScrollerContent className="p-4" aria-busy={loading}>
                {messages.length === 0 && !loading ? (
                  <Empty className="min-h-full flex-1 p-8">
                    <EmptyHeader>
                      <EmptyMedia variant="icon">
                        <MessageCircleDashed />
                      </EmptyMedia>
                      <EmptyTitle>Morning, Cynexus!</EmptyTitle>
                      <EmptyDescription>
                        What are we working on today? Press send to start a new conversation
                      </EmptyDescription>
                    </EmptyHeader>
                  </Empty>
                ) : null}

                {messages.map((message) => (
                  <MessageScrollerItem
                    key={message.id}
                    messageId={message.id}
                    scrollAnchor={message.role === "user"}
                  >
                    <MessageBubble message={message} />
                  </MessageScrollerItem>
                ))}

                {loading ? (
                  <MessageScrollerItem messageId="workspace-chat-loading">
                    <Message>
                      <Marker role="status">
                        <MarkerIcon>
                          <LoaderCircle className="animate-spin" />
                        </MarkerIcon>
                        <MarkerContent>Thinking</MarkerContent>
                      </Marker>
                    </Message>
                  </MessageScrollerItem>
                ) : null}
              </MessageScrollerContent>
            </MessageScrollerViewport>
            <MessageScrollerButton />
          </MessageScroller>
          </MessageScrollerProvider>

          <div className="flex flex-col gap-2 px-5 py-5">
          {error ? <p className="text-xs leading-5 text-destructive">{error}</p> : null}
          <form className="w-full" onSubmit={handleSubmit}>
            <InputGroup className="h-auto flex-col items-stretch">
              <InputGroupTextarea
                ref={inputRef}
                value={input}
                onChange={(event) => setInput(event.target.value)}
                placeholder="What are we working on today?"
                aria-label="Message Cynexus AI"
                disabled={loading}
                rows={2}
                className="scrollbar-none [&::-webkit-scrollbar]:hidden"
              />
              <InputGroupAddon align="block-end">
                <InputGroupButton type="button" aria-label="Add files" disabled>
                  <Plus />
                </InputGroupButton>
                <InputGroupButton
                  type="submit"
                  variant="default"
                  size="icon-sm"
                  aria-label="Send"
                  title="Send"
                  className="ml-auto"
                  disabled={!input.trim() || loading}
                >
                  <ArrowUp />
                </InputGroupButton>
              </InputGroupAddon>
            </InputGroup>
          </form>
          </div>
            </div>,
            document.body,
          )
        : null}
    </Popover>
  )
}
