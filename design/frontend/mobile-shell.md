# Mobile Shell

The mobile experience lives in the same React application but has its own route tree and shell. The first scaffold is available at `/m`, `/m/apps`, and `/m/me`; business pages remain desktop links until they are migrated.

The user's preferred visual direction and original reference set are recorded in [Mobile visual direction](mobile-visual-direction.md). Use that reference for future mobile visual work; this document describes the currently implemented shell.

## Boundaries

- `src/mobile/layout/` owns the mobile header, tab bar, scroll container, safe-area handling, and overlay portal.
- `src/mobile/pages/` owns mobile-only route pages.
- `src/mobile/components/` contains mobile compositions that link to desktop pages during migration.
- `src/mobile/ui/` is the thin project wrapper around HeroUI components. Use HeroUI only inside the mobile tree.
- `src/services/`, authentication, APIs, and business validation remain shared.

## Header

All mobile routes share a transparent floating header: an 80px row with a centered greeting and current English date, a 44px sky-blue workspace circle on the left and app search on the right. The header has no painted canvas and page content scrolls underneath it; the initial content offset keeps its controls clear. Switching Home, Apps or Profile changes only the central route content and active tab; it does not replace the header or its controls. Each page identifies its own content with a heading in the scrolling region. Profile provides the desktop-version link, and the not-found page provides Back to Home.

The workspace switcher opens an anchored 264px white popover with 28px corners and 60px rounded options. Circular icons, a pale-blue selected surface and a dark check indicator identify the active workspace; the trigger gets a subtle expanded-state ring. It retains the HeroUI menu's radio semantics, keyboard navigation, focus restoration and outside/Escape dismissal. The popover stays within narrow viewports and the mobile overlay portal.

Search uses a native modal dialog with keyboard focus, Escape dismissal, filtering of actual workspace apps and real destination links. Its styles and the locally hosted Manrope font load with the mobile layout, including direct visits to Apps and Profile before Home has loaded.

Mobile interface copy is English, including Home, Apps and Profile titles, navigation, workspace names, help text, tooltips, accessibility labels, authentication and confirmation dialogs. The mobile language attribute is `en`, and its React Aria locale provider uses `en-US` so component labels also remain English in non-English browsers. User content and business data retain their original language.

The desktop shell keeps using shadcn/Radix primitives. HeroUI styles are imported by `src/mobile/styles.css` and prefixed below `.mobile-theme`; this prevents component selectors and theme variables from changing desktop pages. Overlay components use the mobile portal so their visual scope remains inside the mobile theme.

## Bottom navigation

Home, Apps and Profile share the same near-black capsule with a white active pill and `aria-current="page"`. It has 20px side spacing (16px below 360px), a 380px maximum width, 56px tabs and bottom spacing equal to the larger of 12px or the device safe-area inset. Each tab places its icon beside its English label, and keyboard focus remains visible.

The header and tab bar persist outside the route outlet in the full-height mobile shell. The header remains above the scrolling content; the tab bar is an absolute transparent overlay anchored to the shell bottom. Its outer area passes pointer events through and has no painted background; only the dark capsule and active white pill are visible. The route container and scroll padding reserve clearance so the final content and focused controls remain reachable without being covered. Shared neutral colors, typography, search and navigation styles belong to `src/mobile/styles.css`; Home's colored cards and artwork remain in its page CSS. The shared content column is capped at 440px.

The mobile theme inherits the existing `html` / `body` / `#root` height chain. Ordinary Safari uses the small viewport (`100svh`), leaving Safari's top and bottom browser controls outside the app; an installed Home Screen app switches to the dynamic visual viewport and can use the full screen. The viewport allows device safe areas and content resizing when a supported browser opens the keyboard. Home-screen metadata uses the light interface's default status-bar treatment. Header, navigation and content retain safe-area insets. The transparent header overlays the scroll region while its top content offset prevents the first item from starting underneath its controls. Desktop height rules remain unchanged. A website cannot hide Safari's browser controls; standalone mode provides the app-like presentation. Browser viewport emulation does not verify an installed iOS home-screen app; device-specific viewport workarounds require evidence from that environment.

## Routing and state

Mobile routes are lazy loaded under `/m`. The route is explicit while the scaffold is being validated; automatic device-based redirects should wait until real mobile business workflows exist. The mobile shell can open the last desktop page for the active workspace. Mobile navigation does not overwrite those desktop destinations.

The personal/development workspace model is shared, but mobile workspace switching keeps the user on the current mobile route. A mobile page should preserve back behavior, scroll position, safe-area insets, keyboard-friendly controls, and a clear desktop fallback while its business page is not migrated.

## Activity calendar preview

