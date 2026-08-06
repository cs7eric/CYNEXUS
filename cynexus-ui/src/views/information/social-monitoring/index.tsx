import { useCallback, useEffect, useMemo, useState } from "react"
import {
  Activity,
  AlertTriangle,
  BarChart3,
  CalendarDays,
  Check,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  CirclePause,
  CirclePlay,
  Clock3,
  Database,
  ExternalLink,
  FileText,
  History,
  Info,
  Link2,
  Loader2,
  LogIn,
  MessageCircle,
  Pencil,
  Plus,
  QrCode,
  RefreshCw,
  Reply,
  Search,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
  Target,
  Trash2,
  Wifi,
  XCircle,
} from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Drawer,
  DrawerContent,
  DrawerDescription,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { Skeleton } from "@/components/ui/skeleton"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { WorkspaceShell } from "@/layout/workspace-shell"
import {
  closeSocialLogin,
  completeSocialLogin,
  fetchSocialLogin,
  fetchSocialLoginStatus,
  fetchSocialPostComments,
  fetchSocialPosts,
  startSocialLogin,
  triggerSocialJob,
  type SocialComment,
  type SocialJobTriggerResult,
  type SocialLoginSession,
  type SocialLoginStatus,
  type SocialPlatform,
  type SocialPost,
} from "@/services/information/social-monitoring"

type CollectionPurpose = "market_sentiment" | "market_analysis"
type SocialJobKind = "discovery" | "comments"
type PageSurface = "report" | "targets"

type Creator = {
  id: string
  platform: SocialPlatform
  purpose: CollectionPurpose
  creatorKey: string
  profileUrl: string
  displayName: string
  enabled: boolean
}

type CreatorEditor = Omit<Creator, "enabled" | "id"> & { id?: string }

type RunRecord = {
  id: string
  platform: SocialPlatform
  kind: SocialJobKind
  purpose: CollectionPurpose
  status: string
  rowsWritten: number
  runId?: string
  createdAt: string
}

const CREATOR_STORAGE_KEY = "cynexus.social-monitoring.creators"
const platformLabels: Record<SocialPlatform, string> = {
  xiaohongshu: "小红书",
  douyin: "抖音",
}
const platforms: SocialPlatform[] = ["xiaohongshu", "douyin"]
const purposes: CollectionPurpose[] = ["market_sentiment", "market_analysis"]
const purposeLabels: Record<CollectionPurpose, string> = {
  market_sentiment: "股市情绪感知",
  market_analysis: "帖子行情分析",
}
const purposeDescriptions: Record<CollectionPurpose, string> = {
  market_sentiment: "从每日评论聚合中观察关注主题、情绪结构和市场周期。",
  market_analysis: "从每日帖子中沉淀观点、叙事和对行情的影响判断。",
}
const emotionPhases = ["冰点", "修复", "升温", "高潮", "分歧", "退潮"]

function normalizeCreators(value: unknown): Creator[] {
  if (!Array.isArray(value)) return []
  return value.flatMap((item) => {
    if (!item || typeof item !== "object") return []
    const creator = item as Partial<Creator>
    if (!creator.platform || !creator.creatorKey || !creator.profileUrl) return []
    return [{
      id: creator.id || `${creator.platform}-${Date.now()}-${Math.random()}`,
      platform: creator.platform,
      purpose: creator.purpose === "market_analysis" ? "market_analysis" : "market_sentiment",
      creatorKey: creator.creatorKey,
      profileUrl: creator.profileUrl,
      displayName: creator.displayName || creator.creatorKey,
      enabled: creator.enabled !== false,
    }]
  })
}

function todayIsoDate() {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Shanghai" }).format(new Date())
}

function formatReportDate(value: string) {
  const date = new Date(`${value}T00:00:00+08:00`)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString("zh-CN", { timeZone: "Asia/Shanghai", year: "numeric", month: "long", day: "numeric", weekday: "short" })
}

function formatDateTime(value?: string | null) {
  if (!value) return "时间未知"
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("zh-CN", { hour12: false })
}

function shiftIsoDate(value: string, delta: number) {
  const date = new Date(`${value}T00:00:00+08:00`)
  date.setDate(date.getDate() + delta)
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Shanghai" }).format(date)
}

function statusLabel(status?: string | null) {
  if (status === "probing") return "检测中"
  if (status === "authenticated") return "已登录"
  if (status === "waiting_login") return "等待 Chrome 登录"
  if (status === "blocked") return "专用 Chrome 未连接"
  if (status === "failed") return "登录失败"
  if (status === "closed") return "已关闭"
  return "未连接"
}

function statusClass(status?: string | null) {
  if (status === "authenticated") return "border-emerald-200 bg-emerald-50 text-emerald-700"
  if (status === "waiting_login") return "border-amber-200 bg-amber-50 text-amber-700"
  if (status === "blocked" || status === "failed") return "border-red-200 bg-red-50 text-red-700"
  return "border-border bg-muted text-muted-foreground"
}

function jobStateKey(platform: SocialPlatform, kind: SocialJobKind) {
  return `${platform}:${kind}` as `${SocialPlatform}:${SocialJobKind}`
}

function jobRows(result: SocialJobTriggerResult) {
  const summary = (result.result?.metadata ?? result.result ?? {}) as Record<string, unknown>
  const rows = Number(summary.rows_written ?? summary.posts_discovered ?? 0)
  return Number.isFinite(rows) ? rows : 0
}

function sourceStatusIcon(status?: string | null) {
  if (status === "authenticated") return <CheckCircle2 className="size-3.5 text-emerald-600" />
  if (status === "blocked" || status === "failed") return <XCircle className="size-3.5 text-destructive" />
  return <Wifi className="size-3.5 text-muted-foreground" />
}

