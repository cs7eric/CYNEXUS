import { SERVICE_PREFIX } from "@/services/common/service-prefix"

const MAX_IMAGE_BYTES = 8 * 1024 * 1024

function blobToDataUrl(blob: Blob) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onerror = () => reject(new Error("Unable to read the cutout image."))
    reader.onload = () => typeof reader.result === "string"
      ? resolve(reader.result)
      : reject(new Error("Unable to read the cutout image."))
    reader.readAsDataURL(blob)
  })
}

/** Sends a camera photo through the shared core proxy and returns the transparent food PNG. */
export async function mattingFoodImage(file: File, signal?: AbortSignal) {
  if (!file.type.startsWith("image/")) throw new Error("Choose an image file.")
  if (file.size > 12 * 1024 * 1024) throw new Error("Choose an image smaller than 12 MB.")
  const body = new FormData()
  body.append("file", file, file.name || "food-photo.jpg")
  const response = await fetch(`${SERVICE_PREFIX.core}/food/matting`, {
    method: "POST",
    cache: "no-store",
    signal,
    body,
  })
  const contentType = response.headers.get("content-type")?.split(";", 1)[0].trim().toLocaleLowerCase()
  if (!response.ok || contentType !== "image/png") {
    const bodyText = await response.text()
    let message = bodyText.replace(/<[^>]*>/g, " ").replace(/\s+/g, " ").trim()
    if (contentType === "application/json") {
      try {
        const parsed = JSON.parse(bodyText) as { message?: unknown }
        if (typeof parsed.message === "string" && parsed.message.trim()) message = parsed.message.trim()
      } catch { /* Fall back to the compact response text. */ }
    }
    throw new Error(message
      ? `${message}${response.ok ? "" : ` (HTTP ${response.status})`}`
      : `Unable to extract the food image${response.ok ? "" : ` (HTTP ${response.status})`}.`)
  }
  const blob = await response.blob()
  if (blob.size === 0 || blob.size > MAX_IMAGE_BYTES) throw new Error("The extracted image is too large. Try a closer photo.")
  return { blob, dataUrl: await blobToDataUrl(blob) }
}
