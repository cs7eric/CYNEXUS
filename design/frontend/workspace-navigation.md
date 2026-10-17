# Workspace Navigation

## Scope

The existing Release backend is shared by two frontend navigation spaces. This is navigation organization; it does not change authorization, tenant identity, stored data, or the Agent tool registry.

| Space | Navigation |
| --- | --- |
| Personal / 个人空间 | Finance, media tools, investment research, intraday monitors |
| Development / 开发后台 | Dashboard, AI configuration, scheduler, data sources, providers, API workflows, WeChat events, POC |
| Shared | Home, Cynexus Agent, Knowledge Base, account/theme controls |

## Configuration

- `cynexus-ui/src/config/sidebar.ts` owns existing menu labels, URLs and children.
- `cynexus-ui/src/config/workspaces.ts` assigns each application to one navigation group and derives route ownership, including children outside their parent's prefix.
- Sidebar, home and command search use this ownership configuration. New menu entries must also be assigned to a workspace or the shared group.
- `pnpm test:workspaces` verifies menu coverage, all router paths, aliases, detail paths, shared routes and persisted-location validation.

## Navigation Contract

- The selector displays the active space with its icon; its dropdown marks the selected entry. Team creation and decorative shortcut labels are removed.
- Switching restores the destination space's last URL, including query and hash. First visits default to `/` for Personal and `/settings/scheduler` for Development.
- Preferences use `cynexus-workspaces-v1` browser storage. Invalid locations fall back to the matching default; unavailable storage does not prevent navigation.
- Exclusive routes determine their own space. Shared pages keep the selected space, stamped into browser history so Back and Forward restore the correct shell.
- Preferences are recorded only after navigation succeeds. A cancelled leave confirmation does not change the active space or remembered destination.
- Menu expansion is stored independently for each space and group. The mobile drawer closes after selecting a space; Home remains accessible from the sidebar.
- Command search shows current-space and shared pages. Development search does not request personal stock, portfolio or media history data.
- Knowledge writing and AI Prompt/Provider editors use `UnsavedChangesGuard` for changed drafts and pending saves. Provider routing edits also participate. Browser reload/close uses the native unload guard. Cancelling keeps the original page and draft.

## Runtime

This UI change is verified through the existing develop/HMR server on port 5174. It does not require restarting backend services or changing scheduler job states.
