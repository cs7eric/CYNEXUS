# scripts

Operator scripts.

Use this folder for one-shot setup, repair, backfill, and validation scripts. Reusable logic should live under `src/cynexus_data`.

## Social browser profiles

Prepare one authorized persistent browser profile per platform from the host
machine. The browser opens visibly so the operator can complete QR code,
password, or SMS login manually:

```powershell
cd E:\project-repo\cynexus\cynexus-data
python scripts\prepare_social_browser_profile.py --platform xiaohongshu
python scripts\prepare_social_browser_profile.py --platform douyin
```

Profiles are stored under `var\social-browser-profiles\<platform>\default`,
which is mounted into the Data Executor as `/app/var`. Do not commit or share
these directories: they contain browser login state.
