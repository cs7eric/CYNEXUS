import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import { createRequire } from "node:module"
import path from "node:path"
import { fileURLToPath } from "node:url"
import vm from "node:vm"
import { test } from "node:test"
import ts from "typescript"

const require = createRequire(import.meta.url)
const sourceRoot = fileURLToPath(new URL("../src/", import.meta.url))
const modules = new Map()

function loadSource(name) {
  const filename = path.join(sourceRoot, `${name}.ts`)
  if (modules.has(filename)) return modules.get(filename)
  const module = { exports: {} }
  const code = ts.transpileModule(readFileSync(filename, "utf8"), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023 },
  }).outputText
  vm.runInNewContext(code, {
    module, exports: module.exports,
    require: (id) => id.startsWith("@/") ? loadSource(id.slice(2)) : require(id),
  }, { filename })
  modules.set(filename, module.exports)
  return module.exports
}

const { sidebarConfig } = loadSource("config/sidebar")
const { workspaces, sharedNavigation, getWorkspaceForPath, isPathInWorkspace, validWorkspaceLocation } = loadSource("config/workspaces")

test("every sidebar application belongs to exactly one group or the shared group", () => {
  const assigned = [sharedNavigation, ...Object.values(workspaces).flatMap((space) => space.groups)]
    .flatMap((group) => group.items.map((item) => item.url))
  assert.equal(new Set(assigned).size, assigned.length)
  assert.deepEqual([...assigned].sort(), Array.from(sidebarConfig.navMain, (item) => item.url).sort())
})

test("all registered routes, including detail routes and legacy aliases, have a space", () => {
  const router = readFileSync(path.join(sourceRoot, "router/index.tsx"), "utf8")
  const paths = []
  const source = ts.createSourceFile("router.tsx", router, ts.ScriptTarget.Latest, true, ts.ScriptKind.TSX)
  function visit(node) {
    if (ts.isPropertyAssignment(node) && node.name.getText(source) === "path" && ts.isStringLiteral(node.initializer)) {
      paths.push(node.initializer.text === "/" ? "/" : `/${node.initializer.text}`)
    }
    ts.forEachChild(node, visit)
  }
  visit(source)
  for (const route of paths) assert.ok(getWorkspaceForPath(route), route)
})

test("personal tools and cross-prefix child routes stay in the personal space", () => {
  for (const route of ["/finance/ledger", "/downloader", "/personal-portfolio/imports/123/review?tab=details", "/stock-overview/self-selected", "/mp4-to-word/history/123", "/data-collector/limit-board-monitor"]) {
    assert.equal(getWorkspaceForPath(route), "personal", route)
    assert.equal(isPathInWorkspace(route, "development"), false, route)
  }
})

test("operations and experiment routes stay in the development space", () => {
  for (const route of ["/settings/scheduler", "/settings/api-workflows", "/ai/provider", "/poc/ai", "/dashboard", "/heatmap-data-debug"]) {
    assert.equal(getWorkspaceForPath(route), "development", route)
    assert.equal(isPathInWorkspace(route, "personal"), false, route)
  }
})

test("home, Agent and Knowledge Base are available in both spaces", () => {
  for (const route of ["/", "/cynexus-agent", "/knowledge-base/writing?id=123", "/ai/agent"]) {
    assert.equal(getWorkspaceForPath(route), "shared", route)
    assert.ok(isPathInWorkspace(route, "personal"))
    assert.ok(isPathInWorkspace(route, "development"))
  }
})

test("restore preserves local query/hash and rejects invalid or foreign locations", () => {
  assert.equal(validWorkspaceLocation("/finance/ledger?account=1#entry", "personal"), "/finance/ledger?account=1#entry")
  for (const route of [null, {}, "https://example.com", "//example.com", "/\\example.com", "/unknown", "/settings/scheduler", "/finance-other"]) {
    assert.equal(validWorkspaceLocation(route, "personal"), workspaces.personal.defaultPath)
  }
  assert.equal(validWorkspaceLocation("/finance/ledger", "development"), workspaces.development.defaultPath)
})

test("navigation state remains usable when browser storage is unavailable", () => {
  const { useWorkspaceState } = loadSource("layout/workspace-state")
  useWorkspaceState.getState().recordLocation("development", "/ai/provider?tab=routing")
  assert.equal(useWorkspaceState.getState().activeWorkspace, "development")
  assert.equal(useWorkspaceState.getState().lastLocations.development, "/ai/provider?tab=routing")
  const previous = useWorkspaceState.getState()
  previous.recordLocation("development", "/ai/provider?tab=routing")
  assert.equal(useWorkspaceState.getState(), previous)
})

test("mobile shell routes share spaces without claiming similar desktop paths", () => {
  for (const route of ["/m", "/m/", "/m/apps", "/m/me?tab=security#session", "/m/unknown"]) {
    assert.equal(getWorkspaceForPath(route), "shared", route)
    assert.ok(isPathInWorkspace(route, "personal"))
    assert.ok(isPathInWorkspace(route, "development"))
  }
  assert.equal(getWorkspaceForPath("/m-other"), undefined)
  assert.equal(getWorkspaceForPath("/mobile"), undefined)
})

test("mobile navigation updates active space while preserving both desktop destinations", () => {
  const { useWorkspaceState } = loadSource("layout/workspace-state")
  useWorkspaceState.getState().recordLocation("personal", "/finance/ledger?account=1#entry")
  useWorkspaceState.getState().recordLocation("development", "/ai/provider?tab=routing")
  useWorkspaceState.getState().recordLocation("personal", "/m/apps")
  assert.equal(useWorkspaceState.getState().activeWorkspace, "personal")
  assert.equal(useWorkspaceState.getState().lastLocations.personal, "/finance/ledger?account=1#entry")
  useWorkspaceState.getState().recordLocation("development", "/m/me")
  assert.equal(useWorkspaceState.getState().activeWorkspace, "development")
  assert.equal(useWorkspaceState.getState().lastLocations.development, "/ai/provider?tab=routing")
})

test("mobile app groups follow the same space ownership as desktop", () => {
  const { getMobileAppGroups } = loadSource("mobile/navigation")
  for (const workspace of ["personal", "development"]) {
    const urls = getMobileAppGroups(workspace).flatMap((group) => group.items.map((item) => item.url))
    assert.equal(new Set(urls).size, urls.length)
    for (const url of urls) assert.ok(isPathInWorkspace(url, workspace), url)
  }
})
