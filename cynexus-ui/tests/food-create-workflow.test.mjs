import assert from "node:assert/strict"
import test from "node:test"
import { elements, hookRenderer, loadTsModule } from "./helpers/react-hooks.mjs"

const nutrientKeys = ["caloriesKcal", "carbsG", "proteinG", "fatG"]
const complete = { caloriesKcal: "400", carbsG: "60", proteinG: "12", fatG: "10" }
const sourcePhoto = { name: "camera.jpg", type: "image/jpeg", size: 100 }
const labelPhoto = { name: "table.jpg", type: "image/jpeg", size: 100 }
const plain = value => JSON.parse(JSON.stringify(value))

function form({ pendingMatting = false } = {}) {
  const renderer = hookRenderer()
  const submissions = [], saved = [], queued = [], preparedPhotos = [], scanImages = [], mattingSignals = []
  let created = 0, changes = 0
  const model = { nutrientKeys, emptyNutrition: () => Object.fromEntries(nutrientKeys.map(key => [key, null])) }
  const nutrition = loadTsModule(new URL("../src/services/food/nutrition-label.ts", import.meta.url), { "@/services/food/model": model })
  const formLogic = loadTsModule(new URL("../src/mobile/pages/food/components/food-form.ts", import.meta.url), { "@/services/food/model": model })
  const jsx = (type, props) => ({ type, props })
  const { CustomFoodForm } = loadTsModule(new URL("../src/mobile/pages/food/components/food-entry.tsx", import.meta.url), {
    react: renderer.react,
    "react/jsx-runtime": { jsx, jsxs: jsx },
    "lucide-react": new Proxy({}, { get: () => () => null }),
    "@/services/food/model": model,
    "@/services/food/api": {
      fetchFoodAssets: async () => [], announceFoodChange: () => changes++,
      createCustomFood: async input => { created++; saved.push(input); return { id: "42", name: input.name } },
    },
    "@/services/food/matting": { mattingFoodImage: async (_file, signal) => {
      mattingSignals.push(signal)
      if (pendingMatting) await new Promise((_, reject) => signal.addEventListener("abort", () => reject(new DOMException("Aborted", "AbortError"))))
      return { blob: {}, dataUrl: "data:image/png;base64,CUTOUT" }
    } },
    "@/services/food/recognition": { submitFoodRecognition: async input => { submissions.push(input); return { id: "42", status: "queued" } } },
    "@/services/food/nutrition-label": {
      ...nutrition,
      prepareNutritionLabelImage: async file => ({ contentType: "image/jpeg", dataUrl: `data:image/jpeg;base64,${file === labelPhoto ? "LABEL" : "WRONG"}` }),
      prepareFoodPhotoImage: async file => { preparedPhotos.push(file); return { contentType: "image/jpeg", dataUrl: "data:image/jpeg;base64,ORIGINAL" } },
    },
    "@/services/ai/vision": {
      enqueueVisionScenario: async (_code, images) => { scanImages.push(images); return { id: "scan" } },
      fetchVisionTask: async () => ({ status: "succeeded", result: JSON.stringify({ recognized: true, foodName: "Oats", brand: null,
        basis: "per_100g", servingSizeGrams: null, confidence: "high", nutrition: { caloriesKcal: 400, carbsG: 60, proteinG: 12, fatG: 10 } }) }),
    },
    "./food-art": { FoodArt: "FoodArt" },
    "./food-form": formLogic,
    "./food-nutrient-fields": { NutrientFields: "NutrientFields" },
    "./scene-model": {}, "./food-entry.css": {},
  }, {
    crypto: { randomUUID: () => "request-key" },
    URL: { createObjectURL: () => "blob:preview", revokeObjectURL() {} },
  })
  renderer.mount(() => CustomFoodForm({ onSaved() {}, onQueued: value => queued.push(value), onBack() {} }))
  const nodes = () => elements(renderer.view())
  return {
    ...renderer, submissions, saved, queued, preparedPhotos, scanImages, mattingSignals,
    changes: () => changes, created: () => created,
    async photo(label, file) {
      nodes().find(node => node.props["aria-label"] === label).props.onChange({ target: { files: [file], value: "" } })
      await renderer.settle()
    },
    async details(values = complete) {
      nodes().find(node => node.type === "input" && node.props.maxLength === 200 && node.props.required != null).props.onChange({ target: { value: "My oats" } })
      for (const [key, value] of Object.entries(values)) nodes().find(node => node.type === "NutrientFields").props.onChange(key, value)
      await renderer.settle()
    },
    async submit() { await renderer.view().props.onSubmit({ preventDefault() {} }); await renderer.settle() },
    async cutout() { nodes().find(node => node.type === "button" && String(node.props.children).includes("Cut out food")).props.onClick(); await renderer.settle() },
    async scan() { nodes().find(node => node.type === "button" && String(node.props.children).includes("Analyze food + nutrition")).props.onClick(); await renderer.settle() },
    submitDisabled: () => nodes().find(node => node.props.type === "submit").props.disabled,
  }
}

