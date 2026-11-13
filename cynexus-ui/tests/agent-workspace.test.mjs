import assert from "node:assert/strict"
import test from "node:test"
import { hookRenderer, loadTsModule } from "./helpers/react-hooks.mjs"

const providerService = {
  AI_REASONING_EFFORT_PARAM: "model_reasoning_effort",
  DEFAULT_AI_REASONING_EFFORT: "medium",
  normalizeAiReasoningEffort: value => ["low", "medium", "high"].includes(value) ? value : null,
  fetchAiProviders: async () => [{ id: 1, code: "bound", name: "Bound provider", is_enabled: true, models: ["base-model"], default_model: "base-model" }],
  fetchAiBindings: async () => [{ capability: "autonomous_agent", is_enabled: true, provider_id: 1, model_override: "agent-model", params: { model_reasoning_effort: "high" } }],
}
const model = loadTsModule(new URL("../src/lib/agent/model.ts", import.meta.url), { "@/services/ai/provider": providerService })
const timeline = loadTsModule(new URL("../src/lib/agent/timeline.ts", import.meta.url), {})
const plain = value => JSON.parse(JSON.stringify(value))
const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}

async function workspace(overrides = {}) {
  const renderer = hookRenderer()
  const services = {
    fetchAgentTools: async () => [],
    fetchAgentConversations: async () => [],
    searchAgentConversations: async () => [],
    fetchAgentMessages: async () => [],
    getAgentRun: async runNo => ({ runNo, conversationNo: "conversation", status: "SUCCEEDED", userInput: "Question", answer: "Answer" }),
    archiveAgentConversation: async () => true,
    ...overrides,
  }
  const { useAgentWorkspace } = loadTsModule(new URL("../src/hooks/use-agent-workspace.ts", import.meta.url), {
    react: renderer.react,
    "@/lib/agent/model": model,
    "@/lib/agent/timeline": timeline,
    "@/services/ai/agent": services,
    "@/services/ai/provider": providerService,
  })
  renderer.mount(() => useAgentWorkspace({ locale: "en-US" }))
  await renderer.settle()
  return renderer
}

test("transcript filtering keeps repeated earlier turns and hides tool preambles", () => {
  const messages = [
    { sequenceNo: 1, role: "user", content: "Again" },
    { sequenceNo: 2, role: "assistant", content: "First answer" },
    { sequenceNo: 3, role: "user", content: "Again" },
    { sequenceNo: 4, role: "assistant", content: "Let me check", toolCallsJson: '[{"name":"market"}]' },
    { sequenceNo: 5, role: "tool", content: "Raw tool output" },
    { sequenceNo: 6, role: "assistant", content: "Second answer", toolCallsJson: "[]" },
    { sequenceNo: 7, role: "system", content: "Internal instructions" },
  ]
  const run = { userInput: "Again", answer: "Second answer" }
  assert.deepEqual(Array.from(model.removeCurrentRunFromTranscript(messages, run, true), item => item.sequenceNo), [1, 2])
  assert.deepEqual(Array.from(model.removeCurrentRunFromTranscript(messages.slice(0, 2), { userInput: "Again" }, false), item => item.sequenceNo), [1, 2])
  assert.deepEqual(Array.from(model.removeCurrentRunFromTranscript(messages, null), item => item.sequenceNo), [1, 2, 3, 6])
})

test("timeline updates a tool in place and deduplicates replayed retry events", () => {
  let entries = timeline.appendThinkingDelta([], "Check ")
  entries = timeline.appendThinkingDelta(entries, "prices")
  entries = timeline.completeThinking(entries)
  entries = timeline.appendToolStep(entries, { stepNo: 2, toolCode: "market.quote", status: "RUNNING" })
  entries = timeline.appendToolStep(entries, { stepNo: 2, toolCode: "market.quote", status: "SUCCEEDED", summary: "Quote available" })
  entries = timeline.appendRetry(entries, { sequenceNo: 8, summary: "Provider retry attempt 2" })
  entries = timeline.appendRetry(entries, { sequenceNo: 8, summary: "Provider retry attempt 2" })
  assert.equal(entries.length, 3)
  assert.equal(entries[0].text, "Check prices")
  assert.equal(entries[0].done, true)
  assert.equal(entries[1].status, "SUCCEEDED")
  assert.equal(entries[1].summary, "Quote available")
  assert.equal(entries[2].attempt, 2)
  const restored = timeline.timelineFromSteps([
    { kind: "THINKING", stepNo: 1, outputJson: '{"content":"Detailed thought"}', summary: "Short thought" },
    { kind: "TOOL", stepNo: 2, toolCode: "market.quote", status: "SUCCEEDED" },
    { kind: "ANSWER", stepNo: 3, summary: "Final answer" },
  ])
  assert.deepEqual(Array.from(restored, entry => entry.type), ["thinking", "tool"])
  assert.equal(restored[0].text, "Detailed thought")
})

