import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import { test } from "node:test"
import vm from "node:vm"
import ts from "typescript"

function load(path, dependencies = {}) {
  const module = { exports: {} }
  const code = ts.transpileModule(readFileSync(new URL(path, import.meta.url), "utf8"), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 },
  }).outputText
  vm.runInNewContext(code, { module, exports: module.exports, require: (name) => { assert.ok(name in dependencies, `Unexpected dependency: ${name}`); return dependencies[name] }, Date })
  return module.exports
}
const model = load("../src/services/workout/model.ts")
const proportions = load("../src/mobile/pages/workout/lib/body-proportions.ts")
const { bodyGeometry, measurementTrend } = load("../src/mobile/pages/workout/lib/body-geometry.ts", { "./model": model, "./body-proportions": proportions })
const observation = (id, metric, value, measuredAt, side = null) => ({ id, metric, value, measuredAt, side })

test("body proportions keep measurement coordinates aligned at a 67% lower-body ratio", () => {
  const { bodyY, bodyPath } = proportions
  const height = bodyY(338) - bodyY(10)
  assert.ok(Math.abs((bodyY(338) - bodyY(146)) / height - .67) < 1e-9)
  const thigh = bodyY(264) - bodyY(209)
  const calf = bodyY(315) - bodyY(264)
  assert.ok(calf >= thigh && calf / thigh < 1.1)
  assert.equal(bodyPath("M25 146H35V264L40 315Z"), "M 25 118.24 H 35 V 244 L 40 316 Z")
})

test("historical body outlines use only measurements available on that date", () => {
  const observations = [observation("old", "waist", 78, "2026-03-01"), observation("new", "waist", 94, "2026-04-01")]
  assert.equal(bodyGeometry(observations, "2026-02-28").torso, bodyGeometry([], "2026-02-28").torso)
  assert.notEqual(bodyGeometry(observations, "2026-03-01").torso, bodyGeometry(observations, "2026-04-01").torso)
  assert.equal(bodyGeometry(observations, "2026-03-15").torso, bodyGeometry(observations, "2026-03-01").torso)
})

test("limb observations remain independent and missing measurements stay neutral", () => {
  const neutral = bodyGeometry([], "2026-10-18")
  const leftArm = bodyGeometry([observation("left", "upperArm", 42, "2026-10-18", "left")], "2026-10-18")
  assert.notEqual(leftArm.leftArm, neutral.leftArm)
  assert.equal(leftArm.rightArm, neutral.rightArm)
  assert.equal(leftArm.torso, neutral.torso)
  assert.equal(leftArm.leftLeg, neutral.leftLeg)
  const weightOnly = bodyGeometry([observation("weight", "weight", 120, "2026-10-18")], "2026-10-18")
  assert.equal(weightOnly.torso, neutral.torso, "weight must not infer circumference")
})

test("extreme valid measurements keep deformation bounded", () => {
  for (const value of [30, 250]) {
    const geometry = bodyGeometry([observation("waist", "waist", value, "2026-10-18")], "2026-10-18")
    assert.ok(geometry.widths.waist >= 26 * .72)
    assert.ok(geometry.widths.waist <= 26 * 1.35)
    assert.ok(!geometry.torso.includes("NaN"))
  }
})

test("trend contains only real observations for the selected metric, side and inclusive dates", () => {
  const entries = [observation("before", "calf", 32, "2026-09-30", "left"), observation("late", "calf", 34, "2026-10-18", "left"), observation("other-side", "calf", 35, "2026-10-11", "right"), observation("early", "calf", 33, "2026-10-01", "left"), observation("wrong-metric", "thigh", 55, "2026-10-11", "left"), observation("future", "calf", 35, "2026-10-19", "left")]
  const points = measurementTrend(entries, "calf", "left", "2026-10-01", "2026-10-18")
  assert.equal(points.length, 2)
  assert.equal(points[0].id, "early")
  assert.equal(points[1].id, "late")
  assert.equal(entries[0].id, "before", "input order is not mutated")
})
