# Mobile Food — UI proposal

Status: proposed, pending product review. This document does not register a route
or implement a working diet app. All illustrated values are sample data.

Interactive visual exploration: [open the self-contained HTML concept](prototypes/food/food-concept.html).
It explores sage accents, a ceramic bowl with an intake orbit, and working sample
interactions across Today, Foods, History and Goals. This prototype is separate
from the application and does not access the database.

## Product intent

Make daily food logging quick, while keeping calorie/macronutrient intake and
targets visible together. The first screen answers: what is recorded for this
date, how much has been consumed, and what can be added next?

Follow the current [mobile shell](mobile-shell.md) and
[mobile direction](mobile-visual-direction.md), using the implemented frosted
white capsule and pale-blue selection. Database semantics come from the
[Life diary contract](../../sql/cynexus_life/README.md).

## App entry and shell

- Proposed route: `/m/food`. Register Food in Apps and shared app search; make
  Home's existing food summary open the corresponding Food diary date.
- Keep the global Home-area navigation at its current size. Food is discoverable
  through Apps and Home rather than adding a sixth item to that capsule.
- Reuse the common greeting header, centered greeting/date, left Back to Home,
  right app search and the shared light scroll-triggered frost.
- Inside Food, use four app tabs: `Today`, `Foods`, `History`, `Goals`. Keep the
  same 64px capsule and 64px detached `+` as the other apps.
- In Food, `+` directly opens the logging sheet, following Finance's contextual
  add behavior. The sheet includes `Search food` and `Quick add` modes. The
  Home-area quick-action menu retains its existing behavior.
- Remember the selected diary date between Food tabs. Adding from History uses
  the selected day; adding from Foods/Goals uses the remembered day, defaulting
  to today. Always show date and meal explicitly before saving.

## Visual treatment

- Reuse the full-page pale-blue/mint background. Use existing mobile sky/mint
  tokens for small accents and progress; white/near-white for readable lists.
- Use existing Manrope for interface text and numbers. Use the existing Outfit
  face only if a display heading is needed; the diary needs no large hero.
- Use 20px page edges, 24px summary corners, 12–16px section spacing and the
  shell's 440px content cap. Keep meaningful actions at least 44px.
- One summary surface carries calories and all three macros. Use flat meal
  groups with dividers below it. Give each meal an outline icon and a clear
  label; individual food rows need no decorative thumbnail or colored tile.
- Use sky for carbohydrate, muted mint for protein and blue-gray for fat,
  consistently across summary, entry preview and History. Labels and values
  remain legible without depending on color.
- Summary height should be approximately 180–200px at 390px width; the initial
  viewport should also show the beginning of the meal list. Validate 320px text
  fit during implementation without shrinking touch targets.

## Today

Order: compact week selector → integrated daily summary → meal groups → diary
completion action. The week selector supports previous/next weeks and a clear
return to today. Tapping the date opens a calendar sheet for older dates;
there is no expanded month grid permanently occupying the diary screen.

```text
        shared greeting header

  Mon   Tue   Wed   Thu   Fri   Sat   Sun
  [19]   20    21    22    23    24    25

  ╭────────────────────────────────────╮
  │ Today                     19 Oct   │
  │ 1,450 / 2,000 kcal                 │
  │ ━━━━━━━━━━━━━━━━━━━━━────────      │
  │ 550 kcal remaining                 │
  │                                    │
  │ Carbs       Protein       Fat      │
  │ 160/240 g   90/125 g      50/60 g   │
  │ ━━━━━──     ━━━━━──       ━━━━━━─  │
  ╰────────────────────────────────────╯

  Breakfast              380 kcal  (+)
    Oats                  60 g · 228
    Milk                250 ml · 152
  ────────────────────────────────────
  Lunch                  620 kcal  (+)
    Chicken & rice bowl    1 bowl
  ────────────────────────────────────
  Dinner                 450 kcal  (+)
    ...
  ────────────────────────────────────
  Snacks                 Not logged (+)

             Complete day

  [ Today   Foods   History   Goals ] (+)
```

The milk and bowl amounts above represent foods with explicit measured
conversions, not generic ml-to-g or bowl-to-g assumptions.

For a past date the summary heading becomes that date, with a `Today` return
control; it must not label yesterday's records Today. Meal sections group real
meal instances and allow repeated snacks, with optional times distinguishing
separate occasions. Empty sections stay compact; populated sections show the
food name, original amount and kcal, with details available on tap.

Tapping an item opens an editor for date/meal, amount, notes and explicit
deletion. A row's `+` preselects its meal. Saving updates the selected date and
Home's aggregate consistently. Support multiple items in the logging session
without repeatedly dismissing and reopening it.

