import assert from "node:assert/strict"
import test from "node:test"
import { hookRenderer, loadTsModule } from "./helpers/react-hooks.mjs"

const food = { id: "42", kind: "custom", name: "Oats", brand: "Grain Co", nutrients: { caloriesKcal: 380, carbsG: 64, proteinG: 12, fatG: 8 } }

function library(filter, query = "", inventory = []) {
  const renderer = hookRenderer(), listeners = new Map(), timers = new Map()
  let foods = [], timerId = 0
  const { useFoodSearch } = loadTsModule(new URL("../src/mobile/pages/food/hooks/use-food-data.ts", import.meta.url), {
    react: renderer.react,
    "@/services/food/api": {
      FOOD_CHANGED: "food-changed", fetchCustomFoods: async () => foods,
      fetchFavorites: async () => [], fetchCatalog: async () => [],
    },
    "@/services/fridge/api": { fetchFridgeInventory: async () => ({ items: inventory }) },
  }, {
    window: {
      addEventListener: (name, callback) => listeners.set(name, callback),
      removeEventListener: name => listeners.delete(name),
      setTimeout: callback => { const id = ++timerId; timers.set(id, callback); return id },
      clearTimeout: id => timers.delete(id),
    },
  })
  renderer.mount(() => useFoodSearch(query, filter))
  async function load() {
    for (const [id, callback] of timers) { timers.delete(id); callback() }
    await renderer.settle()
  }
  return {
    ...renderer, load,
    async complete() { foods = [food]; listeners.get("food-changed")(); await renderer.settle(); await load() },
  }
}

for (const filter of ["all", "mine"]) {
  test(`${filter} refreshes from the server when recognition announces a saved food`, async () => {
    const h = library(filter)
    await h.load()
    assert.equal(h.view().data.length, 0)
    await h.complete()
    assert.equal(h.view().data.length, 1)
    assert.equal(h.view().data[0].id, "42")
    assert.equal(h.view().loading, false)
    h.close()
  })
}

test("a same-name fridge result does not hide newly saved My foods", async () => {
  const h = library("mine", "Oats", [{ id: "stock", name: "Oats", quantity: 100, unit: "g", grams: 100, nutritionStatus: "unavailable" }])
  await h.load()
  await h.complete()
  assert.equal(h.view().data.length, 2)
  assert.equal(h.view().data[0].fridgeItemId, "stock")
  assert.equal(h.view().data[1].id, "42")
  h.close()
})