test("shared controller uses binding defaults, prevents double submit and deduplicates stream replay", async () => {
  const response = deferred(), calls = []
  let emit
  const page = await workspace({
    runAgentStream: (request, onEvent, signal) => { calls.push({ request, signal }); emit = onEvent; return response.promise },
    fetchAgentMessages: async () => [{ sequenceNo: 1, role: "user", content: "Hello" }, { sequenceNo: 2, role: "assistant", content: "World" }],
  })
  try {
    assert.equal(page.view().selectedProviderCode, "bound")
    assert.equal(page.view().selectedModelCode, "agent-model")
    assert.equal(page.view().providerDefaultReasoning, "high")
    page.view().setInput("Hello")
    await page.settle()
    const running = page.view().submit()
    await page.view().submit()
    await page.settle()
    assert.equal(calls.length, 1)
    assert.equal(calls[0].request.locale, "en-US")
    assert.equal(calls[0].request.preferredModelCode, "agent-model")
    emit({ sequenceNo: 1, type: "THINKING_DELTA", runNo: "run-1", delta: "Research" })
    emit({ sequenceNo: 1, type: "THINKING_DELTA", runNo: "run-1", delta: "Research" })
    emit({ sequenceNo: 2, type: "TOKEN_DELTA", runNo: "run-1", delta: "World" })
    emit({ sequenceNo: 2, type: "TOKEN_DELTA", runNo: "run-1", delta: "World" })
    await page.settle()
    assert.equal(page.view().run.answer, "World")
    assert.equal(page.view().timeline[0].text, "Research")
    assert.equal(page.view().canStop, true)
    response.resolve({ runNo: "run-1", conversationNo: "conversation", status: "SUCCEEDED", userInput: "Hello", answer: "World" })
    await running
    await page.settle()
    assert.equal(page.view().loading, false)
    assert.equal(page.view().canStop, false)
    assert.equal(page.view().displayedConversationMessages.length, 0)
  } finally { page.close() }
})

test("repeating the same question preserves the previous turn during streaming and after refresh", async () => {
  const response = deferred()
  const initial = [{ sequenceNo: 1, role: "user", content: "Question" }, { sequenceNo: 2, role: "assistant", content: "Answer" }]
  let messages = initial, emit
  const page = await workspace({
    fetchAgentMessages: async () => messages,
    runAgentStream: (_request, onEvent) => { emit = onEvent; return response.promise },
  })
  try {
    await page.view().openConversation({ conversationNo: "conversation", lastRunNo: "run-1" })
    await page.settle()
    page.view().setInput("Question")
    await page.settle()
    const running = page.view().submit()
    await page.settle()
    assert.deepEqual(plain(page.view().displayedConversationMessages), initial)
    emit({ sequenceNo: 1, runNo: "run-2", type: "TOKEN_DELTA", delta: "New answer" })
    await page.settle()
    assert.deepEqual(plain(page.view().displayedConversationMessages), initial)
    messages = [...initial, { sequenceNo: 3, role: "user", content: "Question" }, { sequenceNo: 4, role: "assistant", content: "New answer" }]
    response.resolve({ runNo: "run-2", conversationNo: "conversation", status: "SUCCEEDED", userInput: "Question", answer: "New answer" })
    await running
    await page.settle()
    assert.deepEqual(plain(page.view().displayedConversationMessages), initial)
  } finally { page.close() }
})

test("a history refresh failure preserves a successful Agent result", async () => {
  const page = await workspace({
    runAgentStream: async () => ({ runNo: "run-1", conversationNo: "conversation", status: "SUCCEEDED", userInput: "Question", answer: "Saved result" }),
    fetchAgentMessages: async () => { throw new Error("History temporarily unavailable") },
  })
  try {
    page.view().setInput("Question")
    await page.settle()
    await page.view().submit()
    await page.settle()
    assert.equal(page.view().run.status, "SUCCEEDED")
    assert.equal(page.view().run.answer, "Saved result")
    assert.equal(page.view().notice.title, "Unable to refresh conversation")
    assert.equal(page.view().loading, false)
  } finally { page.close() }
})

test("a slower history search cannot replace the newest query result", async () => {
  const slow = deferred(), fast = deferred()
  const page = await workspace({ searchAgentConversations: query => query === "slow" ? slow.promise : fast.promise })
  try {
    const first = page.view().refreshConversations("slow")
    const second = page.view().refreshConversations("fast")
    fast.resolve([{ conversationNo: "newest" }])
    await second
    slow.resolve([{ conversationNo: "stale" }])
    await first
    await page.settle()
    assert.equal(page.view().conversations[0].conversationNo, "newest")
    assert.equal(page.view().historyRefreshing, false)
  } finally { page.close() }
})

test("synchronous reruns expose no Stop action and prevent another submission", async () => {
  const response = deferred()
  let submissions = 0
  const page = await workspace({ retryAgent: () => response.promise, runAgentStream: async () => { submissions++; return {} } })
  try {
    await page.view().openConversation({ conversationNo: "conversation", lastRunNo: "run-1" })
    await page.settle()
    const running = page.view().rerun("retry")
    await page.settle()
    assert.equal(page.view().loading, true)
    assert.equal(page.view().canStop, false)
    page.view().setInput("Another question")
    await page.settle()
    await page.view().submit()
    assert.equal(submissions, 0)
    response.resolve({ runNo: "run-2", status: "SUCCEEDED", answer: "Retried" })
    await running
    await page.settle()
    assert.equal(page.view().loading, false)
  } finally { page.close() }
})

test("leaving the page aborts its stream and ignores late stream callbacks", async () => {
  const response = deferred()
  let signal, emit
  const page = await workspace({ runAgentStream: (_request, callback, abortSignal) => { signal = abortSignal; emit = callback; return response.promise } })
  page.view().setInput("Question")
  await page.settle()
  const running = page.view().submit()
  await page.settle()
  page.close()
  assert.equal(signal.aborted, true)
  emit({ sequenceNo: 1, runNo: "late", type: "TOKEN_DELTA", delta: "Late answer" })
  response.resolve({ runNo: "late", status: "SUCCEEDED", answer: "Late answer" })
  await running
  await page.settle()
  assert.equal(page.view().run.answer, "")
})
