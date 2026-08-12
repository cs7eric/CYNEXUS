export type RotationPhase = "启动" | "加速" | "高潮" | "分歧" | "回流" | "退潮"

export interface MockPoolStock {
  code: string
  name: string
  height: number
  firstLimitTime: string
  changePct: number
}

export interface MockLadderLevel {
  height: number
  count: number
  leaders: string[]
  members?: MockPoolStock[]
}

export interface MockIndustryIntradaySnapshot {
  timestamp: number
  timeLabel: string
  phase: RotationPhase
  strengthScore: number
  rank: number
  relativeChangePct: number
  advancingCount: number
  decliningCount: number
  flatCount: number
  medianChangePct: number
  above5PctCount: number
  limitUpCount: number
  touchedLimitUpCount: number
  brokenLimitUpCount: number
  limitDownCount: number
  sealRate: number
  maxHeight: number
  promotionRate: number
  ladder: MockLadderLevel[]
}

export interface MockIndustryDailyState {
  tradeDate: string
  relativeChangePct: number
  strengthScore: number
  rank: number
  advancingPct: number
  advancingCount: number
  decliningCount: number
  flatCount: number
  medianChangePct: number
  limitUpCount: number
  touchedLimitUpCount: number
  brokenLimitUpCount: number
  limitDownCount: number
  above5PctCount: number
  maxHeight: number
  promotionRate: number
  failedPromotionCount: number
  ladder: MockLadderLevel[]
  limitUpPool: MockPoolStock[]
  limitDownPool: MockPoolStock[]
  brokenStocks: string[]
  mainNetAmountWan: number
  financingBalanceYi: number
  financingChange5dPct: number
}

export interface MockMarketStateEvent {
  timestamp: number
  timeLabel: string
  title: string
  tone: "up" | "down" | "warning" | "neutral"
}

export interface MockIndustryMarketState {
  industryName: string
  coverage: {
    thsCount: number
    tdxCount: number
    unionCount: number
    intersectionCount: number
    thsOnlyCount: number
    tdxOnlyCount: number
  }
  intraday: MockIndustryIntradaySnapshot[]
  daily: MockIndustryDailyState[]
  events: MockMarketStateEvent[]
}

export interface MockPoolIntradayPoint {
  timestamp: number
  timeLabel: string
  changePct: number
  averagePct: number
  volume: number
}

const MOCK_STOCK_UNIVERSE = [
  ["000062", "深圳华强"], ["000536", "华映科技"], ["000727", "冠捷科技"],
  ["002045", "国光电器"], ["002106", "莱宝高科"], ["002134", "天津普林"],
  ["002185", "华天科技"], ["002288", "超华科技"], ["002387", "维信诺"],
  ["002436", "兴森科技"], ["002456", "欧菲光"], ["002579", "中京电子"],
  ["002654", "万润科技"], ["002815", "崇达技术"], ["002845", "同兴达"],
  ["300256", "星星科技"], ["300322", "硕贝德"], ["300433", "蓝思科技"],
  ["300657", "弘信电子"], ["300735", "光弘科技"], ["300857", "协创数据"],
  ["600707", "彩虹股份"], ["600839", "四川长虹"], ["603228", "景旺电子"],
  ["603327", "福蓉科技"], ["603629", "利通电子"], ["605058", "澳弘电子"],
  ["688036", "传音控股"], ["688055", "龙腾光电"], ["688183", "生益电子"],
] as const

function hashSeed(value: string) {
  let seed = 2166136261
  for (let index = 0; index < value.length; index += 1) {
    seed ^= value.charCodeAt(index)
    seed = Math.imul(seed, 16777619)
  }
  return Math.abs(seed >>> 0)
}

function noise(seed: number, index: number) {
  const value = Math.sin(seed * 0.00001 + index * 12.9898) * 43758.5453
  return value - Math.floor(value)
}

function round(value: number, digits = 2) {
  const factor = 10 ** digits
  return Math.round(value * factor) / factor
}

function takeCyclic(values: string[], start: number, count: number) {
  return Array.from({ length: Math.min(count, values.length) }, (_, index) => values[(start + index) % values.length])
}

function chinaTimestamp(tradeDate: string, timeLabel: string) {
  return Date.parse(`${tradeDate}T${timeLabel}:00+08:00`)
}

