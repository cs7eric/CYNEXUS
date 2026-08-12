# Findings

- CodeGraph is indexed and up to date.
- Prior commit `3988bff49` added queued URL transcription process history and a media-specific Settings API Workflows page.
- Existing trade import history is event-based in `portfolio_import_process_history`.
- Finance AI capture has batch status and retry APIs but needs a normalized process projection.
- Existing UI business-result routes must remain the source of detailed output.
- Core and Market now expose separate normalized projections so the UI does not create a cross-domain backend dependency.
- No migration is needed for this projection phase: it reads the existing media process, finance capture, and portfolio import history tables.
- The unified operational list is documented as unsuitable for autonomous Agent exposure; detailed business queries remain owned by their existing domains.
- Core combines media transcription and finance capture records; Market projects portfolio imports. Because the Settings timeline spans both services, UI-level merging must happen before slicing the requested global page.
- The existing Core endpoint already accepts type/status; Market only projects portfolio imports, so its workflow service can apply the unified type/status filter before slicing its response.
