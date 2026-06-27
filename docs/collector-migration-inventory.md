# Cynexus Collector Migration Inventory

## Scope

Old project: `F:\dev-repo\mp4-to-word-new`

New service placeholder: `E:\project-repo\cynexus\cynexus-collector`

This inventory is read-only migration preparation. Do not copy the old project wholesale into `cynexus-collector`, because the old repository mixes Python backend code, React frontend code, local model files, DuckDB data, uploaded media, generated runtime data, and scheduler state.

## Current Collector State

- `cynexus-collector` exists but has no files yet.
- It is not part of the Maven reactor, which is fine because it should be a separate Python service, not a Java module.
- It is not yet present in local Docker Compose.
- It is not yet registered in Nacos or routed by Gateway.

## Old Project Shape

The old project is a mixed local-first workbench:

- Flask backend entrypoint: `app.py`
- Flask app factory: `backend/app_factory.py`
- Blueprint and scheduler bootstrap: `backend/bootstrap.py`
- Dev runner: `backend/runner.py`
- SQLAlchemy database config: `backend/config/database.py`
- Runtime path constants: `backend/config/settings.py`
- React/Vite frontend: `frontend`
- Scheduler JSON configs: `scheduler`
- Whisper model files: `models`
- Stock reference data, DuckDB, and local downloaded market files: `reference`
- Upload/output/runtime artifacts: `uploads`, `outputs`, `runtime`

## Startup Risks

- `app.py` calls `create_app(start_schedulers=...)`; `backend/bootstrap.py` may start many scheduler jobs during application startup.
- `backend/runner.py` tries to take over port `5000` by killing stale local processes on Windows. This behavior must not be used in a Docker/service runtime.
- `backend/app_factory.py` calls `load_dotenv()`. For the collector service, environment should come from Docker Compose or Nacos-compatible runtime config, not from copying the old `.env`.
- `backend/config/settings.py` derives many paths from the old repository root. Collector must replace those with explicit mount/env roots.

## Large Data And Model Boundaries

These must not be copied into Git or a Docker build context:

- `models/AI-ModelScope/whisper-large-v3` contains multi-GB model artifacts.
- `reference/stock/duckdb/market_data.duckdb` is several GB.
- `reference/stock/download/*/hsjday.zip` contains large daily downloaded archives.
- `uploads` contains large MP4/WAV files.
- `runtime` and `outputs` are generated runtime artifacts.

Use external bind mounts instead:

- `COLLECTOR_MODEL_ROOT`
- `COLLECTOR_REFERENCE_ROOT`
- `COLLECTOR_UPLOAD_ROOT`
- `COLLECTOR_OUTPUT_ROOT`
- `COLLECTOR_RUNTIME_ROOT`

## API Ownership Draft

Java-owned or already being rebuilt in Java:

- AI provider config and usage bindings
- Application analysis config/targets/results
- Scheduler job metadata, status, categories, and run history
- Market overview snapshots
- Market pulse snapshots/batches
- Market limit daily snapshots/stocks
- Sector fund-flow snapshots
- Self-selected lists/items

Python Collector-owned:

- AkShare/THS/ELTDX crawling quirks
- TDX `.day` file and hsjday archive handling
- Local DuckDB/day-file readers that cannot be moved cleanly to Java yet
- Whisper transcription and media processing
- Low-level collector job execution for market data and transcription

Legacy or later frontend migration:

- Old React routes under `frontend`
- Old `/api/stock-chart/**` compatibility surface
- Old direct `/api/ai/**` routes that conflict with the new Java AI service namespace

## Proposed Microservice Contract

Service name: `cynexus-collector`

Port: `18083`

Gateway route: `/api/collector/** -> lb://cynexus-collector`

Minimal runtime endpoints:

- `GET /actuator/health`
- `GET /api/collector/info`
- `POST /api/collector/internal/jobs/{jobCode}/run`

Core scheduler should decide when to run jobs. Collector should implement how Python-specific jobs run. Core records job metadata/history and calls Collector through HTTP via service discovery.

## Safe First Migration Step

First scaffold only a minimal Python collector service:

- Flask is lower-risk than FastAPI for the first bridge because the old backend is already Flask.
- Do not import old `app.py`, `backend/bootstrap.py`, or `backend/runner.py`.
- Do not auto-start old schedulers.
- Do not copy large model/data/upload directories.
- Add Docker Compose service, external mounts, Nacos registration, and Gateway route.
- Migrate one read-only capability first, such as collector info or local reference metadata.

## Notes From Inspection

- Old project Git status was clean during inspection.
- Current `cynexus-collector` directory was empty during inspection.
- The old `.env` file exists but was intentionally not read.