export default function SocialMonitoringPage() {
  const [creators, setCreators] = useState<Creator[]>(() => {
    if (typeof window === "undefined") return []
    try {
      const saved = window.localStorage.getItem(CREATOR_STORAGE_KEY)
      return saved ? normalizeCreators(JSON.parse(saved)) : []
    } catch {
      return []
    }
  })
  const [workspace, setWorkspace] = useState<CollectionPurpose>("market_sentiment")
  const [surface, setSurface] = useState<PageSurface>("report")
  const [reportDate, setReportDate] = useState(todayIsoDate)
  const [posts, setPosts] = useState<SocialPost[]>([])
  const [postsLoading, setPostsLoading] = useState(false)
  const [postsError, setPostsError] = useState<string | null>(null)
  const [loginStatuses, setLoginStatuses] = useState<Partial<Record<SocialPlatform, SocialLoginStatus["status"]>>>({})
  const [loginPlatform, setLoginPlatform] = useState<SocialPlatform>("xiaohongshu")
  const [loginSession, setLoginSession] = useState<SocialLoginSession | null>(null)
  const [loginOpen, setLoginOpen] = useState(false)
  const [loginLoading, setLoginLoading] = useState(false)
  const [sourceDrawerOpen, setSourceDrawerOpen] = useState(false)
  const [runDrawerOpen, setRunDrawerOpen] = useState(false)
  const [batchRunning, setBatchRunning] = useState(false)
  const [runningJobs, setRunningJobs] = useState<Partial<Record<`${SocialPlatform}:${SocialJobKind}`, boolean>>>({})
  const [runHistory, setRunHistory] = useState<RunRecord[]>([])
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [targetSearch, setTargetSearch] = useState("")
  const [targetPlatform, setTargetPlatform] = useState<SocialPlatform | "all">("all")
  const [targetEditorOpen, setTargetEditorOpen] = useState(false)
  const [editorForm, setEditorForm] = useState<CreatorEditor>({
    platform: "xiaohongshu",
    purpose: "market_sentiment",
    creatorKey: "",
    profileUrl: "",
    displayName: "",
  })
  const [selectedPost, setSelectedPost] = useState<SocialPost | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [comments, setComments] = useState<SocialComment[]>([])
  const [commentsLoading, setCommentsLoading] = useState(false)
  const [commentsError, setCommentsError] = useState<string | null>(null)

  const isToday = reportDate === todayIsoDate()

  useEffect(() => {
    window.localStorage.setItem(CREATOR_STORAGE_KEY, JSON.stringify(creators))
  }, [creators])

  const refreshLoginStatuses = useCallback(async () => {
    const entries = await Promise.all(platforms.map(async (platform) => {
      try {
        const result = await fetchSocialLoginStatus(platform)
        return [platform, result.status] as const
      } catch {
        return [platform, "blocked"] as const
      }
    }))
    setLoginStatuses(Object.fromEntries(entries) as Partial<Record<SocialPlatform, SocialLoginStatus["status"]>>)
  }, [])

  useEffect(() => {
    const initial = window.setTimeout(() => void refreshLoginStatuses(), 0)
    const timer = window.setInterval(() => void refreshLoginStatuses(), 15_000)
    return () => {
      window.clearTimeout(initial)
      window.clearInterval(timer)
    }
  }, [refreshLoginStatuses])

  useEffect(() => {
    if (!loginOpen || !loginSession?.session_id || loginSession.status !== "waiting_login") return
    const timer = window.setInterval(() => {
      void fetchSocialLogin(loginSession.session_id)
        .then(setLoginSession)
        .catch((reason) => setError(reason instanceof Error ? reason.message : "登录状态刷新失败"))
    }, 2500)
    return () => window.clearInterval(timer)
  }, [loginOpen, loginSession?.session_id, loginSession?.status])

  const loadPosts = useCallback(async () => {
    setPostsLoading(true)
    setPostsError(null)
    try {
      const results = await Promise.all(platforms.map((platform) => fetchSocialPosts(platform, true, 100)))
      setPosts(results.flatMap((result) => result.posts))
    } catch (reason) {
      setPostsError(reason instanceof Error ? reason.message : "今日采集数据加载失败")
    } finally {
      setPostsLoading(false)
    }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      if (isToday) void loadPosts()
      else setPosts([])
    }, 0)
    return () => window.clearTimeout(timer)
  }, [isToday, loadPosts, reportDate])

  const openPostDetail = useCallback(async (post: SocialPost) => {
    setSelectedPost(post)
    setComments([])
    setCommentsError(null)
    setDetailOpen(true)
    setCommentsLoading(true)
    try {
      const result = await fetchSocialPostComments(post.platform, post.post_key)
      setSelectedPost(result.post)
      setComments(result.comments)
    } catch (reason) {
      setCommentsError(reason instanceof Error ? reason.message : "评论数据加载失败")
    } finally {
      setCommentsLoading(false)
    }
  }, [])

  const openLogin = useCallback(async (platform: SocialPlatform) => {
    setLoginPlatform(platform)
    setLoginOpen(true)
    setLoginLoading(true)
    setError(null)
    try {
      const probeCreator = creators.find((creator) => creator.platform === platform)
      const next = await startSocialLogin(platform, "default", probeCreator?.profileUrl)
      setLoginSession(next)
      setLoginStatuses((current) => ({ ...current, [platform]: next.status }))
    } catch (reason) {
      setLoginSession(null)
      setError(reason instanceof Error ? reason.message : "无法启动登录会话")
    } finally {
      setLoginLoading(false)
    }
  }, [creators])

  const checkLogin = useCallback(async () => {
    if (!loginSession?.session_id) return
    setLoginLoading(true)
    setError(null)
    try {
      const next = await completeSocialLogin(loginSession.session_id)
      setLoginSession(next)
      setLoginStatuses((current) => ({ ...current, [next.platform]: next.status }))
      if (next.status === "authenticated") {
        setMessage(`${platformLabels[next.platform]}登录成功。`)
        setLoginOpen(false)
      }
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "登录状态检查失败")
    } finally {
      setLoginLoading(false)
    }
  }, [loginSession])

  const cancelLogin = async () => {
    if (loginSession?.session_id) await closeSocialLogin(loginSession.session_id).catch(() => null)
    setLoginSession(null)
    setLoginOpen(false)
  }

  const creatorsForWorkspace = useMemo(
    () => creators.filter((creator) => creator.purpose === workspace),
    [creators, workspace],
  )
  const enabledCreators = creatorsForWorkspace.filter((creator) => creator.enabled)
  const visibleCreators = creatorsForWorkspace.filter((creator) => {
    if (targetPlatform !== "all" && creator.platform !== targetPlatform) return false
    const query = targetSearch.trim().toLowerCase()
    if (!query) return true
    return [creator.displayName, creator.creatorKey, creator.profileUrl, platformLabels[creator.platform]]
      .some((value) => value.toLowerCase().includes(query))
  })

  const postsByPlatform = useMemo(() => platforms.reduce<Record<SocialPlatform, SocialPost[]>>((result, platform) => {
    const purposeByCreatorKey = new Map(creators.map((creator) => [creator.creatorKey, creator.purpose]))
    result[platform] = posts.filter((post) => post.platform === platform && post.creator_key && purposeByCreatorKey.get(post.creator_key) === workspace)
    return result
  }, { xiaohongshu: [], douyin: [] }), [creators, posts, workspace])
  const workspacePosts = postsByPlatform.xiaohongshu.concat(postsByPlatform.douyin)
  const postsWithComments = workspacePosts.filter((post) => post.last_comment_sync_at)
  const commentCount = workspacePosts.reduce((sum, post) => sum + (post.comment_count ?? 0), 0)

  const openCreateTarget = useCallback(() => {
    setEditorForm({ platform: "xiaohongshu", purpose: workspace, creatorKey: "", profileUrl: "", displayName: "" })
    setTargetEditorOpen(true)
  }, [workspace])

  const openEditTarget = useCallback((creator: Creator) => {
    setEditorForm({
      id: creator.id,
      platform: creator.platform,
      purpose: creator.purpose,
      creatorKey: creator.creatorKey,
      profileUrl: creator.profileUrl,
      displayName: creator.displayName,
    })
    setTargetEditorOpen(true)
  }, [])

  const saveTarget = useCallback(() => {
    const creatorKey = editorForm.creatorKey.trim()
    const profileUrl = editorForm.profileUrl.trim()
    if (!creatorKey || !profileUrl) {
      setError("请填写博主标识和主页链接。")
      return
    }
    const nextCreator: Creator = {
      id: editorForm.id || `${editorForm.platform}-${editorForm.purpose}-${Date.now()}`,
      platform: editorForm.platform,
      purpose: editorForm.purpose,
      creatorKey,
      profileUrl,
      displayName: editorForm.displayName.trim() || creatorKey,
      enabled: creators.find((creator) => creator.id === editorForm.id)?.enabled ?? true,
    }
    setCreators((current) => editorForm.id
      ? current.map((creator) => creator.id === editorForm.id ? nextCreator : creator)
      : [...current, nextCreator],
    )
    setTargetEditorOpen(false)
    setError(null)
    setMessage(`${platformLabels[editorForm.platform]}目标已${editorForm.id ? "更新" : "添加"}。`)
  }, [creators, editorForm])

  const toggleTarget = useCallback((creatorId: string) => {
    setCreators((current) => current.map((creator) => creator.id === creatorId ? { ...creator, enabled: !creator.enabled } : creator))
  }, [])

  const removeTarget = useCallback((creator: Creator) => {
    setCreators((current) => current.filter((entry) => entry.id !== creator.id))
    setMessage(`${creator.displayName}已移除。`)
  }, [])

  const runJob = useCallback(async (kind: SocialJobKind, platform: SocialPlatform) => {
    const stateKey = jobStateKey(platform, kind)
    const targetCreators = creatorsForWorkspace.filter((creator) => creator.platform === platform && creator.enabled)
    if (targetCreators.length === 0) {
      throw new Error(`请先为${platformLabels[platform]}配置至少一个已启用目标。`)
    }
    setRunningJobs((current) => ({ ...current, [stateKey]: true }))
    try {
      const result = await triggerSocialJob(
        kind === "discovery" ? "social_creator_discovery" : "social_comment_sync",
        platform,
        kind === "discovery"
          ? {
              creators: targetCreators.map((creator) => ({ creator_key: creator.creatorKey, profile_url: creator.profileUrl, display_name: creator.displayName, purpose: creator.purpose })),
              purpose_targets: targetCreators.map((creator) => ({ creator_key: creator.creatorKey, purpose: creator.purpose })),
              collection_purpose: workspace,
              lock_scope: platform,
              lookback_hours: 24,
              create_table: true,
            }
          : {
              purpose_targets: targetCreators.map((creator) => ({ creator_key: creator.creatorKey, purpose: creator.purpose })),
              collection_purpose: workspace,
              lock_scope: platform,
              today_only: true,
              create_table: true,
            },
      )
      const record: RunRecord = {
        id: `${platform}-${kind}-${Date.now()}`,
        platform,
        kind,
        purpose: workspace,
        status: result.status ?? "accepted",
        rowsWritten: jobRows(result),
        runId: result.run_id,
        createdAt: new Date().toISOString(),
      }
      setRunHistory((current) => [record, ...current].slice(0, 12))
      setMessage(`${platformLabels[platform]}${kind === "discovery" ? "作品发现" : "评论同步"}已触发。`)
      await loadPosts()
    } finally {
      setRunningJobs((current) => ({ ...current, [stateKey]: false }))
    }
  }, [creatorsForWorkspace, loadPosts, workspace])

  const runCollectionBatch = useCallback(async () => {
    setBatchRunning(true)
    setError(null)
    setMessage(null)
    try {
      for (const platform of platforms) {
        await runJob("discovery", platform)
        await runJob("comments", platform)
      }
      setMessage("今日社媒采集批次已完成触发，结果将在任务完成后刷新。")
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "社媒采集批次触发失败")
    } finally {
      setBatchRunning(false)
    }
  }, [runJob])

  const loginBlocked = loginSession?.status === "blocked"
  const dialogPlatform = loginSession?.platform ?? loginPlatform

  return (
    <WorkspaceShell sectionLabel="Information" sectionUrl="/information" pageTitle="Social Research">
      <main className="flex min-h-[calc(100vh-8rem)] flex-col gap-5">
        <header className="flex flex-wrap items-start justify-between gap-4">
          <div className="min-w-0 space-y-2">
            <div className="inline-flex items-center gap-2 text-xs font-medium text-muted-foreground"><Link2 className="size-3.5" />Information · Social Research</div>
            <div className="flex flex-wrap items-center gap-3"><h1 className="text-2xl font-semibold tracking-tight text-foreground">社媒日度研究</h1><Badge variant="outline" className="gap-1.5 rounded-full"><CalendarDays className="size-3" />每日夜间批次</Badge></div>
            <p className="max-w-3xl text-sm leading-6 text-muted-foreground">采集是后台过程，页面聚焦最近一次日度研究档案；小红书和抖音独立连接，研究结果按用途长期沉淀。</p>
          </div>
          <div className="flex flex-wrap items-center justify-end gap-2"><div className="flex items-center gap-1 rounded-md border border-border/70 bg-card px-1.5 py-1"><span className="px-1.5 text-[11px] text-muted-foreground">数据源</span>{platforms.map((platform) => { const status = loginStatuses[platform] ?? "probing"; return <button key={platform} type="button" className="inline-flex items-center gap-1.5 rounded px-2 py-1 text-xs transition-colors hover:bg-muted" onClick={() => setSourceDrawerOpen(true)}><span className={`size-1.5 rounded-full ${status === "authenticated" ? "bg-emerald-500" : status === "blocked" || status === "failed" ? "bg-destructive" : "bg-amber-500"}`} />{platformLabels[platform]}</button> })}</div><Button size="sm" variant="outline" onClick={() => setSourceDrawerOpen(true)}><Wifi className="size-3.5" />管理</Button><Button size="sm" variant="secondary" onClick={() => void runCollectionBatch()} disabled={batchRunning || enabledCreators.length === 0}>{batchRunning ? <Loader2 className="size-3.5 animate-spin" /> : <RefreshCw className="size-3.5" />}手动补采</Button></div>
        </header>

        {error ? <Alert variant="destructive"><AlertTriangle className="size-4" /><AlertTitle>操作未完成</AlertTitle><AlertDescription>{error}</AlertDescription></Alert> : null}
        {message ? <Alert><CheckCircle2 className="size-4 text-emerald-600" /><AlertTitle>状态已更新</AlertTitle><AlertDescription>{message}</AlertDescription></Alert> : null}

        <Tabs value={workspace} onValueChange={(value) => { setWorkspace(value as CollectionPurpose); setSurface("report"); setTargetSearch("") }} className="gap-0">
          <div className="flex flex-wrap items-end justify-between gap-3 border-b border-border/70">
            <TabsList variant="line" className="h-10"><TabsTrigger value="market_sentiment" className="gap-2 px-1.5"><Activity className="size-3.5" />股市情绪感知</TabsTrigger><TabsTrigger value="market_analysis" className="gap-2 px-1.5"><BarChart3 className="size-3.5" />帖子行情分析</TabsTrigger></TabsList>
            <div className="flex items-center gap-1 pb-1"><Button size="sm" variant={surface === "report" ? "secondary" : "ghost"} onClick={() => setSurface("report")}><FileText className="size-3.5" />研究档案</Button><Button size="sm" variant={surface === "targets" ? "secondary" : "ghost"} onClick={() => setSurface("targets")}><Target className="size-3.5" />目标配置<span className="font-mono text-[10px] text-muted-foreground">{creatorsForWorkspace.length}</span></Button></div>
          </div>

          <TabsContent value="market_sentiment" className="mt-5">{surface === "report" ? <DailyReport
            purpose="market_sentiment"
            reportDate={reportDate}
            isToday={isToday}
            posts={workspacePosts}
            postsLoading={postsLoading}
            postsError={postsError}
            postsByPlatform={postsByPlatform}
            postsWithComments={postsWithComments.length}
            commentCount={commentCount}
            creators={creatorsForWorkspace}
            onPreviousDate={() => setReportDate((current) => shiftIsoDate(current, -1))}
            onNextDate={() => setReportDate((current) => shiftIsoDate(current, 1))}
            onToday={() => setReportDate(todayIsoDate())}
            onRefresh={() => void loadPosts()}
            onOpenRun={() => setRunDrawerOpen(true)}
            onOpenPost={openPostDetail}
          /> : <TargetManagement
            purpose="market_sentiment"
            creators={visibleCreators}
            totalCount={creatorsForWorkspace.length}
            targetSearch={targetSearch}
            targetPlatform={targetPlatform}
            onSearch={setTargetSearch}
            onPlatform={setTargetPlatform}
            onCreate={openCreateTarget}
            onEdit={openEditTarget}
            onToggle={toggleTarget}
            onRemove={removeTarget}
          />}</TabsContent>
          <TabsContent value="market_analysis" className="mt-5">{surface === "report" ? <DailyReport
            purpose="market_analysis"
            reportDate={reportDate}
            isToday={isToday}
            posts={workspacePosts}
            postsLoading={postsLoading}
            postsError={postsError}
            postsByPlatform={postsByPlatform}
            postsWithComments={postsWithComments.length}
            commentCount={commentCount}
            creators={creatorsForWorkspace}
            onPreviousDate={() => setReportDate((current) => shiftIsoDate(current, -1))}
            onNextDate={() => setReportDate((current) => shiftIsoDate(current, 1))}
            onToday={() => setReportDate(todayIsoDate())}
            onRefresh={() => void loadPosts()}
            onOpenRun={() => setRunDrawerOpen(true)}
            onOpenPost={openPostDetail}
          /> : <TargetManagement
            purpose="market_analysis"
            creators={visibleCreators}
            totalCount={creatorsForWorkspace.length}
            targetSearch={targetSearch}
            targetPlatform={targetPlatform}
            onSearch={setTargetSearch}
            onPlatform={setTargetPlatform}
            onCreate={openCreateTarget}
            onEdit={openEditTarget}
            onToggle={toggleTarget}
            onRemove={removeTarget}
          />}</TabsContent>
        </Tabs>

        <section className="flex flex-wrap items-center justify-between gap-3 border-t border-border/70 pt-3 text-xs text-muted-foreground"><div className="flex items-center gap-2"><Info className="size-3.5" />当前仅采集授权账号可见内容，不绕过验证码、访问限制或登录保护。</div><Button size="sm" variant="ghost" className="h-7 px-2" onClick={() => setRunDrawerOpen(true)}><History className="size-3.5" />运行记录</Button></section>
      </main>

      <SourceDrawer
        open={sourceDrawerOpen}
        onOpenChange={setSourceDrawerOpen}
        creators={creators}
        loginStatuses={loginStatuses}
        onLogin={(platform) => void openLogin(platform)}
      />
      <RunDrawer
        open={runDrawerOpen}
        onOpenChange={setRunDrawerOpen}
        purpose={workspace}
        runHistory={runHistory}
        runningJobs={runningJobs}
        batchRunning={batchRunning}
        onRun={() => void runCollectionBatch()}
      />
      <TargetEditor
        open={targetEditorOpen}
        onOpenChange={setTargetEditorOpen}
        form={editorForm}
        onChange={setEditorForm}
        onSave={saveTarget}
      />
      <LoginDialog
        open={loginOpen}
        onOpenChange={(open) => { if (!open) void cancelLogin() }}
        platform={dialogPlatform}
        session={loginSession}
        loading={loginLoading}
        blocked={loginBlocked}
        onCancel={() => void cancelLogin()}
        onCheck={() => void checkLogin()}
      />
      <PostDetailSheet
        open={detailOpen}
        onOpenChange={setDetailOpen}
        post={selectedPost}
        comments={comments}
        commentsLoading={commentsLoading}
        commentsError={commentsError}
        onRetry={() => { if (selectedPost) void openPostDetail(selectedPost) }}
      />
    </WorkspaceShell>
  )
}

