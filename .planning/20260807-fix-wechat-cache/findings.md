# Findings

- `cynexus-release-wechat` fails during Spring context initialization because `CynexusCacheAutoConfiguration.redisJsonBundleCacheReader` requires `com.fasterxml.jackson.databind.ObjectMapper`, but no bean exists.
- The repository uses Spring Boot `4.0.7` and Jackson 3 (`tools.jackson.databind`) elsewhere, while `cynexus-common-cache` still uses Jackson 2 (`com.fasterxml.jackson.databind` and `com.fasterxml.jackson.core:jackson-databind`).
- `CynexusWechatApplication` explicitly imports `CynexusCacheAutoConfiguration`, making the mismatch fatal for WeChat startup.
- Current container restart policy is `unless-stopped`; it has been looping rather than staying healthy.
- The worktree contains many unrelated user changes. Only the shared cache files and this task's planning files may be changed.
