import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import test from "node:test"
import vm from "node:vm"
import ts from "typescript"

function loadTypeScript(path, dependencies = {}) {
  const module = { exports: {} }
  const source = readFileSync(new URL(path, import.meta.url), "utf8")
  const output = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 } }).outputText
  vm.runInNewContext(output, { module, exports: module.exports, require: name => dependencies[name], fetch: dependencies.fetch })
  return module.exports
}

const food = loadTypeScript("../src/services/food/nutrition-label.ts", {
  "@/services/food/model": { emptyNutrition: () => ({ caloriesKcal: null, carbsG: null, proteinG: null, fatG: null }), nutrientKeys: ["caloriesKcal", "carbsG", "proteinG", "fatG"] },
})
let visionUrl
let visionInit
const vision = loadTypeScript("../src/services/ai/vision.ts", {
  "@/services/common/cynexus-result": { cynexusResult: async response => response },
  "@/services/common/service-prefix": { SERVICE_PREFIX: { ai: "/api/ai" } },
  fetch: async (url, init) => { visionUrl = url; visionInit = init; return { ok: true } },
})

const label = (overrides = {}) => ({
  recognized: true,
  foodName: "Oat bar",
  brand: "Example",
  basis: "per_serving",
  servingSizeGrams: 40,
  nutrition: { caloriesKcal: 160, carbsG: 24, proteinG: 5, fatG: 4 },
  confidence: "high",
  ...overrides,
})

test("per-serving label values normalize to the form's per-100-gram basis", () => {
  assert.deepEqual(JSON.parse(JSON.stringify(food.nutritionLabelPer100g(label()))), { caloriesKcal: 400, carbsG: 60, proteinG: 12.5, fatG: 10 })
})

test("unknown serving basis or serving weight never invents per-100-gram values", () => {
  assert.deepEqual(JSON.parse(JSON.stringify(food.nutritionLabelPer100g(label({ basis: "unknown" })))), { caloriesKcal: null, carbsG: null, proteinG: null, fatG: null })
  assert.deepEqual(JSON.parse(JSON.stringify(food.nutritionLabelPer100g(label({ servingSizeGrams: null })))), { caloriesKcal: null, carbsG: null, proteinG: null, fatG: null })
  assert.deepEqual(JSON.parse(JSON.stringify(food.nutritionLabelPer100g(label({ servingSizeGrams: 0.1, nutrition: { caloriesKcal: 10_000_000, carbsG: null, proteinG: null, fatG: null } })))), { caloriesKcal: null, carbsG: null, proteinG: null, fatG: null })
  assert.deepEqual(JSON.parse(JSON.stringify(food.nutritionLabelPer100g(label({ recognized: false })))), { caloriesKcal: null, carbsG: null, proteinG: null, fatG: null })
})

test("rescanning refreshes untouched AI values and preserves fields edited by the user", () => {
  const current = {
    name: "Oat bar",
    brand: "Example",
    values: { caloriesKcal: "400", carbsG: "60", proteinG: "12.5", fatG: "10" },
  }
  const next = food.mergeNutritionLabelDraft(current, label({
    foodName: "Fruit bar",
    brand: "New brand",
    basis: "per_100g",
    servingSizeGrams: null,
    nutrition: { caloriesKcal: 360, carbsG: 54, proteinG: 8, fatG: null },
  }), new Set(["brand", "proteinG"]))

  assert.deepEqual(JSON.parse(JSON.stringify(next)), {
    name: "Fruit bar",
    brand: "Example",
    values: { caloriesKcal: "360", carbsG: "54", proteinG: "12.5", fatG: "" },
  })

  const unreadable = food.mergeNutritionLabelDraft(next, label({
    recognized: false,
    foodName: null,
    brand: null,
    basis: "unknown",
    servingSizeGrams: null,
    nutrition: { caloriesKcal: null, carbsG: null, proteinG: null, fatG: null },
  }), new Set(["brand", "proteinG"]))
  assert.deepEqual(JSON.parse(JSON.stringify(unreadable)), {
    name: "",
    brand: "Example",
    values: { caloriesKcal: "", carbsG: "", proteinG: "12.5", fatG: "" },
  })
})

test("choosing another image clears old recognition values but keeps user edits", () => {
  const current = {
    name: "Oat bar",
    brand: "Example",
    values: { caloriesKcal: "400", carbsG: "60", proteinG: "12.5", fatG: "10" },
  }
  const cleared = food.clearUneditedNutritionLabelDraft(current, new Set(["brand", "proteinG"]))

  assert.deepEqual(JSON.parse(JSON.stringify(cleared)), {
    name: "",
    brand: "Example",
    values: { caloriesKcal: "", carbsG: "", proteinG: "12.5", fatG: "" },
  })
})

test("recognition output parsing rejects malformed model output", () => {
  assert.equal(food.parseNutritionLabelRecognition(JSON.stringify(label())).foodName, "Oat bar")
  assert.equal(food.parseNutritionLabelRecognition(JSON.stringify(label({ foodName: "蛋黄派（双图测试-17928244727642）", brand: "达利园" }))).foodName, "蛋黄派")
  assert.equal(food.parseNutritionLabelRecognition(JSON.stringify(label({ foodName: "food-photo-cutout.png" }))).foodName, null)
  assert.throws(() => food.parseNutritionLabelRecognition("not json"))
  assert.throws(() => food.parseNutritionLabelRecognition(JSON.stringify(label({ nutrition: { caloriesKcal: "160" } }))))
})

test("vision request preserves cutout-food-first and nutrition-label image order", async () => {
  await vision.executeVisionScenario("food_nutrition_label_parse", [
    { fileName: "food-photo-cutout.png", contentType: "image/png", dataUrl: "data:image/png;base64,AA==" },
    { fileName: "nutrition-label.jpg", contentType: "image/jpeg", dataUrl: "data:image/jpeg;base64,BB==" },
  ])
  assert.equal(visionUrl, "/api/ai/executions/vision")
  assert.equal(visionInit.method, "POST")
  assert.deepEqual(JSON.parse(visionInit.body), {
    scenarioCode: "food_nutrition_label_parse",
    images: [
      { fileName: "food-photo-cutout.png", contentType: "image/png", dataUrl: "data:image/png;base64,AA==" },
      { fileName: "nutrition-label.jpg", contentType: "image/jpeg", dataUrl: "data:image/jpeg;base64,BB==" },
    ],
  })
})