function DailyReport({
  purpose,
  reportDate,
  isToday,
  posts,
  postsLoading,
  postsError,
  postsByPlatform,
  postsWithComments,
  commentCount,
  creators,
  onPreviousDate,
  onNextDate,
  onToday,
  onRefresh,
  onOpenRun,
  onOpenPost,
}: {
  purpose: CollectionPurpose
  reportDate: string
  isToday: boolean
  posts: SocialPost[]
  postsLoading: boolean
  postsError: string | null
  postsByPlatform: Record<SocialPlatform, SocialPost[]>
  postsWithComments: number
  commentCount: number
  creators: Creator[]
  onPreviousDate: () => void
  onNextDate: () => void
  onToday: () => void
  onRefresh: () => void
  onOpenRun: () => void
  onOpenPost: (post: SocialPost) => void
}) {
  const enabledCount = creators.filter((creator) => creator.enabled).length
  const isAnalysis = purpose === "market_analysis"

  return <div className="space-y-4">
    <section className="rounded-lg border border-border/70 bg-card">
      <div className="flex flex-wrap items-start justify-between gap-4 border-b border-border/70 px-5 py-4">
        <div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><span className="text-xs font-medium text-information">{isAnalysis ? "DAILY MARKET READING" : "DAILY SENTIMENT SNAPSHOT"}</span><Badge variant="outline" className="rounded-full">{isToday ? "今日档案" : "历史档案"}</Badge></div><h2 className="mt-1 text-lg font-semibold">{purposeLabels[purpose]}</h2><p className="mt-1 max-w-2xl text-xs leading-5 text-muted-foreground">{purposeDescriptions[purpose]}</p></div>
        <div className="flex flex-wrap items-center gap-2"><div className="flex items-center gap-1 rounded-md border border-border/70 bg-muted/30 p-1"><Button size="icon" variant="ghost" className="size-7" aria-label="前一天" title="前一天" onClick={onPreviousDate}><ChevronLeft className="size-4" /></Button><span className="min-w-32 px-2 text-center text-xs font-medium tabular-nums">{formatReportDate(reportDate)}</span><Button size="icon" variant="ghost" className="size-7" aria-label="后一天" title="后一天" onClick={onNextDate} disabled={isToday}><ChevronRight className="size-4" /></Button></div>{!isToday ? <Button size="sm" variant="ghost" onClick={onToday}>回到今天</Button> : null}<Button size="sm" variant="outline" onClick={onOpenRun}><History className="size-3.5" />批次详情</Button></div>
      </div>
      <div className="grid gap-0 divide-y divide-border/70 md:grid-cols-4 md:divide-x md:divide-y-0"><SummaryCell label="采集帖子" value={isToday ? String(posts.length) : "—"} detail="今日公开作品" /><SummaryCell label="已同步帖子" value={isToday ? String(postsWithComments) : "—"} detail="已完成评论快照的帖子" /><SummaryCell label="评论总量" value={isToday ? String(commentCount) : "—"} detail="当前可见评论" /><SummaryCell label="覆盖目标" value={`${enabledCount}/${creators.length}`} detail="启用目标 / 已配置目标" /></div>
    </section>

    {postsError ? <Alert variant="destructive"><AlertTriangle className="size-4" /><AlertTitle>采集数据加载失败</AlertTitle><AlertDescription className="flex flex-wrap items-center justify-between gap-2"><span>{postsError}</span><Button size="sm" variant="outline" onClick={onRefresh}>重试</Button></AlertDescription></Alert> : null}

    {!isToday ? <UnavailableArchive /> : <>
      <CollectedPostsPanel posts={posts} postsLoading={postsLoading} onRefresh={onRefresh} onOpenPost={onOpenPost} />
      {purpose === "market_sentiment" ? <SentimentReport posts={posts} postsByPlatform={postsByPlatform} postsLoading={postsLoading} onRefresh={onRefresh} /> : <AnalysisReport />}
    </>}
  </div>
}