Home's static week strip is replaced by `src/mobile/components/activity-calendar/`. The default is the selected date's Sunday–Saturday week. Month expands inline to the selected date's calendar month, aligned to whole weeks. Swipe left for the next month or right for the previous month; labeled arrow buttons provide the same navigation. Vertical scrolling, pinch zoom and ordinary date taps remain available; short, vertical and cancelled gestures do not change months, and a horizontal swipe does not select a date. A stable six-row area prevents the summary from jumping between months. Padding dates and future dates are disabled; historical dates are selectable even beyond the 30-day sample fixture. Browsing months preserves the selected date and its details. Selecting a day updates its food intake in kcal, meal count, workout duration in minutes and activity label. Collapsing keeps the selected day and its week; Today restores both the current month and date. Arrow keys can cross month boundaries, Home/End, a single available date tab stop, date/selection semantics and polite month/detail announcements support keyboard and screen-reader use.

Weekday labels sit in a separate aligned row above the compact 96px white capsules. Each capsule contains a clear date numeral and two centered food/workout rows with small icons; an 8px date-to-metric gap and 6px metric gap leave breathing room without lengthening the capsule. Missing values are muted, rest days remain explicit, and the selected date uses the shell's near-black surface with sky/mint values. The expanded calendar retains its compact date grid.

This is the user-requested visual prototype. Home passes explicitly labeled `Sample data` from a separate fixture module. `DailyActivity` accepts independent nullable nutrition/workout records: missing logs show an em dash and explanatory text; a recorded rest day shows zero minutes and `Rest day`. Daily nutrition targets are explicit sample values, separate from intake logs and stored per date so historical targets can differ. No target calculation, health advice, completion scores, persistence or backend calls are added. Autonomous Agent exposure is excluded at this stage because this preview provides no foundational real-data read or write operation. A future real-data integration must replace the fixtures and evaluate safe Agent access separately.

The selected day's Today/date details use a cool-blue Food intake card beside a mint Workout card. Food intake is slightly wider and contains consumed / target kcal, meal count and three compact `Carbs`, `Protein`, `Fat` rows. Small wheat, protein and droplet icons plus blue, mint and slate 3px intake bars distinguish the rows without separate tiles or a nutrition section. Each row shows consumed grams / daily target, with stronger type for intake. Workout shows duration, activity and logged sessions, retaining its own meaning. Bars cap at the goal while text preserves values above it; a missing intake or non-positive/missing goal leaves an empty track without implying a loading state. Values use grams with up to one decimal place and update with the selected date in either calendar view. Intake and targets are independently nullable: missing intake shows an em dash with `Not logged` assistive text, and missing goals show an em dash with `daily target not set`; available values remain visible. Macro values and goals belong only to daily details and are supplied explicitly by the labeled sample fixtures, rather than inferred from calories. The current sample goals are 2,200 kcal, 250 g carbs, 120 g protein and 80 g fat.

Date helpers use local calendar arithmetic at noon, avoiding UTC date parsing and 24-hour millisecond increments across DST boundaries. Focused tests cover full calendar months, month shifts from the 29th–31st, leap days, year boundaries, partial weeks, unavailable future days and DST timezones. Colors and rounded shapes extend the approved Home/Profile palette; the shared header, tabs and page scrolling remain stable.

## Apps

`/m/apps` uses page-local `apps.css` to match Home and Profile: a compact 20px title, current workspace and actual app count, then sky-blue/mint rounded shortcuts for the two shared apps. Workspace-specific groups are unboxed 68px rows with 23px outline icons, 15px labels, thin dividers and chevrons, using the same spacing and neutral palette as Profile. The old framed white cards and green icon tiles are no longer used by the directory.

All groups, app names, counts and destinations come from the existing navigation configuration. Switching workspace in the shared header updates the directory in place; each app link carries the current workspace to the desktop destination. The fixed greeting header and capsule navigation stay visible while the list scrolls. No usage metrics or new business functionality are implied.

## Profile

The third tab is Profile at the existing `/m/me` address, implemented in `src/mobile/pages/profile.tsx` with page-local CSS. It adapts the supplied fitness profile into Cynexus: configured username/email and an initials avatar, current workspace and owner access, then mint/sky/slate summaries with the actual Personal, Development and shared app counts from navigation configuration. These app counts describe available navigation entries, not usage or backend health. Summary links open the matching app directory and retain the selected workspace.

The lower half is an unboxed list with outline icons, supporting text, thin dividers and chevrons: Your apps, Cynexus Agent, Knowledge Base, Desktop workspace, and Sign-in & security. Desktop links restore the remembered destination. The settings gear opens and focuses the security disclosure; its two sign-out actions reuse the existing confirmation dialogs and authentication service. No profile-edit API, activity metrics, follower counts or session counts are fabricated.

## Migration rule

Move a high-frequency workflow one complete path at a time: list, detail, edit, save, error, and return. Put the page under the owning feature's `mobile/` folder once it has its own interactions. Extract API calls, calculations, and validation to shared services/hooks instead of copying them between desktop and mobile.
