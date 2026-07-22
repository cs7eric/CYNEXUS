# AI Agent Enterprise Security And Operations

## Trust Model

- Gateway identity assertions are the only runtime owner and tenant identity. Release rejects anonymous identity and client-supplied owner values.
- User input, provider output, market text, and knowledge documents are untrusted data. They are never treated as executable instructions.
- The runtime catalog contains only registered handlers. Tool input is schema-validated and the handler must be read-only and non-confirmation-gated for autonomous execution.
- ClickHouse access uses a bounded typed query template. Arbitrary SQL, shell execution, file access, credentials, and arbitrary outbound destinations are outside the Agent boundary.

## Prompt Injection Boundary

Tool results are data. The Agent system prompt explicitly marks external text as untrusted, and the application strips/redacts sensitive values before persistence or the next model turn. A model cannot grant itself a new tool, permission, tenant, or write confirmation.

## Source URL and SSRF Policy

Only application-relative URLs and explicitly trusted HTTPS origins can reach the UI. `javascript:`, `data:`, `file:`, protocol-relative, control-character, and untrusted-origin URLs are rejected. Source metadata is produced only by a registered capability or persisted tool-backed citation.

## Runtime Limits

- 8 model turns, 12 tool calls, 4 calls per turn, 12,000 input characters, 24,000 output characters per tool result, and a 600-second total run limit.
- Redis leases and PostgreSQL terminal state protect concurrent execution. Cancellation is durable and checked during Provider/tool waits.
- Provider/tool retries are bounded and circuit state is shared by dependency key. Validation, authorization, policy, and unsupported-tool errors are not retried.

## Retention And Privacy

Retention archives terminal runs in bounded batches. Archived records retain status, source, tool code, data dates, and failure classification while clearing transcript, tool payload, event answer/delta/error, and other sensitive bodies. Hidden chain-of-thought is never persisted.

## Incident Response

1. Stop the affected Agent worker or disable the capability binding in AI Provider routing.
2. Preserve the run number, trace id, event sequence, provider/model, tool code, and failure category.
3. Rotate the provider secret if a credential exposure is suspected; never include the credential in an issue or log.
4. Review tenant-scoped audit rows and retention status before re-enabling the capability.

## Data Residency

PostgreSQL Agent tables stay in `cynexus_appl_ai`; tool data remains in its owning service. Provider routing must document the configured external endpoint and organization policy before sensitive tenant data is sent outside the deployment region.

## Required Regression Corpus

The test suite must cover instruction-like tool output, secret/email/mobile redaction, malformed schemas, unknown/duplicate calls, cross-tenant access, unsafe source URLs, oversized payloads, quota abuse, provider timeout, tool timeout, and cancellation races.