function SummaryCell({ label, value, detail }: { label: string; value: string; detail: string }) {
  return <div className="px-5 py-3.5"><div className="text-xs text-muted-foreground">{label}</div><div className="mt-1 font-mono text-xl font-semibold tabular-nums text-foreground">{value}</div><div className="mt-0.5 text-[11px] text-muted-foreground">{detail}</div></div>
}

function CollectedPostsPanel({
  posts,
  postsLoading,
  onRefresh,
  onOpenPost,
}: {
  posts: SocialPost[]
  postsLoading: boolean
  onRefresh: () => void
  onOpenPost: (post: SocialPost) => void
}) {
  return <section className="overflow-hidden rounded-lg border border-border/70 bg-card">
    <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/70 px-5 py-4">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="text-base font-semibold">本次采集内容</h3>
          <Badge variant="secondary" className="font-mono text-[10px]">{posts.length} 篇帖子</Badge>
        </div>
        <p className="mt-1 text-xs leading-5 text-muted-foreground">先看采集是否完整，再进入单篇帖子查看评论快照。评论数来自最近一次同步结果。</p>
      </div>
      <Button size="sm" variant="outline" onClick={onRefresh} disabled={postsLoading}>
        {postsLoading ? <Loader2 className="size-3.5 animate-spin" /> : <RefreshCw className="size-3.5" />}
        刷新
      </Button>
    </div>
    {postsLoading ? <div className="space-y-3 p-5">
      {Array.from({ length: 5 }).map((_, index) => <div key={index} className="flex items-center gap-3 border-b border-border/60 pb-3 last:border-0">
        <Skeleton className="h-5 w-12" />
        <div className="min-w-0 flex-1 space-y-2"><Skeleton className="h-4 w-3/5" /><Skeleton className="h-3 w-2/5" /></div>
        <Skeleton className="h-4 w-12" />
        <Skeleton className="h-8 w-20" />
      </div>)}
    </div> : posts.length === 0 ? <div className="px-5 py-12 text-center">
      <Database className="mx-auto size-5 text-muted-foreground" />
      <p className="mt-3 text-sm font-medium">今天还没有可展示的帖子</p>
      <p className="mt-1 text-xs leading-5 text-muted-foreground">完成夜间采集后，帖子会出现在这里；如果已配置目标，可以先刷新或查看批次详情。</p>
    </div> : <Table className="min-w-[900px]">
      <TableHeader>
        <TableRow className="hover:bg-transparent">
          <TableHead className="w-24 px-5 text-xs text-muted-foreground">平台</TableHead>
          <TableHead className="min-w-[260px] text-xs text-muted-foreground">帖子</TableHead>
          <TableHead className="min-w-[150px] text-xs text-muted-foreground">来源</TableHead>
          <TableHead className="w-40 text-xs text-muted-foreground">采集时间</TableHead>
          <TableHead className="w-24 text-right text-xs text-muted-foreground">评论数</TableHead>
          <TableHead className="w-28 text-xs text-muted-foreground">评论同步</TableHead>
          <TableHead className="w-28 pr-5 text-right text-xs text-muted-foreground">操作</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {posts.map((post) => <TableRow key={`${post.platform}-${post.post_key}`}>
          <TableCell className="px-5"><Badge variant="outline" className="text-[10px]">{platformLabels[post.platform]}</Badge></TableCell>
          <TableCell className="max-w-[340px]">
            <div className="truncate text-sm font-medium">{post.title || "无标题作品"}</div>
            <div className="mt-1 truncate font-mono text-[10px] text-muted-foreground">{post.post_key}</div>
          </TableCell>
          <TableCell>
            <div className="max-w-[150px] truncate text-sm">{post.display_name || "未知博主"}</div>
            <div className="mt-1 max-w-[150px] truncate text-[10px] text-muted-foreground">{post.creator_key || "来源未标记"}</div>
          </TableCell>
          <TableCell className="font-mono text-xs text-muted-foreground">{formatDateTime(post.discovered_at || post.published_at)}</TableCell>
          <TableCell className="text-right font-mono text-sm font-semibold tabular-nums">{post.comment_count == null ? "—" : post.comment_count}</TableCell>
          <TableCell>
            {post.last_comment_sync_at ? <div className="flex items-center gap-1.5 text-xs text-emerald-700"><CheckCircle2 className="size-3.5" />已同步</div> : <div className="flex items-center gap-1.5 text-xs text-amber-700"><Clock3 className="size-3.5" />待同步</div>}
          </TableCell>
          <TableCell className="pr-5 text-right"><Button size="sm" variant="ghost" className="h-8 px-2" onClick={() => onOpenPost(post)}><MessageCircle className="size-3.5" />查看评论</Button></TableCell>
        </TableRow>)}
      </TableBody>
    </Table>}
  </section>
}

