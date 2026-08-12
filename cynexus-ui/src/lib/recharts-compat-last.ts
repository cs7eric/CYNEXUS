export default function last<T>(array: ArrayLike<T> | null | undefined): T | undefined {
  if (array == null || array.length === 0) return undefined
  return array[array.length - 1]
}
