# TDX Hsjday Pipeline Design

`sync_tdx_hsjday` prepares local TDX daily `.day` files for later `daily_raw` backfill.

## Responsibilities

- Download `hsjday.zip` from TDX, unless `skip_download=true`.
- Use `download_method=auto` by default:
  - Try `urlopen` first.
  - If TDX returns a JavaScript challenge page, fall back to optional Playwright browser request.
  - Use browser request Range chunks to avoid loading the full zip into memory.
- Verify zip freshness using sample index files.
- Safely extract zip contents with zip-slip protection.
- Verify extracted `.day` root using sample files and target date.
- Replace the configured target root with rollback-on-failure behavior.

## Defaults

- URL: `https://data.tdx.com.cn/vipdoc/hsjday.zip`
- Download method: `auto`
- Work directory: `var/tdx/downloads/<date>`
- Target root: `var/tdx/hsjday`

The default target deliberately avoids overwriting a real TDX installation. Operators can pass `target_root` explicitly.

## Download Modes

`download_method=urlopen` forces the lightweight HTTP path and fails fast if TDX returns an anti-bot JavaScript page.

`download_method=playwright` forces the browser-backed path. It requires:

```powershell
pip install playwright
playwright install chromium
```

`download_method=auto` is the production default. It preserves the fast path when available and only uses Playwright after challenge detection.

## Common Commands

Dry run:

```powershell
python -m cynexus_data.jobs.sync_tdx_hsjday --dry-run --date 2026-06-26
```

Use an already-downloaded zip:

```powershell
python -m cynexus_data.jobs.sync_tdx_hsjday `
  --date 2026-06-26 `
  --skip-download true `
  --zip-path E:\data\tdx\hsjday.zip `
  --target-root E:\data\tdx\hsjday
```

Force browser download:

```powershell
python -m cynexus_data.jobs.sync_tdx_hsjday `
  --date 2026-06-26 `
  --download-method playwright `
  --playwright-headless true
```

Then backfill daily raw:

```powershell
python -m cynexus_data.jobs.backfill_history `
  --dataset kline_daily `
  --day-root E:\data\tdx\hsjday `
  --create-table true
```