function SentimentReport({
  posts,
  postsByPlatform,
  postsLoading,
  onRefresh,
}: {
  posts: SocialPost[]
  postsByPlatform: Record<SocialPlatform, SocialPost[]>
  postsLoading: boolean
  onRefresh: () => void
}) {
  return <>
    <section className="rounded-lg border border-border/70 bg-card">
      <div className="border-b border-border/70 px-5 py-4"><div className="flex flex-wrap items-start justify-between gap-3"><div><h3 className="text-base font-semibold">情绪周期</h3><p className="mt-1 text-xs leading-5 text-muted-foreground">周期判断由评论主题、情绪分布和关注迁移共同生成，当前等待 AI 日度分析接入。</p></div><Badge variant="outline" className="gap-1.5 rounded-full border-amber-200 bg-amber-50 text-amber-700"><Clock3 className="size-3" />待生成</Badge></div></div>
      <div className="grid gap-6 px-5 py-5 xl:grid-cols-[minmax(0,1fr)_280px]"><div><div className="flex flex-wrap items-end gap-3"><div className="text-4xl font-semibold tracking-tight text-muted-foreground">—</div><div className="pb-1"><div className="text-sm font-semibold">尚未生成周期判断</div><div className="mt-1 text-xs text-muted-foreground">完成夜间采集后，由 AI 计算当前市场情绪阶段</div></div></div><div className="mt-6 flex items-center gap-0 overflow-x-auto pb-1">{emotionPhases.map((phase, index) => <div key={phase} className="flex min-w-[88px] flex-1 items-center"><div className="flex flex-col items-center gap-2"><span className={`flex size-7 items-center justify-center rounded-full border text-[11px] font-medium ${index === 0 ? "border-information bg-information/10 text-information" : "border-border bg-muted/40 text-muted-foreground"}`}>{index + 1}</span><span className="whitespace-nowrap text-[11px] text-muted-foreground">{phase}</span></div>{index < emotionPhases.length - 1 ? <span className="mx-2 mt-[-18px] h-px min-w-6 flex-1 bg-border" /> : null}</div>)}</div></div><div className="border-t border-border/70 pt-4 text-xs leading-6 text-muted-foreground xl:border-l xl:border-t-0 xl:pl-6 xl:pt-0"><div className="font-medium text-foreground">日报需要回答</div><div className="mt-2">当前处于什么周期？</div><div>周期正在升温还是退潮？</div><div>大多数人聚焦于什么？</div><div>情绪是否出现明显分歧？</div></div></div>
    </section>
    <section className="grid gap-4 lg:grid-cols-3"><ResearchPlaceholder title="今日关注主题" icon={<Target className="size-4" />} description="按讨论占比、增速和跨平台出现情况排列主题。" /><ResearchPlaceholder title="情绪结构" icon={<Activity className="size-4" />} description="展示积极、中性、谨慎和分歧等情绪结构。" /><ResearchPlaceholder title="关注迁移" icon={<BarChart3 className="size-4" />} description="解释主题进入、升温、降温和退出的变化。" /></section>
    <section className="rounded-lg border border-border/70 bg-muted/30 px-5 py-4"><div className="flex items-center gap-2 text-sm font-semibold"><Sparkles className="size-4 text-information" />AI 日度判断</div><p className="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">当前采集链路已经提供 {posts.length} 篇帖子作为输入，其中小红书 {postsByPlatform.xiaohongshu.length} 篇、抖音 {postsByPlatform.douyin.length} 篇。情绪主题和周期分析接口接入后，这里会生成可追溯的日度判断。</p><div className="mt-3 flex flex-wrap items-center gap-2"><Badge variant="secondary">评论输入待聚合</Badge><Badge variant="secondary">AI 分析待接入</Badge><Button size="sm" variant="outline" onClick={onRefresh} disabled={postsLoading}>{postsLoading ? <Loader2 className="size-3.5 animate-spin" /> : <RefreshCw className="size-3.5" />}刷新采集数据</Button></div></section>
  </>
}

