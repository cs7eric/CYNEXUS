import assert from "node:assert/strict"
import { readdirSync, readFileSync } from "node:fs"
import { join, resolve } from "node:path"
import { fileURLToPath } from "node:url"
import test from "node:test"

const mobileRoot = resolve(fileURLToPath(new URL("../src/mobile", import.meta.url)))
const mobileStyles = readFileSync(join(mobileRoot, "styles.css"), "utf8")

function listTsx(directory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) return listTsx(path)
    return entry.isFile() && entry.name.endsWith(".tsx") ? [path] : []
  })
}

test("mobile routes never autofocus an editable control on page entry", () => {
  for (const path of listTsx(mobileRoot)) {
    assert.doesNotMatch(readFileSync(path, "utf8"), /\bautofocus\b/i, path)
  }
})

test("mobile editable controls use the iOS-safe 16px baseline", () => {
  assert.match(mobileStyles, /font-size:\s*var\(--mobile-editable-font-size,\s*16px\)\s*!important/)
  for (const type of ["file", "image", "color"]) {
    assert.ok(mobileStyles.includes(`:not([type="${type}"])`), `non-text input type ${type} should be excluded`)
  }
  assert.match(mobileStyles, /\.mobile-theme \.food-goal-field input\s*\{\s*--mobile-editable-font-size:\s*26px;/)
})