function marketMinutes() {
  const values: string[] = []
  const append = (startHour: number, startMinute: number, count: number) => {
    for (let index = 0; index < count; index += 1) {
      const total = startHour * 60 + startMinute + index
      values.push(`${String(Math.floor(total / 60)).padStart(2, "0")}:${String(total % 60).padStart(2, "0")}`)
    }
  }
  append(9, 30, 121)
  append(13, 0, 121)
  return values
}

function previousTradingDates(tradeDate: string, count: number) {
  const result: string[] = []
  const current = new Date(`${tradeDate}T00:00:00Z`)
  while (result.length < count) {
    const day = current.getUTCDay()
    if (day !== 0 && day !== 6) result.push(current.toISOString().slice(0, 10))
    current.setUTCDate(current.getUTCDate() - 1)
  }
  return result.reverse()
}

function phaseAt(progress: number): RotationPhase {
  if (progress < 0.14) return "启动"
  if (progress < 0.4) return "加速"
  if (progress < 0.56) return "高潮"
  if (progress < 0.72) return "分歧"
  if (progress < 0.9) return "回流"
  return "退潮"
}

function ladderAt(progress: number, seed: number): MockLadderLevel[] {
  const high = progress < 0.18 ? 3 : progress < 0.66 ? 5 : progress < 0.88 ? 4 : 3
  const leaders = ["华映科技", "中京电子", "宝明科技", "协创数据", "东山精密"]
  return Array.from({ length: high }, (_, index) => {
    const height = high - index
    const count = Math.max(1, Math.round((high - height + 1) * (1.2 + noise(seed, index) * 1.6)))
    return {
      height,
      count,
      leaders: leaders.slice(index, index + Math.min(2, count)),
    }
  })
}

function dailyLadderAt(maxHeight: number, limitUpCount: number, seed: number, dayIndex: number) {
  let remaining = Math.max(maxHeight, limitUpCount)
  let memberOffset = (seed + dayIndex * 7) % MOCK_STOCK_UNIVERSE.length
  return Array.from({ length: maxHeight }, (_, levelIndex) => {
    const height = maxHeight - levelIndex
    const lowerLevels = height - 1
    const availableForLevel = Math.max(1, remaining - lowerLevels)
    const count = height === 1
      ? remaining
      : Math.max(1, Math.min(availableForLevel, 1 + Math.floor(noise(seed + dayIndex, height) * 3)))
    const members = Array.from({ length: count }, (_, memberIndex) => {
      const [code, name] = MOCK_STOCK_UNIVERSE[(memberOffset + memberIndex) % MOCK_STOCK_UNIVERSE.length]
      const minuteOffset = 7 + ((dayIndex * 11 + height * 17 + memberIndex * 13) % 235)
      const hour = minuteOffset <= 120 ? 9 + Math.floor((30 + minuteOffset) / 60) : 13 + Math.floor((minuteOffset - 121) / 60)
      const minute = minuteOffset <= 120 ? (30 + minuteOffset) % 60 : (minuteOffset - 121) % 60
      const limitPct = code.startsWith("30") || code.startsWith("68") ? 20 : 10
      return {
        code,
        name,
        height,
        firstLimitTime: `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`,
        changePct: round(limitPct - 0.02 - noise(seed + height, dayIndex + memberIndex) * 0.08),
      }
    })
    memberOffset = (memberOffset + count) % MOCK_STOCK_UNIVERSE.length
    remaining = Math.max(0, remaining - count)
    return { height, count, leaders: members.slice(0, 2).map((member) => member.name), members }
  })
}

export function buildPoolIntradayMock(tradeDate: string, stock: Pick<MockPoolStock, "code" | "height">) {
  const seed = hashSeed(`${tradeDate}:${stock.code}:pool-intraday`)
  let average = 0
  return marketMinutes().map((timeLabel, index, all) => {
    const progress = index / Math.max(1, all.length - 1)
    const openingGap = (noise(seed, 1) - 0.38) * 4.2
    const trend = (noise(seed, 2) - 0.42) * 7.5 * progress
    const wave = Math.sin(progress * Math.PI * (2.2 + noise(seed, 3) * 2.4)) * (0.7 + noise(seed, index) * 1.4)
    const pullback = progress > 0.38 && progress < 0.66 ? -stock.height * 0.24 : 0
    const changePct = round(Math.max(-10, Math.min(20, openingGap + trend + wave + pullback)), 2)
    average = index === 0 ? changePct : (average * index + changePct) / (index + 1)
    return {
      timestamp: chinaTimestamp(tradeDate, timeLabel),
      timeLabel,
      changePct,
      averagePct: round(average, 2),
      volume: Math.round(1800 + noise(seed + 13, index) * 14_000 * (index < 15 ? 1.8 : 1)),
    }
  })
}

