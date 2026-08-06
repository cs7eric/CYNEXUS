import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

export interface WechatEventHistory {
  id: string
  messageId?: string | null
  direction?: string | null
  messageType?: string | null
  eventType?: string | null
  fromUserName?: string | null
  toUserName?: string | null
  content?: string | null
  processingStatus?: string | null
  imageContentType?: string | null
  imageSize?: number | null
  routeCode?: string | null
  routeConfidence?: number | string | null
  routeReason?: string | null
  routeSignals?: string[] | null
  routeWarnings?: string[] | null
  replyContent?: string | null
  errorMessage?: string | null
  occurredAt?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface WechatEventHistoryPage {
  items: WechatEventHistory[]
  total: number
  limit: number
  offset: number
}

export async function fetchWechatEventHistory(options: {
  limit?: number
  offset?: number
  messageType?: string
  processingStatus?: string
} = {}): Promise<WechatEventHistoryPage> {
  const query = new URLSearchParams({
    limit: String(Math.min(Math.max(options.limit ?? 25, 1), 200)),
    offset: String(Math.max(options.offset ?? 0, 0)),
  })
  if (options.messageType) query.set("messageType", options.messageType)
  if (options.processingStatus) query.set("processingStatus", options.processingStatus)

  return cynexusResult<WechatEventHistoryPage>(
    await fetchWithRetry(`${SERVICE_PREFIX.wechat}/history?${query.toString()}`, { cache: "no-store" }),
  )
}
