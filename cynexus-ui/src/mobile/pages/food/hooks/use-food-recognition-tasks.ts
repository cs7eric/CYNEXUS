import { useCallback, useEffect, useRef } from "react"
import { announceFoodChange } from "@/services/food/api"
import { listFoodRecognitionTasks, type FoodRecognitionTask } from "@/services/food/recognition"

/** Processing and saving are server-owned; this hook only refreshes visible food data. */
export function useFoodRecognitionTasks(onMessage: (message: string) => void) {
  const known = useRef(new Map<string, FoodRecognitionTask["status"]>())
  const messageRef = useRef(onMessage)
  const wakeRef = useRef<(() => void) | null>(null)
  useEffect(() => { messageRef.current = onMessage }, [onMessage])

  const add = useCallback((task: FoodRecognitionTask) => {
    known.current.set(task.id, task.status)
    if (task.status === "succeeded") announceFoodChange()
    wakeRef.current?.()
  }, [])

  useEffect(() => {
    let disposed = false
    let refreshing = false
    let refreshAgain = false
    let timer: number | undefined
    async function refresh() {
      if (refreshing || disposed) return
      refreshing = true
      let active = false
      try {
        const tasks = await listFoodRecognitionTasks()
        if (disposed) return
        let changed = false
        for (const task of tasks) {
          const previous = known.current.get(task.id)
          if (task.status === "queued" || task.status === "running") active = true
          if (previous !== task.status) {
            if (task.status === "succeeded") {
              changed = true
              if (previous) messageRef.current(`${task.foodName || "Your food"} was added to My foods.`)
            } else if (previous && task.status === "failed") {
              messageRef.current(task.errorMessage || "Nutrition analysis failed. The food was not added.")
            }
          }
          known.current.set(task.id, task.status)
        }
        if (changed) announceFoodChange()
      } catch { /* A later poll or page visit recovers from a network interruption. */ }
      finally {
        refreshing = false
        window.clearTimeout(timer)
        if (!disposed) {
          active ||= [...known.current.values()].some(status => status === "queued" || status === "running")
          timer = window.setTimeout(() => void refresh(), refreshAgain ? 0 : active ? 2_500 : 8_000)
          refreshAgain = false
        }
      }
    }
    function wake() {
      window.clearTimeout(timer)
      if (refreshing) refreshAgain = true
      else void refresh()
    }
    function resume() {
      if (document.visibilityState !== "visible") return
      wake()
    }
    wakeRef.current = wake
    void refresh()
    document.addEventListener("visibilitychange", resume)
    return () => {
      disposed = true
      wakeRef.current = null
      window.clearTimeout(timer)
      document.removeEventListener("visibilitychange", resume)
    }
  }, [])

  return { add }
}
