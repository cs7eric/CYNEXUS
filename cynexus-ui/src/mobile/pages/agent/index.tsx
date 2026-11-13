import { ListBox } from "@heroui/react/list-box"
import { Modal } from "@heroui/react/modal"
import { Select } from "@heroui/react/select"
import { Archive, ArrowDown, ArrowRight, ArrowUp, BookOpenText, Check, ChevronDown, Clock3, Download, LoaderCircle, MessageSquare, MoreHorizontal, Plus, RefreshCw, Search, SlidersHorizontal, Sparkles, Square, X } from "lucide-react"
import { Component, lazy, Suspense, useEffect, useLayoutEffect, useRef, useState, type ReactNode } from "react"

import { useAgentWorkspace } from "@/hooks/use-agent-workspace"
import { clarification, conversationLabel, evidence } from "@/lib/agent/model"
import type { AgentTimelineEntry } from "@/lib/agent/timeline"
import { useMobilePortal } from "@/mobile/hooks/use-mobile-portal"
import { MobileButton } from "@/mobile/ui/button"
import type { AgentRun } from "@/services/ai/agent"

import { MobileAgentFeedback, MobileAgentTaskPanel, MobileAgentWritePanel } from "./components/agent-panels"
import "./agent.css"

const AgentMarkdown = lazy(() => import("@/views/cynexus-agent/components/agent-markdown").then(module => ({ default: module.AgentMarkdown })))

const panels = [
  { id: "trace", label: "Activity", icon: Clock3 },
  { id: "write", label: "Save to library", icon: BookOpenText },
  { id: "tasks", label: "Scheduled tasks", icon: Clock3 },
  { id: "modules", label: "Tools", icon: Sparkles },
]

function safeSourceUrl(value?: string | null) {
  if (!value) return undefined
  try {
    const url = new URL(value)
    return ["https:", "http:"].includes(url.protocol) ? url.href : undefined
  } catch { return undefined }
}

function runStatus(run: AgentRun | null, loading: boolean) {
  if (run?.status === "CANCEL_REQUESTED") return "Stopping…"
  if (loading) return "Working…"
  if (run?.status === "RUNNING" || run?.status === "QUEUED") return "Run in progress"
  if (run?.status === "NEEDS_CLARIFICATION") return "Needs your input"
  if (run?.status === "CANCELLED") return "Cancelled"
  if (run?.status === "FAILED") return "Failed"
  return "Cynexus Agent"
}

class AnswerBoundary extends Component<{ children: ReactNode; text: string }, { failed: boolean }> {
  state = { failed: false }
  static getDerivedStateFromError() { return { failed: true } }
  render() { return this.state.failed ? <p className="ma-plain-answer">{this.props.text}</p> : this.props.children }
}

function Answer({ text }: { text: string }) {
  return <AnswerBoundary text={text}><Suspense fallback={<p className="ma-muted" role="status">Loading answer…</p>}><AgentMarkdown markdown={text} className="ma-markdown" /></Suspense></AnswerBoundary>
}

function Activity({ entries, loading }: { entries: AgentTimelineEntry[]; loading: boolean }) {
  return <div className="ma-activity">
    {!entries.length && <p className="ma-muted">{loading ? "Waiting for the Agent…" : "Tool activity will appear here when you send a message."}</p>}
    {entries.map(entry => <div className="ma-activity-entry" key={entry.id}>
      {entry.type === "thinking" ? <><span className="ma-activity-mark"><Sparkles /></span><details><summary>Thinking{!entry.done ? "…" : ""}</summary><p>{entry.text}</p></details></> :
        <><span className="ma-activity-mark">{entry.type === "retry" ? <RefreshCw /> : <Check />}</span><div><h3>{entry.type === "tool" ? entry.toolCode : "Retrying"}</h3><p>{entry.summary || "No additional details."}</p>{entry.type === "tool" && <small>{entry.status.replaceAll("_", " ")}</small>}</div></>}
    </div>)}
  </div>
}

