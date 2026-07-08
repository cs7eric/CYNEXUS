export function formatKnowledgeDate(value?: string | null) {
  if (!value) return "-"
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value))
}

export function estimateReadingMinutes(content: string) {
  const length = content.trim().length
  if (!length) return 0
  return Math.max(1, Math.ceil(length / 600))
}
