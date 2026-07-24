# Findings

## Initial

- Project rules are in `AGENTS.md`; DDD layering guidance is in `guide/DDD-IMPLEMENTATION-GUIDE.md`.
- Existing root planning files describe prior work and contain unrelated in-progress phases, so this task uses an isolated plan directory.

## Service Audit

- `ApplicationAnalysisPageAppService` directly owns `ApplicationAnalysisPageRepository`, target/config/result/snapshot orchestration, JSON parsing, target normalization, and recent30 coordination.
- `MediaTranscriptionAiAppService` directly owns prompt fallback, model routing, Agent completion, execution logging, content validation, and metadata parsing.
- `ApplicationAnalysisExecutionDomainService` already owns the recent30 AI execution flow through domain ports; the page refactor can reuse it instead of adding a wrapper around repository calls.
- `ApplicationAnalysisKlineInputBuilder` currently depends on API gateway/Market DTOs, so the page domain service will consume it through a domain input port implemented by the existing application adapter.
- Media completion and structured JSON extraction need domain ports implemented by application adapters around `AgentExecutor` and `StructuredJsonExtractor`.
- On PowerShell, the unquoted `-Dsurefire.failIfNoSpecifiedTests=false` was split by Maven into an invalid lifecycle phase; quote the full property argument for focused reactor runs.
- Domain focused Maven test compilation is currently blocked by an existing unstaged Knowledge Base constructor migration: `KnowledgeBaseDocumentGenerationDomainServiceTest` calls the old constructor while the user-modified implementation requires two additional ports. This is outside the requested files and will not be changed as part of this refactor.
- Running the domain module alone also requires the matching `cynexus-ai-common:1.1.205-SNAPSHOT` artifact in the local repository; the reactor package produced it but did not install it.
