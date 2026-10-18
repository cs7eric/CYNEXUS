import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import { test } from "node:test"
import vm from "node:vm"
import ts from "typescript"

const model = { exports: {} }
const filename = new URL("../src/mobile/pages/finance/lib/asset-pile.ts", import.meta.url)
const code = ts.transpileModule(readFileSync(filename, "utf8"), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 },
}).outputText
vm.runInNewContext(code, { module: model, exports: model.exports })
const { moneyToFen, formatCny, compactCny, decomposeAssets, getPilePieces, PILE_STAGE_WIDTH, PILE_STAGE_HEIGHT } = model.exports

const groupTotal = (data) => [...data.groups, ...data.loose].reduce((total, group) => total + BigInt(group.valueInFen) * BigInt(group.count), 0n)
const pieceTotal = (pieces) => pieces.reduce((total, piece) => total + BigInt(piece.group.valueInFen) * BigInt(piece.representedCount), 0n)

test("decimal monetary values round to safe integer fen without binary tie errors", () => {
  for (const [amount, expected] of [[0, 0], [1.005, 101], [2.675, 268], [1.0049, 100], [0.005, 1], [-1.005, -101], [-0.001, 0], [28386.61, 2838661], [1e-7, 0], [1e10, 1e12]]) {
    assert.equal(moneyToFen(amount), expected, String(amount))
  }
  for (const amount of [NaN, Infinity, -Infinity, Number.MAX_VALUE, Number.MAX_SAFE_INTEGER]) {
    assert.equal(moneyToFen(amount), null)
  }
})

test("formatting retains cents and debt signs across the safe integer range", () => {
  assert.equal(formatCny(2838661), "¥28,386.61")
  assert.equal(formatCny(-462001), "−¥4,620.01")
  assert.equal(formatCny(Number.MAX_SAFE_INTEGER), "¥90,071,992,547,409.91")
  assert.equal(formatCny(NaN), "Unavailable")
  assert.equal(compactCny(100000), "¥1k")
  assert.equal(compactCny(1000000), "¥10k")
  assert.equal(compactCny(100000000), "¥1m")
})

test("approved sample separates two ten-thousand and eight thousand bundles from the exact remainder", () => {
  const data = decomposeAssets(2838661)
  assert.equal(JSON.stringify(data.groups.map(({ valueInFen, count }) => [valueInFen, count])), JSON.stringify([[1000000, 2], [100000, 8]]))
  assert.equal(groupTotal(data), 2838661n)
  assert.equal(groupTotal({ groups: [], loose: data.loose }), 38661n)
  const pieces = getPilePieces(2838661)
  const tenThousand = pieces.filter((piece) => piece.group.kind === "bundle" && piece.group.valueInFen === 1000000)
  const thousand = pieces.filter((piece) => piece.group.kind === "bundle" && piece.group.valueInFen === 100000)
  assert.equal(tenThousand.length, 2)
  assert.equal(thousand.length, 1)
  assert.ok(tenThousand.every((piece) => piece.representedCount === 1))
  assert.equal(thousand[0].representedCount, 8)
})

test("ten-thousand bundles are individually scattered at five and nine copies", () => {
  for (const [fen, expectedCount] of [[5000000, 5], [9000000, 9]]) {
    const pieces = getPilePieces(fen)
    assert.equal(pieces.length, expectedCount)
    assert.ok(pieces.every((piece) => piece.group.kind === "bundle" && piece.group.valueInFen === 1000000 && piece.representedCount === 1))
    assert.equal(new Set(pieces.map(({ x, y, rotation }) => `${x}:${y}:${rotation}`)).size, expectedCount)
    assert.equal(pieceTotal(pieces), BigInt(fen))
  }
})

test("thousand bundles retain their quantity badge while larger bundles remain separate", () => {
  for (const [fen, expectedCount] of [[500000, 5], [900000, 9]]) {
    const pieces = getPilePieces(fen)
    assert.equal(pieces.length, 1)
    assert.equal(pieces[0].group.valueInFen, 100000)
    assert.equal(pieces[0].representedCount, expectedCount)
    assert.equal(pieceTotal(pieces), BigInt(fen))
  }
  const pieces = getPilePieces(50000000)
  assert.equal(pieces.length, 5)
  assert.ok(pieces.every((piece) => piece.group.valueInFen === 10000000 && piece.representedCount === 1))
})

test("invalid or negative asset amounts never create money", () => {
  for (const amount of [0, -1, -100000, 1.5, NaN, Infinity, Number.MAX_SAFE_INTEGER + 1]) {
    const data = decomposeAssets(amount)
    assert.equal(data.groups.length + data.loose.length, 0)
    assert.equal(getPilePieces(amount).length, 0)
  }
})

test("all denominations and large values preserve exact totals with bounded, stable stage geometry", () => {
  const samples = [1, 9, 10, 49, 50, 99, 100, 500, 1000, 2000, 5000, 10000, 99999, 100000, 38661, 2838661, 123456789, 4444444444444444, 8999999999999999, Number.MAX_SAFE_INTEGER]
  let state = 1234567
  for (let index = 0; index < 300; index++) {
    state = (Math.imul(state, 1664525) + 1013904223) >>> 0
    samples.push(state * 1000003)
  }

  for (const amount of samples) {
    const data = decomposeAssets(amount)
    const pieces = getPilePieces(amount)
    assert.equal(groupTotal(data), BigInt(amount), `decomposition ${amount}`)
    assert.equal(pieceTotal(pieces), BigInt(amount), `pieces ${amount}`)
    assert.ok(pieces.length <= 120, `${amount} drew ${pieces.length} pieces`)
    assert.equal(JSON.stringify(pieces), JSON.stringify(getPilePieces(amount)), `stable ${amount}`)
    assert.equal(new Set(pieces.map((piece) => piece.key)).size, pieces.length)
    for (const piece of pieces) {
      assert.ok(piece.representedCount > 0)
      const angle = piece.rotation * Math.PI / 180
      const height = piece.width * (piece.group.kind === "coin" ? 1.04 : 0.53)
      for (const [dx, dy] of [[-1, -1], [-1, 1], [1, -1], [1, 1]]) {
        const x = piece.x + dx * piece.width / 2 * Math.cos(angle) - dy * height / 2 * Math.sin(angle)
        const y = piece.y + dx * piece.width / 2 * Math.sin(angle) + dy * height / 2 * Math.cos(angle)
        assert.ok(x >= 0 && x <= PILE_STAGE_WIDTH, `width ${piece.key}`)
        assert.ok(y >= 0 && y <= PILE_STAGE_HEIGHT, `height ${piece.key}`)
      }
    }
  }
})