function AnalysisReport() {
  return <>
    <section className="rounded-lg border border-border/70 bg-card"><div className="border-b border-border/70 px-5 py-4"><div className="flex flex-wrap items-start justify-between gap-3"><div><h3 className="text-base font-semibold">今日观点概览</h3><p className="mt-1 text-xs leading-5 text-muted-foreground">按主题聚合帖子内容，再由 AI 输出观点、行情映射和分歧。</p></div><Badge variant="outline" className="gap-1.5 rounded-full border-amber-200 bg-amber-50 text-amber-700"><Clock3 className="size-3" />待生成</Badge></div></div><div className="grid gap-6 px-5 py-5 xl:grid-cols-[minmax(0,1fr)_280px]"><div className="space-y-3"><ResearchPlaceholderRow label="核心叙事" detail="今日讨论最集中的市场叙事" /><ResearchPlaceholderRow label="行情映射" detail="观点涉及的指数、行业、股票或宏观变量" /><ResearchPlaceholderRow label="观点分歧" detail="不同来源之间的共识与冲突" /><ResearchPlaceholderRow label="新增变化" detail="相较前几日新增或反转的判断" /></div><div className="border-t border-border/70 pt-4 text-xs leading-6 text-muted-foreground xl:border-l xl:border-t-0 xl:pl-6 xl:pt-0"><div className="font-medium text-foreground">分析视角</div><div className="mt-2">主题优先，博主作为来源</div><div>原帖只在详情中打开</div><div>AI 结论必须标记日期和输入批次</div></div></div></section>
  </>
}

function ResearchPlaceholder({ title, icon, description }: { title: string; icon: React.ReactNode; description: string }) {
  return <section className="rounded-lg border border-border/70 bg-card p-4"><div className="flex items-center gap-2 text-sm font-semibold">{icon}{title}</div><p className="mt-2 text-xs leading-5 text-muted-foreground">{description}</p><div className="mt-5 rounded-md border border-dashed border-border px-3 py-5 text-center text-xs text-muted-foreground">分析结果将在日度 AI 接入后显示</div></section>
}

function ResearchPlaceholderRow({ label, detail }: { label: string; detail: string }) {
  return <div className="flex flex-wrap items-baseline justify-between gap-2 border-b border-border/60 pb-3 last:border-0 last:pb-0"><div className="text-sm font-medium">{label}</div><div className="text-xs text-muted-foreground">{detail}</div><div className="w-full text-xs text-muted-foreground">等待本日帖子完成主题聚合</div></div>
}

function UnavailableArchive() {
  return <section className="rounded-lg border border-dashed border-border bg-muted/20 px-5 py-12 text-center"><CalendarDays className="mx-auto size-5 text-muted-foreground" /><h3 className="mt-3 text-sm font-semibold">历史日报查询接口尚未接入</h3><p className="mx-auto mt-1 max-w-md text-xs leading-5 text-muted-foreground">页面已经预留按日期查看每日研究档案的结构。当前采集 API 只提供当天数据，因此历史日期暂不展示虚构内容。</p></section>
}

