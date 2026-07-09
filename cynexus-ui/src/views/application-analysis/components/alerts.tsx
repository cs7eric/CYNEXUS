import { useEffect, useMemo, useRef, useState } from "react"
import { AlertTriangle, CheckCircle2, ShieldAlert, X } from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { compactIssueList } from "../lib/format"

export function Alerts({
  error,
  info,
  warnings,
  errors,
}: {
  error: string | null
  info: string | null
  warnings: string[]
  errors: string[]
}) {
  const [visibleError, setVisibleError] = useState(error)
  const [visibleInfo, setVisibleInfo] = useState(info)
  const [visibleWarnings, setVisibleWarnings] = useState(warnings)
  const [visibleErrors, setVisibleErrors] = useState(errors)
  const dismissedKeysRef = useRef(new Set<string>())

  const errorKey = useMemo(() => (error ? `error:${error}` : ""), [error])
  const infoKey = useMemo(() => (info ? `info:${info}` : ""), [info])
  const issuesKey = useMemo(
    () => (warnings.length || errors.length ? `issues:${[...warnings, ...errors].join("|")}` : ""),
    [warnings, errors],
  )

  useEffect(() => {
    setVisibleError(errorKey && dismissedKeysRef.current.has(errorKey) ? null : error)
  }, [error, errorKey])

  useEffect(() => {
    setVisibleInfo(infoKey && dismissedKeysRef.current.has(infoKey) ? null : info)
  }, [info, infoKey])

  useEffect(() => {
    if (issuesKey && dismissedKeysRef.current.has(issuesKey)) {
      setVisibleWarnings([])
      setVisibleErrors([])
      return
    }
    setVisibleWarnings(warnings)
    setVisibleErrors(errors)
  }, [warnings, errors, issuesKey])

  useEffect(() => {
    const hasVisible = Boolean(visibleError || visibleInfo || visibleWarnings.length || visibleErrors.length)
    if (!hasVisible) return
    const timer = window.setTimeout(() => {
      if (visibleError && errorKey) dismissedKeysRef.current.add(errorKey)
      if (visibleInfo && infoKey) dismissedKeysRef.current.add(infoKey)
      if ((visibleWarnings.length || visibleErrors.length) && issuesKey) dismissedKeysRef.current.add(issuesKey)
      setVisibleError(null)
      setVisibleInfo(null)
      setVisibleWarnings([])
      setVisibleErrors([])
    }, 3000)
    return () => window.clearTimeout(timer)
  }, [errorKey, infoKey, issuesKey, visibleError, visibleInfo, visibleWarnings, visibleErrors])

  const issueItems = useMemo(
    () => compactIssueList([...visibleWarnings, ...visibleErrors], 4),
    [visibleWarnings, visibleErrors],
  )

  return (
    <div className="space-y-3">
      {visibleError ? (
        <Alert variant="destructive" className="relative rounded-lg border-red-200 bg-red-50 pr-10">
          <ShieldAlert className="size-4" />
          <AlertTitle>分析失败</AlertTitle>
          <AlertDescription className="line-clamp-3 break-words">{visibleError}</AlertDescription>
          <button
            type="button"
            className="absolute right-2 top-2 rounded-md p-1 text-red-500 transition hover:bg-red-100 hover:text-red-700"
            onClick={() => {
              if (errorKey) dismissedKeysRef.current.add(errorKey)
              setVisibleError(null)
            }}
            aria-label="关闭消息"
          >
            <X className="size-4" />
          </button>
        </Alert>
      ) : null}
      {visibleInfo ? (
        <Alert className="relative rounded-lg border-emerald-200 bg-emerald-50 text-emerald-900 pr-10">
          <CheckCircle2 className="size-4" />
          <AlertTitle>提示</AlertTitle>
          <AlertDescription className="line-clamp-3 break-words">{visibleInfo}</AlertDescription>
          <button
            type="button"
            className="absolute right-2 top-2 rounded-md p-1 text-emerald-600 transition hover:bg-emerald-100 hover:text-emerald-800"
            onClick={() => {
              if (infoKey) dismissedKeysRef.current.add(infoKey)
              setVisibleInfo(null)
            }}
            aria-label="关闭消息"
          >
            <X className="size-4" />
          </button>
        </Alert>
      ) : null}
      {issueItems.length ? (
        <Alert className="relative rounded-lg border-amber-200 bg-amber-50 text-amber-900 pr-10">
          <AlertTriangle className="size-4" />
          <AlertTitle>数据质量 / 错误</AlertTitle>
          <AlertDescription>
            <ul className="space-y-1">
              {issueItems.map((item, index) => (
                <li key={`${index}-${item}`} className="line-clamp-2 break-words">
                  {item}
                </li>
              ))}
            </ul>
          </AlertDescription>
          <button
            type="button"
            className="absolute right-2 top-2 rounded-md p-1 text-amber-700 transition hover:bg-amber-100 hover:text-amber-900"
            onClick={() => {
              if (issuesKey) dismissedKeysRef.current.add(issuesKey)
              setVisibleWarnings([])
              setVisibleErrors([])
            }}
            aria-label="关闭消息"
          >
            <X className="size-4" />
          </button>
        </Alert>
      ) : null}
    </div>
  )
}
