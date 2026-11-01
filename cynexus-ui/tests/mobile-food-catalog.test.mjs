import assert from "node:assert/strict"
import test from "node:test"
import { elements, hookRenderer, loadTsModule } from "./helpers/react-hooks.mjs"

const food = { id: "42", kind: "custom", name: "Oats", nutrients: { caloriesKcal: 380, carbsG: 64, proteinG: 12, fatG: 8 } }
const createCardMock = () => Object.assign(() => null, {
  Header: "Card.Header",
  Title: "Card.Title",
  Description: "Card.Description",
  Content: "Card.Content",
})

function favoriteButton({ favoriteId = null, rejectSave = false } = {}) {
  const renderer = hookRenderer()
  const calls = { added: [], removed: [], changes: 0 }
  const jsx = (type, props) => ({ type, props })
  const { FoodLibraryFavoriteButton } = loadTsModule(new URL("../src/mobile/pages/food/components/food-catalog.tsx", import.meta.url), {
    react: renderer.react,
    "react/jsx-runtime": { jsx, jsxs: jsx },
    "@heroui/react": { Card: createCardMock() },
    "lucide-react": { ChevronRight: "ChevronRight", LoaderCircle: "LoaderCircle", Plus: "Plus", Search: "Search", Star: "Star" },
    "@/services/food/api": {
      addFavorite: async (...args) => {
        calls.added.push(args)
        if (rejectSave) throw new Error("Favorite service unavailable")
        return { ...food, favoriteId: "favorite-42" }
      },
      announceFoodChange: () => { calls.changes++ },
      deleteFavorite: async id => { calls.removed.push(id) },
    },
    "@/services/food/model": { formatNutrition: String },
    "../hooks/use-food-data": { useFoodSearch: () => ({ data: [], loading: false, error: null, reload() {} }) },
    "./food-art": { FoodArt: "FoodArt" },
    "./food-catalog.css": {},
  }, { crypto: { randomUUID: () => "request-key" }, Error })
  renderer.mount(() => FoodLibraryFavoriteButton({ food: { ...food, favoriteId } }))
  const button = () => elements(renderer.view()).find(element => element.type === "button")
  return { ...renderer, calls, button }
}

function foodCatalog({ fridgeItemId } = {}) {
  const renderer = hookRenderer()
  const selected = []
  const fridgeOpened = []
  const catalogFood = fridgeItemId ? { ...food, fridgeItemId, availableQuantity: 1900, availableUnit: "g" } : food
  const Card = createCardMock()
  const jsx = (type, props) => ({ type, props })
  const { FoodCatalog } = loadTsModule(new URL("../src/mobile/pages/food/components/food-catalog.tsx", import.meta.url), {
    react: renderer.react,
    "react/jsx-runtime": { jsx, jsxs: jsx },
    "@heroui/react": { Card },
    "lucide-react": { ChevronRight: "ChevronRight", LoaderCircle: "LoaderCircle", Plus: "Plus", Search: "Search", Star: "Star" },
    "@/services/food/api": { addFavorite: async () => ({}), announceFoodChange() {}, deleteFavorite: async () => {} },
    "@/services/food/model": { formatNutrition: value => value == null ? "—" : String(value) },
    "../hooks/use-food-data": { useFoodSearch: () => ({ data: [catalogFood], loading: false, error: null, reload() {} }) },
    "./food-art": { FoodArt: "FoodArt" },
    "./food-catalog.css": {},
  })
  renderer.mount(() => FoodCatalog({ onSelect: value => selected.push(value), onCustom() {}, onOpenFridge: () => fridgeOpened.push(true), compact: true }))
  return { ...renderer, Card, selected, fridgeOpened, catalogFood }
}

test("food library favorite control saves and removes a favorite", async () => {
  const control = favoriteButton()
  assert.equal(control.button().props["aria-pressed"], false)
  await control.button().props.onClick()
  await control.settle()
  assert.deepEqual(control.calls.added, [["custom", "42", "request-key"]])
  assert.equal(control.calls.changes, 1)
  assert.equal(control.button().props["aria-pressed"], true)

  await control.button().props.onClick()
  await control.settle()
  assert.deepEqual(control.calls.removed, ["favorite-42"])
  assert.equal(control.calls.changes, 2)
  assert.equal(control.button().props["aria-pressed"], false)
  control.close()
})

test("failed favorite save keeps the control unselected and reports the error", async () => {
  const control = favoriteButton({ rejectSave: true })
  await control.button().props.onClick()
  await control.settle()
  assert.equal(control.button().props["aria-pressed"], false)
  assert.equal(elements(control.view()).find(element => element.props?.role === "alert")?.props.children, "Favorite service unavailable")
  assert.equal(control.calls.changes, 0)
  control.close()
})

test("food choice and fridge inventory expose separate actions", () => {
  const catalog = foodCatalog()
  const card = elements(catalog.view()).find(element => element.type === catalog.Card)
  assert.equal(card.props.variant, "default")
  assert.equal(card.props.render({ className: "card" }).type, "div")
  const selectButton = elements(catalog.view()).find(element => element.props?.className === "food-catalog-card-select")
  assert.equal(selectButton.props["aria-label"], "Add Oats to today. 380 kilocalories; per 100 grams: carbs 64 grams, protein 12 grams, fat 8 grams.")
  selectButton.props.onClick()
  assert.deepEqual(catalog.selected, [food])
  catalog.close()
})

test("fridge stock action opens the fridge view without selecting the food", () => {
  const catalog = foodCatalog({ fridgeItemId: "fridge-42" })
  const stockButton = elements(catalog.view()).find(element => element.props?.className === "food-catalog-card-stock food-catalog-card-stock-action")
  let propagationStopped = false
  stockButton.props.onClick({ stopPropagation() { propagationStopped = true } })
  assert.equal(propagationStopped, true)
  assert.deepEqual(catalog.fridgeOpened, [true])
  assert.deepEqual(catalog.selected, [])
  catalog.close()
})
