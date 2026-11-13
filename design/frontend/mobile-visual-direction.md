# Mobile visual direction

The user supplied and approved the four references below on 2026-10-17: “吸收消化一下这组 UI style，我喜欢这个.” Treat this as the preferred direction for future Cynexus mobile design. The request establishes a visual preference; individual screen changes remain separate implementation tasks. Interface copy stays English.

## Reference set

| Reference | What to learn |
| --- | --- |
| [Bicycle controls, light and dark](references/mobile-style/bicycle-controls.jpg) | Quiet surfaces, circular controls, large object imagery, one vivid active region, rounded control groups. |
| [Terrain and social screens](references/mobile-style/terrain-and-social.jpg) | Strong headline hierarchy, image-led compositions, capsule actions, layered color blocks and a dark bottom navigation anchor. |
| [Activity and profile screens](references/mobile-style/activity-and-profile.jpg) | Colored feature cards beside calm list views, pill-shaped date selectors, dark navigation with an inverted active item. |
| [Full style board](references/mobile-style/style-board.jpg) | The wider range: vivid color, oversized editorial type, playful object imagery, asymmetric layouts and a confident black/white foundation. |
| [Profile close-up](references/mobile-style/profile-detail.png) | Circular identity, compact metadata, three pastel summaries and flat divided navigation rows; adapted to Cynexus workspace and security features. |

The user subsequently selected the “Daily challenge / Your plan” Home in `activity-and-profile.jpg` and explicitly requested a close reproduction. The latest global mobile canvas preference is white, with white content surfaces, dark text, neutral black-and-white shared controls and restrained mint-green accents on feature content. The current Home keeps the useful activity calendar and plan surfaces while omitting the decorative Daily challenge feature card. Example people, schedules, watermarks and device mockups are not product requirements; destinations and workspace information come from Cynexus.

## Shared visual language

The recurring language is bold, rounded and tactile: strong black/white contrast, large geometric shapes, clear typography and a small number of vivid surfaces. Some screens are quiet and monochrome; others are expressive. Their common identity comes from shape, scale and hierarchy rather than one fixed color palette.

- **Geometry:** circles for compact actions, capsules for switching and navigation, generous rounded rectangles for substantial content. Rounded shapes must remain visibly circular or pill-shaped rather than slightly softened boxes.
- **Contrast:** near-black and white establish the structure. Inverted controls make active states immediately legible. Neutral canvas areas give bright content room to stand out.
- **Color:** lemon yellow, yellow-green, lavender, sky blue, orange and pink appear as deliberate solid blocks. A screen can use one dominant accent, or a few colors assigned to different content groups. The reference set does not select yellow as the mandatory global brand color.
- **Typography:** geometric sans-serif with a deliberate, restrained size scale. Page titles stay compact on task-focused screens; large editorial type is reserved for a feature-led Home surface. Supporting text is quieter; key numbers stay clear and readable.
- **Composition:** one primary feature or subject leads the screen, followed by useful secondary actions. Vary card proportions where content warrants it. A profile/settings page can remain a simple divided list rather than becoming a wall of cards.
- **Imagery:** bicycles, terrain, avatars and sculptural 3D objects give specific content a focal point. Image cropping and occasional overlap create depth. Use imagery that belongs to the feature rather than adding generic decoration to every panel.
- **Depth:** matte surfaces, tonal separation and restrained shadows. Photo overlays and glass effects belong to specific image treatments; most controls and panels are solid.

## Translation into Cynexus mobile

