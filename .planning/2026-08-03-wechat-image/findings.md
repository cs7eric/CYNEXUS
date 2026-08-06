# Findings: WeChat Image Intake

## Current Flow

- `WechatCallbackController` receives signed GET/POST callbacks.
- `WechatXmlMessageParser` parses XML into `WechatInboundMessage`.
- `WechatInteractionDomainService` currently replies only to `subscribe` and `text` messages.
- `WechatOfficialAccountClient` already owns access-token retrieval and outbound WeChat HTTP calls.
- The current public callback supports plaintext XML only.

## Current Limitation

- Image callbacks contain `MsgType=image`, `PicUrl`, and `MediaId`, but those fields are not represented in the domain model or handled by the domain service.
- No image download/storage port exists.

## Impact Analysis

- `WechatInboundMessage` is used by the parser, domain reply service, and two focused tests.
- `WechatOfficialAccountClient` already centralizes access-token caching, so image download can reuse it through a dedicated domain token-provider port.

## Design Decision

- Use a dedicated `WechatImageGateway` and `WechatAccessTokenProvider` port rather than putting download/storage logic in the application service.
- Mount `/data/cynexus/wechat/images` to `deploy/release/wechat-images` so downloaded files survive container recreation.

## Scope

- Add image metadata to the inbound model and XML parser.
- Download the media using the existing WeChat access-token configuration and save it under a configurable directory.
- Return a short text acknowledgment to the user after a successful save.
