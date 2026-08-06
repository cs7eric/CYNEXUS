# WeChat Official Account Integration

`cynexus-wechat` integrates a WeChat Official Account with Cynexus.

## Endpoints

- `GET /api/wechat/callback`: Official Account callback verification.
- `POST /api/wechat/callback`: Receives non-encrypted XML callbacks and replies to `subscribe` and `text` messages.
- Image callbacks (`MsgType=image`) are downloaded through the WeChat media API and stored under the configured image directory before returning an acknowledgment.
- `POST /api/wechat/messages/text`: Sends customer-service text messages to an explicit list of openids. The request is capped at 100 openids.
- `GET /api/wechat/history`: Lists paged inbound/outbound event history for the Settings UI. Supports `limit`, `offset`, `messageType`, and `processingStatus`.

Example push request:

```json
{
  "openIds": ["openid-example"],
  "content": "Cynexus notification"
}
```

Configure `cynexus.wechat.token`, `cynexus.wechat.app-id`, and `cynexus.wechat.app-secret` through Nacos or the corresponding `CYNEXUS_WECHAT_*` environment variables. Access tokens are cached in Redis under an app-id-scoped key.

Image intake uses these settings:

- `CYNEXUS_WECHAT_IMAGE_STORAGE_DIR`: local storage directory; Release defaults to `/data/cynexus/wechat/images` and is mounted to `deploy/release/wechat-images`.
- `CYNEXUS_WECHAT_IMAGE_MAX_BYTES`: maximum downloaded image size; defaults to 10 MiB.
- `CYNEXUS_WECHAT_IMAGE_REPLY`: callback acknowledgment text.

The saved file path is returned internally as a `WechatStoredImage`. A persisted worker invokes the `wechat_image_route` AI capability and records an extensible route code, confidence, reason, signals, and warnings. The initial route catalog contains `ACCOUNTING_RECEIPT`, `PORTFOLIO_TRADE`, and `UNKNOWN`; adding a route definition and updating the managed prompt extends the catalog without changing the callback contract.

Image routing is only the channel-level classification stage. A classified accounting or trade image must continue through the existing Finance or Portfolio parsing and QC/review flows. WeChat does not add a second task-number confirmation/cancellation protocol, because those existing AI flows already own QC and user confirmation.

The Settings page at `/settings/wechat-history` exposes the event audit trail with the raw WeChat user identifiers masked by the backend. This history is intentionally not exposed as an autonomous Agent tool: it contains external-channel user privacy data and operational audit information rather than a bounded business read model.

The initial callback slice deliberately supports plaintext XML only. WeChat message encryption (`encrypt_type=aes`) and follower persistence are separate follow-up capabilities.

External WeChat delivery is an externally visible side effect. It is not exposed as an autonomous Agent tool in this slice; callers must explicitly invoke the bounded push API and handle its per-openid failure list.
