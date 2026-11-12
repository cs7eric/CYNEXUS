export function applyNumericKey(value: string, key: string, options: { decimal: boolean; maxLength?: number | null }) {
  if (key === "clear") return ""
  if (key === "backspace") return value.slice(0, -1)
  if (key === ".") {
    if (!options.decimal || value.includes(".")) return value
    return value ? `${value}.` : "0."
  }
  if (!/^\d$/.test(key)) return value
  const next = value + key
  return options.maxLength && next.length > options.maxLength ? value : next
}
