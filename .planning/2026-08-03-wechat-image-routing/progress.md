# Progress: WeChat Image Routing and History

## Session Log

- Loaded Cynexus AI, backend, frontend, and planning guidance.
- Confirmed CodeGraph is present and up to date.
- Reviewed prior WeChat image-intake and integration plans; image storage is already implemented.
- Started impact inspection for the new classifier and Settings history page.
- Confirmed the classifier should run through the existing AI vision endpoint via a Feign contract, with `wechat_image_route` as the stable capability.
- Confirmed event history requires a durable WeChat table and a polling worker because the callback must return before the vision call completes.
- Added the Settings history service, route, sidebar entry, filters, pagination, and masked event detail view.
- Added message-id idempotency handling and Release changed-service detection for WeChat migrations.
- Maven WeChat/AI tests and package, UI TypeScript/build, targeted ESLint, PowerShell syntax, and version synchronization passed; full UI lint remains blocked by pre-existing repository errors outside this slice.