| Surface | Preferred treatment |
| --- | --- |
| App shell | Light neutral canvas, ample edge spacing, compact header and clear content hierarchy. Dark surfaces are an optional coherent variation. |
| Header controls | Consistent 44–48px circles, simple outline icons, clear labels for assistive technology. |
| Bottom navigation | A detached capsule with clearly labeled Home, Apps, Finance, Workout and Profile destinations; the active item uses a high-contrast pill. |
| Home | A meaningful feature or current task leads, with a short headline and one deliberate accent surface. Additional app entries support that hierarchy. |
| Apps | Sky-blue/mint shared-app shortcuts with Home's rounded surfaces, followed by Profile-style unboxed grouped rows, outline icons and thin dividers. Keep real workspace context and app counts visible. |
| Profile | Configured identity, workspace/access metadata, three pastel app-count summaries and flat divided action rows based on the supplied profile reference. |
| Data and research | Carry over shape, spacing and hierarchy while preserving precise figures, units, freshness and market semantics. Decorative color must not compete with financial direction/status colors. |

### Knowledge Base

The mobile Knowledge Base uses the shared white canvas with black as its sole emphasis color. Use black for selected filters, active document states and primary actions, with neutral gray for metadata and separators. Do not use the Home / Apps mint accent on Knowledge Base surfaces.

### Mobile typography scale

Use the existing mobile components, Lucide icon system and shared services. Use this type scale in CSS pixels; keep sizes fixed across viewport widths rather than scaling text with `vw` or `clamp()`:

| Role | Size / line height | Use |
| --- | --- | --- |
| Page title | 24px / 30px | One screen-level heading; use 28px only for a feature-led Home headline. |
| Section heading | 16px / 22px | A content section or major card title. |
| Item title | 14px / 20px | Food names, list rows and compact card titles. |
| Body text | 14px / 20px | Main content and entered form values. |
| Supporting text | 12px / 17px | Descriptions, placeholders and secondary labels. |
| Compact control label | 12px / 16px | Buttons, tabs and segmented filters. |
| Dense toolbar label | 10px / 14px | A short search placeholder or compact action/filter label when the toolbar is crowded. Do not use this for body copy. |
| Metadata | 10px / 14px | Counts, timestamps and nonessential eyebrow labels. |
| Key metric | 20–24px / 26–30px | A small number of decision-relevant values, not every number on the screen. |

Keep control hit areas at least 44px even when their labels are compact. Selected controls keep the same font size as unselected controls; indicate selection with surface, color or weight. Validate long labels and the 320px viewport before settling horizontal padding. For editable inputs in iOS Safari, use 16px text when preventing focus zoom is more important than matching the compact toolbar scale.

For large feature panels, keep the existing 24–32px corner range and use fully rounded compact controls. These geometry values are implementation starting points inferred from the references, not measurements of the source designs.

The shared mobile shell uses the reference's greeting/search header, detached white navigation capsule with a high-contrast charcoal active pill, white canvas and Manrope typography. These layout features remain stable while switching tabs and scrolling, with the Agent conversation using the focused header and navigation treatment below. Home uses the week capsules, cool-blue asymmetric cards and mint quick links.

### Agent

The mobile Agent uses a white canvas, Manrope and charcoal as its emphasis color. Its shared header contains only Back on the left and History on the right, with no greeting, date or app search. The bottom navigation capsule and `+` stay hidden throughout the page. History opens a dedicated bottom sheet without workspace tabs, with a search bar, New chat in the header, a highlighted current conversation and readable divided rows with dates and confirmed archiving. Activity, draft confirmation, scheduled tasks and available tools remain in the page toolbar's separate workspace sheet; black identifies the active section.

A compact conversation toolbar leads to a quiet welcome state with a dark Agent mark, a two-line prompt and three flat suggestion rows. Messages use a light-gray user bubble and unboxed Agent responses, with the shared Web read-only Markdown renderer used continuously during streaming and after completion. The outlined, rounded composer has 24px of clearance plus the device safe-area inset from the visible viewport's bottom edge. Provider and Model HeroUI selects sit directly inside the message composer, following the Web layout; their widths adapt to narrow screens and long selected labels truncate. All inputs use at least 16px, with no automatic focus on entry or panel changes. The sheets and composer follow the visible viewport when the keyboard opens.

## Implemented Apps