test("only a nutrition label with missing values queues automatic food creation", async () => {
  const h = form()
  await h.photo("Take a food photo", sourcePhoto)
  await h.photo("Take a nutrition label photo", labelPhoto)
  await h.details({ proteinG: "0" })
  await h.submit()
  assert.equal(h.created(), 0)
  assert.equal(h.queued.length, 1)
  assert.equal(h.mattingSignals.length, 0)
  assert.equal(h.preparedPhotos[0], sourcePhoto)
  assert.equal(h.submissions[0].foodImage.dataUrl, "data:image/jpeg;base64,ORIGINAL")
  assert.equal(h.submissions[0].nutritionLabelImage.dataUrl, "data:image/jpeg;base64,LABEL")
  assert.deepEqual(plain(h.submissions[0].nutrients), { caloriesKcal: null, carbsG: null, proteinG: 0, fatG: null })
  h.close()
})

test("completed manual nutrition saves directly even with both photos attached", async () => {
  const h = form()
  await h.photo("Take a food photo", sourcePhoto)
  await h.photo("Take a nutrition label photo", labelPhoto)
  await h.details()
  await h.submit()
  assert.equal(h.submissions.length, 0)
  assert.equal(h.created(), 1)
  assert.equal(h.changes(), 1)
  assert.equal(h.saved[0].assetImageDataUrl, "data:image/png;base64,CUTOUT")
  h.close()
})

test("a nutrition-label-only draft can be queued without a name or food photo", async () => {
  const h = form()
  await h.photo("Take a nutrition label photo", labelPhoto)
  await h.submit()
  assert.equal(h.queued.length, 1)
  assert.equal(h.submissions[0].foodImage, undefined)
  assert.equal(h.submissions[0].name, undefined)
  assert.equal(h.mattingSignals.length, 0)
  h.close()
})

test("four manually entered zeros count as complete nutrition and skip AI", async () => {
  const h = form()
  await h.photo("Take a nutrition label photo", labelPhoto)
  await h.details(Object.fromEntries(nutrientKeys.map(key => [key, "0"])))
  await h.submit()
  assert.equal(h.submissions.length, 0)
  assert.equal(h.created(), 1)
  h.close()
})

test("a food photo without a nutrition label never creates a recognition task", async () => {
  const h = form()
  await h.photo("Take a food photo", sourcePhoto)
  await h.details({ proteinG: "12" })
  await h.submit()
  assert.equal(h.submissions.length, 0)
  assert.equal(h.created(), 1)
  h.close()
})

test("invalid manual nutrition never submits a task", async () => {
  const h = form()
  await h.photo("Take a nutrition label photo", labelPhoto)
  await h.details({ proteinG: "-1" })
  await h.submit()
  assert.equal(h.submissions.length, 0)
  assert.equal(h.created(), 0)
  h.close()
})

test("recognition can be submitted while matting is still pending", async () => {
  const h = form({ pendingMatting: true })
  await h.photo("Take a food photo", sourcePhoto)
  await h.photo("Take a nutrition label photo", labelPhoto)
  await h.cutout()
  assert.equal(h.mattingSignals.length, 1)
  assert.equal(h.submitDisabled(), false)
  await h.submit()
  assert.equal(h.queued.length, 1)
  assert.equal(h.submissions[0].foodImage.dataUrl, "data:image/jpeg;base64,ORIGINAL")
  h.close()
  assert.equal(h.mattingSignals[0].aborted, true)
  await h.settle()
})

test("preview analysis also starts immediately while matting is pending", async () => {
  const h = form({ pendingMatting: true })
  await h.photo("Take a food photo", sourcePhoto)
  await h.photo("Take a nutrition label photo", labelPhoto)
  await h.cutout()
  await h.scan()
  assert.equal(h.scanImages.length, 1)
  assert.equal(h.mattingSignals[0].aborted, false)
  assert.equal(h.scanImages[0][0].dataUrl, "data:image/jpeg;base64,ORIGINAL")
  h.close()
  await h.settle()
})

test("both analysis and automatic creation use originals after a cutout is available", async () => {
  const h = form()
  await h.photo("Take a food photo", sourcePhoto)
  await h.photo("Take a nutrition label photo", labelPhoto)
  await h.cutout()
  await h.submit()
  await h.scan()
  assert.equal(h.submissions[0].foodImage.dataUrl, "data:image/jpeg;base64,ORIGINAL")
  assert.deepEqual(plain(h.scanImages[0].map(image => image.dataUrl)), ["data:image/jpeg;base64,ORIGINAL", "data:image/jpeg;base64,LABEL"])
  assert.ok(h.preparedPhotos.every(file => file === sourcePhoto))
  h.close()
})