function Sources({ run }: { run: AgentRun }) {
  const sources = evidence(run)
  if (!sources.length) return null
  return <details className="ma-sources"><summary><BookOpenText />Sources · {sources.length}<ChevronDown /></summary>
    <ol>{sources.map((source, index) => <li key={`${source.toolCode}-${index}`}>
      <strong>{source.source || source.toolCode || `Source ${index + 1}`}</strong>
      {source.dataDate && <small>Data as of {source.dataDate}</small>}
      {source.summary && <p>{source.summary}</p>}
      {safeSourceUrl(source.sourceUrl) && <a href={safeSourceUrl(source.sourceUrl)} target="_blank" rel="noreferrer">Open source</a>}
      {source.citations?.map((citation, citationIndex) => <div className="ma-citation" key={citation.citationId || citationIndex}>
        {safeSourceUrl(citation.url) ? <a href={safeSourceUrl(citation.url)} target="_blank" rel="noreferrer">{citation.title || citation.publisher || "Reference"}</a> : <strong>{citation.title || citation.publisher || "Reference"}</strong>}
        <small>{[citation.publisher, citation.publicationDate && `Published ${citation.publicationDate}`, citation.dataDate && `Data ${citation.dataDate}`, citation.retrievedAt && `Retrieved ${citation.retrievedAt}`, citation.freshness, citation.availability, citation.conflict && "Conflicting source"].filter(Boolean).join(" · ")}</small>
        {citation.excerpt && <p>{citation.excerpt}</p>}
      </div>)}
    </li>)}</ol>
  </details>
}

