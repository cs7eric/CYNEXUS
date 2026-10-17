# Findings & Decisions

## Requirements
- Diagnose the HTTP 500 returned when uploading Finance through API Workflows.
- Fix the repository defect if confirmed and verify the result.

## Research Findings
- Gateway routes `/api/workflows/media-transcription/**` and `/api/workflows/finance-ai-capture/**`.
- The worktree contains newly added `ApiWorkflowIngressController.java` files under Core and Market, plus many unrelated pre-existing edits.
- Finance domain/repository code is under `cynexus-core`.
- Repository rules require CodeGraph-first inspection and prohibit starting local Java/Python backend runtimes.
- CodeGraph was present but stale; `codegraph sync` completed and indexed 95 changed files.
- Release stack is online. Core is `cynexus-core:1.1.399-260814-RELEASE`, gateway is `1.1.399-260814-RELEASE`; both have been up about 34 minutes at discovery time.
- No local application runtime was started.

## Technical Decisions
| Decision | Rationale |
|----------|-----------|
| Inspect both workflow ingress controllers and the Finance capture path | The user referred to API Workflows, while the newly added controllers may be part of the active change |

## Issues Encountered
| Issue | Resolution |
|-------|------------|
| Repository-wide search touched locked browser artifact files | Restrict searches to relevant source/config/log paths |

## Resources
- `docs/agent/codegraph.md`
- `docs/agent/backend-ddd.md`
- `deploy/AGENTS.md`
- `cynexus-core/cynexus-core-application/cynexus-core-application-controller/src/main/java/cn/cccs7/cynexus/core/application/controller/ApiWorkflowIngressController.java`
- `docs/agent/api-workflows.md`
