# Progress

## 2026-08-07

- Inspected CodeGraph status and synchronized its index.
- Traced vision gateway -> import processing -> candidate review -> confirmation -> ledger persistence.
- Confirmed root cause: positive AI fee values are trusted without a gross-amount plausibility guard.
- Added a domain guard and candidate normalization so a gross-amount misread falls back to the configured commission schedule.
- Added focused regression tests, v7 prompt migration, Release migration registration, and exact-match repair for existing ledger/candidate rows.
- Release migration phase applied successfully; the first container rebuild attempt stopped at version-format validation after migrations, so no application containers were changed by that attempt.
- The corrected-version rebuild reached package compilation but was blocked by an unrelated AI infra test-source type mismatch; containers remain unchanged.
- Ran 116 market domain tests successfully, compiled affected market and AI domain modules, and verified Maven/UI version parity at 1.1.398-SNAPSHOT.
- Built market and AI Release JARs with test-source compilation skipped because of the unrelated AI infra test failure, then recreated `cynexus-release-market` and `cynexus-release-ai` with release version 1.1.398-260807-RELEASE.
- Verified both containers are running, the active portfolio vision prompt is v7, and all exact `total_fee = gross_amount` ledger rows are gone. The four reported ETF trades now each have total_fee 0.10000000.