export default function MobileAgentPage() {
  const agent = useAgentWorkspace({ locale: "en-US" })
  const portal = useMobilePortal()
  const messages = useRef<HTMLDivElement>(null)
  const composer = useRef<HTMLTextAreaElement>(null)
  const followLatest = useRef(true)
  const [awayFromLatest, setAwayFromLatest] = useState(false)
  const [keyboardOpen, setKeyboardOpen] = useState(false)
  const [archiveCandidate, setArchiveCandidate] = useState<string | null>(null)
  const [panelBusy, setPanelBusy] = useState(false)
  const [visitedPanels, setVisitedPanels] = useState(() => new Set<string>())
  const { workspaceOpen, setWorkspaceOpen, setWorkspaceTab, refreshConversations, historyQuery } = agent
  const question = agent.run ? clarification(agent.run) : null
  const empty = !agent.run && !agent.displayedConversationMessages.length
  const busy = agent.loading || agent.conversationLoading
  const historyOpen = agent.workspaceTab === "history"
  const modelOptions = [...new Set([agent.selectedModelCode, agent.selectedProvider?.default_model, ...(agent.selectedProvider?.models ?? [])].filter((model): model is string => Boolean(model)))]
  const modelTriggerLabel = agent.configLoading
    ? "Loading model…"
    : agent.selectedModelCode || (agent.selectedProviderCode ? "Provider default" : "Workspace default")

  function selectPanel(tab: string) {
    setVisitedPanels(current => current.has(tab) ? current : new Set([...current, tab]))
    agent.setWorkspaceTab(tab)
  }
  function openPanel(tab: string) { selectPanel(tab); agent.setWorkspaceOpen(true) }
  function scrollToLatest() {
    followLatest.current = true
    setAwayFromLatest(false)
    const element = messages.current
    if (element) element.scrollTop = element.scrollHeight
  }

  useLayoutEffect(() => {
    if (followLatest.current && messages.current) messages.current.scrollTop = messages.current.scrollHeight
  }, [agent.run?.answer, agent.run?.status, agent.timeline.length, agent.displayedConversationMessages.length])

  useEffect(() => {
    const element = messages.current
    const content = element?.firstElementChild
    if (!element || !content) return
    const observer = new ResizeObserver(() => {
      if (followLatest.current) element.scrollTop = element.scrollHeight
    })
    observer.observe(content)
    return () => observer.disconnect()
  }, [empty])

  useLayoutEffect(() => {
    const field = composer.current
    if (!field) return
    field.style.height = "auto"
    field.style.height = `${Math.min(field.scrollHeight, 120)}px`
  }, [agent.input])

  useEffect(() => {
    function onBack(event: Event) {
      if (!workspaceOpen) return
      event.preventDefault()
      if (!panelBusy) setWorkspaceOpen(false)
    }
    window.addEventListener("mobile-back", onBack)
    return () => window.removeEventListener("mobile-back", onBack)
  }, [workspaceOpen, setWorkspaceOpen, panelBusy])

  useEffect(() => {
    function onHistory() {
      if (panelBusy) return
      setArchiveCandidate(null)
      setWorkspaceTab("history")
      setWorkspaceOpen(true)
      void refreshConversations(historyQuery)
    }
    window.addEventListener("mobile-agent-history", onHistory)
    return () => window.removeEventListener("mobile-agent-history", onHistory)
  }, [panelBusy, setWorkspaceTab, setWorkspaceOpen, refreshConversations, historyQuery])

  useEffect(() => {
    const viewport = window.visualViewport
    const expandedHeight = Math.max(window.innerHeight, viewport?.height ?? 0)
    function updateKeyboard() {
      const active = document.activeElement
      const editing = active instanceof HTMLElement && active.matches("textarea, input, [contenteditable='true']")
      setKeyboardOpen(editing && expandedHeight - (viewport?.height ?? window.innerHeight) > 140)
    }
    viewport?.addEventListener("resize", updateKeyboard)
    document.addEventListener("focusin", updateKeyboard)
    document.addEventListener("focusout", updateKeyboard)
    return () => {
      viewport?.removeEventListener("resize", updateKeyboard)
      document.removeEventListener("focusin", updateKeyboard)
      document.removeEventListener("focusout", updateKeyboard)
    }
  }, [])

  function send() { scrollToLatest(); void agent.submit() }

  return <section className="mobile-agent" data-keyboard-open={keyboardOpen} aria-label="Cynexus Agent">
    <div className="ma-toolbar">
      <div><h2>{empty ? "Your Agent" : "Conversation"}</h2><p>{agent.loading ? "Working with your workspace" : "A little help, a clearer day."}</p></div>
      <div className="ma-toolbar-actions">
        <MobileButton variant="ghost" isIconOnly aria-label="New conversation" isDisabled={busy} onPress={() => { if (agent.newConversation()) scrollToLatest() }}><Plus /></MobileButton>
        <MobileButton variant="ghost" isIconOnly aria-label="Agent options" onPress={() => openPanel("trace")}><MoreHorizontal /></MobileButton>
      </div>
    </div>

    {agent.notice && <div className="ma-notice" role={agent.notice.tone === "danger" ? "alert" : "status"}><p><strong>{agent.notice.title}</strong>{agent.notice.description && <span>{agent.notice.description}</span>}</p><MobileButton variant="ghost" isIconOnly aria-label="Dismiss notification" onPress={agent.clearNotice}><X /></MobileButton></div>}

    <div className="ma-conversation" ref={messages} role="region" aria-label="Conversation messages" tabIndex={0} onScroll={() => {
      const element = messages.current
      if (!element) return
      followLatest.current = element.scrollHeight - element.scrollTop - element.clientHeight < 72
      setAwayFromLatest(!followLatest.current)
    }}>
      {empty ? <div className="ma-welcome">
        <span className="ma-agent-symbol"><Sparkles strokeWidth={1.4} /></span>
        <h3>What’s on<br />your mind?</h3>
        <p>Find answers, connect the dots,<br />and get things moving.</p>
        <div className="ma-suggestions">
          {["What can you help me with?", "Summarize my recent notes", "Review my spending this month"].map((prompt, index) => <MobileButton key={prompt} variant="ghost" onPress={() => agent.setInput(prompt)}><span>{index === 0 ? <Sparkles /> : index === 1 ? <BookOpenText /> : <SlidersHorizontal />}{prompt}</span><ArrowUp /></MobileButton>)}
        </div>
      </div> : <div className="ma-transcript">
        {agent.displayedConversationMessages.map((message, index) => <article key={`${message.sequenceNo}-${index}`} className={`ma-message ma-message-${message.role}`} aria-label={message.role === "user" ? "You" : "Agent"}>
          {message.role === "user" ? <p>{message.content}</p> : <><span className="ma-author"><Sparkles />Cynexus Agent</span><Answer text={message.content || ""} /></>}
        </article>)}
        {agent.run?.userInput && <article className="ma-message ma-message-user" aria-label="You"><p>{agent.run.userInput}</p></article>}
        {agent.run && <article className="ma-message ma-message-assistant" aria-label="Agent">
          <span className="ma-author">{agent.loading ? <LoaderCircle className="ma-spinning" /> : <Sparkles />}<span role="status">{runStatus(agent.run, agent.loading)}</span></span>
          {(agent.loading || agent.timeline.length > 0) && <MobileButton variant="ghost" className="ma-inline-activity" onPress={() => openPanel("trace")}><Clock3 />{agent.loading ? "View live activity" : `${agent.timeline.length} activity ${agent.timeline.length === 1 ? "step" : "steps"}`}<ChevronDown /></MobileButton>}
          {!agent.loading && ["RUNNING", "QUEUED", "CANCEL_REQUESTED"].includes(agent.run.status || "") && agent.conversations.find(item => item.conversationNo === agent.run?.conversationNo) && <MobileButton variant="ghost" className="ma-inline-activity" isDisabled={agent.historyLoading} onPress={() => { const conversation = agent.conversations.find(item => item.conversationNo === agent.run?.conversationNo); if (conversation) void agent.openConversation(conversation) }}><RefreshCw />Refresh run status</MobileButton>}
          {agent.run.answer && <Answer text={agent.run.answer} />}
          {agent.run.errorMessage && <p className="ma-error" role="alert">{agent.run.errorMessage}</p>}
          {question && <div className="ma-clarification"><h3>A little more detail</h3>{question.summary && <p>{question.summary}</p>}<ul>{question.questions?.map(item => <li key={item}>{item}</li>)}</ul><p>Reply below to continue this conversation.</p></div>}
          {!agent.loading && !agent.run.answer && !agent.run.errorMessage && !question && <p className="ma-muted">{["RUNNING", "QUEUED", "CANCEL_REQUESTED"].includes(agent.run.status || "") ? "This run is still in progress. Refresh to check for updates." : agent.run.status === "CANCELLED" ? "This run was stopped. You can send another message when you’re ready." : "No answer was returned for this run."}</p>}
          {!agent.loading && <Sources run={agent.run} />}
          {agent.showRunActions && <>
            <div className="ma-result-actions"><MobileButton variant="ghost" onPress={() => void agent.rerun(agent.run?.status === "FAILED" || agent.run?.status === "CANCELLED" ? "retry" : "regenerate")}><RefreshCw />{agent.run.status === "FAILED" || agent.run.status === "CANCELLED" ? "Retry" : "Regenerate"}</MobileButton><MobileButton variant="ghost" isIconOnly aria-label="Export answer" onPress={() => void agent.exportResult()}><Download /></MobileButton>{agent.run.answer && <MobileButton variant="ghost" onPress={() => openPanel("write")}><BookOpenText />Save</MobileButton>}</div>
            <MobileAgentFeedback key={agent.run.runNo} run={agent.run} />
          </>}
        </article>}
      </div>}
    </div>

    {awayFromLatest && <MobileButton className="ma-jump" variant="secondary" onPress={scrollToLatest}><ArrowDown />Latest message</MobileButton>}

    <form className="ma-composer" onSubmit={event => { event.preventDefault(); if (!busy) send() }}>
      <label className="ma-sr-only" htmlFor="mobile-agent-message">Message Cynexus Agent</label>
      <textarea ref={composer} id="mobile-agent-message" name="message" rows={1} value={agent.input} onChange={event => agent.setInput(event.target.value)} placeholder={question ? "Add the missing details…" : "Ask anything…"} maxLength={32000} enterKeyHint="enter" />
      {agent.configError && <div className="ma-config-error"><p role="alert" className="ma-error">{agent.configError}</p><MobileButton type="button" variant="ghost" onPress={() => void agent.reloadWorkspace()}>Reload settings</MobileButton></div>}
      <div className="ma-composer-bottom">
        <div className="ma-composer-selects">
          <Select fullWidth className="ma-composer-select" aria-label="Provider" value={agent.selectedProviderCode || "__workspace-default"} isDisabled={agent.configLoading || busy} onChange={value => {
            const code = value === "__workspace-default" ? "" : String(value ?? "")
            const provider = agent.providers.find(item => item.code === (code || agent.defaultProviderCode))
            agent.setSelectedProviderCode(code)
            agent.setSelectedModelCode(code ? provider?.default_model || provider?.models[0] || "" : "")
            agent.setSelectedReasoningEffort("")
          }}>
            <Select.Trigger><Select.Value>{agent.configLoading ? "Provider…" : agent.selectedProviderCode ? agent.providerDisplayName : "Default provider"}</Select.Value><Select.Indicator /></Select.Trigger>
            <Select.Popover UNSTABLE_portalContainer={portal} placement="top start" className="ma-select-popover"><ListBox aria-label="Provider"><ListBox.Item id="__workspace-default" textValue="Default provider">Default provider<ListBox.ItemIndicator /></ListBox.Item>{agent.providers.map(provider => <ListBox.Item id={provider.code} key={provider.code} textValue={provider.name || provider.code}>{provider.name || provider.code}<ListBox.ItemIndicator /></ListBox.Item>)}</ListBox></Select.Popover>
          </Select>
          <Select fullWidth className="ma-composer-select ma-composer-model" aria-label="Model" value={agent.selectedModelCode || "__provider-default"} isDisabled={agent.configLoading || busy || !modelOptions.length} onChange={value => agent.setSelectedModelCode(value === "__provider-default" ? "" : String(value ?? ""))}>
            <Select.Trigger><Select.Value>{modelTriggerLabel}</Select.Value><Select.Indicator /></Select.Trigger>
            <Select.Popover UNSTABLE_portalContainer={portal} placement="top start" className="ma-select-popover"><ListBox aria-label="Model"><ListBox.Item id="__provider-default" textValue="Provider default">Provider default<ListBox.ItemIndicator /></ListBox.Item>{modelOptions.map(model => <ListBox.Item id={model} key={model} textValue={model}>{model}<ListBox.ItemIndicator /></ListBox.Item>)}</ListBox></Select.Popover>
          </Select>
        </div>
        {agent.loading ? <MobileButton type="button" variant="primary" className="ma-send" isIconOnly aria-label={agent.canStop ? "Stop Agent" : "Regenerating answer"} isDisabled={!agent.canStop || agent.run?.status === "CANCEL_REQUESTED"} onPress={() => void agent.stopAgent()}>{agent.canStop ? <Square /> : <LoaderCircle className="ma-spinning" />}</MobileButton> : <MobileButton type="submit" variant="primary" className="ma-send" isIconOnly aria-label="Send message" isDisabled={busy || !agent.input.trim()}><ArrowUp /></MobileButton>}
      </div>
    </form>

    <p className="ma-sr-only" aria-live="polite" aria-atomic="true">{!agent.loading && agent.run ? `Agent response ${agent.run.status === "FAILED" ? "failed" : agent.run.status === "CANCELLED" ? "cancelled" : "complete"}. ${agent.run.answer || agent.run.errorMessage || question?.summary || question?.questions?.join(" ") || ""}` : ""}</p>
    <Modal.Backdrop UNSTABLE_portalContainer={portal} isOpen={agent.workspaceOpen} onOpenChange={open => { if (!panelBusy) agent.setWorkspaceOpen(open) }} isDismissable={!panelBusy} isKeyboardDismissDisabled={panelBusy} variant="opaque" className="ma-backdrop">
      <Modal.Container placement="bottom" scroll="inside" className="ma-panel-container"><Modal.Dialog className={`ma-sheet${historyOpen ? " ma-history-sheet" : ""}`} aria-label={historyOpen ? "Conversation history" : "Agent workspace"}>
        {historyOpen ? <Modal.Header className="ma-sheet-heading ma-history-heading"><Modal.Heading>History</Modal.Heading><div className="ma-history-heading-actions"><MobileButton variant="ghost" className="ma-history-new" isDisabled={busy || Boolean(agent.archiveLoadingNo)} onPress={() => { if (agent.newConversation()) scrollToLatest() }}><Plus />New chat</MobileButton><MobileButton variant="ghost" isIconOnly aria-label="Close conversation history" slot="close"><X /></MobileButton></div></Modal.Header> : <>
          <Modal.Header className="ma-sheet-heading"><Modal.Heading>Agent workspace</Modal.Heading><MobileButton variant="ghost" isIconOnly aria-label="Close Agent workspace" slot="close" isDisabled={panelBusy}><X /></MobileButton></Modal.Header>
          <nav className="ma-panel-tabs" aria-label="Agent workspace sections">{panels.map(panel => <MobileButton key={panel.id} variant="ghost" isDisabled={panelBusy} aria-current={agent.workspaceTab === panel.id ? "page" : undefined} onPress={() => selectPanel(panel.id)}><panel.icon /><span>{panel.label}</span></MobileButton>)}</nav>
        </>}
        <Modal.Body className="ma-panel-body">
          {historyOpen && <section className="ma-history" aria-label="Conversation history">
            <form className="ma-history-search" onSubmit={event => { event.preventDefault(); void agent.refreshConversations(agent.historyQuery) }}><Search aria-hidden="true" /><label className="ma-sr-only" htmlFor="ma-history-query">Search conversations</label><input id="ma-history-query" value={agent.historyQuery} onChange={event => agent.setHistoryQuery(event.target.value)} placeholder="Search conversations" type="search" /><MobileButton type="submit" variant="ghost" isIconOnly aria-label="Search conversations" isDisabled={agent.historyLoading}><ArrowRight /></MobileButton></form>
            <div className="ma-section-title"><h3>{agent.historyQuery ? "Search results" : "Recent conversations"}</h3><MobileButton variant="ghost" isIconOnly aria-label="Refresh conversations" isDisabled={agent.historyLoading} onPress={() => void agent.refreshConversations(agent.historyQuery)}><RefreshCw className={agent.historyLoading ? "ma-spinning" : undefined} /></MobileButton></div>
            {agent.historyLoading && <p className="ma-history-status ma-muted" role="status">{agent.conversationLoading ? "Opening conversation…" : "Loading conversations…"}</p>}
            {agent.historyError && <div className="ma-history-status"><p className="ma-error" role="alert">{agent.historyError}</p><MobileButton variant="secondary" onPress={() => void agent.refreshConversations(agent.historyQuery)}>Try again</MobileButton></div>}
            {!agent.historyLoading && !agent.historyError && !agent.conversations.length && <div className="ma-history-empty"><MessageSquare aria-hidden="true" /><h3>{agent.historyQuery ? "No conversations found" : "Your next idea starts here"}</h3><p>{agent.historyQuery ? "Try a different search." : "Start a chat. You can come back to it here."}</p></div>}
            <div className="ma-history-list">{agent.conversations.map(conversation => {
              const current = conversation.conversationNo === agent.run?.conversationNo
              const date = conversation.updatedAt || conversation.createdAt
              return <div key={conversation.conversationNo} className="ma-history-row" data-current={current || undefined}><MobileButton variant="ghost" className="ma-history-open" aria-current={current ? "page" : undefined} isDisabled={busy || !conversation.lastRunNo || Boolean(agent.archiveLoadingNo)} onPress={() => { scrollToLatest(); void agent.openConversation(conversation) }}><span className="ma-history-icon"><MessageSquare /></span><span className="ma-history-copy"><strong>{conversationLabel(conversation)}</strong><span className="ma-history-meta">{date && <time dateTime={date}>{new Date(date).toLocaleDateString("en-US", { month: "short", day: "numeric" })}</time>}{current && <small>Current</small>}</span></span></MobileButton><MobileButton variant="ghost" className="ma-history-archive" isIconOnly aria-label={`Archive ${conversationLabel(conversation)}`} isDisabled={busy || Boolean(agent.archiveLoadingNo)} onPress={() => setArchiveCandidate(conversation.conversationNo)}><Archive /></MobileButton>{archiveCandidate === conversation.conversationNo && <div className="ma-archive-confirm"><p>Archive this conversation?</p><MobileButton variant="ghost" onPress={() => setArchiveCandidate(null)}>Keep</MobileButton><MobileButton variant="secondary" isDisabled={Boolean(agent.archiveLoadingNo)} onPress={() => { setArchiveCandidate(null); void agent.archiveConversation(conversation) }}>Archive</MobileButton></div>}</div>
            })}</div>
          </section>}
          {agent.workspaceTab === "trace" && <section><h3 className="ma-panel-title">Run activity</h3><Activity entries={agent.timeline} loading={agent.loading} /></section>}
          {visitedPanels.has("write") && <div hidden={agent.workspaceTab !== "write"}><MobileAgentWritePanel key={agent.run?.runNo || agent.run?.conversationNo || "new"} conversationNo={agent.run?.conversationNo} runNo={agent.run?.runNo} answer={agent.run?.answer} onBusyChange={setPanelBusy} /></div>}
          {visitedPanels.has("tasks") && <div hidden={agent.workspaceTab !== "tasks"}><MobileAgentTaskPanel onBusyChange={setPanelBusy} /></div>}
          {agent.workspaceTab === "modules" && <section><h3 className="ma-panel-title">Available tools</h3>{agent.configLoading && <p className="ma-muted">Loading tools…</p>}{agent.configError && <p className="ma-error" role="alert">{agent.configError}</p>}{!agent.configLoading && !agent.configError && !agent.tools.length && <p className="ma-muted">No tools are currently available.</p>}{agent.toolGroups.map(([group, tools]) => <details key={group} className="ma-tool-group"><summary>{group}<span>{tools.length}</span><ChevronDown /></summary>{tools.map(tool => <div className="ma-tool" key={tool.code}><strong>{tool.name || tool.code}</strong><p>{tool.description}</p><small>{tool.requiresConfirmation ? "Requires confirmation" : tool.readOnly ? "Read only" : "Write"} · {tool.availability || "Available"}{tool.freshness ? ` · ${tool.freshness}` : ""}</small></div>)}</details>)}</section>}
        </Modal.Body>
      </Modal.Dialog></Modal.Container>
    </Modal.Backdrop>
  </section>
}
