# Findings: OpenCLI Social Collection Integration

## OpenCLI

- Package: `@jackwener/opencli`, version `1.8.6` via `npx --yes`.
- Xiaohongshu commands include `user`, `note`, and `comments`.
- `user` accepts a user id or profile URL and returns note rows including `id`, `title`, `type`, `likes`, and `url`.
- `note` and `comments` require the full Xiaohongshu URL with `xsec_token`.
- Browser-backed commands require Chrome with the OpenCLI Browser Bridge extension connected.
- Local `whoami` currently fails with `BROWSER_CONNECT`; no data collection was attempted.

## Existing Cynexus path

- Local agent: `cynexus-data/src/cynexus_data/interfaces/local_agent/app.py`, port `18183`.
- Current browser client uses Playwright persistent profiles under `var/social-browser-profiles`.
- Previous Xiaohongshu runs were invalid because the platform redirected/blockaded the Playwright page and returned `300012 IP存在风险`; the 30 discovered rows must not be treated as valid.
- Existing jobs are `social_creator_discovery` and `social_comment_sync`, with Postgres storage and idempotent upserts.

## Integration implication

- The OpenCLI path must run on the user's machine, not inside the Release/Docker executor, because it depends on the local Chrome session and Browser Bridge.
- The existing UI can continue calling `127.0.0.1:18183`; the local agent should dispatch OpenCLI for social jobs.
- The adapter must validate URLs and preserve `xsec_token` exactly; reconstructing a note URL from only an id is invalid for Xiaohongshu.

## Package source inspection

- OpenCLI JSON output is a raw array of rows on stdout; errors are rendered as YAML on stderr.
- XHS `user` builds note URLs containing `xsec_token`, but its rows do not expose publication time.
- XHS `comments` returns rank/author/userId/profileUrl/text/likes/time/is_reply/reply_to, but no stable comment id; the Cynexus adapter must derive a deterministic key from the row content.
- Douyin exposes `user-videos`, not a standalone comments command. `user-videos` can include `top_comments`; its output has `aweme_id`, title, counts, and comments but no publication time. The adapter must retain `sec_uid` and the raw top comments for later synchronization.
- OpenCLI supports a global `--profile <name>` option. The local agent should pass a non-default configured profile alias, while leaving the default unqualified to use the active Browser Bridge profile.