function TargetManagement({
  purpose,
  creators,
  totalCount,
  targetSearch,
  targetPlatform,
  onSearch,
  onPlatform,
  onCreate,
  onEdit,
  onToggle,
  onRemove,
}: {
  purpose: CollectionPurpose
  creators: Creator[]
  totalCount: number
  targetSearch: string
  targetPlatform: SocialPlatform | "all"
  onSearch: (value: string) => void
  onPlatform: (value: SocialPlatform | "all") => void
  onCreate: () => void
  onEdit: (creator: Creator) => void
  onToggle: (id: string) => void
  onRemove: (creator: Creator) => void
}) {
  return <section className="rounded-lg border border-border/70 bg-card"><div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/70 px-5 py-4"><div><div className="flex items-center gap-2"><SlidersHorizontal className="size-4 text-information" /><h2 className="text-base font-semibold">{purposeLabels[purpose]} · 目标配置</h2></div><p className="mt-1 text-xs leading-5 text-muted-foreground">当前支持博主目标；未来可扩展到关键词、话题和搜索入口。</p></div><Button onClick={onCreate}><Plus className="size-3.5" />添加目标</Button></div><div className="flex flex-wrap items-center gap-2 border-b border-border/70 px-5 py-3"><div className="relative"><Search className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" /><Input value={targetSearch} onChange={(event) => onSearch(event.target.value)} className="h-8 w-60 pl-8 text-xs" placeholder="搜索名称、标识或链接" /></div><Select value={targetPlatform} onValueChange={(value) => onPlatform(value as SocialPlatform | "all")}><SelectTrigger className="h-8 w-32 text-xs"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="all">全部平台</SelectItem>{platforms.map((platform) => <SelectItem key={platform} value={platform}>{platformLabels[platform]}</SelectItem>)}</SelectContent></Select><span className="ml-auto text-xs text-muted-foreground">{creators.length} / {totalCount} 个目标</span></div>{creators.length === 0 ? <div className="px-5 py-12 text-center"><Target className="mx-auto size-5 text-muted-foreground" /><p className="mt-3 text-sm font-medium">还没有配置目标</p><p className="mt-1 text-xs text-muted-foreground">添加目标后，夜间 Job 会按当前用途采集。</p></div> : <div className="divide-y divide-border/70">{creators.map((creator) => <div key={creator.id} className={`flex flex-wrap items-center gap-3 px-5 py-3 ${creator.enabled ? "" : "bg-muted/25"}`}><Badge variant="outline" className="shrink-0">{platformLabels[creator.platform]}</Badge><div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-semibold">{creator.displayName.slice(0, 1)}</div><div className="min-w-[12rem] flex-1"><div className="flex items-center gap-2"><span className={`truncate text-sm font-medium ${creator.enabled ? "" : "text-muted-foreground"}`}>{creator.displayName}</span><Badge variant="outline" className={creator.enabled ? "border-emerald-200 bg-emerald-50 text-emerald-700" : "border-border bg-muted text-muted-foreground"}>{creator.enabled ? "启用" : "已暂停"}</Badge></div><div className="mt-0.5 flex min-w-0 items-center gap-1 text-xs text-muted-foreground"><ExternalLink className="size-3 shrink-0" /><span className="truncate">{creator.profileUrl}</span></div></div><div className="flex items-center gap-1"><Button size="icon" variant="ghost" className="size-8" title="编辑目标" aria-label={`编辑${creator.displayName}`} onClick={() => onEdit(creator)}><Pencil className="size-3.5" /></Button><Button size="icon" variant="ghost" className="size-8" title={creator.enabled ? "暂停目标" : "恢复目标"} aria-label={creator.enabled ? `暂停${creator.displayName}` : `恢复${creator.displayName}`} onClick={() => onToggle(creator.id)}>{creator.enabled ? <CirclePause className="size-3.5" /> : <CirclePlay className="size-3.5" />}</Button><Button size="icon" variant="ghost" className="size-8 text-destructive hover:text-destructive" title="删除目标" aria-label={`删除${creator.displayName}`} onClick={() => onRemove(creator)}><Trash2 className="size-3.5" /></Button></div></div>)}</div>}</section>
}

function SourceDrawer({ open, onOpenChange, creators, loginStatuses, onLogin }: { open: boolean; onOpenChange: (open: boolean) => void; creators: Creator[]; loginStatuses: Partial<Record<SocialPlatform, SocialLoginStatus["status"]>>; onLogin: (platform: SocialPlatform) => void }) {
  return <Drawer direction="right" open={open} onOpenChange={onOpenChange}><DrawerContent className="h-full max-h-none w-full overflow-y-auto sm:max-w-md"><DrawerHeader className="border-b border-border/70"><DrawerTitle>数据源状态</DrawerTitle><DrawerDescription>登录和连接状态只影响后续夜间批次，不影响历史研究档案。</DrawerDescription></DrawerHeader><div className="space-y-5 px-5 py-5"><div className="space-y-3">{platforms.map((platform) => { const status = loginStatuses[platform] ?? "probing"; const platformCreators = creators.filter((creator) => creator.platform === platform); return <div key={platform} className="rounded-lg border border-border/70 p-4"><div className="flex items-start justify-between gap-3"><div className="flex items-center gap-2"><div className="flex size-8 items-center justify-center rounded-md bg-muted text-xs font-semibold">{platform === "xiaohongshu" ? "红" : "音"}</div><div><div className="text-sm font-semibold">{platformLabels[platform]}</div><div className="mt-1 flex items-center gap-1.5 text-xs text-muted-foreground">{sourceStatusIcon(status)}{statusLabel(status)}</div></div></div><Button size="sm" variant="outline" onClick={() => onLogin(platform)}><LogIn className="size-3.5" />{status === "authenticated" ? "重新登录" : "登录"}</Button></div><div className="mt-4 grid grid-cols-2 gap-3 border-t border-border/60 pt-3 text-xs"><div><div className="text-muted-foreground">配置目标</div><div className="mt-1 font-mono text-sm">{platformCreators.length}</div></div><div><div className="text-muted-foreground">启用目标</div><div className="mt-1 font-mono text-sm">{platformCreators.filter((creator) => creator.enabled).length}</div></div></div></div> })}</div><div className="rounded-lg bg-muted/35 px-4 py-3 text-xs leading-5 text-muted-foreground"><div className="flex items-center gap-2 font-medium text-foreground"><ShieldCheck className="size-3.5" />采集边界</div><div className="mt-2">仅访问授权账号可见的公开内容；平台要求重新登录或额外验证时，夜间批次会明确标记为异常。</div></div></div></DrawerContent></Drawer>
}

function RunDrawer({ open, onOpenChange, purpose, runHistory, runningJobs, batchRunning, onRun }: { open: boolean; onOpenChange: (open: boolean) => void; purpose: CollectionPurpose; runHistory: RunRecord[]; runningJobs: Partial<Record<`${SocialPlatform}:${SocialJobKind}`, boolean>>; batchRunning: boolean; onRun: () => void }) {
  return <Drawer direction="right" open={open} onOpenChange={onOpenChange}><DrawerContent className="h-full max-h-none w-full overflow-y-auto sm:max-w-xl"><DrawerHeader className="border-b border-border/70"><DrawerTitle>运行记录</DrawerTitle><DrawerDescription>{purposeLabels[purpose]} · 每日夜间批次的采集链路和产出摘要。</DrawerDescription></DrawerHeader><div className="space-y-5 px-5 py-5"><div className="flex flex-wrap items-center justify-between gap-3 rounded-lg bg-muted/35 px-4 py-3"><div><div className="text-sm font-semibold">今日批次</div><div className="mt-1 text-xs text-muted-foreground">当前 UI 仅展示本次会话触发的记录，历史 Job 查询接口待接入。</div></div><Button size="sm" onClick={onRun} disabled={batchRunning}>{batchRunning ? <Loader2 className="size-3.5 animate-spin" /> : <RefreshCw className="size-3.5" />}补跑今日批次</Button></div><div className="space-y-2">{platforms.flatMap((platform) => (["discovery", "comments"] as SocialJobKind[]).map((kind) => { const latest = runHistory.find((run) => run.platform === platform && run.kind === kind); const running = runningJobs[jobStateKey(platform, kind)] === true; return <div key={`${platform}-${kind}`} className="flex items-center gap-3 border-b border-border/60 py-3 last:border-0"><div className={`flex size-7 shrink-0 items-center justify-center rounded-full ${running ? "bg-information/10 text-information" : latest?.status === "success" ? "bg-emerald-50 text-emerald-600" : "bg-muted text-muted-foreground"}`}>{running ? <Loader2 className="size-3.5 animate-spin" /> : latest?.status === "success" ? <Check className="size-3.5" /> : <Database className="size-3.5" />}</div><div className="min-w-0 flex-1"><div className="text-sm font-medium">{platformLabels[platform]} · {kind === "discovery" ? "作品发现" : "评论同步"}</div><div className="mt-1 text-xs text-muted-foreground">{running ? "正在执行" : latest ? `${formatDateTime(latest.createdAt)} · 写入 ${latest.rowsWritten} 条` : "尚未在本次会话执行"}</div></div><Badge variant="outline" className={running ? "border-information/30 bg-information/5 text-information" : latest?.status === "success" ? "border-emerald-200 bg-emerald-50 text-emerald-700" : ""}>{running ? "运行中" : latest?.status ?? "待执行"}</Badge></div> }))}</div><div className="border-t border-border/70 pt-4"><div className="flex items-center gap-2 text-sm font-semibold"><History className="size-4" />本次会话明细</div>{runHistory.length === 0 ? <div className="mt-3 rounded-md border border-dashed border-border px-4 py-6 text-center text-xs text-muted-foreground">暂无手动触发记录。日常调度记录将在 Job 查询接口接入后显示。</div> : <div className="mt-3 space-y-2">{runHistory.map((run) => <div key={run.id} className="rounded-md border border-border/70 px-3 py-2.5 text-xs"><div className="flex flex-wrap items-center justify-between gap-2"><span className="font-medium">{platformLabels[run.platform]} · {run.kind === "discovery" ? "作品发现" : "评论同步"}</span><Badge variant="secondary" className="text-[10px]">{run.status}</Badge></div><div className="mt-1.5 flex flex-wrap gap-x-3 gap-y-1 text-muted-foreground"><span>{formatDateTime(run.createdAt)}</span><span>写入 {run.rowsWritten} 条</span>{run.runId ? <span className="font-mono">{run.runId}</span> : null}</div></div>)}</div>}</div></div></DrawerContent></Drawer>
}

function TargetEditor({ open, onOpenChange, form, onChange, onSave }: { open: boolean; onOpenChange: (open: boolean) => void; form: CreatorEditor; onChange: React.Dispatch<React.SetStateAction<CreatorEditor>>; onSave: () => void }) {
  return <Sheet open={open} onOpenChange={onOpenChange}><SheetContent className="w-full sm:max-w-lg"><SheetHeader><SheetTitle>{form.id ? "编辑采集目标" : "添加采集目标"}</SheetTitle><SheetDescription>目标属于当前用途；小红书和抖音可以同时配置。</SheetDescription></SheetHeader><div className="space-y-5 px-5 pb-6"><label className="block space-y-1.5 text-xs font-medium text-muted-foreground">采集用途<Select value={form.purpose} onValueChange={(value) => onChange((current) => ({ ...current, purpose: value as CollectionPurpose }))}><SelectTrigger className="w-full bg-background"><SelectValue /></SelectTrigger><SelectContent>{purposes.map((purpose) => <SelectItem key={purpose} value={purpose}>{purposeLabels[purpose]}</SelectItem>)}</SelectContent></Select></label><label className="block space-y-1.5 text-xs font-medium text-muted-foreground">平台<Select value={form.platform} onValueChange={(value) => onChange((current) => ({ ...current, platform: value as SocialPlatform }))}><SelectTrigger className="w-full bg-background"><SelectValue /></SelectTrigger><SelectContent>{platforms.map((platform) => <SelectItem key={platform} value={platform}>{platformLabels[platform]}</SelectItem>)}</SelectContent></Select></label><label className="block space-y-1.5 text-xs font-medium text-muted-foreground">博主标识<Input value={form.creatorKey} onChange={(event) => onChange((current) => ({ ...current, creatorKey: event.target.value }))} placeholder="例如 creator-001" /></label><label className="block space-y-1.5 text-xs font-medium text-muted-foreground">主页链接<Input value={form.profileUrl} onChange={(event) => onChange((current) => ({ ...current, profileUrl: event.target.value }))} placeholder="粘贴平台主页 URL" /></label><label className="block space-y-1.5 text-xs font-medium text-muted-foreground">显示名称（可选）<Input value={form.displayName} onChange={(event) => onChange((current) => ({ ...current, displayName: event.target.value }))} placeholder="便于识别" /></label><div className="flex justify-end gap-2"><Button variant="outline" onClick={() => onOpenChange(false)}>取消</Button><Button onClick={onSave}>{form.id ? "保存修改" : "添加目标"}</Button></div></div></SheetContent></Sheet>
}

function LoginDialog({ open, onOpenChange, platform, session, loading, blocked, onCancel, onCheck }: { open: boolean; onOpenChange: (open: boolean) => void; platform: SocialPlatform; session: SocialLoginSession | null; loading: boolean; blocked: boolean; onCancel: () => void; onCheck: () => void }) {
  return <Dialog open={open} onOpenChange={onOpenChange}><DialogContent className="max-w-2xl"><DialogHeader><DialogTitle>{platformLabels[platform]}登录</DialogTitle><DialogDescription>登录通过本机专用 Chrome 的 CDP 会话完成。请在对应平台窗口完成授权，后续夜间任务会复用该 Profile。</DialogDescription></DialogHeader><div className="grid gap-4 md:grid-cols-[minmax(0,1fr)_220px]"><div className="min-w-0 rounded-md border border-border/70 bg-muted/30 p-3">{loading && !session?.screenshot ? <div className="flex min-h-64 items-center justify-center text-sm text-muted-foreground"><Loader2 className="mr-2 size-4 animate-spin" />正在启动登录会话…</div> : session?.screenshot ? <img src={session.screenshot} alt={`${platformLabels[platform]}登录页面`} className="mx-auto max-h-[28rem] w-full object-contain" /> : <div className="flex min-h-64 flex-col items-center justify-center gap-2 px-6 text-center text-sm text-muted-foreground"><div className="font-medium text-foreground">请查看本机 Chrome 窗口</div><div className="max-w-sm text-xs leading-5">在平台页面完成登录，然后点击“检查登录状态”。</div></div>}</div><div className="space-y-4">{blocked ? <Alert variant="destructive"><AlertTriangle className="size-4" /><AlertTitle>专用 Chrome 未连接</AlertTitle><AlertDescription>请先运行本机采集器启动脚本，再重新点击登录。</AlertDescription></Alert> : <div className="rounded-md border border-border/70 bg-card p-4"><div className="flex items-center gap-2 text-sm font-semibold"><QrCode className="size-4" />操作步骤</div><ol className="mt-3 space-y-2 text-xs leading-5 text-muted-foreground"><li>1. 运行本机采集器启动脚本。</li><li>2. 在对应平台的专用 Chrome 窗口完成登录。</li><li>3. 保持 Chrome 和采集器运行。</li><li>4. 点击“检查登录状态”。</li></ol></div>}<div className={`rounded-md border px-3 py-2 text-xs ${statusClass(session?.status)}`}>当前状态：{statusLabel(session?.status)}{session?.message ? <div className="mt-1 leading-5">{session.message}</div> : null}</div></div></div><DialogFooter><Button variant="outline" onClick={onCancel}>取消</Button><Button onClick={onCheck} disabled={loading || !session?.session_id || blocked}>{loading ? <Loader2 className="size-3.5 animate-spin" /> : <CheckCircle2 className="size-3.5" />}{blocked ? "无法检查登录状态" : "检查登录状态"}</Button></DialogFooter></DialogContent></Dialog>
}

function PostDetailSheet({ open, onOpenChange, post, comments, commentsLoading, commentsError, onRetry }: { open: boolean; onOpenChange: (open: boolean) => void; post: SocialPost | null; comments: SocialComment[]; commentsLoading: boolean; commentsError: string | null; onRetry: () => void }) {
  return <Sheet open={open} onOpenChange={onOpenChange}><SheetContent className="w-full gap-0 p-0 sm:max-w-xl"><SheetHeader className="border-b border-border/60 px-5 py-4 pr-12 text-left"><SheetTitle className="truncate text-base">{post?.title || "帖子详情"}</SheetTitle><SheetDescription>{post?.display_name || post?.creator_key || "未知博主"} · {post ? platformLabels[post.platform] : ""}</SheetDescription></SheetHeader><div className="min-h-0 flex-1 overflow-y-auto">{post ? <div className="space-y-5 p-5"><section className="space-y-3 rounded-md bg-muted/45 p-4"><div className="flex items-start justify-between gap-3"><div className="min-w-0"><div className="text-xs text-muted-foreground">原帖入口</div><div className="mt-1 break-all font-mono text-[11px] text-muted-foreground">{post.post_key}</div></div><Button asChild size="sm" variant="outline" className="shrink-0"><a href={post.url} target="_blank" rel="noreferrer">打开原帖 <ExternalLink className="size-3.5" /></a></Button></div><div className="grid gap-3 text-xs sm:grid-cols-2"><div><div className="text-muted-foreground">发布时间</div><div className="mt-1 font-mono">{post.published_at ? formatDateTime(post.published_at) : "时间未知"}</div></div><div><div className="text-muted-foreground">评论数量</div><div className="mt-1 font-mono">{post.comment_count ?? 0}</div></div></div></section><section><div className="flex items-center justify-between gap-3"><div><h2 className="flex items-center gap-2 text-sm font-semibold"><MessageCircle className="size-4" />评论快照</h2><p className="mt-1 text-xs text-muted-foreground">帖子正文和 AI 解读将在分析 API 接入后补充。</p></div>{commentsError ? <Button size="sm" variant="outline" onClick={onRetry} disabled={commentsLoading}>重试</Button> : null}</div>{commentsLoading ? <div className="mt-4 space-y-4">{Array.from({ length: 4 }).map((_, index) => <div key={index} className="space-y-2 border-b border-border/60 pb-4"><Skeleton className="h-4 w-4/5" /><Skeleton className="h-3 w-2/5" /></div>)}</div> : commentsError ? <Alert variant="destructive" className="mt-4"><AlertTriangle className="size-4" /><AlertTitle>评论加载失败</AlertTitle><AlertDescription>{commentsError}</AlertDescription></Alert> : comments.length === 0 ? <div className="mt-4 rounded-md border border-dashed border-border/80 px-4 py-8 text-center text-xs text-muted-foreground">当前帖子尚未保存可见评论，或评论同步尚未完成。</div> : <div className="mt-4 divide-y divide-border/60">{comments.map((comment) => <article key={`${comment.platform}-${comment.comment_key}`} className="space-y-2 py-4 first:pt-0 last:pb-0"><div className="text-xs font-medium text-muted-foreground">{comment.author_name || "匿名用户"}</div><p className="whitespace-pre-wrap break-words text-sm leading-6">{comment.content || "（无文字内容）"}</p><div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground"><span>{comment.published_at ? formatDateTime(comment.published_at) : "时间未知"}</span><span className="inline-flex items-center gap-1"><MessageCircle className="size-3" />{comment.like_count ?? "未知"}</span><span className="inline-flex items-center gap-1"><Reply className="size-3" />{comment.reply_count ?? "未知"}</span></div></article>)}</div>}</section></div> : null}</div></SheetContent></Sheet>
}
