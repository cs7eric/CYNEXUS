import { emptyNutrition, nutrientKeys } from "@/services/food/model"
import type { NutrientKey, Nutrients } from "@/services/food/types"

const MAX_SOURCE_IMAGE_BYTES = 12 * 1024 * 1024
const MAX_IMAGE_EDGE = 1800
const MAX_COMPRESSED_IMAGE_BYTES = 2.5 * 1024 * 1024

export interface NutritionLabelRecognition {
  recognized: boolean
  foodName: string | null
  brand: string | null
  basis: "per_100g" | "per_serving" | "unknown"
  servingSizeGrams: number | null
  nutrition: Nutrients
  confidence: "high" | "medium" | "low"
}

export interface NutritionLabelDraft {
  name: string
  brand: string
  values: Record<NutrientKey, string>
}

export type NutritionLabelDraftField = "name" | "brand" | NutrientKey

const IMAGE_FILENAME = /^(?:food-photo-cutout|nutrition-label|image|img)[-_]?\d*\.(?:jpe?g|png|webp)$/i
const TEST_MARKER_SUFFIX = /\s*[-_]?\s*(?:\(|\[|（|【)\s*(?:双图测试|测试数据|two[- ]?image(?:\s+test)?|test(?:\s+data)?)[^)]*?(?:\)|\]|）|】)\s*$/i

/** Keep model metadata out of editable product fields while preserving printed names. */
export function cleanRecognizedLabelText(value: unknown): string | null {
  if (typeof value !== "string") return null
  const normalized = value.replace(/\s+/g, " ").trim()
  if (!normalized || IMAGE_FILENAME.test(normalized)) return null
  const withoutPrefix = normalized.replace(/^(?:food\s*name|product\s*name|brand|品名|品牌)\s*[:：]\s*/i, "").trim()
  const cleaned = withoutPrefix.replace(TEST_MARKER_SUFFIX, "").trim()
  return cleaned && !IMAGE_FILENAME.test(cleaned) ? cleaned.slice(0, 200) : null
}

export function clearUneditedNutritionLabelDraft(
  current: NutritionLabelDraft,
  manuallyEdited: ReadonlySet<NutritionLabelDraftField>,
): NutritionLabelDraft {
  return {
    name: manuallyEdited.has("name") ? current.name : "",
    brand: manuallyEdited.has("brand") ? current.brand : "",
    values: Object.fromEntries(nutrientKeys.map(key => [
      key,
      manuallyEdited.has(key) ? current.values[key] : "",
    ])) as Record<NutrientKey, string>,
  }
}

function nullableNumber(value: unknown) {
  return value === null || typeof value === "number" && Number.isFinite(value) && value >= 0
}

function nullableString(value: unknown) {
  return value === null || typeof value === "string"
}

export function parseNutritionLabelRecognition(content: string): NutritionLabelRecognition {
  const value: unknown = JSON.parse(content)
  if (!value || typeof value !== "object") throw new Error("The recognition result was not valid JSON.")
  const candidate = value as Record<string, unknown>
  const nutrition = candidate.nutrition
  if (typeof candidate.recognized !== "boolean"
    || !nullableString(candidate.foodName) || !nullableString(candidate.brand)
    || !["per_100g", "per_serving", "unknown"].includes(String(candidate.basis))
    || !(candidate.servingSizeGrams === null || typeof candidate.servingSizeGrams === "number" && Number.isFinite(candidate.servingSizeGrams) && candidate.servingSizeGrams > 0)
    || !["high", "medium", "low"].includes(String(candidate.confidence))
    || !nutrition || typeof nutrition !== "object") {
    throw new Error("The recognition result is missing nutrition label fields.")
  }
  const values = nutrition as Record<string, unknown>
  if (nutrientKeys.some(key => !nullableNumber(values[key]))) {
    throw new Error("The recognition result contains invalid nutrition values.")
  }
  return {
    recognized: candidate.recognized,
    foodName: cleanRecognizedLabelText(candidate.foodName),
    brand: cleanRecognizedLabelText(candidate.brand),
    basis: candidate.basis as NutritionLabelRecognition["basis"],
    servingSizeGrams: candidate.servingSizeGrams as number | null,
    nutrition: Object.fromEntries(nutrientKeys.map(key => [key, values[key] as number | null])) as Nutrients,
    confidence: candidate.confidence as NutritionLabelRecognition["confidence"],
  }
}

export function nutritionLabelPer100g(label: NutritionLabelRecognition): Nutrients {
  if (!label.recognized || label.basis === "unknown"
    || label.basis === "per_serving" && label.servingSizeGrams == null) return emptyNutrition()
  const factor = label.basis === "per_serving" ? 100 / label.servingSizeGrams! : 1
  return Object.fromEntries(nutrientKeys.map(key => {
    const value = label.nutrition[key]
    const normalized = value == null ? null : value * factor
    return [key, normalized == null || !Number.isFinite(normalized) || normalized > 10_000_000
      ? null
      : Math.round(normalized * 100) / 100]
  })) as Nutrients
}

export function mergeNutritionLabelDraft(
  current: NutritionLabelDraft,
  label: NutritionLabelRecognition,
  manuallyEdited: ReadonlySet<NutritionLabelDraftField>,
): NutritionLabelDraft {
  const per100g = nutritionLabelPer100g(label)
  return {
    name: manuallyEdited.has("name") ? current.name : label.foodName?.trim() ?? "",
    brand: manuallyEdited.has("brand") ? current.brand : label.brand?.trim() ?? "",
    values: Object.fromEntries(nutrientKeys.map(key => [
      key,
      manuallyEdited.has(key) ? current.values[key] : per100g[key] == null ? "" : String(per100g[key]),
    ])) as Record<NutrientKey, string>,
  }
}

function blobAsDataUrl(blob: Blob) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onerror = () => reject(new Error("Unable to read the selected image."))
    reader.onload = () => typeof reader.result === "string" ? resolve(reader.result) : reject(new Error("Unable to read the selected image."))
    reader.readAsDataURL(blob)
  })
}

function canvasBlob(canvas: HTMLCanvasElement, quality: number) {
  return new Promise<Blob | null>(resolve => canvas.toBlob(resolve, "image/jpeg", quality))
}

async function prepareVisionPhotoImage(file: File) {
  if (!file.type.startsWith("image/")) throw new Error("Choose an image file.")
  if (file.size > MAX_SOURCE_IMAGE_BYTES) throw new Error("Choose an image smaller than 12 MB.")
  const bitmap = await createImageBitmap(file)
  try {
    const scale = Math.min(1, MAX_IMAGE_EDGE / Math.max(bitmap.width, bitmap.height))
    const canvas = document.createElement("canvas")
    canvas.width = Math.max(1, Math.round(bitmap.width * scale))
    canvas.height = Math.max(1, Math.round(bitmap.height * scale))
    const context = canvas.getContext("2d")
    if (!context) throw new Error("Unable to prepare the selected image.")
    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
    let blob: Blob | null = null
    for (const quality of [0.86, 0.76, 0.66, 0.56]) {
      blob = await canvasBlob(canvas, quality)
      if (blob && blob.size <= MAX_COMPRESSED_IMAGE_BYTES) break
    }
    if (!blob || blob.size > MAX_COMPRESSED_IMAGE_BYTES) throw new Error("This image is too detailed to upload. Choose a smaller image.")
    return { contentType: "image/jpeg", dataUrl: await blobAsDataUrl(blob) }
  } finally {
    bitmap.close()
  }
}

export const prepareNutritionLabelImage = prepareVisionPhotoImage
export const prepareFoodPhotoImage = prepareVisionPhotoImage
