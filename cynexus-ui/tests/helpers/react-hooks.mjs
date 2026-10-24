import { readFileSync } from "node:fs"
import vm from "node:vm"
import ts from "typescript"

export function loadTsModule(url, dependencies, globals = {}) {
  const module = { exports: {} }
  const output = ts.transpileModule(readFileSync(url, "utf8"), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2023, jsx: ts.JsxEmit.ReactJSX },
  }).outputText
  vm.runInNewContext(output, {
    module, exports: module.exports, require: name => {
      if (!(name in dependencies)) throw new Error(`Unexpected dependency: ${name}`)
      return dependencies[name]
    },
    AbortController, DOMException, ...globals,
  })
  return module.exports
}

export function hookRenderer() {
  const slots = [], effects = []
  let cursor = 0, dirty = false, view, renderComponent
  const same = (a, b) => a && b && a.length === b.length && a.every((value, index) => Object.is(value, b[index]))
  const memo = (factory, deps) => {
    const index = cursor++
    if (!same(slots[index]?.deps, deps)) slots[index] = { value: factory(), deps }
    return slots[index].value
  }
  const react = {
    useState(initial) {
      const index = cursor++
      slots[index] ??= { value: typeof initial === "function" ? initial() : initial }
      return [slots[index].value, value => {
        const next = typeof value === "function" ? value(slots[index].value) : value
        if (!Object.is(next, slots[index].value)) { slots[index].value = next; dirty = true }
      }]
    },
    useRef: value => memo(() => ({ current: value }), []),
    useMemo: memo,
    useCallback: (callback, deps) => memo(() => callback, deps),
    useEffect(effect, deps) {
      const index = cursor++
      if (!same(slots[index]?.deps, deps)) effects.push(() => {
        slots[index]?.cleanup?.()
        slots[index] = { deps, cleanup: effect() }
      })
    },
  }
  function render() {
    cursor = 0; dirty = false
    view = renderComponent()
    effects.splice(0).forEach(effect => effect())
  }
  return {
    react,
    mount(component) { renderComponent = component; render() },
    async settle() {
      await new Promise(resolve => setImmediate(resolve))
      while (dirty) render()
    },
    view: () => view,
    close: () => slots.forEach(slot => slot?.cleanup?.()),
  }
}

export function elements(node) {
  if (Array.isArray(node)) return node.flatMap(elements)
  if (!node || typeof node !== "object" || !node.props) return []
  return [node, ...elements(node.props.children)]
}