`/m/apps` follows the approved [Apps HTML prototype](prototypes/apps/mobile.html). It retains the shared header, Manrope font and detached navigation. Shared Agent / Knowledge Base shortcuts use 116px blue/mint cards with 24px corners and circular arrow affordances. Workout / Food sit side by side in a single 62px unboxed row. Workspace groups use 54px divided rows with bare Lucide icons, 14px item labels and quiet counts. Finance and Knowledge Base open their mobile routes; workspace ownership and application counts still come from the shared navigation configuration. Search and workspace switching remain in the shared header. The shell owns scrolling and bottom-navigation clearance.

## Implemented Home

`/m` keeps its content colors and artwork layout in `src/mobile/pages/home.css`; the header, tab bar, font and search load from the shared mobile layout and stylesheet. Your plan contains the selected workspace's first app, Knowledge Base, and Agent / Apps / Profile shortcuts.

The Activity calendar extends the original date capsules into a nutrition/fitness UI preview. It defaults to a week and expands inline to a full calendar month, with horizontal swipes and labeled arrow controls for previous/next months. Weekly capsules show compact food intake and exercise duration; the expanded calendar uses two category markers, with exact values in the selected day's cool-blue/mint summary pair. Month browsing preserves the selected day's details, Today restores the current month/date, and future dates remain disabled. The current day and selected date remain distinct, dates use the local calendar, and all preview values are visibly labeled `Sample data`. The fixtures are isolated from the data-driven component so future integration can replace them without changing its layout. No live health records or editable schedule are implied.

The selected day's summary stays a two-card composition. The slightly wider cool-blue Food intake card integrates three compact `Carbs`, `Protein` and `Fat` rows beneath calories and meal count. Each row has a small outline icon, consumed / daily target grams and a thin blue, mint or slate progress bar. The adjacent mint Workout card shows duration, activity and logged sessions. Both cards use closely related tinted surfaces, a shared corner radius and restrained shadow so they sit naturally on the full-page Home gradient. There is no separate nutrition section or row of macro tiles. Goals are explicit sample data per date. The daily capsules retain their existing calorie and workout content.

The shared navigation capsule floats above the scrolling content. Its outer area is fully transparent and passes interaction through; the dark capsule and active white pill are the only visible navigation surfaces. Bottom scroll clearance keeps final content reachable, and device safe areas remain clear.

Safari uses the browser's small viewport while its address and toolbar controls are visible. The installed Home Screen app uses the dynamic visual viewport for an app-like full-screen shell. Safari browser controls remain system UI and are not hidden by website code.

The supplied Profile close-up adds a specific second page reference: preserve its round avatar, compact identity, mint/sky/slate summary trio and unboxed two-line rows. Translate fitness and social content into real Cynexus workspace/app information and existing actions. The shared greeting header and bottom capsule remain the persistent mobile skeleton.

The mobile layout uses locally hosted Manrope for interface copy and Outfit for the Home display heading, both under their included SIL Open Font Licenses (`public/fonts/manrope/OFL.txt` and `public/fonts/outfit/OFL.txt`). The Knowledge Base card uses a decorative WebP sculpture in `public/mobile/home/`, cropped transparently from the user-supplied `activity-and-profile.jpg`. Controls and typography remain HTML/CSS; the page is not a rasterized mockup.

## Guardrails

- Keep interface copy English and retain useful text labels; an icon-only reference does not justify hiding the meaning of unfamiliar actions.
- Preserve at least 44px touch targets, device safe areas, visible keyboard focus, sufficient contrast and reduced-motion behavior.
- Use bold color to identify content or selection. Avoid applying every reference color to every screen.
- Keep the application task-focused: genuine content supplies the headline, data and imagery.
- Large graphics, theatrical typography, stacked cards and floating stickers from the more expressive board are optional accents for suitable surfaces, not a template for research or settings pages.
- Scope this direction to the mobile experience. The existing dense desktop workbench retains its own visual guidance.
