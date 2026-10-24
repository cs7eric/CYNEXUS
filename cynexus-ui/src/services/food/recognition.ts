import { cynexusResult } from "@/services/common/cynexus-result"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"
import type { AiVisionImageInput, AiVisionTaskSummary } from "@/services/ai/vision"
import type { Nutrients } from "./types"

const API = `${SERVICE_PREFIX.core}/food/recognition-tasks`
const TASK_STATUSES = new Set(["queued", "running", "succeeded", "failed"])

export interface FoodRecognitionInput {
  idempotencyKey: string
  nutritionLabelImage: AiVisionImageInput
  foodImage?: AiVisionImageInput
  name?: string
  brand?: string
  assetKey?: string
  nutrients?: Nutrients
}

export interface FoodRecognitionTask {
  id: string
  processId: string
  status: "queued" | "running" | "succeeded" | "failed"
  accepted?: boolean
  pollUrl?: string
  aiTaskId?: string | null
  foodId?: string | null
  foodName?: string | null
  errorMessage?: string | null
  retryable?: boolean
  attemptCount?: number
  createdAt?: string | null
  updatedAt?: string | null
  completedAt?: string | null
}

export function isFoodRecognitionVisionTask(task: AiVisionTaskSummary) {
  return /^food-recognition:\d+:\d+$/.test(task.idempotencyKey ?? "")
}

function task(value: FoodRecognitionTask): FoodRecognitionTask {
  if (!value || typeof value !== "object" || typeof value.status !== "string"
    || !TASK_STATUSES.has(value.status.toLowerCase())) {
    throw new Error("The food recognition task returned an invalid status.")
  }
  const id = String(value.id ?? value.processId ?? "")
  if (!/^\d+$/.test(id)) throw new Error("The food recognition task returned an invalid ID.")
  return { ...value, id, processId: id, status: value.status.toLowerCase() as FoodRecognitionTask["status"] }
}

export async function submitFoodRecognition(input: FoodRecognitionInput) {
  return task(await cynexusResult<FoodRecognitionTask>(await fetch(API, {
    method: "POST",
    cache: "no-store",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  })))
}

export async function listFoodRecognitionTasks(limit = 100) {
  const result = await cynexusResult<FoodRecognitionTask[]>(await fetch(`${API}?limit=${limit}`, { cache: "no-store" }))
  if (!Array.isArray(result)) throw new Error("Unable to load food recognition tasks.")
  return result.map(task)
}

export async function retryFoodRecognitionTask(id: string) {
  return task(await cynexusResult<FoodRecognitionTask>(await fetch(`${API}/${encodeURIComponent(id)}/retry`, {
    method: "POST",
    cache: "no-store",
  })))
}