export function buildIndustryMarketStateMock(
  tradeDate: string,
  industryName = "电子元件",
): MockIndustryMarketState {
  const seed = hashSeed(`${tradeDate}:${industryName}`)
  const unionCount = 196 + (seed % 13)
  const intersectionCount = 151 + (seed % 11)
  const thsOnlyCount = 24 + (seed % 8)
  const tdxOnlyCount = unionCount - intersectionCount - thsOnlyCount
  const thsCount = intersectionCount + thsOnlyCount
  const tdxCount = intersectionCount + tdxOnlyCount

  const intraday = marketMinutes().map((timeLabel, index, all) => {
    const progress = index / Math.max(1, all.length - 1)
    const impulse = Math.sin(progress * Math.PI * 1.35) * 18
    const divergence = progress > 0.55 && progress < 0.74 ? -12 : 0
    const rebound = progress > 0.72 ? Math.sin((progress - 0.72) * Math.PI * 2.4) * 10 : 0
    const strengthScore = Math.max(24, Math.min(94, 45 + impulse + divergence + rebound + noise(seed, index) * 8))
    const advancingPct = Math.max(0.28, Math.min(0.84, strengthScore / 105 + noise(seed + 7, index) * 0.08))
    const advancingCount = Math.round(unionCount * advancingPct)
    const flatCount = Math.max(2, Math.round(unionCount * (0.025 + noise(seed + 17, index) * 0.025)))
    const decliningCount = Math.max(0, unionCount - advancingCount - flatCount)
    const touchedLimitUpCount = Math.max(0, Math.round((progress * 12 + strengthScore / 18) * (0.72 + noise(seed + 31, index) * 0.45)))
    const brokenLimitUpCount = progress < 0.28 ? 0 : Math.min(touchedLimitUpCount, Math.round(touchedLimitUpCount * (progress > 0.55 ? 0.34 : 0.16)))
    const limitUpCount = Math.max(0, touchedLimitUpCount - brokenLimitUpCount)
    return {
      timestamp: chinaTimestamp(tradeDate, timeLabel),
      timeLabel,
      phase: phaseAt(progress),
      strengthScore: round(strengthScore, 1),
      rank: Math.max(1, Math.min(56, Math.round(39 - strengthScore * 0.38 + noise(seed + 11, index) * 8))),
      relativeChangePct: round((strengthScore - 45) / 17 + Math.sin(progress * Math.PI * 2) * 0.45),
      advancingCount,
      decliningCount,
      flatCount,
      medianChangePct: round((advancingPct - 0.5) * 6.8),
      above5PctCount: Math.max(0, Math.round(limitUpCount * 2.2 + strengthScore / 7)),
      limitUpCount,
      touchedLimitUpCount,
      brokenLimitUpCount,
      limitDownCount: strengthScore < 38 ? Math.round((38 - strengthScore) / 4) : 0,
      sealRate: touchedLimitUpCount ? round((limitUpCount / touchedLimitUpCount) * 100, 1) : 0,
      maxHeight: progress < 0.18 ? 3 : progress < 0.66 ? 5 : progress < 0.88 ? 4 : 3,
      promotionRate: round(Math.max(18, Math.min(76, strengthScore * 0.72 + noise(seed + 41, index) * 9)), 1),
      ladder: ladderAt(progress, seed),
    }
  })

  const dates = previousTradingDates(tradeDate, 240)
  let financingBalance = 118 + (seed % 17)
  const daily = dates.map((date, index) => {
    const progress = index / Math.max(1, dates.length - 1)
    const wave = Math.sin(progress * Math.PI * 2.3 + seed * 0.0001)
    const score = Math.max(28, Math.min(91, 54 + wave * 19 + noise(seed + 53, index) * 12))
    const touched = Math.max(1, Math.round(score / 7 + noise(seed + 61, index) * 5))
    const broken = Math.min(touched, Math.round(touched * (0.12 + noise(seed + 67, index) * 0.35)))
    const limitUpCount = Math.max(0, touched - broken)
    const advancingPct = round(Math.max(25, Math.min(86, score * 0.88 + noise(seed + 83, index) * 9)), 1)
    const flatCount = Math.max(2, Math.round(unionCount * (0.02 + noise(seed + 84, index) * 0.025)))
    const advancingCount = Math.max(0, Math.round(unionCount * advancingPct / 100))
    const decliningCount = Math.max(0, unionCount - advancingCount - flatCount)
    const maxHeight = score >= 76 ? 5 : score >= 62 ? 4 : score >= 48 ? 3 : 2
    const failedPromotionCount = Math.max(0, Math.round((maxHeight + touched / 4) * (0.35 + noise(seed + 86, index) * 0.7)))
    const leaders = ["华映科技", "中京电子", "宝明科技", "协创数据", "东山精密", "深纺织A", "联创电子"]
    const ladder = dailyLadderAt(maxHeight, limitUpCount, seed, index)
    const limitUpPool = ladder.flatMap((level) => level.members ?? [])
    const limitDownCount = score < 42 ? Math.round((42 - score) / 5) : 0
    const limitDownPool = Array.from({ length: Math.min(limitDownCount, 4) }, (_, memberIndex) => {
      const [code, name] = MOCK_STOCK_UNIVERSE[(seed + index * 13 + memberIndex + 11) % MOCK_STOCK_UNIVERSE.length]
      const minuteOffset = 24 + ((index * 7 + memberIndex * 19) % 190)
      const hour = minuteOffset <= 120 ? 9 + Math.floor((30 + minuteOffset) / 60) : 13 + Math.floor((minuteOffset - 121) / 60)
      const minute = minuteOffset <= 120 ? (30 + minuteOffset) % 60 : (minuteOffset - 121) % 60
      return {
        code,
        name,
        height: 0,
        firstLimitTime: `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`,
        changePct: round(-9.92 - noise(seed + 103, index + memberIndex) * 0.08),
      }
    })
    financingBalance += (noise(seed + 71, index) - 0.42) * 1.8
    return {
      tradeDate: date,
      relativeChangePct: round((score - 52) / 15 + (noise(seed + 73, index) - 0.5) * 0.8),
      strengthScore: round(score, 1),
      rank: Math.max(1, Math.min(56, Math.round(43 - score * 0.42 + noise(seed + 79, index) * 9))),
      advancingPct,
      advancingCount,
      decliningCount,
      flatCount,
      medianChangePct: round((advancingPct - 50) * 0.09 + (noise(seed + 85, index) - 0.5) * 0.8),
      limitUpCount,
      touchedLimitUpCount: touched,
      brokenLimitUpCount: broken,
      limitDownCount,
      above5PctCount: Math.max(limitUpCount, Math.round(limitUpCount * 2.1 + score / 8 + noise(seed + 88, index) * 5)),
      maxHeight,
      promotionRate: round(Math.max(12, Math.min(78, score * 0.7 + noise(seed + 89, index) * 8)), 1),
      failedPromotionCount,
      ladder,
      limitUpPool,
      limitDownPool,
      brokenStocks: takeCyclic(leaders, index + 2, Math.min(3, failedPromotionCount)),
      mainNetAmountWan: Math.round((score - 51) * 4300 + (noise(seed + 97, index) - 0.5) * 38_000),
      financingBalanceYi: round(financingBalance, 2),
      financingChange5dPct: round((noise(seed + 101, index) - 0.36) * 4.2),
    }
  })

  const eventDefinitions: Array<[string, string, MockMarketStateEvent["tone"]]> = [
    ["09:42", "上涨占比突破 55%，板块进入启动区", "up"],
    ["10:08", "首只涨停出现，连板梯队开始扩展", "up"],
    ["10:37", "行业强度排名进入全市场前 10", "up"],
    ["11:06", "炸板率升至 30%，盘中分歧扩大", "warning"],
    ["13:22", "午后资金回流，广度重新转强", "up"],
    ["14:12", "高位连板晋级，最高高度升至 5 板", "up"],
    ["14:46", "尾盘封板率回落，进入强分歧", "down"],
  ]
  const events = eventDefinitions.map(([timeLabel, title, tone]) => ({
    timestamp: chinaTimestamp(tradeDate, timeLabel),
    timeLabel,
    title,
    tone,
  }))

  return {
    industryName,
    coverage: {
      thsCount,
      tdxCount,
      unionCount,
      intersectionCount,
      thsOnlyCount,
      tdxOnlyCount,
    },
    intraday,
    daily,
    events,
  }
}
