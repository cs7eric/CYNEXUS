import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"

const LOCAL_SOCIAL_AGENT_BASE = "http://127.0.0.1:18183"

export type SocialPlatform = "xiaohongshu" | "douyin"

export interface SocialLoginSession {
  session_id: string
  platform: SocialPlatform
  profile_name: string
  status: "waiting_login" | "authenticated" | "closed" | string
  message?: string | null
  screenshot?: string | null
  created_at?: string | null
  updated_at?: string | null
}

export interface SocialLoginStatus {
  platform: SocialPlatform
  profile_name: string
  status: "authenticated" | "waiting_login" | "blocked" | "unknown" | string
  message?: string | null
}

export interface SocialJobTriggerResult {
  accepted?: boolean
  run_id?: string
  job_key?: string
  status?: string
  result?: Record<string, unknown>
}

export interface SocialPost {
  id: number
  platform: SocialPlatform
  post_key: string
  url: string
  title?: string | null
  published_at?: string | null
  discovered_at?: string | null
  last_comment_sync_at?: string | null
  creator_key?: string | null
  display_name?: string | null
  comment_count?: number | null
}

export interface SocialComment {
  comment_key: string
  post_id: number
  platform: SocialPlatform
  parent_comment_key?: string | null
  content: string
  published_at?: string | null
  like_count?: number | null
  reply_count?: number | null
  author_platform_id_hash?: string | null
  author_name?: string | null
  first_seen_at?: string | null
  last_seen_at?: string | null
  source_fetched_at?: string | null
  raw_payload?: Record<string, unknown>
}

export async function startSocialLogin(
  platform: SocialPlatform,
  profileName = "default",
  probeUrl?: string,
): Promise<SocialLoginSession> {
  return localSocialRequest<SocialLoginSession>("/local/social/login-sessions", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ platform, profile_name: profileName, probe_url: probeUrl }),
  })
}

export async function fetchSocialLogin(sessionId: string): Promise<SocialLoginSession> {
  return localSocialRequest<SocialLoginSession>(`/local/social/login-sessions/${encodeURIComponent(sessionId)}`, {
    cache: "no-store",
  })
}

export async function completeSocialLogin(sessionId: string): Promise<SocialLoginSession> {
  return localSocialRequest<SocialLoginSession>(`/local/social/login-sessions/${encodeURIComponent(sessionId)}/complete`, {
    method: "POST",
    cache: "no-store",
  })
}

export async function closeSocialLogin(sessionId: string): Promise<SocialLoginSession> {
  return localSocialRequest<SocialLoginSession>(`/local/social/login-sessions/${encodeURIComponent(sessionId)}`, {
    method: "DELETE",
    cache: "no-store",
  })
}

export async function fetchSocialLoginStatus(
  platform: SocialPlatform,
  profileName = "default",
): Promise<SocialLoginStatus> {
  const query = new URLSearchParams({ platform, profile_name: profileName })
  return localSocialRequest<SocialLoginStatus>(`/local/social/login-status?${query.toString()}`, {
    cache: "no-store",
  })
}

async function localSocialRequest<T>(path: string, init: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetchWithRetry(`${LOCAL_SOCIAL_AGENT_BASE}${path}`, init, { retry: false })
  } catch {
    throw new Error("本机采集器未启动。请先运行 cynexus-data/scripts/start_social_local_agent.ps1，再点击登录。")
  }
  return cynexusResult<T>(response)
}

export async function triggerSocialJob(
  jobKey: "social_creator_discovery" | "social_comment_sync",
  platform: SocialPlatform,
  params: Record<string, unknown> = {},
): Promise<SocialJobTriggerResult> {
  return localSocialRequest<SocialJobTriggerResult>(`/local/jobs/${jobKey}/trigger`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      trigger_request_id: `social-ui-${jobKey}-${Date.now()}`,
      triggered_by: "social-monitoring-ui",
      dry_run: false,
      params: { platform, ...params },
    }),
  })
}

export async function fetchSocialPosts(
  platform: SocialPlatform,
  todayOnly = true,
  limit = 100,
): Promise<{ count: number; posts: SocialPost[] }> {
  const query = new URLSearchParams({
    platform,
    today_only: String(todayOnly),
    limit: String(limit),
  })
  return localSocialRequest<{ count: number; posts: SocialPost[] }>(`/local/social/posts?${query.toString()}`, {
    cache: "no-store",
  })
}

export async function fetchSocialPostComments(
  platform: SocialPlatform,
  postKey: string,
  limit?: number,
): Promise<{ post: SocialPost; count: number; comments: SocialComment[] }> {
  const query = new URLSearchParams({ platform })
  if (limit != null) query.set("limit", String(limit))
  return localSocialRequest<{ post: SocialPost; count: number; comments: SocialComment[] }>(
    `/local/social/posts/${encodeURIComponent(postKey)}/comments?${query.toString()}`,
    { cache: "no-store" },
  )
}
