# exchange_margin

Official SSE/SZSE margin trading source.

This provider calls public exchange endpoints directly and normalizes records into the existing margin table shape. `sync_margin` uses it before AkShare so AkShare remains a fallback wrapper instead of the primary source.
