# Progress

## 2026-08-11

- Confirmed the incident cause and exact market-master mappings.
- Read the Cynexus frontend, backend, Agent-tool, release, version-sync, and planning guidance.
- Started an isolated plan at `.planning/2026-08-11-portfolio-import-identity-repair`.
- Implemented exact unique stock/ETF master-data suggestions in the market review path without mutating stored candidate payloads.
- Updated the review table to show and edit the security name plus code, with a visible exact-match indicator for a suggested code.
- Maven compilation passed. The full confirmation-service test class has two existing duplicate-time failures unrelated to the suggestion path; targeted coverage for the new suggestion behavior remains to run.
- Targeted suggestion coverage, UI ESLint/build, Maven compilation, and version-sync verification all passed. Release deployment remains pending: `market,ui` image builds would include unrelated dirty workspace changes and run the curated market-migration bundle.
- User approved Release deployment. The first launch used a comma-delimited `-Services` value rejected by the script; retrying with PowerShell's array syntax.
- Market migration bundle applied successfully and Maven packaging completed, but the image build stopped because Docker could not reach `auth.docker.io` for `eclipse-temurin:21-jre-jammy`.
- Docker Hub remains unreachable and the required base-image tag is absent locally, although the currently running Release images contain compatible runtime layers. Evaluating a temporary local-tag fallback so the approved deployment can proceed without external registry access.
- Release deployment completed for `market` and `ui` at `1.1.398-260811-RELEASE`. Reused the prior Release runtime images as temporary local base-image tags because Docker Hub was unreachable, then removed those temporary tags after the build.
- Verified both rebuilt containers are running, market health is `UP`, the UI root returns HTTP 200, and import review `1786425423280` returns the expected suggestions: `红星发展 -> 600367`, `中京电子 -> 002579`.
