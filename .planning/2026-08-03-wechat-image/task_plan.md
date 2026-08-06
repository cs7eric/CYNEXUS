# Task Plan: WeChat Image Intake

## Goal

Receive WeChat Official Account image messages, retain the uploaded image metadata, download the media into a configurable local directory, and expose a bounded result through the existing callback flow.

## Phases

- [completed] Phase 1: Inspect current callback/parser/infra flow and define image intake contract
- [completed] Phase 2: Implement domain model, parser, port, storage, and callback integration
- [completed] Phase 3: Add focused tests and configuration documentation
- [completed] Phase 4: Build and restart the Release wechat service
- [completed] Phase 5: Verify health, callback behavior, and changed files

## Decisions

- Keep the WeChat callback synchronous and return a text acknowledgment after storing the image.
- Store only metadata plus a local file copy in this slice; AI/OCR processing remains a separate follow-up.
- Bound the downloaded response size and use a configurable storage directory.

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| `cynexus-common-core` missing when invoking the nested WeChat POM directly | 1 | Re-run from the repository root with the starter module selected and `-am` |
| Docker image build ended with BuildKit `rpc error: ... EOF` while uploading context | 1 | Maven/package completed; inspect Docker daemon and retry the image build with plain progress |
| Simulated image URL returned HTTP 503 from `httpbin.org` | 1 | Keep the implementation unchanged; retry the end-to-end storage check with a stable public image URL |
| Cleanup command for the generated synthetic image was blocked by the shell safety policy | 1 | Leave the generated test file in the gitignored image storage directory and report its path |
