import {
  Bot,
  Braces,
  BrainCircuit,
  Cable,
  DatabaseZap,
  FileJson,
  GitBranch,
  MessagesSquare,
  Radar,
  Route,
  ShieldCheck,
  Sparkles,
} from "lucide-react"
import type { LucideIcon } from "lucide-react"

export type AiCapabilityStatus = "ready" | "wiring" | "planned"

export interface AiCapability {
  title: string
  description: string
  status: AiCapabilityStatus
  icon: LucideIcon
  anchors: string[]
}

export interface AiFoundationItem {
  label: string
  value: string
  tone: "ok" | "warn" | "idle"
}

export const aiCapabilities: AiCapability[] = [
  {
    title: "ChatClient",
    description: "Spring AI conversation entry for provider-neutral text generation.",
    status: "wiring",
    icon: MessagesSquare,
    anchors: ["DashScope", "DeepSeek", "OpenAI-compatible"],
  },
  {
    title: "Prompt",
    description: "Tenant prompt registry with versions, scenes, safety notes, and structured variables.",
    status: "ready",
    icon: Sparkles,
    anchors: ["ai_prompt", "version", "scene"],
  },
  {
    title: "Memory",
    description: "Conversation state experiments for short-session continuity and advisor context.",
    status: "planned",
    icon: BrainCircuit,
    anchors: ["session", "summary", "store"],
  },
  {
    title: "SSE",
    description: "Streaming response lane for token-by-token AI feedback in long running analysis.",
    status: "wiring",
    icon: Cable,
    anchors: ["stream", "cancel", "trace"],
  },
  {
    title: "Function Calling",
    description: "Tool boundary for market data, scheduler triggers, stock snapshots, and RAG lookup.",
    status: "planned",
    icon: Braces,
    anchors: ["tool", "schema", "audit"],
  },
  {
    title: "RAG",
    description: "Knowledge base flow using PGVector, document chunks, recall evaluation, and feedback.",
    status: "planned",
    icon: DatabaseZap,
    anchors: ["PGVector", "chunk", "recall"],
  },
  {
    title: "Multi-model Router",
    description: "Route prompts to DashScope, DeepSeek, or local providers by scene and budget.",
    status: "planned",
    icon: Route,
    anchors: ["provider", "policy", "fallback"],
  },
  {
    title: "Structured Output",
    description: "Typed result contracts for stock analysis, ranking reasons, and evaluation reports.",
    status: "wiring",
    icon: FileJson,
    anchors: ["zod", "json schema", "validation"],
  },
]

export const aiFoundationItems: AiFoundationItem[] = [
  { label: "Prompt CRUD", value: "ready", tone: "ok" },
  { label: "Provider settings", value: "ready", tone: "ok" },
  { label: "AI gateway API", value: "wiring", tone: "warn" },
  { label: "Streaming UI", value: "planned", tone: "idle" },
]

export const aiTracks = [
  {
    title: "Provider",
    description: "Spring AI Alibaba / DashScope / DeepSeek integration lane.",
    icon: Bot,
  },
  {
    title: "Agent",
    description: "Advisor, tool calling, memory, and dynamic agent experiments.",
    icon: GitBranch,
  },
  {
    title: "Guardrail",
    description: "Prompt safety, structured output validation, eval, and feedback.",
    icon: ShieldCheck,
  },
  {
    title: "Knowledge",
    description: "PGVector retrieval, chunk quality, recall tests, and citation flow.",
    icon: Radar,
  },
]

export const aiStatusLabel: Record<AiCapabilityStatus, string> = {
  ready: "Ready",
  wiring: "Wiring",
  planned: "Planned",
}
