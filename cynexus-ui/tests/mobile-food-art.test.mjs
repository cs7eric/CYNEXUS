import assert from "node:assert/strict"
import test from "node:test"
import { loadTsModule } from "./helpers/react-hooks.mjs"

function renderFoodArt(food) {
  const jsx = (type, props) => ({ type, props })
  const { FoodArt } = loadTsModule(new URL("../src/mobile/pages/food/components/food-art.tsx", import.meta.url), {
    react: { useState: value => [value, () => {}] },
    "react/jsx-runtime": { jsx, jsxs: jsx },
    "lucide-react": { CookingPot: "CookingPot" },
  })
  return FoodArt({ food })
}

test("only custom uploaded photos receive the optical centering class", () => {
  const uploaded = renderFoodArt({ id: "42", foodId: "42", kind: "custom", entryKind: "custom", assetUrl: "/mapped/food-42.webp", name: "Custard pie" })
  assert.equal(uploaded.props.src, "/api/core/food/custom-foods/42/asset")
  assert.match(uploaded.props.className, /food-art-user-asset/)

  const builtIn = renderFoodArt({ id: "oats", foodId: "201", kind: "catalog", assetKey: "food-201", assetUrl: "/mobile/fridge/foods/food-201.webp", name: "Oats" })
  assert.equal(builtIn.props.src, "/mobile/fridge/foods/food-201.webp")
  assert.doesNotMatch(builtIn.props.className, /food-art-user-asset/)

  const mappedAsset = renderFoodArt({ id: "mapped", kind: "catalog", assetUrl: "/mapped/oats.webp", name: "Oats" })
  assert.equal(mappedAsset.props.src, "/mapped/oats.webp")
  assert.doesNotMatch(mappedAsset.props.className, /food-art-user-asset/)

  const builtInByKey = renderFoodArt({ id: "oats", kind: "catalog", name: "Oats" })
  assert.equal(builtInByKey.props.src, "/mobile/fridge/foods/food-201.webp")
  assert.doesNotMatch(builtInByKey.props.className, /food-art-user-asset/)
})
