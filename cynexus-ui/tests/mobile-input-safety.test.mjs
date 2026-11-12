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
  assert.match(mobileStyles, /^:is\(input[^\n]+\) \{ font-size: var\(--mobile-editable-font-size, 16px\) !important; \}$/m)
  assert.doesNotMatch(mobileStyles, /^\.mobile-theme :is\(input/m, "Vite already scopes mobile styles and must not emit a duplicate theme ancestor")
  for (const type of ["file", "image", "color"]) {
    assert.ok(mobileStyles.includes(`:not([type="${type}"])`), `non-text input type ${type} should be excluded`)
  }
  assert.match(mobileStyles, /^\.food-goal-field input \{ --mobile-editable-font-size: 26px; \}$/m)
})

test("viewport measurements are retried after zoom and focus transitions", () => {
  const viewport = readFileSync(new URL("../src/mobile/hooks/use-mobile-viewport.ts", import.meta.url), "utf8")
  assert.match(viewport, /Date\.now\(\) < retryDeadline/)
  assert.match(viewport, /setTimeout\(\(\) => \{[\s\S]*?requestAnimationFrame\(update\)/)
  assert.match(viewport, /window\.addEventListener\("focusout", schedule\)/)
  assert.match(viewport, /document\.addEventListener\("visibilitychange", schedule\)/)
})