Keep `Open` / `Complete` as compact diary states. `Complete day` explicitly
finishes the date, with a confirmation for an empty zero-intake day; a completed
day can be reopened. If a completed day is edited, ask to reopen before changing
it rather than silently keeping a stale completion state.

## Logging sheet

Use a shared-style bottom sheet that expands to near full height for search and
the keyboard. Keep date/meal context visible and the final action reachable.

1. Show `Log food`, selected diary date and meal selector at the top.
2. `Search food` mode offers a search field, `Favorites` and `My foods` filters.
   With an empty query show favorites and a short history-derived recent list;
   search results are bounded/paginated, with debounced input.
3. Each result shows name, brand when available and kcal per 100 g. Keep dataset
   identity in a quiet secondary line so same-name source records stay distinct.
   Source details disclose estimated/partial/sample data without adding a wall
   of technical badges to every row.
4. Selecting a food replaces results with its amount editor: numeric quantity,
   grams or an explicitly configured food portion, computed gram equivalent and
   a compact preview of actual kcal/Carbs/Protein/Fat. Missing conversion offers
   gram entry instead of making a weight assumption.
5. `Add to lunch` (or the selected meal) saves the selection. Offer `Add another`
   within the sheet and `Done` to return to the selected diary date. A retry
   after a save failure must preserve the draft and avoid duplicate intake.

`Quick add` records actual kcal and optional macro totals, with an optional
description. Blank macros remain unknown. Make the input basis explicit:
`For this entry`, while food definitions use `Per 100 g`. Optional descriptive
amounts in Quick add do not rescale the entered totals.

Create a custom food from Foods or an unsuccessful search. Keep the form small:
name, optional brand, kcal/macros per 100 g and optional source notes; additional
portion conversions can be configured afterward. Do not require a photo.

## Foods

One search entry with `All foods / Favorites / My foods` filters. Search rows
share the logging sheet's name/brand/source/per-100g layout. A favorite star is
a secondary action; tapping the row opens details with `Log food` and, for
owner-created foods, `Edit food`.

Food details contain a per-100g nutrient table, source details and available
portions. Portion editing states both quantity/unit and its measured grams.
Editing a definition explains that previously logged meals keep their saved
values. A historical meal remains editable through its own snapshot editor.

## History

Use `Week / Month / Year` with period arrows and a return-to-current-period
control. A compact nutrient switch selects `Energy / Carbs / Protein / Fat`;
show one primary chart at a time so four metrics do not compete on a phone.

- Week: daily bars with each day's saved target marker when present.
- Month: a calendar with a light intake/progress indicator and missing-record
  state, followed by the selected date's summary and meals.
- Year: monthly average intake per logged day, with logged-day and complete-day
  coverage stated beside each bucket; tapping opens that month.

Selecting a chart day opens that date's diary. Summaries label whether averages
cover logged days or complete known days. Never silently divide a partial month
by every calendar day or draw absent records as zero-intake days. Unknown
nutrients remain gaps, with explicit coverage and known-value information.

## Goals

Show current daily kcal, Carbs, Protein and Fat targets together, then a quiet
dated history list. Editing uses numeric inputs and an effective-from date.
Support manually set or unset individual targets without inferring targets
from body measurements or subtracting workout calories automatically.

Explain that new targets apply to newly created diary days from the selected
date; existing days retain their saved targets. A correction to an existing
day's target is a separate explicit action in that day's detail view.

## Data states and first delivery

- No records: `Nothing logged yet` and a direct logging action. Do not present
  this as a completed zero-intake day.
- Missing nutrients: show known subtotals with `Partial`; complete total,
  remaining and progress for an incomplete nutrient stay unavailable.
- Missing target: show `Target not set` with `Set goal`, while keeping known
  intake visible. Over-target text preserves the exact excess; bars cap at
  their visual endpoint without turning the whole screen into an alarm.
- Loading keeps summary/list geometry; search, summary and save errors offer
  local retries. Saving keeps the draft, action size and selected date stable.
- Prototype fixtures must say `Sample data`. Live UI must distinguish saved
  server data from unavailable data; these database tables do not yet have an API.

Implement a complete Today → search/quick entry → save/edit → updated summary
path first, then Foods, History and Goals against authenticated owner-scoped
services. Future app code belongs in `src/mobile/pages/food/` with private
`components/`, `lib/` and page styles; shared API/model code belongs in services.
Reuse the mobile overlay portal and controls, scroll/safe-area handling and
focus restoration. Review Agent reads and confirmed writes when adding APIs.

## Diary-driven food scene prototype

The next visual direction replaces the fixed decorative bowl inside the intake
summary with a scene driven by the selected date's actual diary items. The user
maintains transparent food artwork separately. Reuse Finance's asset-manifest,
stable-placement and bounded-rendering approach, adapted to food identity and
explicit portions. This is now implemented in the standalone HTML prototype;
production integration and the artwork maintenance screen remain future work.

