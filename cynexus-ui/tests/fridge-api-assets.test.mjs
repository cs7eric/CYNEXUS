import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import test from "node:test"
import vm from "node:vm"
import ts from "typescript"

function loadTypeScript(path, dependencies = {}) {
  const module = { exports: {} }
  const source = readFileSync(new URL(path, import.meta.url), "utf8")
  const output = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 },
  }).outputText
  vm.runInNewContext(output, {
    module,
    exports: module.exports,
    require: (name) => dependencies[name],
    fetch: dependencies.fetch,
  })
  return module.exports
}

const api = loadTypeScript("../src/services/fridge/api.ts", {
  "@/services/common/cynexus-result": { cynexusResult: async (response) => response },
  "@/services/common/service-prefix": { SERVICE_PREFIX: "" },
})
const catalog = loadTypeScript("../src/mobile/pages/fridge/lib/catalog.ts")

test("fridge normalization keeps nutrition IDs separate from visual atlas keys", () => {
  const normalized = api.normalizeFridgeInventory({
    items: [{
      id: "oats",
      catalogFoodId: "48291",
      assetCatalogKey: "48291",
      name: "Oat",
      quantity: 1,
      unit: "pack",
      bestBefore: "2026-10-29",
      createdAt: "2026-10-21T00:00:00.000Z",
      updatedAt: "2026-10-21T00:00:00.000Z",
    }],
  })

  assert.equal(normalized.items[0].catalogId, undefined)
  assert.equal(normalized.items[0].assetCatalogKey, null)
  assert.equal(catalog.getFridgeFoodImageUrl(catalog.findFridgeFoodByName(normalized.items[0].name)), "/mobile/fridge/foods/food-201.webp")
})

test("fridge nutrition match preview sends a batch to the AI mapping endpoint", async () => {
  let capturedUrl
  let capturedInit
  const matchingApi = loadTypeScript("../src/services/fridge/api.ts", {
    "@/services/common/cynexus-result": { cynexusResult: async (response) => response },
    "@/services/common/service-prefix": { SERVICE_PREFIX: { core: "/api/core" } },
    fetch: async (url, init) => {
      capturedUrl = url
      capturedInit = init
      return { ok: true }
    },
  })

  await matchingApi.previewFridgeNutritionMatches([
    { name: "Chicken breast", brand: null },
    { name: "Rolled oats" },
  ])

  assert.equal(capturedUrl, "/api/core/food/fridge/ai/mapping-preview")
  assert.equal(capturedInit.method, "POST")
  assert.deepEqual(JSON.parse(capturedInit.body), {
    items: [{ name: "Chicken breast", brand: null }, { name: "Rolled oats" }],
  })
})
