# Food diary boundaries

The Food APIs use the gateway's signed tenant and principal assertion. The core
service verifies it with `CYNEXUS_TRUSTED_IDENTITY_SECRET`; missing/forged
assertions fail closed. Owner keys never come from the request body. All diary,
custom-food, favorite and goal reads/writes include this verified owner scope.

Autonomous Agent exposure is deliberately excluded for this initial feature.
The diary contains personal dietary/health history, and the Agent does not yet
have a Food-specific opt-in consent/governance contract or a delegated Food
identity API. Exposing it through the Finance default-owner pattern or forwarding
browser assertion headers from a model call would violate the owner boundary.
Catalog reads can become a separate bounded shared-reference tool later; they
are not a reason to grant access to the private diary. Mutations require a future
named preview/confirmation/audit executor; they must never be autonomous tools.

Catalog search returns at most 30 items (default 20, five-second statement
timeout). Private lists are bounded to 200 custom foods, 100 favorites and 200
goal versions. History is limited to 366 dates; individual days to 200 entries.
History returns persisted days only. An absent day reads as an empty open day
without creating a record. Completing an empty day explicitly persists it.

Per-100g nutrition and source restrictions are copied at entry creation. Regular
diary edits reuse those snapshots. Fridge-origin quantity edits restore the
entry's active lot allocations, consume the requested grams again in FEFO order,
and replace the entry snapshot with nutrition from the actual lots consumed, all
inside one transaction. Deleting a fridge-origin entry restores its active lot
allocations and soft-deletes those allocation links with the diary row. Null
nutrients remain unknown. Day targets are copied from the latest goal effective
on that date at day creation and never changed by later goal versions. Goal dates
are unique: select a new effective date for a new goal. All-null goals/custom
foods/quick records are rejected by the schema contract.

Development UI mode uses the Vite listener on port 5174 and the existing Release
backend. Food backend deployment affects only the Release core image; it does
not rebuild or recreate the Release UI.

Photo-created foods use `/food/recognition-tasks` only when a nutrition label
is attached and at least one required nutrient is missing. Fully entered
nutrition and food-photo-only drafts use direct custom-food creation. Both
the preview analysis and background task receive the original food photo
first (when present), then the original nutrition label; client image resizing
is allowed, background removal is never a recognition prerequisite. A pending
cutout preview does not block background submission.

The server validates the recognized name, measurement basis, confidence and
all four nutrients, preserves supplied manual values, and commits the custom
food and successful task together. The mobile observer wakes when a task is
submitted, polls until completion and announces `FOOD_CHANGED`; both All foods
and My foods reload their owner-scoped lists. Returning to the page recovers
tasks completed while it was closed. Invalid or failed recognition never adds
a food to either list. A same-name fridge result cannot hide a saved custom
food from My foods search.
