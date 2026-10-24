# Food Fridge Integration

The fridge inventory is owner scoped and lot based. `food_fridge_item` stores the
mapping state, `food_fridge_stock_lot` stores each expiry batch, and
`food_fridge_stock_event` is the append-only quantity ledger. The inventory view
exposes bounded visual stack fields; those fields are presentation data and are
never written back as stock quantities.

Adding an item first tries an exact catalog/custom Food match. Ambiguous or
missing matches remain `pending`/`unmatched` and are returned as candidates for
user confirmation. Nutrients are copied to the lot as a per-100 g snapshot and
are calculated only when an explicit unit-to-gram conversion exists. Volume
units require an explicit density; count and package units require a measured
grams-per-unit value before nutrition can be calculated.

The fridge write endpoints are intentionally not exposed as autonomous Agent
write tools. They change user-owned inventory and can consume food, so they
require the authenticated UI request and its idempotency key. Meal-originated
consumption can include an existing `mealItemId`; the transaction writes the
stock event and `food_meal_item_fridge_lot` allocation together. A future Agent
integration may expose a bounded read-only inventory query and a preview-only
consumption proposal that requires explicit confirmation.

Editing the quantity of a fridge-origin diary item restores its active lot
allocations and consumes the new gram amount in FEFO order in the same transaction
as the diary update. Deleting the diary item restores those exact lot quantities
and soft-deletes the active allocation links; stock events remain in the ledger.
Food and fridge writes share the same owner-scoped advisory lock.

## Food artwork mapping

`food_asset_mapping` is shared presentation metadata used by the mobile fridge
and is read through `GET /api/core/food/assets`. It is intentionally excluded
from autonomous Agent tools: changing or querying a file-to-image mapping does
not represent a user-recognizable food operation, and the binary artwork stays
in the UI release assets.

`food_asset_catalog_mapping` is the canonical link between nutrition records
and artwork. One `catalog_key` can be shared by many source-specific
`food_nutrition_catalog` rows, while each nutrition row has at most one linked
asset. The original `food_asset_mapping.catalog_food_id` column is retained as
a legacy input only. Migration backfill links exact normalized matches across
catalog names and aliases only when they resolve to one distinct active asset;
ambiguous matches remain unlinked. A confirmed fridge selection may create or
update the association with source `fridge_confirmation`, so fridge and Log
Food views can use the same image for that nutrition record.