### Composition

Upgrade the existing intake surface rather than creating another large summary
block. The prototype uses a compact title/energy row, an open tabletop scene,
and three macro progress rows along the bottom. The scene expands slightly for
six visible groups to keep food distinct. Use the app's sky/mint canvas, soft contact
shadows and sparse, near-invisible plate/tray forms to anchor the food cutouts.

Add a compact, horizontally scrollable scene filter: `All day`, `Breakfast`,
`Lunch`, `Dinner`, `Snacks`. A meal filter changes both the scene and its intake
subtotal; label that subtotal explicitly. Daily goals remain daily goals and
must not be presented as a meal-specific allowance. Keep the day-level summary
and meal subtotal distinct in the composition. For a first version the daily
summary can remain fixed, with the filtered scene's subtotal shown in its caption.

Use a small number of fixed, responsive anchor slots. The center is reserved for
the main dish/bowl, low foreground for smaller foods, and a rear/side slot for
drinks. Stable keys preserve positions when the date or quantity changes. A
gentle transform/opacity transition introduces a new entry; existing foods do
not randomly rearrange. Prefer at most six visible food groups, with
`+N more foods` opening the full list. No continuous animation or physics.

### Meaning and interaction

- A scene object represents a recorded food, with exact name/amount available
  on tap. Illustration size is composed for legibility, not proportional to
  calories, grams, dietary merit or recommended intake.
- In `All day`, repeated records may form one visual group only when their
  food identity, preparation and snapshot basis are compatible. Show the
  accumulated known gram/count total, and list the original meals on selection.
  Never group solely because different foods share the same artwork.
- An explicitly recorded count, such as two eggs, may render two pieces or an
  `×2` label. Gram-only logging must not invent an egg count. Large counts use a
  bounded number of pictures plus a count label. Show fractional count text
  accurately rather than pretending it is a whole piece.
- Food-specific reference grams are display metadata, not nutrition data.
  Discrete small/regular/large variants may be used only with an explicit
  mapping and caption; never infer physical volume or density from grams or
  calorie totals. Default to one representative image and an actual-amount label.
- A mixed dish stays one recorded dish. Do not invent ingredient quantities or
  show salmon/avocado/rice as separate consumed foods unless recorded that way.
- Tapping a visible object highlights it and its corresponding list group,
  revealing name, actual amount, kcal and macros in a compact detail tray. A
  single record offers `Edit amount`; a grouped object first lists its source
  meal entries so the user can choose which one to edit.
- A scene's `+` uses the normal logging sheet. Saving changes the scene, numeric
  summary and food list together. Missing artwork uses a neutral dish/cup
  placeholder with the food name; unknown nutrition remains unknown.
- Meal filters and every selectable food need visible keyboard focus, accessible
  names and a usable list equivalent. Reduced motion removes entry transitions.

### Maintained artwork and mappings

Keep the visual asset library independent of the shared nutrient catalog. A
small manifest maintained with the artwork can define:

| Property | Purpose |
| --- | --- |
| `asset_key` | Stable identity such as `egg-boiled` or `rice-cooked-bowl` |
| `file` / `thumbnail` | Transparent full image and optional lightweight preview |
| `view_family` | One compatible perspective per scene, e.g. a common oblique view |
| `display_scale` / `anchor` / `bounds` | Consistent visual size, contact point and transparent-edge crop |
| `layout_group` | Bowl/dish, piece, fruit, drink or generic, for slot selection |
| `reference_portion` | Optional known unit/count/grams for illustrative size variants; never a nutrient source |
| `attribution` | Source/license information when applicable |

Associate food identities with `asset_key` through explicit mappings, optionally
with a manually confirmed category fallback. Different nutrient-source records
can share artwork; image choice does not merge those records or overwrite their
nutrition. Do not guess visual matches from names during diary rendering.

A future small asset maintenance screen should show the transparent image,
its scene preview, perspective/scale, linked foods and an unmatched-food queue.
Replacing an image updates current visual rendering; old calorie/macronutrient
snapshots remain unchanged. Asset metadata is not yet an implemented database
contract; add a dedicated migration only when that maintenance feature is scoped.

The HTML uses the existing salmon bowl plus seven matching sample SVG cutouts;
no user-maintained artwork from the other session was found in the inspected
local directories. The explicit `sceneArtwork` mapping can accept those files
when available. `All day` and meal filters, selectable cutouts, original-entry
editing, six-group bounded rendering and the overflow list are implemented.
Composition was checked with one, five, six and eight groups before any asset
maintenance flow or backend integration.
