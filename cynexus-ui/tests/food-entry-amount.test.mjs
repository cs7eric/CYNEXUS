import assert from "node:assert/strict"
import test from "node:test"
import { loadTsModule } from "./helpers/react-hooks.mjs"

const {
  availableFoodAmount,
  foodAmountInGrams,
  foodAmountUnits,
  fridgeMealQuantityAndUnit,
} = loadTsModule(new URL("../src/mobile/pages/food/components/food-entry-amount.ts", import.meta.url), {})

const fridgeFood = (overrides = {}) => ({
  id: "food",
  kind: "catalog",
  name: "Food",
  brand: null,
  nutrients: { caloriesKcal: 100, carbsG: 10, proteinG: 10, fatG: 2 },
  fridgeItemId: "stock",
  availableQuantity: 1000,
  availableUnit: "g",
  availableGrams: 1000,
  ...overrides,
})

test("keeps gram stock in the original unit", () => {
  const food = fridgeFood()
  assert.deepEqual(Array.from(foodAmountUnits(food)), ["g"])
  assert.equal(availableFoodAmount(food, "g"), 1000)
  const mealAmount = fridgeMealQuantityAndUnit(food, 250, "g")
  assert.equal(mealAmount?.quantity, 250)
  assert.equal(mealAmount?.unit, "g")
})

test("supports measured millilitres without guessing density", () => {
  const food = fridgeFood({ availableQuantity: 500, availableUnit: "ml", availableGrams: 450 })
  assert.deepEqual(Array.from(foodAmountUnits(food)), ["g", "ml"])
  assert.equal(foodAmountInGrams(food, 100, "ml"), 90)
  const mealAmount = fridgeMealQuantityAndUnit(food, 100, "ml")
  assert.equal(mealAmount?.quantity, 100)
  assert.equal(mealAmount?.unit, "ml")
})

test("converts litres to grams for the meal API", () => {
  const food = fridgeFood({ availableQuantity: 2, availableUnit: "L", availableGrams: 2000 })
  assert.equal(availableFoodAmount(food, "ml"), 2000)
  assert.equal(foodAmountInGrams(food, 250, "ml"), 250)
  const mealAmount = fridgeMealQuantityAndUnit(food, 250, "ml")
  assert.equal(mealAmount?.quantity, 250)
  assert.equal(mealAmount?.unit, "g")
})

test("supports pieces when a measured piece weight is present", () => {
  const food = fridgeFood({ availableQuantity: 10, availableUnit: "pcs", availableGrams: 1200 })
  assert.deepEqual(Array.from(foodAmountUnits(food)), ["g", "pcs"])
  assert.equal(availableFoodAmount(food, "pcs"), 10)
  assert.equal(foodAmountInGrams(food, 2, "pcs"), 240)
  const mealAmount = fridgeMealQuantityAndUnit(food, 2, "pcs")
  assert.equal(mealAmount?.quantity, 2)
  assert.equal(mealAmount?.unit, "pcs")
  assert.equal(mealAmount?.gramsPerUnit, undefined)
})

test("supports entering a custom gram weight for each piece", () => {
  const food = fridgeFood({ availableQuantity: 10, availableUnit: "pcs", availableGrams: null })
  assert.deepEqual(Array.from(foodAmountUnits(food)), ["pcs"])
  assert.equal(availableFoodAmount(food, "pcs"), 10)
  assert.equal(foodAmountInGrams(food, 2, "pcs", 200), 400)
  const mealAmount = fridgeMealQuantityAndUnit(food, 2, "pcs", 200)
  assert.equal(mealAmount?.quantity, 2)
  assert.equal(mealAmount?.unit, "pcs")
  assert.equal(mealAmount?.gramsPerUnit, 200)
})

test("does not calculate piece nutrition without a piece weight", () => {
  const food = fridgeFood({ availableQuantity: 10, availableUnit: "pcs", availableGrams: null })
  assert.equal(foodAmountInGrams(food, 2, "pcs"), null)
  const mealAmount = fridgeMealQuantityAndUnit(food, 2, "pcs")
  assert.equal(mealAmount?.quantity, 2)
  assert.equal(mealAmount?.unit, "pcs")
  assert.equal(mealAmount?.gramsPerUnit, undefined)
})

test("does not expose millilitres for litre stock when density is unknown", () => {
  const food = fridgeFood({ availableQuantity: 2, availableUnit: "L", availableGrams: null })
  assert.deepEqual(Array.from(foodAmountUnits(food)), [])
  assert.equal(availableFoodAmount(food, "ml"), null)
  assert.equal(fridgeMealQuantityAndUnit(food, 100, "ml"), null)
})
