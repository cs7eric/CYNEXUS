import assert from "node:assert/strict"
import test from "node:test"
import { elements, hookRenderer, loadTsModule } from "./helpers/react-hooks.mjs"

const model = loadTsModule(new URL("../src/services/fridge/model.ts", import.meta.url), {})
const jsx = (type, props) => ({ type, props })
const textContent = node => Array.isArray(node)
  ? node.map(textContent).join("")
  : node && typeof node === "object"
    ? textContent(node.props?.children)
    : node == null || typeof node === "boolean" ? "" : String(node)

const stock = overrides => ({
  id: "stock-1", name: "Pork tenderloin", quantity: 1, unit: "kg", grams: 1000,
  bestBefore: "2026-12-31", createdAt: "2026-11-10T00:00:00Z", updatedAt: "2026-11-10T00:00:00Z",
  mappingStatus: "matched", nutrition: { caloriesKcal: 1200, carbsG: 20, proteinG: 200, fatG: 35 },
  ...overrides,
})

async function stockSheet(item = stock()) {
  const page = hookRenderer(), sheet = hookRenderer()
  let activeRenderer = page
  const react = Object.fromEntries(Object.keys(page.react).map(name => [name, (...args) => activeRenderer.react[name](...args)]))
  const noOp = () => {}
  const records = { version: 1, isSample: false, items: [item], operations: [] }
  const { default: MobileFridgePage } = loadTsModule(new URL("../src/mobile/pages/fridge/index.tsx", import.meta.url), {
    react,
    "react/jsx-runtime": { jsx, jsxs: jsx },
    "react-dom": { createPortal: node => node },
    "lucide-react": new Proxy({}, { get: (_, name) => name }),
    "@/mobile/hooks/use-fridge-records": { useFridgeRecords: () => ({ data: records, refresh: noOp, update: noOp, createRemote: noOp, adjustRemote: noOp, confirmMappingRemote: noOp }) },
    "@/mobile/hooks/use-mobile-portal": { useMobilePortal: () => null },
    "@/services/food/api": { fetchFoodAssets: async () => [] },
    "@/services/fridge/api": { previewFridgeInventory: noOp, previewFridgeNutritionMatches: noOp, searchFridgeMappings: noOp },
    "@/services/fridge/model": model,
    "./lib/catalog": { fridgeFoodCatalog: [], findFridgeFoodByName: () => null, getFridgeFoodAssetUrl: () => null, getFridgeFoodImageUrl: () => "", setFridgeFoodAssets: noOp },
    "./fridge.css": {},
  }, { window: { addEventListener: noOp, removeEventListener: noOp } })
  page.mount(() => MobileFridgePage({ initialView: "inside", initialSelectedFoodId: item.id }))
  await page.settle()
  const stockEditor = elements(page.view()).find(element => typeof element.type === "function" && element.props.item === item && typeof element.props.onLogMeal === "function")
  assert.ok(stockEditor, "the selected fridge item opens its stock editor")
  page.close()
  activeRenderer = sheet
  const logged = []
  sheet.mount(() => stockEditor.type({ ...stockEditor.props, onLogMeal: selection => logged.push(selection) }))
  const find = predicate => elements(sheet.view()).find(predicate)
  const unitButton = unit => find(element => element.type === "button" && element.props["data-unit"] === unit)
  const input = label => find(element => element.type === "input" && element.props["aria-label"] === label)
  const submit = () => find(element => element.type === "button" && textContent(element) === "Log as today's meal")
  const preview = () => find(element => element.props.className === "portion-preview")
  return {
    ...sheet, logged, find, unitButton, input, submit, preview,
    async select(unit) { unitButton(unit).props.onClick(); await sheet.settle() },
    async enter(label, value) { input(label).props.onChange({ target: { value: String(value) } }); await sheet.settle() },
  }
}

test("stock nutrition is normalized per 100 g and the selected serving uses its gram weight", async () => {
  const sheet = await stockSheet()
  const reference = sheet.find(element => element.props["aria-label"] === "Nutrition per 100 grams")
  const calories = elements(reference).find(element => element.type === "span" && textContent(element).startsWith("Calories"))
  assert.equal(textContent(calories), "Calories120 kcal")
  assert.equal(sheet.input("Meal amount in g").props.value, 100)
  assert.match(textContent(sheet.preview()), /^120 kcal/)
  assert.equal(sheet.submit().props.disabled, false)
  sheet.close()
})

test("piece weight drives nutrition and preserves the piece selection on submit", async () => {
  const sheet = await stockSheet()
  await sheet.select("pcs")
  assert.equal(sheet.unitButton("pcs").props["aria-pressed"], true)
  assert.equal(sheet.submit().props.disabled, true)
  await sheet.enter("Weight of one piece in grams", 200)
  await sheet.enter("Meal amount in pcs", 2)
  assert.match(textContent(sheet.preview()), /^480 kcal/)
  assert.equal(sheet.submit().props.disabled, false)
  sheet.submit().props.onClick()
  assert.equal(sheet.logged.length, 1)
  assert.equal(sheet.logged[0].quantity, 2)
  assert.equal(sheet.logged[0].unit, "pcs")
  assert.equal(sheet.logged[0].gramsPerUnit, 200)
  sheet.close()
})

test("millilitre servings require a measured density before they can be logged", async () => {
  const sheet = await stockSheet()
  await sheet.select("ml")
  assert.equal(sheet.submit().props.disabled, true)
  assert.match(textContent(sheet.preview()), /^— kcal/)
  await sheet.enter("Weight of one millilitre in grams", 1.05)
  await sheet.enter("Meal amount in ml", 200)
  assert.equal(sheet.submit().props.disabled, false)
  assert.match(textContent(sheet.preview()), /^252 kcal/)
  sheet.submit().props.onClick()
  assert.equal(sheet.logged[0].quantity, 200)
  assert.equal(sheet.logged[0].unit, "ml")
  assert.equal(sheet.logged[0].gramsPerMl, 1.05)
  sheet.close()
})

test("servings cannot exceed the remaining stock in any selected unit", async () => {
  const sheet = await stockSheet()
  await sheet.enter("Meal amount in g", 1001)
  assert.equal(sheet.submit().props.disabled, true)
  await sheet.select("pcs")
  await sheet.enter("Weight of one piece in grams", 200)
  await sheet.enter("Meal amount in pcs", 6)
  assert.equal(sheet.submit().props.disabled, true)
  await sheet.enter("Meal amount in pcs", 5)
  assert.equal(sheet.submit().props.disabled, false)
  sheet.close()
})

test("all three unit choices stay visible when the stock has no measured gram weight", async () => {
  const sheet = await stockSheet(stock({ quantity: 15, unit: "pcs", grams: null }))
  assert.ok(sheet.unitButton("g"))
  assert.ok(sheet.unitButton("ml"))
  assert.ok(sheet.unitButton("pcs"))
  assert.equal(sheet.unitButton("g").props.disabled, true)
  assert.equal(sheet.unitButton("ml").props.disabled, true)
  assert.equal(sheet.unitButton("pcs").props.disabled, false)
  assert.equal(sheet.submit().props.disabled, true)
  sheet.close()
})
