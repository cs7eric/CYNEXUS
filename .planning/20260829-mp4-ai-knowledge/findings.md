# Findings

- MP4 currently invokes polish, summary, and post_metadata separately.
- Core process invokes all three after worker transcription; task GET enrichment can invoke them again.
- Python worker creates fallback summary and sets polished equal to transcript.
- Knowledge writes already have an explicit confirmed Agent write path; automatic MP4 ingestion must use a domain port/owned knowledge API and create a DRAFT with idempotency.
